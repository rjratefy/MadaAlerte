package mg.etat.madaalerte.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// On passe à la version 3 et on ajoute la table CategorieAlerte
@Database(entities = [Alerte::class, Citoyen::class, CategorieAlerte::class], version = 3, exportSchema = false)
abstract class MadaAlerteDatabase : RoomDatabase() {

    abstract fun alerteDao(): AlerteDao
    abstract fun citoyenDao(): CitoyenDao
    abstract fun categorieDao(): CategorieDao // Ajout du DAO Catégorie

    companion object {
        @Volatile
        private var INSTANCE: MadaAlerteDatabase? = null

        fun getDatabase(context: Context): MadaAlerteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MadaAlerteDatabase::class.java,
                    "mada_alerte_database"
                )
                    .fallbackToDestructiveMigration() // Recrée la base proprement avec la nouvelle table
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}