/*
 *
 *  * All Application code is Copyright 2016, The Department of Homeland Security (DHS), U.S. Customs and Border Protection (CBP).
 *  *
 *  * Please see LICENSE.txt for details.
 *
 */

package gov.gtas.job.scheduler;

import gov.gtas.model.RuleHitDetail;
import gov.gtas.model.*;
import gov.gtas.repository.*;
import gov.gtas.repository.udr.RuleMetaRepository;
import gov.gtas.services.AppConfigurationService;
import gov.gtas.services.PassengerService;
import gov.gtas.services.RuleHitPersistenceService;
import gov.gtas.svc.GraphRulesService;
import gov.gtas.svc.TargetingResultServices;
import gov.gtas.svc.util.TargetingResultUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class GraphRulesThread implements Callable<Boolean> {

	private Logger logger = LoggerFactory.getLogger(GraphRulesThread.class);

	private final GraphRulesService graphRulesService;

	private final MessageStatusRepository messageStatusRepository;

	private final PassengerRepository passengerRepository;

	private final ApplicationContext applicationContext;

	private List<MessageStatus> messageStatuses = new ArrayList<>();

	private PassengerService passengerService;
	private AppConfigurationService appConfigurationService;
	private RuleMetaRepository ruleMetaRepository;

	public GraphRulesThread(GraphRulesService graphRulesService, MessageStatusRepository messageStatusRepository,
			ApplicationContext applicationContext, PassengerRepository passengerRepository,
			PassengerService passengerService, AppConfigurationService appConfigurationService,
			RuleMetaRepository ruleMetaRepository) {
		this.graphRulesService = graphRulesService;
		this.messageStatusRepository = messageStatusRepository;
		this.passengerRepository = passengerRepository;
		this.applicationContext = applicationContext;
		this.passengerService = passengerService;
		this.appConfigurationService = appConfigurationService;
		this.ruleMetaRepository = ruleMetaRepository;
	}

	public Boolean call() {
		RuleHitPersistenceService ruleHitPersistenceService = applicationContext
				.getBean(RuleHitPersistenceService.class);

		boolean returnVal = true;
		long start = System.nanoTime();
		List<MessageStatus> processedMessages = getMessageStatuses();
		if (processedMessages.isEmpty()) {
			return true;
		}

		try {
			Set<Long> messageId = processedMessages.stream().map(MessageStatus::getMessageId)
					.collect(Collectors.toSet());
			processedMessages.forEach(ms -> ms.setMessageStatusEnum(MessageStatusEnum.NEO_ANALYZED));
			Set<Passenger> passengers = passengerRepository.getPassengerWithIdInformation(messageId);
			Set<RuleHitDetail> graphHitDetailSet = graphRulesService.graphResults(passengers);
			TargetingResultServices targetingResultServices = getTargetingResultOptions();
			List<RuleHitDetail> filteredList = TargetingResultUtils
					.filterRuleHitDetails(new ArrayList<>(graphHitDetailSet), targetingResultServices);
			Set<HitDetail> hitDetails = graphRulesService.generateHitDetails(filteredList);

			int BATCH_SIZE = Integer.parseInt(appConfigurationService
					.findByOption(AppConfigurationRepository.MAX_FLIGHTS_SAVED_PER_BATCH).getValue());

			List<Set<HitDetail>> batchedTargetingServiceResults = TargetingResultUtils.batchResults(hitDetails,
					BATCH_SIZE);

			int count = 1;
			for (Set<HitDetail> hitDetailSet : batchedTargetingServiceResults) {
				try {
					logger.info("Saving batched graph results " + count + " of " + batchedTargetingServiceResults.size()
							+ "...");
					ruleHitPersistenceService.persistToDatabase(hitDetailSet);
				} catch (Exception ignored) {
					logger.warn("Exception saving hits summaries count " + count + " and/or hit details! ", ignored);
					processedMessages.forEach(ms -> ms.setMessageStatusEnum(MessageStatusEnum.FAILED_NEO_4_J));
				}
				count++;
			}
			if (!batchedTargetingServiceResults.isEmpty()) {
				logger.info("Graph Database Ran in " + (System.nanoTime() - start) / 1000000 + "m/s.");
			}
		} catch (Exception e) {
			logger.warn("Exception running graph rules! ", e);
			processedMessages.forEach(ms -> ms.setMessageStatusEnum(MessageStatusEnum.FAILED_NEO_4_J));
			returnVal = false;
		} finally {
			messageStatusRepository.saveAll(processedMessages);
		}
		return returnVal;
	}

	private List<MessageStatus> getMessageStatuses() {
		return messageStatuses;
	}

	void setMessageStatuses(List<MessageStatus> messageStatuses) {
		this.messageStatuses = messageStatuses;
	}

	private TargetingResultServices getTargetingResultOptions() {
		TargetingResultServices targetingResultServices = new TargetingResultServices();
		targetingResultServices.setAppConfigurationService(appConfigurationService);
		targetingResultServices.setPassengerService(passengerService);
		targetingResultServices.setRuleMetaRepository(ruleMetaRepository);
		return targetingResultServices;
	}
}