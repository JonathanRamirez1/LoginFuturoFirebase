package com.jonathan.loginfuturo.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.icu.number.IntegerWidth
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat.getSystemService
import com.google.gson.Gson
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.model.FCMBody
import com.jonathan.loginfuturo.model.FCMResponse
import com.jonathan.loginfuturo.model.Message
import com.jonathan.loginfuturo.providers.MessageProvider
import com.jonathan.loginfuturo.providers.NotificationProvider
import com.jonathan.loginfuturo.providers.TokenProvider
import com.jonathan.loginfuturo.services.MyFirebaseMessagingClient.Companion.NOTIFICATION_REPLY
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class MessageReceiver: BroadcastReceiver() {

    private var idEmisor: String = ""
    private var idReceptor: String = ""
    private var idChat: String = ""
    private var usernameEmisor: String = ""
    private var usernameReceptor: String = ""
    private var imageEmisor: String = ""
    private var imageReceptor: String = ""
    private var idNotification: Int = 0

    private lateinit var tokenProvider: TokenProvider
    private lateinit var notificationProvider: NotificationProvider

    override fun onReceive(context: Context, intent: Intent) {
        idEmisor = intent.getStringExtra("idEmisor").toString()
        idReceptor = intent.getStringExtra("idReceptor").toString()
        idChat = intent.getStringExtra("idChat").toString()
        usernameEmisor = intent.getStringExtra("usernameEmisor").toString()
        usernameReceptor = intent.getStringExtra("usernameReceptor").toString()
        imageEmisor = intent.getStringExtra("imageEmisor").toString()
        imageReceptor = intent.getStringExtra("imageReceptor").toString()
        idNotification = Integer.parseInt(intent.extras?.getInt("idNotification").toString())

        tokenProvider = TokenProvider()
        notificationProvider = NotificationProvider()

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(idNotification)

        val message: String = getMessageNotification(intent).toString()

        sendMessage(message)
    }

    private fun sendMessage(messageNotification: String) {
        val message = Message()
        message.setIdChat(idChat)
        message.setIdEmisor(idReceptor)
        message.setIdReceptor(idEmisor)
        message.setTimeStamp(Date().time)
        message.setViewed(false)
        message.setIdChat(idChat)
        message.setMessage(messageNotification)

        val messageProvider = MessageProvider()
        messageProvider.create(message).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                getToken(message)
            }
        }
    }

    private fun getToken(message: Message) {
        tokenProvider.getToken(idEmisor).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot != null) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        val token: String = documentSnapshot.getString("token").toString()
                        val gson = Gson()
                        val messageArrayList: ArrayList<Message> = ArrayList()
                        messageArrayList.add(message)
                        val messages = gson.toJson(messageArrayList)
                        sendNotification(token, messages, message)
                    }
                }
            }
        }
    }

    private fun sendNotification(token: String, messages: String, message: Message) {
        val data: MutableMap<String, String> = HashMap()
        data["title"] = "NEW MESSAGE"
        data["body"] = message.getMessage()
        data["idNotification"] = idNotification.toString()
        data["messages"] = messages
        data["usernameEmisor"] = usernameReceptor.uppercase(Locale.getDefault())
        data["usernameReceptor"] = usernameEmisor.uppercase(Locale.getDefault())
        data["imageEmisor"] = imageReceptor
        data["imageReceptor"] = imageEmisor
        data["idEmisor"] = message.getIdEmisor()
        data["idReceptor"] = message.getIdReceptor()
        data["idChat"] = message.getIdChat()

        val body = FCMBody(token, "high", "4500s", data)
        notificationProvider.sendNotification(body)?.enqueue(object : Callback<FCMResponse?> {
            override fun onResponse(call: Call<FCMResponse?>?, response: Response<FCMResponse?>) {

            }

            override fun onFailure(call: Call<FCMResponse?>?, t: Throwable) {
                Log.d("ERROR", "El error fue: " + t.message)
            }
        })
    }

    private fun getMessageNotification(intent: Intent): CharSequence? {
        val remoteInput: Bundle? = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            return remoteInput.getCharSequence(NOTIFICATION_REPLY)
        }
        return null
    }
}