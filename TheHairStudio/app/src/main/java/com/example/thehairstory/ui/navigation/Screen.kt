package com.example.thehairstory.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Register : Screen("register")
    
    object Detail : Screen("detail/{memberId}?isCustomerMode={isCustomerMode}") {
        fun createRoute(memberId: Int, isCustomerMode: Boolean = false) = 
            "detail/$memberId?isCustomerMode=$isCustomerMode"
    }
    
    object AddTransaction : Screen("add_transaction/{memberId}") {
        fun createRoute(memberId: Int) = "add_transaction/$memberId"
    }
}
