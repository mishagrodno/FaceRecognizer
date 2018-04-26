package com.mishagrodno.faceRecognizer.db.service.impl;

import com.mishagrodno.faceRecognizer.db.entity.FaceEntity;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;
import com.mishagrodno.faceRecognizer.db.repository.HumanRepository;
import com.mishagrodno.faceRecognizer.db.service.HumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main implementation for {@link HumanService}.
 *
 * @author Gomanchuk Mikhail.
 */
@Service
public class HumanServiceImpl implements HumanService {

    private final HumanRepository humanRepository;

    /**
     * {@inheritDoc}
     */
    @Autowired
    public HumanServiceImpl(final HumanRepository humanRepository) {
        this.humanRepository = humanRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HumanEntity save(final HumanEntity human) {
        return humanRepository.save(human);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HumanEntity get(final Long id) {
        return humanRepository.findById(id).orElse(null);
    }

    @Override
    public HumanEntity getOrCreate(final String name) {
        HumanEntity human = humanRepository.findByName(name);
        if (human == null) {
            human = new HumanEntity();
            human.setName(name);
            humanRepository.save(human);
        }

        return human;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(HumanEntity human) {
        humanRepository.delete(human);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<HumanEntity> all() {
        return humanRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFace(final FaceEntity face, final HumanEntity human) {
        if (human != null) {
            human.addFace(face);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFace(FaceEntity face, HumanEntity human) {
        if (human != null) {
            human.deleteFace(face);
        }
    }
}
