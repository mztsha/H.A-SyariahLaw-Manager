package com.homework.fypmaza.Client

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.homework.fypmaza.AuthActivity
import com.homework.fypmaza.MessageActivity
import com.homework.fypmaza.R
import com.homework.fypmaza.lib.ProgressDialog
import java.util.Locale

class ClientHomeActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var textview_userdata: TextView
    private lateinit var relativelayout_logout: RelativeLayout
    private lateinit var linearlayout_appointments: LinearLayout
    private lateinit var relativelayout_refresh_appointments: RelativeLayout
    private lateinit var relativelayout_new_appointment: RelativeLayout

    private lateinit var progressDialog: ProgressDialog

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_client_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        mAuth.setLanguageCode(Locale.getDefault().displayLanguage)
        mFirestore = FirebaseFirestore.getInstance()

        progressDialog = ProgressDialog(this@ClientHomeActivity, "Loading...")

        textview_userdata = findViewById(R.id.clienthome_textview_userdata)
        relativelayout_logout = findViewById(R.id.clienthome_relativelayout_logout)
        linearlayout_appointments = findViewById(R.id.clienthome_linearlayout_appointments)
        relativelayout_refresh_appointments = findViewById(R.id.clienthome_relativelayout_refresh_appointments)
        relativelayout_new_appointment = findViewById(R.id.clienthome_relativelayout_new_appointment)

        relativelayout_logout.setOnClickListener {
            mAuth.signOut()
        }

        relativelayout_new_appointment.setOnClickListener {
            startActivity(Intent(this@ClientHomeActivity, ClientNewAppointmentActivity::class.java))
        }

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                finish()
                startActivity(Intent(this@ClientHomeActivity, AuthActivity::class.java))
            } else {
                textview_userdata.text = user.email + "\n" + user.uid
                getAppointments(user)
                relativelayout_refresh_appointments.setOnClickListener {
                    getAppointments(user)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getAppointments(user: FirebaseUser) {
        progressDialog.show()
        linearlayout_appointments.removeAllViewsInLayout()
        mFirestore.collection("users").document(user.email.toString()).collection("appointments").get()
            .addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    val documents = task.result.documents.toTypedArray()
                    var document_index = 0
                    for (document in documents) {
                        document_index++
                        val textappointment_view = createAppointmentView("Appointment ${document_index}\nCase: ${document.data!!["case"]}\nLawyer: ${document.data!!["lawyer"]}")
                        val textappointment_view_opencase = createAppointmentView("Open Case")
                        textappointment_view_opencase.setTypeface(ResourcesCompat.getFont(this@ClientHomeActivity, R.font.sfmedium), Typeface.BOLD)
                        linearlayout_appointments.addView(textappointment_view)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            textappointment_view_opencase.typeface = Typeface.create(resources.getFont(R.font.sfmedium), Typeface.BOLD)
                        linearlayout_appointments.addView(textappointment_view_opencase)
                        textappointment_view_opencase.setOnClickListener {
                            val intent = Intent(this@ClientHomeActivity, MessageActivity::class.java)
                            intent.putExtra("type", "client")
                            intent.putExtra("caseId", document.data!!["case"].toString())
                            startActivity(intent)
                        }
                    }
                } else
                    Toast.makeText(this@ClientHomeActivity, "Error ${task.exception.toString()}", Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }
    }

    private fun createAppointmentView(text: String): TextView {
        val textView = TextView(this)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.text = text
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            textView.typeface = resources.getFont(R.font.sfmedium)
        return textView
    }

    override fun onStart() {
        super.onStart()
        if (::authStateListener.isInitialized)
            mAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        if (::authStateListener.isInitialized)
            mAuth.removeAuthStateListener(authStateListener)
    }
}