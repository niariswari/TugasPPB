package com.example.thehairstory.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.thehairstory.data.repository.MembershipRepository
import com.example.thehairstory.ui.ViewModelFactory
import com.example.thehairstory.ui.screens.splash.SplashScreen
import com.example.thehairstory.ui.screens.home.HomeScreen
import com.example.thehairstory.ui.screens.home.HomeViewModel
import com.example.thehairstory.ui.screens.register.RegisterScreen
import com.example.thehairstory.ui.screens.register.RegisterViewModel
import com.example.thehairstory.ui.screens.detail.MemberDetailScreen
import com.example.thehairstory.ui.screens.detail.MemberDetailViewModel
import com.example.thehairstory.ui.screens.transaction.AddTransactionScreen
import com.example.thehairstory.ui.screens.transaction.AddTransactionViewModel
import com.example.thehairstory.ui.screens.login.LoginScreen
import com.example.thehairstory.ui.screens.login.LoginViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    repository: MembershipRepository
) {
    val factory = ViewModelFactory(repository)

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(400)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(400)
            )
        }
    ) {
        // Splash Route (Instant load, no transition)
        composable(
            route = Screen.Splash.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Route
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { isStaff, memberId ->
                    if (isStaff) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else if (memberId != null) {
                        navController.navigate(Screen.Detail.createRoute(memberId, isCustomerMode = true)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Home Route
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToDetail = { memberId ->
                    navController.navigate(Screen.Detail.createRoute(memberId, isCustomerMode = false))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Register Route
        composable(Screen.Register.route) {
            val registerViewModel: RegisterViewModel = viewModel(factory = factory)
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Detail Route
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("memberId") { type = NavType.IntType },
                navArgument("isCustomerMode") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getInt("memberId") ?: 0
            val isCustomerMode = backStackEntry.arguments?.getBoolean("isCustomerMode") ?: false
            val detailViewModel: MemberDetailViewModel = viewModel(factory = factory)
            MemberDetailScreen(
                memberId = memberId,
                viewModel = detailViewModel,
                isCustomerMode = isCustomerMode,
                onNavigateBack = {
                    if (isCustomerMode) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigateUp()
                    }
                },
                onNavigateToAddTransaction = { id ->
                    navController.navigate(Screen.AddTransaction.createRoute(id))
                }
            )
        }

        // Add Transaction Route
        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getInt("memberId") ?: 0
            val transactionViewModel: AddTransactionViewModel = viewModel(factory = factory)
            AddTransactionScreen(
                memberId = memberId,
                viewModel = transactionViewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}
