package com.swappy.picora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swappy.picora.ui.theme.PicoraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            PicoraTheme {
                val context = LocalContext.current
                val database = (context.applicationContext as PicoraApplication).database
                val appointmentViewModel: AppointmentViewModel = viewModel(
                    factory = AppointmentViewModelFactory(
                        database.appointmentDao(),
                        database.guestDao(),
                        database.photographerDao(),
                        database.occasionDao(),
                        database.timeSlotDao()
                    )
                )
                MainScreen(appointmentViewModel)
            }
        }
    }
}

sealed class Screen(val label: String, val icon: ImageVector) {
    object Main : Screen("Main", Icons.Default.Home)
    object Appointments : Screen("Appointments", Icons.Default.DateRange)
}

val items = listOf(
    Screen.Main,
    Screen.Appointments
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(appointmentViewModel: AppointmentViewModel) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> MainTabContent(appointmentViewModel)
                1 -> AppointmentsTabContent(appointmentViewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PicoraTheme {
        MainScreen(appointmentViewModel = viewModel())
    }
}
