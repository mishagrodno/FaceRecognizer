package com.mishagrodno.faceRecognizer.db.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.sql.Blob;

/**
 * The face entity.
 *
 * @author Gomanchuk Mikhail.
 */
@Entity
public class FaceEntity extends BaseEntity {

    /**
     * Content.
     */
    @Column(nullable = false)
    private Blob content;

    /**
     * Type.
     */
    @Column
    private Integer type;

    /**
     * Width.
     */
    @Column
    private Integer width;

    /**
     * Height.
     */
    @Column
    private Integer height;

    /**
     * Owner.
     */
    @ManyToOne
    private HumanEntity owner;

    public FaceEntity() {
    }

    /**
     * Gets content.
     *
     * @return content.
     */
    public Blob getContent() {
        return content;
    }

    /**
     * Sets content.
     *
     * @param content content.
     */
    public void setContent(Blob content) {
        this.content = content;
    }

    /**
     * Gets type.
     *
     * @return type.
     */
    public Integer getType() {
        return type;
    }

    /**
     * Sets type.
     *
     * @param type content.
     */
    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * Gets width.
     *
     * @return width.
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * Sets width.
     *
     * @param width content.
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Gets height.
     *
     * @return height.
     */
    public Integer getHeight() {
        return height;
    }

    /**
     * Sets height.
     *
     * @param height content.
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Gets owner.
     *
     * @return owner.
     */
    public HumanEntity getOwner() {
        return owner;
    }

    /**
     * Sets owner.
     *
     * @param owner owner.
     */
    public void setOwner(HumanEntity owner) {
        this.owner = owner;
    }
}
