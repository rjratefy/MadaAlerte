package mg.etat.madaalerte.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CitoyenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererCitoyen(citoyen: Citoyen): Long

    @Update
    suspend fun modifierCitoyen(citoyen: Citoyen)

    // Pour la connexion : on cherche si le numéro existe déjà
    @Query("SELECT * FROM citoyens_table WHERE telephone = :telephone LIMIT 1")
    suspend fun getCitoyenByPhone(telephone: String): Citoyen?

    // Pour l'Admin (L'État) : Voir tous les citoyens qui ont envoyé leur CIN
    @Query("SELECT * FROM citoyens_table WHERE statutValidation = 'EN ATTENTE'")
    fun getCitoyensEnAttente(): Flow<List<Citoyen>>
}