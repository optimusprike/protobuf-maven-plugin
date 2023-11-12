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
package io.github.ascopes.protobufmavenplugin.resolver;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinate factory for determining the correct {@code protoc} artifact to use for the current
 * system.
 *
 * @author Ashley Scopes
 */
public final class MavenProtocCoordinateFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenProtocCoordinateFactory.class);

  private static final String GROUP_ID = "com.google.protobuf";
  private static final String ARTIFACT_ID = "protoc";
  private static final String EXTENSION = "exe";

  /**
   * Create the artifact coordinate for the current system.
   *
   * @param versionRange the version or version range of {@code protoc} to use.
   * @return the artifact to resolve.
   * @throws ProtocResolutionException if the system is not supported.
   */
  public ArtifactCoordinate create(String versionRange) throws ProtocResolutionException {
    var coordinate = new DefaultArtifactCoordinate();
    coordinate.setGroupId(GROUP_ID);
    coordinate.setArtifactId(ARTIFACT_ID);
    coordinate.setVersion(versionRange);
    coordinate.setClassifier(determineClassifier());
    coordinate.setExtension(EXTENSION);
    return coordinate;
  }

  private String determineClassifier() throws ProtocResolutionException {
    String classifier;

    if (HostEnvironment.isWindows()) {
      classifier = "windows-" + determineArchitectureForWindows();
    } else if (HostEnvironment.isLinux()) {
      classifier = "linux-" + determineArchitectureForLinux();
    } else if (HostEnvironment.isMacOs()) {
      classifier = "osx-" + determineArchitectureForMacOs();
    } else {
      throw new ProtocResolutionException("No resolvable protoc version for the current OS found");
    }

    LOGGER.debug("Will use {} as the protoc artifact classifier", classifier);
    return classifier;
  }

  private String determineArchitectureForWindows() throws ProtocResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      case "amd64":
      case "x86_64":
        return "x86_64";

      case "x86":
      case "x86_32":
        return "x86_32";

      default:
        throw noResolvableProtocFor("Windows", arch);
    }
  }

  private String determineArchitectureForLinux() throws ProtocResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      // https://bugs.openjdk.org/browse/JDK-8073139
      case "ppc64le":
      case "ppc64":
        return "ppcle_64";

      case "s390":
      case "zarch_64":
        return "s390_64";

      case "aarch64":
        return "aarch_64";

      case "amd64":
        return "x86_64";

      default:
        throw noResolvableProtocFor("Linux", arch);
    }
  }

  private String determineArchitectureForMacOs() {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      case "aarch64":
        return "aarch_64";

      case "amd64":
      case "x86_64":
        return "x86_64";

      default:
        LOGGER.warn(
            "No supported protoc version was found for Mac OS systems using the '{}' architecture,"
                + " attempting to fall back to the Universal Binary distribution. Your mileage may"
                + " vary with this approach.",
            arch
        );
        return "universal_binary";
    }
  }

  private ProtocResolutionException noResolvableProtocFor(String os, String arch) {
    var message = "No resolvable protoc version for " + os + " '" + arch + "' systems found";
    return new ProtocResolutionException(message);
  }
}
