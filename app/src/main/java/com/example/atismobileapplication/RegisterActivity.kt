package com.example.atismobileapplication

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    //initializing dependencies
    private lateinit var auth: FirebaseAuth
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var db: FirebaseFirestore
    private lateinit var secretKey: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = FirebaseFirestore.getInstance()
        secretKey = "atis"

        val tv_haveacc = findViewById<TextView>(R.id.tv_haveaccount)
        val et_email = findViewById<TextView>(R.id.et_email)
        val et_password = findViewById<TextView>(R.id.et_password)
        val btn_confirm = findViewById<TextView>(R.id.btn_signup)
        tv_haveacc.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        btn_confirm.setOnClickListener {
            auth = FirebaseAuth.getInstance()
            email = et_email.text.toString()
            password = et_password.text.toString()
            val uid = auth.currentUser?.uid
            val encryptedPassword = hashPassword(password, secretKey)
            val user = hashMapOf(
                "email" to email,
                "password" to encryptedPassword,
                "created_at" to System.currentTimeMillis(),
                "updated_at" to System.currentTimeMillis(),
                "role" to "user",
                "statusLogin" to false,
                "uid" to uid,
                "lastLogin" to System.currentTimeMillis()
            )
            db.collection("users").document(uid.toString())
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    Toast.makeText(baseContext, "Register Success", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    Toast.makeText(baseContext, "Register Failed", Toast.LENGTH_SHORT).show()
                }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        showMessages("Authentication failed.")
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
        else{
            showMessages("Authentication failed.")
        }
    }



    private fun hashPassword(password: String, secretKey: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        md.update(secretKey.toByteArray())
        val digest = md.digest(bytes)
        return bytesToHex(digest)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }


    public override fun onStart() {
        super.onStart()
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun reload() {
        Toast.makeText(
            baseContext,
            "Authentication failed.",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun showMessages(message: String){
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }
}