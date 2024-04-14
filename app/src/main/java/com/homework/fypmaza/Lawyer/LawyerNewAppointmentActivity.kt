package com.homework.fypmaza.Lawyer

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


class LawyerNewAppointmentActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var progressDialog: ProgressDialog

    private var client: String? = null
    private var caseTicket: String = TicketGenerator.generateTicketNumber()
    private var casetype: String? = null
    private var date: Date? = null
    private var time: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lawyer_new_appointment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val spinner_selectlawyer: Spinner = findViewById(R.id.lawyernewappointment_spinner_selectlawyer)
        val edittext_mobile_no: EditText = findViewById(R.id.lawyernewappointment_edittext_mobile_no)
        val calendarview_select_date: CalendarView = findViewById(R.id.lawyernewappointment_calendarview_select_date)
        val button_select_time: Button = findViewById(R.id.lawyernewappointment_button_select_time)
        val spinner_casetype: Spinner = findViewById(R.id.lawyer_spinner_casetype)
        val edittext_note: EditText = findViewById(R.id.lawyernewappointment_edittext_note)
        val relativelayout_save: RelativeLayout = findViewById(R.id.lawyernewappointment_relativelayout_save)
        val relativelayout_cancel: RelativeLayout = findViewById(R.id.lawyernewappointment_relativelayout_cancel)

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
                            button_select_time.text = "SELECT TIME: ${time}"
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

                val case_types = arrayOf("Family", "Divorce", "Inheritance")
                spinner_casetype.setAdapter(SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, case_types))
                spinner_casetype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        casetype = case_types[position]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                relativelayout_save.setOnClickListener {
                    if (edittext_mobile_no.text.toString().isEmpty()) {
                        edittext_mobile_no.error = "Empty mobile number!"
                        edittext_mobile_no.requestFocus()
                        return@setOnClickListener Toast.makeText(this, "Empty mobile number!", Toast.LENGTH_SHORT).show()
                    }
                    else if (client == null)
                        return@setOnClickListener Toast.makeText(this, "Empty client!", Toast.LENGTH_SHORT).show()
                    else if (date == null)
                        return@setOnClickListener Toast.makeText(this, "Empty date!", Toast.LENGTH_SHORT).show()
                    else if (time == null)
                        return@setOnClickListener Toast.makeText(this, "Empty time!", Toast.LENGTH_SHORT).show()
                    else if (casetype == null)
                        return@setOnClickListener Toast.makeText(this, "Empty case type!", Toast.LENGTH_SHORT).show()
                    mFirestore.collection("users").document(user.email.toString()).collection("appointments").document(caseTicket).set(
                        hashMapOf(
                            "case" to caseTicket,
                            "caseType" to casetype,
                            "client" to client,
                            "lawyer" to user.email.toString(),
                            "name" to "",
                            "gender" to "",
                            "address" to "",
                            "mobileNo" to edittext_mobile_no.text.toString(),
                            "date" to date,
                            "time" to time,
                            "note" to edittext_note.text.toString(),
                            "status" to "Waiting Lawyer",
                            "createdFrom" to "lawyer"
                        )
                    ).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            mFirestore.collection("users").document(client!!).collection("appointments").document(caseTicket).set(
                                hashMapOf(
                                    "case" to caseTicket,
                                    "lawyer" to user.email.toString()
                                )
                            ).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@LawyerNewAppointmentActivity, "Successful Scheduled", Toast.LENGTH_LONG).show()
                                    finish()
                                } else
                                    Toast.makeText(this@LawyerNewAppointmentActivity, "Error ${task.exception.toString()}", Toast.LENGTH_LONG).show()
                            }
                        } else
                            Toast.makeText(this@LawyerNewAppointmentActivity, "Error ${task.exception.toString()}", Toast.LENGTH_LONG).show()
                    }
                }

                mFirestore.collection("users").whereEqualTo("type", "client").get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            progressDialog.dismiss()
                            val clients_emails = mutableListOf<String>()
                            val documents = task.result.documents.toTypedArray()
                            if (documents.size <= 0) {
                                finish()
                                Toast.makeText(this@LawyerNewAppointmentActivity, "No lawyers available.", Toast.LENGTH_SHORT).show()
                            } else {
                                task.result.documents.toTypedArray().forEach { document ->
                                    clients_emails.add(document.reference.path.split("/").last())
                                }
                                spinner_selectlawyer.setAdapter(SpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, clients_emails.toTypedArray()))
                                spinner_selectlawyer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                        client = clients_emails[position]
                                    }
                                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                                }
                            }
                        } else {
                            Toast.makeText(this@LawyerNewAppointmentActivity, "Error ${task.exception.toString()}", Toast.LENGTH_LONG).show()
                            progressDialog.dismiss()
                        }
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