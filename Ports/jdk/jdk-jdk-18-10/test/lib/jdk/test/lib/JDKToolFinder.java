/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class JDKToolFinder {

    private JDKToolFinder() {
    }

    /**
     * Returns the full path to an executable in jdk/bin based on System
     * property {@code test.jdk} or {@code compile.jdk} (both are set by the jtreg test suite)
     *
     * @return Full path to an executable in jdk/bin
     */
    public static String getJDKTool(String tool) {

        // First try to find the executable in test.jdk
        try {
            return getTool(tool, "test.jdk");
        } catch (FileNotFoundException e) {

        }

        // Now see if it's available in compile.jdk
        try {
            return getTool(tool, "compile.jdk");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to find " + tool +
                    ", looked in test.jdk (" + System.getProperty("test.jdk") +
                    ") and compile.jdk (" + System.getProperty("compile.jdk") + ")");
        }
    }

    /**
     * Returns the full path to an executable in jdk/bin based on System
     * property {@code compile.jdk}
     *
     * @return Full path to an executable in jdk/bin
     */
    public static String getCompileJDKTool(String tool) {
        try {
            return getTool(tool, "compile.jdk");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the full path to an executable in jdk/bin based on System
     * property {@code test.jdk}
     *
     * @return Full path to an executable in jdk/bin
     */
    public static String getTestJDKTool(String tool) {
        try {
            return getTool(tool, "test.jdk");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getTool(String tool, String property) throws FileNotFoundException {
        String jdkPath = System.getProperty(property);

        if (jdkPath == null) {
            throw new RuntimeException(
                    "System property '" + property + "' not set. This property is normally set by jtreg. "
                    + "When running test separately, set this property using '-D" + property + "=/path/to/jdk'.");
        }

        Path toolName = Paths.get("bin", tool + (Platform.isWindows() ? ".exe" : ""));

        Path jdkTool = Paths.get(jdkPath, toolName.toString());
        if (!jdkTool.toFile().exists()) {
            throw new FileNotFoundException("Could not find file " + jdkTool.toAbsolutePath());
        }

        return jdkTool.toAbsolutePath().toString();
    }
}
