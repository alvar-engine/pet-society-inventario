package com.petsociety.inventario.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * MODELO: capa de persistencia con SQLiteOpenHelper.
 * Todas las consultas usan parámetros (?) para evitar inyección SQL.
 */
class ProductDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "PetSocietyDB"

        /**
         * Historial del esquema:
         *  v1 -> products(id, name, category, price, stock)            [versión inicial]
         *  v2 -> + brand (marca) y + image_path (evidencia visual)
         * Para un cambio futuro: subir a 3 y agregar un bloque "if (oldVersion < 3)" en onUpgrade.
         */
        private const val DATABASE_VERSION = 2

        private const val TABLE_PRODUCTS = "products"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_BRAND = "brand"
        private const val KEY_CATEGORY = "category"
        private const val KEY_PRICE = "price"
        private const val KEY_STOCK = "stock"
        private const val KEY_IMAGE_PATH = "image_path"
    }

    /** Instalación nueva: se crea directamente el esquema de la versión actual. */
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_PRODUCTS ("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_NAME TEXT NOT NULL,"
                + "$KEY_BRAND TEXT NOT NULL,"
                + "$KEY_CATEGORY TEXT NOT NULL,"
                + "$KEY_PRICE REAL NOT NULL,"
                + "$KEY_STOCK INTEGER NOT NULL,"
                + "$KEY_IMAGE_PATH TEXT" + ")")
        db.execSQL(createTable)
    }

    /**
     * Actualización de la app: migra el esquema paso a paso SIN borrar el inventario.
     * Cada bloque lleva la BD de una versión a la siguiente, así un usuario que salte
     * de la v1 a la v3 (por ejemplo) pasa por todas las migraciones en orden.
     * Se ejecuta dentro de una transacción: si algo falla, no queda a medio migrar.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // v1 -> v2: se agregan marca y ruta de la foto. Los productos existentes
            // conservan sus datos; la marca queda vacía y sin foto hasta que se editen.
            addColumnIfMissing(db, KEY_BRAND, "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, KEY_IMAGE_PATH, "TEXT")
        }
        // if (oldVersion < 3) { ... próxima migración ... }
    }

    /** Evita el error "duplicate column" si la columna ya existe. */
    private fun addColumnIfMissing(db: SQLiteDatabase, column: String, definition: String) {
        val exists = db.rawQuery("PRAGMA table_info($TABLE_PRODUCTS)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == column }
        }
        if (!exists) {
            db.execSQL("ALTER TABLE $TABLE_PRODUCTS ADD COLUMN $column $definition")
        }
    }

    // ---------- CRUD ----------

    /** CREATE: devuelve el id generado, o -1 si falló. */
    fun insertProduct(product: Product): Long =
        writableDatabase.insert(TABLE_PRODUCTS, null, toContentValues(product))

    /** READ: todos los productos ordenados por nombre. */
    fun getAllProducts(): List<Product> {
        val products = mutableListOf<Product>()
        readableDatabase.query(
            TABLE_PRODUCTS, null, null, null, null, null,
            "$KEY_NAME COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                products.add(cursorToProduct(cursor))
            }
        }
        return products
    }

    /** READ: un producto por id, o null si no existe. */
    fun getProductById(id: Long): Product? {
        readableDatabase.query(
            TABLE_PRODUCTS, null, "$KEY_ID = ?", arrayOf(id.toString()),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursorToProduct(cursor) else null
        }
    }

    /** UPDATE: devuelve la cantidad de filas modificadas. */
    fun updateProduct(product: Product): Int =
        writableDatabase.update(
            TABLE_PRODUCTS, toContentValues(product),
            "$KEY_ID = ?", arrayOf(product.id.toString())
        )

    /** DELETE: devuelve la cantidad de filas eliminadas. */
    fun deleteProduct(id: Long): Int =
        writableDatabase.delete(TABLE_PRODUCTS, "$KEY_ID = ?", arrayOf(id.toString()))

    // ---------- Conversión ----------

    private fun toContentValues(product: Product) = ContentValues().apply {
        put(KEY_NAME, product.name)
        put(KEY_BRAND, product.brand)
        put(KEY_CATEGORY, product.category)
        put(KEY_PRICE, product.price)
        put(KEY_STOCK, product.stock)
        put(KEY_IMAGE_PATH, product.imagePath)
    }

    private fun cursorToProduct(cursor: Cursor): Product {
        val imageIndex = cursor.getColumnIndexOrThrow(KEY_IMAGE_PATH)
        return Product(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
            brand = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BRAND)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)),
            price = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PRICE)),
            stock = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_STOCK)),
            imagePath = if (cursor.isNull(imageIndex)) null else cursor.getString(imageIndex)
        )
    }
}
