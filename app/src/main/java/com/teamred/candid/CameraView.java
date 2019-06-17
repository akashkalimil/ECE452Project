package com.teamred.candid;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.io.IOException;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    Camera camera;
    SurfaceHolder holder;
    private CameraListener listener;
    public byte camera_buff[];


    public CameraView(Context context, Camera camera) {
        super(context);
        this.camera = camera;
        holder = getHolder();
        holder.addCallback(this);

    }

    interface CameraListener {
        void onFrameReceived(byte[] imageBytes);
    }

    void setListener(CameraListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Log.d("tag", "onpreviewcalled");
        listener.onFrameReceived(bytes);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Camera.Parameters params = camera.getParameters();

        //change the orientation of the camera

        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            params.set("orientation", "portrait");
            camera.setDisplayOrientation(90);
            params.setRotation(90);
        } else {
            params.set("orientation", "landscape");
            camera.setDisplayOrientation(0);
            params.setRotation(0);
        }

        camera.setParameters(params);
        try {
            //initialize buffer
            int buff_size = (camera.getParameters().getPreviewSize().height) *
                    (camera.getParameters().getPreviewSize().width)*
                    ImageFormat.getBitsPerPixel(ImageFormat.NV21)/8;

            camera_buff = new byte[buff_size];





            camera.setPreviewDisplay(holder);
           // camera.setPreviewCallback(this);

            camera.setPreviewCallbackWithBuffer(this);
            camera.addCallbackBuffer(camera_buff);

            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public void resetCamerabuff() {
        camera.addCallbackBuffer(camera_buff);
    }
}
