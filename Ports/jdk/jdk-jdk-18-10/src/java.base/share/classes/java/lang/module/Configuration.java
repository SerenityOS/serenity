/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.module;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.misc.CDS;
import jdk.internal.module.ModuleReferenceImpl;
import jdk.internal.module.ModuleTarget;
import jdk.internal.vm.annotation.Stable;

/**
 * A configuration that is the result of <a href="package-summary.html#resolution">
 * resolution</a> or resolution with
 * <a href="{@docRoot}/java.base/java/lang/module/Configuration.html#service-binding">service binding</a>.
 *
 * <p> A configuration encapsulates the <em>readability graph</em> that is the
 * output of resolution. A readability graph is a directed graph whose vertices
 * are of type {@link ResolvedModule} and the edges represent the readability
 * amongst the modules. {@code Configuration} defines the {@link #modules()
 * modules()} method to get the set of resolved modules in the graph. {@code
 * ResolvedModule} defines the {@link ResolvedModule#reads() reads()} method to
 * get the set of modules that a resolved module reads. The modules that are
 * read may be in the same configuration or may be in {@link #parents() parent}
 * configurations. </p>
 *
 * <p> Configuration defines the {@link #resolve(ModuleFinder,List,ModuleFinder,Collection)
 * resolve} method to resolve a collection of root modules, and the {@link
 * #resolveAndBind(ModuleFinder,List,ModuleFinder,Collection) resolveAndBind}
 * method to do resolution with service binding. There are instance and
 * static variants of both methods. The instance methods create a configuration
 * with the receiver as the parent configuration. The static methods are for
 * more advanced cases where there can be more than one parent configuration. </p>
 *
 * <p> Each {@link java.lang.ModuleLayer layer} of modules in the Java virtual
 * machine is created from a configuration. The configuration for the {@link
 * java.lang.ModuleLayer#boot() boot} layer is obtained by invoking {@code
 * ModuleLayer.boot().configuration()}. The configuration for the boot layer
 * will often be the parent when creating new configurations. </p>
 *
 * <h2> Example </h2>
 *
 * <p> The following example uses the {@link
 * #resolve(ModuleFinder,ModuleFinder,Collection) resolve} method to resolve a
 * module named <em>myapp</em> with the configuration for the boot layer as the
 * parent configuration. It prints the name of each resolved module and the
 * names of the modules that each module reads. </p>
 *
 * <pre>{@code
 *    ModuleFinder finder = ModuleFinder.of(dir1, dir2, dir3);
 *
 *    Configuration parent = ModuleLayer.boot().configuration();
 *
 *    Configuration cf = parent.resolve(finder, ModuleFinder.of(), Set.of("myapp"));
 *    cf.modules().forEach(m -> {
 *        System.out.format("%s -> %s%n",
 *            m.name(),
 *            m.reads().stream()
 *                .map(ResolvedModule::name)
 *                .collect(Collectors.joining(", ")));
 *    });
 * }</pre>
 *
 * @since 9
 * @see java.lang.ModuleLayer
 */
public final class Configuration {

    // @see Configuration#empty()
    // EMPTY_CONFIGURATION may be initialized from the CDS archive.
    private static @Stable Configuration EMPTY_CONFIGURATION;

    static {
        // Initialize EMPTY_CONFIGURATION from the archive.
        CDS.initializeFromArchive(Configuration.class);
        // Create a new empty Configuration if there is no archived version.
        if (EMPTY_CONFIGURATION == null) {
            EMPTY_CONFIGURATION = new Configuration();
        }
    }

    // parent configurations, in search order
    private final List<Configuration> parents;

    private final Map<ResolvedModule, Set<ResolvedModule>> graph;
    private final Set<ResolvedModule> modules;
    private final Map<String, ResolvedModule> nameToModule;

    // constraint on target platform
    private final String targetPlatform;

    String targetPlatform() { return targetPlatform; }

    private Configuration() {
        this.parents = List.of();
        this.graph = Map.of();
        this.modules = Set.of();
        this.nameToModule = Map.of();
        this.targetPlatform = null;
    }

    private Configuration(List<Configuration> parents, Resolver resolver) {
        Map<ResolvedModule, Set<ResolvedModule>> g = resolver.finish(this);

        @SuppressWarnings(value = {"rawtypes", "unchecked"})
        Entry<String, ResolvedModule>[] nameEntries
            = (Entry<String, ResolvedModule>[])new Entry[g.size()];
        ResolvedModule[] moduleArray = new ResolvedModule[g.size()];
        int i = 0;
        for (ResolvedModule resolvedModule : g.keySet()) {
            moduleArray[i] = resolvedModule;
            nameEntries[i] = Map.entry(resolvedModule.name(), resolvedModule);
            i++;
        }

        this.parents = List.copyOf(parents);
        this.graph = g;
        this.modules = Set.of(moduleArray);
        this.nameToModule = Map.ofEntries(nameEntries);

        this.targetPlatform = resolver.targetPlatform();
    }

    /**
     * Creates the Configuration for the boot layer from a pre-generated
     * readability graph.
     *
     * @apiNote This method is coded for startup performance.
     */
    Configuration(ModuleFinder finder, Map<String, Set<String>> map) {
        int moduleCount = map.size();

        // create map of name -> ResolvedModule
        @SuppressWarnings(value = {"rawtypes", "unchecked"})
        Entry<String, ResolvedModule>[] nameEntries
            = (Entry<String, ResolvedModule>[])new Entry[moduleCount];
        ResolvedModule[] moduleArray = new ResolvedModule[moduleCount];
        String targetPlatform = null;
        int i = 0;
        for (String name : map.keySet()) {
            ModuleReference mref = finder.find(name).orElse(null);
            assert mref != null;

            if (targetPlatform == null && mref instanceof ModuleReferenceImpl) {
                ModuleTarget target = ((ModuleReferenceImpl)mref).moduleTarget();
                if (target != null) {
                    targetPlatform = target.targetPlatform();
                }
            }

            ResolvedModule resolvedModule = new ResolvedModule(this, mref);
            moduleArray[i] = resolvedModule;
            nameEntries[i] = Map.entry(name, resolvedModule);
            i++;
        }
        Map<String, ResolvedModule> nameToModule = Map.ofEntries(nameEntries);

        // create entries for readability graph
        @SuppressWarnings(value = {"rawtypes", "unchecked"})
        Entry<ResolvedModule, Set<ResolvedModule>>[] moduleEntries
            = (Entry<ResolvedModule, Set<ResolvedModule>>[])new Entry[moduleCount];
        i = 0;
        for (ResolvedModule resolvedModule : moduleArray) {
            Set<String> names = map.get(resolvedModule.name());
            ResolvedModule[] readsArray = new ResolvedModule[names.size()];
            int j = 0;
            for (String name : names) {
                readsArray[j++] = nameToModule.get(name);
            }
            moduleEntries[i++] = Map.entry(resolvedModule, Set.of(readsArray));
        }

        this.parents = List.of(empty());
        this.graph = Map.ofEntries(moduleEntries);
        this.modules = Set.of(moduleArray);
        this.nameToModule = nameToModule;
        this.targetPlatform = targetPlatform;
    }

    /**
     * Resolves a collection of root modules, with this configuration as its
     * parent, to create a new configuration. This method works exactly as
     * specified by the static {@link
     * #resolve(ModuleFinder,List,ModuleFinder,Collection) resolve}
     * method when invoked with this configuration as the parent. In other words,
     * if this configuration is {@code cf} then this method is equivalent to
     * invoking:
     * <pre> {@code
     *     Configuration.resolve(before, List.of(cf), after, roots);
     * }</pre>
     *
     * @param  before
     *         The <em>before</em> module finder to find modules
     * @param  after
     *         The <em>after</em> module finder to locate modules when not
     *         located by the {@code before} module finder or in parent
     *         configurations
     * @param  roots
     *         The possibly-empty collection of module names of the modules
     *         to resolve
     *
     * @return The configuration that is the result of resolving the given
     *         root modules
     *
     * @throws FindException
     *         If resolution fails for any of the observability-related reasons
     *         specified by the static {@code resolve} method
     * @throws ResolutionException
     *         If resolution fails any of the consistency checks specified by
     *         the static {@code resolve} method
     * @throws SecurityException
     *         If locating a module is denied by the security manager
     */
    public Configuration resolve(ModuleFinder before,
                                 ModuleFinder after,
                                 Collection<String> roots)
    {
        return resolve(before, List.of(this), after, roots);
    }


    /**
     * Resolves a collection of root modules, with service binding, and with
     * this configuration as its parent, to create a new configuration.
     * This method works exactly as specified by the static {@link
     * #resolveAndBind(ModuleFinder,List,ModuleFinder,Collection)
     * resolveAndBind} method when invoked with this configuration
     * as the parent. In other words, if this configuration is {@code cf} then
     * this method is equivalent to invoking:
     * <pre> {@code
     *     Configuration.resolveAndBind(before, List.of(cf), after, roots);
     * }</pre>
     *
     *
     * @param  before
     *         The <em>before</em> module finder to find modules
     * @param  after
     *         The <em>after</em> module finder to locate modules when not
     *         located by the {@code before} module finder or in parent
     *         configurations
     * @param  roots
     *         The possibly-empty collection of module names of the modules
     *         to resolve
     *
     * @return The configuration that is the result of resolving, with service
     *         binding, the given root modules
     *
     * @throws FindException
     *         If resolution fails for any of the observability-related reasons
     *         specified by the static {@code resolve} method
     * @throws ResolutionException
     *         If resolution fails any of the consistency checks specified by
     *         the static {@code resolve} method
     * @throws SecurityException
     *         If locating a module is denied by the security manager
     */
    public Configuration resolveAndBind(ModuleFinder before,
                                        ModuleFinder after,
                                        Collection<String> roots)
    {
        return resolveAndBind(before, List.of(this), after, roots);
    }


    /**
     * Resolves a collection of root modules, with service binding, and with
     * the empty configuration as its parent.
     *
     * This method is used to create the configuration for the boot layer.
     */
    static Configuration resolveAndBind(ModuleFinder finder,
                                        Collection<String> roots,
                                        PrintStream traceOutput)
    {
        List<Configuration> parents = List.of(empty());
        Resolver resolver = new Resolver(finder, parents, ModuleFinder.of(), traceOutput);
        resolver.resolve(roots).bind(/*bindIncubatorModules*/false);
        return new Configuration(parents, resolver);
    }

    /**
     * Resolves a collection of root modules to create a configuration.
     *
     * <p> Each root module is located using the given {@code before} module
     * finder. If a module is not found then it is located in the parent
     * configuration as if by invoking the {@link #findModule(String)
     * findModule} method on each parent in iteration order. If not found then
     * the module is located using the given {@code after} module finder. The
     * same search order is used to locate transitive dependences. Root modules
     * or dependences that are located in a parent configuration are resolved
     * no further and are not included in the resulting configuration. </p>
     *
     * <p> When all modules have been enumerated then a readability graph
     * is computed, and in conjunction with the module exports and service use,
     * checked for consistency. </p>
     *
     * <p> Resolution may fail with {@code FindException} for the following
     * <em>observability-related</em> reasons: </p>
     *
     * <ul>
     *
     *     <li><p> A root module, or a direct or transitive dependency, is not
     *     found. </p></li>
     *
     *     <li><p> An error occurs when attempting to find a module.
     *     Possible errors include I/O errors, errors detected parsing a module
     *     descriptor ({@code module-info.class}) or two versions of the same
     *     module are found in the same directory. </p></li>
     *
     * </ul>
     *
     * <p> Resolution may fail with {@code ResolutionException} if any of the
     * following consistency checks fail: </p>
     *
     * <ul>
     *
     *     <li><p> A cycle is detected, say where module {@code m1} requires
     *     module {@code m2} and {@code m2} requires {@code m1}. </p></li>
     *
     *     <li><p> A module reads two or more modules with the same name. This
     *     includes the case where a module reads another with the same name as
     *     itself. </p></li>
     *
     *     <li><p> Two or more modules in the configuration export the same
     *     package to a module that reads both. This includes the case where a
     *     module {@code M} containing package {@code p} reads another module
     *     that exports {@code p} to {@code M}. </p></li>
     *
     *     <li><p> A module {@code M} declares that it "{@code uses p.S}" or
     *     "{@code provides p.S with ...}" but package {@code p} is neither in
     *     module {@code M} nor exported to {@code M} by any module that
     *     {@code M} reads. </p></li>
     *
     * </ul>
     *
     * @implNote In the implementation then observability of modules may depend
     * on referential integrity or other checks that ensure different builds of
     * tightly coupled modules or modules for specific operating systems or
     * architectures are not combined in the same configuration.
     *
     * @param  before
     *         The <em>before</em> module finder to find modules
     * @param  parents
     *         The list parent configurations in search order
     * @param  after
     *         The <em>after</em> module finder to locate modules when not
     *         located by the {@code before} module finder or in parent
     *         configurations
     * @param  roots
     *         The possibly-empty collection of module names of the modules
     *         to resolve
     *
     * @return The configuration that is the result of resolving the given
     *         root modules
     *
     * @throws FindException
     *         If resolution fails for any of observability-related reasons
     *         specified above
     * @throws ResolutionException
     *         If resolution fails for any of the consistency checks specified
     *         above
     * @throws IllegalArgumentException
     *         If the list of parents is empty, or the list has two or more
     *         parents with modules for different target operating systems,
     *         architectures, or versions
     *
     * @throws SecurityException
     *         If locating a module is denied by the security manager
     */
    public static Configuration resolve(ModuleFinder before,
                                        List<Configuration> parents,
                                        ModuleFinder after,
                                        Collection<String> roots)
    {
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        Objects.requireNonNull(roots);

        List<Configuration> parentList = new ArrayList<>(parents);
        if (parentList.isEmpty())
            throw new IllegalArgumentException("'parents' is empty");

        Resolver resolver = new Resolver(before, parentList, after, null);
        resolver.resolve(roots);

        return new Configuration(parentList, resolver);
    }

    /**
     * Resolves a collection of root modules, with service binding, to create
     * configuration.
     *
     * <p> This method works exactly as specified by {@link
     * #resolve(ModuleFinder,List,ModuleFinder,Collection)
     * resolve} except that the graph of resolved modules is augmented
     * with modules induced by the service-use dependence relation. </p>
     *
     * <p><a id="service-binding"></a>More specifically, the root modules are
     * resolved as if by calling {@code resolve}. The resolved modules, and
     * all modules in the parent configurations, with {@link ModuleDescriptor#uses()
     * service dependences} are then examined. All modules found by the given
     * module finders that {@link ModuleDescriptor#provides() provide} an
     * implementation of one or more of the service types are added to the
     * module graph and then resolved as if by calling the {@code
     * resolve} method. Adding modules to the module graph may introduce new
     * service-use dependences and so the process works iteratively until no
     * more modules are added. </p>
     *
     * <p> As service binding involves resolution then it may fail with {@code
     * FindException} or {@code ResolutionException} for exactly the same
     * reasons specified in {@code resolve}. </p>
     *
     * @param  before
     *         The <em>before</em> module finder to find modules
     * @param  parents
     *         The list parent configurations in search order
     * @param  after
     *         The <em>after</em> module finder to locate modules when not
     *         located by the {@code before} module finder or in parent
     *         configurations
     * @param  roots
     *         The possibly-empty collection of module names of the modules
     *         to resolve
     *
     * @return The configuration that is the result of resolving, with service
     *         binding, the given root modules
     *
     * @throws FindException
     *         If resolution fails for any of the observability-related reasons
     *         specified by the static {@code resolve} method
     * @throws ResolutionException
     *         If resolution fails any of the consistency checks specified by
     *         the static {@code resolve} method
     * @throws IllegalArgumentException
     *         If the list of parents is empty, or the list has two or more
     *         parents with modules for different target operating systems,
     *         architectures, or versions
     * @throws SecurityException
     *         If locating a module is denied by the security manager
     */
    public static Configuration resolveAndBind(ModuleFinder before,
                                               List<Configuration> parents,
                                               ModuleFinder after,
                                               Collection<String> roots)
    {
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        Objects.requireNonNull(roots);

        List<Configuration> parentList = new ArrayList<>(parents);
        if (parentList.isEmpty())
            throw new IllegalArgumentException("'parents' is empty");

        Resolver resolver = new Resolver(before, parentList, after, null);
        resolver.resolve(roots).bind();

        return new Configuration(parentList, resolver);
    }


    /**
     * Returns the <em>empty</em> configuration. There are no modules in the
     * empty configuration. It has no parents.
     *
     * @return The empty configuration
     */
    public static Configuration empty() {
        return EMPTY_CONFIGURATION;
    }


    /**
     * Returns an unmodifiable list of this configuration's parents, in search
     * order. If this is the {@linkplain #empty() empty configuration} then an
     * empty list is returned.
     *
     * @return A possibly-empty unmodifiable list of this parent configurations
     */
    public List<Configuration> parents() {
        return parents;
    }


    /**
     * Returns an unmodifiable set of the resolved modules in this configuration.
     *
     * @return A possibly-empty unmodifiable set of the resolved modules
     *         in this configuration
     */
    public Set<ResolvedModule> modules() {
        return modules;
    }


    /**
     * Finds a resolved module in this configuration, or if not in this
     * configuration, the {@linkplain #parents() parent} configurations.
     * Finding a module in parent configurations is equivalent to invoking
     * {@code findModule} on each parent, in search order, until the module
     * is found or all parents have been searched. In a <em>tree of
     * configurations</em> then this is equivalent to a depth-first search.
     *
     * @param  name
     *         The module name of the resolved module to find
     *
     * @return The resolved module with the given name or an empty {@code
     *         Optional} if there isn't a module with this name in this
     *         configuration or any parent configurations
     */
    public Optional<ResolvedModule> findModule(String name) {
        Objects.requireNonNull(name);
        ResolvedModule m = nameToModule.get(name);
        if (m != null)
            return Optional.of(m);

        if (!parents.isEmpty()) {
            return configurations()
                    .skip(1)  // skip this configuration
                    .map(cf -> cf.nameToModule.get(name))
                    .filter(Objects::nonNull)
                    .findFirst();
        }

        return Optional.empty();
    }


    Set<ModuleDescriptor> descriptors() {
        if (modules.isEmpty()) {
            return Set.of();
        } else {
            return modules.stream()
                    .map(ResolvedModule::reference)
                    .map(ModuleReference::descriptor)
                    .collect(Collectors.toSet());
        }
    }

    Set<ResolvedModule> reads(ResolvedModule m) {
        // The sets stored in the graph are already immutable sets
        return Set.copyOf(graph.get(m));
    }

    /**
     * Returns an ordered stream of configurations. The first element is this
     * configuration, the remaining elements are the parent configurations
     * in DFS order.
     *
     * @implNote For now, the assumption is that the number of elements will
     * be very low and so this method does not use a specialized spliterator.
     */
    Stream<Configuration> configurations() {
        List<Configuration> allConfigurations = this.allConfigurations;
        if (allConfigurations == null) {
            allConfigurations = new ArrayList<>();
            Set<Configuration> visited = new HashSet<>();
            Deque<Configuration> stack = new ArrayDeque<>();
            visited.add(this);
            stack.push(this);
            while (!stack.isEmpty()) {
                Configuration layer = stack.pop();
                allConfigurations.add(layer);

                // push in reverse order
                for (int i = layer.parents.size() - 1; i >= 0; i--) {
                    Configuration parent = layer.parents.get(i);
                    if (visited.add(parent)) {
                        stack.push(parent);
                    }
                }
            }
            this.allConfigurations = allConfigurations; // no need to do defensive copy
        }
        return allConfigurations.stream();
    }

    private volatile List<Configuration> allConfigurations;


    /**
     * Returns a string describing this configuration.
     *
     * @return A possibly empty string describing this configuration
     */
    @Override
    public String toString() {
        return modules().stream()
                .map(ResolvedModule::name)
                .collect(Collectors.joining(", "));
    }
}
