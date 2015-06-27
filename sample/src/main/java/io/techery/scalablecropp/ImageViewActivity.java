package io.techery.scalablecropp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.techery.scalablecropp.io.R;
import io.techery.scalablecropp.library.GOTOConstants;
import io.techery.scalablecropp.library.ImageCropActivity;
import io.techery.scalablecropp.library.InternalStorageContentProvider;
import io.techery.scalablecropp.library.Utils;


/**
 * @author GT
 */
public class ImageViewActivity extends Activity implements PicModeSelectDialogFragment.IPicModeSelectListener {

    public static final String TAG = "ImageViewActivity";
    public static final int REQUEST_CODE_UPDATE_PIC = 0x1;
    public static final int REQUEST_CODE_PICK_GALLERY = 0x2;
    public static final int REQUEST_CODE_TAKE_PICTURE = 0x3;
    public static final String TEMP_PHOTO_FILE_NAME = "temp_photo.jpg";

    private String imgUri;

    private Button mBtnUpdatePic;
    private ImageView mImageView;
    private CardView mCardView;
    private File mFileTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        mBtnUpdatePic = (Button) findViewById(R.id.btnUpdatePic);
        mImageView = (ImageView) findViewById(R.id.iv_user_pic);
        mCardView = (CardView) findViewById(R.id.cv_image_container);
        initCardView(); //Resize card view according to activity dimension
        mBtnUpdatePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddProfilePicDialog();
            }
        });
        createTempFile();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_CODE_UPDATE_PIC) {
            if (resultCode == RESULT_OK) {
                String imagePath = result.getStringExtra(GOTOConstants.IntentExtras.IMAGE_PATH);
                showCroppedImage(imagePath);
            } else if (resultCode == RESULT_CANCELED) {
                //TODO : Handle case
            } else {
                String errorMsg = result.getStringExtra(ImageCropActivity.ERROR_MSG);
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
            if (resultCode == RESULT_OK) {
                createTempFile();

                Intent intent = new Intent(this, ImageCropActivity.class);
                intent.putExtra(ImageCropActivity.INPUT_FILE_PATH, mFileTemp.getPath());
                startActivityForResult(intent, REQUEST_CODE_UPDATE_PIC);

            } else if (resultCode == RESULT_CANCELED) {
                onCancel();
                return;
            } else {
                onError("Error while opening the image file. Please try again.");
                return;
            }

        } else if (requestCode == REQUEST_CODE_PICK_GALLERY) {
            if (resultCode == RESULT_CANCELED) {
                onCancel();
                return;
            } else if (resultCode == RESULT_OK) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(result.getData()); // Got the bitmap .. Copy it to the temp file for cropping
                    FileOutputStream fileOutputStream = new FileOutputStream(mFileTemp);
                    Utils.copyStream(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    inputStream.close();

                    Intent intent = new Intent(this, ImageCropActivity.class);
                    intent.putExtra(ImageCropActivity.INPUT_FILE_PATH, mFileTemp.getPath());
                    startActivityForResult(intent, REQUEST_CODE_UPDATE_PIC);

                } catch (Exception e) {
                    onError("Error while opening the image file. Please try again.");
                    Log.e(TAG, "", e);
                }
            } else {
                onError("Error while opening the image file. Please try again.");
            }

        }
    }


    private void takePic() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri mImageCaptureUri = null;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mImageCaptureUri = Uri.fromFile(mFileTemp);
            } else {
                /*
                 * The solution is taken from here: http://stackoverflow.com/questions/10042695/how-to-get-camera-result-as-a-uri-in-data-folder
	        	 */
                mImageCaptureUri = InternalStorageContentProvider.CONTENT_URI;
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
            takePictureIntent.putExtra("return-data", true);
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Can't take picture", e);
            Toast.makeText(this, "Can't take picture", Toast.LENGTH_LONG).show();
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_GALLERY);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No image source available", Toast.LENGTH_SHORT).show();
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


    private void showCroppedImage(String mImagePath) {
        if (mImagePath != null) {
            Bitmap myBitmap = BitmapFactory.decodeFile(mImagePath);
            mImageView.setImageBitmap(myBitmap);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //--------Private methods --------

    private void initCardView() {
        mCardView.setPreventCornerOverlap(false);
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        //We are implementing this only for portrait mode so width will be always less
        int w = displayMetrics.widthPixels;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mCardView.getLayoutParams();
        int leftMargin = lp.leftMargin;
        int topMargin = lp.topMargin;
        int rightMargin = lp.rightMargin;
        int paddingLeft = mCardView.getPaddingLeft();
        int paddingRight = mCardView.getPaddingLeft();
        int ch = w - leftMargin - rightMargin + paddingLeft + paddingRight;
        mCardView.getLayoutParams().height = ch;
    }


    private void showAddProfilePicDialog() {
        PicModeSelectDialogFragment dialogFragment = new PicModeSelectDialogFragment();
        dialogFragment.setPicModeSelectListener(this);
        dialogFragment.show(getFragmentManager(), "picModeSelector");
    }


    @Override
    public void onPicModeSelected(GOTOConstants.PicMode mode) {
        switch (mode) {
            case CAMERA:
                getIntent().removeExtra("ACTION");
                takePic();
                break;
            case GALLERY:
                getIntent().removeExtra("ACTION");
                pickImage();
                break;
        }
    }
}
