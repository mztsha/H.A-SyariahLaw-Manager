package com.homework.fypmaza

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.homework.fypmaza.Client.ClientHomeActivity
import com.homework.fypmaza.Lawyer.LawyerDashboardActivity
import com.homework.fypmaza.lib.ProgressDialog
import java.util.Locale

class AuthActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore

    private lateinit var edittextEmail: EditText
    private lateinit var edittextPassword: EditText

    private lateinit var progressdialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        mAuth.setLanguageCode(Locale.getDefault().displayLanguage)
        mFirestore = FirebaseFirestore.getInstance()

        progressdialog = ProgressDialog(this, "Verifying...")
        progressdialog.show()

        edittextEmail = findViewById(R.id.main_edittext_email)
        edittextPassword = findViewById(R.id.main_edittext_password)
        val button_signin: Button = findViewById(R.id.main_button_login)
        val button_signup_lawyer: Button = findViewById(R.id.main_button_signup_lawyer)
        val button_signup_client: Button = findViewById(R.id.main_button_signup_client)
        val textview_lost_password: TextView = findViewById(R.id.main_textview_lost_password)

        // Check if user is signed in (non-null) and update UI accordingly.
        if (mAuth.currentUser == null){
            if (progressdialog.isShowing)
                progressdialog.dismiss()
            textview_lost_password.setOnClickListener {
                val email = edittextEmail.text.toString()
                if (email.isEmpty()) {
                    edittextEmail.error = "Empty email!"
                    edittextEmail.requestFocus()
                    return@setOnClickListener Toast.makeText(
                        this,
                        "Empty email!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (!isValidEmail(email)) {
                    edittextEmail.error = "Not valid email!"
                    edittextEmail.requestFocus()
                    return@setOnClickListener Toast.makeText(
                        this,
                        "Not valid email!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(this) { task ->
                    Toast.makeText(this, if (task.isSuccessful) "Sended successfully!" else "Error sending password reset." + (task.exception?.message ?: task.exception.toString()), Toast.LENGTH_SHORT).show()
                }
            }

            button_signup_lawyer.setOnClickListener {
                progressdialog.setMessage("Creating your account...")
                progressdialog.show()
                if (isValidCredentialsView())
                    createAccount(edittextEmail.text.toString(), edittextPassword.text.toString(), "lawyer")
            }

            button_signup_client.setOnClickListener {
                progressdialog.setMessage("Creating your account...")
                progressdialog.show()
                if (isValidCredentialsView())
                    createAccount(edittextEmail.text.toString(), edittextPassword.text.toString(), "client")
            }

            button_signin.setOnClickListener {
                progressdialog.setMessage("Connecting to your account...")
                progressdialog.show()
                if (isValidCredentialsView())
                    signIn(edittextEmail.text.toString(), edittextPassword.text.toString())
            }
        } else
            try {
                if (mAuth.currentUser!!.email != null)
                    updateUI(mAuth.currentUser)
            } catch (e: Exception) {
                mAuth.signOut()
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_LONG).show()
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matches(email)
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8)
            return false
        // Check for at least one uppercase letter
        var hasUppercase = false
        for (char in password) {
            if (char.isUpperCase()) {
                hasUppercase = true
                break
            }
        }
        if (!hasUppercase)
            return false
        // Check for at least one lowercase letter
        var hasLowercase = false
        for (char in password) {
            if (char.isLowerCase()) {
                hasLowercase = true
                break
            }
        }
        if (!hasLowercase)
            return false
        // Check for at least one digit
        var hasDigit = false
        for (char in password) {
            if (char.isDigit()) {
                hasDigit = true
                break
            }
        }
        if (!hasDigit)
            return false
        // Check for at least one special character
        val specialChars = "!@#$%^&*()_+\\-=\\[{\\]};:'\",<.>/?"
        var hasSpecialChar = false
        for (char in password) {
            if (specialChars.contains(char)) {
                hasSpecialChar = true
                break
            }
        }
        if (!hasSpecialChar)
            return false
        // If all checks pass, the password is valid
        return true
    }

    private fun isValidCredentialsView() : Boolean {
        val email = edittextEmail.text.toString()
        val password = edittextPassword.text.toString()
        if (email.isEmpty()) {
            if (progressdialog.isShowing) progressdialog.dismiss()
            edittextEmail.error = "Empty email!"
            edittextEmail.requestFocus()
            Toast.makeText(this, "Empty email!", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (!isValidEmail(email)) {
            if (progressdialog.isShowing) progressdialog.dismiss()
            edittextEmail.error = "Not valid email!"
            edittextEmail.requestFocus()
            Toast.makeText(this, "Not valid email!", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (password.isEmpty()) {
            if (progressdialog.isShowing) progressdialog.dismiss()
            edittextPassword.error = "Empty password!"
            edittextPassword.requestFocus()
            Toast.makeText(this, "Empty password!", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (!isValidPassword(password)) {
            if (progressdialog.isShowing) progressdialog.dismiss()
            edittextPassword.error = "Minimum length of 8 characters, one uppercase letter, one lowercase letter, one digit and one special character."
            edittextPassword.requestFocus()
            Toast.makeText(this, "Not valid password!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createAccount(email: String, password: String, typeUser: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    if (user != null) {
                        mFirestore.collection("users").document(user.email.toString()).set(hashMapOf(
                            "uid" to user.uid,
                            "type" to typeUser
                        )).addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // Sign in success, update UI with the signed-in user's information
                                updateUI(user)
                                user.sendEmailVerification()
                            } else {
                                if (progressdialog.isShowing) progressdialog.dismiss()
                                // If data creation fails, display a message to the user.
                                Toast.makeText(this,
                                    "Database failed: " + (task.exception?.message
                                        ?: task.exception.toString()),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    if (progressdialog.isShowing) progressdialog.dismiss()
                    // If sign in fails, display a message to the user.
                    Toast.makeText(
                        this,
                        (task.exception?.message ?: task.exception.toString()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun signIn(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful)
                    // Sign in success, update UI with the signed-in user's information
                    updateUI(mAuth.currentUser)
                else {
                    if (progressdialog.isShowing) progressdialog.dismiss()
                    // If sign in fails, display a message to the user.
                    Toast.makeText(
                        baseContext,
                        (task.exception?.message ?: task.exception.toString()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        try {
            if (user == null) {
                if (progressdialog.isShowing) progressdialog.dismiss()
                return Toast.makeText(this, "User not logged.", Toast.LENGTH_SHORT).show()
            }
            if (user.email != null) {
                mFirestore.collection("users").document(user.email.toString()).get()
                    .addOnSuccessListener { document ->
                        if (progressdialog.isShowing) progressdialog.dismiss()
                        if (document != null) {
                            if (document.data == null) {
                                mAuth.signOut()
                                return@addOnSuccessListener Toast.makeText(
                                    this,
                                    "No data in document.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            finish()
                            // Exists data.
                            if (document.data?.get("type").toString() == "lawyer")
                                return@addOnSuccessListener startActivity(Intent(this, LawyerDashboardActivity::class.java))
                            else if (document.data?.get("type").toString() == "client")
                                return@addOnSuccessListener startActivity(Intent(this, ClientHomeActivity::class.java))
                        } else
                            return@addOnSuccessListener Toast.makeText(this, "No such document.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this,"Failed with "+ exception.message.toString(), Toast.LENGTH_SHORT).show()
                    }
            } else {
                if (progressdialog.isShowing) progressdialog.dismiss()
                Toast.makeText(this, "User email logged but email is null.", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            if (progressdialog.isShowing) progressdialog.dismiss()
            Toast.makeText(this, "mAuthentication failed: $e", Toast.LENGTH_SHORT).show()
        }
    }
}