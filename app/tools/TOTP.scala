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

/**
  * This is an example implementation of the OATH
  * TOTP algorithm.
  * Visit www.openauthentication.org for more information.
  *
  * @author Johan Rydell, PortWise, Inc.
  */
object TOTP {
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
    try {
      var hmac: Mac = null
      hmac = Mac.getInstance(crypto)
      val macKey: SecretKeySpec = new SecretKeySpec(keyBytes, "RAW")
      hmac.init(macKey)
      hmac.doFinal(text)
    }
    catch {
      case gse: GeneralSecurityException => throw new UndeclaredThrowableException(gse)
    }
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
    val bArray: Array[Byte] = new BigInteger("10" + hex, 16).toByteArray
    // Copy all the REAL bytes, not the "first"
    val ret: Array[Byte] = new Array[Byte](bArray.length - 1)
    ret.indices.foreach { i =>
      ret(i) = bArray(i + 1)
    }
    ret
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
  def generateTOTP(key: String, time: String, returnDigits: String): String = {
    generateTOTP(key, time, returnDigits, "HmacSHA1")
  }

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
  def generateTOTP256(key: String, time: String, returnDigits: String): String = {
    generateTOTP(key, time, returnDigits, "HmacSHA256")
  }

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
  def generateTOTP512(key: String, time: String, returnDigits: String): String = {
    generateTOTP(key, time, returnDigits, "HmacSHA512")
  }

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
  def generateTOTP(key: String, time: String, returnDigits: String, crypto: String): String = {
    val codeDigits: Int = Integer.decode(returnDigits)
    var result: String = null
    // Using the counter
    // First 8 bytes are for the movingFactor
    // Compliant with base RFC 4226 (HOTP)
    var paddedTime = time
    while (paddedTime.length < 16) paddedTime = "0" + paddedTime

    // Get the HEX in a Byte[]
    val msg: Array[Byte] = hexStr2Bytes(time)
    val k: Array[Byte] = hexStr2Bytes(key)
    val hash: Array[Byte] = hmac_sha(crypto, k, msg)
    // put selected bytes into result int
    val offset: Int = hash(hash.length - 1) & 0xf
    val binary: Int = ((hash(offset) & 0x7f) << 24) | ((hash(offset + 1) & 0xff) << 16) | ((hash(offset + 2) & 0xff) << 8) | (hash(offset + 3) & 0xff)
    val otp: Int = binary % DIGITS_POWER(codeDigits)
    result = Integer.toString(otp)
    while (result.length < codeDigits) {
        result = "0" + result
    }
    result
  }
}
