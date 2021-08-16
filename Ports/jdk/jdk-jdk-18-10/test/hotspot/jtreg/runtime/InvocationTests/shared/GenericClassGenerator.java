/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package shared;

import jdk.internal.org.objectweb.asm.ClassWriter;
import static jdk.internal.org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static jdk.internal.org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static shared.AccessCheck.*;

public class GenericClassGenerator<T extends GenericClassGenerator> {
    private static final String targetMethodName = Utils.TARGET_METHOD_NAME;

    private int flags = 0;
    private ClassWriter writer;
    private String fullClassName = null;
    private String parentClassName = null;

    /*******************************************************************/
    public GenericClassGenerator(String fullClassName) {
        this(fullClassName, "java/lang/Object");
    }

    /*******************************************************************/
    public GenericClassGenerator(String fullClassName, String parentClassName ) {
        this(fullClassName, parentClassName, ACC_PUBLIC);
    }

    /*******************************************************************/
    public GenericClassGenerator(String fullClassName, String parentClassName, int flags) {
        this(fullClassName, parentClassName, flags, new String[0]);
    }

    /*******************************************************************/
    public GenericClassGenerator(String fullClassName, String parentClassName, int flags, String[] implementedInterfaces) {
        writer = new ClassWriter(COMPUTE_FRAMES | COMPUTE_MAXS);

        this.fullClassName = fullClassName;
        this.flags = flags;

        // Construct simple class
        if (parentClassName != null) {
            this.parentClassName = getInternalName(parentClassName);
        } else {
            this.parentClassName = "java/lang/Object";
        }

        String parent = this.parentClassName;
        String name = getInternalName(fullClassName);

        if (Utils.isACC_SUPER) {
            flags = flags | ACC_SUPER;
        }

        writer.visit(Utils.version, flags, name, null, parent, implementedInterfaces);

        // Add constructor
        if ( !isInterface(flags) ) {
            MethodVisitor m =
                    writer.visitMethod(
                            ACC_PUBLIC
                            , "<init>"
                            , "()V"
                            , null
                            , null
                    );

            m.visitCode();
            m.visitVarInsn(ALOAD, 0);
            m.visitMethodInsn(
                      INVOKESPECIAL
                    , getInternalName(parent)
                    , "<init>"
                    , "()V"
            );
            m.visitInsn(RETURN);
            m.visitEnd();
            m.visitMaxs(0,0);
        }
    }

    /*******************************************************************/
    protected static String getInternalName(String fullClassName) {
        return fullClassName.replaceAll("\\.", "/");
    }

    /*******************************************************************/
    public T addTargetConstructor(AccessType access) {
        // AccessType.UNDEF means that the target method isn't defined, so do nothing
        if (access == AccessType.UNDEF || isInterface(flags) ) {
            return (T)this;
        }

        // Add target constructor
        int methodAccessType = access.value();

        MethodVisitor m =
                writer.visitMethod(
                        methodAccessType
                        , "<init>"
                        , "(I)V"
                        , null
                        , null
                );

        // Add a call to parent constructor
        m.visitCode();
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(
                  INVOKESPECIAL
                , getInternalName(parentClassName)
                , "<init>"
                , "()V"
        );

        // Add result reporting
        String shortName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        m.visitLdcInsn(shortName+".<init>");
        m.visitFieldInsn(
                  PUTSTATIC
                , "Result"
                , "value"
                , "Ljava/lang/String;"
        );

        m.visitInsn(RETURN);
        m.visitEnd();
        m.visitMaxs(0,0);

        return (T)this;

    }

    /*******************************************************************/
    public T addTargetMethod(AccessType access) {
        return addTargetMethod(access, 0);
    }

    /*******************************************************************/
    public T addTargetMethod(AccessType access, int additionalFlags) {
        // AccessType.UNDEF means that the target method isn't defined, so do nothing
        if (access == AccessType.UNDEF) {
            return (T)this;
        }

        // Add target method
        int methodAccessType = access.value();
        if ( isInterface(flags) || isAbstract(flags) ) {
            methodAccessType |= ACC_ABSTRACT;
        }

        // Skip method declaration for abstract private case, which doesn't pass
        // classfile verification stage
        if ( isPrivate(methodAccessType) && isAbstract(methodAccessType) ) {
            return (T)this;
        }

        MethodVisitor m =
                writer.visitMethod(
                        methodAccessType | additionalFlags
                        , targetMethodName
                        , "()Ljava/lang/String;"
                        , null
                        , null
                );

        // Don't generate body if the method is abstract
        if ( (methodAccessType & ACC_ABSTRACT) == 0 ) {
            String shortName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);

            // Simply returns info about itself
            m.visitCode();
            m.visitLdcInsn(shortName+"."+targetMethodName);
            m.visitInsn(ARETURN);
            m.visitEnd();
            m.visitMaxs(0,0);
        }

        return (T)this;
    }

    /*******************************************************************/
    public T addField(int access, String name, String type) {
        writer.visitField(
                access
                , name
                , getInternalName(type)
                , null
                , null
        )
                .visitEnd();

        return (T)this;
    }

    /*******************************************************************/
    // Add target method call site into current class
    public T addCaller(String targetClass, int callType) {
        MethodVisitor m = writer.visitMethod(
                ACC_PUBLIC | ACC_STATIC
                , "call"
                , String.format( "(L%s;)Ljava/lang/String;" , getInternalName(targetClass))
                , null
                , null
        );

        m.visitCode();
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(
                  callType
                , getInternalName(targetClass)
                , targetMethodName
                , "()Ljava/lang/String;"
        );
        m.visitInsn(ARETURN);
        m.visitEnd();
        m.visitMaxs(0,0);

        return (T)this;
    }

    /*******************************************************************/
    public byte[] getClassFile() {
        writer.visitEnd();
        return writer.toByteArray();
    }

    /*******************************************************************/
    public String getFullClassName() {
        return fullClassName;
    }
}
