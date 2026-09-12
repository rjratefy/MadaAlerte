package mg.etat.madaalerte.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertes_table")
data class Alerte(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categorie: String,
    val description: String,

    // Localisation séparée pour plus de précision
    val coordonneesGps: String,
    val quartierManuel: String, // Nouveau champ saisi par l'utilisateur

    val niveauDanger: Int,
    val photoUri: String?,
    val statut: String = "EN COURS",
    val timestamp: Long = System.currentTimeMillis(),
    val synchronise: Boolean = false,

    // Traçabilité Citoyen (Anti-escroquerie)
    val citoyenNom: String = "",
    val citoyenTelephone: String = ""
)