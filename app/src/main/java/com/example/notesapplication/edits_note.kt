package com.example.notesapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapplication.databinding.ActivityEditsNoteBinding
import com.example.notesapplication.roomdatabase.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class edits_note : AppCompatActivity() {
    private lateinit var binding: ActivityEditsNoteBinding
    private lateinit var noteId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditsNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        noteId = intent.getStringExtra("noteId").toString()
        val title = intent.getStringExtra("noteTitle")
        val description = intent.getStringExtra("noteDescription")
        binding.editNoteTitle.setText(title)
        binding.editNoteDesc.setText(description)
        binding.editNoteFab.setOnClickListener {
            updateNoteInRoom()
        }
    }

    private fun updateNoteInRoom() {
        val idToUpdate = noteId.toInt()

        CoroutineScope(Dispatchers.IO).launch {
            val noteDao = AppDatabase.getDatabase(applicationContext).noteDao()
            noteDao.updateNote(
                idToUpdate,
                binding.editNoteTitle.text.toString(),
                binding.editNoteDesc.text.toString()
            )

            Log.d("edits_note", "Note updated successfully in Room")
            launch(Dispatchers.Main) {
                finish()
            }
        }
    }
}
