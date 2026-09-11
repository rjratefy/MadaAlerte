package mg.etat.madaalerte.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlerteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserer(alerte: Alerte)

    @Update
    suspend fun modifier(alerte: Alerte)

    // La vraie valeur ajoutée : on trie par niveau de danger (le plus critique en 1er)
    @Query("SELECT * FROM alertes_table ORDER BY niveauDanger DESC, timestamp DESC")
    fun toutesLesAlertes(): Flow<List<Alerte>>

    // Pour le Dashboard Admin : compter les alertes urgentes
    @Query("SELECT COUNT(*) FROM alertes_table WHERE statut = 'SIGNALEMENT REÇU'")
    fun compterAlertesEnAttente(): Flow<Int>

    @Query("SELECT COUNT(*) FROM alertes_table")
    fun compterTotalAlertes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM alertes_table WHERE statut = 'RÉSOLU'")
    fun compterAlertesResolues(): Flow<Int>
}