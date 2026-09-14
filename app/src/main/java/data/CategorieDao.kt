package mg.etat.madaalerte.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserer(categorie: CategorieAlerte)

    @Delete
    suspend fun supprimer(categorie: CategorieAlerte)

    @Query("SELECT * FROM categories_table ORDER BY nomFr ASC")
    fun toutesLesCategories(): Flow<List<CategorieAlerte>>

    @Query("SELECT COUNT(*) FROM categories_table")
    suspend fun compterCategories(): Int
}