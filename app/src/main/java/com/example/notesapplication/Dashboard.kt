package com.example.notesapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapplication.adapter.adapterclass
import com.example.notesapplication.model.dataclass
import com.example.notesapplication.roomdatabase.AppDatabase
import com.example.notesapplication.sharedPreference.sharedpreference
import com.example.notesapplication.databinding.ActivityDashboardBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Dashboard : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var noteAdapter: adapterclass
    private lateinit var noteList: MutableList<dataclass>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        noteList = mutableListOf()
        noteAdapter = adapterclass(noteList) { noteId ->
            deleteNoteFromRoom(noteId)
        }

        binding.homeRecyclerView.adapter = noteAdapter
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.addNoteFab.setOnClickListener {
            val intent = Intent(this, add_note::class.java)
            startActivity(intent)
        }
        fetchNotesFromRoom()
    }

    override fun onResume() {
        super.onResume()
        fetchNotesFromRoom()
    }

    private fun fetchNotesFromRoom() {
        val sharedPreferencesManager = sharedpreference(this)
        val userId = sharedPreferencesManager.getUserData()
        if (userId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val noteDao = AppDatabase.getDatabase(applicationContext).noteDao()
                val notes = noteDao.getNotesByUserId(userId)
                launch(Dispatchers.Main) {
                    noteList.clear()
                    for (note in notes) {
                        noteList.add(dataclass(note.title, note.content, note.id.toString()))
                    }
                    noteAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun deleteNoteFromRoom(noteId: String) {
        val idToDelete = noteId.toInt()
        CoroutineScope(Dispatchers.IO).launch {
            val noteDao = AppDatabase.getDatabase(applicationContext).noteDao()
            noteDao.deleteNoteById(idToDelete)

            launch(Dispatchers.Main) {
                noteList.removeAll { note -> note.id == noteId }
                noteAdapter.notifyDataSetChanged()
                Log.d("Dashboard", "Note deleted from Room and ")
            }
        }
    }
}
