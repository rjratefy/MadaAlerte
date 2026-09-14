package mg.etat.madaalerte

import android.app.Application
import mg.etat.madaalerte.data.MadaAlerteDatabase
import mg.etat.madaalerte.viewmodel.AlerteRepository

class MadaAlerteApp : Application() {
    val database by lazy { MadaAlerteDatabase.getDatabase(this) }

    val repository by lazy {
        AlerteRepository(
            database.alerteDao(),
            database.citoyenDao(),
            database.categorieDao() // Le 3ème DAO manquant est ajouté ici
        )
    }
}