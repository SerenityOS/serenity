/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6930507
 * @summary Symbols for anonymous and local classes made too late for use by java tree API
 * @modules jdk.compiler
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import static javax.lang.model.util.ElementFilter.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

@SupportedOptions({"test", "last"})
@SupportedAnnotationTypes("*")
public class TestGetElement extends AbstractProcessor {
    public static void main(String... args) throws Exception {
        new TestGetElement().run();
    }

    public TestGetElement() { }

    public void run() throws Exception {
        final String testSrc = System.getProperty("test.src");
        final String testClasses = System.getProperty("test.classes");
        final String myClassName = getClass().getName();
        final String mySrc = new File(testSrc, myClassName + ".java").getPath();

        final int NUM_TESTS = 90; // #decls in this source file
        for (int i = 1; i <= NUM_TESTS; i++) {
            System.err.println("test " + i);
            File testDir = new File("test" + i);
            File classesDir = new File(testDir, "classes");
            classesDir.mkdirs();
            String[] args = {
                "-d", classesDir.getPath(),
                "-processorpath", testClasses,
                "-processor", myClassName,
                "-proc:only",
                "-Atest=" + i,
                "-Alast=" + (i == NUM_TESTS),
                mySrc
            };

//            System.err.println("compile: " + Arrays.asList(args));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            int rc = com.sun.tools.javac.Main.compile(args, pw);
            pw.close();
            String out = sw.toString();
            if (out != null)
                System.err.println(out);
            if (rc != 0) {
                System.err.println("compilation failed: rc=" + rc);
                errors++;
            }
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }


    int errors;

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment)
    {
        if (roundEnvironment.processingOver())
            return true;

        Map<String,String> options = processingEnv.getOptions();
        int test = Integer.parseInt(options.get("test"));
        boolean _last = Boolean.parseBoolean(options.get("last"));

        Trees trees = Trees.instance(processingEnv);
        Scanner scanner = new Scanner(trees, _last);
        int nelems = 0;
        for (TypeElement e : typesIn(roundEnvironment.getRootElements())) {
            nelems += scanner.scan(trees.getPath(e), test);
        }

        Messager m = processingEnv.getMessager();
        int EXPECT = 1;
        if (nelems != EXPECT) {
            m.printMessage(Diagnostic.Kind.ERROR,
                    "Unexpected number of elements found: " + nelems + " expected: " + EXPECT);
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    class Scanner extends TreePathScanner<Integer,Integer> {
        final Trees trees;
        final boolean last;
        int count;

        Scanner(Trees trees, boolean last) {
            this.trees = trees;
            this.last = last;
        }

        @Override
        public Integer visitClass(ClassTree tree, Integer test) {
            return reduce(check(test), super.visitClass(tree, test));
        }

        @Override
        public Integer visitMethod(MethodTree tree, Integer test) {
            return reduce(check(test), super.visitMethod(tree, test));
        }

        @Override
        public Integer visitVariable(VariableTree tree, Integer test) {
            return reduce(check(test), super.visitVariable(tree, test));
        }

        @Override
        public Integer reduce(Integer i1, Integer i2) {
            if (i1 == null || i1.intValue() == 0)
                return i2;
            if (i2 == null || i2.intValue() == 0)
                return i1;
            return (i1 + i2);
        }

        int check(int test) {
            count++;

            if (count != test)
                return 0;

            TreePath p = getCurrentPath();
            Element e = trees.getElement(p);

            String text = p.getLeaf().toString().replaceAll("\\s+", " ").trim();
            int MAXLEN = 40;
            if (text.length() > MAXLEN)
                text = text.substring(0, MAXLEN - 3) + "...";

            System.err.println(String.format("%3d: %-" + MAXLEN + "s -- %s",
                    count, text,
                    (e == null ? "null" : e.getKind() + " " + e)));

            Messager m = processingEnv.getMessager();
            if (e == null) {
                m.printMessage(Diagnostic.Kind.ERROR, "Null element found for " + text);
                return 0;
            }

            if (last && !e.getSimpleName().contentEquals("last")) {
                m.printMessage(Diagnostic.Kind.ERROR, "Unexpected name in last test: "
                        + e.getSimpleName() + ", expected: last");
            }

            return 1;
        }
    }

    // following are all fodder for the test

    class MemberClass {
        class NestedMemberClass { }
    }

    {
        class InnerClassInInit { }
        Object o = new Object() { };
    }

    TestGetElement(TestGetElement unused) {
        class InnerClassInConstr { }
        Object o = new Object() { };
    }

    void m() {
        class InnerClassInMethod { }
        Object o = new Object() { };

        class C {
            class MemberClass {
                class NestedMemberClass { }
            }

            {
                class InnerClassInInit { }
                Object o = new Object() { };
            }

            C(Object unused) {
                class InnerClassInConstr { }
                Object o = new Object() { };
            }

            void m() {
                class InnerClassInMethod { }
                Object o = new Object() { };
            }
        }
    }

    int last; // this name is verified by the test to make sure that all decls are checked
}
