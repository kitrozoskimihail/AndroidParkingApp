package com.example.parkly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        val buttonGoToLogin = findViewById<Button>(R.id.buttonGoToLogin)
        buttonGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }
}

