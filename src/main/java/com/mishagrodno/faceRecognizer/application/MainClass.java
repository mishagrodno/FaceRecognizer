package com.mishagrodno.faceRecognizer.application;

import com.mishagrodno.faceRecognizer.FaceRecognizerApplication;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import com.mishagrodno.faceRecognizer.db.service.HumanService;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
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

    private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MainClass.class);

    private final FaceService faceService;
    private final Recognizer recognizer;
    private final Detector detector;
    private final HumanService humanService;
    private final MainForm mainForm = new MainForm();
    private final double scale = 1.2;

    private boolean needSave;

    public MainClass(FaceService faceService, Recognizer recognizer, Detector detector, HumanService humanService) {
        this.faceService = faceService;
        this.recognizer = recognizer;
        this.detector = detector;
        this.humanService = humanService;
    }

    /**
     * Starts face recognizing from webCam.
     */
    public void start() {
        try {
            final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(2);
            grabber.setImageHeight(1280);
            grabber.setImageWidth(720);

            grabber.start();

            Frame grabbedImage = grabber.grab();

            mainForm.create(grabbedImage.imageHeight, grabbedImage.imageWidth);
            mainForm.getReloadButton().addActionListener(e -> recognizer.init());
            mainForm.getSaveButton().addActionListener(e -> {
                needSave = true;
                recognizer.init();
            });

            recognizer.init();
            final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

            while (mainForm.getMainFrame().isVisible() && (grabbedImage = grabber.grab()) != null) {
                long start = System.currentTimeMillis();
                int recognized = 0;

                final Mat matImage = toMat.convert(grabbedImage);

                Mat resized = new Mat();
                resize(matImage, resized, new Size((int) (matImage.size().width() / scale), (int) (matImage.size().height() / scale)));
                cvtColor(resized, resized, CV_BGR2GRAY);

                final IplImage imageIpl = new IplImage(resized);
                final IplImage cannyIpl = cvCreateImage(cvGetSize(imageIpl), IPL_DEPTH_8U, 1);

                cvCanny(imageIpl, cannyIpl, 50, 200, 3);
                cvSub(imageIpl, cannyIpl, cannyIpl);

                final Mat canny = new Mat(cannyIpl);

                //imwrite("canny.jpg", canny);

                final Mat grayResized = resized.clone();
                resized = canny.clone();

                //equalizeHist(resized, resized);
                //imwrite("hist.jpg", resized);

                LOGGER.info("Resized ready");

                final List<Rect> eyes = detector.detectEyes(resized);

                //eyes.forEach(eye -> draw(eye, matImage, 0, Scalar.RED));

                LOGGER.info("{} eye(s) detected", eyes.size());

                final List<Boolean> used = new ArrayList<>();
                for (int i = 0; i < eyes.size(); i++) {
                    used.add(false);
                }

                /*final List<Rect> resultFaces = detector.detectFaces(resized);

                resultFaces.forEach(face -> {
                    final Rect f = recalc(face, new Rect(matImage.size()), 0.0);
                    draw(f, matImage, 0.0, Scalar.BLUE);
                });*/

                final List<Rect> resultFaces = new ArrayList<>();

                for (int i = 0; i < eyes.size() - 1; i++) {
                    if (used.get(i)) {
                        continue;
                    }
                    for (int j = i + 1; j < eyes.size(); j++) {
                        if (used.get(j)) {
                            continue;
                        }

                        final Rect eye1 = eyes.get(i);
                        final Rect eye2 = eyes.get(j);

                        if (distance(eye1, eye2) > Math.max(eye1.width(), eye2.width()) * 2) {
                            continue;
                        }

                        final double angle = faceAngle(eye1, eye2);
                        final Rect faceArea = faceArea(resized, eye1, eye2);

                        final Mat rotated = Utils.rotate(new Mat(resized.clone(), faceArea), Math.toDegrees(angle));

                        final List<Rect> faces = detector.detectFaces(rotated);
                        LOGGER.info("Faces detected");
                        if (!CollectionUtils.isEmpty(faces)) {
                            final Rect face = recalc(faces.get(0), faceArea, angle);

                            //draw(eye1, matImage, 0.0, Scalar.RED);
                            //draw(eye2, matImage, 0.0, Scalar.RED);

                            used.set(i, true);
                            used.set(j, true);

                            if (resultFaces.stream()
                                    .noneMatch(fc -> Utils.contains(fc, face))) {
                                resultFaces.add(face);
                                final Mat faceImage = new Mat(Utils.rotate(new Mat(grayResized, faceArea(grayResized, eye1, eye2)),
                                        Math.toDegrees(angle)), faces.get(0));
                                if (needSave) {
                                    saveFace(faceImage);
                                }

                                final int id = recognizer.recognize(faceImage);
                                final HumanEntity human = humanService.get((long) id);
                                final String text = human == null ? "Unknown" : human.getName();
                                if (human != null) {
                                    recognized++;
                                    LOGGER.info("{} recognized", text);
                                }
                                draw(face, matImage, angle, Scalar.BLUE, text);
                            }

                        }

                        rotated.release();
                    }
                }

                final Mat normalSizedResult = new Mat();
                resize(matImage, normalSizedResult, new Size(matImage.size().width(), matImage.size().height()));

                mainForm.getImage().setIcon(new ImageIcon(Utils.convertToBufferedImage(normalSizedResult)));
                mainForm.getMainFrame().pack();

                System.out.println("Release");

                matImage.release();
                canny.release();
                resized.release();
                //cvReleaseImage(imageIpl);
                cvReleaseImage(cannyIpl);
                normalSizedResult.release();
                grayResized.release();

                System.out.println("Faces found: " + resultFaces.size() + ", recognized: " + recognized + " ,took: " + (System.currentTimeMillis() - start));
                System.out.println("-------------------------------------------------");

            }
            grabber.stop();
            mainForm.getMainFrame().dispose();
            //Utils.safeDelete(Paths.get(frontalClassifierLocation));

        } catch (final Exception e) {
            Logger.getLogger(FaceRecognizerApplication.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void saveFace(Mat face) throws Exception {
        imwrite("saved_face.jpg", face);

        final String name = mainForm.getTextName().getText();
        final HumanEntity human = humanService.getOrCreate(name);

        final BufferedImage bi = Utils.convertToBufferedImage(face);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", baos);

        //byte[] bytes = new byte[(face.rows() * face.cols() * face.channels())];
        //face.data().get(bytes);


        //mat.data().put(bytes);
        //imwrite("face_from_db.jpg", mat);

        faceService.create(name, face.type(), face.size().height(), face.size().width(), baos.toByteArray(), human);
        needSave = false;
    }

    private double distance(Rect rect1, Rect rect2) {
        final int rect1CenterX = rect1.x() + rect1.width() / 2;
        final int rect1CenterY = rect1.y() + rect1.height() / 2;

        final int rect2CenterX = rect2.x() + rect2.width() / 2;
        final int rect2CenterY = rect2.y() + rect2.height() / 2;

        return Math.sqrt(Math.pow(rect1CenterX - rect2CenterX, 2) + Math.pow(rect1CenterY - rect2CenterY, 2));
    }

    private Rect faceArea(Mat image, Rect eye1, Rect eye2) {
        Rect left;
        if (eye1.x() < eye2.x()) {
            left = eye1;
        } else {
            left = eye2;
        }

        final int maxWidth = Math.max(eye1.width(), eye2.width());
        final int maxHeight = Math.max(eye1.height(), eye2.height());

        final int faceX = Math.max(0, left.x() - maxWidth);
        final int faceY = Math.max(0, left.y() - maxHeight);

        final int faceWidth = Math.max(0, Math.min(4 * maxWidth, image.cols() - faceX));
        final int faceHeight = Math.max(0, Math.min(5 * maxHeight, image.rows() - faceY));

        return new Rect(faceX, faceY, faceWidth, faceHeight);
    }

    private double faceAngle(Rect eye1, Rect eye2) {
        Rect left, right;
        if (eye1.x() < eye2.x()) {
            left = eye1;
            right = eye2;
        } else {
            left = eye2;
            right = eye1;
        }

        final int rightCenterX = right.x() + right.width() / 2;
        final int rightCenterY = right.y() + right.height() / 2;

        final int leftCenterX = left.x() + left.width() / 2;
        final int leftCenterY = left.y() + left.height() / 2;

        return Math.atan((double) (rightCenterY - leftCenterY) / (double) (rightCenterX - leftCenterX));
    }

    private Rect recalc(Rect face, Rect faceArea, double angle) {
        final int x = face.x();
        final int y = face.y();

        final int faceWidth = faceArea.width();
        final int faceHeight = faceArea.height();

        final double W = faceWidth * cos(angle) + Math.abs(faceHeight * sin(angle));

        double f;

        int newX = 0, newY = 0;

        if (angle <= 0) {
            angle = Math.toRadians(90) + angle;

            final double l = Math.sqrt((Math.pow(faceWidth * cos(angle) - y, 2) + (W - x) * (W - x)));
            final double b = Math.abs(Math.atan((faceWidth * cos(angle) - y) / (W - x)));

            if (y <= faceWidth * cos(angle)) {
                f = Math.toRadians(90) - angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * cos(f));
                newY = (int) (faceArea.y() + l * sin(f));
            }

            if (y > faceWidth * cos(angle)) {
                f = angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * sin(f));
                newY = (int) (faceArea.y() + l * cos(f));
            }

        } else {

            final double l = Math.sqrt((Math.pow(faceHeight * cos(angle) - y, 2) + (W - x) * (W - x)));
            final double b = Math.abs(Math.atan((faceHeight * cos(angle) - y) / (W - x)));

            if (y <= faceHeight * cos(angle)) {
                f = Math.toRadians(90) - angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * sin(f));
                newY = (int) (faceArea.y() + faceHeight - l * cos(f));
            }

            if (y > faceHeight * cos(angle)) {
                f = angle - b;
                newX = (int) (faceArea.x() + faceWidth - l * cos(f));
                newY = (int) (faceArea.y() + faceHeight - l * sin(f));
            }
        }

        return new Rect((int) (newX * scale), (int) (newY * scale), (int) (face.width() * scale), (int) (face.width() * scale));
    }

    private void draw(Rect face, Mat image, double angle, Scalar color, String name) {

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

        //final Mat textImg = new Mat(image.rows(), image.cols(), image.type());
        putText(image, name, new Point(x, y), FONT_HERSHEY_COMPLEX, 1.0, color);
    }
}
