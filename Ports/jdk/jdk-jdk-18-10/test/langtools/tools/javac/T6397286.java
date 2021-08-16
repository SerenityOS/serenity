/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6397286
 * @summary TaskListener calls are not protected agains user exceptions
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;

public class T6397286 {

    public static void main(String[] args) throws IOException {
        String testSrcDir = System.getProperty("test.src");
        String self = T6397286.class.getName();

        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrcDir, self + ".java")));

            JavacTask task = tool.getTask(null, fm, null, null, null, files);
            task.setTaskListener(new TaskListener() {
                    public void started(TaskEvent e) {
                        throw new TaskEventError(e);
                    }
                    public void finished(TaskEvent e) {
                    }
                });

            try {
                task.call();
                throw new AssertionError("no exception thrown");
            } catch (RuntimeException e) {
                if (e.getCause() instanceof TaskEventError) {
                    TaskEventError tee = (TaskEventError) e.getCause();
                    System.err.println("Exception thrown for " + tee.event + " as expected");
                } else {
                    e.printStackTrace();
                    throw new AssertionError("TaskEventError not thrown");
                }
            }
        }
    }
}

class TaskEventError extends Error {
    public TaskEventError(TaskEvent ev) {
        event = ev;
    }

    TaskEvent event;
}
