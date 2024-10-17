package com.example.notesapplication.adapter

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapplication.R
import com.example.notesapplication.edits_note
import com.example.notesapplication.model.dataclass

class adapterclass(
    private val noteList: MutableList<dataclass>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<adapterclass.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteTitle: TextView = itemView.findViewById(R.id.noteTitle)
        val noteDetail: TextView = itemView.findViewById(R.id.noteDesc)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
        val editIcon: ImageView = itemView.findViewById(R.id.editIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.note_layout, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]
        holder.noteTitle.text = note.datatitle
        holder.noteDetail.text = note.dataDescription

        // this is basically for giving different color to the card view mean it will set the random color to the list and user can be able to see the random color in the card view

        val colors = listOf(
            R.color.colorRed,
            R.color.colorBlue,
            R.color.colorGreen,
            R.color.colorYellow,
            R.color.colorPurple
        )


        val randomColor = colors.random()
        holder.itemView.setBackgroundColor(holder.itemView.context.getColor(randomColor))

        //This will show as alert dialog box for the deletion confirmation that the note is been deleted and from this of any not will be deleted from the list then it will notify and before that it will tell user that he or she want to delete that note of not and the delete that note

        holder.deleteIcon.setOnClickListener {
            // Show an alert dialog for deletion confirmation
            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Delete Note")
            builder.setMessage("Are you sure you want to delete this note?")
            builder.setPositiveButton("Yes") { dialog, which ->
                onDelete(note.id)
            }
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }


        holder.editIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, edits_note::class.java)
            intent.putExtra("noteId", note.id)
            intent.putExtra("noteTitle", note.datatitle)
            intent.putExtra("noteDescription", note.dataDescription)
            holder.itemView.context.startActivity(intent)
        }
    }
// this is basically for counting the number of item in the list from this we can be able to count the now of item in the list
    override fun getItemCount() = noteList.size
}
