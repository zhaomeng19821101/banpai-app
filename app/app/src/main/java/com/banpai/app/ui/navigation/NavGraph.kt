package com.banpai.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.banpai.app.data.FRIENDS
import com.banpai.app.data.Friend
import com.banpai.app.ui.screens.AtlasScreen
import com.banpai.app.ui.screens.CameraScreen
import com.banpai.app.ui.screens.ChatScreen
import com.banpai.app.ui.screens.CheckInScreen
import com.banpai.app.ui.screens.DiaryScreen
import com.banpai.app.ui.screens.DressUpScreen
import com.banpai.app.ui.screens.ExploreScreen
import com.banpai.app.ui.screens.FriendChatScreen
import com.banpai.app.ui.screens.GardenScreen
import com.banpai.app.ui.screens.HomeScreen
import com.banpai.app.ui.screens.IntroScreen
import com.banpai.app.ui.screens.LoginScreen
import com.banpai.app.ui.screens.PartyScreen
import com.banpai.app.ui.screens.PostcardScreen
import com.banpai.app.ui.screens.ProfileScreen
import com.banpai.app.ui.screens.WatchfaceScreen
import com.banpai.app.ui.screens.WelcomeScreen
import com.banpai.app.ui.theme.BanpaiColors
import com.banpai.app.ui.theme.ThemeMode

/** 路由：欢迎 → 认识半拍 → 登录 → 首页（5 Tab）→ 各子屏。 */
object Routes {
    const val WELCOME = "welcome"
    const val INTRO = "intro"
    const val LOGIN = "login"
    const val HOME = "home"          // 陪伴
    const val EXPLORE = "explore"    // 探索
    const val CHECKIN = "checkin"    // 打卡（中间暖金凸起按钮）
    const val ATLAS = "atlas"        // 图鉴
    const val PROFILE = "profile"    // 我的
    const val DRESSUP = "dressup"
    const val CAMERA = "camera"
    const val WATCHFACE = "watchface"
    const val CHAT = "chat"
    const val PARTY = "party"
    const val FRIEND_CHAT = "friend_chat" // 1:1 好友聊天（好友名经 chattingWith 传）
    const val GARDEN = "garden"
    const val DIARY = "diary"
    const val POSTCARD = "postcard"
}

/** 底部 5 Tab：陪伴 / 探索 / 打卡 / 图鉴 / 我的。打卡为中间暖金凸起按钮。 */
private val MAIN_TABS = listOf(
    Routes.HOME to "陪伴",
    Routes.EXPLORE to "探索",
    Routes.CHECKIN to "打卡",
    Routes.ATLAS to "图鉴",
    Routes.PROFILE to "我的"
)

@Composable
fun BanpaiNavGraph(
    navController: NavHostController = rememberNavController(),
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in MAIN_TABS.map { it.first }
    var chattingWith by remember { mutableStateOf(Friend(1, "好友")) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color.Transparent) {
                    MAIN_TABS.forEach { (route, label) ->
                        val selected = currentRoute == route
                        if (route == Routes.CHECKIN) {
                            // 中间暖金凸起按钮
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Surface(
                                        shape = CircleShape,
                                        color = BanpaiColors.WarmGold,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .offset(y = (-12).dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Text("打卡", color = Color(0xFF3A2A12))
                                        }
                                    }
                                },
                                label = { Text("") }
                            )
                        } else {
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (route) {
                                            Routes.HOME -> Icons.Filled.Home
                                            Routes.EXPLORE -> Icons.Filled.Search
                                            Routes.ATLAS -> Icons.Filled.Star
                                            else -> Icons.Filled.Person
                                        },
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.WELCOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(onDone = { navController.navigate(Routes.INTRO) })
            }
            composable(Routes.INTRO) {
                IntroScreen(onDone = { navController.navigate(Routes.LOGIN) })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = { navController.navigate(Routes.HOME) },
                    onBrowseFirst = { navController.navigate(Routes.HOME) }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenExplore = { navController.navigate(Routes.EXPLORE) },
                    onOpenCheckIn = { navController.navigate(Routes.CHECKIN) },
                    onOpenAtlas = { navController.navigate(Routes.ATLAS) },
                    onOpenDressUp = { navController.navigate(Routes.DRESSUP) },
                    onOpenCamera = { navController.navigate(Routes.CAMERA) },
                    onOpenWatchface = { navController.navigate(Routes.WATCHFACE) },
                    onOpenChat = { navController.navigate(Routes.CHAT) },
                    onOpenParty = { navController.navigate(Routes.PARTY) },
                    onOpenGarden = { navController.navigate(Routes.GARDEN) },
                    onOpenDiary = { navController.navigate(Routes.DIARY) },
                    onOpenPostcard = { navController.navigate(Routes.POSTCARD) }
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onOpenCheckIn = { navController.navigate(Routes.CHECKIN) },
                    onOpenAtlas = { navController.navigate(Routes.ATLAS) }
                )
            }
            composable(Routes.CHECKIN) {
                CheckInScreen(onOpenCamera = { navController.navigate(Routes.CAMERA) })
            }
            composable(Routes.ATLAS) { AtlasScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenDressUp = { navController.navigate(Routes.DRESSUP) },
                    onOpenWatchface = { navController.navigate(Routes.WATCHFACE) },
                    onOpenDiary = { navController.navigate(Routes.DIARY) },
                    onOpenPostcard = { navController.navigate(Routes.POSTCARD) },
                    onThemeModeChange = onThemeModeChange
                )
            }
            composable(Routes.DRESSUP) { DressUpScreen() }
            composable(Routes.CAMERA) { CameraScreen() }
            composable(Routes.WATCHFACE) { WatchfaceScreen() }
            composable(Routes.CHAT) { ChatScreen() }
            composable(Routes.PARTY) {
                PartyScreen(onOpenFriendChat = { friend ->
                    chattingWith = friend
                    navController.navigate(Routes.FRIEND_CHAT)
                })
            }
            composable(Routes.FRIEND_CHAT) {
                FriendChatScreen(
                    friendId = chattingWith.id,
                    friendName = chattingWith.name,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.GARDEN) { GardenScreen() }
            composable(Routes.DIARY) { DiaryScreen() }
            composable(Routes.POSTCARD) { PostcardScreen() }
        }
    }
}
