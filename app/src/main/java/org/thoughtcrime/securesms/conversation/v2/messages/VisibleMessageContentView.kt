package org.thoughtcrime.securesms.conversation.v2.messages

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import kotlinx.android.synthetic.main.view_visible_message_content.view.*
import network.loki.messenger.R
import org.session.libsession.utilities.ThemeUtil
import org.session.libsession.utilities.ViewUtil
import org.thoughtcrime.securesms.database.model.MessageRecord
import org.thoughtcrime.securesms.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.loki.utilities.UiMode
import org.thoughtcrime.securesms.loki.utilities.UiModeUtilities
import org.thoughtcrime.securesms.loki.utilities.getColorWithID
import org.thoughtcrime.securesms.loki.utilities.toPx
import org.thoughtcrime.securesms.mms.GlideRequests
import java.lang.IllegalStateException

class VisibleMessageContentView : LinearLayout {
    var onContentClick: (() -> Unit)? = null

    // TODO: Large emojis

    // region Lifecycle
    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        LayoutInflater.from(context).inflate(R.layout.view_visible_message_content, this)
    }
    // endregion

    // region Updating
    fun bind(message: MessageRecord, isStartOfMessageCluster: Boolean, isEndOfMessageCluster: Boolean, glide: GlideRequests) {
        // Background
        val background = getBackground(message.isOutgoing, isStartOfMessageCluster, isEndOfMessageCluster)
        val colorID = if (message.isOutgoing) R.attr.message_sent_background_color else R.attr.message_received_background_color
        val color = ThemeUtil.getThemedColor(context, colorID)
        val filter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        background.colorFilter = filter
        setBackground(background)
        // Body
        mainContainer.removeAllViews()
        onContentClick = null
        if (message is MmsMessageRecord && message.linkPreviews.isNotEmpty()) {
            val linkPreviewView = LinkPreviewView(context)
            linkPreviewView.bind(message, glide, background)
            mainContainer.addView(linkPreviewView)
            // Body text view is inside the link preview for layout convenience
        } else if (message is MmsMessageRecord && message.quote != null) {
            val quote = message.quote!!
            val quoteView = QuoteView(context, QuoteView.Mode.Regular)
            quoteView.bind(quote.author.toString(), quote.text, quote.attachment, message.recipient, message.isOutgoing)
            mainContainer.addView(quoteView)
            val bodyTextView = VisibleMessageContentView.getBodyTextView(context, message)
            ViewUtil.setPaddingTop(bodyTextView, 0)
            mainContainer.addView(bodyTextView)
        } else if (message is MmsMessageRecord && message.slideDeck.audioSlide != null) {
            val voiceMessageView = VoiceMessageView(context)
            voiceMessageView.bind(message, background)
            mainContainer.addView(voiceMessageView)
            onContentClick = { voiceMessageView.togglePlayback() }
        } else if (message is MmsMessageRecord && message.slideDeck.documentSlide != null) {
            val documentView = DocumentView(context)
            documentView.bind(message, VisibleMessageContentView.getTextColor(context, message))
            mainContainer.addView(documentView)
        } else if (message is MmsMessageRecord && message.slideDeck.asAttachments().isNotEmpty()) {
            throw IllegalStateException("Not yet implemented; we may want to use Signal's album view here.")
        } else {
            val bodyTextView = VisibleMessageContentView.getBodyTextView(context, message)
            mainContainer.addView(bodyTextView)
        }
    }

    private fun getBackground(isOutgoing: Boolean, isStartOfMessageCluster: Boolean, isEndOfMessageCluster: Boolean): Drawable {
        val isSingleMessage = (isStartOfMessageCluster && isEndOfMessageCluster)
        @DrawableRes val backgroundID: Int
        if (isSingleMessage) {
            backgroundID = if (isOutgoing) R.drawable.message_bubble_background_sent_alone else R.drawable.message_bubble_background_received_alone
        } else if (isStartOfMessageCluster) {
            backgroundID = if (isOutgoing) R.drawable.message_bubble_background_sent_start else R.drawable.message_bubble_background_received_start
        } else if (isEndOfMessageCluster) {
            backgroundID = if (isOutgoing) R.drawable.message_bubble_background_sent_end else R.drawable.message_bubble_background_received_end
        } else {
            backgroundID = if (isOutgoing) R.drawable.message_bubble_background_sent_middle else R.drawable.message_bubble_background_received_middle
        }
        return ResourcesCompat.getDrawable(resources, backgroundID, context.theme)!!
    }

    fun recycle() {
        mainContainer.removeAllViews()
    }
    // endregion

    // region Convenience
    companion object {

        fun getBodyTextView(context: Context, message: MessageRecord): TextView {
            val result = TextView(context)
            val vPadding = context.resources.getDimension(R.dimen.small_spacing).toInt()
            val hPadding = toPx(12, context.resources)
            result.setPadding(hPadding, vPadding, hPadding, vPadding)
            result.text = message.body
            result.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.small_font_size))
            val color = getTextColor(context, message)
            result.setTextColor(color)
            return result
        }

        @ColorInt
        fun getTextColor(context: Context, message: MessageRecord): Int {
            val uiMode = UiModeUtilities.getUserSelectedUiMode(context)
            val colorID = if (message.isOutgoing) {
                if (uiMode == UiMode.NIGHT) R.color.black else R.color.white
            } else {
                if (uiMode == UiMode.NIGHT) R.color.white else R.color.black
            }
            return context.resources.getColorWithID(colorID, context.theme)
        }
    }
    // endregion
}