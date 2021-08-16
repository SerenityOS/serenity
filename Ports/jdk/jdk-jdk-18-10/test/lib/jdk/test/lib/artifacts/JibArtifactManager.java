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

import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JibArtifactManager implements ArtifactManager {
    private static final String JIB_SERVICE_FACTORY = "com.oracle.jib.api.JibServiceFactory";
    public static final String JIB_HOME_ENV_NAME = "JIB_HOME";
    private static String jibVersion = "1.0";

    private Object installerObject;
    private ClassLoader classLoader;

    private JibArtifactManager(Object installerObject, ClassLoader classLoader) {
        this.installerObject = installerObject;
        this.classLoader = classLoader;
    }

    public static JibArtifactManager newInstance() throws ClassNotFoundException {
        Path jibInstallDir = Paths.get(System.getenv(JIB_HOME_ENV_NAME));
        Path libDir = jibInstallDir.resolve("lib");
        if (!Files.isDirectory(libDir)) {
            throw new ClassNotFoundException(JIB_SERVICE_FACTORY);
        }
        try {
            URL[] jarUrls;
            try (Stream<Path> files = Files.list(libDir)) {
                jarUrls = files.filter(path -> path.toString().endsWith(".jar"))
                        .map(path -> {
                            try {
                                return path.toUri().toURL();
                            } catch (MalformedURLException e) {
                                throw new UncheckedIOException(e);
                            }
                        }).toArray(URL[]::new);
            }
            // Create a class loader using all those jars and set the parent to the
            // current class loader's parent.
            ClassLoader classLoader = new URLClassLoader(jarUrls, JibArtifactManager.class.getClassLoader().getParent());

            // Temporarily replace the context classLoader
            Thread currentThread = Thread.currentThread();
            ClassLoader oldContextLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(classLoader);

            Class jibServiceFactory = classLoader.loadClass(JIB_SERVICE_FACTORY);
            try {
                Object jibArtifactInstaller = jibServiceFactory.getMethod("createJibArtifactInstaller").invoke(null);
                return new JibArtifactManager(jibArtifactInstaller, classLoader);
            } finally {
                currentThread.setContextClassLoader(oldContextLoader);
            }

        } catch (Exception e) {
            throw new ClassNotFoundException(JIB_SERVICE_FACTORY, e);
        }
    }

    private Path download(String jibVersion, Map<String, Object> artifactDescription) throws Exception {
        return invokeInstallerMethod("download", jibVersion, artifactDescription);
    }

    private Path install(String jibVersion, Map<String, Object> artifactDescription) throws Exception {
        return invokeInstallerMethod("install", jibVersion, artifactDescription);
    }

    private Path invokeInstallerMethod(String methodName, String jibVersion,
                                       Map<String, Object> artifactDescription) throws Exception {
        // Temporarily replace the context classLoader
        Thread currentThread = Thread.currentThread();
        ClassLoader oldContextLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(classLoader);
        try {
            Method m = classLoader.loadClass("com.oracle.jib.api.JibArtifactInstaller")
                    .getMethod(methodName, String.class, Map.class);
            return (Path) m.invoke(installerObject, jibVersion, artifactDescription);
        } finally {
            currentThread.setContextClassLoader(oldContextLoader);
        }
    }

    @Override
    public Path resolve(Artifact artifact) throws ArtifactResolverException {
        HashMap<String, Object> artifactDescription = new HashMap<>();
        artifactDescription.put("module", artifact.name());
        artifactDescription.put("organization", artifact.organization());
        artifactDescription.put("ext", artifact.extension());
        artifactDescription.put("revision", artifact.revision());
        if (artifact.classifier().length() > 0) {
            artifactDescription.put("classifier", artifact.classifier());
        }
        return resolve(artifact.name(), artifactDescription, artifact.unpack());
    }

    public Path resolve(String name, Map<String, Object> artifactDescription, boolean unpack)
            throws ArtifactResolverException {
        Path path;
        // Use the DefaultArtifactManager to enable users to override locations
        try {
            DefaultArtifactManager manager = new DefaultArtifactManager();
            path = manager.resolve(name);
        } catch (ArtifactResolverException e) {
            // Location hasn't been overridden, continue to automatically try to resolve the dependency
            try {
                path = download(jibVersion, artifactDescription);
                if (unpack) {
                    path = install(jibVersion, artifactDescription);
                }
            } catch (Exception e2) {
                throw new ArtifactResolverException("Failed to resolve the artifact " + name, e2);
            }
        }
        return path;
    }
}
