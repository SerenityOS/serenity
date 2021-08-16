/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.module.Configuration;
import java.lang.module.ModuleReference;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ResolvedModule;
import java.lang.reflect.AnnotatedElement;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.BootLoader;
import jdk.internal.loader.ClassLoaders;
import jdk.internal.misc.CDS;
import jdk.internal.module.ModuleLoaderMap;
import jdk.internal.module.ServicesCatalog;
import jdk.internal.module.Resources;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.security.util.SecurityConstants;

/**
 * Represents a run-time module, either {@link #isNamed() named} or unnamed.
 *
 * <p> Named modules have a {@link #getName() name} and are constructed by the
 * Java Virtual Machine when a graph of modules is defined to the Java virtual
 * machine to create a {@linkplain ModuleLayer module layer}. </p>
 *
 * <p> An unnamed module does not have a name. There is an unnamed module for
 * each {@link ClassLoader ClassLoader}, obtained by invoking its {@link
 * ClassLoader#getUnnamedModule() getUnnamedModule} method. All types that are
 * not in a named module are members of their defining class loader's unnamed
 * module. </p>
 *
 * <p> The package names that are parameters or returned by methods defined in
 * this class are the fully-qualified names of the packages as defined in
 * section {@jls 6.5.3} of <cite>The Java Language Specification</cite>, for
 * example, {@code "java.lang"}. </p>
 *
 * <p> Unless otherwise specified, passing a {@code null} argument to a method
 * in this class causes a {@link NullPointerException NullPointerException} to
 * be thrown. </p>
 *
 * @since 9
 * @see Class#getModule()
 * @jls 7.7 Module Declarations
 */

public final class Module implements AnnotatedElement {

    // the layer that contains this module, can be null
    private final ModuleLayer layer;

    // module name and loader, these fields are read by VM
    private final String name;
    private final ClassLoader loader;

    // the module descriptor
    private final ModuleDescriptor descriptor;

    // true, if this module allows restricted native access
    private volatile boolean enableNativeAccess;

    /**
     * Creates a new named Module. The resulting Module will be defined to the
     * VM but will not read any other modules, will not have any exports setup
     * and will not be registered in the service catalog.
     */
    Module(ModuleLayer layer,
           ClassLoader loader,
           ModuleDescriptor descriptor,
           URI uri)
    {
        this.layer = layer;
        this.name = descriptor.name();
        this.loader = loader;
        this.descriptor = descriptor;

        // define module to VM

        boolean isOpen = descriptor.isOpen() || descriptor.isAutomatic();
        Version version = descriptor.version().orElse(null);
        String vs = Objects.toString(version, null);
        String loc = Objects.toString(uri, null);
        Object[] packages = descriptor.packages().toArray();
        defineModule0(this, isOpen, vs, loc, packages);
        if (loader == null || loader == ClassLoaders.platformClassLoader()) {
            // boot/builtin modules are always native
            implAddEnableNativeAccess();
        }
    }


    /**
     * Create the unnamed Module for the given ClassLoader.
     *
     * @see ClassLoader#getUnnamedModule
     */
    Module(ClassLoader loader) {
        this.layer = null;
        this.name = null;
        this.loader = loader;
        this.descriptor = null;
    }


    /**
     * Creates a named module but without defining the module to the VM.
     *
     * @apiNote This constructor is for VM white-box testing.
     */
    Module(ClassLoader loader, ModuleDescriptor descriptor) {
        this.layer = null;
        this.name = descriptor.name();
        this.loader = loader;
        this.descriptor = descriptor;
    }


    /**
     * Returns {@code true} if this module is a named module.
     *
     * @return {@code true} if this is a named module
     *
     * @see ClassLoader#getUnnamedModule()
     * @jls 7.7.5 Unnamed Modules
     */
    public boolean isNamed() {
        return name != null;
    }

    /**
     * Returns the module name or {@code null} if this module is an unnamed
     * module.
     *
     * @return The module name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@code ClassLoader} for this module.
     *
     * <p> If there is a security manager then its {@code checkPermission}
     * method if first called with a {@code RuntimePermission("getClassLoader")}
     * permission to check that the caller is allowed to get access to the
     * class loader. </p>
     *
     * @return The class loader for this module
     *
     * @throws SecurityException
     *         If denied by the security manager
     */
    public ClassLoader getClassLoader() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
        }
        return loader;
    }

    /**
     * Returns the module descriptor for this module or {@code null} if this
     * module is an unnamed module.
     *
     * @return The module descriptor for this module
     */
    public ModuleDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the module layer that contains this module or {@code null} if
     * this module is not in a module layer.
     *
     * A module layer contains named modules and therefore this method always
     * returns {@code null} when invoked on an unnamed module.
     *
     * <p> <a href="reflect/Proxy.html#dynamicmodule">Dynamic modules</a> are
     * named modules that are generated at runtime. A dynamic module may or may
     * not be in a module layer. </p>
     *
     * @return The module layer that contains this module
     *
     * @see java.lang.reflect.Proxy
     */
    public ModuleLayer getLayer() {
        if (isNamed()) {
            ModuleLayer layer = this.layer;
            if (layer != null)
                return layer;

            // special-case java.base as it is created before the boot layer
            if (loader == null && name.equals("java.base")) {
                return ModuleLayer.boot();
            }
        }
        return null;
    }

    /**
     * Update this module to allow access to restricted methods.
     */
    Module implAddEnableNativeAccess() {
        enableNativeAccess = true;
        return this;
    }

    /**
     * Update all unnamed modules to allow access to restricted methods.
     */
    static void implAddEnableNativeAccessAllUnnamed() {
        ALL_UNNAMED_MODULE.enableNativeAccess = true;
    }

    /**
     * Returns true if module m can access restricted methods.
     */
    boolean implIsEnableNativeAccess() {
        return isNamed() ?
                enableNativeAccess :
                ALL_UNNAMED_MODULE.enableNativeAccess;
    }

    // --

    // special Module to mean "all unnamed modules"
    private static final Module ALL_UNNAMED_MODULE;
    private static final Set<Module> ALL_UNNAMED_MODULE_SET;

    // special Module to mean "everyone"
    private static final Module EVERYONE_MODULE;
    private static final Set<Module> EVERYONE_SET;

    private static class ArchivedData {
        private static ArchivedData archivedData;
        private final Module allUnnamedModule;
        private final Set<Module> allUnnamedModules;
        private final Module everyoneModule;
        private final Set<Module> everyoneSet;

        private ArchivedData() {
            this.allUnnamedModule = ALL_UNNAMED_MODULE;
            this.allUnnamedModules = ALL_UNNAMED_MODULE_SET;
            this.everyoneModule = EVERYONE_MODULE;
            this.everyoneSet = EVERYONE_SET;
        }

        static void archive() {
            archivedData = new ArchivedData();
        }

        static ArchivedData get() {
            return archivedData;
        }

        static {
            CDS.initializeFromArchive(ArchivedData.class);
        }
    }

    static {
        ArchivedData archivedData = ArchivedData.get();
        if (archivedData != null) {
            ALL_UNNAMED_MODULE = archivedData.allUnnamedModule;
            ALL_UNNAMED_MODULE_SET = archivedData.allUnnamedModules;
            EVERYONE_MODULE = archivedData.everyoneModule;
            EVERYONE_SET = archivedData.everyoneSet;
        } else {
            ALL_UNNAMED_MODULE = new Module(null);
            ALL_UNNAMED_MODULE_SET = Set.of(ALL_UNNAMED_MODULE);
            EVERYONE_MODULE = new Module(null);
            EVERYONE_SET = Set.of(EVERYONE_MODULE);
            ArchivedData.archive();
        }
    }

    /**
     * The holder of data structures to support readability, exports, and
     * service use added at runtime with the reflective APIs.
     */
    private static class ReflectionData {
        /**
         * A module (1st key) reads another module (2nd key)
         */
        static final WeakPairMap<Module, Module, Boolean> reads =
            new WeakPairMap<>();

        /**
         * A module (1st key) exports or opens a package to another module
         * (2nd key). The map value is a map of package name to a boolean
         * that indicates if the package is opened.
         */
        static final WeakPairMap<Module, Module, Map<String, Boolean>> exports =
            new WeakPairMap<>();

        /**
         * A module (1st key) uses a service (2nd key)
         */
        static final WeakPairMap<Module, Class<?>, Boolean> uses =
            new WeakPairMap<>();
    }


    // -- readability --

    // the modules that this module reads
    private volatile Set<Module> reads;

    /**
     * Indicates if this module reads the given module. This method returns
     * {@code true} if invoked to test if this module reads itself. It also
     * returns {@code true} if invoked on an unnamed module (as unnamed
     * modules read all modules).
     *
     * @param  other
     *         The other module
     *
     * @return {@code true} if this module reads {@code other}
     *
     * @see #addReads(Module)
     */
    public boolean canRead(Module other) {
        Objects.requireNonNull(other);

        // an unnamed module reads all modules
        if (!this.isNamed())
            return true;

        // all modules read themselves
        if (other == this)
            return true;

        // check if this module reads other
        if (other.isNamed()) {
            Set<Module> reads = this.reads; // volatile read
            if (reads != null && reads.contains(other))
                return true;
        }

        // check if this module reads the other module reflectively
        if (ReflectionData.reads.containsKeyPair(this, other))
            return true;

        // if other is an unnamed module then check if this module reads
        // all unnamed modules
        if (!other.isNamed()
            && ReflectionData.reads.containsKeyPair(this, ALL_UNNAMED_MODULE))
            return true;

        return false;
    }

    /**
     * If the caller's module is this module then update this module to read
     * the given module.
     *
     * This method is a no-op if {@code other} is this module (all modules read
     * themselves), this module is an unnamed module (as unnamed modules read
     * all modules), or this module already reads {@code other}.
     *
     * @implNote <em>Read edges</em> added by this method are <em>weak</em> and
     * do not prevent {@code other} from being GC'ed when this module is
     * strongly reachable.
     *
     * @param  other
     *         The other module
     *
     * @return this module
     *
     * @throws IllegalCallerException
     *         If this is a named module and the caller's module is not this
     *         module
     *
     * @see #canRead
     */
    @CallerSensitive
    public Module addReads(Module other) {
        Objects.requireNonNull(other);
        if (this.isNamed()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if (caller != this) {
                throw new IllegalCallerException(caller + " != " + this);
            }
            implAddReads(other, true);
        }
        return this;
    }

    /**
     * Updates this module to read another module.
     *
     * @apiNote Used by the --add-reads command line option.
     */
    void implAddReads(Module other) {
        implAddReads(other, true);
    }

    /**
     * Updates this module to read all unnamed modules.
     *
     * @apiNote Used by the --add-reads command line option.
     */
    void implAddReadsAllUnnamed() {
        implAddReads(Module.ALL_UNNAMED_MODULE, true);
    }

    /**
     * Updates this module to read another module without notifying the VM.
     *
     * @apiNote This method is for VM white-box testing.
     */
    void implAddReadsNoSync(Module other) {
        implAddReads(other, false);
    }

    /**
     * Makes the given {@code Module} readable to this module.
     *
     * If {@code syncVM} is {@code true} then the VM is notified.
     */
    private void implAddReads(Module other, boolean syncVM) {
        Objects.requireNonNull(other);
        if (!canRead(other)) {
            // update VM first, just in case it fails
            if (syncVM) {
                if (other == ALL_UNNAMED_MODULE) {
                    addReads0(this, null);
                } else {
                    addReads0(this, other);
                }
            }

            // add reflective read
            ReflectionData.reads.putIfAbsent(this, other, Boolean.TRUE);
        }
    }


    // -- exported and open packages --

    // the packages are open to other modules, can be null
    // if the value contains EVERYONE_MODULE then the package is open to all
    private volatile Map<String, Set<Module>> openPackages;

    // the packages that are exported, can be null
    // if the value contains EVERYONE_MODULE then the package is exported to all
    private volatile Map<String, Set<Module>> exportedPackages;

    /**
     * Returns {@code true} if this module exports the given package to at
     * least the given module.
     *
     * <p> This method returns {@code true} if invoked to test if a package in
     * this module is exported to itself. It always returns {@code true} when
     * invoked on an unnamed module. A package that is {@link #isOpen open} to
     * the given module is considered exported to that module at run-time and
     * so this method returns {@code true} if the package is open to the given
     * module. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param  pn
     *         The package name
     * @param  other
     *         The other module
     *
     * @return {@code true} if this module exports the package to at least the
     *         given module
     *
     * @see ModuleDescriptor#exports()
     * @see #addExports(String,Module)
     */
    public boolean isExported(String pn, Module other) {
        Objects.requireNonNull(pn);
        Objects.requireNonNull(other);
        return implIsExportedOrOpen(pn, other, /*open*/false);
    }

    /**
     * Returns {@code true} if this module has <em>opened</em> a package to at
     * least the given module.
     *
     * <p> This method returns {@code true} if invoked to test if a package in
     * this module is open to itself. It returns {@code true} when invoked on an
     * {@link ModuleDescriptor#isOpen open} module with a package in the module.
     * It always returns {@code true} when invoked on an unnamed module. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param  pn
     *         The package name
     * @param  other
     *         The other module
     *
     * @return {@code true} if this module has <em>opened</em> the package
     *         to at least the given module
     *
     * @see ModuleDescriptor#opens()
     * @see #addOpens(String,Module)
     * @see java.lang.reflect.AccessibleObject#setAccessible(boolean)
     * @see java.lang.invoke.MethodHandles#privateLookupIn
     */
    public boolean isOpen(String pn, Module other) {
        Objects.requireNonNull(pn);
        Objects.requireNonNull(other);
        return implIsExportedOrOpen(pn, other, /*open*/true);
    }

    /**
     * Returns {@code true} if this module exports the given package
     * unconditionally.
     *
     * <p> This method always returns {@code true} when invoked on an unnamed
     * module. A package that is {@link #isOpen(String) opened} unconditionally
     * is considered exported unconditionally at run-time and so this method
     * returns {@code true} if the package is opened unconditionally. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param  pn
     *         The package name
     *
     * @return {@code true} if this module exports the package unconditionally
     *
     * @see ModuleDescriptor#exports()
     */
    public boolean isExported(String pn) {
        Objects.requireNonNull(pn);
        return implIsExportedOrOpen(pn, EVERYONE_MODULE, /*open*/false);
    }

    /**
     * Returns {@code true} if this module has <em>opened</em> a package
     * unconditionally.
     *
     * <p> This method always returns {@code true} when invoked on an unnamed
     * module. Additionally, it always returns {@code true} when invoked on an
     * {@link ModuleDescriptor#isOpen open} module with a package in the
     * module. </p>
     *
     * <p> This method does not check if the given module reads this module. </p>
     *
     * @param  pn
     *         The package name
     *
     * @return {@code true} if this module has <em>opened</em> the package
     *         unconditionally
     *
     * @see ModuleDescriptor#opens()
     */
    public boolean isOpen(String pn) {
        Objects.requireNonNull(pn);
        return implIsExportedOrOpen(pn, EVERYONE_MODULE, /*open*/true);
    }


    /**
     * Returns {@code true} if this module exports or opens the given package
     * to the given module. If the other module is {@code EVERYONE_MODULE} then
     * this method tests if the package is exported or opened unconditionally.
     */
    private boolean implIsExportedOrOpen(String pn, Module other, boolean open) {
        // all packages in unnamed modules are open
        if (!isNamed())
            return true;

        // all packages are exported/open to self
        if (other == this && descriptor.packages().contains(pn))
            return true;

        // all packages in open and automatic modules are open
        if (descriptor.isOpen() || descriptor.isAutomatic())
            return descriptor.packages().contains(pn);

        // exported/opened via module declaration/descriptor
        if (isStaticallyExportedOrOpen(pn, other, open))
            return true;

        // exported via addExports/addOpens
        if (isReflectivelyExportedOrOpen(pn, other, open))
            return true;

        // not exported or open to other
        return false;
    }

    /**
     * Returns {@code true} if this module exports or opens a package to
     * the given module via its module declaration or CLI options.
     */
    private boolean isStaticallyExportedOrOpen(String pn, Module other, boolean open) {
        // test if package is open to everyone or <other>
        Map<String, Set<Module>> openPackages = this.openPackages;
        if (openPackages != null && allows(openPackages.get(pn), other)) {
            return true;
        }

        if (!open) {
            // test package is exported to everyone or <other>
            Map<String, Set<Module>> exportedPackages = this.exportedPackages;
            if (exportedPackages != null && allows(exportedPackages.get(pn), other)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns {@code true} if targets is non-null and contains EVERYONE_MODULE
     * or the given module. Also returns true if the given module is an unnamed
     * module and targets contains ALL_UNNAMED_MODULE.
     */
    private boolean allows(Set<Module> targets, Module module) {
       if (targets != null) {
           if (targets.contains(EVERYONE_MODULE))
               return true;
           if (module != EVERYONE_MODULE) {
               if (targets.contains(module))
                   return true;
               if (!module.isNamed() && targets.contains(ALL_UNNAMED_MODULE))
                   return true;
           }
        }
        return false;
    }

    /**
     * Returns {@code true} if this module reflectively exports or opens the
     * given package to the given module.
     */
    private boolean isReflectivelyExportedOrOpen(String pn, Module other, boolean open) {
        // exported or open to all modules
        Map<String, Boolean> exports = ReflectionData.exports.get(this, EVERYONE_MODULE);
        if (exports != null) {
            Boolean b = exports.get(pn);
            if (b != null) {
                boolean isOpen = b.booleanValue();
                if (!open || isOpen) return true;
            }
        }

        if (other != EVERYONE_MODULE) {

            // exported or open to other
            exports = ReflectionData.exports.get(this, other);
            if (exports != null) {
                Boolean b = exports.get(pn);
                if (b != null) {
                    boolean isOpen = b.booleanValue();
                    if (!open || isOpen) return true;
                }
            }

            // other is an unnamed module && exported or open to all unnamed
            if (!other.isNamed()) {
                exports = ReflectionData.exports.get(this, ALL_UNNAMED_MODULE);
                if (exports != null) {
                    Boolean b = exports.get(pn);
                    if (b != null) {
                        boolean isOpen = b.booleanValue();
                        if (!open || isOpen) return true;
                    }
                }
            }

        }

        return false;
    }

    /**
     * Returns {@code true} if this module reflectively exports the
     * given package to the given module.
     */
    boolean isReflectivelyExported(String pn, Module other) {
        return isReflectivelyExportedOrOpen(pn, other, false);
    }

    /**
     * Returns {@code true} if this module reflectively opens the
     * given package to the given module.
     */
    boolean isReflectivelyOpened(String pn, Module other) {
        return isReflectivelyExportedOrOpen(pn, other, true);
    }


    /**
     * If the caller's module is this module then update this module to export
     * the given package to the given module.
     *
     * <p> This method has no effect if the package is already exported (or
     * <em>open</em>) to the given module. </p>
     *
     * @apiNote As specified in section {@jvms 5.4.3} of the <cite>The Java
     * Virtual Machine Specification </cite>, if an attempt to resolve a
     * symbolic reference fails because of a linkage error, then subsequent
     * attempts to resolve the reference always fail with the same error that
     * was thrown as a result of the initial resolution attempt.
     *
     * @param  pn
     *         The package name
     * @param  other
     *         The module
     *
     * @return this module
     *
     * @throws IllegalArgumentException
     *         If {@code pn} is {@code null}, or this is a named module and the
     *         package {@code pn} is not a package in this module
     * @throws IllegalCallerException
     *         If this is a named module and the caller's module is not this
     *         module
     *
     * @jvms 5.4.3 Resolution
     * @see #isExported(String,Module)
     */
    @CallerSensitive
    public Module addExports(String pn, Module other) {
        if (pn == null)
            throw new IllegalArgumentException("package is null");
        Objects.requireNonNull(other);

        if (isNamed()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if (caller != this) {
                throw new IllegalCallerException(caller + " != " + this);
            }
            implAddExportsOrOpens(pn, other, /*open*/false, /*syncVM*/true);
        }

        return this;
    }

    /**
     * If this module has <em>opened</em> a package to at least the caller
     * module then update this module to open the package to the given module.
     * Opening a package with this method allows all types in the package,
     * and all their members, not just public types and their public members,
     * to be reflected on by the given module when using APIs that support
     * private access or a way to bypass or suppress default Java language
     * access control checks.
     *
     * <p> This method has no effect if the package is already <em>open</em>
     * to the given module. </p>
     *
     * @apiNote This method can be used for cases where a <em>consumer
     * module</em> uses a qualified opens to open a package to an <em>API
     * module</em> but where the reflective access to the members of classes in
     * the consumer module is delegated to code in another module. Code in the
     * API module can use this method to open the package in the consumer module
     * to the other module.
     *
     * @param  pn
     *         The package name
     * @param  other
     *         The module
     *
     * @return this module
     *
     * @throws IllegalArgumentException
     *         If {@code pn} is {@code null}, or this is a named module and the
     *         package {@code pn} is not a package in this module
     * @throws IllegalCallerException
     *         If this is a named module and this module has not opened the
     *         package to at least the caller's module
     *
     * @see #isOpen(String,Module)
     * @see java.lang.reflect.AccessibleObject#setAccessible(boolean)
     * @see java.lang.invoke.MethodHandles#privateLookupIn
     */
    @CallerSensitive
    public Module addOpens(String pn, Module other) {
        if (pn == null)
            throw new IllegalArgumentException("package is null");
        Objects.requireNonNull(other);

        if (isNamed()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if (caller != this && (caller == null || !isOpen(pn, caller)))
                throw new IllegalCallerException(pn + " is not open to " + caller);
            implAddExportsOrOpens(pn, other, /*open*/true, /*syncVM*/true);
        }

        return this;
    }


    /**
     * Updates this module to export a package unconditionally.
     *
     * @apiNote This method is for JDK tests only.
     */
    void implAddExports(String pn) {
        implAddExportsOrOpens(pn, Module.EVERYONE_MODULE, false, true);
    }

    /**
     * Updates this module to export a package to another module.
     *
     * @apiNote Used by Instrumentation::redefineModule and --add-exports
     */
    void implAddExports(String pn, Module other) {
        implAddExportsOrOpens(pn, other, false, true);
    }

    /**
     * Updates this module to export a package to all unnamed modules.
     *
     * @apiNote Used by the --add-exports command line option.
     */
    void implAddExportsToAllUnnamed(String pn) {
        implAddExportsOrOpens(pn, Module.ALL_UNNAMED_MODULE, false, true);
    }

    /**
     * Updates this export to export a package unconditionally without
     * notifying the VM.
     *
     * @apiNote This method is for VM white-box testing.
     */
    void implAddExportsNoSync(String pn) {
        implAddExportsOrOpens(pn.replace('/', '.'), Module.EVERYONE_MODULE, false, false);
    }

    /**
     * Updates a module to export a package to another module without
     * notifying the VM.
     *
     * @apiNote This method is for VM white-box testing.
     */
    void implAddExportsNoSync(String pn, Module other) {
        implAddExportsOrOpens(pn.replace('/', '.'), other, false, false);
    }

    /**
     * Updates this module to open a package unconditionally.
     *
     * @apiNote This method is for JDK tests only.
     */
    void implAddOpens(String pn) {
        implAddExportsOrOpens(pn, Module.EVERYONE_MODULE, true, true);
    }

    /**
     * Updates this module to open a package to another module.
     *
     * @apiNote Used by Instrumentation::redefineModule and --add-opens
     */
    void implAddOpens(String pn, Module other) {
        implAddExportsOrOpens(pn, other, true, true);
    }

    /**
     * Updates this module to open a package to all unnamed modules.
     *
     * @apiNote Used by the --add-opens command line option.
     */
    void implAddOpensToAllUnnamed(String pn) {
        implAddExportsOrOpens(pn, Module.ALL_UNNAMED_MODULE, true, true);
    }

    /**
     * Updates a module to export or open a module to another module.
     *
     * If {@code syncVM} is {@code true} then the VM is notified.
     */
    private void implAddExportsOrOpens(String pn,
                                       Module other,
                                       boolean open,
                                       boolean syncVM) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(pn);

        // all packages are open in unnamed, open, and automatic modules
        if (!isNamed() || descriptor.isOpen() || descriptor.isAutomatic())
            return;

        // check if the package is already exported/open to other
        if (implIsExportedOrOpen(pn, other, open))
            return;

        // can only export a package in the module
        if (!descriptor.packages().contains(pn)) {
            throw new IllegalArgumentException("package " + pn
                                               + " not in contents");
        }

        // update VM first, just in case it fails
        if (syncVM) {
            if (other == EVERYONE_MODULE) {
                addExportsToAll0(this, pn);
            } else if (other == ALL_UNNAMED_MODULE) {
                addExportsToAllUnnamed0(this, pn);
            } else {
                addExports0(this, pn, other);
            }
        }

        // add package name to exports if absent
        Map<String, Boolean> map = ReflectionData.exports
            .computeIfAbsent(this, other,
                             (m1, m2) -> new ConcurrentHashMap<>());
        if (open) {
            map.put(pn, Boolean.TRUE);  // may need to promote from FALSE to TRUE
        } else {
            map.putIfAbsent(pn, Boolean.FALSE);
        }
    }

    /**
     * Updates a module to open all packages in the given sets to all unnamed
     * modules.
     *
     * @apiNote Used during startup to open packages for illegal access.
     */
    void implAddOpensToAllUnnamed(Set<String> concealedPkgs, Set<String> exportedPkgs) {
        if (jdk.internal.misc.VM.isModuleSystemInited()) {
            throw new IllegalStateException("Module system already initialized");
        }

        // replace this module's openPackages map with a new map that opens
        // the packages to all unnamed modules.
        Map<String, Set<Module>> openPackages = this.openPackages;
        if (openPackages == null) {
            openPackages = new HashMap<>((4 * (concealedPkgs.size() + exportedPkgs.size()) / 3) + 1);
        } else {
            openPackages = new HashMap<>(openPackages);
        }
        implAddOpensToAllUnnamed(concealedPkgs, openPackages);
        implAddOpensToAllUnnamed(exportedPkgs, openPackages);
        this.openPackages = openPackages;
    }

    private void implAddOpensToAllUnnamed(Set<String> pkgs, Map<String, Set<Module>> openPackages) {
        for (String pn : pkgs) {
            Set<Module> prev = openPackages.putIfAbsent(pn, ALL_UNNAMED_MODULE_SET);
            if (prev != null) {
                prev.add(ALL_UNNAMED_MODULE);
            }

            // update VM to export the package
            addExportsToAllUnnamed0(this, pn);
        }
    }

    // -- services --

    /**
     * If the caller's module is this module then update this module to add a
     * service dependence on the given service type. This method is intended
     * for use by frameworks that invoke {@link java.util.ServiceLoader
     * ServiceLoader} on behalf of other modules or where the framework is
     * passed a reference to the service type by other code. This method is
     * a no-op when invoked on an unnamed module or an automatic module.
     *
     * <p> This method does not cause {@link Configuration#resolveAndBind
     * resolveAndBind} to be re-run. </p>
     *
     * @param  service
     *         The service type
     *
     * @return this module
     *
     * @throws IllegalCallerException
     *         If this is a named module and the caller's module is not this
     *         module
     *
     * @see #canUse(Class)
     * @see ModuleDescriptor#uses()
     */
    @CallerSensitive
    public Module addUses(Class<?> service) {
        Objects.requireNonNull(service);

        if (isNamed() && !descriptor.isAutomatic()) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if (caller != this) {
                throw new IllegalCallerException(caller + " != " + this);
            }
            implAddUses(service);
        }

        return this;
    }

    /**
     * Update this module to add a service dependence on the given service
     * type.
     */
    void implAddUses(Class<?> service) {
        if (!canUse(service)) {
            ReflectionData.uses.putIfAbsent(this, service, Boolean.TRUE);
        }
    }


    /**
     * Indicates if this module has a service dependence on the given service
     * type. This method always returns {@code true} when invoked on an unnamed
     * module or an automatic module.
     *
     * @param  service
     *         The service type
     *
     * @return {@code true} if this module uses service type {@code st}
     *
     * @see #addUses(Class)
     */
    public boolean canUse(Class<?> service) {
        Objects.requireNonNull(service);

        if (!isNamed())
            return true;

        if (descriptor.isAutomatic())
            return true;

        // uses was declared
        if (descriptor.uses().contains(service.getName()))
            return true;

        // uses added via addUses
        return ReflectionData.uses.containsKeyPair(this, service);
    }



    // -- packages --

    /**
     * Returns the set of package names for the packages in this module.
     *
     * <p> For named modules, the returned set contains an element for each
     * package in the module. </p>
     *
     * <p> For unnamed modules, the returned set contains an element for
     * each package that {@link ClassLoader#getDefinedPackages() has been defined}
     * in the unnamed module.</p>
     *
     * @return the set of the package names of the packages in this module
     */
    public Set<String> getPackages() {
        if (isNamed()) {
            return descriptor.packages();
        } else {
            // unnamed module
            Stream<Package> packages;
            if (loader == null) {
                packages = BootLoader.packages();
            } else {
                packages = loader.packages();
            }
            return packages.filter(p -> p.module() == this)
                           .map(Package::getName).collect(Collectors.toSet());
        }
    }

    // -- creating Module objects --

    /**
     * Defines all module in a configuration to the runtime.
     *
     * @return a map of module name to runtime {@code Module}
     *
     * @throws IllegalArgumentException
     *         If the function maps a module to the null or platform class loader
     * @throws IllegalStateException
     *         If the module cannot be defined to the VM or its packages overlap
     *         with another module mapped to the same class loader
     */
    static Map<String, Module> defineModules(Configuration cf,
                                             Function<String, ClassLoader> clf,
                                             ModuleLayer layer)
    {
        boolean isBootLayer = (ModuleLayer.boot() == null);

        int numModules = cf.modules().size();
        int cap = (int)(numModules / 0.75f + 1.0f);
        Map<String, Module> nameToModule = new HashMap<>(cap);

        // to avoid repeated lookups and reduce iteration overhead, we create
        // arrays holding correlated information about each module.
        ResolvedModule[] resolvedModules = new ResolvedModule[numModules];
        Module[] modules = new Module[numModules];
        ClassLoader[] classLoaders = new ClassLoader[numModules];

        resolvedModules = cf.modules().toArray(resolvedModules);

        // record that we want to bind the layer to non-boot and non-platform
        // module loaders as a final step
        HashSet<ClassLoader> toBindLoaders = new HashSet<>(4);
        boolean hasPlatformModules = false;

        // map each module to a class loader
        ClassLoader pcl = ClassLoaders.platformClassLoader();
        boolean isModuleLoaderMapper = ModuleLoaderMap.isBuiltinMapper(clf);

        for (int index = 0; index < numModules; index++) {
            String name = resolvedModules[index].name();
            ClassLoader loader = clf.apply(name);

            if (loader == null || loader == pcl) {
                if (!isModuleLoaderMapper) {
                    throw new IllegalArgumentException("loader can't be 'null'"
                            + " or the platform class loader");
                }
                hasPlatformModules = true;
            } else {
                toBindLoaders.add(loader);
            }

            classLoaders[index] = loader;
        }

        // define each module in the configuration to the VM
        for (int index = 0; index < numModules; index++) {
            ModuleReference mref = resolvedModules[index].reference();
            ModuleDescriptor descriptor = mref.descriptor();
            String name = descriptor.name();
            ClassLoader loader = classLoaders[index];
            Module m;
            if (loader == null && name.equals("java.base")) {
                // java.base is already defined to the VM
                m = Object.class.getModule();
            } else {
                URI uri = mref.location().orElse(null);
                m = new Module(layer, loader, descriptor, uri);
            }
            nameToModule.put(name, m);
            modules[index] = m;
        }

        // setup readability and exports/opens
        for (int index = 0; index < numModules; index++) {
            ResolvedModule resolvedModule = resolvedModules[index];
            ModuleReference mref = resolvedModule.reference();
            ModuleDescriptor descriptor = mref.descriptor();
            Module m = modules[index];

            // reads
            Set<Module> reads = new HashSet<>();

            // name -> source Module when in parent layer
            Map<String, Module> nameToSource = Map.of();

            for (ResolvedModule other : resolvedModule.reads()) {
                Module m2 = null;
                if (other.configuration() == cf) {
                    // this configuration
                    m2 = nameToModule.get(other.name());
                    assert m2 != null;
                } else {
                    // parent layer
                    for (ModuleLayer parent: layer.parents()) {
                        m2 = findModule(parent, other);
                        if (m2 != null)
                            break;
                    }
                    assert m2 != null;
                    if (nameToSource.isEmpty())
                        nameToSource = new HashMap<>();
                    nameToSource.put(other.name(), m2);
                }
                reads.add(m2);

                // update VM view
                addReads0(m, m2);
            }
            m.reads = reads;

            // automatic modules read all unnamed modules
            if (descriptor.isAutomatic()) {
                m.implAddReads(ALL_UNNAMED_MODULE, true);
            }

            // exports and opens, skipped for open and automatic
            if (!descriptor.isOpen() && !descriptor.isAutomatic()) {
                if (isBootLayer && descriptor.opens().isEmpty()) {
                    // no open packages, no qualified exports to modules in parent layers
                    initExports(m, nameToModule);
                } else {
                    initExportsAndOpens(m, nameToSource, nameToModule, layer.parents());
                }
            }
        }

        // if there are modules defined to the boot or platform class loaders
        // then register the modules in the class loader's services catalog
        if (hasPlatformModules) {
            ServicesCatalog bootCatalog = BootLoader.getServicesCatalog();
            ServicesCatalog pclCatalog = ServicesCatalog.getServicesCatalog(pcl);
            for (int index = 0; index < numModules; index++) {
                ResolvedModule resolvedModule = resolvedModules[index];
                ModuleReference mref = resolvedModule.reference();
                ModuleDescriptor descriptor = mref.descriptor();
                if (!descriptor.provides().isEmpty()) {
                    Module m = modules[index];
                    ClassLoader loader = classLoaders[index];
                    if (loader == null) {
                        bootCatalog.register(m);
                    } else if (loader == pcl) {
                        pclCatalog.register(m);
                    }
                }
            }
        }

        // record that there is a layer with modules defined to the class loader
        for (ClassLoader loader : toBindLoaders) {
            layer.bindToLoader(loader);
        }

        return nameToModule;
    }

    /**
     * Find the runtime Module corresponding to the given ResolvedModule
     * in the given parent layer (or its parents).
     */
    private static Module findModule(ModuleLayer parent,
                                     ResolvedModule resolvedModule) {
        Configuration cf = resolvedModule.configuration();
        String dn = resolvedModule.name();
        return parent.layers()
                .filter(l -> l.configuration() == cf)
                .findAny()
                .map(layer -> {
                    Optional<Module> om = layer.findModule(dn);
                    assert om.isPresent() : dn + " not found in layer";
                    Module m = om.get();
                    assert m.getLayer() == layer : m + " not in expected layer";
                    return m;
                })
                .orElse(null);
    }

    /**
     * Initialize/setup a module's exports.
     *
     * @param m the module
     * @param nameToModule map of module name to Module (for qualified exports)
     */
    private static void initExports(Module m, Map<String, Module> nameToModule) {
        Map<String, Set<Module>> exportedPackages = new HashMap<>();

        for (Exports exports : m.getDescriptor().exports()) {
            String source = exports.source();
            if (exports.isQualified()) {
                // qualified exports
                Set<Module> targets = new HashSet<>();
                for (String target : exports.targets()) {
                    Module m2 = nameToModule.get(target);
                    if (m2 != null) {
                        addExports0(m, source, m2);
                        targets.add(m2);
                    }
                }
                if (!targets.isEmpty()) {
                    exportedPackages.put(source, targets);
                }
            } else {
                // unqualified exports
                addExportsToAll0(m, source);
                exportedPackages.put(source, EVERYONE_SET);
            }
        }

        if (!exportedPackages.isEmpty())
            m.exportedPackages = exportedPackages;
    }

    /**
     * Initialize/setup a module's exports.
     *
     * @param m the module
     * @param nameToSource map of module name to Module for modules that m reads
     * @param nameToModule map of module name to Module for modules in the layer
     *                     under construction
     * @param parents the parent layers
     */
    private static void initExportsAndOpens(Module m,
                                            Map<String, Module> nameToSource,
                                            Map<String, Module> nameToModule,
                                            List<ModuleLayer> parents) {
        ModuleDescriptor descriptor = m.getDescriptor();
        Map<String, Set<Module>> openPackages = new HashMap<>();
        Map<String, Set<Module>> exportedPackages = new HashMap<>();

        // process the open packages first
        for (Opens opens : descriptor.opens()) {
            String source = opens.source();

            if (opens.isQualified()) {
                // qualified opens
                Set<Module> targets = new HashSet<>();
                for (String target : opens.targets()) {
                    Module m2 = findModule(target, nameToSource, nameToModule, parents);
                    if (m2 != null) {
                        addExports0(m, source, m2);
                        targets.add(m2);
                    }
                }
                if (!targets.isEmpty()) {
                    openPackages.put(source, targets);
                }
            } else {
                // unqualified opens
                addExportsToAll0(m, source);
                openPackages.put(source, EVERYONE_SET);
            }
        }

        // next the exports, skipping exports when the package is open
        for (Exports exports : descriptor.exports()) {
            String source = exports.source();

            // skip export if package is already open to everyone
            Set<Module> openToTargets = openPackages.get(source);
            if (openToTargets != null && openToTargets.contains(EVERYONE_MODULE))
                continue;

            if (exports.isQualified()) {
                // qualified exports
                Set<Module> targets = new HashSet<>();
                for (String target : exports.targets()) {
                    Module m2 = findModule(target, nameToSource, nameToModule, parents);
                    if (m2 != null) {
                        // skip qualified export if already open to m2
                        if (openToTargets == null || !openToTargets.contains(m2)) {
                            addExports0(m, source, m2);
                            targets.add(m2);
                        }
                    }
                }
                if (!targets.isEmpty()) {
                    exportedPackages.put(source, targets);
                }
            } else {
                // unqualified exports
                addExportsToAll0(m, source);
                exportedPackages.put(source, EVERYONE_SET);
            }
        }

        if (!openPackages.isEmpty())
            m.openPackages = openPackages;
        if (!exportedPackages.isEmpty())
            m.exportedPackages = exportedPackages;
    }

    /**
     * Find the runtime Module with the given name. The module name is the
     * name of a target module in a qualified exports or opens directive.
     *
     * @param target The target module to find
     * @param nameToSource The modules in parent layers that are read
     * @param nameToModule The modules in the layer under construction
     * @param parents The parent layers
     */
    private static Module findModule(String target,
                                     Map<String, Module> nameToSource,
                                     Map<String, Module> nameToModule,
                                     List<ModuleLayer> parents) {
        Module m = nameToSource.get(target);
        if (m == null) {
            m = nameToModule.get(target);
            if (m == null) {
                for (ModuleLayer parent : parents) {
                    m = parent.findModule(target).orElse(null);
                    if (m != null) break;
                }
            }
        }
        return m;
    }


    // -- annotations --

    /**
     * {@inheritDoc}
     * This method returns {@code null} when invoked on an unnamed module.
     *
     * <p> Note that any annotation returned by this method is a
     * declaration annotation.
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return moduleInfoClass().getDeclaredAnnotation(annotationClass);
    }

    /**
     * {@inheritDoc}
     * This method returns an empty array when invoked on an unnamed module.
     *
     * <p> Note that any annotations returned by this method are
     * declaration annotations.
     */
    @Override
    public Annotation[] getAnnotations() {
        return moduleInfoClass().getAnnotations();
    }

    /**
     * {@inheritDoc}
     * This method returns an empty array when invoked on an unnamed module.
     *
     * <p> Note that any annotations returned by this method are
     * declaration annotations.
     */
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return moduleInfoClass().getDeclaredAnnotations();
    }

    // cached class file with annotations
    private volatile Class<?> moduleInfoClass;

    @SuppressWarnings("removal")
    private Class<?> moduleInfoClass() {
        Class<?> clazz = this.moduleInfoClass;
        if (clazz != null)
            return clazz;

        synchronized (this) {
            clazz = this.moduleInfoClass;
            if (clazz == null) {
                if (isNamed()) {
                    PrivilegedAction<Class<?>> pa = this::loadModuleInfoClass;
                    clazz = AccessController.doPrivileged(pa);
                }
                if (clazz == null) {
                    class DummyModuleInfo { }
                    clazz = DummyModuleInfo.class;
                }
                this.moduleInfoClass = clazz;
            }
            return clazz;
        }
    }

    private Class<?> loadModuleInfoClass() {
        Class<?> clazz = null;
        try (InputStream in = getResourceAsStream("module-info.class")) {
            if (in != null)
                clazz = loadModuleInfoClass(in);
        } catch (Exception ignore) { }
        return clazz;
    }

    /**
     * Loads module-info.class as a package-private interface in a class loader
     * that is a child of this module's class loader.
     */
    private Class<?> loadModuleInfoClass(InputStream in) throws IOException {
        final String MODULE_INFO = "module-info";

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {
            @Override
            public void visit(int version,
                              int access,
                              String name,
                              String signature,
                              String superName,
                              String[] interfaces) {
                cw.visit(version,
                        Opcodes.ACC_INTERFACE
                            + Opcodes.ACC_ABSTRACT
                            + Opcodes.ACC_SYNTHETIC,
                        MODULE_INFO,
                        null,
                        "java/lang/Object",
                        null);
            }
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                // keep annotations
                return super.visitAnnotation(desc, visible);
            }
            @Override
            public void visitAttribute(Attribute attr) {
                // drop non-annotation attributes
            }
            @Override
            public ModuleVisitor visitModule(String name, int flags, String version) {
                // drop Module attribute
                return null;
            }
        };

        ClassReader cr = new ClassReader(in);
        cr.accept(cv, 0);
        byte[] bytes = cw.toByteArray();

        ClassLoader cl = new ClassLoader(loader) {
            @Override
            protected Class<?> findClass(String cn)throws ClassNotFoundException {
                if (cn.equals(MODULE_INFO)) {
                    return super.defineClass(cn, bytes, 0, bytes.length);
                } else {
                    throw new ClassNotFoundException(cn);
                }
            }
            @Override
            protected Class<?> loadClass(String cn, boolean resolve)
                throws ClassNotFoundException
            {
                synchronized (getClassLoadingLock(cn)) {
                    Class<?> c = findLoadedClass(cn);
                    if (c == null) {
                        if (cn.equals(MODULE_INFO)) {
                            c = findClass(cn);
                        } else {
                            c = super.loadClass(cn, resolve);
                        }
                    }
                    if (resolve)
                        resolveClass(c);
                    return c;
                }
            }
        };

        try {
            return cl.loadClass(MODULE_INFO);
        } catch (ClassNotFoundException e) {
            throw new InternalError(e);
        }
    }


    // -- misc --


    /**
     * Returns an input stream for reading a resource in this module.
     * The {@code name} parameter is a {@code '/'}-separated path name that
     * identifies the resource. As with {@link Class#getResourceAsStream
     * Class.getResourceAsStream}, this method delegates to the module's class
     * loader {@link ClassLoader#findResource(String,String)
     * findResource(String,String)} method, invoking it with the module name
     * (or {@code null} when the module is unnamed) and the name of the
     * resource. If the resource name has a leading slash then it is dropped
     * before delegation.
     *
     * <p> A resource in a named module may be <em>encapsulated</em> so that
     * it cannot be located by code in other modules. Whether a resource can be
     * located or not is determined as follows: </p>
     *
     * <ul>
     *     <li> If the resource name ends with  "{@code .class}" then it is not
     *     encapsulated. </li>
     *
     *     <li> A <em>package name</em> is derived from the resource name. If
     *     the package name is a {@linkplain #getPackages() package} in the
     *     module then the resource can only be located by the caller of this
     *     method when the package is {@linkplain #isOpen(String,Module) open}
     *     to at least the caller's module. If the resource is not in a
     *     package in the module then the resource is not encapsulated. </li>
     * </ul>
     *
     * <p> In the above, the <em>package name</em> for a resource is derived
     * from the subsequence of characters that precedes the last {@code '/'} in
     * the name and then replacing each {@code '/'} character in the subsequence
     * with {@code '.'}. A leading slash is ignored when deriving the package
     * name. As an example, the package name derived for a resource named
     * "{@code a/b/c/foo.properties}" is "{@code a.b.c}". A resource name
     * with the name "{@code META-INF/MANIFEST.MF}" is never encapsulated
     * because "{@code META-INF}" is not a legal package name. </p>
     *
     * <p> This method returns {@code null} if the resource is not in this
     * module, the resource is encapsulated and cannot be located by the caller,
     * or access to the resource is denied by the security manager. </p>
     *
     * @param  name
     *         The resource name
     *
     * @return An input stream for reading the resource or {@code null}
     *
     * @throws IOException
     *         If an I/O error occurs
     *
     * @see Class#getResourceAsStream(String)
     */
    @CallerSensitive
    public InputStream getResourceAsStream(String name) throws IOException {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        if (isNamed() && Resources.canEncapsulate(name)) {
            Module caller = getCallerModule(Reflection.getCallerClass());
            if (caller != this && caller != Object.class.getModule()) {
                String pn = Resources.toPackageName(name);
                if (getPackages().contains(pn)) {
                    if (caller == null && !isOpen(pn)) {
                        // no caller, package not open
                        return null;
                    }
                    if (!isOpen(pn, caller)) {
                        // package not open to caller
                        return null;
                    }
                }
            }
        }

        String mn = this.name;

        // special-case built-in class loaders to avoid URL connection
        if (loader == null) {
            return BootLoader.findResourceAsStream(mn, name);
        } else if (loader instanceof BuiltinClassLoader) {
            return ((BuiltinClassLoader) loader).findResourceAsStream(mn, name);
        }

        // locate resource in module
        URL url = loader.findResource(mn, name);
        if (url != null) {
            try {
                return url.openStream();
            } catch (SecurityException e) { }
        }

        return null;
    }

    /**
     * Returns the string representation of this module. For a named module,
     * the representation is the string {@code "module"}, followed by a space,
     * and then the module name. For an unnamed module, the representation is
     * the string {@code "unnamed module"}, followed by a space, and then an
     * implementation specific string that identifies the unnamed module.
     *
     * @return The string representation of this module
     */
    @Override
    public String toString() {
        if (isNamed()) {
            return "module " + name;
        } else {
            String id = Integer.toHexString(System.identityHashCode(this));
            return "unnamed module @" + id;
        }
    }

    /**
     * Returns the module that a given caller class is a member of. Returns
     * {@code null} if the caller is {@code null}.
     */
    private Module getCallerModule(Class<?> caller) {
        return (caller != null) ? caller.getModule() : null;
    }


    // -- native methods --

    // JVM_DefineModule
    private static native void defineModule0(Module module,
                                             boolean isOpen,
                                             String version,
                                             String location,
                                             Object[] pns);

    // JVM_AddReadsModule
    private static native void addReads0(Module from, Module to);

    // JVM_AddModuleExports
    private static native void addExports0(Module from, String pn, Module to);

    // JVM_AddModuleExportsToAll
    private static native void addExportsToAll0(Module from, String pn);

    // JVM_AddModuleExportsToAllUnnamed
    private static native void addExportsToAllUnnamed0(Module from, String pn);
}
