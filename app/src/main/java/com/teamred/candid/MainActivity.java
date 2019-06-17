package com.teamred.candid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraView.CameraListener, ImageReader.OnImageAvailableListener {

    private static final int IMAGE_WIDTH = 480;
    private static final int IMAGE_HEIGHT = 320;
    //    private CameraView cameraView;
    private FrameLayout frameLayout;
    //    private Camera camera;
//
    private FirebaseVisionFaceDetector detector;
//
//    public Bitmap getBitmapFromAssets(String fileName) {
//        AssetManager assetManager = this.getAssets();
//
//        InputStream istr = null;
//        try {
//            istr = assetManager.open(fileName);
//        } catch (IOException e) {
//            return null;
//        }
//
//        return BitmapFactory.decodeStream(istr);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        openCamera((CameraManager) getSystemService(Context.CAMERA_SERVICE));

//        //open the camera
//        camera = Camera.open();
//        cameraView = new CameraView(this, camera);
//        cameraView.setListener(this);
//        frameLayout.addView(cameraView);
//
//        // High-accuracy landmark detection and face classification
//

        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
//                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setMinFaceSize(0.1f) // face size to detect (relative to size of image)
                .build();

        detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
    }

    private void openCamera(CameraManager camManager) throws CameraAccessException {
        String[] camIds = camManager.getCameraIdList();
        if (camIds.length < 1) {
            Log.e("e", "Camera not available");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.YUV_420_888, 10);
        mImageReader.setOnImageAvailableListener(this, backgroundHandler);
        camManager.openCamera(camIds[0],
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        Log.i("i", "Camera opened");
                        startCamera(camera);
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.e("e", "Error [" + error + "]");
                    }
                },
                new Handler());
    }

    private ImageReader mImageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private int[] output;
    private byte[][] cachedYuvBytes;
    private Bitmap displayBitmap;

    private void startCamera(CameraDevice cameraDevice) {
        try {
            final CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(mImageReader.getSurface());
            cameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            CaptureRequest request = requestBuilder.build();
                            try {
                                session.setRepeatingRequest(request, null, backgroundHandler);
                            } catch (CameraAccessException cae) {
                                cae.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        }
                    },
                    backgroundHandler);
        } catch (CameraAccessException cae) {
            cae.printStackTrace();
//            listener.onError();
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();

        // Log.d(TAG, "Image height ["+image.getHeight()+"] - Width ["+image.getWidth()+"]");
        //imageToBitmap(image, displayBitmap);

        if (image == null)
            return;

//        ImageUtils.convertImageToBitmap(image, IMAGE_WIDTH, IMAGE_HEIGHT, output, cachedYuvBytes);

        displayBitmap.setPixels(output, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        //Log.d(TAG, "Output size ["+output.length+"]");
        //Log.d(TAG, "Display ["+displayBitmap.getWidth()+"x" + displayBitmap.getHeight()+"]");
        image.close();


        FirebaseVisionImage firebaseImage = FirebaseVisionImage.fromMediaImage(image, 0);

        //Log.d(TAG, "Firebase Image ["+firebaseImage+"]");
        Task result =
                detector
                        .detectInImage(firebaseImage)
                        .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                // Log.d(TAG, "Faces ["+faces.size()+"]");
                                for (FirebaseVisionFace face : faces) {
                                    Log.d("t", "face = " + face.getSmilingProbability());
                                }

                            }
                        });

//        uiHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (displayBitmap) {
//                    img.setImageBitmap(displayBitmap);
//                }
//            }
//        });
    }

//    public void onFrameReceived(byte[] imageBytes) {
//        FirebaseVisionImageMetadata metadata = new
//                FirebaseVisionImageMetadata.Builder()
//                .setWidth(480)
//                .setHeight(360)
//                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
//                .setRotation(0)
//                .build();
//
//        new Bitmap()
//
//       // Log.d("pp", "width " + cameraView.getWidth() + " height " + cameraView.getHeight());
//        FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(imageBytes, metadata);
//        ImageView iv = new ImageView(this);
//        frameLayout.removeView(cameraView);
//        frameLayout.addView(iv);
//        iv.setImageBitmap(image.getBitmap());
//        // Log.d("pp", "width " + image.getBitmap().getWidth() + " height " + image.getBitmap().getHeight());
//        detector.detectInImage(image)
//                .addOnSuccessListener(
//                        new OnSuccessListener<List<FirebaseVisionFace>>() {
//                            @Override
//                            public void onSuccess(List<FirebaseVisionFace> faces) {
//                                Log.d("pp", "found faces " + faces.size());
//                                for (FirebaseVisionFace face : faces) {
//                                    System.out.println(face.toString());
//
//                                }
//                                cameraView.resetCamerabuff();
//                            }
//                        })
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                // Task failed with an exception
//                                Log.d("pp", "failed " + e.getMessage());
//                                cameraView.resetCamerabuff();
//
//                            }
//                        });
//
//    }
}
