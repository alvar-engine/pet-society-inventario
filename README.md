# Pet Society — Inventario

Aplicación Android para gestionar el inventario de **Pet Society**, una tienda de productos para mascotas (alimentos premium, snacks, salud y bienestar, higiene, juguetes y accesorios de marcas como Bravery, Fit Formula y Doc Kennedy).

Permite registrar, ver, editar y eliminar productos, cada uno con su foto tomada con la cámara del teléfono. Los datos se guardan en una base de datos SQLite local, así que no se pierden al cerrar la app.

---

## Índice

1. [Funcionalidades](#funcionalidades)
2. [Tecnologías](#tecnologías)
3. [Estructura del proyecto (MVC)](#estructura-del-proyecto-mvc)
4. [Requisitos](#requisitos)
5. [Cómo ejecutar el proyecto paso a paso](#cómo-ejecutar-el-proyecto-paso-a-paso)
6. [Cómo usar la app](#cómo-usar-la-app)
7. [Cómo trabajar en el proyecto](#cómo-trabajar-en-el-proyecto)
   - [Agregar o cambiar categorías](#agregar-o-cambiar-categorías)
   - [Cambiar el límite de stock bajo](#cambiar-el-límite-de-stock-bajo)
   - [Cambiar las reglas de validación](#cambiar-las-reglas-de-validación)
   - [Agregar un campo nuevo al producto (migrar la BD)](#agregar-un-campo-nuevo-al-producto-migrar-la-bd)
   - [Cambiar textos, colores e íconos](#cambiar-textos-colores-e-íconos)
8. [Revisar la base de datos](#revisar-la-base-de-datos)
9. [Problemas frecuentes](#problemas-frecuentes)

---

## Funcionalidades

- **CRUD completo** de productos: crear, listar, editar y eliminar (con confirmación antes de borrar).
- **Campos del producto:** nombre, marca, categoría, precio y stock, además de una foto opcional.
- **Foto del producto** con la cámara del sistema. Las fotos se guardan en la memoria privada de la app y se borran al eliminar o reemplazar la foto.
- **Validación de datos:** cada error se muestra debajo de su campo (propiedad `.error`) y desaparece al corregirlo.
- **Aviso de stock bajo:** los productos con **5 unidades o menos** aparecen marcados, y la barra superior muestra cuántos hay.
- **Precios en pesos chilenos** (ej.: `$54.990`).
- **Migración de la base de datos** sin pérdida de datos: al pasar de la versión 1 a la 2 se agregaron la marca y la foto.

## Tecnologías

| Elemento | Versión / detalle |
|---|---|
| Lenguaje | Kotlin 1.9.22 |
| Interfaz | Vistas XML + ViewBinding + Material Components |
| Base de datos | SQLite con `SQLiteOpenHelper` |
| Listado | `RecyclerView` + patrón ViewHolder (`ListAdapter` + `DiffUtil`) |
| Imágenes | Glide 4.16 |
| Fotos | `ActivityResultContracts.TakePicture` + `FileProvider` |
| Build | Gradle 8.5, Android Gradle Plugin 8.2.2 |
| SDK | `minSdk 24` (Android 7.0), `targetSdk`/`compileSdk 34` |

## Estructura del proyecto (MVC)

```
PetSociety/
├── app/
│   ├── build.gradle                         ← dependencias y configuración del módulo
│   └── src/main/
│       ├── AndroidManifest.xml              ← actividades, FileProvider
│       ├── java/com/petsociety/inventario/
│       │   ├── model/                       ← MODELO: datos y base de datos
│       │   │   ├── Product.kt               ← clase Product, lista de CATEGORIES, límite de stock bajo
│       │   │   └── ProductDbHelper.kt       ← SQLiteOpenHelper: tabla, CRUD y migraciones (onUpgrade)
│       │   ├── view/                        ← VISTA: cómo se muestran los datos
│       │   │   └── ProductAdapter.kt        ← adaptador del RecyclerView (ViewHolder)
│       │   └── controller/                  ← CONTROLADOR: lógica de cada pantalla
│       │       ├── MainActivity.kt          ← pantalla de listado
│       │       └── ProductFormActivity.kt   ← formulario: crear/editar/eliminar, validación, cámara
│       └── res/
│           ├── layout/                      ← activity_main.xml, activity_product_form.xml, item_product.xml
│           ├── values/                      ← strings.xml (textos), colors.xml, themes.xml
│           ├── drawable/                    ← íconos (agregar, cámara, imagen por defecto)
│           └── xml/file_paths.xml           ← carpetas compartidas con la cámara
├── build.gradle, settings.gradle, gradle.properties
└── gradlew / gradlew.bat                    ← Gradle wrapper (no hace falta instalar Gradle)
```

**Cómo se comunican las capas:**
`MainActivity` le pide los productos a `ProductDbHelper` (modelo) y se los entrega a `ProductAdapter` (vista) para que los dibuje. Al tocar un producto o el botón **+**, se abre `ProductFormActivity`, que valida lo ingresado y lo guarda con `ProductDbHelper`. Al volver, `MainActivity.onResume()` recarga la lista.

---

## Requisitos

- **Android Studio** (Hedgehog 2023.1 o más reciente). Ya trae el JDK necesario (JBR 17).
- **Android SDK** con la plataforma **API 34** (Android Studio la instala si falta).
- Un **emulador** (AVD) o un **teléfono Android 7.0 o superior**.
- Conexión a internet la primera vez, para descargar Gradle y las dependencias.

---

## Cómo ejecutar el proyecto paso a paso

### Opción A: con Android Studio (recomendada)

1. **Abrir el proyecto.** En Android Studio elige **File → Open...** y selecciona la carpeta `PetSociety` (la que contiene `settings.gradle`). Ábrela como proyecto propio, no como módulo.
2. **Esperar la sincronización.** Android Studio ejecuta *Gradle Sync* y descarga las dependencias. Espera a que termine la barra de progreso inferior.
   - Si aparece un aviso de que falta el SDK 34, haz clic en **Install** en el mismo aviso.
   - Si falla con *"SDK location not found"*, revisa el archivo `local.properties` (ver [Problemas frecuentes](#problemas-frecuentes)).
3. **Preparar un dispositivo:**
   - **Emulador:** abre **Tools → Device Manager → Create Device**, elige un teléfono (ej. *Pixel 6*) y una imagen de sistema con API 34 o 35, y pulsa **Finish**. Luego pulsa ▶ junto al emulador para encenderlo.
   - **Teléfono físico:** activa las *Opciones de desarrollador* (toca 7 veces *Número de compilación* en *Ajustes → Acerca del teléfono*), activa la **Depuración USB** y conéctalo por cable. Acepta el mensaje de autorización que aparece en el teléfono.
4. **Ejecutar.** En la barra superior, comprueba que la configuración seleccionada sea **app** y que el dispositivo correcto esté elegido. Luego pulsa **Run ▶** (`Shift + F10`).
5. La app se instala y abre sola. La primera vez verás el mensaje *"Tu inventario está vacío"*.

> **Cámara en el emulador:** por defecto, el emulador muestra una escena virtual en 3D como cámara trasera. Para fotografiar algo real, en el Device Manager edita el AVD, ve a **Show Advanced Settings → Camera** y elige **Webcam0** como cámara trasera.

### Opción B: desde la terminal

Desde la carpeta `PetSociety`:

```bash
# Windows (PowerShell o CMD)
gradlew.bat assembleDebug

# Git Bash / Linux / macOS
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en el emulador o teléfono conectado:

```bash
gradlew.bat installDebug
# o bien
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Si Gradle dice que no encuentra Java, puedes usar el JDK que trae Android Studio:
> - PowerShell: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
> - Git Bash: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`

### Pasarle el APK a otra persona

El archivo `app-debug.apk` se puede copiar a cualquier teléfono Android 7.0 o superior e instalarlo directamente. En el teléfono hay que permitir *"Instalar apps de origen desconocido"*.

---

## Cómo usar la app

### Agregar un producto
1. En la pantalla principal toca el botón **+** (abajo a la derecha).
2. Completa los campos:
   - **Nombre** (3 a 60 caracteres). Ej.: *Bravery Salmón Adulto 12 kg*
   - **Marca** (máx. 40 caracteres). Ej.: *Bravery*
   - **Categoría**: elígela de la lista desplegable.
   - **Precio**: número mayor que 0. Se aceptan decimales con punto o coma.
   - **Stock**: número entero entre 0 y 99.999.
3. *(Opcional)* Toca **Tomar foto**, saca la foto y confírmala con ✓.
4. Toca **Guardar producto**. Volverás a la lista con el producto agregado.

Si algún dato está mal, el campo se marca en rojo con una explicación, y el cursor salta al primer campo con error.

### Editar un producto
1. Toca el producto en la lista.
2. Cambia lo que necesites. Con **Cambiar foto** puedes reemplazar la imagen; la foto anterior se borra.
3. Toca **Guardar producto**.

Si sales con la flecha ← sin guardar, no se aplica ningún cambio.

### Eliminar un producto
1. Toca el producto en la lista.
2. Toca **Eliminar producto** y confirma en el cuadro de diálogo. Se borran tanto el producto como su foto.

### Leer la lista
- Cada fila muestra la foto, el nombre, la marca, la categoría, el precio y el stock.
- La etiqueta **Stock bajo** aparece cuando quedan 5 unidades o menos.
- El subtítulo de la barra superior resume el inventario, por ejemplo: *"12 productos · 2 con stock bajo"*.

---

## Cómo trabajar en el proyecto

Después de cualquier cambio en el código, vuelve a ejecutar con **Run ▶** (o `gradlew.bat installDebug`). Los datos ya guardados en el dispositivo se mantienen entre instalaciones.

### Agregar o cambiar categorías

Las categorías son una lista fija en **`app/src/main/java/com/petsociety/inventario/model/Product.kt`**:

```kotlin
val CATEGORIES = listOf(
    "Alimentos Premium",
    "Snacks",
    "Salud y Bienestar",
    "Higiene y Cepillos",
    "Juguetes",
    "Accesorios"
)
```

**Para agregar una categoría**, añade el texto a la lista, sin olvidar la coma:

```kotlin
val CATEGORIES = listOf(
    "Alimentos Premium",
    "Snacks",
    "Salud y Bienestar",
    "Higiene y Cepillos",
    "Juguetes",
    "Accesorios",
    "Camas y Transportadoras"   // ← nueva
)
```

Guarda el archivo y ejecuta la app. La nueva categoría aparece automáticamente en el desplegable del formulario y se acepta en la validación. **No hay que tocar la base de datos**, porque la categoría se guarda como texto.

**Para renombrar o quitar una categoría** hay que tener cuidado: los productos que ya tienen la categoría antigua la conservan en la base de datos. La lista los muestra igual, pero al editarlos la validación responde *"Categoría no válida"* hasta que se les asigne una categoría de la lista actual. Hay dos opciones:
- editar a mano esos productos y elegirles una categoría nueva, o
- migrarlos automáticamente con un bloque en `onUpgrade` (ver [la sección de migraciones](#agregar-un-campo-nuevo-al-producto-migrar-la-bd)), por ejemplo:
  ```kotlin
  if (oldVersion < 3) {
      db.execSQL("UPDATE products SET category = 'Salud' WHERE category = 'Salud y Bienestar'")
  }
  ```

### Cambiar el límite de stock bajo

En `Product.kt`:

```kotlin
const val LOW_STOCK_LIMIT = 5   // un producto con este stock o menos se marca como "Stock bajo"
```

### Cambiar las reglas de validación

Los límites están al principio de **`controller/ProductFormActivity.kt`**:

```kotlin
private const val NAME_MIN = 3            // largo mínimo del nombre
private const val NAME_MAX = 60           // largo máximo del nombre
private const val BRAND_MAX = 40          // largo máximo de la marca
private const val PRICE_MAX = 10_000_000.0
private const val STOCK_MAX = 99_999
```

La lógica de validación está en la función `validateInput()`. Cada campo sigue el mismo patrón:
1. limpiar con `.trim()`,
2. revisar si está vacío con `.isEmpty()`,
3. convertir el número con `.toDoubleOrNull()` o `.toIntOrNull()`,
4. revisar que esté dentro del rango,
5. si algo falla, mostrar el error con `binding.tilCampo.error = getString(R.string.error_...)`.

Los mensajes de error están en `res/values/strings.xml` (los que empiezan con `error_`).

### Agregar un campo nuevo al producto (migrar la BD)

Ejemplo: agregar un campo **"Código de barras"** (`barcode`). Se necesitan 6 pasos.

**1. Modelo**: en `Product.kt`, agrega la propiedad con un valor por defecto:
```kotlin
data class Product(
    ...
    val imagePath: String? = null,
    val barcode: String = ""          // ← nuevo
)
```

**2. Base de datos**: en `ProductDbHelper.kt`:
```kotlin
private const val DATABASE_VERSION = 3                 // ← subir de 2 a 3
private const val KEY_BARCODE = "barcode"              // ← nueva constante
```
- En `onCreate`, agrega la columna para las instalaciones nuevas:
  ```kotlin
  + "$KEY_IMAGE_PATH TEXT,"
  + "$KEY_BARCODE TEXT NOT NULL DEFAULT ''" + ")"
  ```
- En `onUpgrade`, agrega la migración para quienes ya tienen la app instalada:
  ```kotlin
  if (oldVersion < 3) {
      addColumnIfMissing(db, KEY_BARCODE, "TEXT NOT NULL DEFAULT ''")
  }
  ```
- En `toContentValues`, agrega `put(KEY_BARCODE, product.barcode)`.
- En `cursorToProduct`, agrega `barcode = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BARCODE))`.

> ⚠️ **Nunca bajes `DATABASE_VERSION` ni modifiques un bloque `if (oldVersion < N)` que ya se haya publicado.** Agrega siempre un bloque nuevo al final. Así, alguien que actualice desde la v1 pasa por todas las migraciones en orden, sin perder datos.

**3. Formulario (vista)**: en `res/layout/activity_product_form.xml`, copia el bloque `TextInputLayout` de la marca (`tilBrand`) y cambia los ids a `tilBarcode` / `etBarcode`.

**4. Textos**: en `res/values/strings.xml`, agrega `<string name="hint_barcode">Código de barras</string>`.

**5. Controlador**: en `ProductFormActivity.kt`:
- en `fillForm()`: `binding.etBarcode.setText(product.barcode)`
- en `validateInput()`: lee el campo con `.trim()`, valídalo y pásalo al crear el `Product(...)`
- en `allFields()`: agrega `binding.tilBarcode`, para que el error se borre al escribir.

**6. Lista** *(opcional)*: para mostrarlo en cada fila, agrega un `TextView` en `item_product.xml` y asígnale el valor en `ProductAdapter.bind()`.

### Cambiar textos, colores e íconos

| Qué | Dónde |
|---|---|
| Todos los textos de la app | `res/values/strings.xml` |
| Colores (principal, acento, stock bajo, etc.) | `res/values/colors.xml` |
| Tema general | `res/values/themes.xml` |
| Diseño de cada fila de la lista | `res/layout/item_product.xml` |
| Diseño del formulario | `res/layout/activity_product_form.xml` |
| Ícono de la app | `res/drawable/ic_launcher_foreground.xml` y `ic_launcher_background.xml` |
| Formato del precio (moneda) | `PRICE_FORMAT` en `view/ProductAdapter.kt` (`Locale("es", "CL")`) |

---

## Revisar la base de datos

Con la app abierta en el emulador o teléfono, en Android Studio abre **View → Tool Windows → App Inspection → Database Inspector**. Selecciona el proceso `com.petsociety.inventario` y la base **PetSocietyDB → products**. Desde ahí puedes ver los datos en vivo y ejecutar consultas SQL.

Desde la terminal (la app debe ser *debug*):

```bash
adb shell run-as com.petsociety.inventario ls databases files/images
```

Para **borrar todos los datos** y empezar de cero: *Ajustes → Apps → Pet Society → Almacenamiento → Borrar datos*, o bien `adb shell pm clear com.petsociety.inventario`.

---

## Problemas frecuentes

| Problema | Solución |
|---|---|
| `SDK location not found` o `Invalid file path` | Crea o edita `local.properties` en la raíz con la ruta del SDK **usando barras normales**: `sdk.dir=C:/Users/TU_USUARIO/AppData/Local/Android/Sdk` |
| `JAVA_HOME is not set` al usar `gradlew` | Apunta `JAVA_HOME` al JDK de Android Studio (ver [Opción B](#opción-b-desde-la-terminal)). |
| *"No hay una aplicación de cámara disponible"* | El dispositivo no tiene app de cámara. En el emulador, usa una imagen *Google APIs* o *Google Play*. |
| La cámara del emulador muestra una sala 3D | Es la cámara virtual. Cambia la cámara trasera a *Webcam0* en la configuración del AVD. |
| El emulador va muy lento o se cierra | Cierra otros programas pesados. También puedes bajar la RAM del AVD o usar un teléfono físico. |
| Cambié el esquema y la app se cierra al abrir | Probablemente olvidaste subir `DATABASE_VERSION` o agregar el bloque en `onUpgrade`. Mientras desarrollas, también puedes borrar los datos de la app. |
| Error de compilación con `binding.xxx` | El id del XML no coincide. ViewBinding convierte `et_name` / `etName` en `binding.etName`. Haz **Build → Rebuild Project**. |
