package mg.etat.madaalerte.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mg.etat.madaalerte.R
import mg.etat.madaalerte.ui.theme.MalagasyDark
import mg.etat.madaalerte.ui.theme.MalagasyGreen
import mg.etat.madaalerte.ui.theme.MalagasyRed
import mg.etat.madaalerte.ui.theme.MalagasySurface
import mg.etat.madaalerte.viewmodel.AlerteViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

fun sauvegarderPreuve(context: Context, bitmap: Bitmap): String {
    val nomFichier = "preuve_${System.currentTimeMillis()}.jpg"
    val fichier = File(context.filesDir, nomFichier)
    FileOutputStream(fichier).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }
    return fichier.absolutePath
}

fun genererPhotoTest(context: Context): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(AndroidColor.DKGRAY)
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlerteScreen(viewModel: AlerteViewModel, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- VARIABLES POUR LA CATÉGORIE INTELLIGENTE ---
    val categories by viewModel.categories.collectAsState()
    val langueCourante = Locale.getDefault().language
    var categorieSelectionnee by remember { mutableStateOf<mg.etat.madaalerte.data.CategorieAlerte?>(null) }
    var expandedCat by remember { mutableStateOf(false) }
    val categorieBaseDeDonnees = categorieSelectionnee?.nomFr ?: "" // Enregistre toujours le nom FR pour les stats

    var description by remember { mutableStateOf("") }

    var quartierManuel by remember { mutableStateOf("") }
    var coordonneesGps by remember { mutableStateOf("") }
    var expandedQuartier by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) } // POPUP GPS

    val quartiersTana = listOf("Analakely", "Ivandry", "Ankorondrano", "67ha", "Tsaralalàna", "Andohalo", "Ilafy", "Ambohijatovo", "Anosy")

    var niveauDanger by remember { mutableStateOf(3f) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun obtenirAdresseQuartier(lat: Double, lon: Double) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                withContext(Dispatchers.Main) {
                    coordonneesGps = "[$lat, $lon]"
                    if (!addresses.isNullOrEmpty()) {
                        val adresse = addresses[0]
                        val quartier = adresse.subLocality ?: adresse.locality ?: ""
                        if (quartier.isNotBlank()) quartierManuel = quartier
                    }
                    isFetchingLocation = false // FIN DU CHARGEMENT
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    coordonneesGps = "[$lat, $lon]"
                    isFetchingLocation = false // FIN DU CHARGEMENT MÊME SI ERREUR
                }
            }
        }
    }

    fun capturerPositionGPS() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isFetchingLocation = true // DÉBUT DU CHARGEMENT
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        obtenirAdresseQuartier(location.latitude, location.longitude)
                    } else {
                        isFetchingLocation = false // ANNULER SI NULL
                    }
                }.addOnFailureListener {
                    isFetchingLocation = false // ANNULER SI ÉCHEC
                }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) capturerPositionGPS()
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBitmap = bitmap
            photoUri = sauvegarderPreuve(context, bitmap)
        }
    }

    val citoyenActuel = viewModel.citoyenConnecte

    // POPUP DE CHARGEMENT GPS
    if (isFetchingLocation) {
        AlertDialog(
            onDismissRequest = { /* Empêche l'annulation en cliquant à côté */ },
            confirmButton = {},
            title = null,
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    CircularProgressIndicator(color = MalagasyGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Veuillez patienter...", fontWeight = FontWeight.Bold, color = MalagasyDark)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(id = R.string.nouveau_signalement), fontWeight = FontWeight.Bold)
                        if (citoyenActuel != null) {
                            Text(text = "${citoyenActuel.nomPrenom}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White) }
                },
                actions = {
                    TextButton(onClick = {
                        val courante = Locale.getDefault().language
                        val nouvelle = when (courante) { "fr" -> "mg"; "mg" -> "en"; else -> "fr" }
                        val prefs = context.getSharedPreferences("prefs_mada", Context.MODE_PRIVATE)
                        prefs.edit().putString("selected_lang", nouvelle).apply()
                        val locale = Locale(nouvelle)
                        Locale.setDefault(locale)
                        // CORRECTION ICI : Spécification exacte de la classe Configuration
                        val config = android.content.res.Configuration(context.resources.configuration)
                        config.setLocale(locale)
                        @Suppress("DEPRECATION")
                        context.resources.updateConfiguration(config, context.resources.displayMetrics)
                        (context as? android.app.Activity)?.recreate()
                    }) {
                        Icon(Icons.Default.Language, contentDescription = "Langue", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(when (Locale.getDefault().language) { "mg" -> "MG"; "en" -> "EN"; else -> "FR" }, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MalagasyRed, titleContentColor = Color.White)
            )
        }
    ){ paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:117"))) },
                modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MalagasyRed)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Appeler", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.appel_urgence), color = Color.White, fontWeight = FontWeight.Bold)
            }

            // --- MENU DÉROULANT DES CATÉGORIES (CRUD INTELLIGENT) ---
            @Suppress("DEPRECATION")
            ExposedDropdownMenuBox(
                expanded = expandedCat,
                onExpandedChange = { expandedCat = !expandedCat }
            ) {
                OutlinedTextField(
                    value = categorieSelectionnee?.getNom(langueCourante) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.type_probleme)) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = MalagasyGreen)
                )
                ExposedDropdownMenu(
                    expanded = expandedCat,
                    onDismissRequest = { expandedCat = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.getNom(langueCourante)) },
                            onClick = {
                                categorieSelectionnee = cat
                                expandedCat = false
                            }
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MalagasySurface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.localisation_site), fontWeight = FontWeight.Bold, color = MalagasyDark)
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                capturerPositionGPS()
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = MalagasyGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    @Suppress("DEPRECATION")
                    ExposedDropdownMenuBox(expanded = expandedQuartier, onExpandedChange = { expandedQuartier = !expandedQuartier }) {
                        OutlinedTextField(
                            value = quartierManuel, onValueChange = { quartierManuel = it },
                            label = { Text(stringResource(id = R.string.saisir_quartier)) }, modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandedQuartier, onDismissRequest = { expandedQuartier = false }) {
                            quartiersTana.filter { it.contains(quartierManuel, ignoreCase = true) }.forEach { selectionOption ->
                                DropdownMenuItem(text = { Text(selectionOption) }, onClick = { quartierManuel = selectionOption; expandedQuartier = false })
                            }
                        }
                    }

                    if (coordonneesGps.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = MalagasyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(coordonneesGps, color = MalagasyGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text(stringResource(id = R.string.description_detaillee)) }, modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 4
            )

            Text("${stringResource(id = R.string.niveau_gravite)} : ${niveauDanger.roundToInt()} / 5", fontWeight = FontWeight.Bold)
            Slider(value = niveauDanger, onValueChange = { niveauDanger = it }, valueRange = 1f..5f, steps = 3, colors = SliderDefaults.colors(thumbColor = MalagasyRed, activeTrackColor = MalagasyRed))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MalagasyDark)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Caméra")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.photo_preuve), fontSize = MaterialTheme.typography.labelLarge.fontSize)
                }
            }

            photoBitmap?.let { bitmap -> Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Preuve", modifier = Modifier.fillMaxWidth().height(130.dp)) }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOUTON DE SOUMISSION REMIS COMME AVANT ---
            Button(
                onClick = {
                    if (categorieBaseDeDonnees.isNotBlank() && quartierManuel.isNotBlank() && photoUri != null) {
                        viewModel.signalerProbleme(categorieBaseDeDonnees, description, coordonneesGps, quartierManuel, niveauDanger.roundToInt(), photoUri)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                enabled = categorieBaseDeDonnees.isNotBlank() && quartierManuel.isNotBlank() && photoUri != null, // Désactivé si un champ manque
                colors = ButtonDefaults.buttonColors(containerColor = MalagasyGreen)
            ) { Text(stringResource(id = R.string.transmettre_etat), fontWeight = FontWeight.Bold, color = Color.White) }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}