/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.invoke.TypeDescriptor;
import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleReader;
import java.lang.ref.SoftReference;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.constant.Constable;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.internal.loader.BootLoader;
import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.misc.Unsafe;
import jdk.internal.module.Resources;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.ConstantPool;
import jdk.internal.reflect.Reflection;
import jdk.internal.reflect.ReflectionFactory;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.IntrinsicCandidate;
import sun.invoke.util.Wrapper;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.ClassRepository;
import sun.reflect.generics.repository.MethodRepository;
import sun.reflect.generics.repository.ConstructorRepository;
import sun.reflect.generics.scope.ClassScope;
import sun.security.util.SecurityConstants;
import sun.reflect.annotation.*;
import sun.reflect.misc.ReflectUtil;

/**
 * Instances of the class {@code Class} represent classes and
 * interfaces in a running Java application. An enum class and a record
 * class are kinds of class; an annotation interface is a kind of
 * interface. Every array also belongs to a class that is reflected as
 * a {@code Class} object that is shared by all arrays with the same
 * element type and number of dimensions.  The primitive Java types
 * ({@code boolean}, {@code byte}, {@code char}, {@code short}, {@code
 * int}, {@code long}, {@code float}, and {@code double}), and the
 * keyword {@code void} are also represented as {@code Class} objects.
 *
 * <p> {@code Class} has no public constructor. Instead a {@code Class}
 * object is constructed automatically by the Java Virtual Machine when
 * a class is derived from the bytes of a {@code class} file through
 * the invocation of one of the following methods:
 * <ul>
 * <li> {@link ClassLoader#defineClass(String, byte[], int, int) ClassLoader::defineClass}
 * <li> {@link java.lang.invoke.MethodHandles.Lookup#defineClass(byte[])
 *      java.lang.invoke.MethodHandles.Lookup::defineClass}
 * <li> {@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass(byte[], boolean, MethodHandles.Lookup.ClassOption...)
 *      java.lang.invoke.MethodHandles.Lookup::defineHiddenClass}
 * </ul>
 *
 * <p> The methods of class {@code Class} expose many characteristics of a
 * class or interface. Most characteristics are derived from the {@code class}
 * file that the class loader passed to the Java Virtual Machine or
 * from the {@code class} file passed to {@code Lookup::defineClass}
 * or {@code Lookup::defineHiddenClass}.
 * A few characteristics are determined by the class loading environment
 * at run time, such as the module returned by {@link #getModule() getModule()}.
 *
 * <p> The following example uses a {@code Class} object to print the
 * class name of an object:
 *
 * <blockquote><pre>
 *     void printClassName(Object obj) {
 *         System.out.println("The class of " + obj +
 *                            " is " + obj.getClass().getName());
 *     }
 * </pre></blockquote>
 *
 * It is also possible to get the {@code Class} object for a named
 * class or interface (or for {@code void}) using a <i>class literal</i>.
 * For example:
 *
 * <blockquote>
 *     {@code System.out.println("The name of class Foo is: "+Foo.class.getName());}
 * </blockquote>
 *
 * <p> Some methods of class {@code Class} expose whether the declaration of
 * a class or interface in Java source code was <em>enclosed</em> within
 * another declaration. Other methods describe how a class or interface
 * is situated in a <em>nest</em>. A <a id="nest">nest</a> is a set of
 * classes and interfaces, in the same run-time package, that
 * allow mutual access to their {@code private} members.
 * The classes and interfaces are known as <em>nestmates</em>.
 * One nestmate acts as the
 * <em>nest host</em>, and enumerates the other nestmates which
 * belong to the nest; each of them in turn records it as the nest host.
 * The classes and interfaces which belong to a nest, including its host, are
 * determined when
 * {@code class} files are generated, for example, a Java compiler
 * will typically record a top-level class as the host of a nest where the
 * other members are the classes and interfaces whose declarations are
 * enclosed within the top-level class declaration.
 *
 * <p> A class or interface created by the invocation of
 * {@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass(byte[], boolean, MethodHandles.Lookup.ClassOption...)
 * Lookup::defineHiddenClass} is a {@linkplain Class#isHidden() <em>hidden</em>}
 * class or interface.
 * All kinds of class, including enum classes and record classes, may be
 * hidden classes; all kinds of interface, including annotation interfaces,
 * may be hidden interfaces.
 *
 * The {@linkplain #getName() name of a hidden class or interface} is
 * not a <a href="ClassLoader.html#binary-name">binary name</a>,
 * which means the following:
 * <ul>
 * <li>A hidden class or interface cannot be referenced by the constant pools
 *     of other classes and interfaces.
 * <li>A hidden class or interface cannot be described in
 *     {@linkplain java.lang.constant.ConstantDesc <em>nominal form</em>} by
 *     {@link #describeConstable() Class::describeConstable},
 *     {@link ClassDesc#of(String) ClassDesc::of}, or
 *     {@link ClassDesc#ofDescriptor(String) ClassDesc::ofDescriptor}.
 * <li>A hidden class or interface cannot be discovered by {@link #forName Class::forName}
 *     or {@link ClassLoader#loadClass(String, boolean) ClassLoader::loadClass}.
 * </ul>
 *
 * A hidden class or interface is never an array class, but may be
 * the element type of an array. In all other respects, the fact that
 * a class or interface is hidden has no bearing on the characteristics
 * exposed by the methods of class {@code Class}.
 *
 * @param <T> the type of the class modeled by this {@code Class}
 * object.  For example, the type of {@code String.class} is {@code
 * Class<String>}.  Use {@code Class<?>} if the class being modeled is
 * unknown.
 *
 * @see     java.lang.ClassLoader#defineClass(byte[], int, int)
 * @since   1.0
 * @jls 15.8.2 Class Literals
 */
public final class Class<T> implements java.io.Serializable,
                              GenericDeclaration,
                              Type,
                              AnnotatedElement,
                              TypeDescriptor.OfField<Class<?>>,
                              Constable {
    private static final int ANNOTATION= 0x00002000;
    private static final int ENUM      = 0x00004000;
    private static final int SYNTHETIC = 0x00001000;

    private static native void registerNatives();
    static {
        registerNatives();
    }

    /*
     * Private constructor. Only the Java Virtual Machine creates Class objects.
     * This constructor is not used and prevents the default constructor being
     * generated.
     */
    private Class(ClassLoader loader, Class<?> arrayComponentType) {
        // Initialize final field for classLoader.  The initialization value of non-null
        // prevents future JIT optimizations from assuming this final field is null.
        classLoader = loader;
        componentType = arrayComponentType;
    }

    /**
     * Converts the object to a string. The string representation is the
     * string "class" or "interface", followed by a space, and then by the
     * name of the class in the format returned by {@code getName}.
     * If this {@code Class} object represents a primitive type,
     * this method returns the name of the primitive type.  If
     * this {@code Class} object represents void this method returns
     * "void". If this {@code Class} object represents an array type,
     * this method returns "class " followed by {@code getName}.
     *
     * @return a string representation of this {@code Class} object.
     */
    public String toString() {
        String kind = isInterface() ? "interface " : isPrimitive() ? "" : "class ";
        return kind.concat(getName());
    }

    /**
     * Returns a string describing this {@code Class}, including
     * information about modifiers and type parameters.
     *
     * The string is formatted as a list of type modifiers, if any,
     * followed by the kind of type (empty string for primitive types
     * and {@code class}, {@code enum}, {@code interface},
     * {@code @interface}, or {@code record} as appropriate), followed
     * by the type's name, followed by an angle-bracketed
     * comma-separated list of the type's type parameters, if any,
     * including informative bounds on the type parameters, if any.
     *
     * A space is used to separate modifiers from one another and to
     * separate any modifiers from the kind of type. The modifiers
     * occur in canonical order. If there are no type parameters, the
     * type parameter list is elided.
     *
     * For an array type, the string starts with the type name,
     * followed by an angle-bracketed comma-separated list of the
     * type's type parameters, if any, followed by a sequence of
     * {@code []} characters, one set of brackets per dimension of
     * the array.
     *
     * <p>Note that since information about the runtime representation
     * of a type is being generated, modifiers not present on the
     * originating source code or illegal on the originating source
     * code may be present.
     *
     * @return a string describing this {@code Class}, including
     * information about modifiers and type parameters
     *
     * @since 1.8
     */
    public String toGenericString() {
        if (isPrimitive()) {
            return toString();
        } else {
            StringBuilder sb = new StringBuilder();
            Class<?> component = this;
            int arrayDepth = 0;

            if (isArray()) {
                do {
                    arrayDepth++;
                    component = component.getComponentType();
                } while (component.isArray());
                sb.append(component.getName());
            } else {
                // Class modifiers are a superset of interface modifiers
                int modifiers = getModifiers() & Modifier.classModifiers();
                if (modifiers != 0) {
                    sb.append(Modifier.toString(modifiers));
                    sb.append(' ');
                }

                if (isAnnotation()) {
                    sb.append('@');
                }
                if (isInterface()) { // Note: all annotation interfaces are interfaces
                    sb.append("interface");
                } else {
                    if (isEnum())
                        sb.append("enum");
                    else if (isRecord())
                        sb.append("record");
                    else
                        sb.append("class");
                }
                sb.append(' ');
                sb.append(getName());
            }

            TypeVariable<?>[] typeparms = component.getTypeParameters();
            if (typeparms.length > 0) {
                sb.append(Arrays.stream(typeparms)
                          .map(Class::typeVarBounds)
                          .collect(Collectors.joining(",", "<", ">")));
            }

            if (arrayDepth > 0) sb.append("[]".repeat(arrayDepth));

            return sb.toString();
        }
    }

    static String typeVarBounds(TypeVariable<?> typeVar) {
        Type[] bounds = typeVar.getBounds();
        if (bounds.length == 1 && bounds[0].equals(Object.class)) {
            return typeVar.getName();
        } else {
            return typeVar.getName() + " extends " +
                Arrays.stream(bounds)
                .map(Type::getTypeName)
                .collect(Collectors.joining(" & "));
        }
    }

    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given string name.  Invoking this method is
     * equivalent to:
     *
     * <blockquote>
     *  {@code Class.forName(className, true, currentLoader)}
     * </blockquote>
     *
     * where {@code currentLoader} denotes the defining class loader of
     * the current class.
     *
     * <p> For example, the following code fragment returns the
     * runtime {@code Class} descriptor for the class named
     * {@code java.lang.Thread}:
     *
     * <blockquote>
     *   {@code Class t = Class.forName("java.lang.Thread")}
     * </blockquote>
     * <p>
     * A call to {@code forName("X")} causes the class named
     * {@code X} to be initialized.
     *
     * @param      className   the fully qualified name of the desired class.
     * @return     the {@code Class} object for the class with the
     *             specified name.
     * @throws    LinkageError if the linkage fails
     * @throws    ExceptionInInitializerError if the initialization provoked
     *            by this method fails
     * @throws    ClassNotFoundException if the class cannot be located
     *
     * @jls 12.2 Loading of Classes and Interfaces
     * @jls 12.3 Linking of Classes and Interfaces
     * @jls 12.4 Initialization of Classes and Interfaces
     */
    @CallerSensitive
    public static Class<?> forName(String className)
                throws ClassNotFoundException {
        Class<?> caller = Reflection.getCallerClass();
        return forName0(className, true, ClassLoader.getClassLoader(caller), caller);
    }


    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given string name, using the given class loader.
     * Given the fully qualified name for a class or interface (in the same
     * format returned by {@code getName}) this method attempts to
     * locate and load the class or interface.  The specified class
     * loader is used to load the class or interface.  If the parameter
     * {@code loader} is null, the class is loaded through the bootstrap
     * class loader.  The class is initialized only if the
     * {@code initialize} parameter is {@code true} and if it has
     * not been initialized earlier.
     *
     * <p> If {@code name} denotes a primitive type or void, an attempt
     * will be made to locate a user-defined class in the unnamed package whose
     * name is {@code name}. Therefore, this method cannot be used to
     * obtain any of the {@code Class} objects representing primitive
     * types or void.
     *
     * <p> If {@code name} denotes an array class, the component type of
     * the array class is loaded but not initialized.
     *
     * <p> For example, in an instance method the expression:
     *
     * <blockquote>
     *  {@code Class.forName("Foo")}
     * </blockquote>
     *
     * is equivalent to:
     *
     * <blockquote>
     *  {@code Class.forName("Foo", true, this.getClass().getClassLoader())}
     * </blockquote>
     *
     * Note that this method throws errors related to loading, linking
     * or initializing as specified in Sections {@jls 12.2}, {@jls
     * 12.3}, and {@jls 12.4} of <cite>The Java Language
     * Specification</cite>.
     * Note that this method does not check whether the requested class
     * is accessible to its caller.
     *
     * @param name       fully qualified name of the desired class

     * @param initialize if {@code true} the class will be initialized
     *                   (which implies linking). See Section {@jls
     *                   12.4} of <cite>The Java Language
     *                   Specification</cite>.
     * @param loader     class loader from which the class must be loaded
     * @return           class object representing the desired class
     *
     * @throws    LinkageError if the linkage fails
     * @throws    ExceptionInInitializerError if the initialization provoked
     *            by this method fails
     * @throws    ClassNotFoundException if the class cannot be located by
     *            the specified class loader
     * @throws    SecurityException
     *            if a security manager is present, and the {@code loader} is
     *            {@code null}, and the caller's class loader is not
     *            {@code null}, and the caller does not have the
     *            {@link RuntimePermission}{@code ("getClassLoader")}
     *
     * @see       java.lang.Class#forName(String)
     * @see       java.lang.ClassLoader
     *
     * @jls 12.2 Loading of Classes and Interfaces
     * @jls 12.3 Linking of Classes and Interfaces
     * @jls 12.4 Initialization of Classes and Interfaces
     * @since     1.2
     */
    @CallerSensitive
    public static Class<?> forName(String name, boolean initialize,
                                   ClassLoader loader)
        throws ClassNotFoundException
    {
        Class<?> caller = null;
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // Reflective call to get caller class is only needed if a security manager
            // is present.  Avoid the overhead of making this call otherwise.
            caller = Reflection.getCallerClass();
            if (loader == null) {
                ClassLoader ccl = ClassLoader.getClassLoader(caller);
                if (ccl != null) {
                    sm.checkPermission(
                        SecurityConstants.GET_CLASSLOADER_PERMISSION);
                }
            }
        }
        return forName0(name, initialize, loader, caller);
    }

    /** Called after security check for system loader access checks have been made. */
    private static native Class<?> forName0(String name, boolean initialize,
                                            ClassLoader loader,
                                            Class<?> caller)
        throws ClassNotFoundException;


    /**
     * Returns the {@code Class} with the given <a href="ClassLoader.html#binary-name">
     * binary name</a> in the given module.
     *
     * <p> This method attempts to locate and load the class or interface.
     * It does not link the class, and does not run the class initializer.
     * If the class is not found, this method returns {@code null}. </p>
     *
     * <p> If the class loader of the given module defines other modules and
     * the given name is a class defined in a different module, this method
     * returns {@code null} after the class is loaded. </p>
     *
     * <p> This method does not check whether the requested class is
     * accessible to its caller. </p>
     *
     * @apiNote
     * This method returns {@code null} on failure rather than
     * throwing a {@link ClassNotFoundException}, as is done by
     * the {@link #forName(String, boolean, ClassLoader)} method.
     * The security check is a stack-based permission check if the caller
     * loads a class in another module.
     *
     * @param  module   A module
     * @param  name     The <a href="ClassLoader.html#binary-name">binary name</a>
     *                  of the class
     * @return {@code Class} object of the given name defined in the given module;
     *         {@code null} if not found.
     *
     * @throws NullPointerException if the given module or name is {@code null}
     *
     * @throws LinkageError if the linkage fails
     *
     * @throws SecurityException
     *         <ul>
     *         <li> if the caller is not the specified module and
     *         {@code RuntimePermission("getClassLoader")} permission is denied; or</li>
     *         <li> access to the module content is denied. For example,
     *         permission check will be performed when a class loader calls
     *         {@link ModuleReader#open(String)} to read the bytes of a class file
     *         in a module.</li>
     *         </ul>
     *
     * @jls 12.2 Loading of Classes and Interfaces
     * @jls 12.3 Linking of Classes and Interfaces
     * @since 9
     */
    @SuppressWarnings("removal")
    @CallerSensitive
    public static Class<?> forName(Module module, String name) {
        Objects.requireNonNull(module);
        Objects.requireNonNull(name);

        ClassLoader cl;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Class<?> caller = Reflection.getCallerClass();
            if (caller != null && caller.getModule() != module) {
                // if caller is null, Class.forName is the last java frame on the stack.
                // java.base has all permissions
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
            PrivilegedAction<ClassLoader> pa = module::getClassLoader;
            cl = AccessController.doPrivileged(pa);
        } else {
            cl = module.getClassLoader();
        }

        if (cl != null) {
            return cl.loadClass(module, name);
        } else {
            return BootLoader.loadClass(module, name);
        }
    }

    /**
     * Creates a new instance of the class represented by this {@code Class}
     * object.  The class is instantiated as if by a {@code new}
     * expression with an empty argument list.  The class is initialized if it
     * has not already been initialized.
     *
     * @deprecated This method propagates any exception thrown by the
     * nullary constructor, including a checked exception.  Use of
     * this method effectively bypasses the compile-time exception
     * checking that would otherwise be performed by the compiler.
     * The {@link
     * java.lang.reflect.Constructor#newInstance(java.lang.Object...)
     * Constructor.newInstance} method avoids this problem by wrapping
     * any exception thrown by the constructor in a (checked) {@link
     * java.lang.reflect.InvocationTargetException}.
     *
     * <p>The call
     *
     * <pre>{@code
     * clazz.newInstance()
     * }</pre>
     *
     * can be replaced by
     *
     * <pre>{@code
     * clazz.getDeclaredConstructor().newInstance()
     * }</pre>
     *
     * The latter sequence of calls is inferred to be able to throw
     * the additional exception types {@link
     * InvocationTargetException} and {@link
     * NoSuchMethodException}. Both of these exception types are
     * subclasses of {@link ReflectiveOperationException}.
     *
     * @return  a newly allocated instance of the class represented by this
     *          object.
     * @throws  IllegalAccessException  if the class or its nullary
     *          constructor is not accessible.
     * @throws  InstantiationException
     *          if this {@code Class} represents an abstract class,
     *          an interface, an array class, a primitive type, or void;
     *          or if the class has no nullary constructor;
     *          or if the instantiation fails for some other reason.
     * @throws  ExceptionInInitializerError if the initialization
     *          provoked by this method fails.
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and
     *          the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class.
     */
    @SuppressWarnings("removal")
    @CallerSensitive
    @Deprecated(since="9")
    public T newInstance()
        throws InstantiationException, IllegalAccessException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), false);
        }

        // Constructor lookup
        Constructor<T> tmpConstructor = cachedConstructor;
        if (tmpConstructor == null) {
            if (this == Class.class) {
                throw new IllegalAccessException(
                    "Can not call newInstance() on the Class for java.lang.Class"
                );
            }
            try {
                Class<?>[] empty = {};
                final Constructor<T> c = getReflectionFactory().copyConstructor(
                    getConstructor0(empty, Member.DECLARED));
                // Disable accessibility checks on the constructor
                // access check is done with the true caller
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<>() {
                        public Void run() {
                                c.setAccessible(true);
                                return null;
                            }
                        });
                cachedConstructor = tmpConstructor = c;
            } catch (NoSuchMethodException e) {
                throw (InstantiationException)
                    new InstantiationException(getName()).initCause(e);
            }
        }

        try {
            Class<?> caller = Reflection.getCallerClass();
            return getReflectionFactory().newInstance(tmpConstructor, null, caller);
        } catch (InvocationTargetException e) {
            Unsafe.getUnsafe().throwException(e.getTargetException());
            // Not reached
            return null;
        }
    }

    private transient volatile Constructor<T> cachedConstructor;

    /**
     * Determines if the specified {@code Object} is assignment-compatible
     * with the object represented by this {@code Class}.  This method is
     * the dynamic equivalent of the Java language {@code instanceof}
     * operator. The method returns {@code true} if the specified
     * {@code Object} argument is non-null and can be cast to the
     * reference type represented by this {@code Class} object without
     * raising a {@code ClassCastException.} It returns {@code false}
     * otherwise.
     *
     * <p> Specifically, if this {@code Class} object represents a
     * declared class, this method returns {@code true} if the specified
     * {@code Object} argument is an instance of the represented class (or
     * of any of its subclasses); it returns {@code false} otherwise. If
     * this {@code Class} object represents an array class, this method
     * returns {@code true} if the specified {@code Object} argument
     * can be converted to an object of the array class by an identity
     * conversion or by a widening reference conversion; it returns
     * {@code false} otherwise. If this {@code Class} object
     * represents an interface, this method returns {@code true} if the
     * class or any superclass of the specified {@code Object} argument
     * implements this interface; it returns {@code false} otherwise. If
     * this {@code Class} object represents a primitive type, this method
     * returns {@code false}.
     *
     * @param   obj the object to check
     * @return  true if {@code obj} is an instance of this class
     *
     * @since 1.1
     */
    @IntrinsicCandidate
    public native boolean isInstance(Object obj);


    /**
     * Determines if the class or interface represented by this
     * {@code Class} object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * {@code Class} parameter. It returns {@code true} if so;
     * otherwise it returns {@code false}. If this {@code Class}
     * object represents a primitive type, this method returns
     * {@code true} if the specified {@code Class} parameter is
     * exactly this {@code Class} object; otherwise it returns
     * {@code false}.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * or via a widening reference conversion. See <cite>The Java Language
     * Specification</cite>, sections {@jls 5.1.1} and {@jls 5.1.4},
     * for details.
     *
     * @param     cls the {@code Class} object to be checked
     * @return    the {@code boolean} value indicating whether objects of the
     *            type {@code cls} can be assigned to objects of this class
     * @throws    NullPointerException if the specified Class parameter is
     *            null.
     * @since     1.1
     */
    @IntrinsicCandidate
    public native boolean isAssignableFrom(Class<?> cls);


    /**
     * Determines if this {@code Class} object represents an
     * interface type.
     *
     * @return  {@code true} if this {@code Class} object represents an interface;
     *          {@code false} otherwise.
     */
    @IntrinsicCandidate
    public native boolean isInterface();


    /**
     * Determines if this {@code Class} object represents an array class.
     *
     * @return  {@code true} if this {@code Class} object represents an array class;
     *          {@code false} otherwise.
     * @since   1.1
     */
    @IntrinsicCandidate
    public native boolean isArray();


    /**
     * Determines if the specified {@code Class} object represents a
     * primitive type.
     *
     * <p> There are nine predefined {@code Class} objects to represent
     * the eight primitive types and void.  These are created by the Java
     * Virtual Machine, and have the same names as the primitive types that
     * they represent, namely {@code boolean}, {@code byte},
     * {@code char}, {@code short}, {@code int},
     * {@code long}, {@code float}, and {@code double}.
     *
     * <p> These objects may only be accessed via the following public static
     * final variables, and are the only {@code Class} objects for which
     * this method returns {@code true}.
     *
     * @return true if and only if this class represents a primitive type
     *
     * @see     java.lang.Boolean#TYPE
     * @see     java.lang.Character#TYPE
     * @see     java.lang.Byte#TYPE
     * @see     java.lang.Short#TYPE
     * @see     java.lang.Integer#TYPE
     * @see     java.lang.Long#TYPE
     * @see     java.lang.Float#TYPE
     * @see     java.lang.Double#TYPE
     * @see     java.lang.Void#TYPE
     * @since 1.1
     */
    @IntrinsicCandidate
    public native boolean isPrimitive();

    /**
     * Returns true if this {@code Class} object represents an annotation
     * interface.  Note that if this method returns true, {@link #isInterface()}
     * would also return true, as all annotation interfaces are also interfaces.
     *
     * @return {@code true} if this {@code Class} object represents an annotation
     *      interface; {@code false} otherwise
     * @since 1.5
     */
    public boolean isAnnotation() {
        return (getModifiers() & ANNOTATION) != 0;
    }

    /**
     *{@return {@code true} if and only if this class has the synthetic modifier
     * bit set}
     *
     * @jls 13.1 The Form of a Binary
     * @jvms 4.1 The {@code ClassFile} Structure
     * @see <a
     * href="{@docRoot}/java.base/java/lang/reflect/package-summary.html#LanguageJvmModel">Java
     * programming language and JVM modeling in core reflection</a>
     * @since 1.5
     */
    public boolean isSynthetic() {
        return (getModifiers() & SYNTHETIC) != 0;
    }

    /**
     * Returns the  name of the entity (class, interface, array class,
     * primitive type, or void) represented by this {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface,
     * not an array class, then:
     * <ul>
     * <li> If the class or interface is not {@linkplain #isHidden() hidden},
     *      then the <a href="ClassLoader.html#binary-name">binary name</a>
     *      of the class or interface is returned.
     * <li> If the class or interface is hidden, then the result is a string
     *      of the form: {@code N + '/' + <suffix>}
     *      where {@code N} is the <a href="ClassLoader.html#binary-name">binary name</a>
     *      indicated by the {@code class} file passed to
     *      {@link java.lang.invoke.MethodHandles.Lookup#defineHiddenClass(byte[], boolean, MethodHandles.Lookup.ClassOption...)
     *      Lookup::defineHiddenClass}, and {@code <suffix>} is an unqualified name.
     * </ul>
     *
     * <p> If this {@code Class} object represents an array class, then
     * the result is a string consisting of one or more '{@code [}' characters
     * representing the depth of the array nesting, followed by the element
     * type as encoded using the following table:
     *
     * <blockquote><table class="striped">
     * <caption style="display:none">Element types and encodings</caption>
     * <thead>
     * <tr><th scope="col"> Element Type <th scope="col"> Encoding
     * </thead>
     * <tbody style="text-align:left">
     * <tr><th scope="row"> {@code boolean} <td style="text-align:center"> {@code Z}
     * <tr><th scope="row"> {@code byte}    <td style="text-align:center"> {@code B}
     * <tr><th scope="row"> {@code char}    <td style="text-align:center"> {@code C}
     * <tr><th scope="row"> class or interface with <a href="ClassLoader.html#binary-name">binary name</a> <i>N</i>
     *                                      <td style="text-align:center"> {@code L}<em>N</em>{@code ;}
     * <tr><th scope="row"> {@code double}  <td style="text-align:center"> {@code D}
     * <tr><th scope="row"> {@code float}   <td style="text-align:center"> {@code F}
     * <tr><th scope="row"> {@code int}     <td style="text-align:center"> {@code I}
     * <tr><th scope="row"> {@code long}    <td style="text-align:center"> {@code J}
     * <tr><th scope="row"> {@code short}   <td style="text-align:center"> {@code S}
     * </tbody>
     * </table></blockquote>
     *
     * <p> If this {@code Class} object represents a primitive type or {@code void},
     * then the result is a string with the same spelling as the Java language
     * keyword which corresponds to the primitive type or {@code void}.
     *
     * <p> Examples:
     * <blockquote><pre>
     * String.class.getName()
     *     returns "java.lang.String"
     * byte.class.getName()
     *     returns "byte"
     * (new Object[3]).getClass().getName()
     *     returns "[Ljava.lang.Object;"
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     *     returns "[[[[[[[I"
     * </pre></blockquote>
     *
     * @return  the name of the class, interface, or other entity
     *          represented by this {@code Class} object.
     * @jls 13.1 The Form of a Binary
     */
    public String getName() {
        String name = this.name;
        return name != null ? name : initClassName();
    }

    // Cache the name to reduce the number of calls into the VM.
    // This field would be set by VM itself during initClassName call.
    private transient String name;
    private native String initClassName();

    /**
     * Returns the class loader for the class.  Some implementations may use
     * null to represent the bootstrap class loader. This method will return
     * null in such implementations if this class was loaded by the bootstrap
     * class loader.
     *
     * <p>If this {@code Class} object
     * represents a primitive type or void, null is returned.
     *
     * @return  the class loader that loaded the class or interface
     *          represented by this {@code Class} object.
     * @throws  SecurityException
     *          if a security manager is present, and the caller's class loader
     *          is not {@code null} and is not the same as or an ancestor of the
     *          class loader for the class whose class loader is requested,
     *          and the caller does not have the
     *          {@link RuntimePermission}{@code ("getClassLoader")}
     * @see java.lang.ClassLoader
     * @see SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     */
    @CallerSensitive
    @ForceInline // to ensure Reflection.getCallerClass optimization
    public ClassLoader getClassLoader() {
        ClassLoader cl = classLoader;
        if (cl == null)
            return null;
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ClassLoader.checkClassLoaderPermission(cl, Reflection.getCallerClass());
        }
        return cl;
    }

    // Package-private to allow ClassLoader access
    ClassLoader getClassLoader0() { return classLoader; }

    /**
     * Returns the module that this class or interface is a member of.
     *
     * If this class represents an array type then this method returns the
     * {@code Module} for the element type. If this class represents a
     * primitive type or void, then the {@code Module} object for the
     * {@code java.base} module is returned.
     *
     * If this class is in an unnamed module then the {@linkplain
     * ClassLoader#getUnnamedModule() unnamed} {@code Module} of the class
     * loader for this class is returned.
     *
     * @return the module that this class or interface is a member of
     *
     * @since 9
     */
    public Module getModule() {
        return module;
    }

    // set by VM
    private transient Module module;

    // Initialized in JVM not by private constructor
    // This field is filtered from reflection access, i.e. getDeclaredField
    // will throw NoSuchFieldException
    private final ClassLoader classLoader;

    // Set by VM
    private transient Object classData;

    // package-private
    Object getClassData() {
        return classData;
    }

    /**
     * Returns an array of {@code TypeVariable} objects that represent the
     * type variables declared by the generic declaration represented by this
     * {@code GenericDeclaration} object, in declaration order.  Returns an
     * array of length 0 if the underlying generic declaration declares no type
     * variables.
     *
     * @return an array of {@code TypeVariable} objects that represent
     *     the type variables declared by this generic declaration
     * @throws java.lang.reflect.GenericSignatureFormatError if the generic
     *     signature of this generic declaration does not conform to
     *     the format specified in section {@jvms 4.7.9} of
     *     <cite>The Java Virtual Machine Specification</cite>
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public TypeVariable<Class<T>>[] getTypeParameters() {
        ClassRepository info = getGenericInfo();
        if (info != null)
            return (TypeVariable<Class<T>>[])info.getTypeParameters();
        else
            return (TypeVariable<Class<T>>[])new TypeVariable<?>[0];
    }


    /**
     * Returns the {@code Class} representing the direct superclass of the
     * entity (class, interface, primitive type or void) represented by
     * this {@code Class}.  If this {@code Class} represents either the
     * {@code Object} class, an interface, a primitive type, or void, then
     * null is returned.  If this {@code Class} object represents an array class
     * then the {@code Class} object representing the {@code Object} class is
     * returned.
     *
     * @return the direct superclass of the class represented by this {@code Class} object
     */
    @IntrinsicCandidate
    public native Class<? super T> getSuperclass();


    /**
     * Returns the {@code Type} representing the direct superclass of
     * the entity (class, interface, primitive type or void) represented by
     * this {@code Class} object.
     *
     * <p>If the superclass is a parameterized type, the {@code Type}
     * object returned must accurately reflect the actual type
     * arguments used in the source code. The parameterized type
     * representing the superclass is created if it had not been
     * created before. See the declaration of {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * semantics of the creation process for parameterized types.  If
     * this {@code Class} object represents either the {@code Object}
     * class, an interface, a primitive type, or void, then null is
     * returned.  If this {@code Class} object represents an array class
     * then the {@code Class} object representing the {@code Object} class is
     * returned.
     *
     * @throws java.lang.reflect.GenericSignatureFormatError if the generic
     *     class signature does not conform to the format specified in
     *     section {@jvms 4.7.9} of <cite>The Java Virtual
     *     Machine Specification</cite>
     * @throws TypeNotPresentException if the generic superclass
     *     refers to a non-existent type declaration
     * @throws java.lang.reflect.MalformedParameterizedTypeException if the
     *     generic superclass refers to a parameterized type that cannot be
     *     instantiated  for any reason
     * @return the direct superclass of the class represented by this {@code Class} object
     * @since 1.5
     */
    public Type getGenericSuperclass() {
        ClassRepository info = getGenericInfo();
        if (info == null) {
            return getSuperclass();
        }

        // Historical irregularity:
        // Generic signature marks interfaces with superclass = Object
        // but this API returns null for interfaces
        if (isInterface()) {
            return null;
        }

        return info.getSuperclass();
    }

    /**
     * Gets the package of this class.
     *
     * <p>If this class represents an array type, a primitive type or void,
     * this method returns {@code null}.
     *
     * @return the package of this class.
     * @revised 9
     */
    public Package getPackage() {
        if (isPrimitive() || isArray()) {
            return null;
        }
        ClassLoader cl = classLoader;
        return cl != null ? cl.definePackage(this)
                          : BootLoader.definePackage(this);
    }

    /**
     * Returns the fully qualified package name.
     *
     * <p> If this class is a top level class, then this method returns the fully
     * qualified name of the package that the class is a member of, or the
     * empty string if the class is in an unnamed package.
     *
     * <p> If this class is a member class, then this method is equivalent to
     * invoking {@code getPackageName()} on the {@linkplain #getEnclosingClass
     * enclosing class}.
     *
     * <p> If this class is a {@linkplain #isLocalClass local class} or an {@linkplain
     * #isAnonymousClass() anonymous class}, then this method is equivalent to
     * invoking {@code getPackageName()} on the {@linkplain #getDeclaringClass
     * declaring class} of the {@linkplain #getEnclosingMethod enclosing method} or
     * {@linkplain #getEnclosingConstructor enclosing constructor}.
     *
     * <p> If this class represents an array type then this method returns the
     * package name of the element type. If this class represents a primitive
     * type or void then the package name "{@code java.lang}" is returned.
     *
     * @return the fully qualified package name
     *
     * @since 9
     * @jls 6.7 Fully Qualified Names
     */
    public String getPackageName() {
        String pn = this.packageName;
        if (pn == null) {
            Class<?> c = isArray() ? elementType() : this;
            if (c.isPrimitive()) {
                pn = "java.lang";
            } else {
                String cn = c.getName();
                int dot = cn.lastIndexOf('.');
                pn = (dot != -1) ? cn.substring(0, dot).intern() : "";
            }
            this.packageName = pn;
        }
        return pn;
    }

    // cached package name
    private transient String packageName;

    /**
     * Returns the interfaces directly implemented by the class or interface
     * represented by this {@code Class} object.
     *
     * <p>If this {@code Class} object represents a class, the return value is an array
     * containing objects representing all interfaces directly implemented by
     * the class.  The order of the interface objects in the array corresponds
     * to the order of the interface names in the {@code implements} clause of
     * the declaration of the class represented by this {@code Class} object.  For example,
     * given the declaration:
     * <blockquote>
     * {@code class Shimmer implements FloorWax, DessertTopping { ... }}
     * </blockquote>
     * suppose the value of {@code s} is an instance of
     * {@code Shimmer}; the value of the expression:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[0]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code FloorWax}; and the value of:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[1]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code DessertTopping}.
     *
     * <p>If this {@code Class} object represents an interface, the array contains objects
     * representing all interfaces directly extended by the interface.  The
     * order of the interface objects in the array corresponds to the order of
     * the interface names in the {@code extends} clause of the declaration of
     * the interface represented by this {@code Class} object.
     *
     * <p>If this {@code Class} object represents a class or interface that implements no
     * interfaces, the method returns an array of length 0.
     *
     * <p>If this {@code Class} object represents a primitive type or void, the method
     * returns an array of length 0.
     *
     * <p>If this {@code Class} object represents an array type, the
     * interfaces {@code Cloneable} and {@code java.io.Serializable} are
     * returned in that order.
     *
     * @return an array of interfaces directly implemented by this class
     */
    public Class<?>[] getInterfaces() {
        // defensively copy before handing over to user code
        return getInterfaces(true);
    }

    private Class<?>[] getInterfaces(boolean cloneArray) {
        ReflectionData<T> rd = reflectionData();
        if (rd == null) {
            // no cloning required
            return getInterfaces0();
        } else {
            Class<?>[] interfaces = rd.interfaces;
            if (interfaces == null) {
                interfaces = getInterfaces0();
                rd.interfaces = interfaces;
            }
            // defensively copy if requested
            return cloneArray ? interfaces.clone() : interfaces;
        }
    }

    private native Class<?>[] getInterfaces0();

    /**
     * Returns the {@code Type}s representing the interfaces
     * directly implemented by the class or interface represented by
     * this {@code Class} object.
     *
     * <p>If a superinterface is a parameterized type, the
     * {@code Type} object returned for it must accurately reflect
     * the actual type arguments used in the source code. The
     * parameterized type representing each superinterface is created
     * if it had not been created before. See the declaration of
     * {@link java.lang.reflect.ParameterizedType ParameterizedType}
     * for the semantics of the creation process for parameterized
     * types.
     *
     * <p>If this {@code Class} object represents a class, the return value is an array
     * containing objects representing all interfaces directly implemented by
     * the class.  The order of the interface objects in the array corresponds
     * to the order of the interface names in the {@code implements} clause of
     * the declaration of the class represented by this {@code Class} object.
     *
     * <p>If this {@code Class} object represents an interface, the array contains objects
     * representing all interfaces directly extended by the interface.  The
     * order of the interface objects in the array corresponds to the order of
     * the interface names in the {@code extends} clause of the declaration of
     * the interface represented by this {@code Class} object.
     *
     * <p>If this {@code Class} object represents a class or interface that implements no
     * interfaces, the method returns an array of length 0.
     *
     * <p>If this {@code Class} object represents a primitive type or void, the method
     * returns an array of length 0.
     *
     * <p>If this {@code Class} object represents an array type, the
     * interfaces {@code Cloneable} and {@code java.io.Serializable} are
     * returned in that order.
     *
     * @throws java.lang.reflect.GenericSignatureFormatError
     *     if the generic class signature does not conform to the
     *     format specified in section {@jvms 4.7.9} of <cite>The
     *     Java Virtual Machine Specification</cite>
     * @throws TypeNotPresentException if any of the generic
     *     superinterfaces refers to a non-existent type declaration
     * @throws java.lang.reflect.MalformedParameterizedTypeException
     *     if any of the generic superinterfaces refer to a parameterized
     *     type that cannot be instantiated for any reason
     * @return an array of interfaces directly implemented by this class
     * @since 1.5
     */
    public Type[] getGenericInterfaces() {
        ClassRepository info = getGenericInfo();
        return (info == null) ?  getInterfaces() : info.getSuperInterfaces();
    }


    /**
     * Returns the {@code Class} representing the component type of an
     * array.  If this class does not represent an array class this method
     * returns null.
     *
     * @return the {@code Class} representing the component type of this
     * class if this class is an array
     * @see     java.lang.reflect.Array
     * @since 1.1
     */
    public Class<?> getComponentType() {
        // Only return for array types. Storage may be reused for Class for instance types.
        if (isArray()) {
            return componentType;
        } else {
            return null;
        }
    }

    private final Class<?> componentType;

    /*
     * Returns the {@code Class} representing the element type of an array class.
     * If this class does not represent an array class, then this method returns
     * {@code null}.
     */
    private Class<?> elementType() {
        if (!isArray()) return null;

        Class<?> c = this;
        while (c.isArray()) {
            c = c.getComponentType();
        }
        return c;
    }

    /**
     * Returns the Java language modifiers for this class or interface, encoded
     * in an integer. The modifiers consist of the Java Virtual Machine's
     * constants for {@code public}, {@code protected},
     * {@code private}, {@code final}, {@code static},
     * {@code abstract} and {@code interface}; they should be decoded
     * using the methods of class {@code Modifier}.
     *
     * <p> If the underlying class is an array class, then its
     * {@code public}, {@code private} and {@code protected}
     * modifiers are the same as those of its component type.  If this
     * {@code Class} object represents a primitive type or void, its
     * {@code public} modifier is always {@code true}, and its
     * {@code protected} and {@code private} modifiers are always
     * {@code false}. If this {@code Class} object represents an array class, a
     * primitive type or void, then its {@code final} modifier is always
     * {@code true} and its interface modifier is always
     * {@code false}. The values of its other modifiers are not determined
     * by this specification.
     *
     * <p> The modifier encodings are defined in section {@jvms 4.1}
     * of <cite>The Java Virtual Machine Specification</cite>.
     *
     * @return the {@code int} representing the modifiers for this class
     * @see     java.lang.reflect.Modifier
     * @see <a
     * href="{@docRoot}/java.base/java/lang/reflect/package-summary.html#LanguageJvmModel">Java
     * programming language and JVM modeling in core reflection</a>
     * @since 1.1
     * @jls 8.1.1 Class Modifiers
     * @jls 9.1.1. Interface Modifiers
     */
    @IntrinsicCandidate
    public native int getModifiers();


    /**
     * Gets the signers of this class.
     *
     * @return  the signers of this class, or null if there are no signers.  In
     *          particular, this method returns null if this {@code Class} object represents
     *          a primitive type or void.
     * @since   1.1
     */
    public native Object[] getSigners();


    /**
     * Set the signers of this class.
     */
    native void setSigners(Object[] signers);


    /**
     * If this {@code Class} object represents a local or anonymous
     * class within a method, returns a {@link
     * java.lang.reflect.Method Method} object representing the
     * immediately enclosing method of the underlying class. Returns
     * {@code null} otherwise.
     *
     * In particular, this method returns {@code null} if the underlying
     * class is a local or anonymous class immediately enclosed by a class or
     * interface declaration, instance initializer or static initializer.
     *
     * @return the immediately enclosing method of the underlying class, if
     *     that class is a local or anonymous class; otherwise {@code null}.
     *
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and any of the
     *         following conditions is met:
     *
     *         <ul>
     *
     *         <li> the caller's class loader is not the same as the
     *         class loader of the enclosing class and invocation of
     *         {@link SecurityManager#checkPermission
     *         s.checkPermission} method with
     *         {@code RuntimePermission("accessDeclaredMembers")}
     *         denies access to the methods within the enclosing class
     *
     *         <li> the caller's class loader is not the same as or an
     *         ancestor of the class loader for the enclosing class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of the enclosing class
     *
     *         </ul>
     * @since 1.5
     */
    @CallerSensitive
    public Method getEnclosingMethod() throws SecurityException {
        EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();

        if (enclosingInfo == null)
            return null;
        else {
            if (!enclosingInfo.isMethod())
                return null;

            MethodRepository typeInfo = MethodRepository.make(enclosingInfo.getDescriptor(),
                                                              getFactory());
            Class<?>   returnType       = toClass(typeInfo.getReturnType());
            Type []    parameterTypes   = typeInfo.getParameterTypes();
            Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

            // Convert Types to Classes; returned types *should*
            // be class objects since the methodDescriptor's used
            // don't have generics information
            for(int i = 0; i < parameterClasses.length; i++)
                parameterClasses[i] = toClass(parameterTypes[i]);

            // Perform access check
            final Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkMemberAccess(sm, Member.DECLARED,
                                                     Reflection.getCallerClass(), true);
            }
            Method[] candidates = enclosingCandidate.privateGetDeclaredMethods(false);

            /*
             * Loop over all declared methods; match method name,
             * number of and type of parameters, *and* return
             * type.  Matching return type is also necessary
             * because of covariant returns, etc.
             */
            ReflectionFactory fact = getReflectionFactory();
            for (Method m : candidates) {
                if (m.getName().equals(enclosingInfo.getName()) &&
                    arrayContentsEq(parameterClasses,
                                    fact.getExecutableSharedParameterTypes(m))) {
                    // finally, check return type
                    if (m.getReturnType().equals(returnType)) {
                        return fact.copyMethod(m);
                    }
                }
            }

            throw new InternalError("Enclosing method not found");
        }
    }

    private native Object[] getEnclosingMethod0();

    private EnclosingMethodInfo getEnclosingMethodInfo() {
        Object[] enclosingInfo = getEnclosingMethod0();
        if (enclosingInfo == null)
            return null;
        else {
            return new EnclosingMethodInfo(enclosingInfo);
        }
    }

    private static final class EnclosingMethodInfo {
        private final Class<?> enclosingClass;
        private final String name;
        private final String descriptor;

        static void validate(Object[] enclosingInfo) {
            if (enclosingInfo.length != 3)
                throw new InternalError("Malformed enclosing method information");
            try {
                // The array is expected to have three elements:

                // the immediately enclosing class
                Class<?> enclosingClass = (Class<?>)enclosingInfo[0];
                assert(enclosingClass != null);

                // the immediately enclosing method or constructor's
                // name (can be null).
                String name = (String)enclosingInfo[1];

                // the immediately enclosing method or constructor's
                // descriptor (null iff name is).
                String descriptor = (String)enclosingInfo[2];
                assert((name != null && descriptor != null) || name == descriptor);
            } catch (ClassCastException cce) {
                throw new InternalError("Invalid type in enclosing method information", cce);
            }
        }

        EnclosingMethodInfo(Object[] enclosingInfo) {
            validate(enclosingInfo);
            this.enclosingClass = (Class<?>)enclosingInfo[0];
            this.name = (String)enclosingInfo[1];
            this.descriptor = (String)enclosingInfo[2];
        }

        boolean isPartial() {
            return enclosingClass == null || name == null || descriptor == null;
        }

        boolean isConstructor() { return !isPartial() && "<init>".equals(name); }

        boolean isMethod() { return !isPartial() && !isConstructor() && !"<clinit>".equals(name); }

        Class<?> getEnclosingClass() { return enclosingClass; }

        String getName() { return name; }

        String getDescriptor() { return descriptor; }

    }

    private static Class<?> toClass(Type o) {
        if (o instanceof GenericArrayType)
            return Array.newInstance(toClass(((GenericArrayType)o).getGenericComponentType()),
                                     0)
                .getClass();
        return (Class<?>)o;
     }

    /**
     * If this {@code Class} object represents a local or anonymous
     * class within a constructor, returns a {@link
     * java.lang.reflect.Constructor Constructor} object representing
     * the immediately enclosing constructor of the underlying
     * class. Returns {@code null} otherwise.  In particular, this
     * method returns {@code null} if the underlying class is a local
     * or anonymous class immediately enclosed by a class or
     * interface declaration, instance initializer or static initializer.
     *
     * @return the immediately enclosing constructor of the underlying class, if
     *     that class is a local or anonymous class; otherwise {@code null}.
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and any of the
     *         following conditions is met:
     *
     *         <ul>
     *
     *         <li> the caller's class loader is not the same as the
     *         class loader of the enclosing class and invocation of
     *         {@link SecurityManager#checkPermission
     *         s.checkPermission} method with
     *         {@code RuntimePermission("accessDeclaredMembers")}
     *         denies access to the constructors within the enclosing class
     *
     *         <li> the caller's class loader is not the same as or an
     *         ancestor of the class loader for the enclosing class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of the enclosing class
     *
     *         </ul>
     * @since 1.5
     */
    @CallerSensitive
    public Constructor<?> getEnclosingConstructor() throws SecurityException {
        EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();

        if (enclosingInfo == null)
            return null;
        else {
            if (!enclosingInfo.isConstructor())
                return null;

            ConstructorRepository typeInfo = ConstructorRepository.make(enclosingInfo.getDescriptor(),
                                                                        getFactory());
            Type []    parameterTypes   = typeInfo.getParameterTypes();
            Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

            // Convert Types to Classes; returned types *should*
            // be class objects since the methodDescriptor's used
            // don't have generics information
            for(int i = 0; i < parameterClasses.length; i++)
                parameterClasses[i] = toClass(parameterTypes[i]);

            // Perform access check
            final Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkMemberAccess(sm, Member.DECLARED,
                                                     Reflection.getCallerClass(), true);
            }

            Constructor<?>[] candidates = enclosingCandidate
                    .privateGetDeclaredConstructors(false);
            /*
             * Loop over all declared constructors; match number
             * of and type of parameters.
             */
            ReflectionFactory fact = getReflectionFactory();
            for (Constructor<?> c : candidates) {
                if (arrayContentsEq(parameterClasses,
                                    fact.getExecutableSharedParameterTypes(c))) {
                    return fact.copyConstructor(c);
                }
            }

            throw new InternalError("Enclosing constructor not found");
        }
    }


    /**
     * If the class or interface represented by this {@code Class} object
     * is a member of another class, returns the {@code Class} object
     * representing the class in which it was declared.  This method returns
     * null if this class or interface is not a member of any other class.  If
     * this {@code Class} object represents an array class, a primitive
     * type, or void,then this method returns null.
     *
     * @return the declaring class for this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and the caller's
     *         class loader is not the same as or an ancestor of the class
     *         loader for the declaring class and invocation of {@link
     *         SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *         denies access to the package of the declaring class
     * @since 1.1
     */
    @CallerSensitive
    public Class<?> getDeclaringClass() throws SecurityException {
        final Class<?> candidate = getDeclaringClass0();

        if (candidate != null) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                candidate.checkPackageAccess(sm,
                    ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return candidate;
    }

    private native Class<?> getDeclaringClass0();


    /**
     * Returns the immediately enclosing class of the underlying
     * class.  If the underlying class is a top level class this
     * method returns {@code null}.
     * @return the immediately enclosing class of the underlying class
     * @throws     SecurityException
     *             If a security manager, <i>s</i>, is present and the caller's
     *             class loader is not the same as or an ancestor of the class
     *             loader for the enclosing class and invocation of {@link
     *             SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *             denies access to the package of the enclosing class
     * @since 1.5
     */
    @CallerSensitive
    public Class<?> getEnclosingClass() throws SecurityException {
        // There are five kinds of classes (or interfaces):
        // a) Top level classes
        // b) Nested classes (static member classes)
        // c) Inner classes (non-static member classes)
        // d) Local classes (named classes declared within a method)
        // e) Anonymous classes


        // JVM Spec 4.7.7: A class must have an EnclosingMethod
        // attribute if and only if it is a local class or an
        // anonymous class.
        EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();
        Class<?> enclosingCandidate;

        if (enclosingInfo == null) {
            // This is a top level or a nested class or an inner class (a, b, or c)
            enclosingCandidate = getDeclaringClass0();
        } else {
            Class<?> enclosingClass = enclosingInfo.getEnclosingClass();
            // This is a local class or an anonymous class (d or e)
            if (enclosingClass == this || enclosingClass == null)
                throw new InternalError("Malformed enclosing method information");
            else
                enclosingCandidate = enclosingClass;
        }

        if (enclosingCandidate != null) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                enclosingCandidate.checkPackageAccess(sm,
                    ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return enclosingCandidate;
    }

    /**
     * Returns the simple name of the underlying class as given in the
     * source code. An empty string is returned if the underlying class is
     * {@linkplain #isAnonymousClass() anonymous}.
     * A {@linkplain #isSynthetic() synthetic class}, one not present
     * in source code, can have a non-empty name including special
     * characters, such as "{@code $}".
     *
     * <p>The simple name of an {@linkplain #isArray() array class} is the simple name of the
     * component type with "[]" appended.  In particular the simple
     * name of an array class whose component type is anonymous is "[]".
     *
     * @return the simple name of the underlying class
     * @since 1.5
     */
    public String getSimpleName() {
        ReflectionData<T> rd = reflectionData();
        String simpleName = rd.simpleName;
        if (simpleName == null) {
            rd.simpleName = simpleName = getSimpleName0();
        }
        return simpleName;
    }

    private String getSimpleName0() {
        if (isArray()) {
            return getComponentType().getSimpleName().concat("[]");
        }
        String simpleName = getSimpleBinaryName();
        if (simpleName == null) { // top level class
            simpleName = getName();
            simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1); // strip the package name
        }
        return simpleName;
    }

    /**
     * Return an informative string for the name of this class or interface.
     *
     * @return an informative string for the name of this class or interface
     * @since 1.8
     */
    public String getTypeName() {
        if (isArray()) {
            try {
                Class<?> cl = this;
                int dimensions = 0;
                do {
                    dimensions++;
                    cl = cl.getComponentType();
                } while (cl.isArray());
                return cl.getName().concat("[]".repeat(dimensions));
            } catch (Throwable e) { /*FALLTHRU*/ }
        }
        return getName();
    }

    /**
     * Returns the canonical name of the underlying class as
     * defined by <cite>The Java Language Specification</cite>.
     * Returns {@code null} if the underlying class does not have a canonical
     * name. Classes without canonical names include:
     * <ul>
     * <li>a {@linkplain #isLocalClass() local class}
     * <li>a {@linkplain #isAnonymousClass() anonymous class}
     * <li>a {@linkplain #isHidden() hidden class}
     * <li>an array whose component type does not have a canonical name</li>
     * </ul>
     *
     * @return the canonical name of the underlying class if it exists, and
     * {@code null} otherwise.
     * @since 1.5
     */
    public String getCanonicalName() {
        ReflectionData<T> rd = reflectionData();
        String canonicalName = rd.canonicalName;
        if (canonicalName == null) {
            rd.canonicalName = canonicalName = getCanonicalName0();
        }
        return canonicalName == ReflectionData.NULL_SENTINEL? null : canonicalName;
    }

    private String getCanonicalName0() {
        if (isArray()) {
            String canonicalName = getComponentType().getCanonicalName();
            if (canonicalName != null)
                return canonicalName.concat("[]");
            else
                return ReflectionData.NULL_SENTINEL;
        }
        if (isHidden() || isLocalOrAnonymousClass())
            return ReflectionData.NULL_SENTINEL;
        Class<?> enclosingClass = getEnclosingClass();
        if (enclosingClass == null) { // top level class
            return getName();
        } else {
            String enclosingName = enclosingClass.getCanonicalName();
            if (enclosingName == null)
                return ReflectionData.NULL_SENTINEL;
            String simpleName = getSimpleName();
            return new StringBuilder(enclosingName.length() + simpleName.length() + 1)
                    .append(enclosingName)
                    .append('.')
                    .append(simpleName)
                    .toString();
        }
    }

    /**
     * Returns {@code true} if and only if the underlying class
     * is an anonymous class.
     *
     * @apiNote
     * An anonymous class is not a {@linkplain #isHidden() hidden class}.
     *
     * @return {@code true} if and only if this class is an anonymous class.
     * @since 1.5
     * @jls 15.9.5 Anonymous Class Declarations
     */
    public boolean isAnonymousClass() {
        return !isArray() && isLocalOrAnonymousClass() &&
                getSimpleBinaryName0() == null;
    }

    /**
     * Returns {@code true} if and only if the underlying class
     * is a local class.
     *
     * @return {@code true} if and only if this class is a local class.
     * @since 1.5
     * @jls 14.3 Local Class Declarations
     */
    public boolean isLocalClass() {
        return isLocalOrAnonymousClass() &&
                (isArray() || getSimpleBinaryName0() != null);
    }

    /**
     * Returns {@code true} if and only if the underlying class
     * is a member class.
     *
     * @return {@code true} if and only if this class is a member class.
     * @since 1.5
     * @jls 8.5 Member Type Declarations
     */
    public boolean isMemberClass() {
        return !isLocalOrAnonymousClass() && getDeclaringClass0() != null;
    }

    /**
     * Returns the "simple binary name" of the underlying class, i.e.,
     * the binary name without the leading enclosing class name.
     * Returns {@code null} if the underlying class is a top level
     * class.
     */
    private String getSimpleBinaryName() {
        if (isTopLevelClass())
            return null;
        String name = getSimpleBinaryName0();
        if (name == null) // anonymous class
            return "";
        return name;
    }

    private native String getSimpleBinaryName0();

    /**
     * Returns {@code true} if this is a top level class.  Returns {@code false}
     * otherwise.
     */
    private boolean isTopLevelClass() {
        return !isLocalOrAnonymousClass() && getDeclaringClass0() == null;
    }

    /**
     * Returns {@code true} if this is a local class or an anonymous
     * class.  Returns {@code false} otherwise.
     */
    private boolean isLocalOrAnonymousClass() {
        // JVM Spec 4.7.7: A class must have an EnclosingMethod
        // attribute if and only if it is a local class or an
        // anonymous class.
        return hasEnclosingMethodInfo();
    }

    private boolean hasEnclosingMethodInfo() {
        Object[] enclosingInfo = getEnclosingMethod0();
        if (enclosingInfo != null) {
            EnclosingMethodInfo.validate(enclosingInfo);
            return true;
        }
        return false;
    }

    /**
     * Returns an array containing {@code Class} objects representing all
     * the public classes and interfaces that are members of the class
     * represented by this {@code Class} object.  This includes public
     * class and interface members inherited from superclasses and public class
     * and interface members declared by the class.  This method returns an
     * array of length 0 if this {@code Class} object has no public member
     * classes or interfaces.  This method also returns an array of length 0 if
     * this {@code Class} object represents a primitive type, an array
     * class, or void.
     *
     * @return the array of {@code Class} objects representing the public
     *         members of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     */
    @SuppressWarnings("removal")
    @CallerSensitive
    public Class<?>[] getClasses() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), false);
        }

        // Privileged so this implementation can look at DECLARED classes,
        // something the caller might not have privilege to do.  The code here
        // is allowed to look at DECLARED classes because (1) it does not hand
        // out anything other than public members and (2) public member access
        // has already been ok'd by the SecurityManager.

        return java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<>() {
                public Class<?>[] run() {
                    List<Class<?>> list = new ArrayList<>();
                    Class<?> currentClass = Class.this;
                    while (currentClass != null) {
                        for (Class<?> m : currentClass.getDeclaredClasses()) {
                            if (Modifier.isPublic(m.getModifiers())) {
                                list.add(m);
                            }
                        }
                        currentClass = currentClass.getSuperclass();
                    }
                    return list.toArray(new Class<?>[0]);
                }
            });
    }


    /**
     * Returns an array containing {@code Field} objects reflecting all
     * the accessible public fields of the class or interface represented by
     * this {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface with
     * no accessible public fields, then this method returns an array of length
     * 0.
     *
     * <p> If this {@code Class} object represents a class, then this method
     * returns the public fields of the class and of all its superclasses and
     * superinterfaces.
     *
     * <p> If this {@code Class} object represents an interface, then this
     * method returns the fields of the interface and of all its
     * superinterfaces.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then this method returns an array of length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return the array of {@code Field} objects representing the
     *         public fields
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field[] getFields() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return copyFields(privateGetPublicFields());
    }


    /**
     * Returns an array containing {@code Method} objects reflecting all the
     * public methods of the class or interface represented by this {@code
     * Class} object, including those declared by the class or interface and
     * those inherited from superclasses and superinterfaces.
     *
     * <p> If this {@code Class} object represents an array type, then the
     * returned array has a {@code Method} object for each of the public
     * methods inherited by the array type from {@code Object}. It does not
     * contain a {@code Method} object for {@code clone()}.
     *
     * <p> If this {@code Class} object represents an interface then the
     * returned array does not contain any implicitly declared methods from
     * {@code Object}. Therefore, if no methods are explicitly declared in
     * this interface or any of its superinterfaces then the returned array
     * has length 0. (Note that a {@code Class} object which represents a class
     * always has public methods, inherited from {@code Object}.)
     *
     * <p> The returned array never contains methods with names "{@code <init>}"
     * or "{@code <clinit>}".
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * <p> Generally, the result is computed as with the following 4 step algorithm.
     * Let C be the class or interface represented by this {@code Class} object:
     * <ol>
     * <li> A union of methods is composed of:
     *   <ol type="a">
     *   <li> C's declared public instance and static methods as returned by
     *        {@link #getDeclaredMethods()} and filtered to include only public
     *        methods.</li>
     *   <li> If C is a class other than {@code Object}, then include the result
     *        of invoking this algorithm recursively on the superclass of C.</li>
     *   <li> Include the results of invoking this algorithm recursively on all
     *        direct superinterfaces of C, but include only instance methods.</li>
     *   </ol></li>
     * <li> Union from step 1 is partitioned into subsets of methods with same
     *      signature (name, parameter types) and return type.</li>
     * <li> Within each such subset only the most specific methods are selected.
     *      Let method M be a method from a set of methods with same signature
     *      and return type. M is most specific if there is no such method
     *      N != M from the same set, such that N is more specific than M.
     *      N is more specific than M if:
     *   <ol type="a">
     *   <li> N is declared by a class and M is declared by an interface; or</li>
     *   <li> N and M are both declared by classes or both by interfaces and
     *        N's declaring type is the same as or a subtype of M's declaring type
     *        (clearly, if M's and N's declaring types are the same type, then
     *        M and N are the same method).</li>
     *   </ol></li>
     * <li> The result of this algorithm is the union of all selected methods from
     *      step 3.</li>
     * </ol>
     *
     * @apiNote There may be more than one method with a particular name
     * and parameter types in a class because while the Java language forbids a
     * class to declare multiple methods with the same signature but different
     * return types, the Java virtual machine does not.  This
     * increased flexibility in the virtual machine can be used to
     * implement various language features.  For example, covariant
     * returns can be implemented with {@linkplain
     * java.lang.reflect.Method#isBridge bridge methods}; the bridge
     * method and the overriding method would have the same
     * signature but different return types.
     *
     * @return the array of {@code Method} objects representing the
     *         public methods of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method[] getMethods() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return copyMethods(privateGetPublicMethods());
    }


    /**
     * Returns an array containing {@code Constructor} objects reflecting
     * all the public constructors of the class represented by this
     * {@code Class} object.  An array of length 0 is returned if the
     * class has no public constructors, or if the class is an array class, or
     * if the class reflects a primitive type or void.
     *
     * @apiNote
     * While this method returns an array of {@code
     * Constructor<T>} objects (that is an array of constructors from
     * this class), the return type of this method is {@code
     * Constructor<?>[]} and <em>not</em> {@code Constructor<T>[]} as
     * might be expected.  This less informative return type is
     * necessary since after being returned from this method, the
     * array could be modified to hold {@code Constructor} objects for
     * different classes, which would violate the type guarantees of
     * {@code Constructor<T>[]}.
     *
     * @return the array of {@code Constructor} objects representing the
     *         public constructors of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     */
    @CallerSensitive
    public Constructor<?>[] getConstructors() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return copyConstructors(privateGetDeclaredConstructors(true));
    }


    /**
     * Returns a {@code Field} object that reflects the specified public member
     * field of the class or interface represented by this {@code Class}
     * object. The {@code name} parameter is a {@code String} specifying the
     * simple name of the desired field.
     *
     * <p> The field to be reflected is determined by the algorithm that
     * follows.  Let C be the class or interface represented by this {@code Class} object:
     *
     * <OL>
     * <LI> If C declares a public field with the name specified, that is the
     *      field to be reflected.</LI>
     * <LI> If no field was found in step 1 above, this algorithm is applied
     *      recursively to each direct superinterface of C. The direct
     *      superinterfaces are searched in the order they were declared.</LI>
     * <LI> If no field was found in steps 1 and 2 above, and C has a
     *      superclass S, then this algorithm is invoked recursively upon S.
     *      If C has no superclass, then a {@code NoSuchFieldException}
     *      is thrown.</LI>
     * </OL>
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code length} field of the array type.
     *
     * @param name the field name
     * @return the {@code Field} object of this class specified by
     *         {@code name}
     * @throws NoSuchFieldException if a field with the specified name is
     *         not found.
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field getField(String name)
        throws NoSuchFieldException, SecurityException {
        Objects.requireNonNull(name);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        Field field = getField0(name);
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        return getReflectionFactory().copyField(field);
    }


    /**
     * Returns a {@code Method} object that reflects the specified public
     * member method of the class or interface represented by this
     * {@code Class} object. The {@code name} parameter is a
     * {@code String} specifying the simple name of the desired method. The
     * {@code parameterTypes} parameter is an array of {@code Class}
     * objects that identify the method's formal parameter types, in declared
     * order. If {@code parameterTypes} is {@code null}, it is
     * treated as if it were an empty array.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method finds any public method inherited by the array type from
     * {@code Object} except method {@code clone()}.
     *
     * <p> If this {@code Class} object represents an interface then this
     * method does not find any implicitly declared method from
     * {@code Object}. Therefore, if no methods are explicitly declared in
     * this interface or any of its superinterfaces, then this method does not
     * find any method.
     *
     * <p> This method does not find any method with name "{@code <init>}" or
     * "{@code <clinit>}".
     *
     * <p> Generally, the method to be reflected is determined by the 4 step
     * algorithm that follows.
     * Let C be the class or interface represented by this {@code Class} object:
     * <ol>
     * <li> A union of methods is composed of:
     *   <ol type="a">
     *   <li> C's declared public instance and static methods as returned by
     *        {@link #getDeclaredMethods()} and filtered to include only public
     *        methods that match given {@code name} and {@code parameterTypes}</li>
     *   <li> If C is a class other than {@code Object}, then include the result
     *        of invoking this algorithm recursively on the superclass of C.</li>
     *   <li> Include the results of invoking this algorithm recursively on all
     *        direct superinterfaces of C, but include only instance methods.</li>
     *   </ol></li>
     * <li> This union is partitioned into subsets of methods with same
     *      return type (the selection of methods from step 1 also guarantees that
     *      they have the same method name and parameter types).</li>
     * <li> Within each such subset only the most specific methods are selected.
     *      Let method M be a method from a set of methods with same VM
     *      signature (return type, name, parameter types).
     *      M is most specific if there is no such method N != M from the same
     *      set, such that N is more specific than M. N is more specific than M
     *      if:
     *   <ol type="a">
     *   <li> N is declared by a class and M is declared by an interface; or</li>
     *   <li> N and M are both declared by classes or both by interfaces and
     *        N's declaring type is the same as or a subtype of M's declaring type
     *        (clearly, if M's and N's declaring types are the same type, then
     *        M and N are the same method).</li>
     *   </ol></li>
     * <li> The result of this algorithm is chosen arbitrarily from the methods
     *      with most specific return type among all selected methods from step 3.
     *      Let R be a return type of a method M from the set of all selected methods
     *      from step 3. M is a method with most specific return type if there is
     *      no such method N != M from the same set, having return type S != R,
     *      such that S is a subtype of R as determined by
     *      R.class.{@link #isAssignableFrom}(S.class).
     * </ol>
     *
     * @apiNote There may be more than one method with matching name and
     * parameter types in a class because while the Java language forbids a
     * class to declare multiple methods with the same signature but different
     * return types, the Java virtual machine does not.  This
     * increased flexibility in the virtual machine can be used to
     * implement various language features.  For example, covariant
     * returns can be implemented with {@linkplain
     * java.lang.reflect.Method#isBridge bridge methods}; the bridge
     * method and the overriding method would have the same
     * signature but different return types. This method would return the
     * overriding method as it would have a more specific return type.
     *
     * @param name the name of the method
     * @param parameterTypes the list of parameters
     * @return the {@code Method} object that matches the specified
     *         {@code name} and {@code parameterTypes}
     * @throws NoSuchMethodException if a matching method is not found
     *         or if the name is "&lt;init&gt;"or "&lt;clinit&gt;".
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method getMethod(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        Objects.requireNonNull(name);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        Method method = getMethod0(name, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(methodToString(name, parameterTypes));
        }
        return getReflectionFactory().copyMethod(method);
    }

    /**
     * Returns a {@code Constructor} object that reflects the specified
     * public constructor of the class represented by this {@code Class}
     * object. The {@code parameterTypes} parameter is an array of
     * {@code Class} objects that identify the constructor's formal
     * parameter types, in declared order.
     *
     * If this {@code Class} object represents an inner class
     * declared in a non-static context, the formal parameter types
     * include the explicit enclosing instance as the first parameter.
     *
     * <p> The constructor to reflect is the public constructor of the class
     * represented by this {@code Class} object whose formal parameter
     * types match those specified by {@code parameterTypes}.
     *
     * @param parameterTypes the parameter array
     * @return the {@code Constructor} object of the public constructor that
     *         matches the specified {@code parameterTypes}
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and
     *         the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class.
     *
     * @since 1.1
     */
    @CallerSensitive
    public Constructor<T> getConstructor(Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException
    {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.PUBLIC, Reflection.getCallerClass(), true);
        }
        return getReflectionFactory().copyConstructor(
            getConstructor0(parameterTypes, Member.PUBLIC));
    }


    /**
     * Returns an array of {@code Class} objects reflecting all the
     * classes and interfaces declared as members of the class represented by
     * this {@code Class} object. This includes public, protected, default
     * (package) access, and private classes and interfaces declared by the
     * class, but excludes inherited classes and interfaces.  This method
     * returns an array of length 0 if the class declares no classes or
     * interfaces as members, or if this {@code Class} object represents a
     * primitive type, an array class, or void.
     *
     * @return the array of {@code Class} objects representing all the
     *         declared members of this class
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and any of the
     *         following conditions is met:
     *
     *         <ul>
     *
     *         <li> the caller's class loader is not the same as the
     *         class loader of this class and invocation of
     *         {@link SecurityManager#checkPermission
     *         s.checkPermission} method with
     *         {@code RuntimePermission("accessDeclaredMembers")}
     *         denies access to the declared classes within this class
     *
     *         <li> the caller's class loader is not the same as or an
     *         ancestor of the class loader for the current class and
     *         invocation of {@link SecurityManager#checkPackageAccess
     *         s.checkPackageAccess()} denies access to the package
     *         of this class
     *
     *         </ul>
     *
     * @since 1.1
     * @jls 8.5 Member Type Declarations
     */
    @CallerSensitive
    public Class<?>[] getDeclaredClasses() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), false);
        }
        return getDeclaredClasses0();
    }


    /**
     * Returns an array of {@code Field} objects reflecting all the fields
     * declared by the class or interface represented by this
     * {@code Class} object. This includes public, protected, default
     * (package) access, and private fields, but excludes inherited fields.
     *
     * <p> If this {@code Class} object represents a class or interface with no
     * declared fields, then this method returns an array of length 0.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then this method returns an array of length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return  the array of {@code Field} objects representing all the
     *          declared fields of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared fields within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field[] getDeclaredFields() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        return copyFields(privateGetDeclaredFields(false));
    }

    /**
     * Returns an array of {@code RecordComponent} objects representing all the
     * record components of this record class, or {@code null} if this class is
     * not a record class.
     *
     * <p> The components are returned in the same order that they are declared
     * in the record header. The array is empty if this record class has no
     * components. If the class is not a record class, that is {@link
     * #isRecord()} returns {@code false}, then this method returns {@code null}.
     * Conversely, if {@link #isRecord()} returns {@code true}, then this method
     * returns a non-null value.
     *
     * @apiNote
     * <p> The following method can be used to find the record canonical constructor:
     *
     * <pre>{@code
     * static <T extends Record> Constructor<T> getCanonicalConstructor(Class<T> cls)
     *     throws NoSuchMethodException {
     *   Class<?>[] paramTypes =
     *     Arrays.stream(cls.getRecordComponents())
     *           .map(RecordComponent::getType)
     *           .toArray(Class<?>[]::new);
     *   return cls.getDeclaredConstructor(paramTypes);
     * }}</pre>
     *
     * @return  An array of {@code RecordComponent} objects representing all the
     *          record components of this record class, or {@code null} if this
     *          class is not a record class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared methods within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.10 Record Classes
     * @since 16
     */
    @CallerSensitive
    public RecordComponent[] getRecordComponents() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        if (!isRecord()) {
            return null;
        }
        return getRecordComponents0();
    }

    /**
     * Returns an array containing {@code Method} objects reflecting all the
     * declared methods of the class or interface represented by this {@code
     * Class} object, including public, protected, default (package)
     * access, and private methods, but excluding inherited methods.
     * The declared methods may include methods <em>not</em> in the
     * source of the class or interface, including {@linkplain
     * Method#isBridge bridge methods} and other {@linkplain
     * Executable#isSynthetic synthetic} methods added by compilers.
     *
     * <p> If this {@code Class} object represents a class or interface that
     * has multiple declared methods with the same name and parameter types,
     * but different return types, then the returned array has a {@code Method}
     * object for each such method.
     *
     * <p> If this {@code Class} object represents a class or interface that
     * has a class initialization method {@code <clinit>}, then the returned
     * array does <em>not</em> have a corresponding {@code Method} object.
     *
     * <p> If this {@code Class} object represents a class or interface with no
     * declared methods, then the returned array has length 0.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then the returned array has length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return  the array of {@code Method} objects representing all the
     *          declared methods of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared methods within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @see <a
     * href="{@docRoot}/java.base/java/lang/reflect/package-summary.html#LanguageJvmModel">Java
     * programming language and JVM modeling in core reflection</a>
     * @since 1.1
     */
    @CallerSensitive
    public Method[] getDeclaredMethods() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        return copyMethods(privateGetDeclaredMethods(false));
    }


    /**
     * Returns an array of {@code Constructor} objects reflecting all the
     * constructors declared by the class represented by this
     * {@code Class} object. These are public, protected, default
     * (package) access, and private constructors.  The elements in the array
     * returned are not sorted and are not in any particular order.  If the
     * class has a default constructor, it is included in the returned array.
     * This method returns an array of length 0 if this {@code Class}
     * object represents an interface, a primitive type, an array class, or
     * void.
     *
     * <p> See <cite>The Java Language Specification</cite>,
     * section {@jls 8.2}.
     *
     * @return  the array of {@code Constructor} objects representing all the
     *          declared constructors of this class
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared constructors within this class
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     * @jls 8.8 Constructor Declarations
     */
    @CallerSensitive
    public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        return copyConstructors(privateGetDeclaredConstructors(false));
    }


    /**
     * Returns a {@code Field} object that reflects the specified declared
     * field of the class or interface represented by this {@code Class}
     * object. The {@code name} parameter is a {@code String} that specifies
     * the simple name of the desired field.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code length} field of the array type.
     *
     * @param name the name of the field
     * @return  the {@code Field} object for the specified field in this
     *          class
     * @throws  NoSuchFieldException if a field with the specified name is
     *          not found.
     * @throws  NullPointerException if {@code name} is {@code null}
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared field
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     * @jls 8.2 Class Members
     * @jls 8.3 Field Declarations
     */
    @CallerSensitive
    public Field getDeclaredField(String name)
        throws NoSuchFieldException, SecurityException {
        Objects.requireNonNull(name);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        Field field = searchFields(privateGetDeclaredFields(false), name);
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        return getReflectionFactory().copyField(field);
    }


    /**
     * Returns a {@code Method} object that reflects the specified
     * declared method of the class or interface represented by this
     * {@code Class} object. The {@code name} parameter is a
     * {@code String} that specifies the simple name of the desired
     * method, and the {@code parameterTypes} parameter is an array of
     * {@code Class} objects that identify the method's formal parameter
     * types, in declared order.  If more than one method with the same
     * parameter types is declared in a class, and one of these methods has a
     * return type that is more specific than any of the others, that method is
     * returned; otherwise one of the methods is chosen arbitrarily.  If the
     * name is "&lt;init&gt;"or "&lt;clinit&gt;" a {@code NoSuchMethodException}
     * is raised.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code clone()} method.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter array
     * @return  the {@code Method} object for the method of this class
     *          matching the specified name and parameters
     * @throws  NoSuchMethodException if a matching method is not found.
     * @throws  NullPointerException if {@code name} is {@code null}
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared method
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @jls 8.2 Class Members
     * @jls 8.4 Method Declarations
     * @since 1.1
     */
    @CallerSensitive
    public Method getDeclaredMethod(String name, Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException {
        Objects.requireNonNull(name);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }
        Method method = searchMethods(privateGetDeclaredMethods(false), name, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(methodToString(name, parameterTypes));
        }
        return getReflectionFactory().copyMethod(method);
    }

    /**
     * Returns the list of {@code Method} objects for the declared public
     * methods of this class or interface that have the specified method name
     * and parameter types.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter array
     * @return the list of {@code Method} objects for the public methods of
     *         this class matching the specified name and parameters
     */
    List<Method> getDeclaredPublicMethods(String name, Class<?>... parameterTypes) {
        Method[] methods = privateGetDeclaredMethods(/* publicOnly */ true);
        ReflectionFactory factory = getReflectionFactory();
        List<Method> result = new ArrayList<>();
        for (Method method : methods) {
            if (method.getName().equals(name)
                && Arrays.equals(
                    factory.getExecutableSharedParameterTypes(method),
                    parameterTypes)) {
                result.add(factory.copyMethod(method));
            }
        }
        return result;
    }

    /**
     * Returns a {@code Constructor} object that reflects the specified
     * constructor of the class or interface represented by this
     * {@code Class} object.  The {@code parameterTypes} parameter is
     * an array of {@code Class} objects that identify the constructor's
     * formal parameter types, in declared order.
     *
     * If this {@code Class} object represents an inner class
     * declared in a non-static context, the formal parameter types
     * include the explicit enclosing instance as the first parameter.
     *
     * @param parameterTypes the parameter array
     * @return  The {@code Constructor} object for the constructor with the
     *          specified parameter list
     * @throws  NoSuchMethodException if a matching method is not found.
     * @throws  SecurityException
     *          If a security manager, <i>s</i>, is present and any of the
     *          following conditions is met:
     *
     *          <ul>
     *
     *          <li> the caller's class loader is not the same as the
     *          class loader of this class and invocation of
     *          {@link SecurityManager#checkPermission
     *          s.checkPermission} method with
     *          {@code RuntimePermission("accessDeclaredMembers")}
     *          denies access to the declared constructor
     *
     *          <li> the caller's class loader is not the same as or an
     *          ancestor of the class loader for the current class and
     *          invocation of {@link SecurityManager#checkPackageAccess
     *          s.checkPackageAccess()} denies access to the package
     *          of this class
     *
     *          </ul>
     *
     * @since 1.1
     */
    @CallerSensitive
    public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)
        throws NoSuchMethodException, SecurityException
    {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkMemberAccess(sm, Member.DECLARED, Reflection.getCallerClass(), true);
        }

        return getReflectionFactory().copyConstructor(
            getConstructor0(parameterTypes, Member.DECLARED));
    }

    /**
     * Finds a resource with a given name.
     *
     * <p> If this class is in a named {@link Module Module} then this method
     * will attempt to find the resource in the module. This is done by
     * delegating to the module's class loader {@link
     * ClassLoader#findResource(String,String) findResource(String,String)}
     * method, invoking it with the module name and the absolute name of the
     * resource. Resources in named modules are subject to the rules for
     * encapsulation specified in the {@code Module} {@link
     * Module#getResourceAsStream getResourceAsStream} method and so this
     * method returns {@code null} when the resource is a
     * non-"{@code .class}" resource in a package that is not open to the
     * caller's module.
     *
     * <p> Otherwise, if this class is not in a named module then the rules for
     * searching resources associated with a given class are implemented by the
     * defining {@linkplain ClassLoader class loader} of the class.  This method
     * delegates to this {@code Class} object's class loader.
     * If this {@code Class} object was loaded by the bootstrap class loader,
     * the method delegates to {@link ClassLoader#getSystemResourceAsStream}.
     *
     * <p> Before delegation, an absolute resource name is constructed from the
     * given resource name using this algorithm:
     *
     * <ul>
     *
     * <li> If the {@code name} begins with a {@code '/'}
     * (<code>'&#92;u002f'</code>), then the absolute name of the resource is the
     * portion of the {@code name} following the {@code '/'}.
     *
     * <li> Otherwise, the absolute name is of the following form:
     *
     * <blockquote>
     *   {@code modified_package_name/name}
     * </blockquote>
     *
     * <p> Where the {@code modified_package_name} is the package name of this
     * object with {@code '/'} substituted for {@code '.'}
     * (<code>'&#92;u002e'</code>).
     *
     * </ul>
     *
     * @param  name name of the desired resource
     * @return  A {@link java.io.InputStream} object; {@code null} if no
     *          resource with this name is found, the resource is in a package
     *          that is not {@linkplain Module#isOpen(String, Module) open} to at
     *          least the caller module, or access to the resource is denied
     *          by the security manager.
     * @throws  NullPointerException If {@code name} is {@code null}
     *
     * @see Module#getResourceAsStream(String)
     * @since  1.1
     * @revised 9
     */
    @CallerSensitive
    public InputStream getResourceAsStream(String name) {
        name = resolveName(name);

        Module thisModule = getModule();
        if (thisModule.isNamed()) {
            // check if resource can be located by caller
            if (Resources.canEncapsulate(name)
                && !isOpenToCaller(name, Reflection.getCallerClass())) {
                return null;
            }

            // resource not encapsulated or in package open to caller
            String mn = thisModule.getName();
            ClassLoader cl = classLoader;
            try {

                // special-case built-in class loaders to avoid the
                // need for a URL connection
                if (cl == null) {
                    return BootLoader.findResourceAsStream(mn, name);
                } else if (cl instanceof BuiltinClassLoader) {
                    return ((BuiltinClassLoader) cl).findResourceAsStream(mn, name);
                } else {
                    URL url = cl.findResource(mn, name);
                    return (url != null) ? url.openStream() : null;
                }

            } catch (IOException | SecurityException e) {
                return null;
            }
        }

        // unnamed module
        ClassLoader cl = classLoader;
        if (cl == null) {
            return ClassLoader.getSystemResourceAsStream(name);
        } else {
            return cl.getResourceAsStream(name);
        }
    }

    /**
     * Finds a resource with a given name.
     *
     * <p> If this class is in a named {@link Module Module} then this method
     * will attempt to find the resource in the module. This is done by
     * delegating to the module's class loader {@link
     * ClassLoader#findResource(String,String) findResource(String,String)}
     * method, invoking it with the module name and the absolute name of the
     * resource. Resources in named modules are subject to the rules for
     * encapsulation specified in the {@code Module} {@link
     * Module#getResourceAsStream getResourceAsStream} method and so this
     * method returns {@code null} when the resource is a
     * non-"{@code .class}" resource in a package that is not open to the
     * caller's module.
     *
     * <p> Otherwise, if this class is not in a named module then the rules for
     * searching resources associated with a given class are implemented by the
     * defining {@linkplain ClassLoader class loader} of the class.  This method
     * delegates to this {@code Class} object's class loader.
     * If this {@code Class} object was loaded by the bootstrap class loader,
     * the method delegates to {@link ClassLoader#getSystemResource}.
     *
     * <p> Before delegation, an absolute resource name is constructed from the
     * given resource name using this algorithm:
     *
     * <ul>
     *
     * <li> If the {@code name} begins with a {@code '/'}
     * (<code>'&#92;u002f'</code>), then the absolute name of the resource is the
     * portion of the {@code name} following the {@code '/'}.
     *
     * <li> Otherwise, the absolute name is of the following form:
     *
     * <blockquote>
     *   {@code modified_package_name/name}
     * </blockquote>
     *
     * <p> Where the {@code modified_package_name} is the package name of this
     * object with {@code '/'} substituted for {@code '.'}
     * (<code>'&#92;u002e'</code>).
     *
     * </ul>
     *
     * @param  name name of the desired resource
     * @return A {@link java.net.URL} object; {@code null} if no resource with
     *         this name is found, the resource cannot be located by a URL, the
     *         resource is in a package that is not
     *         {@linkplain Module#isOpen(String, Module) open} to at least the caller
     *         module, or access to the resource is denied by the security
     *         manager.
     * @throws NullPointerException If {@code name} is {@code null}
     * @since  1.1
     * @revised 9
     */
    @CallerSensitive
    public URL getResource(String name) {
        name = resolveName(name);

        Module thisModule = getModule();
        if (thisModule.isNamed()) {
            // check if resource can be located by caller
            if (Resources.canEncapsulate(name)
                && !isOpenToCaller(name, Reflection.getCallerClass())) {
                return null;
            }

            // resource not encapsulated or in package open to caller
            String mn = thisModule.getName();
            ClassLoader cl = classLoader;
            try {
                if (cl == null) {
                    return BootLoader.findResource(mn, name);
                } else {
                    return cl.findResource(mn, name);
                }
            } catch (IOException ioe) {
                return null;
            }
        }

        // unnamed module
        ClassLoader cl = classLoader;
        if (cl == null) {
            return ClassLoader.getSystemResource(name);
        } else {
            return cl.getResource(name);
        }
    }

    /**
     * Returns true if a resource with the given name can be located by the
     * given caller. All resources in a module can be located by code in
     * the module. For other callers, then the package needs to be open to
     * the caller.
     */
    private boolean isOpenToCaller(String name, Class<?> caller) {
        // assert getModule().isNamed();
        Module thisModule = getModule();
        Module callerModule = (caller != null) ? caller.getModule() : null;
        if (callerModule != thisModule) {
            String pn = Resources.toPackageName(name);
            if (thisModule.getDescriptor().packages().contains(pn)) {
                if (callerModule == null && !thisModule.isOpen(pn)) {
                    // no caller, package not open
                    return false;
                }
                if (!thisModule.isOpen(pn, callerModule)) {
                    // package not open to caller
                    return false;
                }
            }
        }
        return true;
    }


    /** protection domain returned when the internal domain is null */
    private static java.security.ProtectionDomain allPermDomain;

    /**
     * Returns the {@code ProtectionDomain} of this class.  If there is a
     * security manager installed, this method first calls the security
     * manager's {@code checkPermission} method with a
     * {@code RuntimePermission("getProtectionDomain")} permission to
     * ensure it's ok to get the
     * {@code ProtectionDomain}.
     *
     * @return the ProtectionDomain of this class
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        {@code checkPermission} method doesn't allow
     *        getting the ProtectionDomain.
     *
     * @see java.security.ProtectionDomain
     * @see SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     * @since 1.2
     */
    public java.security.ProtectionDomain getProtectionDomain() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.GET_PD_PERMISSION);
        }
        return protectionDomain();
    }

    // package-private
    java.security.ProtectionDomain protectionDomain() {
        java.security.ProtectionDomain pd = getProtectionDomain0();
        if (pd == null) {
            if (allPermDomain == null) {
                java.security.Permissions perms =
                    new java.security.Permissions();
                perms.add(SecurityConstants.ALL_PERMISSION);
                allPermDomain =
                    new java.security.ProtectionDomain(null, perms);
            }
            pd = allPermDomain;
        }
        return pd;
    }

    /**
     * Returns the ProtectionDomain of this class.
     */
    private native java.security.ProtectionDomain getProtectionDomain0();

    /*
     * Return the Virtual Machine's Class object for the named
     * primitive type.
     */
    static native Class<?> getPrimitiveClass(String name);

    /*
     * Check if client is allowed to access members.  If access is denied,
     * throw a SecurityException.
     *
     * This method also enforces package access.
     *
     * <p> Default policy: allow all clients access with normal Java access
     * control.
     *
     * <p> NOTE: should only be called if a SecurityManager is installed
     */
    private void checkMemberAccess(@SuppressWarnings("removal") SecurityManager sm, int which,
                                   Class<?> caller, boolean checkProxyInterfaces) {
        /* Default policy allows access to all {@link Member#PUBLIC} members,
         * as well as access to classes that have the same class loader as the caller.
         * In all other cases, it requires RuntimePermission("accessDeclaredMembers")
         * permission.
         */
        final ClassLoader ccl = ClassLoader.getClassLoader(caller);
        if (which != Member.PUBLIC) {
            final ClassLoader cl = classLoader;
            if (ccl != cl) {
                sm.checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
            }
        }
        this.checkPackageAccess(sm, ccl, checkProxyInterfaces);
    }

    /*
     * Checks if a client loaded in ClassLoader ccl is allowed to access this
     * class under the current package access policy. If access is denied,
     * throw a SecurityException.
     *
     * NOTE: this method should only be called if a SecurityManager is active
     */
    private void checkPackageAccess(@SuppressWarnings("removal") SecurityManager sm, final ClassLoader ccl,
                                    boolean checkProxyInterfaces) {
        final ClassLoader cl = classLoader;

        if (ReflectUtil.needsPackageAccessCheck(ccl, cl)) {
            String pkg = this.getPackageName();
            if (!pkg.isEmpty()) {
                // skip the package access check on a proxy class in default proxy package
                if (!Proxy.isProxyClass(this) || ReflectUtil.isNonPublicProxyClass(this)) {
                    sm.checkPackageAccess(pkg);
                }
            }
        }
        // check package access on the proxy interfaces
        if (checkProxyInterfaces && Proxy.isProxyClass(this)) {
            ReflectUtil.checkProxyPackageAccess(ccl, this.getInterfaces());
        }
    }

    /*
     * Checks if a client loaded in ClassLoader ccl is allowed to access the provided
     * classes under the current package access policy. If access is denied,
     * throw a SecurityException.
     *
     * NOTE: this method should only be called if a SecurityManager is active
     *       classes must be non-empty
     *       all classes provided must be loaded by the same ClassLoader
     * NOTE: this method does not support Proxy classes
     */
    private static void checkPackageAccessForPermittedSubclasses(@SuppressWarnings("removal") SecurityManager sm,
                                    final ClassLoader ccl, Class<?>[] subClasses) {
        final ClassLoader cl = subClasses[0].classLoader;

        if (ReflectUtil.needsPackageAccessCheck(ccl, cl)) {
            Set<String> packages = new HashSet<>();

            for (Class<?> c : subClasses) {
                if (Proxy.isProxyClass(c))
                    throw new InternalError("a permitted subclass should not be a proxy class: " + c);
                String pkg = c.getPackageName();
                if (!pkg.isEmpty()) {
                    packages.add(pkg);
                }
            }
            for (String pkg : packages) {
                sm.checkPackageAccess(pkg);
            }
        }
    }

    /**
     * Add a package name prefix if the name is not absolute. Remove leading "/"
     * if name is absolute
     */
    private String resolveName(String name) {
        if (!name.startsWith("/")) {
            String baseName = getPackageName();
            if (!baseName.isEmpty()) {
                int len = baseName.length() + 1 + name.length();
                StringBuilder sb = new StringBuilder(len);
                name = sb.append(baseName.replace('.', '/'))
                    .append('/')
                    .append(name)
                    .toString();
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }

    /**
     * Atomic operations support.
     */
    private static class Atomic {
        // initialize Unsafe machinery here, since we need to call Class.class instance method
        // and have to avoid calling it in the static initializer of the Class class...
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        // offset of Class.reflectionData instance field
        private static final long reflectionDataOffset
                = unsafe.objectFieldOffset(Class.class, "reflectionData");
        // offset of Class.annotationType instance field
        private static final long annotationTypeOffset
                = unsafe.objectFieldOffset(Class.class, "annotationType");
        // offset of Class.annotationData instance field
        private static final long annotationDataOffset
                = unsafe.objectFieldOffset(Class.class, "annotationData");

        static <T> boolean casReflectionData(Class<?> clazz,
                                             SoftReference<ReflectionData<T>> oldData,
                                             SoftReference<ReflectionData<T>> newData) {
            return unsafe.compareAndSetReference(clazz, reflectionDataOffset, oldData, newData);
        }

        static boolean casAnnotationType(Class<?> clazz,
                                         AnnotationType oldType,
                                         AnnotationType newType) {
            return unsafe.compareAndSetReference(clazz, annotationTypeOffset, oldType, newType);
        }

        static boolean casAnnotationData(Class<?> clazz,
                                         AnnotationData oldData,
                                         AnnotationData newData) {
            return unsafe.compareAndSetReference(clazz, annotationDataOffset, oldData, newData);
        }
    }

    /**
     * Reflection support.
     */

    // Reflection data caches various derived names and reflective members. Cached
    // values may be invalidated when JVM TI RedefineClasses() is called
    private static class ReflectionData<T> {
        volatile Field[] declaredFields;
        volatile Field[] publicFields;
        volatile Method[] declaredMethods;
        volatile Method[] publicMethods;
        volatile Constructor<T>[] declaredConstructors;
        volatile Constructor<T>[] publicConstructors;
        // Intermediate results for getFields and getMethods
        volatile Field[] declaredPublicFields;
        volatile Method[] declaredPublicMethods;
        volatile Class<?>[] interfaces;

        // Cached names
        String simpleName;
        String canonicalName;
        static final String NULL_SENTINEL = new String();

        // Value of classRedefinedCount when we created this ReflectionData instance
        final int redefinedCount;

        ReflectionData(int redefinedCount) {
            this.redefinedCount = redefinedCount;
        }
    }

    private transient volatile SoftReference<ReflectionData<T>> reflectionData;

    // Incremented by the VM on each call to JVM TI RedefineClasses()
    // that redefines this class or a superclass.
    private transient volatile int classRedefinedCount;

    // Lazily create and cache ReflectionData
    private ReflectionData<T> reflectionData() {
        SoftReference<ReflectionData<T>> reflectionData = this.reflectionData;
        int classRedefinedCount = this.classRedefinedCount;
        ReflectionData<T> rd;
        if (reflectionData != null &&
            (rd = reflectionData.get()) != null &&
            rd.redefinedCount == classRedefinedCount) {
            return rd;
        }
        // else no SoftReference or cleared SoftReference or stale ReflectionData
        // -> create and replace new instance
        return newReflectionData(reflectionData, classRedefinedCount);
    }

    private ReflectionData<T> newReflectionData(SoftReference<ReflectionData<T>> oldReflectionData,
                                                int classRedefinedCount) {
        while (true) {
            ReflectionData<T> rd = new ReflectionData<>(classRedefinedCount);
            // try to CAS it...
            if (Atomic.casReflectionData(this, oldReflectionData, new SoftReference<>(rd))) {
                return rd;
            }
            // else retry
            oldReflectionData = this.reflectionData;
            classRedefinedCount = this.classRedefinedCount;
            if (oldReflectionData != null &&
                (rd = oldReflectionData.get()) != null &&
                rd.redefinedCount == classRedefinedCount) {
                return rd;
            }
        }
    }

    // Generic signature handling
    private native String getGenericSignature0();

    // Generic info repository; lazily initialized
    private transient volatile ClassRepository genericInfo;

    // accessor for factory
    private GenericsFactory getFactory() {
        // create scope and factory
        return CoreReflectionFactory.make(this, ClassScope.make(this));
    }

    // accessor for generic info repository;
    // generic info is lazily initialized
    private ClassRepository getGenericInfo() {
        ClassRepository genericInfo = this.genericInfo;
        if (genericInfo == null) {
            String signature = getGenericSignature0();
            if (signature == null) {
                genericInfo = ClassRepository.NONE;
            } else {
                genericInfo = ClassRepository.make(signature, getFactory());
            }
            this.genericInfo = genericInfo;
        }
        return (genericInfo != ClassRepository.NONE) ? genericInfo : null;
    }

    // Annotations handling
    native byte[] getRawAnnotations();
    // Since 1.8
    native byte[] getRawTypeAnnotations();
    static byte[] getExecutableTypeAnnotationBytes(Executable ex) {
        return getReflectionFactory().getExecutableTypeAnnotationBytes(ex);
    }

    native ConstantPool getConstantPool();

    //
    //
    // java.lang.reflect.Field handling
    //
    //

    // Returns an array of "root" fields. These Field objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field[] privateGetDeclaredFields(boolean publicOnly) {
        Field[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.declaredPublicFields : rd.declaredFields;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        res = Reflection.filterFields(this, getDeclaredFields0(publicOnly));
        if (rd != null) {
            if (publicOnly) {
                rd.declaredPublicFields = res;
            } else {
                rd.declaredFields = res;
            }
        }
        return res;
    }

    // Returns an array of "root" fields. These Field objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field[] privateGetPublicFields() {
        Field[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = rd.publicFields;
            if (res != null) return res;
        }

        // Use a linked hash set to ensure order is preserved and
        // fields from common super interfaces are not duplicated
        LinkedHashSet<Field> fields = new LinkedHashSet<>();

        // Local fields
        addAll(fields, privateGetDeclaredFields(true));

        // Direct superinterfaces, recursively
        for (Class<?> si : getInterfaces()) {
            addAll(fields, si.privateGetPublicFields());
        }

        // Direct superclass, recursively
        Class<?> sc = getSuperclass();
        if (sc != null) {
            addAll(fields, sc.privateGetPublicFields());
        }

        res = fields.toArray(new Field[0]);
        if (rd != null) {
            rd.publicFields = res;
        }
        return res;
    }

    private static void addAll(Collection<Field> c, Field[] o) {
        for (Field f : o) {
            c.add(f);
        }
    }


    //
    //
    // java.lang.reflect.Constructor handling
    //
    //

    // Returns an array of "root" constructors. These Constructor
    // objects must NOT be propagated to the outside world, but must
    // instead be copied via ReflectionFactory.copyConstructor.
    private Constructor<T>[] privateGetDeclaredConstructors(boolean publicOnly) {
        Constructor<T>[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.publicConstructors : rd.declaredConstructors;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        if (isInterface()) {
            @SuppressWarnings("unchecked")
            Constructor<T>[] temporaryRes = (Constructor<T>[]) new Constructor<?>[0];
            res = temporaryRes;
        } else {
            res = getDeclaredConstructors0(publicOnly);
        }
        if (rd != null) {
            if (publicOnly) {
                rd.publicConstructors = res;
            } else {
                rd.declaredConstructors = res;
            }
        }
        return res;
    }

    //
    //
    // java.lang.reflect.Method handling
    //
    //

    // Returns an array of "root" methods. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method[] privateGetDeclaredMethods(boolean publicOnly) {
        Method[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = publicOnly ? rd.declaredPublicMethods : rd.declaredMethods;
            if (res != null) return res;
        }
        // No cached value available; request value from VM
        res = Reflection.filterMethods(this, getDeclaredMethods0(publicOnly));
        if (rd != null) {
            if (publicOnly) {
                rd.declaredPublicMethods = res;
            } else {
                rd.declaredMethods = res;
            }
        }
        return res;
    }

    // Returns an array of "root" methods. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method[] privateGetPublicMethods() {
        Method[] res;
        ReflectionData<T> rd = reflectionData();
        if (rd != null) {
            res = rd.publicMethods;
            if (res != null) return res;
        }

        // No cached value available; compute value recursively.
        // Start by fetching public declared methods...
        PublicMethods pms = new PublicMethods();
        for (Method m : privateGetDeclaredMethods(/* publicOnly */ true)) {
            pms.merge(m);
        }
        // ...then recur over superclass methods...
        Class<?> sc = getSuperclass();
        if (sc != null) {
            for (Method m : sc.privateGetPublicMethods()) {
                pms.merge(m);
            }
        }
        // ...and finally over direct superinterfaces.
        for (Class<?> intf : getInterfaces(/* cloneArray */ false)) {
            for (Method m : intf.privateGetPublicMethods()) {
                // static interface methods are not inherited
                if (!Modifier.isStatic(m.getModifiers())) {
                    pms.merge(m);
                }
            }
        }

        res = pms.toArray();
        if (rd != null) {
            rd.publicMethods = res;
        }
        return res;
    }


    //
    // Helpers for fetchers of one field, method, or constructor
    //

    // This method does not copy the returned Field object!
    private static Field searchFields(Field[] fields, String name) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    // Returns a "root" Field object. This Field object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyField.
    private Field getField0(String name) {
        // Note: the intent is that the search algorithm this routine
        // uses be equivalent to the ordering imposed by
        // privateGetPublicFields(). It fetches only the declared
        // public fields for each class, however, to reduce the number
        // of Field objects which have to be created for the common
        // case where the field being requested is declared in the
        // class which is being queried.
        Field res;
        // Search declared public fields
        if ((res = searchFields(privateGetDeclaredFields(true), name)) != null) {
            return res;
        }
        // Direct superinterfaces, recursively
        Class<?>[] interfaces = getInterfaces(/* cloneArray */ false);
        for (Class<?> c : interfaces) {
            if ((res = c.getField0(name)) != null) {
                return res;
            }
        }
        // Direct superclass, recursively
        if (!isInterface()) {
            Class<?> c = getSuperclass();
            if (c != null) {
                if ((res = c.getField0(name)) != null) {
                    return res;
                }
            }
        }
        return null;
    }

    // This method does not copy the returned Method object!
    private static Method searchMethods(Method[] methods,
                                        String name,
                                        Class<?>[] parameterTypes)
    {
        ReflectionFactory fact = getReflectionFactory();
        Method res = null;
        for (Method m : methods) {
            if (m.getName().equals(name)
                && arrayContentsEq(parameterTypes,
                                   fact.getExecutableSharedParameterTypes(m))
                && (res == null
                    || (res.getReturnType() != m.getReturnType()
                        && res.getReturnType().isAssignableFrom(m.getReturnType()))))
                res = m;
        }
        return res;
    }

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    // Returns a "root" Method object. This Method object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private Method getMethod0(String name, Class<?>[] parameterTypes) {
        PublicMethods.MethodList res = getMethodsRecursive(
            name,
            parameterTypes == null ? EMPTY_CLASS_ARRAY : parameterTypes,
            /* includeStatic */ true);
        return res == null ? null : res.getMostSpecific();
    }

    // Returns a list of "root" Method objects. These Method objects must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyMethod.
    private PublicMethods.MethodList getMethodsRecursive(String name,
                                                         Class<?>[] parameterTypes,
                                                         boolean includeStatic) {
        // 1st check declared public methods
        Method[] methods = privateGetDeclaredMethods(/* publicOnly */ true);
        PublicMethods.MethodList res = PublicMethods.MethodList
            .filter(methods, name, parameterTypes, includeStatic);
        // if there is at least one match among declared methods, we need not
        // search any further as such match surely overrides matching methods
        // declared in superclass(es) or interface(s).
        if (res != null) {
            return res;
        }

        // if there was no match among declared methods,
        // we must consult the superclass (if any) recursively...
        Class<?> sc = getSuperclass();
        if (sc != null) {
            res = sc.getMethodsRecursive(name, parameterTypes, includeStatic);
        }

        // ...and coalesce the superclass methods with methods obtained
        // from directly implemented interfaces excluding static methods...
        for (Class<?> intf : getInterfaces(/* cloneArray */ false)) {
            res = PublicMethods.MethodList.merge(
                res, intf.getMethodsRecursive(name, parameterTypes,
                                              /* includeStatic */ false));
        }

        return res;
    }

    // Returns a "root" Constructor object. This Constructor object must NOT
    // be propagated to the outside world, but must instead be copied
    // via ReflectionFactory.copyConstructor.
    private Constructor<T> getConstructor0(Class<?>[] parameterTypes,
                                        int which) throws NoSuchMethodException
    {
        ReflectionFactory fact = getReflectionFactory();
        Constructor<T>[] constructors = privateGetDeclaredConstructors((which == Member.PUBLIC));
        for (Constructor<T> constructor : constructors) {
            if (arrayContentsEq(parameterTypes,
                                fact.getExecutableSharedParameterTypes(constructor))) {
                return constructor;
            }
        }
        throw new NoSuchMethodException(methodToString("<init>", parameterTypes));
    }

    //
    // Other helpers and base implementation
    //

    private static boolean arrayContentsEq(Object[] a1, Object[] a2) {
        if (a1 == null) {
            return a2 == null || a2.length == 0;
        }

        if (a2 == null) {
            return a1.length == 0;
        }

        if (a1.length != a2.length) {
            return false;
        }

        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    private static Field[] copyFields(Field[] arg) {
        Field[] out = new Field[arg.length];
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < arg.length; i++) {
            out[i] = fact.copyField(arg[i]);
        }
        return out;
    }

    private static Method[] copyMethods(Method[] arg) {
        Method[] out = new Method[arg.length];
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < arg.length; i++) {
            out[i] = fact.copyMethod(arg[i]);
        }
        return out;
    }

    private static <U> Constructor<U>[] copyConstructors(Constructor<U>[] arg) {
        Constructor<U>[] out = arg.clone();
        ReflectionFactory fact = getReflectionFactory();
        for (int i = 0; i < out.length; i++) {
            out[i] = fact.copyConstructor(out[i]);
        }
        return out;
    }

    private native Field[]       getDeclaredFields0(boolean publicOnly);
    private native Method[]      getDeclaredMethods0(boolean publicOnly);
    private native Constructor<T>[] getDeclaredConstructors0(boolean publicOnly);
    private native Class<?>[]    getDeclaredClasses0();

    /*
     * Returns an array containing the components of the Record attribute,
     * or null if the attribute is not present.
     *
     * Note that this method returns non-null array on a class with
     * the Record attribute even if this class is not a record.
     */
    private native RecordComponent[] getRecordComponents0();
    private native boolean       isRecord0();

    /**
     * Helper method to get the method name from arguments.
     */
    private String methodToString(String name, Class<?>[] argTypes) {
        return getName() + '.' + name +
                ((argTypes == null || argTypes.length == 0) ?
                "()" :
                Arrays.stream(argTypes)
                        .map(c -> c == null ? "null" : c.getName())
                        .collect(Collectors.joining(",", "(", ")")));
    }

    /** use serialVersionUID from JDK 1.1 for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = 3206093459760846163L;


    /**
     * Class Class is special cased within the Serialization Stream Protocol.
     *
     * A Class instance is written initially into an ObjectOutputStream in the
     * following format:
     * <pre>
     *      {@code TC_CLASS} ClassDescriptor
     *      A ClassDescriptor is a special cased serialization of
     *      a {@code java.io.ObjectStreamClass} instance.
     * </pre>
     * A new handle is generated for the initial time the class descriptor
     * is written into the stream. Future references to the class descriptor
     * are written as references to the initial class descriptor instance.
     *
     * @see java.io.ObjectStreamClass
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];


    /**
     * Returns the assertion status that would be assigned to this
     * class if it were to be initialized at the time this method is invoked.
     * If this class has had its assertion status set, the most recent
     * setting will be returned; otherwise, if any package default assertion
     * status pertains to this class, the most recent setting for the most
     * specific pertinent package default assertion status is returned;
     * otherwise, if this class is not a system class (i.e., it has a
     * class loader) its class loader's default assertion status is returned;
     * otherwise, the system class default assertion status is returned.
     *
     * @apiNote
     * Few programmers will have any need for this method; it is provided
     * for the benefit of the JDK itself.  (It allows a class to determine at
     * the time that it is initialized whether assertions should be enabled.)
     * Note that this method is not guaranteed to return the actual
     * assertion status that was (or will be) associated with the specified
     * class when it was (or will be) initialized.
     *
     * @return the desired assertion status of the specified class.
     * @see    java.lang.ClassLoader#setClassAssertionStatus
     * @see    java.lang.ClassLoader#setPackageAssertionStatus
     * @see    java.lang.ClassLoader#setDefaultAssertionStatus
     * @since  1.4
     */
    public boolean desiredAssertionStatus() {
        ClassLoader loader = classLoader;
        // If the loader is null this is a system class, so ask the VM
        if (loader == null)
            return desiredAssertionStatus0(this);

        // If the classloader has been initialized with the assertion
        // directives, ask it. Otherwise, ask the VM.
        synchronized(loader.assertionLock) {
            if (loader.classAssertionStatus != null) {
                return loader.desiredAssertionStatus(getName());
            }
        }
        return desiredAssertionStatus0(this);
    }

    // Retrieves the desired assertion status of this class from the VM
    private static native boolean desiredAssertionStatus0(Class<?> clazz);

    /**
     * Returns true if and only if this class was declared as an enum in the
     * source code.
     *
     * Note that {@link java.lang.Enum} is not itself an enum class.
     *
     * Also note that if an enum constant is declared with a class body,
     * the class of that enum constant object is an anonymous class
     * and <em>not</em> the class of the declaring enum class. The
     * {@link Enum#getDeclaringClass} method of an enum constant can
     * be used to get the class of the enum class declaring the
     * constant.
     *
     * @return true if and only if this class was declared as an enum in the
     *     source code
     * @since 1.5
     * @jls 8.9.1 Enum Constants
     */
    public boolean isEnum() {
        // An enum must both directly extend java.lang.Enum and have
        // the ENUM bit set; classes for specialized enum constants
        // don't do the former.
        return (this.getModifiers() & ENUM) != 0 &&
        this.getSuperclass() == java.lang.Enum.class;
    }

    /**
     * Returns {@code true} if and only if this class is a record class.
     *
     * <p> The {@linkplain #getSuperclass() direct superclass} of a record
     * class is {@code java.lang.Record}. A record class is {@linkplain
     * Modifier#FINAL final}. A record class has (possibly zero) record
     * components; {@link #getRecordComponents()} returns a non-null but
     * possibly empty value for a record.
     *
     * <p> Note that class {@link Record} is not a record class and thus
     * invoking this method on class {@code Record} returns {@code false}.
     *
     * @return true if and only if this class is a record class, otherwise false
     * @jls 8.10 Record Classes
     * @since 16
     */
    public boolean isRecord() {
        // this superclass and final modifier check is not strictly necessary
        // they are intrinsified and serve as a fast-path check
        return getSuperclass() == java.lang.Record.class &&
                (this.getModifiers() & Modifier.FINAL) != 0 &&
                isRecord0();
    }

    // Fetches the factory for reflective objects
    @SuppressWarnings("removal")
    private static ReflectionFactory getReflectionFactory() {
        if (reflectionFactory == null) {
            reflectionFactory =
                java.security.AccessController.doPrivileged
                    (new ReflectionFactory.GetReflectionFactoryAction());
        }
        return reflectionFactory;
    }
    private static ReflectionFactory reflectionFactory;

    /**
     * Returns the elements of this enum class or null if this
     * Class object does not represent an enum class.
     *
     * @return an array containing the values comprising the enum class
     *     represented by this {@code Class} object in the order they're
     *     declared, or null if this {@code Class} object does not
     *     represent an enum class
     * @since 1.5
     * @jls 8.9.1 Enum Constants
     */
    public T[] getEnumConstants() {
        T[] values = getEnumConstantsShared();
        return (values != null) ? values.clone() : null;
    }

    /**
     * Returns the elements of this enum class or null if this
     * Class object does not represent an enum class;
     * identical to getEnumConstants except that the result is
     * uncloned, cached, and shared by all callers.
     */
    @SuppressWarnings("removal")
    T[] getEnumConstantsShared() {
        T[] constants = enumConstants;
        if (constants == null) {
            if (!isEnum()) return null;
            try {
                final Method values = getMethod("values");
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<>() {
                        public Void run() {
                                values.setAccessible(true);
                                return null;
                            }
                        });
                @SuppressWarnings("unchecked")
                T[] temporaryConstants = (T[])values.invoke(null);
                enumConstants = constants = temporaryConstants;
            }
            // These can happen when users concoct enum-like classes
            // that don't comply with the enum spec.
            catch (InvocationTargetException | NoSuchMethodException |
                   IllegalAccessException ex) { return null; }
        }
        return constants;
    }
    private transient volatile T[] enumConstants;

    /**
     * Returns a map from simple name to enum constant.  This package-private
     * method is used internally by Enum to implement
     * {@code public static <T extends Enum<T>> T valueOf(Class<T>, String)}
     * efficiently.  Note that the map is returned by this method is
     * created lazily on first use.  Typically it won't ever get created.
     */
    Map<String, T> enumConstantDirectory() {
        Map<String, T> directory = enumConstantDirectory;
        if (directory == null) {
            T[] universe = getEnumConstantsShared();
            if (universe == null)
                throw new IllegalArgumentException(
                    getName() + " is not an enum class");
            directory = new HashMap<>((int)(universe.length / 0.75f) + 1);
            for (T constant : universe) {
                directory.put(((Enum<?>)constant).name(), constant);
            }
            enumConstantDirectory = directory;
        }
        return directory;
    }
    private transient volatile Map<String, T> enumConstantDirectory;

    /**
     * Casts an object to the class or interface represented
     * by this {@code Class} object.
     *
     * @param obj the object to be cast
     * @return the object after casting, or null if obj is null
     *
     * @throws ClassCastException if the object is not
     * null and is not assignable to the type T.
     *
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    @IntrinsicCandidate
    public T cast(Object obj) {
        if (obj != null && !isInstance(obj))
            throw new ClassCastException(cannotCastMsg(obj));
        return (T) obj;
    }

    private String cannotCastMsg(Object obj) {
        return "Cannot cast " + obj.getClass().getName() + " to " + getName();
    }

    /**
     * Casts this {@code Class} object to represent a subclass of the class
     * represented by the specified class object.  Checks that the cast
     * is valid, and throws a {@code ClassCastException} if it is not.  If
     * this method succeeds, it always returns a reference to this {@code Class} object.
     *
     * <p>This method is useful when a client needs to "narrow" the type of
     * a {@code Class} object to pass it to an API that restricts the
     * {@code Class} objects that it is willing to accept.  A cast would
     * generate a compile-time warning, as the correctness of the cast
     * could not be checked at runtime (because generic types are implemented
     * by erasure).
     *
     * @param <U> the type to cast this {@code Class} object to
     * @param clazz the class of the type to cast this {@code Class} object to
     * @return this {@code Class} object, cast to represent a subclass of
     *    the specified class object.
     * @throws ClassCastException if this {@code Class} object does not
     *    represent a subclass of the specified class (here "subclass" includes
     *    the class itself).
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public <U> Class<? extends U> asSubclass(Class<U> clazz) {
        if (clazz.isAssignableFrom(this))
            return (Class<? extends U>) this;
        else
            throw new ClassCastException(this.toString());
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotation returned by this method is a
     * declaration annotation.
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        return (A) annotationData().annotations.get(annotationClass);
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return GenericDeclaration.super.isAnnotationPresent(annotationClass);
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        AnnotationData annotationData = annotationData();
        return AnnotationSupport.getAssociatedAnnotations(annotationData.declaredAnnotations,
                                                          this,
                                                          annotationClass);
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     *
     * @since 1.5
     */
    @Override
    public Annotation[] getAnnotations() {
        return AnnotationParser.toArray(annotationData().annotations);
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotation returned by this method is a
     * declaration annotation.
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        return (A) annotationData().declaredAnnotations.get(annotationClass);
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass);

        return AnnotationSupport.getDirectlyAndIndirectlyPresent(annotationData().declaredAnnotations,
                                                                 annotationClass);
    }

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are
     * declaration annotations.
     *
     * @since 1.5
     */
    @Override
    public Annotation[] getDeclaredAnnotations()  {
        return AnnotationParser.toArray(annotationData().declaredAnnotations);
    }

    // annotation data that might get invalidated when JVM TI RedefineClasses() is called
    private static class AnnotationData {
        final Map<Class<? extends Annotation>, Annotation> annotations;
        final Map<Class<? extends Annotation>, Annotation> declaredAnnotations;

        // Value of classRedefinedCount when we created this AnnotationData instance
        final int redefinedCount;

        AnnotationData(Map<Class<? extends Annotation>, Annotation> annotations,
                       Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
                       int redefinedCount) {
            this.annotations = annotations;
            this.declaredAnnotations = declaredAnnotations;
            this.redefinedCount = redefinedCount;
        }
    }

    // Annotations cache
    @SuppressWarnings("UnusedDeclaration")
    private transient volatile AnnotationData annotationData;

    private AnnotationData annotationData() {
        while (true) { // retry loop
            AnnotationData annotationData = this.annotationData;
            int classRedefinedCount = this.classRedefinedCount;
            if (annotationData != null &&
                annotationData.redefinedCount == classRedefinedCount) {
                return annotationData;
            }
            // null or stale annotationData -> optimistically create new instance
            AnnotationData newAnnotationData = createAnnotationData(classRedefinedCount);
            // try to install it
            if (Atomic.casAnnotationData(this, annotationData, newAnnotationData)) {
                // successfully installed new AnnotationData
                return newAnnotationData;
            }
        }
    }

    private AnnotationData createAnnotationData(int classRedefinedCount) {
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations =
            AnnotationParser.parseAnnotations(getRawAnnotations(), getConstantPool(), this);
        Class<?> superClass = getSuperclass();
        Map<Class<? extends Annotation>, Annotation> annotations = null;
        if (superClass != null) {
            Map<Class<? extends Annotation>, Annotation> superAnnotations =
                superClass.annotationData().annotations;
            for (Map.Entry<Class<? extends Annotation>, Annotation> e : superAnnotations.entrySet()) {
                Class<? extends Annotation> annotationClass = e.getKey();
                if (AnnotationType.getInstance(annotationClass).isInherited()) {
                    if (annotations == null) { // lazy construction
                        annotations = new LinkedHashMap<>((Math.max(
                                declaredAnnotations.size(),
                                Math.min(12, declaredAnnotations.size() + superAnnotations.size())
                            ) * 4 + 2) / 3
                        );
                    }
                    annotations.put(annotationClass, e.getValue());
                }
            }
        }
        if (annotations == null) {
            // no inherited annotations -> share the Map with declaredAnnotations
            annotations = declaredAnnotations;
        } else {
            // at least one inherited annotation -> declared may override inherited
            annotations.putAll(declaredAnnotations);
        }
        return new AnnotationData(annotations, declaredAnnotations, classRedefinedCount);
    }

    // Annotation interfaces cache their internal (AnnotationType) form

    @SuppressWarnings("UnusedDeclaration")
    private transient volatile AnnotationType annotationType;

    boolean casAnnotationType(AnnotationType oldType, AnnotationType newType) {
        return Atomic.casAnnotationType(this, oldType, newType);
    }

    AnnotationType getAnnotationType() {
        return annotationType;
    }

    Map<Class<? extends Annotation>, Annotation> getDeclaredAnnotationMap() {
        return annotationData().declaredAnnotations;
    }

    /* Backing store of user-defined values pertaining to this class.
     * Maintained by the ClassValue class.
     */
    transient ClassValue.ClassValueMap classValueMap;

    /**
     * Returns an {@code AnnotatedType} object that represents the use of a
     * type to specify the superclass of the entity represented by this {@code
     * Class} object. (The <em>use</em> of type Foo to specify the superclass
     * in '...  extends Foo' is distinct from the <em>declaration</em> of class
     * Foo.)
     *
     * <p> If this {@code Class} object represents a class whose declaration
     * does not explicitly indicate an annotated superclass, then the return
     * value is an {@code AnnotatedType} object representing an element with no
     * annotations.
     *
     * <p> If this {@code Class} represents either the {@code Object} class, an
     * interface type, an array type, a primitive type, or void, the return
     * value is {@code null}.
     *
     * @return an object representing the superclass
     * @since 1.8
     */
    public AnnotatedType getAnnotatedSuperclass() {
        if (this == Object.class ||
                isInterface() ||
                isArray() ||
                isPrimitive() ||
                this == Void.TYPE) {
            return null;
        }

        return TypeAnnotationParser.buildAnnotatedSuperclass(getRawTypeAnnotations(), getConstantPool(), this);
    }

    /**
     * Returns an array of {@code AnnotatedType} objects that represent the use
     * of types to specify superinterfaces of the entity represented by this
     * {@code Class} object. (The <em>use</em> of type Foo to specify a
     * superinterface in '... implements Foo' is distinct from the
     * <em>declaration</em> of interface Foo.)
     *
     * <p> If this {@code Class} object represents a class, the return value is
     * an array containing objects representing the uses of interface types to
     * specify interfaces implemented by the class. The order of the objects in
     * the array corresponds to the order of the interface types used in the
     * 'implements' clause of the declaration of this {@code Class} object.
     *
     * <p> If this {@code Class} object represents an interface, the return
     * value is an array containing objects representing the uses of interface
     * types to specify interfaces directly extended by the interface. The
     * order of the objects in the array corresponds to the order of the
     * interface types used in the 'extends' clause of the declaration of this
     * {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface whose
     * declaration does not explicitly indicate any annotated superinterfaces,
     * the return value is an array of length 0.
     *
     * <p> If this {@code Class} object represents either the {@code Object}
     * class, an array type, a primitive type, or void, the return value is an
     * array of length 0.
     *
     * @return an array representing the superinterfaces
     * @since 1.8
     */
    public AnnotatedType[] getAnnotatedInterfaces() {
         return TypeAnnotationParser.buildAnnotatedInterfaces(getRawTypeAnnotations(), getConstantPool(), this);
    }

    private native Class<?> getNestHost0();

    /**
     * Returns the nest host of the <a href=#nest>nest</a> to which the class
     * or interface represented by this {@code Class} object belongs.
     * Every class and interface belongs to exactly one nest.
     *
     * If the nest host of this class or interface has previously
     * been determined, then this method returns the nest host.
     * If the nest host of this class or interface has
     * not previously been determined, then this method determines the nest
     * host using the algorithm of JVMS 5.4.4, and returns it.
     *
     * Often, a class or interface belongs to a nest consisting only of itself,
     * in which case this method returns {@code this} to indicate that the class
     * or interface is the nest host.
     *
     * <p>If this {@code Class} object represents a primitive type, an array type,
     * or {@code void}, then this method returns {@code this},
     * indicating that the represented entity belongs to the nest consisting only of
     * itself, and is the nest host.
     *
     * @return the nest host of this class or interface
     *
     * @throws SecurityException
     *         If the returned class is not the current class, and
     *         if a security manager, <i>s</i>, is present and the caller's
     *         class loader is not the same as or an ancestor of the class
     *         loader for the returned class and invocation of {@link
     *         SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *         denies access to the package of the returned class
     * @since 11
     * @jvms 4.7.28 The {@code NestHost} Attribute
     * @jvms 4.7.29 The {@code NestMembers} Attribute
     * @jvms 5.4.4 Access Control
     */
    @CallerSensitive
    public Class<?> getNestHost() {
        if (isPrimitive() || isArray()) {
            return this;
        }

        Class<?> host = getNestHost0();
        if (host == this) {
            return this;
        }
        // returning a different class requires a security check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkPackageAccess(sm,
                               ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
        }
        return host;
    }

    /**
     * Determines if the given {@code Class} is a nestmate of the
     * class or interface represented by this {@code Class} object.
     * Two classes or interfaces are nestmates
     * if they have the same {@linkplain #getNestHost() nest host}.
     *
     * @param c the class to check
     * @return {@code true} if this class and {@code c} are members of
     * the same nest; and {@code false} otherwise.
     *
     * @since 11
     */
    public boolean isNestmateOf(Class<?> c) {
        if (this == c) {
            return true;
        }
        if (isPrimitive() || isArray() ||
            c.isPrimitive() || c.isArray()) {
            return false;
        }

        return getNestHost() == c.getNestHost();
    }

    private native Class<?>[] getNestMembers0();

    /**
     * Returns an array containing {@code Class} objects representing all the
     * classes and interfaces that are members of the nest to which the class
     * or interface represented by this {@code Class} object belongs.
     *
     * First, this method obtains the {@linkplain #getNestHost() nest host},
     * {@code H}, of the nest to which the class or interface represented by
     * this {@code Class} object belongs. The zeroth element of the returned
     * array is {@code H}.
     *
     * Then, for each class or interface {@code C} which is recorded by {@code H}
     * as being a member of its nest, this method attempts to obtain the {@code Class}
     * object for {@code C} (using {@linkplain #getClassLoader() the defining class
     * loader} of the current {@code Class} object), and then obtains the
     * {@linkplain #getNestHost() nest host} of the nest to which {@code C} belongs.
     * The classes and interfaces which are recorded by {@code H} as being members
     * of its nest, and for which {@code H} can be determined as their nest host,
     * are indicated by subsequent elements of the returned array. The order of
     * such elements is unspecified. Duplicates are permitted.
     *
     * <p>If this {@code Class} object represents a primitive type, an array type,
     * or {@code void}, then this method returns a single-element array containing
     * {@code this}.
     *
     * @apiNote
     * The returned array includes only the nest members recorded in the {@code NestMembers}
     * attribute, and not any hidden classes that were added to the nest via
     * {@link MethodHandles.Lookup#defineHiddenClass(byte[], boolean, MethodHandles.Lookup.ClassOption...)
     * Lookup::defineHiddenClass}.
     *
     * @return an array of all classes and interfaces in the same nest as
     * this class or interface
     *
     * @throws SecurityException
     * If any returned class is not the current class, and
     * if a security manager, <i>s</i>, is present and the caller's
     * class loader is not the same as or an ancestor of the class
     * loader for that returned class and invocation of {@link
     * SecurityManager#checkPackageAccess s.checkPackageAccess()}
     * denies access to the package of that returned class
     *
     * @since 11
     * @see #getNestHost()
     * @jvms 4.7.28 The {@code NestHost} Attribute
     * @jvms 4.7.29 The {@code NestMembers} Attribute
     */
    @CallerSensitive
    public Class<?>[] getNestMembers() {
        if (isPrimitive() || isArray()) {
            return new Class<?>[] { this };
        }
        Class<?>[] members = getNestMembers0();
        // Can't actually enable this due to bootstrapping issues
        // assert(members.length != 1 || members[0] == this); // expected invariant from VM

        if (members.length > 1) {
            // If we return anything other than the current class we need
            // a security check
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkPackageAccess(sm,
                                   ClassLoader.getClassLoader(Reflection.getCallerClass()), true);
            }
        }
        return members;
    }

    /**
     * Returns the descriptor string of the entity (class, interface, array class,
     * primitive type, or {@code void}) represented by this {@code Class} object.
     *
     * <p> If this {@code Class} object represents a class or interface,
     * not an array class, then:
     * <ul>
     * <li> If the class or interface is not {@linkplain Class#isHidden() hidden},
     *      then the result is a field descriptor (JVMS {@jvms 4.3.2})
     *      for the class or interface. Calling
     *      {@link ClassDesc#ofDescriptor(String) ClassDesc::ofDescriptor}
     *      with the result descriptor string produces a {@link ClassDesc ClassDesc}
     *      describing this class or interface.
     * <li> If the class or interface is {@linkplain Class#isHidden() hidden},
     *      then the result is a string of the form:
     *      <blockquote>
     *      {@code "L" +} <em>N</em> {@code + "." + <suffix> + ";"}
     *      </blockquote>
     *      where <em>N</em> is the <a href="ClassLoader.html#binary-name">binary name</a>
     *      encoded in internal form indicated by the {@code class} file passed to
     *      {@link MethodHandles.Lookup#defineHiddenClass(byte[], boolean, MethodHandles.Lookup.ClassOption...)
     *      Lookup::defineHiddenClass}, and {@code <suffix>} is an unqualified name.
     *      A hidden class or interface has no {@linkplain ClassDesc nominal descriptor}.
     *      The result string is not a type descriptor.
     * </ul>
     *
     * <p> If this {@code Class} object represents an array class, then
     * the result is a string consisting of one or more '{@code [}' characters
     * representing the depth of the array nesting, followed by the
     * descriptor string of the element type.
     * <ul>
     * <li> If the element type is not a {@linkplain Class#isHidden() hidden} class
     * or interface, then this array class can be described nominally.
     * Calling {@link ClassDesc#ofDescriptor(String) ClassDesc::ofDescriptor}
     * with the result descriptor string produces a {@link ClassDesc ClassDesc}
     * describing this array class.
     * <li> If the element type is a {@linkplain Class#isHidden() hidden} class or
     * interface, then this array class cannot be described nominally.
     * The result string is not a type descriptor.
     * </ul>
     *
     * <p> If this {@code Class} object represents a primitive type or
     * {@code void}, then the result is a field descriptor string which
     * is a one-letter code corresponding to a primitive type or {@code void}
     * ({@code "B", "C", "D", "F", "I", "J", "S", "Z", "V"}) (JVMS {@jvms 4.3.2}).
     *
     * @apiNote
     * This is not a strict inverse of {@link #forName};
     * distinct classes which share a common name but have different class loaders
     * will have identical descriptor strings.
     *
     * @return the descriptor string for this {@code Class} object
     * @jvms 4.3.2 Field Descriptors
     * @since 12
     */
    @Override
    public String descriptorString() {
        if (isPrimitive())
            return Wrapper.forPrimitiveType(this).basicTypeString();

        if (isArray()) {
            return "[" + componentType.descriptorString();
        } else if (isHidden()) {
            String name = getName();
            int index = name.indexOf('/');
            return new StringBuilder(name.length() + 2)
                    .append('L')
                    .append(name.substring(0, index).replace('.', '/'))
                    .append('.')
                    .append(name, index + 1, name.length())
                    .append(';')
                    .toString();
        } else {
            String name = getName().replace('.', '/');
            return new StringBuilder(name.length() + 2)
                    .append('L')
                    .append(name)
                    .append(';')
                    .toString();
        }
    }

    /**
     * Returns the component type of this {@code Class}, if it describes
     * an array type, or {@code null} otherwise.
     *
     * @implSpec
     * Equivalent to {@link Class#getComponentType()}.
     *
     * @return a {@code Class} describing the component type, or {@code null}
     * if this {@code Class} does not describe an array type
     * @since 12
     */
    @Override
    public Class<?> componentType() {
        return isArray() ? componentType : null;
    }

    /**
     * Returns a {@code Class} for an array type whose component type
     * is described by this {@linkplain Class}.
     *
     * @return a {@code Class} describing the array type
     * @since 12
     */
    @Override
    public Class<?> arrayType() {
        return Array.newInstance(this, 0).getClass();
    }

    /**
     * Returns a nominal descriptor for this instance, if one can be
     * constructed, or an empty {@link Optional} if one cannot be.
     *
     * @return An {@link Optional} containing the resulting nominal descriptor,
     * or an empty {@link Optional} if one cannot be constructed.
     * @since 12
     */
    @Override
    public Optional<ClassDesc> describeConstable() {
        Class<?> c = isArray() ? elementType() : this;
        return c.isHidden() ? Optional.empty()
                            : Optional.of(ClassDesc.ofDescriptor(descriptorString()));
   }

    /**
     * Returns {@code true} if and only if the underlying class is a hidden class.
     *
     * @return {@code true} if and only if this class is a hidden class.
     *
     * @since 15
     * @see MethodHandles.Lookup#defineHiddenClass
     */
    @IntrinsicCandidate
    public native boolean isHidden();

    /**
     * Returns an array containing {@code Class} objects representing the
     * direct subinterfaces or subclasses permitted to extend or
     * implement this class or interface if it is sealed.  The order of such elements
     * is unspecified. The array is empty if this sealed class or interface has no
     * permitted subclass. If this {@code Class} object represents a primitive type,
     * {@code void}, an array type, or a class or interface that is not sealed,
     * that is {@link #isSealed()} returns {@code false}, then this method returns {@code null}.
     * Conversely, if {@link #isSealed()} returns {@code true}, then this method
     * returns a non-null value.
     *
     * For each class or interface {@code C} which is recorded as a permitted
     * direct subinterface or subclass of this class or interface,
     * this method attempts to obtain the {@code Class}
     * object for {@code C} (using {@linkplain #getClassLoader() the defining class
     * loader} of the current {@code Class} object).
     * The {@code Class} objects which can be obtained and which are direct
     * subinterfaces or subclasses of this class or interface,
     * are indicated by elements of the returned array. If a {@code Class} object
     * cannot be obtained, it is silently ignored, and not included in the result
     * array.
     *
     * @return an array of {@code Class} objects of the permitted subclasses of this class or interface,
     *         or {@code null} if this class or interface is not sealed.
     *
     * @throws SecurityException
     *         If a security manager, <i>s</i>, is present and the caller's
     *         class loader is not the same as or an ancestor of the class
     *         loader for that returned class and invocation of {@link
     *         SecurityManager#checkPackageAccess s.checkPackageAccess()}
     *         denies access to the package of any class in the returned array.
     *
     * @jls 8.1 Class Declarations
     * @jls 9.1 Interface Declarations
     * @since 17
     */
    @CallerSensitive
    public Class<?>[] getPermittedSubclasses() {
        Class<?>[] subClasses;
        if (isArray() || isPrimitive() || (subClasses = getPermittedSubclasses0()) == null) {
            return null;
        }
        if (subClasses.length > 0) {
            if (Arrays.stream(subClasses).anyMatch(c -> !isDirectSubType(c))) {
                subClasses = Arrays.stream(subClasses)
                                   .filter(this::isDirectSubType)
                                   .toArray(s -> new Class<?>[s]);
            }
        }
        if (subClasses.length > 0) {
            // If we return some classes we need a security check:
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkPackageAccessForPermittedSubclasses(sm,
                                             ClassLoader.getClassLoader(Reflection.getCallerClass()),
                                             subClasses);
            }
        }
        return subClasses;
    }

    private boolean isDirectSubType(Class<?> c) {
        if (isInterface()) {
            for (Class<?> i : c.getInterfaces(/* cloneArray */ false)) {
                if (i == this) {
                    return true;
                }
            }
        } else {
            return c.getSuperclass() == this;
        }
        return false;
    }

    /**
     * Returns {@code true} if and only if this {@code Class} object represents
     * a sealed class or interface. If this {@code Class} object represents a
     * primitive type, {@code void}, or an array type, this method returns
     * {@code false}. A sealed class or interface has (possibly zero) permitted
     * subclasses; {@link #getPermittedSubclasses()} returns a non-null but
     * possibly empty value for a sealed class or interface.
     *
     * @return {@code true} if and only if this {@code Class} object represents
     * a sealed class or interface.
     *
     * @jls 8.1 Class Declarations
     * @jls 9.1 Interface Declarations
     * @since 17
     */
    public boolean isSealed() {
        if (isArray() || isPrimitive()) {
            return false;
        }
        return getPermittedSubclasses() != null;
    }

    private native Class<?>[] getPermittedSubclasses0();
}
