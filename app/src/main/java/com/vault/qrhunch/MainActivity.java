package com.vault.qrhunch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView previewView;
    ImageCapture imageCapture;
    MaterialButton scanButton;
    ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkCameraPermission();
        scanButton = findViewById(R.id.materialButton);
        previewView = findViewById(R.id.preview);
        showCamera();
        scanButton.setOnClickListener(v -> scanBarCode());
    }


    //this is function is used for decoding barcodes
    private void scanBarCode() {
        Log.i("scanbarCode","executed");
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getApplicationContext()), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                Log.i("analyze method","executed");
                Image image1 = image.getImage();
                assert  image1!=null;
                InputImage inputImage = InputImage.fromMediaImage(image1,image.getImageInfo().getRotationDegrees());
                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();
                BarcodeScanner scanner = BarcodeScanning.getClient(options);
                Task<List<Barcode>> result = scanner.process(inputImage)
                        .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                            @Override
                            public void onSuccess(List<Barcode> barcodes) {
                                imageAnalysis.clearAnalyzer();
                                Log.i("onSuccess","executed");
                                for (Barcode barcode: barcodes) {
                                    int valueType = barcode.getValueType();
                                    Log.i("valuetype",Integer.toString(valueType));
                                    switch (valueType) {
                                        case Barcode.TYPE_WIFI:
                                            String ssid = barcode.getWifi().getSsid();
                                            Log.i("ssid",ssid);
                                            String password = barcode.getWifi().getPassword();
                                            Log.i("password",password);
                                            int type = barcode.getWifi().getEncryptionType();
                                            Log.i("encryption",Integer.toString(type));
                                            break;
                                        case Barcode.TYPE_URL:
                                            String title = barcode.getUrl().getTitle();
                                            String url = barcode.getUrl().getUrl();
                                            Uri link = Uri.parse(url);
                                            Log.i("url",url);
                                            startActivity(new Intent(Intent.ACTION_VIEW,link));

                                            break;
                                        case Barcode.TYPE_PHONE:
                                            String phone = barcode.getPhone().getNumber();
                                            Uri u = Uri.parse("tel:"+ phone);
                                            startActivity(new Intent(Intent.ACTION_DIAL,u));
                                            Toast.makeText(MainActivity.this, "Opened Dialer Successfully" , Toast.LENGTH_SHORT).show();
                                            break;
                                        case Barcode.TYPE_EMAIL:
                                            Log.i("type email","approved");
                                            String mailadress = barcode.getEmail().getAddress();
                                            String body = barcode.getEmail().getBody();
                                            String subject = barcode.getEmail().getBody();
                                            Uri u1 = Uri.parse("mailto:"+mailadress);
                                            Intent mailIntent = new Intent(Intent.ACTION_SENDTO,u1);
                                            mailIntent.putExtra(Intent.EXTRA_SUBJECT,subject);
                                            mailIntent.putExtra(Intent.EXTRA_TEXT,body);
                                            startActivity(mailIntent);
                                            break;

                                        default:
                                            Log.i("default case", "executed");
                                            String raw = barcode.getRawValue();
                                            Toast.makeText(MainActivity.this, raw + " :this is content of provided barcode", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        })
                        .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                            @Override
                            public void onComplete(@NonNull Task<List<Barcode>> task) {
                                image.close();
                            }
                        });
            }
        });
    }

    //this function is used to show camera preview
    protected void showCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());
        cameraProviderFuture.addListener(() -> {
            try{
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview( cameraProvider);
            }
            catch (Exception e){}
        },ContextCompat.getMainExecutor(getApplicationContext()));
    }
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();
         imageCapture =
                new ImageCapture.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();
         imageAnalysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,preview,imageCapture,imageAnalysis);
    }

    //this function is used to check Camera Permission
    private  void checkCameraPermission() {
        if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // show camera preview when permission granted
            Log.i("granted","permission already granted");
        }
        else if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},23);
            Toast.makeText(MainActivity.this, "camera permission is needed to show camera preview", Toast.LENGTH_SHORT).show();
        }
        else {
            requestPermissions(new String[]{Manifest.permission.CAMERA},23);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 23 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // show camera preview
        }
        else {
            Toast.makeText(getApplicationContext(), "cannot scan QR without camera", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
}