/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.internal.doclets.formats.html.markup.Comment;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.AnnotationTypeRequiredMemberWriter;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.MemberSummaryWriter;


/**
 * Writes annotation type required member documentation in HTML format.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class AnnotationTypeRequiredMemberWriterImpl extends AbstractMemberWriter
    implements AnnotationTypeRequiredMemberWriter, MemberSummaryWriter {

    /**
     * Construct a new AnnotationTypeRequiredMemberWriterImpl.
     *
     * @param writer         the writer that will write the output.
     * @param annotationType the AnnotationType that holds this member.
     */
    public AnnotationTypeRequiredMemberWriterImpl(SubWriterHolderWriter writer,
            TypeElement annotationType) {
        super(writer, annotationType);
    }

    @Override
    public Content getMemberSummaryHeader(TypeElement typeElement,
            Content memberSummaryTree) {
        memberSummaryTree.add(selectComment(
                MarkerComments.START_OF_ANNOTATION_TYPE_REQUIRED_MEMBER_SUMMARY,
                MarkerComments.START_OF_ANNOTATION_INTERFACE_REQUIRED_MEMBER_SUMMARY));
        Content memberTree = new ContentBuilder();
        writer.addSummaryHeader(this, memberTree);
        return memberTree;
    }

    @Override
    public Content getMemberTreeHeader() {
        return writer.getMemberTreeHeader();
    }

    @Override
    public void addSummary(Content summariesList, Content content) {
        writer.addSummary(HtmlStyle.memberSummary,
                HtmlIds.ANNOTATION_TYPE_REQUIRED_ELEMENT_SUMMARY, summariesList, content);
    }

    @Override
    public void addAnnotationDetailsMarker(Content memberDetails) {
        memberDetails.add(selectComment(
                MarkerComments.START_OF_ANNOTATION_TYPE_DETAILS,
                MarkerComments.START_OF_ANNOTATION_INTERFACE_DETAILS));
    }

    @Override
    public Content getAnnotationDetailsTreeHeader() {
        Content memberDetailsTree = new ContentBuilder();
        if (!writer.printedAnnotationHeading) {
            Content heading = HtmlTree.HEADING(Headings.TypeDeclaration.DETAILS_HEADING,
                    contents.annotationTypeDetailsLabel);
            memberDetailsTree.add(heading);
            writer.printedAnnotationHeading = true;
        }
        return memberDetailsTree;
    }

    @Override
    public Content getAnnotationDocTreeHeader(Element member) {
        Content annotationDocTree = new ContentBuilder();
        Content heading = HtmlTree.HEADING(Headings.TypeDeclaration.MEMBER_HEADING,
                Text.of(name(member)));
        annotationDocTree.add(heading);
        return HtmlTree.SECTION(HtmlStyle.detail, annotationDocTree)
                .setId(htmlIds.forMember(typeElement, (ExecutableElement) member));
    }

    @Override
    public Content getSignature(Element member) {
        return new Signatures.MemberSignature(member, this)
                .setType(getType(member))
                .setAnnotations(writer.getAnnotationInfo(member, true))
                .toContent();
    }

    @Override
    public void addDeprecated(Element member, Content annotationDocTree) {
        addDeprecatedInfo(member, annotationDocTree);
    }

    @Override
    public void addPreview(Element member, Content contentTree) {
        addPreviewInfo(member, contentTree);
    }

    @Override
    public void addComments(Element member, Content annotationDocTree) {
        addComment(member, annotationDocTree);
    }

    @Override
    public void addTags(Element member, Content annotationDocTree) {
        writer.addTagsInfo(member, annotationDocTree);
    }

    @Override
    public Content getAnnotationDetails(Content annotationDetailsTreeHeader, Content annotationDetailsTree) {
        Content annotationDetails = new ContentBuilder(annotationDetailsTreeHeader, annotationDetailsTree);
        return getMemberTree(HtmlTree.SECTION(HtmlStyle.memberDetails, annotationDetails));
    }

    @Override
    public void addSummaryLabel(Content memberTree) {
        HtmlTree label = HtmlTree.HEADING(Headings.TypeDeclaration.SUMMARY_HEADING,
                contents.annotateTypeRequiredMemberSummaryLabel);
        memberTree.add(label);
    }

    /**
     * Get the caption for the summary table.
     * @return the caption
     */
    // Overridden by AnnotationTypeOptionalMemberWriterImpl
    protected Content getCaption() {
        return contents.getContent("doclet.Annotation_Type_Required_Members");
    }

    @Override
    public TableHeader getSummaryTableHeader(Element member) {
        return new TableHeader(contents.modifierAndTypeLabel,
                contents.annotationTypeRequiredMemberLabel, contents.descriptionLabel);
    }

    @Override
    protected Table createSummaryTable() {
        return new Table(HtmlStyle.summaryTable)
                .setCaption(getCaption())
                .setHeader(getSummaryTableHeader(typeElement))
                .setColumnStyles(HtmlStyle.colFirst, HtmlStyle.colSecond, HtmlStyle.colLast);
    }

    @Override
    public void addInheritedSummaryLabel(TypeElement typeElement, Content inheritedTree) {
    }

    @Override
    protected void addSummaryLink(HtmlLinkInfo.Kind context, TypeElement typeElement, Element member,
                                  Content tdSummary) {
        Content memberLink = writer.getDocLink(context, utils.getEnclosingTypeElement(member), member,
                name(member), HtmlStyle.memberNameLink);
        Content code = HtmlTree.CODE(memberLink);
        tdSummary.add(code);
    }

    @Override
    protected void addInheritedSummaryLink(TypeElement typeElement,
            Element member, Content linksTree) {
        //Not applicable.
    }

    @Override
    protected void addSummaryType(Element member, Content tdSummaryType) {
        addModifierAndType(member, getType(member), tdSummaryType);
    }

    @Override
    protected Content getSummaryLink(Element member) {
        String name = utils.getFullyQualifiedName(member) + "." + member.getSimpleName();
        return writer.getDocLink(HtmlLinkInfo.Kind.MEMBER_DEPRECATED_PREVIEW, member, name);
    }

    protected Comment selectComment(Comment c1, Comment c2) {
        HtmlConfiguration configuration = writer.configuration;
        SourceVersion sv = configuration.docEnv.getSourceVersion();
        return sv.compareTo(SourceVersion.RELEASE_16) < 0 ? c1 : c2;
    }

    private TypeMirror getType(Element member) {
        return utils.isExecutableElement(member)
                ? utils.getReturnType(typeElement, (ExecutableElement) member)
                : member.asType();
    }
}
