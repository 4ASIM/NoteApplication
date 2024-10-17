package com.example.notesapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.notesapplication.databinding.ActivityMainBinding
import com.example.notesapplication.roomdatabase.AppDatabase
import com.example.notesapplication.sharedPreference.sharedpreference
import com.example.notesapplication.workmanager.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var roomDb: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        roomDb = AppDatabase.getDatabase(this)
        binding.signinButton.setOnClickListener {
            val email = binding.signinEmail.text.toString().trim()
            val password = binding.signinPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {

                CoroutineScope(Dispatchers.IO).launch {
                    val user = roomDb.userDao().getUserByEmail(email)

                    if (user != null && user.password == password) {

                        val sharedPreferencesManager = sharedpreference(this@MainActivity)
                        sharedPreferencesManager.saveUserData(user.id.toString())
                        scheduleSyncWorker()
                        CoroutineScope(Dispatchers.Main).launch {
                            val intent = Intent(this@MainActivity, Dashboard::class.java)
                            startActivity(intent)
                            Log.e("button", "Dashboard is opened")
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                this@MainActivity,
                                "User does not exist or invalid credentials",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        binding.loginRedirectText.setOnClickListener {
            val intent = Intent(this, Signin::class.java)
            startActivity(intent)
        }
    }

    private fun scheduleSyncWorker() {
        val syncRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(
            SyncWorker::class.java, 15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncNotesWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
