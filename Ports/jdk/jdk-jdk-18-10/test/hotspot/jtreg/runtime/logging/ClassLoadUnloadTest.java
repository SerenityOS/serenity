/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test ClassLoadUnloadTest
 * @bug 8142506
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library classes
 * @build test.Empty
 * @run driver ClassLoadUnloadTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.test.lib.classloader.ClassUnloadCommon;

public class ClassLoadUnloadTest {
    private static OutputAnalyzer out;
    private static ProcessBuilder pb;
    private static class ClassUnloadTestMain {
        public static void main(String... args) throws Exception {
            String className = "test.Empty";
            ClassLoader cl = ClassUnloadCommon.newClassLoader();
            Class<?> c = cl.loadClass(className);
            cl = null; c = null;
            ClassUnloadCommon.triggerUnloading();
        }
    }

    static void checkFor(String... outputStrings) throws Exception {
        out = new OutputAnalyzer(pb.start());
        for (String s: outputStrings) {
            out.shouldContain(s);
        }
        out.shouldHaveExitValue(0);
    }

    static void checkAbsent(String... outputStrings) throws Exception {
        out = new OutputAnalyzer(pb.start());
        for (String s: outputStrings) {
            out.shouldNotContain(s);
        }
        out.shouldHaveExitValue(0);
    }

    // Use the same command-line heap size setting as ../ClassUnload/UnloadTest.java
    static ProcessBuilder exec(String... args) throws Exception {
        List<String> argsList = new ArrayList<>();
        Collections.addAll(argsList, args);
        Collections.addAll(argsList, "-Xmn8m");
        Collections.addAll(argsList, "-Dtest.class.path=" + System.getProperty("test.class.path", "."));
        Collections.addAll(argsList, ClassUnloadTestMain.class.getName());
        return ProcessTools.createJavaProcessBuilder(argsList);
    }

    public static void main(String... args) throws Exception {

        //  -Xlog:class+unload=info
        pb = exec("-Xlog:class+unload=info");
        checkFor("[class,unload]", "unloading class");

        //  -Xlog:class+unload=off
        pb = exec("-Xlog:class+unload=off");
        checkAbsent("[class,unload]");

        //  -Xlog:class+load=info
        pb = exec("-Xlog:class+load=info");
        checkFor("[class,load]", "java.lang.Object", "source:");

        //  -Xlog:class+load=debug
        pb = exec("-Xlog:class+load=debug");
        checkFor("[class,load]", "java.lang.Object", "source:", "klass:", "super:", "loader:", "bytes:");

        //  -Xlog:class+load=off
        pb = exec("-Xlog:class+load=off");
        checkAbsent("[class,load]");

        //  -verbose:class
        pb = exec("-verbose:class");
        checkFor("[class,load]", "java.lang.Object", "source:");
        checkFor("[class,unload]", "unloading class");

        //  -Xlog:class+loader+data=trace
        pb = exec("-Xlog:class+loader+data=trace");
        checkFor("[class,loader,data]", "create loader data");

    }
}
