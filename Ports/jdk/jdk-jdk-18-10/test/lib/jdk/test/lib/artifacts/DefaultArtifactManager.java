/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.artifacts;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultArtifactManager implements ArtifactManager {
    @Override
    public Path resolve(Artifact artifact) throws ArtifactResolverException {
        return resolve(artifact.name());
    }

    public Path resolve(String name) throws ArtifactResolverException {
        String location = System.getProperty(artifactProperty(name));
        if (location == null) {
            throw new ArtifactResolverException("Couldn't automatically resolve dependency for " + name + "\n" +
                    "Please specify the location using " + artifactProperty(name));
        }
        return Paths.get(location);
    }
    private static String artifactProperty(String name) {
        return "jdk.test.lib.artifacts." + name;
    }
}
