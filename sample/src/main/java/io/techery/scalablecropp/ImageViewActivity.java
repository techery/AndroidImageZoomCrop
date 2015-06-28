package io.techery.scalablecropp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;

import io.techery.scalablecropp.io.R;
import io.techery.scalablecropp.library.Crop;
import io.techery.scalablecropp.library.GOTOConstants;
import io.techery.scalablecropp.library.Utils;

import static io.techery.scalablecropp.ImageDeliveryClub.REQUEST_CODE_PICK_GALLERY;
import static io.techery.scalablecropp.ImageDeliveryClub.REQUEST_CODE_TAKE_PICTURE;


public class ImageViewActivity extends Activity implements PicModeSelectDialogFragment.IPicModeSelectListener, Crop.ImageCropListener {

    public static final String TAG = "ImageViewActivity";
    public static final String TEMP_PHOTO_FILE_NAME = "temp_photo.jpg";

    private ImageView mImageView;
    private File mFileTemp;
    private ImageDeliveryClub imageDeliveryClub = new ImageDeliveryClub();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        mImageView = (ImageView) findViewById(R.id.iv_user_pic);
        findViewById(R.id.btnUpdatePic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPicDialog();
            }
        });
        createTempFile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {

        if (Crop.onActivityResult(requestCode, resultCode, result, this)) {

        } else if (requestCode == REQUEST_CODE_TAKE_PICTURE || requestCode == REQUEST_CODE_PICK_GALLERY) {
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_CODE_PICK_GALLERY) {
                    try {
                        Uri data = result.getData();
                        InputStream inputStream = getContentResolver().openInputStream(data); // Got the bitmap .. Copy it to the temp file for cropping
                        Utils.copy(inputStream, mFileTemp);
                    } catch (Exception e) {
                        onError("Error while opening the image file. Please try again.");
                    }
                }
                Crop.prepare(mFileTemp.getPath()).startFrom(this);
            } else if (resultCode == RESULT_CANCELED) {
                onCancel();
            } else {
                onError("Error while opening the image file. Please try again.");
            }
        }
    }

    private void createTempFile() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mFileTemp = new File(Environment.getExternalStorageDirectory(), TEMP_PHOTO_FILE_NAME);
        } else {
            mFileTemp = new File(getFilesDir(), TEMP_PHOTO_FILE_NAME);
        }
    }

    public void onCancel() {
        Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
    }

    public void onError(String msg) {
        Toast.makeText(this, "Error:" + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPicModeSelected(GOTOConstants.PicMode mode) {
        switch (mode) {
            case CAMERA:
                imageDeliveryClub.takePic(this, mFileTemp);
                break;
            case GALLERY:
                imageDeliveryClub.pickImage(this);
                break;
        }
    }

    private void showCroppedImage(String mImagePath) {
        if (mImagePath != null) {
            Bitmap myBitmap = BitmapFactory.decodeFile(mImagePath);
            mImageView.setImageBitmap(myBitmap);
        }
    }

    private void showAddPicDialog() {
        PicModeSelectDialogFragment dialogFragment = new PicModeSelectDialogFragment();
        dialogFragment.setPicModeSelectListener(this);
        dialogFragment.show(getFragmentManager(), "picModeSelector");
    }

    @Override
    public void onImageCropped(String filePath, String msg) {
        if (filePath != null) showCroppedImage(filePath);
        if (msg != null) onError(msg);
    }
}
