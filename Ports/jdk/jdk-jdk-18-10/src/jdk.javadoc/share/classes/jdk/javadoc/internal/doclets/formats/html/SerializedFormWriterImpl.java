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

import java.util.Set;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Navigation.PageMode;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.SerializedFormWriter;
import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;
import jdk.javadoc.internal.doclets.toolkit.util.DocPaths;
import jdk.javadoc.internal.doclets.toolkit.util.IndexItem;

/**
 *  Generates the Serialized Form Information Page, <i>serialized-form.html</i>.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SerializedFormWriterImpl extends SubWriterHolderWriter
    implements SerializedFormWriter {

    Set<TypeElement> visibleClasses;

    /**
     * @param configuration the configuration data for the doclet
     */
    public SerializedFormWriterImpl(HtmlConfiguration configuration) {
        super(configuration, DocPaths.SERIALIZED_FORM);
        visibleClasses = configuration.getIncludedTypeElements();
        configuration.conditionalPages.add(HtmlConfiguration.ConditionalPage.SERIALIZED_FORM);
    }

    /**
     * Get the given header.
     *
     * @param header the header to write
     * @return the body content tree
     */
    @Override
    public Content getHeader(String header) {
        HtmlTree bodyTree = getBody(getWindowTitle(header));
        Content h1Content = Text.of(header);
        Content heading = HtmlTree.HEADING_TITLE(Headings.PAGE_TITLE_HEADING,
                HtmlStyle.title, h1Content);
        Content div = HtmlTree.DIV(HtmlStyle.header, heading);
        bodyContents.setHeader(getHeader(PageMode.SERIALIZED_FORM))
                .addMainContent(div);
        return bodyTree;
    }

    /**
     * Get the serialized form summaries header.
     *
     * @return the serialized form summary header tree
     */
    @Override
    public Content getSerializedSummariesHeader() {
        HtmlTree ul = new HtmlTree(TagName.UL);
        ul.setStyle(HtmlStyle.blockList);
        return ul;
    }

    /**
     * Get the package serialized form header.
     *
     * @return the package serialized form header tree
     */
    @Override
    public Content getPackageSerializedHeader() {
        return HtmlTree.SECTION(HtmlStyle.serializedPackageContainer);
    }

    /**
     * Get the given package header.
     *
     * @param packageElement the package element to write
     * @return a content tree for the package header
     */
    @Override
    public Content getPackageHeader(PackageElement packageElement) {
        Content heading = HtmlTree.HEADING_TITLE(Headings.SerializedForm.PACKAGE_HEADING,
                contents.packageLabel);
        heading.add(Entity.NO_BREAK_SPACE);
        heading.add(getPackageLink(packageElement, Text.of(utils.getPackageName(packageElement))));
        return heading;
    }

    /**
     * Get the serialized class header.
     *
     * @return a content tree for the serialized class header
     */
    @Override
    public Content getClassSerializedHeader() {
        HtmlTree ul = new HtmlTree(TagName.UL);
        ul.setStyle(HtmlStyle.blockList);
        return ul;
    }

    /**
     * Checks if a class is generated and is visible.
     *
     * @param typeElement the class being processed.
     * @return true if the class, that is being processed, is generated and is visible.
     */
    public boolean isVisibleClass(TypeElement typeElement) {
        return visibleClasses.contains(typeElement) && configuration.isGeneratedDoc(typeElement)
                && !utils.hasHiddenTag(typeElement);
    }

    /**
     * Get the serializable class heading.
     *
     * @param typeElement the class being processed
     * @return a content tree for the class header
     */
    @Override
    public Content getClassHeader(TypeElement typeElement) {
        Content classLink = (isVisibleClass(typeElement))
                ? getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.DEFAULT, typeElement)
                        .label(configuration.getClassName(typeElement)))
                : Text.of(utils.getFullyQualifiedName(typeElement));
        Content section = HtmlTree.SECTION(HtmlStyle.serializedClassDetails)
                .setId(htmlIds.forClass(typeElement));
        Content superClassLink = typeElement.getSuperclass() != null
                ? getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.SERIALIZED_FORM,
                        typeElement.getSuperclass()))
                : null;
        Content interfaceLink = getLink(new HtmlLinkInfo(configuration, HtmlLinkInfo.Kind.SERIALIZED_FORM,
                utils.isExternalizable(typeElement)
                        ? utils.getExternalizableType()
                        : utils.getSerializableType()));

        // Print the heading.
        Content className = new ContentBuilder();
        className.add(utils.getTypeElementKindName(typeElement, false));
        className.add(Entity.NO_BREAK_SPACE);
        className.add(classLink);
        section.add(HtmlTree.HEADING(Headings.SerializedForm.CLASS_HEADING, className));
        // Print a simplified signature.
        Content signature = new ContentBuilder();
        signature.add("class ");
        signature.add(typeElement.getSimpleName());
        signature.add(" extends ");
        signature.add(superClassLink);
        signature.add(" implements ");
        signature.add(interfaceLink);
        section.add(HtmlTree.DIV(HtmlStyle.typeSignature, signature));
        return section;
    }

    /**
     * Get the serial UID info header.
     *
     * @return a content tree for the serial uid info header
     */
    @Override
    public Content getSerialUIDInfoHeader() {
        return HtmlTree.DL(HtmlStyle.nameValue);
    }

    /**
     * Adds the serial UID info.
     *
     * @param header the header that will show up before the UID.
     * @param serialUID the serial UID to print.
     * @param serialUidTree the serial UID content tree to which the serial UID
     *                      content will be added
     */
    @Override
    public void addSerialUIDInfo(String header,
                                 String serialUID,
                                 Content serialUidTree)
    {
        Content headerContent = Text.of(header);
        serialUidTree.add(HtmlTree.DT(headerContent));
        Content serialContent = Text.of(serialUID);
        serialUidTree.add(HtmlTree.DD(serialContent));
    }

    /**
     * Get the class serialize content header.
     *
     * @return a content tree for the class serialize content header
     */
    @Override
    public Content getClassContentHeader() {
        HtmlTree ul = new HtmlTree(TagName.UL);
        ul.setStyle(HtmlStyle.blockList);
        return ul;
    }

    /**
     * Add the serialized content tree section.
     *
     * @param serializedTreeContent the serialized content tree to be added
     */
    @Override
    public void addSerializedContent(Content serializedTreeContent) {
        bodyContents.addMainContent(serializedTreeContent);
    }

    @Override
    public void addPackageSerializedTree(Content serializedSummariesTree,
                                         Content packageSerializedTree)
    {
        serializedSummariesTree.add(HtmlTree.LI(packageSerializedTree));
    }

    /**
     * Add the footer.
     */
    @Override
    public void addFooter() {
        bodyContents.setFooter(getFooter());
    }

    @Override
    public void printDocument(Content serializedTree) throws DocFileIOException {
        serializedTree.add(bodyContents);
        printHtmlDocument(null, "serialized forms", serializedTree);

        if (configuration.mainIndex != null) {
            configuration.mainIndex.add(IndexItem.of(IndexItem.Category.TAGS,
                    resources.getText("doclet.Serialized_Form"), path));
        }
    }

    /**
     * Return an instance of a SerialFieldWriter.
     *
     * @return an instance of a SerialFieldWriter.
     */
    @Override
    public SerialFieldWriter getSerialFieldWriter(TypeElement typeElement) {
        return new HtmlSerialFieldWriter(this, typeElement);
    }

    /**
     * Return an instance of a SerialMethodWriter.
     *
     * @return an instance of a SerialMethodWriter.
     */
    @Override
    public SerialMethodWriter getSerialMethodWriter(TypeElement typeElement) {
        return new HtmlSerialMethodWriter(this, typeElement);
    }
}
