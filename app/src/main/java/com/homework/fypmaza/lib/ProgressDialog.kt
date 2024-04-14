package com.homework.fypmaza.lib

import android.app.ProgressDialog
import android.content.Context
import com.homework.fypmaza.R

class ProgressDialog(context: Context, message: String): ProgressDialog(context) {

    init {
        setIcon(R.drawable.ic_android_black_24dp)
        setTitle(context.getString(R.string.app_name))
        setMessage(message)
        setCancelable(false)
    }
}