/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6358024
 * @summary TaskListener should be propogated between processing rounds
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.*;
import java.util.*;
import java.util.List;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.*;


@SupportedAnnotationTypes("*")
public class T6358024 extends AbstractProcessor {
    static JavacFileManager fm;
    public static void main(String... args) throws Throwable {
        String self = T6358024.class.getName();

        String testSrc = System.getProperty("test.src");

        fm = new JavacFileManager(new Context(), false, null);
        JavaFileObject f = fm.getJavaFileObject(testSrc + File.separatorChar + self + ".java");

        test(fm, f,
             new Option[] { new Option("-d", ".")},
             8);

        test(fm, f,
             new Option[] { new XOption("-XprintRounds"),
                            new Option("-processorpath", "."),
                            new Option("-processor", self) },
             13);
    }

    static void test(JavacFileManager fm, JavaFileObject f, Option[] opts, int expect) throws Throwable {
        PrintWriter out = new PrintWriter(System.err, true);

        JavacTool tool = JavacTool.create();
        List<String> flags = new ArrayList<String>();
        flags.addAll(Arrays.asList(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"));
        for (Option opt: opts) {
            flags.add(opt.name);
            for (Object arg : opt.args)
                flags.add(arg.toString());
        }

        JavacTaskImpl task = (JavacTaskImpl) tool.getTask(out,
                                                          fm,
                                                          null,
                                                          flags,
                                                          null,
                                                          Arrays.asList(f));
        MyTaskListener tl = new MyTaskListener();
        task.setTaskListener(tl);
        task.call();
        if (tl.started != expect)
            throw new AssertionError("Unexpected number of TaskListener events; "
                                     + "expected " + expect + ", found " + tl.started);
    }

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        return true;
    }

    static class MyTaskListener implements TaskListener {
        public void started(TaskEvent e) {
            System.err.println("Started: " + e);
            started++;
        }
        public void finished(TaskEvent e) {
        }

        int started = 0;
    }

    static class Option {
        Option(String name, String... args) {
            this.name = name;
            this.args = args;
        }
        public final String name;
        public final String[] args;
    }

    static class XOption extends Option {
        XOption(String name, String... args) {
            super(name, args);
        }
    }
}
