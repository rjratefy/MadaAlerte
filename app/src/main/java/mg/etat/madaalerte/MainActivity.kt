package mg.etat.madaalerte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mg.etat.madaalerte.ui.AddAlerteScreen
import mg.etat.madaalerte.ui.HomeScreen
import mg.etat.madaalerte.ui.LoginScreen
import mg.etat.madaalerte.ui.theme.MadaAlerteTheme
import mg.etat.madaalerte.viewmodel.AlerteViewModel
import mg.etat.madaalerte.viewmodel.AlerteViewModelFactory
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Charger la langue sauvegardée, ou "mg" (Malagasy) par défaut au premier lancement
        val prefs = getSharedPreferences("prefs_mada", MODE_PRIVATE)
        val lang = prefs.getString("selected_lang", "mg") ?: "mg"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MadaAlerteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val application = application as MadaAlerteApp
                    val viewModel: AlerteViewModel = viewModel(
                        factory = AlerteViewModelFactory(application.repository)
                    )

                    val navController = rememberNavController()
                    var isAdminState by rememberSaveable { mutableStateOf(false) }

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                viewModel = viewModel,
                                setAdminMode = { estAdmin ->
                                    isAdminState = estAdmin
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                navController = navController,
                                isAdmin = isAdminState
                            )
                        }

                        composable("add_alerte") {
                            AddAlerteScreen(viewModel = viewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}