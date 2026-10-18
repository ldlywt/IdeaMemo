package com.ldlywt.note.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ldlywt.note.R
import com.ldlywt.note.utils.Constant
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.UnstableSaltApi

@OptIn(UnstableSaltApi::class)
@Composable
fun WelcomePage(onAgree: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
            .statusBarsPadding()
            .padding(horizontal = 32.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Header
        Text(
            text = stringResource(R.string.welcome),
            style = SaltTheme.textStyles.main.copy(
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 52.sp
            ),
            color = SaltTheme.colors.text
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "IdeaMemo",
            style = SaltTheme.textStyles.main.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = SaltTheme.colors.highlight
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        // Features
        WelcomeFeatureItem(
            title = stringResource(R.string.welcome_pure_title),
            desc = stringResource(R.string.welcome_pure_desc),
            icon = Icons.Outlined.AutoAwesome
        )
        Spacer(modifier = Modifier.height(28.dp))
        WelcomeFeatureItem(
            title = stringResource(R.string.welcome_card_title),
            desc = stringResource(R.string.welcome_card_desc),
            icon = Icons.Outlined.Style
        )
        Spacer(modifier = Modifier.height(28.dp))
        WelcomeFeatureItem(
            title = stringResource(R.string.welcome_privacy_title),
            desc = stringResource(R.string.welcome_privacy_desc),
            icon = Icons.Outlined.Security
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Usage Guide Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SaltTheme.colors.subBackground)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = SaltTheme.colors.highlight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.warm_reminder),
                    style = SaltTheme.textStyles.main.copy(fontWeight = FontWeight.Bold),
                    color = SaltTheme.colors.text
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.warm_reminder_desc),
                style = SaltTheme.textStyles.sub.copy(fontSize = 14.sp, lineHeight = 24.sp),
                color = SaltTheme.colors.subText
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Legal Section
        val tosText = stringResource(R.string.browse_tos_tips_service)
        val privacyText = stringResource(R.string.browse_tos_tips_privacy)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = tosText,
                style = SaltTheme.textStyles.sub.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                color = SaltTheme.colors.highlight,
                modifier = Modifier.clickable { Constant.startUserAgreeUrl(context) }
            )
            Text(
                text = " & ",
                style = SaltTheme.textStyles.sub.copy(fontSize = 12.sp),
                color = SaltTheme.colors.subText,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Text(
                text = privacyText,
                style = SaltTheme.textStyles.sub.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                color = SaltTheme.colors.highlight,
                modifier = Modifier.clickable { Constant.startPrivacyUrl(context) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Button
        TextButton(
            onClick = onAgree,
            text = stringResource(R.string.agree),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun WelcomeFeatureItem(title: String, desc: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp).padding(top = 2.dp),
            tint = SaltTheme.colors.highlight
        )
        Spacer(modifier = Modifier.size(18.dp))
        Column {
            Text(
                text = title,
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = SaltTheme.colors.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = SaltTheme.textStyles.sub.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = SaltTheme.colors.subText
            )
        }
    }
}
