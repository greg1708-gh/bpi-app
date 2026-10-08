package au.bpi.inspect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Contact::class, Job::class, ItemResult::class, Finding::class, Photo::class, LibComment::class],
    version = 1,
    exportSchema = true
)
abstract class AppDb : RoomDatabase() {
    abstract fun contacts(): ContactDao
    abstract fun jobs(): JobDao
    abstract fun results(): ResultDao
    abstract fun findings(): FindingDao
    abstract fun photos(): PhotoDao
    abstract fun library(): LibraryDao

    companion object {
        fun build(context: Context): AppDb =
            Room.databaseBuilder(context, AppDb::class.java, "bpi.db")
                // Future versions add migrations here so your data is kept on every update.
                .build()
    }
}
