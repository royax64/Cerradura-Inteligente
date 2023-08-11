package com.fusufum.cerradurainteligente.initial_steps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fusufum.cerradurainteligente.R

//Esta clase va a tomar 3 listas, una para los titulos, otra para los subtitulos y la tercera para imagenes que queramos que aparezcan durante la introducción
//Básicamente, va a tomar cada elemento de la lista y la va a "insertar" dentro de los objetos llamado "titulo", "subtitulo" e "imagen" presentes en initial_steps_guide.xml
class SlidePageAdapter(private var titulo: MutableList<String>, private var subtitulo: MutableList<String>, private var imagen: MutableList<Int>) : RecyclerView.Adapter <SlidePageAdapter.Pager2ViewHolder>(){


    inner class Pager2ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val itemtitulo: TextView = itemView.findViewById(R.id.titulo)
        val itemsubtitulo: TextView = itemView.findViewById(R.id.subtitulo)
        val itemimagen: ImageView = itemView.findViewById(R.id.imagen)



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Pager2ViewHolder {
        return Pager2ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.initial_steps_guide,parent,false))
    }

    override fun getItemCount(): Int {
        return titulo.size
    }

    override fun onBindViewHolder(holder: Pager2ViewHolder, position: Int) {
        holder.itemtitulo.text = titulo[position]
        holder.itemsubtitulo.text = subtitulo[position]
        holder.itemimagen.setImageResource(imagen[position])


    }


}
