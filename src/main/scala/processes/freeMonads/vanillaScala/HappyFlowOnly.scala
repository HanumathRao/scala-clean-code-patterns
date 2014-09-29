package processes.freeMonads.vanillaScala

import processes.PatchAssignment
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.AnyContent
import processes.Services

class HappyFlowOnly(services:Services) extends PatchAssignment with Machinery {

  import domain.Profile

  sealed trait Method[ReturnType]
  case class ParseJson(request: Request[AnyContent]) extends Method[JsValue]
  case class JsonToProfile(json: JsValue) extends Method[Profile]
  case class GetProfileById(id: String) extends Method[Profile]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Method[Profile]
  case class UpdateProfile(id: String, profile: Profile) extends Method[Unit]
  
  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {
    val patchProgram =
      for {
        json <- ParseJson(request)
        newProfile <- JsonToProfile(json)
        oldProfile <- GetProfileById(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    patchProgram.run(PatchProgramRunner).map(_.merge)
  }

  object PatchProgramRunner extends (Method ~> HttpResult) {
    def apply[A](fa: Method[A]) = fa match {

      case ParseJson(request: Request[AnyContent]) =>
        val result =
          services
            .parseJson(request)
            .toRight(left = results.badRequest)

        Future successful result

      case JsonToProfile(json) =>
        val result =
          services
            .jsonToProfile(json)
            .asEither
            .left.map(results.unprocessableEntity)

        Future successful result

      case GetProfileById(id) =>
        services
          .getProfileById(id)
          .map(_.toRight(left = results.notFound(id)))

      case MergeProfile(oldProfile, newProfile) =>
        val result = services.mergeProfile(oldProfile, newProfile)

        Future successful Right(result)

      case UpdateProfile(id, profile) =>
        services
          .updateProfile(id, profile)
          .map(Right.apply)
    }
  }
}