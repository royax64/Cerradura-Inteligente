package com.fusufum.cerradurainteligente

import android.graphics.Bitmap

//Esta clase es para las personas de la aplicación, hay un id, un nombre y su foto de la cara en formato Base64. Pueden usar esta clase para la lógica de la aplicación
data class Persona(val nombre: String, val foto: String)
