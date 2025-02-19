package com.ldlywt.note.ui.page.router

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ldlywt.note.ui.page.PictureDisplayPage
import com.ldlywt.note.ui.page.search.SearchPage
import com.ldlywt.note.ui.page.StartupPage
import com.ldlywt.note.ui.page.data.DataCloudConfigPage
import com.ldlywt.note.ui.page.data.DataManagerPage
import com.ldlywt.note.ui.page.input.MemoInputPage
import com.ldlywt.note.ui.page.main.MainScreen
import com.ldlywt.note.ui.page.settings.ExplorePage
import com.ldlywt.note.ui.page.settings.GalleryPage
import com.ldlywt.note.ui.page.settings.MoreInfoPage
import com.ldlywt.note.ui.page.share.SharePage
import com.ldlywt.note.ui.page.tag.LocationDetailPage
import com.ldlywt.note.ui.page.tag.LocationListPage
import com.ldlywt.note.ui.page.tag.TagDetailPage
import com.ldlywt.note.ui.page.tag.TagListPage
import com.ldlywt.note.ui.page.tag.YearDetailPage
import com.ldlywt.note.utils.SettingsPreferences
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.lightSaltColors
import com.moriafly.salt.ui.saltColorsByColorScheme
import com.moriafly.salt.ui.saltConfigs

@SuppressLint("UnrememberedMutableState")
@OptIn(UnstableSaltApi::class)
@Composable
fun App() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val themeModeState by SettingsPreferences.themeMode.collectAsState(SettingsPreferences.ThemeMode.SYSTEM)
    val dynamicColor by SettingsPreferences.dynamicColor.collectAsState(false)

    val darkTheme = when (themeModeState) {
        SettingsPreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        SettingsPreferences.ThemeMode.DARK -> true
        else -> false
    }

    val colors = when (themeModeState) {
        SettingsPreferences.ThemeMode.LIGHT -> if (dynamicColor) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            saltColorsByColorScheme(
                dynamicLightColorScheme(context)
            )
        } else {
            TODO("VERSION.SDK_INT < S")
        } else lightSaltColors()

        SettingsPreferences.ThemeMode.DARK -> if (dynamicColor) saltColorsByColorScheme(dynamicDarkColorScheme(context)) else darkSaltColors()

        SettingsPreferences.ThemeMode.SYSTEM -> {
            if (isSystemInDarkTheme())
                if (dynamicColor) saltColorsByColorScheme(
                    dynamicDarkColorScheme(context)
                ) else darkSaltColors()
            else
                if (dynamicColor) saltColorsByColorScheme(
                    dynamicLightColorScheme(context)
                ) else lightSaltColors()
        }
    }


    CompositionLocalProvider(LocalRootNavController provides navController) {
        SaltTheme(
            colors = colors,
            configs = saltConfigs(isDarkTheme = darkTheme),
        ) {
            NavHostContainer(navController = navController)
        }
    }
}

val LocalRootNavController = compositionLocalOf<NavHostController> { error("Not find") }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavHostContainer(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val firstLaunch by SettingsPreferences.firstLaunch.collectAsState(false)
    NavHost(
        navController,
        startDestination = if (firstLaunch) Screen.Startup else Screen.Main,
    ) {
        composable<Screen.Startup>{
            StartupPage(navHostController = navController)
        }
        composable<Screen.Explore> {
            ExplorePage(navHostController = navController)
        }
        composable<Screen.TagList> {
            TagListPage(navController = navController)
        }

        composable<Screen.Main> {
            MainScreen(navController = navController)
        }

        composable<Screen.RandomWalk> {
            ExplorePage(navHostController = navController)
        }
        composable<Screen.Gallery> {
            GalleryPage(navHostController = navController)
        }
        composable<Screen.Search> {
            SearchPage(navController = navController)
        }
        composable<Screen.DataManager> {
            DataManagerPage(navController = navController)
        }
        composable<Screen.DataCloudConfig> {
            DataCloudConfigPage(navController = navController)
        }
        composable<Screen.LocationList> {
            LocationListPage(navHostController = navController)
        }
        composable<Screen.MoreInfo> {
            MoreInfoPage(navController = navController)
        }

        composable<Screen.TagDetail> { navBackStackEntry ->
            val args = navBackStackEntry.toRoute<Screen.TagDetail>()
            TagDetailPage(tag = args.tag, navController = navController)
        }

        composable<Screen.YearDetail> { navBackStackEntry ->
            val args = navBackStackEntry.toRoute<Screen.YearDetail>()
            YearDetailPage(year = args.year, navController = navController)
        }

        composable<Screen.LocationDetail> { navBackStackEntry ->
            val args = navBackStackEntry.toRoute<Screen.LocationDetail>()
            LocationDetailPage(location = args.location, navController = navController)
        }

        composable<Screen.PictureDisplay> { navBackStackEntry ->
            val args = navBackStackEntry.toRoute<Screen.PictureDisplay>()
            PictureDisplayPage(path = args.path, onBack = { navController.popBackStack() })
        }

        composable<Screen.InputDetail> { navBackStackEntry ->
            val args = navBackStackEntry.toRoute<Screen.InputDetail>()
            MemoInputPage(args.id)
        }

        composable<Screen.Share> { navBackStackEntry ->
            val args = navBackStackEntry.toRoute<Screen.Share>()
            SharePage(args.id, navController)
        }
    }
}