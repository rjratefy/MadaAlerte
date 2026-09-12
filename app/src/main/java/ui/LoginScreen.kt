package mg.etat.madaalerte.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mg.etat.madaalerte.R
import mg.etat.madaalerte.ui.theme.MalagasyDark
import mg.etat.madaalerte.ui.theme.MalagasyGreen
import mg.etat.madaalerte.ui.theme.MalagasyRed
import mg.etat.madaalerte.ui.theme.MalagasySurface
import mg.etat.madaalerte.viewmodel.AlerteViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

// --- LA FONCTION MAGIQUE POUR CHANGER DE LANGUE ---
fun changerLangue(context: Context, codeLangue: String) {
    val prefs = context.getSharedPreferences("prefs_mada", Context.MODE_PRIVATE)
    prefs.edit().putString("selected_lang", codeLangue).apply()

    val locale = Locale(codeLangue)
    Locale.setDefault(locale)
    val config = context.resources.configuration
    config.setLocale(locale)
    @Suppress("DEPRECATION")
    context.resources.updateConfiguration(config, context.resources.displayMetrics)

    (context as? android.app.Activity)?.recreate()
}

fun sauvegarderCIN(context: Context, bitmap: Bitmap): String {
    val fichier = File(context.filesDir, "cin_${System.currentTimeMillis()}.jpg")
    FileOutputStream(fichier).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
    return fichier.absolutePath
}

enum class LoginMode { LOGIN_CITOYEN, REGISTER_CITOYEN, LOGIN_ADMIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: AlerteViewModel, setAdminMode: (Boolean) -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(LoginMode.LOGIN_CITOYEN) }
    var messageErreur by remember { mutableStateOf("") }

    // Détecter la langue actuelle pour l'affichage du bouton
    val codeLangueCourante = Locale.getDefault().language
    val texteBoutonLangue = when (codeLangueCourante) {
        "mg" -> "MG"
        "en" -> "EN"
        else -> "FR"
    }

    // Champs
    var telephone by remember { mutableStateOf("") }
    var nomPrenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var cinBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cinUri by remember { mutableStateOf<String?>(null) }
    var codeAdmin by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            cinBitmap = bitmap
            cinUri = sauvegarderCIN(context, bitmap)
        }
    }

    // BOX PRINCIPALE POUR PERMETTRE LE PLACEMENT EXACT EN HAUT À DROITE
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // LE BOUTON DE LANGUE (FIXÉ EN HAUT À DROITE)
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            TextButton(onClick = {
                val nouvelleLangue = when (codeLangueCourante) {
                    "fr" -> "mg"
                    "mg" -> "en"
                    else -> "fr"
                }
                changerLangue(context, nouvelleLangue)
            }) {
                Icon(Icons.Default.Language, contentDescription = "Changer la langue", tint = MalagasyDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text(texteBoutonLangue, color = MalagasyDark, fontWeight = FontWeight.Bold)
            }
        }

        // LE RESTE DE L'ÉCRAN CENTRÉ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(id = R.drawable.logo_mada), contentDescription = "Logo", modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(id = R.string.mada_alerte), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MalagasyDark)

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MalagasySurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.notice_utilisation),
                    modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MalagasyDark, textAlign = TextAlign.Center
                )
            }

            if (messageErreur.isNotBlank()) {
                Text(messageErreur, color = MalagasyRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp), textAlign = TextAlign.Center)
            }

            when (mode) {
                LoginMode.LOGIN_CITOYEN -> {
                    Text(stringResource(id = R.string.identification_citoyen), fontWeight = FontWeight.Bold, color = MalagasyGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = telephone, onValueChange = { telephone = it }, label = { Text(stringResource(id = R.string.numero_telephone)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.connecterCitoyen(telephone) { citoyen ->
                                if (citoyen == null) {
                                    messageErreur = "Numéro inconnu. Veuillez vous inscrire."
                                } else if (citoyen.statutValidation == "EN ATTENTE") {
                                    messageErreur = "Profil en cours de validation par l'État."
                                } else if (citoyen.statutValidation == "VALIDE") {
                                    setAdminMode(false)
                                    navController.navigate("home")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MalagasyGreen)
                    ) { Text(stringResource(id = R.string.acceder_service), color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { mode = LoginMode.REGISTER_CITOYEN; messageErreur = "" }) { Text("Pas de compte ? Créer un profil", color = MalagasyDark) }
                    TextButton(onClick = { mode = LoginMode.LOGIN_ADMIN; messageErreur = "" }) { Text(stringResource(id = R.string.agent_etat), color = MalagasyRed) }
                }

                LoginMode.REGISTER_CITOYEN -> {
                    Text("Nouveau Profil Citoyen", fontWeight = FontWeight.Bold, color = MalagasyGreen)
                    OutlinedTextField(value = nomPrenom, onValueChange = { nomPrenom = it }, label = { Text(stringResource(id = R.string.nom_prenom)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = telephone, onValueChange = { telephone = it }, label = { Text(stringResource(id = R.string.numero_telephone)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(id = R.string.adresse_email)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MalagasyDark)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Caméra")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (cinUri == null) stringResource(id = R.string.cin_obligatoire) else stringResource(id = R.string.cin_enregistree))
                    }

                    cinBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = "CIN", modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp)) }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (nomPrenom.isNotBlank() && telephone.isNotBlank() && cinUri != null) {
                                viewModel.inscrireCitoyen(nomPrenom, telephone, email, cinUri!!) {
                                    messageErreur = "Profil enregistré ! En attente de validation."
                                    mode = LoginMode.LOGIN_CITOYEN
                                }
                            } else {
                                messageErreur = "Veuillez remplir tous les champs et photographier votre CIN."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MalagasyGreen)
                    ) { Text("S'INSCRIRE", color = Color.White, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { mode = LoginMode.LOGIN_CITOYEN; messageErreur = "" }) { Text(stringResource(id = R.string.retour), color = MalagasyDark) }
                }

                LoginMode.LOGIN_ADMIN -> {
                    Text(stringResource(id = R.string.connexion_agent), fontWeight = FontWeight.Bold, color = MalagasyRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = codeAdmin, onValueChange = { codeAdmin = it }, label = { Text(stringResource(id = R.string.code_acces)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (codeAdmin == "1234") {
                                setAdminMode(true)
                                navController.navigate("home")
                            } else {
                                messageErreur = "Code incorrect."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MalagasyRed)
                    ) { Text(stringResource(id = R.string.connexion_securisee), color = Color.White, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { mode = LoginMode.LOGIN_CITOYEN; messageErreur = "" }) { Text(stringResource(id = R.string.retour), color = MalagasyDark) }
                }
            }
        }
    }
}