SELECT c.*, 
	   f.`id` "flight.id",
       f.`eta` "flight.eta", 
       f.`eta_date` "flight.eta_date", 
       f.`etd` "flight.etd", 
       f.`etd_date` "flight.etd_date", 
       f.`flight_date` "flight.flight_date", 
       f.`flight_number` "flight.flight_number", 
       f.`full_flight_number` "flight.full_flight_number", 
       f.`origin` "flight.origin", 
       f.`origin_country` "flight.origin_country", 
--        f.`rule_hit_count` "flight.rule_hit_count", 
       f.`passenger_count` "flight.passenger_count",
       f.`direction` "flight.direction",
       seat.`number` "flight.seat_number",
       
       p.`id` "passenger.paxid",
       p.`age` "passenger.age",
       p.`citizenship_country` "passenger.citizenship_country",
       p.`debark_country` "passenger.debark_country",
       p.`debarkation` "passenger.debarkation",
       debark_ar.longitude "passenger.debarkation.lon",
       debark_ar.latitude "passenger.debarkation.lat",
       embark_ar.longitude "passenger.embarkation.lon",
       embark_ar.latitude "passenger.embarkation.lat",
       p.`dob` "passenger.dob",
       p.`embark_country` "passenger.embark_country",
       p.`embarkation` "passenger.embarkation",
       p.`first_name` "passenger.first_name",
       p.`last_name` "passenger.last_name",
       p.`gender` "passenger.gender",
       p.`middle_name` "passenger.middle_name",
       p.`days_visa_valid` "passenger.days_visa_valid",
       p.`passenger_type` "passenger.passenger_type",
       p.`ref_number` "passenger.ref_number",
       p.`residency_country` "passenger.residency_country",
       p.`suffix` "passenger.suffix",
       p.`travel_frequency` "passenger.travel_frequency",
       p.`watchlistCheckTimestamp` "passenger.watchlistCheckTimestamp",
       
       hd.`id` "hit_disposition.id",
       hd.`status` "hit_disposition.status",
	   hd.created_at "hit_disposition.created_at",
	   hd.created_by "hit_disposition.created_by",
	   hd.updated_at "hit_disposition.updated_at",
	   hd.updated_by "hit_disposition.updated_by",
	   hd.description "hit_disposition.description",
	   hd.hit_id "hit_disposition.hit_id",
	   hd.valid "hit_disposition.valid",
	   hd.rule_cat_id "hit_disposition.rule_cat_id",
	   
	   hdc.id "hit_disposition.comment.id",
	   hdc.created_at "hit_disposition.comment.created_at",
	   hdc.created_by "hit_disposition.comment.created_by",
	   hdc.updated_at "hit_disposition.comment.updated_at",
	   hdc.updated_by "hit_disposition.comment.updated_by",
	   hdc.comments "hit_disposition.comment.comments",
	   hdc.hit_id "hit_disposition.comment.hit_id",
	   hdc.`hit_disp_id` "hit_disposition.comment.hit_disp_id",
	   
	   h_summary.`created_date` "h_summary_created_date",
	   h_summary.`hit_type` "h_summary_hit_type",
	   h_summary.`id` "h_summary.id",
	   h_summary.`rule_hit_count` "h_summary_rule_hit_count",
	   h_summary.`wl_hit_count` "h_summary_wl_hit_count",
	   h_summary.`flight_id` "h_summary_flight_id",
	   h_summary.`passenger_id` "h_summary_passenger_id",
	   
	   h_detail.`cond_text` "h_detail_cond_text",
	   h_detail.`created_date` "h_detail_created_date",
	   h_detail.`description` "h_detail_description",
	   h_detail.`hit_type` "h_detail_hit_type",
	   h_detail.`hits_summary_id` "h_detail_hits_summary_id",
	   h_detail.`id` "h_detail.id",
	   h_detail.`rule_id` "h_detail_rule_id",
	   h_detail.`title` "h_detail_title",

          	pax_watchlist.`id` "pax_watchlist.id",
	pax_watchlist.`last_run_timestamp` "pax_watchlist.last_run_timestamp",
	pax_watchlist.`passenger_id` "pax_watchlist.passenger_id",
	pax_watchlist.`percent_match` "pax_watchlist.percent_match",
	pax_watchlist.`verified_status` "pax_watchlist.verified_status",
	pax_watchlist.`watchlist_item_id` "pax_watchlist.watchlist_item_id"
	   	   
FROM   cases c 
       LEFT JOIN flight f 
              ON ( c.flightid = f.id )
       LEFT JOIN passenger p
              ON (c.paxId = p.id)
       LEFT JOIN `hits_disposition` hd
       		  ON (hd.`case_id` = c.`id`)
       LEFT JOIN `hits_disposition_comments` hdc
       		  ON (hd.`id` = hdc.`hit_disp_id`)
       left join `hits_summary` h_summary
       		  on (c.`paxId` = h_summary.`passenger_id` and c.`flightId` = h_summary.`flight_id`)
       left join `hit_detail` h_detail
       		  on (h_summary.`id` = h_detail.`hits_summary_id`)
       left join `airport` debark_ar
       		  on (debark_ar.iata=p.debarkation and debark_ar.country=p.debark_country)
        left join `airport` embark_ar
       		  on (embark_ar.iata=p.embarkation and embark_ar.country=p.embark_country)
       left join `pax_watchlist_link` pax_watchlist
		on (pax_watchlist.passenger_id=p.id)
       left join `seat` seat 
              on seat.flight_id=f.ID and seat.passenger_id=p.id