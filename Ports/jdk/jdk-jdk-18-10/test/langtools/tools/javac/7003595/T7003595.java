/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7003595
 * @summary IncompatibleClassChangeError with unreferenced local class with subclass
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.InnerClasses_attribute;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.api.JavacTool;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.ArrayList;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


public class T7003595 {

    /** global decls ***/

    //statistics
    static int checkCount = 0;

    enum ClassKind {
        NESTED("static class #N { #B }", "$", true),
        INNER("class #N { #B }", "$", false),
        LOCAL_REF("void test() { class #N { #B }; new #N(); }", "$1", false),
        LOCAL_NOREF("void test() { class #N { #B }; }", "$1", false),
        ANON("void test() { new Object() { #B }; }", "$1", false),
        NONE("", "", false);

        String memberInnerStr;
        String sep;
        boolean staticAllowed;

        private ClassKind(String memberInnerStr, String sep, boolean staticAllowed) {
            this.memberInnerStr = memberInnerStr;
            this.sep = sep;
            this.staticAllowed = staticAllowed;
        }

        String getSource(String className, String outerName, String nested) {
            return memberInnerStr.replaceAll("#O", outerName).
                    replaceAll("#N", className).replaceAll("#B", nested);
        }

        static String getClassfileName(String[] names, ClassKind[] outerKinds, int pos) {
            System.out.println(" pos = " + pos + " kind = " + outerKinds[pos] + " sep = " + outerKinds[pos].sep);
            String name = outerKinds[pos] != ANON ?
                    names[pos] : "";
            if (pos == 0) {
                return "Test" + outerKinds[pos].sep + name;
            } else {
                String outerStr = getClassfileName(names, outerKinds, pos - 1);
                return outerStr + outerKinds[pos].sep + name;
            }
        }

        boolean isAllowed(ClassKind nestedKind) {
            return nestedKind != NESTED ||
                    staticAllowed;
        }
    }

    enum LocalInnerClass {
        LOCAL_REF("class L {}; new L();", "Test$1L"),
        LOCAL_NOREF("class L {};", "Test$1L"),
        ANON("new Object() {};", "Test$1"),
        NONE("", "");

        String localInnerStr;
        String canonicalInnerStr;

        private LocalInnerClass(String localInnerStr, String canonicalInnerStr) {
            this.localInnerStr = localInnerStr;
            this.canonicalInnerStr = canonicalInnerStr;
        }
    }

    public static void main(String... args) throws Exception {
        // Create a single file manager and reuse it for each compile to save time.
        try (StandardJavaFileManager fm = JavacTool.create().getStandardFileManager(null, null, null)) {
            for (ClassKind ck1 : ClassKind.values()) {
                String cname1 = "C1";
                for (ClassKind ck2 : ClassKind.values()) {
                    if (!ck1.isAllowed(ck2)) continue;
                    String cname2 = "C2";
                    for (ClassKind ck3 : ClassKind.values()) {
                        if (!ck2.isAllowed(ck3)) continue;
                        String cname3 = "C3";
                        new T7003595(fm, new ClassKind[] {ck1, ck2, ck3}, new String[] { cname1, cname2, cname3 }).compileAndCheck();
                    }
                }
            }
        }

        System.out.println("Total checks made: " + checkCount);
    }

    /** instance decls **/

    ClassKind[] cks;
    String[] cnames;
    StandardJavaFileManager fm;

    T7003595(StandardJavaFileManager fm, ClassKind[] cks, String[] cnames) {
        this.fm = fm;
        this.cks = cks;
        this.cnames = cnames;
    }

    void compileAndCheck() throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavaSource source = new JavaSource();
        JavacTask ct = (JavacTask)tool.getTask(null, fm, null,
                null, null, Arrays.asList(source));
        ct.call();
        verifyBytecode(source);
    }

    void verifyBytecode(JavaSource source) {
        for (int i = 0; i < 3 ; i ++) {
            if (cks[i] == ClassKind.NONE) break;
            checkCount++;
            String filename = cks[i].getClassfileName(cnames, cks, i);
            File compiledTest = new File(filename + ".class");
            try {
                ClassFile cf = ClassFile.read(compiledTest);
                if (cf == null) {
                    throw new Error("Classfile not found: " + filename);
                }

                InnerClasses_attribute innerClasses = (InnerClasses_attribute)cf.getAttribute(Attribute.InnerClasses);

                ArrayList<String> foundInnerSig = new ArrayList<>();
                if (innerClasses != null) {
                    for (InnerClasses_attribute.Info info : innerClasses.classes) {
                        String foundSig = info.getInnerClassInfo(cf.constant_pool).getName();
                        foundInnerSig.add(foundSig);
                    }
                }

                ArrayList<String> expectedInnerSig = new ArrayList<>();
                //add inner class (if any)
                if (i < 2 && cks[i + 1] != ClassKind.NONE) {
                    expectedInnerSig.add(cks[i + 1].getClassfileName(cnames, cks, i + 1));
                }
                //add inner classes
                for (int j = 0 ; j != i + 1 && j < 3; j++) {
                    expectedInnerSig.add(cks[j].getClassfileName(cnames, cks, j));
                }

                if (expectedInnerSig.size() != foundInnerSig.size()) {
                    throw new Error("InnerClasses attribute for " + cnames[i] + " has wrong size\n" +
                                    "expected " + expectedInnerSig.size() + "\n" +
                                    "found " + innerClasses.number_of_classes + "\n" +
                                    source);
                }

                for (String foundSig : foundInnerSig) {
                    if (!expectedInnerSig.contains(foundSig)) {
                        throw new Error("InnerClasses attribute for " + cnames[i] + " has unexpected signature: " +
                                foundSig + "\n" + source + "\n" + expectedInnerSig);
                    }
                }

                for (String expectedSig : expectedInnerSig) {
                    if (!foundInnerSig.contains(expectedSig)) {
                        throw new Error("InnerClasses attribute for " + cnames[i] + " does not contain expected signature: " +
                                    expectedSig + "\n" + source);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error("error reading " + compiledTest +": " + e);
            }
        }
    }

    class JavaSource extends SimpleJavaFileObject {

        static final String source_template = "class Test { #C }";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            String c3 = cks[2].getSource(cnames[2], cnames[1], "");
            String c2 = cks[1].getSource(cnames[1], cnames[0], c3);
            String c1 = cks[0].getSource(cnames[0], "Test", c2);
            source = source_template.replace("#C", c1);
        }

        @Override
        public String toString() {
            return source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
