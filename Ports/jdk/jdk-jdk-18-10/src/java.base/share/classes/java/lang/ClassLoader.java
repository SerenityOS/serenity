/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Azul Systems, Inc. All rights reserved.
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

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jdk.internal.loader.BootLoader;
import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.ClassLoaders;
import jdk.internal.loader.NativeLibrary;
import jdk.internal.loader.NativeLibraries;
import jdk.internal.perf.PerfCounter;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.util.StaticProperty;
import sun.reflect.misc.ReflectUtil;
import sun.security.util.SecurityConstants;

/**
 * A class loader is an object that is responsible for loading classes. The
 * class {@code ClassLoader} is an abstract class.  Given the <a
 * href="#binary-name">binary name</a> of a class, a class loader should attempt to
 * locate or generate data that constitutes a definition for the class.  A
 * typical strategy is to transform the name into a file name and then read a
 * "class file" of that name from a file system.
 *
 * <p> Every {@link java.lang.Class Class} object contains a {@link
 * Class#getClassLoader() reference} to the {@code ClassLoader} that defined
 * it.
 *
 * <p> {@code Class} objects for array classes are not created by class
 * loaders, but are created automatically as required by the Java runtime.
 * The class loader for an array class, as returned by {@link
 * Class#getClassLoader()} is the same as the class loader for its element
 * type; if the element type is a primitive type, then the array class has no
 * class loader.
 *
 * <p> Applications implement subclasses of {@code ClassLoader} in order to
 * extend the manner in which the Java virtual machine dynamically loads
 * classes.
 *
 * <p> Class loaders may typically be used by security managers to indicate
 * security domains.
 *
 * <p> In addition to loading classes, a class loader is also responsible for
 * locating resources. A resource is some data (a "{@code .class}" file,
 * configuration data, or an image for example) that is identified with an
 * abstract '/'-separated path name. Resources are typically packaged with an
 * application or library so that they can be located by code in the
 * application or library. In some cases, the resources are included so that
 * they can be located by other libraries.
 *
 * <p> The {@code ClassLoader} class uses a delegation model to search for
 * classes and resources.  Each instance of {@code ClassLoader} has an
 * associated parent class loader. When requested to find a class or
 * resource, a {@code ClassLoader} instance will usually delegate the search
 * for the class or resource to its parent class loader before attempting to
 * find the class or resource itself.
 *
 * <p> Class loaders that support concurrent loading of classes are known as
 * <em>{@linkplain #isRegisteredAsParallelCapable() parallel capable}</em> class
 * loaders and are required to register themselves at their class initialization
 * time by invoking the {@link
 * #registerAsParallelCapable ClassLoader.registerAsParallelCapable}
 * method. Note that the {@code ClassLoader} class is registered as parallel
 * capable by default. However, its subclasses still need to register themselves
 * if they are parallel capable.
 * In environments in which the delegation model is not strictly
 * hierarchical, class loaders need to be parallel capable, otherwise class
 * loading can lead to deadlocks because the loader lock is held for the
 * duration of the class loading process (see {@link #loadClass
 * loadClass} methods).
 *
 * <h2> <a id="builtinLoaders">Run-time Built-in Class Loaders</a></h2>
 *
 * The Java run-time has the following built-in class loaders:
 *
 * <ul>
 * <li><p>Bootstrap class loader.
 *     It is the virtual machine's built-in class loader, typically represented
 *     as {@code null}, and does not have a parent.</li>
 * <li><p>{@linkplain #getPlatformClassLoader() Platform class loader}.
 *     The platform class loader is responsible for loading the
 *     <em>platform classes</em>.  Platform classes include Java SE platform APIs,
 *     their implementation classes and JDK-specific run-time classes that are
 *     defined by the platform class loader or its ancestors.
 *     The platform class loader can be used as the parent of a {@code ClassLoader}
 *     instance.
 *     <p> To allow for upgrading/overriding of modules defined to the platform
 *     class loader, and where upgraded modules read modules defined to class
 *     loaders other than the platform class loader and its ancestors, then
 *     the platform class loader may have to delegate to other class loaders,
 *     the application class loader for example.
 *     In other words, classes in named modules defined to class loaders
 *     other than the platform class loader and its ancestors may be visible
 *     to the platform class loader. </li>
 * <li><p>{@linkplain #getSystemClassLoader() System class loader}.
 *     It is also known as <em>application class loader</em> and is distinct
 *     from the platform class loader.
 *     The system class loader is typically used to define classes on the
 *     application class path, module path, and JDK-specific tools.
 *     The platform class loader is the parent or an ancestor of the system class
 *     loader, so the system class loader can load platform classes by delegating
 *     to its parent.</li>
 * </ul>
 *
 * <p> Normally, the Java virtual machine loads classes from the local file
 * system in a platform-dependent manner.
 * However, some classes may not originate from a file; they may originate
 * from other sources, such as the network, or they could be constructed by an
 * application.  The method {@link #defineClass(String, byte[], int, int)
 * defineClass} converts an array of bytes into an instance of class
 * {@code Class}. Instances of this newly defined class can be created using
 * {@link Class#newInstance Class.newInstance}.
 *
 * <p> The methods and constructors of objects created by a class loader may
 * reference other classes.  To determine the class(es) referred to, the Java
 * virtual machine invokes the {@link #loadClass loadClass} method of
 * the class loader that originally created the class.
 *
 * <p> For example, an application could create a network class loader to
 * download class files from a server.  Sample code might look like:
 *
 * <blockquote><pre>
 *   ClassLoader loader&nbsp;= new NetworkClassLoader(host,&nbsp;port);
 *   Object main&nbsp;= loader.loadClass("Main", true).newInstance();
 *       &nbsp;.&nbsp;.&nbsp;.
 * </pre></blockquote>
 *
 * <p> The network class loader subclass must define the methods {@link
 * #findClass findClass} and {@code loadClassData} to load a class
 * from the network.  Once it has downloaded the bytes that make up the class,
 * it should use the method {@link #defineClass defineClass} to
 * create a class instance.  A sample implementation is:
 *
 * <blockquote><pre>
 *     class NetworkClassLoader extends ClassLoader {
 *         String host;
 *         int port;
 *
 *         public Class findClass(String name) {
 *             byte[] b = loadClassData(name);
 *             return defineClass(name, b, 0, b.length);
 *         }
 *
 *         private byte[] loadClassData(String name) {
 *             // load the class data from the connection
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote>
 *
 * <h3> <a id="binary-name">Binary names</a> </h3>
 *
 * <p> Any class name provided as a {@code String} parameter to methods in
 * {@code ClassLoader} must be a binary name as defined by
 * <cite>The Java Language Specification</cite>.
 *
 * <p> Examples of valid class names include:
 * <blockquote><pre>
 *   "java.lang.String"
 *   "javax.swing.JSpinner$DefaultEditor"
 *   "java.security.KeyStore$Builder$FileBuilder$1"
 *   "java.net.URLClassLoader$3$1"
 * </pre></blockquote>
 *
 * <p> Any package name provided as a {@code String} parameter to methods in
 * {@code ClassLoader} must be either the empty string (denoting an unnamed package)
 * or a fully qualified name as defined by
 * <cite>The Java Language Specification</cite>.
 *
 * @jls 6.7 Fully Qualified Names
 * @jls 13.1 The Form of a Binary
 * @see      #resolveClass(Class)
 * @since 1.0
 * @revised 9
 */
public abstract class ClassLoader {

    private static native void registerNatives();
    static {
        registerNatives();
    }

    // The parent class loader for delegation
    // Note: VM hardcoded the offset of this field, thus all new fields
    // must be added *after* it.
    private final ClassLoader parent;

    // class loader name
    private final String name;

    // the unnamed module for this ClassLoader
    private final Module unnamedModule;

    // a string for exception message printing
    private final String nameAndId;

    /**
     * Encapsulates the set of parallel capable loader types.
     */
    private static class ParallelLoaders {
        private ParallelLoaders() {}

        // the set of parallel capable loader types
        private static final Set<Class<? extends ClassLoader>> loaderTypes =
            Collections.newSetFromMap(new WeakHashMap<>());
        static {
            synchronized (loaderTypes) { loaderTypes.add(ClassLoader.class); }
        }

        /**
         * Registers the given class loader type as parallel capable.
         * Returns {@code true} is successfully registered; {@code false} if
         * loader's super class is not registered.
         */
        static boolean register(Class<? extends ClassLoader> c) {
            synchronized (loaderTypes) {
                if (loaderTypes.contains(c.getSuperclass())) {
                    // register the class loader as parallel capable
                    // if and only if all of its super classes are.
                    // Note: given current classloading sequence, if
                    // the immediate super class is parallel capable,
                    // all the super classes higher up must be too.
                    loaderTypes.add(c);
                    return true;
                } else {
                    return false;
                }
            }
        }

        /**
         * Returns {@code true} if the given class loader type is
         * registered as parallel capable.
         */
        static boolean isRegistered(Class<? extends ClassLoader> c) {
            synchronized (loaderTypes) {
                return loaderTypes.contains(c);
            }
        }
    }

    // Maps class name to the corresponding lock object when the current
    // class loader is parallel capable.
    // Note: VM also uses this field to decide if the current class loader
    // is parallel capable and the appropriate lock object for class loading.
    private final ConcurrentHashMap<String, Object> parallelLockMap;

    // Maps packages to certs
    private final ConcurrentHashMap<String, Certificate[]> package2certs;

    // Shared among all packages with unsigned classes
    private static final Certificate[] nocerts = new Certificate[0];

    // The classes loaded by this class loader. The only purpose of this table
    // is to keep the classes from being GC'ed until the loader is GC'ed.
    private final ArrayList<Class<?>> classes = new ArrayList<>();

    // The "default" domain. Set as the default ProtectionDomain on newly
    // created classes.
    private final ProtectionDomain defaultDomain =
        new ProtectionDomain(new CodeSource(null, (Certificate[]) null),
                             null, this, null);

    // Invoked by the VM to record every loaded class with this loader.
    void addClass(Class<?> c) {
        synchronized (classes) {
            classes.add(c);
        }
    }

    // The packages defined in this class loader.  Each package name is
    // mapped to its corresponding NamedPackage object.
    //
    // The value is a Package object if ClassLoader::definePackage,
    // Class::getPackage, ClassLoader::getDefinePackage(s) or
    // Package::getPackage(s) method is called to define it.
    // Otherwise, the value is a NamedPackage object.
    private final ConcurrentHashMap<String, NamedPackage> packages
            = new ConcurrentHashMap<>();

    /*
     * Returns a named package for the given module.
     */
    private NamedPackage getNamedPackage(String pn, Module m) {
        NamedPackage p = packages.get(pn);
        if (p == null) {
            p = new NamedPackage(pn, m);

            NamedPackage value = packages.putIfAbsent(pn, p);
            if (value != null) {
                // Package object already be defined for the named package
                p = value;
                // if definePackage is called by this class loader to define
                // a package in a named module, this will return Package
                // object of the same name.  Package object may contain
                // unexpected information but it does not impact the runtime.
                // this assertion may be helpful for troubleshooting
                assert value.module() == m;
            }
        }
        return p;
    }

    private static Void checkCreateClassLoader() {
        return checkCreateClassLoader(null);
    }

    private static Void checkCreateClassLoader(String name) {
        if (name != null && name.isEmpty()) {
            throw new IllegalArgumentException("name must be non-empty or null");
        }

        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        return null;
    }

    private ClassLoader(Void unused, String name, ClassLoader parent) {
        this.name = name;
        this.parent = parent;
        this.unnamedModule = new Module(this);
        if (ParallelLoaders.isRegistered(this.getClass())) {
            parallelLockMap = new ConcurrentHashMap<>();
            assertionLock = new Object();
        } else {
            // no finer-grained lock; lock on the classloader instance
            parallelLockMap = null;
            assertionLock = this;
        }
        this.package2certs = new ConcurrentHashMap<>();
        this.nameAndId = nameAndId(this);
    }

    /**
     * If the defining loader has a name explicitly set then
     *       '<loader-name>' @<id>
     * If the defining loader has no name then
     *       <qualified-class-name> @<id>
     * If it's built-in loader then omit `@<id>` as there is only one instance.
     */
    private static String nameAndId(ClassLoader ld) {
        String nid = ld.getName() != null ? "\'" + ld.getName() + "\'"
                                          : ld.getClass().getName();
        if (!(ld instanceof BuiltinClassLoader)) {
            String id = Integer.toHexString(System.identityHashCode(ld));
            nid = nid + " @" + id;
        }
        return nid;
    }

    /**
     * Creates a new class loader of the specified name and using the
     * specified parent class loader for delegation.
     *
     * @apiNote If the parent is specified as {@code null} (for the
     * bootstrap class loader) then there is no guarantee that all platform
     * classes are visible.
     *
     * @param  name   class loader name; or {@code null} if not named
     * @param  parent the parent class loader
     *
     * @throws IllegalArgumentException if the given name is empty.
     *
     * @throws SecurityException
     *         If a security manager exists and its
     *         {@link SecurityManager#checkCreateClassLoader()}
     *         method doesn't allow creation of a new class loader.
     *
     * @since  9
     */
    protected ClassLoader(String name, ClassLoader parent) {
        this(checkCreateClassLoader(name), name, parent);
    }

    /**
     * Creates a new class loader using the specified parent class loader for
     * delegation.
     *
     * <p> If there is a security manager, its {@link
     * SecurityManager#checkCreateClassLoader() checkCreateClassLoader} method
     * is invoked.  This may result in a security exception.  </p>
     *
     * @apiNote If the parent is specified as {@code null} (for the
     * bootstrap class loader) then there is no guarantee that all platform
     * classes are visible.
     *
     * @param  parent
     *         The parent class loader
     *
     * @throws SecurityException
     *         If a security manager exists and its
     *         {@code checkCreateClassLoader} method doesn't allow creation
     *         of a new class loader.
     *
     * @since  1.2
     */
    protected ClassLoader(ClassLoader parent) {
        this(checkCreateClassLoader(), null, parent);
    }

    /**
     * Creates a new class loader using the {@code ClassLoader} returned by
     * the method {@link #getSystemClassLoader()
     * getSystemClassLoader()} as the parent class loader.
     *
     * <p> If there is a security manager, its {@link
     * SecurityManager#checkCreateClassLoader()
     * checkCreateClassLoader} method is invoked.  This may result in
     * a security exception.  </p>
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@code checkCreateClassLoader} method doesn't allow creation
     *          of a new class loader.
     */
    protected ClassLoader() {
        this(checkCreateClassLoader(), null, getSystemClassLoader());
    }

    /**
     * Returns the name of this class loader or {@code null} if
     * this class loader is not named.
     *
     * @apiNote This method is non-final for compatibility.  If this
     * method is overridden, this method must return the same name
     * as specified when this class loader was instantiated.
     *
     * @return name of this class loader; or {@code null} if
     * this class loader is not named.
     *
     * @since 9
     */
    public String getName() {
        return name;
    }

    // package-private used by StackTraceElement to avoid
    // calling the overrideable getName method
    final String name() {
        return name;
    }

    // -- Class --

    /**
     * Loads the class with the specified <a href="#binary-name">binary name</a>.
     * This method searches for classes in the same manner as the {@link
     * #loadClass(String, boolean)} method.  It is invoked by the Java virtual
     * machine to resolve class references.  Invoking this method is equivalent
     * to invoking {@link #loadClass(String, boolean) loadClass(name,
     * false)}.
     *
     * @param   name
     *          The <a href="#binary-name">binary name</a> of the class
     *
     * @return  The resulting {@code Class} object
     *
     * @throws  ClassNotFoundException
     *          If the class was not found
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * Loads the class with the specified <a href="#binary-name">binary name</a>.  The
     * default implementation of this method searches for classes in the
     * following order:
     *
     * <ol>
     *
     *   <li><p> Invoke {@link #findLoadedClass(String)} to check if the class
     *   has already been loaded.  </p></li>
     *
     *   <li><p> Invoke the {@link #loadClass(String) loadClass} method
     *   on the parent class loader.  If the parent is {@code null} the class
     *   loader built into the virtual machine is used, instead.  </p></li>
     *
     *   <li><p> Invoke the {@link #findClass(String)} method to find the
     *   class.  </p></li>
     *
     * </ol>
     *
     * <p> If the class was found using the above steps, and the
     * {@code resolve} flag is true, this method will then invoke the {@link
     * #resolveClass(Class)} method on the resulting {@code Class} object.
     *
     * <p> Subclasses of {@code ClassLoader} are encouraged to override {@link
     * #findClass(String)}, rather than this method.  </p>
     *
     * <p> Unless overridden, this method synchronizes on the result of
     * {@link #getClassLoadingLock getClassLoadingLock} method
     * during the entire class loading process.
     *
     * @param   name
     *          The <a href="#binary-name">binary name</a> of the class
     *
     * @param   resolve
     *          If {@code true} then resolve the class
     *
     * @return  The resulting {@code Class} object
     *
     * @throws  ClassNotFoundException
     *          If the class could not be found
     */
    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    if (parent != null) {
                        c = parent.loadClass(name, false);
                    } else {
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    long t1 = System.nanoTime();
                    c = findClass(name);

                    // this is the defining class loader; record the stats
                    PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    PerfCounter.getFindClasses().increment();
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    /**
     * Loads the class with the specified <a href="#binary-name">binary name</a>
     * in a module defined to this class loader.  This method returns {@code null}
     * if the class could not be found.
     *
     * @apiNote This method does not delegate to the parent class loader.
     *
     * @implSpec The default implementation of this method searches for classes
     * in the following order:
     *
     * <ol>
     *   <li>Invoke {@link #findLoadedClass(String)} to check if the class
     *   has already been loaded.</li>
     *   <li>Invoke the {@link #findClass(String, String)} method to find the
     *   class in the given module.</li>
     * </ol>
     *
     * @param  module
     *         The module
     * @param  name
     *         The <a href="#binary-name">binary name</a> of the class
     *
     * @return The resulting {@code Class} object in a module defined by
     *         this class loader, or {@code null} if the class could not be found.
     */
    final Class<?> loadClass(Module module, String name) {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = findClass(module.getName(), name);
            }
            if (c != null && c.getModule() == module) {
                return c;
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the lock object for class loading operations.
     * For backward compatibility, the default implementation of this method
     * behaves as follows. If this ClassLoader object is registered as
     * parallel capable, the method returns a dedicated object associated
     * with the specified class name. Otherwise, the method returns this
     * ClassLoader object.
     *
     * @param  className
     *         The name of the to-be-loaded class
     *
     * @return the lock for class loading operations
     *
     * @throws NullPointerException
     *         If registered as parallel capable and {@code className} is null
     *
     * @see #loadClass(String, boolean)
     *
     * @since  1.7
     */
    protected Object getClassLoadingLock(String className) {
        Object lock = this;
        if (parallelLockMap != null) {
            Object newLock = new Object();
            lock = parallelLockMap.putIfAbsent(className, newLock);
            if (lock == null) {
                lock = newLock;
            }
        }
        return lock;
    }

    // Invoked by the VM after loading class with this loader.
    @SuppressWarnings("removal")
    private void checkPackageAccess(Class<?> cls, ProtectionDomain pd) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (ReflectUtil.isNonPublicProxyClass(cls)) {
                for (Class<?> intf: cls.getInterfaces()) {
                    checkPackageAccess(intf, pd);
                }
                return;
            }

            final String packageName = cls.getPackageName();
            if (!packageName.isEmpty()) {
                AccessController.doPrivileged(new PrivilegedAction<>() {
                    public Void run() {
                        sm.checkPackageAccess(packageName);
                        return null;
                    }
                }, new AccessControlContext(new ProtectionDomain[] {pd}));
            }
        }
    }

    /**
     * Finds the class with the specified <a href="#binary-name">binary name</a>.
     * This method should be overridden by class loader implementations that
     * follow the delegation model for loading classes, and will be invoked by
     * the {@link #loadClass loadClass} method after checking the
     * parent class loader for the requested class.
     *
     * @implSpec The default implementation throws {@code ClassNotFoundException}.
     *
     * @param   name
     *          The <a href="#binary-name">binary name</a> of the class
     *
     * @return  The resulting {@code Class} object
     *
     * @throws  ClassNotFoundException
     *          If the class could not be found
     *
     * @since  1.2
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    /**
     * Finds the class with the given <a href="#binary-name">binary name</a>
     * in a module defined to this class loader.
     * Class loader implementations that support loading from modules
     * should override this method.
     *
     * @apiNote This method returns {@code null} rather than throwing
     *          {@code ClassNotFoundException} if the class could not be found.
     *
     * @implSpec The default implementation attempts to find the class by
     * invoking {@link #findClass(String)} when the {@code moduleName} is
     * {@code null}. It otherwise returns {@code null}.
     *
     * @param  moduleName
     *         The module name; or {@code null} to find the class in the
     *         {@linkplain #getUnnamedModule() unnamed module} for this
     *         class loader
     * @param  name
     *         The <a href="#binary-name">binary name</a> of the class
     *
     * @return The resulting {@code Class} object, or {@code null}
     *         if the class could not be found.
     *
     * @since 9
     */
    protected Class<?> findClass(String moduleName, String name) {
        if (moduleName == null) {
            try {
                return findClass(name);
            } catch (ClassNotFoundException ignore) { }
        }
        return null;
    }


    /**
     * Converts an array of bytes into an instance of class {@code Class}.
     * Before the {@code Class} can be used it must be resolved.  This method
     * is deprecated in favor of the version that takes a <a
     * href="#binary-name">binary name</a> as its first argument, and is more secure.
     *
     * @param  b
     *         The bytes that make up the class data.  The bytes in positions
     *         {@code off} through {@code off+len-1} should have the format
     *         of a valid class file as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *
     * @param  off
     *         The start offset in {@code b} of the class data
     *
     * @param  len
     *         The length of the class data
     *
     * @return  The {@code Class} object that was created from the specified
     *          class data
     *
     * @throws  ClassFormatError
     *          If the data did not contain a valid class
     *
     * @throws  IndexOutOfBoundsException
     *          If either {@code off} or {@code len} is negative, or if
     *          {@code off+len} is greater than {@code b.length}.
     *
     * @throws  SecurityException
     *          If an attempt is made to add this class to a package that
     *          contains classes that were signed by a different set of
     *          certificates than this class, or if an attempt is made
     *          to define a class in a package with a fully-qualified name
     *          that starts with "{@code java.}".
     *
     * @see  #loadClass(String, boolean)
     * @see  #resolveClass(Class)
     *
     * @deprecated  Replaced by {@link #defineClass(String, byte[], int, int)
     * defineClass(String, byte[], int, int)}
     */
    @Deprecated(since="1.1")
    protected final Class<?> defineClass(byte[] b, int off, int len)
        throws ClassFormatError
    {
        return defineClass(null, b, off, len, null);
    }

    /**
     * Converts an array of bytes into an instance of class {@code Class}.
     * Before the {@code Class} can be used it must be resolved.
     *
     * <p> This method assigns a default {@link java.security.ProtectionDomain
     * ProtectionDomain} to the newly defined class.  The
     * {@code ProtectionDomain} is effectively granted the same set of
     * permissions returned when {@link
     * java.security.Policy#getPermissions(java.security.CodeSource)
     * Policy.getPolicy().getPermissions(new CodeSource(null, null))}
     * is invoked.  The default protection domain is created on the first invocation
     * of {@link #defineClass(String, byte[], int, int) defineClass},
     * and re-used on subsequent invocations.
     *
     * <p> To assign a specific {@code ProtectionDomain} to the class, use
     * the {@link #defineClass(String, byte[], int, int,
     * java.security.ProtectionDomain) defineClass} method that takes a
     * {@code ProtectionDomain} as one of its arguments.  </p>
     *
     * <p>
     * This method defines a package in this class loader corresponding to the
     * package of the {@code Class} (if such a package has not already been defined
     * in this class loader). The name of the defined package is derived from
     * the <a href="#binary-name">binary name</a> of the class specified by
     * the byte array {@code b}.
     * Other properties of the defined package are as specified by {@link Package}.
     *
     * @param  name
     *         The expected <a href="#binary-name">binary name</a> of the class, or
     *         {@code null} if not known
     *
     * @param  b
     *         The bytes that make up the class data.  The bytes in positions
     *         {@code off} through {@code off+len-1} should have the format
     *         of a valid class file as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *
     * @param  off
     *         The start offset in {@code b} of the class data
     *
     * @param  len
     *         The length of the class data
     *
     * @return  The {@code Class} object that was created from the specified
     *          class data.
     *
     * @throws  ClassFormatError
     *          If the data did not contain a valid class
     *
     * @throws  IndexOutOfBoundsException
     *          If either {@code off} or {@code len} is negative, or if
     *          {@code off+len} is greater than {@code b.length}.
     *
     * @throws  SecurityException
     *          If an attempt is made to add this class to a package that
     *          contains classes that were signed by a different set of
     *          certificates than this class (which is unsigned), or if
     *          {@code name} begins with "{@code java.}".
     *
     * @see  #loadClass(String, boolean)
     * @see  #resolveClass(Class)
     * @see  java.security.CodeSource
     * @see  java.security.SecureClassLoader
     *
     * @since  1.1
     * @revised 9
     */
    protected final Class<?> defineClass(String name, byte[] b, int off, int len)
        throws ClassFormatError
    {
        return defineClass(name, b, off, len, null);
    }

    /* Determine protection domain, and check that:
        - not define java.* class,
        - signer of this class matches signers for the rest of the classes in
          package.
    */
    private ProtectionDomain preDefineClass(String name,
                                            ProtectionDomain pd)
    {
        if (!checkName(name))
            throw new NoClassDefFoundError("IllegalName: " + name);

        // Note:  Checking logic in java.lang.invoke.MemberName.checkForTypeAlias
        // relies on the fact that spoofing is impossible if a class has a name
        // of the form "java.*"
        if ((name != null) && name.startsWith("java.")
                && this != getBuiltinPlatformClassLoader()) {
            throw new SecurityException
                ("Prohibited package name: " +
                 name.substring(0, name.lastIndexOf('.')));
        }
        if (pd == null) {
            pd = defaultDomain;
        }

        if (name != null) {
            checkCerts(name, pd.getCodeSource());
        }

        return pd;
    }

    private String defineClassSourceLocation(ProtectionDomain pd) {
        CodeSource cs = pd.getCodeSource();
        String source = null;
        if (cs != null && cs.getLocation() != null) {
            source = cs.getLocation().toString();
        }
        return source;
    }

    private void postDefineClass(Class<?> c, ProtectionDomain pd) {
        // define a named package, if not present
        getNamedPackage(c.getPackageName(), c.getModule());

        if (pd.getCodeSource() != null) {
            Certificate certs[] = pd.getCodeSource().getCertificates();
            if (certs != null)
                setSigners(c, certs);
        }
    }

    /**
     * Converts an array of bytes into an instance of class {@code Class},
     * with a given {@code ProtectionDomain}.
     *
     * <p> If the given {@code ProtectionDomain} is {@code null},
     * then a default protection domain will be assigned to the class as specified
     * in the documentation for {@link #defineClass(String, byte[], int, int)}.
     * Before the class can be used it must be resolved.
     *
     * <p> The first class defined in a package determines the exact set of
     * certificates that all subsequent classes defined in that package must
     * contain.  The set of certificates for a class is obtained from the
     * {@link java.security.CodeSource CodeSource} within the
     * {@code ProtectionDomain} of the class.  Any classes added to that
     * package must contain the same set of certificates or a
     * {@code SecurityException} will be thrown.  Note that if
     * {@code name} is {@code null}, this check is not performed.
     * You should always pass in the <a href="#binary-name">binary name</a> of the
     * class you are defining as well as the bytes.  This ensures that the
     * class you are defining is indeed the class you think it is.
     *
     * <p> If the specified {@code name} begins with "{@code java.}", it can
     * only be defined by the {@linkplain #getPlatformClassLoader()
     * platform class loader} or its ancestors; otherwise {@code SecurityException}
     * will be thrown.  If {@code name} is not {@code null}, it must be equal to
     * the <a href="#binary-name">binary name</a> of the class
     * specified by the byte array {@code b}, otherwise a {@link
     * NoClassDefFoundError NoClassDefFoundError} will be thrown.
     *
     * <p> This method defines a package in this class loader corresponding to the
     * package of the {@code Class} (if such a package has not already been defined
     * in this class loader). The name of the defined package is derived from
     * the <a href="#binary-name">binary name</a> of the class specified by
     * the byte array {@code b}.
     * Other properties of the defined package are as specified by {@link Package}.
     *
     * @param  name
     *         The expected <a href="#binary-name">binary name</a> of the class, or
     *         {@code null} if not known
     *
     * @param  b
     *         The bytes that make up the class data. The bytes in positions
     *         {@code off} through {@code off+len-1} should have the format
     *         of a valid class file as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *
     * @param  off
     *         The start offset in {@code b} of the class data
     *
     * @param  len
     *         The length of the class data
     *
     * @param  protectionDomain
     *         The {@code ProtectionDomain} of the class
     *
     * @return  The {@code Class} object created from the data,
     *          and {@code ProtectionDomain}.
     *
     * @throws  ClassFormatError
     *          If the data did not contain a valid class
     *
     * @throws  NoClassDefFoundError
     *          If {@code name} is not {@code null} and not equal to the
     *          <a href="#binary-name">binary name</a> of the class specified by {@code b}
     *
     * @throws  IndexOutOfBoundsException
     *          If either {@code off} or {@code len} is negative, or if
     *          {@code off+len} is greater than {@code b.length}.
     *
     * @throws  SecurityException
     *          If an attempt is made to add this class to a package that
     *          contains classes that were signed by a different set of
     *          certificates than this class, or if {@code name} begins with
     *          "{@code java.}" and this class loader is not the platform
     *          class loader or its ancestor.
     *
     * @revised 9
     */
    protected final Class<?> defineClass(String name, byte[] b, int off, int len,
                                         ProtectionDomain protectionDomain)
        throws ClassFormatError
    {
        protectionDomain = preDefineClass(name, protectionDomain);
        String source = defineClassSourceLocation(protectionDomain);
        Class<?> c = defineClass1(this, name, b, off, len, protectionDomain, source);
        postDefineClass(c, protectionDomain);
        return c;
    }

    /**
     * Converts a {@link java.nio.ByteBuffer ByteBuffer} into an instance
     * of class {@code Class}, with the given {@code ProtectionDomain}.
     * If the given {@code ProtectionDomain} is {@code null}, then a default
     * protection domain will be assigned to the class as
     * specified in the documentation for {@link #defineClass(String, byte[],
     * int, int)}.  Before the class can be used it must be resolved.
     *
     * <p>The rules about the first class defined in a package determining the
     * set of certificates for the package, the restrictions on class names,
     * and the defined package of the class
     * are identical to those specified in the documentation for {@link
     * #defineClass(String, byte[], int, int, ProtectionDomain)}.
     *
     * <p> An invocation of this method of the form
     * <i>cl</i>{@code .defineClass(}<i>name</i>{@code ,}
     * <i>bBuffer</i>{@code ,} <i>pd</i>{@code )} yields exactly the same
     * result as the statements
     *
     *<p> <code>
     * ...<br>
     * byte[] temp = new byte[bBuffer.{@link
     * java.nio.ByteBuffer#remaining remaining}()];<br>
     *     bBuffer.{@link java.nio.ByteBuffer#get(byte[])
     * get}(temp);<br>
     *     return {@link #defineClass(String, byte[], int, int, ProtectionDomain)
     * cl.defineClass}(name, temp, 0,
     * temp.length, pd);<br>
     * </code></p>
     *
     * @param  name
     *         The expected <a href="#binary-name">binary name</a>. of the class, or
     *         {@code null} if not known
     *
     * @param  b
     *         The bytes that make up the class data. The bytes from positions
     *         {@code b.position()} through {@code b.position() + b.limit() -1
     *         } should have the format of a valid class file as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *
     * @param  protectionDomain
     *         The {@code ProtectionDomain} of the class, or {@code null}.
     *
     * @return  The {@code Class} object created from the data,
     *          and {@code ProtectionDomain}.
     *
     * @throws  ClassFormatError
     *          If the data did not contain a valid class.
     *
     * @throws  NoClassDefFoundError
     *          If {@code name} is not {@code null} and not equal to the
     *          <a href="#binary-name">binary name</a> of the class specified by {@code b}
     *
     * @throws  SecurityException
     *          If an attempt is made to add this class to a package that
     *          contains classes that were signed by a different set of
     *          certificates than this class, or if {@code name} begins with
     *          "{@code java.}".
     *
     * @see      #defineClass(String, byte[], int, int, ProtectionDomain)
     *
     * @since  1.5
     * @revised 9
     */
    protected final Class<?> defineClass(String name, java.nio.ByteBuffer b,
                                         ProtectionDomain protectionDomain)
        throws ClassFormatError
    {
        int len = b.remaining();

        // Use byte[] if not a direct ByteBuffer:
        if (!b.isDirect()) {
            if (b.hasArray()) {
                return defineClass(name, b.array(),
                                   b.position() + b.arrayOffset(), len,
                                   protectionDomain);
            } else {
                // no array, or read-only array
                byte[] tb = new byte[len];
                b.get(tb);  // get bytes out of byte buffer.
                return defineClass(name, tb, 0, len, protectionDomain);
            }
        }

        protectionDomain = preDefineClass(name, protectionDomain);
        String source = defineClassSourceLocation(protectionDomain);
        Class<?> c = defineClass2(this, name, b, b.position(), len, protectionDomain, source);
        postDefineClass(c, protectionDomain);
        return c;
    }

    static native Class<?> defineClass1(ClassLoader loader, String name, byte[] b, int off, int len,
                                        ProtectionDomain pd, String source);

    static native Class<?> defineClass2(ClassLoader loader, String name, java.nio.ByteBuffer b,
                                        int off, int len, ProtectionDomain pd,
                                        String source);

    /**
     * Defines a class of the given flags via Lookup.defineClass.
     *
     * @param loader the defining loader
     * @param lookup nest host of the Class to be defined
     * @param name the binary name or {@code null} if not findable
     * @param b class bytes
     * @param off the start offset in {@code b} of the class bytes
     * @param len the length of the class bytes
     * @param pd protection domain
     * @param initialize initialize the class
     * @param flags flags
     * @param classData class data
     */
    static native Class<?> defineClass0(ClassLoader loader,
                                        Class<?> lookup,
                                        String name,
                                        byte[] b, int off, int len,
                                        ProtectionDomain pd,
                                        boolean initialize,
                                        int flags,
                                        Object classData);

    // true if the name is null or has the potential to be a valid binary name
    private static boolean checkName(String name) {
        if ((name == null) || (name.isEmpty()))
            return true;
        if ((name.indexOf('/') != -1) || (name.charAt(0) == '['))
            return false;
        return true;
    }

    private void checkCerts(String name, CodeSource cs) {
        int i = name.lastIndexOf('.');
        String pname = (i == -1) ? "" : name.substring(0, i);

        Certificate[] certs = null;
        if (cs != null) {
            certs = cs.getCertificates();
        }
        certs = certs == null ? nocerts : certs;
        Certificate[] pcerts = package2certs.putIfAbsent(pname, certs);
        if (pcerts != null && !compareCerts(pcerts, certs)) {
            throw new SecurityException("class \"" + name
                + "\"'s signer information does not match signer information"
                + " of other classes in the same package");
        }
    }

    /**
     * check to make sure the certs for the new class (certs) are the same as
     * the certs for the first class inserted in the package (pcerts)
     */
    private boolean compareCerts(Certificate[] pcerts, Certificate[] certs) {
        // empty array fast-path
        if (certs.length == 0)
            return pcerts.length == 0;

        // the length must be the same at this point
        if (certs.length != pcerts.length)
            return false;

        // go through and make sure all the certs in one array
        // are in the other and vice-versa.
        boolean match;
        for (Certificate cert : certs) {
            match = false;
            for (Certificate pcert : pcerts) {
                if (cert.equals(pcert)) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        // now do the same for pcerts
        for (Certificate pcert : pcerts) {
            match = false;
            for (Certificate cert : certs) {
                if (pcert.equals(cert)) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        return true;
    }

    /**
     * Links the specified class.  This (misleadingly named) method may be
     * used by a class loader to link a class.  If the class {@code c} has
     * already been linked, then this method simply returns. Otherwise, the
     * class is linked as described in the "Execution" chapter of
     * <cite>The Java Language Specification</cite>.
     *
     * @param  c
     *         The class to link
     *
     * @throws  NullPointerException
     *          If {@code c} is {@code null}.
     *
     * @see  #defineClass(String, byte[], int, int)
     */
    protected final void resolveClass(Class<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Finds a class with the specified <a href="#binary-name">binary name</a>,
     * loading it if necessary.
     *
     * <p> This method loads the class through the system class loader (see
     * {@link #getSystemClassLoader()}).  The {@code Class} object returned
     * might have more than one {@code ClassLoader} associated with it.
     * Subclasses of {@code ClassLoader} need not usually invoke this method,
     * because most class loaders need to override just {@link
     * #findClass(String)}.  </p>
     *
     * @param  name
     *         The <a href="#binary-name">binary name</a> of the class
     *
     * @return  The {@code Class} object for the specified {@code name}
     *
     * @throws  ClassNotFoundException
     *          If the class could not be found
     *
     * @see  #ClassLoader(ClassLoader)
     * @see  #getParent()
     */
    protected final Class<?> findSystemClass(String name)
        throws ClassNotFoundException
    {
        return getSystemClassLoader().loadClass(name);
    }

    /**
     * Returns a class loaded by the bootstrap class loader;
     * or return null if not found.
     */
    static Class<?> findBootstrapClassOrNull(String name) {
        if (!checkName(name)) return null;

        return findBootstrapClass(name);
    }

    // return null if not found
    private static native Class<?> findBootstrapClass(String name);

    /**
     * Returns the class with the given <a href="#binary-name">binary name</a> if this
     * loader has been recorded by the Java virtual machine as an initiating
     * loader of a class with that <a href="#binary-name">binary name</a>.  Otherwise
     * {@code null} is returned.
     *
     * @param  name
     *         The <a href="#binary-name">binary name</a> of the class
     *
     * @return  The {@code Class} object, or {@code null} if the class has
     *          not been loaded
     *
     * @since  1.1
     */
    protected final Class<?> findLoadedClass(String name) {
        if (!checkName(name))
            return null;
        return findLoadedClass0(name);
    }

    private final native Class<?> findLoadedClass0(String name);

    /**
     * Sets the signers of a class.  This should be invoked after defining a
     * class.
     *
     * @param  c
     *         The {@code Class} object
     *
     * @param  signers
     *         The signers for the class
     *
     * @since  1.1
     */
    protected final void setSigners(Class<?> c, Object[] signers) {
        c.setSigners(signers);
    }


    // -- Resources --

    /**
     * Returns a URL to a resource in a module defined to this class loader.
     * Class loader implementations that support loading from modules
     * should override this method.
     *
     * @apiNote This method is the basis for the {@link
     * Class#getResource Class.getResource}, {@link Class#getResourceAsStream
     * Class.getResourceAsStream}, and {@link Module#getResourceAsStream
     * Module.getResourceAsStream} methods. It is not subject to the rules for
     * encapsulation specified by {@code Module.getResourceAsStream}.
     *
     * @implSpec The default implementation attempts to find the resource by
     * invoking {@link #findResource(String)} when the {@code moduleName} is
     * {@code null}. It otherwise returns {@code null}.
     *
     * @param  moduleName
     *         The module name; or {@code null} to find a resource in the
     *         {@linkplain #getUnnamedModule() unnamed module} for this
     *         class loader
     * @param  name
     *         The resource name
     *
     * @return A URL to the resource; {@code null} if the resource could not be
     *         found, a URL could not be constructed to locate the resource,
     *         access to the resource is denied by the security manager, or
     *         there isn't a module of the given name defined to the class
     *         loader.
     *
     * @throws IOException
     *         If I/O errors occur
     *
     * @see java.lang.module.ModuleReader#find(String)
     * @since 9
     */
    protected URL findResource(String moduleName, String name) throws IOException {
        if (moduleName == null) {
            return findResource(name);
        } else {
            return null;
        }
    }

    /**
     * Finds the resource with the given name.  A resource is some data
     * (images, audio, text, etc) that can be accessed by class code in a way
     * that is independent of the location of the code.
     *
     * <p> The name of a resource is a '{@code /}'-separated path name that
     * identifies the resource. </p>
     *
     * <p> Resources in named modules are subject to the encapsulation rules
     * specified by {@link Module#getResourceAsStream Module.getResourceAsStream}.
     * Additionally, and except for the special case where the resource has a
     * name ending with "{@code .class}", this method will only find resources in
     * packages of named modules when the package is {@link Module#isOpen(String)
     * opened} unconditionally (even if the caller of this method is in the
     * same module as the resource). </p>
     *
     * @implSpec The default implementation will first search the parent class
     * loader for the resource; if the parent is {@code null} the path of the
     * class loader built into the virtual machine is searched. If not found,
     * this method will invoke {@link #findResource(String)} to find the resource.
     *
     * @apiNote Where several modules are defined to the same class loader,
     * and where more than one module contains a resource with the given name,
     * then the ordering that modules are searched is not specified and may be
     * very unpredictable.
     * When overriding this method it is recommended that an implementation
     * ensures that any delegation is consistent with the {@link
     * #getResources(java.lang.String) getResources(String)} method.
     *
     * @param  name
     *         The resource name
     *
     * @return  {@code URL} object for reading the resource; {@code null} if
     *          the resource could not be found, a {@code URL} could not be
     *          constructed to locate the resource, the resource is in a package
     *          that is not opened unconditionally, or access to the resource is
     *          denied by the security manager.
     *
     * @throws  NullPointerException If {@code name} is {@code null}
     *
     * @since  1.1
     * @revised 9
     */
    public URL getResource(String name) {
        Objects.requireNonNull(name);
        URL url;
        if (parent != null) {
            url = parent.getResource(name);
        } else {
            url = BootLoader.findResource(name);
        }
        if (url == null) {
            url = findResource(name);
        }
        return url;
    }

    /**
     * Finds all the resources with the given name. A resource is some data
     * (images, audio, text, etc) that can be accessed by class code in a way
     * that is independent of the location of the code.
     *
     * <p> The name of a resource is a {@code /}-separated path name that
     * identifies the resource. </p>
     *
     * <p> Resources in named modules are subject to the encapsulation rules
     * specified by {@link Module#getResourceAsStream Module.getResourceAsStream}.
     * Additionally, and except for the special case where the resource has a
     * name ending with "{@code .class}", this method will only find resources in
     * packages of named modules when the package is {@link Module#isOpen(String)
     * opened} unconditionally (even if the caller of this method is in the
     * same module as the resource). </p>
     *
     * @implSpec The default implementation will first search the parent class
     * loader for the resource; if the parent is {@code null} the path of the
     * class loader built into the virtual machine is searched. It then
     * invokes {@link #findResources(String)} to find the resources with the
     * name in this class loader. It returns an enumeration whose elements
     * are the URLs found by searching the parent class loader followed by
     * the elements found with {@code findResources}.
     *
     * @apiNote Where several modules are defined to the same class loader,
     * and where more than one module contains a resource with the given name,
     * then the ordering is not specified and may be very unpredictable.
     * When overriding this method it is recommended that an
     * implementation ensures that any delegation is consistent with the {@link
     * #getResource(java.lang.String) getResource(String)} method. This should
     * ensure that the first element returned by the Enumeration's
     * {@code nextElement} method is the same resource that the
     * {@code getResource(String)} method would return.
     *
     * @param  name
     *         The resource name
     *
     * @return  An enumeration of {@link java.net.URL URL} objects for the
     *          resource. If no resources could be found, the enumeration will
     *          be empty. Resources for which a {@code URL} cannot be
     *          constructed, are in a package that is not opened
     *          unconditionally, or access to the resource is denied by the
     *          security manager, are not returned in the enumeration.
     *
     * @throws  IOException
     *          If I/O errors occur
     * @throws  NullPointerException If {@code name} is {@code null}
     *
     * @since  1.2
     * @revised 9
     */
    public Enumeration<URL> getResources(String name) throws IOException {
        Objects.requireNonNull(name);
        @SuppressWarnings("unchecked")
        Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[2];
        if (parent != null) {
            tmp[0] = parent.getResources(name);
        } else {
            tmp[0] = BootLoader.findResources(name);
        }
        tmp[1] = findResources(name);

        return new CompoundEnumeration<>(tmp);
    }

    /**
     * Returns a stream whose elements are the URLs of all the resources with
     * the given name. A resource is some data (images, audio, text, etc) that
     * can be accessed by class code in a way that is independent of the
     * location of the code.
     *
     * <p> The name of a resource is a {@code /}-separated path name that
     * identifies the resource.
     *
     * <p> The resources will be located when the returned stream is evaluated.
     * If the evaluation results in an {@code IOException} then the I/O
     * exception is wrapped in an {@link UncheckedIOException} that is then
     * thrown.
     *
     * <p> Resources in named modules are subject to the encapsulation rules
     * specified by {@link Module#getResourceAsStream Module.getResourceAsStream}.
     * Additionally, and except for the special case where the resource has a
     * name ending with "{@code .class}", this method will only find resources in
     * packages of named modules when the package is {@link Module#isOpen(String)
     * opened} unconditionally (even if the caller of this method is in the
     * same module as the resource). </p>
     *
     * @implSpec The default implementation invokes {@link #getResources(String)
     * getResources} to find all the resources with the given name and returns
     * a stream with the elements in the enumeration as the source.
     *
     * @apiNote When overriding this method it is recommended that an
     * implementation ensures that any delegation is consistent with the {@link
     * #getResource(java.lang.String) getResource(String)} method. This should
     * ensure that the first element returned by the stream is the same
     * resource that the {@code getResource(String)} method would return.
     *
     * @param  name
     *         The resource name
     *
     * @return  A stream of resource {@link java.net.URL URL} objects. If no
     *          resources could  be found, the stream will be empty. Resources
     *          for which a {@code URL} cannot be constructed, are in a package
     *          that is not opened unconditionally, or access to the resource
     *          is denied by the security manager, will not be in the stream.
     *
     * @throws  NullPointerException If {@code name} is {@code null}
     *
     * @since  9
     */
    public Stream<URL> resources(String name) {
        Objects.requireNonNull(name);
        int characteristics = Spliterator.NONNULL | Spliterator.IMMUTABLE;
        Supplier<Spliterator<URL>> si = () -> {
            try {
                return Spliterators.spliteratorUnknownSize(
                    getResources(name).asIterator(), characteristics);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        return StreamSupport.stream(si, characteristics, false);
    }

    /**
     * Finds the resource with the given name. Class loader implementations
     * should override this method.
     *
     * <p> For resources in named modules then the method must implement the
     * rules for encapsulation specified in the {@code Module} {@link
     * Module#getResourceAsStream getResourceAsStream} method. Additionally,
     * it must not find non-"{@code .class}" resources in packages of named
     * modules unless the package is {@link Module#isOpen(String) opened}
     * unconditionally. </p>
     *
     * @implSpec The default implementation returns {@code null}.
     *
     * @param  name
     *         The resource name
     *
     * @return  {@code URL} object for reading the resource; {@code null} if
     *          the resource could not be found, a {@code URL} could not be
     *          constructed to locate the resource, the resource is in a package
     *          that is not opened unconditionally, or access to the resource is
     *          denied by the security manager.
     *
     * @since  1.2
     * @revised 9
     */
    protected URL findResource(String name) {
        return null;
    }

    /**
     * Returns an enumeration of {@link java.net.URL URL} objects
     * representing all the resources with the given name. Class loader
     * implementations should override this method.
     *
     * <p> For resources in named modules then the method must implement the
     * rules for encapsulation specified in the {@code Module} {@link
     * Module#getResourceAsStream getResourceAsStream} method. Additionally,
     * it must not find non-"{@code .class}" resources in packages of named
     * modules unless the package is {@link Module#isOpen(String) opened}
     * unconditionally. </p>
     *
     * @implSpec The default implementation returns an enumeration that
     * contains no elements.
     *
     * @param  name
     *         The resource name
     *
     * @return  An enumeration of {@link java.net.URL URL} objects for
     *          the resource. If no resources could  be found, the enumeration
     *          will be empty. Resources for which a {@code URL} cannot be
     *          constructed, are in a package that is not opened unconditionally,
     *          or access to the resource is denied by the security manager,
     *          are not returned in the enumeration.
     *
     * @throws  IOException
     *          If I/O errors occur
     *
     * @since  1.2
     * @revised 9
     */
    protected Enumeration<URL> findResources(String name) throws IOException {
        return Collections.emptyEnumeration();
    }

    /**
     * Registers the caller as
     * {@linkplain #isRegisteredAsParallelCapable() parallel capable}.
     * The registration succeeds if and only if all of the following
     * conditions are met:
     * <ol>
     * <li> no instance of the caller has been created</li>
     * <li> all of the super classes (except class Object) of the caller are
     * registered as parallel capable</li>
     * </ol>
     * <p>Note that once a class loader is registered as parallel capable, there
     * is no way to change it back.</p>
     *
     * @return  {@code true} if the caller is successfully registered as
     *          parallel capable and {@code false} if otherwise.
     *
     * @see #isRegisteredAsParallelCapable()
     *
     * @since   1.7
     */
    @CallerSensitive
    protected static boolean registerAsParallelCapable() {
        Class<? extends ClassLoader> callerClass =
            Reflection.getCallerClass().asSubclass(ClassLoader.class);
        return ParallelLoaders.register(callerClass);
    }

    /**
     * Returns {@code true} if this class loader is registered as
     * {@linkplain #registerAsParallelCapable parallel capable}, otherwise
     * {@code false}.
     *
     * @return  {@code true} if this class loader is parallel capable,
     *          otherwise {@code false}.
     *
     * @see #registerAsParallelCapable()
     *
     * @since   9
     */
    public final boolean isRegisteredAsParallelCapable() {
        return ParallelLoaders.isRegistered(this.getClass());
    }

    /**
     * Find a resource of the specified name from the search path used to load
     * classes.  This method locates the resource through the system class
     * loader (see {@link #getSystemClassLoader()}).
     *
     * <p> Resources in named modules are subject to the encapsulation rules
     * specified by {@link Module#getResourceAsStream Module.getResourceAsStream}.
     * Additionally, and except for the special case where the resource has a
     * name ending with "{@code .class}", this method will only find resources in
     * packages of named modules when the package is {@link Module#isOpen(String)
     * opened} unconditionally. </p>
     *
     * @param  name
     *         The resource name
     *
     * @return  A {@link java.net.URL URL} to the resource; {@code
     *          null} if the resource could not be found, a URL could not be
     *          constructed to locate the resource, the resource is in a package
     *          that is not opened unconditionally or access to the resource is
     *          denied by the security manager.
     *
     * @since  1.1
     * @revised 9
     */
    public static URL getSystemResource(String name) {
        return getSystemClassLoader().getResource(name);
    }

    /**
     * Finds all resources of the specified name from the search path used to
     * load classes.  The resources thus found are returned as an
     * {@link java.util.Enumeration Enumeration} of {@link
     * java.net.URL URL} objects.
     *
     * <p> The search order is described in the documentation for {@link
     * #getSystemResource(String)}.  </p>
     *
     * <p> Resources in named modules are subject to the encapsulation rules
     * specified by {@link Module#getResourceAsStream Module.getResourceAsStream}.
     * Additionally, and except for the special case where the resource has a
     * name ending with "{@code .class}", this method will only find resources in
     * packages of named modules when the package is {@link Module#isOpen(String)
     * opened} unconditionally. </p>
     *
     * @param  name
     *         The resource name
     *
     * @return  An enumeration of {@link java.net.URL URL} objects for
     *          the resource. If no resources could  be found, the enumeration
     *          will be empty. Resources for which a {@code URL} cannot be
     *          constructed, are in a package that is not opened unconditionally,
     *          or access to the resource is denied by the security manager,
     *          are not returned in the enumeration.
     *
     * @throws  IOException
     *          If I/O errors occur
     *
     * @since  1.2
     * @revised 9
     */
    public static Enumeration<URL> getSystemResources(String name)
        throws IOException
    {
        return getSystemClassLoader().getResources(name);
    }

    /**
     * Returns an input stream for reading the specified resource.
     *
     * <p> The search order is described in the documentation for {@link
     * #getResource(String)}.  </p>
     *
     * <p> Resources in named modules are subject to the encapsulation rules
     * specified by {@link Module#getResourceAsStream Module.getResourceAsStream}.
     * Additionally, and except for the special case where the resource has a
     * name ending with "{@code .class}", this method will only find resources in
     * packages of named modules when the package is {@link Module#isOpen(String)
     * opened} unconditionally. </p>
     *
     * @param  name
     *         The resource name
     *
     * @return  An input stream for reading the resource; {@code null} if the
     *          resource could not be found, the resource is in a package that
     *          is not opened unconditionally, or access to the resource is
     *          denied by the security manager.
     *
     * @throws  NullPointerException If {@code name} is {@code null}
     *
     * @since  1.1
     * @revised 9
     */
    public InputStream getResourceAsStream(String name) {
        Objects.requireNonNull(name);
        URL url = getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Open for reading, a resource of the specified name from the search path
     * used to load classes.  This method locates the resource through the
     * system class loader (see {@link #getSystemClassLoader()}).
     *
     * <p> Resources in named modules are subject to the encapsulation rules
     * specified by {@link Module#getResourceAsStream Module.getResourceAsStream}.
     * Additionally, and except for the special case where the resource has a
     * name ending with "{@code .class}", this method will only find resources in
     * packages of named modules when the package is {@link Module#isOpen(String)
     * opened} unconditionally. </p>
     *
     * @param  name
     *         The resource name
     *
     * @return  An input stream for reading the resource; {@code null} if the
     *          resource could not be found, the resource is in a package that
     *          is not opened unconditionally, or access to the resource is
     *          denied by the security manager.
     *
     * @since  1.1
     * @revised 9
     */
    public static InputStream getSystemResourceAsStream(String name) {
        URL url = getSystemResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }


    // -- Hierarchy --

    /**
     * Returns the parent class loader for delegation. Some implementations may
     * use {@code null} to represent the bootstrap class loader. This method
     * will return {@code null} in such implementations if this class loader's
     * parent is the bootstrap class loader.
     *
     * @return  The parent {@code ClassLoader}
     *
     * @throws  SecurityException
     *          If a security manager is present, and the caller's class loader
     *          is not {@code null} and is not an ancestor of this class loader,
     *          and the caller does not have the
     *          {@link RuntimePermission}{@code ("getClassLoader")}
     *
     * @since  1.2
     */
    @CallerSensitive
    public final ClassLoader getParent() {
        if (parent == null)
            return null;
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // Check access to the parent class loader
            // If the caller's class loader is same as this class loader,
            // permission check is performed.
            checkClassLoaderPermission(parent, Reflection.getCallerClass());
        }
        return parent;
    }

    /**
     * Returns the unnamed {@code Module} for this class loader.
     *
     * @return The unnamed Module for this class loader
     *
     * @see Module#isNamed()
     * @since 9
     */
    public final Module getUnnamedModule() {
        return unnamedModule;
    }

    /**
     * Returns the platform class loader.  All
     * <a href="#builtinLoaders">platform classes</a> are visible to
     * the platform class loader.
     *
     * @implNote The name of the builtin platform class loader is
     * {@code "platform"}.
     *
     * @return  The platform {@code ClassLoader}.
     *
     * @throws  SecurityException
     *          If a security manager is present, and the caller's class loader is
     *          not {@code null}, and the caller's class loader is not the same
     *          as or an ancestor of the platform class loader,
     *          and the caller does not have the
     *          {@link RuntimePermission}{@code ("getClassLoader")}
     *
     * @since 9
     */
    @CallerSensitive
    public static ClassLoader getPlatformClassLoader() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        ClassLoader loader = getBuiltinPlatformClassLoader();
        if (sm != null) {
            checkClassLoaderPermission(loader, Reflection.getCallerClass());
        }
        return loader;
    }

    /**
     * Returns the system class loader.  This is the default
     * delegation parent for new {@code ClassLoader} instances, and is
     * typically the class loader used to start the application.
     *
     * <p> This method is first invoked early in the runtime's startup
     * sequence, at which point it creates the system class loader. This
     * class loader will be the context class loader for the main application
     * thread (for example, the thread that invokes the {@code main} method of
     * the main class).
     *
     * <p> The default system class loader is an implementation-dependent
     * instance of this class.
     *
     * <p> If the system property "{@systemProperty java.system.class.loader}"
     * is defined when this method is first invoked then the value of that
     * property is taken to be the name of a class that will be returned as the
     * system class loader. The class is loaded using the default system class
     * loader and must define a public constructor that takes a single parameter
     * of type {@code ClassLoader} which is used as the delegation parent. An
     * instance is then created using this constructor with the default system
     * class loader as the parameter.  The resulting class loader is defined
     * to be the system class loader. During construction, the class loader
     * should take great care to avoid calling {@code getSystemClassLoader()}.
     * If circular initialization of the system class loader is detected then
     * an {@code IllegalStateException} is thrown.
     *
     * @implNote The system property to override the system class loader is not
     * examined until the VM is almost fully initialized. Code that executes
     * this method during startup should take care not to cache the return
     * value until the system is fully initialized.
     *
     * <p> The name of the built-in system class loader is {@code "app"}.
     * The system property "{@code java.class.path}" is read during early
     * initialization of the VM to determine the class path.
     * An empty value of "{@code java.class.path}" property is interpreted
     * differently depending on whether the initial module (the module
     * containing the main class) is named or unnamed:
     * If named, the built-in system class loader will have no class path and
     * will search for classes and resources using the application module path;
     * otherwise, if unnamed, it will set the class path to the current
     * working directory.
     *
     * <p> JAR files on the class path may contain a {@code Class-Path} manifest
     * attribute to specify dependent JAR files to be included in the class path.
     * {@code Class-Path} entries must meet certain conditions for validity (see
     * the <a href="{@docRoot}/../specs/jar/jar.html#class-path-attribute">
     * JAR File Specification</a> for details).  Invalid {@code Class-Path}
     * entries are ignored.  For debugging purposes, ignored entries can be
     * printed to the console if the
     * {@systemProperty jdk.net.URLClassPath.showIgnoredClassPathEntries} system
     * property is set to {@code true}.
     *
     * @return  The system {@code ClassLoader}
     *
     * @throws  SecurityException
     *          If a security manager is present, and the caller's class loader
     *          is not {@code null} and is not the same as or an ancestor of the
     *          system class loader, and the caller does not have the
     *          {@link RuntimePermission}{@code ("getClassLoader")}
     *
     * @throws  IllegalStateException
     *          If invoked recursively during the construction of the class
     *          loader specified by the "{@code java.system.class.loader}"
     *          property.
     *
     * @throws  Error
     *          If the system property "{@code java.system.class.loader}"
     *          is defined but the named class could not be loaded, the
     *          provider class does not define the required constructor, or an
     *          exception is thrown by that constructor when it is invoked. The
     *          underlying cause of the error can be retrieved via the
     *          {@link Throwable#getCause()} method.
     *
     * @revised  1.4
     * @revised 9
     */
    @CallerSensitive
    public static ClassLoader getSystemClassLoader() {
        switch (VM.initLevel()) {
            case 0:
            case 1:
            case 2:
                // the system class loader is the built-in app class loader during startup
                return getBuiltinAppClassLoader();
            case 3:
                String msg = "getSystemClassLoader cannot be called during the system class loader instantiation";
                throw new IllegalStateException(msg);
            default:
                // system fully initialized
                assert VM.isBooted() && scl != null;
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    checkClassLoaderPermission(scl, Reflection.getCallerClass());
                }
                return scl;
        }
    }

    static ClassLoader getBuiltinPlatformClassLoader() {
        return ClassLoaders.platformClassLoader();
    }

    static ClassLoader getBuiltinAppClassLoader() {
        return ClassLoaders.appClassLoader();
    }

    /*
     * Initialize the system class loader that may be a custom class on the
     * application class path or application module path.
     *
     * @see java.lang.System#initPhase3
     */
    static synchronized ClassLoader initSystemClassLoader() {
        if (VM.initLevel() != 3) {
            throw new InternalError("system class loader cannot be set at initLevel " +
                                    VM.initLevel());
        }

        // detect recursive initialization
        if (scl != null) {
            throw new IllegalStateException("recursive invocation");
        }

        ClassLoader builtinLoader = getBuiltinAppClassLoader();

        // All are privileged frames.  No need to call doPrivileged.
        String cn = System.getProperty("java.system.class.loader");
        if (cn != null) {
            try {
                // custom class loader is only supported to be loaded from unnamed module
                Constructor<?> ctor = Class.forName(cn, false, builtinLoader)
                                           .getDeclaredConstructor(ClassLoader.class);
                scl = (ClassLoader) ctor.newInstance(builtinLoader);
            } catch (Exception e) {
                Throwable cause = e;
                if (e instanceof InvocationTargetException) {
                    cause = e.getCause();
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new Error(cause.getMessage(), cause);
            }
        } else {
            scl = builtinLoader;
        }
        return scl;
    }

    // Returns true if the specified class loader can be found in this class
    // loader's delegation chain.
    boolean isAncestor(ClassLoader cl) {
        ClassLoader acl = this;
        do {
            acl = acl.parent;
            if (cl == acl) {
                return true;
            }
        } while (acl != null);
        return false;
    }

    // Tests if class loader access requires "getClassLoader" permission
    // check.  A class loader 'from' can access class loader 'to' if
    // class loader 'from' is same as class loader 'to' or an ancestor
    // of 'to'.  The class loader in a system domain can access
    // any class loader.
    private static boolean needsClassLoaderPermissionCheck(ClassLoader from,
                                                           ClassLoader to)
    {
        if (from == to)
            return false;

        if (from == null)
            return false;

        return !to.isAncestor(from);
    }

    // Returns the class's class loader, or null if none.
    static ClassLoader getClassLoader(Class<?> caller) {
        // This can be null if the VM is requesting it
        if (caller == null) {
            return null;
        }
        // Circumvent security check since this is package-private
        return caller.getClassLoader0();
    }

    /*
     * Checks RuntimePermission("getClassLoader") permission
     * if caller's class loader is not null and caller's class loader
     * is not the same as or an ancestor of the given cl argument.
     */
    static void checkClassLoaderPermission(ClassLoader cl, Class<?> caller) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // caller can be null if the VM is requesting it
            ClassLoader ccl = getClassLoader(caller);
            if (needsClassLoaderPermissionCheck(ccl, cl)) {
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
        }
    }

    // The system class loader
    // @GuardedBy("ClassLoader.class")
    private static volatile ClassLoader scl;

    // -- Package --

    /**
     * Define a Package of the given Class object.
     *
     * If the given class represents an array type, a primitive type or void,
     * this method returns {@code null}.
     *
     * This method does not throw IllegalArgumentException.
     */
    Package definePackage(Class<?> c) {
        if (c.isPrimitive() || c.isArray()) {
            return null;
        }

        return definePackage(c.getPackageName(), c.getModule());
    }

    /**
     * Defines a Package of the given name and module
     *
     * This method does not throw IllegalArgumentException.
     *
     * @param name package name
     * @param m    module
     */
    Package definePackage(String name, Module m) {
        if (name.isEmpty() && m.isNamed()) {
            throw new InternalError("unnamed package in  " + m);
        }

        // check if Package object is already defined
        NamedPackage pkg = packages.get(name);
        if (pkg instanceof Package)
            return (Package)pkg;

        return (Package)packages.compute(name, (n, p) -> toPackage(n, p, m));
    }

    /*
     * Returns a Package object for the named package
     */
    private Package toPackage(String name, NamedPackage p, Module m) {
        // define Package object if the named package is not yet defined
        if (p == null)
            return NamedPackage.toPackage(name, m);

        // otherwise, replace the NamedPackage object with Package object
        if (p instanceof Package)
            return (Package)p;

        return NamedPackage.toPackage(p.packageName(), p.module());
    }

    /**
     * Defines a package by <a href="#binary-name">name</a> in this {@code ClassLoader}.
     * <p>
     * <a href="#binary-name">Package names</a> must be unique within a class loader and
     * cannot be redefined or changed once created.
     * <p>
     * If a class loader wishes to define a package with specific properties,
     * such as version information, then the class loader should call this
     * {@code definePackage} method before calling {@code defineClass}.
     * Otherwise, the
     * {@link #defineClass(String, byte[], int, int, ProtectionDomain) defineClass}
     * method will define a package in this class loader corresponding to the package
     * of the newly defined class; the properties of this defined package are
     * specified by {@link Package}.
     *
     * @apiNote
     * A class loader that wishes to define a package for classes in a JAR
     * typically uses the specification and implementation titles, versions, and
     * vendors from the JAR's manifest. If the package is specified as
     * {@linkplain java.util.jar.Attributes.Name#SEALED sealed} in the JAR's manifest,
     * the {@code URL} of the JAR file is typically used as the {@code sealBase}.
     * If classes of package {@code 'p'} defined by this class loader
     * are loaded from multiple JARs, the {@code Package} object may contain
     * different information depending on the first class of package {@code 'p'}
     * defined and which JAR's manifest is read first to explicitly define
     * package {@code 'p'}.
     *
     * <p> It is strongly recommended that a class loader does not call this
     * method to explicitly define packages in <em>named modules</em>; instead,
     * the package will be automatically defined when a class is {@linkplain
     * #defineClass(String, byte[], int, int, ProtectionDomain) being defined}.
     * If it is desirable to define {@code Package} explicitly, it should ensure
     * that all packages in a named module are defined with the properties
     * specified by {@link Package}.  Otherwise, some {@code Package} objects
     * in a named module may be for example sealed with different seal base.
     *
     * @param  name
     *         The <a href="#binary-name">package name</a>
     *
     * @param  specTitle
     *         The specification title
     *
     * @param  specVersion
     *         The specification version
     *
     * @param  specVendor
     *         The specification vendor
     *
     * @param  implTitle
     *         The implementation title
     *
     * @param  implVersion
     *         The implementation version
     *
     * @param  implVendor
     *         The implementation vendor
     *
     * @param  sealBase
     *         If not {@code null}, then this package is sealed with
     *         respect to the given code source {@link java.net.URL URL}
     *         object.  Otherwise, the package is not sealed.
     *
     * @return  The newly defined {@code Package} object
     *
     * @throws  NullPointerException
     *          if {@code name} is {@code null}.
     *
     * @throws  IllegalArgumentException
     *          if a package of the given {@code name} is already
     *          defined by this class loader
     *
     *
     * @since  1.2
     * @revised 9
     *
     * @jvms 5.3 Creation and Loading
     * @see <a href="{@docRoot}/../specs/jar/jar.html#package-sealing">
     *      The JAR File Specification: Package Sealing</a>
     */
    protected Package definePackage(String name, String specTitle,
                                    String specVersion, String specVendor,
                                    String implTitle, String implVersion,
                                    String implVendor, URL sealBase)
    {
        Objects.requireNonNull(name);

        // definePackage is not final and may be overridden by custom class loader
        Package p = new Package(name, specTitle, specVersion, specVendor,
                                implTitle, implVersion, implVendor,
                                sealBase, this);

        if (packages.putIfAbsent(name, p) != null)
            throw new IllegalArgumentException(name);

        return p;
    }

    /**
     * Returns a {@code Package} of the given <a href="#binary-name">name</a> that
     * has been defined by this class loader.
     *
     * @param  name The <a href="#binary-name">package name</a>
     *
     * @return The {@code Package} of the given name that has been defined
     *         by this class loader, or {@code null} if not found
     *
     * @throws  NullPointerException
     *          if {@code name} is {@code null}.
     *
     * @jvms 5.3 Creation and Loading
     *
     * @since  9
     */
    public final Package getDefinedPackage(String name) {
        Objects.requireNonNull(name, "name cannot be null");

        NamedPackage p = packages.get(name);
        if (p == null)
            return null;

        return definePackage(name, p.module());
    }

    /**
     * Returns all of the {@code Package}s that have been defined by
     * this class loader.  The returned array has no duplicated {@code Package}s
     * of the same name.
     *
     * @apiNote This method returns an array rather than a {@code Set} or {@code Stream}
     *          for consistency with the existing {@link #getPackages} method.
     *
     * @return The array of {@code Package} objects that have been defined by
     *         this class loader; or an zero length array if no package has been
     *         defined by this class loader.
     *
     * @jvms 5.3 Creation and Loading
     *
     * @since  9
     */
    public final Package[] getDefinedPackages() {
        return packages().toArray(Package[]::new);
    }

    /**
     * Finds a package by <a href="#binary-name">name</a> in this class loader and its ancestors.
     * <p>
     * If this class loader defines a {@code Package} of the given name,
     * the {@code Package} is returned. Otherwise, the ancestors of
     * this class loader are searched recursively (parent by parent)
     * for a {@code Package} of the given name.
     *
     * @apiNote The {@link #getPlatformClassLoader() platform class loader}
     * may delegate to the application class loader but the application class
     * loader is not its ancestor.  When invoked on the platform class loader,
     * this method  will not find packages defined to the application
     * class loader.
     *
     * @param  name
     *         The <a href="#binary-name">package name</a>
     *
     * @return The {@code Package} of the given name that has been defined by
     *         this class loader or its ancestors, or {@code null} if not found.
     *
     * @throws  NullPointerException
     *          if {@code name} is {@code null}.
     *
     * @deprecated
     * If multiple class loaders delegate to each other and define classes
     * with the same package name, and one such loader relies on the lookup
     * behavior of {@code getPackage} to return a {@code Package} from
     * a parent loader, then the properties exposed by the {@code Package}
     * may not be as expected in the rest of the program.
     * For example, the {@code Package} will only expose annotations from the
     * {@code package-info.class} file defined by the parent loader, even if
     * annotations exist in a {@code package-info.class} file defined by
     * a child loader.  A more robust approach is to use the
     * {@link ClassLoader#getDefinedPackage} method which returns
     * a {@code Package} for the specified class loader.
     *
     * @see ClassLoader#getDefinedPackage(String)
     *
     * @since  1.2
     * @revised 9
     */
    @Deprecated(since="9")
    protected Package getPackage(String name) {
        Package pkg = getDefinedPackage(name);
        if (pkg == null) {
            if (parent != null) {
                pkg = parent.getPackage(name);
            } else {
                pkg = BootLoader.getDefinedPackage(name);
            }
        }
        return pkg;
    }

    /**
     * Returns all of the {@code Package}s that have been defined by
     * this class loader and its ancestors.  The returned array may contain
     * more than one {@code Package} object of the same package name, each
     * defined by a different class loader in the class loader hierarchy.
     *
     * @apiNote The {@link #getPlatformClassLoader() platform class loader}
     * may delegate to the application class loader. In other words,
     * packages in modules defined to the application class loader may be
     * visible to the platform class loader.  On the other hand,
     * the application class loader is not its ancestor and hence
     * when invoked on the platform class loader, this method will not
     * return any packages defined to the application class loader.
     *
     * @return  The array of {@code Package} objects that have been defined by
     *          this class loader and its ancestors
     *
     * @see ClassLoader#getDefinedPackages()
     *
     * @since  1.2
     * @revised 9
     */
    protected Package[] getPackages() {
        Stream<Package> pkgs = packages();
        ClassLoader ld = parent;
        while (ld != null) {
            pkgs = Stream.concat(ld.packages(), pkgs);
            ld = ld.parent;
        }
        return Stream.concat(BootLoader.packages(), pkgs)
                     .toArray(Package[]::new);
    }



    // package-private

    /**
     * Returns a stream of Packages defined in this class loader
     */
    Stream<Package> packages() {
        return packages.values().stream()
                       .map(p -> definePackage(p.packageName(), p.module()));
    }

    // -- Native library access --

    /**
     * Returns the absolute path name of a native library.  The VM invokes this
     * method to locate the native libraries that belong to classes loaded with
     * this class loader. If this method returns {@code null}, the VM
     * searches the library along the path specified as the
     * "{@code java.library.path}" property.
     *
     * @param  libname
     *         The library name
     *
     * @return  The absolute path of the native library
     *
     * @see  System#loadLibrary(String)
     * @see  System#mapLibraryName(String)
     *
     * @since  1.2
     */
    protected String findLibrary(String libname) {
        return null;
    }

    private final NativeLibraries libraries = NativeLibraries.jniNativeLibraries(this);

    // Invoked in the java.lang.Runtime class to implement load and loadLibrary.
    static NativeLibrary loadLibrary(Class<?> fromClass, File file) {
        ClassLoader loader = (fromClass == null) ? null : fromClass.getClassLoader();
        NativeLibraries libs = loader != null ? loader.libraries : BootLoader.getNativeLibraries();
        NativeLibrary nl = libs.loadLibrary(fromClass, file);
        if (nl != null) {
            return nl;
        }
        throw new UnsatisfiedLinkError("Can't load library: " + file);
    }
    static NativeLibrary loadLibrary(Class<?> fromClass, String name) {
        ClassLoader loader = (fromClass == null) ? null : fromClass.getClassLoader();
        if (loader == null) {
            NativeLibrary nl = BootLoader.getNativeLibraries().loadLibrary(fromClass, name);
            if (nl != null) {
                return nl;
            }
            throw new UnsatisfiedLinkError("no " + name +
                    " in system library path: " + StaticProperty.sunBootLibraryPath());
        }

        NativeLibraries libs = loader.libraries;
        // First load from the file returned from ClassLoader::findLibrary, if found.
        String libfilename = loader.findLibrary(name);
        if (libfilename != null) {
            File libfile = new File(libfilename);
            if (!libfile.isAbsolute()) {
                throw new UnsatisfiedLinkError(
                        "ClassLoader.findLibrary failed to return an absolute path: " + libfilename);
            }
            NativeLibrary nl = libs.loadLibrary(fromClass, libfile);
            if (nl != null) {
                return nl;
            }
            throw new UnsatisfiedLinkError("Can't load " + libfilename);
        }
        // Then load from system library path and java library path
        NativeLibrary nl = libs.loadLibrary(fromClass, name);
        if (nl != null) {
            return nl;
        }

        // Oops, it failed
        throw new UnsatisfiedLinkError("no " + name +
                " in java.library.path: " + StaticProperty.javaLibraryPath());
    }

    /*
     * Invoked in the VM class linking code.
     */
    static long findNative(ClassLoader loader, String entryName) {
        if (loader == null) {
            return BootLoader.getNativeLibraries().find(entryName);
        } else {
            return loader.libraries.find(entryName);
        }
    }

    // -- Assertion management --

    final Object assertionLock;

    // The default toggle for assertion checking.
    // @GuardedBy("assertionLock")
    private boolean defaultAssertionStatus = false;

    // Maps String packageName to Boolean package default assertion status Note
    // that the default package is placed under a null map key.  If this field
    // is null then we are delegating assertion status queries to the VM, i.e.,
    // none of this ClassLoader's assertion status modification methods have
    // been invoked.
    // @GuardedBy("assertionLock")
    private Map<String, Boolean> packageAssertionStatus = null;

    // Maps String fullyQualifiedClassName to Boolean assertionStatus If this
    // field is null then we are delegating assertion status queries to the VM,
    // i.e., none of this ClassLoader's assertion status modification methods
    // have been invoked.
    // @GuardedBy("assertionLock")
    Map<String, Boolean> classAssertionStatus = null;

    /**
     * Sets the default assertion status for this class loader.  This setting
     * determines whether classes loaded by this class loader and initialized
     * in the future will have assertions enabled or disabled by default.
     * This setting may be overridden on a per-package or per-class basis by
     * invoking {@link #setPackageAssertionStatus(String, boolean)} or {@link
     * #setClassAssertionStatus(String, boolean)}.
     *
     * @param  enabled
     *         {@code true} if classes loaded by this class loader will
     *         henceforth have assertions enabled by default, {@code false}
     *         if they will have assertions disabled by default.
     *
     * @since  1.4
     */
    public void setDefaultAssertionStatus(boolean enabled) {
        synchronized (assertionLock) {
            if (classAssertionStatus == null)
                initializeJavaAssertionMaps();

            defaultAssertionStatus = enabled;
        }
    }

    /**
     * Sets the package default assertion status for the named package.  The
     * package default assertion status determines the assertion status for
     * classes initialized in the future that belong to the named package or
     * any of its "subpackages".
     *
     * <p> A subpackage of a package named p is any package whose name begins
     * with "{@code p.}".  For example, {@code javax.swing.text} is a
     * subpackage of {@code javax.swing}, and both {@code java.util} and
     * {@code java.lang.reflect} are subpackages of {@code java}.
     *
     * <p> In the event that multiple package defaults apply to a given class,
     * the package default pertaining to the most specific package takes
     * precedence over the others.  For example, if {@code javax.lang} and
     * {@code javax.lang.reflect} both have package defaults associated with
     * them, the latter package default applies to classes in
     * {@code javax.lang.reflect}.
     *
     * <p> Package defaults take precedence over the class loader's default
     * assertion status, and may be overridden on a per-class basis by invoking
     * {@link #setClassAssertionStatus(String, boolean)}.  </p>
     *
     * @param  packageName
     *         The name of the package whose package default assertion status
     *         is to be set. A {@code null} value indicates the unnamed
     *         package that is "current"
     *         (see section {@jls 7.4.2} of
     *         <cite>The Java Language Specification</cite>.)
     *
     * @param  enabled
     *         {@code true} if classes loaded by this classloader and
     *         belonging to the named package or any of its subpackages will
     *         have assertions enabled by default, {@code false} if they will
     *         have assertions disabled by default.
     *
     * @since  1.4
     */
    public void setPackageAssertionStatus(String packageName,
                                          boolean enabled) {
        synchronized (assertionLock) {
            if (packageAssertionStatus == null)
                initializeJavaAssertionMaps();

            packageAssertionStatus.put(packageName, enabled);
        }
    }

    /**
     * Sets the desired assertion status for the named top-level class in this
     * class loader and any nested classes contained therein.  This setting
     * takes precedence over the class loader's default assertion status, and
     * over any applicable per-package default.  This method has no effect if
     * the named class has already been initialized.  (Once a class is
     * initialized, its assertion status cannot change.)
     *
     * <p> If the named class is not a top-level class, this invocation will
     * have no effect on the actual assertion status of any class. </p>
     *
     * @param  className
     *         The fully qualified class name of the top-level class whose
     *         assertion status is to be set.
     *
     * @param  enabled
     *         {@code true} if the named class is to have assertions
     *         enabled when (and if) it is initialized, {@code false} if the
     *         class is to have assertions disabled.
     *
     * @since  1.4
     */
    public void setClassAssertionStatus(String className, boolean enabled) {
        synchronized (assertionLock) {
            if (classAssertionStatus == null)
                initializeJavaAssertionMaps();

            classAssertionStatus.put(className, enabled);
        }
    }

    /**
     * Sets the default assertion status for this class loader to
     * {@code false} and discards any package defaults or class assertion
     * status settings associated with the class loader.  This method is
     * provided so that class loaders can be made to ignore any command line or
     * persistent assertion status settings and "start with a clean slate."
     *
     * @since  1.4
     */
    public void clearAssertionStatus() {
        /*
         * Whether or not "Java assertion maps" are initialized, set
         * them to empty maps, effectively ignoring any present settings.
         */
        synchronized (assertionLock) {
            classAssertionStatus = new HashMap<>();
            packageAssertionStatus = new HashMap<>();
            defaultAssertionStatus = false;
        }
    }

    /**
     * Returns the assertion status that would be assigned to the specified
     * class if it were to be initialized at the time this method is invoked.
     * If the named class has had its assertion status set, the most recent
     * setting will be returned; otherwise, if any package default assertion
     * status pertains to this class, the most recent setting for the most
     * specific pertinent package default assertion status is returned;
     * otherwise, this class loader's default assertion status is returned.
     * </p>
     *
     * @param  className
     *         The fully qualified class name of the class whose desired
     *         assertion status is being queried.
     *
     * @return  The desired assertion status of the specified class.
     *
     * @see  #setClassAssertionStatus(String, boolean)
     * @see  #setPackageAssertionStatus(String, boolean)
     * @see  #setDefaultAssertionStatus(boolean)
     *
     * @since  1.4
     */
    boolean desiredAssertionStatus(String className) {
        synchronized (assertionLock) {
            // assert classAssertionStatus   != null;
            // assert packageAssertionStatus != null;

            // Check for a class entry
            Boolean result = classAssertionStatus.get(className);
            if (result != null)
                return result.booleanValue();

            // Check for most specific package entry
            int dotIndex = className.lastIndexOf('.');
            if (dotIndex < 0) { // default package
                result = packageAssertionStatus.get(null);
                if (result != null)
                    return result.booleanValue();
            }
            while(dotIndex > 0) {
                className = className.substring(0, dotIndex);
                result = packageAssertionStatus.get(className);
                if (result != null)
                    return result.booleanValue();
                dotIndex = className.lastIndexOf('.', dotIndex-1);
            }

            // Return the classloader default
            return defaultAssertionStatus;
        }
    }

    // Set up the assertions with information provided by the VM.
    // Note: Should only be called inside a synchronized block
    private void initializeJavaAssertionMaps() {
        // assert Thread.holdsLock(assertionLock);

        classAssertionStatus = new HashMap<>();
        packageAssertionStatus = new HashMap<>();
        AssertionStatusDirectives directives = retrieveDirectives();

        for(int i = 0; i < directives.classes.length; i++)
            classAssertionStatus.put(directives.classes[i],
                                     directives.classEnabled[i]);

        for(int i = 0; i < directives.packages.length; i++)
            packageAssertionStatus.put(directives.packages[i],
                                       directives.packageEnabled[i]);

        defaultAssertionStatus = directives.deflt;
    }

    // Retrieves the assertion directives from the VM.
    private static native AssertionStatusDirectives retrieveDirectives();


    // -- Misc --

    /**
     * Returns the ConcurrentHashMap used as a storage for ClassLoaderValue(s)
     * associated with this ClassLoader, creating it if it doesn't already exist.
     */
    ConcurrentHashMap<?, ?> createOrGetClassLoaderValueMap() {
        ConcurrentHashMap<?, ?> map = classLoaderValueMap;
        if (map == null) {
            map = new ConcurrentHashMap<>();
            boolean set = trySetObjectField("classLoaderValueMap", map);
            if (!set) {
                // beaten by someone else
                map = classLoaderValueMap;
            }
        }
        return map;
    }

    // the storage for ClassLoaderValue(s) associated with this ClassLoader
    private volatile ConcurrentHashMap<?, ?> classLoaderValueMap;

    /**
     * Attempts to atomically set a volatile field in this object. Returns
     * {@code true} if not beaten by another thread. Avoids the use of
     * AtomicReferenceFieldUpdater in this class.
     */
    private boolean trySetObjectField(String name, Object obj) {
        Unsafe unsafe = Unsafe.getUnsafe();
        Class<?> k = ClassLoader.class;
        long offset;
        offset = unsafe.objectFieldOffset(k, name);
        return unsafe.compareAndSetReference(this, offset, null, obj);
    }

    /**
     * Called by the VM, during -Xshare:dump
     */
    private void resetArchivedStates() {
        parallelLockMap.clear();
        packages.clear();
        package2certs.clear();
        classes.clear();
        classLoaderValueMap = null;
    }
}

/*
 * A utility class that will enumerate over an array of enumerations.
 */
final class CompoundEnumeration<E> implements Enumeration<E> {
    private final Enumeration<E>[] enums;
    private int index;

    public CompoundEnumeration(Enumeration<E>[] enums) {
        this.enums = enums;
    }

    private boolean next() {
        while (index < enums.length) {
            if (enums[index] != null && enums[index].hasMoreElements()) {
                return true;
            }
            index++;
        }
        return false;
    }

    public boolean hasMoreElements() {
        return next();
    }

    public E nextElement() {
        if (!next()) {
            throw new NoSuchElementException();
        }
        return enums[index].nextElement();
    }
}
