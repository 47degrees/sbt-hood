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

import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import com.fortysevendeg.hood.model.{HoodError, MissingGitHubParameter}

sealed trait GithubState {
  val value: String
}

final case object GithubStatusError extends GithubState {
  val value = "error"
}

final case object GithubStatusSuccess extends GithubState {
  val value = "success"
}

case class GithubStatusDescription(state: GithubState, description: String)

object GithubModel {
  val githubStatusContext = "sbt-hood"
}

case class GitHubParameters(
    accessToken: String,
    repositoryOwner: String,
    repositoryName: String,
    pullRequestNumber: Int,
    targetUrl: Option[String],
    branch: String,
    commitMessage: String
)

object GitHubParameters {
  type ValidationResult[A] = ValidatedNec[HoodError, A]

  def fromParams(
      accessToken: Option[String],
      repositoryOwner: Option[String],
      repositoryName: Option[String],
      pullRequestNumber: Option[Int],
      targetUrl: Option[String],
      branch: String,
      commitMessage: String
  ): Either[NonEmptyChain[HoodError], GitHubParameters] =
    (
      checkMandatoryParameter(accessToken, "gitHubToken"),
      checkMandatoryParameter(repositoryOwner, "repositoryOwner"),
      checkMandatoryParameter(repositoryName, "repositoryName"),
      checkMandatoryParameter(pullRequestNumber, "pullRequestNumber")
    ).mapN {
      case (token, repoOwner, repoName, pull) =>
        GitHubParameters(token, repoOwner, repoName, pull, targetUrl, branch, commitMessage)
    }.toEither

  private[this] def checkMandatoryParameter[A](
      param: Option[A],
      name: String
  ): ValidationResult[A] =
    param.fold(
      MissingGitHubParameter(s"Missing required parameter for GitHub integration: $name")
        .invalidNec[A]
    )(_.validNec)
}
