package com.fusufum.cerradurainteligente.initial_steps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.fusufum.cerradurainteligente.MainActivity
import com.fusufum.cerradurainteligente.R
import com.fusufum.cerradurainteligente.databinding.IntroduccionActividadBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ingenieriajhr.blujhr.BluJhr
import me.relex.circleindicator.CircleIndicator3

//Más abajo está la lista de titulos, subtitulos, imagenes y botones que queremos que se muestren durante la introducción de la app.
//Aqui se puede añadir la logica para la conexión a bluetooth
//Si crees que sea necesario modificar la implementación, adelante.

//Esta clase declara una lista de elementos los cuales, con la ayuda de SlidePageAdapter.kt y de viewpager, va a mostrar varias pantallas para el setup de la aplicación
//Osea, debe conectarse al arduino a bluetooth a la aplicación.

//Esta implementación trabaja usando "vistas", pero si crees que sea necesario puede hacer que trabaje usando "fragmentos".

//La declaración de los elementos de la pantalla se encuentran en introduccion_actividad.xml (aqui está el botón siguiente, los circulos indicadores de progreso y el viewpager)
//Y las pantallas de la configuración están en initial_steps_guide.xml (lo que cambia).
class Introduction : AppCompatActivity() {

    private var listatitulos = mutableListOf<String>()
    private var listasubtitulos = mutableListOf<String>()
    private var listaimagenes = mutableListOf<Int>()
    private lateinit var binding: IntroduccionActividadBinding
    lateinit var bluetooth: BluJhr

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mandandoInfo() //Lo que viene en cada pagina

        binding =
            IntroduccionActividadBinding.inflate(layoutInflater)   //binding es un objeto que contiene todas las vistas de activity_introduction.xml
        setContentView(binding.root)

        val viewpager =
            binding.viewPager   //viewpager contiene el objeto ViewPager en activity_introduction.xml
        viewpager.adapter = SlidePageAdapter(
            listatitulos,
            listasubtitulos,
            listaimagenes
        )    //Añadiendolas para que agarre

        val indicator3: CircleIndicator3 =
            findViewById(R.id.indicador)     //Haciendo lo mismo con el objeto indicador en activity_introduction
        indicator3.setViewPager(viewpager)

        val botonSiguiente =
            binding.botonSiguiente       //Obteniendo el botón siguiente en activity_introduction


        if (!checarPermisos()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }



        viewpager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() { //Para que abran las funciones cuando se deslize el dedo
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                whatToDo(viewpager)
            }
        })

        botonSiguiente.setOnClickListener {//Lo que se va a hacer al presionar el botón
            when (viewpager.currentItem) {
                0 -> {
                    viewpager.apply {           //Aqui se hace el swipe falso al presionar el botón "Continuar"
                        beginFakeDrag()
                        fakeDragBy(-800f)
                        endFakeDrag()
                    }

                }
                1 -> {
                    mostrarPopup()
                }
            }

        }

    }


    private fun mandandoInfo() {     //Aqui se modifica el texto.
        //, hay que cambiar este texto al que queremos que se muestre en la aplicación
        listatitulos.add(0, "Te damos la bienvenida a la app de Cerradura Inteligente!")
        listatitulos.add(1, "Configura y activa Bluetooth")
        listasubtitulos.add(
            0,
            "En esta sección podrás configurar correctamente tu nueva cerradura inteligente, haga click en \"Continuar\" para continuar."
        )
        listasubtitulos.add(
            1,
            "Es necesario activar el Bluetooth para que la app funcione, por favor, actívelo cuando se le pida.\nAl habilitar presione nuestro dispositivo, una vez establecida la conexión se le mandará al menú principal."
        )
        listaimagenes.add(
            0,
            R.drawable.ic_launcher_foreground
        )  //Pongan imagenes vectoriales (.xml) o png para que se vea mas chido
        listaimagenes.add(
            1,
            R.drawable.step_2_figure
        )  //La pones en la carpeta app/res/drawable y luego sustituyes ic_launcher_foreground por el nombre de tu imagen
    }

    private fun mostrarPopup() {
        val fragmentManager = supportFragmentManager
        val newFragment = ListaBluetoothFragmento()

        //Popup chico
        newFragment.show(fragmentManager, "dialog")

    }

    private fun whatToDo(viewpager: ViewPager2) { //Todas las funciones que se mandan a llamar en cada una de las páginas, la ultima página no aparece porque será necesario que se apriete el botón.
        when (viewpager.currentItem) {
            0 -> {
                //Pagina 1
                bluetooth = BluJhr(this)
                bluetooth.onBluetooth()
            }
            1 -> {
                //Pagina 2
                mostrarPopup()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!checarPermisos()) {
                try {
                    bluetooth.initializeBluetooth()
                } catch (e: Exception) {
                    mostrarAviso(
                        "Faltan Permisos",
                        "Ingrese a configuración -> Aplicaciones -> Cerradura Inteligente -> Permisos y autorize todos los permisos solicitados."
                    )
                }
            }
        }
    }

    private fun checarPermisos() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT
            ).apply {
            }.toTypedArray()
    }


    private fun mostrarAviso(
        titulo: String,
        subtitulo: String
    ): AlertDialog? {
        val aviso = this.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(titulo)
                .setMessage(subtitulo)
                .show()
        }; return aviso
    }
}


