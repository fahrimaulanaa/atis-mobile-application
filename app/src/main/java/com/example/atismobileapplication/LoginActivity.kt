package com.example.atismobileapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var auth: FirebaseAuth
    private lateinit var btn_login: AppCompatButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val txt_register = findViewById<TextView>(R.id.tv_doesnthaveaccount)
        val txt_forgotpassword = findViewById<TextView>(R.id.tv_forgotpw)
        txt_register.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        btn_login = findViewById(R.id.btn_login)
        btn_login.setOnClickListener {
            email = findViewById<TextView>(R.id.et_email).text.toString()
            password = findViewById<TextView>(R.id.et_password).text.toString()
            auth = FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        updateUI(null)
                    }
                }
        }
    }
    private fun updateUI(user: FirebaseUser?){
        if(user != null){
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
    }
}