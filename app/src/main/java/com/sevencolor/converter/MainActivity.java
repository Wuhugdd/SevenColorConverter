package com.sevencolor.converter;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView imgOriginal;
    private ImageView imgResult;
    private Button btnPick;
    private Button btnConvert;
    private Button btnSave;
    private TextView tvStatus;

    private Bitmap originalBitmap;
    private Bitmap convertedBitmap;

    private final ActivityResultLauncher<String> pickImage =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                loadOriginal(uri);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgOriginal = findViewById(R.id.imgOriginal);
        imgResult = findViewById(R.id.imgResult);
        btnPick = findViewById(R.id.btnPick);
        btnConvert = findViewById(R.id.btnConvert);
        btnSave = findViewById(R.id.btnSave);
        tvStatus = findViewById(R.id.tvStatus);

        btnPick.setOnClickListener(v -> pickImage.launch("image/*"));

        btnConvert.setOnClickListener(v -> {
            if (originalBitmap == null) return;
            tvStatus.setText("转换中...");
            btnConvert.setEnabled(false);

            // 后台线程执行转换
            new Thread(() -> {
                Bitmap result = ColorConverter.convert(originalBitmap);
                runOnUiThread(() -> {
                    convertedBitmap = result;
                    imgResult.setImageBitmap(result);
                    btnSave.setEnabled(true);
                    tvStatus.setText(String.format("转换完成 %dx%d", result.getWidth(), result.getHeight()));
                    btnConvert.setEnabled(true);
                });
            }).start();
        });

        btnSave.setOnClickListener(v -> {
            if (convertedBitmap == null) return;
            saveBmp(convertedBitmap);
        });
    }

    private void loadOriginal(Uri uri) {
        try {
            originalBitmap = getBitmapFromUri(uri);
            imgOriginal.setImageBitmap(originalBitmap);
            imgResult.setImageBitmap(null);
            convertedBitmap = null;
            btnSave.setEnabled(false);
            btnConvert.setEnabled(true);
            tvStatus.setText(String.format("已加载 %dx%d，点击转换", originalBitmap.getWidth(), originalBitmap.getHeight()));
        } catch (Exception e) {
            Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return androidx.core.graphics.ImageDecoder.createBitmap(
                androidx.core.graphics.ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            return android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }
    }

    private void saveBmp(Bitmap bitmap) {
        String filename = "EPD_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".bmp";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 用 MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/bmp");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    if (os != null) os.close();
                    tvStatus.setText("已保存到 Downloads/" + filename);
                }
            } else {
                // Android 9 及以下直接写 Downloads
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(dir, filename);
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                tvStatus.setText("已保存到 " + file.getAbsolutePath());
            }
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
