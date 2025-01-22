/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.sync.aspects.storage

import com.google.idea.blaze.base.sync.SyncScope.SyncFailedException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class AspectWriterImpl : AspectWriter {

  override fun name(): String {
    return "Default Aspects"
  }

  override fun write(dst: Path, project: Project) {
    val src = AspectRepositoryProvider.aspectDirectory()
      .orElseThrow { SyncFailedException("Could not find aspect directory") }

    try {
      // no read lock, is this safe?
      VfsUtilCore.iterateChildrenRecursively(src, null) { entry ->
        val path = VfsUtil.getRelativePath(entry, src)?.let(Path::of)
          ?: throw IOException("Could not determine relative path of ${entry.path}")

        if (entry.isDirectory) {
          Files.createDirectories(dst.resolve(path))
        } else {
          Files.newOutputStream(
            dst.resolve(path),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
          ).use { output ->
            entry.inputStream.use { input ->
              input.transferTo(output)
            }
          }
        }

        true // continue to iterate
      }
    } catch (e: IOException) {
      throw SyncFailedException("Could not copy aspects", e)
    }
  }
}