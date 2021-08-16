/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.test.lib.containers.docker;

import jdk.test.lib.Platform;

// Use the following properties to specify docker base image at test execution time:
// Image name: jdk.test.docker.image.name
// Image version: jdk.test.docker.image.version
// Usage:
//     jtreg -Djdk.test.docker.image.name=<BASE_IMAGE_NAME> -Djdk.test.docker.image.version=<BASE_IMAGE_VERSION> test/hotspot/jtreg/runtime/containers/docker/
// E.g.:
//     jtreg -Djdk.test.docker.image.name=ubuntu -Djdk.test.docker.image.version=latest test/hotspot/jtreg/runtime/containers/docker/
// Using make:
//     make test TEST="test/hotspot/jtreg/runtime/containers/docker" JTREG="JAVA_OPTIONS=-Djdk.test.docker.image.name=ubuntu -Djdk.test.docker.image.version=latest"
// Note: base image version should not be an empty string. Use "latest" to get the latest version.

public class DockerfileConfig {
    static String getBaseImageName() {
        String name = System.getProperty("jdk.test.docker.image.name");
        if (name != null) {
            System.out.println("DockerfileConfig: using custom image name: " + name);
            return name;
        }

        switch (Platform.getOsArch()) {
            case "aarch64":
                return "arm64v8/ubuntu";
            case "ppc64le":
                return "ppc64le/ubuntu";
            case "s390x":
                return "s390x/ubuntu";
            default:
                return "ubuntu";
        }
    }

    static String getBaseImageVersion() {
        String version = System.getProperty("jdk.test.docker.image.version");
        if (version != null) {
            System.out.println("DockerfileConfig: using custom image version: " + version);
            return version;
        }

        return "latest";
    }
}
