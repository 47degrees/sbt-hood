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

import cats.Monad
import cats.data.EitherT
import cats.free.Free
import github4s.GithubResponses._

object instances {
  type Github4sResponse[A] = EitherT[GHIO, GHException, GHResult[A]]

  implicit val ghResponseMonad: Monad[Github4sResponse] = new Monad[Github4sResponse] {

    override def flatMap[A, B](
        fa: Github4sResponse[A]
    )(f: A => Github4sResponse[B]): Github4sResponse[B] =
      fa.flatMap(ghResult => f(ghResult.result))

    override def tailRecM[A, B](
        a: A
    )(f: A => Github4sResponse[Either[A, B]]): Github4sResponse[B] = {
      f(a).flatMap { ghResult =>
        ghResult.result match {
          case Right(v) =>
            val ghio: GHIO[GHResponse[B]] =
              Free.pure(Right(GHResult(v, ghResult.statusCode, ghResult.headers)))
            EitherT(ghio)
          case Left(e) => tailRecM(e)(f)
        }
      }
    }

    override def pure[A](x: A): Github4sResponse[A] = EitherT.pure(GHResult(x, 200, Map.empty))
  }

  implicit def ghResultSyntax[A](gHResult: GHResult[A]): GHResultOps[A] =
    new GHResultOps[A](gHResult)

  final class GHResultOps[A](gHResult: GHResult[A]) {
    def map[B](f: A => B): GHResult[B] =
      gHResult.copy(result = f(gHResult.result))
  }
}
