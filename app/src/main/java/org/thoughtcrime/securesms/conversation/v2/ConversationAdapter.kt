package org.thoughtcrime.securesms.conversation.v2

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import network.loki.messenger.R
import org.thoughtcrime.securesms.conversation.v2.messages.ControlMessageView
import org.thoughtcrime.securesms.conversation.v2.messages.VisibleMessageView
import org.thoughtcrime.securesms.database.CursorRecyclerViewAdapter
import org.thoughtcrime.securesms.database.DatabaseFactory
import org.thoughtcrime.securesms.database.model.MessageRecord
import org.thoughtcrime.securesms.loki.utilities.getColorWithID
import java.lang.IllegalStateException

class ConversationAdapter(context: Context, cursor: Cursor, private val onItemPress: (MessageRecord, Int) -> Unit,
    private val onItemLongPress: (MessageRecord, Int) -> Unit) : CursorRecyclerViewAdapter<ViewHolder>(context, cursor) {
    private val messageDB = DatabaseFactory.getMmsSmsDatabase(context)
    var selectedItems = mutableSetOf<MessageRecord>()

    sealed class ViewType(val rawValue: Int) {
        object Visible : ViewType(0)
        object Control : ViewType(1)

        companion object {

            val allValues: Map<Int, ViewType> get() = mapOf(
                Visible.rawValue to Visible,
                Control.rawValue to Control
            )
        }
    }

    class VisibleMessageViewHolder(val view: VisibleMessageView) : ViewHolder(view)
    class ControlMessageViewHolder(val view: ControlMessageView) : ViewHolder(view)

    override fun getItemViewType(cursor: Cursor): Int {
        val message = getMessage(cursor)!!
        if (message.isControlMessage) { return ViewType.Control.rawValue }
        return ViewType.Visible.rawValue
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        @Suppress("NAME_SHADOWING")
        val viewType = ViewType.allValues[viewType]
        when (viewType) {
            ViewType.Visible -> {
                val view = VisibleMessageView(context)
                return VisibleMessageViewHolder(view)
            }
            ViewType.Control -> {
                val view = ControlMessageView(context)
                return ControlMessageViewHolder(view)
            }
            else -> throw IllegalStateException("Unexpected view type: $viewType.")
        }
    }

    override fun onBindItemViewHolder(viewHolder: ViewHolder, cursor: Cursor) {
        val message = getMessage(cursor)!!
        when (viewHolder) {
            is VisibleMessageViewHolder -> {
                val view = viewHolder.view
                view.background = if (selectedItems.contains(message)) {
                    ColorDrawable(context.resources.getColorWithID(R.color.red, context.theme))
                } else {
                    null
                }
                view.bind(message)
                view.setOnClickListener { onItemPress(message, viewHolder.adapterPosition) }
                view.setOnLongClickListener {
                    onItemLongPress(message, viewHolder.adapterPosition)
                    true
                }
            }
            is ControlMessageViewHolder -> viewHolder.view.bind(message)
        }
    }

    override fun onItemViewRecycled(viewHolder: ViewHolder?) {
        when (viewHolder) {
            is VisibleMessageViewHolder -> viewHolder.view.recycle()
            is ControlMessageViewHolder -> viewHolder.view.recycle()
        }
        super.onItemViewRecycled(viewHolder)
    }

    private fun getMessage(cursor: Cursor): MessageRecord? {
        return messageDB.readerFor(cursor).current
    }

    fun toggleSelection(message: MessageRecord, position: Int) {
        if (selectedItems.contains(message)) selectedItems.remove(message) else selectedItems.add(message)
        notifyItemChanged(position)
    }
}