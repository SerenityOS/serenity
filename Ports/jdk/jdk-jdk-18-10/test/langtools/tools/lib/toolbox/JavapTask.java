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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A task to configure and run the disassembler tool, javap.
 */
public class JavapTask extends AbstractTask<JavapTask> {
    private String classpath;
    private List<String> options;
    private List<String> classes;

    /**
     * Create a task to execute {@code javap} using {@code CMDLINE} mode.
     * @param toolBox the {@code ToolBox} to use
     */
    public JavapTask(ToolBox toolBox) {
        super(toolBox, Task.Mode.CMDLINE);
    }

    /**
     * Sets the classpath.
     * @param classpath the classpath
     * @return this task object
     */
    public JavapTask classpath(String classpath) {
        this.classpath = classpath;
        return this;
    }

    /**
     * Sets the options.
     * @param options the options
     * @return this task object
     */
    public JavapTask options(String... options) {
        this.options = Arrays.asList(options);
        return this;
    }

    /**
     * Sets the classes to be analyzed.
     * @param classes the classes
     * @return this task object
     */
    public JavapTask classes(String... classes) {
        this.classes = Arrays.asList(classes);
        return this;
    }

    /**
     * {@inheritDoc}
     * @return the name "javap"
     */
    @Override
    public String name() {
        return "javap";
    }

    /**
     * Calls the javap tool with the arguments as currently configured.
     * @return a Result object indicating the outcome of the task
     * and the content of any output written to stdout, stderr, or the
     * main stream.
     * @throws TaskError if the outcome of the task is not as expected.
     */
    @Override
    public Task.Result run() {
        List<String> args = new ArrayList<>();
        if (options != null)
            args.addAll(options);
        if (classpath != null) {
            args.add("-classpath");
            args.add(classpath);
        }
        if (classes != null)
            args.addAll(classes);

        AbstractTask.WriterOutput direct = new AbstractTask.WriterOutput();
        // These are to catch output to System.out and System.err,
        // in case these are used instead of the primary streams
        AbstractTask.StreamOutput sysOut = new AbstractTask.StreamOutput(System.out, System::setOut);
        AbstractTask.StreamOutput sysErr = new AbstractTask.StreamOutput(System.err, System::setErr);

        int rc;
        Map<Task.OutputKind, String> outputMap = new HashMap<>();
        try {
            rc = com.sun.tools.javap.Main.run(args.toArray(new String[args.size()]), direct.pw);
        } finally {
            outputMap.put(Task.OutputKind.STDOUT, sysOut.close());
            outputMap.put(Task.OutputKind.STDERR, sysErr.close());
            outputMap.put(Task.OutputKind.DIRECT, direct.close());
        }
        return checkExit(new Task.Result(toolBox, this, rc, outputMap));
    }
}
