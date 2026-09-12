package mg.etat.madaalerte.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mg.etat.madaalerte.data.Alerte
import mg.etat.madaalerte.data.Citoyen

class AlerteViewModel(private val repository: AlerteRepository) : ViewModel() {

    var citoyenConnecte: Citoyen? = null

    val citoyensEnAttente: StateFlow<List<Citoyen>> = repository.citoyensEnAttente
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun inscrireCitoyen(nom: String, tel: String, email: String, photoCin: String, onResult: () -> Unit) {
        viewModelScope.launch {
            repository.inscrireCitoyen(Citoyen(nomPrenom = nom, telephone = tel, email = email, pieceIdentiteUri = photoCin, statutValidation = "EN ATTENTE"))
            onResult()
        }
    }

    fun connecterCitoyen(telephone: String, onResult: (Citoyen?) -> Unit) {
        viewModelScope.launch {
            val citoyen = repository.getCitoyenByPhone(telephone)
            if (citoyen != null) citoyenConnecte = citoyen
            onResult(citoyen)
        }
    }

    fun validerCitoyen(citoyen: Citoyen) {
        viewModelScope.launch { repository.validerCitoyen(citoyen) }
    }

    private val _filtreUrgence = MutableStateFlow(0)
    val filtreUrgence: StateFlow<Int> = _filtreUrgence

    val alertesFiltrees: StateFlow<List<Alerte>> = combine(repository.toutesLesAlertes, _filtreUrgence) { alertes, niveau ->
        if (niveau == 0) alertes else alertes.filter { it.niveauDanger == niveau }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // NOUVEAU : LA LISTE DES ALERTES UNIQUEMENT POUR LE CITOYEN CONNECTÉ
    val mesAlertes: StateFlow<List<Alerte>> = repository.toutesLesAlertes.map { alertes ->
        alertes.filter { it.citoyenTelephone == citoyenConnecte?.telephone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertesEnAttente: StateFlow<Int> = repository.alertesEnAttente.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalAlertes: StateFlow<Int> = repository.totalAlertes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val alertesResolues: StateFlow<Int> = repository.alertesResolues.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun filtrerParNiveau(niveau: Int) { _filtreUrgence.value = niveau }

    fun signalerProbleme(categorie: String, description: String, coordonneesGps: String, quartierManuel: String, niveauDanger: Int, photoUri: String?) {
        viewModelScope.launch {
            repository.inserer(
                Alerte(
                    categorie = categorie, description = description, coordonneesGps = coordonneesGps, quartierManuel = quartierManuel,
                    niveauDanger = niveauDanger, photoUri = photoUri, statut = "EN COURS", synchronise = false,
                    citoyenNom = citoyenConnecte?.nomPrenom ?: "Anonyme", citoyenTelephone = citoyenConnecte?.telephone ?: "Inconnu"
                )
            )
        }
    }

    fun resoudreAlerte(alerte: Alerte) {
        viewModelScope.launch { repository.marquerCommeResolu(alerte.copy(statut = "RÉSOLU")) }
    }

    // NOUVEAU : MARQUER COMME DOUBLON
    fun marquerCommeDoublon(alerte: Alerte) {
        viewModelScope.launch { repository.inserer(alerte.copy(statut = "DOUBLON")) }
    }
}

class AlerteViewModelFactory(private val repository: AlerteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlerteViewModel::class.java)) return AlerteViewModel(repository) as T
        throw IllegalArgumentException("Classe ViewModel inconnue")
    }
}