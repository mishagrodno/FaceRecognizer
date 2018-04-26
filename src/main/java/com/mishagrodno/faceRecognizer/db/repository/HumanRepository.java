package com.mishagrodno.faceRecognizer.db.repository;

import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * The Human repository.
 *
 * @author Gomanchuk Mikhail.
 */
public interface HumanRepository extends PagingAndSortingRepository<HumanEntity, Long> {

    /**
     * Finds human by name.
     *
     * @param name name
     * @return human.
     */
    HumanEntity findByName(String name);
}
