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

import jdk.test.lib.util.JarUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Launches a new Java process using -jar Java option.
 */

public class TestProcessJarLauncher extends TestProcessLauncher {

    private static final String JAR_FILE = "testprocess.jar";


    public TestProcessJarLauncher(String className) {
        super(className);
    }

    protected String prepareLaunch(String javaExec, String pipePort) {
        try {
            File jarFile = prepareJar();
            return javaExec + " -jar " + jarFile.getAbsolutePath() + " -pipe.port=" + pipePort;
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare a jar file", e);
        }
    }

    private File prepareJar() throws IOException {
        Path jarFile = USER_DIR.resolve(JAR_FILE);
        Manifest manifest = createManifest();
        Path testClass = TEST_CLASSES_DIR.resolve(className + ".class");
        JarUtils.createJarFile(jarFile, manifest, TEST_CLASSES_DIR, Paths.get("."));
        return jarFile.toFile();
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, className);
        return manifest;
    }

    public String getJarFile() {
        return JAR_FILE;
    }
}
