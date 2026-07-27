package com.trax.app.base

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    //==============================================================================
    // Utility / Helper Functions
    //==============================================================================

    //--------------------------------------------------
    // Applies status bar offset top padding to the target view.
    //--------------------------------------------------
    protected fun applyStatusBarPadding(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(
                v.paddingLeft,
                v.paddingTop + statusBarHeight,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
    }

    //--------------------------------------------------
    // Applies bottom navigation padding offset to the target view.
    //--------------------------------------------------
    protected fun applyBottomPadding(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                v.paddingBottom + bottom
            )
            insets
        }
    }
}