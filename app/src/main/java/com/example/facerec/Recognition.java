package com.example.facerec;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_face;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


import static com.example.facerec.Methods.reset;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class Recognition extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private static final String TAG = "LOGS";
    Context context = this;
    DataBase dbHelper;
    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier mFaceDetector;
    private File mCascadeFile;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    private opencv_face.FaceRecognizer mLBPHFaceRecognizer = opencv_face.LBPHFaceRecognizer.create();
    private int mCameraId = 1;
    String result = "Unknown";
    String name = "Unknown";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                //Завантаження класифікатору
                                InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                                File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                                mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
                                FileOutputStream os = new FileOutputStream(mCascadeFile);

                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }
                                is.close();
                                os.close();

                                mFaceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                                if (mFaceDetector.empty()) {
                                    Log.e(TAG, "Failed to load cascade");
                                    mFaceDetector = null;
                                }else {
                                    Log.i(TAG, "Loaded cascade from " + mCascadeFile.getAbsolutePath());
                                }
                                cascadeDir.delete();

                            }catch (IOException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Failed to load cascade. Exception: " + e);
                            }
                            return null;
                        }
                    }.execute();

                    mOpenCvCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition_page);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mOpenCvCameraView = findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.setCvCameraViewListener(this);

        dbHelper = new DataBase(this);

        ImageButton mFlipCamera = findViewById(R.id.btnMode);
        mFlipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraId ==1 ) mCameraId = 0; else mCameraId = 1;
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(mCameraId);
                mOpenCvCameraView.enableView();
            }
        });

        ImageButton RecognizeBtn = findViewById(R.id.btnRecognize);
        RecognizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(0);
            }
        });

        //Загрузка створеного класифікатору з FacePics
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), Methods.FACE_PICS);
                    File f = new File(folder, Methods.CLASSIFIER);
                    Log.i(TAG, "Classifier = " + f);
                    mLBPHFaceRecognizer.read(f.getAbsolutePath());
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
                return null;
            }
        }.execute();

    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        // Обличчя становитиме 20% висоти екрана
        absoluteFaceSize = (int) (height * 0.2);

    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {

        //Підбір розміру

        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        MatOfRect faces = new MatOfRect();

        //Викорсання створеного каскаду
        if (mFaceDetector != null) {
            mFaceDetector.detectMultiScale(grayscaleImage, faces, 1.1, 4, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }else {
            Log.e(TAG, "Detection is not selected!");
        }

        //Малює квадрат
        Rect[] facesArray = faces.toArray();
        for (Rect face : facesArray) {
            Imgproc.rectangle(aInputFrame, face.tl(), face.br(), new Scalar(0, 255, 0, 255), 5);}

        //Якщо 1 лице
        if (facesArray.length == 1) {
            try {
                //Конверція OpenCV Mat в JavaCV Mat
                opencv_core.Mat javaCvMat = new opencv_core.Mat((Pointer) null) {{address = grayscaleImage.getNativeObjAddr();}};
                //Ресайз
                resize(javaCvMat, javaCvMat, new opencv_core.Size(Methods.IMG_WIDTH, Methods.IMG_HEIGHT));
                equalizeHist(javaCvMat, javaCvMat);

                IntPointer label = new IntPointer(1);
                DoublePointer confidence = new DoublePointer(1);
                mLBPHFaceRecognizer.predict(javaCvMat, label, confidence);
                int predictedLabel = label.get(0);
                double acceptanceLevel = confidence.get(0);

                Log.d(TAG, "Prediction completed, predictedLabel: " + predictedLabel + ", acceptanceLevel: " + acceptanceLevel);
                if (predictedLabel == -1 || acceptanceLevel >= 88.0D) {
                    name = "Unknown";
                    Log.d(TAG, "Closest picture: № " + predictedLabel + " Name: " + name);
                } else {
                    name =  Methods.getPhotoName(predictedLabel);
                    Log.d(TAG, "Closest picture: № " + predictedLabel + " Name: " + name);
                }

                //Отображение текста
                for (Rect face : facesArray) {
                    int posX = (int) Math.max(face.tl().x, 0);
                    int posY = (int) Math.max(face.tl().y, 0);
                    Imgproc.putText(aInputFrame, name + " Acceptence " + String.format("%.2f", acceptanceLevel), new Point(posX, posY),
                            Core.FONT_HERSHEY_COMPLEX, 1.5, new Scalar(0, 255, 0, 255),5);
                }

                result = "Person name: " + name;
            }catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage(), e);
            }
        }
        return aInputFrame;
    }

    //Діалог підтвердження персони
    protected Dialog onCreateDialog(int id) {
        String person = name;
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Confirmation");
        adb.setMessage("You confirm that you are " + person + "?");
        adb.setNegativeButton("NO", myClickListener);
        adb.setPositiveButton("YES", myClickListener);
        return adb.create();

    }
    DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    dbHelper.InsertToDB(name, context);
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    break;
            }
        }
    };
}
