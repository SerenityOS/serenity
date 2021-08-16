/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.DocletEnvironment.ModuleMode;
import jdk.javadoc.internal.doclets.formats.html.markup.BodyContents;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.ModuleSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;

/**
 * Class to generate file for each module contents in the right-hand frame. This will list all the
 * required modules, packages and service types for the module. A click on any of the links will update
 * the frame with the clicked element page.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ModuleWriterImpl extends HtmlDocletWriter implements ModuleSummaryWriter {

    /**
     * The module being documented.
     */
    protected ModuleElement mdle;

    /**
     * The module mode for this javadoc run. It can be set to "api" or "all".
     */
    private final ModuleMode moduleMode;

    /**
     * Map of module elements and modifiers required by this module.
     */
    private final Map<ModuleElement, Content> requires
            = new TreeMap<>(comparators.makeModuleComparator());

    /**
     * Map of indirect modules and modifiers, transitive closure, required by this module.
     */
    private final Map<ModuleElement, Content> indirectModules
            = new TreeMap<>(comparators.makeModuleComparator());

    /**
     * Details about a package in a module.
     * A package may be not exported, or exported to some modules, or exported to all modules.
     * A package may be not opened, or opened to some modules, or opened to all modules.
     * A package that is neither exported or opened to any modules is a concealed package.
     * An open module opens all its packages to all modules.
     */
    class PackageEntry {
        /**
         * Summary of package exports:
         * If null, the package is not exported to any modules;
         * if empty, the package is exported to all modules;
         * otherwise, the package is exported to these modules.
         */
        Set<ModuleElement> exportedTo;

        /**
         * Summary of package opens:
         * If null, the package is not opened to any modules;
         * if empty, the package is opened to all modules;
         * otherwise, the package is opened to these modules.
         */
        Set<ModuleElement> openedTo;
    }

    /**
     * Map of packages of this module, and details of whether they are exported or opened.
     */
    private final Map<PackageElement, PackageEntry> packages = new TreeMap<>(utils.comparators.makePackageComparator());

    /**
     * Map of indirect modules (transitive closure) and their exported packages.
     */
    private final Map<ModuleElement, SortedSet<PackageElement>> indirectPackages
            = new TreeMap<>(comparators.makeModuleComparator());

    /**
     * Map of indirect modules (transitive closure) and their open packages.
     */
    private final Map<ModuleElement, SortedSet<PackageElement>> indirectOpenPackages
            = new TreeMap<>(comparators.makeModuleComparator());

    /**
     * Set of services used by the module.
     */
    private final SortedSet<TypeElement> uses
            = new TreeSet<>(comparators.makeAllClassesComparator());

    /**
     * Map of services used by the module and specified using @uses javadoc tag, and description.
     */
    private final Map<TypeElement, Content> usesTrees
            = new TreeMap<>(comparators.makeAllClassesComparator());

    /**
     * Map of services provided by this module, and set of its implementations.
     */
    private final Map<TypeElement, SortedSet<TypeElement>> provides
            = new TreeMap<>(comparators.makeAllClassesComparator());

    /**
     * Map of services provided by the module and specified using @provides javadoc tag, and
     * description.
     */
    private final Map<TypeElement, Content> providesTrees
            = new TreeMap<>(comparators.makeAllClassesComparator());

    private final BodyContents bodyContents = new BodyContents();

    /**
     * Constructor to construct ModuleWriter object and to generate "moduleName-summary.html" file.
     *
     * @param configuration the configuration of the doclet.
     * @param mdle        Module under consideration.
     */
    public ModuleWriterImpl(HtmlConfiguration configuration, ModuleElement mdle) {
        super(configuration, configuration.docPaths.moduleSummary(mdle));
        this.mdle = mdle;
        this.moduleMode = configuration.docEnv.getModuleMode();
        computeModulesData();
    }

    /**
     * Get the module header.
     *
     * @param heading the heading for the section
     */
    @Override
    public Content getModuleHeader(String heading) {
        HtmlTree bodyTree = getBody(getWindowTitle(mdle.getQualifiedName().toString()));
        HtmlTree div = new HtmlTree(TagName.DIV);
        div.setStyle(HtmlStyle.header);
        Content moduleHead = new ContentBuilder();
        moduleHead.add(mdle.isOpen() && (configuration.docEnv.getModuleMode() == ModuleMode.ALL)
                ? contents.openModuleLabel : contents.moduleLabel);
        moduleHead.add(" ").add(heading);
        Content tHeading = HtmlTree.HEADING_TITLE(Headings.PAGE_TITLE_HEADING,
                HtmlStyle.title, moduleHead);
        div.add(tHeading);
        bodyContents.setHeader(getHeader(PageMode.MODULE, mdle))
                .addMainContent(div);
        return bodyTree;
    }

    @Override
    protected Navigation getNavBar(PageMode pageMode, Element element) {
        return super.getNavBar(pageMode, element)
                .setSubNavLinks(() -> List.of(
                        links.createLink(HtmlIds.MODULE_DESCRIPTION, contents.navDescription,
                            !utils.getFullBody(mdle).isEmpty() && !options.noComment()),
                        links.createLink(HtmlIds.MODULES, contents.navModules,
                            display(requires) || display(indirectModules)),
                        links.createLink(HtmlIds.PACKAGES, contents.navPackages,
                            display(packages) || display(indirectPackages) || display(indirectOpenPackages)),
                        links.createLink(HtmlIds.SERVICES, contents.navServices,
                            displayServices(uses, usesTrees) || displayServices(provides.keySet(), providesTrees))
                ));
    }

    /**
     * Get the content header.
     */
    @Override
    public Content getContentHeader() {
        return new ContentBuilder();
    }

    /**
     * Get the summary section header.
     */
    @Override
    public Content getSummariesList() {
        return new HtmlTree(TagName.UL).setStyle(HtmlStyle.summaryList);
    }

    /**
     * Get the summary tree.
     *
     * @param summaryContentTree the content tree to be added to the summary tree.
     */
    @Override
    public Content getSummaryTree(Content summaryContentTree) {
        return HtmlTree.SECTION(HtmlStyle.summary, summaryContentTree);
    }

    /**
     * Compute the modules data that will be displayed in various tables on the module summary page.
     */
    public void computeModulesData() {
        CommentHelper ch = utils.getCommentHelper(mdle);
        // Get module dependencies using the module's transitive closure.
        Map<ModuleElement, String> dependentModules = utils.getDependentModules(mdle);
        // Add all dependent modules to indirect modules set. We will remove the modules,
        // listed using the requires directive, from this set to come up with the table of indirect
        // required modules.
        dependentModules.forEach((module, mod) -> {
            if (shouldDocument(module)) {
                indirectModules.put(module, Text.of(mod));
            }
        });
        ElementFilter.requiresIn(mdle.getDirectives()).forEach(directive -> {
            ModuleElement m = directive.getDependency();
            if (shouldDocument(m)) {
                if (moduleMode == ModuleMode.ALL || directive.isTransitive()) {
                    requires.put(m, Text.of(utils.getModifiers(directive)));
                } else {
                    // For api mode, just keep the public requires in dependentModules for display of
                    // indirect packages in the "Packages" section.
                    dependentModules.remove(m);
                }
                indirectModules.remove(m);
            }
        });

        // Get all packages if module is open or if displaying concealed modules
        for (PackageElement pkg : utils.getModulePackageMap().getOrDefault(mdle, Collections.emptySet())) {
            if (shouldDocument(pkg) && (mdle.isOpen() || moduleMode == ModuleMode.ALL)) {
                PackageEntry e = new PackageEntry();
                if (mdle.isOpen()) {
                    e.openedTo = Collections.emptySet();
                }
                packages.put(pkg, e);
            }
        }

        // Get all exported packages for the module, using the exports directive for the module.
        for (ModuleElement.ExportsDirective directive : ElementFilter.exportsIn(mdle.getDirectives())) {
            PackageElement p = directive.getPackage();
            if (shouldDocument(p)) {
                List<? extends ModuleElement> targetMdles = directive.getTargetModules();
                // Include package if in details mode, or exported to all (i.e. targetModules == null)
                if (moduleMode == ModuleMode.ALL || targetMdles == null) {
                    PackageEntry packageEntry = packages.computeIfAbsent(p, pkg -> new PackageEntry());
                    SortedSet<ModuleElement> mdleList = new TreeSet<>(utils.comparators.makeModuleComparator());
                    if (targetMdles != null) {
                        mdleList.addAll(targetMdles);
                    }
                    packageEntry.exportedTo = mdleList;
                }
            }
        }

        // Get all opened packages for the module, using the opens directive for the module.
        // If it is an open module, there will be no separate opens directives.
        for (ModuleElement.OpensDirective directive : ElementFilter.opensIn(mdle.getDirectives())) {
            PackageElement p = directive.getPackage();
            if (shouldDocument(p)) {
                List<? extends ModuleElement> targetMdles = directive.getTargetModules();
                // Include package if in details mode, or opened to all (i.e. targetModules == null)
                if (moduleMode == ModuleMode.ALL || targetMdles == null) {
                    PackageEntry packageEntry = packages.computeIfAbsent(p, pkg -> new PackageEntry());
                    SortedSet<ModuleElement> mdleList = new TreeSet<>(utils.comparators.makeModuleComparator());
                    if (targetMdles != null) {
                        mdleList.addAll(targetMdles);
                    }
                    packageEntry.openedTo = mdleList;
                }
            }
        }

        // Get all the exported and opened packages, for the transitive closure of the module, to be displayed in
        // the indirect packages tables.
        dependentModules.forEach((module, mod) -> {
            SortedSet<PackageElement> exportedPackages = new TreeSet<>(utils.comparators.makePackageComparator());
            ElementFilter.exportsIn(module.getDirectives()).forEach(directive -> {
                PackageElement pkg = directive.getPackage();
                if (shouldDocument(pkg)) {
                    // Qualified exports are not displayed in API mode
                    if (moduleMode == ModuleMode.ALL || directive.getTargetModules() == null) {
                        exportedPackages.add(pkg);
                    }
                }
            });
            // If none of the indirect modules have exported packages to be displayed, we should not be
            // displaying the table and so it should not be added to the map.
            if (!exportedPackages.isEmpty()) {
                indirectPackages.put(module, exportedPackages);
            }
            SortedSet<PackageElement> openPackages = new TreeSet<>(utils.comparators.makePackageComparator());
            if (module.isOpen()) {
                openPackages.addAll(utils.getModulePackageMap().getOrDefault(module, Collections.emptySet()));
            } else {
                ElementFilter.opensIn(module.getDirectives()).forEach(directive -> {
                    PackageElement pkg = directive.getPackage();
                    if (shouldDocument(pkg)) {
                        // Qualified opens are not displayed in API mode
                        if (moduleMode == ModuleMode.ALL || directive.getTargetModules() == null) {
                            openPackages.add(pkg);
                        }
                    }
                });
            }
            // If none of the indirect modules have opened packages to be displayed, we should not be
            // displaying the table and so it should not be added to the map.
            if (!openPackages.isEmpty()) {
                indirectOpenPackages.put(module, openPackages);
            }
        });
        // Get all the services listed as uses directive.
        ElementFilter.usesIn(mdle.getDirectives()).forEach(directive -> {
            TypeElement u = directive.getService();
            if (shouldDocument(u)) {
                uses.add(u);
            }
        });
        // Get all the services and implementations listed as provides directive.
        ElementFilter.providesIn(mdle.getDirectives()).forEach(directive -> {
            TypeElement u = directive.getService();
            if (shouldDocument(u)) {
                List<? extends TypeElement> implList = directive.getImplementations();
                SortedSet<TypeElement> implSet = new TreeSet<>(utils.comparators.makeAllClassesComparator());
                implSet.addAll(implList);
                provides.put(u, implSet);
            }
        });
        // Generate the map of all services listed using @provides, and the description.
        utils.getProvidesTrees(mdle).forEach(tree -> {
            TypeElement t = ch.getServiceType(tree);
            if (t != null) {
                providesTrees.put(t, commentTagsToContent(tree, mdle, ch.getDescription(tree), false, true));
            }
        });
        // Generate the map of all services listed using @uses, and the description.
        utils.getUsesTrees(mdle).forEach(tree -> {
            TypeElement t = ch.getServiceType(tree);
            if (t != null) {
                usesTrees.put(t, commentTagsToContent(tree, mdle, ch.getDescription(tree), false, true));
            }
        });
    }

    /**
     * Returns true if the element should be documented on the module summary page.
     *
     * @param element the element to be checked
     * @return true if the element should be documented
     */
    public boolean shouldDocument(Element element) {
        return (moduleMode == ModuleMode.ALL || utils.isIncluded(element));
    }

    /**
     * Returns true if there are elements to be displayed.
     *
     * @param section set of elements
     * @return true if there are elements to be displayed
     */
    public boolean display(Set<? extends Element> section) {
        return section != null && !section.isEmpty();
    }

    /**
     * Returns true if there are elements to be displayed.
     *
     * @param section map of elements.
     * @return true if there are elements to be displayed
     */
    public boolean display(Map<? extends Element, ?> section) {
        return section != null && !section.isEmpty();
    }

    /*
     * Returns true, in API mode, if at least one type element in
     * the typeElements set is referenced by a javadoc tag in tagsMap.
     */
    private boolean displayServices(Set<TypeElement> typeElements,
                                    Map<TypeElement, Content> tagsMap) {
        return typeElements != null &&
                typeElements.stream().anyMatch(v -> displayServiceDirective(v, tagsMap));
    }

    /*
     * Returns true, in API mode, if the type element is referenced
     * from a javadoc tag in tagsMap.
     */
    private boolean displayServiceDirective(TypeElement typeElement,
                                            Map<TypeElement, Content> tagsMap) {
        return moduleMode == ModuleMode.ALL || tagsMap.containsKey(typeElement);
    }

    /**
     * Add the summary header.
     *
     * @param startMarker the marker comment
     * @param heading the heading for the section
     * @param htmltree the content tree to which the information is added
     */
    public void addSummaryHeader(Content startMarker, Content heading,
            Content htmltree) {
        htmltree.add(startMarker);
        htmltree.add(HtmlTree.HEADING(Headings.ModuleDeclaration.SUMMARY_HEADING, heading));
    }

    /**
     * Get a table, with two columns.
     *
     * @param caption the table caption
     * @param tableHeader the table header
     * @return a content object
     */
    private Table getTable2(Content caption, TableHeader tableHeader) {
        return new Table(HtmlStyle.detailsTable)
                .setCaption(caption)
                .setHeader(tableHeader)
                .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colLast);
    }

    /**
     * Get a table, with three columns, with the second column being the defining column.
     *
     * @param caption the table caption
     * @param tableHeader the table header
     * @return a content object
     */
    private Table getTable3(Content caption, TableHeader tableHeader) {
        return new Table(HtmlStyle.detailsTable)
                .setCaption(caption)
                .setHeader(tableHeader)
                .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colSecond, HtmlStyle.colLast);
    }

    @Override
    public void addModulesSummary(Content summariesList) {
        if (display(requires) || display(indirectModules)) {
            TableHeader requiresTableHeader =
                    new TableHeader(contents.modifierLabel, contents.moduleLabel,
                            contents.descriptionLabel);
            HtmlTree section = HtmlTree.SECTION(HtmlStyle.modulesSummary)
                    .setId(HtmlIds.MODULES);
            addSummaryHeader(MarkerComments.START_OF_MODULES_SUMMARY, contents.navModules, section);
            if (display(requires)) {
                String text = resources.getText("doclet.Requires_Summary");
                Content caption = Text.of(text);
                Table table = getTable3(caption, requiresTableHeader);
                addModulesList(requires, table);
                section.add(table);
            }
            // Display indirect modules table in both "api" and "all" mode.
            if (display(indirectModules)) {
                String amrText = resources.getText("doclet.Indirect_Requires_Summary");
                Content amrCaption = Text.of(amrText);
                Table amrTable = getTable3(amrCaption, requiresTableHeader);
                addModulesList(indirectModules, amrTable);
                section.add(amrTable);
            }
            summariesList.add(HtmlTree.LI(section));
        }
    }

    /**
     * Add the list of modules.
     *
     * @param mdleMap map of modules and modifiers
     * @param table the table to which the list will be added
     */
    private void addModulesList(Map<ModuleElement, Content> mdleMap, Table table) {
        for (ModuleElement m : mdleMap.keySet()) {
            Content modifiers = mdleMap.get(m);
            Content moduleLink = getModuleLink(m, Text.of(m.getQualifiedName()));
            Content moduleSummary = new ContentBuilder();
            addSummaryComment(m, moduleSummary);
            table.addRow(modifiers, moduleLink, moduleSummary);
        }
    }

    @Override
    public void addPackagesSummary(Content summariesList) {
        if (display(packages)
                || display(indirectPackages) || display(indirectOpenPackages)) {
            HtmlTree section = HtmlTree.SECTION(HtmlStyle.packagesSummary)
                    .setId(HtmlIds.PACKAGES);
            addSummaryHeader(MarkerComments.START_OF_PACKAGES_SUMMARY, contents.navPackages, section);
            if (display(packages)) {
                addPackageSummary(section);
            }
            TableHeader indirectPackagesHeader =
                    new TableHeader(contents.fromLabel, contents.packagesLabel);
            if (display(indirectPackages)) {
                String aepText = resources.getText("doclet.Indirect_Exports_Summary");
                Table aepTable = getTable2(Text.of(aepText), indirectPackagesHeader);
                addIndirectPackages(aepTable, indirectPackages);
                section.add(aepTable);
            }
            if (display(indirectOpenPackages)) {
                String aopText = resources.getText("doclet.Indirect_Opens_Summary");
                Table aopTable = getTable2(Text.of(aopText), indirectPackagesHeader);
                addIndirectPackages(aopTable, indirectOpenPackages);
                section.add(aopTable);
            }
            summariesList.add(HtmlTree.LI(section));
        }
    }

    /**
     * Add the package summary for the module.
     *
     * @param li
     */
    public void addPackageSummary(HtmlTree li) {
        Table table = new Table(HtmlStyle.summaryTable)
                .setId(HtmlIds.PACKAGE_SUMMARY_TABLE)
                .setDefaultTab(contents.getContent("doclet.All_Packages"))
                .addTab(contents.getContent("doclet.Exported_Packages_Summary"), this::isExported)
                .addTab(contents.getContent("doclet.Opened_Packages_Summary"), this::isOpened)
                .addTab(contents.getContent("doclet.Concealed_Packages_Summary"), this::isConcealed);

        // Determine whether to show the "Exported To" and "Opened To" columns,
        // based on whether such columns would provide "useful" info.
        int numExports = 0;
        int numUnqualifiedExports = 0;
        int numOpens = 0;
        int numUnqualifiedOpens = 0;

        for (PackageEntry e : packages.values()) {
            if (e.exportedTo != null) {
                numExports++;
                if (e.exportedTo.isEmpty()) {
                    numUnqualifiedExports++;
                }
            }
            if (e.openedTo != null) {
                numOpens++;
                if (e.openedTo.isEmpty()) {
                    numUnqualifiedOpens++;
                }
            }
        }

        boolean showExportedTo = numExports > 0 && (numOpens > 0   || numUnqualifiedExports < packages.size());
        boolean showOpenedTo   = numOpens > 0   && (numExports > 0 || numUnqualifiedOpens < packages.size());

        // Create the table header and column styles.
        List<Content> colHeaders = new ArrayList<>();
        List<HtmlStyle> colStyles = new ArrayList<>();
        colHeaders.add(contents.packageLabel);
        colStyles.add(HtmlStyle.colFirst);

        if (showExportedTo) {
            colHeaders.add(contents.exportedTo);
            colStyles.add(HtmlStyle.colSecond);
        }

        if (showOpenedTo) {
            colHeaders.add(contents.openedTo);
            colStyles.add(HtmlStyle.colSecond);
        }

        colHeaders.add(contents.descriptionLabel);
        colStyles.add(HtmlStyle.colLast);

        table.setHeader(new TableHeader(colHeaders).styles(colStyles))
                .setColumnStyles(colStyles);

        // Add the table rows, based on the "packages" map.
        for (Map.Entry<PackageElement, PackageEntry> e : packages.entrySet()) {
            PackageElement pkg = e.getKey();
            PackageEntry entry = e.getValue();
            List<Content> row = new ArrayList<>();
            Content pkgLinkContent = getPackageLink(pkg, getLocalizedPackageName(pkg));
            row.add(pkgLinkContent);

            if (showExportedTo) {
                row.add(getPackageExportOpensTo(entry.exportedTo));
            }
            if (showOpenedTo) {
                row.add(getPackageExportOpensTo(entry.openedTo));
            }
            Content summary = new ContentBuilder();
            addPreviewSummary(pkg, summary);
            addSummaryComment(pkg, summary);
            row.add(summary);

            table.addRow(pkg, row);
        }

        li.add(table);
        if (table.needsScript()) {
            mainBodyScript.append(table.getScript());
        }
    }

    private boolean isExported(Element e) {
        PackageEntry entry = packages.get((PackageElement) e);
        return (entry != null) && (entry.exportedTo != null);
    }

    private boolean isOpened(Element e) {
        PackageEntry entry = packages.get((PackageElement) e);
        return (entry != null) && (entry.openedTo != null);
    }

    private boolean isConcealed(Element e) {
        PackageEntry entry = packages.get((PackageElement) e);
        return (entry != null) && (entry.exportedTo == null) && (entry.openedTo == null);
    }

    private Content getPackageExportOpensTo(Set<ModuleElement> modules) {
        if (modules == null) {
            return contents.getContent("doclet.None");
        } else if (modules.isEmpty()) {
            return contents.getContent("doclet.All_Modules");
        } else {
            Content list = new ContentBuilder();
            for (ModuleElement m : modules) {
                if (!list.isEmpty()) {
                    list.add(Text.of(", "));
                }
                list.add(getModuleLink(m, Text.of(m.getQualifiedName())));
            }
            return list;
        }
    }

    /**
     * Add the indirect packages for the module being documented.
     *
     * @param table the table to which the content rows will be added
     * @param ip indirect packages to be added
     */
    public void addIndirectPackages(Table table, Map<ModuleElement, SortedSet<PackageElement>> ip) {
        for (Map.Entry<ModuleElement, SortedSet<PackageElement>> entry : ip.entrySet()) {
            ModuleElement m = entry.getKey();
            SortedSet<PackageElement> pkgList = entry.getValue();
            Content moduleLinkContent = getModuleLink(m, Text.of(m.getQualifiedName()));
            Content list = new ContentBuilder();
            String sep = "";
            for (PackageElement pkg : pkgList) {
                list.add(sep);
                list.add(getPackageLink(pkg, getLocalizedPackageName(pkg)));
                sep = " ";
            }
            table.addRow(moduleLinkContent, list);
        }
    }

    @Override
    public void addServicesSummary(Content summariesList) {

        boolean haveUses = displayServices(uses, usesTrees);
        boolean haveProvides = displayServices(provides.keySet(), providesTrees);

        if (haveProvides || haveUses) {
            HtmlTree section = HtmlTree.SECTION(HtmlStyle.servicesSummary)
                    .setId(HtmlIds.SERVICES);
            addSummaryHeader(MarkerComments.START_OF_SERVICES_SUMMARY, contents.navServices, section);
            TableHeader usesProvidesTableHeader =
                    new TableHeader(contents.typeLabel, contents.descriptionLabel);
            if (haveProvides) {
                String label = resources.getText("doclet.Provides_Summary");
                Table table = getTable2(Text.of(label), usesProvidesTableHeader);
                addProvidesList(table);
                if (!table.isEmpty()) {
                    section.add(table);
                }
            }
            if (haveUses){
                String label = resources.getText("doclet.Uses_Summary");
                Table table = getTable2(Text.of(label), usesProvidesTableHeader);
                addUsesList(table);
                if (!table.isEmpty()) {
                    section.add(table);
                }
            }
            summariesList.add(HtmlTree.LI(section));
        }
    }

    /**
     * Add the uses list for the module.
     *
     * @param table the table to which the services used will be added
     */
    public void addUsesList(Table table) {
        Content typeLinkContent;
        Content description;
        for (TypeElement t : uses) {
            if (!displayServiceDirective(t, usesTrees)) {
                continue;
            }
            typeLinkContent = getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.PACKAGE, t));
            Content summary = new ContentBuilder();
            if (display(usesTrees)) {
                description = usesTrees.get(t);
                if (description != null && !description.isEmpty()) {
                    summary.add(HtmlTree.DIV(HtmlStyle.block, description));
                } else {
                    addSummaryComment(t, summary);
                }
            } else {
                summary.add(Entity.NO_BREAK_SPACE);
            }
            table.addRow(typeLinkContent, summary);
        }
    }

    /**
     * Add the provides list for the module.
     *
     * @param table the table to which the services provided will be added
     */
    public void addProvidesList(Table table) {
        SortedSet<TypeElement> implSet;
        Content description;
        for (Map.Entry<TypeElement, SortedSet<TypeElement>> entry : provides.entrySet()) {
            TypeElement srv = entry.getKey();
            if (!displayServiceDirective(srv, providesTrees)) {
                continue;
            }
            implSet = entry.getValue();
            Content srvLinkContent = getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.PACKAGE, srv));
            Content desc = new ContentBuilder();
            if (display(providesTrees)) {
                description = providesTrees.get(srv);
                desc.add((description != null && !description.isEmpty())
                        ? HtmlTree.DIV(HtmlStyle.block, description)
                        : Entity.NO_BREAK_SPACE);
            } else {
                desc.add(Entity.NO_BREAK_SPACE);
                }
            // Only display the implementation details in the "all" mode.
            if (moduleMode == ModuleMode.ALL && !implSet.isEmpty()) {
                desc.add(new HtmlTree(TagName.BR));
                desc.add("(");
                HtmlTree implSpan = HtmlTree.SPAN(HtmlStyle.implementationLabel, contents.implementation);
                desc.add(implSpan);
                desc.add(Entity.NO_BREAK_SPACE);
                String sep = "";
                for (TypeElement impl : implSet) {
                    desc.add(sep);
                    desc.add(getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.PACKAGE, impl)));
                    sep = ", ";
                }
                desc.add(")");
            }
            table.addRow(srvLinkContent, desc);
        }
    }

    /**
     * Add the module deprecation information to the documentation tree.
     *
     * @param div the content tree to which the deprecation information will be added
     */
    public void addDeprecationInfo(Content div) {
        List<? extends DeprecatedTree> deprs = utils.getDeprecatedTrees(mdle);
        if (utils.isDeprecated(mdle)) {
            CommentHelper ch = utils.getCommentHelper(mdle);
            HtmlTree deprDiv = new HtmlTree(TagName.DIV);
            deprDiv.setStyle(HtmlStyle.deprecationBlock);
            Content deprPhrase = HtmlTree.SPAN(HtmlStyle.deprecatedLabel, getDeprecatedPhrase(mdle));
            deprDiv.add(deprPhrase);
            if (!deprs.isEmpty()) {
                List<? extends DocTree> commentTags = ch.getDescription(deprs.get(0));
                if (!commentTags.isEmpty()) {
                    addInlineDeprecatedComment(mdle, deprs.get(0), deprDiv);
                }
            }
            div.add(deprDiv);
        }
    }

    @Override
    public void addModuleDescription(Content moduleContentTree) {
        addPreviewInfo(mdle, moduleContentTree);
        if (!utils.getFullBody(mdle).isEmpty()) {
            HtmlTree tree = HtmlTree.SECTION(HtmlStyle.moduleDescription)
                    .setId(HtmlIds.MODULE_DESCRIPTION);
            addDeprecationInfo(tree);
            tree.add(MarkerComments.START_OF_MODULE_DESCRIPTION);
            addInlineComment(mdle, tree);
            addTagsInfo(mdle, tree);
            moduleContentTree.add(tree);
        }
    }

    @Override
    public void addModuleSignature(Content moduleContentTree) {
        moduleContentTree.add(new HtmlTree(TagName.HR));
        moduleContentTree.add(Signatures.getModuleSignature(mdle, this));
    }

    @Override
    public void addModuleContent(Content moduleContentTree) {
        bodyContents.addMainContent(moduleContentTree);
    }

    @Override
    public void addModuleFooter() {
        bodyContents.setFooter(getFooter());
    }

    @Override
    public void printDocument(Content contentTree) throws DocFileIOException {
        contentTree.add(bodyContents);
        printHtmlDocument(configuration.metakeywords.getMetaKeywordsForModule(mdle),
                getDescription("declaration", mdle), getLocalStylesheets(mdle), contentTree);
    }

    /**
     * Add the module package deprecation information to the documentation tree.
     *
     * @param li the content tree to which the deprecation information will be added
     * @param pkg the PackageDoc that is added
     */
    public void addPackageDeprecationInfo(Content li, PackageElement pkg) {
        if (utils.isDeprecated(pkg)) {
            List<? extends DeprecatedTree> deprs = utils.getDeprecatedTrees(pkg);
            HtmlTree deprDiv = new HtmlTree(TagName.DIV);
            deprDiv.setStyle(HtmlStyle.deprecationBlock);
            Content deprPhrase = HtmlTree.SPAN(HtmlStyle.deprecatedLabel, getDeprecatedPhrase(pkg));
            deprDiv.add(deprPhrase);
            if (!deprs.isEmpty()) {
                CommentHelper ch = utils.getCommentHelper(pkg);
                List<? extends DocTree> commentTags = ch.getDescription(deprs.get(0));
                if (!commentTags.isEmpty()) {
                    addInlineDeprecatedComment(pkg, deprs.get(0), deprDiv);
                }
            }
            li.add(deprDiv);
        }
    }
}
