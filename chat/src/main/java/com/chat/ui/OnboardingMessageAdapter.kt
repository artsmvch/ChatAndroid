package com.chat.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.chat.utils.getFirstInstallTime
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel

internal class OnboardingMessageAdapter : RecyclerView.Adapter<OnboardingMessageAdapter.ViewHolder>() {

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_message, parent, false)
        itemView.findViewById<MaterialCardView>(R.id.card)?.apply {
            val cornerRadius = context.resources.getDimension(R.dimen.message_corner_radius)
            val corner = RoundedCornerTreatment(cornerRadius)
            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCorner(corner)
                .setTopRightCorner(corner)
                .setBottomRightCorner(corner)
                .build()
        }
        itemView.findViewById<TextView>(R.id.date)?.apply {
            text = context.getFirstInstallTime().let {
                MessageDateUtils.getDateText(it ?: System.currentTimeMillis())
            }
        }
        itemView.disableTouchRecursively()
        return ViewHolder(itemView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun View.disableTouchRecursively() {
        setOnTouchListener { _, _ -> true }
        if (this is ViewGroup) {
            this.forEach { it.disableTouchRecursively() }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}