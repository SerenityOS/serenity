/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.io.PrintStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jdk.internal.access.JavaLangModuleAccess;
import jdk.internal.loader.BootLoader;
import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.ClassLoaders;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

/**
 * A helper class for creating and updating modules. This class is intended to
 * support command-line options, tests, and the instrumentation API. It is also
 * used by the VM to load modules or add read edges when agents are instrumenting
 * code that need to link to supporting classes.
 *
 * The parameters that are package names in this API are the fully-qualified
 * names of the packages as defined in section 6.5.3 of <cite>The Java
 * Language Specification </cite>, for example, {@code "java.lang"}.
 */

public class Modules {
    private Modules() { }

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    private static final JavaLangModuleAccess JLMA = SharedSecrets.getJavaLangModuleAccess();

    /**
     * Creates a new Module. The module has the given ModuleDescriptor and
     * is defined to the given class loader.
     *
     * The resulting Module is in a larval state in that it does not read
     * any other module and does not have any exports.
     *
     * The URI is for information purposes only.
     */
    public static Module defineModule(ClassLoader loader,
                                      ModuleDescriptor descriptor,
                                      URI uri)
    {
        return JLA.defineModule(loader, descriptor, uri);
    }

    /**
     * Updates m1 to read m2.
     * Same as m1.addReads(m2) but without a caller check.
     */
    public static void addReads(Module m1, Module m2) {
        JLA.addReads(m1, m2);
    }

    /**
     * Update module m to read all unnamed modules.
     */
    public static void addReadsAllUnnamed(Module m) {
        JLA.addReadsAllUnnamed(m);
    }

    /**
     * Updates module m1 to export a package to module m2.
     * Same as m1.addExports(pn, m2) but without a caller check
     */
    public static void addExports(Module m1, String pn, Module m2) {
        JLA.addExports(m1, pn, m2);
    }

    /**
     * Updates module m to export a package unconditionally.
     */
    public static void addExports(Module m, String pn) {
        JLA.addExports(m, pn);
    }

    /**
     * Updates module m to export a package to all unnamed modules.
     */
    public static void addExportsToAllUnnamed(Module m, String pn) {
        JLA.addExportsToAllUnnamed(m, pn);
    }

    /**
     * Updates module m1 to open a package to module m2.
     * Same as m1.addOpens(pn, m2) but without a caller check.
     */
    public static void addOpens(Module m1, String pn, Module m2) {
        JLA.addOpens(m1, pn, m2);
    }

    /**
     * Updates module m to open a package to all unnamed modules.
     */
    public static void addOpensToAllUnnamed(Module m, String pn) {
        JLA.addOpensToAllUnnamed(m, pn);
    }

    /**
     * Updates module m to use a service.
     * Same as m2.addUses(service) but without a caller check.
     */
    public static void addUses(Module m, Class<?> service) {
        JLA.addUses(m, service);
    }

    /**
     * Updates module m to provide a service
     */
    public static void addProvides(Module m, Class<?> service, Class<?> impl) {
        ModuleLayer layer = m.getLayer();

        PrivilegedAction<ClassLoader> pa = m::getClassLoader;
        @SuppressWarnings("removal")
        ClassLoader loader = AccessController.doPrivileged(pa);

        ClassLoader platformClassLoader = ClassLoaders.platformClassLoader();
        if (layer == null || loader == null || loader == platformClassLoader) {
            // update ClassLoader catalog
            ServicesCatalog catalog;
            if (loader == null) {
                catalog = BootLoader.getServicesCatalog();
            } else {
                catalog = ServicesCatalog.getServicesCatalog(loader);
            }
            catalog.addProvider(m, service, impl);
        }

        if (layer != null) {
            // update Layer catalog
            JLA.getServicesCatalog(layer).addProvider(m, service, impl);
        }
    }

    /**
     * Resolves a collection of root modules, with service binding and the empty
     * Configuration as the parent to create a Configuration for the boot layer.
     *
     * This method is intended to be used to create the Configuration for the
     * boot layer during startup or at a link-time.
     */
    public static Configuration newBootLayerConfiguration(ModuleFinder finder,
                                                          Collection<String> roots,
                                                          PrintStream traceOutput)
    {
        return JLMA.resolveAndBind(finder, roots, traceOutput);
    }

    /**
     * Called by the VM when code in the given Module has been transformed by
     * an agent and so may have been instrumented to call into supporting
     * classes on the boot class path or application class path.
     */
    public static void transformedByAgent(Module m) {
        addReads(m, BootLoader.getUnnamedModule());
        addReads(m, ClassLoaders.appClassLoader().getUnnamedModule());
    }

    /**
     * Called by the VM to load a system module, typically "java.instrument" or
     * "jdk.management.agent". If the module is not loaded then it is resolved
     * and loaded (along with any dependences that weren't previously loaded)
     * into a child layer.
     */
    public static synchronized Module loadModule(String name) {
        ModuleLayer top = topLayer;
        if (top == null)
            top = ModuleLayer.boot();

        Module module = top.findModule(name).orElse(null);
        if (module != null) {
            // module already loaded
            return module;
        }

        // resolve the module with the top-most layer as the parent
        ModuleFinder empty = ModuleFinder.of();
        ModuleFinder finder = ModuleBootstrap.unlimitedFinder();
        Set<String> roots = Set.of(name);
        Configuration cf = top.configuration().resolveAndBind(empty, finder, roots);

        // create the child layer
        Function<String, ClassLoader> clf = ModuleLoaderMap.mappingFunction(cf);
        ModuleLayer newLayer = top.defineModules(cf, clf);

        // add qualified exports/opens to give access to modules in child layer
        Map<String, Module> map = newLayer.modules().stream()
                                          .collect(Collectors.toMap(Module::getName,
                                                  Function.identity()));
        ModuleLayer layer = top;
        while (layer != null) {
            for (Module m : layer.modules()) {
                // qualified exports
                m.getDescriptor().exports().stream()
                    .filter(ModuleDescriptor.Exports::isQualified)
                    .forEach(e -> e.targets().forEach(target -> {
                        Module other = map.get(target);
                        if (other != null) {
                            addExports(m, e.source(), other);
                        }}));

                // qualified opens
                m.getDescriptor().opens().stream()
                    .filter(ModuleDescriptor.Opens::isQualified)
                    .forEach(o -> o.targets().forEach(target -> {
                        Module other = map.get(target);
                        if (other != null) {
                            addOpens(m, o.source(), other);
                        }}));
            }

            List<ModuleLayer> parents = layer.parents();
            assert parents.size() <= 1;
            layer = parents.isEmpty() ? null : parents.get(0);
        }

        // update security manager before making types visible
        JLA.addNonExportedPackages(newLayer);

        // update the built-in class loaders to make the types visible
        for (ResolvedModule resolvedModule : cf.modules()) {
            ModuleReference mref = resolvedModule.reference();
            String mn = mref.descriptor().name();
            ClassLoader cl = clf.apply(mn);
            if (cl == null) {
                BootLoader.loadModule(mref);
            } else {
                ((BuiltinClassLoader) cl).loadModule(mref);
            }
        }

        // new top layer
        topLayer = newLayer;

        // return module
        return newLayer.findModule(name)
                       .orElseThrow(() -> new InternalError("module not loaded"));

    }

    /**
     * Finds the module with the given name in the boot layer or any child
     * layers created to load the "java.instrument" or "jdk.management.agent"
     * modules into a running VM.
     */
    public static Optional<Module> findLoadedModule(String name) {
        ModuleLayer top = topLayer;
        if (top == null)
            top = ModuleLayer.boot();
        return top.findModule(name);
    }

    // the top-most layer
    private static volatile ModuleLayer topLayer;

}
