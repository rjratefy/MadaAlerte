package mg.etat.madaalerte.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import mg.etat.madaalerte.viewmodel.AlerteViewModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

val VertOfficiel = Color(0xFF007A3D)

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

    var categorie by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var localisation by remember { mutableStateOf("") }
    var niveauDanger by remember { mutableStateOf(3f) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Permission & GPS sécurisé
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        localisation = "${location.latitude}, ${location.longitude}"
                    } else {
                        localisation = "-18.9136, 47.5251" // Secours Antananarivo
                    }
                }.addOnFailureListener {
                    localisation = "-18.9136, 47.5251"
                }
            } catch (e: SecurityException) {
                localisation = "-18.9136, 47.5251"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBitmap = bitmap
            photoUri = sauvegarderPreuve(context, bitmap)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau signalement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFCE1126),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = categorie,
                onValueChange = { categorie = it },
                label = { Text("Type de problème (ex: Lalan-dratsy)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = localisation,
                    onValueChange = { localisation = it },
                    label = { Text("Coordonnées ou Adresse") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                if (location != null) {
                                    localisation = "${location.latitude}, ${location.longitude}"
                                } else {
                                    localisation = "-18.9136, 47.5251" // Secours automatique
                                }
                            }.addOnFailureListener {
                                localisation = "-18.9136, 47.5251"
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = VertOfficiel)
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description détaillée") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            Text("Niveau de gravité : ${niveauDanger.roundToInt()} / 5", fontWeight = FontWeight.Bold)
            Slider(
                value = niveauDanger,
                onValueChange = { niveauDanger = it },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.error,
                    activeTrackColor = MaterialTheme.colorScheme.error
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Caméra")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PHOTO OBLIGATOIRE")
                }

                IconButton(
                    onClick = {
                        val simBitmap = genererPhotoTest(context)
                        photoBitmap = simBitmap
                        photoUri = sauvegarderPreuve(context, simBitmap)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.DeveloperMode, contentDescription = "Simuler Photo")
                }
            }

            photoBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Preuve visuelle",
                    modifier = Modifier.fillMaxWidth().height(130.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (categorie.isNotBlank() && localisation.isNotBlank() && photoUri != null) {
                        viewModel.signalerProbleme(
                            categorie = categorie,
                            description = description,
                            localisation = localisation,
                            niveauDanger = niveauDanger.roundToInt(),
                            photoUri = photoUri
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                enabled = categorie.isNotBlank() && localisation.isNotBlank() && photoUri != null
            ) {
                Text("TRANSMETTRE À L'ÉTAT", fontWeight = FontWeight.Bold)
            }
        }
    }
}