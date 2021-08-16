/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileOutputStream;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test
 * @summary Test that anewarray bytecode is valid only if it specifies 254 or fewer dimensions.
 *          255 is invalid because the anewarray would then create an array with 256 dimensions.
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDignore.symbol.file TestANewArray.java
 * @run driver TestANewArray 49
 * @run driver TestANewArray 52
 */

/*
 * Testing anewarray instruction with 254, 255 & 264 dimensions to verify JVM Spec
 * Section 4.9.1, Static Constraints that states the following:
 *
 * "No anewarray instruction may be used to create an array of more than 255 dimensions."
 *
 */

public class TestANewArray {

    static String classCName = null; // the generated class name

    static final int test_Dimension_254 = 254; // should always pass
    static final int test_Dimension_255 = 255; // should always pass, except for cfv 49
    static final int test_Dimension_264 = 264; // should always fail

    static final String array_Dimension_254 = genArrayDim(test_Dimension_254);
    static final String array_Dimension_255 = genArrayDim(test_Dimension_255);
    static final String array_Dimension_264 = genArrayDim(test_Dimension_264);

    public static void main(String... args) throws Exception {
        int cfv = Integer.parseInt(args[0]);

        // 254 array dimensions
        byte[] classFile_254 = dumpClassFile(cfv, test_Dimension_254, array_Dimension_254);
        writeClassFileFromByteArray(classFile_254);
        System.err.println("Running with cfv: " + cfv + ", test_Dimension_254");
        ProcessBuilder pb = ProcessTools.createTestJvm("-verify", "-cp", ".",  classCName);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("java.lang.VerifyError");
        output.shouldHaveExitValue(0);

        // 255 array dimensions
        byte[] classFile_255 = dumpClassFile(cfv, test_Dimension_255, array_Dimension_255);
        writeClassFileFromByteArray(classFile_255);
        System.err.println("Running with cfv: " + cfv + ", test_Dimension_255");
        pb = ProcessTools.createTestJvm("-verify", "-cp", ".",  classCName);
        output = new OutputAnalyzer(pb.start());
        // If anewarray has an operand with 255 array dimensions then VerifyError should
        // be thrown because the resulting array would have 256 dimensions.
        output.shouldContain("java.lang.VerifyError");
        // VerifyError exception messages differ between verifiers.
        if (cfv == 49) {
            output.shouldContain("Array with too many dimensions");
        } else {
            output.shouldContain("Illegal anewarray instruction, array has more than 255 dimensions");
        }
        output.shouldHaveExitValue(1);

        // 264 array dimensions
        byte[] classFile_264 = dumpClassFile(cfv, test_Dimension_264, array_Dimension_264);
        writeClassFileFromByteArray(classFile_264);
        System.err.println("Running with cfv: " + cfv + ", test_Dimension_264");
        pb = ProcessTools.createTestJvm("-verify", "-cp", ".",  classCName);
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("java.lang.ClassFormatError");
        output.shouldHaveExitValue(1);
    }

    public static byte[] dumpClassFile(int cfv, int testDimension264, String arrayDim) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv;

        classCName = "classCName_" + cfv + "_" + testDimension264;

        cw.visit(cfv, ACC_PUBLIC + ACC_SUPER, classCName, null, "java/lang/Object", null);
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {   // classCName main method
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitIntInsn(BIPUSH, 1);
            mv.visitTypeInsn(ANEWARRAY, arrayDim); // Test ANEWARRAY bytecode with various dimensions
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    public static FileOutputStream writeClassFileFromByteArray(byte[] classFileByteArray) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(classCName + ".class"));
        fos.write(classFileByteArray);
        fos.close();
        return fos;
    }

    private static String genArrayDim(int testDim) {
        StringBuilder array_Dimension = new StringBuilder();
        for (int i = 0; i < testDim; i++)
        {
            array_Dimension.append("[");
        }
        return array_Dimension.append("Ljava/lang/Object;").toString();
    }
}
