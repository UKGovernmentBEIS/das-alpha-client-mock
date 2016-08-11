package controllers

sealed trait SchemeStatus extends Product with Serializable

case class Unclaimed(empref: String) extends SchemeStatus

case class UserClaimed(empref: String) extends SchemeStatus

case class OtherClaimed(empref: String) extends SchemeStatus