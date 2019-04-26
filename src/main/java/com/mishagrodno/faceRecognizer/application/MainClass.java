package com.mishagrodno.faceRecognizer.application;

import com.mishagrodno.faceRecognizer.FaceRecognizerApplication;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import com.mishagrodno.faceRecognizer.db.service.HumanService;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
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
    private final MainForm mainForm = new MainForm();
    private final double scale = 1.0;

    private String frontalClassifierName = "frontal_face.xml";
    private String profileClassifierName = "haar_eyes.xml";

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

            grabber.start();

            Frame grabbedImage = grabber.grab();

            mainForm.create(grabbedImage.imageHeight, grabbedImage.imageWidth);
            mainForm.getReloadButton().addActionListener(e -> {
                recognizer.init();
            });

            final URL frontalClassifierURL = getClass().getClassLoader().getResource(frontalClassifierName);

            final String frontalClassifierLocation = Loader
                    .extractResource(frontalClassifierURL, null, "frontal-classifier", ".xml")
                    .getAbsolutePath();

            final CascadeClassifier frontalClassifier = new CascadeClassifier(frontalClassifierLocation);

            final URL profileClassifierURL = getClass().getClassLoader().getResource(profileClassifierName);

            final String eyesClassifierLocation = Loader
                    .extractResource(profileClassifierURL, null, "eyes-classifier", ".xml")
                    .getAbsolutePath();

            final CascadeClassifier eyesClassifier = new CascadeClassifier(eyesClassifierLocation);

            recognizer.init();
            final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

            while (mainForm.getMainFrame().isVisible() && (grabbedImage = grabber.grab()) != null) {

                long now = System.currentTimeMillis();

                Mat matImage = toMat.convert(grabbedImage);

                resize(matImage, matImage, new Size((int) (matImage.size().width() / scale), (int) (matImage.size().height() / scale)));

                final RectVector eyes = detectEyes(matImage, eyesClassifier);

                final List<Boolean> used = new ArrayList<>();
                for (int i = 0; i < eyes.size(); i++) {
                    used.add(false);
                }

                for (int i = 0; i < eyes.size() - 1; i++) {
                    if (used.get(i)) {
                        continue;
                    }
                    for (int j = i + 1; j < eyes.size(); j++) {
                        if (used.get(j)) {
                            continue;
                        }
                        final RecognizedFace recognizedFace = checkFace(matImage, eyes.get(i), eyes.get(j), frontalClassifier);
                        if (recognizedFace.isRecognized()) {
                            used.set(i, true);
                            used.set(j, true);

                            final Rect rect = new Rect(recognizedFace.getX(), recognizedFace.getY(),
                                    recognizedFace.getWidth(), recognizedFace.getHeight());

                            draw(rect, matImage, recognizedFace.getAngle(), Scalar.BLUE);
                        }
                    }
                }

                final Mat normalSizedResult = new Mat();
                resize(matImage, normalSizedResult, new Size(matImage.size().width(), matImage.size().height()));

                mainForm.getImage().setIcon(new ImageIcon(Utils.convertToBufferedImage(normalSizedResult)));
                mainForm.getMainFrame().pack();

                System.out.println("Took: " + (System.currentTimeMillis() - now));
            }
            grabber.stop();
            mainForm.getMainFrame().dispose();
            Utils.safeDelete(Paths.get(frontalClassifierLocation));

        } catch (final Exception e) {
            Logger.getLogger(FaceRecognizerApplication.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private RectVector detectEyes(Mat image, CascadeClassifier eyesClassifier) {
        final RectVector frontalEyesVector = new RectVector();
        eyesClassifier.detectMultiScale(image, frontalEyesVector, 1.1, 5, 1, new Size(),
                new Size(image.size().width(), image.size().height()));

        final RectVector filteredEyes = new RectVector();

        for (Rect rect : frontalEyesVector.get()) {
            if (rect.height() > 70 || rect.width() > 70) {
                continue;
            }

            filteredEyes.push_back(rect);
        }

        return filteredEyes;
    }

    private RecognizedFace checkFace(Mat image, Rect eye1, Rect eye2, CascadeClassifier frontalClassifier) throws Exception {
        Rect left, right;
        if (eye1.x() < eye2.x()) {
            left = eye1;
            right = eye2;
        } else {
            left = eye2;
            right = eye1;
        }

        final int maxWidth = Math.max(eye1.width(), eye2.width());
        final int maxHeight = Math.max(eye1.height(), eye2.height());

        final int faceX = Math.max(0, left.x() - 2 * maxWidth);
        final int faceY = Math.max(0, left.y() - 4 * maxHeight);

        final int faceWidth = Math.max(0, Math.min(7 * maxWidth, image.size().width() - faceX));
        final int faceHeight = Math.max(0, Math.min(9 * maxHeight, image.size().height() - faceY));

        final Rect faceRectangle = new Rect(faceX, faceY, faceWidth, faceHeight);
        final Mat cropped = new Mat(image, faceRectangle);

        final int rightCenterX = right.x() + right.width() / 2;
        final int rightCenterY = right.y() + right.height() / 2;

        final int leftCenterX = left.x() + left.width() / 2;
        final int leftCenterY = left.y() + left.height() / 2;

        double angle = Math.atan((double) (rightCenterY - leftCenterY) / (double) (rightCenterX - leftCenterX));

        final RecognizedFace face = getRecognizedFace(image, frontalClassifier, cropped, Math.toDegrees(angle));

        final int x = face.getX();
        final int y = face.getY();

        final double W = face.getFace().size().width();

        double f;

        int newX = 0, newY = 0;

        if (angle <= 0) {
            angle = Math.toRadians(90) + angle;

            final double l = Math.sqrt((Math.pow(faceWidth * cos(angle) - y, 2) + (W - x) * (W - x)));
            final double b = Math.abs(Math.atan((faceWidth * cos(angle) - y) / (W - x)));

            if (y <= faceWidth * cos(angle)) {
                f = Math.toRadians(90) - angle - b;
                newX = (int) (faceX + faceWidth - l * cos(f));
                newY = (int) (faceY + l * sin(f));
            }

            if (y > faceWidth * cos(angle)) {
                f = angle - b;
                newX = (int) (faceX + faceWidth - l * sin(f));
                newY = (int) (faceY + l * cos(f));
            }

            face.setX(newX);
            face.setY(newY);

            angle = angle - Math.toRadians(90);
        } else {

            final double l = Math.sqrt((Math.pow(faceHeight * cos(angle) - y, 2) + (W - x) * (W - x)));
            final double b = Math.abs(Math.atan((faceHeight * cos(angle) - y) / (W - x)));

            if (y <= faceHeight * cos(angle)) {
                f = Math.toRadians(90) - angle - b;
                newX = (int) (faceX + faceWidth - l * sin(f));
                newY = (int) (faceY + faceHeight - l * cos(f));
            }

            if (y > faceHeight * cos(angle)) {
                f = angle - b;
                newX = (int) (faceX + faceWidth - l * cos(f));
                newY = (int) (faceY + faceHeight - l * sin(f));
            }

            face.setX(newX);
            face.setY(newY);

        }

        face.setAngle(angle);

        return face;
    }

    private RecognizedFace getRecognizedFace(Mat image, CascadeClassifier frontalClassifier, Mat cropped, double angle) {
        final RecognizedFace result = detect(cropped, angle, frontalClassifier);

        if (result.isRecognized()) {
            return result;
        }

        return new RecognizedFace(false, image);
    }

    private RecognizedFace detect(Mat image, double angle, CascadeClassifier frontalClassifier) {
        final Mat rotated = Utils.rotate(image, angle);

        final RectVector frontalFacesVector = new RectVector();

        frontalClassifier.detectMultiScale(rotated, frontalFacesVector, 1.1, 3, 1, new Size(),
                new Size(rotated.size().width(), rotated.size().height()));

        final RectVector filteredFaces = new RectVector();
        for (Rect rect : frontalFacesVector.get()) {
            if (rect.height() < 0.25 * image.size().height() || rect.width() < 0.25 * image.size().width()) {
                continue;
            }

            filteredFaces.push_back(rect);
            break;
        }

        final Rect face = filteredFaces.size() > 0 ? filteredFaces.get(0) : null;

        return face != null ? new RecognizedFace(true, rotated, face.x(), face.y(), face.height(), face.width()) :
                new RecognizedFace(false, rotated);
    }

    private void draw(Rect face, Mat image, double angle, Scalar color) {

        if (face.x() < 0 || face.y() < 0) {
            return;
        }

        final int x = face.x();
        final int y = face.y();
        final int w = face.width();
        final int h = face.height();

        final Point ru = new Point((int) (x + w * cos(angle)), (int) (y + w * sin(angle)));
        final Point ld = new Point((int) (x - h * sin(angle)), (int) (y + h * cos(angle)));
        final Point rd = new Point((int) (ld.x() + w * cos(angle)), (int) (ld.y() + w * sin(angle)));

        line(image, new Point(x, y), ru, color, 3, CV_AA, 0);
        line(image, new Point(x, y), ld, color, 3, CV_AA, 0);
        line(image, ld, rd, color, 3, CV_AA, 0);
        line(image, ru, rd, color, 3, CV_AA, 0);
    }


    private class RecognizedFace {

        private boolean recognized;

        private Mat face;

        private int x;

        private int y;

        private int height;

        private int width;

        private double angle;

        public RecognizedFace() {
        }

        public RecognizedFace(boolean recognized, Mat face) {
            this.recognized = recognized;
            this.face = face;
        }

        public RecognizedFace(boolean recognized, Mat face, int x, int y, int height, int width) {
            this.recognized = recognized;
            this.face = face;
            this.x = x;
            this.y = y;
            this.height = height;
            this.width = width;
        }

        public boolean isRecognized() {
            return recognized;
        }

        public void setRecognized(boolean recognized) {
            this.recognized = recognized;
        }

        public Mat getFace() {
            return face;
        }

        public void setFace(Mat face) {
            this.face = face;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public double getAngle() {
            return angle;
        }

        public void setAngle(double angle) {
            this.angle = angle;
        }
    }
}
