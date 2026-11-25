package pl.ospzelazkow.glos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AliasEntity::class, VoteLogEntity::class], version = 2, exportSchema = false)
abstract class OspDatabase : RoomDatabase() {
    abstract fun dao(): OspDao

    companion object {
        @Volatile private var inst: OspDatabase? = null
        fun get(ctx: Context): OspDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, OspDatabase::class.java, "osp.db")
                .fallbackToDestructiveMigration().build().also { inst = it }
        }
    }
}