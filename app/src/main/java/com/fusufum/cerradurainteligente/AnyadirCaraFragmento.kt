package com.fusufum.cerradurainteligente

import android.os.Bundle
import android.text.SpannableString
import android.text.style.BulletSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fusufum.cerradurainteligente.databinding.AnyadirCaraBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/*
                Base64aFoto(fotoDePersonaBase64)

                database = FirebaseDatabase.getInstance().getReference()

                val juanito = Persona("juanito", fotoDePersonaBase64)
                //Log.d("OBJETO PERSONA",juanito.nombre+juanito.foto)
                database.child("personas").child(juanito.nombre).setValue(juanito.foto)
 */

class AnyadirCaraFragmento : Fragment() {

    private lateinit var binding: AnyadirCaraBinding
    private lateinit var database: DatabaseReference
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var capturaimg: ImageCapture
    private var nombreDePersona: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AnyadirCaraBinding.inflate(inflater, container, false)
        return binding.root


    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) { //Aqui puedes añadir la logica del fragmento
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance().reference //Instancia de la base de datos
        cameraExecutor = Executors.newSingleThreadExecutor()
        arrancarCamara()


        //Haciendo que se vean bonitas las instrucciones
        val bulletedList = listOf(
            "Ingrese su nombre en el campo de texto inferior\n",
            "Asegurese que su cara sea visible y libre de obstrucciones.\n",
            "Mire a la cámara hasta que se le indique.\n",
            "Bienvenido a Cerradura Inteligente!\n"
        ).toBulletedList()
        binding.SecondText.text = bulletedList


        binding.aceptarBoton.setOnClickListener {  //Si presionamos el botón aceptar (ver layout/anyadir_cara.xml
            val popup: androidx.appcompat.app.AlertDialog? = mostrarAviso("Espere","Estamos resgistrando su cara")

            try {
                nombreDePersona =
                    binding.TextField.text.toString() //Obteniendo el nombre de la persona
                if (nombreDePersona == "") throw Exception("No se ha escrito un nombre")

                //Tomando foto y guardando en la db
                tomarFoto( fun(): Unit {
                    popup?.dismiss()
                    mostrarAviso("Persona añadida con éxito","Ahora puedes seleccionar tu cara para usar la Cerradura Inteligente")
                    findNavController().popBackStack()  //Nos regresamos el menu princpal
                })

            } catch (e: Exception) {
                popup?.dismiss()
                e.message?.let { it1 -> mostrarAviso("Error al registrar cara", it1) }
                return@setOnClickListener
            }

        }

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
                        throw Exception("Error en la captura de la foto, reinicie la aplicación")
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val msg = "Photo capture succeeded: ${output.savedUri}"
                        Log.e("Foto exitosa", msg)

                        val fotoDePersonaBase64 = MenuPrincipalFragmento().fotoABase64(foto)
                        val persona = Persona(nombreDePersona, fotoDePersonaBase64)
                        Log.e("OBJETO PERSONA", persona.nombre + persona.foto)
                        database.child("personas").child(persona.nombre)
                            .setValue(persona.foto) //Subiendo a

                        val mensaje = "Database exitoso!"
                        Log.e("Foto exitosa", mensaje)

                        callback() //Haciendo el resto de cosas

                        fun debugearCodigo() {
                            Log.e("binding", binding.toString())
                            Log.e("database", database.toString())
                            Log.e("cameraExecutor", cameraExecutor.isShutdown.toString())
                            Log.e("capturaimg", capturaimg.toString())
                            Log.e("fotoDePersona", foto.path)
                            Log.e("fotoDePersonaBase64", fotoDePersonaBase64)
                        }
                        debugearCodigo()


                    }
                }
            )
        }

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

}


//Función que imprime una lista tabulada en un TextView.
fun List<String>.toBulletedList(): CharSequence {
    return SpannableString(this.joinToString("\n")).apply {
        this@toBulletedList.foldIndexed(0) { index, acc, span ->
            val end = acc + span.length + if (index != this@toBulletedList.size - 1) 1 else 0
            this.setSpan(BulletSpan(16), acc, end, 0)
            end
        }
    }
}






