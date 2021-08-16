/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027232
 * @summary ensures that j.l.i.InvokerByteCodeGenerator and ASM visitMethodInsn
 * generate  bytecodes with correct constant pool references
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.zipfs
 * @compile -XDignore.symbol.file LambdaAsm.java LUtils.java
 * @run main/othervm LambdaAsm
 */
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.CPInfo;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;

import static java.nio.file.Files.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class LambdaAsm {

    static final File TestFile = new File("A.java");

    static void init() {
        emitCode();
        LUtils.compile(TestFile.getName());
        LUtils.TestResult tr = LUtils.doExec(LUtils.JAVA_CMD.getAbsolutePath(),
                "-Djdk.internal.lambda.dumpProxyClasses=.",
                "-cp", ".", "A");
        if (tr.exitValue != 0) {
            System.out.println("Error: " + tr.toString());
            throw new RuntimeException("could not create proxy classes");
        }
    }

    static void emitCode() {
        ArrayList<String> scratch = new ArrayList<>();
        scratch.add("import java.util.function.*;");
        scratch.add("class A {");
        scratch.add("   interface I {");
        scratch.add("       default Supplier<Integer> a() { return () -> 1; }");
        scratch.add("       default Supplier<Integer> b(int i) { return () -> i; }");
        scratch.add("       default Supplier<Integer> c(int i) { return () -> m(i); }");
        scratch.add("       int m(int i);");
        scratch.add("       static Integer d() { return 0; }");
        scratch.add("   }");
        scratch.add("   static class C implements I {");
        scratch.add("       public int m(int i) { return i;}");
        scratch.add("   }");
        scratch.add("   public static void main(String[] args) {");
        scratch.add("       I i = new C();");
        scratch.add("       i.a();");
        scratch.add("       i.b(1);");
        scratch.add("       i.c(1);");
        scratch.add("       I.d();");
        scratch.add("   }");
        scratch.add("}");
        LUtils.createFile(TestFile, scratch);
    }

    static void checkMethod(String cname, String mname, ConstantPool cp,
            Code_attribute code) throws ConstantPool.InvalidIndex {
        for (Instruction i : code.getInstructions()) {
            String iname = i.getMnemonic();
            if ("invokespecial".equals(iname)
                    || "invokestatic".equals(iname)) {
                int idx = i.getByte(2);
                System.out.println("Verifying " + cname + ":" + mname +
                        " instruction:" + iname + " index @" + idx);
                CPInfo cpinfo = cp.get(idx);
                if (cpinfo instanceof ConstantPool.CONSTANT_Methodref_info) {
                    throw new RuntimeException("unexpected CP type expected "
                            + "InterfaceMethodRef, got MethodRef, " + cname
                            + ", " + mname);
                }
            }
        }
    }

    static int checkMethod(ClassFile cf, String mthd) throws Exception {
        if (cf.major_version < 52) {
            throw new RuntimeException("unexpected class file version, in "
                    + cf.getName() + "expected 52, got " + cf.major_version);
        }
        int count = 0;
        for (Method m : cf.methods) {
            String mname = m.getName(cf.constant_pool);
            if (mname.equals(mthd)) {
                for (Attribute a : m.attributes) {
                    if ("Code".equals(a.getName(cf.constant_pool))) {
                        count++;
                        checkMethod(cf.getName(), mname, cf.constant_pool,
                                (Code_attribute) a);
                    }
                }
            }
        }
        return count;
    }

    static void verifyInvokerBytecodeGenerator() throws Exception {
        int count = 0;
        int mcount = 0;
        try (DirectoryStream<Path> ds = newDirectoryStream(new File(".").toPath(),
                // filter in lambda proxy classes
                "A$I$$Lambda$*.class")) {
            for (Path p : ds) {
                System.out.println(p.toFile());
                ClassFile cf = ClassFile.read(p.toFile());
                // Check those methods implementing Supplier.get
                mcount += checkMethod(cf, "get");
                count++;
            }
        }
        if (count < 3) {
            throw new RuntimeException("unexpected number of files, "
                    + "expected atleast 3 files, but got only " + count);
        }
        if (mcount < 3) {
            throw new RuntimeException("unexpected number of methods, "
                    + "expected atleast 3 methods, but got only " + mcount);
        }
    }

    static void verifyASM() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC, "X", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "foo",
                "()V", null, null);
        mv.visitMaxs(2, 1);
        mv.visitMethodInsn(INVOKESTATIC,
                "java/util/function/Function.class",
                "identity", "()Ljava/util/function/Function;", true);
        mv.visitInsn(RETURN);
        cw.visitEnd();
        byte[] carray = cw.toByteArray();
        // for debugging
        // write((new File("X.class")).toPath(), carray, CREATE, TRUNCATE_EXISTING);

        // verify using javap/classfile reader
        ClassFile cf = ClassFile.read(new ByteArrayInputStream(carray));
        int mcount = checkMethod(cf, "foo");
        if (mcount < 1) {
            throw new RuntimeException("unexpected method count, expected 1" +
                    "but got " + mcount);
        }
    }

    public static void main(String... args) throws Exception {
        init();
        verifyInvokerBytecodeGenerator();
        verifyASM();
    }
}
