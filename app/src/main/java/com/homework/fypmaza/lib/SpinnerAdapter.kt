package com.homework.fypmaza.lib

//noinspection SuspiciousImport
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SpinnerAdapter(context: Context, resource: Int, private val data: Array<String>) : ArrayAdapter<String>(context, resource, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        if (row == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            row = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        }
        val textView: TextView = row!!.findViewById(android.R.id.text1)
        textView.text = data[position]
        return row
    }
}