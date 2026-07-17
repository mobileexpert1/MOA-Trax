package com.trax.app.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

open class BaseFragment : Fragment() {

    /**
     * Apply status bar padding to any view
     */
    protected fun applyStatusBarPadding(view: View) {

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->

            val statusBarHeight =
                insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                ).top

            v.setPadding(
                v.paddingLeft,
                v.paddingTop + statusBarHeight,
                v.paddingRight,
                v.paddingBottom
            )

            insets
        }
    }

    /**
     * Apply bottom navigation padding
     */
    protected fun applyBottomPadding(view: View) {

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->

            val bottom =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                ).bottom

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