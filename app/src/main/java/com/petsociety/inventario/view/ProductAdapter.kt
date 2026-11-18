package com.petsociety.inventario.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.petsociety.inventario.R
import com.petsociety.inventario.databinding.ItemProductBinding
import com.petsociety.inventario.model.Product
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * VISTA: adaptador del RecyclerView.
 * - Patrón ViewHolder: cada fila se infla una sola vez y luego se recicla.
 * - DiffUtil: solo se redibujan las filas que cambiaron.
 * - Glide: las fotos se decodifican fuera del hilo principal (sin "jank").
 */
class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val PRICE_FORMAT: NumberFormat =
            NumberFormat.getCurrencyInstance(Locale("es", "CL"))

        fun formatPrice(price: Double): String = PRICE_FORMAT.format(price)

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Product, newItem: Product) =
                oldItem == newItem
        }
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onItemClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentProduct: Product? = null

        init {
            // El listener se asigna una sola vez por ViewHolder, no en cada bind
            binding.root.setOnClickListener { currentProduct?.let(onItemClick) }
        }

        fun bind(product: Product) {
            currentProduct = product
            val context = binding.root.context

            binding.tvProductName.text = product.name
            binding.tvProductBrand.text = product.brand
            // Productos migrados desde la v1 pueden no tener marca todavía
            binding.tvProductBrand.visibility = if (product.brand.isBlank()) View.GONE else View.VISIBLE
            binding.tvProductCategory.text = product.category
            binding.tvProductPrice.text = formatPrice(product.price)
            binding.tvProductStock.text = context.getString(R.string.stock_value, product.stock)
            binding.tvLowStock.visibility = if (product.isLowStock) View.VISIBLE else View.GONE

            Glide.with(binding.ivProductPhoto)
                .load(product.imagePath?.let { File(it) })
                .placeholder(R.drawable.ic_pet_placeholder)
                .error(R.drawable.ic_pet_placeholder)
                .fallback(R.drawable.ic_pet_placeholder)
                .centerCrop()
                .into(binding.ivProductPhoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
