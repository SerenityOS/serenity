/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import sun.invoke.util.VerifyAccess;
import sun.invoke.util.VerifyType;
import sun.invoke.util.Wrapper;
import sun.reflect.misc.ReflectUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.invoke.LambdaForm.BasicType;
import static java.lang.invoke.LambdaForm.BasicType.*;
import static java.lang.invoke.LambdaForm.*;
import static java.lang.invoke.MethodHandleNatives.Constants.*;
import static java.lang.invoke.MethodHandleStatics.*;
import static java.lang.invoke.MethodHandles.Lookup.*;

/**
 * Code generation backend for LambdaForm.
 * <p>
 * @author John Rose, JSR 292 EG
 */
class InvokerBytecodeGenerator {
    /** Define class names for convenience. */
    private static final String MH      = "java/lang/invoke/MethodHandle";
    private static final String MHI     = "java/lang/invoke/MethodHandleImpl";
    private static final String LF      = "java/lang/invoke/LambdaForm";
    private static final String LFN     = "java/lang/invoke/LambdaForm$Name";
    private static final String CLS     = "java/lang/Class";
    private static final String OBJ     = "java/lang/Object";
    private static final String OBJARY  = "[Ljava/lang/Object;";

    private static final String LOOP_CLAUSES = MHI + "$LoopClauses";
    private static final String MHARY2       = "[[L" + MH + ";";
    private static final String MH_SIG       = "L" + MH + ";";


    private static final String LF_SIG  = "L" + LF + ";";
    private static final String LFN_SIG = "L" + LFN + ";";
    private static final String LL_SIG  = "(L" + OBJ + ";)L" + OBJ + ";";
    private static final String LLV_SIG = "(L" + OBJ + ";L" + OBJ + ";)V";
    private static final String CLASS_PREFIX = LF + "$";
    private static final String SOURCE_PREFIX = "LambdaForm$";

    /** Name of its super class*/
    static final String INVOKER_SUPER_NAME = OBJ;

    /** Name of new class */
    private final String name;
    private final String className;

    private final LambdaForm lambdaForm;
    private final String     invokerName;
    private final MethodType invokerType;

    /** Info about local variables in compiled lambda form */
    private int[]       localsMap;    // index
    private Class<?>[]  localClasses; // type

    /** ASM bytecode generation. */
    private ClassWriter cw;
    private MethodVisitor mv;
    private final List<ClassData> classData = new ArrayList<>();

    /** Single element internal class name lookup cache. */
    private Class<?> lastClass;
    private String lastInternalName;

    private static final MemberName.Factory MEMBERNAME_FACTORY = MemberName.getFactory();
    private static final Class<?> HOST_CLASS = LambdaForm.class;
    private static final MethodHandles.Lookup LOOKUP = lookup();

    private static MethodHandles.Lookup lookup() {
        try {
            return MethodHandles.privateLookupIn(HOST_CLASS, IMPL_LOOKUP);
        } catch (IllegalAccessException e) {
            throw newInternalError(e);
        }
    }

    /** Main constructor; other constructors delegate to this one. */
    private InvokerBytecodeGenerator(LambdaForm lambdaForm, int localsMapSize,
                                     String name, String invokerName, MethodType invokerType) {
        int p = invokerName.indexOf('.');
        if (p > -1) {
            name = invokerName.substring(0, p);
            invokerName = invokerName.substring(p + 1);
        }
        if (DUMP_CLASS_FILES) {
            name = makeDumpableClassName(name);
        }
        this.name = name;
        this.className = CLASS_PREFIX + name;
        this.lambdaForm = lambdaForm;
        this.invokerName = invokerName;
        this.invokerType = invokerType;
        this.localsMap = new int[localsMapSize+1]; // last entry of localsMap is count of allocated local slots
        this.localClasses = new Class<?>[localsMapSize+1];
    }

    /** For generating LambdaForm interpreter entry points. */
    private InvokerBytecodeGenerator(String name, String invokerName, MethodType invokerType) {
        this(null, invokerType.parameterCount(),
             name, invokerName, invokerType);
        MethodType mt = invokerType.erase();
        // Create an array to map name indexes to locals indexes.
        localsMap[0] = 0; // localsMap has at least one element
        for (int i = 1, index = 0; i < localsMap.length; i++) {
            Wrapper w = Wrapper.forBasicType(mt.parameterType(i - 1));
            index += w.stackSlots();
            localsMap[i] = index;
        }
    }

    /** For generating customized code for a single LambdaForm. */
    private InvokerBytecodeGenerator(String name, LambdaForm form, MethodType invokerType) {
        this(name, form.lambdaName(), form, invokerType);
    }

    /** For generating customized code for a single LambdaForm. */
    InvokerBytecodeGenerator(String name, String invokerName,
            LambdaForm form, MethodType invokerType) {
        this(form, form.names.length,
             name, invokerName, invokerType);
        // Create an array to map name indexes to locals indexes.
        Name[] names = form.names;
        for (int i = 0, index = 0; i < localsMap.length; i++) {
            localsMap[i] = index;
            if (i < names.length) {
                BasicType type = names[i].type();
                index += type.basicTypeSlots();
            }
        }
    }

    /** instance counters for dumped classes */
    private static final HashMap<String,Integer> DUMP_CLASS_FILES_COUNTERS;
    /** debugging flag for saving generated class files */
    private static final File DUMP_CLASS_FILES_DIR;

    static {
        if (DUMP_CLASS_FILES) {
            DUMP_CLASS_FILES_COUNTERS = new HashMap<>();
            try {
                File dumpDir = new File("DUMP_CLASS_FILES");
                if (!dumpDir.exists()) {
                    dumpDir.mkdirs();
                }
                DUMP_CLASS_FILES_DIR = dumpDir;
                System.out.println("Dumping class files to "+DUMP_CLASS_FILES_DIR+"/...");
            } catch (Exception e) {
                throw newInternalError(e);
            }
        } else {
            DUMP_CLASS_FILES_COUNTERS = null;
            DUMP_CLASS_FILES_DIR = null;
        }
    }

    private void maybeDump(final byte[] classFile) {
        if (DUMP_CLASS_FILES) {
            maybeDump(className, classFile);
        }
    }

    // Also used from BoundMethodHandle
    @SuppressWarnings("removal")
    static void maybeDump(final String className, final byte[] classFile) {
        if (DUMP_CLASS_FILES) {
            java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<>() {
                public Void run() {
                    try {
                        String dumpName = className.replace('.','/');
                        File dumpFile = new File(DUMP_CLASS_FILES_DIR, dumpName+".class");
                        System.out.println("dump: " + dumpFile);
                        dumpFile.getParentFile().mkdirs();
                        FileOutputStream file = new FileOutputStream(dumpFile);
                        file.write(classFile);
                        file.close();
                        return null;
                    } catch (IOException ex) {
                        throw newInternalError(ex);
                    }
                }
            });
        }
    }

    private static String makeDumpableClassName(String className) {
        Integer ctr;
        synchronized (DUMP_CLASS_FILES_COUNTERS) {
            ctr = DUMP_CLASS_FILES_COUNTERS.get(className);
            if (ctr == null)  ctr = 0;
            DUMP_CLASS_FILES_COUNTERS.put(className, ctr+1);
        }
        String sfx = ctr.toString();
        while (sfx.length() < 3)
            sfx = "0" + sfx;
        className += sfx;
        return className;
    }

    static class ClassData {
        final String name;
        final String desc;
        final Object value;

        ClassData(String name, String desc, Object value) {
            this.name = name;
            this.desc = desc;
            this.value = value;
        }

        public String name() { return name; }
        public String toString() {
            return name + ",value="+value;
        }
    }

    String classData(Object arg) {
        String desc;
        if (arg instanceof Class) {
            desc = "Ljava/lang/Class;";
        } else if (arg instanceof MethodHandle) {
            desc = MH_SIG;
        } else if (arg instanceof LambdaForm) {
            desc = LF_SIG;
        } else {
            desc = "Ljava/lang/Object;";
        }

        // unique static variable name
        String name;
        if (DUMP_CLASS_FILES) {
            Class<?> c = arg.getClass();
            while (c.isArray()) {
                c = c.getComponentType();
            }
            name = "_DATA_" + c.getSimpleName() + "_" + classData.size();
        } else {
            name = "_D_" + classData.size();
        }
        ClassData cd = new ClassData(name, desc, arg);
        classData.add(cd);
        return name;
    }

    List<Object> classDataValues() {
        final List<ClassData> cd = classData;
        return switch(cd.size()) {
            case 0 -> List.of();
            case 1 -> List.of(cd.get(0).value);
            case 2 -> List.of(cd.get(0).value, cd.get(1).value);
            case 3 -> List.of(cd.get(0).value, cd.get(1).value, cd.get(2).value);
            case 4 -> List.of(cd.get(0).value, cd.get(1).value, cd.get(2).value, cd.get(3).value);
            default -> {
                Object[] data = new Object[classData.size()];
                for (int i = 0; i < classData.size(); i++) {
                    data[i] = classData.get(i).value;
                }
                yield List.of(data);
            }
        };
    }

    private static String debugString(Object arg) {
        if (arg instanceof MethodHandle mh) {
            MemberName member = mh.internalMemberName();
            if (member != null)
                return member.toString();
            return mh.debugString();
        }
        return arg.toString();
    }

    /**
     * Extract the MemberName of a newly-defined method.
     */
    private MemberName loadMethod(byte[] classFile) {
        Class<?> invokerClass = LOOKUP.makeHiddenClassDefiner(className, classFile)
                                      .defineClass(true, classDataValues());
        return resolveInvokerMember(invokerClass, invokerName, invokerType);
    }

    private static MemberName resolveInvokerMember(Class<?> invokerClass, String name, MethodType type) {
        MemberName member = new MemberName(invokerClass, name, type, REF_invokeStatic);
        try {
            member = MEMBERNAME_FACTORY.resolveOrFail(REF_invokeStatic, member,
                                                      HOST_CLASS, LM_TRUSTED,
                                                      ReflectiveOperationException.class);
        } catch (ReflectiveOperationException e) {
            throw newInternalError(e);
        }
        return member;
    }

    /**
     * Set up class file generation.
     */
    private ClassWriter classFilePrologue() {
        final int NOT_ACC_PUBLIC = 0;  // not ACC_PUBLIC
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        setClassWriter(cw);
        cw.visit(CLASSFILE_VERSION, NOT_ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                className, null, INVOKER_SUPER_NAME, null);
        cw.visitSource(SOURCE_PREFIX + name, null);
        return cw;
    }

    private void methodPrologue() {
        String invokerDesc = invokerType.toMethodDescriptorString();
        mv = cw.visitMethod(Opcodes.ACC_STATIC, invokerName, invokerDesc, null, null);
    }

    /**
     * Tear down class file generation.
     */
    private void methodEpilogue() {
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /*
     * <clinit> to initialize the static final fields with the live class data
     * LambdaForms can't use condy due to bootstrapping issue.
     */
    static void clinit(ClassWriter cw, String className, List<ClassData> classData) {
        if (classData.isEmpty())
            return;

        for (ClassData p : classData) {
            // add the static field
            FieldVisitor fv = cw.visitField(Opcodes.ACC_STATIC|Opcodes.ACC_FINAL, p.name, p.desc, null, null);
            fv.visitEnd();
        }

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getType("L" + className + ";"));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandleNatives",
                           "classData", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        // we should optimize one single element case that does not need to create a List
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/util/List");
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        int index = 0;
        for (ClassData p : classData) {
            // initialize the static field
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            emitIconstInsn(mv, index++);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List",
                               "get", "(I)Ljava/lang/Object;", true);
            mv.visitTypeInsn(Opcodes.CHECKCAST, p.desc.substring(1, p.desc.length()-1));
            mv.visitFieldInsn(Opcodes.PUTSTATIC, className, p.name, p.desc);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    /*
     * Low-level emit helpers.
     */
    private void emitConst(Object con) {
        if (con == null) {
            mv.visitInsn(Opcodes.ACONST_NULL);
            return;
        }
        if (con instanceof Integer) {
            emitIconstInsn((int) con);
            return;
        }
        if (con instanceof Byte) {
            emitIconstInsn((byte)con);
            return;
        }
        if (con instanceof Short) {
            emitIconstInsn((short)con);
            return;
        }
        if (con instanceof Character) {
            emitIconstInsn((char)con);
            return;
        }
        if (con instanceof Long) {
            long x = (long) con;
            short sx = (short)x;
            if (x == sx) {
                if (sx >= 0 && sx <= 1) {
                    mv.visitInsn(Opcodes.LCONST_0 + (int) sx);
                } else {
                    emitIconstInsn((int) x);
                    mv.visitInsn(Opcodes.I2L);
                }
                return;
            }
        }
        if (con instanceof Float) {
            float x = (float) con;
            short sx = (short)x;
            if (x == sx) {
                if (sx >= 0 && sx <= 2) {
                    mv.visitInsn(Opcodes.FCONST_0 + (int) sx);
                } else {
                    emitIconstInsn((int) x);
                    mv.visitInsn(Opcodes.I2F);
                }
                return;
            }
        }
        if (con instanceof Double) {
            double x = (double) con;
            short sx = (short)x;
            if (x == sx) {
                if (sx >= 0 && sx <= 1) {
                    mv.visitInsn(Opcodes.DCONST_0 + (int) sx);
                } else {
                    emitIconstInsn((int) x);
                    mv.visitInsn(Opcodes.I2D);
                }
                return;
            }
        }
        if (con instanceof Boolean) {
            emitIconstInsn((boolean) con ? 1 : 0);
            return;
        }
        // fall through:
        mv.visitLdcInsn(con);
    }

    private void emitIconstInsn(final int cst) {
        emitIconstInsn(mv, cst);
    }

    private static void emitIconstInsn(MethodVisitor mv, int cst) {
        if (cst >= -1 && cst <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + cst);
        } else if (cst >= Byte.MIN_VALUE && cst <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, cst);
        } else if (cst >= Short.MIN_VALUE && cst <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, cst);
        } else {
            mv.visitLdcInsn(cst);
        }
    }

    /*
     * NOTE: These load/store methods use the localsMap to find the correct index!
     */
    private void emitLoadInsn(BasicType type, int index) {
        int opcode = loadInsnOpcode(type);
        mv.visitVarInsn(opcode, localsMap[index]);
    }

    private int loadInsnOpcode(BasicType type) throws InternalError {
        return switch (type) {
            case I_TYPE -> Opcodes.ILOAD;
            case J_TYPE -> Opcodes.LLOAD;
            case F_TYPE -> Opcodes.FLOAD;
            case D_TYPE -> Opcodes.DLOAD;
            case L_TYPE -> Opcodes.ALOAD;
            default -> throw new InternalError("unknown type: " + type);
        };
    }
    private void emitAloadInsn(int index) {
        emitLoadInsn(L_TYPE, index);
    }

    private void emitStoreInsn(BasicType type, int index) {
        int opcode = storeInsnOpcode(type);
        mv.visitVarInsn(opcode, localsMap[index]);
    }

    private int storeInsnOpcode(BasicType type) throws InternalError {
        return switch (type) {
            case I_TYPE -> Opcodes.ISTORE;
            case J_TYPE -> Opcodes.LSTORE;
            case F_TYPE -> Opcodes.FSTORE;
            case D_TYPE -> Opcodes.DSTORE;
            case L_TYPE -> Opcodes.ASTORE;
            default -> throw new InternalError("unknown type: " + type);
        };
    }
    private void emitAstoreInsn(int index) {
        emitStoreInsn(L_TYPE, index);
    }

    private byte arrayTypeCode(Wrapper elementType) {
        return (byte) switch (elementType) {
            case BOOLEAN -> Opcodes.T_BOOLEAN;
            case BYTE    -> Opcodes.T_BYTE;
            case CHAR    -> Opcodes.T_CHAR;
            case SHORT   -> Opcodes.T_SHORT;
            case INT     -> Opcodes.T_INT;
            case LONG    -> Opcodes.T_LONG;
            case FLOAT   -> Opcodes.T_FLOAT;
            case DOUBLE  -> Opcodes.T_DOUBLE;
            case OBJECT  -> 0; // in place of Opcodes.T_OBJECT
            default -> throw new InternalError();
        };
    }

    private int arrayInsnOpcode(byte tcode, int aaop) throws InternalError {
        assert(aaop == Opcodes.AASTORE || aaop == Opcodes.AALOAD);
        int xas = switch (tcode) {
            case Opcodes.T_BOOLEAN -> Opcodes.BASTORE;
            case Opcodes.T_BYTE    -> Opcodes.BASTORE;
            case Opcodes.T_CHAR    -> Opcodes.CASTORE;
            case Opcodes.T_SHORT   -> Opcodes.SASTORE;
            case Opcodes.T_INT     -> Opcodes.IASTORE;
            case Opcodes.T_LONG    -> Opcodes.LASTORE;
            case Opcodes.T_FLOAT   -> Opcodes.FASTORE;
            case Opcodes.T_DOUBLE  -> Opcodes.DASTORE;
            case 0                 -> Opcodes.AASTORE;
            default -> throw new InternalError();
        };
        return xas - Opcodes.AASTORE + aaop;
    }

    /**
     * Emit a boxing call.
     *
     * @param wrapper primitive type class to box.
     */
    private void emitBoxing(Wrapper wrapper) {
        String owner = "java/lang/" + wrapper.wrapperType().getSimpleName();
        String name  = "valueOf";
        String desc  = "(" + wrapper.basicTypeChar() + ")L" + owner + ";";
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc, false);
    }

    /**
     * Emit an unboxing call (plus preceding checkcast).
     *
     * @param wrapper wrapper type class to unbox.
     */
    private void emitUnboxing(Wrapper wrapper) {
        String owner = "java/lang/" + wrapper.wrapperType().getSimpleName();
        String name  = wrapper.primitiveSimpleName() + "Value";
        String desc  = "()" + wrapper.basicTypeChar();
        emitReferenceCast(wrapper.wrapperType(), null);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, desc, false);
    }

    /**
     * Emit an implicit conversion for an argument which must be of the given pclass.
     * This is usually a no-op, except when pclass is a subword type or a reference other than Object or an interface.
     *
     * @param ptype type of value present on stack
     * @param pclass type of value required on stack
     * @param arg compile-time representation of value on stack (Node, constant) or null if none
     */
    private void emitImplicitConversion(BasicType ptype, Class<?> pclass, Object arg) {
        assert(basicType(pclass) == ptype);  // boxing/unboxing handled by caller
        if (pclass == ptype.basicTypeClass() && ptype != L_TYPE)
            return;   // nothing to do
        switch (ptype) {
            case L_TYPE:
                if (VerifyType.isNullConversion(Object.class, pclass, false)) {
                    if (PROFILE_LEVEL > 0)
                        emitReferenceCast(Object.class, arg);
                    return;
                }
                emitReferenceCast(pclass, arg);
                return;
            case I_TYPE:
                if (!VerifyType.isNullConversion(int.class, pclass, false))
                    emitPrimCast(ptype.basicTypeWrapper(), Wrapper.forPrimitiveType(pclass));
                return;
        }
        throw newInternalError("bad implicit conversion: tc="+ptype+": "+pclass);
    }

    /** Update localClasses type map.  Return true if the information is already present. */
    private boolean assertStaticType(Class<?> cls, Name n) {
        int local = n.index();
        Class<?> aclass = localClasses[local];
        if (aclass != null && (aclass == cls || cls.isAssignableFrom(aclass))) {
            return true;  // type info is already present
        } else if (aclass == null || aclass.isAssignableFrom(cls)) {
            localClasses[local] = cls;  // type info can be improved
        }
        return false;
    }

    private void emitReferenceCast(Class<?> cls, Object arg) {
        Name writeBack = null;  // local to write back result
        if (arg instanceof Name n) {
            if (lambdaForm.useCount(n) > 1) {
                // This guy gets used more than once.
                writeBack = n;
                if (assertStaticType(cls, n)) {
                    return; // this cast was already performed
                }
            }
        }
        if (isStaticallyNameable(cls)) {
            String sig = getInternalName(cls);
            mv.visitTypeInsn(Opcodes.CHECKCAST, sig);
        } else {
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, classData(cls), "Ljava/lang/Class;");
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CLS, "cast", LL_SIG, false);
            if (Object[].class.isAssignableFrom(cls))
                mv.visitTypeInsn(Opcodes.CHECKCAST, OBJARY);
            else if (PROFILE_LEVEL > 0)
                mv.visitTypeInsn(Opcodes.CHECKCAST, OBJ);
        }
        if (writeBack != null) {
            mv.visitInsn(Opcodes.DUP);
            emitAstoreInsn(writeBack.index());
        }
    }

    /**
     * Emits an actual return instruction conforming to the given return type.
     */
    private void emitReturnInsn(BasicType type) {
        int opcode = switch (type) {
            case I_TYPE -> Opcodes.IRETURN;
            case J_TYPE -> Opcodes.LRETURN;
            case F_TYPE -> Opcodes.FRETURN;
            case D_TYPE -> Opcodes.DRETURN;
            case L_TYPE -> Opcodes.ARETURN;
            case V_TYPE -> Opcodes.RETURN;
            default -> throw new InternalError("unknown return type: " + type);
        };
        mv.visitInsn(opcode);
    }

    private String getInternalName(Class<?> c) {
        if (c == Object.class)             return OBJ;
        else if (c == Object[].class)      return OBJARY;
        else if (c == Class.class)         return CLS;
        else if (c == MethodHandle.class)  return MH;
        assert(VerifyAccess.isTypeVisible(c, Object.class)) : c.getName();

        if (c == lastClass) {
            return lastInternalName;
        }
        lastClass = c;
        return lastInternalName = c.getName().replace('.', '/');
    }

    private static MemberName resolveFrom(String name, MethodType type, Class<?> holder) {
        MemberName member = new MemberName(holder, name, type, REF_invokeStatic);
        MemberName resolvedMember = MemberName.getFactory().resolveOrNull(REF_invokeStatic, member, holder, LM_TRUSTED);
        traceLambdaForm(name, type, holder, resolvedMember);
        return resolvedMember;
    }

    private static MemberName lookupPregenerated(LambdaForm form, MethodType invokerType) {
        if (form.customized != null) {
            // No pre-generated version for customized LF
            return null;
        }
        String name = form.kind.methodName;
        switch (form.kind) {
            case BOUND_REINVOKER: {
                name = name + "_" + BoundMethodHandle.speciesDataFor(form).key();
                return resolveFrom(name, invokerType, DelegatingMethodHandle.Holder.class);
            }
            case DELEGATE:                  return resolveFrom(name, invokerType, DelegatingMethodHandle.Holder.class);
            case ZERO:                      // fall-through
            case IDENTITY: {
                name = name + "_" + form.returnType().basicTypeChar();
                return resolveFrom(name, invokerType, LambdaForm.Holder.class);
            }
            case EXACT_INVOKER:             // fall-through
            case EXACT_LINKER:              // fall-through
            case LINK_TO_CALL_SITE:         // fall-through
            case LINK_TO_TARGET_METHOD:     // fall-through
            case GENERIC_INVOKER:           // fall-through
            case GENERIC_LINKER:            return resolveFrom(name, invokerType, Invokers.Holder.class);
            case GET_REFERENCE:             // fall-through
            case GET_BOOLEAN:               // fall-through
            case GET_BYTE:                  // fall-through
            case GET_CHAR:                  // fall-through
            case GET_SHORT:                 // fall-through
            case GET_INT:                   // fall-through
            case GET_LONG:                  // fall-through
            case GET_FLOAT:                 // fall-through
            case GET_DOUBLE:                // fall-through
            case PUT_REFERENCE:             // fall-through
            case PUT_BOOLEAN:               // fall-through
            case PUT_BYTE:                  // fall-through
            case PUT_CHAR:                  // fall-through
            case PUT_SHORT:                 // fall-through
            case PUT_INT:                   // fall-through
            case PUT_LONG:                  // fall-through
            case PUT_FLOAT:                 // fall-through
            case PUT_DOUBLE:                // fall-through
            case DIRECT_NEW_INVOKE_SPECIAL: // fall-through
            case DIRECT_INVOKE_INTERFACE:   // fall-through
            case DIRECT_INVOKE_SPECIAL:     // fall-through
            case DIRECT_INVOKE_SPECIAL_IFC: // fall-through
            case DIRECT_INVOKE_STATIC:      // fall-through
            case DIRECT_INVOKE_STATIC_INIT: // fall-through
            case DIRECT_INVOKE_VIRTUAL:     return resolveFrom(name, invokerType, DirectMethodHandle.Holder.class);
        }
        return null;
    }

    /**
     * Generate customized bytecode for a given LambdaForm.
     */
    static MemberName generateCustomizedCode(LambdaForm form, MethodType invokerType) {
        MemberName pregenerated = lookupPregenerated(form, invokerType);
        if (pregenerated != null)  return pregenerated; // pre-generated bytecode

        InvokerBytecodeGenerator g = new InvokerBytecodeGenerator("MH", form, invokerType);
        return g.loadMethod(g.generateCustomizedCodeBytes());
    }

    /** Generates code to check that actual receiver and LambdaForm matches */
    private boolean checkActualReceiver() {
        // Expects MethodHandle on the stack and actual receiver MethodHandle in slot #0
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, localsMap[0]);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, MHI, "assertSame", LLV_SIG, false);
        return true;
    }

    static String className(String cn) {
        assert checkClassName(cn): "Class not found: " + cn;
        return cn;
    }

    static boolean checkClassName(String cn) {
        Type tp = Type.getType(cn);
        // additional sanity so only valid "L;" descriptors work
        if (tp.getSort() != Type.OBJECT) {
            return false;
        }
        try {
            Class<?> c = Class.forName(tp.getClassName(), false, null);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static final String      DONTINLINE_SIG = className("Ljdk/internal/vm/annotation/DontInline;");
    static final String     FORCEINLINE_SIG = className("Ljdk/internal/vm/annotation/ForceInline;");
    static final String          HIDDEN_SIG = className("Ljdk/internal/vm/annotation/Hidden;");
    static final String INJECTEDPROFILE_SIG = className("Ljava/lang/invoke/InjectedProfile;");
    static final String     LF_COMPILED_SIG = className("Ljava/lang/invoke/LambdaForm$Compiled;");

    /**
     * Generate an invoker method for the passed {@link LambdaForm}.
     */
    private byte[] generateCustomizedCodeBytes() {
        classFilePrologue();
        addMethod();
        clinit(cw, className, classData);
        bogusMethod(lambdaForm);

        final byte[] classFile = toByteArray();
        maybeDump(classFile);
        return classFile;
    }

    void setClassWriter(ClassWriter cw) {
        this.cw = cw;
    }

    void addMethod() {
        methodPrologue();

        // Suppress this method in backtraces displayed to the user.
        mv.visitAnnotation(HIDDEN_SIG, true);

        // Mark this method as a compiled LambdaForm
        mv.visitAnnotation(LF_COMPILED_SIG, true);

        if (lambdaForm.forceInline) {
            // Force inlining of this invoker method.
            mv.visitAnnotation(FORCEINLINE_SIG, true);
        } else {
            mv.visitAnnotation(DONTINLINE_SIG, true);
        }

        classData(lambdaForm); // keep LambdaForm instance & its compiled form lifetime tightly coupled.

        if (lambdaForm.customized != null) {
            // Since LambdaForm is customized for a particular MethodHandle, it's safe to substitute
            // receiver MethodHandle (at slot #0) with an embedded constant and use it instead.
            // It enables more efficient code generation in some situations, since embedded constants
            // are compile-time constants for JIT compiler.
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, classData(lambdaForm.customized), MH_SIG);
            mv.visitTypeInsn(Opcodes.CHECKCAST, MH);
            assert(checkActualReceiver()); // expects MethodHandle on top of the stack
            mv.visitVarInsn(Opcodes.ASTORE, localsMap[0]);
        }

        // iterate over the form's names, generating bytecode instructions for each
        // start iterating at the first name following the arguments
        Name onStack = null;
        for (int i = lambdaForm.arity; i < lambdaForm.names.length; i++) {
            Name name = lambdaForm.names[i];

            emitStoreResult(onStack);
            onStack = name;  // unless otherwise modified below
            MethodHandleImpl.Intrinsic intr = name.function.intrinsicName();
            switch (intr) {
                case SELECT_ALTERNATIVE:
                    assert lambdaForm.isSelectAlternative(i);
                    if (PROFILE_GWT) {
                        assert(name.arguments[0] instanceof Name &&
                                ((Name)name.arguments[0]).refersTo(MethodHandleImpl.class, "profileBoolean"));
                        mv.visitAnnotation(INJECTEDPROFILE_SIG, true);
                    }
                    onStack = emitSelectAlternative(name, lambdaForm.names[i+1]);
                    i++;  // skip MH.invokeBasic of the selectAlternative result
                    continue;
                case GUARD_WITH_CATCH:
                    assert lambdaForm.isGuardWithCatch(i);
                    onStack = emitGuardWithCatch(i);
                    i += 2; // jump to the end of GWC idiom
                    continue;
                case TRY_FINALLY:
                    assert lambdaForm.isTryFinally(i);
                    onStack = emitTryFinally(i);
                    i += 2; // jump to the end of the TF idiom
                    continue;
                case TABLE_SWITCH:
                    assert lambdaForm.isTableSwitch(i);
                    int numCases = (Integer) name.function.intrinsicData();
                    onStack = emitTableSwitch(i, numCases);
                    i += 2; // jump to the end of the TS idiom
                    continue;
                case LOOP:
                    assert lambdaForm.isLoop(i);
                    onStack = emitLoop(i);
                    i += 2; // jump to the end of the LOOP idiom
                    continue;
                case ARRAY_LOAD:
                    emitArrayLoad(name);
                    continue;
                case ARRAY_STORE:
                    emitArrayStore(name);
                    continue;
                case ARRAY_LENGTH:
                    emitArrayLength(name);
                    continue;
                case IDENTITY:
                    assert(name.arguments.length == 1);
                    emitPushArguments(name, 0);
                    continue;
                case ZERO:
                    assert(name.arguments.length == 0);
                    emitConst(name.type.basicTypeWrapper().zero());
                    continue;
                case NONE:
                    // no intrinsic associated
                    break;
                default:
                    throw newInternalError("Unknown intrinsic: "+intr);
            }

            MemberName member = name.function.member();
            if (isStaticallyInvocable(member)) {
                emitStaticInvoke(member, name);
            } else {
                emitInvoke(name);
            }
        }

        // return statement
        emitReturn(onStack);

        methodEpilogue();
    }

    /*
     * @throws BytecodeGenerationException if something goes wrong when
     *         generating the byte code
     */
    private byte[] toByteArray() {
        try {
            return cw.toByteArray();
        } catch (RuntimeException e) {
            throw new BytecodeGenerationException(e);
        }
    }

    /**
     * The BytecodeGenerationException.
     */
    @SuppressWarnings("serial")
    static final class BytecodeGenerationException extends RuntimeException {
        BytecodeGenerationException(Exception cause) {
            super(cause);
        }
    }

    void emitArrayLoad(Name name)   { emitArrayOp(name, Opcodes.AALOAD);      }
    void emitArrayStore(Name name)  { emitArrayOp(name, Opcodes.AASTORE);     }
    void emitArrayLength(Name name) { emitArrayOp(name, Opcodes.ARRAYLENGTH); }

    void emitArrayOp(Name name, int arrayOpcode) {
        assert arrayOpcode == Opcodes.AALOAD || arrayOpcode == Opcodes.AASTORE || arrayOpcode == Opcodes.ARRAYLENGTH;
        Class<?> elementType = name.function.methodType().parameterType(0).getComponentType();
        assert elementType != null;
        emitPushArguments(name, 0);
        if (arrayOpcode != Opcodes.ARRAYLENGTH && elementType.isPrimitive()) {
            Wrapper w = Wrapper.forPrimitiveType(elementType);
            arrayOpcode = arrayInsnOpcode(arrayTypeCode(w), arrayOpcode);
        }
        mv.visitInsn(arrayOpcode);
    }

    /**
     * Emit an invoke for the given name.
     */
    void emitInvoke(Name name) {
        assert(!name.isLinkerMethodInvoke());  // should use the static path for these
        if (true) {
            // push receiver
            MethodHandle target = name.function.resolvedHandle();
            assert(target != null) : name.exprString();
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, classData(target), MH_SIG);
            emitReferenceCast(MethodHandle.class, target);
        } else {
            // load receiver
            emitAloadInsn(0);
            emitReferenceCast(MethodHandle.class, null);
            mv.visitFieldInsn(Opcodes.GETFIELD, MH, "form", LF_SIG);
            mv.visitFieldInsn(Opcodes.GETFIELD, LF, "names", LFN_SIG);
            // TODO more to come
        }

        // push arguments
        emitPushArguments(name, 0);

        // invocation
        MethodType type = name.function.methodType();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", type.basicType().toMethodDescriptorString(), false);
    }

    private static final Class<?>[] STATICALLY_INVOCABLE_PACKAGES = {
        // Sample classes from each package we are willing to bind to statically:
        java.lang.Object.class,
        java.util.Arrays.class,
        jdk.internal.misc.Unsafe.class
        //MethodHandle.class already covered
    };

    static boolean isStaticallyInvocable(NamedFunction ... functions) {
        for (NamedFunction nf : functions) {
            if (!isStaticallyInvocable(nf.member())) {
                return false;
            }
        }
        return true;
    }

    static boolean isStaticallyInvocable(Name name) {
        return isStaticallyInvocable(name.function.member());
    }

    static boolean isStaticallyInvocable(MemberName member) {
        if (member == null)  return false;
        if (member.isConstructor())  return false;
        Class<?> cls = member.getDeclaringClass();
        // Fast-path non-private members declared by MethodHandles, which is a common
        // case
        if (MethodHandle.class.isAssignableFrom(cls) && !member.isPrivate()) {
            assert(isStaticallyInvocableType(member.getMethodOrFieldType()));
            return true;
        }
        if (cls.isArray() || cls.isPrimitive())
            return false;  // FIXME
        if (cls.isAnonymousClass() || cls.isLocalClass())
            return false;  // inner class of some sort
        if (cls.getClassLoader() != MethodHandle.class.getClassLoader())
            return false;  // not on BCP
        if (cls.isHidden())
            return false;
        if (!isStaticallyInvocableType(member.getMethodOrFieldType()))
            return false;
        if (!member.isPrivate() && VerifyAccess.isSamePackage(MethodHandle.class, cls))
            return true;   // in java.lang.invoke package
        if (member.isPublic() && isStaticallyNameable(cls))
            return true;
        return false;
    }

    private static boolean isStaticallyInvocableType(MethodType mtype) {
        if (!isStaticallyNameable(mtype.returnType()))
            return false;
        for (Class<?> ptype : mtype.parameterArray())
            if (!isStaticallyNameable(ptype))
                return false;
        return true;
    }

    static boolean isStaticallyNameable(Class<?> cls) {
        if (cls == Object.class)
            return true;
        if (MethodHandle.class.isAssignableFrom(cls)) {
            assert(!cls.isHidden());
            return true;
        }
        while (cls.isArray())
            cls = cls.getComponentType();
        if (cls.isPrimitive())
            return true;  // int[].class, for example
        if (cls.isHidden())
            return false;
        // could use VerifyAccess.isClassAccessible but the following is a safe approximation
        if (cls.getClassLoader() != Object.class.getClassLoader())
            return false;
        if (VerifyAccess.isSamePackage(MethodHandle.class, cls))
            return true;
        if (!Modifier.isPublic(cls.getModifiers()))
            return false;
        for (Class<?> pkgcls : STATICALLY_INVOCABLE_PACKAGES) {
            if (VerifyAccess.isSamePackage(pkgcls, cls))
                return true;
        }
        return false;
    }

    void emitStaticInvoke(Name name) {
        emitStaticInvoke(name.function.member(), name);
    }

    /**
     * Emit an invoke for the given name, using the MemberName directly.
     */
    void emitStaticInvoke(MemberName member, Name name) {
        assert(member.equals(name.function.member()));
        Class<?> defc = member.getDeclaringClass();
        String cname = getInternalName(defc);
        String mname = member.getName();
        String mtype;
        byte refKind = member.getReferenceKind();
        if (refKind == REF_invokeSpecial) {
            // in order to pass the verifier, we need to convert this to invokevirtual in all cases
            assert(member.canBeStaticallyBound()) : member;
            refKind = REF_invokeVirtual;
        }

        assert(!(member.getDeclaringClass().isInterface() && refKind == REF_invokeVirtual));

        // push arguments
        emitPushArguments(name, 0);

        // invocation
        if (member.isMethod()) {
            mtype = member.getMethodType().toMethodDescriptorString();
            mv.visitMethodInsn(refKindOpcode(refKind), cname, mname, mtype,
                               member.getDeclaringClass().isInterface());
        } else {
            mtype = MethodType.toFieldDescriptorString(member.getFieldType());
            mv.visitFieldInsn(refKindOpcode(refKind), cname, mname, mtype);
        }
        // Issue a type assertion for the result, so we can avoid casts later.
        if (name.type == L_TYPE) {
            Class<?> rtype = member.getInvocationType().returnType();
            assert(!rtype.isPrimitive());
            if (rtype != Object.class && !rtype.isInterface()) {
                assertStaticType(rtype, name);
            }
        }
    }

    int refKindOpcode(byte refKind) {
        switch (refKind) {
        case REF_invokeVirtual:      return Opcodes.INVOKEVIRTUAL;
        case REF_invokeStatic:       return Opcodes.INVOKESTATIC;
        case REF_invokeSpecial:      return Opcodes.INVOKESPECIAL;
        case REF_invokeInterface:    return Opcodes.INVOKEINTERFACE;
        case REF_getField:           return Opcodes.GETFIELD;
        case REF_putField:           return Opcodes.PUTFIELD;
        case REF_getStatic:          return Opcodes.GETSTATIC;
        case REF_putStatic:          return Opcodes.PUTSTATIC;
        }
        throw new InternalError("refKind="+refKind);
    }

    /**
     * Emit bytecode for the selectAlternative idiom.
     *
     * The pattern looks like (Cf. MethodHandleImpl.makeGuardWithTest):
     * <blockquote><pre>{@code
     *   Lambda(a0:L,a1:I)=>{
     *     t2:I=foo.test(a1:I);
     *     t3:L=MethodHandleImpl.selectAlternative(t2:I,(MethodHandle(int)int),(MethodHandle(int)int));
     *     t4:I=MethodHandle.invokeBasic(t3:L,a1:I);t4:I}
     * }</pre></blockquote>
     */
    private Name emitSelectAlternative(Name selectAlternativeName, Name invokeBasicName) {
        assert isStaticallyInvocable(invokeBasicName);

        Name receiver = (Name) invokeBasicName.arguments[0];

        Label L_fallback = new Label();
        Label L_done     = new Label();

        // load test result
        emitPushArgument(selectAlternativeName, 0);

        // if_icmpne L_fallback
        mv.visitJumpInsn(Opcodes.IFEQ, L_fallback);

        // invoke selectAlternativeName.arguments[1]
        Class<?>[] preForkClasses = localClasses.clone();
        emitPushArgument(selectAlternativeName, 1);  // get 2nd argument of selectAlternative
        emitAstoreInsn(receiver.index());  // store the MH in the receiver slot
        emitStaticInvoke(invokeBasicName);

        // goto L_done
        mv.visitJumpInsn(Opcodes.GOTO, L_done);

        // L_fallback:
        mv.visitLabel(L_fallback);

        // invoke selectAlternativeName.arguments[2]
        System.arraycopy(preForkClasses, 0, localClasses, 0, preForkClasses.length);
        emitPushArgument(selectAlternativeName, 2);  // get 3rd argument of selectAlternative
        emitAstoreInsn(receiver.index());  // store the MH in the receiver slot
        emitStaticInvoke(invokeBasicName);

        // L_done:
        mv.visitLabel(L_done);
        // for now do not bother to merge typestate; just reset to the dominator state
        System.arraycopy(preForkClasses, 0, localClasses, 0, preForkClasses.length);

        return invokeBasicName;  // return what's on stack
    }

    /**
     * Emit bytecode for the guardWithCatch idiom.
     *
     * The pattern looks like (Cf. MethodHandleImpl.makeGuardWithCatch):
     * <blockquote><pre>{@code
     *  guardWithCatch=Lambda(a0:L,a1:L,a2:L,a3:L,a4:L,a5:L,a6:L,a7:L)=>{
     *    t8:L=MethodHandle.invokeBasic(a4:L,a6:L,a7:L);
     *    t9:L=MethodHandleImpl.guardWithCatch(a1:L,a2:L,a3:L,t8:L);
     *   t10:I=MethodHandle.invokeBasic(a5:L,t9:L);t10:I}
     * }</pre></blockquote>
     *
     * It is compiled into bytecode equivalent of the following code:
     * <blockquote><pre>{@code
     *  try {
     *      return a1.invokeBasic(a6, a7);
     *  } catch (Throwable e) {
     *      if (!a2.isInstance(e)) throw e;
     *      return a3.invokeBasic(ex, a6, a7);
     *  }}</pre></blockquote>
     */
    private Name emitGuardWithCatch(int pos) {
        Name args    = lambdaForm.names[pos];
        Name invoker = lambdaForm.names[pos+1];
        Name result  = lambdaForm.names[pos+2];

        Label L_startBlock = new Label();
        Label L_endBlock = new Label();
        Label L_handler = new Label();
        Label L_done = new Label();

        Class<?> returnType = result.function.resolvedHandle().type().returnType();
        MethodType type = args.function.resolvedHandle().type()
                              .dropParameterTypes(0,1)
                              .changeReturnType(returnType);

        mv.visitTryCatchBlock(L_startBlock, L_endBlock, L_handler, "java/lang/Throwable");

        // Normal case
        mv.visitLabel(L_startBlock);
        // load target
        emitPushArgument(invoker, 0);
        emitPushArguments(args, 1); // skip 1st argument: method handle
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", type.basicType().toMethodDescriptorString(), false);
        mv.visitLabel(L_endBlock);
        mv.visitJumpInsn(Opcodes.GOTO, L_done);

        // Exceptional case
        mv.visitLabel(L_handler);

        // Check exception's type
        mv.visitInsn(Opcodes.DUP);
        // load exception class
        emitPushArgument(invoker, 1);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "isInstance", "(Ljava/lang/Object;)Z", false);
        Label L_rethrow = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, L_rethrow);

        // Invoke catcher
        // load catcher
        emitPushArgument(invoker, 2);
        mv.visitInsn(Opcodes.SWAP);
        emitPushArguments(args, 1); // skip 1st argument: method handle
        MethodType catcherType = type.insertParameterTypes(0, Throwable.class);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", catcherType.basicType().toMethodDescriptorString(), false);
        mv.visitJumpInsn(Opcodes.GOTO, L_done);

        mv.visitLabel(L_rethrow);
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitLabel(L_done);

        return result;
    }

    /**
     * Emit bytecode for the tryFinally idiom.
     * <p>
     * The pattern looks like (Cf. MethodHandleImpl.makeTryFinally):
     * <blockquote><pre>{@code
     * // a0: BMH
     * // a1: target, a2: cleanup
     * // a3: box, a4: unbox
     * // a5 (and following): arguments
     * tryFinally=Lambda(a0:L,a1:L,a2:L,a3:L,a4:L,a5:L)=>{
     *   t6:L=MethodHandle.invokeBasic(a3:L,a5:L);         // box the arguments into an Object[]
     *   t7:L=MethodHandleImpl.tryFinally(a1:L,a2:L,t6:L); // call the tryFinally executor
     *   t8:L=MethodHandle.invokeBasic(a4:L,t7:L);t8:L}    // unbox the result; return the result
     * }</pre></blockquote>
     * <p>
     * It is compiled into bytecode equivalent to the following code:
     * <blockquote><pre>{@code
     * Throwable t;
     * Object r;
     * try {
     *     r = a1.invokeBasic(a5);
     * } catch (Throwable thrown) {
     *     t = thrown;
     *     throw t;
     * } finally {
     *     r = a2.invokeBasic(t, r, a5);
     * }
     * return r;
     * }</pre></blockquote>
     * <p>
     * Specifically, the bytecode will have the following form (the stack effects are given for the beginnings of
     * blocks, and for the situations after executing the given instruction - the code will have a slightly different
     * shape if the return type is {@code void}):
     * <blockquote><pre>{@code
     * TRY:                 (--)
     *                      load target                             (-- target)
     *                      load args                               (-- args... target)
     *                      INVOKEVIRTUAL MethodHandle.invokeBasic  (depends)
     * FINALLY_NORMAL:      (-- r_2nd* r)
     *                      store returned value                    (--)
     *                      load cleanup                            (-- cleanup)
     *                      ACONST_NULL                             (-- t cleanup)
     *                      load returned value                     (-- r_2nd* r t cleanup)
     *                      load args                               (-- args... r_2nd* r t cleanup)
     *                      INVOKEVIRTUAL MethodHandle.invokeBasic  (-- r_2nd* r)
     *                      GOTO DONE
     * CATCH:               (-- t)
     *                      DUP                                     (-- t t)
     * FINALLY_EXCEPTIONAL: (-- t t)
     *                      load cleanup                            (-- cleanup t t)
     *                      SWAP                                    (-- t cleanup t)
     *                      load default for r                      (-- r_2nd* r t cleanup t)
     *                      load args                               (-- args... r_2nd* r t cleanup t)
     *                      INVOKEVIRTUAL MethodHandle.invokeBasic  (-- r_2nd* r t)
     *                      POP/POP2*                               (-- t)
     *                      ATHROW
     * DONE:                (-- r)
     * }</pre></blockquote>
     * * = depends on whether the return type takes up 2 stack slots.
     */
    private Name emitTryFinally(int pos) {
        Name args    = lambdaForm.names[pos];
        Name invoker = lambdaForm.names[pos+1];
        Name result  = lambdaForm.names[pos+2];

        Label lFrom = new Label();
        Label lTo = new Label();
        Label lCatch = new Label();
        Label lDone = new Label();

        Class<?> returnType = result.function.resolvedHandle().type().returnType();
        BasicType basicReturnType = BasicType.basicType(returnType);
        boolean isNonVoid = returnType != void.class;

        MethodType type = args.function.resolvedHandle().type()
                .dropParameterTypes(0,1)
                .changeReturnType(returnType);
        MethodType cleanupType = type.insertParameterTypes(0, Throwable.class);
        if (isNonVoid) {
            cleanupType = cleanupType.insertParameterTypes(1, returnType);
        }
        String cleanupDesc = cleanupType.basicType().toMethodDescriptorString();

        // exception handler table
        mv.visitTryCatchBlock(lFrom, lTo, lCatch, "java/lang/Throwable");

        // TRY:
        mv.visitLabel(lFrom);
        emitPushArgument(invoker, 0); // load target
        emitPushArguments(args, 1); // load args (skip 0: method handle)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", type.basicType().toMethodDescriptorString(), false);
        mv.visitLabel(lTo);

        // FINALLY_NORMAL:
        int index = extendLocalsMap(new Class<?>[]{ returnType });
        if (isNonVoid) {
            emitStoreInsn(basicReturnType, index);
        }
        emitPushArgument(invoker, 1); // load cleanup
        mv.visitInsn(Opcodes.ACONST_NULL);
        if (isNonVoid) {
            emitLoadInsn(basicReturnType, index);
        }
        emitPushArguments(args, 1); // load args (skip 0: method handle)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", cleanupDesc, false);
        mv.visitJumpInsn(Opcodes.GOTO, lDone);

        // CATCH:
        mv.visitLabel(lCatch);
        mv.visitInsn(Opcodes.DUP);

        // FINALLY_EXCEPTIONAL:
        emitPushArgument(invoker, 1); // load cleanup
        mv.visitInsn(Opcodes.SWAP);
        if (isNonVoid) {
            emitZero(BasicType.basicType(returnType)); // load default for result
        }
        emitPushArguments(args, 1); // load args (skip 0: method handle)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", cleanupDesc, false);
        if (isNonVoid) {
            emitPopInsn(basicReturnType);
        }
        mv.visitInsn(Opcodes.ATHROW);

        // DONE:
        mv.visitLabel(lDone);

        return result;
    }

    private void emitPopInsn(BasicType type) {
        mv.visitInsn(popInsnOpcode(type));
    }

    private static int popInsnOpcode(BasicType type) {
        return switch (type) {
            case I_TYPE, F_TYPE, L_TYPE -> Opcodes.POP;
            case J_TYPE, D_TYPE         -> Opcodes.POP2;
            default -> throw new InternalError("unknown type: " + type);
        };
    }

    private Name emitTableSwitch(int pos, int numCases) {
        Name args    = lambdaForm.names[pos];
        Name invoker = lambdaForm.names[pos + 1];
        Name result  = lambdaForm.names[pos + 2];

        Class<?> returnType = result.function.resolvedHandle().type().returnType();
        MethodType caseType = args.function.resolvedHandle().type()
            .dropParameterTypes(0, 1) // drop collector
            .changeReturnType(returnType);
        String caseDescriptor = caseType.basicType().toMethodDescriptorString();

        emitPushArgument(invoker, 2); // push cases
        mv.visitFieldInsn(Opcodes.GETFIELD, "java/lang/invoke/MethodHandleImpl$CasesHolder", "cases",
            "[Ljava/lang/invoke/MethodHandle;");
        int casesLocal = extendLocalsMap(new Class<?>[] { MethodHandle[].class });
        emitStoreInsn(L_TYPE, casesLocal);

        Label endLabel = new Label();
        Label defaultLabel = new Label();
        Label[] caseLabels = new Label[numCases];
        for (int i = 0; i < caseLabels.length; i++) {
            caseLabels[i] = new Label();
        }

        emitPushArgument(invoker, 0); // push switch input
        mv.visitTableSwitchInsn(0, numCases - 1, defaultLabel, caseLabels);

        mv.visitLabel(defaultLabel);
        emitPushArgument(invoker, 1); // push default handle
        emitPushArguments(args, 1); // again, skip collector
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", caseDescriptor, false);
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);

        for (int i = 0; i < numCases; i++) {
            mv.visitLabel(caseLabels[i]);
            // Load the particular case:
            emitLoadInsn(L_TYPE, casesLocal);
            emitIconstInsn(i);
            mv.visitInsn(Opcodes.AALOAD);

            // invoke it:
            emitPushArguments(args, 1); // again, skip collector
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", caseDescriptor, false);

            mv.visitJumpInsn(Opcodes.GOTO, endLabel);
        }

        mv.visitLabel(endLabel);

        return result;
    }

    /**
     * Emit bytecode for the loop idiom.
     * <p>
     * The pattern looks like (Cf. MethodHandleImpl.loop):
     * <blockquote><pre>{@code
     * // a0: BMH
     * // a1: LoopClauses (containing an array of arrays: inits, steps, preds, finis)
     * // a2: box, a3: unbox
     * // a4 (and following): arguments
     * loop=Lambda(a0:L,a1:L,a2:L,a3:L,a4:L)=>{
     *   t5:L=MethodHandle.invokeBasic(a2:L,a4:L);          // box the arguments into an Object[]
     *   t6:L=MethodHandleImpl.loop(bt:L,a1:L,t5:L);        // call the loop executor (with supplied types in bt)
     *   t7:L=MethodHandle.invokeBasic(a3:L,t6:L);t7:L}     // unbox the result; return the result
     * }</pre></blockquote>
     * <p>
     * It is compiled into bytecode equivalent to the code seen in {@link MethodHandleImpl#loop(BasicType[],
     * MethodHandleImpl.LoopClauses, Object...)}, with the difference that no arrays
     * will be used for local state storage. Instead, the local state will be mapped to actual stack slots.
     * <p>
     * Bytecode generation applies an unrolling scheme to enable better bytecode generation regarding local state type
     * handling. The generated bytecode will have the following form ({@code void} types are ignored for convenience).
     * Assume there are {@code C} clauses in the loop.
     * <blockquote><pre>{@code
     * PREINIT: ALOAD_1
     *          CHECKCAST LoopClauses
     *          GETFIELD LoopClauses.clauses
     *          ASTORE clauseDataIndex          // place the clauses 2-dimensional array on the stack
     * INIT:    (INIT_SEQ for clause 1)
     *          ...
     *          (INIT_SEQ for clause C)
     * LOOP:    (LOOP_SEQ for clause 1)
     *          ...
     *          (LOOP_SEQ for clause C)
     *          GOTO LOOP
     * DONE:    ...
     * }</pre></blockquote>
     * <p>
     * The {@code INIT_SEQ_x} sequence for clause {@code x} (with {@code x} ranging from {@code 0} to {@code C-1}) has
     * the following shape. Assume slot {@code vx} is used to hold the state for clause {@code x}.
     * <blockquote><pre>{@code
     * INIT_SEQ_x:  ALOAD clauseDataIndex
     *              ICONST_0
     *              AALOAD      // load the inits array
     *              ICONST x
     *              AALOAD      // load the init handle for clause x
     *              load args
     *              INVOKEVIRTUAL MethodHandle.invokeBasic
     *              store vx
     * }</pre></blockquote>
     * <p>
     * The {@code LOOP_SEQ_x} sequence for clause {@code x} (with {@code x} ranging from {@code 0} to {@code C-1}) has
     * the following shape. Again, assume slot {@code vx} is used to hold the state for clause {@code x}.
     * <blockquote><pre>{@code
     * LOOP_SEQ_x:  ALOAD clauseDataIndex
     *              ICONST_1
     *              AALOAD              // load the steps array
     *              ICONST x
     *              AALOAD              // load the step handle for clause x
     *              load locals
     *              load args
     *              INVOKEVIRTUAL MethodHandle.invokeBasic
     *              store vx
     *              ALOAD clauseDataIndex
     *              ICONST_2
     *              AALOAD              // load the preds array
     *              ICONST x
     *              AALOAD              // load the pred handle for clause x
     *              load locals
     *              load args
     *              INVOKEVIRTUAL MethodHandle.invokeBasic
     *              IFNE LOOP_SEQ_x+1   // predicate returned false -> jump to next clause
     *              ALOAD clauseDataIndex
     *              ICONST_3
     *              AALOAD              // load the finis array
     *              ICONST x
     *              AALOAD              // load the fini handle for clause x
     *              load locals
     *              load args
     *              INVOKEVIRTUAL MethodHandle.invokeBasic
     *              GOTO DONE           // jump beyond end of clauses to return from loop
     * }</pre></blockquote>
     */
    private Name emitLoop(int pos) {
        Name args    = lambdaForm.names[pos];
        Name invoker = lambdaForm.names[pos+1];
        Name result  = lambdaForm.names[pos+2];

        // extract clause and loop-local state types
        // find the type info in the loop invocation
        BasicType[] loopClauseTypes = (BasicType[]) invoker.arguments[0];
        Class<?>[] loopLocalStateTypes = Stream.of(loopClauseTypes).
                filter(bt -> bt != BasicType.V_TYPE).map(BasicType::basicTypeClass).toArray(Class<?>[]::new);
        Class<?>[] localTypes = new Class<?>[loopLocalStateTypes.length + 1];
        localTypes[0] = MethodHandleImpl.LoopClauses.class;
        System.arraycopy(loopLocalStateTypes, 0, localTypes, 1, loopLocalStateTypes.length);

        final int clauseDataIndex = extendLocalsMap(localTypes);
        final int firstLoopStateIndex = clauseDataIndex + 1;

        Class<?> returnType = result.function.resolvedHandle().type().returnType();
        MethodType loopType = args.function.resolvedHandle().type()
                .dropParameterTypes(0,1)
                .changeReturnType(returnType);
        MethodType loopHandleType = loopType.insertParameterTypes(0, loopLocalStateTypes);
        MethodType predType = loopHandleType.changeReturnType(boolean.class);
        MethodType finiType = loopHandleType;

        final int nClauses = loopClauseTypes.length;

        // indices to invoker arguments to load method handle arrays
        final int inits = 1;
        final int steps = 2;
        final int preds = 3;
        final int finis = 4;

        Label lLoop = new Label();
        Label lDone = new Label();
        Label lNext;

        // PREINIT:
        emitPushArgument(MethodHandleImpl.LoopClauses.class, invoker.arguments[1]);
        mv.visitFieldInsn(Opcodes.GETFIELD, LOOP_CLAUSES, "clauses", MHARY2);
        emitAstoreInsn(clauseDataIndex);

        // INIT:
        for (int c = 0, state = 0; c < nClauses; ++c) {
            MethodType cInitType = loopType.changeReturnType(loopClauseTypes[c].basicTypeClass());
            emitLoopHandleInvoke(invoker, inits, c, args, false, cInitType, loopLocalStateTypes, clauseDataIndex,
                    firstLoopStateIndex);
            if (cInitType.returnType() != void.class) {
                emitStoreInsn(BasicType.basicType(cInitType.returnType()), firstLoopStateIndex + state);
                ++state;
            }
        }

        // LOOP:
        mv.visitLabel(lLoop);

        for (int c = 0, state = 0; c < nClauses; ++c) {
            lNext = new Label();

            MethodType stepType = loopHandleType.changeReturnType(loopClauseTypes[c].basicTypeClass());
            boolean isVoid = stepType.returnType() == void.class;

            // invoke loop step
            emitLoopHandleInvoke(invoker, steps, c, args, true, stepType, loopLocalStateTypes, clauseDataIndex,
                    firstLoopStateIndex);
            if (!isVoid) {
                emitStoreInsn(BasicType.basicType(stepType.returnType()), firstLoopStateIndex + state);
                ++state;
            }

            // invoke loop predicate
            emitLoopHandleInvoke(invoker, preds, c, args, true, predType, loopLocalStateTypes, clauseDataIndex,
                    firstLoopStateIndex);
            mv.visitJumpInsn(Opcodes.IFNE, lNext);

            // invoke fini
            emitLoopHandleInvoke(invoker, finis, c, args, true, finiType, loopLocalStateTypes, clauseDataIndex,
                    firstLoopStateIndex);
            mv.visitJumpInsn(Opcodes.GOTO, lDone);

            // this is the beginning of the next loop clause
            mv.visitLabel(lNext);
        }

        mv.visitJumpInsn(Opcodes.GOTO, lLoop);

        // DONE:
        mv.visitLabel(lDone);

        return result;
    }

    private int extendLocalsMap(Class<?>[] types) {
        int firstSlot = localsMap.length - 1;
        localsMap = Arrays.copyOf(localsMap, localsMap.length + types.length);
        localClasses = Arrays.copyOf(localClasses, localClasses.length + types.length);
        System.arraycopy(types, 0, localClasses, firstSlot, types.length);
        int index = localsMap[firstSlot - 1] + 1;
        int lastSlots = 0;
        for (int i = 0; i < types.length; ++i) {
            localsMap[firstSlot + i] = index;
            lastSlots = BasicType.basicType(localClasses[firstSlot + i]).basicTypeSlots();
            index += lastSlots;
        }
        localsMap[localsMap.length - 1] = index - lastSlots;
        return firstSlot;
    }

    private void emitLoopHandleInvoke(Name holder, int handles, int clause, Name args, boolean pushLocalState,
                                      MethodType type, Class<?>[] loopLocalStateTypes, int clauseDataSlot,
                                      int firstLoopStateSlot) {
        // load handle for clause
        emitPushClauseArray(clauseDataSlot, handles);
        emitIconstInsn(clause);
        mv.visitInsn(Opcodes.AALOAD);
        // load loop state (preceding the other arguments)
        if (pushLocalState) {
            for (int s = 0; s < loopLocalStateTypes.length; ++s) {
                emitLoadInsn(BasicType.basicType(loopLocalStateTypes[s]), firstLoopStateSlot + s);
            }
        }
        // load loop args (skip 0: method handle)
        emitPushArguments(args, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", type.toMethodDescriptorString(), false);
    }

    private void emitPushClauseArray(int clauseDataSlot, int which) {
        emitAloadInsn(clauseDataSlot);
        emitIconstInsn(which - 1);
        mv.visitInsn(Opcodes.AALOAD);
    }

    private void emitZero(BasicType type) {
        mv.visitInsn(switch (type) {
            case I_TYPE -> Opcodes.ICONST_0;
            case J_TYPE -> Opcodes.LCONST_0;
            case F_TYPE -> Opcodes.FCONST_0;
            case D_TYPE -> Opcodes.DCONST_0;
            case L_TYPE -> Opcodes.ACONST_NULL;
            default -> throw new InternalError("unknown type: " + type);
        });
    }

    private void emitPushArguments(Name args, int start) {
        MethodType type = args.function.methodType();
        for (int i = start; i < args.arguments.length; i++) {
            emitPushArgument(type.parameterType(i), args.arguments[i]);
        }
    }

    private void emitPushArgument(Name name, int paramIndex) {
        Object arg = name.arguments[paramIndex];
        Class<?> ptype = name.function.methodType().parameterType(paramIndex);
        emitPushArgument(ptype, arg);
    }

    private void emitPushArgument(Class<?> ptype, Object arg) {
        BasicType bptype = basicType(ptype);
        if (arg instanceof Name n) {
            emitLoadInsn(n.type, n.index());
            emitImplicitConversion(n.type, ptype, n);
        } else if (arg == null && bptype == L_TYPE) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        } else if (arg instanceof String && bptype == L_TYPE) {
            mv.visitLdcInsn(arg);
        } else {
            if (Wrapper.isWrapperType(arg.getClass()) && bptype != L_TYPE) {
                emitConst(arg);
            } else {
                mv.visitFieldInsn(Opcodes.GETSTATIC, className, classData(arg), "Ljava/lang/Object;");
                emitImplicitConversion(L_TYPE, ptype, arg);
            }
        }
    }

    /**
     * Store the name to its local, if necessary.
     */
    private void emitStoreResult(Name name) {
        if (name != null && name.type != V_TYPE) {
            // non-void: actually assign
            emitStoreInsn(name.type, name.index());
        }
    }

    /**
     * Emits a return statement from a LF invoker. If required, the result type is cast to the correct return type.
     */
    private void emitReturn(Name onStack) {
        // return statement
        Class<?> rclass = invokerType.returnType();
        BasicType rtype = lambdaForm.returnType();
        assert(rtype == basicType(rclass));  // must agree
        if (rtype == V_TYPE) {
            // void
            mv.visitInsn(Opcodes.RETURN);
            // it doesn't matter what rclass is; the JVM will discard any value
        } else {
            LambdaForm.Name rn = lambdaForm.names[lambdaForm.result];

            // put return value on the stack if it is not already there
            if (rn != onStack) {
                emitLoadInsn(rtype, lambdaForm.result);
            }

            emitImplicitConversion(rtype, rclass, rn);

            // generate actual return statement
            emitReturnInsn(rtype);
        }
    }

    /**
     * Emit a type conversion bytecode casting from "from" to "to".
     */
    private void emitPrimCast(Wrapper from, Wrapper to) {
        // Here's how.
        // -   indicates forbidden
        // <-> indicates implicit
        //      to ----> boolean  byte     short    char     int      long     float    double
        // from boolean    <->        -        -        -        -        -        -        -
        //      byte        -       <->       i2s      i2c      <->      i2l      i2f      i2d
        //      short       -       i2b       <->      i2c      <->      i2l      i2f      i2d
        //      char        -       i2b       i2s      <->      <->      i2l      i2f      i2d
        //      int         -       i2b       i2s      i2c      <->      i2l      i2f      i2d
        //      long        -     l2i,i2b   l2i,i2s  l2i,i2c    l2i      <->      l2f      l2d
        //      float       -     f2i,i2b   f2i,i2s  f2i,i2c    f2i      f2l      <->      f2d
        //      double      -     d2i,i2b   d2i,i2s  d2i,i2c    d2i      d2l      d2f      <->
        if (from == to) {
            // no cast required, should be dead code anyway
            return;
        }
        if (from.isSubwordOrInt()) {
            // cast from {byte,short,char,int} to anything
            emitI2X(to);
        } else {
            // cast from {long,float,double} to anything
            if (to.isSubwordOrInt()) {
                // cast to {byte,short,char,int}
                emitX2I(from);
                if (to.bitWidth() < 32) {
                    // targets other than int require another conversion
                    emitI2X(to);
                }
            } else {
                // cast to {long,float,double} - this is verbose
                boolean error = false;
                switch (from) {
                    case LONG -> {
                        switch (to) {
                            case FLOAT  -> mv.visitInsn(Opcodes.L2F);
                            case DOUBLE -> mv.visitInsn(Opcodes.L2D);
                            default -> error = true;
                        }
                    }
                    case FLOAT -> {
                        switch (to) {
                            case LONG   -> mv.visitInsn(Opcodes.F2L);
                            case DOUBLE -> mv.visitInsn(Opcodes.F2D);
                            default -> error = true;
                        }
                    }
                    case DOUBLE -> {
                        switch (to) {
                            case LONG  -> mv.visitInsn(Opcodes.D2L);
                            case FLOAT -> mv.visitInsn(Opcodes.D2F);
                            default -> error = true;
                        }
                    }
                    default -> error = true;
                }
                if (error) {
                    throw new IllegalStateException("unhandled prim cast: " + from + "2" + to);
                }
            }
        }
    }

    private void emitI2X(Wrapper type) {
        switch (type) {
        case BYTE:    mv.visitInsn(Opcodes.I2B);  break;
        case SHORT:   mv.visitInsn(Opcodes.I2S);  break;
        case CHAR:    mv.visitInsn(Opcodes.I2C);  break;
        case INT:     /* naught */                break;
        case LONG:    mv.visitInsn(Opcodes.I2L);  break;
        case FLOAT:   mv.visitInsn(Opcodes.I2F);  break;
        case DOUBLE:  mv.visitInsn(Opcodes.I2D);  break;
        case BOOLEAN:
            // For compatibility with ValueConversions and explicitCastArguments:
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IAND);
            break;
        default:   throw new InternalError("unknown type: " + type);
        }
    }

    private void emitX2I(Wrapper type) {
        switch (type) {
            case LONG -> mv.visitInsn(Opcodes.L2I);
            case FLOAT -> mv.visitInsn(Opcodes.F2I);
            case DOUBLE -> mv.visitInsn(Opcodes.D2I);
            default -> throw new InternalError("unknown type: " + type);
        }
    }

    /**
     * Generate bytecode for a LambdaForm.vmentry which calls interpretWithArguments.
     */
    static MemberName generateLambdaFormInterpreterEntryPoint(MethodType mt) {
        assert(isValidSignature(basicTypeSignature(mt)));
        String name = "interpret_"+basicTypeChar(mt.returnType());
        MethodType type = mt;  // includes leading argument
        type = type.changeParameterType(0, MethodHandle.class);
        InvokerBytecodeGenerator g = new InvokerBytecodeGenerator("LFI", name, type);
        return g.loadMethod(g.generateLambdaFormInterpreterEntryPointBytes());
    }

    private byte[] generateLambdaFormInterpreterEntryPointBytes() {
        classFilePrologue();
        methodPrologue();

        // Suppress this method in backtraces displayed to the user.
        mv.visitAnnotation(HIDDEN_SIG, true);

        // Don't inline the interpreter entry.
        mv.visitAnnotation(DONTINLINE_SIG, true);

        // create parameter array
        emitIconstInsn(invokerType.parameterCount());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        // fill parameter array
        for (int i = 0; i < invokerType.parameterCount(); i++) {
            Class<?> ptype = invokerType.parameterType(i);
            mv.visitInsn(Opcodes.DUP);
            emitIconstInsn(i);
            emitLoadInsn(basicType(ptype), i);
            // box if primitive type
            if (ptype.isPrimitive()) {
                emitBoxing(Wrapper.forPrimitiveType(ptype));
            }
            mv.visitInsn(Opcodes.AASTORE);
        }
        // invoke
        emitAloadInsn(0);
        mv.visitFieldInsn(Opcodes.GETFIELD, MH, "form", "Ljava/lang/invoke/LambdaForm;");
        mv.visitInsn(Opcodes.SWAP);  // swap form and array; avoid local variable
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, LF, "interpretWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false);

        // maybe unbox
        Class<?> rtype = invokerType.returnType();
        if (rtype.isPrimitive() && rtype != void.class) {
            emitUnboxing(Wrapper.forPrimitiveType(rtype));
        }

        // return statement
        emitReturnInsn(basicType(rtype));

        methodEpilogue();
        clinit(cw, className, classData);
        bogusMethod(invokerType);

        final byte[] classFile = cw.toByteArray();
        maybeDump(classFile);
        return classFile;
    }

    /**
     * Generate bytecode for a NamedFunction invoker.
     */
    static MemberName generateNamedFunctionInvoker(MethodTypeForm typeForm) {
        MethodType invokerType = NamedFunction.INVOKER_METHOD_TYPE;
        String invokerName = "invoke_" + shortenSignature(basicTypeSignature(typeForm.erasedType()));
        InvokerBytecodeGenerator g = new InvokerBytecodeGenerator("NFI", invokerName, invokerType);
        return g.loadMethod(g.generateNamedFunctionInvokerImpl(typeForm));
    }

    private byte[] generateNamedFunctionInvokerImpl(MethodTypeForm typeForm) {
        MethodType dstType = typeForm.erasedType();
        classFilePrologue();
        methodPrologue();

        // Suppress this method in backtraces displayed to the user.
        mv.visitAnnotation(HIDDEN_SIG, true);

        // Force inlining of this invoker method.
        mv.visitAnnotation(FORCEINLINE_SIG, true);

        // Load receiver
        emitAloadInsn(0);

        // Load arguments from array
        for (int i = 0; i < dstType.parameterCount(); i++) {
            emitAloadInsn(1);
            emitIconstInsn(i);
            mv.visitInsn(Opcodes.AALOAD);

            // Maybe unbox
            Class<?> dptype = dstType.parameterType(i);
            if (dptype.isPrimitive()) {
                Wrapper dstWrapper = Wrapper.forBasicType(dptype);
                Wrapper srcWrapper = dstWrapper.isSubwordOrInt() ? Wrapper.INT : dstWrapper;  // narrow subword from int
                emitUnboxing(srcWrapper);
                emitPrimCast(srcWrapper, dstWrapper);
            }
        }

        // Invoke
        String targetDesc = dstType.basicType().toMethodDescriptorString();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MH, "invokeBasic", targetDesc, false);

        // Box primitive types
        Class<?> rtype = dstType.returnType();
        if (rtype != void.class && rtype.isPrimitive()) {
            Wrapper srcWrapper = Wrapper.forBasicType(rtype);
            Wrapper dstWrapper = srcWrapper.isSubwordOrInt() ? Wrapper.INT : srcWrapper;  // widen subword to int
            // boolean casts not allowed
            emitPrimCast(srcWrapper, dstWrapper);
            emitBoxing(dstWrapper);
        }

        // If the return type is void we return a null reference.
        if (rtype == void.class) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
        emitReturnInsn(L_TYPE);  // NOTE: NamedFunction invokers always return a reference value.

        methodEpilogue();
        clinit(cw, className, classData);
        bogusMethod(dstType);

        final byte[] classFile = cw.toByteArray();
        maybeDump(classFile);
        return classFile;
    }

    /**
     * Emit a bogus method that just loads some string constants. This is to get the constants into the constant pool
     * for debugging purposes.
     */
    private void bogusMethod(Object os) {
        if (DUMP_CLASS_FILES) {
            mv = cw.visitMethod(Opcodes.ACC_STATIC, "dummy", "()V", null, null);
            mv.visitLdcInsn(os.toString());
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
}
