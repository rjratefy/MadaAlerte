package mg.etat.madaalerte.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "citoyens_table")
data class Citoyen(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nomPrenom: String,
    val telephone: String,
    val email: String,
    val pieceIdentiteUri: String?, // Lien vers la photo de la CIN (Carte d'Identité)
    val statutValidation: String = "EN ATTENTE", // Peut être: EN ATTENTE, VALIDE, REJETE
    val timestampInscription: Long = System.currentTimeMillis()
)