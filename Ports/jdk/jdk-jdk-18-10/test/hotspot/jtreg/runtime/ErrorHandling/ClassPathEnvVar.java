/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8271003
 * @summary CLASSPATH env variable setting should not be truncated in a hs err log.
 * @requires vm.debug
 * @library /test/lib
 * @run driver ClassPathEnvVar
 */
import java.io.File;
import java.util.Map;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ClassPathEnvVar {
    private static final String pathSep = File.pathSeparator;
    private static final String sep = File.separator;
    private static final String classPathEnv = "CLASSPATH";
    private static final String endPath = "end-path";

    public static void main(String[] args) throws Exception {
        OutputAnalyzer output = runCrasher().shouldContain("CreateCoredumpOnCrash turned off, no core file dumped")
                                             .shouldNotHaveExitValue(0);

        checkErrorLog(output);

    }
    private static OutputAnalyzer runCrasher() throws Exception {
        ProcessBuilder pb =
            ProcessTools.createJavaProcessBuilder("-XX:-CreateCoredumpOnCrash",
                                                  "-XX:ErrorHandlerTest=14",
                                                  "-XX:+ErrorFileToStdout");

        // Obtain the CLASSPATH setting and expand it to more than 2000 chars.
        Map<String, String> envMap = pb.environment();
        String cp = envMap.get(classPathEnv);
        if (cp == null) {
            cp = "this" + sep + "is" + sep + "dummy" + sep + "path";
        }
        while (cp.length() < 2000) {
            cp += pathSep + cp;
        }
        cp += pathSep + endPath;
        envMap.put(classPathEnv, cp);

        return new OutputAnalyzer(pb.start());
    }

    private static void checkErrorLog(OutputAnalyzer output) throws Exception {
        String classPathLine = output.firstMatch("CLASSPATH=.*");

        if (classPathLine == null) {
            throw new RuntimeException("CLASSPATH setting not found in hs err log.");
        }

        // Check if the CLASSPATH line has been truncated.
        if (!classPathLine.endsWith(endPath)) {
            throw new RuntimeException("CLASSPATH was truncated in the hs err log.");
        }
    }
}
