package br.com.coletafacil.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object KronosStatus {
    const val PENDING = "Pendente"
    val values = listOf(PENDING, "Sim", "Não")
}

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val closed: Boolean = false,
)

@Entity(tableName = "items", foreignKeys = [ForeignKey(
    entity = CollectionEntity::class, parentColumns = ["id"], childColumns = ["collectionId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("collectionId")])
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val collectionId: String,
    val code: String,
    val name: String = "",
    val status: String = KronosStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
)

data class CollectionSummary(
    @Embedded val collection: CollectionEntity,
    val total: Int,
    val pending: Int,
    val unnamed: Int,
)

@Dao
interface ColetaDao {
    @Query("""SELECT c.*, COUNT(i.id) AS total,
        COALESCE(SUM(CASE WHEN i.status = 'Pendente' THEN 1 ELSE 0 END),0) AS pending,
        COALESCE(SUM(CASE WHEN i.id IS NOT NULL AND TRIM(i.name) = '' THEN 1 ELSE 0 END),0) AS unnamed
        FROM collections c LEFT JOIN items i ON c.id = i.collectionId
        GROUP BY c.id ORDER BY c.createdAt DESC""")
    fun observeCollections(): Flow<List<CollectionSummary>>
    @Query("SELECT * FROM items WHERE collectionId = :collectionId ORDER BY createdAt DESC, id DESC")
    fun observeItems(collectionId: String): Flow<List<ItemEntity>>
    @Query("SELECT * FROM items WHERE collectionId = :collectionId ORDER BY createdAt ASC, id ASC")
    suspend fun itemsSnapshot(collectionId: String): List<ItemEntity>
    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun collection(id: String): CollectionEntity?
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun item(id: String): ItemEntity?
    @Insert suspend fun insertCollection(collection: CollectionEntity)
    @Insert suspend fun insertItem(item: ItemEntity)
    @Query("UPDATE items SET code = :code, name = :name, status = :status WHERE id = :id")
    suspend fun updateItem(id: String, code: String, name: String, status: String)
    @Query("DELETE FROM items WHERE id = :id") suspend fun deleteItem(id: String)
    @Query("UPDATE collections SET closed = :closed WHERE id = :id") suspend fun setClosed(id: String, closed: Boolean)
}

@Database(entities = [CollectionEntity::class, ItemEntity::class], version = 1, exportSchema = true)
abstract class ColetaDatabase : RoomDatabase() {
    abstract fun dao(): ColetaDao
    companion object {
        @Volatile private var instance: ColetaDatabase? = null
        fun get(context: Context): ColetaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ColetaDatabase::class.java, "coleta-facil.db")
                .build().also { instance = it }
        }
    }
}
