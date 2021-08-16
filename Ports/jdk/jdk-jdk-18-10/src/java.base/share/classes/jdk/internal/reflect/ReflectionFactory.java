/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.Properties;
import jdk.internal.access.JavaLangReflectAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.VM;
import sun.reflect.misc.ReflectUtil;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

/** <P> The master factory for all reflective objects, both those in
    java.lang.reflect (Fields, Methods, Constructors) as well as their
    delegates (FieldAccessors, MethodAccessors, ConstructorAccessors).
    </P>

    <P> The methods in this class are extremely unsafe and can cause
    subversion of both the language and the verifier. For this reason,
    they are all instance methods, and access to the constructor of
    this factory is guarded by a security check, in similar style to
    {@link jdk.internal.misc.Unsafe}. </P>
*/

public class ReflectionFactory {

    private static boolean initted = false;
    private static final ReflectionFactory soleInstance = new ReflectionFactory();


    /* Method for static class initializer <clinit>, or null */
    private static volatile Method hasStaticInitializerMethod;

    //
    // "Inflation" mechanism. Loading bytecodes to implement
    // Method.invoke() and Constructor.newInstance() currently costs
    // 3-4x more than an invocation via native code for the first
    // invocation (though subsequent invocations have been benchmarked
    // to be over 20x faster). Unfortunately this cost increases
    // startup time for certain applications that use reflection
    // intensively (but only once per class) to bootstrap themselves.
    // To avoid this penalty we reuse the existing JVM entry points
    // for the first few invocations of Methods and Constructors and
    // then switch to the bytecode-based implementations.
    //
    // Package-private to be accessible to NativeMethodAccessorImpl
    // and NativeConstructorAccessorImpl
    private static boolean noInflation        = false;
    private static int     inflationThreshold = 15;

    // true if deserialization constructor checking is disabled
    private static boolean disableSerialConstructorChecks = false;

    private final JavaLangReflectAccess langReflectAccess;
    private ReflectionFactory() {
        this.langReflectAccess = SharedSecrets.getJavaLangReflectAccess();
    }

    /**
     * A convenience class for acquiring the capability to instantiate
     * reflective objects.  Use this instead of a raw call to {@link
     * #getReflectionFactory} in order to avoid being limited by the
     * permissions of your callers.
     *
     * <p>An instance of this class can be used as the argument of
     * <code>AccessController.doPrivileged</code>.
     */
    public static final class GetReflectionFactoryAction
        implements PrivilegedAction<ReflectionFactory> {
        public ReflectionFactory run() {
            return getReflectionFactory();
        }
    }

    /**
     * Provides the caller with the capability to instantiate reflective
     * objects.
     *
     * <p> First, if there is a security manager, its
     * <code>checkPermission</code> method is called with a {@link
     * java.lang.RuntimePermission} with target
     * <code>"reflectionFactoryAccess"</code>.  This may result in a
     * security exception.
     *
     * <p> The returned <code>ReflectionFactory</code> object should be
     * carefully guarded by the caller, since it can be used to read and
     * write private data and invoke private methods, as well as to load
     * unverified bytecodes.  It must never be passed to untrusted code.
     *
     * @exception SecurityException if a security manager exists and its
     *             <code>checkPermission</code> method doesn't allow
     *             access to the RuntimePermission "reflectionFactoryAccess".  */
    public static ReflectionFactory getReflectionFactory() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                SecurityConstants.REFLECTION_FACTORY_ACCESS_PERMISSION);
        }
        return soleInstance;
    }

    /**
     * Returns an alternate reflective Method instance for the given method
     * intended for reflection to invoke, if present.
     *
     * A trusted method can define an alternate implementation for a method `foo`
     * by defining a method named "reflected$foo" that will be invoked
     * reflectively.
     */
    private static Method findMethodForReflection(Method method) {
        String altName = "reflected$" + method.getName();
        try {
           return method.getDeclaringClass()
                        .getDeclaredMethod(altName, method.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    //--------------------------------------------------------------------------
    //
    // Routines used by java.lang.reflect
    //
    //

    /*
     * Note: this routine can cause the declaring class for the field
     * be initialized and therefore must not be called until the
     * first get/set of this field.
     * @param field the field
     * @param override true if caller has overridden accessibility
     */
    public FieldAccessor newFieldAccessor(Field field, boolean override) {
        checkInitted();

        Field root = langReflectAccess.getRoot(field);
        if (root != null) {
            // FieldAccessor will use the root unless the modifiers have
            // been overridden
            if (root.getModifiers() == field.getModifiers() || !override) {
                field = root;
            }
        }
        boolean isFinal = Modifier.isFinal(field.getModifiers());
        boolean isReadOnly = isFinal && (!override || langReflectAccess.isTrustedFinalField(field));
        return UnsafeFieldAccessorFactory.newFieldAccessor(field, isReadOnly);
    }

    public MethodAccessor newMethodAccessor(Method method) {
        checkInitted();

        if (Reflection.isCallerSensitive(method)) {
            Method altMethod = findMethodForReflection(method);
            if (altMethod != null) {
                method = altMethod;
            }
        }

        // use the root Method that will not cache caller class
        Method root = langReflectAccess.getRoot(method);
        if (root != null) {
            method = root;
        }

        if (noInflation && !method.getDeclaringClass().isHidden()) {
            return new MethodAccessorGenerator().
                generateMethod(method.getDeclaringClass(),
                               method.getName(),
                               method.getParameterTypes(),
                               method.getReturnType(),
                               method.getExceptionTypes(),
                               method.getModifiers());
        } else {
            NativeMethodAccessorImpl acc =
                new NativeMethodAccessorImpl(method);
            DelegatingMethodAccessorImpl res =
                new DelegatingMethodAccessorImpl(acc);
            acc.setParent(res);
            return res;
        }
    }

    public ConstructorAccessor newConstructorAccessor(Constructor<?> c) {
        checkInitted();

        Class<?> declaringClass = c.getDeclaringClass();
        if (Modifier.isAbstract(declaringClass.getModifiers())) {
            return new InstantiationExceptionConstructorAccessorImpl(null);
        }
        if (declaringClass == Class.class) {
            return new InstantiationExceptionConstructorAccessorImpl
                ("Can not instantiate java.lang.Class");
        }

        // use the root Constructor that will not cache caller class
        Constructor<?> root = langReflectAccess.getRoot(c);
        if (root != null) {
            c = root;
        }

        // Bootstrapping issue: since we use Class.newInstance() in
        // the ConstructorAccessor generation process, we have to
        // break the cycle here.
        if (Reflection.isSubclassOf(declaringClass,
                                    ConstructorAccessorImpl.class)) {
            return new BootstrapConstructorAccessorImpl(c);
        }

        if (noInflation && !c.getDeclaringClass().isHidden()) {
            return new MethodAccessorGenerator().
                generateConstructor(c.getDeclaringClass(),
                                    c.getParameterTypes(),
                                    c.getExceptionTypes(),
                                    c.getModifiers());
        } else {
            NativeConstructorAccessorImpl acc =
                new NativeConstructorAccessorImpl(c);
            DelegatingConstructorAccessorImpl res =
                new DelegatingConstructorAccessorImpl(acc);
            acc.setParent(res);
            return res;
        }
    }

    //--------------------------------------------------------------------------
    //
    // Routines used by java.lang
    //
    //

    /** Creates a new java.lang.reflect.Constructor. Access checks as
        per java.lang.reflect.AccessibleObject are not overridden. */
    public Constructor<?> newConstructor(Class<?> declaringClass,
                                         Class<?>[] parameterTypes,
                                         Class<?>[] checkedExceptions,
                                         int modifiers,
                                         int slot,
                                         String signature,
                                         byte[] annotations,
                                         byte[] parameterAnnotations)
    {
        return langReflectAccess.newConstructor(declaringClass,
                                                parameterTypes,
                                                checkedExceptions,
                                                modifiers,
                                                slot,
                                                signature,
                                                annotations,
                                                parameterAnnotations);
    }

    /** Gets the ConstructorAccessor object for a
        java.lang.reflect.Constructor */
    public ConstructorAccessor getConstructorAccessor(Constructor<?> c) {
        return langReflectAccess.getConstructorAccessor(c);
    }

    /** Sets the ConstructorAccessor object for a
        java.lang.reflect.Constructor */
    public void setConstructorAccessor(Constructor<?> c,
                                       ConstructorAccessor accessor)
    {
        langReflectAccess.setConstructorAccessor(c, accessor);
    }

    /** Makes a copy of the passed method. The returned method is a
        "child" of the passed one; see the comments in Method.java for
        details. */
    public Method copyMethod(Method arg) {
        return langReflectAccess.copyMethod(arg);
    }

    /** Makes a copy of the passed method. The returned method is NOT
     * a "child" but a "sibling" of the Method in arg. Should only be
     * used on non-root methods. */
    public Method leafCopyMethod(Method arg) {
        return langReflectAccess.leafCopyMethod(arg);
    }


    /** Makes a copy of the passed field. The returned field is a
        "child" of the passed one; see the comments in Field.java for
        details. */
    public Field copyField(Field arg) {
        return langReflectAccess.copyField(arg);
    }

    /** Makes a copy of the passed constructor. The returned
        constructor is a "child" of the passed one; see the comments
        in Constructor.java for details. */
    public <T> Constructor<T> copyConstructor(Constructor<T> arg) {
        return langReflectAccess.copyConstructor(arg);
    }

    /** Gets the byte[] that encodes TypeAnnotations on an executable.
     */
    public byte[] getExecutableTypeAnnotationBytes(Executable ex) {
        return langReflectAccess.getExecutableTypeAnnotationBytes(ex);
    }

    public Class<?>[] getExecutableSharedParameterTypes(Executable ex) {
        return langReflectAccess.getExecutableSharedParameterTypes(ex);
    }

    public <T> T newInstance(Constructor<T> ctor, Object[] args, Class<?> caller)
        throws IllegalAccessException, InstantiationException, InvocationTargetException
    {
        return langReflectAccess.newInstance(ctor, args, caller);
    }

    //--------------------------------------------------------------------------
    //
    // Routines used by serialization
    //
    //

    public final Constructor<?> newConstructorForExternalization(Class<?> cl) {
        if (!Externalizable.class.isAssignableFrom(cl)) {
            return null;
        }
        try {
            Constructor<?> cons = cl.getConstructor();
            cons.setAccessible(true);
            return cons;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public final Constructor<?> newConstructorForSerialization(Class<?> cl,
                                                               Constructor<?> constructorToCall)
    {
        if (constructorToCall.getDeclaringClass() == cl) {
            constructorToCall.setAccessible(true);
            return constructorToCall;
        }
        return generateConstructor(cl, constructorToCall);
    }

    /**
     * Given a class, determines whether its superclass has
     * any constructors that are accessible from the class.
     * This is a special purpose method intended to do access
     * checking for a serializable class and its superclasses
     * up to, but not including, the first non-serializable
     * superclass. This also implies that the superclass is
     * always non-null, because a serializable class must be a
     * class (not an interface) and Object is not serializable.
     *
     * @param cl the class from which access is checked
     * @return whether the superclass has a constructor accessible from cl
     */
    private boolean superHasAccessibleConstructor(Class<?> cl) {
        Class<?> superCl = cl.getSuperclass();
        assert Serializable.class.isAssignableFrom(cl);
        assert superCl != null;
        if (packageEquals(cl, superCl)) {
            // accessible if any non-private constructor is found
            for (Constructor<?> ctor : superCl.getDeclaredConstructors()) {
                if ((ctor.getModifiers() & Modifier.PRIVATE) == 0) {
                    return true;
                }
            }
            if (Reflection.areNestMates(cl, superCl)) {
                return true;
            }
            return false;
        } else {
            // sanity check to ensure the parent is protected or public
            if ((superCl.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC)) == 0) {
                return false;
            }
            // accessible if any constructor is protected or public
            for (Constructor<?> ctor : superCl.getDeclaredConstructors()) {
                if ((ctor.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC)) != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns a constructor that allocates an instance of cl and that then initializes
     * the instance by calling the no-arg constructor of its first non-serializable
     * superclass. This is specified in the Serialization Specification, section 3.1,
     * in step 11 of the deserialization process. If cl is not serializable, returns
     * cl's no-arg constructor. If no accessible constructor is found, or if the
     * class hierarchy is somehow malformed (e.g., a serializable class has no
     * superclass), null is returned.
     *
     * @param cl the class for which a constructor is to be found
     * @return the generated constructor, or null if none is available
     */
    public final Constructor<?> newConstructorForSerialization(Class<?> cl) {
        Class<?> initCl = cl;
        while (Serializable.class.isAssignableFrom(initCl)) {
            Class<?> prev = initCl;
            if ((initCl = initCl.getSuperclass()) == null ||
                (!disableSerialConstructorChecks && !superHasAccessibleConstructor(prev))) {
                return null;
            }
        }
        Constructor<?> constructorToCall;
        try {
            constructorToCall = initCl.getDeclaredConstructor();
            int mods = constructorToCall.getModifiers();
            if ((mods & Modifier.PRIVATE) != 0 ||
                    ((mods & (Modifier.PUBLIC | Modifier.PROTECTED)) == 0 &&
                            !packageEquals(cl, initCl))) {
                return null;
            }
        } catch (NoSuchMethodException ex) {
            return null;
        }
        return generateConstructor(cl, constructorToCall);
    }

    private final Constructor<?> generateConstructor(Class<?> cl,
                                                     Constructor<?> constructorToCall) {


        ConstructorAccessor acc = new MethodAccessorGenerator().
            generateSerializationConstructor(cl,
                                             constructorToCall.getParameterTypes(),
                                             constructorToCall.getExceptionTypes(),
                                             constructorToCall.getModifiers(),
                                             constructorToCall.getDeclaringClass());
        Constructor<?> c = newConstructor(constructorToCall.getDeclaringClass(),
                                          constructorToCall.getParameterTypes(),
                                          constructorToCall.getExceptionTypes(),
                                          constructorToCall.getModifiers(),
                                          langReflectAccess.
                                          getConstructorSlot(constructorToCall),
                                          langReflectAccess.
                                          getConstructorSignature(constructorToCall),
                                          langReflectAccess.
                                          getConstructorAnnotations(constructorToCall),
                                          langReflectAccess.
                                          getConstructorParameterAnnotations(constructorToCall));
        setConstructorAccessor(c, acc);
        c.setAccessible(true);
        return c;
    }

    public final MethodHandle readObjectForSerialization(Class<?> cl) {
        return findReadWriteObjectForSerialization(cl, "readObject", ObjectInputStream.class);
    }

    public final MethodHandle readObjectNoDataForSerialization(Class<?> cl) {
        return findReadWriteObjectForSerialization(cl, "readObjectNoData", ObjectInputStream.class);
    }

    public final MethodHandle writeObjectForSerialization(Class<?> cl) {
        return findReadWriteObjectForSerialization(cl, "writeObject", ObjectOutputStream.class);
    }

    private final MethodHandle findReadWriteObjectForSerialization(Class<?> cl,
                                                                   String methodName,
                                                                   Class<?> streamClass) {
        if (!Serializable.class.isAssignableFrom(cl)) {
            return null;
        }

        try {
            Method meth = cl.getDeclaredMethod(methodName, streamClass);
            int mods = meth.getModifiers();
            if (meth.getReturnType() != Void.TYPE ||
                    Modifier.isStatic(mods) ||
                    !Modifier.isPrivate(mods)) {
                return null;
            }
            meth.setAccessible(true);
            return MethodHandles.lookup().unreflect(meth);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (IllegalAccessException ex1) {
            throw new InternalError("Error", ex1);
        }
    }

    /**
     * Returns a MethodHandle for {@code writeReplace} on the serializable class
     * or null if no match found.
     * @param cl a serializable class
     * @returnss the {@code writeReplace} MethodHandle or {@code null} if not found
     */
    public final MethodHandle writeReplaceForSerialization(Class<?> cl) {
        return getReplaceResolveForSerialization(cl, "writeReplace");
    }

    /**
     * Returns a MethodHandle for {@code readResolve} on the serializable class
     * or null if no match found.
     * @param cl a serializable class
     * @returns the {@code writeReplace} MethodHandle or {@code null} if not found
     */
    public final MethodHandle readResolveForSerialization(Class<?> cl) {
        return getReplaceResolveForSerialization(cl, "readResolve");
    }

    /**
     * Lookup readResolve or writeReplace on a class with specified
     * signature constraints.
     * @param cl a serializable class
     * @param methodName the method name to find
     * @returns a MethodHandle for the method or {@code null} if not found or
     *       has the wrong signature.
     */
    private MethodHandle getReplaceResolveForSerialization(Class<?> cl,
                                                           String methodName) {
        if (!Serializable.class.isAssignableFrom(cl)) {
            return null;
        }

        Class<?> defCl = cl;
        while (defCl != null) {
            try {
                Method m = defCl.getDeclaredMethod(methodName);
                if (m.getReturnType() != Object.class) {
                    return null;
                }
                int mods = m.getModifiers();
                if (Modifier.isStatic(mods) | Modifier.isAbstract(mods)) {
                    return null;
                } else if (Modifier.isPublic(mods) | Modifier.isProtected(mods)) {
                    // fall through
                } else if (Modifier.isPrivate(mods) && (cl != defCl)) {
                    return null;
                } else if (!packageEquals(cl, defCl)) {
                    return null;
                }
                try {
                    // Normal return
                    m.setAccessible(true);
                    return MethodHandles.lookup().unreflect(m);
                } catch (IllegalAccessException ex0) {
                    // setAccessible should prevent IAE
                    throw new InternalError("Error", ex0);
                }
            } catch (NoSuchMethodException ex) {
                defCl = defCl.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Returns true if the given class defines a static initializer method,
     * false otherwise.
     */
    public final boolean hasStaticInitializerForSerialization(Class<?> cl) {
        Method m = hasStaticInitializerMethod;
        if (m == null) {
            try {
                m = ObjectStreamClass.class.getDeclaredMethod("hasStaticInitializer",
                        new Class<?>[]{Class.class});
                m.setAccessible(true);
                hasStaticInitializerMethod = m;
            } catch (NoSuchMethodException ex) {
                throw new InternalError("No such method hasStaticInitializer on "
                        + ObjectStreamClass.class, ex);
            }
        }
        try {
            return (Boolean) m.invoke(null, cl);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            throw new InternalError("Exception invoking hasStaticInitializer", ex);
        }
    }

    /**
     * Return the accessible constructor for OptionalDataException signaling eof.
     * @returns the eof constructor for OptionalDataException
     */
    public final Constructor<OptionalDataException> newOptionalDataExceptionForSerialization() {
        try {
            Constructor<OptionalDataException> boolCtor =
                    OptionalDataException.class.getDeclaredConstructor(Boolean.TYPE);
            boolCtor.setAccessible(true);
            return boolCtor;
        } catch (NoSuchMethodException ex) {
            throw new InternalError("Constructor not found", ex);
        }
    }

    //--------------------------------------------------------------------------
    //
    // Internals only below this point
    //

    static int inflationThreshold() {
        return inflationThreshold;
    }

    /** We have to defer full initialization of this class until after
        the static initializer is run since java.lang.reflect.Method's
        static initializer (more properly, that for
        java.lang.reflect.AccessibleObject) causes this class's to be
        run, before the system properties are set up. */
    private static void checkInitted() {
        if (initted) return;

        // Defer initialization until module system is initialized so as
        // to avoid inflation and spinning bytecode in unnamed modules
        // during early startup.
        if (!VM.isModuleSystemInited()) {
            return;
        }

        Properties props = GetPropertyAction.privilegedGetProperties();
        String val = props.getProperty("sun.reflect.noInflation");
        if (val != null && val.equals("true")) {
            noInflation = true;
        }

        val = props.getProperty("sun.reflect.inflationThreshold");
        if (val != null) {
            try {
                inflationThreshold = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Unable to parse property sun.reflect.inflationThreshold", e);
            }
        }

        disableSerialConstructorChecks =
            "true".equals(props.getProperty("jdk.disableSerialConstructorChecks"));

        initted = true;
    }

    /**
     * Returns true if classes are defined in the classloader and same package, false
     * otherwise.
     * @param cl1 a class
     * @param cl2 another class
     * @returns true if the two classes are in the same classloader and package
     */
    private static boolean packageEquals(Class<?> cl1, Class<?> cl2) {
        assert !cl1.isArray() && !cl2.isArray();

        if (cl1 == cl2) {
            return true;
        }

        return cl1.getClassLoader() == cl2.getClassLoader() &&
                cl1.getPackageName() == cl2.getPackageName();
    }

}
