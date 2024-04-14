package com.homework.fypmaza.Lawyer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.homework.fypmaza.R
import com.homework.fypmaza.AuthActivity
import com.homework.fypmaza.MessageActivity
import com.homework.fypmaza.lib.ProgressDialog
import java.util.Locale

class LawyerDashboardActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    private lateinit var textview_total_clients: TextView
    private lateinit var textview_total_cases: TextView
    private lateinit var textview_total_appointments: TextView
    private lateinit var relativelayout_logout: RelativeLayout
    private lateinit var calendarview_appointmentcalendar: TextView
    private lateinit var relativelayout_addappointment: RelativeLayout
    private lateinit var linearlayout_clients: LinearLayout

    private lateinit var progressdialog: ProgressDialog

    private var user: FirebaseUser? = null
    private var total_clients: Int = 0
    private var total_cases: Int = 0
    private var total_appointments: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lawyer_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        mAuth.setLanguageCode(Locale.getDefault().displayLanguage)
        mFirestore = FirebaseFirestore.getInstance()

        textview_total_clients = findViewById(R.id.lawyerdashboard_textview_total_clients)
        textview_total_cases = findViewById(R.id.lawyerdashboard_textview_total_cases)
        textview_total_appointments = findViewById(R.id.lawyerdashboard_textview_total_appointments)
        relativelayout_logout = findViewById(R.id.lawyerdashboard_relativelayout_logout)
        calendarview_appointmentcalendar = findViewById(R.id.lawyerdashboard_calendarview_appointmentcalendar)
        relativelayout_addappointment = findViewById(R.id.lawyerdashboard_relativelayout_addappointment)
        linearlayout_clients = findViewById(R.id.lawyerdashboard_linearlayout_clients)

        progressdialog = ProgressDialog(this, "Connecting to database...")
        progressdialog.show()

        relativelayout_addappointment.setOnClickListener {
            startActivity(Intent(this, LawyerNewAppointmentActivity::class.java))
        }

        relativelayout_logout.setOnClickListener {
            mAuth.signOut()
            validateAuth()
        }

        validateAuth()
    }

    private fun validateAuth() {
        user = mAuth.currentUser
        if (user == null) {
            finish()
            startActivity(Intent(this@LawyerDashboardActivity, AuthActivity::class.java))
        } else
            updateView(user!!)
    }

    override fun onResume() {
        super.onResume()
        validateAuth()
    }

    @SuppressLint("SetTextI18n")
    fun updateView(user: FirebaseUser) {
        if (!progressdialog.isShowing)
            progressdialog.show()
        mFirestore
            .collection("users")
            .document(user.email.toString())
            .collection("appointments")
            .get()
            .addOnCompleteListener { task ->
                total_clients = 0
                total_cases = 0
                total_appointments = 0
                calendarview_appointmentcalendar.text = ""
                if (task.isSuccessful) {
                    val documents = task.result.documents.toTypedArray()
                    var document_index = 0
                    val clients = mutableMapOf<String, MutableMap<String, Any>>()
                    for (document in documents)
                        if (document.data != null) {
                            try {
                                if (!clients.containsKey(document.data!!["client"].toString()))
                                    clients[document.data!!["client"].toString()] = mutableMapOf(
                                        "appointment" to 0,
                                        "cases" to 0,
                                        "cases_array" to Array(documents.size) { document.data!!["case"].toString() }
                                    )
                                if (document.data!!["status"].toString() != "Waiting Lawyer" && document.data!!["status"].toString() != "Successful") {
                                    total_cases += 1
                                    clients[document.data!!["client"].toString()]?.let {
                                        it["cases"] = it.getOrDefault("cases", 0) as Int + 1
                                    }
                                }
                                clients[document.data!!["client"].toString()]?.let {
                                    it["appointment"] = it.getOrDefault("appointment", 0) as Int + 1
                                }
                                clients[document.data!!["client"].toString()]?.let {
                                    (it["cases_array"] as Array<String>).set(total_appointments, document.data!!["case"].toString())
                                }
                                document_index++
                                total_appointments++
                                calendarview_appointmentcalendar.text = calendarview_appointmentcalendar.text.toString() +
                                        "\nAppointment ${document_index}\n" +
                                        "Case: ${document.data!!["case"]}\n" +
                                        "Case Type: ${document.data!!["caseType"]}\n" +
                                        "Client: ${document.data!!["client"]}\n" +
                                        "Date: ${(document.data!!["date"] as Timestamp).toDate()}\n" +
                                        "Time: ${document.data!!["time"]}\n" +
                                        "Gender: ${document.data!!["gender"]}\n" +
                                        "Mobile Number: ${document.data!!["mobileNo"]}\n" +
                                        "Status: ${document.data!!["status"]}\n" +
                                        "Notes: ${document.data!!["note"]}\n"
                            } catch (_: Exception) {}
                        }
                    linearlayout_clients.removeAllViewsInLayout()
                    // Clients dashboard section.
                    total_clients = clients.keys.size
                    for ((email, data) in clients)
                        linearlayout_clients.addView(
                            createClientView(
                                this@LawyerDashboardActivity,
                                email,
                                "${data["cases"]} cases.\n${data["appointment"]} appointments.",
                                data["cases_array"] as Array<String>
                            )
                        )
                } else {
                    Toast.makeText(
                        this@LawyerDashboardActivity,
                        "Error reading database. ${task.exception.toString()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // When finish reading... -> Clients
                progressdialog.dismiss()
                // When all is finished set count of clients and cases.
                textview_total_clients.text = "$total_clients"
                textview_total_cases.text = "$total_cases"
                textview_total_appointments.text = "$total_appointments"
            }
    }

    @SuppressLint("SetTextI18n")
    fun createClientView(
        context: Context,
        email: String,
        cases_appointments: String,
        case_tickets: Array<String>
    ): LinearLayout {
        val parentLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg1)
            setPadding(50, 50, 50, 50)
            val marginParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            marginParams.setMargins(0, 10, 0, 10)
            layoutParams = marginParams
        }

        val emailLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val emailLabel = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Email:"
            setTypeface(ResourcesCompat.getFont(this@LawyerDashboardActivity, R.font.sfmedium), Typeface.BOLD)
            textSize = 16f
            setTextColor(0xFF0085FF.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }

        val emailTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            val marginParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            marginParams.setMargins(15, 0, 0, 0)
            layoutParams = marginParams
            text = email
            textSize = 16f
        }

        emailLayout.addView(emailLabel)
        emailLayout.addView(emailTextView)

        val casesTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg2)
            setPadding(30, 30, 30, 30)
            text = cases_appointments
        }

        val sendMessageLabel = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Send Message"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.real_gray))
            setTypeface(ResourcesCompat.getFont(this@LawyerDashboardActivity, R.font.sfmedium), Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            val marginParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            marginParams.setMargins(0, 0, 0, 10)
            layoutParams = marginParams
        }

        fun showListViewDialog(items: Array<String>, onItemSelectListener: (String) -> Unit) {
            val context = this@LawyerDashboardActivity
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Select a case")
            val listView = ListView(context)
            listView.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, items)
            builder.setView(listView)
            val dialog = builder.create()
            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedItem = items[position]
                onItemSelectListener(selectedItem)
                dialog.dismiss()
            }
            dialog.show()
        }

        sendMessageLabel.setOnClickListener {
            showListViewDialog(case_tickets) { selectedItem ->
                val intent = Intent(this@LawyerDashboardActivity, MessageActivity::class.java)
                intent.putExtra("type", "lawyer")
                intent.putExtra("caseId", selectedItem)
                startActivity(intent)
            }
        }

        parentLayout.addView(emailLayout)
        parentLayout.addView(sendMessageLabel)
        parentLayout.addView(casesTextView)
        return parentLayout
    }
}