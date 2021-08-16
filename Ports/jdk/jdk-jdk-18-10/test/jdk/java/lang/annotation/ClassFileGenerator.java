/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Create class file using ASM, slightly modified the ASMifier output
 */



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import jdk.internal.org.objectweb.asm.*;


public class ClassFileGenerator {

    public static void main(String... args) throws Exception {
        classFileWriter("AnnotationWithVoidReturn.class", AnnotationWithVoidReturnDump.dump());
        classFileWriter("AnnotationWithParameter.class", AnnotationWithParameterDump.dump());
        classFileWriter("AnnotationWithExtraInterface.class", AnnotationWithExtraInterfaceDump.dump());
        classFileWriter("AnnotationWithException.class", AnnotationWithExceptionDump.dump());
        classFileWriter("AnnotationWithHashCode.class", AnnotationWithHashCodeDump.dump());
        classFileWriter("AnnotationWithDefaultMember.class", AnnotationWithDefaultMemberDump.dump());
        classFileWriter("AnnotationWithoutAnnotationAccessModifier.class",
                        AnnotationWithoutAnnotationAccessModifierDump.dump());
        classFileWriter("HolderX.class", HolderXDump.dump());
    }

    private static void classFileWriter(String name, byte[] contents) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(System.getProperty("test.classes"),
                name))) {
            fos.write(contents);
        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac:

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithVoidReturn {
        void m() default 1;
    }

    */

    private static class AnnotationWithVoidReturnDump implements Opcodes {
        public static byte[] dump() throws Exception {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            AnnotationVisitor av0;

            cw.visit(52, ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
                    "AnnotationWithVoidReturn", null,
                    "java/lang/Object", new String[]{"java/lang/annotation/Annotation"});

            {
                av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
                av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;",
                        "RUNTIME");
                av0.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m", "()V", null, null);
                mv.visitEnd();
            }
            {
                av0 = mv.visitAnnotationDefault();
                av0.visit(null, new Integer(1));
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();

        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac:

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithParameter {
        int m(int x) default -1;
    }

    */

    private static class AnnotationWithParameterDump implements Opcodes {
        public static byte[] dump() throws Exception {

            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            AnnotationVisitor av0;

            cw.visit(52, ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
                    "AnnotationWithParameter", null,
                    "java/lang/Object", new String[]{"java/lang/annotation/Annotation"});

            {
                av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
                av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;",
                        "RUNTIME");
                av0.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT,
                        "badValue",
                        "(I)I", // Bad method with a parameter
                        null, null);
                mv.visitEnd();
            }
            {
                av0 = mv.visitAnnotationDefault();
                av0.visit(null, new Integer(-1));
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac:

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithExtraInterface extends java.io.Serializable {
        int m() default 1;
    }

    */

    private static class AnnotationWithExtraInterfaceDump implements Opcodes {
        public static byte[] dump() throws Exception {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            AnnotationVisitor av0;

            cw.visit(52, ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
                    "AnnotationWithExtraInterface", null,
                    "java/lang/Object", new String[]{"java/lang/annotation/Annotation",
                                                     "java/io/Serializable"});

            {
                av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
                av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;",
                        "RUNTIME");
                av0.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m", "()I", null, null);
                mv.visitEnd();
            }
            {
                av0 = mv.visitAnnotationDefault();
                av0.visit(null, new Integer(1));
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac:

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithException {
        int m() throws Exception default 1;
    }

    */

    private static class AnnotationWithExceptionDump implements Opcodes {
        public static byte[] dump() throws Exception {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            AnnotationVisitor av0;

            cw.visit(52, ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
                    "AnnotationWithException", null,
                    "java/lang/Object", new String[]{"java/lang/annotation/Annotation"});

            {
                av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
                av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;",
                        "RUNTIME");
                av0.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m", "()I", null,
                                    new String[] {"java/lang/Exception"});
                mv.visitEnd();
            }
            {
                av0 = mv.visitAnnotationDefault();
                av0.visit(null, new Integer(1));
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac:

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithHashCode {
        int hashCode() default 1;
    }

    */

    private static class AnnotationWithHashCodeDump implements Opcodes {
        public static byte[] dump() throws Exception {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            AnnotationVisitor av0;

            cw.visit(52, ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
                    "AnnotationWithHashCode", null,
                    "java/lang/Object", new String[]{"java/lang/annotation/Annotation"});

            {
                av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
                av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;",
                        "RUNTIME");
                av0.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "hashCode", "()I", null, null);
                mv.visitEnd();
            }
            {
                av0 = mv.visitAnnotationDefault();
                av0.visit(null, new Integer(1));
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac:

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithDefaultMember {
        int m() default 1;
        default int d() default 2 { return 2; }
    }

    */

    private static class AnnotationWithDefaultMemberDump implements Opcodes {
        public static byte[] dump() throws Exception {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv, dv;
            AnnotationVisitor av0;

            cw.visit(52, ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
                    "AnnotationWithDefaultMember", null,
                    "java/lang/Object", new String[]{"java/lang/annotation/Annotation"});

            {
                av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
                av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;",
                        "RUNTIME");
                av0.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m", "()I", null, null);
                mv.visitEnd();
            }
            {
                av0 = mv.visitAnnotationDefault();
                av0.visit(null, new Integer(1));
                av0.visitEnd();
            }
            {
                dv = cw.visitMethod(ACC_PUBLIC, "d", "()I", null, null);
                dv.visitMaxs(1, 1);
                dv.visitCode();
                dv.visitInsn(Opcodes.ICONST_2);
                dv.visitInsn(Opcodes.IRETURN);
                dv.visitEnd();
            }
            {
                av0 = dv.visitAnnotationDefault();
                av0.visit(null, new Integer(2));
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac:

    @Retention(RetentionPolicy.RUNTIME)
    public interface AnnotationWithoutAnnotationAccessModifier extends java.lang.annotation.Annotation {
        int m() default 1;
    }

    */

    private static class AnnotationWithoutAnnotationAccessModifierDump implements Opcodes {
        public static byte[] dump() throws Exception {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            AnnotationVisitor av0;

            cw.visit(52, ACC_PUBLIC + /* ACC_ANNOTATION +*/ ACC_ABSTRACT + ACC_INTERFACE,
                    "AnnotationWithoutAnnotationAccessModifier", null,
                    "java/lang/Object", new String[]{"java/lang/annotation/Annotation"});

            {
                av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
                av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;",
                        "RUNTIME");
                av0.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m", "()I", null, null);
                mv.visitEnd();
            }
            {
                av0 = mv.visitAnnotationDefault();
                av0.visit(null, new Integer(1));
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }

    /* Following code creates equivalent classfile, which is not allowed by javac
       since AnnotationWithoutAnnotationAccessModifier is not marked with ACC_ANNOTATION:

    @GoodAnnotation
    @AnnotationWithoutAnnotationAccessModifier
    public interface HolderX {
    }

    */

    private static class HolderXDump implements Opcodes {
        public static byte[] dump() throws Exception {
            ClassWriter cw = new ClassWriter(0);

            cw.visit(52, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                    "HolderX", null,
                    "java/lang/Object", new String[0]);

            {
                AnnotationVisitor av0;
                av0 = cw.visitAnnotation("LGoodAnnotation;", true);
                av0.visitEnd();
                av0 = cw.visitAnnotation("LAnnotationWithoutAnnotationAccessModifier;", true);
                av0.visitEnd();
            }
            cw.visitEnd();

            return cw.toByteArray();
        }
    }
}
