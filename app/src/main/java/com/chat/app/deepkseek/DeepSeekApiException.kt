package com.chat.app.deepkseek


internal class DeepSeekApiException(
    val responseCode: Int,
    message: String?
): RuntimeException(message) {
    fun isUnauthorized(): Boolean {
        return responseCode == 401
    }
}