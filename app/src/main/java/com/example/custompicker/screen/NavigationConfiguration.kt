package com.example.custompicker.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.custompicker.MainContract
import com.example.custompicker.MainViewModel
import com.example.custompicker.screen.inital.InitializeScreen

@Composable
fun NavigationConfiguration(
    modifier: Modifier = Modifier,
    hasStoragePermission: Boolean,
    onInitializeClick: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val startDestination =
        if (hasStoragePermission) {
            NavigationRoute.MEDIA_TAB
        } else {
            NavigationRoute.INITIALIZE
        }

    LaunchedEffect(hasStoragePermission, navBackStackEntry?.destination?.route) {
        when {
            hasStoragePermission && navBackStackEntry?.destination?.route == NavigationRoute.INITIALIZE -> {
                navController.navigate(NavigationRoute.MEDIA_TAB) {
                    popUpTo(NavigationRoute.INITIALIZE) {
                        inclusive = true
                    }
                }
            }

            !hasStoragePermission && navBackStackEntry?.destination?.route == NavigationRoute.MEDIA_TAB -> {
                navController.navigate(NavigationRoute.INITIALIZE) {
                    popUpTo(NavigationRoute.MEDIA_TAB) {
                        inclusive = true
                    }
                }
            }
        }
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
                },
            )
        }

        composable(route = NavigationRoute.MEDIA_TAB) {
            val viewModel: MainViewModel = hiltViewModel()
            val uiState = viewModel.viewState.collectAsState().value
            MainTabScreen(
                title = uiState.selectedDirectoryName,
                selectedTabIndex = uiState.selectedTabIndex,
                directoryList = uiState.directoryList,
                mediaList = uiState.mediaList,
                currentSortingType = uiState.sortingType,
                onInitialize = {
                    viewModel.setEvent(MainContract.Event.Initialize)
                },
                onTabSelected = { tabIndex ->
                    viewModel.setEvent(MainContract.Event.OnTabSelected(tabIndex))
                },
                onDirectorySelected = { directory ->
                    viewModel.setEvent(MainContract.Event.OnDirectorySelected(directory))
                },
                onMediaOptionsSaved = { sortingType ->
                    viewModel.setEvent(MainContract.Event.OnSortingTypeChanged(sortingType))
                },
            )
        }
    }
}
