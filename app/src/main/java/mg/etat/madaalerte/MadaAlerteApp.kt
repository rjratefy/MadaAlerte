package mg.etat.madaalerte

import android.app.Application
import mg.etat.madaalerte.data.AppDatabase
import mg.etat.madaalerte.viewmodel.AlerteRepository

class MadaAlerteApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlerteRepository(database.alerteDao()) }
}