package mg.etat.madaalerte.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Alerte::class], version = 2, exportSchema = false) // Version passée à 2
abstract class AppDatabase : RoomDatabase() {

    abstract fun alerteDao(): AlerteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "madaalerte_database"
                )
                    .fallbackToDestructiveMigration() // Évite les crashs et nettoie proprement si la structure change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Database(entities = [Alerte::class, Citoyen::class], version = 2, exportSchema = false)
abstract class MadaAlerteDatabase : RoomDatabase() {

    abstract fun alerteDao(): AlerteDao
    abstract fun citoyenDao(): CitoyenDao // Ajout du DAO Citoyen

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
                    .fallbackToDestructiveMigration() // Empêche les crashs lors des mises à jour de la base !
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}