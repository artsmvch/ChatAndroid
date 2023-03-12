package com.chat.ui

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet

private const val DATABASE_VERSION = 1

internal fun getMessageDatabase(context: Context, key: String): MessageDatabase {
    return MessageDatabaseImpl(context, key)
}

internal interface MessageDatabase {
    fun queryMessages(): Flow<List<Message>>
    suspend fun insertMessage(message: Message)
    suspend fun getLastMessageId(): Long
}

private class MessageDatabaseImpl(
    private val context: Context,
    private val key: String
): MessageDatabase {
//    private val notificationUri = Uri.Builder()
//        .scheme("content")
//        .authority("chat.$key")
//        .path("messages")
//        .build()
    private val helper = SQLiteOpenHelperImpl(context, "chat.$key.messages")
    private val handler = Handler(context.mainLooper)

    private val contentObservers = CopyOnWriteArraySet<ContentObserver>()

    override fun queryMessages(): Flow<List<Message>> {
        val flow = callbackFlow {
            val database = helper.readableDatabase
            val cursor = database.query("messages", null,
                null, null, null, null, null)
            if (cursor == null) {
                database.close()
                return@callbackFlow
            }
            val observer = object : ContentObserver(handler) {
                override fun deliverSelfNotifications(): Boolean = true
                override fun onChange(selfChange: Boolean) {
                    launch {
                        trySend(rows())
                    }
                }
            }
            cursor.registerContentObserver(observer)
            contentObservers.add(observer)
            trySend(rows())
            awaitClose {
                cursor.unregisterContentObserver(observer)
                contentObservers.remove(observer)
                cursor.close()
                database.close()
            }
        }
        return flow
            .map { messages ->
                withContext(Dispatchers.Default) {
                    messages.distinctBy { it.id }
                }
            }
    }

    private suspend fun rows(): List<Message> = withContext(Dispatchers.IO) {
        helper.readableDatabase.use { database ->
            val cursor = database.query("messages", null,
                null, null, null, null, null)
                ?: return@withContext emptyList()
            cursor.use {
                if (!cursor.moveToFirst()) {
                    return@use emptyList()
                }
                val messages = ArrayList<Message>(cursor.count)
                do {
                    val message = object : Message {
                        override val id: Long = cursor.getLong(cursor.getColumnIndex("id"))
                        override val isFromUser: Boolean =
                            cursor.getInt(cursor.getColumnIndex("from_user")) != 0
                        override val text: CharSequence =
                            cursor.getString(cursor.getColumnIndex("text"))
                    }
                    messages.add(message)
                } while (cursor.moveToNext())
                return@withContext messages
            }
        }
    }

    override suspend fun insertMessage(message: Message) {
        withContext(Dispatchers.IO) {
            val contentValues = ContentValues(3)
            contentValues.put("id", message.id)
            contentValues.put("from_user", if (message.isFromUser) 1 else 0)
            contentValues.put("text", message.text.toString())
            helper.writableDatabase.use { database ->
                database.insert("messages", null, contentValues)
                contentObservers.forEach { observer -> observer.onChange(false) }
            }
        }
    }

    override suspend fun getLastMessageId(): Long {
        return withContext(Dispatchers.IO) {
            helper.readableDatabase.use { database ->
                val cursor: Cursor? = database.rawQuery(
                    "select MAX(id) from messages",
                    null
                )
                val lastId: Long = if (cursor != null && cursor.moveToFirst()) {
                    cursor.getLong(0)
                } else {
                    0L
                }
                cursor?.close()
                database.close()
                return@use lastId
            }
        }
    }

    private class SQLiteOpenHelperImpl(context: Context, name: String) :
        SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE messages(" +
                    "id integer primary key autoincrement not null," +
                    "from_user bit," +
                    "text text" +
                    ")"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}