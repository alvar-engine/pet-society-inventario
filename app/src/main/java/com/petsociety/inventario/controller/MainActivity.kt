package com.petsociety.inventario.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.petsociety.inventario.R
import com.petsociety.inventario.databinding.ActivityMainBinding
import com.petsociety.inventario.model.ProductDbHelper
import com.petsociety.inventario.view.ProductAdapter

/**
 * CONTROLADOR: listado del inventario.
 * Pide los datos al modelo (ProductDbHelper) y se los entrega a la vista (ProductAdapter).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: ProductDbHelper
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ProductDbHelper(this)
        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, ProductFormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Se recarga al volver del formulario (alta, edición o borrado)
        refreshList()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter { product ->
            val intent = Intent(this, ProductFormActivity::class.java)
            intent.putExtra(ProductFormActivity.EXTRA_PRODUCT_ID, product.id)
            startActivity(intent)
        }
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.setHasFixedSize(true)
        binding.rvProducts.adapter = adapter
    }

    private fun refreshList() {
        val products = dbHelper.getAllProducts()
        adapter.submitList(products)

        val isEmpty = products.isEmpty()
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvProducts.visibility = if (isEmpty) View.GONE else View.VISIBLE

        val lowStock = products.count { it.isLowStock }
        // getQuantityString elige "1 producto" o "N productos" según la cantidad
        val count = resources.getQuantityString(R.plurals.products_count, products.size, products.size)
        supportActionBar?.subtitle = when {
            isEmpty -> null
            lowStock > 0 -> getString(R.string.subtitle_with_low_stock, count, lowStock)
            else -> getString(R.string.subtitle_count, count)
        }
    }
}
