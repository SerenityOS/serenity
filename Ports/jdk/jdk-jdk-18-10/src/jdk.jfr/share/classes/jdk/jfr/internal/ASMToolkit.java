/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.Method;
import jdk.internal.org.objectweb.asm.util.TraceClassVisitor;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.internal.EventInstrumentation.FieldInfo;

final class ASMToolkit {
    private static Type TYPE_STRING = Type.getType(String.class);
    private static Type Type_THREAD = Type.getType(Thread.class);
    private static Type TYPE_CLASS = Type.getType(Class.class);

    public static void invokeSpecial(MethodVisitor methodVisitor, String className, Method m) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, className, m.getName(), m.getDescriptor(), false);
    }

    public static void invokeStatic(MethodVisitor methodVisitor, String className, Method m) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, className, m.getName(), m.getDescriptor(), false);
    }

    public static void invokeVirtual(MethodVisitor methodVisitor, String className, Method m) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, m.getName(), m.getDescriptor(), false);
    }


    public static Type toType(ValueDescriptor v) {
        String typeName = v.getTypeName();

        switch (typeName) {
        case "byte":
            return Type.BYTE_TYPE;
        case "short":
            return Type.SHORT_TYPE;
        case "int":
            return Type.INT_TYPE;
        case "long":
            return Type.LONG_TYPE;
        case "double":
            return Type.DOUBLE_TYPE;
        case "float":
            return Type.FLOAT_TYPE;
        case "char":
            return Type.CHAR_TYPE;
        case "boolean":
            return Type.BOOLEAN_TYPE;
        case "java.lang.String":
            return TYPE_STRING;
        case "java.lang.Thread":
            return Type_THREAD;
        case "java.lang.Class":
            return TYPE_CLASS;
        }
        // Add support for SettingControl?
       throw new Error("Not a valid type " + v.getTypeName());
    }

    /**
     * Converts "int" into "I" and "java.lang.String" into "Ljava/lang/String;"
     *
     * @param typeName
     *            type
     *
     * @return descriptor
     */
    public static String getDescriptor(String typeName) {
        if ("int".equals(typeName)) {
            return "I";
        }
        if ("long".equals(typeName)) {
            return "J";
        }
        if ("boolean".equals(typeName)) {
            return "Z";
        }
        if ("float".equals(typeName)) {
            return "F";
        }
        if ("double".equals(typeName)) {
            return "D";
        }
        if ("short".equals(typeName)) {
            return "S";
        }
        if ("char".equals(typeName)) {
            return "C";
        }
        if ("byte".equals(typeName)) {
            return "B";
        }
        String internal = getInternalName(typeName);
        return Type.getObjectType(internal).getDescriptor();
    }

    /**
     * Converts java.lang.String into java/lang/String
     *
     * @param className
     *
     * @return internal name
     */
    public static String getInternalName(String className) {
        return className.replace(".", "/");
    }

    public static Method makeWriteMethod(List<FieldInfo> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (FieldInfo v : fields) {
            if (!v.fieldName.equals(EventInstrumentation.FIELD_EVENT_THREAD) && !v.fieldName.equals(EventInstrumentation.FIELD_STACK_TRACE)) {
                sb.append(v.fieldDescriptor);
            }
        }
        sb.append(")V");
        return new Method("write", sb.toString());
    }

    public static void logASM(String className, byte[] bytes) {
        Logger.log(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.INFO, "Generated bytecode for class " + className);
        if (Logger.shouldLog(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.TRACE)) {
            ClassReader cr = new ClassReader(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter w = new PrintWriter(baos);
            w.println("Bytecode:");
            cr.accept(new TraceClassVisitor(w), 0);
            Logger.log(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.TRACE, baos.toString());
        };
    }

}
