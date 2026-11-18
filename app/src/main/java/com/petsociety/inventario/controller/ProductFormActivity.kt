package com.petsociety.inventario.controller

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.petsociety.inventario.R
import com.petsociety.inventario.databinding.ActivityProductFormBinding
import com.petsociety.inventario.model.Product
import com.petsociety.inventario.model.ProductDbHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CONTROLADOR: alta, edición y borrado de un producto.
 * - Valida y sanitiza lo que el usuario escribe (la vista solo muestra los errores).
 * - Usa el servicio de cámara del sistema para capturar la evidencia visual.
 */
class ProductFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"

        private const val STATE_PENDING_PHOTO = "state_pending_photo"
        private const val STATE_TEMP_PHOTO = "state_temp_photo"

        private const val NAME_MIN = 3
        private const val NAME_MAX = 60
        private const val BRAND_MAX = 40
        private const val PRICE_MAX = 10_000_000.0
        private const val STOCK_MAX = 99_999
    }

    private lateinit var binding: ActivityProductFormBinding
    private lateinit var dbHelper: ProductDbHelper
    private var existingProduct: Product? = null

    /** Foto capturada en esta pantalla que todavía no se ha guardado en la BD. */
    private var pendingPhotoPath: String? = null

    /** Archivo entregado a la cámara mientras esperamos su respuesta. */
    private var tempPhotoPath: String? = null

    private var isSaved = false

    /** Campos que el usuario ya visitó: solo estos se validan mientras escribe. */
    private val touchedFields = mutableSetOf<TextInputLayout>()

    // Servicio del sistema: la app de cámara escribe la foto en el archivo que le entregamos
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val photoPath = tempPhotoPath ?: return@registerForActivityResult
            tempPhotoPath = null
            if (success) {
                deleteFileIfExists(pendingPhotoPath) // descarta una foto anterior no guardada
                pendingPhotoPath = photoPath
                showPhoto(photoPath)
            } else {
                deleteFileIfExists(photoPath)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = ProductDbHelper(this)

        val productId = intent.getLongExtra(EXTRA_PRODUCT_ID, 0L)
        if (productId > 0) {
            existingProduct = dbHelper.getProductById(productId)
            if (existingProduct == null) {
                Toast.makeText(this, R.string.msg_product_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }

        savedInstanceState?.let {
            pendingPhotoPath = it.getString(STATE_PENDING_PHOTO)
            tempPhotoPath = it.getString(STATE_TEMP_PHOTO)
        }

        existingProduct?.let { product ->
            title = getString(R.string.title_edit_product)
            binding.btnDelete.visibility = View.VISIBLE
            // Al rotar la pantalla los campos se restauran solos; solo se cargan la primera vez
            if (savedInstanceState == null) fillForm(product)
        } ?: run {
            title = getString(R.string.title_new_product)
        }

        showPhoto(pendingPhotoPath ?: existingProduct?.imagePath)
        setupLiveValidation()

        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnSave.setOnClickListener { saveProduct() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    override fun onResume() {
        super.onResume()
        // Se asigna aquí para que el desplegable muestre todas las opciones tras rotar
        binding.actvCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, Product.CATEGORIES)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PENDING_PHOTO, pendingPhotoPath)
        outState.putString(STATE_TEMP_PHOTO, tempPhotoPath)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        // Si el usuario salió sin guardar, la foto nueva no debe quedar huérfana
        if (isFinishing && !isSaved) deleteFileIfExists(pendingPhotoPath)
        dbHelper.close()
        super.onDestroy()
    }

    private fun fillForm(product: Product) {
        binding.etName.setText(product.name)
        binding.etBrand.setText(product.brand)
        binding.actvCategory.setText(product.category, false)
        binding.etPrice.setText(product.price.toBigDecimal().stripTrailingZeros().toPlainString())
        binding.etStock.setText(product.stock.toString())
    }

    // ---------- Cámara ----------

    private fun takePhoto() {
        val photoFile = createImageFile()
        tempPhotoPath = photoFile.absolutePath
        val photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        try {
            takePicture.launch(photoUri)
        } catch (e: ActivityNotFoundException) {
            deleteFileIfExists(tempPhotoPath)
            tempPhotoPath = null
            Toast.makeText(this, R.string.msg_no_camera, Toast.LENGTH_SHORT).show()
        }
    }

    /** Las fotos se guardan en el almacenamiento interno privado de la app. */
    private fun createImageFile(): File {
        val dir = File(filesDir, "images").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(dir, "PET_$timeStamp.jpg")
    }

    private fun showPhoto(path: String?) {
        binding.btnTakePhoto.setText(
            if (path == null) R.string.btn_take_photo else R.string.btn_retake_photo
        )
        Glide.with(this)
            .load(path?.let { File(it) })
            .placeholder(R.drawable.ic_pet_placeholder)
            .error(R.drawable.ic_pet_placeholder)
            .fallback(R.drawable.ic_pet_placeholder)
            .centerCrop()
            .into(binding.ivPhoto)
    }

    private fun deleteFileIfExists(path: String?) {
        if (path != null) File(path).delete()
    }

    // ---------- Guardar ----------

    private fun saveProduct() {
        val validated = validateInput() ?: return

        val oldProduct = existingProduct
        val product = validated.copy(
            id = oldProduct?.id ?: 0L,
            imagePath = pendingPhotoPath ?: oldProduct?.imagePath
        )

        val success = if (oldProduct == null) {
            dbHelper.insertProduct(product) != -1L
        } else {
            dbHelper.updateProduct(product) > 0
        }

        if (success) {
            // Si se reemplazó la foto, se borra la antigua del almacenamiento
            if (pendingPhotoPath != null && oldProduct?.imagePath != pendingPhotoPath) {
                deleteFileIfExists(oldProduct?.imagePath)
            }
            isSaved = true
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, R.string.msg_save_error, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Validación defensiva, campo por campo:
     * 1. Sanitiza cada entrada con trim().
     * 2. Revisa campos vacíos con isEmpty().
     * 3. Convierte números con toDoubleOrNull()/toIntOrNull() (nunca lanza excepción).
     * 4. Revisa rangos lógicos.
     * Cada función devuelve el mensaje de error, o null si el campo es válido.
     * Se usan tanto al guardar como mientras el usuario escribe (validación en vivo).
     */
    private fun nameError(): String? {
        val name = binding.etName.text.toString().trim()
        return when {
            name.isEmpty() -> getString(R.string.error_required)
            name.length !in NAME_MIN..NAME_MAX -> getString(R.string.error_name_length, NAME_MIN, NAME_MAX)
            else -> null
        }
    }

    private fun brandError(): String? {
        val brand = binding.etBrand.text.toString().trim()
        return when {
            brand.isEmpty() -> getString(R.string.error_required)
            brand.length > BRAND_MAX -> getString(R.string.error_brand_length, BRAND_MAX)
            else -> null
        }
    }

    private fun categoryError(): String? {
        val category = binding.actvCategory.text.toString().trim()
        return when {
            category.isEmpty() -> getString(R.string.error_category_required)
            category !in Product.CATEGORIES -> getString(R.string.error_category_invalid)
            else -> null
        }
    }

    private fun priceError(): String? {
        val priceText = priceInput()
        val price = priceText.toDoubleOrNull()
        return when {
            priceText.isEmpty() -> getString(R.string.error_required)
            price == null -> getString(R.string.error_price_format)
            price <= 0 || price > PRICE_MAX -> getString(R.string.error_price_range)
            else -> null
        }
    }

    private fun stockError(): String? {
        val stockText = binding.etStock.text.toString().trim()
        val stock = stockText.toIntOrNull()
        return when {
            stockText.isEmpty() -> getString(R.string.error_required)
            stock == null -> getString(R.string.error_stock_format)
            stock !in 0..STOCK_MAX -> getString(R.string.error_stock_range, STOCK_MAX)
            else -> null
        }
    }

    /** Se acepta coma o punto como separador decimal. */
    private fun priceInput() = binding.etPrice.text.toString().trim().replace(',', '.')

    /** Cada campo del formulario junto a la función que lo valida. */
    private fun fieldValidators(): List<Pair<TextInputLayout, () -> String?>> = listOf(
        binding.tilName to ::nameError,
        binding.tilBrand to ::brandError,
        binding.tilCategory to ::categoryError,
        binding.tilPrice to ::priceError,
        binding.tilStock to ::stockError
    )

    /**
     * Al guardar: valida todos los campos y muestra cada error con .error.
     * Devuelve el producto si todo es válido, o null si hay algún error.
     */
    private fun validateInput(): Product? {
        var isValid = true
        fieldValidators().forEach { (layout, validate) ->
            touchedFields.add(layout) // desde ahora también se valida en vivo
            layout.error = validate()
            if (layout.error != null) isValid = false
        }

        val price = priceInput().toDoubleOrNull()
        val stock = binding.etStock.text.toString().trim().toIntOrNull()
        if (!isValid || price == null || stock == null) {
            focusFirstError()
            return null
        }

        return Product(
            name = binding.etName.text.toString().trim(),
            brand = binding.etBrand.text.toString().trim(),
            category = binding.actvCategory.text.toString().trim(),
            price = price,
            stock = stock
        )
    }

    private fun focusFirstError() {
        fieldValidators().firstOrNull { it.first.error != null }?.first?.editText?.requestFocus()
    }

    /**
     * Validación en vivo (feedback inmediato y no intrusivo):
     * - Al salir de un campo, se valida y se muestra su error con .error.
     * - Desde ese momento, el campo se revalida con cada tecla: el error aparece
     *   en cuanto el dato deja de ser válido y desaparece en cuanto se corrige.
     * Un campo que el usuario todavía no ha tocado nunca se marca en rojo.
     */
    private fun setupLiveValidation() {
        fieldValidators().forEach { (layout, validate) ->
            val editText = layout.editText ?: return@forEach
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    touchedFields.add(layout)
                    layout.error = validate()
                }
            }
            editText.doAfterTextChanged {
                if (layout in touchedFields) layout.error = validate()
            }
        }
    }

    // ---------- Borrar ----------

    private fun confirmDelete() {
        val product = existingProduct ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, product.name))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ -> deleteProduct(product) }
            .show()
    }

    private fun deleteProduct(product: Product) {
        if (dbHelper.deleteProduct(product.id) > 0) {
            deleteFileIfExists(product.imagePath)
            deleteFileIfExists(pendingPhotoPath)
            pendingPhotoPath = null
            isSaved = true
            Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, R.string.msg_delete_error, Toast.LENGTH_SHORT).show()
        }
    }
}
