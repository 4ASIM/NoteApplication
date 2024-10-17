package com.example.notesapplication.sharedPreference

import android.content.Context
import android.content.SharedPreferences

class sharedpreference(context: Context) {
 // this is the  shared preference file
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("Userid", Context.MODE_PRIVATE)

    fun saveUserData(userId: String) {
        val editor = sharedPreferences.edit()
        editor.putString("USER_ID", userId)
        editor.apply()
    }

    fun getUserData(): String? {
        return sharedPreferences.getString("USER_ID", null)
    }
}
