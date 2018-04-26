package com.mishagrodno.faceRecognizer.db.entity;

import javax.persistence.*;

/**
 * The base implementation of a db entity.
 *
 * @author Gomanchuk Mikhail.
 */
@MappedSuperclass
public class BaseEntity {

    /**
     * Id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, precision = 15)
    protected Long id;

    public BaseEntity() {
    }

    /**
     * Gets id.
     *
     * @return id.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id id.
     */
    public void setId(Long id) {
        this.id = id;
    }
}
