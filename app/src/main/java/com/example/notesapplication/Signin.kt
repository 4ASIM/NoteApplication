package com.example.notesapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapplication.databinding.ActivitySigninBinding // Import the generated binding class
import com.example.notesapplication.roomdatabase.AppDatabase
import com.example.notesapplication.roomdatabase.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Signin : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private lateinit var roomDb: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)
        roomDb = AppDatabase.getDatabase(this)

        binding.signupButton.setOnClickListener {
            val emaildata = binding.signupEmail.text.toString().trim()
            val passworddata = binding.signupPassword.text.toString().trim()
            val conpassworddata = binding.signupConfirm.text.toString().trim()

            if (emaildata.isEmpty() || passworddata.isEmpty() || conpassworddata.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show()
            } else if (passworddata != conpassworddata) {
                Toast.makeText(
                    this,
                    "Password and confirm password do not match",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val existingUser = roomDb.userDao().getUserByEmail(emaildata)

                    if (existingUser != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                this@Signin,
                                "User already registered with this email",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val user = User(email = emaildata, password = passworddata)
                        roomDb.userDao().insertUser(user)

                        CoroutineScope(Dispatchers.Main).launch {
                            binding.signupEmail.text.clear()
                            binding.signupPassword.text.clear()
                            binding.signupConfirm.text.clear()

                            Toast.makeText(
                                this@Signin,
                                "User added Successfully",
                                Toast.LENGTH_LONG
                            ).show()

                            val intent = Intent(this@Signin, MainActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}
