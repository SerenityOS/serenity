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

package toolbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A task to configure and run the Java launcher.
 */
public class JavaTask extends AbstractTask<JavaTask> {
    boolean includeStandardOptions = true;
    private String classpath;
    private List<String> vmOptions;
    private String className;
    private List<String> classArgs;

    /**
     * Create a task to run the Java launcher, using {@code EXEC} mode.
     * @param toolBox the {@code ToolBox} to use
     */
    public JavaTask(ToolBox toolBox) {
        super(toolBox, Task.Mode.EXEC);
    }

    /**
     * Sets the classpath.
     * @param classpath the classpath
     * @return this task object
     */
    public JavaTask classpath(String classpath) {
        this.classpath = classpath;
        return this;
    }

    /**
     * Sets the VM options.
     * @param vmOptions the options
     * @return this task object
     */
    public JavaTask vmOptions(String... vmOptions) {
        this.vmOptions = Arrays.asList(vmOptions);
        return this;
    }

    /**
     * Sets the VM options.
     * @param vmOptions the options
     * @return this task object
     */
    public JavaTask vmOptions(List<String> vmOptions) {
        this.vmOptions = vmOptions;
        return this;
    }

    /**
     * Sets the name of the class to be executed.
     * @param className the name of the class
     * @return this task object
     */
    public JavaTask className(String className) {
        this.className = className;
        return this;
    }

    /**
     * Sets the arguments for the class to be executed.
     * @param classArgs the arguments
     * @return this task object
     */
    public JavaTask classArgs(String... classArgs) {
        this.classArgs = Arrays.asList(classArgs);
        return this;
    }

    /**
     * Sets the arguments for the class to be executed.
     * @param classArgs the arguments
     * @return this task object
     */
    public JavaTask classArgs(List<String> classArgs) {
        this.classArgs = classArgs;
        return this;
    }

    /**
     * Sets whether or not the standard VM and java options for the test should be passed
     * to the new VM instance. If this method is not called, the default behavior is that
     * the options will be passed to the new VM instance.
     *
     * @param includeStandardOptions whether or not the standard VM and java options for
     *                               the test should be passed to the new VM instance.
     * @return this task object
     */
    public JavaTask includeStandardOptions(boolean includeStandardOptions) {
        this.includeStandardOptions = includeStandardOptions;
        return this;
    }

    /**
     * {@inheritDoc}
     * @return the name "java"
     */
    @Override
    public String name() {
        return "java";
    }

    /**
     * Calls the Java launcher with the arguments as currently configured.
     * @return a Result object indicating the outcome of the task
     * and the content of any output written to stdout or stderr.
     * @throws TaskError if the outcome of the task is not as expected.
     */
    @Override
    public Task.Result run() {
        List<String> args = new ArrayList<>();
        args.add(toolBox.getJDKTool("java").toString());
        if (includeStandardOptions) {
            args.addAll(toolBox.split(System.getProperty("test.vm.opts"), " +"));
            args.addAll(toolBox.split(System.getProperty("test.java.opts"), " +"));
        }
        if (classpath != null) {
            args.add("-classpath");
            args.add(classpath);
        }
        if (vmOptions != null)
            args.addAll(vmOptions);
        if (className != null)
            args.add(className);
        if (classArgs != null)
            args.addAll(classArgs);
        ProcessBuilder pb = getProcessBuilder();
        pb.command(args);
        try {
            return runProcess(toolBox, this, pb.start());
        } catch (IOException | InterruptedException e) {
            throw new Error(e);
        }
    }
}
