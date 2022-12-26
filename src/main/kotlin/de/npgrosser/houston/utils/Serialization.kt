package de.npgrosser.houston.utils

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream


fun serialize(obj: Any?): ByteArray {
    val bos = ByteArrayOutputStream()
    val out = ObjectOutputStream(bos)
    out.writeObject(obj)
    out.close()
    return bos.toByteArray()
}

fun deserialize(bytes: ByteArray): Any? {
    val bis = bytes.inputStream()
    val ois = java.io.ObjectInputStream(bis)
    val obj = ois.readObject()
    ois.close()
    return obj
}