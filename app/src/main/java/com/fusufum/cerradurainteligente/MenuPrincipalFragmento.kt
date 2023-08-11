package com.fusufum.cerradurainteligente

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fusufum.cerradurainteligente.databinding.MenuPrincipalEstiloBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*
import com.ingenieriajhr.blujhr.BluJhr
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.*

//Aqui se modifica el codigo para el menu principal
//Usen la variable binding para referenciar a botones y otros elementos de la pantalla.
//Desde aqui pueden mandar a llamar a un fragmento (una vez declarado y una vez creada la acción de navegación en nav_graph.xml )
//La lógica para la mayoría del programa va a estar aquí (excepto para la Introducción, eso está en el paquete inital_Steps)

class MenuPrincipalFragmento : Fragment() {

    private lateinit var binding: MenuPrincipalEstiloBinding
    private lateinit var database: DatabaseReference
    private lateinit var cameraExecutor: ExecutorService
    private var capturaimg: ImageCapture? = null
    private lateinit var fotoDePersona: File
    private lateinit var personaSeleccionada: Persona
    private var personas = mutableListOf<Persona>()
    private var fotoDePersonaBase64 = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MenuPrincipalEstiloBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        personaSeleccionada = Persona("noPersona", "")
        database = FirebaseDatabase.getInstance().reference //Instancia de la base de datos
        getPersonas() //Obteniendo personas de la nube

        //ARRANCANDO CAMARA
        cameraExecutor = Executors.newSingleThreadExecutor()
        arrancarCamara()



        binding.unlockButton.setOnClickListener {
            //Limpiando algunas variables
            cleanup()

            //Si la persona todavía no ha seleccionado una persona
            try {
                if (personaSeleccionada.nombre == "noPersona") throw Exception("No persona seleccionada")
            } catch (e: Exception) {
                mostrarAviso(
                    "Selecciona una persona",
                    "Primero seleccione a una persona antes de proceder al desbloqueo."
                )
                Log.e("ERROR", "PERSONA NO SELECCIONADA")
                return@setOnClickListener
            }

            var isDoorAllowedUnlocking: Boolean? = false

            val popup: AlertDialog? = mostrarAviso("Espere", "Estamos analizando su rostro...")

            tomarFoto(
                fun(): Unit{ //Tomando la foto
                    try{ isDoorAllowedUnlocking = checkFaces(fotoDePersonaBase64, personaSeleccionada.foto) }  //Checando las caras
                    catch(e: Exception) { popup?.dismiss(); mostrarAviso("No se ha detectado ninguna cara", "Compruebe que tenga buena iluminación alrededor, no mueva el teléfono hasta que el proceso acabe."); return; } //Si algo falla mandaremos aviso

                    //Nos vamos al fragmento de BloquearSesbloquearPuerta
                    //Callback dentro de otro callback
                    val callback = popup?.dismiss(); if (isDoorAllowedUnlocking == true) { findNavController().navigate(R.id.action_menuprincipal_a_bloqueardesbloquear) }   //Nos vamos al fragmento de BloquearSesbloquearPuerta
                                                     else { mostrarAviso("Las caras no coinciden", "Compruebe que no tenga obstrucciones en la cara ni modificaciones en esta.") }  //SI algo falla mandaremos aviso
                }
            )

        }

        binding.addnewfaceButton.setOnClickListener {//Lo mismo que arriba
            findNavController().navigate(R.id.action_menuprincipal_a_anyadircara)
        }

        //Arreglo de strings de nombres, tendrán que obtener esto de la base de datos

        binding.whoareyouButton.setOnClickListener { //Cuando presionemos el botón de seleciconar o eliminar caras se muestra la lista de personas
            crearListaPersonas(
                personas,
                true
            ) //definición de función más abajo, el primer parámetro es el arreglo, el segundo es la acción (les recomiendo no modificar)
        }

        binding.whoareyouPic.setOnClickListener {//También la imagen es presionable
            crearListaPersonas(
                personas,
                true
            ) //definición de función más abajo, el primer parámetro es el arreglo, el segundo es la acción (les recomiendo no modificar)
        }

        binding.deletefaceButton.setOnClickListener {//Cuando presionemos el botón de seleciconar o eliminar caras se muestra la lista de personas
            crearListaPersonas(
                personas,
                false
            ) //definición de función más abajo, el primer parámetro es el arreglo, el segundo es la acción (les recomiendo no modificar)
        }


    }

    //Esta función va a mostrar la lista de personas en un popup dialog para escoger cual es la persona que estamos detectando en el reconocimiento facial o la que vamos a eliminar
    //Osea, se abre el mismo menú para ambos botones pero el parámetro acción determina si seleccionar o eliminar la persona
    //Les recomiendo no modificar esa función a menos que sea necesario
    private fun crearListaPersonas(
        personas: MutableList<Persona>,
        accion: Boolean
    ) { //De parámetros el arreglo de personas y la acción a realizar

        if (personas.isEmpty()) {
            mostrarAviso("Error, no se puede conectar al internet.","Porfavor, intente conectarse al internet o intente pulsar el botón de nuevo.")
            return
        }

        //Hay 2 arreglos, uno de nombres de personas y otro de fotos de las personas
        val arrNombres = procesarPersonas(personas)

        this.context?.let {
            MaterialAlertDialogBuilder(it)      //Comanzamos a declarar el pop-up
                .setTitle("Selecciona una persona") //El titulo del pop-up
                .setSingleChoiceItems(arrNombres, -1)
                { p0, posicion ->
                    if (accion) seleccionarPersonasDialogo(personas[posicion]) else eliminarPersonasDialogo(
                        posicion
                    ); p0.dismiss()
                }
                //Si acción = true entonces seleccionamos esa persona, pero si acción = false entonces la eliminamos.
                .show() //Mostrar al usuario
        }

    }

    //Aquí está la lógica para seleccionar una persona para compararla con la foto a tomar, obviamente se va a modificar acorde a lo que se tenga que hacer,
    //incluyendo cambiar la fotografía de la persona y su nombre en el botón pero el código para hacer esto se encuentra al final de la función.

    //Por cierto, en el archivo menu_principal_estilo.xml se pueden dar cuenta que el contenedor de la camara es un ConstraintLayout, nomás borra ese TextView dentro y reemplazalo
    //por la vista de la camara.
    private fun seleccionarPersonasDialogo(persona: Persona) {    //Toma como parámetro la posición de la persona seleccionada en el menú
        personaSeleccionada = persona
        binding.whoareyouButton.text = persona.nombre //Mostramos el nombre de la cara que estamos detectando, aquí va el nombre de la persona para que aparezca en el menú
        binding.whoareyouPic.imageTintList = null  //Para que no se vea azulada la foto
        binding.whoareyouPic.setImageDrawable(base64aFoto(persona.foto)) //Ponemos su foto en el contenedor. El argumento es un objeto tipo drawable, probablemente ocupan uno que otro casteo.
        binding.whoareyouPic.rotation = -90F
        binding.whoareyouPic.scaleX = 1.33F
        binding.whoareyouPic.scaleY = 1.33F
    }

    private fun eliminarPersonasDialogo(posicion: Int) {
        this.context?.let {         //Aqui vamos a mostrar un popup alert cuando nos estemos regresando al menu principal
            MaterialAlertDialogBuilder(it)
                .setTitle("Desea eliminar a ${personas[posicion].nombre}?")     //Establecemos el titulo
                .setPositiveButton("Aceptar") { _, _ ->
                    val query = database.child("personas").child(personas[posicion].nombre)
                    query.removeValue()
                    Toast.makeText(this.context, "Se ha eliminado esta cara con éxito.", Toast.LENGTH_SHORT).show()
                    getPersonas()
                }
                .show()
        }
    }

    private fun checkFaces(fotoTomada: String, fotoAComparar: String): Boolean? {
        if (fotoAComparar == "" || fotoTomada == "") return false
        val compararcaras = this.context?.let { CompararPersonas(fotoTomada, fotoAComparar, it) }
        return compararcaras?.compararCaras()
    }

    private fun arrancarCamara() {
        val cameraProviderFuture = this.context?.let { ProcessCameraProvider.getInstance(it) }

        this.context?.let { ContextCompat.getMainExecutor(it) }?.let { it ->
            cameraProviderFuture?.addListener({

                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.camaracontainer.surfaceProvider)
                    }


                // Selecciona la camara frontal
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                capturaimg = ImageCapture.Builder().build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, capturaimg
                    )

                } catch (exc: Exception) {
                    Log.e("Use case binding failed", "No arrancó camara")
                }
            }, it)
        }
    }

    private fun tomarFoto(callback: () -> Unit) {
        val capturaimg = capturaimg ?: return

        val name = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.US
        ).format(System.currentTimeMillis())

        val foto = File(this.context?.filesDir?.path, "${name}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(foto)
            .build()


        this.context?.let { ContextCompat.getMainExecutor(it) }?.let {
            capturaimg.takePicture(
                outputOptions,
                it,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("Error de foto", "Captura fallida: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        fotoDePersona = foto
                        fotoDePersonaBase64 = fotoABase64(fotoDePersona)
                        val msg = "Photo capture succeeded: ${output.savedUri}"
                        Log.e("Foto exitosa", msg)
                        callback()
                    }
                }
            )
        }
    }

    private fun getPersonas() {
        personas.clear()
        try {
            database.get().addOnSuccessListener { it ->
                it.child("personas").children.forEach {
                    val persona: Persona? =
                        it.key?.let { it1 -> Persona(it1, it.child("").value as String) }
                    persona?.let { it1 -> personas.add(it1) }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this.context,
                "Hubo un error al conectarse a la base de datos, por favor conéctese a internet",
                Toast.LENGTH_SHORT
            ).show()
            e.message?.let { Log.e("ERRORDB", it) }
            this.activity?.finish()
        }
    }

    private fun procesarPersonas(personas: MutableList<Persona>): Array<String> {
        val nombres: MutableList<String> = mutableListOf()

        personas.iterator().forEach {
            nombres.add(it.nombre)
        }
        return nombres.toTypedArray()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun fotoABase64(imagen: File): String {
        val bitmap: Bitmap = BitmapFactory.decodeFile(imagen.absolutePath)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
        return Base64.encode(out.toByteArray(), 0, out.toByteArray().size)
    }

    fun base64aFoto(foto: String): Drawable {
        val inputbytes = java.util.Base64.getDecoder().decode(foto)
        val bitmap = BitmapFactory.decodeByteArray(inputbytes, 0, inputbytes.size)
        return BitmapDrawable(resources, bitmap)
    }

    private fun mostrarAviso(
        titulo: String,
        subtitulo: String
    ): AlertDialog? {
        val aviso = this.context?.let {
                MaterialAlertDialogBuilder(it)
                    .setTitle(titulo)
                    .setMessage(subtitulo)
                    .show()
        }; return aviso
    }

    private fun cleanup() {
        fotoDePersona = File("")
        fotoDePersonaBase64 = ""
    }


}