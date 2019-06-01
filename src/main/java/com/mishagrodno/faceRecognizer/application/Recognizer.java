package com.mishagrodno.faceRecognizer.application;

import com.google.common.collect.Iterables;
import com.mishagrodno.faceRecognizer.db.entity.FaceEntity;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_face.LBPHFaceRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.stream.StreamSupport;

import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

/**
 * The Recognizer class.
 *
 * @author Gomanchuk Mikhail.
 */
@Component
public class Recognizer {

    private final Logger LOGGER = LoggerFactory.getLogger(Recognizer.class);
    private final FaceService faceService;
    private FaceRecognizer faceRecognizer;

    @Autowired
    public Recognizer(final FaceService faceService) {
        this.faceService = faceService;
    }

    /**
     * Initialization of recognizer.
     */
    public void init() {
        final Iterable<FaceEntity> faces = faceService.all();
        if (Iterables.isEmpty(faces)) {
            return;
        }

        final MatVector images = new MatVector();
        final Mat labels = new Mat(Iterables.size(faces), 1, CV_32SC1);
        final IntBuffer labelsBuffer = labels.createBuffer();

        StreamSupport.stream(faces.spliterator(), false).forEach(face -> {
            final Mat img = faceToMat(face);
            images.push_back(img);
            labelsBuffer.put(face.getOwner().getId().intValue());
        });

        faceRecognizer = LBPHFaceRecognizer.create();
        faceRecognizer.train(images, labels);
    }

    /**
     * Recognizes face.
     *
     * @param face face.
     * @return recognized face.
     */
    public int recognize(final Mat face) {
        final long start = System.currentTimeMillis();
        if (faceRecognizer == null || faceRecognizer.empty()) {
            return -1;
        }

        final IntPointer label = new IntPointer(1);
        final DoublePointer confidence = new DoublePointer(0);
        faceRecognizer.predict(face, label, confidence);

        LOGGER.info("Took: {}", (System.currentTimeMillis() - start));

        return confidence.get() > 60 ? -1 : label.get(0);
    }

    /**
     * Converts Blob to mat.
     *
     * @param face face.
     * @return Mat from blob.
     */
    private Mat faceToMat(final FaceEntity face) {
        try {
            final BufferedImage faceImage = ImageIO.read(face.getContent().getBinaryStream());
            final Mat gray = new Mat(Utils.convertToIplImage(faceImage));
            //final Mat gray = new Mat();
            imwrite("face_from_db.jpg", gray);
            return gray;
        } catch (final Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            return null;
        }
    }
}
