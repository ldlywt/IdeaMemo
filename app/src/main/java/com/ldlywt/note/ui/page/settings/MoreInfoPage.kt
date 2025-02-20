package com.ldlywt.note.ui.page.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.utils.Constant
import com.ldlywt.note.utils.DonateUtils
import com.ldlywt.note.utils.openMail
import com.ldlywt.note.utils.shareApp
import com.ldlywt.note.utils.str
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi


@Composable
fun ContactDialog(block: () -> Unit) {
    AlertDialog(
        containerColor = SaltTheme.colors.background,
        onDismissRequest = { },
        title = { androidx.compose.material3.Text(stringResource(R.string.contact), color = SaltTheme.colors.text) },
        text = { AboutComposeScreen() },
        confirmButton = {
            Button(onClick = {
                block()
            }) {
                Text("OK", color = Color.White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, UnstableSaltApi::class)
@Composable
fun MoreInfoPage(
    navController: NavHostController
) {
    val context = LocalContext.current
    val settingList = listOf(
        SettingsBean(R.string.share_app, Icons.Outlined.Share) { shareApp(context) },
        SettingsBean(R.string.five_star_review, Icons.Outlined.Star) { DonateUtils.openGooglePlay(context) },
        SettingsBean(R.string.feedback, Icons.Outlined.Feedback) { openMail(context) },
        SettingsBean(R.string.donate_app, Icons.Outlined.LocalCafe) { DonateUtils.openALiPay(context) },
    )
    val aboutList = listOf(
        SettingsBean(R.string.user_agree, Icons.AutoMirrored.Outlined.Assignment) {
            Constant.startUserAgreeUrl(context)
        },
        SettingsBean(R.string.privacy_policy, Icons.Outlined.PrivacyTip) {
            Constant.startPrivacyUrl(context)
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        TitleBar(
            onBack = {
                navController.popBackStack()
            },
            text = R.string.other.str
        )

        Column(
            modifier = Modifier
                .background(color = SaltTheme.colors.background)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.mipmap.ic_launcher,
                    null
                )?.let {
                    Image(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        bitmap = (it).toBitmap().asImageBitmap(),
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        id = R.string.app_name
                    ), fontSize = 19.sp, color = SaltTheme.colors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.packageManager.getPackageInfo(
                        context.packageName,
                        0
                    ).versionName!!, color = SaltTheme.colors.text
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }


        RoundedColumn {
            settingList.forEachIndexed { index, it ->
                Item(
                    onClick = {
                        it.onClick()
                    },
                    text = it.title.str,
                    iconPainter = rememberVectorPainter(it.imageVector),
                )
            }
        }
        RoundedColumn {
            var yesNoDialog by remember { mutableStateOf(false) }
            if (yesNoDialog) {
                ContactDialog {
                    yesNoDialog = false
                }
            }
            aboutList.forEachIndexed { index, it ->
                Item(
                    onClick = {
                        it.onClick()
                    },
                    text = it.title.str,
                    iconPainter = rememberVectorPainter(it.imageVector),
                )
            }

            Item(
                onClick = {
                    yesNoDialog = true
                },
                text = R.string.contact.str,
                iconPainter = rememberVectorPainter(Icons.Outlined.Contactless),
            )
        }

    }
}
