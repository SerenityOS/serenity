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

import java.util.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.DocTree;

import com.sun.source.doctree.SerialTree;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.TagName;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.markup.RawHtml;
import jdk.javadoc.internal.doclets.formats.html.markup.Text;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.SerializedFormWriter;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;

/**
 * Generate serialized form for serializable fields.
 * Documentation denoted by the tags <code>serial</code> and
 * <code>serialField</code> is processed.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class HtmlSerialFieldWriter extends FieldWriterImpl
        implements SerializedFormWriter.SerialFieldWriter {

    public HtmlSerialFieldWriter(SubWriterHolderWriter writer, TypeElement typeElement) {
        super(writer, typeElement);
    }

    public SortedSet<VariableElement> members(TypeElement te) {
        return utils.serializableFields(te);
    }

    /**
     * Return the header for serializable fields section.
     *
     * @return a content tree for the header
     */
    @Override
    public Content getSerializableFieldsHeader() {
        HtmlTree ul = new HtmlTree(TagName.UL);
        ul.setStyle(HtmlStyle.blockList);
        return ul;
    }

    /**
     * Return the header for serializable fields content section.
     *
     * @param isLastContent true if the content being documented is the last content.
     * @return a content tree for the header
     */
    @Override
    public Content getFieldsContentHeader(boolean isLastContent) {
        HtmlTree li = new HtmlTree(TagName.LI);
        li.setStyle(HtmlStyle.blockList);
        return li;
    }

    /**
     * Add serializable fields.
     *
     * @param heading the heading for the section
     * @param serializableFieldsTree the tree to be added to the serializable fields
     *        content tree
     * @return a content tree for the serializable fields content
     */
    @Override
    public Content getSerializableFields(String heading, Content serializableFieldsTree) {
        HtmlTree section = HtmlTree.SECTION(HtmlStyle.detail);
        if (serializableFieldsTree.isValid()) {
            Content headingContent = Text.of(heading);
            Content serialHeading = HtmlTree.HEADING(Headings.SerializedForm.CLASS_SUBHEADING, headingContent);
            section.add(serialHeading);
            section.add(serializableFieldsTree);
        }
        return HtmlTree.LI(section);
    }

    @Override
    public void addMemberHeader(TypeMirror fieldType, String fieldName, Content contentTree) {
        Content nameContent = Text.of(fieldName);
        Content heading = HtmlTree.HEADING(Headings.SerializedForm.MEMBER_HEADING, nameContent);
        contentTree.add(heading);
        Content pre = new HtmlTree(TagName.PRE);
        Content fieldContent = writer.getLink(new HtmlLinkInfo(
                configuration, HtmlLinkInfo.Kind.SERIAL_MEMBER, fieldType));
        pre.add(fieldContent);
        pre.add(" ");
        pre.add(fieldName);
        contentTree.add(pre);
    }

    /**
     * Add the deprecated information for this member.
     *
     * @param field the field to document.
     * @param contentTree the tree to which the deprecated info will be added
     */
    @Override
    public void addMemberDeprecatedInfo(VariableElement field, Content contentTree) {
        addDeprecatedInfo(field, contentTree);
    }

    /**
     * Add the description text for this member.
     *
     * @param field the field to document.
     * @param contentTree the tree to which the deprecated info will be added
     */
    @Override
    public void addMemberDescription(VariableElement field, Content contentTree) {
        if (!utils.getFullBody(field).isEmpty()) {
            writer.addInlineComment(field, contentTree);
        }
        List<? extends SerialTree> tags = utils.getSerialTrees(field);
        if (!tags.isEmpty() && !tags.get(0).getDescription().isEmpty()) {
            writer.addInlineComment(field, tags.get(0), contentTree);
        }
    }

    /**
     * Add the description text for this member represented by the tag.
     *
     * @param serialFieldTag the field to document (represented by tag)
     * @param contentTree the tree to which the deprecated info will be added
     */
    @Override
    public void addMemberDescription(VariableElement field, DocTree serialFieldTag, Content contentTree) {
        CommentHelper ch = utils.getCommentHelper(field);
        List<? extends DocTree> description = ch.getDescription(serialFieldTag);
        if (!description.isEmpty()) {
            Content serialFieldContent = new RawHtml(ch.getText(description));
            Content div = HtmlTree.DIV(HtmlStyle.block, serialFieldContent);
            contentTree.add(div);
        }
    }

    /**
     * Add the tag information for this member.
     *
     * @param field the field to document.
     * @param contentTree the tree to which the member tags info will be added
     */
    @Override
    public void addMemberTags(VariableElement field, Content contentTree) {
        Content tagContent = writer.getBlockTagOutput(field);
        if (!tagContent.isEmpty()) {
            HtmlTree dl = HtmlTree.DL(HtmlStyle.notes);
            dl.add(tagContent);
            contentTree.add(dl);
        }
    }

    /**
     * Check to see if overview details should be printed. If
     * nocomment option set or if there is no text to be printed
     * for deprecation info, comment or tags, do not print overview details.
     *
     * @param field the field to check overview details for.
     * @return true if overview details need to be printed
     */
    @Override
    public boolean shouldPrintOverview(VariableElement field) {
        if (!options.noComment()) {
            if(!utils.getFullBody(field).isEmpty() ||
                    writer.hasSerializationOverviewTags(field))
                return true;
        }
        if (utils.isDeprecated(field))
            return true;
        return false;
    }
}
