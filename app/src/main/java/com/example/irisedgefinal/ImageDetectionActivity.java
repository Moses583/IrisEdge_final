package com.example.irisedgefinal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.irisedgefinal.ml.AutoModel1;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ImageDetectionActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    TextureView textureView;
    Handler handler;
    CameraManager cameraManager;
    CameraDevice cameraDevice;
    ImageView imageView;
    Bitmap bitmap;
    AutoModel1 model;
    Paint paint;
    List<String> labels;

    ImageProcessor imageProcessor;
    private TextToSpeech toSpeech;
    private static final int CAMERA_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_detection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getPermission();


        labels = new ArrayList<>();
        try {
            labels = FileUtil.loadLabels(this,"labels.txt");
        } catch (IOException e) {
            Toast.makeText(this, "Cannot load labels", Toast.LENGTH_SHORT).show();
        }

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
                .build();


        try {
            model = AutoModel1.newInstance(this);

        } catch (IOException e) {
            Toast.makeText(this, "model error", Toast.LENGTH_SHORT).show();
        }
        HandlerThread handlerThread = new HandlerThread("videoThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        imageView = findViewById(R.id.myImageView);
        textureView = (TextureView) findViewById(R.id.myTextureView);
        textureView.setSurfaceTextureListener(this);

        cameraManager= (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }
    private void getPermission() {
        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request permission
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            // Permission already granted, proceed with camera functionality
            // ... your camera code here ...
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with camera functionality
                // ... your camera code here ...
            } else {
                // Permission denied, inform user
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        try {
            openCamera();
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Open Camera error", Toast.LENGTH_SHORT).show();
        }
    }
    @SuppressLint("MissingPermission")
    private void openCamera() throws CameraAccessException {
        cameraManager.openCamera(cameraManager.getCameraIdList()[0], new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                Surface surface = new Surface(surfaceTexture);
                try {
                    CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.addTarget(surface);

                    cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(captureRequestBuilder.build(),null,null);
                            } catch (CameraAccessException e) {

                                throw new RuntimeException(e);

                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, handler);
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {

            }
        },handler);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        bitmap = textureView.getBitmap();

//        // Creates inputs for reference.
        TensorImage image = TensorImage.fromBitmap(bitmap);
        image = imageProcessor.process(image);
//        // Runs model inference and gets result.
        // Runs model inference and gets result.
        AutoModel1.Outputs outputs = model.process(image);

        float[] locations = outputs.getLocationsAsTensorBuffer().getFloatArray();
        float[] classes = outputs.getClassesAsTensorBuffer().getFloatArray();
        float[] scores = outputs.getScoresAsTensorBuffer().getFloatArray();
        float[] numberOfDetections = outputs.getNumberOfDetectionsAsTensorBuffer().getFloatArray();


        Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(mutable);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(2); // Adjust the stroke width as needed

        int h = mutable.getHeight();
        int w = mutable.getWidth();
        paint.setTextSize(35f);
//        paint.setStrokeWidth(h / 85f);
        int x = 0;

        int numDetections = Math.min((int) numberOfDetections[0], scores.length);
        int imageWidth = bitmap.getWidth();
        int imageHeight = bitmap.getHeight();

        for (int i = 0; i < numDetections; i++) {
            if (scores[i] > 0.5) { // Adjust the threshold as needed
                float ymin = locations[i * 4] * imageHeight;
                float xmin = locations[i * 4 + 1] * imageWidth;
                float ymax = locations[i * 4 + 2] * imageHeight;
                float xmax = locations[i * 4 + 3] * imageWidth;

                canvas.drawRect(xmin, ymin, xmax, ymax, paint);
                int classIndex = (int) classes[i];
                String className = (classIndex >= 0 && classIndex < labels.size()) ? labels.get(classIndex) : "Unknown";
                if (className.equals("person")){
                    String label = "Class: " + className + ", Score: " + scores[i];
                    setTxtSpeech("Warning! "+className+" detected ahead!");
                    vibratePhone();
                    canvas.drawText(label, xmin, ymin, paint);
                    Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
                }else if(className.equals("chair")){
                    String label = "Class: " + className + ", Score: " + scores[i];
                    setTxtSpeech("Warning! "+className+" detected ahead!");
                    vibratePhone();
                    canvas.drawText(label, xmin, ymin, paint);
                    Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
                } else if (className.equals("laptop")) {
                    String label = "Class: " + className + ", Score: " + scores[i];
                    setTxtSpeech("Warning! "+className+" detected ahead!");
                    vibratePhone();
                    canvas.drawText(label, xmin, ymin, paint);
                    Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
                }
                // Optionally, overlay text with class label and confidence score

            }
        }
        imageView.setImageBitmap(mutable);


    }
    public void vibratePhone(){
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For newer versions use VibrationEffect
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(500);
        }
    }
    public void setTxtSpeech(String text){
        toSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    toSpeech.setLanguage(Locale.UK);
                }

                toSpeech.speak(text,TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }
    private void handleInferenceResults(List<Category> output) {
        for(int index = 0; index < output.size();index++){
            Toast.makeText(this, output.get(index).toString(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.close();
    }
}