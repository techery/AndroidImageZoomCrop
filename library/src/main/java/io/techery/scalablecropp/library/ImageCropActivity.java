package io.techery.scalablecropp.library;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.techery.scalablecropp.library.imagecrop.cropoverlay.CropOverlayView;
import io.techery.scalablecropp.library.imagecrop.cropoverlay.edge.Edge;
import io.techery.scalablecropp.library.imagecrop.cropoverlay.utils.ImageViewUtil;
import io.techery.scalablecropp.library.imagecrop.photoview.PhotoView;
import io.techery.scalablecropp.library.imagecrop.photoview.PhotoViewAttacher;
import scalecropview.techery.io.library.R;

public class ImageCropActivity extends Activity {

    public static final String TAG = "ImageCropActivity";
    public static final String INPUT_FILE_PATH = "FILE_PATH";
    public static final String INPUT_RATIO_X = "INPUT_RATIO_X";
    public static final String INPUT_RATIO_Y = "INPUT_RATIO_Y";

    public static final String ERROR_MSG = "error_msg";

    private static final int IMAGE_MAX_SIZE = 1024;

    private final Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.JPEG;

    PhotoView mImageView;
    CropOverlayView mCropOverlayView;
    View mMoveResizeText;
    Toolbar toolbar;
    private ContentResolver mContentResolver;
    private float minScale = 1f;

    //Temp file to save cropped image
    private String mImagePath;
    private Uri mSaveUri = null;
    private Uri mImageUri = null;
    private PhotoViewAttacher mAttacher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        mContentResolver = getContentResolver();
        mImageView = (PhotoView) findViewById(R.id.iv_photo);
        mCropOverlayView = (CropOverlayView) findViewById(R.id.crop_overlay);
        mMoveResizeText = findViewById(R.id.tv_move_resize_txt);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        findViewById(R.id.btn_done).setOnClickListener(banDoneListener);
        mImageView.addListener(new PhotoViewAttacher.IGetImageBounds() {
            @Override
            public Rect getImageBounds() {
                return new Rect((int) Edge.LEFT.getCoordinate(), (int) Edge.TOP.getCoordinate(), (int) Edge.RIGHT.getCoordinate(), (int) Edge.BOTTOM.getCoordinate());
            }
        });
        String file_path = getIntent().getStringExtra(INPUT_FILE_PATH);
        int ratioX = getIntent().getIntExtra(INPUT_RATIO_X, 3);
        int ratioY = getIntent().getIntExtra(INPUT_RATIO_Y, 1);

        mCropOverlayView.setRatio(ratioX, ratioY);
        mImagePath = new File(file_path).getPath();
        mSaveUri = Utils.getImageUri(mImagePath);
        mImageUri = Utils.getImageUri(mImagePath);
        init(ratioX, ratioY);
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    private void init(int requiredRatioX, int requiredRatioY) {
        Bitmap b = getBitmap(mImageUri);
        Drawable bitmap = new BitmapDrawable(getResources(), b);
        int h = bitmap.getIntrinsicHeight();
        int w = bitmap.getIntrinsicWidth();
        final float cropWindowWidth = Edge.getWidth();
        final float cropWindowHeight = Edge.getHeight();
        minScale = (cropWindowWidth + 1f) / w;

        mImageView.setMaximumScale(minScale * 3);
        mImageView.setMediumScale(minScale * 2);
        mImageView.setMinimumScale(minScale);
        mImageView.setImageDrawable(bitmap);

        if (requiredRatioX == requiredRatioY) {
            int focalY = h + (int)(minScale * (cropWindowHeight - h * minScale));
            if (minScale > 1) {
                focalY = - focalY;
            }
            mImageView.setScale(minScale, 0, focalY, false);
        } else {
            mImageView.setScale(minScale, 0, cropWindowHeight*10, false);
        }

        //Initialize the MoveResize text
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mMoveResizeText.getLayoutParams();
        lp.setMargins(0, Math.round(Edge.BOTTOM.getCoordinate()) + 20, 0, 0);
        mMoveResizeText.setLayoutParams(lp);
    }

    private View.OnClickListener banDoneListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            saveUploadCroppedImage();
        }
    };

    private void saveUploadCroppedImage() {
        boolean saved = saveOutput();
        if (saved) {
            //USUALLY Upload image to server here
            Intent intent = new Intent();
            intent.putExtra(GOTOConstants.IntentExtras.IMAGE_PATH, mImagePath);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(this, "Unable to save Image into your device.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("restoreState", true);
    }

    private Bitmap getBitmap(Uri uri) {
        InputStream in = null;
        Bitmap returnedBitmap = null;
        try {
            in = mContentResolver.openInputStream(uri);
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = mContentResolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, o2);
            in.close();

            //First check
            ExifInterface ei = new ExifInterface(uri.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    returnedBitmap = rotateImage(bitmap, 90);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    returnedBitmap = rotateImage(bitmap, 180);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    returnedBitmap = rotateImage(bitmap, 270);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                default:
                    returnedBitmap = bitmap;
            }
            return returnedBitmap;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    private Bitmap getCurrentDisplayedImage() {
        Bitmap result = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(), Bitmap.Config.RGB_565);
        Canvas c = new Canvas(result);
        mImageView.draw(c);
        return result;
    }

    public Bitmap getCroppedImage() {

        Bitmap mCurrentDisplayedBitmap = getCurrentDisplayedImage();
        Rect displayedImageRect = ImageViewUtil.getBitmapRectCenterInside(mCurrentDisplayedBitmap, mImageView);

        // Get the scale factor between the actual Bitmap dimensions and the
        // displayed dimensions for width.
        float actualImageWidth = mCurrentDisplayedBitmap.getWidth();
        float displayedImageWidth = displayedImageRect.width();
        float scaleFactorWidth = actualImageWidth / displayedImageWidth;

        // Get the scale factor between the actual Bitmap dimensions and the
        // displayed dimensions for height.
        float actualImageHeight = mCurrentDisplayedBitmap.getHeight();
        float displayedImageHeight = displayedImageRect.height();
        float scaleFactorHeight = actualImageHeight / displayedImageHeight;

        // Get crop window position relative to the displayed image.
        float cropWindowX = Edge.LEFT.getCoordinate() - displayedImageRect.left;
        float cropWindowY = Edge.TOP.getCoordinate() - displayedImageRect.top;
        float cropWindowWidth = Edge.getWidth();
        float cropWindowHeight = Edge.getHeight();

        // Scale the crop window position to the actual size of the Bitmap.
        float actualCropX = cropWindowX * scaleFactorWidth;
        float actualCropY = cropWindowY * scaleFactorHeight;
        float actualCropWidth = cropWindowWidth * scaleFactorWidth;
        float actualCropHeight = cropWindowHeight * scaleFactorHeight;

        // Crop the subset from the original Bitmap.
        Bitmap croppedBitmap = Bitmap.createBitmap(mCurrentDisplayedBitmap, (int) actualCropX, (int) actualCropY, (int) actualCropWidth, (int) actualCropHeight);
        return croppedBitmap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
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


    private boolean saveOutput() {
        Bitmap croppedImage = getCroppedImage();
        if (mSaveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = mContentResolver.openOutputStream(mSaveUri);
                if (outputStream != null) {
                    croppedImage.compress(mOutputFormat, 90, outputStream);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            } finally {
                closeSilently(outputStream);
            }
        } else {
            Log.e(TAG, "not defined image url");
            return false;
        }
        croppedImage.recycle();
        return true;
    }


    public void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }


    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


}
