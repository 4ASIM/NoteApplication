package com.example.notesapplication.workmanager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.notesapplication.R
import com.example.notesapplication.roomdatabase.AppDatabase
import com.example.notesapplication.roomdatabase.Note
import com.example.notesapplication.roomdatabase.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private var isDataChanged = false

    override fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val noteDao = db.noteDao()
        val userDao = db.userDao()
        val firestore = FirebaseFirestore.getInstance()
        sendSyncStartNotification(applicationContext)
        isDataChanged = false
        CoroutineScope(Dispatchers.IO).launch {
            val roomNotes = noteDao.getAllNotes()
            val roomUsers = userDao.getAllUsers()

            firestore.collection("Notes").get().addOnSuccessListener { firestoreNotes ->

                val firestoreNoteIds = firestoreNotes.documents.mapNotNull { it.id.toIntOrNull() }

                val notesToDelete = firestoreNoteIds.filter { id -> roomNotes.none { it.id == id } }
                val notesToUploadOrUpdate = roomNotes.filter { roomNote ->
                    firestoreNotes.documents.none { it.id == roomNote.id.toString() } ||
                            firestoreNotes.documents.any { it.id == roomNote.id.toString() &&
                                    (it.getString("title") != roomNote.title ||
                                            it.getString("content") != roomNote.content) }
                }

                if (notesToDelete.isNotEmpty() || notesToUploadOrUpdate.isNotEmpty()) {
                    isDataChanged = true
                    deleteNotesFromFirestore(notesToDelete, firestore)
                    uploadOrUpdateNotes(notesToUploadOrUpdate, firestore)
                }
            }

            firestore.collection("Users").get().addOnSuccessListener { firestoreUsers ->
                val firestoreUserIds = firestoreUsers.documents.mapNotNull { it.id.toIntOrNull() }
                val usersToDelete = firestoreUserIds.filter { id -> roomUsers.none { it.id == id } }
                val usersToUploadOrUpdate = roomUsers.filter { roomUser ->
                    firestoreUsers.documents.none { it.id == roomUser.id.toString() } ||
                            firestoreUsers.documents.any { it.id == roomUser.id.toString() &&
                                    it.getString("email") != roomUser.email }
                }

                if (usersToDelete.isNotEmpty() || usersToUploadOrUpdate.isNotEmpty()) {
                    isDataChanged = true
                    deleteUsersFromFirestore(usersToDelete, firestore)
                    uploadOrUpdateUsers(usersToUploadOrUpdate, firestore)
                }

                if (isDataChanged) {
                    sendSyncCompletionNotification(applicationContext, success = true)
                } else {
                    sendSyncCompletionNotification(applicationContext, success = false)
                }
            }
        }

        return Result.success()
    }

    private fun deleteNotesFromFirestore(notesToDelete: List<Int>, firestore: FirebaseFirestore) {
        notesToDelete.forEach { id ->
            firestore.collection("Notes").document(id.toString()).delete()
                .addOnSuccessListener { Log.d("SyncWorker", "Note deleted: $id") }
                .addOnFailureListener { e -> Log.e("SyncWorker", "Failed to delete note: $id", e) }
        }
    }

    private fun deleteUsersFromFirestore(usersToDelete: List<Int>, firestore: FirebaseFirestore) {
        usersToDelete.forEach { id ->
            firestore.collection("Users").document(id.toString()).delete()
                .addOnSuccessListener { Log.d("SyncWorker", "User deleted: $id") }
                .addOnFailureListener { e -> Log.e("SyncWorker", "Failed to delete user: $id", e) }
        }
    }

    private fun uploadOrUpdateNotes(notesToUploadOrUpdate: List<Note>, firestore: FirebaseFirestore) {
        notesToUploadOrUpdate.forEach { note ->
            val noteRef = firestore.collection("Notes").document()  // Generate new document ID
            noteRef.set(note)
                .addOnSuccessListener { Log.d("SyncWorker", "Note synced: ${noteRef.id}") }
                .addOnFailureListener { e -> Log.e("SyncWorker", "Failed to sync note: ${note.id}", e) }
        }
    }

    private fun uploadOrUpdateUsers(usersToUploadOrUpdate: List<User>, firestore: FirebaseFirestore) {
        usersToUploadOrUpdate.forEach { user ->
            val userRef = firestore.collection("Users").document()
            userRef.set(user)
                .addOnSuccessListener {  Log.d("SyncWorker", "User synced: ${userRef.id}") }
                .addOnFailureListener { e -> Log.e("SyncWorker", "Failed to sync user: ${user.id}", e) }
        }
    }

    private fun sendSyncStartNotification(context: Context) {
        val channelId = "sync_channel"
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notebookapp)
            .setContentTitle("Sync Started")
            .setContentText("Data sync is in progress")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sync Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun sendSyncCompletionNotification(context: Context, success: Boolean) {
        val channelId = "sync_channel"
        val message = if (success) {
            "Data sync completed successfully"
        } else {
            "No changes detected"
        }
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notebookapp)
            .setContentTitle("Sync Completed")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sync Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(2, notificationBuilder.build())
    }
}
