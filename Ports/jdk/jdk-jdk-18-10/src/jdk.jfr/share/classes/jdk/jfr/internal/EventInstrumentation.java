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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.Method;
import jdk.internal.org.objectweb.asm.tree.AnnotationNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Registered;
import jdk.jfr.SettingControl;
import jdk.jfr.SettingDefinition;
import jdk.jfr.internal.handlers.EventHandler;

/**
 * Class responsible for adding instrumentation to a subclass of {@link Event}.
 *
 */
public final class EventInstrumentation {
    static final class SettingInfo {
        private String methodName;
        private String internalSettingName;
        private String settingDescriptor;
        final String fieldName;
        final int index;

        // The settingControl is passed to EventHandler where it is
        // used to check enablement before calling commit
        // Methods on settingControl must never be invoked
        // directly by JFR, instead use jdk.jfr.internal.Control
        SettingControl settingControl;

        public SettingInfo(String fieldName, int index) {
            this.fieldName = fieldName;
            this.index = index;
        }
    }

    static final class FieldInfo {
        private static final Type STRING = Type.getType(String.class);
        final String fieldName;
        final String fieldDescriptor;
        final String internalClassName;

        public FieldInfo(String fieldName, String fieldDescriptor, String internalClassName) {
            this.fieldName = fieldName;
            this.fieldDescriptor = fieldDescriptor;
            this.internalClassName = internalClassName;
        }

        public boolean isString() {
            return STRING.getDescriptor().equals(fieldDescriptor);
        }
    }

    public static final String FIELD_EVENT_THREAD = "eventThread";
    public static final String FIELD_STACK_TRACE = "stackTrace";
    public static final String FIELD_DURATION = "duration";

    static final String FIELD_EVENT_HANDLER = "eventHandler";
    static final String FIELD_START_TIME = "startTime";

    private static final Type ANNOTATION_TYPE_NAME = Type.getType(Name.class);
    private static final Type ANNOTATION_TYPE_REGISTERED = Type.getType(Registered.class);
    private static final Type ANNOTATION_TYPE_ENABLED = Type.getType(Enabled.class);
    private static final Type TYPE_EVENT_HANDLER = Type.getType(EventHandler.class);
    private static final Type TYPE_SETTING_CONTROL = Type.getType(SettingControl.class);
    private static final Type TYPE_OBJECT  = Type.getType(Object.class);
    private static final Method METHOD_COMMIT = new Method("commit", Type.VOID_TYPE, new Type[0]);
    private static final Method METHOD_BEGIN = new Method("begin", Type.VOID_TYPE, new Type[0]);
    private static final Method METHOD_END = new Method("end", Type.VOID_TYPE, new Type[0]);
    private static final Method METHOD_IS_ENABLED = new Method("isEnabled", Type.BOOLEAN_TYPE, new Type[0]);
    private static final Method METHOD_TIME_STAMP = new Method("timestamp", Type.LONG_TYPE, new Type[0]);
    private static final Method METHOD_EVENT_SHOULD_COMMIT = new Method("shouldCommit", Type.BOOLEAN_TYPE, new Type[0]);
    private static final Method METHOD_EVENT_HANDLER_SHOULD_COMMIT = new Method("shouldCommit", Type.BOOLEAN_TYPE, new Type[] { Type.LONG_TYPE });
    private static final Method METHOD_DURATION = new Method("duration", Type.LONG_TYPE, new Type[] { Type.LONG_TYPE });

    private final ClassNode classNode;
    private final List<SettingInfo> settingInfos;
    private final List<FieldInfo> fieldInfos;;
    private final Method writeMethod;
    private final String eventHandlerXInternalName;
    private final String eventName;
    private final boolean untypedEventHandler;
    private boolean guardHandlerReference;
    private Class<?> superClass;

    EventInstrumentation(Class<?> superClass, byte[] bytes, long id) {
        this.superClass = superClass;
        this.classNode = createClassNode(bytes);
        this.settingInfos = buildSettingInfos(superClass, classNode);
        this.fieldInfos = buildFieldInfos(superClass, classNode);
        this.untypedEventHandler = hasUntypedHandler();
        this.writeMethod = makeWriteMethod(fieldInfos);
        this.eventHandlerXInternalName = ASMToolkit.getInternalName(EventHandlerCreator.makeEventHandlerName(id));
        String n =  annotationValue(classNode, ANNOTATION_TYPE_NAME.getDescriptor(), String.class);
        this.eventName = n == null ? classNode.name.replace("/", ".") : n;
    }

    private boolean hasUntypedHandler() {
        for (FieldNode field : classNode.fields) {
            if (FIELD_EVENT_HANDLER.equals(field.name)) {
                return field.desc.equals(TYPE_OBJECT.getDescriptor());
            }
        }
        throw new InternalError("Class missing handler field");
    }

    public String getClassName() {
      return classNode.name.replace("/",".");
    }

    private ClassNode createClassNode(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        return classNode;
    }

    boolean isRegistered() {
        Boolean result = annotationValue(classNode, ANNOTATION_TYPE_REGISTERED.getDescriptor(), Boolean.class);
        if (result != null) {
            return result.booleanValue();
        }
        if (superClass != null) {
            Registered r = superClass.getAnnotation(Registered.class);
            if (r != null) {
                return r.value();
            }
        }
        return true;
    }

    boolean isEnabled() {
        Boolean result = annotationValue(classNode, ANNOTATION_TYPE_ENABLED.getDescriptor(), Boolean.class);
        if (result != null) {
            return result.booleanValue();
        }
        if (superClass != null) {
            Enabled e = superClass.getAnnotation(Enabled.class);
            if (e != null) {
                return e.value();
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> T annotationValue(ClassNode classNode, String typeDescriptor, Class<?> type) {
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode a : classNode.visibleAnnotations) {
                if (typeDescriptor.equals(a.desc)) {
                    List<Object> values = a.values;
                    if (values != null && values.size() == 2) {
                        Object key = values.get(0);
                        Object value = values.get(1);
                        if (key instanceof String keyName && value != null) {
                            if (type == value.getClass()) {
                                if ("value".equals(keyName)) {
                                   return (T) value;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<SettingInfo> buildSettingInfos(Class<?> superClass, ClassNode classNode) {
        Set<String> methodSet = new HashSet<>();
        List<SettingInfo> settingInfos = new ArrayList<>();
        String settingDescriptor = Type.getType(SettingDefinition.class).getDescriptor();
        for (MethodNode m : classNode.methods) {
            if (m.visibleAnnotations != null) {
                for (AnnotationNode an : m.visibleAnnotations) {
                    // We can't really validate the method at this
                    // stage. We would need to check that the parameter
                    // is an instance of SettingControl.
                    if (settingDescriptor.equals(an.desc)) {
                        Type returnType = Type.getReturnType(m.desc);
                        if (returnType.equals(Type.getType(Boolean.TYPE))) {
                            Type[] args = Type.getArgumentTypes(m.desc);
                            if (args.length == 1) {
                                Type paramType = args[0];
                                String fieldName = EventControl.FIELD_SETTING_PREFIX + settingInfos.size();
                                int index = settingInfos.size();
                                SettingInfo si = new SettingInfo(fieldName, index);
                                si.methodName = m.name;
                                si.settingDescriptor = paramType.getDescriptor();
                                si.internalSettingName = paramType.getInternalName();
                                methodSet.add(m.name);
                                settingInfos.add(si);
                            }
                        }
                    }
                }
            }
        }
        for (Class<?> c = superClass; c != jdk.internal.event.Event.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Method method : c.getDeclaredMethods()) {
                if (!methodSet.contains(method.getName())) {
                    // skip private method in base classes
                    if (!Modifier.isPrivate(method.getModifiers())) {
                        if (method.getReturnType().equals(Boolean.TYPE)) {
                            if (method.getParameterCount() == 1) {
                                Parameter param = method.getParameters()[0];
                                Type paramType = Type.getType(param.getType());
                                String fieldName = EventControl.FIELD_SETTING_PREFIX + settingInfos.size();
                                int index = settingInfos.size();
                                SettingInfo si = new SettingInfo(fieldName, index);
                                si.methodName = method.getName();
                                si.settingDescriptor = paramType.getDescriptor();
                                si.internalSettingName = paramType.getInternalName();
                                methodSet.add(method.getName());
                                settingInfos.add(si);
                            }
                        }
                    }
                }
            }
        }
        return settingInfos;
    }

    private static List<FieldInfo> buildFieldInfos(Class<?> superClass, ClassNode classNode) {
        Set<String> fieldSet = new HashSet<>();
        List<FieldInfo> fieldInfos = new ArrayList<>(classNode.fields.size());
        // These two field are added by native as transient so they will be
        // ignored by the loop below.
        // The benefit of adding them manually is that we can
        // control in which order they occur and we can add @Name, @Description
        // in Java, instead of in native. It also means code for adding implicit
        // fields for native can be reused by Java.
        fieldInfos.add(new FieldInfo("startTime", Type.LONG_TYPE.getDescriptor(), classNode.name));
        fieldInfos.add(new FieldInfo("duration", Type.LONG_TYPE.getDescriptor(), classNode.name));
        for (FieldNode field : classNode.fields) {
            if (!fieldSet.contains(field.name) && isValidField(field.access, Type.getType(field.desc).getClassName())) {
                FieldInfo fi = new FieldInfo(field.name, field.desc, classNode.name);
                fieldInfos.add(fi);
                fieldSet.add(field.name);
            }
        }
        for (Class<?> c = superClass; c != jdk.internal.event.Event.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                // skip private field in base classes
                if (!Modifier.isPrivate(field.getModifiers())) {
                    if (isValidField(field.getModifiers(), field.getType().getName())) {
                        String fieldName = field.getName();
                        if (!fieldSet.contains(fieldName)) {
                            Type fieldType = Type.getType(field.getType());
                            String internalClassName = ASMToolkit.getInternalName(c.getName());
                            fieldInfos.add(new FieldInfo(fieldName, fieldType.getDescriptor(), internalClassName));
                            fieldSet.add(fieldName);
                        }
                    }
                }
            }
        }
        return fieldInfos;
    }

    public static boolean isValidField(int access, String className) {
        if (Modifier.isTransient(access) || Modifier.isStatic(access)) {
            return false;
        }
        return jdk.jfr.internal.Type.isValidJavaFieldType(className);
    }

    public byte[] buildInstrumented() {
        makeInstrumented();
        return toByteArray();
    }

    private byte[] toByteArray() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        cw.visitEnd();
        byte[] result = cw.toByteArray();
        Utils.writeGeneratedASM(classNode.name, result);
        return result;
    }

    public byte[] buildUninstrumented() {
        makeUninstrumented();
        return toByteArray();
    }

    private void makeInstrumented() {
        // MyEvent#isEnabled()
        updateMethod(METHOD_IS_ENABLED, methodVisitor -> {
            Label nullLabel = new Label();
            if (guardHandlerReference) {
                getEventHandler(methodVisitor);
                methodVisitor.visitJumpInsn(Opcodes.IFNULL, nullLabel);
            }
            getEventHandler(methodVisitor);
            ASMToolkit.invokeVirtual(methodVisitor, TYPE_EVENT_HANDLER.getInternalName(), METHOD_IS_ENABLED);
            methodVisitor.visitInsn(Opcodes.IRETURN);
            if (guardHandlerReference) {
                methodVisitor.visitLabel(nullLabel);
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitInsn(Opcodes.IRETURN);
            }
        });

        // MyEvent#begin()
        updateMethod(METHOD_BEGIN, methodVisitor -> {
            methodVisitor.visitIntInsn(Opcodes.ALOAD, 0);
            ASMToolkit.invokeStatic(methodVisitor, TYPE_EVENT_HANDLER.getInternalName(), METHOD_TIME_STAMP);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getInternalClassName(), FIELD_START_TIME, "J");
            methodVisitor.visitInsn(Opcodes.RETURN);
        });

        // MyEvent#end()
        updateMethod(METHOD_END, methodVisitor -> {
            methodVisitor.visitIntInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitIntInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getInternalClassName(), FIELD_START_TIME, "J");
            ASMToolkit.invokeStatic(methodVisitor, TYPE_EVENT_HANDLER.getInternalName(), METHOD_DURATION);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getInternalClassName(), FIELD_DURATION, "J");
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(0, 0);
        });

        updateMethod(METHOD_COMMIT, methodVisitor -> {
            // if (!isEnable()) {
            // return;
            // }
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getInternalClassName(), METHOD_IS_ENABLED.getName(), METHOD_IS_ENABLED.getDescriptor(), false);
            Label l0 = new Label();
            methodVisitor.visitJumpInsn(Opcodes.IFNE, l0);
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitLabel(l0);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            // if (startTime == 0) {
            // startTime = EventWriter.timestamp();
            // } else {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getInternalClassName(), FIELD_START_TIME, "J");
            methodVisitor.visitInsn(Opcodes.LCONST_0);
            methodVisitor.visitInsn(Opcodes.LCMP);
            Label durationalEvent = new Label();
            methodVisitor.visitJumpInsn(Opcodes.IFNE, durationalEvent);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE_EVENT_HANDLER.getInternalName(), METHOD_TIME_STAMP.getName(), METHOD_TIME_STAMP.getDescriptor(), false);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getInternalClassName(), FIELD_START_TIME, "J");
            Label commit = new Label();
            methodVisitor.visitJumpInsn(Opcodes.GOTO, commit);
            // if (duration == 0) {
            // duration = EventWriter.timestamp() - startTime;
            // }
            // }
            methodVisitor.visitLabel(durationalEvent);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getInternalClassName(), FIELD_DURATION, "J");
            methodVisitor.visitInsn(Opcodes.LCONST_0);
            methodVisitor.visitInsn(Opcodes.LCMP);
            methodVisitor.visitJumpInsn(Opcodes.IFNE, commit);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE_EVENT_HANDLER.getInternalName(), METHOD_TIME_STAMP.getName(), METHOD_TIME_STAMP.getDescriptor(), false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getInternalClassName(), FIELD_START_TIME, "J");
            methodVisitor.visitInsn(Opcodes.LSUB);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getInternalClassName(), FIELD_DURATION, "J");
            methodVisitor.visitLabel(commit);
            // if (shouldCommit()) {
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getInternalClassName(), METHOD_EVENT_SHOULD_COMMIT.getName(), METHOD_EVENT_SHOULD_COMMIT.getDescriptor(), false);
            Label end = new Label();
            // eventHandler.write(...);
            // }
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, end);
            getEventHandler(methodVisitor);

            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, eventHandlerXInternalName);
            for (FieldInfo fi : fieldInfos) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, fi.internalClassName, fi.fieldName, fi.fieldDescriptor);
            }

            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, eventHandlerXInternalName, writeMethod.getName(), writeMethod.getDescriptor(), false);
            methodVisitor.visitLabel(end);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitEnd();
        });

        // MyEvent#shouldCommit()
        updateMethod(METHOD_EVENT_SHOULD_COMMIT, methodVisitor -> {
            Label fail = new Label();
            if (guardHandlerReference) {
                getEventHandler(methodVisitor);
                methodVisitor.visitJumpInsn(Opcodes.IFNULL, fail);
            }
            // if (!eventHandler.shouldCommit(duration) goto fail;
            getEventHandler(methodVisitor);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getInternalClassName(), FIELD_DURATION, "J");
            ASMToolkit.invokeVirtual(methodVisitor, TYPE_EVENT_HANDLER.getInternalName(), METHOD_EVENT_HANDLER_SHOULD_COMMIT);
            methodVisitor.visitJumpInsn(Opcodes.IFEQ, fail);
            for (SettingInfo si : settingInfos) {
                // if (!settingsMethod(eventHandler.settingX)) goto fail;
                methodVisitor.visitIntInsn(Opcodes.ALOAD, 0);
                if (untypedEventHandler) {
                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, getInternalClassName(), FIELD_EVENT_HANDLER, TYPE_OBJECT.getDescriptor());
                } else {
                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, getInternalClassName(), FIELD_EVENT_HANDLER, Type.getDescriptor(EventHandler.class));
                }
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, eventHandlerXInternalName);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, eventHandlerXInternalName, si.fieldName, TYPE_SETTING_CONTROL.getDescriptor());
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, si.internalSettingName);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getInternalClassName(), si.methodName, "(" + si.settingDescriptor + ")Z", false);
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, fail);
            }
            // return true
            methodVisitor.visitInsn(Opcodes.ICONST_1);
            methodVisitor.visitInsn(Opcodes.IRETURN);
            // return false
            methodVisitor.visitLabel(fail);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.IRETURN);
        });
    }


    private void getEventHandler(MethodVisitor methodVisitor) {
        if (untypedEventHandler) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, getInternalClassName(), FIELD_EVENT_HANDLER, TYPE_OBJECT.getDescriptor());
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, TYPE_EVENT_HANDLER.getInternalName());
        } else {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, getInternalClassName(), FIELD_EVENT_HANDLER, Type.getDescriptor(EventHandler.class));
        }
    }

    private void makeUninstrumented() {
        updateExistingWithReturnFalse(METHOD_EVENT_SHOULD_COMMIT);
        updateExistingWithReturnFalse(METHOD_IS_ENABLED);
        updateExistingWithEmptyVoidMethod(METHOD_COMMIT);
        updateExistingWithEmptyVoidMethod(METHOD_BEGIN);
        updateExistingWithEmptyVoidMethod(METHOD_END);
    }

    private final void updateExistingWithEmptyVoidMethod(Method voidMethod) {
        updateMethod(voidMethod, methodVisitor -> {
            methodVisitor.visitInsn(Opcodes.RETURN);
        });
    }

    private final void updateExistingWithReturnFalse(Method voidMethod) {
        updateMethod(voidMethod, methodVisitor -> {
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.IRETURN);
        });
    }

    private MethodNode getMethodNode(Method method) {
        for (MethodNode m : classNode.methods) {
            if (m.name.equals(method.getName()) && m.desc.equals(method.getDescriptor())) {
                return m;
            }
        }
        return null;
    }

    private final void updateMethod(Method method, Consumer<MethodVisitor> code) {
        MethodNode old = getMethodNode(method);
        int index = classNode.methods.indexOf(old);
        classNode.methods.remove(old);
        MethodVisitor mv = classNode.visitMethod(old.access, old.name, old.desc, null, null);
        mv.visitCode();
        code.accept(mv);
        mv.visitMaxs(0, 0);
        MethodNode newMethod = getMethodNode(method);
        classNode.methods.remove(newMethod);
        classNode.methods.add(index, newMethod);
    }

    public static Method makeWriteMethod(List<FieldInfo> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (FieldInfo v : fields) {
            sb.append(v.fieldDescriptor);
        }
        sb.append(")V");
        return new Method("write", sb.toString());
    }

    private String getInternalClassName() {
        return classNode.name;
    }

    public List<SettingInfo> getSettingInfos() {
        return settingInfos;
    }

    public List<FieldInfo> getFieldInfos() {
        return fieldInfos;
    }

    public String getEventName() {
        return eventName;
    }

    public void setGuardHandler(boolean guardHandlerReference) {
        this.guardHandlerReference = guardHandlerReference;
    }
}
