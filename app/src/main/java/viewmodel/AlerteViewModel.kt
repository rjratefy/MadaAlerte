package mg.etat.madaalerte.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mg.etat.madaalerte.data.Alerte

class AlerteViewModel(private val repository: AlerteRepository) : ViewModel() {

    // Gestion du filtre dynamique (0 = Tous, 5 = Niveau 5 critique uniquement)
    private val _filtreUrgence = MutableStateFlow(0)
    val filtreUrgence: StateFlow<Int> = _filtreUrgence

    // Liste filtrée automatiquement selon le choix de l'agent
    val alertesFiltrees: StateFlow<List<Alerte>> = combine(
        repository.toutesLesAlertes,
        _filtreUrgence
    ) { alertes, niveau ->
        if (niveau == 0) alertes else alertes.filter { it.niveauDanger == niveau }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // On garde aussi l'ancienne liste au cas où
    val listeAlertes: StateFlow<List<Alerte>> = repository.toutesLesAlertes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertesEnAttente: StateFlow<Int> = repository.alertesEnAttente
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAlertes: StateFlow<Int> = repository.totalAlertes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val alertesResolues: StateFlow<Int> = repository.alertesResolues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun filtrerParNiveau(niveau: Int) {
        _filtreUrgence.value = niveau
    }

    fun signalerProbleme(
        categorie: String,
        description: String,
        localisation: String,
        niveauDanger: Int,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val nouvelleAlerte = Alerte(
                categorie = categorie,
                description = description,
                localisation = localisation,
                niveauDanger = niveauDanger,
                photoUri = photoUri,
                synchronise = false // Marqué synchronisé ou géré hors-ligne
            )
            repository.inserer(nouvelleAlerte)
        }
    }

    fun resoudreAlerte(alerte: Alerte) {
        viewModelScope.launch {
            delay(2000)
            repository.marquerCommeResolu(alerte)
        }
    }
}

class AlerteViewModelFactory(private val repository: AlerteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlerteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlerteViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel inconnue")
    }
}