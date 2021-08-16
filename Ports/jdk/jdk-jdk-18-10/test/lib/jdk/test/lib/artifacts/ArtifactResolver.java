/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;

public class ArtifactResolver {
    public static Map<String, Path> resolve(Class<?> klass) throws ArtifactResolverException {
        ArtifactManager manager;
        try {
            String managerName = System.getProperty("jdk.test.lib.artifacts.artifactmanager");
            if (managerName != null) {
                manager = (ArtifactManager) Class.forName(managerName).newInstance();
            } else if (System.getenv().containsKey(JibArtifactManager.JIB_HOME_ENV_NAME)) {
                manager = JibArtifactManager.newInstance();
            } else {
                manager = new DefaultArtifactManager();
            }
        } catch (Exception e) {
            throw new ArtifactResolverException("Failed to load ArtifactManager", e);
        }

        ArtifactContainer artifactContainer = klass.getAnnotation(ArtifactContainer.class);
        HashMap<String, Path> locations = new HashMap<>();
        Artifact[] artifacts;

        if (artifactContainer == null) {
            artifacts = new Artifact[]{klass.getAnnotation(Artifact.class)};
        } else {
            artifacts = artifactContainer.value();
        }
        for (Artifact artifact : artifacts) {
            locations.put(artifactName(artifact), manager.resolve(artifact));
        }

        return locations;
    }

    private static String artifactName(Artifact artifact) {
        // Format of the artifact name is <organization>.<name>-<revision>(-<classifier>)
        String name = String.format("%s.%s-%s", artifact.organization(), artifact.name(), artifact.revision());
        if (artifact.classifier().length() != 0) {
            name = name +"-" + artifact.classifier();
        }
        return name;
    }
}
