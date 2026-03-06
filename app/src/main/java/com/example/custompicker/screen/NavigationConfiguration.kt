package com.example.custompicker.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.custompicker.screen.inital.InitializeScreen

@Composable
fun NavigationConfiguration(
    modifier: Modifier = Modifier,
    hasStoragePermission: Boolean,
    onInitializeClick: () -> Unit,
) {
    val navController = rememberNavController()
    val startDestination =
        if (hasStoragePermission) {
            NavigationRoute.MEDIA_TAB
        } else {
            NavigationRoute.INITIALIZE
        }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(route = NavigationRoute.INITIALIZE) {
            InitializeScreen(
                onInitializeClick = {
                    onInitializeClick()
                    navController.navigate(NavigationRoute.MEDIA_TAB)
                },
            )
        }

        composable(route = NavigationRoute.MEDIA_TAB) {
            MediaTabScreen()
        }
    }
}
