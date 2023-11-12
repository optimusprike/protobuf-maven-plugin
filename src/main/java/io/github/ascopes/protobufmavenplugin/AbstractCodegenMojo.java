/*
 * Copyright (C) 2023, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin;

import io.github.ascopes.protobufmavenplugin.executor.DefaultProtocExecutor;
import io.github.ascopes.protobufmavenplugin.resolver.MavenProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolver.PathProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolver.ProtocResolutionException;
import java.nio.file.Path;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;

/**
 * Base Mojo to generate protobuf sources.
 *
 * <p>Can be extended for each language that this plugin supports.
 *
 * @author Ashley Scopes
 */
public abstract class AbstractCodegenMojo extends AbstractMojo {

  /**
   * The artifact resolver.
   */
  @Component
  private ArtifactResolver artifactResolver;

  /**
   * The Maven session that is in use.
   *
   * <p>This is passed in by Maven automatically, so can be ignored.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  /**
   * The version of protoc to use.
   *
   * <p>This can be a static version, or a valid Maven version range (such as
   * "{@code [3.5.0,4.0.0)}"). It is recommended to use a static version to ensure your builds are
   * reproducible.
   *
   * <p>If set to "{@code PATH}", then {@code protoc} is resolved from the system path rather than
   * being downloaded. This is useful if you need to use an unsupported architecture/OS, or a
   * development version of {@code protoc}.
   */
  @Parameter(required = true, property = "protoc.version")
  private String version;

  /**
   * The root directories to look for protobuf sources in.
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/protobuf")
  private Set<String> sourceDirectories;

  /**
   * The directory to output generated sources to.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/protoc")
  private String outputDirectory;

  /**
   * Whether to treat {@code protoc} compiler warnings as errors.
   */
  @Parameter(defaultValue = "false")
  private boolean fatalWarnings;

  /**
   * Whether to attempt to force builds to be reproducible.
   *
   * <p>When enabled, {@code protoc} may attempt to keep things like map ordering
   * consistent between builds as long as the same version of {@code protoc} is
   * used each time.
   */
  @Parameter(defaultValue = "false")
  private boolean reproducibleBuilds;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var protocPath = resolveProtocPath();
    var executor = new DefaultProtocExecutor(protocPath);
  }

  private Path resolveProtocPath() throws MojoExecutionException, MojoFailureException {
    try {
      var resolver = version.trim().equalsIgnoreCase("PATH")
          ? new PathProtocResolver()
          : new MavenProtocResolver(version, artifactResolver, session);

      return resolver.resolveProtoc();

    } catch (ProtocResolutionException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }
}
