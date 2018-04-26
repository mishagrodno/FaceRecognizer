package com.mishagrodno.faceRecognizer.db.repository;

import com.mishagrodno.faceRecognizer.db.entity.FaceEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * The Face repository.
 *
 * @author Gomanchuk Mikhail.
 */
public interface FaceRepository extends PagingAndSortingRepository<FaceEntity, Long> {
}
