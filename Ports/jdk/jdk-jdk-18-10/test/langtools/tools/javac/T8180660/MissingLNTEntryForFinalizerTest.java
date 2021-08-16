/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8180141
 * @summary Missing entry in LineNumberTable for break statement that jumps out of try-finally
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile -g MissingLNTEntryForFinalizerTest.java
 * @run main MissingLNTEntryForFinalizerTest
 */

import java.io.File;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.util.List.of;
import static com.sun.tools.javac.tree.JCTree.Tag.*;

public class MissingLNTEntryForFinalizerTest {
    protected ReusableJavaCompiler tool;
    Context context;

    MissingLNTEntryForFinalizerTest() {
        context = new Context();
        JavacFileManager.preRegister(context);
        MyAttr.preRegister(context);
        tool = new ReusableJavaCompiler(context);
    }

    public static void main(String... args) throws Throwable {
        new MissingLNTEntryForFinalizerTest().test();
    }

    void test() throws Throwable {
        JavaSource source = new JavaSource("1");
        tool.clear();
        List<JavaFileObject> inputs = of(source);
        try {
            tool.compile(inputs);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
        File testClasses = new File(".");
        File file = new File(testClasses, "Test1.class");
        ClassFile classFile = ClassFile.read(file);
        for (Method m : classFile.methods) {
            if (classFile.constant_pool.getUTF8Value(m.name_index).equals("foo")) {
                Code_attribute code = (Code_attribute)m.attributes.get(Attribute.Code);
                LineNumberTable_attribute lnt = (LineNumberTable_attribute)code.attributes.get(Attribute.LineNumberTable);
                checkLNT(lnt, MyAttr.lineNumber);
            }
        }
    }

    void checkLNT(LineNumberTable_attribute lnt, int lineToCheckFor) {
        for (LineNumberTable_attribute.Entry e: lnt.line_number_table) {
            if (e.line_number == lineToCheckFor) {
                return;
            }
        }
        throw new AssertionError("seek line number not found in the LNT for method foo()");
    }

    class JavaSource extends SimpleJavaFileObject {
        String id;
        String template =
                "import java.util.*;\n" +
                "class Test#Id {\n" +
                "    void foo() {\n" +
                "        List<String> l = null;\n" +
                "        String first = null;\n" +
                "        try {\n" +
                "            first = l.get(0);\n" +
                "        } finally {\n" +
                "            if (first != null) {\n" +
                "                System.out.println(\"finalizer\");\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        JavaSource(String id) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.id = id;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return template.replace("#Id", id);
        }
    }

    /* this class has been set up to do not depend on a fixed line number, this Attr subclass will
     * look for 'break' or 'continue' statements in order to find the actual line number they occupy.
     * This way the test can find if that line number appears in the LNT generated for a given class.
     */
    static class MyAttr extends Attr {
        static int lineNumber;

        static void preRegister(Context context) {
            context.put(attrKey, (com.sun.tools.javac.util.Context.Factory<Attr>) c -> new MyAttr(c));
        }

        MyAttr(Context context) {
            super(context);
        }

        @Override
        public com.sun.tools.javac.code.Type attribStat(JCTree tree, Env<AttrContext> env) {
            com.sun.tools.javac.code.Type result = super.attribStat(tree, env);
            if (tree.hasTag(TRY)) {
                JCTry tryTree = (JCTry)tree;
                lineNumber = env.toplevel.lineMap.getLineNumber(tryTree.finalizer.endpos);
            }
            return result;
        }
    }

    static class ReusableJavaCompiler extends JavaCompiler {
        ReusableJavaCompiler(Context context) {
            super(context);
        }

        @Override
        protected void checkReusable() {
            // do nothing
        }

        @Override
        public void close() {
            //do nothing
        }

        void clear() {
            newRound();
            Modules.instance(context).newRound();
        }
    }
}
