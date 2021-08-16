/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html.markup;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Enum representing HTML styles, with associated entries in the stylesheet files.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @apiNote
 * Despite the name, the members of this enum provide values for the HTML {@code class} attribute,
 * and <strong>not</strong> the HTML {@code style} attribute.
 * This is to avoid confusion with the widespread use of the word "class" in the Java ecosystem,
 * and the potential for clashes with methods called {@code setClass} instead of {@code setStyle}.
 *
 * @apiNote
 * The description of some members refer to "elements".
 * This typically refers to "HTML elements", but may in some cases refer to
 * or {@link javax.lang.model.element.Element "language model elements"}.
 * The usage is made explicit when it is not clear from the surrounding context.
 *
 * @see <a href="https://html.spec.whatwg.org/#classes">WhatWG: {@code class} attribute</a>
 */
public enum HtmlStyle {
    block,
    blockList,
    circle,
    classUses,
    externalLink,
    hierarchy,
    horizontal,
    implementationLabel,
    index,
    inheritance,
    inheritedList,
    legalCopy,
    memberNameLabel,
    memberNameLink,
    nameValue,
    packages,
    packageHierarchyLabel,
    packageUses,
    serializedPackageContainer,
    sourceContainer,
    sourceLineNo,
    typeNameLabel,
    typeNameLink,

    //<editor-fold desc="navigation bar">
    //
    // The following constants are used for the main navigation bar that appears in the
    // {@code header} and {@code footer} elements on each page.

    /**
     * The class for the overall {@code div} element containing the {@code header} element for the page.
     */
    topNav,

    /**
     * The class for the element containing the information (such as the product name and version)
     * provided by the {@code -header} or {@code -footer} command line option.
     */
    aboutLanguage,

    /**
     * The class for the highlighted item in the list of navigation links, indicating
     * the current page.
     */
    // The etymology of the name is a mystery.
    navBarCell1Rev,

    /**
     * The class for the primary list of navigation links.
     */
    navList,

    /**
     * The class for the {@code div} element containing the "Search" control.
     */
    navListSearch,

    /**
     * The class for a {@code div} element containing a link to skip the navigation header.
     * The element is typically invisible, but may be used when navigating the page
     * with a screen reader.
     */
    skipNav,

    /**
     * The class for a {@code div} element containing a list of subsidiary navigation links.
     */
    subNav,

    /**
     * The class for the list of subsidiary navigation links.
     */
    subNavList,

    //</editor-fold>

    //<editor-fold desc="header (title block)">
    //
    // The following constants are used for the main "header" ("heading") that
    // provides the title for the page. This should not be confused with the
    // {@code header} element that contains the top navigation bar.

    /**
     * The class for the element that contains all of the main heading for the page.
     */
    header,

    /**
     * The class for the "module" label in the heading for a package declaration.
     */
    moduleLabelInPackage,

    /**
     * The class for the "module" label in the heading for a type declaration.
     */
    moduleLabelInType,

    /**
     * The class for the "package" label in the heading for a type declaration.
     */
    packageLabelInType,

    /**
     * The class for the element containing the label and name for the module
     * or package that precedes the main title for the declaration of a
     * package or type.
     */
    subTitle,

    /**
     * The class for the element containing the label and name for
     * the main title on a page for the declaration of a package or type.
     */
    title,

    //</editor-fold>

    //<editor-fold desc="summaries">
    //
    // The following constants are used for the HTML elements that provide
    // summary information, either of the program elements enclosed
    // by some program element, or in pages providing aggregate information
    // about similar program elements.
    // As a general rule, summaries are typically displayed as tables,
    // with rows containing the name or signature of an element, and the
    // first sentence of the description of that element. The name or signature
    // is typically linked to a corresponding "Details" section.
    //
    // Note: the "Summary" information on a module declaration page for
    // "Services" would be better characterized as "Details" information,
    // since it contains the full text of the descriptions in the
    // @provides and @uses tags, and not just the first sentence.

    /**
     * The class for the overall {@code section} element containing all the
     * different kinds of summary for the parts of the program element
     * declared on this page.
     */
    summary,

    /**
     * The class for the {@code list} element of all the different kinds of
     * summary for the parts of the program element declared on this page.
     */
    summaryList,

    /**
     * The class for the {@code section} element containing a summary of
     * the constructors for a type.
     */
    constructorSummary,

    /**
     * The class for a {@code section} element containing a summary of
     * the fields of a type.
     */
    fieldSummary,

    /**
     * The class for a {@code section} element containing the members
     * of a given kind for a type.
     */
    memberSummary,

    /**
     * The class for the {@code section} element containing a summary of
     * the methods of a type.
     */
    methodSummary,

    /**
     * The class for the {@code section} element containing a summary of
     * the module dependencies of a module.
     */
    modulesSummary,

    /**
     * The class for the {@code section} element containing a summary of
     * the nested classes of a type.
     */
    nestedClassSummary,

    /**
     * The class for the {@code section} element containing a summary of
     * the packages provided by a module,
     * <i>and</i>
     * the class for the list of packages on the "All Packages" index page.
     */
    packagesSummary,

    /**
     * The class for the {@code section} element containing a summary of
     * the properties for a type.
     */
    propertySummary,

    /**
     * The class for the {@code section} element containing a summary of
     * the services provided or used by a module.
     */
    servicesSummary,

    /**
     * The class for a {@code section} element on the "Constants Field Values" page,
     * <i>and</i>
     * the class for the {@code section} element for the enum constants of an enum class.
     */
    constantsSummary,
    //</editor-fold>

    //<editor-fold desc="details">
    //
    // The following constants are used for the details of the enclosed
    // (program) elements of an enclosing element, such as a module,
    // package or type declaration.

    /**
     * The class for the overall {@code section} element for all the details
     * about enclosed program elements.
     */
    details,

    /**
     * The class for the list of sublists containing the details for
     * the different kinds of program elements.
     */
    detailsList,

    /**
     * The class for the {@code section} element containing the details
     * for a single enclosed element.
     */
    detail,

    /**
     * The class for the list containing the details for the members
     * of a given element kind.
     */
    memberList,

    /**
     * The class for the {@code section} containing the list of details for
     * all the constructors declared in a type element.
     */
    constructorDetails,

    /**
     * The class for the {@code section} containing the list of details for
     * all the enum constants declared in a enum element.
     */
    constantDetails,

    /**
     * The class for the {@code section} containing the list of details for
     * all the fields declared in a type element.
     */
    fieldDetails,

    /**
     * The class for the {@code section} containing the list of details for
     * all the members declared in a annotation type element.
     */
    memberDetails,

    /**
     * The class for the {@code section} containing the list of details for
     * all the methods declared in a type element.
     */
    methodDetails,

    /**
     * The class for the {@code section} containing the list of details for
     * all the properties declared in a JavaFX type element.
     */
    propertyDetails,

    /**
     * The class for the list containing the {@code @see} tags of an element.
     */
    seeList,

    /**
     * The class for the list containing the {@code @see} tags of an element
     * when some of the tags have longer labels.
     */
    seeListLong,

    /**
     * The class for a {@code section} element containing details of the
     * serialized form of an element, on the "Serialized Form" page.
     */
    serializedClassDetails,

    //</editor-fold>

    //<editor-fold desc="additional details">
    //
    // The following constants are used for the additional information that may be provided
    // for a declaration, such as whether it is deprecated or is a "preview" feature.

    /**
     * The class for the "Deprecated" label in a block describing the "deprecated" status
     * of a declaration.
     */
    deprecatedLabel,

    /**
     * The class for a block describing the "deprecated" status of a declaration.
     */
    deprecationBlock,

    /**
     * The class for the details in a block describing the "deprecated" status of a declaration.
     */
    deprecationComment,

    /**
     * The class for a label indicating the element from which a description has been copied.
     */
    // This should be renamed to something less cryptic
    descfrmTypeLabel,

    /**
     * The class for a note providing information about the permitted subtypes of a
     * sealed class.
     */
    permitsNote,

    /**
     * The class for a block describing the "preview" status of a declaration.
     */
    previewBlock,

    /**
     * The class for the details in a block describing the "preview" status of a declaration.
     */
    previewComment,

    /**
     * The class for the "Preview" label in a block describing the "preview" status
     * of a declaration.
     */
    previewLabel,

    //</editor-fold>

    //<editor-fold desc="tables">
    //
    // The following constants are used for "summary" and "details" tables.
    // Most tables are summary tables, meaning that, in part, they provide links to details elsewhere.
    // A module page has details tables containing the details of the directives.

    /**
     * The class of a {@code div} element whose content should be rendered as a table
     * with two columns.
     */
    twoColumnSummary,

    /**
     * The class of a {@code div} element whose content should be rendered as a table
     * with three columns.
     */
    threeColumnSummary,

    /**
     * The class of a {@code div} element whose content should be rendered as a table
     * with four columns.
     */
    fourColumnSummary,

    /**
     * The class of a {@code div} element used to present details of a program element.
     */
    detailsTable,

    /**
     * The class of a {@code div} element used to present a summary of the enclosed
     * elements of a program element.  A {@code summaryTable} typically references
     * items in a corresponding {@link #detailsList}.
     */
    summaryTable,

    /**
     * The class of the "tab" that indicates the currently displayed contents of a table.
     * This is used when the table provides filtered views.
     */
    activeTableTab,

    /**
     * The class for the caption of a table. The caption is displayed as a single
     * inactive tab above the table.
     */
    caption,

    /**
     * The class of an element that is part of a table header.
     */
    tableHeader,

    /**
     * The class of a "tab" that indicates an alternate view of the contents of a table.
     * This is used when the table provides filtered views.
     */
    tableTab,

    /**
     * The class of the {@code div} element that contains the tabs used to select
     * the contents of the associated table to be displayed.
     */
    tableTabs,

    /**
     * The class of the cells in a table column used to display the name
     * of a constructor.
     */
    colConstructorName,

    /**
     * The class of the first column of cells in a table.
     * This is typically the "type and modifiers" column, where the type is
     * the type of a field or the return type of a method.
     */
    colFirst,

    /**
     * The class of the last column of cells in a table.
     * This is typically the "description" column, where the description is
     * the first sentence of the elements documentation comment.
     */
    colLast,

    /**
     * The class of the cells in a table column used to display the name
     * of a summary item.
     */
    colSummaryItemName,

    /**
     * The class of the cells in a table column used to display additional
     * information without any particular style.
     */
    colPlain,

    /**
     * The class of the second column of cells in a table.
     * This is typically the column that defines the name of a field or the
     * name and parameters of a method.
     */
    colSecond,

    /**
     * A class used to provide the background for the rows of a table,
     * to provide a "striped" effect. This class and {@link #oddRowColor}
     * are used on alternating rows.
     * The classes are applied dynamically when table "tabs" are used
     * to filter the set of rows to be displayed
     */
    evenRowColor,

    /**
     * A class used to provide the background for the rows of a table,
     * to provide a "striped" effect. This class and {@link #evenRowColor}
     * are used on alternating rows.
     * The classes are applied dynamically when table "tabs" are used
     * to filter the set of rows to be displayed
     */
    oddRowColor,
    //</editor-fold>

    //<editor-fold desc="documentation comments">
    //
    // The following constants are used for the components used to present the content
    // generated from documentation comments.

    /**
     * The class of the element used to present the documentation comment for a type element.
     * The content of the block tags will be in a nested element with class {@link #notes}.
     */
    classDescription,

    /**
     * The class of the element used to present the documentation comment for a module element,
     * excluding block tags.
     */
    moduleDescription,

    /**
     * The class of the element used to present the documentation comment for package element.
     * The content of the block tags will be in a nested element with class {@link #notes}.
     */
    packageDescription,

    /**
     * The class of the {@code dl} element used to present the block tags in the documentation
     * comment for a package, type or member element.
     * Additional (derived) information, such as implementation or inheritance details, may
     * also appear in this element.
     */
    notes,
    //</editor-fold>

    //<editor-fold desc="flex layout">
    //
    // The following constants are used for the components of the top-level structures for "flex" layout.

    /**
     * The class of the top-level {@code div} element used to arrange for "flex" layout in
     * a browser window. The element should contain two child elements: one with class
     * {@link #flexHeader flex-header} and one with class {@link #flexContent flex-content}.
     */
    flexBox,

    /**
     * The class of the {@code header} element within a {@link #flexBox flex-box} container.
     * The element is always displayed at the top of the viewport.
     */
    flexHeader,

    /**
     * The class of the {@code div} element within a {@link #flexBox flex-box} container
     * This element appears below the header and can be scrolled if too big for the available height.
     */
    flexContent,
    //</editor-fold>

    //<editor-fold desc="signatures">
    //
    // The following constants are used for the components of a signature of an element

    /**
     * The class of an element containing a module signature.
     */
    moduleSignature,

    /**
     * The class of an element containing a package signature.
     */
    packageSignature,

    /**
     * The class of an element containing a type signature.
     */
    typeSignature,

    /**
     * The class of an element containing a member signature.
     * The signature will contain a member name and, depending on the kind of element,
     * any of the following:
     * annotations, type parameters, modifiers, return type, parameters, and exceptions.
     */
    memberSignature,

    /**
     * The class of a {@code span} element containing any annotations in the signature of an element.
     */
    annotations,

    /**
     * The class of a {@code span} element containing any exceptions in a signature of an executable element.
     */
    exceptions,

    /**
     * The class of a {@code span} element containing the {@code extends} or {@code implements} section
     * in a signature of a type element.
     */
    extendsImplements,

    /**
     * The class of a {@code span} containing the element name in the element's signature.
     */
    elementName,

    /**
     * The class of a {@code span} containing any modifiers in the signature of an element.
     */
    modifiers,

    /**
     * The class of a {@code span} containing any parameters in the signature of an executable element.
     */
    parameters,

    /**
     * The class of a {@code span} containing the {@code permits} section of a sealed class element.
     */
    permits,

    /**
     * The class of a {@code span} containing the return type in the signature of a method element.
     */
    returnType,

    /**
     * The class of a {@code span} containing type parameters in the signature of an element,
     * used when the type parameters should reasonably be displayed inline.
     */
    typeParameters,

    /**
     * The class of a {@code span} containing type parameters in the signature of an element,
     * used when the type parameters are too long to be displayed inline.
     * @implNote
     * The threshold for choosing between {@code typeParameters} and {@code typeParametersLong}
     * is 50 characters.
     */
    typeParametersLong,
    //</editor-fold>

    //<editor-fold desc="search index and results">
    //
    // The following constants are used for items in the static and interactive search indexes.

    /**
     * The class for a link in the static "Index" pages to a custom searchable item,
     * such as defined with an {@code @index} tag.
     */
    searchTagLink,

    /**
     * The class for a custom searchable item,
     * such as defined with an {@code @index} tag.
     */
    searchTagResult,

    /**
     * The class for the separator in the list of pages given at the top of the
     * static "Index" page(s).
     */
    verticalSeparator,

    //</editor-fold>

    //<editor-fold desc="page styles for <body> elements">
    //
    // The following constants are used for the class of the {@code <body>} element
    // for the corresponding pages.

    /**
     * The class of the {@code body} element for the "All Classes" index page.
     */
    allClassesIndexPage,

    /**
     * The class of the {@code body} element for the "All Packages" index page.
     */
    allPackagesIndexPage,

    /**
     * The class of the {@code body} element for a class-declaration page.
     */
    classDeclarationPage,

    /**
     * The class of the {@code body} element for a class-use page.
     */
    classUsePage,

    /**
     * The class of the {@code body} element for the constants-summary page.
     */
    constantsSummaryPage,

    /**
     * The class of the {@code body} element for the page listing any deprecated items.
     */
    deprecatedListPage,

    /**
     * The class of the {@code body} element for the page listing any deprecated items.
     */
    deprecatedInReleasePage,

    /**
     * The class of the {@code body} element for a "doc-file" page..
     */
    docFilePage,

    /**
     * The class of the {@code body} element for the "help" page.
     */
    helpPage,

    /**
     * The class of the {@code body} element for a page in either the "single" or "split index".
     */
    indexPage,

    /**
     * The class of the {@code body} element for the top-level redirect page.
     */
    indexRedirectPage,

    /**
     * The class of the {@code body} element for a module-declaration page.
     */
    moduleDeclarationPage,

    /**
     * The class of the {@code body} element for the module-index page.
     */
    moduleIndexPage,

    /**
     * The class of the {@code body} element for the page listing new API elements.
     */
    newApiListPage,

    /**
     * The class of the {@code body} element for a package-declaration page.
     */
    packageDeclarationPage,

    /**
     * The class of the {@code body} element for the package-index page.
     */
    packageIndexPage,

    /**
     * The class of the {@code body} element for the page for the package hierarchy.
     */
    packageTreePage,

    /**
     * The class of the {@code body} element for a package-use page.
     */
    packageUsePage,

    /**
     * The class of the {@code body} element for the page listing any preview items.
     */
    previewListPage,

    /**
     * The class of the {@code body} element for the serialized-forms page.
     */
    serializedFormPage,

    /**
     * The class of the {@code body} element for a page with the source code for a class.
     */
    sourcePage,

    /**
     * The class of the {@code body} element for the system-properties page.
     */
    systemPropertiesPage,

    /**
     * The class of the {@code body} element for the page for the class hierarchy.
     */
    treePage,
    //</editor-fold>

    //<editor-fold desc="help page">
    //
    // The following constants are used for the contents of the "Help" page.

    /**
     * The class of the footnote at the bottom of the page.
     */
    helpFootnote,

    /**
     * The class of the "Note:" prefix.
     */
    helpNote,

    /**
     * The class of each subsection in the page.
     */
    helpSection,

    /**
     * The class of lists in a subsection in the page.
     */
    helpSectionList,

    /**
     * The class of the top level list for the table of contents for the page.
     */
    helpTOC("help-toc"),

    /**
     * The class of the second-level lists in the table of contents for the page.
     */
    helpSubTOC("help-subtoc");

    //</editor-fold>

    private final String cssName;

    HtmlStyle() {
        cssName = Pattern.compile("\\p{Upper}")
                .matcher(toString())
                .replaceAll(mr -> "-" + mr.group().toLowerCase(Locale.US));
    }

    HtmlStyle(String cssName) {
        this.cssName = cssName;
    }

    /**
     * Returns the CSS class name associated with this style.
     * @return the CSS class name
     */
    public String cssName() {
        return cssName;
    }
}
