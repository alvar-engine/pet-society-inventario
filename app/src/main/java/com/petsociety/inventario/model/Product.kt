package com.petsociety.inventario.model

/**
 * MODELO: representa un producto del inventario de Pet Society.
 * id = 0 significa que el producto aún no existe en la base de datos.
 */
data class Product(
    val id: Long = 0,
    val name: String,
    val brand: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val imagePath: String? = null
) {
    val isLowStock: Boolean
        get() = stock <= LOW_STOCK_LIMIT

    companion object {
        const val LOW_STOCK_LIMIT = 5

        // Líneas de producto de la tienda
        val CATEGORIES = listOf(
            "Alimentos Premium",
            "Snacks",
            "Salud y Bienestar",
            "Higiene y Cepillos",
            "Juguetes",
            "Accesorios"
        )
    }
}
