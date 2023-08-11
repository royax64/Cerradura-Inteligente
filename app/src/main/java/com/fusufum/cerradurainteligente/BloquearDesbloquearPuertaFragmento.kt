package com.fusufum.cerradurainteligente

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.ingenieriajhr.blujhr.BluJhr
import com.fusufum.cerradurainteligente.databinding.BloquearDesbloquearPuertaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class BloquearDesbloquearPuertaFragmento : Fragment() {

    private lateinit var binding: BloquearDesbloquearPuertaBinding
    lateinit var bluetooth:  BluJhr


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val obtenerDatos: PasaDatos = this.activity as PasaDatos
        bluetooth = obtenerDatos.getDatos() as BluJhr
        bluetooth.bluTx("1")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = BloquearDesbloquearPuertaBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {       //Aqui puedes añadir la logica del fragmento
        super.onViewCreated(view, savedInstanceState)

        binding.aceptarBotton.setOnClickListener {  //Si presionamos el botón aceptar (ver layout/bloquear_desbloquear_puerta.xml
            this.context?.let { Toast.makeText(this.context, "La puerta está cerrada.", Toast.LENGTH_SHORT).show() }
            findNavController().popBackStack()  //Nos regresamos el menu princpal

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        bluetooth.bluTx("0")
    }


}