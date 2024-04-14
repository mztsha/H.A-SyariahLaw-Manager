package com.homework.fypmaza

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.homework.fypmaza.lib.ProgressDialog
import com.homework.fypmaza.lib.SuccessfulDialog
import com.homework.fypmaza.lib.TicketGenerator
import java.util.Date
import java.util.Locale

class MessageActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var mDatabase: FirebaseDatabase

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var childEventListener: ChildEventListener
    private lateinit var referenceMessages: DatabaseReference

    private lateinit var progressDialog: ProgressDialog

    private lateinit var textview_casenumber: TextView
    private lateinit var textview_status: TextView
    private lateinit var linearlayout_messagescontainer: LinearLayout
    private lateinit var edittext_message: EditText
    private lateinit var cardview_send: CardView

    private lateinit var buttons_cardviews: GridLayout
    private lateinit var successful_cardview: CardView
    private lateinit var approve_cardview: CardView
    private lateinit var reject_cardview: CardView

    /* Status of cases:
    *
    * 1. Approved
    * 2. Rejected
    * 3. Waiting Lawyer
    * 4. Successful
    * */

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        mAuth = FirebaseAuth.getInstance()
        mAuth.setLanguageCode(Locale.getDefault().displayLanguage)
        mFirestore = FirebaseFirestore.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        progressDialog = ProgressDialog(this, "Loading...")
        progressDialog.show()

        textview_casenumber = findViewById(R.id.message_textview_casenumber)
        textview_status = findViewById(R.id.message_textview_status)
        linearlayout_messagescontainer = findViewById(R.id.message_linearlayout_messagescontainer)
        edittext_message = findViewById(R.id.message_edittext_message)
        cardview_send = findViewById(R.id.message_cardview_send)

        buttons_cardviews = findViewById(R.id.buttons_cardviews)
        successful_cardview = findViewById(R.id.successful_cardview)
        approve_cardview = findViewById(R.id.approve_cardview)
        reject_cardview = findViewById(R.id.reject_cardview)

        val intent_type = intent.getStringExtra("type")
        val intent_caseId = intent.getStringExtra("caseId")

        if (intent_type == null || intent_caseId == null) {
            Toast.makeText(this@MessageActivity, "No data from case...", Toast.LENGTH_SHORT).show()
            object : CountDownTimer(1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    finish()
                }
            }.start()
            return
        } else {
            authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user == null) {
                    progressDialog.dismiss()
                    finish()
                    startActivity(Intent(this, AuthActivity::class.java))
                } else {
                    fun loadView() {
                        clearMessages()
                        if (::referenceMessages.isInitialized)
                            referenceMessages.removeEventListener(childEventListener)
                        if (!progressDialog.isShowing)
                            progressDialog.show()
                        // Get actual user data and get case id - Read case and get lawyer from lawyer/client account to read case from lawyer account.
                        textview_casenumber.text = "Case : $intent_caseId"
                        mFirestore
                            .collection("users")
                            .document(user.email.toString())
                            .collection("appointments")
                            .document(intent_caseId)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result.data
                                    // All data from case readed from email lawyer in the database - Read case from lawyer account appointments.
                                    if (document != null)
                                        mFirestore
                                            .collection("users")
                                            .document(document["lawyer"].toString())
                                            .collection("appointments")
                                            .document(intent_caseId)
                                            .get()
                                            .addOnCompleteListener { task2 ->
                                                if (task2.isSuccessful) {
                                                    val document = task2.result.data
                                                    if (document != null) {
                                                        // if lawyer, can modify case.
                                                        if (intent_type == "lawyer" && document["status"].toString() != "Successful") {
                                                            buttons_cardviews.visibility = View.VISIBLE
                                                            successful_cardview.setOnClickListener {
                                                                changeStatusCase(
                                                                    document["lawyer"].toString(),
                                                                    "Successful",
                                                                    intent_caseId,
                                                                    intent_type
                                                                ) {
                                                                    loadView()
                                                                    SuccessfulDialog(this, intent_caseId,  object : SuccessfulDialog.onDialogEventListener {
                                                                        override fun onGoToHomepage() {
                                                                            finish()
                                                                        }
                                                                    }).show()
                                                                }
                                                            }
                                                            approve_cardview.setOnClickListener {
                                                                changeStatusCase(
                                                                    document["lawyer"].toString(),
                                                                    "Approved",
                                                                    intent_caseId,
                                                                    intent_type
                                                                ) {
                                                                    loadView()
                                                                }
                                                            }
                                                            reject_cardview.setOnClickListener {
                                                                changeStatusCase(
                                                                    document["lawyer"].toString(),
                                                                    "Rejected",
                                                                    intent_caseId,
                                                                    intent_type
                                                                ) {
                                                                    loadView()
                                                                }
                                                            }
                                                        }

                                                        textview_status.text = document["status"].toString() // Change status text.
                                                        childEventListener =
                                                            object : ChildEventListener {
                                                                override fun onChildAdded(
                                                                    dataSnapshot: DataSnapshot,
                                                                    prevChildKey: String?
                                                                ) {
                                                                    // Logic of messages...
                                                                    if (dataSnapshot.childrenCount <= 0) {
                                                                        clearMessages()
                                                                        if (intent_type == "client")
                                                                            createOtherPersonMessage("Case created, waiting for lawyer...")
                                                                        else if (intent_type == "lawyer")
                                                                            createOtherPersonMessage("Change status of case with buttons...")
                                                                    } else {
                                                                        if (dataSnapshot.child("type").value.toString() == intent_type)
                                                                            createOwnMessage(dataSnapshot.child("message").value.toString())
                                                                        else
                                                                            createOtherPersonMessage(dataSnapshot.child("message").value.toString())
                                                                    }
                                                                    // When receive message, scroll down.
                                                                    scrollToDownMessages()
                                                                }

                                                                override fun onChildChanged(
                                                                    dataSnapshot: DataSnapshot,
                                                                    prevChildKey: String?
                                                                ) {}

                                                                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}

                                                                override fun onChildMoved(
                                                                    dataSnapshot: DataSnapshot,
                                                                    prevChildKey: String?
                                                                ) {}

                                                                override fun onCancelled(databaseError: DatabaseError) {
                                                                    Toast.makeText(
                                                                        this@MessageActivity,
                                                                        databaseError.toException().message.toString(),
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        if (::mDatabase.isInitialized) {
                                                            referenceMessages = mDatabase.reference.child("messages").child(intent_caseId.replace("#", ""))
                                                            referenceMessages.removeEventListener(childEventListener)
                                                            referenceMessages.addChildEventListener(childEventListener)
                                                        } else this@MessageActivity.finish()

                                                        // If status is successful, case is closed.
                                                        if (document["status"].toString() == "Successful") {
                                                            edittext_message.setEnabled(false)
                                                            edittext_message.isFocusable = false
                                                            edittext_message.setHint("Case closed. Nothing to do now.")
                                                            cardview_send.setCardBackgroundColor(this@MessageActivity.getColor(R.color.real_gray))
                                                        } else {
                                                            // IF case status is not successful, can send messages.
                                                            cardview_send.setOnClickListener {
                                                                sendMessage(
                                                                    intent_caseId.replace("#", ""),
                                                                    intent_type,
                                                                    edittext_message.text.toString()
                                                                ) {}
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            this@MessageActivity,
                                                            "Null data: ${task2.exception} ${task2.result}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        finish()
                                                    }
                                                    progressDialog.dismiss()
                                                } else {
                                                    progressDialog.dismiss()
                                                    Toast.makeText(
                                                        this@MessageActivity,
                                                        "Error reading data: ${task2.exception.toString()}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    finish()
                                                }
                                            }
                                    else {
                                        progressDialog.dismiss()
                                        Toast.makeText(
                                            this@MessageActivity,
                                            "Error reading data: ${task.exception.toString()}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    }
                                } else {
                                    progressDialog.dismiss()
                                    Toast.makeText(
                                        this@MessageActivity,
                                        "Error reading data: ${task.exception.toString()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                            }
                    }

                    loadView()
                }
            }
            if (::authStateListener.isInitialized)
                mAuth.addAuthStateListener(authStateListener)
        }
    }

    private fun changeStatusCase(lawyer: String, status: String, caseId: String, type_user: String, onChange: () -> Unit) {
        // Change status from case in lawyer account.
        mFirestore
            .collection("users")
            .document(lawyer)
            .collection("appointments")
            .document(caseId)
            .update(
                mapOf(
                    "status" to status
                )
            ).addOnCompleteListener { task ->
                Toast.makeText(this@MessageActivity, if (task.isSuccessful) "Successful updated!" else "Error updating: ${task.exception?.message.toString()}", Toast.LENGTH_LONG).show()
                sendMessage(
                    caseId.replace("#", ""),
                    type_user,
                    "Case changed from ${textview_status.text} to ${status}"
                ) {
                    onChange()
                }
            }
    }

    private fun scrollToDownMessages() {
        val scrollView = (linearlayout_messagescontainer.parent as ScrollView)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun sendMessage(caseId: String, type_user: String, message: String, onSend: () -> Unit){
        mDatabase.reference.child("messages").child(caseId).get().addOnSuccessListener { datasnapshot ->
            mDatabase.reference.child("messages").child(caseId).child(datasnapshot.childrenCount.toString()).setValue(
                    mapOf(
                        "type" to type_user,
                        "message" to message,
                        "date" to Date()
                    )
                ).addOnCompleteListener { task ->
                    if (!task.isSuccessful)
                        Toast.makeText(
                            this@MessageActivity,
                            task.exception.toString(),
                            Toast.LENGTH_LONG
                        ).show()
                    edittext_message.setText("")
                    onSend()
                }
        }.addOnFailureListener{ exception ->
            Toast.makeText(this@MessageActivity, exception.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun clearMessages() {
        linearlayout_messagescontainer.removeAllViewsInLayout()
    }

    private fun createOtherPersonMessage(message: String) {
        val textView = TextView(this)
        textView.apply {
            text = message
            setBackgroundResource(R.drawable.bg2)
            setPadding(30, 30, 30, 30)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    (15 * resources.displayMetrics.scaledDensity + 0.5f).toInt(),
                    (15 * resources.displayMetrics.scaledDensity + 0.5f).toInt(),
                    (150 * resources.displayMetrics.scaledDensity + 0.5f).toInt(),
                    0
                )
            }
        }
        linearlayout_messagescontainer.addView(textView)
        scrollToDownMessages()
    }

    private fun createOwnMessage(message: String) {
        val textView = TextView(this)
        textView.apply {
            text = message
            setBackgroundResource(R.drawable.bg1)
            setPadding(30, 30, 30, 30)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    (150 * resources.displayMetrics.scaledDensity + 0.5f).toInt(),
                    (15 * resources.displayMetrics.scaledDensity + 0.5f).toInt(),
                    (15 * resources.displayMetrics.scaledDensity + 0.5f).toInt(),
                    0
                )
            }
        }
        linearlayout_messagescontainer.addView(textView)
        scrollToDownMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::authStateListener.isInitialized)
            mAuth.removeAuthStateListener(authStateListener)
        if (::referenceMessages.isInitialized)
            referenceMessages.removeEventListener(childEventListener)
    }
}