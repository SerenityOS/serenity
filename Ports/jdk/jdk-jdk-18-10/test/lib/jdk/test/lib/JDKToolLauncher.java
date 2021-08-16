/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A utility for constructing command lines for starting JDK tool processes.
 *
 * The JDKToolLauncher can in particular be combined with a
 * java.lang.ProcessBuilder to easily run a JDK tool. For example, the following
 * code run {@code jmap -heap} against a process with GC logging turned on for
 * the {@code jmap} process:
 *
 * <pre>
 * {@code
 * JDKToolLauncher jmap = JDKToolLauncher.create("jmap")
 *                                       .addVMArg("-XX:+PrintGC");
 *                                       .addVMArg("-XX:+PrintGCDetails")
 *                                       .addToolArg("-heap")
 *                                       .addToolArg(pid);
 * ProcessBuilder pb = new ProcessBuilder(jmap.getCommand());
 * Process p = pb.start();
 * }
 * </pre>
 */
public class JDKToolLauncher {
    private final String executable;
    private final List<String> vmArgs = new ArrayList<String>();
    private final List<String> toolArgs = new ArrayList<String>();

    private JDKToolLauncher(String tool, boolean useCompilerJDK) {
        if (useCompilerJDK) {
            executable = JDKToolFinder.getJDKTool(tool);
        } else {
            executable = JDKToolFinder.getTestJDKTool(tool);
        }
    }

    /**
     * Creates a new JDKToolLauncher for the specified tool. Using tools path
     * from the compiler JDK.
     *
     * @param tool
     *            The name of the tool
     * @return A new JDKToolLauncher
     */
    public static JDKToolLauncher create(String tool) {
        return new JDKToolLauncher(tool, true);
    }

    /**
     * Creates a new JDKToolLauncher for the specified tool in the Tested JDK.
     *
     * @param tool
     *            The name of the tool
     *
     * @return A new JDKToolLauncher
     */
    public static JDKToolLauncher createUsingTestJDK(String tool) {
        return new JDKToolLauncher(tool, false);
    }

    /**
     * Adds an argument to the JVM running the tool.
     *
     * The JVM arguments are passed to the underlying JVM running the tool.
     * Arguments will automatically be prepended with "-J".
     *
     * Any platform specific arguments required for running the tool are
     * automatically added.
     *
     *
     * @param arg
     *            The argument to VM running the tool
     * @return The JDKToolLauncher instance
     */
    public JDKToolLauncher addVMArg(String arg) {
        vmArgs.add(arg);
        return this;
    }

    /**
     * Adds arguments to the JVM running the tool.
     *
     * The JVM arguments are passed to the underlying JVM running the tool.
     * Arguments will automatically be prepended with "-J".
     *
     * Any platform specific arguments required for running the tool are
     * automatically added.
     *
     * @param args
     *            The arguments to VM running the tool
     * @return The JDKToolLauncher instance
     */
    public JDKToolLauncher addVMArgs(String[] args) {
        Stream.of(args).forEach(vmArgs::add);
        return this;
    }

    /**
     * Adds an argument to the tool.
     *
     * @param arg
     *            The argument to the tool
     * @return The JDKToolLauncher instance
     */
    public JDKToolLauncher addToolArg(String arg) {
        toolArgs.add(arg);
        return this;
    }

    /**
     * Returns the command that can be used for running the tool.
     *
     * @return An array whose elements are the arguments of the command.
     */
    public String[] getCommand() {
        List<String> command = new ArrayList<String>();
        command.add(executable);
        // Add -J in front of all vmArgs
        for (String arg : vmArgs) {
            command.add("-J" + arg);
        }
        command.addAll(toolArgs);
        return command.toArray(new String[command.size()]);
    }
}
