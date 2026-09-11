package mg.etat.madaalerte.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertes_table")
data class Alerte(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categorie: String,
    val description: String,
    val localisation: String,
    val niveauDanger: Int,
    val photoUri: String? = null, // Nouveau champ pour la preuve visuelle
    val statut: String = "SIGNALEMENT REÇU",
    val synchronise: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)