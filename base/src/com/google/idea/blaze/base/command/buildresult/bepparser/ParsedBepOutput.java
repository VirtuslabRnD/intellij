/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.command.buildresult.bepparser;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.idea.blaze.common.artifact.OutputArtifact;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

/** A data class representing blaze's build event protocol (BEP) output for a build. */
public final class ParsedBepOutput {

  @VisibleForTesting
  public static final ParsedBepOutput EMPTY =
      new ParsedBepOutput(
          "build-id",
          ImmutableMap.of(),
          ImmutableMap.of(),
          ImmutableSetMultimap.of(),
          0,
          0,
          0,
          ImmutableSet.of());

  @Nullable public final String buildId;

  final ImmutableMap<String, String> workspaceStatus;

  /** A map from file set ID to file set, with the same ordering as the BEP stream. */
  private final ImmutableMap<String, FileSet> fileSets;

  /** The set of named file sets directly produced by each target. */
  private final SetMultimap<String, String> targetFileSets;

  final long syncStartTimeMillis;

  private final int buildResult;
  private final long bepBytesConsumed;
  private final ImmutableSet<String> targetsWithErrors;

  ParsedBepOutput(
    @Nullable String buildId,
    ImmutableMap<String, String> workspaceStatus,
    ImmutableMap<String, FileSet> fileSets,
    ImmutableSetMultimap<String, String> targetFileSets,
    long syncStartTimeMillis,
    int buildResult,
    long bepBytesConsumed,
    ImmutableSet<String> targetsWithErrors) {
    this.buildId = buildId;
    this.workspaceStatus = workspaceStatus;
    this.fileSets = fileSets;
    this.targetFileSets = targetFileSets;
    this.syncStartTimeMillis = syncStartTimeMillis;
    this.buildResult = buildResult;
    this.bepBytesConsumed = bepBytesConsumed;
    this.targetsWithErrors = targetsWithErrors;
  }

  /**
   * Returns URI of the source that the build consumed, if available. The format will be VCS
   * specific. If present, the value will be can be used with {@link
   * com.google.idea.blaze.base.vcs.BlazeVcsHandlerProvider.BlazeVcsHandler#vcsStateForSourceUri(String)}.
   */
  public ImmutableMap<String, String> getWorkspaceStatus() {
    return workspaceStatus;
  }

  /** Returns the build result. */
  public int getBuildResult() {
    return buildResult;
  }

  public long getBepBytesConsumed() {
    return bepBytesConsumed;
  }

  /** Returns all output artifacts of the build. */
  @TestOnly
  public ImmutableSet<OutputArtifact> getAllOutputArtifactsForTesting() {
    return fileSets.values().stream()
        .map(s -> s.parsedOutputs)
        .flatMap(List::stream)
        .collect(toImmutableSet());
  }

  /** Returns the set of artifacts directly produced by the given target.
   * Deprecated since AOSP pick b228d1ef2bdb093b73203176ec8158061042505e */
  @Deprecated
  public ImmutableSet<OutputArtifact> getDirectArtifactsForTarget(String label) {
    return targetFileSets.get(label).stream()
        .map(s -> fileSets.get(s).parsedOutputs)
        .flatMap(List::stream)
        .collect(toImmutableSet());
  }

  /** Returns the set of artifacts directly produced by the given target. */
  public ImmutableSet<OutputArtifact> getOutputGroupTargetArtifacts(String outputGroup, String label) {
    return fileSets.values().stream()
      .filter(f -> f.targets.contains(label) && f.outputGroups.contains(outputGroup))
      .map(f -> f.parsedOutputs)
      .flatMap(List::stream)
      .distinct()
      .collect(toImmutableSet());
  }

  public ImmutableList<OutputArtifact> getOutputGroupArtifacts(String outputGroup) {
    return fileSets.values().stream()
        .filter(f -> f.outputGroups.contains(outputGroup))
        .map(f -> f.parsedOutputs)
        .flatMap(List::stream)
        .distinct()
        .collect(toImmutableList());
  }

  /**
   * Returns a map from artifact key to {@link BepArtifactData} for all artifacts reported during
   * the build.
   */
  public ImmutableMap<String, BepArtifactData> getFullArtifactData() {
    return ImmutableMap.copyOf(
      Maps.transformValues(
        fileSets.values().stream()
          .flatMap(FileSet::toPerArtifactData)
          .collect(groupingBy(d -> d.artifact.getBazelOutRelativePath(), toImmutableSet())),
        BepArtifactData::combine));
  }

  /** Returns the set of build targets that had an error. */
  public ImmutableSet<String> getTargetsWithErrors() {
    return targetsWithErrors;
  }

  static class FileSet {
    private final ImmutableList<OutputArtifact> parsedOutputs;
    private final ImmutableSet<String> outputGroups;
    private final ImmutableSet<String> targets;

    FileSet(
      ImmutableList<OutputArtifact> parsedOutputs,
        Set<String> outputGroups,
        Set<String> targets) {
      this.parsedOutputs = parsedOutputs;
      this.outputGroups = ImmutableSet.copyOf(outputGroups);
      this.targets = ImmutableSet.copyOf(targets);
    }

    private Stream<BepArtifactData> toPerArtifactData() {
      return parsedOutputs.stream().map(a -> new BepArtifactData(a, outputGroups, targets));
    }
  }
}
