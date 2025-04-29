package com.trax.app.progressBar

import android.app.Activity
import android.app.Dialog
import android.view.Window
import com.trax.app.R

class ProgressView {
    companion object {
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
