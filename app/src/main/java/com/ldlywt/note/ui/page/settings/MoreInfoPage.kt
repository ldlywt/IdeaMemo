package com.ldlywt.note.ui.page.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.ui.page.router.debouncedPopBackStack
import com.ldlywt.note.utils.Constant
import com.ldlywt.note.utils.DonateUtils
import com.ldlywt.note.utils.openMail
import com.ldlywt.note.utils.openUrl
import com.ldlywt.note.utils.shareApp
import com.ldlywt.note.utils.str
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.dialog.BasicDialog

@Composable
fun ContactDialog(block: () -> Unit) {
    BasicDialog(onDismissRequest = { }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SaltTheme.dimens.outerHorizontalPadding,
                    vertical = SaltTheme.dimens.outerVerticalPadding
                )
        ) {
            Text(
                text = stringResource(R.string.contact),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SaltTheme.colors.text
            )
            Spacer(modifier = Modifier.height(SaltTheme.dimens.innerVerticalPadding))
            AboutComposeScreen()
            Spacer(modifier = Modifier.height(SaltTheme.dimens.innerVerticalPadding))
            com.moriafly.salt.ui.TextButton(
                onClick = block,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.ok)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, UnstableSaltApi::class)
@Composable
fun MoreInfoPage(
    navController: NavHostController
) {
    val context = LocalContext.current

    val settingList = listOf(
        SettingsBean(R.string.share_app, Icons.Outlined.Share) { shareApp(context) },
        SettingsBean(R.string.five_star_review, Icons.Outlined.Star) {
            DonateUtils.openGooglePlay(
                context
            )
        },
    )
    val aboutList = listOf(
        SettingsBean(R.string.user_agree, Icons.AutoMirrored.Outlined.Assignment) {
            context.openUrl(Constant.USER_AGREEMENT)
        },
        SettingsBean(R.string.privacy_policy, Icons.Outlined.PrivacyTip) {
            context.openUrl(Constant.PRIVACY_POLICY)
        },
        SettingsBean(R.string.github, Icons.Outlined.Code) {
            context.openUrl(Constant.GITHUB_URL)
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
                navController.debouncedPopBackStack()
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
        }
    }
}