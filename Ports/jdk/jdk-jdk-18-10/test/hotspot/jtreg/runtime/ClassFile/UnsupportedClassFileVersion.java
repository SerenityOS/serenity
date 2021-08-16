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

/*
 * @test
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.management
 * @run driver UnsupportedClassFileVersion
 */

import java.io.File;
import java.io.FileOutputStream;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class UnsupportedClassFileVersion implements Opcodes {
    public static void main(String... args) throws Exception {
        writeClassFile();
        ProcessBuilder pb = ProcessTools.createTestJvm("-cp", ".",  "ClassFile");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("ClassFile has been compiled by a more recent version of the " +
                            "Java Runtime (class file version 99.0), this version of " +
                            "the Java Runtime only recognizes class file versions up to " +
                            System.getProperty("java.class.version"));

        output.shouldHaveExitValue(1);
    }

    public static void writeClassFile() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(99, ACC_PUBLIC + ACC_SUPER, "ClassFile", null, "java/lang/Object", null);
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();

        try (FileOutputStream fos = new FileOutputStream(new File("ClassFile.class"))) {
             fos.write(cw.toByteArray());
        }
    }
}
