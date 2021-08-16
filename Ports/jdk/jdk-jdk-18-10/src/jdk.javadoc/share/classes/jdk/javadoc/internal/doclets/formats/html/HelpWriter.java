/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import javax.lang.model.element.Element;

import jdk.javadoc.internal.doclets.formats.html.markup.BodyContents;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocLink;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;


/**
 * Generate the Help File for the generated API documentation. The help file
 * contents are helpful for browsing the generated documentation.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class HelpWriter extends HtmlDocletWriter {

    private final String[][] SEARCH_EXAMPLES = {
            {"j.l.obj", "\"java.lang.Object\""},
            {"InpStr", "\"java.io.InputStream\""},
            {"HM.cK", "\"java.util.HashMap.containsKey(Object)\""}
    };

    Content overviewLink;
    Content indexLink;
    Content allClassesLink;
    Content allPackagesLink;

    /**
     * Constructor to construct HelpWriter object.
     * @param configuration the configuration
     * @param filename File to be generated.
     */
    public HelpWriter(HtmlConfiguration configuration,
                      DocPath filename) {
        super(configuration, filename);

        // yes, INDEX is correct in the following line
        overviewLink = links.createLink(DocPaths.INDEX, resources.getText("doclet.Overview"));
        allPackagesLink = links.createLink(DocPaths.ALLPACKAGES_INDEX, resources.getText("doclet.All_Packages"));
        allClassesLink = links.createLink(DocPaths.ALLCLASSES_INDEX, resources.getText("doclet.All_Classes_And_Interfaces"));
        DocPath dp = options.splitIndex()
                ? DocPaths.INDEX_FILES.resolve(DocPaths.indexN(1))
                : DocPaths.INDEX_ALL;
        indexLink = links.createLink(dp, resources.getText("doclet.Index"));
    }

    /**
     * Construct the HelpWriter object and then use it to generate the help
     * file. The name of the generated file is "help-doc.html". The help file
     * will get generated if and only if "-helpfile" and "-nohelp" is not used
     * on the command line.
     *
     * @param configuration the configuration
     * @throws DocFileIOException if there is a problem while generating the documentation
     */
    public static void generate(HtmlConfiguration configuration) throws DocFileIOException {
        DocPath filename = DocPaths.HELP_DOC;
        HelpWriter helpgen = new HelpWriter(configuration, filename);
        helpgen.generateHelpFile();
    }

    /**
     * Generate the help file contents.
     *
     * @throws DocFileIOException if there is a problem while generating the documentation
     */
    protected void generateHelpFile() throws DocFileIOException {
        String title = resources.getText("doclet.Window_Help_title");
        HtmlTree body = getBody(getWindowTitle(title));
        ContentBuilder helpFileContent = new ContentBuilder();
        addHelpFileContents(helpFileContent);
        body.add(new BodyContents()
                .setHeader(getHeader(PageMode.HELP))
                .addMainContent(helpFileContent)
                .setFooter(getFooter()));
        printHtmlDocument(null, "help", body);
    }

    /**
     * Adds the help file contents from the resource file to the content tree. While adding the
     * help file contents it also keeps track of user options.
     *
     * The general organization is:
     * <ul>
     * <li>Heading, and TOC
     * <li>Navigation help
     * <li>Page-specific help
     * </ul>
     */
    protected void addHelpFileContents(Content contentTree) {
        HtmlTree mainTOC = new HtmlTree(TagName.UL).setStyle(HtmlStyle.helpTOC);



        contentTree.add(HtmlTree.HEADING(Headings.PAGE_TITLE_HEADING, HtmlStyle.title,
                                        getContent("doclet.help.main_heading")))
                .add(mainTOC)
                .add(new HtmlTree(TagName.HR))
                .add(getNavigationSection(mainTOC))
                .add(new HtmlTree(TagName.HR))
                .add(getPageKindSection(mainTOC))
                .add(new HtmlTree(TagName.HR))
                .add(HtmlTree.SPAN(HtmlStyle.helpFootnote,
                        getContent("doclet.help.footnote")));
    }

    @Override
    protected Navigation getNavBar(PageMode pageMode, Element element) {
        return super.getNavBar(pageMode, element)
                .setSubNavLinks(() -> List.of(
                        links.createLink(HtmlIds.HELP_NAVIGATION, contents.navHelpNavigation),
                        links.createLink(HtmlIds.HELP_PAGES, contents.navHelpPages))
                );
    }

    /**
     * Creates the navigation help, adding an entry into the main table-of-contents.
     *
     * The general organization is:
     * <ul>
     * <li>General notes
     * <li>Search
     * </ul>
     *
     * @param mainTOC the main table-of-contents
     *
     * @return the content containing the help
     */
    private Content getNavigationSection(HtmlTree mainTOC) {
        Content content = new ContentBuilder();

        Content navHeading = contents.getContent("doclet.help.navigation.head");
        HtmlTree navSection = HtmlTree.DIV(HtmlStyle.subTitle)
                .add(HtmlTree.HEADING(Headings.CONTENT_HEADING, navHeading).setId(HtmlIds.HELP_NAVIGATION))
                .add(contents.getContent("doclet.help.navigation.intro", overviewLink));
        if (options.createIndex()) {
            Content links = new ContentBuilder();
            if (!configuration.packages.isEmpty()) {
                links.add(allPackagesLink);
                links.add(", ");
            }
            links.add(allClassesLink);
            navSection.add(" ")
                    .add(contents.getContent("doclet.help.navigation.index", indexLink, links));
        }
        content.add(navSection);

        HtmlTree subTOC = new HtmlTree(TagName.UL).setStyle(HtmlStyle.helpSubTOC);

        HtmlTree section;

        // Search
        if (options.createIndex()) {
            section = newHelpSection(getContent("doclet.help.search.head"), subTOC, HtmlIds.HELP_SEARCH);
            Content searchIntro = HtmlTree.P(getContent("doclet.help.search.intro"));
            Content searchExamples = new HtmlTree(TagName.UL).setStyle(HtmlStyle.helpSectionList);
            for (String[] example : SEARCH_EXAMPLES) {
                searchExamples.add(HtmlTree.LI(
                        getContent("doclet.help.search.example",
                                HtmlTree.CODE(Text.of(example[0])), example[1])));
            }
            Content searchSpecLink = HtmlTree.A(
                    resources.getText("doclet.help.search.spec.url", configuration.getDocletVersion().feature()),
                    getContent("doclet.help.search.spec.title"));
            Content searchRefer = HtmlTree.P(getContent("doclet.help.search.refer", searchSpecLink));
            section.add(searchIntro)
                    .add(searchExamples)
                    .add(searchRefer);
            navSection.add(section);
        }

        mainTOC.add(HtmlTree.LI(new ContentBuilder(
                links.createLink(DocLink.fragment(HtmlIds.HELP_NAVIGATION.name()), navHeading),
                Text.of(": "), subTOC)));

        return content;
    }


    /**
     * Creates the page-specific help, adding an entry into the main table-of-contents.
     *
     * The general organization is:
     * <ul>
     * <li>Overview
     * <li>Declaration pages: module, package, classes
     * <li>Derived info for declarations: use and tree
     * <li>General summary info: deprecated, preview
     * <li>Detailed summary info: constant values, serialized form, system properties
     * <li>Index info: all packages, all classes, full index
     * </ul>
     *
     * @param mainTOC the main table-of-contents
     *
     * @return the content containing the help
     */
    private Content getPageKindSection(HtmlTree mainTOC) {
        Content pageKindsHeading = contents.getContent("doclet.help.page_kinds.head");
        HtmlTree pageKindsSection = HtmlTree.DIV(HtmlStyle.subTitle)
                .add(HtmlTree.HEADING(Headings.CONTENT_HEADING, pageKindsHeading).setId(HtmlIds.HELP_PAGES))
                .add(contents.getContent("doclet.help.page_kinds.intro"));

        HtmlTree subTOC = new HtmlTree(TagName.UL).setStyle(HtmlStyle.helpSubTOC);

        HtmlTree section;

        // Overview
        if (options.createOverview()) {
            section = newHelpSection(contents.overviewLabel, PageMode.OVERVIEW, subTOC);
            String overviewKey = configuration.showModules
                    ? "doclet.help.overview.modules.body"
                    : "doclet.help.overview.packages.body";
            section.add(HtmlTree.P(getContent(overviewKey, overviewLink)));
            pageKindsSection.add(section);
        }

        // Module
        if (configuration.showModules) {
            section = newHelpSection(contents.moduleLabel, PageMode.MODULE, subTOC);
            Content moduleIntro = getContent("doclet.help.module.intro");
            Content modulePara = HtmlTree.P(moduleIntro);
            section.add(modulePara)
                    .add(newHelpSectionList(
                            contents.packagesLabel,
                            contents.modulesLabel,
                            contents.servicesLabel));
            pageKindsSection.add(section);
        }

        // Package
        section = newHelpSection(contents.packageLabel, PageMode.PACKAGE, subTOC)
                .add(HtmlTree.P(getContent("doclet.help.package.intro")))
                .add(newHelpSectionList(
                        contents.interfaces,
                        contents.classes,
                        contents.enums,
                        contents.exceptions,
                        contents.errors,
                        contents.annotationTypes));
        pageKindsSection.add(section);

        // Class/interface
        Content notes = new ContentBuilder(
                HtmlTree.SPAN(HtmlStyle.helpNote, getContent("doclet.help.class_interface.note")),
                Text.of(" "),
                getContent("doclet.help.class_interface.anno"),
                Text.of(" "),
                getContent("doclet.help.class_interface.enum"),
                Text.of(" "),
                getContent("doclet.help.class_interface.record"),
                Text.of(" "),
                getContent("doclet.help.class_interface.property"));

        section = newHelpSection(getContent("doclet.help.class_interface.head"), PageMode.CLASS, subTOC)
                .add(HtmlTree.P(getContent("doclet.help.class_interface.intro")))
                .add(newHelpSectionList(
                        getContent("doclet.help.class_interface.inheritance_diagram"),
                        getContent("doclet.help.class_interface.subclasses"),
                        getContent("doclet.help.class_interface.subinterfaces"),
                        getContent("doclet.help.class_interface.implementations"),
                        getContent("doclet.help.class_interface.declaration"),
                        getContent("doclet.help.class_interface.description")))
                .add(new HtmlTree(TagName.BR))
                .add(newHelpSectionList(
                        contents.nestedClassSummary,
                        contents.enumConstantSummary,
                        contents.fieldSummaryLabel,
                        contents.propertySummaryLabel,
                        contents.constructorSummaryLabel,
                        contents.methodSummary,
                        contents.annotateTypeRequiredMemberSummaryLabel,
                        contents.annotateTypeOptionalMemberSummaryLabel))
                .add(new HtmlTree(TagName.BR))
                .add(newHelpSectionList(
                        contents.enumConstantDetailLabel,
                        contents.fieldDetailsLabel,
                        contents.propertyDetailsLabel,
                        contents.constructorDetailsLabel,
                        contents.methodDetailLabel,
                        contents.annotationTypeMemberDetail))
                .add(HtmlTree.P(notes))
                .add(HtmlTree.P(getContent("doclet.help.class_interface.member_order")));
        pageKindsSection.add(section);

        section = newHelpSection(getContent("doclet.help.other_files.head"), PageMode.DOC_FILE, subTOC)
                .add(HtmlTree.P(getContent("doclet.help.other_files.body")));
        pageKindsSection.add(section);

        // Class Use
        if (options.classUse()) {
            section = newHelpSection(getContent("doclet.help.use.head"), PageMode.USE, subTOC)
                    .add(HtmlTree.P(getContent("doclet.help.use.body")));
            pageKindsSection.add(section);
        }

        // Tree
        if (options.createTree()) {
            section = newHelpSection(getContent("doclet.help.tree.head"), PageMode.TREE, subTOC);
            Content treeIntro = getContent("doclet.help.tree.intro",
                    links.createLink(DocPaths.OVERVIEW_TREE, resources.getText("doclet.Class_Hierarchy")),
                    HtmlTree.CODE(Text.of("java.lang.Object")));
            section.add(HtmlTree.P(treeIntro))
                    .add(newHelpSectionList(
                            getContent("doclet.help.tree.overview"),
                            getContent("doclet.help.tree.package")));
            pageKindsSection.add(section);
        }

        // Preview
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.PREVIEW)) {
            section = newHelpSection(contents.previewAPI, PageMode.PREVIEW, subTOC);
            Content previewBody = getContent("doclet.help.preview.body",
                    links.createLink(DocPaths.PREVIEW_LIST, contents.previewAPI));
            section.add(HtmlTree.P(previewBody));
            pageKindsSection.add(section);
        }

        // New
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.NEW)) {
            section = newHelpSection(contents.newAPI, PageMode.NEW, subTOC);
            Content newBody = getContent("doclet.help.new.body",
                    links.createLink(DocPaths.NEW_LIST, contents.newAPI));
            section.add(HtmlTree.P(newBody));
            pageKindsSection.add(section);
        }

        // Deprecated
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.DEPRECATED)) {
            section = newHelpSection(contents.deprecatedAPI, PageMode.DEPRECATED, subTOC);
            Content deprBody = getContent("doclet.help.deprecated.body",
                    links.createLink(DocPaths.DEPRECATED_LIST, resources.getText("doclet.Deprecated_API")));
            section.add(HtmlTree.P(deprBody));
            pageKindsSection.add(section);
        }

        // Constant Field Values
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.CONSTANT_VALUES)) {
            section = newHelpSection(contents.constantsSummaryTitle, PageMode.CONSTANT_VALUES, subTOC);
            Content constantsBody = getContent("doclet.help.constants.body",
                    links.createLink(DocPaths.CONSTANT_VALUES, resources.getText("doclet.Constants_Summary")));
            section.add(HtmlTree.P(constantsBody));
            pageKindsSection.add(section);
        }

        // Serialized Form
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.SERIALIZED_FORM)) {
            section = newHelpSection(contents.serializedForm, PageMode.SERIALIZED_FORM, subTOC)
                    .add(HtmlTree.P(getContent("doclet.help.serial_form.body")));
            pageKindsSection.add(section);
        }

        // System Properties
        if (configuration.conditionalPages.contains(HtmlConfiguration.ConditionalPage.SYSTEM_PROPERTIES)) {
            section = newHelpSection(contents.systemPropertiesLabel, PageMode.SYSTEM_PROPERTIES, subTOC);
            Content sysPropsBody = getContent("doclet.help.systemProperties.body",
                    links.createLink(DocPaths.SYSTEM_PROPERTIES, resources.getText("doclet.systemProperties")));
            section.add(HtmlTree.P(sysPropsBody));
            pageKindsSection.add(section);
        }

        // Index
        if (options.createIndex()) {
            if (!configuration.packages.isEmpty()) {
                section = newHelpSection(getContent("doclet.help.all_packages.head"), PageMode.ALL_PACKAGES, subTOC)
                        .add(HtmlTree.P(getContent("doclet.help.all_packages.body", allPackagesLink)));
                pageKindsSection.add(section);
            }

            section = newHelpSection(getContent("doclet.help.all_classes.head"), PageMode.ALL_CLASSES, subTOC)
                    .add(HtmlTree.P(getContent("doclet.help.all_classes.body", allClassesLink)));
            pageKindsSection.add(section);

            Content links = new ContentBuilder();
            if (!configuration.packages.isEmpty()) {
                links.add(allPackagesLink);
                links.add(", ");
            }
            links.add(allClassesLink);
            section = newHelpSection(getContent("doclet.help.index.head"), PageMode.INDEX, subTOC)
                    .add(HtmlTree.P(getContent("doclet.help.index.body", indexLink, links)));
            pageKindsSection.add(section);
        }

        mainTOC.add(HtmlTree.LI(new ContentBuilder(
                links.createLink(DocLink.fragment(HtmlIds.HELP_PAGES.name()), pageKindsHeading),
                Text.of(": "), subTOC)));

        return pageKindsSection;
    }

    private Content getContent(String key) {
        return contents.getContent(key);
    }

    private Content getContent(String key, Object arg) {
        return contents.getContent(key, arg);
    }

    private Content getContent(String key, Object arg1, Object arg2) {
        return contents.getContent(key, arg1, arg2);
    }

    private HtmlTree newHelpSection(Content headingContent, HtmlTree toc, HtmlId id) {
        Content link = links.createLink(DocLink.fragment(id.name()), headingContent);
        toc.add(HtmlTree.LI(link));

        return HtmlTree.SECTION(HtmlStyle.helpSection,
                HtmlTree.HEADING(Headings.SUB_HEADING, headingContent))
                .setId(id);
    }

    private HtmlTree newHelpSection(Content headingContent, Navigation.PageMode pm, HtmlTree toc) {
        return newHelpSection(headingContent, toc, htmlIds.forPage(pm));
    }

    private HtmlTree newHelpSectionList(Content first, Content... rest) {
        HtmlTree list = HtmlTree.UL(HtmlStyle.helpSectionList, HtmlTree.LI(first));
        List.of(rest).forEach(i -> list.add(HtmlTree.LI(i)));
        return list;
    }
}
