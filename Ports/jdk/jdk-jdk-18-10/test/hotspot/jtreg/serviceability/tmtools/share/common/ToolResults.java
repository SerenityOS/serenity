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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Common results of running an executable such as the saved exit code, the
 * saved stdout and stderr
 */
public class ToolResults {

    public int getExitCode() {
        return exitCode;
    }

    public List<String> getStdout() {
        return stdout;
    }

    public List<String> getStderr() {
        return stderr;
    }

    public String getStdoutString() {
        return stdout.stream().collect(Collectors.joining(System.getProperty("line.separator")));
    }

    /**
     * Helper function to return a specified line from the saved stdout
     *
     * @return the line indexed with ndx from the saved stdout. The indexes are
     * zero-based so that getStdoutLine(0) returns the first line.
     */
    public String getStdoutLine(int ndx) {
        return stdout.get(ndx);
    }

    public ToolResults(int exitCode, List<String> stdin, List<String> stderr) {
        this.exitCode = exitCode;
        this.stdout = stdin;
        this.stderr = stderr;
    }

    public ToolResults(ToolResults rawResults) {
        this(rawResults.exitCode, rawResults.stdout, rawResults.stderr);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Exit code: ").append(exitCode).append("\n");
        sb.append("stdout:");
        stdout.stream().forEach((s) -> {
            sb.append(s).append("\n");
        });
        sb.append("stderr:");
        stderr.stream().forEach((s) -> {
            sb.append(s).append("\n");
        });
        return sb.toString();
    }

    private final int exitCode;
    private final List<String> stdout;
    private final List<String> stderr;

}
