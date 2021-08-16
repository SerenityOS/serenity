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
package jdk.tools.jlink.internal.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jdk.internal.module.Checks;
import jdk.internal.module.DefaultRoots;
import jdk.internal.module.Modules;
import jdk.internal.module.ModuleHashes;
import jdk.internal.module.ModuleInfo.Attributes;
import jdk.internal.module.ModuleInfoExtender;
import jdk.internal.module.ModuleReferenceImpl;
import jdk.internal.module.ModuleResolution;
import jdk.internal.module.ModuleTarget;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import jdk.tools.jlink.internal.ModuleSorter;
import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;

/**
 * Jlink plugin to reconstitute module descriptors and other attributes for system
 * modules. The plugin generates implementations of SystemModules to avoid parsing
 * module-info.class files at startup. It also generates SystemModulesMap to return
 * the SystemModules implementation for a specific initial module.
 *
 * As a side effect, the plugin adds the ModulePackages class file attribute to the
 * module-info.class files that don't have the attribute.
 *
 * @see jdk.internal.module.SystemModuleFinders
 * @see jdk.internal.module.SystemModules
 */

public final class SystemModulesPlugin extends AbstractPlugin {
    private static final String SYSTEM_MODULES_MAP_CLASS =
            "jdk/internal/module/SystemModulesMap";
    private static final String SYSTEM_MODULES_CLASS_PREFIX =
            "jdk/internal/module/SystemModules$";
    private static final String ALL_SYSTEM_MODULES_CLASS =
            SYSTEM_MODULES_CLASS_PREFIX + "all";
    private static final String DEFAULT_SYSTEM_MODULES_CLASS =
            SYSTEM_MODULES_CLASS_PREFIX + "default";

    private boolean enabled;

    public SystemModulesPlugin() {
        super("system-modules");
        this.enabled = true;
    }

    @Override
    public Set<State> getState() {
        return enabled ? EnumSet.of(State.AUTO_ENABLED, State.FUNCTIONAL)
                       : EnumSet.of(State.DISABLED);
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public void configure(Map<String, String> config) {
        String arg = config.get(getName());
        if (arg != null) {
            throw new IllegalArgumentException(getName() + ": " + arg);
        }
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        if (!enabled) {
            throw new PluginException(getName() + " was set");
        }

        // validate, transform (if needed), and add the module-info.class files
        List<ModuleInfo> moduleInfos = transformModuleInfos(in, out);

        // generate and add the SystemModuleMap and SystemModules classes
        Set<String> generated = genSystemModulesClasses(moduleInfos, out);

        // pass through all other resources
        in.entries()
            .filter(data -> !data.path().endsWith("/module-info.class")
                    && !generated.contains(data.path()))
            .forEach(data -> out.add(data));

        return out.build();
    }

    /**
     * Validates and transforms the module-info.class files in the modules, adding
     * the ModulePackages class file attribute if needed.
     *
     * @return the list of ModuleInfo objects, the first element is java.base
     */
    List<ModuleInfo> transformModuleInfos(ResourcePool in, ResourcePoolBuilder out) {
        List<ModuleInfo> moduleInfos = new ArrayList<>();

        // Sort modules in the topological order so that java.base is always first.
        new ModuleSorter(in.moduleView()).sorted().forEach(module -> {
            ResourcePoolEntry data = module.findEntry("module-info.class").orElseThrow(
                // automatic modules not supported
                () ->  new PluginException("module-info.class not found for " +
                        module.name() + " module")
            );

            assert module.name().equals(data.moduleName());

            try {
                byte[] content = data.contentBytes();
                Set<String> packages = module.packages();
                ModuleInfo moduleInfo = new ModuleInfo(content, packages);

                // link-time validation
                moduleInfo.validateNames();

                // check if any exported or open package is not present
                moduleInfo.validatePackages();

                // module-info.class may be overridden to add ModulePackages
                if (moduleInfo.shouldRewrite()) {
                    data = data.copyWithContent(moduleInfo.getBytes());
                }
                moduleInfos.add(moduleInfo);

                // add resource pool entry
                out.add(data);
            } catch (IOException e) {
                throw new PluginException(e);
            }
        });

        return moduleInfos;
    }

    /**
     * Generates the SystemModules classes (at least one) and the SystemModulesMap
     * class to map initial modules to a SystemModules class.
     *
     * @return the resource names of the resources added to the pool
     */
    private Set<String> genSystemModulesClasses(List<ModuleInfo> moduleInfos,
                                                ResourcePoolBuilder out) {
        int moduleCount = moduleInfos.size();
        ModuleFinder finder = finderOf(moduleInfos);
        assert finder.findAll().size() == moduleCount;

        // map of initial module name to SystemModules class name
        Map<String, String> map = new LinkedHashMap<>();

        // the names of resources written to the pool
        Set<String> generated = new HashSet<>();

        // generate the SystemModules implementation to reconstitute all modules
        Set<String> allModuleNames = moduleInfos.stream()
                .map(ModuleInfo::moduleName)
                .collect(Collectors.toSet());
        String rn = genSystemModulesClass(moduleInfos,
                                          resolve(finder, allModuleNames),
                                          ALL_SYSTEM_MODULES_CLASS,
                                          out);
        generated.add(rn);

        // generate, if needed, a SystemModules class to reconstitute the modules
        // needed for the case that the initial module is the unnamed module.
        String defaultSystemModulesClassName;
        Configuration cf = resolve(finder, DefaultRoots.compute(finder));
        if (cf.modules().size() == moduleCount) {
            // all modules are resolved so no need to generate a class
            defaultSystemModulesClassName = ALL_SYSTEM_MODULES_CLASS;
        } else {
            defaultSystemModulesClassName = DEFAULT_SYSTEM_MODULES_CLASS;
            rn = genSystemModulesClass(sublist(moduleInfos, cf),
                                       cf,
                                       defaultSystemModulesClassName,
                                       out);
            generated.add(rn);
        }

        // Generate a SystemModules class for each module with a main class
        int suffix = 0;
        for (ModuleInfo mi : moduleInfos) {
            if (mi.descriptor().mainClass().isPresent()) {
                String moduleName = mi.moduleName();
                cf = resolve(finder, Set.of(moduleName));
                if (cf.modules().size() == moduleCount) {
                    // resolves all modules so no need to generate a class
                    map.put(moduleName, ALL_SYSTEM_MODULES_CLASS);
                } else {
                    String cn = SYSTEM_MODULES_CLASS_PREFIX + (suffix++);
                    rn = genSystemModulesClass(sublist(moduleInfos, cf), cf, cn, out);
                    map.put(moduleName, cn);
                    generated.add(rn);
                }
            }
        }

        // generate SystemModulesMap
        rn = genSystemModulesMapClass(ALL_SYSTEM_MODULES_CLASS,
                                      defaultSystemModulesClassName,
                                      map,
                                      out);
        generated.add(rn);

        // return the resource names of the generated classes
        return generated;
    }

    /**
     * Resolves a collection of root modules, with service binding, to create
     * a Configuration for the boot layer.
     */
    private Configuration resolve(ModuleFinder finder, Set<String> roots) {
        return Modules.newBootLayerConfiguration(finder, roots, null);
    }

    /**
     * Returns the list of ModuleInfo objects that correspond to the modules in
     * the given configuration.
     */
    private List<ModuleInfo> sublist(List<ModuleInfo> moduleInfos, Configuration cf) {
        Set<String> names = cf.modules()
                .stream()
                .map(ResolvedModule::name)
                .collect(Collectors.toSet());
        return moduleInfos.stream()
                .filter(mi -> names.contains(mi.moduleName()))
                .toList();
    }

    /**
     * Generate a SystemModules implementation class and add it as a resource.
     *
     * @return the name of the class resource added to the pool
     */
    private String genSystemModulesClass(List<ModuleInfo> moduleInfos,
                                         Configuration cf,
                                         String className,
                                         ResourcePoolBuilder out) {
        SystemModulesClassGenerator generator
            = new SystemModulesClassGenerator(className, moduleInfos);
        byte[] bytes = generator.getClassWriter(cf).toByteArray();
        String rn = "/java.base/" + className + ".class";
        ResourcePoolEntry e = ResourcePoolEntry.create(rn, bytes);
        out.add(e);
        return rn;
    }

    static class ModuleInfo {
        private final ByteArrayInputStream bais;
        private final Attributes attrs;
        private final Set<String> packages;
        private final boolean addModulePackages;
        private ModuleDescriptor descriptor;  // may be different that the original one

        ModuleInfo(byte[] bytes, Set<String> packages) throws IOException {
            this.bais = new ByteArrayInputStream(bytes);
            this.packages = packages;
            this.attrs = jdk.internal.module.ModuleInfo.read(bais, null);

            // If ModulePackages attribute is present, the packages from this
            // module descriptor returns the packages in that attribute.
            // If it's not present, ModuleDescriptor::packages only contains
            // the exported and open packages from module-info.class
            this.descriptor = attrs.descriptor();
            if (descriptor.isAutomatic()) {
                throw new InternalError("linking automatic module is not supported");
            }

            // add ModulePackages attribute if this module contains some packages
            // and ModulePackages is not present
            this.addModulePackages = packages.size() > 0 && !hasModulePackages();
        }

        String moduleName() {
            return attrs.descriptor().name();
        }

        ModuleDescriptor descriptor() {
            return descriptor;
        }

        Set<String> packages() {
            return packages;
        }

        ModuleTarget target() {
            return attrs.target();
        }

        ModuleHashes recordedHashes() {
            return attrs.recordedHashes();
        }

        ModuleResolution moduleResolution() {
            return attrs.moduleResolution();
        }

        /**
         * Validates names in ModuleDescriptor
         */
        void validateNames() {
            Checks.requireModuleName(descriptor.name());
            for (Requires req : descriptor.requires()) {
                Checks.requireModuleName(req.name());
            }
            for (Exports e : descriptor.exports()) {
                Checks.requirePackageName(e.source());
                if (e.isQualified())
                    e.targets().forEach(Checks::requireModuleName);
            }
            for (Opens opens : descriptor.opens()) {
                Checks.requirePackageName(opens.source());
                if (opens.isQualified())
                    opens.targets().forEach(Checks::requireModuleName);
            }
            for (Provides provides : descriptor.provides()) {
                Checks.requireServiceTypeName(provides.service());
                provides.providers().forEach(Checks::requireServiceProviderName);
            }
            for (String service : descriptor.uses()) {
                Checks.requireServiceTypeName(service);
            }
            for (String pn : descriptor.packages()) {
                Checks.requirePackageName(pn);
            }
            for (String pn : packages) {
                Checks.requirePackageName(pn);
            }
        }

        /**
         * Validates if exported and open packages are present
         */
        void validatePackages() {
            Set<String> nonExistPackages = new TreeSet<>();
            descriptor.exports().stream()
                .map(Exports::source)
                .filter(pn -> !packages.contains(pn))
                .forEach(nonExistPackages::add);

            descriptor.opens().stream()
                .map(Opens::source)
                .filter(pn -> !packages.contains(pn))
                .forEach(nonExistPackages::add);

            if (!nonExistPackages.isEmpty()) {
                throw new PluginException("Packages that are exported or open in "
                    + descriptor.name() + " are not present: " + nonExistPackages);
            }
        }

        boolean hasModulePackages() throws IOException {
            Set<String> packages = new HashSet<>();
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM7) {
                @Override
                public ModuleVisitor visitModule(String name,
                                                 int flags,
                                                 String version) {
                    return new ModuleVisitor(Opcodes.ASM7) {
                        @Override
                        public void visitPackage(String pn) {
                            packages.add(pn);
                        }
                    };
                }
            };

            try (InputStream in = getInputStream()) {
                // parse module-info.class
                ClassReader cr = new ClassReader(in);
                cr.accept(cv, 0);
                return packages.size() > 0;
            }
        }

        /**
         * Returns true if module-info.class should be rewritten to add the
         * ModulePackages attribute.
         */
        boolean shouldRewrite() {
            return addModulePackages;
        }

        /**
         * Returns the bytes for the (possibly updated) module-info.class.
         */
        byte[] getBytes() throws IOException {
            try (InputStream in = getInputStream()) {
                if (shouldRewrite()) {
                    ModuleInfoRewriter rewriter = new ModuleInfoRewriter(in);
                    if (addModulePackages) {
                        rewriter.addModulePackages(packages);
                    }
                    // rewritten module descriptor
                    byte[] bytes = rewriter.getBytes();
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                        this.descriptor = ModuleDescriptor.read(bais);
                    }
                    return bytes;
                } else {
                    return in.readAllBytes();
                }
            }
        }

        /*
         * Returns the input stream of the module-info.class
         */
        InputStream getInputStream() {
            bais.reset();
            return bais;
        }

        class ModuleInfoRewriter extends ByteArrayOutputStream {
            final ModuleInfoExtender extender;
            ModuleInfoRewriter(InputStream in) {
                this.extender = ModuleInfoExtender.newExtender(in);
            }

            void addModulePackages(Set<String> packages) {
                // Add ModulePackages attribute
                if (packages.size() > 0) {
                    extender.packages(packages);
                }
            }

            byte[] getBytes() throws IOException {
                extender.write(this);
                return buf;
            }
        }
    }

    /**
     * Generates a SystemModules class to reconstitute the ModuleDescriptor
     * and other attributes of system modules.
     */
    static class SystemModulesClassGenerator {
        private static final String MODULE_DESCRIPTOR_BUILDER =
            "jdk/internal/module/Builder";
        private static final String MODULE_DESCRIPTOR_ARRAY_SIGNATURE =
            "[Ljava/lang/module/ModuleDescriptor;";
        private static final String REQUIRES_MODIFIER_CLASSNAME =
            "java/lang/module/ModuleDescriptor$Requires$Modifier";
        private static final String EXPORTS_MODIFIER_CLASSNAME =
            "java/lang/module/ModuleDescriptor$Exports$Modifier";
        private static final String OPENS_MODIFIER_CLASSNAME =
            "java/lang/module/ModuleDescriptor$Opens$Modifier";
        private static final String MODULE_TARGET_CLASSNAME  =
            "jdk/internal/module/ModuleTarget";
        private static final String MODULE_TARGET_ARRAY_SIGNATURE  =
            "[Ljdk/internal/module/ModuleTarget;";
        private static final String MODULE_HASHES_ARRAY_SIGNATURE  =
            "[Ljdk/internal/module/ModuleHashes;";
        private static final String MODULE_RESOLUTION_CLASSNAME  =
            "jdk/internal/module/ModuleResolution";
        private static final String MODULE_RESOLUTIONS_ARRAY_SIGNATURE  =
            "[Ljdk/internal/module/ModuleResolution;";

        private static final int MAX_LOCAL_VARS = 256;

        private final int BUILDER_VAR    = 0;
        private final int MD_VAR         = 1;  // variable for ModuleDescriptor
        private final int MT_VAR         = 1;  // variable for ModuleTarget
        private final int MH_VAR         = 1;  // variable for ModuleHashes
        private int nextLocalVar         = 2;  // index to next local variable

        // Method visitor for generating the SystemModules::modules() method
        private MethodVisitor mv;

        // name of class to generate
        private final String className;

        // list of all ModuleDescriptorBuilders, invoked in turn when building.
        private final List<ModuleInfo> moduleInfos;

        // A builder to create one single Set instance for a given set of
        // names or modifiers to reduce the footprint
        // e.g. target modules of qualified exports
        private final DedupSetBuilder dedupSetBuilder
            = new DedupSetBuilder(this::getNextLocalVar);

        public SystemModulesClassGenerator(String className,
                                           List<ModuleInfo> moduleInfos) {
            this.className = className;
            this.moduleInfos = moduleInfos;
            moduleInfos.forEach(mi -> dedups(mi.descriptor()));
        }

        private int getNextLocalVar() {
            return nextLocalVar++;
        }

        /*
         * Adds the given ModuleDescriptor to the system module list.
         * It performs link-time validation and prepares mapping from various
         * Sets to SetBuilders to emit an optimized number of sets during build.
         */
        private void dedups(ModuleDescriptor md) {
            // exports
            for (Exports e : md.exports()) {
                dedupSetBuilder.stringSet(e.targets());
                dedupSetBuilder.exportsModifiers(e.modifiers());
            }

            // opens
            for (Opens opens : md.opens()) {
                dedupSetBuilder.stringSet(opens.targets());
                dedupSetBuilder.opensModifiers(opens.modifiers());
            }

            // requires
            for (Requires r : md.requires()) {
                dedupSetBuilder.requiresModifiers(r.modifiers());
            }

            // uses
            dedupSetBuilder.stringSet(md.uses());
        }

        /**
         * Generate SystemModules class
         */
        public ClassWriter getClassWriter(Configuration cf) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                             + ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8,
                     ACC_FINAL+ACC_SUPER,
                     className,
                     null,
                     "java/lang/Object",
                     new String[] { "jdk/internal/module/SystemModules" });

            // generate <init>
            genConstructor(cw);

            // generate hasSplitPackages
            genHasSplitPackages(cw);

            // generate hasIncubatorModules
            genIncubatorModules(cw);

            // generate moduleDescriptors
            genModuleDescriptorsMethod(cw);

            // generate moduleTargets
            genModuleTargetsMethod(cw);

            // generate moduleHashes
            genModuleHashesMethod(cw);

            // generate moduleResolutions
            genModuleResolutionsMethod(cw);

            // generate moduleReads
            genModuleReads(cw, cf);

            return cw;
        }

        /**
         * Generate byteccode for no-arg constructor
         */
        private void genConstructor(ClassWriter cw) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL,
                               "java/lang/Object",
                               "<init>",
                               "()V",
                               false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /**
         * Generate bytecode for hasSplitPackages method
         */
        private void genHasSplitPackages(ClassWriter cw) {
            boolean distinct = moduleInfos.stream()
                    .map(ModuleInfo::packages)
                    .flatMap(Set::stream)
                    .allMatch(new HashSet<>()::add);
            boolean hasSplitPackages = !distinct;

            mv = cw.visitMethod(ACC_PUBLIC,
                                "hasSplitPackages",
                                "()Z",
                                "()Z",
                                null);
            mv.visitCode();
            if (hasSplitPackages) {
                mv.visitInsn(ICONST_1);
            } else {
                mv.visitInsn(ICONST_0);
            }
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /**
         * Generate bytecode for hasIncubatorModules method
         */
        private void genIncubatorModules(ClassWriter cw) {
            boolean hasIncubatorModules = moduleInfos.stream()
                    .map(ModuleInfo::moduleResolution)
                    .filter(mres -> (mres != null && mres.hasIncubatingWarning()))
                    .findFirst()
                    .isPresent();

            mv = cw.visitMethod(ACC_PUBLIC,
                                "hasIncubatorModules",
                                "()Z",
                                "()Z",
                                null);
            mv.visitCode();
            if (hasIncubatorModules) {
                mv.visitInsn(ICONST_1);
            } else {
                mv.visitInsn(ICONST_0);
            }
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /**
         * Generate bytecode for moduleDescriptors method
         */
        private void genModuleDescriptorsMethod(ClassWriter cw) {
            this.mv = cw.visitMethod(ACC_PUBLIC,
                                     "moduleDescriptors",
                                     "()" + MODULE_DESCRIPTOR_ARRAY_SIGNATURE,
                                     "()" + MODULE_DESCRIPTOR_ARRAY_SIGNATURE,
                                     null);
            mv.visitCode();
            pushInt(mv, moduleInfos.size());
            mv.visitTypeInsn(ANEWARRAY, "java/lang/module/ModuleDescriptor");
            mv.visitVarInsn(ASTORE, MD_VAR);

            for (int index = 0; index < moduleInfos.size(); index++) {
                ModuleInfo minfo = moduleInfos.get(index);
                new ModuleDescriptorBuilder(minfo.descriptor(),
                                            minfo.packages(),
                                            index).build();
            }
            mv.visitVarInsn(ALOAD, MD_VAR);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /**
         * Generate bytecode for moduleTargets method
         */
        private void genModuleTargetsMethod(ClassWriter cw) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                                              "moduleTargets",
                                              "()" + MODULE_TARGET_ARRAY_SIGNATURE,
                                              "()" + MODULE_TARGET_ARRAY_SIGNATURE,
                                              null);
            mv.visitCode();
            pushInt(mv, moduleInfos.size());
            mv.visitTypeInsn(ANEWARRAY, MODULE_TARGET_CLASSNAME);
            mv.visitVarInsn(ASTORE, MT_VAR);


            // if java.base has a ModuleTarget attribute then generate the array
            // with one element, all other elements will be null.

            ModuleInfo base = moduleInfos.get(0);
            if (!base.moduleName().equals("java.base"))
                throw new InternalError("java.base should be first module in list");
            ModuleTarget target = base.target();

            int count;
            if (target != null && target.targetPlatform() != null) {
                count = 1;
            } else {
                count = moduleInfos.size();
            }

            for (int index = 0; index < count; index++) {
                ModuleInfo minfo = moduleInfos.get(index);
                if (minfo.target() != null) {
                    mv.visitVarInsn(ALOAD, MT_VAR);
                    pushInt(mv, index);

                    // new ModuleTarget(String)
                    mv.visitTypeInsn(NEW, MODULE_TARGET_CLASSNAME);
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(minfo.target().targetPlatform());
                    mv.visitMethodInsn(INVOKESPECIAL, MODULE_TARGET_CLASSNAME,
                                       "<init>", "(Ljava/lang/String;)V", false);

                    mv.visitInsn(AASTORE);
                }
            }

            mv.visitVarInsn(ALOAD, MT_VAR);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /**
         * Generate bytecode for moduleHashes method
         */
        private void genModuleHashesMethod(ClassWriter cw) {
            MethodVisitor hmv =
                cw.visitMethod(ACC_PUBLIC,
                               "moduleHashes",
                               "()" + MODULE_HASHES_ARRAY_SIGNATURE,
                               "()" + MODULE_HASHES_ARRAY_SIGNATURE,
                               null);
            hmv.visitCode();
            pushInt(hmv, moduleInfos.size());
            hmv.visitTypeInsn(ANEWARRAY, "jdk/internal/module/ModuleHashes");
            hmv.visitVarInsn(ASTORE, MH_VAR);

            for (int index = 0; index < moduleInfos.size(); index++) {
                ModuleInfo minfo = moduleInfos.get(index);
                if (minfo.recordedHashes() != null) {
                    new ModuleHashesBuilder(minfo.recordedHashes(),
                                            index,
                                            hmv).build();
                }
            }

            hmv.visitVarInsn(ALOAD, MH_VAR);
            hmv.visitInsn(ARETURN);
            hmv.visitMaxs(0, 0);
            hmv.visitEnd();
        }

        /**
         * Generate bytecode for moduleResolutions method
         */
        private void genModuleResolutionsMethod(ClassWriter cw) {
            MethodVisitor mresmv =
                cw.visitMethod(ACC_PUBLIC,
                               "moduleResolutions",
                               "()" + MODULE_RESOLUTIONS_ARRAY_SIGNATURE,
                               "()" + MODULE_RESOLUTIONS_ARRAY_SIGNATURE,
                               null);
            mresmv.visitCode();
            pushInt(mresmv, moduleInfos.size());
            mresmv.visitTypeInsn(ANEWARRAY, MODULE_RESOLUTION_CLASSNAME);
            mresmv.visitVarInsn(ASTORE, 0);

            for (int index=0; index < moduleInfos.size(); index++) {
                ModuleInfo minfo = moduleInfos.get(index);
                if (minfo.moduleResolution() != null) {
                    mresmv.visitVarInsn(ALOAD, 0);
                    pushInt(mresmv, index);
                    mresmv.visitTypeInsn(NEW, MODULE_RESOLUTION_CLASSNAME);
                    mresmv.visitInsn(DUP);
                    mresmv.visitLdcInsn(minfo.moduleResolution().value());
                    mresmv.visitMethodInsn(INVOKESPECIAL,
                                           MODULE_RESOLUTION_CLASSNAME,
                                           "<init>",
                                           "(I)V", false);
                    mresmv.visitInsn(AASTORE);
                }
            }
            mresmv.visitVarInsn(ALOAD, 0);
            mresmv.visitInsn(ARETURN);
            mresmv.visitMaxs(0, 0);
            mresmv.visitEnd();
        }

        /**
         * Generate bytecode for moduleReads method
         */
        private void genModuleReads(ClassWriter cw, Configuration cf) {
            // module name -> names of modules that it reads
            Map<String, Set<String>> map = cf.modules().stream()
                    .collect(Collectors.toMap(
                            ResolvedModule::name,
                            m -> m.reads().stream()
                                    .map(ResolvedModule::name)
                                    .collect(Collectors.toSet())));
            generate(cw, "moduleReads", map, true);
        }

        /**
         * Generate method to return {@code Map<String, Set<String>>}.
         *
         * If {@code dedup} is true then the values are de-duplicated.
         */
        private void generate(ClassWriter cw,
                              String methodName,
                              Map<String, Set<String>> map,
                              boolean dedup) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                                              methodName,
                                              "()Ljava/util/Map;",
                                              "()Ljava/util/Map;",
                                              null);
            mv.visitCode();

            // map of Set -> local
            Map<Set<String>, Integer> locals;

            // generate code to create the sets that are duplicated
            if (dedup) {
                Collection<Set<String>> values = map.values();
                Set<Set<String>> duplicateSets = values.stream()
                        .distinct()
                        .filter(s -> Collections.frequency(values, s) > 1)
                        .collect(Collectors.toSet());
                locals = new HashMap<>();
                int index = 1;
                for (Set<String> s : duplicateSets) {
                    genImmutableSet(mv, s);
                    mv.visitVarInsn(ASTORE, index);
                    locals.put(s, index);
                    if (++index >= MAX_LOCAL_VARS) {
                        break;
                    }
                }
            } else {
                locals = Map.of();
            }

            // new Map$Entry[size]
            pushInt(mv, map.size());
            mv.visitTypeInsn(ANEWARRAY, "java/util/Map$Entry");

            int index = 0;
            for (var e : new TreeMap<>(map).entrySet()) {
                String name = e.getKey();
                Set<String> s = e.getValue();

                mv.visitInsn(DUP);
                pushInt(mv, index);
                mv.visitLdcInsn(name);

                // if de-duplicated then load the local, otherwise generate code
                Integer varIndex = locals.get(s);
                if (varIndex == null) {
                    genImmutableSet(mv, s);
                } else {
                    mv.visitVarInsn(ALOAD, varIndex);
                }

                String desc = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map$Entry;";
                mv.visitMethodInsn(INVOKESTATIC,
                                   "java/util/Map",
                                   "entry",
                                   desc,
                                   true);
                mv.visitInsn(AASTORE);
                index++;
            }

            // invoke Map.ofEntries(Map$Entry[])
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Map", "ofEntries",
                    "([Ljava/util/Map$Entry;)Ljava/util/Map;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /**
         * Generate code to generate an immutable set.
         */
        private void genImmutableSet(MethodVisitor mv, Set<String> set) {
            int size = set.size();

            // use Set.of(Object[]) when there are more than 2 elements
            // use Set.of(Object) or Set.of(Object, Object) when fewer
            if (size > 2) {
                pushInt(mv, size);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
                int i = 0;
                for (String element : sorted(set)) {
                    mv.visitInsn(DUP);
                    pushInt(mv, i);
                    mv.visitLdcInsn(element);
                    mv.visitInsn(AASTORE);
                    i++;
                }
                mv.visitMethodInsn(INVOKESTATIC,
                        "java/util/Set",
                        "of",
                        "([Ljava/lang/Object;)Ljava/util/Set;",
                        true);
            } else {
                StringBuilder sb = new StringBuilder("(");
                for (String element : sorted(set)) {
                    mv.visitLdcInsn(element);
                    sb.append("Ljava/lang/Object;");
                }
                sb.append(")Ljava/util/Set;");
                mv.visitMethodInsn(INVOKESTATIC,
                        "java/util/Set",
                        "of",
                        sb.toString(),
                        true);
            }
        }

        class ModuleDescriptorBuilder {
            static final String BUILDER_TYPE = "Ljdk/internal/module/Builder;";
            static final String EXPORTS_TYPE =
                "Ljava/lang/module/ModuleDescriptor$Exports;";
            static final String OPENS_TYPE =
                "Ljava/lang/module/ModuleDescriptor$Opens;";
            static final String PROVIDES_TYPE =
                "Ljava/lang/module/ModuleDescriptor$Provides;";
            static final String REQUIRES_TYPE =
                "Ljava/lang/module/ModuleDescriptor$Requires;";

            // method signature for static Builder::newExports, newOpens,
            // newProvides, newRequires methods
            static final String EXPORTS_MODIFIER_SET_STRING_SET_SIG =
                "(Ljava/util/Set;Ljava/lang/String;Ljava/util/Set;)"
                    + EXPORTS_TYPE;
            static final String EXPORTS_MODIFIER_SET_STRING_SIG =
                "(Ljava/util/Set;Ljava/lang/String;)" + EXPORTS_TYPE;
            static final String OPENS_MODIFIER_SET_STRING_SET_SIG =
                "(Ljava/util/Set;Ljava/lang/String;Ljava/util/Set;)"
                    + OPENS_TYPE;
            static final String OPENS_MODIFIER_SET_STRING_SIG =
                "(Ljava/util/Set;Ljava/lang/String;)" + OPENS_TYPE;
            static final String PROVIDES_STRING_LIST_SIG =
                "(Ljava/lang/String;Ljava/util/List;)" + PROVIDES_TYPE;
            static final String REQUIRES_SET_STRING_SIG =
                "(Ljava/util/Set;Ljava/lang/String;)" + REQUIRES_TYPE;
            static final String REQUIRES_SET_STRING_STRING_SIG =
                "(Ljava/util/Set;Ljava/lang/String;Ljava/lang/String;)" + REQUIRES_TYPE;

            // method signature for Builder instance methods that
            // return this Builder instance
            static final String EXPORTS_ARRAY_SIG =
                "([" + EXPORTS_TYPE + ")" + BUILDER_TYPE;
            static final String OPENS_ARRAY_SIG =
                "([" + OPENS_TYPE + ")" + BUILDER_TYPE;
            static final String PROVIDES_ARRAY_SIG =
                "([" + PROVIDES_TYPE + ")" + BUILDER_TYPE;
            static final String REQUIRES_ARRAY_SIG =
                "([" + REQUIRES_TYPE + ")" + BUILDER_TYPE;
            static final String SET_SIG = "(Ljava/util/Set;)" + BUILDER_TYPE;
            static final String STRING_SIG = "(Ljava/lang/String;)" + BUILDER_TYPE;
            static final String BOOLEAN_SIG = "(Z)" + BUILDER_TYPE;

            final ModuleDescriptor md;
            final Set<String> packages;
            final int index;

            ModuleDescriptorBuilder(ModuleDescriptor md, Set<String> packages, int index) {
                if (md.isAutomatic()) {
                    throw new InternalError("linking automatic module is not supported");
                }
                this.md = md;
                this.packages = packages;
                this.index = index;
            }

            void build() {
                // new jdk.internal.module.Builder
                newBuilder();

                // requires
                requires(md.requires());

                // exports
                exports(md.exports());

                // opens
                opens(md.opens());

                // uses
                uses(md.uses());

                // provides
                provides(md.provides());

                // all packages
                packages(packages);

                // version
                md.version().ifPresent(this::version);

                // main class
                md.mainClass().ifPresent(this::mainClass);

                putModuleDescriptor();
            }

            void newBuilder() {
                mv.visitTypeInsn(NEW, MODULE_DESCRIPTOR_BUILDER);
                mv.visitInsn(DUP);
                mv.visitLdcInsn(md.name());
                mv.visitMethodInsn(INVOKESPECIAL, MODULE_DESCRIPTOR_BUILDER,
                    "<init>", "(Ljava/lang/String;)V", false);
                mv.visitVarInsn(ASTORE, BUILDER_VAR);
                mv.visitVarInsn(ALOAD, BUILDER_VAR);

                if (md.isOpen()) {
                    setModuleBit("open", true);
                }
                if (md.modifiers().contains(ModuleDescriptor.Modifier.SYNTHETIC)) {
                    setModuleBit("synthetic", true);
                }
                if (md.modifiers().contains(ModuleDescriptor.Modifier.MANDATED)) {
                    setModuleBit("mandated", true);
                }
            }

            /*
             * Invoke Builder.<methodName>(boolean value)
             */
            void setModuleBit(String methodName, boolean value) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                if (value) {
                    mv.visitInsn(ICONST_1);
                } else {
                    mv.visitInsn(ICONST_0);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    methodName, BOOLEAN_SIG, false);
                mv.visitInsn(POP);
            }

            /*
             * Put ModuleDescriptor into the modules array
             */
            void putModuleDescriptor() {
                mv.visitVarInsn(ALOAD, MD_VAR);
                pushInt(mv, index);
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                mv.visitLdcInsn(md.hashCode());
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "build", "(I)Ljava/lang/module/ModuleDescriptor;",
                    false);
                mv.visitInsn(AASTORE);
            }

            /*
             * Call Builder::newRequires to create Requires instances and
             * then pass it to the builder by calling:
             *      Builder.requires(Requires[])
             *
             */
            void requires(Set<Requires> requires) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                pushInt(mv, requires.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/module/ModuleDescriptor$Requires");
                int arrayIndex = 0;
                for (Requires require : sorted(requires)) {
                    String compiledVersion = null;
                    if (require.compiledVersion().isPresent()) {
                        compiledVersion = require.compiledVersion().get().toString();
                    }

                    mv.visitInsn(DUP);               // arrayref
                    pushInt(mv, arrayIndex++);
                    newRequires(require.modifiers(), require.name(), compiledVersion);
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "requires", REQUIRES_ARRAY_SIG, false);
            }

            /*
             * Invoke Builder.newRequires(Set<Modifier> mods, String mn, String compiledVersion)
             *
             * Set<Modifier> mods = ...
             * Builder.newRequires(mods, mn, compiledVersion);
             */
            void newRequires(Set<Requires.Modifier> mods, String name, String compiledVersion) {
                int varIndex = dedupSetBuilder.indexOfRequiresModifiers(mods);
                mv.visitVarInsn(ALOAD, varIndex);
                mv.visitLdcInsn(name);
                if (compiledVersion != null) {
                    mv.visitLdcInsn(compiledVersion);
                    mv.visitMethodInsn(INVOKESTATIC, MODULE_DESCRIPTOR_BUILDER,
                        "newRequires", REQUIRES_SET_STRING_STRING_SIG, false);
                } else {
                    mv.visitMethodInsn(INVOKESTATIC, MODULE_DESCRIPTOR_BUILDER,
                        "newRequires", REQUIRES_SET_STRING_SIG, false);
                }
            }

            /*
             * Call Builder::newExports to create Exports instances and
             * then pass it to the builder by calling:
             *      Builder.exports(Exports[])
             *
             */
            void exports(Set<Exports> exports) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                pushInt(mv, exports.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/module/ModuleDescriptor$Exports");
                int arrayIndex = 0;
                for (Exports export : sorted(exports)) {
                    mv.visitInsn(DUP);    // arrayref
                    pushInt(mv, arrayIndex++);
                    newExports(export.modifiers(), export.source(), export.targets());
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "exports", EXPORTS_ARRAY_SIG, false);
            }

            /*
             * Invoke
             *     Builder.newExports(Set<Exports.Modifier> ms, String pn,
             *                        Set<String> targets)
             * or
             *     Builder.newExports(Set<Exports.Modifier> ms, String pn)
             *
             * Set<String> targets = new HashSet<>();
             * targets.add(t);
             * :
             * :
             *
             * Set<Modifier> mods = ...
             * Builder.newExports(mods, pn, targets);
             */
            void newExports(Set<Exports.Modifier> ms, String pn, Set<String> targets) {
                int modifiersSetIndex = dedupSetBuilder.indexOfExportsModifiers(ms);
                if (!targets.isEmpty()) {
                    int stringSetIndex = dedupSetBuilder.indexOfStringSet(targets);
                    mv.visitVarInsn(ALOAD, modifiersSetIndex);
                    mv.visitLdcInsn(pn);
                    mv.visitVarInsn(ALOAD, stringSetIndex);
                    mv.visitMethodInsn(INVOKESTATIC, MODULE_DESCRIPTOR_BUILDER,
                        "newExports", EXPORTS_MODIFIER_SET_STRING_SET_SIG, false);
                } else {
                    mv.visitVarInsn(ALOAD, modifiersSetIndex);
                    mv.visitLdcInsn(pn);
                    mv.visitMethodInsn(INVOKESTATIC, MODULE_DESCRIPTOR_BUILDER,
                        "newExports", EXPORTS_MODIFIER_SET_STRING_SIG, false);
                }
            }


            /**
             * Call Builder::newOpens to create Opens instances and
             * then pass it to the builder by calling:
             * Builder.opens(Opens[])
             */
            void opens(Set<Opens> opens) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                pushInt(mv, opens.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/module/ModuleDescriptor$Opens");
                int arrayIndex = 0;
                for (Opens open : sorted(opens)) {
                    mv.visitInsn(DUP);    // arrayref
                    pushInt(mv, arrayIndex++);
                    newOpens(open.modifiers(), open.source(), open.targets());
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "opens", OPENS_ARRAY_SIG, false);
            }

            /*
             * Invoke
             *     Builder.newOpens(Set<Opens.Modifier> ms, String pn,
             *                        Set<String> targets)
             * or
             *     Builder.newOpens(Set<Opens.Modifier> ms, String pn)
             *
             * Set<String> targets = new HashSet<>();
             * targets.add(t);
             * :
             * :
             *
             * Set<Modifier> mods = ...
             * Builder.newOpens(mods, pn, targets);
             */
            void newOpens(Set<Opens.Modifier> ms, String pn, Set<String> targets) {
                int modifiersSetIndex = dedupSetBuilder.indexOfOpensModifiers(ms);
                if (!targets.isEmpty()) {
                    int stringSetIndex = dedupSetBuilder.indexOfStringSet(targets);
                    mv.visitVarInsn(ALOAD, modifiersSetIndex);
                    mv.visitLdcInsn(pn);
                    mv.visitVarInsn(ALOAD, stringSetIndex);
                    mv.visitMethodInsn(INVOKESTATIC, MODULE_DESCRIPTOR_BUILDER,
                        "newOpens", OPENS_MODIFIER_SET_STRING_SET_SIG, false);
                } else {
                    mv.visitVarInsn(ALOAD, modifiersSetIndex);
                    mv.visitLdcInsn(pn);
                    mv.visitMethodInsn(INVOKESTATIC, MODULE_DESCRIPTOR_BUILDER,
                        "newOpens", OPENS_MODIFIER_SET_STRING_SIG, false);
                }
            }

            /*
             * Invoke Builder.uses(Set<String> uses)
             */
            void uses(Set<String> uses) {
                int varIndex = dedupSetBuilder.indexOfStringSet(uses);
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                mv.visitVarInsn(ALOAD, varIndex);
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "uses", SET_SIG, false);
                mv.visitInsn(POP);
            }

            /*
            * Call Builder::newProvides to create Provides instances and
            * then pass it to the builder by calling:
            *      Builder.provides(Provides[] provides)
            *
            */
            void provides(Collection<Provides> provides) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                pushInt(mv, provides.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/module/ModuleDescriptor$Provides");
                int arrayIndex = 0;
                for (Provides provide : sorted(provides)) {
                    mv.visitInsn(DUP);    // arrayref
                    pushInt(mv, arrayIndex++);
                    newProvides(provide.service(), provide.providers());
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "provides", PROVIDES_ARRAY_SIG, false);
            }

            /*
             * Invoke Builder.newProvides(String service, Set<String> providers)
             *
             * Set<String> providers = new HashSet<>();
             * providers.add(impl);
             * :
             * :
             * Builder.newProvides(service, providers);
             */
            void newProvides(String service, List<String> providers) {
                mv.visitLdcInsn(service);
                pushInt(mv, providers.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
                int arrayIndex = 0;
                for (String provider : providers) {
                    mv.visitInsn(DUP);    // arrayref
                    pushInt(mv, arrayIndex++);
                    mv.visitLdcInsn(provider);
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKESTATIC, "java/util/List",
                    "of", "([Ljava/lang/Object;)Ljava/util/List;", true);
                mv.visitMethodInsn(INVOKESTATIC, MODULE_DESCRIPTOR_BUILDER,
                    "newProvides", PROVIDES_STRING_LIST_SIG, false);
            }

            /*
             * Invoke Builder.packages(String pn)
             */
            void packages(Set<String> packages) {
                int varIndex = dedupSetBuilder.newStringSet(packages);
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                mv.visitVarInsn(ALOAD, varIndex);
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "packages", SET_SIG, false);
                mv.visitInsn(POP);
            }

            /*
             * Invoke Builder.mainClass(String cn)
             */
            void mainClass(String cn) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                mv.visitLdcInsn(cn);
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "mainClass", STRING_SIG, false);
                mv.visitInsn(POP);
            }

            /*
             * Invoke Builder.version(Version v);
             */
            void version(Version v) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                mv.visitLdcInsn(v.toString());
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    "version", STRING_SIG, false);
                mv.visitInsn(POP);
            }

            void invokeBuilderMethod(String methodName, String value) {
                mv.visitVarInsn(ALOAD, BUILDER_VAR);
                mv.visitLdcInsn(value);
                mv.visitMethodInsn(INVOKEVIRTUAL, MODULE_DESCRIPTOR_BUILDER,
                    methodName, STRING_SIG, false);
                mv.visitInsn(POP);
            }
        }

        class ModuleHashesBuilder {
            private static final String MODULE_HASHES_BUILDER =
                "jdk/internal/module/ModuleHashes$Builder";
            private static final String MODULE_HASHES_BUILDER_TYPE =
                "L" + MODULE_HASHES_BUILDER + ";";
            static final String STRING_BYTE_ARRAY_SIG =
                "(Ljava/lang/String;[B)" + MODULE_HASHES_BUILDER_TYPE;

            final ModuleHashes recordedHashes;
            final MethodVisitor hmv;
            final int index;

            ModuleHashesBuilder(ModuleHashes hashes, int index, MethodVisitor hmv) {
                this.recordedHashes = hashes;
                this.hmv = hmv;
                this.index = index;
            }

            /**
             * Build ModuleHashes
             */
            void build() {
                if (recordedHashes == null)
                    return;

                // new jdk.internal.module.ModuleHashes.Builder
                newModuleHashesBuilder();

                // Invoke ModuleHashes.Builder::hashForModule
                recordedHashes
                    .names()
                    .stream()
                    .sorted()
                    .forEach(mn -> hashForModule(mn, recordedHashes.hashFor(mn)));

                // Put ModuleHashes into the hashes array
                pushModuleHashes();
            }


            /*
             * Create ModuleHashes.Builder instance
             */
            void newModuleHashesBuilder() {
                hmv.visitTypeInsn(NEW, MODULE_HASHES_BUILDER);
                hmv.visitInsn(DUP);
                hmv.visitLdcInsn(recordedHashes.algorithm());
                pushInt(hmv, ((4 * recordedHashes.names().size()) / 3) + 1);
                hmv.visitMethodInsn(INVOKESPECIAL, MODULE_HASHES_BUILDER,
                    "<init>", "(Ljava/lang/String;I)V", false);
                hmv.visitVarInsn(ASTORE, BUILDER_VAR);
                hmv.visitVarInsn(ALOAD, BUILDER_VAR);
            }


            /*
             * Invoke ModuleHashes.Builder::build and put the returned
             * ModuleHashes to the hashes array
             */
            void pushModuleHashes() {
                hmv.visitVarInsn(ALOAD, MH_VAR);
                pushInt(hmv, index);
                hmv.visitVarInsn(ALOAD, BUILDER_VAR);
                hmv.visitMethodInsn(INVOKEVIRTUAL, MODULE_HASHES_BUILDER,
                    "build", "()Ljdk/internal/module/ModuleHashes;",
                    false);
                hmv.visitInsn(AASTORE);
            }

            /*
             * Invoke ModuleHashes.Builder.hashForModule(String name, byte[] hash);
             */
            void hashForModule(String name, byte[] hash) {
                hmv.visitVarInsn(ALOAD, BUILDER_VAR);
                hmv.visitLdcInsn(name);

                pushInt(hmv, hash.length);
                hmv.visitIntInsn(NEWARRAY, T_BYTE);
                for (int i = 0; i < hash.length; i++) {
                    hmv.visitInsn(DUP);              // arrayref
                    pushInt(hmv, i);
                    hmv.visitIntInsn(BIPUSH, hash[i]);
                    hmv.visitInsn(BASTORE);
                }

                hmv.visitMethodInsn(INVOKEVIRTUAL, MODULE_HASHES_BUILDER,
                    "hashForModule", STRING_BYTE_ARRAY_SIG, false);
                hmv.visitInsn(POP);
            }
        }

        /*
         * Wraps set creation, ensuring identical sets are properly deduplicated.
         */
        class DedupSetBuilder {
            // map Set<String> to a specialized builder to allow them to be
            // deduplicated as they are requested
            final Map<Set<String>, SetBuilder<String>> stringSets = new HashMap<>();

            // map Set<Requires.Modifier> to a specialized builder to allow them to be
            // deduplicated as they are requested
            final Map<Set<Requires.Modifier>, EnumSetBuilder<Requires.Modifier>>
                requiresModifiersSets = new HashMap<>();

            // map Set<Exports.Modifier> to a specialized builder to allow them to be
            // deduplicated as they are requested
            final Map<Set<Exports.Modifier>, EnumSetBuilder<Exports.Modifier>>
                exportsModifiersSets = new HashMap<>();

            // map Set<Opens.Modifier> to a specialized builder to allow them to be
            // deduplicated as they are requested
            final Map<Set<Opens.Modifier>, EnumSetBuilder<Opens.Modifier>>
                opensModifiersSets = new HashMap<>();

            private final int stringSetVar;
            private final int enumSetVar;
            private final IntSupplier localVarSupplier;

            DedupSetBuilder(IntSupplier localVarSupplier) {
                this.stringSetVar = localVarSupplier.getAsInt();
                this.enumSetVar = localVarSupplier.getAsInt();
                this.localVarSupplier = localVarSupplier;
            }

            /*
             * Add the given set of strings to this builder.
             */
            void stringSet(Set<String> strings) {
                stringSets.computeIfAbsent(strings,
                    s -> new SetBuilder<>(s, stringSetVar, localVarSupplier)
                ).increment();
            }

            /*
             * Add the given set of Exports.Modifiers
             */
            void exportsModifiers(Set<Exports.Modifier> mods) {
                exportsModifiersSets.computeIfAbsent(mods, s ->
                                new EnumSetBuilder<>(s, EXPORTS_MODIFIER_CLASSNAME,
                                        enumSetVar, localVarSupplier)
                ).increment();
            }

            /*
             * Add the given set of Opens.Modifiers
             */
            void opensModifiers(Set<Opens.Modifier> mods) {
                opensModifiersSets.computeIfAbsent(mods, s ->
                                new EnumSetBuilder<>(s, OPENS_MODIFIER_CLASSNAME,
                                        enumSetVar, localVarSupplier)
                ).increment();
            }

            /*
             * Add the given set of Requires.Modifiers
             */
            void requiresModifiers(Set<Requires.Modifier> mods) {
                requiresModifiersSets.computeIfAbsent(mods, s ->
                    new EnumSetBuilder<>(s, REQUIRES_MODIFIER_CLASSNAME,
                                         enumSetVar, localVarSupplier)
                ).increment();
            }

            /*
             * Retrieve the index to the given set of Strings. Emit code to
             * generate it when SetBuilder::build is called.
             */
            int indexOfStringSet(Set<String> names) {
                return stringSets.get(names).build();
            }

            /*
             * Retrieve the index to the given set of Exports.Modifier.
             * Emit code to generate it when EnumSetBuilder::build is called.
             */
            int indexOfExportsModifiers(Set<Exports.Modifier> mods) {
                return exportsModifiersSets.get(mods).build();
            }

            /**
             * Retrieve the index to the given set of Opens.Modifier.
             * Emit code to generate it when EnumSetBuilder::build is called.
             */
            int indexOfOpensModifiers(Set<Opens.Modifier> mods) {
                return opensModifiersSets.get(mods).build();
            }


            /*
             * Retrieve the index to the given set of Requires.Modifier.
             * Emit code to generate it when EnumSetBuilder::build is called.
             */
            int indexOfRequiresModifiers(Set<Requires.Modifier> mods) {
                return requiresModifiersSets.get(mods).build();
            }

            /*
             * Build a new string set without any attempt to deduplicate it.
             */
            int newStringSet(Set<String> names) {
                int index = new SetBuilder<>(names, stringSetVar, localVarSupplier).build();
                assert index == stringSetVar;
                return index;
            }
        }

        /*
         * SetBuilder generates bytecode to create one single instance of Set
         * for a given set of elements and assign to a local variable slot.
         * When there is only one single reference to a Set<T>,
         * it will reuse defaultVarIndex.  For a Set with multiple references,
         * it will use a new local variable retrieved from the nextLocalVar
         */
        class SetBuilder<T extends Comparable<T>> {
            private final Set<T> elements;
            private final int defaultVarIndex;
            private final IntSupplier nextLocalVar;
            private int refCount;
            private int localVarIndex;

            SetBuilder(Set<T> elements,
                       int defaultVarIndex,
                       IntSupplier nextLocalVar) {
                this.elements = elements;
                this.defaultVarIndex = defaultVarIndex;
                this.nextLocalVar = nextLocalVar;
            }

            /*
             * Increments the number of references to this particular set.
             */
            final void increment() {
                refCount++;
            }

            /**
             * Generate the appropriate instructions to load an object reference
             * to the element onto the stack.
             */
            void visitElement(T element, MethodVisitor mv) {
                mv.visitLdcInsn(element);
            }

            /*
             * Build bytecode for the Set represented by this builder,
             * or get the local variable index of a previously generated set
             * (in the local scope).
             *
             * @return local variable index of the generated set.
             */
            final int build() {
                int index = localVarIndex;
                if (localVarIndex == 0) {
                    // if non-empty and more than one set reference this builder,
                    // emit to a unique local
                    index = refCount <= 1 ? defaultVarIndex
                                          : nextLocalVar.getAsInt();
                    if (index < MAX_LOCAL_VARS) {
                        localVarIndex = index;
                    } else {
                        // overflow: disable optimization by using localVarIndex = 0
                        index = defaultVarIndex;
                    }

                    generateSetOf(index);
                }
                return index;
            }

            private void generateSetOf(int index) {
                if (elements.size() <= 10) {
                    // call Set.of(e1, e2, ...)
                    StringBuilder sb = new StringBuilder("(");
                    for (T t : sorted(elements)) {
                        sb.append("Ljava/lang/Object;");
                        visitElement(t, mv);
                    }
                    sb.append(")Ljava/util/Set;");
                    mv.visitMethodInsn(INVOKESTATIC, "java/util/Set",
                            "of", sb.toString(), true);
                } else {
                    // call Set.of(E... elements)
                    pushInt(mv, elements.size());
                    mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
                    int arrayIndex = 0;
                    for (T t : sorted(elements)) {
                        mv.visitInsn(DUP);    // arrayref
                        pushInt(mv, arrayIndex);
                        visitElement(t, mv);  // value
                        mv.visitInsn(AASTORE);
                        arrayIndex++;
                    }
                    mv.visitMethodInsn(INVOKESTATIC, "java/util/Set",
                            "of", "([Ljava/lang/Object;)Ljava/util/Set;", true);
                }
                mv.visitVarInsn(ASTORE, index);
            }
        }

        /*
         * Generates bytecode to create one single instance of EnumSet
         * for a given set of modifiers and assign to a local variable slot.
         */
        class EnumSetBuilder<T extends Comparable<T>> extends SetBuilder<T> {

            private final String className;

            EnumSetBuilder(Set<T> modifiers, String className,
                           int defaultVarIndex,
                           IntSupplier nextLocalVar) {
                super(modifiers, defaultVarIndex, nextLocalVar);
                this.className = className;
            }

            /**
             * Loads an Enum field.
             */
            @Override
            void visitElement(T t, MethodVisitor mv) {
                mv.visitFieldInsn(GETSTATIC, className, t.toString(),
                                  "L" + className + ";");
            }
        }
    }

    /**
     * Generate SystemModulesMap and add it as a resource.
     *
     * @return the name of the class resource added to the pool
     */
    private String genSystemModulesMapClass(String allSystemModulesClassName,
                                            String defaultSystemModulesClassName,
                                            Map<String, String> map,
                                            ResourcePoolBuilder out) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8,
                 ACC_FINAL+ACC_SUPER,
                 SYSTEM_MODULES_MAP_CLASS,
                 null,
                 "java/lang/Object",
                 null);

        // <init>
        MethodVisitor mv = cw.visitMethod(0, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL,
                           "java/lang/Object",
                           "<init>",
                           "()V",
                           false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // allSystemModules()
        mv = cw.visitMethod(ACC_STATIC,
                            "allSystemModules",
                            "()Ljdk/internal/module/SystemModules;",
                            "()Ljdk/internal/module/SystemModules;",
                            null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, allSystemModulesClassName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL,
                           allSystemModulesClassName,
                           "<init>",
                           "()V",
                           false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // defaultSystemModules()
        mv = cw.visitMethod(ACC_STATIC,
                            "defaultSystemModules",
                            "()Ljdk/internal/module/SystemModules;",
                            "()Ljdk/internal/module/SystemModules;",
                            null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, defaultSystemModulesClassName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL,
                           defaultSystemModulesClassName,
                           "<init>",
                           "()V",
                           false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // moduleNames()
        mv = cw.visitMethod(ACC_STATIC,
                            "moduleNames",
                            "()[Ljava/lang/String;",
                            "()[Ljava/lang/String;",
                            null);
        mv.visitCode();
        pushInt(mv, map.size());
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

        int index = 0;
        for (String moduleName : sorted(map.keySet())) {
            mv.visitInsn(DUP);                  // arrayref
            pushInt(mv, index);
            mv.visitLdcInsn(moduleName);
            mv.visitInsn(AASTORE);
            index++;
        }

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // classNames()
        mv = cw.visitMethod(ACC_STATIC,
                            "classNames",
                            "()[Ljava/lang/String;",
                            "()[Ljava/lang/String;",
                            null);
        mv.visitCode();
        pushInt(mv, map.size());
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

        index = 0;
        for (String className : sorted(map.values())) {
            mv.visitInsn(DUP);                  // arrayref
            pushInt(mv, index);
            mv.visitLdcInsn(className.replace('/', '.'));
            mv.visitInsn(AASTORE);
            index++;
        }

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // write the class file to the pool as a resource
        String rn = "/java.base/" + SYSTEM_MODULES_MAP_CLASS + ".class";
        ResourcePoolEntry e = ResourcePoolEntry.create(rn, cw.toByteArray());
        out.add(e);

        return rn;
    }

    /**
     * Returns a sorted copy of a collection.
     *
     * This is useful to ensure a deterministic iteration order.
     *
     * @return a sorted copy of the given collection.
     */
    private static <T extends Comparable<T>> List<T> sorted(Collection<T> c) {
        var l = new ArrayList<T>(c);
        Collections.sort(l);
        return l;
    }

    /**
     * Pushes an int constant
     */
    private static void pushInt(MethodVisitor mv, int value) {
        if (value <= 5) {
            mv.visitInsn(ICONST_0 + value);
        } else if (value < Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, value);
        } else if (value < Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            throw new IllegalArgumentException("exceed limit: " + value);
        }
    }

    /**
     * Returns a module finder that finds all modules in the given list
     */
    private static ModuleFinder finderOf(Collection<ModuleInfo> moduleInfos) {
        Supplier<ModuleReader> readerSupplier = () -> null;
        Map<String, ModuleReference> namesToReference = new HashMap<>();
        for (ModuleInfo mi : moduleInfos) {
            String name = mi.moduleName();
            ModuleReference mref
                = new ModuleReferenceImpl(mi.descriptor(),
                                          URI.create("jrt:/" + name),
                                          readerSupplier,
                                          null,
                                          mi.target(),
                                          null,
                                          null,
                                          mi.moduleResolution());
            namesToReference.put(name, mref);
        }

        return new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                Objects.requireNonNull(name);
                return Optional.ofNullable(namesToReference.get(name));
            }
            @Override
            public Set<ModuleReference> findAll() {
                return new HashSet<>(namesToReference.values());
            }
        };
    }
}
