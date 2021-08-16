/*
 * Copyright (c) 2018, Google LLC. All rights reserved.
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
 * @bug     8214071
 * @summary Broken msg.bug diagnostics when using the compiler API
 * @library lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 * @build ToolTester
 * @run main CrashReport
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.ClientCodeWrapper.Trusted;
import java.io.File;
import java.io.StringWriter;
import java.util.List;

public class CrashReport extends ToolTester {

    public static void main(String[] args) {
        new CrashReport().test();
    }

    void test() {
        StringWriter pw = new StringWriter();
        JavacTask task =
                (JavacTask)
                        tool.getTask(
                                pw,
                                fm,
                                null,
                                List.of(),
                                null,
                                fm.getJavaFileObjects(new File(test_src, "CrashReport.java")));
        task.addTaskListener(new Listener());
        boolean ok = task.call();
        if (ok) {
            throw new AssertionError("expected compilation to fail");
        }
        String output = pw.toString();
        if (!output.contains("An exception has occurred in the compiler")) {
            throw new AssertionError("expected msg.bug diagnostic, got:\n" + output);
        }
    }

    @Trusted
    static class Listener implements TaskListener {
        @Override
        public void started(TaskEvent e) {
            throw new AssertionError();
        }
    }
}
