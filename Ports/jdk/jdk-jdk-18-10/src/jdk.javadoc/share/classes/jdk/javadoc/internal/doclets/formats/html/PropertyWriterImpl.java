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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.MemberSummaryWriter;
import jdk.javadoc.internal.doclets.toolkit.PropertyWriter;

/**
 * Writes property documentation in HTML format.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class PropertyWriterImpl extends AbstractMemberWriter
    implements PropertyWriter, MemberSummaryWriter {

    public PropertyWriterImpl(SubWriterHolderWriter writer, TypeElement typeElement) {
        super(writer, typeElement);
    }

    @Override
    public Content getMemberSummaryHeader(TypeElement typeElement, Content memberSummaryTree) {
        memberSummaryTree.add(MarkerComments.START_OF_PROPERTY_SUMMARY);
        Content memberTree = new ContentBuilder();
        writer.addSummaryHeader(this, memberTree);
        return memberTree;
    }

    @Override
    public void addSummary(Content summariesList, Content content) {
        writer.addSummary(HtmlStyle.propertySummary,
                HtmlIds.PROPERTY_SUMMARY, summariesList, content);
    }

    @Override
    public Content getPropertyDetailsTreeHeader(Content memberDetailsTree) {
        memberDetailsTree.add(MarkerComments.START_OF_PROPERTY_DETAILS);
        Content propertyDetailsTree = new ContentBuilder();
        Content heading = HtmlTree.HEADING(Headings.TypeDeclaration.DETAILS_HEADING,
                contents.propertyDetailsLabel);
        propertyDetailsTree.add(heading);
        return propertyDetailsTree;
    }

    @Override
    public Content getPropertyDocTreeHeader(ExecutableElement property) {
        Content propertyDocTree = new ContentBuilder();
        Content heading = HtmlTree.HEADING(Headings.TypeDeclaration.MEMBER_HEADING,
                Text.of(utils.getPropertyLabel(name(property))));
        propertyDocTree.add(heading);
        return HtmlTree.SECTION(HtmlStyle.detail, propertyDocTree)
                .setId(htmlIds.forProperty(property));
    }

    @Override
    public Content getSignature(ExecutableElement property) {
        return new Signatures.MemberSignature(property, this)
                .setType(utils.getReturnType(typeElement, property))
                .setAnnotations(writer.getAnnotationInfo(property, true))
                .toContent();
    }

    @Override
    public void addDeprecated(ExecutableElement property, Content propertyDocTree) {
    }

    @Override
    public void addPreview(ExecutableElement property, Content propertyDocTree) {
    }

    @Override
    public void addComments(ExecutableElement property, Content propertyDocTree) {
        TypeElement holder = (TypeElement)property.getEnclosingElement();
        if (!utils.getFullBody(property).isEmpty()) {
            if (holder.equals(typeElement) ||
                    (!utils.isPublic(holder) || utils.isLinkable(holder))) {
                writer.addInlineComment(property, propertyDocTree);
            } else {
                if (!utils.hasHiddenTag(holder) && !utils.hasHiddenTag(property)) {
                    Content link =
                            writer.getDocLink(HtmlLinkInfo.Kind.PROPERTY_COPY,
                                    holder, property,
                                    utils.isIncluded(holder)
                                            ? holder.getSimpleName() : holder.getQualifiedName());
                    Content codeLink = HtmlTree.CODE(link);
                    Content descfrmLabel = HtmlTree.SPAN(HtmlStyle.descfrmTypeLabel,
                            utils.isClass(holder)
                                    ? contents.descfrmClassLabel
                                    : contents.descfrmInterfaceLabel);
                    descfrmLabel.add(Entity.NO_BREAK_SPACE);
                    descfrmLabel.add(codeLink);
                    propertyDocTree.add(HtmlTree.DIV(HtmlStyle.block, descfrmLabel));
                }
                writer.addInlineComment(property, propertyDocTree);
            }
        }
    }

    @Override
    public void addTags(ExecutableElement property, Content propertyDocTree) {
        writer.addTagsInfo(property, propertyDocTree);
    }

    @Override
    public Content getPropertyDetails(Content propertyDetailsTreeHeader, Content propertyDetailsTree) {
        return writer.getDetailsListItem(
                HtmlTree.SECTION(HtmlStyle.propertyDetails)
                        .setId(HtmlIds.PROPERTY_DETAIL)
                        .add(propertyDetailsTreeHeader)
                        .add(propertyDetailsTree));
    }

    @Override
    public void addSummaryLabel(Content memberTree) {
        Content label = HtmlTree.HEADING(Headings.TypeDeclaration.SUMMARY_HEADING,
                contents.propertySummaryLabel);
        memberTree.add(label);
    }

    @Override
    public TableHeader getSummaryTableHeader(Element member) {
        return new TableHeader(contents.typeLabel, contents.propertyLabel,
                contents.descriptionLabel);
    }

    @Override
    protected Table createSummaryTable() {
        return new Table(HtmlStyle.summaryTable)
                .setCaption(contents.properties)
                .setHeader(getSummaryTableHeader(typeElement))
                .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colSecond, HtmlStyle.colLast);
    }

    @Override
    public void addInheritedSummaryLabel(TypeElement typeElement, Content inheritedTree) {
        Content classLink = writer.getPreQualifiedClassLink(
                HtmlLinkInfo.Kind.MEMBER, typeElement);
        Content label;
        if (options.summarizeOverriddenMethods()) {
            label = Text.of(utils.isClass(typeElement)
                    ? resources.getText("doclet.Properties_Declared_In_Class")
                    : resources.getText("doclet.Properties_Declared_In_Interface"));
        } else {
            label = Text.of(utils.isClass(typeElement)
                    ? resources.getText("doclet.Properties_Inherited_From_Class")
                    : resources.getText("doclet.Properties_Inherited_From_Interface"));
        }
        HtmlTree labelHeading =
                HtmlTree.HEADING(Headings.TypeDeclaration.INHERITED_SUMMARY_HEADING, label)
                        .setId(htmlIds.forInheritedProperties(typeElement))
                        .add(Entity.NO_BREAK_SPACE)
                        .add(classLink);
        inheritedTree.add(labelHeading);
    }

    @Override
    protected void addSummaryLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element member,
                                  Content tdSummary) {
        Content memberLink = writer.getDocLink(context, typeElement,
                member,
                Text.of(utils.getPropertyLabel(name(member))),
                HtmlStyle.memberNameLink,
                true);

        Content code = HtmlTree.CODE(memberLink);
        tdSummary.add(code);
    }

    @Override
    protected void addInheritedSummaryLink(TypeElement typeElement, Element member, Content linksTree) {
        String mname = name(member);
        Content content = writer.getDocLink(HtmlLinkInfo.Kind.MEMBER, typeElement, member,
                utils.isProperty(mname) ? utils.getPropertyName(mname) : mname, true);
        linksTree.add(content);
    }

    @Override
    protected void addSummaryType(Element member, Content tdSummaryType) {
        addModifierAndType(member, utils.getReturnType(typeElement, (ExecutableElement)member), tdSummaryType);
    }

    @Override
    protected Content getSummaryLink(Element member) {
        return writer.getDocLink(HtmlLinkInfo.Kind.MEMBER_DEPRECATED_PREVIEW, member,
                utils.getFullyQualifiedName(member));
    }

    @Override
    public Content getMemberTreeHeader(){
        return writer.getMemberTreeHeader();
    }
}
