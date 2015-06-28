package io.techery.scalablecropp.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class Crop {

    private static final int REQUEST_CODE_UPDATE_PIC = 0x16;

    int ratioX = 3;
    int ratioY = 1;
    String filePath;

    private Crop(String filePath) {
        this.filePath = filePath;
    }

    public static Crop prepare(String filePath) {
        return new Crop(filePath);
    }

    public Crop ratio(int x, int y) {
        ratioX = x;
        ratioY = y;
        return this;
    }

    public void startFrom(Fragment fragment) {
        if (fragment.getActivity() != null)
            fragment.startActivityForResult(prepareIntent(fragment.getActivity()), REQUEST_CODE_UPDATE_PIC);
    }

    public void startFrom(android.app.Fragment fragment) {
        if (fragment.getActivity() != null)
            fragment.startActivityForResult(prepareIntent(fragment.getActivity()), REQUEST_CODE_UPDATE_PIC);
    }

    public void startFrom(Activity activity) {
        activity.startActivityForResult(prepareIntent(activity), REQUEST_CODE_UPDATE_PIC);
    }

    private Intent prepareIntent(Context context) {
        Intent intent = new Intent(context, ImageCropActivity.class);
        intent.putExtra(ImageCropActivity.INPUT_FILE_PATH, filePath);
        intent.putExtra(ImageCropActivity.INPUT_RATIO_X, ratioX);
        intent.putExtra(ImageCropActivity.INPUT_RATIO_Y, ratioY);
        return intent;
    }


    public static boolean onActivityResult(int requestCode, int resultCode, Intent result, ImageCropListener resultListener) {
        if (requestCode == REQUEST_CODE_UPDATE_PIC) {
            if (resultCode == Activity.RESULT_OK) {
                String imagePath = result.getStringExtra(GOTOConstants.IntentExtras.IMAGE_PATH);
                resultListener.onImageCropped(imagePath, null);
                return true;
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //TODO : Handle case
            } else {
                String errorMsg = result.getStringExtra(ImageCropActivity.ERROR_MSG);
                resultListener.onImageCropped(null, errorMsg);
            }
        }
        return false;
    }

    public interface ImageCropListener {
        void onImageCropped(String filePath, String error);
    }
}
