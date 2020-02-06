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

package com.fortysevendeg.hood.utils

import cats.effect.{Resource, Sync}
import java.io.{File, PrintWriter}

import scala.io.{BufferedSource, Source}

sealed trait FileType {
  val extensions: List[String] = List.empty
}
final case object Csv extends FileType {
  override val extensions = List("csv")
}
final case object Json extends FileType {
  override val extensions = List("json")
}
final case object Unknown extends FileType

object FileUtils {

  def openFile[F[_]](file: File)(implicit S: Sync[F]): Resource[F, BufferedSource] =
    Resource(S.delay {
      val fileBuffer = Source.fromFile(file)
      (fileBuffer, S.delay(fileBuffer.close()))
    })

  def fileType(file: File): FileType =
    if (Csv.extensions.contains(fileExtension(file))) {
      Csv
    } else if (Json.extensions.contains(fileExtension(file))) {
      Json
    } else Unknown

  def writeFile[F[_]](file: File, contents: String)(
      implicit S: Sync[F]
  ): F[Either[Throwable, Unit]] =
    S.attempt(
      S.bracket(S.delay(new PrintWriter(file)))(writer => S.delay(writer.write(contents)))(writer =>
        S.delay(writer.close())
      )
    )

  def readFile[F[_]](file: File)(implicit S: Sync[F]): F[Either[Throwable, String]] = {
    S.attempt(
      S.bracket(
        S.delay(
          Source.fromFile(file)
        )
      )(source => S.delay(source.mkString))(source => S.delay(source.close))
    )
  }

  private[this] def fileExtension(file: File): String =
    file.getName.toLowerCase.split('.').lastOption.getOrElse("")
}
