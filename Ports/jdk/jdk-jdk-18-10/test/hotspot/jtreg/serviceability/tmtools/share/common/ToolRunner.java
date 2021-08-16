/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.time.Instant;

/**
 * This class starts a process specified by the passed command line waits till
 * the process completes and returns the process exit code and stdout and stderr
 * output as ToolResults
 */
class ToolRunner {
    private final String[] cmdArgs;

    ToolRunner(String cmdLine) {
        cmdArgs = cmdLine.split(" +");
    }

    /**
     * Starts the process, waits for the process completion and returns the
     * results
     *
     * @return process results
     * @throws Exception if anything goes wrong
     */
    ToolResults runToCompletion() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);
        Path out = Files.createTempFile(Paths.get("."), "out.", ".txt");
        Path err = out.resolveSibling(out.getFileName().toString().replaceFirst("out", "err"));

        Process p = pb.redirectOutput(ProcessBuilder.Redirect.to(out.toFile()))
                      .redirectError(ProcessBuilder.Redirect.to(err.toFile()))
                      .start();
        System.out.printf("[%s] started process %d %s with out/err redirected to '%s' and '%s'%n",
                Instant.now().toString(), p.pid(), pb.command(), out.toString(), err.toString());
        int exitCode = p.waitFor();
        System.out.printf("[%s] process %d finished with exit code = %d%n",
                Instant.now().toString(), p.pid(), exitCode);
        return new ToolResults(exitCode, Files.readAllLines(out), Files.readAllLines(err));
    }
}
