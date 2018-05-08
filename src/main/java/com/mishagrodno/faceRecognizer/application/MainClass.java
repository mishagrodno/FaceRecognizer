package com.mishagrodno.faceRecognizer.application;

import com.mishagrodno.faceRecognizer.FaceRecognizerApplication;
import com.mishagrodno.faceRecognizer.db.entity.HumanEntity;
import com.mishagrodno.faceRecognizer.db.service.FaceService;
import com.mishagrodno.faceRecognizer.db.service.HumanService;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;
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

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

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
    private boolean isInitialization;

    private String classifierName = "haarcascade_frontalface_alt_tree.xml";

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
            final FrameGrabber grabber = new VideoInputFrameGrabber(0);
            grabber.start();
            Frame grabbedImage = grabber.grab();

            final MainForm mainForm = new MainForm();
            mainForm.create(grabbedImage.imageHeight, grabbedImage.imageWidth);
            mainForm.getReloadButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    isInitialization = true;
                    recognizer.init();
                    isInitialization = false;
                }
            });
            grabber.setFrameRate(grabber.getFrameRate());

            final CvMemStorage storage = CvMemStorage.create();

            final URL classifierURL = getClass().getClassLoader().getResource(classifierName);

            final String classifierLocation = Loader
                    .extractResource(classifierURL, null, "classifier", ".xml")
                    .getAbsolutePath();

            final CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(classifierLocation));

            recognizer.init();

            while (mainForm.getMainFrame().isVisible() && (grabbedImage = grabber.grab()) != null) {
                final IplImage convert = Utils.convertToIplImage(grabbedImage);

                final IplImage grayImage = IplImage.create(convert.width(), convert.height(), IPL_DEPTH_8U, 1);
                cvCvtColor(convert, grayImage, CV_BGR2GRAY);

                final CvSeq faces = cvHaarDetectObjects(grayImage, cascade, storage, 1.1, 3,
                        CV_HAAR_DO_CANNY_PRUNING);

                for (int i = 0; i < faces.total(); i++) {
                    final CvRect rectangle = new CvRect(cvGetSeqElem(faces, i));

                    final IplImage temp = cvCreateImage(cvGetSize(convert), convert.depth(), convert.nChannels());
                    cvCopy(convert, temp);
                    cvSetImageROI(temp, new CvRect(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()));

                    final IplImage cropped = cvCreateImage(cvGetSize(temp), temp.depth(), temp.nChannels());
                    cvCopy(temp, cropped);

                    if (mainForm.isToSave()) {
                        final BufferedImage bi = Utils.convertToBufferedImage(cropped);
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bi, "jpg", baos);

                        final Mat mat = new Mat(cropped);
                        final String name = mainForm.getTextName().getText();
                        final HumanEntity human = humanService.getOrCreate(name);

                        faceService.create(name, mat.type(), cropped.height(), cropped.width(), baos.toByteArray(), human);
                        mainForm.setToSave(false);
                    }

                    cvRectangle(convert, cvPoint(rectangle.x(), rectangle.y()), cvPoint(rectangle.x() + rectangle.width(),
                            rectangle.y() + rectangle.height()), CvScalar.BLUE, 3, CV_AA, 0);

                    if (isInitialization) continue;
                    final int recognizedFace = recognizer.recognize(cropped);

                    final HumanEntity human = humanService.get((long) recognizedFace);
                    final String text = human == null ? "Unknown" : human.getName();
                    cvPutText(convert, text, cvPoint(rectangle.x(), rectangle.y() - 10), cvFont(3, 3),
                            CvScalar.BLUE);

                    cvReleaseImage(temp);
                    cvReleaseImage(cropped);
                }
                mainForm.getImage().setIcon(new ImageIcon(Utils.convertToBufferedImage(convert)));
                mainForm.getMainFrame().pack();
            }
            grabber.stop();
            mainForm.getMainFrame().dispose();
            Utils.safeDelete(Paths.get(classifierLocation));

        } catch (final IOException e) {
            Logger.getLogger(FaceRecognizerApplication.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
