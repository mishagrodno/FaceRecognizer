package com.mishagrodno.faceRecognizer.db.service;

import com.mishagrodno.faceRecognizer.db.entity.FaceEntity;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;

/**
 * The face service interface.
 *
 * @author Gomanchuk Mikhail.
 */
public interface FaceService {

    /**
     * Saves face.
     *
     * @param face face.
     * @return saved face.
     */
    FaceEntity save(FaceEntity face);

    /**
     * Creates new face.
     *
     * @param name    name.
     * @param type    type.
     * @param height  height.
     * @param width   width.
     * @param content content.
     * @param owner   owner.
     */
    FaceEntity create(String name, Integer type, Integer height, Integer width, byte[] content, HumanEntity owner);

    /**
     * Gets face by id.
     *
     * @param id id.
     * @return face.
     */
    FaceEntity get(Long id);

    /**
     * Deletes face.
     *
     * @param face face.
     */
    void delete(FaceEntity face);

    /**
     * Gets all faces.
     *
     * @return all faces.
     */
    Iterable<FaceEntity> all();
}
