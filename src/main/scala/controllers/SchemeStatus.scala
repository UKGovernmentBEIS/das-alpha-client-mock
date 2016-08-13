package controllers

import models.EmployerDetail


sealed trait SchemeStatus extends Product with Serializable

case class Unclaimed(details: EmployerDetail) extends SchemeStatus

case class UserClaimed(details: EmployerDetail) extends SchemeStatus

case class OtherClaimed(details: EmployerDetail) extends SchemeStatus