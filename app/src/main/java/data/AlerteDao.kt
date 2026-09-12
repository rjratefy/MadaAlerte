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

    @Query("SELECT * FROM alertes_table ORDER BY niveauDanger DESC, timestamp DESC")
    fun toutesLesAlertes(): Flow<List<Alerte>>

    @Query("SELECT COUNT(*) FROM alertes_table WHERE statut = 'SIGNALEMENT REÇU'")
    fun compterAlertesEnAttente(): Flow<Int>

    @Query("SELECT COUNT(*) FROM alertes_table")
    fun compterTotalAlertes(): Flow<Int>

    // Utilisation de LIKE pour garantir un comptage infaillible
    @Query("SELECT COUNT(*) FROM alertes_table WHERE statut LIKE 'RÉSOLU%'")
    fun compterAlertesResolues(): Flow<Int>
}