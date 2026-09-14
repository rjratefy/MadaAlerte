package mg.etat.madaalerte.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories_table")
data class CategorieAlerte(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nomFr: String,
    val nomMg: String,
    val nomEn: String
) {
    fun getNom(langue: String): String = when (langue) {
        "mg" -> nomMg
        "en" -> nomEn
        else -> nomFr
    }
}