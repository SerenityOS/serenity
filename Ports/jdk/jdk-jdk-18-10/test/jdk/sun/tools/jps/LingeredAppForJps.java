/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.apps.LingeredApp;

public class LingeredAppForJps extends LingeredApp {

    // if set, the app is run from jar file
    private File jarFile;

    @Override
    protected void runAddAppName(List<String> cmd) {
        if (jarFile != null) {
            cmd.add("-Xdiag");
            cmd.add("-jar");
            cmd.add(jarFile.getAbsolutePath());
        } else {
            super.runAddAppName(cmd);
        }
    }

    /**
     * The jps output should contain processes' names
     * (except when jps is started in quite mode).
     * The expected name of the test process is prepared here.
     */
    public String getProcessName() {
        return jarFile == null
                ? getClass().getSimpleName()
                : jarFile.getName();
    }

    // full package name for the application's main class or the full path
    // name to the application's JAR file:
    public String getFullProcessName() {
        return jarFile == null
                ? getClass().getCanonicalName()
                : jarFile.getAbsolutePath();
    }

    public void buildJar() throws IOException {
        String className = LingeredAppForJps.class.getName();
        File jar = new File(className + ".jar");
        String testClassPath = System.getProperty("test.class.path", "?");

        // Classpath contains test class dir, libraries class dir(s), and
        // may contains some additional dirs.
        // We need to add to jar only classes from the test class directory.
        // Main class (this class) should only be found in one directory
        // from the classpath (test class dir), therefore only added once.
        // Libraries class dir(s) and any additional classpath directories
        // are written the jar manifest.

        File manifestFile = new File(className + ".mf");
        String nl = System.getProperty("line.separator");
        String manifestClasspath = "";

        List<String> jarArgs = new ArrayList<>();
        jarArgs.add("-cfm");
        jarArgs.add(jar.getAbsolutePath());
        jarArgs.add(manifestFile.getAbsolutePath());
        for (String path : testClassPath.split(File.pathSeparator)) {
            String classFullName = path + File.separator + className + ".class";
            File f = new File(classFullName);
            if (f.exists()) {
                jarArgs.add("-C");
                jarArgs.add(path);
                jarArgs.add(".");
                System.out.println("INFO: scheduled to jar " + path);
            } else {
                manifestClasspath += " " + new File(path).toURI();
            }
        }
        try (BufferedWriter output = new BufferedWriter(new FileWriter(manifestFile))) {
            output.write("Main-Class: " + className + nl);
            if (!manifestClasspath.isEmpty()) {
                output.write("Class-Path: " + manifestClasspath + nl);
            }
        }

        System.out.println("Running jar " + jarArgs.toString());
        sun.tools.jar.Main jarTool = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!jarTool.run(jarArgs.toArray(new String[jarArgs.size()]))) {
            throw new IOException("jar failed: args=" + jarArgs.toString());
        }

        manifestFile.delete();
        jar.deleteOnExit();

        // Print content of jar file
        System.out.println("Content of jar file" + jar.getAbsolutePath());

        jarArgs = new ArrayList<>();
        jarArgs.add("-tvf");
        jarArgs.add(jar.getAbsolutePath());

        jarTool = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!jarTool.run(jarArgs.toArray(new String[jarArgs.size()]))) {
            throw new IOException("jar failed: args=" + jarArgs.toString());
        }

        jarFile = jar;
    }

    public static void main(String args[]) {
        LingeredApp.main(args);
    }
 }
