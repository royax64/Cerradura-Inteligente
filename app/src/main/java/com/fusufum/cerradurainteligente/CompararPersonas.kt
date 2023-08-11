package com.fusufum.cerradurainteligente


import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.CompareFacesMatch
import com.amazonaws.services.rekognition.model.CompareFacesRequest
import com.amazonaws.services.rekognition.model.Image
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.runBlocking

//Aqui se realizan todas las operaciones de comparar caras
val MINIMO_DE_SIMILITUD = 95F

class CompararPersonas (fotoTomada: String, fotoComparar: String, contexto: Context){

    val fotoT = base64aImage(fotoTomada, contexto)
    val fotoC = base64aImage(fotoComparar, contexto)

    val solicitud = CompareFacesRequest()
    val credenciales = BasicAWSCredentials("UR_OWN_CREDENTIALS","UR_OWN_CREDENTIALS")
    lateinit var resultado: List<CompareFacesMatch>

    fun compararCaras(): Boolean? {
        solicitud.sourceImage = fotoC
        solicitud.targetImage = fotoT
        solicitud.similarityThreshold = MINIMO_DE_SIMILITUD
        val cliente =  AmazonRekognitionClient(credenciales)
        cliente.setRegion(Region.getRegion(Regions.US_EAST_2))

        resultado = cliente.compareFaces(solicitud).faceMatches
        Log.e("INFORMACION AWS", resultado.toString())

        if (resultado != null){
            for (i: CompareFacesMatch in resultado){
                val cara = i.face
                val posicionCara = cara?.boundingBox
                if (posicionCara != null){
                    return cara.confidence?.let { it1 -> sentenciarResultado(it1) }
                }else return false
            }
        }
        return false
    }
}

private fun base64aImage(foto: String, contexto: Context): Image? {
    try {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val decodedBytes = Base64.getDecoder().decode(foto)
        val file = File(contexto.filesDir.path,"${name}.jpg")
        file.writeBytes(decodedBytes)

        val byteBuffer = ByteBuffer.allocate(file.length().toInt())
        val filestream = FileInputStream(file)

        filestream.use {
            byteBuffer.put(it.readBytes())
        }
        byteBuffer.flip()

        val imagen: Image = Image()
        imagen.bytes = byteBuffer
        return imagen

    } catch (e: Exception) {
        e.message?.let { Log.e("ERROR DECODIFICACION BASE 64 A IMAGE", it) }
        return null
    }
}

private fun sentenciarResultado(confianza: Float): Boolean{
    return confianza > MINIMO_DE_SIMILITUD
}
