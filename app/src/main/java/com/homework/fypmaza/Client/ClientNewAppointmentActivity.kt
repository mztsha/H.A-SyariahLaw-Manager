package com.homework.fypmaza.Client

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.homework.fypmaza.AuthActivity
import com.homework.fypmaza.R
import com.homework.fypmaza.lib.ProgressDialog
import com.homework.fypmaza.lib.SpinnerAdapter
import com.homework.fypmaza.lib.TicketGenerator
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClientNewAppointmentActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var spinner_selectlawyer: Spinner
    private lateinit var edittext_name: EditText
    private lateinit var spinner_gender: Spinner
    private lateinit var edittext_address: EditText
    private lateinit var edittext_mobile_no: EditText
    private lateinit var calendarview_select_date: CalendarView
    private lateinit var button_select_time: Button
    private lateinit var spinner_casetype: Spinner
    private lateinit var edittext_note: EditText
    private lateinit var relativelayout_save: RelativeLayout
    private lateinit var relativelayout_cancel: RelativeLayout

    private lateinit var progressDialog: ProgressDialog

    private var lawyer: String? = null
    private var caseTicket: String = TicketGenerator.generateTicketNumber()
    private var casetype: String? = null
    private var gender: String? = null
    private var date: Date? = null
    private var time: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_client_new_appointment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        mAuth.setLanguageCode(Locale.getDefault().displayLanguage)
        mFirestore = FirebaseFirestore.getInstance()

        spinner_selectlawyer = findViewById(R.id.client_newappointment_spinner_selectlawyer)
        edittext_name = findViewById(R.id.client_newappointment_edittext_name)
        spinner_gender = findViewById(R.id.client_newappointment_spinner_gender)
        edittext_address = findViewById(R.id.client_newappointment_edittext_address)
        edittext_mobile_no = findViewById(R.id.client_newappointment_edittext_mobile_no)
        calendarview_select_date = findViewById(R.id.client_newappointment_calendarview_select_date)
        button_select_time = findViewById(R.id.client_newappointment_button_select_time)
        spinner_casetype = findViewById(R.id.client_spinner_casetype)
        edittext_note = findViewById(R.id.client_newappointment_edittext_note)
        relativelayout_save = findViewById(R.id.client_newappointment_relativelayout_save)
        relativelayout_cancel = findViewById(R.id.client_newappointment_relativelayout_cancel)

        progressDialog = ProgressDialog(this, "Loading...")
        progressDialog.show()

        mAuth = FirebaseAuth.getInstance()
        mAuth.setLanguageCode(Locale.getDefault().displayLanguage)
        mFirestore = FirebaseFirestore.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                finish()
                startActivity(Intent(this, AuthActivity::class.java))
            } else {
                relativelayout_cancel.setOnClickListener {
                    finish()
                }

                button_select_time.setOnClickListener {
                    val timepicker = TimePickerDialog(
                        this, { view, hourOfDay, minute ->
                            time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                            button_select_time.setText("SELECT TIME: ${time}")
                        },
                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                        Calendar.getInstance().get(Calendar.MINUTE),
                        true
                    )
                    timepicker.show()
                    timepicker.setCancelable(false)
                }

                calendarview_select_date.setOnDateChangeListener { view, year, month, dayOfMonth ->
                    date = if (
                        year >= Calendar.getInstance().get(Calendar.YEAR) &&
                        month >= Calendar.getInstance().get(Calendar.MONTH) &&
                        dayOfMonth >= Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    ) {
                        val calendar = Calendar.getInstance()
                        calendar.set(year, month, dayOfMonth)
                        Date(calendar.timeInMillis)
                    } else {
                        Toast.makeText(this, "Select another date!", Toast.LENGTH_SHORT).show()
                        null
                    }
                }

                val genders = arrayOf("Male", "Female",)
                spinner_gender.setAdapter(SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders))
                spinner_gender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        gender = genders[position]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                val case_types = arrayOf("Family", "Divorce", "Inheritance")
                spinner_casetype.setAdapter(SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, case_types))
                spinner_casetype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        casetype = case_types[position]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                relativelayout_save.setOnClickListener {
                    if (lawyer == null)
                        return@setOnClickListener Toast.makeText(this, "Empty lawyer!", Toast.LENGTH_SHORT).show()
                    if (edittext_name.text.toString().isEmpty()) {
                        edittext_name.error = "Empty name!"
                        edittext_name.requestFocus()
                        return@setOnClickListener Toast.makeText(this, "Empty name!", Toast.LENGTH_SHORT).show()
                    }
                    else if (gender == null)
                        return@setOnClickListener Toast.makeText(this, "Empty gender!", Toast.LENGTH_SHORT).show()
                    else if (edittext_address.text.toString().isEmpty()) {
                        edittext_address.error = "Empty address!"
                        edittext_address.requestFocus()
                        return@setOnClickListener Toast.makeText(this, "Empty address!", Toast.LENGTH_SHORT).show()
                    }
                    else if (edittext_mobile_no.text.toString().isEmpty()) {
                        edittext_mobile_no.error = "Empty mobile number!"
                        edittext_mobile_no.requestFocus()
                        return@setOnClickListener Toast.makeText(this, "Empty mobile number!", Toast.LENGTH_SHORT).show()
                    }
                    else if (date == null)
                        return@setOnClickListener Toast.makeText(this, "Empty date!", Toast.LENGTH_SHORT).show()
                    else if (time == null)
                        return@setOnClickListener Toast.makeText(this, "Empty time!", Toast.LENGTH_SHORT).show()
                    else if (casetype == null)
                        return@setOnClickListener Toast.makeText(this, "Empty case type!", Toast.LENGTH_SHORT).show()
                    progressDialog.show()
                    mFirestore.collection("users").document(lawyer!!).collection("appointments").document(caseTicket).set(
                        hashMapOf(
                            "case" to caseTicket,
                            "caseType" to casetype,
                            "client" to user.email,
                            "lawyer" to lawyer!!,
                            "name" to edittext_name.text.toString(),
                            "gender" to gender,
                            "address" to edittext_address.text.toString(),
                            "mobileNo" to edittext_mobile_no.text.toString(),
                            "date" to date,
                            "time" to time,
                            "note" to edittext_note.text.toString(),
                            "status" to "Waiting Lawyer",
                            "createdFrom" to "client"
                        )
                    ).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            mFirestore.collection("users").document(user.email.toString()).collection("appointments").document(caseTicket).set(
                                hashMapOf(
                                    "case" to caseTicket,
                                    "lawyer" to lawyer!!
                                )
                            ).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@ClientNewAppointmentActivity, "Successful Scheduled", Toast.LENGTH_LONG).show()
                                    finish()
                                } else
                                    Toast.makeText(
                                        this@ClientNewAppointmentActivity,
                                        "Error ${task.exception.toString()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                progressDialog.dismiss()
                            }
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@ClientNewAppointmentActivity,
                                "Error ${task.exception.toString()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                mFirestore.collection("users").whereEqualTo("type", "lawyer").get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val lawyers_emails = mutableListOf<String>()
                            val documents = task.result.documents.toTypedArray()
                            if (documents.size <= 0) {
                                finish()
                                Toast.makeText(this@ClientNewAppointmentActivity, "No lawyers available.", Toast.LENGTH_SHORT).show()
                            } else {
                                documents.forEach { document ->
                                    lawyers_emails.add(document.reference.path.split("/").last())
                                }
                                spinner_selectlawyer.setAdapter(SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, lawyers_emails.toTypedArray()))
                                spinner_selectlawyer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                        lawyer = lawyers_emails[position]
                                    }
                                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                                }
                            }
                        } else
                            Toast.makeText(this@ClientNewAppointmentActivity, "Error ${task.exception.toString()}", Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
            }
        }
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