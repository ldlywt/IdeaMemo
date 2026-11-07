package dev.jeziellago.compose.markdowntext

import android.content.Context
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.TextViewCompat
import coil.ImageLoader
import com.ldlywt.note.utils.TopicUtils
import io.noties.markwon.Markwon

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    linkColor: Color = Color.Unspecified,
    truncateOnTextOverflow: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    isTextSelectable: Boolean = false,
    autoSizeConfig: AutoSizeConfig? = null,
    @FontRes fontResource: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    @IdRes viewId: Int? = null,
    onClick: (() -> Unit)? = null,
    // this option will disable all clicks on links, inside the markdown text
    // it also enable the parent view to receive the click event
    disableLinkMovementMethod: Boolean = false,
    imageLoader: ImageLoader? = null,
    autoLink: Boolean = true,
    linkifyMask: Int = if (autoLink) Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS or Linkify.WEB_URLS else 0,
    enableSoftBreakAddsNewLine: Boolean = true,
    onLinkClicked: ((String) -> Unit)? = null,
    onTextLayout: ((numLines: Int) -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null,
    highlightText: String = "",
) {
    val defaultColor: Color = LocalContentColor.current
    val context: Context = LocalContext.current
    val markdownRender: Markwon =
        remember(linkifyMask) {
            MarkdownRender.create(
                context,
                imageLoader,
                linkifyMask,
                enableSoftBreakAddsNewLine,
                onLinkClicked
            )
        }

    val androidViewModifier = if (onClick != null) {
        Modifier
            .clickable { onClick() }
            .then(modifier)
    } else {
        modifier
    }
    AndroidView(
        modifier = androidViewModifier,
        factory = { factoryContext ->

            val linkTextColor = linkColor.takeOrElse { style.color.takeOrElse { defaultColor } }

            CustomTextView(factoryContext).apply {
                viewId?.let { id = viewId }
                fontResource?.let { font -> applyFontResource(font) }

                setMaxLines(maxLines)
                setLinkTextColor(linkTextColor.toArgb())

                setTextIsSelectable(isTextSelectable)

                if (!isTextSelectable) {
                    movementMethod = LinkMovementMethod.getInstance()
                }

                if (truncateOnTextOverflow) enableTextOverflow()

                autoSizeConfig?.let { config ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                            this,
                            config.autoSizeMinTextSize,
                            config.autoSizeMaxTextSize,
                            config.autoSizeStepGranularity,
                            config.unit
                        )
                    }
                }
            }
        },
        update = { textView ->
            with(textView) {
                applyTextColor(style.color.takeOrElse { defaultColor }.toArgb())
                applyFontSize(style)
                applyLineHeight(style)
                applyTextDecoration(style)

                with(style) {
                    applyTextAlign(textAlign)
                    fontStyle?.let { applyFontStyle(it) }
                    fontWeight?.let { applyFontWeight(it) }
                    fontFamily?.let { applyFontFamily(it) }
                }
            }
            markdownRender.setMarkdown(textView, markdown)
            if (disableLinkMovementMethod) {
                textView.movementMethod = null
            } else if (isTextSelectable) {
                // When selectable, we don't want LinkMovementMethod as it conflicts with selection
                if (textView.movementMethod is LinkMovementMethod) {
                    textView.movementMethod = null
                    textView.setTextIsSelectable(true)
                }
            }

            if (onTextLayout != null) {
                textView.post {
                    onTextLayout(textView.lineCount)
                }
            }
            textView.maxLines = maxLines
            textView.highlightTagsWithClick(onTagClick, isTextSelectable)
            textView.highlightSearchText(highlightText)
        }
    )
}

// TextView 扩展方法，传递回调函数处理点击事件
fun TextView.highlightTagsWithClick(onTagClick: ((String) -> Unit)?, isTextSelectable: Boolean) {
    val originalText = this.text.toString()
    
    // 使用新的正则逻辑获取需要高亮的标签
    val matcher = TopicUtils.pattern.matcher(originalText)
    val foundTags = mutableListOf<Pair<Int, Int>>()
    while (matcher.find()) {
        foundTags.add(Pair(matcher.start(), matcher.end()))
    }

    if (foundTags.isEmpty()) return

    // 创建 SpannableString 用于设置文字的不同样式
    val spannableString = SpannableString(this.text)
    val textColor = android.graphics.Color.parseColor("#4D84F7")

    // 遍历匹配到的标签索引
    for (tagRange in foundTags) {
        val startIndex = tagRange.first
        val endIndex = tagRange.second
        val tagText = originalText.substring(startIndex, endIndex)

        // 设置文字颜色为蓝色
        spannableString.setSpan(
            ForegroundColorSpan(textColor),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 设置点击事件，并重写 updateDrawState 去掉下划线
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                // 触发回调，将当前点击的完整 tag 传递出去 (包含 /)
                onTagClick?.invoke(tagText)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = textColor
                ds.isUnderlineText = false
            }
        }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // 将设置好的 SpannableString 应用到 TextView
    this.text = spannableString

    // 使 TextView 支持点击
    if (!isTextSelectable) {
        this.movementMethod = LinkMovementMethod.getInstance()
        this.highlightColor = android.graphics.Color.TRANSPARENT
    }
}

fun TextView.highlightSearchText(highlightText: String) {
    if (highlightText.isEmpty()) return
    val spannable = text as? Spannable ?: return
    val content = spannable.toString()
    var start = 0
    while (start < content.length) {
        val index = content.indexOf(highlightText, start, ignoreCase = true)
        if (index == -1) break
        spannable.setSpan(
            BackgroundColorSpan(android.graphics.Color.parseColor("#FFD700")), // 金色/黄色高亮
            index,
            index + highlightText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        start = index + highlightText.length
    }
}
