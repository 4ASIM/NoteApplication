package com.example.notesapplication.roomdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(notes: List<Note>)

    @Update
    suspend fun update(note: Note)

    @Query("SELECT * FROM notes")
     fun getAllNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId")
     fun getNotesByUserId(userId: String): List<Note>

    @Query("UPDATE notes SET title = :title, content = :content WHERE id = :noteId")
    fun updateNote(noteId: Int, title: String, content: String)


    @Query("DELETE FROM notes WHERE id = :noteId")
    fun deleteNoteById(noteId: Int)

}
