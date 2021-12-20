/*
 * Copyright 2019-2020 47 Degrees Open Source <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortysevendeg.hood.github

import cats.data.{EitherT, NonEmptyList}
import cats.effect.{Async, Resource}
import cats.implicits._
import com.fortysevendeg.hood.github.instances._
import com.github.marklister.base64.Base64._
import github4s.Github
import github4s.domain._
import org.typelevel.log4cats.Logger
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client

import scala.concurrent.ExecutionContext

trait GithubService[F[_]] {

  def publishComment(
      accessToken: String,
      owner: String,
      repository: String,
      pullRequestNumber: Int,
      comment: String
  ): Github4sResponse[F, Comment]

  def editComment(
      accessToken: String,
      owner: String,
      repository: String,
      commentId: Long,
      comment: String
  ): Github4sResponse[F, Comment]

  def listComments(
      accessToken: String,
      owner: String,
      repository: String,
      pullRequestNumber: Int
  ): Github4sResponse[F, List[Comment]]

  def createStatus(
      accessToken: String,
      owner: String,
      repository: String,
      pullRequestNumber: Int,
      state: GithubState,
      targetUrl: Option[String],
      description: String,
      context: String
  ): Github4sResponse[F, Status]

  def commitFilesAndContents(
      accessToken: String,
      owner: String,
      repository: String,
      branch: String,
      message: String,
      filesAndContents: List[(String, String)]
  ): Github4sResponse[F, Option[Ref]]
}

object GithubService {

  def build[F[_]: Async: Logger](
      clientEC: ExecutionContext
  ): Resource[F, GithubService[F]] =
    BlazeClientBuilder[F].withExecutionContext(clientEC).resource.map(new GithubServiceImpl[F](_))

  class GithubServiceImpl[F[_]: Async](clientEC: Client[F])(implicit L: Logger[F])
      extends GithubService[F] {

    def publishComment(
        accessToken: String,
        owner: String,
        repository: String,
        pullRequestNumber: Int,
        comment: String
    ): Github4sResponse[F, Comment] =
      toResponse(for {
        result <-
          Github[F](clientEC, Some(accessToken)).issues
            .createComment(owner, repository, pullRequestNumber, comment)
            .onError { case e => L.error(e)("Found error while accessing GitHub API.") }
        _ <- L.info("Comment sent to GitHub successfully.")
      } yield result)

    def editComment(
        accessToken: String,
        owner: String,
        repository: String,
        commentId: Long,
        comment: String
    ): Github4sResponse[F, Comment] =
      toResponse(for {
        result <-
          Github[F](clientEC, Some(accessToken)).issues
            .editComment(owner, repository, commentId, comment)
            .onError { case e => L.error(e)("Found error while accessing GitHub API.") }
        _ <- L.info("Comment edited successfully.")
      } yield result)

    def listComments(
        accessToken: String,
        owner: String,
        repository: String,
        pullRequestNumber: Int
    ): Github4sResponse[F, List[Comment]] =
      toResponse(
        Github[F](clientEC, Some(accessToken)).issues
          .listComments(owner, repository, pullRequestNumber)
          .onError { case e => L.error(e)("Found error while accessing GitHub API.") }
      )

    def createStatus(
        accessToken: String,
        owner: String,
        repository: String,
        pullRequestNumber: Int,
        state: GithubState,
        targetUrl: Option[String],
        description: String,
        context: String
    ): Github4sResponse[F, Status] = {
      val gh = Github[F](clientEC, Some(accessToken))

      (for {
        pr <- toResponse(gh.pullRequests.getPullRequest(owner, repository, pullRequestNumber))
        head <- EitherT.fromOption[F](
          pr.head,
          Github4sUnexpectedError("Couldn't find a head SHA for the specified pull request.")
        )
        sha = head.sha
        result <- toResponse(
          gh.repos
            .createStatus(
              owner,
              repository,
              sha,
              state.value,
              targetUrl,
              description.some,
              context.some
            )
        )
      } yield result).onError {
        case Github4sLibError(e) =>
          EitherT.liftF(L.error(e)("Found error while accessing GitHub API."))
        case Github4sUnexpectedError(msg) =>
          EitherT.liftF(L.error(msg))
      }
    }

    def commitFilesAndContents(
        accessToken: String,
        owner: String,
        repository: String,
        branch: String,
        message: String,
        filesAndContents: List[(String, String)]
    ): Github4sResponse[F, Option[Ref]] = {
      val gh = Github[F](clientEC, Some(accessToken))

      def fetchBaseTreeSha(commitSha: String): Github4sResponse[F, RefCommit] =
        toResponse(gh.gitData.getCommit(owner, repository, commitSha))

      def fetchFilesContents(
          commitSha: String
      ): Github4sResponse[F, List[(String, Option[String])]] = {

        def fetchFileContents(
            path: String,
            commitSha: String
        ): Github4sResponse[F, (String, Option[String])] =
          toResponse(
            gh.repos
              .getContents(owner = owner, repo = repository, path = path, ref = Some(commitSha))
          ).map(contents => path -> contents.head.content)

        filesAndContents.map(_._1).traverse(fetchFileContents(_, commitSha))
      }

      def filterNonChangedFiles(remote: List[(String, Option[String])]): List[(String, String)] = {
        val remoteMap = remote.collect { case (path, Some(c)) =>
          path -> c
        }.toMap
        filesAndContents.filterNot { case (path, content) =>
          remoteMap.get(path).exists { remoteContent =>
            remoteContent.trim.replaceAll("\n", "") == content.getBytes.toBase64.trim
          }
        }
      }

      def createTree(
          baseTreeSha: String,
          filteredFilesContent: List[(String, String)]
      ): Github4sResponse[F, TreeResult] = {

        def treeData: List[TreeDataBlob] =
          filteredFilesContent.map { case (path, content) =>
            TreeDataBlob(path, "100644", "blob", content)
          }

        toResponse(gh.gitData.createTree(owner, repository, Some(baseTreeSha), treeData))
      }

      def createCommit(treeSha: String, baseCommitSha: String): Github4sResponse[F, RefCommit] =
        toResponse(
          gh.gitData.createCommit(owner, repository, message, treeSha, List(baseCommitSha), None)
        )

      def updateHead(branch: String, commitSha: String): Github4sResponse[F, Ref] =
        toResponse(
          gh.gitData.updateReference(owner, repository, s"heads/$branch", commitSha, force = false)
        )

      def fetchHeadCommit(branch: String): Github4sResponse[F, Ref] = {

        def findReference(result: NonEmptyList[Ref]): Either[Github4sError, Ref] =
          result.toList
            .find(_.ref == s"refs/heads/$branch")
            .toRight(Github4sUnexpectedError(s"Branch $branch not found"))

        toResponse(gh.gitData.getReference(owner, repository, s"heads/$branch"))
          .subflatMap(findReference)
      }

      def commitFilesIfChanged(
          baseTreeSha: String,
          parentCommitSha: String,
          filteredFilesContent: List[(String, String)]
      ): Github4sResponse[F, Option[Ref]] =
        filteredFilesContent match {
          case Nil =>
            EitherT.pure[F, Github4sError](none[Ref])
          case list =>
            for {
              ghResultTree   <- createTree(baseTreeSha, list)
              ghResultCommit <- createCommit(ghResultTree.sha, parentCommitSha)
              ghResultUpdate <- updateHead(branch, ghResultCommit.sha)
            } yield ghResultUpdate.some
        }

      for {
        gHResultParentCommit <- fetchHeadCommit(branch)
        parentCommitSha = gHResultParentCommit.`object`.sha
        gHResultBaseTree <- fetchBaseTreeSha(parentCommitSha)
        baseTreeSha = gHResultBaseTree.tree.sha
        ghResultFilesContent <- fetchFilesContents(parentCommitSha)
        ghResultUpdate <- commitFilesIfChanged(
          baseTreeSha,
          parentCommitSha,
          filterNonChangedFiles(ghResultFilesContent)
        )
      } yield ghResultUpdate
    }

  }

}
