/*
 *
 *  * All Application code is Copyright 2016, The Department of Homeland Security (DHS), U.S. Customs and Border Protection (CBP).
 *  *
 *  * Please see LICENSE.txt for details.
 *
 */

package gov.gtas.repository;

import gov.gtas.model.NoteType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Component
public interface NoteTypeRepository extends CrudRepository<NoteType, Long> {

    Optional<NoteType> findByType(String type);

    Set<NoteType> findAllById(Set<Long> ids);
    
    List<NoteType> findAll();
}
