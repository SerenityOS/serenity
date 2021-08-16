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

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ResolvedModule;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.loader.ClassLoaderValue;
import jdk.internal.loader.Loader;
import jdk.internal.loader.LoaderPool;
import jdk.internal.module.ServicesCatalog;
import jdk.internal.misc.CDS;
import jdk.internal.vm.annotation.Stable;
import sun.security.util.SecurityConstants;


/**
 * A layer of modules in the Java virtual machine.
 *
 * <p> A layer is created from a graph of modules in a {@link Configuration}
 * and a function that maps each module to a {@link ClassLoader}.
 * Creating a layer informs the Java virtual machine about the classes that
 * may be loaded from the modules so that the Java virtual machine knows which
 * module that each class is a member of. </p>
 *
 * <p> Creating a layer creates a {@link Module} object for each {@link
 * ResolvedModule} in the configuration. For each resolved module that is
 * {@link ResolvedModule#reads() read}, the {@code Module} {@link
 * Module#canRead reads} the corresponding run-time {@code Module}, which may
 * be in the same layer or a {@link #parents() parent} layer. </p>
 *
 * <p> The {@link #defineModulesWithOneLoader defineModulesWithOneLoader} and
 * {@link #defineModulesWithManyLoaders defineModulesWithManyLoaders} methods
 * provide convenient ways to create a module layer where all modules are
 * mapped to a single class loader or where each module is mapped to its own
 * class loader. The {@link #defineModules defineModules} method is for more
 * advanced cases where modules are mapped to custom class loaders by means of
 * a function specified to the method. Each of these methods has an instance
 * and static variant. The instance methods create a layer with the receiver
 * as the parent layer. The static methods are for more advanced cases where
 * there can be more than one parent layer or where a {@link
 * ModuleLayer.Controller Controller} is needed to control modules in the layer
 * </p>
 *
 * <p> A Java virtual machine has at least one non-empty layer, the {@link
 * #boot() boot} layer, that is created when the Java virtual machine is
 * started. The boot layer contains module {@code java.base} and is the only
 * layer in the Java virtual machine with a module named "{@code java.base}".
 * The modules in the boot layer are mapped to the bootstrap class loader and
 * other class loaders that are <a href="ClassLoader.html#builtinLoaders">
 * built-in</a> into the Java virtual machine. The boot layer will often be
 * the {@link #parents() parent} when creating additional layers. </p>
 *
 * <p> Each {@code Module} in a layer is created so that it {@link
 * Module#isExported(String) exports} and {@link Module#isOpen(String) opens}
 * the packages described by its {@link ModuleDescriptor}. Qualified exports
 * (where a package is exported to a set of target modules rather than all
 * modules) are reified when creating the layer as follows: </p>
 * <ul>
 *     <li> If module {@code X} exports a package to {@code Y}, and if the
 *     runtime {@code Module} {@code X} reads {@code Module} {@code Y}, then
 *     the package is exported to {@code Module} {@code Y} (which may be in
 *     the same layer as {@code X} or a parent layer). </li>
 *
 *     <li> If module {@code X} exports a package to {@code Y}, and if the
 *     runtime {@code Module} {@code X} does not read {@code Y} then target
 *     {@code Y} is located as if by invoking {@link #findModule(String)
 *     findModule} to find the module in the layer or its parent layers. If
 *     {@code Y} is found then the package is exported to the instance of
 *     {@code Y} that was found. If {@code Y} is not found then the qualified
 *     export is ignored. </li>
 * </ul>
 *
 * <p> Qualified opens are handled in same way as qualified exports. </p>
 *
 * <p> As when creating a {@code Configuration},
 * {@link ModuleDescriptor#isAutomatic() automatic} modules receive special
 * treatment when creating a layer. An automatic module is created in the
 * Java virtual machine as a {@code Module} that reads every unnamed {@code
 * Module} in the Java virtual machine. </p>
 *
 * <p> Unless otherwise specified, passing a {@code null} argument to a method
 * in this class causes a {@link NullPointerException NullPointerException} to
 * be thrown. </p>
 *
 * <h2> Example usage: </h2>
 *
 * <p> This example creates a configuration by resolving a module named
 * "{@code myapp}" with the configuration for the boot layer as the parent. It
 * then creates a new layer with the modules in this configuration. All modules
 * are defined to the same class loader. </p>
 *
 * <pre>{@code
 *     ModuleFinder finder = ModuleFinder.of(dir1, dir2, dir3);
 *
 *     ModuleLayer parent = ModuleLayer.boot();
 *
 *     Configuration cf = parent.configuration().resolve(finder, ModuleFinder.of(), Set.of("myapp"));
 *
 *     ClassLoader scl = ClassLoader.getSystemClassLoader();
 *
 *     ModuleLayer layer = parent.defineModulesWithOneLoader(cf, scl);
 *
 *     Class<?> c = layer.findLoader("myapp").loadClass("app.Main");
 * }</pre>
 *
 * @since 9
 * @see Module#getLayer()
 */

public final class ModuleLayer {

    // the empty layer (may be initialized from the CDS archive)
    private static @Stable ModuleLayer EMPTY_LAYER;
    static {
        CDS.initializeFromArchive(ModuleLayer.class);
        if (EMPTY_LAYER == null) {
            // create a new empty layer if there is no archived version.
            EMPTY_LAYER = new ModuleLayer(Configuration.empty(), List.of(), null);
        }
    }

    // the configuration from which this layer was created
    private final Configuration cf;

    // parent layers, empty in the case of the empty layer
    private final List<ModuleLayer> parents;

    // maps module name to jlr.Module
    private final Map<String, Module> nameToModule;

    /**
     * Creates a new module layer from the modules in the given configuration.
     */
    private ModuleLayer(Configuration cf,
                        List<ModuleLayer> parents,
                        Function<String, ClassLoader> clf)
    {
        this.cf = cf;
        this.parents = parents; // no need to do defensive copy

        Map<String, Module> map;
        if (parents.isEmpty()) {
            map = Map.of();
        } else {
            map = Module.defineModules(cf, clf, this);
        }
        this.nameToModule = map; // no need to do defensive copy
    }

    /**
     * Controls a module layer. The static methods defined by {@link ModuleLayer}
     * to create module layers return a {@code Controller} that can be used to
     * control modules in the layer.
     *
     * <p> Unless otherwise specified, passing a {@code null} argument to a
     * method in this class causes a {@link NullPointerException
     * NullPointerException} to be thrown. </p>
     *
     * @apiNote Care should be taken with {@code Controller} objects, they
     * should never be shared with untrusted code.
     *
     * @since 9
     */
    public static final class Controller {
        private final ModuleLayer layer;

        Controller(ModuleLayer layer) {
            this.layer = layer;
        }

        /**
         * Returns the layer that this object controls.
         *
         * @return the module layer
         */
        public ModuleLayer layer() {
            return layer;
        }

        private void ensureInLayer(Module source) {
            if (source.getLayer() != layer)
                throw new IllegalArgumentException(source + " not in layer");
        }


        /**
         * Updates module {@code source} in the layer to read module
         * {@code target}. This method is a no-op if {@code source} already
         * reads {@code target}.
         *
         * @implNote <em>Read edges</em> added by this method are <em>weak</em>
         * and do not prevent {@code target} from being GC'ed when {@code source}
         * is strongly reachable.
         *
         * @param  source
         *         The source module
         * @param  target
         *         The target module to read
         *
         * @return This controller
         *
         * @throws IllegalArgumentException
         *         If {@code source} is not in the module layer
         *
         * @see Module#addReads
         */
        public Controller addReads(Module source, Module target) {
            ensureInLayer(source);
            source.implAddReads(target);
            return this;
        }

        /**
         * Updates module {@code source} in the layer to export a package to
         * module {@code target}. This method is a no-op if {@code source}
         * already exports the package to at least {@code target}.
         *
         * @param  source
         *         The source module
         * @param  pn
         *         The package name
         * @param  target
         *         The target module
         *
         * @return This controller
         *
         * @throws IllegalArgumentException
         *         If {@code source} is not in the module layer or the package
         *         is not in the source module
         *
         * @see Module#addExports
         */
        public Controller addExports(Module source, String pn, Module target) {
            ensureInLayer(source);
            source.implAddExports(pn, target);
            return this;
        }

        /**
         * Updates module {@code source} in the layer to open a package to
         * module {@code target}. This method is a no-op if {@code source}
         * already opens the package to at least {@code target}.
         *
         * @param  source
         *         The source module
         * @param  pn
         *         The package name
         * @param  target
         *         The target module
         *
         * @return This controller
         *
         * @throws IllegalArgumentException
         *         If {@code source} is not in the module layer or the package
         *         is not in the source module
         *
         * @see Module#addOpens
         */
        public Controller addOpens(Module source, String pn, Module target) {
            ensureInLayer(source);
            source.implAddOpens(pn, target);
            return this;
        }
    }


    /**
     * Creates a new module layer, with this layer as its parent, by defining the
     * modules in the given {@code Configuration} to the Java virtual machine.
     * This method creates one class loader and defines all modules to that
     * class loader. The {@link ClassLoader#getParent() parent} of each class
     * loader is the given parent class loader. This method works exactly as
     * specified by the static {@link
     * #defineModulesWithOneLoader(Configuration,List,ClassLoader)
     * defineModulesWithOneLoader} method when invoked with this layer as the
     * parent. In other words, if this layer is {@code thisLayer} then this
     * method is equivalent to invoking:
     * <pre> {@code
     *     ModuleLayer.defineModulesWithOneLoader(cf, List.of(thisLayer), parentLoader).layer();
     * }</pre>
     *
     * @param  cf
     *         The configuration for the layer
     * @param  parentLoader
     *         The parent class loader for the class loader created by this
     *         method; may be {@code null} for the bootstrap class loader
     *
     * @return The newly created layer
     *
     * @throws IllegalArgumentException
     *         If the given configuration has more than one parent or the parent
     *         of the configuration is not the configuration for this layer
     * @throws LayerInstantiationException
     *         If the layer cannot be created for any of the reasons specified
     *         by the static {@code defineModulesWithOneLoader} method
     * @throws SecurityException
     *         If {@code RuntimePermission("createClassLoader")} or
     *         {@code RuntimePermission("getClassLoader")} is denied by
     *         the security manager
     *
     * @see #findLoader
     */
    public ModuleLayer defineModulesWithOneLoader(Configuration cf,
                                                  ClassLoader parentLoader) {
        return defineModulesWithOneLoader(cf, List.of(this), parentLoader).layer();
    }


    /**
     * Creates a new module layer, with this layer as its parent, by defining the
     * modules in the given {@code Configuration} to the Java virtual machine.
     * Each module is defined to its own {@link ClassLoader} created by this
     * method. The {@link ClassLoader#getParent() parent} of each class loader
     * is the given parent class loader. This method works exactly as specified
     * by the static {@link
     * #defineModulesWithManyLoaders(Configuration,List,ClassLoader)
     * defineModulesWithManyLoaders} method when invoked with this layer as the
     * parent. In other words, if this layer is {@code thisLayer} then this
     * method is equivalent to invoking:
     * <pre> {@code
     *     ModuleLayer.defineModulesWithManyLoaders(cf, List.of(thisLayer), parentLoader).layer();
     * }</pre>
     *
     * @param  cf
     *         The configuration for the layer
     * @param  parentLoader
     *         The parent class loader for each of the class loaders created by
     *         this method; may be {@code null} for the bootstrap class loader
     *
     * @return The newly created layer
     *
     * @throws IllegalArgumentException
     *         If the given configuration has more than one parent or the parent
     *         of the configuration is not the configuration for this layer
     * @throws LayerInstantiationException
     *         If the layer cannot be created for any of the reasons specified
     *         by the static {@code defineModulesWithManyLoaders} method
     * @throws SecurityException
     *         If {@code RuntimePermission("createClassLoader")} or
     *         {@code RuntimePermission("getClassLoader")} is denied by
     *         the security manager
     *
     * @see #findLoader
     */
    public ModuleLayer defineModulesWithManyLoaders(Configuration cf,
                                                    ClassLoader parentLoader) {
        return defineModulesWithManyLoaders(cf, List.of(this), parentLoader).layer();
    }


    /**
     * Creates a new module layer, with this layer as its parent, by defining the
     * modules in the given {@code Configuration} to the Java virtual machine.
     * Each module is mapped, by name, to its class loader by means of the
     * given function. This method works exactly as specified by the static
     * {@link #defineModules(Configuration,List,Function) defineModules}
     * method when invoked with this layer as the parent. In other words, if
     * this layer is {@code thisLayer} then this method is equivalent to
     * invoking:
     * <pre> {@code
     *     ModuleLayer.defineModules(cf, List.of(thisLayer), clf).layer();
     * }</pre>
     *
     * @param  cf
     *         The configuration for the layer
     * @param  clf
     *         The function to map a module name to a class loader
     *
     * @return The newly created layer
     *
     * @throws IllegalArgumentException
     *         If the given configuration has more than one parent or the parent
     *         of the configuration is not the configuration for this layer
     * @throws LayerInstantiationException
     *         If the layer cannot be created for any of the reasons specified
     *         by the static {@code defineModules} method
     * @throws SecurityException
     *         If {@code RuntimePermission("getClassLoader")} is denied by
     *         the security manager
     */
    public ModuleLayer defineModules(Configuration cf,
                                     Function<String, ClassLoader> clf) {
        return defineModules(cf, List.of(this), clf).layer();
    }

    /**
     * Creates a new module layer by defining the modules in the given {@code
     * Configuration} to the Java virtual machine. This method creates one
     * class loader and defines all modules to that class loader.
     *
     * <p> The class loader created by this method implements <em>direct
     * delegation</em> when loading classes from modules. If the {@link
     * ClassLoader#loadClass(String, boolean) loadClass} method is invoked to
     * load a class then it uses the package name of the class to map it to a
     * module. This may be a module in this layer and hence defined to the same
     * class loader. It may be a package in a module in a parent layer that is
     * exported to one or more of the modules in this layer. The class
     * loader delegates to the class loader of the module, throwing {@code
     * ClassNotFoundException} if not found by that class loader.
     * When {@code loadClass} is invoked to load classes that do not map to a
     * module then it delegates to the parent class loader. </p>
     *
     * <p> The class loader created by this method locates resources
     * ({@link ClassLoader#getResource(String) getResource}, {@link
     * ClassLoader#getResources(String) getResources}, and other resource
     * methods) in all modules in the layer before searching the parent class
     * loader. </p>
     *
     * <p> Attempting to create a layer with all modules defined to the same
     * class loader can fail for the following reasons:
     *
     * <ul>
     *
     *     <li><p> <em>Overlapping packages</em>: Two or more modules in the
     *     configuration have the same package. </p></li>
     *
     *     <li><p> <em>Split delegation</em>: The resulting class loader would
     *     need to delegate to more than one class loader in order to load
     *     classes in a specific package. </p></li>
     *
     * </ul>
     *
     * <p> In addition, a layer cannot be created if the configuration contains
     * a module named "{@code java.base}", or a module contains a package named
     * "{@code java}" or a package with a name starting with "{@code java.}". </p>
     *
     * <p> If there is a security manager then the class loader created by
     * this method will load classes and resources with privileges that are
     * restricted by the calling context of this method. </p>
     *
     * @param  cf
     *         The configuration for the layer
     * @param  parentLayers
     *         The list of parent layers in search order
     * @param  parentLoader
     *         The parent class loader for the class loader created by this
     *         method; may be {@code null} for the bootstrap class loader
     *
     * @return A controller that controls the newly created layer
     *
     * @throws IllegalArgumentException
     *         If the parent(s) of the given configuration do not match the
     *         configuration of the parent layers, including order
     * @throws LayerInstantiationException
     *         If all modules cannot be defined to the same class loader for any
     *         of the reasons listed above
     * @throws SecurityException
     *         If {@code RuntimePermission("createClassLoader")} or
     *         {@code RuntimePermission("getClassLoader")} is denied by
     *         the security manager
     *
     * @see #findLoader
     */
    public static Controller defineModulesWithOneLoader(Configuration cf,
                                                        List<ModuleLayer> parentLayers,
                                                        ClassLoader parentLoader)
    {
        List<ModuleLayer> parents = List.copyOf(parentLayers);
        checkConfiguration(cf, parents);

        checkCreateClassLoaderPermission();
        checkGetClassLoaderPermission();

        try {
            Loader loader = new Loader(cf.modules(), parentLoader);
            loader.initRemotePackageMap(cf, parents);
            ModuleLayer layer = new ModuleLayer(cf, parents, mn -> loader);
            return new Controller(layer);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new LayerInstantiationException(e.getMessage());
        }
    }

    /**
     * Creates a new module layer by defining the modules in the given {@code
     * Configuration} to the Java virtual machine. Each module is defined to
     * its own {@link ClassLoader} created by this method. The {@link
     * ClassLoader#getParent() parent} of each class loader is the given parent
     * class loader.
     *
     * <p> The class loaders created by this method implement <em>direct
     * delegation</em> when loading classes from modules. If the {@link
     * ClassLoader#loadClass(String, boolean) loadClass} method is invoked to
     * load a class then it uses the package name of the class to map it to a
     * module. The package may be in the module defined to the class loader.
     * The package may be exported by another module in this layer to the
     * module defined to the class loader. It may be in a package exported by a
     * module in a parent layer. The class loader delegates to the class loader
     * of the module, throwing {@code ClassNotFoundException} if not found by
     * that class loader. When {@code loadClass} is invoked to load a class
     * that does not map to a module then it delegates to the parent class
     * loader. </p>
     *
     * <p> The class loaders created by this method locate resources
     * ({@link ClassLoader#getResource(String) getResource}, {@link
     * ClassLoader#getResources(String) getResources}, and other resource
     * methods) in the module defined to the class loader before searching
     * the parent class loader. </p>
     *
     * <p> If there is a security manager then the class loaders created by
     * this method will load classes and resources with privileges that are
     * restricted by the calling context of this method. </p>
     *
     * @param  cf
     *         The configuration for the layer
     * @param  parentLayers
     *         The list of parent layers in search order
     * @param  parentLoader
     *         The parent class loader for each of the class loaders created by
     *         this method; may be {@code null} for the bootstrap class loader
     *
     * @return A controller that controls the newly created layer
     *
     * @throws IllegalArgumentException
     *         If the parent(s) of the given configuration do not match the
     *         configuration of the parent layers, including order
     * @throws LayerInstantiationException
     *         If the layer cannot be created because the configuration contains
     *         a module named "{@code java.base}" or a module contains a package
     *         named "{@code java}" or a package with a name starting with
     *         "{@code java.}"
     *
     * @throws SecurityException
     *         If {@code RuntimePermission("createClassLoader")} or
     *         {@code RuntimePermission("getClassLoader")} is denied by
     *         the security manager
     *
     * @see #findLoader
     */
    public static Controller defineModulesWithManyLoaders(Configuration cf,
                                                          List<ModuleLayer> parentLayers,
                                                          ClassLoader parentLoader)
    {
        List<ModuleLayer> parents = List.copyOf(parentLayers);
        checkConfiguration(cf, parents);

        checkCreateClassLoaderPermission();
        checkGetClassLoaderPermission();

        LoaderPool pool = new LoaderPool(cf, parents, parentLoader);
        try {
            ModuleLayer layer = new ModuleLayer(cf, parents, pool::loaderFor);
            return new Controller(layer);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new LayerInstantiationException(e.getMessage());
        }
    }

    /**
     * Creates a new module layer by defining the modules in the given {@code
     * Configuration} to the Java virtual machine. The given function maps each
     * module in the configuration, by name, to a class loader. Creating the
     * layer informs the Java virtual machine about the classes that may be
     * loaded so that the Java virtual machine knows which module that each
     * class is a member of.
     *
     * <p> The class loader delegation implemented by the class loaders must
     * respect module readability. The class loaders should be
     * {@link ClassLoader#registerAsParallelCapable parallel-capable} so as to
     * avoid deadlocks during class loading. In addition, the entity creating
     * a new layer with this method should arrange that the class loaders be
     * ready to load from these modules before there are any attempts to load
     * classes or resources. </p>
     *
     * <p> Creating a layer can fail for the following reasons: </p>
     *
     * <ul>
     *
     *     <li><p> Two or more modules with the same package are mapped to the
     *     same class loader. </p></li>
     *
     *     <li><p> A module is mapped to a class loader that already has a
     *     module of the same name defined to it. </p></li>
     *
     *     <li><p> A module is mapped to a class loader that has already
     *     defined types in any of the packages in the module. </p></li>
     *
     * </ul>
     *
     * <p> In addition, a layer cannot be created if the configuration contains
     * a module named "{@code java.base}", a configuration contains a module
     * with a package named "{@code java}" or a package name starting with
     * "{@code java.}", or the function to map a module name to a class loader
     * returns {@code null} or the {@linkplain ClassLoader#getPlatformClassLoader()
     * platform class loader}. </p>
     *
     * <p> If the function to map a module name to class loader throws an error
     * or runtime exception then it is propagated to the caller of this method.
     * </p>
     *
     * @apiNote It is implementation specific as to whether creating a layer
     * with this method is an atomic operation or not. Consequentially it is
     * possible for this method to fail with some modules, but not all, defined
     * to the Java virtual machine.
     *
     * @param  cf
     *         The configuration for the layer
     * @param  parentLayers
     *         The list of parent layers in search order
     * @param  clf
     *         The function to map a module name to a class loader
     *
     * @return A controller that controls the newly created layer
     *
     * @throws IllegalArgumentException
     *         If the parent(s) of the given configuration do not match the
     *         configuration of the parent layers, including order
     * @throws LayerInstantiationException
     *         If creating the layer fails for any of the reasons listed above
     * @throws SecurityException
     *         If {@code RuntimePermission("getClassLoader")} is denied by
     *         the security manager
     */
    public static Controller defineModules(Configuration cf,
                                           List<ModuleLayer> parentLayers,
                                           Function<String, ClassLoader> clf)
    {
        List<ModuleLayer> parents = List.copyOf(parentLayers);
        checkConfiguration(cf, parents);
        Objects.requireNonNull(clf);

        checkGetClassLoaderPermission();

        // The boot layer is checked during module system initialization
        if (boot() != null) {
            checkForDuplicatePkgs(cf, clf);
        }

        try {
            ModuleLayer layer = new ModuleLayer(cf, parents, clf);
            return new Controller(layer);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new LayerInstantiationException(e.getMessage());
        }
    }


    /**
     * Checks that the parent configurations match the configuration of
     * the parent layers.
     */
    private static void checkConfiguration(Configuration cf,
                                           List<ModuleLayer> parentLayers)
    {
        Objects.requireNonNull(cf);

        List<Configuration> parentConfigurations = cf.parents();
        if (parentLayers.size() != parentConfigurations.size())
            throw new IllegalArgumentException("wrong number of parents");

        int index = 0;
        for (ModuleLayer parent : parentLayers) {
            if (parent.configuration() != parentConfigurations.get(index)) {
                throw new IllegalArgumentException(
                        "Parent of configuration != configuration of this Layer");
            }
            index++;
        }
    }

    private static void checkCreateClassLoaderPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(SecurityConstants.CREATE_CLASSLOADER_PERMISSION);
    }

    private static void checkGetClassLoaderPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
    }

    /**
     * Checks a configuration and the module-to-loader mapping to ensure that
     * no two modules mapped to the same class loader have the same package.
     * It also checks that no two automatic modules have the same package.
     *
     * @throws LayerInstantiationException
     */
    private static void checkForDuplicatePkgs(Configuration cf,
                                              Function<String, ClassLoader> clf)
    {
        // HashMap allows null keys
        Map<ClassLoader, Set<String>> loaderToPackages = new HashMap<>();
        for (ResolvedModule resolvedModule : cf.modules()) {
            ModuleDescriptor descriptor = resolvedModule.reference().descriptor();
            ClassLoader loader = clf.apply(descriptor.name());

            Set<String> loaderPackages
                = loaderToPackages.computeIfAbsent(loader, k -> new HashSet<>());

            for (String pkg : descriptor.packages()) {
                boolean added = loaderPackages.add(pkg);
                if (!added) {
                    throw fail("More than one module with package %s mapped" +
                               " to the same class loader", pkg);
                }
            }
        }
    }

    /**
     * Creates a LayerInstantiationException with the a message formatted from
     * the given format string and arguments.
     */
    private static LayerInstantiationException fail(String fmt, Object ... args) {
        String msg = String.format(fmt, args);
        return new LayerInstantiationException(msg);
    }


    /**
     * Returns the configuration for this layer.
     *
     * @return The configuration for this layer
     */
    public Configuration configuration() {
        return cf;
    }

    /**
     * Returns an unmodifiable list of this layer's parents, in search
     * order. If this is the {@linkplain #empty() empty layer} then an
     * empty list is returned.
     *
     * @return A possibly-empty unmodifiable list of this layer's parents
     */
    public List<ModuleLayer> parents() {
        return parents;
    }


    /**
     * Returns an ordered stream of layers. The first element is this layer,
     * the remaining elements are the parent layers in DFS order.
     *
     * @implNote For now, the assumption is that the number of elements will
     * be very low and so this method does not use a specialized spliterator.
     */
    Stream<ModuleLayer> layers() {
        List<ModuleLayer> allLayers = this.allLayers;
        if (allLayers != null)
            return allLayers.stream();

        allLayers = new ArrayList<>();
        Set<ModuleLayer> visited = new HashSet<>();
        Deque<ModuleLayer> stack = new ArrayDeque<>();
        visited.add(this);
        stack.push(this);

        while (!stack.isEmpty()) {
            ModuleLayer layer = stack.pop();
            allLayers.add(layer);

            // push in reverse order
            for (int i = layer.parents.size() - 1; i >= 0; i--) {
                ModuleLayer parent = layer.parents.get(i);
                if (visited.add(parent)) {
                    stack.push(parent);
                }
            }
        }

        this.allLayers = allLayers = Collections.unmodifiableList(allLayers);
        return allLayers.stream();
    }

    private volatile List<ModuleLayer> allLayers;

    /**
     * Returns an unmodifiable set of the modules in this layer.
     *
     * @return A possibly-empty unmodifiable set of the modules in this layer
     */
    public Set<Module> modules() {
        Set<Module> modules = this.modules;
        if (modules == null) {
            this.modules = modules = Set.copyOf(nameToModule.values());
        }
        return modules;
    }

    private volatile Set<Module> modules;


    /**
     * Returns the module with the given name in this layer, or if not in this
     * layer, the {@linkplain #parents() parent} layers. Finding a module in
     * parent layers is equivalent to invoking {@code findModule} on each
     * parent, in search order, until the module is found or all parents have
     * been searched. In a <em>tree of layers</em>  then this is equivalent to
     * a depth-first search.
     *
     * @param  name
     *         The name of the module to find
     *
     * @return The module with the given name or an empty {@code Optional}
     *         if there isn't a module with this name in this layer or any
     *         parent layer
     */
    public Optional<Module> findModule(String name) {
        Objects.requireNonNull(name);
        if (this == EMPTY_LAYER)
            return Optional.empty();
        Module m = nameToModule.get(name);
        if (m != null)
            return Optional.of(m);

        return layers()
                .skip(1)  // skip this layer
                .map(l -> l.nameToModule.get(name))
                .filter(Objects::nonNull)
                .findAny();
    }


    /**
     * Returns the {@code ClassLoader} for the module with the given name. If
     * a module of the given name is not in this layer then the {@link #parents()
     * parent} layers are searched in the manner specified by {@link
     * #findModule(String) findModule}.
     *
     * <p> If there is a security manager then its {@code checkPermission}
     * method is called with a {@code RuntimePermission("getClassLoader")}
     * permission to check that the caller is allowed to get access to the
     * class loader. </p>
     *
     * @apiNote This method does not return an {@code Optional<ClassLoader>}
     * because `null` must be used to represent the bootstrap class loader.
     *
     * @param  name
     *         The name of the module to find
     *
     * @return The ClassLoader that the module is defined to
     *
     * @throws IllegalArgumentException if a module of the given name is not
     *         defined in this layer or any parent of this layer
     *
     * @throws SecurityException if denied by the security manager
     */
    public ClassLoader findLoader(String name) {
        Optional<Module> om = findModule(name);

        // can't use map(Module::getClassLoader) as class loader can be null
        if (om.isPresent()) {
            return om.get().getClassLoader();
        } else {
            throw new IllegalArgumentException("Module " + name
                                               + " not known to this layer");
        }
    }

    /**
     * Returns a string describing this module layer.
     *
     * @return A possibly empty string describing this module layer
     */
    @Override
    public String toString() {
        return modules().stream()
                .map(Module::getName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns the <em>empty</em> layer. There are no modules in the empty
     * layer. It has no parents.
     *
     * @return The empty layer
     */
    public static ModuleLayer empty() {
        return EMPTY_LAYER;
    }


    /**
     * Returns the boot layer. The boot layer contains at least one module,
     * {@code java.base}. Its parent is the {@link #empty() empty} layer.
     *
     * @apiNote This method returns {@code null} during startup and before
     *          the boot layer is fully initialized.
     *
     * @return The boot layer
     */
    public static ModuleLayer boot() {
        return System.bootLayer;
    }

    /**
     * Returns the ServicesCatalog for this Layer, creating it if not
     * already created.
     */
    ServicesCatalog getServicesCatalog() {
        ServicesCatalog servicesCatalog = this.servicesCatalog;
        if (servicesCatalog != null)
            return servicesCatalog;

        synchronized (this) {
            servicesCatalog = this.servicesCatalog;
            if (servicesCatalog == null) {
                servicesCatalog = ServicesCatalog.create();
                for (Module m : nameToModule.values()) {
                    servicesCatalog.register(m);
                }
                this.servicesCatalog = servicesCatalog;
            }
        }

        return servicesCatalog;
    }

    private volatile ServicesCatalog servicesCatalog;


    /**
     * Record that this layer has at least one module defined to the given
     * class loader.
     */
    void bindToLoader(ClassLoader loader) {
        // CLV.computeIfAbsent(loader, (cl, clv) -> new CopyOnWriteArrayList<>())
        List<ModuleLayer> list = CLV.get(loader);
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            List<ModuleLayer> previous = CLV.putIfAbsent(loader, list);
            if (previous != null) list = previous;
        }
        list.add(this);
    }

    /**
     * Returns a stream of the layers that have at least one module defined to
     * the given class loader.
     */
    static Stream<ModuleLayer> layers(ClassLoader loader) {
        List<ModuleLayer> list = CLV.get(loader);
        if (list != null) {
            return list.stream();
        } else {
            return Stream.empty();
        }
    }

    // the list of layers with modules defined to a class loader
    private static final ClassLoaderValue<List<ModuleLayer>> CLV = new ClassLoaderValue<>();
}
