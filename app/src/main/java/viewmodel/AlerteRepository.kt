package mg.etat.madaalerte.viewmodel

import kotlinx.coroutines.flow.Flow
import mg.etat.madaalerte.data.Alerte
import mg.etat.madaalerte.data.AlerteDao
import mg.etat.madaalerte.data.Citoyen
import mg.etat.madaalerte.data.CitoyenDao

class AlerteRepository(
    private val alerteDao: AlerteDao,
    private val citoyenDao: CitoyenDao // Ajout du DAO Citoyen
) {
    // --- GESTION DES ALERTES ---
    val toutesLesAlertes: Flow<List<Alerte>> = alerteDao.toutesLesAlertes()
    val alertesEnAttente: Flow<Int> = alerteDao.compterAlertesEnAttente()
    val totalAlertes: Flow<Int> = alerteDao.compterTotalAlertes()
    val alertesResolues: Flow<Int> = alerteDao.compterAlertesResolues()

    suspend fun inserer(alerte: Alerte) = alerteDao.inserer(alerte)
    suspend fun marquerCommeResolu(alerte: Alerte) {
        alerteDao.modifier(alerte.copy(statut = "RÉSOLU", synchronise = true))
    }

    // --- GESTION DES CITOYENS (ANTI-FRAUDE) ---
    suspend fun inscrireCitoyen(citoyen: Citoyen) = citoyenDao.insererCitoyen(citoyen)
    suspend fun getCitoyenByPhone(telephone: String) = citoyenDao.getCitoyenByPhone(telephone)
    val citoyensEnAttente: Flow<List<Citoyen>> = citoyenDao.getCitoyensEnAttente()
    suspend fun validerCitoyen(citoyen: Citoyen) = citoyenDao.modifierCitoyen(citoyen.copy(statutValidation = "VALIDE"))
}