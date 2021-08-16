/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.dcmd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Executes Diagnostic Commands on the target VM (specified by pid) using the jcmd tool and its ability to read
 *          Diagnostic Commands from a file.
 */
public class FileJcmdExecutor extends PidJcmdExecutor {

    /**
     * Instantiates a new FileJcmdExecutor targeting the current VM
     */
    public FileJcmdExecutor() {
        super();
    }

    /**
     * Instantiates a new FileJcmdExecutor targeting the VM indicated by the given pid
     *
     * @param target Pid of the target VM
     */
    public FileJcmdExecutor(String target) {
        super(target);
    }

    protected List<String> createCommandLine(String cmd) throws CommandExecutorException {
        File cmdFile = createTempFile();
        writeCommandToTemporaryFile(cmd, cmdFile);

        return Arrays.asList(jcmdBinary, Long.toString(pid),
                "-f", cmdFile.getAbsolutePath());
    }

    private void writeCommandToTemporaryFile(String cmd, File cmdFile) {
        try (PrintWriter pw = new PrintWriter(cmdFile)) {
            pw.println(cmd);
        } catch (IOException e) {
            String message = "Could not write to file: " + cmdFile.getAbsolutePath();
            throw new CommandExecutorException(message, e);
        }
    }

    private File createTempFile() {
        try {
            File cmdFile = File.createTempFile("input", "jcmd");
            cmdFile.deleteOnExit();
            return cmdFile;
        } catch (IOException e) {
            throw new CommandExecutorException("Could not create temporary file", e);
        }
    }

}
