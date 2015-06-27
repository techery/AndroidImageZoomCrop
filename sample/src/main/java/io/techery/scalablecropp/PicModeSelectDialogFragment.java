package io.techery.scalablecropp;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import io.techery.scalablecropp.library.GOTOConstants;


/**
 * @author GT
 */
public class PicModeSelectDialogFragment extends DialogFragment {


    private IPicModeSelectListener iPicModeSelectListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final GOTOConstants.PicMode[] values = GOTOConstants.PicMode.values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].getTitle();
        }
        builder.setTitle("Select Mode")
                .setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (iPicModeSelectListener != null) {
                            GOTOConstants.PicMode mode = values[which];
                            iPicModeSelectListener.onPicModeSelected(mode);
                        }
                    }
                });
        return builder.create();
    }

    public void setPicModeSelectListener(IPicModeSelectListener iPicModeSelectListener) {
        this.iPicModeSelectListener = iPicModeSelectListener;
    }

    public interface IPicModeSelectListener {
        void onPicModeSelected(GOTOConstants.PicMode mode);
    }
}
