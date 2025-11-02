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

    fun deleteNotificationAt(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val list = getNotifications(context).toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
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
    }

    fun deleteNotificationMatching(context: Context, message: String, time: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val list = getNotifications(context).toMutableList()
        val iterator = list.iterator()
        var removed = false

        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.message == message && item.time == time) {
                iterator.remove()
                removed = true
                break
            }
        }

        if (removed) {
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
        return removed
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
