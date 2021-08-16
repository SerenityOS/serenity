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
 * @bug 6199075
 *
 * @summary Unambiguous varargs method calls flagged as ambiguous
 * @author mcimadamore
 *
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.classfile.Method;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.List;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class T6199075 {

    int checkCount = 0;
    int bytecodeCheckCount = 0;

    enum TypeKind {
        BYTE("byte", "(byte)1", "[B", 0),
        CHAR("char", "'c'", "[C", 1),
        SHORT("short", "(short)1", "[S", 2),
        INT("int", "1", "[I", 3),
        LONG("long", "1L", "[J", 4),
        FLOAT("float", "1.0F", "[F", 5),
        DOUBLE("double", "1.0D", "[D", 6),
        BOOLEAN("boolean", "true", "[Z", -1);

        String typeString;
        String valueString;
        String bytecodeString;
        private int subtypeTag;

        TypeKind(String typeString, String valueString, String bytecodeString, int subtypeTag) {
            this.typeString = typeString;
            this.valueString = valueString;
            this.bytecodeString = bytecodeString;
            this.subtypeTag = subtypeTag;
        }

        boolean isSubtypeOf(TypeKind that) {
            switch (this) {
                case BOOLEAN:
                    return that == BOOLEAN;
                case BYTE:
                case CHAR:
                    return this.subtypeTag == that.subtypeTag ||
                            this.subtypeTag + 2 <= that.subtypeTag;
                default:
                    return this.subtypeTag <= that.subtypeTag;
            }
        }
    }

    enum ArgumentsArity {
        ZERO(0),
        ONE(1),
        TWO(2),
        THREE(3);

        int arity;

        ArgumentsArity(int arity) {
            this.arity = arity;
        }

        String asExpressionList(TypeKind type) {
            StringBuilder buf = new StringBuilder();
            String sep = "";
            for (int i = 0; i < arity; i++) {
                buf.append(sep);
                buf.append(type.valueString);
                sep = ",";
            }
            return buf.toString();
        }
    }

    static class VarargsMethod {
        TypeKind varargsElement;

        VarargsMethod(TypeKind varargsElement) {
            this.varargsElement = varargsElement;
        }

        @Override
        public String toString() {
            return "void m("+ varargsElement.typeString+ "... args) {}";
        }

        boolean isApplicable(TypeKind actual, ArgumentsArity argsArity) {
            return argsArity == ArgumentsArity.ZERO ||
                    actual.isSubtypeOf(varargsElement);
        }

        boolean isMoreSpecificThan(VarargsMethod that) {
            return varargsElement.isSubtypeOf(that.varargsElement);
        }
    }

    public static void main(String... args) throws Exception {
        new T6199075().test();
    }

    void test() throws Exception {
        try {
            for (TypeKind formal1 : TypeKind.values()) {
                VarargsMethod m1 = new VarargsMethod(formal1);
                for (TypeKind formal2 : TypeKind.values()) {
                    VarargsMethod m2 = new VarargsMethod(formal2);
                    for (TypeKind actual : TypeKind.values()) {
                        for (ArgumentsArity argsArity : ArgumentsArity.values()) {
                            compileAndCheck(m1, m2, actual, argsArity);
                        }
                    }
                }
            }

            System.out.println("Total checks made: " + checkCount);
            System.out.println("Bytecode checks made: " + bytecodeCheckCount);
        } finally {
            fm.close();
        }
    }

    // Create a single file manager and reuse it for each compile to save time.
    StandardJavaFileManager fm = JavacTool.create().getStandardFileManager(null, null, null);

    void compileAndCheck(VarargsMethod m1, VarargsMethod m2, TypeKind actual, ArgumentsArity argsArity) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavaSource source = new JavaSource(m1, m2, actual, argsArity);
        ErrorChecker ec = new ErrorChecker();
        JavacTask ct = (JavacTask)tool.getTask(null, fm, ec,
                null, null, Arrays.asList(source));
        ct.generate();
        check(source, ec, m1, m2, actual, argsArity);
    }

    void check(JavaSource source, ErrorChecker ec, VarargsMethod m1, VarargsMethod m2, TypeKind actual, ArgumentsArity argsArity) {
        checkCount++;
        boolean resolutionError = false;
        VarargsMethod selectedMethod = null;

        boolean m1_applicable = m1.isApplicable(actual, argsArity);
        boolean m2_applicable = m2.isApplicable(actual, argsArity);

        if (!m1_applicable && !m2_applicable) {
            resolutionError = true;
        } else if (m1_applicable && m2_applicable) {
            //most specific
            boolean m1_moreSpecific = m1.isMoreSpecificThan(m2);
            boolean m2_moreSpecific = m2.isMoreSpecificThan(m1);
            resolutionError = m1_moreSpecific == m2_moreSpecific;
            selectedMethod = m1_moreSpecific ? m1 : m2;
        } else {
            selectedMethod = m1_applicable ?
                m1 : m2;
        }

        if (ec.errorFound != resolutionError) {
            throw new Error("invalid diagnostics for source:\n" +
                    source.getCharContent(true) +
                    "\nExpected resolution error: " + resolutionError +
                    "\nFound error: " + ec.errorFound +
                    "\nCompiler diagnostics:\n" + ec.printDiags());
        } else if (!resolutionError) {
            verifyBytecode(selectedMethod);
        }
    }

    void verifyBytecode(VarargsMethod selected) {
        bytecodeCheckCount++;
        File compiledTest = new File("Test.class");
        try {
            ClassFile cf = ClassFile.read(compiledTest);
            Method testMethod = null;
            for (Method m : cf.methods) {
                if (m.getName(cf.constant_pool).equals("test")) {
                    testMethod = m;
                    break;
                }
            }
            if (testMethod == null) {
                throw new Error("Test method not found");
            }
            Code_attribute ea = (Code_attribute)testMethod.attributes.get(Attribute.Code);
            if (testMethod == null) {
                throw new Error("Code attribute for test() method not found");
            }

            for (Instruction i : ea.getInstructions()) {
                if (i.getMnemonic().equals("invokevirtual")) {
                    int cp_entry = i.getUnsignedShort(1);
                    CONSTANT_Methodref_info methRef =
                            (CONSTANT_Methodref_info)cf.constant_pool.get(cp_entry);
                    String type = methRef.getNameAndTypeInfo().getType();
                    if (!type.contains(selected.varargsElement.bytecodeString)) {
                        throw new Error("Unexpected type method call: " + type);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + compiledTest +": " + e);
        }
    }

    static class JavaSource extends SimpleJavaFileObject {

        static final String source_template = "class Test {\n" +
                "   #V1\n" +
                "   #V2\n" +
                "   void test() { m(#E); }\n" +
                "}";

        String source;

        public JavaSource(VarargsMethod m1, VarargsMethod m2, TypeKind actual, ArgumentsArity argsArity) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = source_template.replaceAll("#V1", m1.toString()).
                    replaceAll("#V2", m2.toString()).
                    replaceAll("#E", argsArity.asExpressionList(actual));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    static class ErrorChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound;
        List<String> errDiags = List.nil();

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errDiags = errDiags.append(diagnostic.getMessage(Locale.getDefault()));
                errorFound = true;
            }
        }

        String printDiags() {
            StringBuilder buf = new StringBuilder();
            for (String s : errDiags) {
                buf.append(s);
                buf.append("\n");
            }
            return buf.toString();
        }
    }
}
