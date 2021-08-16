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

/**
 * @test
 * @bug 6341866
 * @summary Source files loaded from source path are not subject to annotation processing
 * @modules java.compiler
 *          jdk.compiler
 * @build Anno T6341866
 * @run main T6341866
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.tools.*;

/**
 * For each of a number of implicit compilation scenarios,
 * and for each of a set of annotation processing scenarios,
 * verify that a class file is generated, or not, for an
 * implicitly compiled source file and that the correct
 * warning message is given for implicitly compiled files
 * when annotation processing.
 */
public class T6341866 {
    static final String testSrc = System.getProperty("test.src", ".");
    static final String testClasses = System.getProperty("test.classes", ".");
    static final File a_java = new File(testSrc, "A.java");
    static final File a_class = new File("A.class");
    static final File b_java = new File(testSrc, "B.java");
    static final File b_class = new File("B.class");
    static final File processorServices = services(Processor.class);

    enum ImplicitType {
        NONE(null),                     // don't use implicit compilation
        OPT_UNSET(null),                // implicit compilation, but no -implicit option
        OPT_NONE("-implicit:none"),     // implicit compilation wiith -implicit:none
        OPT_CLASS("-implicit:class");   // implicit compilation wiith -implicit:class

        ImplicitType(String opt) {
            this.opt = opt;
        }
        final String opt;
    };

    enum AnnoType {
        NONE,           // no annotation processing
        SERVICE,        // implicit annotation processing, via ServiceLoader
        SPECIFY         // explicit annotation processing
    };


    public static void main(String ... args) throws Exception {
        boolean ok = true;

        // iterate over all combinations
        for (ImplicitType implicitType: EnumSet.allOf(ImplicitType.class)) {
            for (AnnoType annoType: EnumSet.allOf(AnnoType.class)) {
                ok &= test(implicitType, annoType);
            }
        }

        if (!ok)
            throw new AssertionError("test failed");
    }

    /**
     * Verify that a class file is generated, or not, for an implicitly compiled source file,
     * and that the correct warning message is given for implicitly compiled files when annotation processing.
     */
    static boolean test(ImplicitType implicitType, AnnoType annoType) throws IOException {
        System.err.println("test  implicit=" + implicitType + "  anno=" + annoType);

        // ensure clean start
        a_class.delete();
        b_class.delete();
        processorServices.delete();

        List<String> opts = new ArrayList<String>();
        opts.addAll(Arrays.asList("-d", ".", "-sourcepath", testSrc, "-classpath", testClasses, "-Xlint:-options"));
        if (implicitType.opt != null)
            opts.add(implicitType.opt);

        switch (annoType) {
        case SERVICE:
            createProcessorServices(Anno.class.getName());
            break;
        case SPECIFY:
            opts.addAll(Arrays.asList("-processor", Anno.class.getName()));
            break;
        }


        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        MyDiagListener dl = new MyDiagListener();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(dl, null, null)) {

            // Note: class A references class B, so compile A if we want implicit compilation
            File file =  (implicitType != ImplicitType.NONE) ? a_java : b_java;
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(file);

            //System.err.println("compile: " + opts + " " + files);

            boolean ok = javac.getTask(null, fm, dl, opts, null, files).call();
            if (!ok) {
                error("compilation failed");
                return false;
            }

            // check implicit compilation results if necessary
            if (implicitType != ImplicitType.NONE) {
                boolean expectClass = (implicitType != ImplicitType.OPT_NONE);
                if (b_class.exists() != expectClass) {
                    if (b_class.exists())
                        error("B implicitly compiled unexpectedly");
                    else
                        error("B not impliictly compiled");
                    return false;
                }
            }

            // check message key results
            String expectKey = null;
            if (implicitType == ImplicitType.OPT_UNSET) {
                switch (annoType) {
                case SERVICE:
                    expectKey = "compiler.warn.proc.use.proc.or.implicit";
                    break;
                case SPECIFY:
                    expectKey = "compiler.warn.proc.use.implicit";
                    break;
                }
            }

            if (expectKey == null) {
                if (dl.diagCodes.size() != 0) {
                    error("no diagnostics expected");
                    return false;
                }
            } else {
                if (!(dl.diagCodes.size() == 1 && dl.diagCodes.get(0).equals(expectKey))) {
                    error("unexpected diagnostics generated");
                    return false;
                }
            }

            return true;
        }
    }

    static void createProcessorServices(String name) throws IOException {
        processorServices.getParentFile().mkdirs();

        BufferedWriter out = new BufferedWriter(new FileWriter(processorServices));
        out.write(name);
        out.newLine();
        out.close();
    }

    static class MyDiagListener implements DiagnosticListener<JavaFileObject> {
        public void report(Diagnostic d) {
            diagCodes.add(d.getCode());
            System.err.println(d);
        }

        List<String> diagCodes = new ArrayList<String>();
    }

    static void error(String msg) {
        System.err.println("ERROR: " + msg);
    }

    static File services(Class<?> service) {
        String[] dirs = { testClasses, "META-INF", "services" };
        File dir = null;
        for (String d: dirs)
            dir = (dir == null ? new File(d) : new File(dir, d));

        return new File(dir, service.getName());
    }
}
