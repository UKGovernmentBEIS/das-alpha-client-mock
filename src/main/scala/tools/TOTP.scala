/**
  * Based on https://github.com/hmrc/totp-generator/blob/master/src/main/scala/uk/gov/hmrc/totp/TotpGenerator.scala
  */
package tools

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Base32

import scala.math._

sealed trait CryptoAlgorithm

case object HmacSHA512 extends CryptoAlgorithm

case class TimeWindow(value: Long) extends AnyVal {
  def next: TimeWindow = TimeWindow(value + 1)

  def prev: TimeWindow = TimeWindow(value - 1)
}

object TimeWindow {
  def forTimestamp(ts: Long): TimeWindow = TimeWindow(ts / 30000)

  def forNow: TimeWindow = forTimestamp(System.currentTimeMillis())

  /**
    * Generate three time windows  - one for the given timestamp and the
    * previous and next windows. The protocol for using TOTP suggest it is a good
    * idea to check all three of these when validating a code to account for small
    * discrepancies in system clocks between the client and the server.
    */
  def around(ts: Long): Seq[TimeWindow] = {
    val tw = TimeWindow.forTimestamp(ts)
    Seq(tw, tw.next, tw.prev)
  }
}

case class TOTPCode(value: String) extends AnyVal

trait TOTP {
  def generateCodeAtTime(base32EncodedSecret: String, ts: Long): TOTPCode =
    generateCode(base32EncodedSecret, TimeWindow.forTimestamp(ts))

  /**
    * Generate codes for the time window for the timestamp as well as the
    * previous and next time windows.
    */
  def generateCodesAround(base32EncodedSecret: String, ts: Long): Seq[TOTPCode] =
    TimeWindow.around(ts).map(generateCode(base32EncodedSecret, _))

  def generateCode(base32EncodedSecret: String, timeWindow: TimeWindow): TOTPCode = {
    val codeLength = 8
    val crypto = HmacSHA512
    val msg: Array[Byte] = BigInt(timeWindow.value).toByteArray.reverse.padTo(8, 0.toByte).reverse

    val hash = hmac_sha(crypto.toString, new Base32().decode(base32EncodedSecret), msg)
    val offset: Int = hash(hash.length - 1) & 0xf
    val binary: Long = ((hash(offset) & 0x7f) << 24) |
      ((hash(offset + 1) & 0xff) << 16) |
      ((hash(offset + 2) & 0xff) << 8 |
        (hash(offset + 3) & 0xff))

    val otp: Long = binary % pow(10, codeLength).toLong

    TOTPCode(("0" * codeLength + otp.toString).takeRight(codeLength))
  }

  private def hmac_sha(crypto: String, keyBytes: Array[Byte], text: Array[Byte]): Array[Byte] = {
    val hmac: Mac = Mac.getInstance(crypto)
    val macKey = new SecretKeySpec(keyBytes, "RAW")
    hmac.init(macKey)
    hmac.doFinal(text)
  }
}

object TOTP extends TOTP {
  def generateCode(base32EncodedSecret: String): TOTPCode = generateCodeAtTime(base32EncodedSecret, System.currentTimeMillis())
}



