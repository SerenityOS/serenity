/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.SortedSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlId;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;
import jdk.javadoc.internal.doclets.toolkit.util.SummaryAPIListBuilder;
import jdk.javadoc.internal.doclets.toolkit.util.SummaryAPIListBuilder.SummaryElementKind;

/**
 * Base class for generating a summary page that lists elements with a common characteristic,
 * such as deprecated elements, preview elements, and so on.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SummaryListWriter<L extends SummaryAPIListBuilder> extends SubWriterHolderWriter {

    private String getHeadingKey(SummaryElementKind kind) {
        return switch (kind) {
            case MODULE -> "doclet.Modules";
            case PACKAGE -> "doclet.Packages";
            case INTERFACE -> "doclet.Interfaces";
            case CLASS -> "doclet.Classes";
            case ENUM -> "doclet.Enums";
            case EXCEPTION -> "doclet.Exceptions";
            case ERROR -> "doclet.Errors";
            case ANNOTATION_TYPE -> "doclet.Annotation_Types";
            case FIELD -> "doclet.Fields";
            case METHOD -> "doclet.Methods";
            case CONSTRUCTOR -> "doclet.Constructors";
            case ENUM_CONSTANT -> "doclet.Enum_Constants";
            case ANNOTATION_TYPE_MEMBER -> "doclet.Annotation_Type_Members";
            case RECORD_CLASS -> "doclet.RecordClasses";
        };
    }

    private String getHeaderKey(SummaryElementKind kind) {
        return switch (kind) {
            case MODULE -> "doclet.Module";
            case PACKAGE -> "doclet.Package";
            case INTERFACE -> "doclet.Interface";
            case CLASS -> "doclet.Class";
            case ENUM -> "doclet.Enum";
            case EXCEPTION -> "doclet.Exceptions";
            case ERROR -> "doclet.Errors";
            case ANNOTATION_TYPE -> "doclet.AnnotationType";
            case FIELD -> "doclet.Field";
            case METHOD -> "doclet.Method";
            case CONSTRUCTOR -> "doclet.Constructor";
            case ENUM_CONSTANT -> "doclet.Enum_Constant";
            case ANNOTATION_TYPE_MEMBER -> "doclet.Annotation_Type_Member";
            case RECORD_CLASS -> "doclet.RecordClass";
        };
    }

    private final PageMode pageMode;
    private final String description;
    private final Content headContent;
    private final String titleKey;

    /**
     * Constructor.
     *
     * @param configuration the configuration for this doclet
     * @param filename the file to be generated
     * @param pageMode page mode to use
     * @param description page description
     * @param headContent page heading content
     * @param titleKey page title resource key
     */

    public SummaryListWriter(HtmlConfiguration configuration, DocPath filename,
                             PageMode pageMode, String description,
                             Content headContent, String titleKey) {
        super(configuration, filename);
        this.pageMode = pageMode;
        this.description = description;
        this.headContent = headContent;
        this.titleKey = titleKey;
    }

    /**
     * Generate the API summary.
     *
     * @param summaryapi list of API summary built already.
     * @throws DocFileIOException if there is a problem writing the summary list
     */
    protected void generateSummaryListFile(L summaryapi)
            throws DocFileIOException {
        HtmlTree body = getHeader();
        bodyContents.addMainContent(getContentsList(summaryapi));
        Content content = new ContentBuilder();
        addExtraSection(summaryapi, content);
        for (SummaryElementKind kind : SummaryElementKind.values()) {
            if (summaryapi.hasDocumentation(kind)) {
                addSummaryAPI(summaryapi.getSet(kind), HtmlIds.forSummaryKind(kind),
                            getHeadingKey(kind), getHeaderKey(kind), content);
            }
        }
        bodyContents.addMainContent(content);
        bodyContents.setFooter(getFooter());
        body.add(bodyContents);
        printHtmlDocument(null, description, body);
    }

    /**
     * Add the index link.
     *
     * @param id the id for the link
     * @param headingKey
     * @param contentTree the content tree to which the index link will be added
     */
    protected void addIndexLink(HtmlId id, String headingKey, Content contentTree) {
        Content li = HtmlTree.LI(links.createLink(id,
                contents.getContent(headingKey)));
        contentTree.add(li);
    }

    /**
     * Get the contents list.
     *
     * @param apiSummary the summary list builder
     * @return a content tree for the contents list
     */
    public Content getContentsList(L apiSummary) {
        Content heading = HtmlTree.HEADING_TITLE(Headings.PAGE_TITLE_HEADING,
                HtmlStyle.title, headContent);
        Content div = HtmlTree.DIV(HtmlStyle.header, heading);
        Content headingContent = contents.contentsHeading;
        div.add(HtmlTree.HEADING_TITLE(Headings.CONTENT_HEADING,
                headingContent));
        Content ul = new HtmlTree(TagName.UL);
        addExtraIndexLink(apiSummary, ul);
        for (SummaryElementKind kind : SummaryElementKind.values()) {
            if (apiSummary.hasDocumentation(kind)) {
                addIndexLink(HtmlIds.forSummaryKind(kind), getHeadingKey(kind), ul);
            }
        }
        div.add(ul);
        return div;
    }

    /**
     * Get the header for the API Summary Listing.
     *
     * @return a content tree for the header
     */
    public HtmlTree getHeader() {
        String title = resources.getText(titleKey);
        HtmlTree bodyTree = getBody(getWindowTitle(title));
        bodyContents.setHeader(getHeader(pageMode));
        return bodyTree;
    }

    /**
     * Add summary information to the documentation tree
     *
     * @param apiList list of API summary elements
     * @param id the id attribute of the table
     * @param headingKey the caption for the summary table
     * @param headerKey table header key for the summary table
     * @param contentTree the content tree to which the summary table will be added
     */
    protected void addSummaryAPI(SortedSet<Element> apiList, HtmlId id,
                                 String headingKey, String headerKey,
                                 Content contentTree) {
        if (apiList.size() > 0) {
            TableHeader tableHeader = new TableHeader(
                    contents.getContent(headerKey), contents.descriptionLabel);

            Table table = new Table(HtmlStyle.summaryTable)
                    .setCaption(getTableCaption(headingKey))
                    .setHeader(tableHeader)
                    .setId(id)
                    .setColumnStyles(HtmlStyle.colSummaryItemName, HtmlStyle.colLast);
            addTableTabs(table, headingKey);
            for (Element e : apiList) {
                Content link;
                switch (e.getKind()) {
                    case MODULE:
                        ModuleElement m = (ModuleElement) e;
                        link = getModuleLink(m, Text.of(m.getQualifiedName()));
                        break;
                    case PACKAGE:
                        PackageElement pkg = (PackageElement) e;
                        link = getPackageLink(pkg, getLocalizedPackageName(pkg));
                        break;
                    default:
                        link = getSummaryLink(e);
                }
                Content desc = new ContentBuilder();
                addComments(e, desc);
                table.addRow(e, link, desc);
            }
            // note: singleton list
            contentTree.add(HtmlTree.UL(HtmlStyle.blockList, HtmlTree.LI(table)));
        }
    }

    /**
     * Add summary text for the given element.
     *
     * @param e the element for which the summary text should be added
     * @param desc the target to which the text should be added
     */
    protected void addComments(Element e, Content desc) {
    }

    protected Content getSummaryLink(Element e) {
        AbstractMemberWriter writer = switch (e.getKind()) {
            case INTERFACE, CLASS, ENUM,
                 ANNOTATION_TYPE, RECORD -> new NestedClassWriterImpl(this);
            case FIELD -> new FieldWriterImpl(this);
            case METHOD -> new MethodWriterImpl(this);
            case CONSTRUCTOR -> new ConstructorWriterImpl(this);
            case ENUM_CONSTANT -> new EnumConstantWriterImpl(this);
            case RECORD_COMPONENT ->
                throw new AssertionError("Record components are not supported by SummaryListWriter!");
            default -> new AnnotationTypeOptionalMemberWriterImpl(this, null);
        };
        return writer.getSummaryLink(e);
    }

    /**
     * Add an extra optional section to the content.
     *
     * @param list the element list
     * @param target the target content to which the section should be added
     */
    protected void addExtraSection(L list, Content target) {
    }

    /**
     * Add an extra optional index link.
     *
     * @param list the element list
     * @param target the target content to which the link should be added
     */
    protected void addExtraIndexLink(L list, Content target) {
    }

    /**
     * Returns the caption for the table with the given {@code headingKey}.
     *
     * @param headingKey the key for the table heading
     * @return the table caption
     */
    protected Content getTableCaption(String headingKey) {
        return contents.getContent(headingKey);
    }

    /**
     * Allow subclasses to add extra tabs to the element tables.
     *
     * @param table the element table
     * @param headingKey the key for the caption (default tab)
     */
    protected void addTableTabs(Table table, String headingKey) {}
}
