 /*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004182 8028545
 * @summary Add support for profiles in javac
 * @modules java.desktop
 *          java.sql.rowset
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.security.auth
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.jvm.Profile;
import com.sun.tools.javac.jvm.Target;


public class ProfileOptionTest {
    public static void main(String... args) throws Exception {
        new ProfileOptionTest().run();
    }

    private final JavaCompiler javac = JavacTool.create();
    private final StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null);


    // ---------- Test cases, invoked reflectively via run. ----------

    @Test
    void testInvalidProfile_API() throws Exception {
        JavaFileObject fo = new StringJavaFileObject("Test.java", "class Test { }");
        String badName = "foo";
        List<String> opts = Arrays.asList("--release", "8", "-profile", badName);
        StringWriter sw = new StringWriter();
        try {
            JavacTask task = (JavacTask) javac.getTask(sw, fm, null, opts, null,
                Arrays.asList(fo));
            throw new Exception("expected exception not thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    void testInvalidProfile_CommandLine() throws Exception {
        String badName = "foo";
        String[] opts = { "--release", "8", "-profile", badName };
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(opts, pw);

        // sadly, command line errors are not (yet?) reported to
        // the diag listener
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out.trim());

        if (!out.contains("invalid profile: " + badName)) {
            error("expected message not found");
        }
    }

    @Test
    void testTargetProfileCombinations() throws Exception {
        JavaFileObject fo = new StringJavaFileObject("Test.java", "class Test { }");
        for (Target t: Target.values()) {
            switch (t) {
                case JDK1_1:
                case JDK1_2:
                case JDK1_3:
                case JDK1_4:
                case JDK1_5: // not supported
                    continue;
            }

            for (Profile p: Profile.values()) {
                List<String> opts = new ArrayList<>();
                opts.addAll(Arrays.asList("-source", t.name, "-target", t.name));
                opts.add("-Xlint:-options"); // don't warn about no -bootclasspath
                if (p != Profile.DEFAULT)
                    opts.addAll(Arrays.asList("-profile", p.name));

                IllegalStateException ise;
                StringWriter sw = new StringWriter();
                try {
                    JavacTask task = (JavacTask) javac.getTask(sw, fm, null, opts, null,
                            Arrays.asList(fo));
                    task.analyze();
                    ise = null;
                } catch (IllegalStateException e) {
                    ise = e;
                }

                // sadly, command line errors are not (yet?) reported to
                // the diag listener
                String out = sw.toString();
                if (!out.isEmpty())
                    System.err.println(out.trim());

                switch (t) {
                    case JDK1_8:
                        if (ise != null)
                            error("unexpected exception from compiler: " + ise);
                        break;
                    default:
                        if (p == Profile.DEFAULT)
                            break;
                        if (ise == null)
                            error("IllegalStateException not thrown as expected");
                        else if (t.compareTo(Target.JDK1_9) >= 0) {
                            if (!ise.getMessage().contains("option -profile " +
                                    "not allowed with target " + t.name)) {
                                error("exception not thrown as expected: " + ise);
                            }
                        } else if (!ise.getMessage().contains("profile " + p.name
                                    + " is not valid for target release " + t.name)) {
                            error("exception not thrown as expected: " + ise);
                        }
                        break;
                }
            }
        }
    }

    @Test
    void testClassesInProfiles() throws Exception {
        for (Profile p: Profile.values()) {
            for (Map.Entry<Profile, List<JavaFileObject>> e: testClasses.entrySet()) {
                for (JavaFileObject fo: e.getValue()) {
                    DiagnosticCollector<JavaFileObject> dl =
                            new DiagnosticCollector<JavaFileObject>();
                    List<String> opts = (p == Profile.DEFAULT)
                            ? Collections.<String>emptyList()
                            : Arrays.asList("--release", "8", "-profile", p.name);
                    JavacTask task = (JavacTask) javac.getTask(null, fm, dl, opts, null,
                            Arrays.asList(fo));
                    task.analyze();

                    List<String> expectDiagCodes = new ArrayList<>();
                    if (fo.getName().equals("TPolicyFile.java")) {
                        expectDiagCodes.add("compiler.warn.has.been.deprecated.for.removal");
                    }

                    if (p.value < e.getKey().value) {
                        expectDiagCodes.add("compiler.err.not.in.profile");
                    }

                    checkDiags(opts + " " + fo.getName(), dl.getDiagnostics(), expectDiagCodes);
                }
            }
        }
    }

    Map<Profile, List<JavaFileObject>> testClasses =
            new EnumMap<Profile, List<JavaFileObject>>(Profile.class);

    void initTestClasses() {
        // The following table assumes the existence of specific classes
        // in specific profiles, as defined in the Java SE 8 spec.
        init(Profile.COMPACT1,
                java.lang.String.class);

        init(Profile.COMPACT2,
                javax.xml.XMLConstants.class);

        //init(Profile.COMPACT3,
        //        javax.sql.rowset.Predicate.class,
        //        com.sun.security.auth.PolicyFile.class); // specifically included in 3

        init(Profile.COMPACT3,
                javax.sql.rowset.Predicate.class);

        init(Profile.DEFAULT,
                java.beans.BeanInfo.class);
    }

    void init(Profile p, Class<?>... classes) {
        List<JavaFileObject> srcs = new ArrayList<JavaFileObject>();
        for (Class<?> c: classes) {
            String name = "T" + c.getSimpleName();
            String src =
                    "class T" + name + "{" + "\n" +
                    "    Class<?> c = " + c.getName() + ".class;\n" +
                    "}";
            srcs.add(new StringJavaFileObject(name + ".java", src));
        }
        testClasses.put(p, srcs);
    }

    void checkDiags(String msg, List<Diagnostic<? extends JavaFileObject>> diags, List<String> expectDiagCodes) {
        System.err.print(msg);
        if (diags.isEmpty())
            System.err.println(" OK");
        else {
            System.err.println();
            System.err.println(diags);
        }

        List<String> foundDiagCodes = new ArrayList<String>();
        for (Diagnostic<? extends JavaFileObject> d: diags)
            foundDiagCodes.add(d.getCode());

        if (!foundDiagCodes.equals(expectDiagCodes)) {
            System.err.println("Found diag codes:    " + foundDiagCodes);
            System.err.println("Expected diag codes: " + expectDiagCodes);
            error("expected diagnostics not found");
        }
    }

    /** Marker annotation for test cases. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Test { }

    /** Run all test cases. */
    void run() throws Exception {
        try {
            initTestClasses();

            for (Method m: getClass().getDeclaredMethods()) {
                Annotation a = m.getAnnotation(Test.class);
                if (a != null) {
                    System.err.println(m.getName());
                    try {
                        m.invoke(this, new Object[] { });
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        throw (cause instanceof Exception) ? ((Exception) cause) : e;
                    }
                    System.err.println();
                }
            }

            if (errors > 0)
                throw new Exception(errors + " errors occurred");
        } finally {
            fm.close();
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    private static class StringJavaFileObject extends SimpleJavaFileObject {
        StringJavaFileObject(String name, String text) {
            super(URI.create(name), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean b) {
            return text;
        }
        private String text;
    }
}
