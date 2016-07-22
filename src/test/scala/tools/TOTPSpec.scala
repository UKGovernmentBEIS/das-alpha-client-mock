package tools

import org.scalatest.{Matchers, WordSpecLike}

class TOTPSpec extends WordSpecLike with Matchers {

  import TOTP.generate

  val testTimes = Seq(
    59L -> Seq(94287082, 46119246, 90693936),
    1111111109L -> Seq(7081804, 68084774, 25091201),
    1111111111L -> Seq(14050471, 67062674, 99943326),
    1234567890L -> Seq(89005924, 91819424, 93441116),
    2000000000L -> Seq(69279037, 90698825, 38618901),
    20000000000L -> Seq(65353130, 77737706, 47863826))

  // Seed for HMAC-SHA1 - 20 bytes
  val seed: String = "3132333435363738393031323334353637383930"
  // Seed for HMAC-SHA256 - 32 bytes
  val seed32: String = "3132333435363738393031323334353637383930" + "313233343536373839303132"
  // Seed for HMAC-SHA512 - 64 bytes
  val seed64: String = "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930" + "31323334"

  val T0: Long = 0
  val X: Long = 30

  "generateTOTP" should {
    "generate correct values" in {
      testTimes.foreach { case (testTime, expected) =>
        val T: Long = (testTime - T0) / X
        val steps = T.toHexString.toUpperCase

        val fmtTime = f"$testTime%11s"
        val totpSHA1 = generate(seed, steps, 8, "HmacSHA1")
        val totpSHA256 = generate(seed32, steps, 8, "HmacSHA256")
        val totpSHA512 = generate(seed64, steps, 8, "HmacSHA512")

        Seq(totpSHA1, totpSHA256, totpSHA512) shouldBe expected.map(i => f"$i%08d")
      }
    }
  }
}
