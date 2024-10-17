package com.example.notesapplication.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.notesapplication.roomdatabase.AppDatabase
import com.example.notesapplication.roomdatabase.Note
import com.google.firebase.firestore.FirebaseFirestore

class NotesSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {


    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()


    private var isDataChanged = false

    override fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()

        // Create database instance
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "NotesDatabase"
        ).build()

        val noteDao = db.noteDao()


        sendSyncStartNotification(applicationContext)

        // Fetch notes from Room DB
        val roomNotes = noteDao.getNotesByUserId(userId)

        // Fetch notes from Firestore and compare
        firestore.collection("notes").get().addOnSuccessListener { firestoreNotes ->
            val firestoreNoteIds = firestoreNotes.documents.map { it.id.toInt() }

            // Determine which notes to delete and which to upload/update
            val notesToDelete = firestoreNoteIds.filter { id -> roomNotes.none { it.id == id } }
            val notesToUploadOrUpdate = roomNotes.filter { roomNote ->
                firestoreNotes.documents.none { it.id == roomNote.id.toString() } ||
                        firestoreNotes.documents.any {
                            it.id == roomNote.id.toString() &&
                                    it.getString("title") != roomNote.title ||
                                    it.getString("content") != roomNote.content
                        }
            }

            // Sync notes: delete or upload/update
            if (notesToDelete.isNotEmpty() || notesToUploadOrUpdate.isNotEmpty()) {
                isDataChanged = true
                deleteNotesFromFirestore(notesToDelete)
                uploadOrUpdateNotes(notesToUploadOrUpdate)
            }

            // Send sync completion notification
            sendSyncCompletionNotification(applicationContext, isDataChanged)
        }

        return Result.success()
    }

    private fun deleteNotesFromFirestore(notesToDelete: List<Int>) {
        notesToDelete.forEach { id ->
            firestore.collection("notes").document(id.toString()).delete()
                .addOnSuccessListener {
                    showToast("Note deleted from Firestore: $id")
                }
                .addOnFailureListener { e ->
                    showToast("Failed to delete note: $id")
                }
        }
    }

    private fun uploadOrUpdateNotes(roomNotes: List<Note>) {
        var successCount = 0
        var failureCount = 0
        val totalNotes = roomNotes.size

        roomNotes.forEach { note ->
            val documentId = note.id.toString()
            firestore.collection("notes")
                .document(documentId)
                .set(note)
                .addOnSuccessListener {
                    successCount++
                    checkIfFinished(successCount, failureCount, totalNotes, "notes")
                }
                .addOnFailureListener {
                    failureCount++
                    checkIfFinished(successCount, failureCount, totalNotes, "notes")
                }
        }
    }

    private fun checkIfFinished(
        successCount: Int,
        failureCount: Int,
        totalItems: Int,
        itemType: String
    ) {
        if (successCount + failureCount == totalItems) {
            val message = "$itemType upload completed: $successCount success, $failureCount failed"
            showToast(message)
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSyncStartNotification(context: Context) {
        val channelId = "notes_sync_channel"
        val notificationBuilder = NotificationCompat.Builder(context, channelId)

            .setContentTitle("Data Sync Started")
            .setContentText("Data synchronization is in progress...")
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sync Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun sendSyncCompletionNotification(context: Context, success: Boolean) {
        val channelId = "notes_sync_channel"
        val notificationMessage = if (success) {
            "Data synchronization completed successfully."
        } else {
            "No changes detected. Data synchronization skipped."
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)

            .setContentTitle("Data Sync Completed")
            .setContentText(notificationMessage)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
        notificationManager.notify(2, notificationBuilder.build())
    }
}
