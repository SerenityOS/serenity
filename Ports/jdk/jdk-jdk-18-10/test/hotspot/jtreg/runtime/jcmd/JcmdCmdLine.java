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
 *
 */

/*
 * @test
 * @bug 8263640
 * @summary In case of a class path is longer than O_BUFLEN, the class path
 *          should not be truncated in the output from jcmd VM.command_line.
 * @library /test/lib
 * @run driver JcmdCmdLine
 */

import java.io.File;
import java.util.List;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;

public class JcmdCmdLine {
    private static LingeredApp theApp = null;
    private static final int BUFFER_LEN = 2000;
    private static final String JCMD_OPT = "VM.command_line";
    private static final String CLASS_PATH_LINE = "java_class_path (initial):";
    private static final String TRUNCATE_WARNING = "outputStream::do_vsnprintf output truncated";
    public static void main(String[] args) throws Exception {
        try {
            theApp = new LingeredApp();
            theApp.setUseDefaultClasspath(false);
            String classPath = System.getProperty("test.class.path");
            while (classPath.length() < BUFFER_LEN) {
                classPath += File.pathSeparator + classPath;
            }
            LingeredApp.startAppExactJvmOpts(theApp, "-cp",  classPath);
            long pid = theApp.getPid();

            PidJcmdExecutor cmdExecutor = new PidJcmdExecutor(String.valueOf(pid));
            OutputAnalyzer output = cmdExecutor.execute(JCMD_OPT, true/*silent*/);
            output.shouldHaveExitValue(0);
            boolean seenClassPath = false;
            List<String> lines = output.asLines();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(CLASS_PATH_LINE)) {
                    seenClassPath = true;
                    if (!line.endsWith(classPath)) {
                        throw new RuntimeException("Incomplete java_class_path line.");
                    }
                }
            }
            if (!seenClassPath) {
                throw new RuntimeException("Missing java_class_path line.");
            }
        } finally {
            LingeredApp.stopApp(theApp);
            if (theApp.getOutput().getStderr().contains(TRUNCATE_WARNING)) {
                throw new RuntimeException("Unexpected truncation warning.");
            }
        }
    }
}
