package processes.freeMonads.vanillaScala.single

import scala.concurrent.Future

import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import processes.PatchAssignment
import processes.Services
import processes.freeMonads.HappyFlowOnlyProgramParts
import processes.freeMonads.HappyFlowOnlyProgramRunner

class HappyFlowOnly(protected val services: Services) extends PatchAssignment
  with Machinery with HappyFlowOnlyProgramParts with HappyFlowOnlyProgramRunner {

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
    def apply[A](fa: Method[A]) = patchProgramRunner(fa)
  }
}