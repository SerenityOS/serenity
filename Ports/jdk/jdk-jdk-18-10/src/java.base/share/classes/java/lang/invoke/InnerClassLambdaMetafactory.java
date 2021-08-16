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

import jdk.internal.misc.CDS;
import jdk.internal.misc.VM;
import jdk.internal.org.objectweb.asm.*;
import sun.invoke.util.BytecodeDescriptor;
import sun.invoke.util.VerifyAccess;
import sun.security.action.GetPropertyAction;
import sun.security.action.GetBooleanAction;

import java.io.FilePermission;
import java.io.Serializable;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.PropertyPermission;
import java.util.Set;

import static java.lang.invoke.MethodHandles.Lookup.ClassOption.NESTMATE;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.STRONG;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * Lambda metafactory implementation which dynamically creates an
 * inner-class-like class per lambda callsite.
 *
 * @see LambdaMetafactory
 */
/* package */ final class InnerClassLambdaMetafactory extends AbstractValidatingLambdaMetafactory {
    private static final int CLASSFILE_VERSION = VM.classFileVersion();
    private static final String METHOD_DESCRIPTOR_VOID = Type.getMethodDescriptor(Type.VOID_TYPE);
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final String NAME_CTOR = "<init>";
    private static final String LAMBDA_INSTANCE_FIELD = "LAMBDA_INSTANCE$";

    //Serialization support
    private static final String NAME_SERIALIZED_LAMBDA = "java/lang/invoke/SerializedLambda";
    private static final String NAME_NOT_SERIALIZABLE_EXCEPTION = "java/io/NotSerializableException";
    private static final String DESCR_METHOD_WRITE_REPLACE = "()Ljava/lang/Object;";
    private static final String DESCR_METHOD_WRITE_OBJECT = "(Ljava/io/ObjectOutputStream;)V";
    private static final String DESCR_METHOD_READ_OBJECT = "(Ljava/io/ObjectInputStream;)V";

    private static final String NAME_METHOD_WRITE_REPLACE = "writeReplace";
    private static final String NAME_METHOD_READ_OBJECT = "readObject";
    private static final String NAME_METHOD_WRITE_OBJECT = "writeObject";

    private static final String DESCR_CLASS = "Ljava/lang/Class;";
    private static final String DESCR_STRING = "Ljava/lang/String;";
    private static final String DESCR_OBJECT = "Ljava/lang/Object;";
    private static final String DESCR_CTOR_SERIALIZED_LAMBDA
            = "(" + DESCR_CLASS + DESCR_STRING + DESCR_STRING + DESCR_STRING + "I"
            + DESCR_STRING + DESCR_STRING + DESCR_STRING + DESCR_STRING + "[" + DESCR_OBJECT + ")V";

    private static final String DESCR_CTOR_NOT_SERIALIZABLE_EXCEPTION = "(Ljava/lang/String;)V";
    private static final String[] SER_HOSTILE_EXCEPTIONS = new String[] {NAME_NOT_SERIALIZABLE_EXCEPTION};

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    // Used to ensure that each spun class name is unique
    private static final AtomicInteger counter = new AtomicInteger();

    // For dumping generated classes to disk, for debugging purposes
    private static final ProxyClassesDumper dumper;

    private static final boolean disableEagerInitialization;

    // condy to load implMethod from class data
    private static final ConstantDynamic implMethodCondy;

    static {
        final String dumpProxyClassesKey = "jdk.internal.lambda.dumpProxyClasses";
        String dumpPath = GetPropertyAction.privilegedGetProperty(dumpProxyClassesKey);
        dumper = (null == dumpPath) ? null : ProxyClassesDumper.getInstance(dumpPath);

        final String disableEagerInitializationKey = "jdk.internal.lambda.disableEagerInitialization";
        disableEagerInitialization = GetBooleanAction.privilegedGetProperty(disableEagerInitializationKey);

        // condy to load implMethod from class data
        MethodType classDataMType = MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class);
        Handle classDataBsm = new Handle(H_INVOKESTATIC, Type.getInternalName(MethodHandles.class), "classData",
                                         classDataMType.descriptorString(), false);
        implMethodCondy = new ConstantDynamic(ConstantDescs.DEFAULT_NAME, MethodHandle.class.descriptorString(), classDataBsm);
    }

    // See context values in AbstractValidatingLambdaMetafactory
    private final String implMethodClassName;        // Name of type containing implementation "CC"
    private final String implMethodName;             // Name of implementation method "impl"
    private final String implMethodDesc;             // Type descriptor for implementation methods "(I)Ljava/lang/String;"
    private final MethodType constructorType;        // Generated class constructor type "(CC)void"
    private final ClassWriter cw;                    // ASM class writer
    private final String[] argNames;                 // Generated names for the constructor arguments
    private final String[] argDescs;                 // Type descriptors for the constructor arguments
    private final String lambdaClassName;            // Generated name for the generated class "X$$Lambda$1"
    private final boolean useImplMethodHandle;       // use MethodHandle invocation instead of symbolic bytecode invocation

    /**
     * General meta-factory constructor, supporting both standard cases and
     * allowing for uncommon options such as serialization or bridging.
     *
     * @param caller Stacked automatically by VM; represents a lookup context
     *               with the accessibility privileges of the caller.
     * @param factoryType Stacked automatically by VM; the signature of the
     *                    invoked method, which includes the expected static
     *                    type of the returned lambda object, and the static
     *                    types of the captured arguments for the lambda.  In
     *                    the event that the implementation method is an
     *                    instance method, the first argument in the invocation
     *                    signature will correspond to the receiver.
     * @param interfaceMethodName Name of the method in the functional interface to
     *                   which the lambda or method reference is being
     *                   converted, represented as a String.
     * @param interfaceMethodType Type of the method in the functional interface to
     *                            which the lambda or method reference is being
     *                            converted, represented as a MethodType.
     * @param implementation The implementation method which should be called (with
     *                       suitable adaptation of argument types, return types,
     *                       and adjustment for captured arguments) when methods of
     *                       the resulting functional interface instance are invoked.
     * @param dynamicMethodType The signature of the primary functional
     *                          interface method after type variables are
     *                          substituted with their instantiation from
     *                          the capture site
     * @param isSerializable Should the lambda be made serializable?  If set,
     *                       either the target type or one of the additional SAM
     *                       types must extend {@code Serializable}.
     * @param altInterfaces Additional interfaces which the lambda object
     *                      should implement.
     * @param altMethods Method types for additional signatures to be
     *                   implemented by invoking the implementation method
     * @throws LambdaConversionException If any of the meta-factory protocol
     *         invariants are violated
     * @throws SecurityException If a security manager is present, and it
     *         <a href="MethodHandles.Lookup.html#secmgr">denies access</a>
     *         from {@code caller} to the package of {@code implementation}.
     */
    public InnerClassLambdaMetafactory(MethodHandles.Lookup caller,
                                       MethodType factoryType,
                                       String interfaceMethodName,
                                       MethodType interfaceMethodType,
                                       MethodHandle implementation,
                                       MethodType dynamicMethodType,
                                       boolean isSerializable,
                                       Class<?>[] altInterfaces,
                                       MethodType[] altMethods)
            throws LambdaConversionException {
        super(caller, factoryType, interfaceMethodName, interfaceMethodType,
              implementation, dynamicMethodType,
              isSerializable, altInterfaces, altMethods);
        implMethodClassName = implClass.getName().replace('.', '/');
        implMethodName = implInfo.getName();
        implMethodDesc = implInfo.getMethodType().toMethodDescriptorString();
        constructorType = factoryType.changeReturnType(Void.TYPE);
        lambdaClassName = lambdaClassName(targetClass);
        // If the target class invokes a protected method inherited from a
        // superclass in a different package, or does 'invokespecial', the
        // lambda class has no access to the resolved method. Instead, we need
        // to pass the live implementation method handle to the proxy class
        // to invoke directly. (javac prefers to avoid this situation by
        // generating bridges in the target class)
        useImplMethodHandle = (Modifier.isProtected(implInfo.getModifiers()) &&
                               !VerifyAccess.isSamePackage(targetClass, implInfo.getDeclaringClass())) ||
                               implKind == H_INVOKESPECIAL;
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int parameterCount = factoryType.parameterCount();
        if (parameterCount > 0) {
            argNames = new String[parameterCount];
            argDescs = new String[parameterCount];
            for (int i = 0; i < parameterCount; i++) {
                argNames[i] = "arg$" + (i + 1);
                argDescs[i] = BytecodeDescriptor.unparse(factoryType.parameterType(i));
            }
        } else {
            argNames = argDescs = EMPTY_STRING_ARRAY;
        }
    }

    private static String lambdaClassName(Class<?> targetClass) {
        String name = targetClass.getName();
        if (targetClass.isHidden()) {
            // use the original class name
            name = name.replace('/', '_');
        }
        return name.replace('.', '/') + "$$Lambda$" + counter.incrementAndGet();
    }

    /**
     * Build the CallSite. Generate a class file which implements the functional
     * interface, define the class, if there are no parameters create an instance
     * of the class which the CallSite will return, otherwise, generate handles
     * which will call the class' constructor.
     *
     * @return a CallSite, which, when invoked, will return an instance of the
     * functional interface
     * @throws LambdaConversionException If properly formed functional interface
     * is not found
     */
    @Override
    CallSite buildCallSite() throws LambdaConversionException {
        final Class<?> innerClass = spinInnerClass();
        if (factoryType.parameterCount() == 0) {
            // In the case of a non-capturing lambda, we optimize linkage by pre-computing a single instance,
            // unless we've suppressed eager initialization
            if (disableEagerInitialization) {
                try {
                    return new ConstantCallSite(caller.findStaticGetter(innerClass, LAMBDA_INSTANCE_FIELD,
                            factoryType.returnType()));
                } catch (ReflectiveOperationException e) {
                    throw new LambdaConversionException(
                            "Exception finding " +  LAMBDA_INSTANCE_FIELD + " static field", e);
                }
            } else {
                @SuppressWarnings("removal")
                final Constructor<?>[] ctrs = AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            @Override
                            public Constructor<?>[] run() {
                                Constructor<?>[] ctrs = innerClass.getDeclaredConstructors();
                                if (ctrs.length == 1) {
                                    // The lambda implementing inner class constructor is private, set
                                    // it accessible (by us) before creating the constant sole instance
                                    ctrs[0].setAccessible(true);
                                }
                                return ctrs;
                            }
                        });
                if (ctrs.length != 1) {
                    throw new LambdaConversionException("Expected one lambda constructor for "
                            + innerClass.getCanonicalName() + ", got " + ctrs.length);
                }

                try {
                    Object inst = ctrs[0].newInstance();
                    return new ConstantCallSite(MethodHandles.constant(interfaceClass, inst));
                } catch (ReflectiveOperationException e) {
                    throw new LambdaConversionException("Exception instantiating lambda object", e);
                }
            }
        } else {
            try {
                MethodHandle mh = caller.findConstructor(innerClass, constructorType);
                return new ConstantCallSite(mh.asType(factoryType));
            } catch (ReflectiveOperationException e) {
                throw new LambdaConversionException("Exception finding constructor", e);
            }
        }
    }

    /**
     * Spins the lambda proxy class.
     *
     * This first checks if a lambda proxy class can be loaded from CDS archive.
     * Otherwise, generate the lambda proxy class. If CDS dumping is enabled, it
     * registers the lambda proxy class for including into the CDS archive.
     */
    private Class<?> spinInnerClass() throws LambdaConversionException {
        // CDS does not handle disableEagerInitialization.
        if (!disableEagerInitialization) {
            // include lambda proxy class in CDS archive at dump time
            if (CDS.isDumpingArchive()) {
                Class<?> innerClass = generateInnerClass();
                LambdaProxyClassArchive.register(targetClass,
                                                 interfaceMethodName,
                                                 factoryType,
                                                 interfaceMethodType,
                                                 implementation,
                                                 dynamicMethodType,
                                                 isSerializable,
                                                 altInterfaces,
                                                 altMethods,
                                                 innerClass);
                return innerClass;
            }

            // load from CDS archive if present
            Class<?> innerClass = LambdaProxyClassArchive.find(targetClass,
                                                               interfaceMethodName,
                                                               factoryType,
                                                               interfaceMethodType,
                                                               implementation,
                                                               dynamicMethodType,
                                                               isSerializable,
                                                               altInterfaces,
                                                               altMethods);
            if (innerClass != null) return innerClass;
        }
        return generateInnerClass();
    }

    /**
     * Generate a class file which implements the functional
     * interface, define and return the class.
     *
     * @return a Class which implements the functional interface
     * @throws LambdaConversionException If properly formed functional interface
     * is not found
     */
    @SuppressWarnings("removal")
    private Class<?> generateInnerClass() throws LambdaConversionException {
        String[] interfaceNames;
        String interfaceName = interfaceClass.getName().replace('.', '/');
        boolean accidentallySerializable = !isSerializable && Serializable.class.isAssignableFrom(interfaceClass);
        if (altInterfaces.length == 0) {
            interfaceNames = new String[]{interfaceName};
        } else {
            // Assure no duplicate interfaces (ClassFormatError)
            Set<String> itfs = new LinkedHashSet<>(altInterfaces.length + 1);
            itfs.add(interfaceName);
            for (Class<?> i : altInterfaces) {
                itfs.add(i.getName().replace('.', '/'));
                accidentallySerializable |= !isSerializable && Serializable.class.isAssignableFrom(i);
            }
            interfaceNames = itfs.toArray(new String[itfs.size()]);
        }

        cw.visit(CLASSFILE_VERSION, ACC_SUPER + ACC_FINAL + ACC_SYNTHETIC,
                 lambdaClassName, null,
                 JAVA_LANG_OBJECT, interfaceNames);

        // Generate final fields to be filled in by constructor
        for (int i = 0; i < argDescs.length; i++) {
            FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL,
                                            argNames[i],
                                            argDescs[i],
                                            null, null);
            fv.visitEnd();
        }

        generateConstructor();

        if (factoryType.parameterCount() == 0 && disableEagerInitialization) {
            generateClassInitializer();
        }

        // Forward the SAM method
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, interfaceMethodName,
                                          interfaceMethodType.toMethodDescriptorString(), null, null);
        new ForwardingMethodGenerator(mv).generate(interfaceMethodType);

        // Forward the altMethods
        if (altMethods != null) {
            for (MethodType mt : altMethods) {
                mv = cw.visitMethod(ACC_PUBLIC, interfaceMethodName,
                                    mt.toMethodDescriptorString(), null, null);
                new ForwardingMethodGenerator(mv).generate(mt);
            }
        }

        if (isSerializable)
            generateSerializationFriendlyMethods();
        else if (accidentallySerializable)
            generateSerializationHostileMethods();

        cw.visitEnd();

        // Define the generated class in this VM.

        final byte[] classBytes = cw.toByteArray();
        // If requested, dump out to a file for debugging purposes
        if (dumper != null) {
            AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Void run() {
                    dumper.dumpClass(lambdaClassName, classBytes);
                    return null;
                }
            }, null,
            new FilePermission("<<ALL FILES>>", "read, write"),
            // createDirectories may need it
            new PropertyPermission("user.dir", "read"));
        }
        try {
            // this class is linked at the indy callsite; so define a hidden nestmate
            Lookup lookup;
            if (useImplMethodHandle) {
                lookup = caller.defineHiddenClassWithClassData(classBytes, implementation, !disableEagerInitialization,
                                                               NESTMATE, STRONG);
            } else {
                lookup = caller.defineHiddenClass(classBytes, !disableEagerInitialization, NESTMATE, STRONG);
            }
            return lookup.lookupClass();
        } catch (IllegalAccessException e) {
            throw new LambdaConversionException("Exception defining lambda proxy class", e);
        } catch (Throwable t) {
            throw new InternalError(t);
        }
    }

    /**
     * Generate a static field and a static initializer that sets this field to an instance of the lambda
     */
    private void generateClassInitializer() {
        String lambdaTypeDescriptor = factoryType.returnType().descriptorString();

        // Generate the static final field that holds the lambda singleton
        FieldVisitor fv = cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                LAMBDA_INSTANCE_FIELD, lambdaTypeDescriptor, null, null);
        fv.visitEnd();

        // Instantiate the lambda and store it to the static final field
        MethodVisitor clinit = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();

        clinit.visitTypeInsn(NEW, lambdaClassName);
        clinit.visitInsn(Opcodes.DUP);
        assert factoryType.parameterCount() == 0;
        clinit.visitMethodInsn(INVOKESPECIAL, lambdaClassName, NAME_CTOR, constructorType.toMethodDescriptorString(), false);
        clinit.visitFieldInsn(PUTSTATIC, lambdaClassName, LAMBDA_INSTANCE_FIELD, lambdaTypeDescriptor);

        clinit.visitInsn(RETURN);
        clinit.visitMaxs(-1, -1);
        clinit.visitEnd();
    }

    /**
     * Generate the constructor for the class
     */
    private void generateConstructor() {
        // Generate constructor
        MethodVisitor ctor = cw.visitMethod(ACC_PRIVATE, NAME_CTOR,
                                            constructorType.toMethodDescriptorString(), null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, NAME_CTOR,
                             METHOD_DESCRIPTOR_VOID, false);
        int parameterCount = factoryType.parameterCount();
        for (int i = 0, lvIndex = 0; i < parameterCount; i++) {
            ctor.visitVarInsn(ALOAD, 0);
            Class<?> argType = factoryType.parameterType(i);
            ctor.visitVarInsn(getLoadOpcode(argType), lvIndex + 1);
            lvIndex += getParameterSize(argType);
            ctor.visitFieldInsn(PUTFIELD, lambdaClassName, argNames[i], argDescs[i]);
        }
        ctor.visitInsn(RETURN);
        // Maxs computed by ClassWriter.COMPUTE_MAXS, these arguments ignored
        ctor.visitMaxs(-1, -1);
        ctor.visitEnd();
    }

    /**
     * Generate a writeReplace method that supports serialization
     */
    private void generateSerializationFriendlyMethods() {
        TypeConvertingMethodAdapter mv
                = new TypeConvertingMethodAdapter(
                    cw.visitMethod(ACC_PRIVATE + ACC_FINAL,
                    NAME_METHOD_WRITE_REPLACE, DESCR_METHOD_WRITE_REPLACE,
                    null, null));

        mv.visitCode();
        mv.visitTypeInsn(NEW, NAME_SERIALIZED_LAMBDA);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(Type.getType(targetClass));
        mv.visitLdcInsn(factoryType.returnType().getName().replace('.', '/'));
        mv.visitLdcInsn(interfaceMethodName);
        mv.visitLdcInsn(interfaceMethodType.toMethodDescriptorString());
        mv.visitLdcInsn(implInfo.getReferenceKind());
        mv.visitLdcInsn(implInfo.getDeclaringClass().getName().replace('.', '/'));
        mv.visitLdcInsn(implInfo.getName());
        mv.visitLdcInsn(implInfo.getMethodType().toMethodDescriptorString());
        mv.visitLdcInsn(dynamicMethodType.toMethodDescriptorString());
        mv.iconst(argDescs.length);
        mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_OBJECT);
        for (int i = 0; i < argDescs.length; i++) {
            mv.visitInsn(DUP);
            mv.iconst(i);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, lambdaClassName, argNames[i], argDescs[i]);
            mv.boxIfTypePrimitive(Type.getType(argDescs[i]));
            mv.visitInsn(AASTORE);
        }
        mv.visitMethodInsn(INVOKESPECIAL, NAME_SERIALIZED_LAMBDA, NAME_CTOR,
                DESCR_CTOR_SERIALIZED_LAMBDA, false);
        mv.visitInsn(ARETURN);
        // Maxs computed by ClassWriter.COMPUTE_MAXS, these arguments ignored
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    /**
     * Generate a readObject/writeObject method that is hostile to serialization
     */
    private void generateSerializationHostileMethods() {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE + ACC_FINAL,
                                          NAME_METHOD_WRITE_OBJECT, DESCR_METHOD_WRITE_OBJECT,
                                          null, SER_HOSTILE_EXCEPTIONS);
        mv.visitCode();
        mv.visitTypeInsn(NEW, NAME_NOT_SERIALIZABLE_EXCEPTION);
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Non-serializable lambda");
        mv.visitMethodInsn(INVOKESPECIAL, NAME_NOT_SERIALIZABLE_EXCEPTION, NAME_CTOR,
                           DESCR_CTOR_NOT_SERIALIZABLE_EXCEPTION, false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PRIVATE + ACC_FINAL,
                            NAME_METHOD_READ_OBJECT, DESCR_METHOD_READ_OBJECT,
                            null, SER_HOSTILE_EXCEPTIONS);
        mv.visitCode();
        mv.visitTypeInsn(NEW, NAME_NOT_SERIALIZABLE_EXCEPTION);
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Non-serializable lambda");
        mv.visitMethodInsn(INVOKESPECIAL, NAME_NOT_SERIALIZABLE_EXCEPTION, NAME_CTOR,
                           DESCR_CTOR_NOT_SERIALIZABLE_EXCEPTION, false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    /**
     * This class generates a method body which calls the lambda implementation
     * method, converting arguments, as needed.
     */
    private class ForwardingMethodGenerator extends TypeConvertingMethodAdapter {

        ForwardingMethodGenerator(MethodVisitor mv) {
            super(mv);
        }

        void generate(MethodType methodType) {
            visitCode();

            if (implKind == MethodHandleInfo.REF_newInvokeSpecial) {
                visitTypeInsn(NEW, implMethodClassName);
                visitInsn(DUP);
            }
            if (useImplMethodHandle) {
                visitLdcInsn(implMethodCondy);
            }
            for (int i = 0; i < argNames.length; i++) {
                visitVarInsn(ALOAD, 0);
                visitFieldInsn(GETFIELD, lambdaClassName, argNames[i], argDescs[i]);
            }

            convertArgumentTypes(methodType);

            if (useImplMethodHandle) {
                MethodType mtype = implInfo.getMethodType();
                if (implKind != MethodHandleInfo.REF_invokeStatic) {
                    mtype = mtype.insertParameterTypes(0, implClass);
                }
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
                                "invokeExact", mtype.descriptorString(), false);
            } else {
                // Invoke the method we want to forward to
                visitMethodInsn(invocationOpcode(), implMethodClassName,
                                implMethodName, implMethodDesc,
                                implClass.isInterface());
            }
            // Convert the return value (if any) and return it
            // Note: if adapting from non-void to void, the 'return'
            // instruction will pop the unneeded result
            Class<?> implReturnClass = implMethodType.returnType();
            Class<?> samReturnClass = methodType.returnType();
            convertType(implReturnClass, samReturnClass, samReturnClass);
            visitInsn(getReturnOpcode(samReturnClass));
            // Maxs computed by ClassWriter.COMPUTE_MAXS,these arguments ignored
            visitMaxs(-1, -1);
            visitEnd();
        }

        private void convertArgumentTypes(MethodType samType) {
            int lvIndex = 0;
            int samParametersLength = samType.parameterCount();
            int captureArity = factoryType.parameterCount();
            for (int i = 0; i < samParametersLength; i++) {
                Class<?> argType = samType.parameterType(i);
                visitVarInsn(getLoadOpcode(argType), lvIndex + 1);
                lvIndex += getParameterSize(argType);
                convertType(argType, implMethodType.parameterType(captureArity + i), dynamicMethodType.parameterType(i));
            }
        }

        private int invocationOpcode() throws InternalError {
            return switch (implKind) {
                case MethodHandleInfo.REF_invokeStatic     -> INVOKESTATIC;
                case MethodHandleInfo.REF_newInvokeSpecial -> INVOKESPECIAL;
                case MethodHandleInfo.REF_invokeVirtual    -> INVOKEVIRTUAL;
                case MethodHandleInfo.REF_invokeInterface  -> INVOKEINTERFACE;
                case MethodHandleInfo.REF_invokeSpecial    -> INVOKESPECIAL;
                default -> throw new InternalError("Unexpected invocation kind: " + implKind);
            };
        }
    }

    static int getParameterSize(Class<?> c) {
        if (c == Void.TYPE) {
            return 0;
        } else if (c == Long.TYPE || c == Double.TYPE) {
            return 2;
        }
        return 1;
    }

    static int getLoadOpcode(Class<?> c) {
        if(c == Void.TYPE) {
            throw new InternalError("Unexpected void type of load opcode");
        }
        return ILOAD + getOpcodeOffset(c);
    }

    static int getReturnOpcode(Class<?> c) {
        if(c == Void.TYPE) {
            return RETURN;
        }
        return IRETURN + getOpcodeOffset(c);
    }

    private static int getOpcodeOffset(Class<?> c) {
        if (c.isPrimitive()) {
            if (c == Long.TYPE) {
                return 1;
            } else if (c == Float.TYPE) {
                return 2;
            } else if (c == Double.TYPE) {
                return 3;
            }
            return 0;
        } else {
            return 4;
        }
    }

}
