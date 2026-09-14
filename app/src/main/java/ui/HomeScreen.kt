package mg.etat.madaalerte.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mg.etat.madaalerte.R
import mg.etat.madaalerte.data.Alerte
import mg.etat.madaalerte.data.Citoyen
import mg.etat.madaalerte.viewmodel.AlerteViewModel
import mg.etat.madaalerte.ui.theme.MalagasyRed
import mg.etat.madaalerte.ui.theme.MalagasyGreen
import mg.etat.madaalerte.ui.theme.MalagasyDark
import mg.etat.madaalerte.ui.theme.MalagasySurface
import java.io.File
import java.util.Locale

fun getTexteLangue(): String = when (Locale.getDefault().language) { "mg" -> "MG"; "en" -> "EN"; else -> "FR" }

fun basculerLangue(context: Context) {
    val courante = Locale.getDefault().language
    val nouvelle = when (courante) { "fr" -> "mg"; "mg" -> "en"; else -> "fr" }
    val prefs = context.getSharedPreferences("prefs_mada", Context.MODE_PRIVATE)
    prefs.edit().putString("selected_lang", nouvelle).apply()
    val locale = Locale(nouvelle)
    Locale.setDefault(locale)
    val config = context.resources.configuration
    config.setLocale(locale)
    @Suppress("DEPRECATION")
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    (context as? android.app.Activity)?.recreate()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AlerteViewModel, navController: NavController, isAdmin: Boolean) {
    val context = LocalContext.current

    val alertesAdmin by viewModel.alertesFiltrees.collectAsState()
    val alertesCitoyen by viewModel.mesAlertes.collectAsState()
    val listeAffichage = if (isAdmin) alertesAdmin else alertesCitoyen

    val enAttente by viewModel.alertesEnAttente.collectAsState()
    val totalAlertes by viewModel.totalAlertes.collectAsState()
    val alertesResolues by viewModel.alertesResolues.collectAsState()
    val filtreActuel by viewModel.filtreUrgence.collectAsState()
    val filtreCatActuel by viewModel.filtreCategorie.collectAsState()
    val citoyensEnAttente by viewModel.citoyensEnAttente.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val stats by viewModel.statsCategories.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val langueCourante = Locale.getDefault().language

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painter = painterResource(id = R.drawable.logo_mada), contentDescription = "Logo", modifier = Modifier.size(36.dp).padding(end = 8.dp))
                            Column {
                                Text(if (isAdmin) stringResource(id = R.string.espace_officiel) else stringResource(id = R.string.mada_alerte), fontWeight = FontWeight.Bold)
                                val profilActuel = if (isAdmin) "Agent habilité (État)" else "${viewModel.citoyenConnecte?.nomPrenom ?: "Citoyen"}"
                                Text(text = profilActuel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    },
                    actions = {
                        TextButton(onClick = { basculerLangue(context) }) {
                            Icon(Icons.Default.Language, contentDescription = "Langue", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getTexteLangue(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (isAdmin) {
                            IconButton(onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "Rapport Infrastructures")
                                    putExtra(Intent.EXTRA_TEXT, "Total signalements : $totalAlertes")
                                    type = "text/plain"
                                }
                                try { context.startActivity(Intent.createChooser(sendIntent, "Export")) } catch (e: Exception) { }
                            }) { Icon(Icons.Default.Share, contentDescription = "Exporter", tint = Color.White) }
                        }
                        IconButton(onClick = { viewModel.citoyenConnecte = null; navController.navigate("login") { popUpTo(0) } }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Déconnexion", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MalagasyRed, titleContentColor = Color.White)
                )

                if (isAdmin) {
                    TabRow(selectedTabIndex = selectedTabIndex, containerColor = MalagasyRed, contentColor = Color.White) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text(stringResource(id = R.string.infrastructures), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) })
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("${stringResource(id = R.string.citoyens)} (${citoyensEnAttente.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) })
                        Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("Stats & Types", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) })
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isAdmin) {
                FloatingActionButton(onClick = { navController.navigate("add_alerte") }, containerColor = MalagasyGreen) {
                    Icon(Icons.Default.Add, contentDescription = "Signaler", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        when {
            isAdmin && selectedTabIndex == 1 -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text(stringResource(id = R.string.citoyens_en_attente), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MalagasyDark) }
                    if (citoyensEnAttente.isEmpty()) item { Text(stringResource(id = R.string.aucun_profil), color = Color.Gray) }
                    items(citoyensEnAttente) { citoyen -> CitoyenCard(citoyen = citoyen, onValider = { viewModel.validerCitoyen(citoyen) }) }
                }
            }
            isAdmin && selectedTabIndex == 2 -> {
                // ONGLET 3 : STATISTIQUES ET CRUD INTELLIGENT
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("Répartition par type de problème", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MalagasyDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MalagasySurface), elevation = CardDefaults.cardElevation(2.dp)) {
                        PieChartStatistiques(stats = stats, modifier = Modifier.padding(16.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Gestion des Catégories (Traductions)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MalagasyDark)
                    Spacer(modifier = Modifier.height(8.dp))

                    var newFr by remember { mutableStateOf("") }
                    var newMg by remember { mutableStateOf("") }
                    var newEn by remember { mutableStateOf("") }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = newFr, onValueChange = { newFr = it }, label = { Text("Nom Français") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = newMg, onValueChange = { newMg = it }, label = { Text("Anarana Malagasy") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = newEn, onValueChange = { newEn = it }, label = { Text("English Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Button(
                                onClick = {
                                    viewModel.ajouterCategorie(newFr, newMg, newEn)
                                    newFr = ""; newMg = ""; newEn = ""
                                },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MalagasyDark),
                                enabled = newFr.isNotBlank() && newMg.isNotBlank() && newEn.isNotBlank()
                            ) { Text("Ajouter au système", color = Color.White) }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    categories.forEach { cat ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MalagasySurface)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.nomFr, fontWeight = FontWeight.Bold, color = MalagasyDark)
                                    Text("MG: ${cat.nomMg} | EN: ${cat.nomEn}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                IconButton(onClick = { viewModel.supprimerCategorie(cat) }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MalagasyRed) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isAdmin) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MalagasySurface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, tint = MalagasyRed)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(id = R.string.indicateurs_nationaux), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MalagasyDark)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        StatItem(label = stringResource(id = R.string.total), value = totalAlertes.toString(), color = MalagasyDark)
                                        StatItem(label = stringResource(id = R.string.en_attente), value = enAttente.toString(), color = MalagasyRed)
                                        StatItem(label = stringResource(id = R.string.resolues), value = alertesResolues.toString(), color = MalagasyGreen)
                                    }
                                    val tauxResolution = if (totalAlertes > 0) alertesResolues.toFloat() / totalAlertes.toFloat() else 0f
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("${stringResource(id = R.string.taux_reparation)} ${(tauxResolution * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MalagasyDark)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(progress = tauxResolution, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MalagasyGreen, trackColor = MalagasyRed.copy(alpha = 0.2f))
                                }
                            }
                        }

                        // --- BARRE DE FILTRES CORRIGÉE ---
                        // --- BARRE DE FILTRES OPTIMISÉE (UX) ---
                        item {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // 1. Filtres d'urgence rapides
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = filtreActuel == 0 && filtreCatActuel == "Toutes",
                                        onClick = { viewModel.reinitialiserFiltres() },
                                        label = { Text("Tous les dossiers", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MalagasyDark, selectedLabelColor = Color.White)
                                    )
                                    FilterChip(
                                        selected = filtreActuel == 5,
                                        onClick = { if(filtreActuel == 5) viewModel.filtrerParNiveau(0) else viewModel.filtrerParNiveau(5) },
                                        label = { Text(stringResource(id = R.string.urgences_niv_5)) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MalagasyRed, selectedLabelColor = Color.White)
                                    )
                                }

                                // 2. Menu déroulant explicite pour les catégories
                                var expandedFiltreCat by remember { mutableStateOf(false) }
                                @Suppress("DEPRECATION")
                                ExposedDropdownMenuBox(
                                    expanded = expandedFiltreCat,
                                    onExpandedChange = { expandedFiltreCat = !expandedFiltreCat }
                                ) {
                                    val texteAffiche = if (filtreCatActuel == "Toutes") "Toutes les catégories"
                                    else categories.find { it.nomFr == filtreCatActuel }?.getNom(langueCourante) ?: filtreCatActuel

                                    OutlinedTextField(
                                        value = texteAffiche,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Filtrer par type de problème") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = MalagasyDark)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedFiltreCat,
                                        onDismissRequest = { expandedFiltreCat = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Toutes les catégories", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                viewModel.filtrerParCategorie("Toutes")
                                                expandedFiltreCat = false
                                            }
                                        )
                                        categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.getNom(langueCourante)) },
                                                onClick = {
                                                    viewModel.filtrerParCategorie(cat.nomFr)
                                                    expandedFiltreCat = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Text(stringResource(id = R.string.fil_actualite), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    } else {
                        item { Text(stringResource(id = R.string.mes_signalements), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    }

                    // --- LE MESSAGE D'ERREUR UX QUAND LA LISTE EST VIDE ---
                    if (listeAffichage.isEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Aucun dossier correspondant à ce filtre.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        items(listeAffichage) { alerte ->
                            AlerteCard(alerte = alerte, onResoudre = { viewModel.resoudreAlerte(alerte) }, onDoublon = { viewModel.marquerCommeDoublon(alerte) }, isAdmin = isAdmin)
                        }
                    }
                }
            }
        }
    }
}

// COMPOSANT CAMEMBERT NATIF
@Composable
fun PieChartStatistiques(stats: Map<String, Int>, modifier: Modifier = Modifier) {
    val couleursPalette = listOf(MalagasyRed, MalagasyGreen, MalagasyDark, Color(0xFFF57C00), Color(0xFF1976D2), Color(0xFF8E24AA))
    val total = stats.values.sum().toFloat()

    if (total == 0f) {
        Text("Aucune donnée disponible pour le graphique", color = Color.Gray, modifier = modifier)
        return
    }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            stats.values.forEachIndexed { index, count ->
                val sweepAngle = (count / total) * 360f
                drawArc(
                    color = couleursPalette[index % couleursPalette.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            stats.entries.forEachIndexed { index, entry ->
                val pourcentage = ((entry.value / total) * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(modifier = Modifier.size(12.dp).background(couleursPalette[index % couleursPalette.size], RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${entry.key} ($pourcentage%)", style = MaterialTheme.typography.bodySmall, color = MalagasyDark, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CitoyenCard(citoyen: Citoyen, onValider: () -> Unit) {
    var afficherDetail by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { afficherDetail = true }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MalagasyDark, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = citoyen.nomPrenom, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MalagasyDark)
                Text(text = citoyen.telephone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(text = citoyen.email, style = MaterialTheme.typography.bodySmall, color = MalagasyRed)
            }
        }
    }

    if (afficherDetail) {
        AlertDialog(
            onDismissRequest = { afficherDetail = false },
            title = { Text(stringResource(id = R.string.identification_citoyen), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(citoyen.nomPrenom, fontWeight = FontWeight.Bold)
                    Text(citoyen.telephone)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(id = R.string.document_identite), fontWeight = FontWeight.Bold, color = MalagasyGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    citoyen.pieceIdentiteUri?.let { uri ->
                        val file = File(uri)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)?.let { b ->
                                Image(bitmap = b.asImageBitmap(), contentDescription = "CIN", modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onValider(); afficherDetail = false }, colors = ButtonDefaults.buttonColors(containerColor = MalagasyGreen)) { Text(stringResource(id = R.string.valider_citoyen), color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { afficherDetail = false }) { Text(stringResource(id = R.string.retour)) } }
        )
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
fun AlerteCard(alerte: Alerte, onResoudre: () -> Unit, onDoublon: () -> Unit, isAdmin: Boolean) {
    val context = LocalContext.current
    val dangerColor = when (alerte.niveauDanger) { 5, 4 -> MalagasyRed; 3 -> Color(0xFFF57C00); else -> MalagasyGreen }
    val tempsEcoule = DateUtils.getRelativeTimeSpanString(alerte.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    var afficherDetail by remember { mutableStateOf(false) }

    val estResolu = alerte.statut.uppercase().contains("RÉSOLU")
    val estDoublon = alerte.statut.uppercase().contains("DOUBLON") || alerte.statut.uppercase().contains("MIVERINA")
    val couleurStatut = if (estResolu) MalagasyGreen else if (estDoublon) Color.Gray else MalagasyRed

    val localisationComplete = if (alerte.coordonneesGps.isNotBlank()) "${alerte.quartierManuel} ${alerte.coordonneesGps}" else alerte.quartierManuel

    Card(modifier = Modifier.fillMaxWidth().clickable { afficherDetail = true }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = alerte.categorie, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MalagasyDark)
                Badge(containerColor = dangerColor) { Text(text = "Niv. ${alerte.niveauDanger}", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${stringResource(id = R.string.zone)} $localisationComplete", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Text(text = alerte.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = if(estDoublon) stringResource(id = R.string.statut_doublon) else alerte.statut, fontWeight = FontWeight.Bold, color = couleurStatut)
                Text(text = "${stringResource(id = R.string.signale)} $tempsEcoule", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (afficherDetail) {
        AlertDialog(
            onDismissRequest = { afficherDetail = false },
            title = { Text(stringResource(id = R.string.fiche_intervention), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isAdmin) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(stringResource(id = R.string.identite_signaleur), fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MalagasyDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(alerte.citoyenNom.ifBlank { stringResource(id = R.string.citoyen_non_identifie) }, style = MaterialTheme.typography.bodyMedium, color = MalagasyDark)
                                }
                                if (alerte.citoyenTelephone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = MalagasyDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(alerte.citoyenTelephone, style = MaterialTheme.typography.bodyMedium, color = MalagasyDark)
                                    }
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(id = R.string.quartier_coordonnees), fontWeight = FontWeight.Bold, color = MalagasyGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(localisationComplete, style = MaterialTheme.typography.bodySmall, color = MalagasyDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val coordPropres = alerte.coordonneesGps.replace("[", "").replace("]", "").replace(" ", "")
                                    val queryMaps = if (coordPropres.isNotBlank()) "$coordPropres(${Uri.encode(alerte.quartierManuel)})" else Uri.encode(alerte.quartierManuel)
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=$queryMaps")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try { context.startActivity(mapIntent) } catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$queryMaps"))) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MalagasyGreen), modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(id = R.string.ouvrir_carte), color = Color.White) }
                        }
                    }
                    Text("${stringResource(id = R.string.description_detaillee)} :", fontWeight = FontWeight.Bold)
                    Text(alerte.description.ifBlank { stringResource(id = R.string.aucune_description) }, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(id = R.string.preuve_visuelle), fontWeight = FontWeight.Bold)
                    alerte.photoUri?.let { uri ->
                        val file = File(uri)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)?.let { b ->
                                Image(bitmap = b.asImageBitmap(), contentDescription = "Preuve terrain", modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isAdmin && !estResolu && !estDoublon) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Bouton principal : Valider la résolution
                        Button(
                            onClick = { onResoudre(); afficherDetail = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MalagasyGreen)
                        ) {
                            Text(stringResource(id = R.string.valider_resolution), color = Color.White)
                        }

                        // 2. Bouton secondaire : Marquer comme doublon
                        OutlinedButton(
                            onClick = { onDoublon(); afficherDetail = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.marquer_doublon), color = MalagasyRed)
                        }

                        // 3. Bouton Retour tout en bas pour une ergonomie parfaite
                        TextButton(
                            onClick = { afficherDetail = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.retour), color = MalagasyDark)
                        }
                    }
                } else {
                    // Pour le citoyen ou si le dossier est déjà résolu/doublon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { afficherDetail = false }) {
                            Text(stringResource(id = R.string.fermer))
                        }
                    }
                }
            },
            dismissButton = {
                // On laisse vide car le bouton Retour est maintenant géré proprement dans la colonne du bas
            }
        )
    }
}