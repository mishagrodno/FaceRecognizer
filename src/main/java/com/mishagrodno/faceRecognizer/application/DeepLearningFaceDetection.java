package com.mishagrodno.faceRecognizer.application;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_dnn.Net;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.IOException;
import java.net.URL;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_dnn.blobFromImage;
import static org.bytedeco.javacpp.opencv_dnn.readNetFromCaffe;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_WIDTH;

public class DeepLearningFaceDetection {

    private static final String PROTO_FILE = "deploy.prototxt";
    private static final String CAFFE_MODEL_FILE = "caffe_300x300.caffemodel";
    private static final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
    private static Net net = null;

    static {
        try {
            final URL protoURL = DeepLearningFaceDetection.class.getClassLoader().getResource(PROTO_FILE);

            final String proto = Loader
                    .extractResource(protoURL, null, "deploy", ".prototxt")
                    .getAbsolutePath();

            final URL caffeURL = DeepLearningFaceDetection.class.getClassLoader().getResource(CAFFE_MODEL_FILE);

            final String caffe = Loader
                    .extractResource(caffeURL, null, "caffe", ".caffemodel")
                    .getAbsolutePath();

            net = readNetFromCaffe(proto, caffe);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void detectAndDraw(Mat image) {//detect faces and draw a blue rectangle arroung each face

        resize(image, image, new Size(300, 300));//resize the image to match the input size of the model

        //create a 4-dimensional blob from image with NCHW (Number of images in the batch -for training only-, Channel, Height, Width) dimensions order,
        //for more detailes read the official docs at https://docs.opencv.org/trunk/d6/d0f/group__dnn.html#gabd0e76da3c6ad15c08b01ef21ad55dd8
        Mat blob = blobFromImage(image, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0, 0), false, false, CV_32F);

        net.setInput(blob);//set the input to network model
        Mat output = net.forward();//feed forward the input to the netwrok to get the output matrix

        Mat ne = new Mat(new Size(output.size(3), output.size(2)), CV_32F, output.ptr(0, 0));//extract a 2d matrix for 4d output matrix with form of (number of detections x 7)

        FloatIndexer srcIndexer = ne.createIndexer(); // create indexer to access elements of the matric

        for (int i = 0; i < output.size(3); i++) {//iterate to extract elements
            float confidence = srcIndexer.get(i, 2);
            float leftX = srcIndexer.get(i, 3);
            float leftY = srcIndexer.get(i, 4);
            float rightX = srcIndexer.get(i, 5);
            float rightY = srcIndexer.get(i, 6);
            if (confidence > .8) {
                float tx = leftX * 300;//top left point's x
                float ty = leftY * 300;//top left point's y
                float bx = rightX * 300;//bottom right point's x
                float by = rightY * 300;//bottom right point's y
                rectangle(image, new Rect(new Point((int) tx, (int) ty), new Point((int) bx, (int) by)), new Scalar(255, 0, 0, 0));//print blue rectangle
            }
        }
    }

    public static void main(String[] args) {
        VideoCapture capture = new VideoCapture();
        capture.set(CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(CAP_PROP_FRAME_HEIGHT, 720);

        if (!capture.open(0)) {
            System.out.println("Can not open the cam !!!");
        }

        Mat colorimg = new Mat();

        CanvasFrame mainframe = new CanvasFrame("Face Detection", CanvasFrame.getDefaultGamma() / 2.2);
        mainframe.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        mainframe.setCanvasSize(600, 600);
        mainframe.setLocationRelativeTo(null);
        mainframe.setVisible(true);

        while (true) {
            while (capture.read(colorimg) && mainframe.isVisible()) {
                long now = System.currentTimeMillis();
                detectAndDraw(colorimg);
                mainframe.showImage(converter.convert(colorimg));
                //System.out.println(System.currentTimeMillis() - now);
            }
        }
    }
}
