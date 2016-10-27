package controllers

import api.ApiError._
import models.{ ApiToken}
import akka.actor.ActorSystem
import javax.inject.Inject
import com.difflang.models.User1
import play.api.i18n.MessagesApi
import play.api.libs.functional.syntax._
import play.api.libs.json._
import repos.UserRepository
import scala.concurrent.ExecutionContext.Implicits.global

class Auth @Inject() (val messagesApi: MessagesApi, system: ActorSystem, userRepo: UserRepository) extends api.ApiController {

  implicit val loginInfoReads: Reads[Tuple2[String, String]] = (
    (__ \ "email").read[String](Reads.email) and
      (__ \ "password").read[String] tupled
  )

  /*def signIn() = ApiActionWithBody { implicit request =>
    readFromRequest[Tuple2[String, String]] {
      case (email, pwd) =>
        userRepo.findByEmail2(email).flatMap {
          case None => errorUserNotFound
          case Some(user) => {
            if (user.password != pwd) errorUserNotFound
            else if (!user.emailConfirmed) errorUserEmailUnconfirmed
            else if (!user.active) errorUserInactive
            else ApiToken.create(request.apiKeyOpt.get, user.id).flatMap { token =>
              ok(Json.obj(
                "token" -> token,
                "minutes" -> 10
              ))
            }
          }
        }
    }
  }  */

  def signIn() = ApiActionWithBody { implicit request =>
    readFromRequest[Tuple2[String, String]] {
      case (email, pwd) =>
        userRepo.findByEmail(email).flatMap {
          case None => errorUserNotFound
          case Some(user) => {
            if (user.password != pwd) errorUserNotFound
            else if (!user.confirm_email) errorUserEmailUnconfirmed
            else if (!user.active) errorUserInactive
            // userId should be chang to long if using with relational database
            else ApiToken.create(request.apiKeyOpt.get, user.id.toString).flatMap { token =>
              ok(Json.obj(
                "token" -> token,
                "minutes" -> 10
              ))
            }
          }
        }
    }
  }

  def signOut = SecuredApiAction { implicit request =>
    ApiToken.delete(request.token).flatMap { _ =>
      noContent()
    }
  }

  implicit val signUpInfoReads: Reads[Tuple3[String, String, User1]] = (
    (__ \ "email").read[String](Reads.email) and
      (__ \ "password").read[String](Reads.minLength[String](6)) and
      (__ \ "user").read[User1] tupled
  )


  def signUp = ApiActionWithBody { implicit request =>
    readFromRequest[Tuple3[String, String, User1]] {
      case (email, password, user1) =>
        userRepo.findByEmail(email).flatMap {
          case Some(anotherUser) => errorCustom("api.error.signup.email.exists")
          case None => {
            val user: User1 = User1(user1.id, user1.first_name, user1.last_name, password, email, user1.address, user1.country, user1.state, user1.city, user1.zip, user1.mobile, true, true)
            userRepo.save(user).flatMap(result => created("Insert Success"))
          }
        }
    }
  }

}

