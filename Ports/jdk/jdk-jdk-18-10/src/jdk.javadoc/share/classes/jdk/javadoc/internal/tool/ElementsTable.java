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
package jdk.javadoc.internal.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCModuleDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.DocletEnvironment.ModuleMode;

import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;

import static javax.lang.model.util.Elements.Origin.*;
import static javax.tools.JavaFileObject.Kind.*;

import static jdk.javadoc.internal.tool.Main.Result.*;
import static jdk.javadoc.internal.tool.JavadocTool.isValidClassName;


/**
 * This class manages elements specified on the command line, and
 * produces "specified" and "included" data sets, needed by the
 * doclet environment, as well as querying an elements' visibility
 * or inclusion.
 *
 * A. Initialization phase: the class is initialized with the
 *    options table by the caller. Some program elements may not
 *    be specified via specific options, such as packages, classes,
 *    these are set with the use of setter methods, such setClassArgList
 *    and setClassDeclList.
 *
 * B. Scan and decode phase: this is performed by scanSpecifiedItems,
 *    to identify the modules specified on the command line, modules
 *    specified with qualified packages and qualified subpackages, the
 *    modules so identified are used to initialize the module system.
 *
 * C. Intermediate phase: before the final analysis can be done,
 *    intermediate methods can be used to get specified elements from
 *    the initialization phase, typically used to parse sources or packages
 *    specified on the command line.
 *
 * D. Analysis phase: the final analysis is performed to determine
 *    the packages that ought to be included, as follows:
 *
 *    1. computes the specified modules, by considering the option
 *       "expand-requires", this must be done exhaustively, as the package
 *       computation phase expects a completed module graph, in order to
 *       check the target of a qualified export is in the included set.
 *
 *    2. computes the packages that must be documented, by considering
 *       the option "show-packages", also if only exported packages are
 *       to be considered, then also check for qualified packages, and
 *       include only those packages whose target is in the included set.
 *
 *    3. compute the specified packages, as part of this, first compute
 *       the subpackages and exclude any packages, if required.
 *
 *    4. Finally, compute the types found by previous parsing steps,
 *       noting that, all enclosed types (nested types) must also be
 *       considered.
 *
 * E. Finally, this class provides methods to obtain the specified sets,
 *    which are frozen and cached in the analysis phase, the included
 *    sets, are computed lazily and cached for future use. An element
 *    can be checked if it should be documented, in which case, the
 *    element is checked against the included set and the result is
 *    cached, for performance reasons.
 *
 * Definitions:
 *    Fully included: an element is included and some or parts
 *    of it components are included implicitly, subject to a
 *    selection criteria of its enclosed children.
 *
 *    Included: if the item should be documented.
 *
 * Rules for processing:
 *
 * 1. A specified element, meaning an element given on the
 *    command-line, and exposed via specified elements collections.
 * 2. Expand-contents, an internal pseudo term, meaning
 *    it is part of the recursive expansion of specified
 *    elements, meaning, the modules are expanded first, then
 *    the packages contained in the expanded modules, and then
 *    the types contained within the packages, to produce the
 *    collections returned by the methods
 *    getInclude{Module|Package|Type}Elements(), this is a
 *    downward expansion.
 * 3. An included element, meaning it should be documented, and
 *    exposed via isIncluded, this enclosing element (module, package)
 *    is recursively included.
 */
public class ElementsTable {

    private final ToolEnvironment toolEnv;
    private final Symtab syms;
    private final Names names;
    private final JavaFileManager fm;
    private final List<Location> locations;
    private final Modules modules;
    private final ToolOptions options;
    private final JavadocLog log;
    private final JavaCompiler compiler;

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    // specified elements
    private Set<ModuleElement> specifiedModuleElements = new LinkedHashSet<>();
    private Set<PackageElement> specifiedPackageElements = new LinkedHashSet<>();
    private Set<TypeElement> specifiedTypeElements = new LinkedHashSet<>();

    // included elements
    private Set<ModuleElement> includedModuleElements = null;
    private Set<PackageElement> includedPackageElements = null;
    private Set<TypeElement> includedTypeElements = null;

    // cmdline specifiers
    private Set<ModulePackage> cmdLinePackages = new LinkedHashSet<>();
    private Set<ModulePackage> excludePackages = new LinkedHashSet<>();
    private Set<ModulePackage> subPackages = new LinkedHashSet<>();

    private List<JCClassDecl> classDecList = Collections.emptyList();
    private List<String> classArgList = Collections.emptyList();
    private com.sun.tools.javac.util.List<JCCompilationUnit> classTreeList = null;

    private final Set<JavaFileObject.Kind> sourceKinds = EnumSet.of(JavaFileObject.Kind.SOURCE);

    private final ModifierFilter accessFilter;

    private final AccessKind expandRequires;

    final boolean xclasses;

    /**
     * Creates the table to manage included and excluded elements.
     *
     * @param context the context to locate commonly used objects
     * @param options the tool options
     */
    ElementsTable(Context context, ToolOptions options) {
        this.toolEnv = ToolEnvironment.instance(context);
        this.syms = Symtab.instance(context);
        this.names = Names.instance(context);
        this.fm = toolEnv.fileManager;
        this.modules = Modules.instance(context);
        this.options = options;
        this.log = JavadocLog.instance0(context);
        this.compiler = JavaCompiler.instance(context);
        Source source = Source.instance(context);

        List<Location> locs = new ArrayList<>();
        if (modules.multiModuleMode) {
            locs.add(StandardLocation.MODULE_SOURCE_PATH);
        } else {
            if (toolEnv.fileManager.hasLocation(StandardLocation.SOURCE_PATH))
                locs.add(StandardLocation.SOURCE_PATH);
            else
                locs.add(StandardLocation.CLASS_PATH);
        }
        if (Feature.MODULES.allowedInSource(source) && toolEnv.fileManager.hasLocation(StandardLocation.PATCH_MODULE_PATH))
            locs.add(StandardLocation.PATCH_MODULE_PATH);
        this.locations = Collections.unmodifiableList(locs);

        getEntry("").excluded = false;

        accessFilter = new ModifierFilter(options);
        xclasses = options.xclasses();
        expandRequires = options.expandRequires();
    }

    /**
     * Returns the module documentation level mode.
     * @return the module documentation level mode
     */
    public ModuleMode getModuleMode() {
        switch(accessFilter.getAccessValue(ElementKind.MODULE)) {
            case PACKAGE: case PRIVATE:
                return DocletEnvironment.ModuleMode.ALL;
            default:
                return DocletEnvironment.ModuleMode.API;
        }
    }

    private Set<Element> specifiedElements = null;
    /**
     * Returns a set of elements specified on the
     * command line, including any inner classes.
     *
     * @return the set of elements specified on the command line
     */
    public Set<? extends Element> getSpecifiedElements() {
        if (specifiedElements == null) {
            Set<Element> result = new LinkedHashSet<>();
            result.addAll(specifiedModuleElements);
            result.addAll(specifiedPackageElements);
            result.addAll(specifiedTypeElements);
            specifiedElements = Collections.unmodifiableSet(result);
        }
        return specifiedElements;
    }

    private Set<Element> includedElements = null;
    /**
     * Returns a set of elements included elements. The inclusion is as
     * follows:
     * A module is fully included,
     *   - is specified on the command line --module
     *   - is derived from the module graph, that is, by expanding the
     *     requires directive, based on --expand-requires
     *
     * A module is included if an enclosed package or type is
     * specified on the command line.
     *
     * A package is fully included,
     *  - is specified on the command line
     *  - is derived from expanding -subpackages
     *  - can be documented in a fully included module based on --show-packages
     *
     * A package is included, if an enclosed package or a type is specified on
     * the command line.
     *
     * Included type elements (including those within specified or included packages)
     * to be documented.
     *
     * A type is fully included if
     *  - is specified on the command line with -sourcepath
     *  - is visible with --show-types filter
     * A nested type is fully included if
     *  - is visible with --show-types filter
     *  - is enclosed in a fully included type
     * @return the set of elements specified on the command line
     */
    public Set<? extends Element> getIncludedElements() {
        if (includedElements == null) {
            Set<Element> result = new LinkedHashSet<>();
            result.addAll(includedModuleElements);
            result.addAll(includedPackageElements);
            result.addAll(includedTypeElements);
            includedElements = Collections.unmodifiableSet(result);
        }
        return includedElements;
    }

    private IncludedVisitor includedVisitor = null;

    /**
     * Returns true if the given element is included for consideration.
     * This method accumulates elements in the cache as enclosed elements of
     * fully included elements are tested.
     * A member (constructor, method, field) is included if
     *  - it is visible in a fully included type (--show-members)
     *
     * @param e the element in question
     *
     * @see #getIncludedElements()
     *
     * @return true if included
     */
    public boolean isIncluded(Element e) {
        if (e == null) {
            return false;
        }
        if (includedVisitor == null) {
            includedVisitor = new IncludedVisitor();
        }
        return includedVisitor.visit(e);
    }

    /**
     * Performs the final computation and freezes the collections.
     * This is a terminal operation, thus no further modifications
     * are allowed to the specified data sets.
     *
     * @throws ToolException if an error occurs
     */
    void analyze() throws ToolException {
        // compute the specified element, by expanding module dependencies
        computeSpecifiedModules();

        // compute all specified packages and subpackages
        computeSpecifiedPackages();

        // compute the specified types
        computeSpecifiedTypes();

        // compute the packages belonging to all the specified modules
        Set<PackageElement> expandedModulePackages = computeModulePackages();
        initializeIncludedSets(expandedModulePackages);
    }

    ElementsTable classTrees(com.sun.tools.javac.util.List<JCCompilationUnit> classTrees) {
        this.classTreeList = classTrees;
        return this;
    }

    /*
     * This method sanity checks the following cases:
     * a. a source-path containing a single module and many modules specified with --module
     * b. no modules on source-path
     * c. mismatched source-path and many modules specified with --module
     */
    void sanityCheckSourcePathModules(List<String> moduleNames) throws ToolException {
        if (!haveSourceLocationWithModule)
            return;

        if (moduleNames.size() > 1) {
            String text = log.getText("main.cannot_use_sourcepath_for_modules",
                    String.join(", ", moduleNames));
            throw new ToolException(CMDERR, text);
        }

        String foundModule = getModuleName(StandardLocation.SOURCE_PATH);
        if (foundModule == null) {
            String text = log.getText("main.module_not_found_on_sourcepath", moduleNames.get(0));
            throw new ToolException(CMDERR, text);
        }

        if (!moduleNames.get(0).equals(foundModule)) {
            String text = log.getText("main.sourcepath_does_not_contain_module", moduleNames.get(0));
            throw new ToolException(CMDERR, text);
        }
    }

    private String getModuleName(Location location) throws ToolException {
        try {
            JavaFileObject jfo = fm.getJavaFileForInput(location,
                    "module-info", JavaFileObject.Kind.SOURCE);
            if (jfo != null) {
                JCCompilationUnit jcu = compiler.parse(jfo);
                JCModuleDecl module = TreeInfo.getModule(jcu);
                if (module != null) {
                    return module.getName().toString();
                }
            }
        } catch (IOException ioe) {
            String text = log.getText("main.file.manager.list", location);
            throw new ToolException(SYSERR, text, ioe);
        }
        return null;
    }

    ElementsTable scanSpecifiedItems() throws ToolException {

        // scan modules specified on the command line
        List<String> modules = options.modules();
        List<String> mlist = new ArrayList<>();
        for (String m : modules) {
            List<Location> moduleLocations = getModuleLocation(locations, m);
            if (moduleLocations.isEmpty()) {
                String text = log.getText("main.module_not_found", m);
                throw new ToolException(CMDERR, text);
            }
            if (moduleLocations.contains(StandardLocation.SOURCE_PATH)) {
                sanityCheckSourcePathModules(modules);
            }
            mlist.add(m);
            ModuleSymbol msym = syms.enterModule(names.fromString(m));
            specifiedModuleElements.add(msym);
        }

        // scan for modules with qualified packages
        cmdLinePackages.stream()
                .filter(ModulePackage::hasModule)
                .forEachOrdered(mpkg -> mlist.add(mpkg.moduleName));

        // scan for modules with qualified subpackages
        options.subpackages().stream()
            .map(ModulePackage::new)
            .forEachOrdered(mpkg -> {
                subPackages.add(mpkg);
                if (mpkg.hasModule()) {
                    mlist.add(mpkg.moduleName);
                }
            });

        // all the modules specified on the command line have been scraped
        // init the module systems
        this.modules.addExtraAddModules(mlist.toArray(new String[mlist.size()]));
        this.modules.initModules(this.classTreeList);

        return this;
    }

    /**
     * Returns the includes table after setting a class names specified on the command line.
     *
     * @param classList
     * @return the include table
     */
    ElementsTable setClassArgList(List<String> classList) {
        classArgList = classList;
        return this;
    }

    /**
     * Returns the includes table after setting the parsed class names.
     *
     * @param classesDecList
     * @return the include table
     */
    ElementsTable setClassDeclList(List<JCClassDecl> classesDecList) {
        this.classDecList = classesDecList;
        return this;
    }

    /**
     * Returns an includes table after setting the specified package
     * names.
     * @param packageNames packages on the command line
     * @return the includes table after setting the specified package
     * names
     */
    ElementsTable packages(Collection<String> packageNames) {
        packageNames.stream()
            .map(ModulePackage::new)
            .forEachOrdered(mpkg -> cmdLinePackages.add(mpkg));
        return this;
    }

    /**
     * Returns the aggregate set of included packages and specified
     * sub packages.
     *
     * @return the aggregate set of included packages and specified
     * sub packages
     */
    Iterable<ModulePackage> getPackagesToParse() throws IOException {
        List<ModulePackage> result = new ArrayList<>();
        result.addAll(cmdLinePackages);
        result.addAll(subPackages);
        return result;
    }

    private void computeSubpackages() throws ToolException {
        options.excludes().stream()
                .map(ModulePackage::new)
                .forEachOrdered(mpkg -> excludePackages.add(mpkg));

        excludePackages.forEach(p -> getEntry(p).excluded = true);

        for (ModulePackage modpkg : subPackages) {
            List<Location> locs = getLocation(modpkg);
            for (Location loc : locs) {
                addPackagesFromLocations(loc, modpkg);
            }
        }
    }

    /* Call fm.list and wrap any IOException that occurs in a ToolException */
    private Iterable<JavaFileObject> fmList(Location location,
                                            String packagename,
                                            Set<JavaFileObject.Kind> kinds,
                                            boolean recurse) throws ToolException {
        try {
            return fm.list(location, packagename, kinds, recurse);
        } catch (IOException ioe) {
            String text = log.getText("main.file.manager.list", packagename);
            throw new ToolException(SYSERR, text, ioe);
        }
    }

    private void addPackagesFromLocations(Location packageLocn, ModulePackage modpkg) throws ToolException {
        for (JavaFileObject fo : fmList(packageLocn, modpkg.packageName, sourceKinds, true)) {
            String binaryName = fm.inferBinaryName(packageLocn, fo);
            String pn = getPackageName(binaryName);
            String simpleName = getSimpleName(binaryName);
            Entry e = getEntry(pn);
            if (!e.isExcluded() && isValidClassName(simpleName)) {
                ModuleSymbol msym = (modpkg.hasModule())
                        ? syms.getModule(names.fromString(modpkg.moduleName))
                        : findModuleOfPackageName(modpkg.packageName);

                if (msym != null && !msym.isUnnamed()) {
                    syms.enterPackage(msym, names.fromString(pn));
                    ModulePackage npkg = new ModulePackage(msym.toString(), pn);
                    cmdLinePackages.add(npkg);
                } else {
                    cmdLinePackages.add(e.modpkg);
                }
                e.files = (e.files == null
                        ? com.sun.tools.javac.util.List.of(fo)
                        : e.files.prepend(fo));
            }
        }
    }

    /**
     * Returns the "requires" modules for the target module.
     * @param mdle the target module element
     * @param onlyTransitive true gets all the requires transitive, otherwise
     *                 gets all the non-transitive requires
     *
     * @return a set of modules
     */
    private Set<ModuleElement> getModuleRequires(ModuleElement mdle, boolean onlyTransitive) throws ToolException {
        Set<ModuleElement> result = new HashSet<>();
        for (RequiresDirective rd : ElementFilter.requiresIn(mdle.getDirectives())) {
            ModuleElement dep = rd.getDependency();
            if (result.contains(dep))
                continue;
            if (!isMandated(mdle, rd) && onlyTransitive == rd.isTransitive()) {
                if (!haveModuleSources(dep)) {
                    if (!warnedNoSources.contains(dep)) {
                        log.printWarningUsingKey(dep, "main.module_source_not_found", dep.getQualifiedName());
                        warnedNoSources.add(dep);
                    }
                }
                result.add(dep);
            } else if (isMandated(mdle, rd) && haveModuleSources(dep)) {
                result.add(dep);
            }
        }
        return result;
    }

    private boolean isMandated(ModuleElement mdle, RequiresDirective rd) {
        return toolEnv.elements.getOrigin(mdle, rd) == MANDATED;
    }

    Set<ModuleElement> warnedNoSources = new HashSet<>();

    Map<ModuleSymbol, Boolean> haveModuleSourcesCache = new HashMap<>();
    private boolean haveModuleSources(ModuleElement mdle) throws ToolException {
        ModuleSymbol msym =  (ModuleSymbol) mdle;
        if (msym.sourceLocation != null) {
            return true;
        }
        if (msym.patchLocation != null) {
            Boolean value = haveModuleSourcesCache.get(msym);
            if (value == null) {
                value = fmList(msym.patchLocation, "", sourceKinds, true).iterator().hasNext();
                haveModuleSourcesCache.put(msym, value);
            }
            return value;
        }
        return false;
    }

    private void computeSpecifiedModules() throws ToolException {
        if (expandRequires == null) { // no expansion requested
            specifiedModuleElements = Collections.unmodifiableSet(specifiedModuleElements);
            return;
        }

        final boolean expandAll = expandRequires.equals(AccessKind.PRIVATE)
                || expandRequires.equals(AccessKind.PACKAGE);

        Set<ModuleElement> result = new LinkedHashSet<>();
        ListBuffer<ModuleElement> queue = new ListBuffer<>();

        // expand each specified module
        for (ModuleElement mdle : specifiedModuleElements) {
            result.add(mdle); // a specified module is included
            queue.append(mdle);
            Set<ModuleElement> publicRequires = getModuleRequires(mdle, true);
            result.addAll(publicRequires);
            // add all requires public
            queue.addAll(publicRequires);

            if (expandAll) {
                 // add non-public requires if needed
                result.addAll(getModuleRequires(mdle, false));
            }
        }

        // compute the transitive closure of all the requires public
        for (ModuleElement m = queue.poll() ; m != null ; m = queue.poll()) {
            for (ModuleElement mdle : getModuleRequires(m, true)) {
                if (!result.contains(mdle)) {
                    result.add(mdle);
                    queue.append(mdle);
                }
            }
        }
        specifiedModuleElements = Collections.unmodifiableSet(result);
    }

    private Set<PackageElement> getAllModulePackages(ModuleElement mdle) throws ToolException {
        Set<PackageElement> result = new HashSet<>();
        ModuleSymbol msym = (ModuleSymbol) mdle;
        List<Location> msymlocs = getModuleLocation(locations, msym.name.toString());
        for (Location msymloc : msymlocs) {
            for (JavaFileObject fo : fmList(msymloc, "", sourceKinds, true)) {
                if (fo.getName().endsWith("module-info.java")) {
                    continue;
                }
                String binaryName = fm.inferBinaryName(msymloc, fo);
                String pn = getPackageName(binaryName);
                PackageSymbol psym = syms.enterPackage(msym, names.fromString(pn));
                result.add((PackageElement) psym);
            }
        }
        return result;
    }

    private Set<PackageElement> computeModulePackages() throws ToolException {
        AccessKind accessValue = accessFilter.getAccessValue(ElementKind.PACKAGE);
        final boolean documentAllModulePackages = (accessValue == AccessKind.PACKAGE ||
                accessValue == AccessKind.PRIVATE);

        accessValue = accessFilter.getAccessValue(ElementKind.MODULE);
        final boolean moduleDetailedMode = (accessValue == AccessKind.PACKAGE ||
                accessValue == AccessKind.PRIVATE);
        Set<PackageElement> expandedModulePackages = new LinkedHashSet<>();

        for (ModuleElement mdle : specifiedModuleElements) {
            if (documentAllModulePackages) { // include all packages
                List<PackageElement> packages = ElementFilter.packagesIn(mdle.getEnclosedElements());
                expandedModulePackages.addAll(packages);
                expandedModulePackages.addAll(getAllModulePackages(mdle));
            } else { // selectively include required packages
                List<ExportsDirective> exports = ElementFilter.exportsIn(mdle.getDirectives());
                for (ExportsDirective export : exports) {
                    // add if fully exported or add qualified exports only if desired
                    if (export.getTargetModules() == null
                            || documentAllModulePackages || moduleDetailedMode) {
                        expandedModulePackages.add(export.getPackage());
                    }
                }
            }

            // add all packages specified on the command line
            // belonging to this module
            if (!cmdLinePackages.isEmpty()) {
                for (ModulePackage modpkg : cmdLinePackages) {
                    PackageElement pkg = toolEnv.elements.getPackageElement(mdle,
                            modpkg.packageName);
                    if (pkg != null) {
                        expandedModulePackages.add(pkg);
                    }
                }
            }
        }
        return expandedModulePackages;
    }

    private void initializeIncludedSets(Set<PackageElement> expandedModulePackages) {

        // process modules
        Set<ModuleElement> imodules = new LinkedHashSet<>();
        // add all the expanded modules
        imodules.addAll(specifiedModuleElements);

        // process packages
        Set<PackageElement> ipackages = new LinkedHashSet<>();
        // add all packages belonging to expanded modules
        ipackages.addAll(expandedModulePackages);
        // add all specified packages
        specifiedPackageElements.forEach(pkg -> {
            ModuleElement mdle = toolEnv.elements.getModuleOf(pkg);
            if (mdle != null)
                imodules.add(mdle);
            ipackages.add(pkg);
        });

        // process types
        Set<TypeElement> iclasses = new LinkedHashSet<>();
        // add all types enclosed in expanded modules and packages
        ipackages.forEach(pkg -> addAllClasses(iclasses, pkg));
        // add all types and its nested types
        specifiedTypeElements.forEach(klass -> {
            ModuleElement mdle = toolEnv.elements.getModuleOf(klass);
            if (mdle != null && !mdle.isUnnamed())
                imodules.add(mdle);
            PackageElement pkg = toolEnv.elements.getPackageOf(klass);
            ipackages.add(pkg);
            addAllClasses(iclasses, klass, true);
        });

        // all done, freeze the collections
        includedModuleElements = Collections.unmodifiableSet(imodules);
        includedPackageElements = Collections.unmodifiableSet(ipackages);
        includedTypeElements = Collections.unmodifiableSet(iclasses);
    }

    /*
     * Computes the included packages and freezes the specified packages list.
     */
    private void computeSpecifiedPackages() throws ToolException {

        computeSubpackages();

        Set<PackageElement> packlist = new LinkedHashSet<>();
        cmdLinePackages.forEach(modpkg -> {
            PackageElement pkg;
            if (modpkg.hasModule()) {
                ModuleElement mdle = toolEnv.elements.getModuleElement(modpkg.moduleName);
                pkg = toolEnv.elements.getPackageElement(mdle, modpkg.packageName);
            } else {
                pkg = toolEnv.elements.getPackageElement(modpkg.toString());
            }

            if (pkg != null) {
                packlist.add(pkg);
            } else {
                log.printWarningUsingKey("main.package_not_found", modpkg.toString());
            }
        });
        specifiedPackageElements = Collections.unmodifiableSet(packlist);
    }

    /**
     * Adds all classes as well as inner classes, to the specified
     * list.
     */
    private void computeSpecifiedTypes() throws ToolException {
        Set<TypeElement> classes = new LinkedHashSet<>();
          classDecList.forEach(def -> {
            TypeElement te = def.sym;
            if (te != null) {
                addAllClasses(classes, te, true);
            }
        });
        for (String className : classArgList) {
            TypeElement te = toolEnv.loadClass(className);
            if (te == null) {
                String text = log.getText("javadoc.class_not_found", className);
                throw new ToolException(CMDERR, text);
            } else {
                addAllClasses(classes, te, true);
            }
        }
        specifiedTypeElements = Collections.unmodifiableSet(classes);
    }

    private void addFilesForParser(Collection<JavaFileObject> result,
            Collection<ModulePackage> collection,
            boolean recurse) throws ToolException {
        for (ModulePackage modpkg : collection) {
            toolEnv.notice("main.Loading_source_files_for_package", modpkg.toString());
            List<JavaFileObject> files = getFiles(modpkg, recurse);
            if (files.isEmpty()) {
                String text = log.getText("main.no_source_files_for_package",
                        modpkg.toString());
                throw new ToolException(CMDERR, text);
            } else {
                result.addAll(files);
            }
        }
    }

    /**
     * Returns an aggregated list of java file objects from the items
     * specified on the command line. The packages specified should not
     * recurse, however sub-packages should recurse into the sub directories.
     * @return a list of java file objects
     * @throws IOException if an error occurs
     */
    List<JavaFileObject> getFilesToParse() throws ToolException {
        List<JavaFileObject> result = new ArrayList<>();
        addFilesForParser(result, cmdLinePackages, false);
        addFilesForParser(result, subPackages, true);
        return result;
    }

    /**
     * Returns the set of source files for a package.
     *
     * @param modpkg the specified package
     * @return the set of file objects for the specified package
     * @throws ToolException if an error occurs while accessing the files
     */
    private List<JavaFileObject> getFiles(ModulePackage modpkg,
            boolean recurse) throws ToolException {
        Entry e = getEntry(modpkg);
        // The files may have been found as a side effect of searching for subpackages
        if (e.files != null) {
            return e.files;
        }

        ListBuffer<JavaFileObject> lb = new ListBuffer<>();
        List<Location> locs = getLocation(modpkg);
        if (locs.isEmpty()) {
            return Collections.emptyList();
        }
        String pname = modpkg.packageName;
        for (Location packageLocn : locs) {
            for (JavaFileObject fo : fmList(packageLocn, pname, sourceKinds, recurse)) {
                String binaryName = fm.inferBinaryName(packageLocn, fo);
                String simpleName = getSimpleName(binaryName);
                if (isValidClassName(simpleName)) {
                    lb.append(fo);
                }
            }
        }
        return lb.toList();
    }

    private ModuleSymbol findModuleOfPackageName(String packageName) {
            Name pack = names.fromString(packageName);
            for (ModuleSymbol msym : modules.allModules()) {
                PackageSymbol p = syms.getPackage(msym, pack);
                if (p != null && !p.members().isEmpty()) {
                    return msym;
                }
            }
            return null;
    }

    private List<Location> getLocation(ModulePackage modpkg) throws ToolException {
        if (locations.size() == 1 && !locations.contains(StandardLocation.MODULE_SOURCE_PATH)) {
            return Collections.singletonList(locations.get(0));
        }

        if (modpkg.hasModule()) {
            return getModuleLocation(locations, modpkg.moduleName);
        }
        // TODO: handle invalid results better.
        ModuleSymbol msym = findModuleOfPackageName(modpkg.packageName);
        if (msym == null) {
            return Collections.emptyList();
        }
        return getModuleLocation(locations, msym.name.toString());
    }

    boolean haveSourceLocationWithModule = false;

    private List<Location> getModuleLocation(List<Location> locations, String msymName) throws ToolException {
        List<Location> out = new ArrayList<>();
        // search in the patch module first, this overrides others
        if (locations.contains(StandardLocation.PATCH_MODULE_PATH)) {
            Location loc = getModuleLocation(StandardLocation.PATCH_MODULE_PATH, msymName);
            if (loc != null)
                out.add(loc);
        }
        for (Location location : locations) {
            // skip patch module, already done
            if (location == StandardLocation.PATCH_MODULE_PATH) {
                continue;
            } else if (location == StandardLocation.MODULE_SOURCE_PATH) {
                Location loc = getModuleLocation(location, msymName);
                if (loc != null)
                    out.add(loc);
            } else if (location == StandardLocation.SOURCE_PATH) {
                haveSourceLocationWithModule = true;
                out.add(StandardLocation.SOURCE_PATH);
            }
        }
        return out;
    }

    private Location getModuleLocation(Location location, String msymName) throws ToolException {
        try {
            return fm.getLocationForModule(location, msymName);
        } catch (IOException ioe) {
            String text = log.getText("main.doclet_could_not_get_location", msymName);
            throw new ToolException(ERROR, text, ioe);
        }
    }

    private Entry getEntry(String name) {
        return getEntry(new ModulePackage(name));
    }

    private Entry getEntry(ModulePackage modpkg) {
        Entry e = entries.get(modpkg.packageName);
        if (e == null) {
            entries.put(modpkg.packageName, e = new Entry(modpkg));
        }
        return e;
    }

    private String getPackageName(String name) {
        int lastDot = name.lastIndexOf(".");
        return (lastDot == -1 ? "" : name.substring(0, lastDot));
    }

    private String getSimpleName(String name) {
        int lastDot = name.lastIndexOf(".");
        return (lastDot == -1 ? name : name.substring(lastDot + 1));
    }

    /**
     * Adds all inner classes of this class, and their inner classes recursively, to the list
     */
    private void addAllClasses(Collection<TypeElement> list, TypeElement typeElement, boolean filtered) {
        ClassSymbol klass = (ClassSymbol)typeElement;
        try {
            // eliminate needless checking, do this first.
            if (list.contains(klass)) return;
            // ignore classes with invalid Java class names
            if (!JavadocTool.isValidClassName(klass.name.toString())) return;
            if (filtered && !isTypeElementSelected(klass)) return;
            list.add(klass);
            for (Symbol sym : klass.members().getSymbols(NON_RECURSIVE)) {
                if (sym != null && sym.kind == Kind.TYP) {
                    ClassSymbol s = (ClassSymbol)sym;
                    addAllClasses(list, s, filtered);
                }
            }
        } catch (CompletionFailure e) {
            if (e.getMessage() != null)
                log.printWarning(e.getMessage());
            else
                log.printWarningUsingKey("main.unexpected.exception", e);
        }
    }

    /**
     * Returns a list of all classes contained in this package, including
     * member classes of those classes, and their member classes, etc.
     */
    private void addAllClasses(Collection<TypeElement> list, PackageElement pkg) {
        boolean filtered = true;
        for (Element isym : pkg.getEnclosedElements()) {
            addAllClasses(list, (TypeElement)isym, filtered);
        }
    }

    private boolean isTypeElementSelected(TypeElement te) {
        return (xclasses || toolEnv.getFileKind(te) == SOURCE) && isSelected(te);
    }

    SimpleElementVisitor14<Boolean, Void> visibleElementVisitor = null;
    /**
     * Returns true if the element is selected, by applying
     * the access filter checks. Special treatment is applied to
     * types, for a top level type the access filter applies completely,
     * however if is a nested type then it is allowed either  if
     * the enclosing is a static or the enclosing is also selected.
     *
     * @param e the element to be checked
     * @return true if the element is visible
     */
    public boolean isSelected(Element e) {
        if (toolEnv.isSynthetic((Symbol) e)) {
            return false;
        }
        if (visibleElementVisitor == null) {
            visibleElementVisitor = new SimpleElementVisitor14<Boolean, Void>() {
                @Override
                public Boolean visitType(TypeElement e, Void p) {
                    if (!accessFilter.checkModifier(e)) {
                        return false; // it is not allowed
                    }
                    Element encl = e.getEnclosingElement();

                    // check if nested
                    if (encl.getKind() == ElementKind.PACKAGE)
                        return true; // top-level class, allow it

                    // is enclosed static
                    if (encl.getModifiers().contains(Modifier.STATIC))
                        return true; // allowed

                    // check the enclosing
                    return visit(encl);
                }

                @Override
                protected Boolean defaultAction(Element e, Void p) {
                    return accessFilter.checkModifier(e);
                }

                @Override
                public Boolean visitUnknown(Element e, Void p) {
                    throw new AssertionError("unknown element: " + e);
                }
            };
        }
        return visibleElementVisitor.visit(e);
    }

    private class IncludedVisitor extends SimpleElementVisitor14<Boolean, Void> {
        private final Set<Element> includedCache;

        public IncludedVisitor() {
            includedCache = new LinkedHashSet<>();
        }

        @Override
        public Boolean visitModule(ModuleElement e, Void p) {
            // deduced by specified and/or requires expansion
            return includedModuleElements.contains(e);
        }

        @Override
        public Boolean visitPackage(PackageElement e, Void p) {
            // deduced by specified or downward expansions
            return includedPackageElements.contains(e);
        }

        @Override
        public Boolean visitType(TypeElement e, Void p) {
            if (includedTypeElements.contains(e)) {
                return true;
            }
            if (isTypeElementSelected(e)) {
                // Class is nameable from top-level and
                // the class and all enclosing classes
                // pass the modifier filter.
                PackageElement pkg = toolEnv.elements.getPackageOf(e);
                if (specifiedPackageElements.contains(pkg)) {
                    return true;
                }
                Element enclosing = e.getEnclosingElement();
                if (enclosing != null) {
                    switch(enclosing.getKind()) {
                        case PACKAGE:
                            return specifiedPackageElements.contains((PackageElement)enclosing);
                        case CLASS: case INTERFACE: case ENUM: case ANNOTATION_TYPE:
                            return visit((TypeElement) enclosing);
                        default:
                            throw new AssertionError("unknown element: " + enclosing);
                    }
                }
            }
            return false;
        }

        // members
        @Override
        public Boolean defaultAction(Element e, Void p) {
            if (includedCache.contains(e))
                return true;
            if (visit(e.getEnclosingElement()) && isSelected(e)) {
                switch(e.getKind()) {
                    case ANNOTATION_TYPE: case CLASS: case ENUM: case INTERFACE:
                    case MODULE: case OTHER: case PACKAGE:
                        throw new AssertionError("invalid element for this operation: " + e);
                    default:
                        // the only allowed kinds in the cache are "members"
                        includedCache.add(e);
                        return true;
                }
            }
            return false;
        }

        @Override
        public Boolean visitUnknown(Element e, Void p) {
            throw new AssertionError("unknown element: " + e);
        }

    }

    class Entry {
        final ModulePackage modpkg;
        Boolean excluded = false;
        com.sun.tools.javac.util.List<JavaFileObject> files;

        Entry(ModulePackage modpkg) {
            this.modpkg = modpkg;
        }

        Entry(String name) {
            modpkg = new ModulePackage(name);
        }

        boolean isExcluded() {
            return excluded;
        }

        @Override
        public String toString() {
            return "Entry{" + "modpkg=" + modpkg + ", excluded=" + excluded + ", files=" + files + '}';
        }
    }

    /**
     * A container class to retrieve the module and package pair
     * from a parsed qualified package name.
     */
    static class ModulePackage {

        public final String moduleName;
        public final String packageName;

        ModulePackage(String modulename, String packagename) {
            this.moduleName = modulename;
            this.packageName = packagename;
        }

        ModulePackage(ModuleElement msym, String packagename) {
            this.moduleName = msym.toString();
            this.packageName = packagename;
        }

        ModulePackage(String name) {
            String a[] = name.split("/");
            if (a.length == 2) {
                this.moduleName = a[0];
                this.packageName = a[1];
            } else {
                moduleName = null;
                packageName = name;
            }
        }

        boolean hasModule() {
            return this.moduleName != null;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ModulePackage that) {
                return this.toString().equals(that.toString());
            }
            return false;
        }

        @Override
        public int hashCode() {
             return toString().hashCode();
        }

        @Override
        public String toString() {
            return moduleName == null ? packageName : moduleName + "/" + packageName;
        }
    }

    /**
     * A class which filters the access flags on classes, fields, methods, etc.
     *
     * @see javax.lang.model.element.Modifier
     */

    static class ModifierFilter {
        /**
         * The allowed ElementKind that can be stored.
         */
        static final EnumSet<ElementKind> ALLOWED_KINDS = EnumSet.of(ElementKind.METHOD,
                                                    ElementKind.CLASS,
                                                    ElementKind.PACKAGE,
                                                    ElementKind.MODULE);

        // all possible access levels allowed for each element
        private final EnumMap<ElementKind, EnumSet<AccessKind>> filterMap =
                new EnumMap<>(ElementKind.class);

        // the specified access level for each element
        private final EnumMap<ElementKind, AccessKind> accessMap =
                new EnumMap<>(ElementKind.class);

        /**
         * Constructor - Specify a filter.
         *
         * @param options the tool options
         */
        ModifierFilter(ToolOptions options) {

            AccessKind accessValue = null;
            for (ElementKind kind : ALLOWED_KINDS) {
                switch (kind) {
                    case METHOD:
                        accessValue  = options.showMembersAccess();
                        break;
                    case CLASS:
                        accessValue  = options.showTypesAccess();
                        break;
                    case PACKAGE:
                        accessValue  = options.showPackagesAccess();
                        break;
                    case MODULE:
                        accessValue  = options.showModuleContents();
                        break;
                    default:
                        throw new AssertionError("unknown element: " + kind);

                }
                accessMap.put(kind, accessValue);
                filterMap.put(kind, getFilterSet(accessValue));
            }
        }

        static EnumSet<AccessKind> getFilterSet(AccessKind accessValue) {
            switch (accessValue) {
                case PUBLIC:
                    return EnumSet.of(AccessKind.PUBLIC);
                case PROTECTED:
                default:
                    return EnumSet.of(AccessKind.PUBLIC, AccessKind.PROTECTED);
                case PACKAGE:
                    return EnumSet.of(AccessKind.PUBLIC, AccessKind.PROTECTED, AccessKind.PACKAGE);
                case PRIVATE:
                    return EnumSet.allOf(AccessKind.class);
            }
        }

        public AccessKind getAccessValue(ElementKind kind) {
            if (!ALLOWED_KINDS.contains(kind)) {
                throw new IllegalArgumentException("not allowed: " + kind);
            }
            return accessMap.getOrDefault(kind, AccessKind.PROTECTED);
        }

        /**
         * Returns true if access is allowed.
         *
         * @param e the element in question
         * @return whether the modifiers pass this filter
         */
        public boolean checkModifier(Element e) {
            Set<Modifier> modifiers = e.getModifiers();
            AccessKind fflag = AccessKind.PACKAGE;
            if (modifiers.contains(Modifier.PUBLIC)) {
                fflag = AccessKind.PUBLIC;
            } else if (modifiers.contains(Modifier.PROTECTED)) {
                fflag = AccessKind.PROTECTED;
            } else if (modifiers.contains(Modifier.PRIVATE)) {
                fflag = AccessKind.PRIVATE;
            }
            EnumSet<AccessKind> filterSet = filterMap.get(getAllowedKind(e.getKind()));
            return filterSet.contains(fflag);
        }

        // convert a requested element kind to an allowed access kind
        private ElementKind getAllowedKind(ElementKind kind) {
            switch (kind) {
                case CLASS: case METHOD: case MODULE: case PACKAGE:
                    return kind;
                case RECORD: case ANNOTATION_TYPE: case ENUM: case INTERFACE:
                    return ElementKind.CLASS;
                case CONSTRUCTOR: case ENUM_CONSTANT: case EXCEPTION_PARAMETER:
                case FIELD: case INSTANCE_INIT: case LOCAL_VARIABLE: case PARAMETER:
                case RESOURCE_VARIABLE: case STATIC_INIT: case TYPE_PARAMETER:
                case RECORD_COMPONENT:
                    return ElementKind.METHOD;
                default:
                    throw new AssertionError("unsupported kind: " + kind);
            }
        }
    } // end ModifierFilter
}
