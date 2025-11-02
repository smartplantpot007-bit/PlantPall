package com.example.plantpall

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NotificationStorage {

    private const val PREF_NAME = "notification_history"
    private const val KEY_NOTIFICATIONS = "notifications"

    fun saveNotification(context: Context, notification: NotificationModel) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val list = getNotifications(context).toMutableList()
        list.add(notification)

        val jsonArray = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("plantName", it.plantName)
            obj.put("message", it.message)
            obj.put("time", it.time)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
    }

    fun getNotifications(context: Context): List<NotificationModel> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_NOTIFICATIONS, "[]")
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<NotificationModel>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                NotificationModel(
                    obj.getString("plantName"),
                    obj.getString("message"),
                    obj.getString("time")
                )
            )
        }
        return list
    }
}
