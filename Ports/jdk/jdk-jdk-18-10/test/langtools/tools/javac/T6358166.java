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
 * @bug 6358166
 * @summary -verbose reports absurd times when annotation processing involved
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.*;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;


@SupportedAnnotationTypes("*")
public class T6358166 extends AbstractProcessor {
    public static void main(String... args) throws Throwable {
        String self = T6358166.class.getName();

        String testSrc = System.getProperty("test.src");

        JavacFileManager fm = new JavacFileManager(new Context(), false, null);
        JavaFileObject f = fm.getJavaFileObject(testSrc + File.separatorChar + self + ".java");

        List<String> addExports = Arrays.asList(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");

        test(fm, f, addExports, "-verbose", "-d", ".");

        test(fm, f, addExports, "-verbose", "-d", ".", "-XprintRounds", "-processorpath", ".", "-processor", self);
    }

    static void test(JavacFileManager fm, JavaFileObject f, List<String> addExports, String... args) throws Throwable {
        List<String> allArgs = new ArrayList<>();
        allArgs.addAll(addExports);
        allArgs.addAll(Arrays.asList(args));

        Context context = new Context();

        JavacTool tool = JavacTool.create();
        JavacTaskImpl task = (JavacTaskImpl) tool.getTask(null, fm, null, allArgs, null, List.of(f), context);
        task.call();

        JavaCompiler c = JavaCompiler.instance(context);
        if (c.errorCount() != 0)
            throw new AssertionError("compilation failed");

        long msec = c.elapsed_msec;
        if (msec < 0 || msec > 5 * 60 * 1000) // allow test 5 mins to execute, should be more than enough!
            throw new AssertionError("elapsed time is suspect: " + msec);
    }

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        return true;
    }
}
