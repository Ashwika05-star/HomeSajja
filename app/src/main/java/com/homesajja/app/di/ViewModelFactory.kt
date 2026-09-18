package com.homesajja.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.homesajja.app.viewmodel.HomeViewModel
import com.homesajja.app.viewmodel.LoginViewModel
import com.homesajja.app.viewmodel.SignupViewModel
import com.homesajja.app.viewmodel.SplashViewModel

/** Manual ViewModel factory matching [AppContainer]'s manual DI — see its
 * doc comment for why this project doesn't use Hilt. */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            SplashViewModel::class.java -> SplashViewModel(
                container.authRepository,
                container.sessionRepository,
            )
            SignupViewModel::class.java -> SignupViewModel(
                container.authRepository,
                container.userRepository,
                container.vendorRepository,
                container.sessionRepository,
            )
            LoginViewModel::class.java -> LoginViewModel(
                container.authRepository,
                container.userRepository,
                container.vendorRepository,
                container.sessionRepository,
            )
            HomeViewModel::class.java -> HomeViewModel(
                container.authRepository,
                container.sessionRepository,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
