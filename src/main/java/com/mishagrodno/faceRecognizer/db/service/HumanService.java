package com.mishagrodno.faceRecognizer.db.service;

import com.mishagrodno.faceRecognizer.db.entity.FaceEntity;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;

/**
 * The human service interface.
 *
 * @author Gomanchuk Mikhail.
 */
public interface HumanService {

    /**
     * Saves human.
     *
     * @param human human.
     * @return saved human.
     */
    HumanEntity save(HumanEntity human);

    /**
     * Gets human by id.
     *
     * @param id id
     * @return human.
     */
    HumanEntity get(Long id);

    /**
     * Gets user by name or creates new.
     *
     * @param name name.
     */
    HumanEntity getOrCreate(String name);

    /**
     * Deletes human by id.
     *
     * @param human human.
     * @return is delete successful.
     */
    void delete(HumanEntity human);

    /**
     * Gets all humans.
     *
     * @return all humans.
     */
    Iterable<HumanEntity> all();

    /**
     * Adds face to human.
     *
     * @param face  face.
     * @param human human.
     */
    void addFace(FaceEntity face, HumanEntity human);

    /**
     * Removes face from human.
     *
     * @param face  face.
     * @param human human.
     */
    void removeFace(FaceEntity face, HumanEntity human);
}
