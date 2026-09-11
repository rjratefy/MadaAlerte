package mg.etat.madaalerte.viewmodel

import kotlinx.coroutines.flow.Flow
import mg.etat.madaalerte.data.Alerte
import mg.etat.madaalerte.data.AlerteDao

class AlerteRepository(private val alerteDao: AlerteDao) {
    val toutesLesAlertes: Flow<List<Alerte>> = alerteDao.toutesLesAlertes()
    val alertesEnAttente: Flow<Int> = alerteDao.compterAlertesEnAttente()

    // Ici on utilise Flow<Int>, pas StateFlow !
    val totalAlertes: Flow<Int> = alerteDao.compterTotalAlertes()
    val alertesResolues: Flow<Int> = alerteDao.compterAlertesResolues()

    suspend fun inserer(alerte: Alerte) {
        alerteDao.inserer(alerte)
    }

    suspend fun marquerCommeResolu(alerte: Alerte) {
        alerteDao.modifier(alerte.copy(statut = "RÉSOLU ✅", synchronise = true))
    }
}