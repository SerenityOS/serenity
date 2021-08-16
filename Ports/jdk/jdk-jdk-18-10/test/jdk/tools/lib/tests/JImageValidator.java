/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import jdk.internal.jimage.BasicImageReader;
import jdk.internal.jimage.ImageLocation;

/**
 *
 * JDK Modular image validator
 */
public class JImageValidator {

    private static final String[] dirs = {"bin", "lib"};

    private final File rootDir;
    private final List<String> expectedLocations;
    private final String module;
    private long moduleExecutionTime;
    private long javaExecutionTime;
    private final List<String> unexpectedPaths;
    private final List<String> unexpectedFiles;
    private final String[] expectedFiles;

    public JImageValidator(String module, List<String> expectedLocations,
            File rootDir,
            List<String> unexpectedPaths,
            List<String> unexpectedFiles) throws Exception {
        this(module, expectedLocations, rootDir, unexpectedPaths, unexpectedFiles, null);
    }

    public JImageValidator(String module, List<String> expectedLocations,
            File rootDir,
            List<String> unexpectedPaths,
            List<String> unexpectedFiles,
            String[] expectedFiles) throws IOException {
        if (!rootDir.exists()) {
            throw new IOException("Image root dir not found " +
                    rootDir.getAbsolutePath());
        }
        this.expectedLocations = expectedLocations;
        this.rootDir = rootDir;
        this.module = module;
        this.unexpectedPaths = unexpectedPaths;
        this.unexpectedFiles = unexpectedFiles;
        this.expectedFiles = expectedFiles == null ? new String[0] : expectedFiles;
    }

    public void validate() throws IOException {
        for (String d : dirs) {
            File dir = new File(rootDir, d);
            if (!dir.isDirectory()) {
                throw new IOException("Invalid directory " + d);
            }
        }

        //check jimage file
        Path path = rootDir.toPath().resolve("lib").resolve("modules");
        if (!Files.isRegularFile(path)) {
            throw new IOException(path + " No jimage file generated");
        }

        // Check binary file
        File launcher = new File(rootDir, "bin" + File.separator + module);
        if (launcher.exists()) {
            ProcessBuilder builder = new ProcessBuilder("sh", launcher.getAbsolutePath());
            long t = System.currentTimeMillis();
            Process process = builder.inheritIO().start();
            int ret = waitFor(process);
            moduleExecutionTime += System.currentTimeMillis() - t;
            if (ret != 0) {
                throw new IOException("Image " + module + " execution failed, check logs.");
            }
        }

        for (String f : expectedFiles) {
            File dd = new File(rootDir, f);
            if (!dd.exists()) {
                throw new IOException("Expected File " + f + " not found");
            }
        }

        //Walk and check that unexpected files are not there
        try (java.util.stream.Stream<Path> stream = Files.walk(rootDir.toPath())) {
            stream.forEach((p) -> {
                for (String u : unexpectedFiles) {
                    if (p.toString().equals(u)) {
                        throw new RuntimeException("Seen unexpected path " + p);
                    }
                }
            });
        }

        File javaLauncher = new File(rootDir, "bin" + File.separator +
                (isWindows() ? "java.exe" : "java"));
        if (javaLauncher.exists()) {
            ProcessBuilder builder = new ProcessBuilder(javaLauncher.getAbsolutePath(),
                    "-version");
            long t = System.currentTimeMillis();
            Process process = builder.start();
            int ret = waitFor(process);
            javaExecutionTime += System.currentTimeMillis() - t;
            if (ret != 0) {
                throw new RuntimeException("java launcher execution failed, check logs.");
            }
        } else {
            throw new IOException("java launcher not found.");
        }

        // Check release file
        File release = new File(rootDir, "release");
        if (!release.exists()) {
            throw new IOException("Release file not generated");
        } else {
            Properties props = new Properties();
            try (FileInputStream fs = new FileInputStream(release)) {
                props.load(fs);
                String s = props.getProperty("MODULES");
                if (s == null) {
                    throw new IOException("No MODULES property in release");
                }
                if (!s.contains(module)) {
                    throw new IOException("Module not found in release file " + s);
                }
            }
        }

    }

    private int waitFor(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    public static void validate(Path jimage, List<String> expectedLocations,
            List<String> unexpectedPaths) throws IOException {
        BasicImageReader reader = BasicImageReader.open(jimage);
        // Validate expected locations
        List<String> seenLocations = new ArrayList<>();
        for (String loc : expectedLocations) {
            ImageLocation il = reader.findLocation(loc);
            if (il == null) {
                throw new IOException("Location " + loc + " not present in " + jimage);
            }
        }
        seenLocations.addAll(expectedLocations);

        for (String s : reader.getEntryNames()) {
            if (s.endsWith(".class") && !s.endsWith("module-info.class")) {
                ImageLocation il = reader.findLocation(s);
                try {
                    byte[] r = reader.getResource(il);
                    if(r == null) {
                        System.out.println("IL, compressed " +
                                il.getCompressedSize() + " uncompressed " +
                                il.getUncompressedSize());
                        throw new IOException("NULL RESOURCE " + s);
                    }
                    readClass(r);
                } catch (IOException ex) {
                    System.err.println(s + " ERROR " + ex);
                    throw ex;
                }
            }
            if (seenLocations.contains(s)) {
                seenLocations.remove(s);
            }
            for(String p : unexpectedPaths) {
                if (s.equals(p)) {
                    throw new IOException("Seen unexpected path " + s);
                }
            }
        }
        if (!seenLocations.isEmpty()) {
            throw new IOException("ImageReader did not return " + seenLocations);
        }
    }

    public long getJavaLauncherExecutionTime() {
        return javaExecutionTime;
    }

    public long getModuleLauncherExecutionTime() {
        return moduleExecutionTime;
    }

    public static void readClass(byte[] clazz) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(clazz)) {
            ClassFile.read(stream);
        } catch (ConstantPoolException e) {
            throw new IOException(e);
        }
    }
}
