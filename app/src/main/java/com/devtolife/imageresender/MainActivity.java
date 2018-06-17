package com.devtolife.imageresender;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_SENDER = 14222; // 14222 it is the fictional number.

    private static final String TAG = "log";
    private Button btnStart;
    private Context context;
    private String imgName;
    private String photoURL = "http://www.freepngimg.com/thumb/color_effects/1-2-color-effects-free-download-png-thumb.png"; // just for example.

    Uri photoUriForGalleryIndexation;
    Uri photoUri;

    File storageDir;
    private File imgFile = null;

    public File getImgFile() {
        return imgFile;
    }

    public void setImgFile(File imgFile) {
        this.imgFile = imgFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        btnStart = findViewById(R.id.button);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getRemoteUrlString();

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                    createEmptyFile();
                    shareImage();

                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_SENDER);
                    }
                }
            }
        });
    }

    private void getRemoteUrlString() {

        String nameWithExtension = Uri.parse(photoURL).getLastPathSegment();

        int pos = nameWithExtension.lastIndexOf(".");
        if (pos > 0)
            imgName = nameWithExtension.substring(0, pos);
        else
            imgName = nameWithExtension;
    }

    private void createEmptyFile() {

        storageDir = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "MyImages");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
            storageDir = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "MyImages");
        }

        setImgFile(new File(storageDir + "/" + imgName + ".jpg"));

        createPhotoUriForIndexation(getImgFile());
        createPhotoUriForSharing(getImgFile());
    }

    private void createPhotoUriForIndexation(File fileIndex) {

        String contentPathString = "imgFile:" + fileIndex.getAbsolutePath();
        photoUriForGalleryIndexation = Uri.parse(contentPathString);

    }

    private void createPhotoUriForSharing(File fileShare) {

        if (Build.VERSION.SDK_INT <= 19) {
            photoUri = Uri.fromFile(fileShare);
        } else {
            photoUri = FileProvider.getUriForFile(getApplicationContext(),
                    BuildConfig.APPLICATION_ID + ".provider", fileShare);
        }
    }

    private void shareImage() {

        Observable<File> observable = Observable
                .create(new ObservableOnSubscribe<File>() {
                            @Override

                            public void subscribe(final ObservableEmitter<File> emitt) {

                                Picasso.with(context).load(photoURL)
                                        .into(new Target() {

                                            @Override
                                            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {

                                                try {

                                                    FileOutputStream ostream = new FileOutputStream(getImgFile());
                                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);

                                                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream)) {
                                                        galleryAddPic();
                                                        emitt.onComplete();
                                                    } else {
                                                        emitt.onNext(getImgFile());
                                                    }
                                                    ostream.close();

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            @Override
                                            public void onBitmapFailed(Drawable errorDrawable) {
                                            }

                                            @Override
                                            public void onPrepareLoad(Drawable placeHolderDrawable) {
                                            }
                                        });
                            }
                        }
                );


        Observer<File> observer = new Observer<File>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.e(TAG, "onSubscribe: ");
            }

            @Override
            public void onNext(File value) {
                Log.e(TAG, "onNext: ");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ");
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete: All Done!");
                sendImg();
            }
        };

        observable.subscribe(observer);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(photoUriForGalleryIndexation);
        this.sendBroadcast(mediaScanIntent);

    }

    private void sendImg() {

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
        sendIntent.setType("image/jpg");
        startActivity(sendIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_SENDER && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                createEmptyFile();
                shareImage();

            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_SENDER);
            }
        }
    }
}
