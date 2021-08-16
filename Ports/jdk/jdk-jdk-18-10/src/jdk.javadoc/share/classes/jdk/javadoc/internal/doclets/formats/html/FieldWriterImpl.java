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

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.Entity;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.FieldWriter;
import jdk.javadoc.internal.doclets.toolkit.MemberSummaryWriter;

/**
 * Writes field documentation in HTML format.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class FieldWriterImpl extends AbstractMemberWriter
    implements FieldWriter, MemberSummaryWriter {

    public FieldWriterImpl(SubWriterHolderWriter writer, TypeElement typeElement) {
        super(writer, typeElement);
    }

    public FieldWriterImpl(SubWriterHolderWriter writer) {
        super(writer);
    }

    @Override
    public Content getMemberSummaryHeader(TypeElement typeElement,
            Content memberSummaryTree) {
        memberSummaryTree.add(MarkerComments.START_OF_FIELD_SUMMARY);
        Content memberTree = new ContentBuilder();
        writer.addSummaryHeader(this, memberTree);
        return memberTree;
    }

    @Override
    public void addSummary(Content summariesList, Content content) {
        writer.addSummary(HtmlStyle.fieldSummary,
                HtmlIds.FIELD_SUMMARY, summariesList, content);
    }

    @Override
    public Content getFieldDetailsTreeHeader(Content memberDetailsTree) {
        memberDetailsTree.add(MarkerComments.START_OF_FIELD_DETAILS);
        Content fieldDetailsTree = new ContentBuilder();
        Content heading = HtmlTree.HEADING(Headings.TypeDeclaration.DETAILS_HEADING,
                contents.fieldDetailsLabel);
        fieldDetailsTree.add(heading);
        return fieldDetailsTree;
    }

    @Override
    public Content getFieldDocTreeHeader(VariableElement field) {
        Content fieldTree = new ContentBuilder();
        Content heading = HtmlTree.HEADING(Headings.TypeDeclaration.MEMBER_HEADING,
                Text.of(name(field)));
        fieldTree.add(heading);
        return HtmlTree.SECTION(HtmlStyle.detail, fieldTree)
                .setId(htmlIds.forMember(field));
    }

    @Override
    public Content getSignature(VariableElement field) {
        return new Signatures.MemberSignature(field, this)
                .setType(utils.asInstantiatedFieldType(typeElement, field))
                .setAnnotations(writer.getAnnotationInfo(field, true))
                .toContent();
    }

    @Override
    public void addDeprecated(VariableElement field, Content fieldTree) {
        addDeprecatedInfo(field, fieldTree);
    }

    @Override
    public void addPreview(VariableElement field, Content fieldTree) {
        addPreviewInfo(field, fieldTree);
    }

    @Override
    public void addComments(VariableElement field, Content fieldTree) {
        if (!utils.getFullBody(field).isEmpty()) {
            writer.addInlineComment(field, fieldTree);
        }
    }

    @Override
    public void addTags(VariableElement field, Content fieldTree) {
        writer.addTagsInfo(field, fieldTree);
    }

    @Override
    public Content getFieldDetails(Content fieldDetailsTreeHeader, Content fieldDetailsTree) {
        return writer.getDetailsListItem(
                HtmlTree.SECTION(HtmlStyle.fieldDetails)
                        .setId(HtmlIds.FIELD_DETAIL)
                        .add(fieldDetailsTreeHeader)
                        .add(fieldDetailsTree));
    }

    @Override
    public void addSummaryLabel(Content memberTree) {
        Content label = HtmlTree.HEADING(Headings.TypeDeclaration.SUMMARY_HEADING,
                contents.fieldSummaryLabel);
        memberTree.add(label);
    }

    @Override
    public TableHeader getSummaryTableHeader(Element member) {
        return new TableHeader(contents.modifierAndTypeLabel, contents.fieldLabel,
                contents.descriptionLabel);
    }

    @Override
    protected Table createSummaryTable() {
        List<HtmlStyle> bodyRowStyles = Arrays.asList(HtmlStyle.colFirst, HtmlStyle.colSecond,
                HtmlStyle.colLast);

        return new Table(HtmlStyle.summaryTable)
                .setCaption(contents.fields)
                .setHeader(getSummaryTableHeader(typeElement))
                .setColumnStyles(bodyRowStyles);
    }

    @Override
    public void addInheritedSummaryLabel(TypeElement typeElement, Content inheritedTree) {
        Content classLink = writer.getPreQualifiedClassLink(
                HtmlLinkInfo.Kind.MEMBER, typeElement);
        Content label;
        if (options.summarizeOverriddenMethods()) {
            label = Text.of(utils.isClass(typeElement)
                    ? resources.getText("doclet.Fields_Declared_In_Class")
                    : resources.getText("doclet.Fields_Declared_In_Interface"));
        } else {
            label = Text.of(utils.isClass(typeElement)
                    ? resources.getText("doclet.Fields_Inherited_From_Class")
                    : resources.getText("doclet.Fields_Inherited_From_Interface"));
        }
        HtmlTree labelHeading = HtmlTree.HEADING(Headings.TypeDeclaration.INHERITED_SUMMARY_HEADING,
                label);
        labelHeading.setId(htmlIds.forInheritedFields(typeElement));
        labelHeading.add(Entity.NO_BREAK_SPACE);
        labelHeading.add(classLink);
        inheritedTree.add(labelHeading);
    }

    @Override
    protected void addSummaryLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element member,
                                  Content tdSummary) {
        Content memberLink = writer.getDocLink(context, typeElement , member, name(member),
                HtmlStyle.memberNameLink);
        Content code = HtmlTree.CODE(memberLink);
        tdSummary.add(code);
    }

    @Override
    protected void addInheritedSummaryLink(TypeElement typeElement, Element member, Content linksTree) {
        linksTree.add(
                writer.getDocLink(HtmlLinkInfo.Kind.MEMBER, typeElement, member, name(member)));
    }

    @Override
    protected void addSummaryType(Element member, Content tdSummaryType) {
        addModifierAndType(member, utils.asInstantiatedFieldType(typeElement, (VariableElement)member), tdSummaryType);
    }

    @Override
    protected Content getSummaryLink(Element member) {
        String name = utils.getFullyQualifiedName(member) + "." + member.getSimpleName();
        return writer.getDocLink(HtmlLinkInfo.Kind.MEMBER_DEPRECATED_PREVIEW, member, name);
    }

    @Override
    public Content getMemberTreeHeader(){
        return writer.getMemberTreeHeader();
    }
}
