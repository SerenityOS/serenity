/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * Utilities for process operations.
 */
public class ProcUtils {

    /*
     * Executes java program.
     * After the program finishes, it returns OutputAnalyzer.
     */
    public static OutputAnalyzer java(Path javaPath, Class<?> clazz,
            Map<String, String> props) {
        ProcessBuilder pb = createJavaProcessBuilder(javaPath, clazz, props);
        try {
            return ProcessTools.executeCommand(pb);
        } catch (Throwable e) {
            throw new RuntimeException("Executes java program failed!", e);
        }
    }

    private static ProcessBuilder createJavaProcessBuilder(Path javaPath,
            Class<?> clazz, Map<String, String> props) {
        List<String> cmds = new ArrayList<>();
        cmds.add(javaPath.toString());

        if (props != null) {
            for (Map.Entry<String, String> prop : props.entrySet()) {
                cmds.add("-D" + prop.getKey() + "=" + prop.getValue());
            }
        }

        cmds.add("-cp");
        cmds.add(System.getProperty("test.class.path"));
        cmds.add(clazz.getName());
        ProcessBuilder pb = new ProcessBuilder(cmds);
        pb.redirectErrorStream(true);
        return pb;
    }

    /*
     * Executes a shell command and return a OutputAnalyzer wrapping the process.
     */
    public static OutputAnalyzer shell(String command, Map<String, String> env)
            throws IOException {
        Process process = shellProc(command, null, env);
        return getProcessOutput(process);
    }

    /*
     * Executes win command and return a OutputAnalyzer wrapping the process.
     */
    public static OutputAnalyzer win(String command, Map<String, String> env)
            throws IOException {
        Process process = winProc(command, null, env);
        return getProcessOutput(process);
    }

    /*
     * Executes a shell command and return the process.
     */
    public static Process shellProc(String command, Path outputPath,
                                    Map<String, String> env) throws IOException {
        String[] cmds = new String[3];
        cmds[0] = "sh";
        cmds[1] = "-c";
        cmds[2] = command;
        return startAndGetProc(cmds, outputPath, env);
    }

    /*
     * Executes a win command and returns the process.
     */
    public static Process winProc(String command, Path outputPath,
                                  Map<String, String> env)
            throws IOException {
        String[] cmds = new String[3];
        cmds[0] = "cmd.exe";
        cmds[1] = "/C";
        cmds[2] = command;
        return startAndGetProc(cmds, outputPath, env);
    }

    /*
     * Returns a OutputAnalyzer wrapping the process.
     */
    private static OutputAnalyzer getProcessOutput (Process process) throws IOException {
        OutputAnalyzer oa = new OutputAnalyzer(process);
        try {
            process.waitFor();
            return oa;
        } catch (InterruptedException e) {
            throw new RuntimeException("Process is interrupted!", e);
        }
    }

    /*
     * Executes a command, redirects the output to a local file and returns the process.
     */
    private static Process startAndGetProc(String[] cmds, Path outputPath, Map<String,
            String> env) throws IOException {
        System.out.println("command to run: " + Arrays.toString(cmds));
        ProcessBuilder pb = new ProcessBuilder(cmds);
        if (env != null) {
            pb.environment().putAll(env);
        }
        pb.redirectErrorStream(true);
        if (outputPath != null) {
            pb.redirectOutput(outputPath.toFile());
        }
        return pb.start();
    }
}
