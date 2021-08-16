/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlAttr;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Links;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocFile;
import jdk.javadoc.internal.doclets.toolkit.util.DocLink;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Factory for navigation bar.
 *
 * <p>
 * <b>This is NOT part of any supported API. If you write code that depends on this, you do so at
 * your own risk. This code and its internal interfaces are subject to change or deletion without
 * notice.</b>
 */
public class Navigation {

    private final HtmlConfiguration configuration;
    private final HtmlOptions options;
    private final Element element;
    private final Contents contents;
    private final HtmlIds htmlIds;
    private final DocPath path;
    private final DocPath pathToRoot;
    private final Links links;
    private final PageMode documentedPage;
    private Content navLinkModule;
    private Content navLinkPackage;
    private Content navLinkClass;
    private Content userHeader;
    private final String rowListTitle;
    private final Content searchLabel;
    private SubNavLinks subNavLinks;

    public enum PageMode {
        ALL_CLASSES,
        ALL_PACKAGES,
        CLASS,
        CONSTANT_VALUES,
        DEPRECATED,
        DOC_FILE,
        HELP,
        INDEX,
        MODULE,
        NEW,
        OVERVIEW,
        PACKAGE,
        PREVIEW,
        SERIALIZED_FORM,
        SYSTEM_PROPERTIES,
        TREE,
        USE;
    }

    /**
     * An interface to provide links for the sub-navigation area.
     */
    public interface SubNavLinks {
        /**
         * {@return a list of links to display in the sub-navigation area}
         * Links should be wrapped in {@code HtmlTree.LI} elements as they are
         * displayed within an unordered list.
         */
        List<Content> getSubNavLinks();
    }

    /**
     * Creates a {@code Navigation} object for a specific file, to be written in a specific HTML
     * version.
     *
     * @param element element being documented. null if its not an element documentation page
     * @param configuration the configuration object
     * @param page the kind of page being documented
     * @param path the DocPath object
     */
    public Navigation(Element element, HtmlConfiguration configuration, PageMode page, DocPath path) {
        this.configuration = configuration;
        this.options = configuration.getOptions();
        this.element = element;
        this.contents = configuration.getContents();
        this.htmlIds = configuration.htmlIds;
        this.documentedPage = page;
        this.path = path;
        this.pathToRoot = path.parent().invert();
        this.links = new Links(path);
        this.rowListTitle = configuration.getDocResources().getText("doclet.Navigation");
        this.searchLabel = contents.getContent("doclet.search");
    }

    public Navigation setNavLinkModule(Content navLinkModule) {
        this.navLinkModule = navLinkModule;
        return this;
    }

    public Navigation setNavLinkPackage(Content navLinkPackage) {
        this.navLinkPackage = navLinkPackage;
        return this;
    }

    public Navigation setNavLinkClass(Content navLinkClass) {
        this.navLinkClass = navLinkClass;
        return this;
    }

    public Navigation setUserHeader(Content userHeader) {
        this.userHeader = userHeader;
        return this;
    }

    public Navigation setSubNavLinks(SubNavLinks subNavLinks) {
        this.subNavLinks = subNavLinks;
        return this;
    }

    /**
     * Adds the links for the main navigation.
     *
     * @param tree the content tree to which the main navigation will added
     */
    private void addMainNavLinks(Content tree) {
        switch (documentedPage) {
            case OVERVIEW:
                addActivePageLink(tree, contents.overviewLabel, options.createOverview());
                addModuleLink(tree);
                addPackageLink(tree);
                addPageLabel(tree, contents.classLabel, true);
                addPageLabel(tree, contents.useLabel, options.classUse());
                addTreeLink(tree);
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            case MODULE:
                addOverviewLink(tree);
                addActivePageLink(tree, contents.moduleLabel, configuration.showModules);
                addPackageLink(tree);
                addPageLabel(tree, contents.classLabel, true);
                addPageLabel(tree, contents.useLabel, options.classUse());
                addTreeLink(tree);
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            case PACKAGE:
                addOverviewLink(tree);
                addModuleOfElementLink(tree);
                addActivePageLink(tree, contents.packageLabel, true);
                addPageLabel(tree, contents.classLabel, true);
                if (options.classUse()) {
                    addContentToTree(tree, links.createLink(DocPaths.PACKAGE_USE,
                            contents.useLabel, ""));
                }
                if (options.createTree()) {
                    addContentToTree(tree, links.createLink(DocPaths.PACKAGE_TREE,
                            contents.treeLabel, ""));
                }
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            case CLASS:
                addOverviewLink(tree);
                addModuleOfElementLink(tree);
                addPackageSummaryLink(tree);
                addActivePageLink(tree, contents.classLabel, true);
                if (options.classUse()) {
                    addContentToTree(tree, links.createLink(DocPaths.CLASS_USE.resolve(path.basename()),
                            contents.useLabel));
                }
                if (options.createTree()) {
                    addContentToTree(tree, links.createLink(DocPaths.PACKAGE_TREE,
                            contents.treeLabel, ""));
                }
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            case USE:
                addOverviewLink(tree);
                addModuleOfElementLink(tree);
                if (element instanceof PackageElement) {
                    addPackageSummaryLink(tree);
                    addPageLabel(tree, contents.classLabel, true);
                } else {
                    addPackageOfElementLink(tree);
                    addContentToTree(tree, navLinkClass);
                }
                addActivePageLink(tree, contents.useLabel, options.classUse());
                if (element instanceof PackageElement) {
                    addContentToTree(tree, links.createLink(DocPaths.PACKAGE_TREE, contents.treeLabel));
                } else {
                    addContentToTree(tree, configuration.utils.isEnclosingPackageIncluded((TypeElement) element)
                            ? links.createLink(DocPath.parent.resolve(DocPaths.PACKAGE_TREE), contents.treeLabel)
                            : links.createLink(pathToRoot.resolve(DocPaths.OVERVIEW_TREE), contents.treeLabel));
                }
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            case TREE:
                addOverviewLink(tree);
                if (element == null) {
                    addPageLabel(tree, contents.moduleLabel, configuration.showModules);
                    addPageLabel(tree, contents.packageLabel, true);
                } else {
                    addModuleOfElementLink(tree);
                    addPackageSummaryLink(tree);
                }
                addPageLabel(tree, contents.classLabel, true);
                addPageLabel(tree, contents.useLabel, options.classUse());
                addActivePageLink(tree, contents.treeLabel, options.createTree());
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            case DEPRECATED:
            case INDEX:
            case HELP:
            case PREVIEW:
            case NEW:
                addOverviewLink(tree);
                addModuleLink(tree);
                addPackageLink(tree);
                addPageLabel(tree, contents.classLabel, true);
                addPageLabel(tree, contents.useLabel, options.classUse());
                addTreeLink(tree);
                if (documentedPage == PageMode.PREVIEW) {
                    addActivePageLink(tree, contents.previewLabel,
                            configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.PREVIEW));
                } else {
                    addPreviewLink(tree);
                }
                if (documentedPage == PageMode.NEW) {
                    addActivePageLink(tree, contents.newLabel,
                            configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.NEW));
                } else {
                    addNewLink(tree);
                }
                if (documentedPage == PageMode.DEPRECATED) {
                    addActivePageLink(tree, contents.deprecatedLabel,
                            configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.DEPRECATED));
                } else {
                    addDeprecatedLink(tree);
                }
                if (documentedPage == PageMode.INDEX) {
                    addActivePageLink(tree, contents.indexLabel, options.createIndex());
                } else {
                    addIndexLink(tree);
                }
                if (documentedPage == PageMode.HELP) {
                    addActivePageLink(tree, contents.helpLabel, !options.noHelp());
                } else {
                    addHelpLink(tree);
                }
                break;
            case ALL_CLASSES:
            case ALL_PACKAGES:
            case CONSTANT_VALUES:
            case SERIALIZED_FORM:
            case SYSTEM_PROPERTIES:
                addOverviewLink(tree);
                addModuleLink(tree);
                addPackageLink(tree);
                addPageLabel(tree, contents.classLabel, true);
                addPageLabel(tree, contents.useLabel, options.classUse());
                addTreeLink(tree);
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            case DOC_FILE:
                addOverviewLink(tree);
                addModuleOfElementLink(tree);
                addContentToTree(tree, navLinkPackage);
                addPageLabel(tree, contents.classLabel, true);
                addPageLabel(tree, contents.useLabel, options.classUse());
                addTreeLink(tree);
                addPreviewLink(tree);
                addNewLink(tree);
                addDeprecatedLink(tree);
                addIndexLink(tree);
                addHelpLink(tree);
                break;
            default:
                break;
        }
    }

    /**
     * Adds the summary links to the sub-navigation.
     *
     * @param tree the content tree to which the sub-navigation will added
     */
    private void addSummaryLinks(Content tree) {
        switch (documentedPage) {
            case MODULE, PACKAGE, CLASS, HELP -> {
                List<? extends Content> listContents = subNavLinks.getSubNavLinks()
                        .stream().map(HtmlTree::LI).toList();
                if (!listContents.isEmpty()) {
                    tree.add(HtmlTree.LI(switch (documentedPage) {
                        case MODULE -> contents.moduleSubNavLabel;
                        case PACKAGE -> contents.packageSubNavLabel;
                        case CLASS -> contents.summaryLabel;
                        case HELP -> contents.helpSubNavLabel;
                        default -> Text.EMPTY;
                    }).add(Entity.NO_BREAK_SPACE));
                    addListToNav(listContents, tree);
                }
            }
        }
    }

    /**
     * Adds the detail links to sub-navigation.
     *
     * @param tree the content tree to which the links will be added
     */
    private void addDetailLinks(Content tree) {
        if (documentedPage == PageMode.CLASS) {
            List<Content> listContents = new ArrayList<>();
            VisibleMemberTable vmt = configuration.getVisibleMemberTable((TypeElement) element);
            if (element.getKind() == ElementKind.ANNOTATION_TYPE) {
                // Handle annotation interfaces separately as required and optional elements
                // share a combined details section.
                addTypeDetailLink(FIELDS, !vmt.getVisibleMembers(FIELDS).isEmpty(), listContents);
                boolean hasAnnotationElements =
                        !vmt.getVisibleMembers(ANNOTATION_TYPE_MEMBER_OPTIONAL).isEmpty()
                                || !vmt.getVisibleMembers(ANNOTATION_TYPE_MEMBER_REQUIRED).isEmpty();
                addTypeDetailLink(ANNOTATION_TYPE_MEMBER_REQUIRED, hasAnnotationElements, listContents);
            } else {
                Set<VisibleMemberTable.Kind> detailSet = VisibleMemberTable.Kind.forDetailsOf(element.getKind());
                for (VisibleMemberTable.Kind kind : detailSet) {
                    addTypeDetailLink(kind, !vmt.getVisibleMembers(kind).isEmpty(), listContents);
                }
            }
            if (!listContents.isEmpty()) {
                Content li = HtmlTree.LI(contents.detailLabel);
                li.add(Entity.NO_BREAK_SPACE);
                tree.add(li);
                addListToNav(listContents, tree);
            }
        }
    }

    /**
     * Adds the navigation Type detail link.
     *
     * @param kind the kind of member being documented
     * @param link true if the members are listed and need to be linked
     * @param listContents the list of contents to which the detail will be added.
     */
    protected void addTypeDetailLink(VisibleMemberTable.Kind kind, boolean link, List<Content> listContents) {
        addContentToList(listContents, switch (kind) {
            case CONSTRUCTORS -> links.createLink(HtmlIds.CONSTRUCTOR_DETAIL, contents.navConstructor, link);
            case ENUM_CONSTANTS -> links.createLink(HtmlIds.ENUM_CONSTANT_DETAIL, contents.navEnum, link);
            case FIELDS -> links.createLink(HtmlIds.FIELD_DETAIL, contents.navField, link);
            case METHODS -> links.createLink(HtmlIds.METHOD_DETAIL, contents.navMethod, link);
            case PROPERTIES -> links.createLink(HtmlIds.PROPERTY_DETAIL, contents.navProperty, link);
            case ANNOTATION_TYPE_MEMBER_REQUIRED,
                 ANNOTATION_TYPE_MEMBER_OPTIONAL ->
                    links.createLink(HtmlIds.ANNOTATION_TYPE_ELEMENT_DETAIL,
                            contents.navAnnotationTypeMember, link);
            default -> Text.EMPTY;
        });
    }

    private void addContentToList(List<Content> listContents, Content tree) {
        listContents.add(HtmlTree.LI(tree));
    }

    private void addContentToTree(Content tree, Content content) {
        tree.add(HtmlTree.LI(content));
    }

    private void addListToNav(List<? extends Content> listContents, Content tree) {
        int count = 0;
        for (Content liContent : listContents) {
            if (count < listContents.size() - 1) {
                liContent.add(Entity.NO_BREAK_SPACE);
                liContent.add("|");
                liContent.add(Entity.NO_BREAK_SPACE);
            }
            tree.add(liContent);
            count++;
        }
    }

    private void addActivePageLink(Content tree, Content label, boolean display) {
        if (display) {
            tree.add(HtmlTree.LI(HtmlStyle.navBarCell1Rev, label));
        }
    }

    private void addPageLabel(Content tree, Content label, boolean display) {
        if (display) {
            tree.add(HtmlTree.LI(label));
        }
    }

    private void addOverviewLink(Content tree) {
        if (options.createOverview()) {
            tree.add(HtmlTree.LI(links.createLink(pathToRoot.resolve(DocPaths.INDEX),
                    contents.overviewLabel, "")));
        }
    }

    private void addModuleLink(Content tree) {
        if (configuration.showModules) {
            if (configuration.modules.size() == 1) {
                ModuleElement mdle = configuration.modules.first();
                boolean included = configuration.utils.isIncluded(mdle);
                tree.add(HtmlTree.LI((included)
                        ? links.createLink(pathToRoot.resolve(configuration.docPaths.moduleSummary(mdle)), contents.moduleLabel, "")
                        : contents.moduleLabel));
            } else if (!configuration.modules.isEmpty()) {
                addPageLabel(tree, contents.moduleLabel, true);
            }
        }
    }

    private void addModuleOfElementLink(Content tree) {
        if (configuration.showModules) {
            tree.add(HtmlTree.LI(navLinkModule));
        }
    }

    private void addPackageLink(Content tree) {
        if (configuration.packages.size() == 1) {
            PackageElement packageElement = configuration.packages.first();
            boolean included = packageElement != null && configuration.utils.isIncluded(packageElement);
            if (!included) {
                for (PackageElement p : configuration.packages) {
                    if (p.equals(packageElement)) {
                        included = true;
                        break;
                    }
                }
            }
            if (included || packageElement == null) {
                tree.add(HtmlTree.LI(links.createLink(
                        pathToRoot.resolve(configuration.docPaths.forPackage(packageElement).resolve(DocPaths.PACKAGE_SUMMARY)),
                        contents.packageLabel)));
            } else {
                DocLink crossPkgLink = configuration.extern.getExternalLink(
                        packageElement, pathToRoot, DocPaths.PACKAGE_SUMMARY.getPath());
                if (crossPkgLink != null) {
                    tree.add(HtmlTree.LI(links.createLink(crossPkgLink, contents.packageLabel)));
                } else {
                    tree.add(HtmlTree.LI(contents.packageLabel));
                }
            }
        } else if (!configuration.packages.isEmpty()) {
            addPageLabel(tree, contents.packageLabel, true);
        }
    }

    private void addPackageOfElementLink(Content tree) {
        tree.add(HtmlTree.LI(links.createLink(DocPath.parent.resolve(DocPaths.PACKAGE_SUMMARY),
                contents.packageLabel)));
    }

    private void addPackageSummaryLink(Content tree) {
        tree.add(HtmlTree.LI(links.createLink(DocPaths.PACKAGE_SUMMARY, contents.packageLabel)));
    }

    private void addTreeLink(Content tree) {
        if (options.createTree()) {
            List<PackageElement> packages = new ArrayList<>(configuration.getSpecifiedPackageElements());
            DocPath docPath = packages.size() == 1 && configuration.getSpecifiedTypeElements().isEmpty()
                    ? pathToRoot.resolve(configuration.docPaths.forPackage(packages.get(0)).resolve(DocPaths.PACKAGE_TREE))
                    : pathToRoot.resolve(DocPaths.OVERVIEW_TREE);
            tree.add(HtmlTree.LI(links.createLink(docPath, contents.treeLabel, "")));
        }
    }

    private void addDeprecatedLink(Content tree) {
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.DEPRECATED)) {
            tree.add(HtmlTree.LI(links.createLink(pathToRoot.resolve(DocPaths.DEPRECATED_LIST),
                    contents.deprecatedLabel, "")));
        }
    }

    private void addPreviewLink(Content tree) {
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.PREVIEW)) {
            tree.add(HtmlTree.LI(links.createLink(pathToRoot.resolve(DocPaths.PREVIEW_LIST),
                    contents.previewLabel, "")));
        }
    }

    private void addNewLink(Content tree) {
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.NEW)) {
            tree.add(HtmlTree.LI(links.createLink(pathToRoot.resolve(DocPaths.NEW_LIST),
                    contents.newLabel, "")));
        }
    }

    private void addIndexLink(Content tree) {
        if (options.createIndex()) {
            tree.add(HtmlTree.LI(links.createLink(pathToRoot.resolve(
                    (options.splitIndex()
                            ? DocPaths.INDEX_FILES.resolve(DocPaths.indexN(1))
                            : DocPaths.INDEX_ALL)),
                    contents.indexLabel, "")));
        }
    }

    private void addHelpLink(Content tree) {
        if (!options.noHelp()) {
            String helpfile = options.helpFile();
            DocPath helpfilenm;
            if (helpfile.isEmpty()) {
                helpfilenm = DocPaths.HELP_DOC;
            } else {
                DocFile file = DocFile.createFileForInput(configuration, helpfile);
                helpfilenm = DocPath.create(file.getName());
            }
            tree.add(HtmlTree.LI(links.createLink(
                    new DocLink(pathToRoot.resolve(helpfilenm), htmlIds.forPage(documentedPage).name()),
                    contents.helpLabel, "")));
        }
    }

    private void addSearch(Content tree) {
        String search = "search";
        String reset = "reset";
        HtmlTree inputText = HtmlTree.INPUT("text", HtmlIds.SEARCH_INPUT, search);
        HtmlTree inputReset = HtmlTree.INPUT(reset, HtmlIds.RESET_BUTTON, reset);
        HtmlTree searchDiv = HtmlTree.DIV(HtmlStyle.navListSearch,
                HtmlTree.LABEL(HtmlIds.SEARCH_INPUT.name(), searchLabel));
        searchDiv.add(inputText);
        searchDiv.add(inputReset);
        tree.add(searchDiv);
    }

    /**
     * Returns the navigation content.
     *
     * @return the navigation content
     */
    public Content getContent() {
        if (options.noNavbar()) {
            return new ContentBuilder();
        }
        Content tree = HtmlTree.NAV();

        HtmlTree navDiv = new HtmlTree(TagName.DIV);
        Content skipNavLinks = contents.getContent("doclet.Skip_navigation_links");
        tree.add(MarkerComments.START_OF_TOP_NAVBAR);
        navDiv.setStyle(HtmlStyle.topNav)
                .setId(HtmlIds.NAVBAR_TOP)
                .add(HtmlTree.DIV(HtmlStyle.skipNav,
                        links.createLink(HtmlIds.SKIP_NAVBAR_TOP, skipNavLinks,
                                skipNavLinks.toString())));
        Content aboutContent = userHeader;
        boolean addSearch = options.createIndex();

        Content aboutDiv = HtmlTree.DIV(HtmlStyle.aboutLanguage, aboutContent);
        navDiv.add(aboutDiv);
        HtmlTree navList = new HtmlTree(TagName.UL)
                .setId(HtmlIds.NAVBAR_TOP_FIRSTROW)
                .setStyle(HtmlStyle.navList)
                .put(HtmlAttr.TITLE, rowListTitle);
        addMainNavLinks(navList);
        navDiv.add(navList);
        tree.add(navDiv);

        HtmlTree subDiv = new HtmlTree(TagName.DIV).setStyle(HtmlStyle.subNav);

        HtmlTree div = new HtmlTree(TagName.DIV);
        // Add the summary links if present.
        HtmlTree ulNavSummary = new HtmlTree(TagName.UL).setStyle(HtmlStyle.subNavList);
        addSummaryLinks(ulNavSummary);
        div.add(ulNavSummary);
        // Add the detail links if present.
        HtmlTree ulNavDetail = new HtmlTree(TagName.UL).setStyle(HtmlStyle.subNavList);
        addDetailLinks(ulNavDetail);
        div.add(ulNavDetail);
        subDiv.add(div);

        if (addSearch) {
            addSearch(subDiv);
        }
        tree.add(subDiv);

        tree.add(MarkerComments.END_OF_TOP_NAVBAR);
        tree.add(HtmlTree.SPAN(HtmlStyle.skipNav, HtmlTree.EMPTY)
                .setId(HtmlIds.SKIP_NAVBAR_TOP));

        return tree;
    }
}
