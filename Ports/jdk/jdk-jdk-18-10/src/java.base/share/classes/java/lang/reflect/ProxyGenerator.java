/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

import jdk.internal.misc.VM;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import sun.security.action.GetBooleanAction;

import java.io.IOException;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * ProxyGenerator contains the code to generate a dynamic proxy class
 * for the java.lang.reflect.Proxy API.
 * <p>
 * The external interface to ProxyGenerator is the static
 * "generateProxyClass" method.
 */
final class ProxyGenerator extends ClassWriter {
    private static final int CLASSFILE_VERSION = VM.classFileVersion();
    private static final String JL_CLASS = "java/lang/Class";
    private static final String JL_OBJECT = "java/lang/Object";
    private static final String JL_THROWABLE = "java/lang/Throwable";
    private static final String JL_CLASS_NOT_FOUND_EX = "java/lang/ClassNotFoundException";
    private static final String JL_ILLEGAL_ACCESS_EX = "java/lang/IllegalAccessException";

    private static final String JL_NO_CLASS_DEF_FOUND_ERROR = "java/lang/NoClassDefFoundError";
    private static final String JL_NO_SUCH_METHOD_EX = "java/lang/NoSuchMethodException";
    private static final String JL_NO_SUCH_METHOD_ERROR = "java/lang/NoSuchMethodError";
    private static final String JLI_LOOKUP = "java/lang/invoke/MethodHandles$Lookup";
    private static final String JLI_METHODHANDLES = "java/lang/invoke/MethodHandles";

    private static final String JLR_INVOCATION_HANDLER = "java/lang/reflect/InvocationHandler";
    private static final String JLR_PROXY = "java/lang/reflect/Proxy";
    private static final String JLR_UNDECLARED_THROWABLE_EX = "java/lang/reflect/UndeclaredThrowableException";

    private static final String LJL_CLASS = "Ljava/lang/Class;";
    private static final String LJLR_METHOD = "Ljava/lang/reflect/Method;";
    private static final String LJLR_INVOCATION_HANDLER = "Ljava/lang/reflect/InvocationHandler;";

    private static final String MJLR_INVOCATIONHANDLER = "(Ljava/lang/reflect/InvocationHandler;)V";

    private static final String NAME_CTOR = "<init>";
    private static final String NAME_CLINIT = "<clinit>";
    private static final String NAME_LOOKUP_ACCESSOR = "proxyClassLookup";

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    /**
     * name of field for storing a proxy instance's invocation handler
     */
    private static final String handlerFieldName = "h";

    /**
     * debugging flag for saving generated class files
     */
    @SuppressWarnings("removal")
    private static final boolean saveGeneratedFiles =
            java.security.AccessController.doPrivileged(
                    new GetBooleanAction(
                            "jdk.proxy.ProxyGenerator.saveGeneratedFiles"));

    /* Preloaded ProxyMethod objects for methods in java.lang.Object */
    private static final ProxyMethod hashCodeMethod;
    private static final ProxyMethod equalsMethod;
    private static final ProxyMethod toStringMethod;

    static {
        try {
            hashCodeMethod = new ProxyMethod(Object.class.getMethod("hashCode"), "m0");
            equalsMethod = new ProxyMethod(Object.class.getMethod("equals", Object.class), "m1");
            toStringMethod = new ProxyMethod(Object.class.getMethod("toString"), "m2");
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    /**
     * Class loader
     */
    private final ClassLoader loader;

    /**
     * Name of proxy class
     */
    private final String className;

    /**
     * Proxy interfaces
     */
    private final List<Class<?>> interfaces;

    /**
     * Proxy class access flags
     */
    private final int accessFlags;

    /**
     * Maps method signature string to list of ProxyMethod objects for
     * proxy methods with that signature.
     * Kept in insertion order to make it easier to compare old and new.
     */
    private final Map<String, List<ProxyMethod>> proxyMethods = new LinkedHashMap<>();

    /**
     * Ordinal of next ProxyMethod object added to proxyMethods.
     * Indexes are reserved for hashcode(0), equals(1), toString(2).
     */
    private int proxyMethodCount = 3;

    /**
     * Construct a ProxyGenerator to generate a proxy class with the
     * specified name and for the given interfaces.
     * <p>
     * A ProxyGenerator object contains the state for the ongoing
     * generation of a particular proxy class.
     */
    private ProxyGenerator(ClassLoader loader, String className, List<Class<?>> interfaces,
                           int accessFlags) {
        super(ClassWriter.COMPUTE_FRAMES);
        this.loader = loader;
        this.className = className;
        this.interfaces = interfaces;
        this.accessFlags = accessFlags;
    }

    /**
     * Generate a proxy class given a name and a list of proxy interfaces.
     *
     * @param name        the class name of the proxy class
     * @param interfaces  proxy interfaces
     * @param accessFlags access flags of the proxy class
     */
    @SuppressWarnings("removal")
    static byte[] generateProxyClass(ClassLoader loader,
                                     final String name,
                                     List<Class<?>> interfaces,
                                     int accessFlags) {
        ProxyGenerator gen = new ProxyGenerator(loader, name, interfaces, accessFlags);
        final byte[] classFile = gen.generateClassFile();

        if (saveGeneratedFiles) {
            java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Void>() {
                        public Void run() {
                            try {
                                int i = name.lastIndexOf('.');
                                Path path;
                                if (i > 0) {
                                    Path dir = Path.of(dotToSlash(name.substring(0, i)));
                                    Files.createDirectories(dir);
                                    path = dir.resolve(name.substring(i + 1) + ".class");
                                } else {
                                    path = Path.of(name + ".class");
                                }
                                Files.write(path, classFile);
                                return null;
                            } catch (IOException e) {
                                throw new InternalError(
                                        "I/O exception saving generated file: " + e);
                            }
                        }
                    });
        }

        return classFile;
    }

    /**
     * Return an array of the class and interface names from an array of Classes.
     *
     * @param classes an array of classes or interfaces
     * @return the array of class and interface names; or null if classes is
     * null or empty
     */
    private static String[] typeNames(List<Class<?>> classes) {
        if (classes == null || classes.size() == 0)
            return null;
        int size = classes.size();
        String[] ifaces = new String[size];
        for (int i = 0; i < size; i++)
            ifaces[i] = dotToSlash(classes.get(i).getName());
        return ifaces;
    }

    /**
     * For a given set of proxy methods with the same signature, check
     * that their return types are compatible according to the Proxy
     * specification.
     *
     * Specifically, if there is more than one such method, then all
     * of the return types must be reference types, and there must be
     * one return type that is assignable to each of the rest of them.
     */
    private static void checkReturnTypes(List<ProxyMethod> methods) {
        /*
         * If there is only one method with a given signature, there
         * cannot be a conflict.  This is the only case in which a
         * primitive (or void) return type is allowed.
         */
        if (methods.size() < 2) {
            return;
        }

        /*
         * List of return types that are not yet known to be
         * assignable from ("covered" by) any of the others.
         */
        List<Class<?>> uncoveredReturnTypes = new ArrayList<>(1);

        nextNewReturnType:
        for (ProxyMethod pm : methods) {
            Class<?> newReturnType = pm.returnType;
            if (newReturnType.isPrimitive()) {
                throw new IllegalArgumentException(
                        "methods with same signature " +
                                pm.shortSignature +
                                " but incompatible return types: " +
                                newReturnType.getName() + " and others");
            }
            boolean added = false;

            /*
             * Compare the new return type to the existing uncovered
             * return types.
             */
            ListIterator<Class<?>> liter = uncoveredReturnTypes.listIterator();
            while (liter.hasNext()) {
                Class<?> uncoveredReturnType = liter.next();

                /*
                 * If an existing uncovered return type is assignable
                 * to this new one, then we can forget the new one.
                 */
                if (newReturnType.isAssignableFrom(uncoveredReturnType)) {
                    assert !added;
                    continue nextNewReturnType;
                }

                /*
                 * If the new return type is assignable to an existing
                 * uncovered one, then should replace the existing one
                 * with the new one (or just forget the existing one,
                 * if the new one has already be put in the list).
                 */
                if (uncoveredReturnType.isAssignableFrom(newReturnType)) {
                    // (we can assume that each return type is unique)
                    if (!added) {
                        liter.set(newReturnType);
                        added = true;
                    } else {
                        liter.remove();
                    }
                }
            }

            /*
             * If we got through the list of existing uncovered return
             * types without an assignability relationship, then add
             * the new return type to the list of uncovered ones.
             */
            if (!added) {
                uncoveredReturnTypes.add(newReturnType);
            }
        }

        /*
         * We shouldn't end up with more than one return type that is
         * not assignable from any of the others.
         */
        if (uncoveredReturnTypes.size() > 1) {
            ProxyMethod pm = methods.get(0);
            throw new IllegalArgumentException(
                    "methods with same signature " +
                            pm.shortSignature +
                            " but incompatible return types: " + uncoveredReturnTypes);
        }
    }

    /**
     * Given the exceptions declared in the throws clause of a proxy method,
     * compute the exceptions that need to be caught from the invocation
     * handler's invoke method and rethrown intact in the method's
     * implementation before catching other Throwables and wrapping them
     * in UndeclaredThrowableExceptions.
     *
     * The exceptions to be caught are returned in a List object.  Each
     * exception in the returned list is guaranteed to not be a subclass of
     * any of the other exceptions in the list, so the catch blocks for
     * these exceptions may be generated in any order relative to each other.
     *
     * Error and RuntimeException are each always contained by the returned
     * list (if none of their superclasses are contained), since those
     * unchecked exceptions should always be rethrown intact, and thus their
     * subclasses will never appear in the returned list.
     *
     * The returned List will be empty if java.lang.Throwable is in the
     * given list of declared exceptions, indicating that no exceptions
     * need to be caught.
     */
    private static List<Class<?>> computeUniqueCatchList(Class<?>[] exceptions) {
        List<Class<?>> uniqueList = new ArrayList<>();
        // unique exceptions to catch

        uniqueList.add(Error.class);            // always catch/rethrow these
        uniqueList.add(RuntimeException.class);

        nextException:
        for (Class<?> ex : exceptions) {
            if (ex.isAssignableFrom(Throwable.class)) {
                /*
                 * If Throwable is declared to be thrown by the proxy method,
                 * then no catch blocks are necessary, because the invoke
                 * can, at most, throw Throwable anyway.
                 */
                uniqueList.clear();
                break;
            } else if (!Throwable.class.isAssignableFrom(ex)) {
                /*
                 * Ignore types that cannot be thrown by the invoke method.
                 */
                continue;
            }
            /*
             * Compare this exception against the current list of
             * exceptions that need to be caught:
             */
            for (int j = 0; j < uniqueList.size(); ) {
                Class<?> ex2 = uniqueList.get(j);
                if (ex2.isAssignableFrom(ex)) {
                    /*
                     * if a superclass of this exception is already on
                     * the list to catch, then ignore this one and continue;
                     */
                    continue nextException;
                } else if (ex.isAssignableFrom(ex2)) {
                    /*
                     * if a subclass of this exception is on the list
                     * to catch, then remove it;
                     */
                    uniqueList.remove(j);
                } else {
                    j++;        // else continue comparing.
                }
            }
            // This exception is unique (so far): add it to the list to catch.
            uniqueList.add(ex);
        }
        return uniqueList;
    }

    /**
     * Convert a fully qualified class name that uses '.' as the package
     * separator, the external representation used by the Java language
     * and APIs, to a fully qualified class name that uses '/' as the
     * package separator, the representation used in the class file
     * format (see JVMS section {@jvms 4.2}).
     */
    private static String dotToSlash(String name) {
        return name.replace('.', '/');
    }

    /**
     * Return the number of abstract "words", or consecutive local variable
     * indexes, required to contain a value of the given type.  See JVMS
     * section {@jvms 3.6.1}.
     * <p>
     * Note that the original version of the JVMS contained a definition of
     * this abstract notion of a "word" in section 3.4, but that definition
     * was removed for the second edition.
     */
    private static int getWordsPerType(Class<?> type) {
        if (type == long.class || type == double.class) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * Add to the given list all of the types in the "from" array that
     * are not already contained in the list and are assignable to at
     * least one of the types in the "with" array.
     * <p>
     * This method is useful for computing the greatest common set of
     * declared exceptions from duplicate methods inherited from
     * different interfaces.
     */
    private static void collectCompatibleTypes(Class<?>[] from,
                                               Class<?>[] with,
                                               List<Class<?>> list) {
        for (Class<?> fc : from) {
            if (!list.contains(fc)) {
                for (Class<?> wc : with) {
                    if (wc.isAssignableFrom(fc)) {
                        list.add(fc);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns the {@link ClassLoader} to be used by the default implementation of {@link
     * #getCommonSuperClass(String, String)}, that of this {@link ClassWriter}'s runtime type by
     * default.
     *
     * @return ClassLoader
     */
    protected ClassLoader getClassLoader() {
        return loader;
    }

    /**
     * Generate a class file for the proxy class.  This method drives the
     * class file generation process.
     */
    private byte[] generateClassFile() {
        visit(CLASSFILE_VERSION, accessFlags, dotToSlash(className), null,
                JLR_PROXY, typeNames(interfaces));

        /*
         * Add proxy methods for the hashCode, equals,
         * and toString methods of java.lang.Object.  This is done before
         * the methods from the proxy interfaces so that the methods from
         * java.lang.Object take precedence over duplicate methods in the
         * proxy interfaces.
         */
        addProxyMethod(hashCodeMethod);
        addProxyMethod(equalsMethod);
        addProxyMethod(toStringMethod);

        /*
         * Accumulate all of the methods from the proxy interfaces.
         */
        for (Class<?> intf : interfaces) {
            for (Method m : intf.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    addProxyMethod(m, intf);
                }
            }
        }

        /*
         * For each set of proxy methods with the same signature,
         * verify that the methods' return types are compatible.
         */
        for (List<ProxyMethod> sigmethods : proxyMethods.values()) {
            checkReturnTypes(sigmethods);
        }

        generateConstructor();

        for (List<ProxyMethod> sigmethods : proxyMethods.values()) {
            for (ProxyMethod pm : sigmethods) {
                // add static field for the Method object
                visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, pm.methodFieldName,
                        LJLR_METHOD, null, null);

                // Generate code for proxy method
                pm.generateMethod(this, className);
            }
        }

        generateStaticInitializer();
        generateLookupAccessor();
        return toByteArray();
    }

    /**
     * Add another method to be proxied, either by creating a new
     * ProxyMethod object or augmenting an old one for a duplicate
     * method.
     *
     * "fromClass" indicates the proxy interface that the method was
     * found through, which may be different from (a subinterface of)
     * the method's "declaring class".  Note that the first Method
     * object passed for a given name and descriptor identifies the
     * Method object (and thus the declaring class) that will be
     * passed to the invocation handler's "invoke" method for a given
     * set of duplicate methods.
     */
    private void addProxyMethod(Method m, Class<?> fromClass) {
        Class<?> returnType = m.getReturnType();
        Class<?>[] exceptionTypes = m.getExceptionTypes();

        String sig = m.toShortSignature();
        List<ProxyMethod> sigmethods = proxyMethods.computeIfAbsent(sig,
                (f) -> new ArrayList<>(3));
        for (ProxyMethod pm : sigmethods) {
            if (returnType == pm.returnType) {
                /*
                 * Found a match: reduce exception types to the
                 * greatest set of exceptions that can be thrown
                 * compatibly with the throws clauses of both
                 * overridden methods.
                 */
                List<Class<?>> legalExceptions = new ArrayList<>();
                collectCompatibleTypes(
                        exceptionTypes, pm.exceptionTypes, legalExceptions);
                collectCompatibleTypes(
                        pm.exceptionTypes, exceptionTypes, legalExceptions);
                pm.exceptionTypes = legalExceptions.toArray(EMPTY_CLASS_ARRAY);
                return;
            }
        }
        sigmethods.add(new ProxyMethod(m, sig, m.getParameterTypes(), returnType,
                exceptionTypes, fromClass,
                "m" + proxyMethodCount++));
    }

    /**
     * Add an existing ProxyMethod (hashcode, equals, toString).
     *
     * @param pm an existing ProxyMethod
     */
    private void addProxyMethod(ProxyMethod pm) {
        String sig = pm.shortSignature;
        List<ProxyMethod> sigmethods = proxyMethods.computeIfAbsent(sig,
                (f) -> new ArrayList<>(3));
        sigmethods.add(pm);
    }

    /**
     * Generate the constructor method for the proxy class.
     */
    private void generateConstructor() {
        MethodVisitor ctor = visitMethod(Modifier.PUBLIC, NAME_CTOR,
                MJLR_INVOCATIONHANDLER, null, null);
        ctor.visitParameter(null, 0);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitMethodInsn(INVOKESPECIAL, JLR_PROXY, NAME_CTOR,
                MJLR_INVOCATIONHANDLER, false);
        ctor.visitInsn(RETURN);

        // Maxs computed by ClassWriter.COMPUTE_FRAMES, these arguments ignored
        ctor.visitMaxs(-1, -1);
        ctor.visitEnd();
    }

    /**
     * Generate the static initializer method for the proxy class.
     */
    private void generateStaticInitializer() {

        MethodVisitor mv = visitMethod(Modifier.STATIC, NAME_CLINIT,
                "()V", null, null);
        mv.visitCode();
        Label L_startBlock = new Label();
        Label L_endBlock = new Label();
        Label L_NoMethodHandler = new Label();
        Label L_NoClassHandler = new Label();

        mv.visitTryCatchBlock(L_startBlock, L_endBlock, L_NoMethodHandler,
                JL_NO_SUCH_METHOD_EX);
        mv.visitTryCatchBlock(L_startBlock, L_endBlock, L_NoClassHandler,
                JL_CLASS_NOT_FOUND_EX);

        mv.visitLabel(L_startBlock);
        for (List<ProxyMethod> sigmethods : proxyMethods.values()) {
            for (ProxyMethod pm : sigmethods) {
                pm.codeFieldInitialization(mv, className);
            }
        }
        mv.visitInsn(RETURN);
        mv.visitLabel(L_endBlock);
        // Generate exception handler

        mv.visitLabel(L_NoMethodHandler);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitTypeInsn(Opcodes.NEW, JL_NO_SUCH_METHOD_ERROR);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, JL_THROWABLE,
                "getMessage", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, JL_NO_SUCH_METHOD_ERROR,
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(L_NoClassHandler);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitTypeInsn(Opcodes.NEW, JL_NO_CLASS_DEF_FOUND_ERROR);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, JL_THROWABLE,
                "getMessage", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, JL_NO_CLASS_DEF_FOUND_ERROR,
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);

        // Maxs computed by ClassWriter.COMPUTE_FRAMES, these arguments ignored
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    /**
     * Generate the static lookup accessor method that returns the Lookup
     * on this proxy class if the caller's lookup class is java.lang.reflect.Proxy;
     * otherwise, IllegalAccessException is thrown
     */
    private void generateLookupAccessor() {
        MethodVisitor mv = visitMethod(ACC_PRIVATE | ACC_STATIC, NAME_LOOKUP_ACCESSOR,
                "(Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;", null,
                new String[] { JL_ILLEGAL_ACCESS_EX });
        mv.visitCode();
        Label L_illegalAccess = new Label();

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, JLI_LOOKUP, "lookupClass",
                "()Ljava/lang/Class;", false);
        mv.visitLdcInsn(Type.getType(Proxy.class));
        mv.visitJumpInsn(IF_ACMPNE, L_illegalAccess);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, JLI_LOOKUP, "hasFullPrivilegeAccess",
                "()Z", false);
        mv.visitJumpInsn(IFEQ, L_illegalAccess);
        mv.visitMethodInsn(INVOKESTATIC, JLI_METHODHANDLES, "lookup",
                "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
        mv.visitInsn(ARETURN);

        mv.visitLabel(L_illegalAccess);
        mv.visitTypeInsn(Opcodes.NEW, JL_ILLEGAL_ACCESS_EX);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, JLI_LOOKUP, "toString",
                "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, JL_ILLEGAL_ACCESS_EX,
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);

        // Maxs computed by ClassWriter.COMPUTE_FRAMES, these arguments ignored
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    /**
     * A ProxyMethod object represents a proxy method in the proxy class
     * being generated: a method whose implementation will encode and
     * dispatch invocations to the proxy instance's invocation handler.
     */
    private static class ProxyMethod {

        private final Method method;
        private final String shortSignature;
        private final Class<?> fromClass;
        private final Class<?>[] parameterTypes;
        private final Class<?> returnType;
        private final String methodFieldName;
        private Class<?>[] exceptionTypes;

        private ProxyMethod(Method method, String sig, Class<?>[] parameterTypes,
                            Class<?> returnType, Class<?>[] exceptionTypes,
                            Class<?> fromClass, String methodFieldName) {
            this.method = method;
            this.shortSignature = sig;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.exceptionTypes = exceptionTypes;
            this.fromClass = fromClass;
            this.methodFieldName = methodFieldName;
        }

        /**
         * Create a new specific ProxyMethod with a specific field name
         *
         * @param method          The method for which to create a proxy
         * @param methodFieldName the fieldName to generate
         */
        private ProxyMethod(Method method, String methodFieldName) {
            this(method, method.toShortSignature(),
                    method.getParameterTypes(), method.getReturnType(),
                    method.getExceptionTypes(), method.getDeclaringClass(), methodFieldName);
        }

        /**
         * Generate this method, including the code and exception table entry.
         */
        private void generateMethod(ClassWriter cw, String className) {
            MethodType mt = MethodType.methodType(returnType, parameterTypes);
            String desc = mt.toMethodDescriptorString();
            int accessFlags = ACC_PUBLIC | ACC_FINAL;
            if (method.isVarArgs()) accessFlags |= ACC_VARARGS;

            MethodVisitor mv = cw.visitMethod(accessFlags,
                    method.getName(), desc, null,
                    typeNames(Arrays.asList(exceptionTypes)));

            int[] parameterSlot = new int[parameterTypes.length];
            int nextSlot = 1;
            for (int i = 0; i < parameterSlot.length; i++) {
                parameterSlot[i] = nextSlot;
                nextSlot += getWordsPerType(parameterTypes[i]);
            }

            mv.visitCode();
            Label L_startBlock = new Label();
            Label L_endBlock = new Label();
            Label L_RuntimeHandler = new Label();
            Label L_ThrowableHandler = new Label();

            List<Class<?>> catchList = computeUniqueCatchList(exceptionTypes);
            if (catchList.size() > 0) {
                for (Class<?> ex : catchList) {
                    mv.visitTryCatchBlock(L_startBlock, L_endBlock, L_RuntimeHandler,
                            dotToSlash(ex.getName()));
                }

                mv.visitTryCatchBlock(L_startBlock, L_endBlock, L_ThrowableHandler,
                        JL_THROWABLE);
            }
            mv.visitLabel(L_startBlock);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, JLR_PROXY, handlerFieldName,
                    LJLR_INVOCATION_HANDLER);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETSTATIC, dotToSlash(className), methodFieldName,
                    LJLR_METHOD);

            if (parameterTypes.length > 0) {
                // Create an array and fill with the parameters converting primitives to wrappers
                emitIconstInsn(mv, parameterTypes.length);
                mv.visitTypeInsn(Opcodes.ANEWARRAY, JL_OBJECT);
                for (int i = 0; i < parameterTypes.length; i++) {
                    mv.visitInsn(DUP);
                    emitIconstInsn(mv, i);
                    codeWrapArgument(mv, parameterTypes[i], parameterSlot[i]);
                    mv.visitInsn(Opcodes.AASTORE);
                }
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
            }

            mv.visitMethodInsn(INVOKEINTERFACE, JLR_INVOCATION_HANDLER,
                    "invoke",
                    "(Ljava/lang/Object;Ljava/lang/reflect/Method;" +
                            "[Ljava/lang/Object;)Ljava/lang/Object;", true);

            if (returnType == void.class) {
                mv.visitInsn(POP);
                mv.visitInsn(RETURN);
            } else {
                codeUnwrapReturnValue(mv, returnType);
            }

            mv.visitLabel(L_endBlock);

            // Generate exception handler
            mv.visitLabel(L_RuntimeHandler);
            mv.visitInsn(ATHROW);   // just rethrow the exception

            mv.visitLabel(L_ThrowableHandler);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitTypeInsn(Opcodes.NEW, JLR_UNDECLARED_THROWABLE_EX);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, JLR_UNDECLARED_THROWABLE_EX,
                    "<init>", "(Ljava/lang/Throwable;)V", false);
            mv.visitInsn(ATHROW);
            // Maxs computed by ClassWriter.COMPUTE_FRAMES, these arguments ignored
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        /**
         * Generate code for wrapping an argument of the given type
         * whose value can be found at the specified local variable
         * index, in order for it to be passed (as an Object) to the
         * invocation handler's "invoke" method.
         */
        private void codeWrapArgument(MethodVisitor mv, Class<?> type, int slot) {
            if (type.isPrimitive()) {
                PrimitiveTypeInfo prim = PrimitiveTypeInfo.get(type);

                if (type == int.class ||
                        type == boolean.class ||
                        type == byte.class ||
                        type == char.class ||
                        type == short.class) {
                    mv.visitVarInsn(ILOAD, slot);
                } else if (type == long.class) {
                    mv.visitVarInsn(LLOAD, slot);
                } else if (type == float.class) {
                    mv.visitVarInsn(FLOAD, slot);
                } else if (type == double.class) {
                    mv.visitVarInsn(DLOAD, slot);
                } else {
                    throw new AssertionError();
                }
                mv.visitMethodInsn(INVOKESTATIC, prim.wrapperClassName, "valueOf",
                        prim.wrapperValueOfDesc, false);
            } else {
                mv.visitVarInsn(ALOAD, slot);
            }
        }

        /**
         * Generate code for unwrapping a return value of the given
         * type from the invocation handler's "invoke" method (as type
         * Object) to its correct type.
         */
        private void codeUnwrapReturnValue(MethodVisitor mv, Class<?> type) {
            if (type.isPrimitive()) {
                PrimitiveTypeInfo prim = PrimitiveTypeInfo.get(type);

                mv.visitTypeInsn(CHECKCAST, prim.wrapperClassName);
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        prim.wrapperClassName,
                        prim.unwrapMethodName, prim.unwrapMethodDesc, false);

                if (type == int.class ||
                        type == boolean.class ||
                        type == byte.class ||
                        type == char.class ||
                        type == short.class) {
                    mv.visitInsn(IRETURN);
                } else if (type == long.class) {
                    mv.visitInsn(LRETURN);
                } else if (type == float.class) {
                    mv.visitInsn(FRETURN);
                } else if (type == double.class) {
                    mv.visitInsn(DRETURN);
                } else {
                    throw new AssertionError();
                }
            } else {
                mv.visitTypeInsn(CHECKCAST, dotToSlash(type.getName()));
                mv.visitInsn(ARETURN);
            }
        }

        /**
         * Generate code for initializing the static field that stores
         * the Method object for this proxy method.
         */
        private void codeFieldInitialization(MethodVisitor mv, String className) {
            codeClassForName(mv, fromClass);

            mv.visitLdcInsn(method.getName());

            emitIconstInsn(mv, parameterTypes.length);

            mv.visitTypeInsn(Opcodes.ANEWARRAY, JL_CLASS);

            // Construct an array with the parameter types mapping primitives to Wrapper types
            for (int i = 0; i < parameterTypes.length; i++) {
                mv.visitInsn(DUP);
                emitIconstInsn(mv, i);

                if (parameterTypes[i].isPrimitive()) {
                    PrimitiveTypeInfo prim =
                            PrimitiveTypeInfo.get(parameterTypes[i]);
                    mv.visitFieldInsn(GETSTATIC,
                            prim.wrapperClassName, "TYPE", LJL_CLASS);
                } else {
                    codeClassForName(mv, parameterTypes[i]);
                }
                mv.visitInsn(Opcodes.AASTORE);
            }
            // lookup the method
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    JL_CLASS,
                    "getMethod",
                    "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                    false);

            mv.visitFieldInsn(PUTSTATIC,
                    dotToSlash(className),
                    methodFieldName, LJLR_METHOD);
        }

        /*
         * =============== Code Generation Utility Methods ===============
         */

        /**
         * Generate code to invoke the Class.forName with the name of the given
         * class to get its Class object at runtime.  The code is written to
         * the supplied stream.  Note that the code generated by this method
         * may cause the checked ClassNotFoundException to be thrown.
         */
        private void codeClassForName(MethodVisitor mv, Class<?> cl) {
            mv.visitLdcInsn(cl.getName());
            mv.visitMethodInsn(INVOKESTATIC,
                    JL_CLASS,
                    "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        }

        /**
         * Visit a bytecode for a constant.
         *
         * @param mv  The MethodVisitor
         * @param cst The constant value
         */
        private void emitIconstInsn(MethodVisitor mv, final int cst) {
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

        @Override
        public String toString() {
            return method.toShortString();
        }
    }

    /**
     * A PrimitiveTypeInfo object contains assorted information about
     * a primitive type in its public fields.  The struct for a particular
     * primitive type can be obtained using the static "get" method.
     */
    private static class PrimitiveTypeInfo {

        private static Map<Class<?>, PrimitiveTypeInfo> table = new HashMap<>();

        static {
            add(byte.class, Byte.class);
            add(char.class, Character.class);
            add(double.class, Double.class);
            add(float.class, Float.class);
            add(int.class, Integer.class);
            add(long.class, Long.class);
            add(short.class, Short.class);
            add(boolean.class, Boolean.class);
        }

        /**
         * name of corresponding wrapper class
         */
        private String wrapperClassName;
        /**
         * method descriptor for wrapper class "valueOf" factory method
         */
        private String wrapperValueOfDesc;
        /**
         * name of wrapper class method for retrieving primitive value
         */
        private String unwrapMethodName;
        /**
         * descriptor of same method
         */
        private String unwrapMethodDesc;

        private PrimitiveTypeInfo(Class<?> primitiveClass, Class<?> wrapperClass) {
            assert primitiveClass.isPrimitive();

            /**
             * "base type" used in various descriptors (see JVMS section 4.3.2)
             */
            String baseTypeString =
                    Array.newInstance(primitiveClass, 0)
                            .getClass().getName().substring(1);
            wrapperClassName = dotToSlash(wrapperClass.getName());
            wrapperValueOfDesc =
                    "(" + baseTypeString + ")L" + wrapperClassName + ";";
            unwrapMethodName = primitiveClass.getName() + "Value";
            unwrapMethodDesc = "()" + baseTypeString;
        }

        private static void add(Class<?> primitiveClass, Class<?> wrapperClass) {
            table.put(primitiveClass,
                    new PrimitiveTypeInfo(primitiveClass, wrapperClass));
        }

        public static PrimitiveTypeInfo get(Class<?> cl) {
            return table.get(cl);
        }
    }
}
