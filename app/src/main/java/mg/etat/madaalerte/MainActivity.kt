package mg.etat.madaalerte


import mg.etat.madaalerte.ui.AddAlerteScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mg.etat.madaalerte.ui.theme.MadaAlerteTheme
import mg.etat.madaalerte.viewmodel.AlerteViewModel
import mg.etat.madaalerte.viewmodel.AlerteViewModelFactory
import mg.etat.madaalerte.ui.HomeScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import mg.etat.madaalerte.ui.LoginScreen
// import mg.etat.madaalerte.ui.AddAlerteScreen (On va le créer juste après)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MadaAlerteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Initialisation propre du ViewModel avec notre Factory
                    val application = application as MadaAlerteApp
                    val viewModel: AlerteViewModel = viewModel(
                        factory = AlerteViewModelFactory(application.repository)
                    )

                    val navController = rememberNavController()

                    // Le routeur démarre désormais sur "login"
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(navController = navController)
                        }

                        // L'écran d'accueil reçoit le statut (citoyen ou admin)
                        composable(
                            route = "home/{isAdmin}",
                            arguments = listOf(navArgument("isAdmin") { type = NavType.BoolType })
                        ) { backStackEntry ->
                            val isAdmin = backStackEntry.arguments?.getBoolean("isAdmin") ?: false
                            HomeScreen(viewModel = viewModel, navController = navController, isAdmin = isAdmin)
                        }

                        composable("add_alerte") {
                            // Assure-toi que AddAlerteScreen est bien importé
                            mg.etat.madaalerte.ui.AddAlerteScreen(viewModel = viewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}