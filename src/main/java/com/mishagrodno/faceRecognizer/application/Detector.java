package com.mishagrodno.faceRecognizer.application;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class Detector {

    private static final Logger LOGGER = LoggerFactory.getLogger(Detector.class);

    private CascadeClassifier eyeClassifier;

    private CascadeClassifier faceClassifier;

    @Value("${classifier.eye.name}")
    private String eyeClassifierName;

    @Value("${classifier.face.name}")
    private String faceClassifierName;

    public Detector() {

    }

    @PostConstruct
    void init() {
        eyeClassifier = createClassifier(eyeClassifierName, "eye-cascade");
        faceClassifier = createClassifier(faceClassifierName, "face-cascade");
    }

    public List<Rect> detectFaces(Mat image) {
        return detect(image, faceClassifier);
    }

    public List<Rect> detectEyes(Mat image) {
        return detect(image, eyeClassifier);
    }

    private List<Rect> detect(Mat image, CascadeClassifier eyeClassifier) {
        final RectVector eyesVector = new RectVector();

        eyeClassifier.detectMultiScale(image, eyesVector, 1.1, 5, 1, new Size(),
                new Size(image.size().width(), image.size().height()));

        final List<Rect> filteredEyes = new ArrayList<>();

        for (final Rect rect : eyesVector.get()) {
            if (filteredEyes.stream().noneMatch(eye -> Utils.contains(eye, rect))) {
                filteredEyes.add(rect);
            }
        }

        return filteredEyes;
    }

    private CascadeClassifier createClassifier(String fileName, String prefix) {
        final URL classifierURL = getClass().getClassLoader().getResource(fileName);

        try {
            final String classifierLocation = Loader
                    .extractResource(classifierURL, null, prefix, ".xml")
                    .getAbsolutePath();

            return new CascadeClassifier(classifierLocation);
        } catch (final IOException e) {
            LOGGER.error("Can't create classifier: {}", fileName);
            return null;
        }
    }


}
