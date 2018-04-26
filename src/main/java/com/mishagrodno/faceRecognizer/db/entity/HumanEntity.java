package com.mishagrodno.faceRecognizer.db.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * The HumanEntity entity.
 *
 * @author Gomanchuk Mikhail.
 */
@Entity
public class HumanEntity extends BaseEntity {

    /**
     * HumanEntity's first name.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * HumanEntity's faces.
     */
    @OneToMany
    private List<FaceEntity> faces = new ArrayList<>();

    public HumanEntity() {
    }

    /**
     * Gets HumanEntity's first name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets HumanEntity's first name.
     *
     * @param name first name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets HumanEntity's faces.
     *
     * @return name
     */
    public List<FaceEntity> getFaces() {
        return faces;
    }

    /**
     * Sets HumanEntity's faces.
     *
     * @param faces faces.
     */
    public void setFaces(List<FaceEntity> faces) {
        this.faces = faces;
    }

    /**
     * Adds face.
     *
     * @param face face.
     */
    public void addFace(final FaceEntity face) {
        if (!faces.contains(face)) {
            faces.add(face);
        }
    }

    /**
     * Deletes face.
     *
     * @param face face.
     */
    public void deleteFace(final FaceEntity face) {
        faces.remove(face);
    }
}
