package com.example.notesapplication
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notesapplication.databinding.ActivityAddNoteBinding
import com.example.notesapplication.roomdatabase.AppDatabase
import com.example.notesapplication.roomdatabase.Note
import com.example.notesapplication.sharedPreference.sharedpreference
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class add_note : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appDatabase = AppDatabase.getDatabase(this)

        val sharedPreferencesManager = sharedpreference(this)
        val userId = sharedPreferencesManager.getUserData()

        binding.btnAddNote.setOnClickListener {
            val noteTitle = binding.addNoteTitle.text.toString().trim()
            val noteContent = binding.addNoteDesc.text.toString().trim()

            if (noteTitle.isNotEmpty() && noteContent.isNotEmpty() && userId != null) {
                val noteData = Note(
                    title = noteTitle,
                    content = noteContent,
                    userId = userId
                )

                lifecycleScope.launch {
                    appDatabase.noteDao().insert(noteData)
                }

                db.collection("Notes")
                    .add(noteData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Note added successfully", Toast.LENGTH_SHORT).show()
                        binding.addNoteTitle.text.clear()
                        binding.addNoteDesc.text.clear()
                        val intent = Intent(this@add_note, Dashboard::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to add note: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Please fill out both title and content.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
