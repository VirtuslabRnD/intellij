/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.async.executor;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.intellij.openapi.application.ApplicationManager;
import java.util.concurrent.Callable;

/** Shared thread pool for blaze tasks. */
public abstract class BlazeExecutor {

  public static BlazeExecutor getInstance() {
    return ApplicationManager.getApplication().getService(BlazeExecutor.class);
  }

  public abstract <T> ListenableFuture<T> submit(Callable<T> callable);

  public abstract ListeningExecutorService getExecutor();
}
