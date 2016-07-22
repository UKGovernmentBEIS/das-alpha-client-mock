/*
 * Copyright (c) 2011 IETF Trust and the persons identified as
 * authors of the code. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted pursuant to, and subject to the license
 * terms contained in, the Simplified BSD License set forth in Section
 * 4.c of the IETF Trust's Legal Provisions Relating to IETF Documents
 * (http://trustee.ietf.org/license-info).
 *//*
 * Copyright (c) 2011 IETF Trust and the persons identified as
 * authors of the code. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted pursuant to, and subject to the license
 * terms contained in, the Simplified BSD License set forth in Section
 * 4.c of the IETF Trust's Legal Provisions Relating to IETF Documents
 * (http://trustee.ietf.org/license-info).
 */
package tools

import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.security.GeneralSecurityException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.util.{Failure, Try}

/**
  * This is an example implementation of the OATH
  * TOTP algorithm.
  * Visit www.openauthentication.org for more information.
  *
  * @author Johan Rydell, PortWise, Inc.
  */
object TOTP {

  implicit class StringSyntax(s: String) {
    def prePad(n: Int, c: Char): String = s.reverse.padTo(n, c).reverse
  }

  /**
    * This method uses the JCE to provide the crypto algorithm.
    * HMAC computes a Hashed Message Authentication Code with the
    * crypto hash algorithm as a parameter.
    *
    * @param crypto   :   the crypto algorithm (HmacSHA1, HmacSHA256,
    *                 HmacSHA512)
    * @param keyBytes : the bytes to use for the HMAC key
    * @param text     :     the message or text to be authenticated
    */
  private def hmac_sha(crypto: String, keyBytes: Array[Byte], text: Array[Byte]): Array[Byte] = {
    val hmac = Mac.getInstance(crypto)
    hmac.init(new SecretKeySpec(keyBytes, "RAW"))
    hmac.doFinal(text)
  }

  /**
    * This method converts a HEX string to Byte[]
    *
    * @param hex : the HEX string
    * @return a byte array
    */
  private def hexStr2Bytes(hex: String): Array[Byte] = {
    // Adding one byte to get the right conversion
    // Values starting with "0" can be converted
    new BigInteger("10" + hex, 16).toByteArray.drop(1)
  }

  private val DIGITS_POWER: Array[Int] = // 0 1  2   3    4     5      6       7        8
    Array(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

  /**
    * This method generates a TOTP value for the given
    * set of parameters.
    *
    * @param key          :          the shared secret, HEX encoded
    * @param time         :         a value that reflects a time
    * @param returnDigits : number of digits to return
    * @return a numeric String in base 10 that includes
    *         {truncationDigits} digits
    */
  def generate(key: String, time: String, returnDigits: Int): String =
  generate(key, time, returnDigits, "HmacSHA1")

  /**
    * This method generates a TOTP value for the given
    * set of parameters.
    *
    * @param key          :          the shared secret, HEX encoded
    * @param time         :         a value that reflects a time
    * @param returnDigits : number of digits to return
    * @return a numeric String in base 10 that includes
    *         {truncationDigits} digits
    */
  def generate256(key: String, time: String, returnDigits: Int): String =
  generate(key, time, returnDigits, "HmacSHA256")

  /**
    * This method generates a TOTP value for the given
    * set of parameters.
    *
    * @param key          :          the shared secret, HEX encoded
    * @param time         :         a value that reflects a time
    * @param returnDigits : number of digits to return
    * @return a numeric String in base 10 that includes
    *         {truncationDigits} digits
    */
  def generate512(key: String, time: String, returnDigits: Int): String =
  generate(key, time, returnDigits, "HmacSHA512")

  /**
    * This method generates a TOTP value for the given
    * set of parameters.
    *
    * @param key          :          the shared secret, HEX encoded
    * @param time         :         a value that reflects a time
    * @param returnDigits : number of digits to return
    * @param crypto       :       the crypto function to use
    * @return a numeric String in base 10 that includes
    *         {truncationDigits} digits
    */
  def generate(key: String, time: String, returnDigits: Int, crypto: String): String = {
    val hash = hmac_sha(crypto, hexStr2Bytes(key), hexStr2Bytes(time.prePad(16, '0')))
    // put selected bytes into result int
    val offset = hash(hash.length - 1) & 0xf
    val binary = ((hash(offset) & 0x7f) << 24) | ((hash(offset + 1) & 0xff) << 16) | ((hash(offset + 2) & 0xff) << 8) | (hash(offset + 3) & 0xff)
    val otp = binary % DIGITS_POWER(returnDigits)

    Integer.toString(otp).prePad(returnDigits, '0')
  }
}
