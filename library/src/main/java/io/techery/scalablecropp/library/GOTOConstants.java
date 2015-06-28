package io.techery.scalablecropp.library;

/**
 * @author GT
 */
public class GOTOConstants {

    public interface IntentExtras {
        String ACTION_CAMERA = "action-camera";
        String ACTION_GALLERY = "action-gallery";
        String IMAGE_PATH = "image-path";
    }

    public enum PicMode {
        CAMERA("Camera"), GALLERY("Gallery");

        String title;

        PicMode(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }
}
