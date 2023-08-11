package com.fusufum.cerradurainteligente

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.ingenieriajhr.blujhr.BluJhr
import com.fusufum.cerradurainteligente.databinding.ActivityMainBinding
import com.fusufum.cerradurainteligente.initial_steps.Introduction
import com.google.android.material.dialog.MaterialAlertDialogBuilder


//En la actividad principal esta el codigo para la creación de la barra superior (Material Toolbar) y el contenedor de los fragmentos (todas las pantallas de la aplicación)
//Para las personas que les tocó "Añadir Cara", "Bloquear o Desbloquear puerta" o "Introduccción": para modificar la lógica de sus partes busquen la clase correspondiente, si buscan modificar su aspecto busquen en res/drawable/layout el xml correspondiente a la clase;
//si buscan crear una nueva pantalla (fragmento), vayan a res/navigation/nav_graph.xml para declararla.
//Si te tocó cualquier otra parte entonces tu lógica está en MenuPrincipalFragmento.kt
//Aun así les recomiendo a todos leer los comentarios de MenuPrincipalFragmento.kt para que vean como está estructurada la aplicación.


class MainActivity : AppCompatActivity(), PasaDatos {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var direccion = ""
    lateinit var bluetooth: BluJhr
    private var estadoConexion = BluJhr.Connected.False


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)   //Obteniendo las vistas
        setContentView(binding.root)

        setSupportActionBar(binding.materialToolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)  //Obteniendo el contenedor de fragmentos
        appBarConfiguration = AppBarConfiguration(navController.graph)              //Aplicando el grafo de navegación
        setupActionBarWithNavController(navController, appBarConfiguration)

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        if (!checarPermisos()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        direccion = intent.getStringExtra("direccion").toString()

        //Configurando bluetooth y el estado de la conexion
        bluetooth = BluJhr(this)

        bluetooth.setDataLoadFinishedListener(object:BluJhr.ConnectedBluetooth{
            override fun onConnectState(state: BluJhr.Connected) {
                when (state) {
                    BluJhr.Connected.True -> {
                        Toast.makeText(applicationContext, "Se ha conectado correctamente a la cerradura", Toast.LENGTH_SHORT).show()
                        estadoConexion = state
                        bluetooth.bluTx("0")   //Cerrando la puerta al iniciar
                    }
                    BluJhr.Connected.Pending -> {
                        Toast.makeText(applicationContext, "Espere un momento, la cerradura se está conectando ", Toast.LENGTH_SHORT).show()
                        estadoConexion = state
                    }
                    BluJhr.Connected.False -> {
                        Toast.makeText(applicationContext, "La cerradura no está conectada, verifique la integridad de esta.", Toast.LENGTH_SHORT).show()
                        estadoConexion = state
                    }
                    BluJhr.Connected.Disconnect -> {
                        Toast.makeText(applicationContext, "Se ha desconectado la cerradura", Toast.LENGTH_SHORT).show()
                        estadoConexion = state
                        startActivity(Intent(applicationContext,Introduction::class.java))
                    }
                }
            }
        })

    }

    override fun getDatos(): Any {
        return bluetooth
    }

    override fun onResume() {
        super.onResume()
        if (estadoConexion != BluJhr.Connected.True){
            bluetooth.connect(direccion)
        }
    }

    override fun onPause() {
        super.onPause()
        bluetooth.closeConnection()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!checarPermisos()) {
                Toast.makeText(this,
                    "Permisos no concedidos",
                    Toast.LENGTH_SHORT).show()
                finish()
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
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
            }.toTypedArray()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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