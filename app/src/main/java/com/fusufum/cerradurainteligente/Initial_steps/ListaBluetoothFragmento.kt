package com.fusufum.cerradurainteligente.initial_steps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.fusufum.cerradurainteligente.MainActivity
import com.fusufum.cerradurainteligente.PasaDatos
import com.fusufum.cerradurainteligente.databinding.FragmentListaBluetoothFragmentoBinding
import com.ingenieriajhr.blujhr.BluJhr


class ListaBluetoothFragmento : DialogFragment(){

    private lateinit var binding: FragmentListaBluetoothFragmentoBinding
    lateinit var bluetooth: BluJhr
    private var dispositivos = ArrayList<String>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentListaBluetoothFragmentoBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    @SuppressLint("UseRequireInsteadOfGet")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Mostrando la lista de bluetooth
        bluetooth = this.context?.let { BluJhr(it) }!!
        bluetooth.onBluetooth()

        if(!bluetooth.stateBluetoooth()) bluetooth.initializeBluetooth()
        else {
            dispositivos = bluetooth.deviceBluetooth()
            if(dispositivos.isNotEmpty()){
                val adaptador = ArrayAdapter(this.context!!, android.R.layout.simple_list_item_1, dispositivos)
                binding.deviceList.adapter = adaptador
            }else{
                Toast.makeText(this.context, "No tienes vinculados dispositivos", Toast.LENGTH_SHORT).show()
            }

            binding.deviceList.setOnItemClickListener{adapterview, vista, i, l ->
                val intent = Intent(this.context,MainActivity::class.java)
                intent.putExtra("direccion", dispositivos[i] )
                startActivity(intent)
            }
        }


    }

}