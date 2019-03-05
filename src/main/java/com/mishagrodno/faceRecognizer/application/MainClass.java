package com.mishagrodno.faceRecognizer.application;

import com.mishagrodno.faceRecognizer.FaceRecognizerApplication;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import com.mishagrodno.faceRecognizer.db.service.HumanService;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_dnn;
import org.bytedeco.javacpp.opencv_dnn.Net;
import org.bytedeco.javacpp.opencv_face.FacemarkKazemi;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
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
    private final MainForm mainForm = new MainForm();
    private final double scale = 1;

    private final Net net = new Net();

    private boolean isInitialization;

    private String frontalClassifierName = "frontal_face.xml";
    private String profileClassifierName = "profile_face_1.xml";

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
            grabber.setImageHeight(360);
            grabber.setImageWidth(480);

            System.out.println(grabber.getImageHeight());
            System.out.println(grabber.getImageWidth());

            grabber.start();

            Frame grabbedImage = grabber.grab();

            mainForm.create(grabbedImage.imageHeight, grabbedImage.imageWidth);
            mainForm.getReloadButton().addActionListener(e -> {
                isInitialization = true;
                recognizer.init();
                isInitialization = false;
            });
            //grabber.setFrameRate(grabber.getFrameRate());

            final URL frontalClassifierURL = getClass().getClassLoader().getResource(frontalClassifierName);

            final String frontalClassifierLocation = Loader
                    .extractResource(frontalClassifierURL, null, "frontal-classifier", ".xml")
                    .getAbsolutePath();

            final CascadeClassifier frontalClassifier = new CascadeClassifier(frontalClassifierLocation);

            final URL profileClassifierURL = getClass().getClassLoader().getResource(profileClassifierName);

            final String profileClassifierLocation = Loader
                    .extractResource(profileClassifierURL, null, "profile-classifier", ".xml")
                    .getAbsolutePath();

            final CascadeClassifier profileClassifier = new CascadeClassifier(profileClassifierLocation);

            recognizer.init();
            final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

            while (mainForm.getMainFrame().isVisible() && (grabbedImage = grabber.grab()) != null) {

                long now = System.currentTimeMillis();

                Mat matImage = toMat.convert(grabbedImage);

                Mat result = new Mat();

                resize(matImage, result, new Size((int) (matImage.size().width() / scale), (int) (matImage.size().height() / scale)));

                result = recognize(result, 0.0, frontalClassifier, profileClassifier);
                result = recognize(result, -40.0, frontalClassifier, profileClassifier);

                result = Utils.rotate(result, 40.0);
                result = Utils.fit(result, (int) (grabbedImage.imageWidth / scale), (int) (grabbedImage.imageHeight / scale));

                result = recognize(result, 40.0, frontalClassifier, profileClassifier);
                result = Utils.rotate(result, -40.0);

                //Mat result = recognize(toMat.convert(grabbedImage), 0.0, cascade);

                result = Utils.fit(result, (int) (grabbedImage.imageWidth / scale), (int) (grabbedImage.imageHeight / scale));

                Mat normalSizedResult = new Mat();
                resize(result, normalSizedResult, new Size(matImage.size().width(), matImage.size().height()));

                mainForm.getImage().setIcon(new ImageIcon(Utils.convertToBufferedImage(normalSizedResult)));
                mainForm.getMainFrame().pack();

                System.out.println(System.currentTimeMillis() - now);
            }
            grabber.stop();
            mainForm.getMainFrame().dispose();
            Utils.safeDelete(Paths.get(frontalClassifierLocation));

        } catch (final IOException e) {
            Logger.getLogger(FaceRecognizerApplication.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private Mat recognize(Mat grabbedImage, double angle, CascadeClassifier frontalClassifier, CascadeClassifier profileClassifier) throws IOException {
        final Mat rotated = Utils.rotate(grabbedImage, angle);

        final RectVector frontalFacesVector = new RectVector();
        final RectVector profileFacesVector = new RectVector();
        final RectVector profileFacesVector1 = new RectVector();

        frontalClassifier.detectMultiScale(rotated, frontalFacesVector, 1.1, 4, 1, new Size(),
                new Size(rotated.size().width(), rotated.size().height()));

        draw(frontalFacesVector, rotated, Scalar.BLUE);

        profileClassifier.detectMultiScale(rotated, profileFacesVector, 1.1, 5, 1, new Size(),
                new Size(rotated.size().width(), rotated.size().height()));

        draw(profileFacesVector, rotated, Scalar.RED);

        final Mat mirrowed = Utils.mirrow(rotated);
        profileClassifier.detectMultiScale(mirrowed, profileFacesVector1, 1.1, 5, 1, new Size(),
                new Size(rotated.size().width(), rotated.size().height()));

        draw(profileFacesVector1, mirrowed, Scalar.GREEN);

        return Utils.mirrow(mirrowed);
    }

    private void draw(RectVector facesVector, Mat image, Scalar color) throws IOException {
        final Rect[] rects = facesVector.get();
        for (final Rect rectangle : rects) {
            final Mat cropped = new Mat(image, rectangle);
            final Mat resizedCrop = new Mat();

            resize(cropped, resizedCrop, new Size((int) (cropped.size().width() * scale), (int) (cropped.size().height() * scale)));

            if (mainForm.isToSave()) {
                final BufferedImage bi = Utils.convertToBufferedImage(resizedCrop);
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "jpg", baos);

                final Mat mat = new Mat(cropped);
                final String name = mainForm.getTextName().getText();
                final HumanEntity human = humanService.getOrCreate(name);

                faceService.create(name, mat.type(), resizedCrop.size().height(), resizedCrop.size().width(), baos.toByteArray(), human);
                mainForm.setToSave(false);
            }

            rectangle(image, rectangle, color, 3, CV_AA, 0);

            if (isInitialization) continue;
            final int recognizedFace = recognizer.recognize(resizedCrop);

            final HumanEntity human = humanService.get((long) recognizedFace);
            final String text = human == null ? "Unknown" : human.getName();
            putText(image, text, new Point(rectangle.x(), rectangle.y() - 10), 1, 2.0, Scalar.BLUE);
        }
    }
}
