package net.debasishg.domain.trade
package util

import java.io._

object Serialization {
  def serialize(obj: AnyRef) = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)

    oos.writeObject(obj)
    oos.flush()
    bos.toByteArray
  }

  def deserialize(bytes: Array[Byte]) = {
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis)

    ois.readObject()
  }
}

