/*
 * Copyright 2019-2020 47 Degrees, LLC. <http://www.47deg.com>
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
import cats.effect.Sync
import cats.implicits._
import com.github.marklister.base64.Base64._
import instances._
import github4s.Github
import github4s.Github._
import github4s.cats.effect.jvm.Implicits._
import github4s.GithubResponses._
import github4s.free.domain._
import io.chrisdavenport.log4cats.Logger

trait GithubService[F[_]] {

  def publishComment(
      accessToken: String,
      owner: String,
      repository: String,
      pullRequestNumber: Int,
      comment: String
  ): F[GHResponse[Comment]]

  def editComment(
      accessToken: String,
      owner: String,
      repository: String,
      commentId: Int,
      comment: String
  ): F[GHResponse[Comment]]

  def listComments(
      accessToken: String,
      owner: String,
      repository: String,
      pullRequestNumber: Int
  ): F[GHResponse[List[Comment]]]

  def createStatus(
      accessToken: String,
      owner: String,
      repository: String,
      pullRequestNumber: Int,
      state: GithubState,
      targetUrl: Option[String],
      description: String,
      context: String
  ): F[GHResponse[Status]]

  def commitFilesAndContents(
      accessToken: String,
      owner: String,
      repository: String,
      branch: String,
      message: String,
      filesAndContents: List[(String, String)]
  ): F[GHResponse[Option[Ref]]]
}

object GithubService {

  def build[F[_]: Sync: Logger]: GithubService[F] = new GithubServiceImpl[F]

  class GithubServiceImpl[F[_]: Sync](implicit L: Logger[F]) extends GithubService[F] {

    def publishComment(
        accessToken: String,
        owner: String,
        repository: String,
        pullRequestNumber: Int,
        comment: String
    ): F[GHResponse[Comment]] =
      for {
        result <- Github(Some(accessToken)).issues
          .createComment(owner, repository, pullRequestNumber, comment)
          .exec()
          .onError { case e => L.error(e)("Found error while accessing GitHub API.") }
        _ <- L.info("Comment sent to GitHub successfully.")
      } yield result

    def editComment(
        accessToken: String,
        owner: String,
        repository: String,
        commentId: Int,
        comment: String
    ): F[GHResponse[Comment]] =
      for {
        result <- Github(Some(accessToken)).issues
          .editComment(owner, repository, commentId, comment)
          .exec()
          .onError { case e => L.error(e)("Found error while accessing GitHub API.") }
        _ <- L.info("Comment edited successfully.")
      } yield result

    def listComments(
        accessToken: String,
        owner: String,
        repository: String,
        pullRequestNumber: Int
    ): F[GHResponse[List[Comment]]] =
      for {
        result <- Github(Some(accessToken)).issues
          .listComments(owner, repository, pullRequestNumber)
          .exec()
          .onError { case e => L.error(e)("Found error while accessing GitHub API.") }
      } yield result

    def createStatus(
        accessToken: String,
        owner: String,
        repository: String,
        pullRequestNumber: Int,
        state: GithubState,
        targetUrl: Option[String],
        description: String,
        context: String
    ): F[GHResponse[Status]] = {
      val gh = Github(Some(accessToken))

      (for {
        pr <- EitherT(gh.pullRequests.get(owner, repository, pullRequestNumber).exec())
        head <- EitherT.fromOption[F](
          pr.result.head,
          UnexpectedException("Couldn't find a head SHA for the specified pull request.")
        )
        sha = head.sha
        result <- EitherT(
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
            .exec()
        )
      } yield result).value.onError {
        case e => L.error(e)("Found error while accessing GitHub API.")
      }
    }

    def commitFilesAndContents(
        accessToken: String,
        owner: String,
        repository: String,
        branch: String,
        message: String,
        filesAndContents: List[(String, String)]
    ): F[GHResponse[Option[Ref]]] = {
      val gh = Github(Some(accessToken))

      def fetchBaseTreeSha(commitSha: String): Github4sResponse[RefCommit] =
        EitherT(gh.gitData.getCommit(owner, repository, commitSha))

      def fetchFilesContents(
          commitSha: String
      ): Github4sResponse[List[(String, Option[String])]] = {

        def fetchFileContents(
            path: String,
            commitSha: String
        ): Github4sResponse[(String, Option[String])] =
          EitherT(
            gh.repos
              .getContents(owner = owner, repo = repository, path = path, ref = Some(commitSha))
          ).map(ghRes => ghRes.map(contents => path -> contents.head.content))
            .orElse((path, none[String]).pure[Github4sResponse])

        filesAndContents.map(_._1).traverse(fetchFileContents(_, commitSha))
      }

      def filterNonChangedFiles(remote: List[(String, Option[String])]): List[(String, String)] = {
        val remoteMap = remote.collect {
          case (path, Some(c)) => path -> c
        }.toMap
        filesAndContents.filterNot {
          case (path, content) =>
            remoteMap.get(path).exists { remoteContent =>
              remoteContent.trim.replaceAll("\n", "") == content.getBytes.toBase64.trim
            }
        }
      }

      def createTree(
          baseTreeSha: String,
          filteredFilesContent: List[(String, String)]
      ): Github4sResponse[TreeResult] = {

        def treeData: List[TreeDataBlob] = filteredFilesContent.map {
          case (path, content) => TreeDataBlob(path, "100644", "blob", content)
        }

        EitherT(gh.gitData.createTree(owner, repository, Some(baseTreeSha), treeData))
      }

      def createCommit(treeSha: String, baseCommitSha: String): Github4sResponse[RefCommit] =
        EitherT(gh.gitData.createCommit(owner, repository, message, treeSha, List(baseCommitSha)))

      def updateHead(branch: String, commitSha: String): Github4sResponse[Ref] =
        EitherT(gh.gitData.updateReference(owner, repository, s"heads/$branch", commitSha))

      def fetchHeadCommit(branch: String): Github4sResponse[Ref] = {

        def findReference(gHResult: GHResult[NonEmptyList[Ref]]): GHResponse[Ref] =
          gHResult.result.toList.find(_.ref == s"refs/heads/$branch") match {
            case Some(ref) => Right(gHResult.map(_ => ref))
            case None      => Left(UnexpectedException(s"Branch $branch not found"))
          }

        EitherT(gh.gitData.getReference(owner, repository, s"heads/$branch"))
          .subflatMap(findReference)
      }

      def commitFilesIfChanged(
          baseTreeSha: String,
          parentCommitSha: String,
          filteredFilesContent: List[(String, String)]
      ): Github4sResponse[Option[Ref]] =
        filteredFilesContent match {
          case Nil =>
            none[Ref].pure[Github4sResponse]
          case list =>
            for {
              ghResultTree   <- createTree(baseTreeSha, list)
              ghResultCommit <- createCommit(ghResultTree.result.sha, parentCommitSha)
              ghResultUpdate <- updateHead(branch, ghResultCommit.result.sha)
            } yield ghResultUpdate.map(Option(_))
        }

      val op = for {
        gHResultParentCommit <- fetchHeadCommit(branch)
        parentCommitSha = gHResultParentCommit.result.`object`.sha
        gHResultBaseTree <- fetchBaseTreeSha(parentCommitSha)
        baseTreeSha = gHResultBaseTree.result.tree.sha
        ghResultFilesContent <- fetchFilesContents(parentCommitSha)
        ghResultUpdate <- commitFilesIfChanged(
          baseTreeSha,
          parentCommitSha,
          filterNonChangedFiles(ghResultFilesContent.result)
        )
      } yield ghResultUpdate

      op.value.exec()
    }

  }

}
