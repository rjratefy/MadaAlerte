package mg.etat.madaalerte.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mg.etat.madaalerte.R
import mg.etat.madaalerte.data.Alerte
import mg.etat.madaalerte.viewmodel.AlerteViewModel
import java.io.File

val MalagasyRed = Color(0xFFCE1126)
val MalagasyGreen = Color(0xFF007A3D)
val MalagasyDark = Color(0xFF1E1E1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AlerteViewModel, navController: NavController, isAdmin: Boolean) {
    val context = LocalContext.current
    val alertes by viewModel.alertesFiltrees.collectAsState()
    val enAttente by viewModel.alertesEnAttente.collectAsState()
    val totalAlertes by viewModel.totalAlertes.collectAsState()
    val alertesResolues by viewModel.alertesResolues.collectAsState()
    val filtreActuel by viewModel.filtreUrgence.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Le Logo dans le Header (Taille réduite pour le header)
                        Image(
                            painter = painterResource(id = R.drawable.logo_mada),
                            contentDescription = "Logo",
                            modifier = Modifier.size(36.dp).padding(end = 8.dp)
                        )
                        Text(if (isAdmin) "ESPACE OFFICIEL" else "MADA-ALERTE", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = {
                            val rapportTexte = "RAPPORT OFFICIEL - MADA-ALERTE\nTotal : $totalAlertes"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_SUBJECT, "Rapport Infrastructures")
                                putExtra(Intent.EXTRA_TEXT, rapportTexte)
                                type = "text/plain"
                            }
                            try {
                                context.startActivity(Intent.createChooser(sendIntent, "Exporter"))
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Aucune application de partage", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Exporter", tint = Color.White)
                        }
                    }
                    IconButton(onClick = {
                        navController.navigate("login") { popUpTo(0) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Déconnexion", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MalagasyRed,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_alerte") },
                    containerColor = MalagasyGreen
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Signaler", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        // CORRECTION ARCHITECTURE : Tout est dans un seul LazyColumn pour un scroll parfait sans crash
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. DASHBOARD STATISTIQUES ET JAUGE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = MalagasyRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Indicateurs Nationaux", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MalagasyDark)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(label = "Total", value = totalAlertes.toString(), color = MalagasyDark)
                            StatItem(label = "En attente", value = enAttente.toString(), color = MalagasyRed)
                            StatItem(label = "Résolues", value = alertesResolues.toString(), color = MalagasyGreen)
                        }

                        // INTÉGRATION DE LA JAUGE DE RÉSOLUTION
                        val tauxResolution = if (totalAlertes > 0) alertesResolues.toFloat() / totalAlertes.toFloat() else 0f
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Taux de réparation national : ${(tauxResolution * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MalagasyDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { tauxResolution },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MalagasyGreen,
                            trackColor = MalagasyRed.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            // 2. FILTRES DYNAMIQUES (Seulement pour l'État)
            if (isAdmin) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filtreActuel == 0,
                            onClick = { viewModel.filtrerParNiveau(0) },
                            label = { Text("Tous les dossiers") }
                        )
                        FilterChip(
                            selected = filtreActuel == 5,
                            onClick = { viewModel.filtrerParNiveau(5) },
                            label = { Text("Urgences Niv. 5 (Critique)") }
                        )
                    }
                }
            }

            // 3. TITRE DU FIL D'ACTUALITÉ
            item {
                Text("Fil d'actualité des infrastructures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // 4. LISTE DES ALERTES
            items(alertes) { alerte ->
                AlerteCard(alerte = alerte, onResoudre = { viewModel.resoudreAlerte(alerte) }, isAdmin = isAdmin)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun AlerteCard(alerte: Alerte, onResoudre: () -> Unit, isAdmin: Boolean) {
    val dangerColor = when (alerte.niveauDanger) {
        5, 4 -> MalagasyRed
        3 -> Color(0xFFF57C00)
        else -> MalagasyGreen
    }

    // INTÉGRATION DE L'HORODATAGE AVANCÉ ("Il y a 2 heures")
    val tempsEcoule = DateUtils.getRelativeTimeSpanString(
        alerte.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    var isSyncing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = alerte.categorie, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MalagasyDark)
                Badge(containerColor = dangerColor) {
                    Text(text = "Niv. ${alerte.niveauDanger}", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📍 ${alerte.localisation}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontWeight = FontWeight.SemiBold)

            alerte.photoUri?.let { uri ->
                val file = File(uri)
                if (file.exists()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    bitmap?.let { b ->
                        Image(
                            bitmap = b.asImageBitmap(),
                            contentDescription = "Preuve",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Text(text = alerte.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alerte.statut,
                    fontWeight = FontWeight.Bold,
                    color = if (alerte.statut.contains("RÉSOLU")) MalagasyGreen else MalagasyRed
                )

                if (!alerte.statut.contains("RÉSOLU")) {
                    if (isAdmin) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MalagasyGreen)
                        } else {
                            Button(
                                onClick = {
                                    isSyncing = true
                                    onResoudre()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MalagasyGreen)
                            ) {
                                Text("Valider la résolution", color = Color.White)
                            }
                        }
                    } else {
                        Text("En cours d'analyse", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Indicateur Hors-Ligne & Horodatage Relatif
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (alerte.synchronise) "SYNCHRONISÉ (4G)" else "EN ATTENTE RÉSEAU",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (alerte.synchronise) MalagasyGreen else MalagasyRed,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Signalé $tempsEcoule", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}