/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Tool to compile Java sources and pack in a jar file.
 */
public final class JarBuilder {

    public JarBuilder() {
        sourceFiles = new ArrayList<>();
    }

    public JarBuilder setOutputJar(Path v) {
        outputJar = v;
        return this;
    }

    public JarBuilder setMainClass(String v) {
        mainClass = v;
        return this;
    }

    public JarBuilder addSourceFile(Path v) {
        sourceFiles.add(v);
        return this;
    }

    public JarBuilder setModuleVersion(String v) {
        moduleVersion = v;
        return this;
    }

    public void create() {
        TKit.withTempDirectory("jar-workdir", workDir -> {
            if (!sourceFiles.isEmpty()) {
                new Executor()
                        .setToolProvider(JavaTool.JAVAC)
                        .addArguments("-d", workDir.toString())
                        .addPathArguments(sourceFiles)
                        .execute();
            }

            Files.createDirectories(outputJar.getParent());
            if (Files.exists(outputJar)) {
                TKit.trace(String.format("Delete [%s] existing jar file", outputJar));
                Files.deleteIfExists(outputJar);
            }

            Executor jarExe = new Executor()
                    .setToolProvider(JavaTool.JAR)
                    .addArguments("-c", "-f", outputJar.toString());
            if (moduleVersion != null) {
                jarExe.addArguments(String.format("--module-version=%s",
                        moduleVersion));
            }
            if (mainClass != null) {
                jarExe.addArguments("-e", mainClass);
            }
            jarExe.addArguments("-C", workDir.toString(), ".");
            jarExe.execute();
        });
    }
    private List<Path> sourceFiles;
    private Path outputJar;
    private String mainClass;
    private String moduleVersion;
}
