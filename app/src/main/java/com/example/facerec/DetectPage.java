package com.example.facerec;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.bytedeco.javacpp.opencv_face;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DetectPage extends Activity
        implements CvCameraViewListener {

    JavaCameraView javaCameraView;
    EditText nameText;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    ImageButton btnMode;
    private int mCameraId = 1;
    String TAG = "LOG";
    private boolean takePhoto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_page);

        //параметри камери
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        javaCameraView = (JavaCameraView)findViewById(R.id.javaCamera);

        javaCameraView.setCameraIndex(1);// 1 - фронтальна 0 - передня


        btnMode = (ImageButton)findViewById(R.id.btnMode) ;
        nameText = (EditText)findViewById(R.id.nameTxt);


        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
        else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        javaCameraView.setCvCameraViewListener(this);


        findViewById(R.id.btnCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto = true;
            }
        });

        findViewById(R.id.btnRecognise).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Methods.isTrained()) {
                    Intent faceRecognizerActivityIntent = new Intent(DetectPage.this, RecognitionPage.class);
                    startActivity(faceRecognizerActivityIntent);
                }else {
                    Toast.makeText(DetectPage.this, "You need to train first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Methods.reset();
                    Toast.makeText(DetectPage.this, "Data cleared", Toast.LENGTH_SHORT).show();
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
            }
        });

        findViewById(R.id.btnTrain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                train();
            }
        });
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        // Обличчя становитиме 20% висоти екрана
        absoluteFaceSize = (int) (height * 0.2);
        Log.d("LOGI", "Siza: " + absoluteFaceSize);
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        // Створення зображення в градаціях сірого
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        MatOfRect faces = new MatOfRect();

        // Використання класифікатор для виявлення облич
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 4, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // Якщо знайдені обличчя, ставим навколо нього прямокутник
        Rect[] facesArray = faces.toArray();
        for (Rect face : facesArray) {
            Imgproc.rectangle(aInputFrame, face.tl(), face.br(), new Scalar(0, 255, 0, 255), 5);}

        //Якщо виявлено одне обличчя і натиснута кнопка btnCapture, робимо фото
        if (facesArray.length == 1) {
            if (takePhoto) {
                capturePhoto(aInputFrame);
                alertRemainingPhotos();
            }
        }

        return aInputFrame;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies() {

        try {
            // Копіювання кастаду у тимчасовий файл, щоб OpenCV міг його завантажити
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Завантаження класифікатор каскаду
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // go
        javaCameraView.enableView();
    }

    public void swapCamera(View v) {
        if(mCameraId ==1 ) mCameraId = 0; else mCameraId = 1; //bitwise not operation to flip 1 to 0 and vice versa
        javaCameraView.disableView();
        javaCameraView.setCameraIndex(mCameraId);
        javaCameraView.enableView();
    }

    private void capturePhoto(Mat rgbaMat) {
        try {
            String text = nameText.getText().toString();

            Methods.takePhoto(Methods.numPhotos() + 1, rgbaMat.clone(), cascadeClassifier, absoluteFaceSize, text);
        }catch (Exception e) {
            e.printStackTrace();
        }
        takePhoto = false;
    }

    private void alertRemainingPhotos() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int remainingPhotos =/* Methods.PHOTOS_TRAIN_QTY - */Methods.numPhotos();
                if (remainingPhotos > 0) {
                    Toast.makeText(getBaseContext(),  remainingPhotos + " photo(s) in DB", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(DetectPage.this, "You took max number of photos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void train() {
        int remainingPhotos = /*Methods.PHOTOS_TRAIN_QTY -*/ Methods.numPhotos();
        if (remainingPhotos < 2) {
            Toast.makeText(this, "You need at least two persons", Toast.LENGTH_SHORT).show();
            return;
        }else if (Methods.isTrained()) {
            Toast.makeText(this, "Already trained", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Training started", Toast.LENGTH_SHORT).show();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    if (!Methods.isTrained()) {
                        Methods.train();
                    }
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    if (Methods.isTrained()) {
                        Toast.makeText(DetectPage.this, "Training successful", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(DetectPage.this, "Training unsuccessful", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
            }
        }.execute();
    }

}
