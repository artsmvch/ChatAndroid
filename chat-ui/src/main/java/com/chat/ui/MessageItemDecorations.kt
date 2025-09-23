package com.chat.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.chat.utils.dp

internal class MessageMarginItemDecoration : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val margin = view.context.dp(8)
        outRect.set(margin, margin, margin, margin)
    }
}

//internal class MessageDateStickyHeadersAdapter(
//    private val adapter: MessageAdapter
//) : StickyRecyclerHeadersAdapter<MessageDateStickyHeadersAdapter.ViewHolder> {
//    override fun getHeaderId(position: Int): Long {
//        val item = adapter.getItem(position) ?: return 0L
//        val millisInDay = 24 * 60 * 60 * 1000
//        return item.timestamp / millisInDay
//    }
//
//    override fun onCreateHeaderViewHolder(parent: ViewGroup): ViewHolder {
//        val itemView = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_date, parent, false)
//        return ViewHolder(itemView)
//    }
//
//    override fun onBindHeaderViewHolder(holder: ViewHolder, position: Int) {
//        holder.bind(adapter.getItem(position))
//    }
//
//    override fun getItemCount(): Int = adapter.itemCount
//
//    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
//        private val textView: TextView = itemView.findViewById(R.id.text)
//
//        @SuppressLint("SetTextI18n")
//        fun bind(item: Message?) {
//            if (item != null) {
//                val date = Date(item.timestamp)
//                textView.text = DATE_FORMAT.format(date)
//            } else {
//                textView.text = "null"
//            }
//        }
//    }
//
//    companion object {
//        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyy")
//    }
//}