package com.trax.app.progressBar

import android.app.Activity
import android.app.Dialog
import android.view.Window
import com.trax.app.R

class ProgressView {

    //==============================================================================
    // Companion Object
    //==============================================================================

    companion object {

        //--------------------------------------------------
        // What it does: Instantiates a non-cancelable dialog containing a custom progress layout.
        // When it is called: Triggered when loading API networks or processing files.
        // Why it is required: Displays visual progress loaders to signify background processes.
        //--------------------------------------------------
        fun getLoader(context: Activity): Dialog {
            val dialog = Dialog(context, R.style.DialogFragmentTheme)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.custom_progress_dialog)
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            return dialog
        }
    }
}
