package com.mishagrodno.faceRecognizer.application;

import com.mishagrodno.faceRecognizer.FaceRecognizerApplication;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import com.mishagrodno.faceRecognizer.db.service.HumanService;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_face.FacemarkKazemi;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Application class.
 *
 * @author Gomanchuk Mikhail.
 */
@Component
public class MainClass {

    private final FaceService faceService;
    private final Recognizer recognizer;
    private final HumanService humanService;
    private final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
    private final MainForm mainForm = new MainForm();
    private boolean isInitialization;

    private String classifierName = "frontal_face.xml";

    @Autowired
    public MainClass(FaceService faceService, Recognizer recognizer, HumanService humanService) {
        this.faceService = faceService;
        this.recognizer = recognizer;
        this.humanService = humanService;
    }

    /**
     * Starts face recognizing from webCam.
     */
    public void start() {
        try {
            final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
            grabber.start();
            Frame grabbedImage = grabber.grab();

            mainForm.create(grabbedImage.imageHeight, grabbedImage.imageWidth);
            mainForm.getReloadButton().addActionListener(e -> {
                isInitialization = true;
                recognizer.init();
                isInitialization = false;
            });
            grabber.setFrameRate(grabber.getFrameRate());

            final URL classifierURL = getClass().getClassLoader().getResource(classifierName);

            final String classifierLocation = Loader
                    .extractResource(classifierURL, null, "classifier", ".xml")
                    .getAbsolutePath();

            final CascadeClassifier cascade = new CascadeClassifier(classifierLocation);

            final FacemarkKazemi facemark = FacemarkKazemi.create();

            final URL landmarkURL = getClass().getClassLoader().getResource("face_landmark_model.dat");

            final String landmarkLocation = Loader
                    .extractResource(landmarkURL, null, "landmark", ".dat")
                    .getAbsolutePath();

            facemark.loadModel(landmarkLocation);

            recognizer.init();
            final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

            while (mainForm.getMainFrame().isVisible() && (grabbedImage = grabber.grab()) != null) {

                Mat result = recognize(toMat.convert(grabbedImage), 0.0, cascade);
                result = recognize(result, -40.0, cascade);

                result = Utils.rotate(result, 40.0);
                result = Utils.fit(result, grabbedImage.imageWidth, grabbedImage.imageHeight);

                result = recognize(result, 40.0, cascade);
                result = Utils.rotate(result, -40.0);

                //Mat result = recognize(toMat.convert(grabbedImage), 0.0, cascade);

                result = Utils.fit(result, grabbedImage.imageWidth, grabbedImage.imageHeight);

                mainForm.getImage().setIcon(new ImageIcon(Utils.convertToBufferedImage(result)));
                mainForm.getMainFrame().pack();
            }
            grabber.stop();
            mainForm.getMainFrame().dispose();
            Utils.safeDelete(Paths.get(classifierLocation));

        } catch (final IOException e) {
            Logger.getLogger(FaceRecognizerApplication.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private Mat recognize(Mat grabbedImage, double angle, CascadeClassifier cascade) throws IOException {
        long first = System.currentTimeMillis();
        final Mat rotated = Utils.rotate(grabbedImage, angle);

        final RectVector facesVector = new RectVector();

        cascade.detectMultiScale(rotated, facesVector, 1.1, 4, 1, new Size(),
                new Size(rotated.size().width(), rotated.size().height()));

        draw(facesVector, rotated);

        System.out.println("angle: " + angle + "detect time: " + (System.currentTimeMillis() - first));

        return rotated;
    }

    private void draw(RectVector facesVector, Mat image) throws IOException {
        final Rect[] rects = facesVector.get();
        for (int i = 0; i < rects.length; i++) {
            final Rect rectangle = rects[i];

            final Mat cropped = new Mat(image, rectangle);

            if (mainForm.isToSave()) {
                final BufferedImage bi = Utils.convertToBufferedImage(cropped);
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "jpg", baos);

                final Mat mat = new Mat(cropped);
                final String name = mainForm.getTextName().getText();
                final HumanEntity human = humanService.getOrCreate(name);

                faceService.create(name, mat.type(), cropped.size().height(), cropped.size().width(), baos.toByteArray(), human);
                mainForm.setToSave(false);
            }

            rectangle(image, rectangle, Scalar.BLUE, 3, CV_AA, 0);

            if (isInitialization) continue;
            final int recognizedFace = recognizer.recognize(cropped);

            final HumanEntity human = humanService.get((long) recognizedFace);
            final String text = human == null ? "Unknown" : human.getName();
            putText(image, text, new Point(rectangle.x(), rectangle.y() - 10), 1, 2.0, Scalar.BLUE);
        }
    }
}
