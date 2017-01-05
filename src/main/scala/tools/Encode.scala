package tools

import org.apache.commons.codec.binary.Base32

object Encode extends App with TOTP {
  def encodeSecret(secret: String): String = new String(new Base32().encode(secret.getBytes))

  if (args.length < 1) println("Secret is missing.")
  else println(encodeSecret(args(0)))
}
