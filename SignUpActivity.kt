package com.example.plantpall

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignUpActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLogin: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 1001

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvLogin = findViewById(R.id.tvLogin)
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val googleSignInLayout = findViewById<LinearLayout>(R.id.googleSignInLayout)

        ivBack.setOnClickListener { finish() }

        // ---- PASSWORD TOGGLE ----
        etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                etPassword.compoundDrawables[2]?.let { drawable ->
                    if (event.rawX >= (etPassword.right - drawable.bounds.width())) {
                        togglePasswordVisibility(etPassword, true)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        etConfirmPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                etConfirmPassword.compoundDrawables[2]?.let { drawable ->
                    if (event.rawX >= (etConfirmPassword.right - drawable.bounds.width())) {
                        togglePasswordVisibility(etConfirmPassword, false)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        // ---- GOOGLE SIGN-IN SETUP ----
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLayout.setOnClickListener {
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)
        }

        // ---- EMAIL SIGNUP ----
        btnSignUp.setOnClickListener {
            validateInputs()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun togglePasswordVisibility(editText: EditText, isMainPassword: Boolean) {
        val isVisible = if (isMainPassword) isPasswordVisible else isConfirmPasswordVisible

        editText.inputType = if (isVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        editText.setCompoundDrawablesWithIntrinsicBounds(
            0, 0,
            if (isVisible) R.drawable.ic_eye_off else R.drawable.ic_eye,
            0
        )
        editText.setSelection(editText.text.length)

        if (isMainPassword) isPasswordVisible = !isPasswordVisible
        else isConfirmPasswordVisible = !isConfirmPasswordVisible
    }

    // ---- EMAIL SIGNUP VALIDATION ----
    private fun validateInputs() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        when {
            email.isEmpty() -> etEmail.error = "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                etEmail.error = "Invalid email format"
            password.isEmpty() -> etPassword.error = "Password cannot be empty"
            password.length < 8 -> etPassword.error = "Password must be at least 8 characters"
            !password.contains(Regex(".*\\d.*")) ->
                etPassword.error = "Password must contain at least one number"
            !password.contains(Regex(".*[!@#\$%^&*(),.?\":{}|<>].*")) ->
                etPassword.error = "Password must contain a special character"
            confirmPassword.isEmpty() -> etConfirmPassword.error = "Confirm Password cannot be empty"
            password != confirmPassword -> etConfirmPassword.error = "Passwords do not match"

            else -> {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, HomepgActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()

                        } else {
                            Toast.makeText(this, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }

    // ---- RESULT FOR GOOGLE LOGIN ----
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- FIREBASE GOOGLE AUTH ----
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    Toast.makeText(this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()

                    // FIX: Prevents going back to MainActivity
                    val intent = Intent(this, HomepgActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
