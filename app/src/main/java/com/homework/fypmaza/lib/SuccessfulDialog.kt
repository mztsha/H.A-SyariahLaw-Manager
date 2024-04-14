package com.homework.fypmaza.lib

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import com.homework.fypmaza.R

@SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
class SuccessfulDialog(context: Context, caseId: String, onDialogEventListener: onDialogEventListener): Dialog(context) {
    init {
        setCancelable(false)
        setContentView(R.layout.dialog_successful_case)

        val layoutParams: WindowManager.LayoutParams = window!!.attributes
        layoutParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT

        window!!.setAttributes(layoutParams)

        val gotohomepage: RelativeLayout = window!!.findViewById(R.id.successfuldialog_gotohomepage)
        val case: TextView = window!!.findViewById(R.id.successfuldialog_case)

        case.setText("Case : ${caseId}")
        gotohomepage.setOnClickListener { onDialogEventListener.onGoToHomepage() }
    }

    interface onDialogEventListener {
        fun onGoToHomepage()
    }
}