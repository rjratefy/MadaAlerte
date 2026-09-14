package mg.etat.madaalerte.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mg.etat.madaalerte.data.Alerte
import mg.etat.madaalerte.data.CategorieAlerte
import mg.etat.madaalerte.data.Citoyen

class AlerteViewModel(private val repository: AlerteRepository) : ViewModel() {

    var citoyenConnecte: Citoyen? = null

    val citoyensEnAttente: StateFlow<List<Citoyen>> = repository.citoyensEnAttente
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategorieAlerte>> = repository.toutesLesCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsCategories: StateFlow<Map<String, Int>> = repository.toutesLesAlertes.map { alertes ->
        alertes.groupBy { it.categorie }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            if (repository.compterCategories() == 0) {
                repository.insererCategorie(CategorieAlerte(nomFr = "Infrastructures routières", nomMg = "Fahasimban'ny lalana", nomEn = "Road infrastructure"))
                repository.insererCategorie(CategorieAlerte(nomFr = "Éclairage public", nomMg = "Jiro an-dalana", nomEn = "Public lighting"))
                repository.insererCategorie(CategorieAlerte(nomFr = "Assainissement & Eaux", nomMg = "Fikorianan'ny rano", nomEn = "Sanitation & Water"))
            }
        }
    }

    // --- GESTION DES FILTRES CORRIGÉE ---
    private val _filtreUrgence = MutableStateFlow(0)
    val filtreUrgence: StateFlow<Int> = _filtreUrgence
    fun filtrerParNiveau(niveau: Int) { _filtreUrgence.value = niveau }

    private val _filtreCategorie = MutableStateFlow("Toutes")
    val filtreCategorie: StateFlow<String> = _filtreCategorie
    fun filtrerParCategorie(cat: String) { _filtreCategorie.value = cat }

    fun reinitialiserFiltres() {
        _filtreUrgence.value = 0
        _filtreCategorie.value = "Toutes"
    }

    val alertesFiltrees: StateFlow<List<Alerte>> = combine(repository.toutesLesAlertes, _filtreUrgence, _filtreCategorie) { alertes, niveau, categorie ->
        var resultats = alertes
        if (niveau > 0) resultats = resultats.filter { it.niveauDanger == niveau }
        if (categorie != "Toutes") {
            // tolérance aux espaces et aux majuscules pour les vieilles données
            resultats = resultats.filter { it.categorie.trim().equals(categorie.trim(), ignoreCase = true) }
        }
        resultats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // -------------------------------------

    val mesAlertes: StateFlow<List<Alerte>> = repository.toutesLesAlertes.map { alertes ->
        alertes.filter { it.citoyenTelephone == citoyenConnecte?.telephone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertesEnAttente: StateFlow<Int> = repository.alertesEnAttente.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalAlertes: StateFlow<Int> = repository.totalAlertes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val alertesResolues: StateFlow<Int> = repository.alertesResolues.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    fun signalerProbleme(categorie: String, description: String, coordonneesGps: String, quartierManuel: String, niveauDanger: Int, photoUri: String?) {
        viewModelScope.launch {
            repository.inserer(
                Alerte(
                    categorie = categorie, description = description, coordonneesGps = coordonneesGps, quartierManuel = quartierManuel,
                    niveauDanger = niveauDanger, photoUri = photoUri, statut = "EN ATTENTE", synchronise = false,
                    citoyenNom = citoyenConnecte?.nomPrenom ?: "Anonyme", citoyenTelephone = citoyenConnecte?.telephone ?: "Inconnu"
                )
            )
        }
    }

    fun resoudreAlerte(alerte: Alerte) {
        viewModelScope.launch { repository.marquerCommeResolu(alerte.copy(statut = "RÉSOLU")) }
    }

    fun marquerCommeDoublon(alerte: Alerte) {
        viewModelScope.launch { repository.inserer(alerte.copy(statut = "DOUBLON")) }
    }

    fun ajouterCategorie(fr: String, mg: String, en: String) {
        if (fr.isNotBlank() && mg.isNotBlank() && en.isNotBlank()) {
            viewModelScope.launch { repository.insererCategorie(CategorieAlerte(nomFr = fr, nomMg = mg, nomEn = en)) }
        }
    }

    fun supprimerCategorie(categorie: CategorieAlerte) {
        viewModelScope.launch { repository.supprimerCategorie(categorie) }
    }
}

class AlerteViewModelFactory(private val repository: AlerteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlerteViewModel::class.java)) return AlerteViewModel(repository) as T
        throw IllegalArgumentException("Classe ViewModel inconnue")
    }
}