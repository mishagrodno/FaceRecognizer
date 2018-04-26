package com.mishagrodno.faceRecognizer;

import com.mishagrodno.faceRecognizer.application.MainClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Spring application starts here.
 *
 * @author Gomanchuk Mikhail.
 */
@SpringBootApplication
public class FaceRecognizerApplication {

    private static MainClass mainClass;

    @Autowired
    public FaceRecognizerApplication(MainClass mainClass) {
        FaceRecognizerApplication.mainClass = mainClass;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(FaceRecognizerApplication.class)
                .headless(false)
                .run(args);

        mainClass.start();
    }

}
