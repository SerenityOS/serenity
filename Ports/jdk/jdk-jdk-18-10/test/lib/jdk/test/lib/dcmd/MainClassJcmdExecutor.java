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

import java.util.Arrays;
import java.util.List;

/**
 * Executes Diagnostic Commands on the target VM (specified by main class) using the jcmd tool
 */
public class MainClassJcmdExecutor extends JcmdExecutor {
    private final String mainClass;

    /**
     * Instantiates a new MainClassJcmdExecutor targeting the current VM
     */
    public MainClassJcmdExecutor() {
        super();
        mainClass = System.getProperty("sun.java.command").split(" ")[0];
    }

    /**
     * Instantiates a new MainClassJcmdExecutor targeting the VM indicated by the given main class
     *
     * @param target Main class of the target VM
     */
    public MainClassJcmdExecutor(String target) {
        super();
        mainClass = target;
    }

    protected List<String> createCommandLine(String cmd) throws CommandExecutorException {
        return Arrays.asList(jcmdBinary, mainClass, cmd);
    }

}
