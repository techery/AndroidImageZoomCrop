package io.techery.scalablecropp;

import android.app.Activity;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
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
    public static final String TEMP_PHOTO_FILE_PREFIX_FOR_EXIST_IMAGE = "copy_of_";
    public static final String TEMP_PHOTO_FILE_NAME = "temp_photo.jpg";

    public static final Uri CONTENT_URI = Uri.parse("content://com.myntra.profilepic.crop/");

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {

        if (Crop.onActivityResult(requestCode, resultCode, result, this)) {

        } else if (requestCode == REQUEST_CODE_TAKE_PICTURE || requestCode == REQUEST_CODE_PICK_GALLERY) {
            if (resultCode == RESULT_OK) {
                if (requestCode == REQUEST_CODE_PICK_GALLERY) {
                    try {
                        Uri data = result.getData();
                        File sourceFile = new File(getRealPathFromURI(data));
                        createTempFileCopy(sourceFile);
                        InputStream inputStream = getContentResolver().openInputStream(data); // Got the bitmap .. Copy it to the temp file for cropping
                        Utils.copy(inputStream, mFileTemp);
                    } catch (Exception e) {
                        onError("Error while opening the image file. Please try again.");
                    }
                } else { //requestCode == REQUEST_CODE_TAKE_PICTURE
                    mFileTemp = new File(getTempFilePathForCamera());
                }
                Crop.prepare(mFileTemp.getPath()).ratio(3, 1).startFrom(this);
            } else if (resultCode == RESULT_CANCELED) {
                onCancel();
            } else {
                onError("Error while opening the image file. Please try again.");
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(columnIndex);
        cursor.close();
        return result;
    }

    private void createTempFileCopy(File sourceFile) {
        String tempFileName = TEMP_PHOTO_FILE_PREFIX_FOR_EXIST_IMAGE + sourceFile.getName();
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mFileTemp = new File(Environment.getExternalStorageDirectory(), tempFileName);
        } else {
            mFileTemp = new File(getFilesDir(), tempFileName);
        }
    }

    private String getTempFilePathForCamera() {
        String state = Environment.getExternalStorageState();
        String tempFilePath;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            tempFilePath = Environment.getExternalStorageDirectory() + "/" + TEMP_PHOTO_FILE_NAME;
        } else {
            tempFilePath = Environment.getExternalStorageDirectory() + "/" + TEMP_PHOTO_FILE_NAME;
        }
        return tempFilePath;
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
                imageDeliveryClub.takePic(this, new File(getTempFilePathForCamera()));
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
        if (mFileTemp != null){
            mFileTemp.delete();
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
