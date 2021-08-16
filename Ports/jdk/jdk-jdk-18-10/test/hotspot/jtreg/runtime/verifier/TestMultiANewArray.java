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
 * @test TestMultiANewArray
 * @bug 8038076
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.management
 * @compile -XDignore.symbol.file TestMultiANewArray.java
 * @run driver TestMultiANewArray 49
 * @run driver TestMultiANewArray 50
 * @run driver TestMultiANewArray 51
 * @run driver TestMultiANewArray 52
 */

public class TestMultiANewArray {
    public static void main(String... args) throws Exception {
        int cfv = Integer.parseInt(args[0]);
        writeClassFile(cfv);
        System.err.println("Running with cfv: " + cfv);
        ProcessBuilder pb = ProcessTools.createTestJvm("-cp", ".",  "ClassFile");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("VerifyError");
        output.shouldHaveExitValue(1);
    }

    public static void writeClassFile(int cfv) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(cfv, ACC_PUBLIC + ACC_SUPER, "ClassFile", null, "java/lang/Object", null);
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ICONST_2);
        mv.visitMultiANewArrayInsn("[I", 2);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        cw.visitEnd();

        try (FileOutputStream fos = new FileOutputStream(new File("ClassFile.class"))) {
             fos.write(cw.toByteArray());
        }
    }
}
