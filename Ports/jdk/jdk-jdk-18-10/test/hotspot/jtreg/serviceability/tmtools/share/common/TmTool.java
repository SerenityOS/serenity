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

package common;

import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.Platform;

/**
 * A tool, such as jstat, jmap, etc Specific tools are defined as subclasses
 * parameterized by their corresponding ToolResults subclasses
 */
public abstract class TmTool<T extends ToolResults> {

    private final Class<T> resultsClz;
    private final String cmdLine;

    public TmTool(Class<T> resultsClz, String toolName, String otherArgs) {
        this.resultsClz = resultsClz;
        this.cmdLine = adjustForTestJava(toolName) + " " + otherArgs;
    }

    /**
     * Runs the tool to completion and returns the results
     *
     * @return the tool results
     * @throws Exception if anything goes wrong
     */
    public T measure() throws Exception {
        ToolRunner runner = new ToolRunner(cmdLine);
        ToolResults rawResults = runner.runToCompletion();
        System.out.println("Process output: " + rawResults);
        return resultsClz.getDeclaredConstructor(ToolResults.class).newInstance(rawResults);
    }

    private String adjustForTestJava(String toolName) {
        // We need to make sure we are running the tol from the JDK under testing
        String jdkPath = System.getProperty("test.jdk");
        if (jdkPath == null || !Paths.get(jdkPath).toFile().exists()) {
            throw new RuntimeException("property test.jdk not not set");
        }
        Path toolPath = Paths.get("bin", toolName + (Platform.isWindows() ? ".exe" : ""));
        return Paths.get(jdkPath, toolPath.toString()).toString();
    }

}
