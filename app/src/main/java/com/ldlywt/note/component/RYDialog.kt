package com.ldlywt.note.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ldlywt.note.R
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.dialog.BasicDialog

/**
 * 项目统一风格的弹框：基于 SaltUI [BasicDialog]，与设置页、欢迎页等保持一致。
 */
@Composable
fun RYDialog(
    modifier: Modifier = Modifier,
    visible: Boolean,
    properties: DialogProperties = DialogProperties(),
    onDismissRequest: () -> Unit = {},
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    if (!visible) return
    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SaltTheme.dimens.outerHorizontalPadding,
                    vertical = SaltTheme.dimens.outerVerticalPadding
                )
        ) {
            icon?.let {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { it() }
                Spacer(modifier = Modifier.height(SaltTheme.dimens.contentPadding))
            }
            title?.let {
                it()
                Spacer(modifier = Modifier.height(SaltTheme.dimens.innerVerticalPadding))
            }
            text?.let {
                it()
                Spacer(modifier = Modifier.height(SaltTheme.dimens.innerVerticalPadding))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaltTheme.dimens.contentPadding)
            ) {
                dismissButton?.let {
                    Box(Modifier.weight(1f)) { it() }
                }
                Box(Modifier.weight(1f)) { confirmButton() }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    visible: Boolean,
    title: String,
    content: String,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    RYDialog(
        visible = visible,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SaltTheme.colors.text
            )
        },
        text = {
            Text(
                text = content,
                style = SaltTheme.textStyles.paragraph,
                color = SaltTheme.colors.subText
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmRequest,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.ok)
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.cancel),
                textColor = SaltTheme.colors.subText,
                backgroundColor = SaltTheme.colors.subBackground
            )
        },
    )
}
