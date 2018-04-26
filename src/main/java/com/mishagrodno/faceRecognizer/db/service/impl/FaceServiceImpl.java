package com.mishagrodno.faceRecognizer.db.service.impl;

import com.mishagrodno.faceRecognizer.db.entity.FaceEntity;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;
import com.mishagrodno.faceRecognizer.db.repository.FaceRepository;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.LobCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManagerFactory;
import javax.sql.rowset.serial.SerialBlob;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import static org.hibernate.Hibernate.getLobCreator;

/**
 * Main implementation for {@link FaceService}.
 *
 * @author Gomanchuk Mikhail.
 */
@Service
public class FaceServiceImpl implements FaceService {

    private final FaceRepository faceRepository;

    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    public FaceServiceImpl(final FaceRepository faceRepository, final EntityManagerFactory entityManagerFactory) {
        this.faceRepository = faceRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FaceEntity save(final FaceEntity face) {
        return faceRepository.save(face);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FaceEntity create(final String name, final Integer type, final Integer height, final Integer width,
                             final byte[] content, final HumanEntity owner) {

        try {
            final Blob blob = new SerialBlob(content);
            final FaceEntity faceEntity = new FaceEntity();
            faceEntity.setContent(blob);
            faceEntity.setOwner(owner);
            faceEntity.setType(type);
            faceEntity.setHeight(height);
            faceEntity.setWidth(width);

            return faceRepository.save(faceEntity);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FaceEntity get(final Long id) {
        return faceRepository.findById(id).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final FaceEntity face) {
        faceRepository.delete(face);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<FaceEntity> all() {
        return faceRepository.findAll();
    }

    private FaceEntity createFaceEntity(final Integer type, final Integer height, final Integer width,
                                        final InputStream content, final long contentLength, final HumanEntity owner) {

        final SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

        try (final Session session = sessionFactory.openSession()) {

            final LobCreator lobCreator = getLobCreator(session);
            final Blob blob = lobCreator.createBlob(content, contentLength);
            final FaceEntity faceEntity = new FaceEntity();
            faceEntity.setContent(blob);
            faceEntity.setOwner(owner);
            faceEntity.setType(type);
            faceEntity.setHeight(height);
            faceEntity.setWidth(width);

            faceRepository.save(faceEntity);

            return faceEntity;
        }
    }
}
