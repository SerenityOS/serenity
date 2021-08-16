/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.Method;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.SettingControl;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.internal.EventInstrumentation.FieldInfo;
import jdk.jfr.internal.EventInstrumentation.SettingInfo;
import jdk.jfr.internal.handlers.EventHandler;

final class EventHandlerCreator {
    // TODO:
    // How can we find out class version without loading a
    // class as resource in a privileged block and use ASM to inspect
    // the contents. Using '52' even though we know later versions
    // are available. The reason for this is compatibility aspects
    // with for example WLS.
    private static final int CLASS_VERSION = 52;

    // This is needed so a new EventHandler is automatically generated in MetadataRepository
    // if a user Event class is loaded using APPCDS/CDS.
    private static final String SUFFIX  = "_" + System.currentTimeMillis() + "-" + JVM.getJVM().getPid();

    private static final String FIELD_EVENT_TYPE = "platformEventType";
    private static final String FIELD_PREFIX_STRING_POOL = "stringPool";

    private static final Type TYPE_STRING_POOL = Type.getType(StringPool.class);
    private static final Type TYPE_EVENT_WRITER = Type.getType(EventWriter.class);
    private static final Type TYPE_PLATFORM_EVENT_TYPE = Type.getType(PlatformEventType.class);
    private static final Type TYPE_EVENT_HANDLER = Type.getType(EventHandler.class);
    private static final Type TYPE_SETTING_CONTROL = Type.getType(SettingControl.class);
    private static final Type TYPE_EVENT_TYPE = Type.getType(EventType.class);
    private static final Type TYPE_EVENT_CONTROL = Type.getType(EventControl.class);
    private static final String DESCRIPTOR_EVENT_HANDLER = "(" + Type.BOOLEAN_TYPE.getDescriptor() + TYPE_EVENT_TYPE.getDescriptor() + TYPE_EVENT_CONTROL.getDescriptor() + ")V";
    private static final Method METHOD_GET_EVENT_WRITER = new Method("getEventWriter", "()" + TYPE_EVENT_WRITER.getDescriptor());
    private static final Method METHOD_EVENT_HANDLER_CONSTRUCTOR = new Method("<init>", DESCRIPTOR_EVENT_HANDLER);
    private static final Method METHOD_RESET = new Method("reset", "()V");

    private final ClassWriter classWriter;
    private final String className;
    private final String internalClassName;
    private final List<SettingInfo> settingInfos;
    private final List<FieldInfo> fields;

    public EventHandlerCreator(long id, List<SettingInfo> settingInfos, List<FieldInfo> fields) {
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.className = makeEventHandlerName(id);
        this.internalClassName = ASMToolkit.getInternalName(className);
        this.settingInfos = settingInfos;
        this.fields = fields;
    }

    public static String makeEventHandlerName(long id) {
        return EventHandler.class.getName() + id + SUFFIX;
    }

    public EventHandlerCreator(long id, List<SettingInfo> settingInfos, EventType type, Class<? extends jdk.internal.event.Event> eventClass) {
        this(id, settingInfos, createFieldInfos(eventClass, type));
    }

    private static List<FieldInfo> createFieldInfos(Class<? extends jdk.internal.event.Event> eventClass, EventType type) throws Error {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (ValueDescriptor v : type.getFields()) {
            // Only value descriptors that are not fields on the event class.
            if (v != TypeLibrary.STACK_TRACE_FIELD && v != TypeLibrary.THREAD_FIELD) {
                String fieldName = PrivateAccess.getInstance().getFieldName(v);
                String fieldDescriptor = ASMToolkit.getDescriptor(v.getTypeName());
                Class<?> c = eventClass;
                String internalName = null;
                while (c != Event.class) {
                    try {
                        Field field = c.getDeclaredField(fieldName);
                        if (c == eventClass || !Modifier.isPrivate(field.getModifiers())) {
                            internalName = ASMToolkit.getInternalName(c.getName());
                            break;
                        }
                    } catch (NoSuchFieldException | SecurityException e) {
                        // ignore
                    }
                    c = c.getSuperclass();
                }
                if (internalName != null) {
                    fieldInfos.add(new FieldInfo(fieldName, fieldDescriptor, internalName));
                } else {
                    throw new InternalError("Could not locate field " + fieldName + " for event type" + type.getName());
                }
            }
        }
        return fieldInfos;
    }

    public Class<? extends EventHandler> makeEventHandlerClass() {
        buildClassInfo();
        buildConstructor();
        buildWriteMethod();
        byte[] bytes = classWriter.toByteArray();
        ASMToolkit.logASM(className, bytes);
        return SecuritySupport.defineClass(EventHandler.class, bytes).asSubclass(EventHandler.class);
    }

    public static EventHandler instantiateEventHandler(Class<? extends EventHandler> handlerClass, boolean registered, EventType eventType, EventControl eventControl) throws Error {
        final Constructor<?> cc;
        try {
            cc = handlerClass.getDeclaredConstructors()[0];
        } catch (Exception e) {
            throw (Error) new InternalError("Could not get handler constructor for " + eventType.getName()).initCause(e);
        }
        // Users should not be allowed to create instances of the event handler
        // so we need to unlock it here.
        SecuritySupport.setAccessible(cc);
        try {
            List<SettingInfo> settingInfos = eventControl.getSettingInfos();
            Object[] arguments = new Object[3 + settingInfos.size()];
            arguments[0] = registered;
            arguments[1] = eventType;
            arguments[2] = eventControl;
            for (SettingInfo si : settingInfos) {
                arguments[si.index + 3] = si.settingControl;
            }
            return (EventHandler) cc.newInstance(arguments);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw (Error) new InternalError("Could not instantiate event handler for " + eventType.getName() + ". " + e.getMessage()).initCause(e);
        }
    }

    private void buildConstructor() {
        MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PRIVATE, METHOD_EVENT_HANDLER_CONSTRUCTOR.getName(), makeConstructorDescriptor(settingInfos), null, null);
        mv.visitVarInsn(Opcodes.ALOAD, 0); // this
        mv.visitVarInsn(Opcodes.ILOAD, 1); // registered
        mv.visitVarInsn(Opcodes.ALOAD, 2); // event type
        mv.visitVarInsn(Opcodes.ALOAD, 3); // event control
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(EventHandler.class), METHOD_EVENT_HANDLER_CONSTRUCTOR.getName(), METHOD_EVENT_HANDLER_CONSTRUCTOR.getDescriptor(), false);
        for (SettingInfo si : settingInfos) {
            mv.visitVarInsn(Opcodes.ALOAD, 0); // this
            mv.visitVarInsn(Opcodes.ALOAD, si.index + 4); // Setting Control
            mv.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, si.fieldName, TYPE_SETTING_CONTROL.getDescriptor());
        }
        // initialized string field writers
        int fieldIndex = 0;
        for (FieldInfo field : fields) {
            if (field.isString()) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(EventHandler.class), "createStringFieldWriter", "()" + TYPE_STRING_POOL.getDescriptor(), false);
                mv.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, FIELD_PREFIX_STRING_POOL + fieldIndex, TYPE_STRING_POOL.getDescriptor());
            }
            fieldIndex++;
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void buildClassInfo() {
        String internalSuperName = ASMToolkit.getInternalName(EventHandler.class.getName());
        classWriter.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER, internalClassName, null, internalSuperName, null);
        for (SettingInfo si : settingInfos) {
            classWriter.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, si.fieldName, TYPE_SETTING_CONTROL.getDescriptor(), null, null);
        }
        int fieldIndex = 0;
        for (FieldInfo field : fields) {
            if (field.isString()) {
                classWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, FIELD_PREFIX_STRING_POOL+ fieldIndex, TYPE_STRING_POOL.getDescriptor(), null, null);
            }
            fieldIndex++;
        }
    }

    private void visitMethod(final MethodVisitor mv, final int opcode, final Type type, final Method method) {
        mv.visitMethodInsn(opcode, type.getInternalName(), method.getName(), method.getDescriptor(), false);
    }

    private void buildWriteMethod() {
        int argIndex = 0; // // indexes the argument type array, the argument type array does not include 'this'
        int slotIndex = 1; // indexes the proper slot in the local variable table, takes type size into account, therefore sometimes argIndex != slotIndex
        int fieldIndex = 0;
        Method desc = ASMToolkit.makeWriteMethod(fields);
        Type[] argumentTypes = Type.getArgumentTypes(desc.getDescriptor());
        MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, desc.getName(), desc.getDescriptor(), null, null);
        mv.visitCode();
        Label start = new Label();
        Label endTryBlock = new Label();
        Label exceptionHandler = new Label();
        mv.visitTryCatchBlock(start, endTryBlock, exceptionHandler, "java/lang/Throwable");
        mv.visitLabel(start);
        visitMethod(mv, Opcodes.INVOKESTATIC, TYPE_EVENT_WRITER, METHOD_GET_EVENT_WRITER);
        // stack: [BW]
        mv.visitInsn(Opcodes.DUP);
        // stack: [BW], [BW]
        // write begin event
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        // stack: [BW], [BW], [this]
        mv.visitFieldInsn(Opcodes.GETFIELD, TYPE_EVENT_HANDLER.getInternalName(), FIELD_EVENT_TYPE, TYPE_PLATFORM_EVENT_TYPE.getDescriptor());
        // stack: [BW], [BW], [BS]
        visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, EventWriterMethod.BEGIN_EVENT.asASM());
        // stack: [BW], [integer]
        Label recursive = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, recursive);
        // stack: [BW]
        // write startTime
        mv.visitInsn(Opcodes.DUP);
        // stack: [BW], [BW]
        mv.visitVarInsn(argumentTypes[argIndex].getOpcode(Opcodes.ILOAD), slotIndex);
        // stack: [BW], [BW], [long]
        slotIndex += argumentTypes[argIndex++].getSize();
        visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, EventWriterMethod.PUT_LONG.asASM());
        // stack: [BW]
        fieldIndex++;
        // write duration
        mv.visitInsn(Opcodes.DUP);
        // stack: [BW], [BW]
        mv.visitVarInsn(argumentTypes[argIndex].getOpcode(Opcodes.ILOAD), slotIndex);
        // stack: [BW], [BW], [long]
        slotIndex += argumentTypes[argIndex++].getSize();
        visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, EventWriterMethod.PUT_LONG.asASM());
        // stack: [BW]
        fieldIndex++;
        // write eventThread
        mv.visitInsn(Opcodes.DUP);
        // stack: [BW], [BW]
        visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, EventWriterMethod.PUT_EVENT_THREAD.asASM());
        // stack: [BW]
        // write stackTrace
        mv.visitInsn(Opcodes.DUP);
        // stack: [BW], [BW]
        visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, EventWriterMethod.PUT_STACK_TRACE.asASM());
        // stack: [BW]
        // write custom fields
        while (fieldIndex < fields.size()) {
            mv.visitInsn(Opcodes.DUP);
            // stack: [BW], [BW]
            mv.visitVarInsn(argumentTypes[argIndex].getOpcode(Opcodes.ILOAD), slotIndex);
            // stack:[BW], [BW], [field]
            slotIndex += argumentTypes[argIndex++].getSize();
            FieldInfo field = fields.get(fieldIndex);
            if (field.isString()) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                // stack:[BW], [BW], [field], [this]
                mv.visitFieldInsn(Opcodes.GETFIELD, this.internalClassName, FIELD_PREFIX_STRING_POOL + fieldIndex, TYPE_STRING_POOL.getDescriptor());
                // stack:[BW], [BW], [field], [string]
            }
            EventWriterMethod eventMethod = EventWriterMethod.lookupMethod(field);
            visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, eventMethod.asASM());
            // stack: [BW]
            fieldIndex++;
        }
        // stack: [BW]
        // write end event (writer already on stack)
        visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, EventWriterMethod.END_EVENT.asASM());
        // stack [integer]
        // notified -> restart event write attempt
        mv.visitJumpInsn(Opcodes.IFEQ, start);
        // stack:
        mv.visitLabel(endTryBlock);
        Label end = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, end);
        mv.visitLabel(exceptionHandler);
        // stack: [ex]
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
        visitMethod(mv, Opcodes.INVOKESTATIC, TYPE_EVENT_WRITER, METHOD_GET_EVENT_WRITER);
        // stack: [ex] [BW]
        mv.visitInsn(Opcodes.DUP);
        // stack: [ex] [BW] [BW]
        Label rethrow = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, rethrow);
        // stack: [ex] [BW]
        mv.visitInsn(Opcodes.DUP);
        // stack: [ex] [BW] [BW]
        visitMethod(mv, Opcodes.INVOKEVIRTUAL, TYPE_EVENT_WRITER, METHOD_RESET);
        mv.visitLabel(rethrow);
        // stack:[ex] [BW]
        mv.visitFrame(Opcodes.F_SAME, 0, null, 2, new Object[] {"java/lang/Throwable", TYPE_EVENT_WRITER.getInternalName()});
        mv.visitInsn(Opcodes.POP);
        // stack:[ex]
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(recursive);
        // stack: [BW]
        mv.visitFrame(Opcodes.F_SAME, 0, null, 1, new Object[] { TYPE_EVENT_WRITER.getInternalName()} );
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(end);
        // stack:
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String makeConstructorDescriptor(List<SettingInfo> settingsInfos) {
        StringJoiner constructordescriptor = new StringJoiner("", "(", ")V");
        constructordescriptor.add(Type.BOOLEAN_TYPE.getDescriptor());
        constructordescriptor.add(Type.getType(EventType.class).getDescriptor());
        constructordescriptor.add(Type.getType(EventControl.class).getDescriptor());
        for (int i = 0; i < settingsInfos.size(); i++) {
            constructordescriptor.add(TYPE_SETTING_CONTROL.getDescriptor());
        }
        return constructordescriptor.toString();
    }
}
