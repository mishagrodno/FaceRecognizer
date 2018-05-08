package com.mishagrodno.faceRecognizer.application;

import com.google.common.collect.Iterables;
import com.mishagrodno.faceRecognizer.db.entity.FaceEntity;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_face.LBPHFaceRecognizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.cvarrToMat;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;

/**
 * The Recognizer class.
 *
 * @author Gomanchuk Mikhail.
 */
@Component
public class Recognizer {

    private final Logger LOGGER = Logger.getLogger(Recognizer.class.getName());
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
     * @param faceData faces.
     * @return recognized face.
     */
    public int recognize(final IplImage faceData) throws IOException {
        final Mat face = cvarrToMat(faceData);
        final Mat grayFace = new Mat();
        cvtColor(face, grayFace, CV_BGR2GRAY);

        final IntPointer label = new IntPointer(1);
        final DoublePointer confidence = new DoublePointer(0);
        faceRecognizer.predict(grayFace, label, confidence);

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
            final Mat tmp = new Mat(Utils.convertToIplImage(faceImage));
            final Mat gray = new Mat();
            cvtColor(tmp, gray, CV_BGR2GRAY);
            return gray;
        } catch (final SQLException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
