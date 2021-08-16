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

package jdk.javadoc.internal.doclets.toolkit.builders;

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import jdk.javadoc.internal.doclets.toolkit.BaseOptions;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.EnumConstantWriter;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Builds documentation for a enum constants.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class EnumConstantBuilder extends AbstractMemberBuilder {

    /**
     * The writer to output the enum constants documentation.
     */
    private final EnumConstantWriter writer;

    /**
     * The set of enum constants being documented.
     */
    private final List<? extends Element> enumConstants;

    /**
     * The current enum constant that is being documented at this point
     * in time.
     */
    private VariableElement currentElement;

    /**
     * Construct a new EnumConstantsBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     */
    private EnumConstantBuilder(Context context,
            TypeElement typeElement, EnumConstantWriter writer) {
        super(context, typeElement);
        this.writer = Objects.requireNonNull(writer);
        enumConstants = getVisibleMembers(ENUM_CONSTANTS);
    }

    /**
     * Construct a new EnumConstantsBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     * @return the new EnumConstantsBuilder
     */
    public static EnumConstantBuilder getInstance(Context context,
            TypeElement typeElement, EnumConstantWriter writer) {
        return new EnumConstantBuilder(context, typeElement, writer);
    }

    /**
     * Returns whether or not there are members to document.
     *
     * @return whether or not there are members to document
     */
    @Override
    public boolean hasMembersToDocument() {
        return !enumConstants.isEmpty();
    }

    @Override
    public void build(Content contentTree) throws DocletException {
        buildEnumConstant(contentTree);
    }

    /**
     * Build the enum constant documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException is there is a problem while building the documentation
     */
    protected void buildEnumConstant(Content detailsList) throws DocletException {
        if (hasMembersToDocument()) {
            Content enumConstantsDetailsTreeHeader = writer.getEnumConstantsDetailsTreeHeader(typeElement,
                    detailsList);
            Content memberList = writer.getMemberList();

            for (Element enumConstant : enumConstants) {
                currentElement = (VariableElement)enumConstant;
                Content enumConstantsTree = writer.getEnumConstantsTreeHeader(currentElement,
                        memberList);

                buildSignature(enumConstantsTree);
                buildDeprecationInfo(enumConstantsTree);
                buildPreviewInfo(enumConstantsTree);
                buildEnumConstantComments(enumConstantsTree);
                buildTagInfo(enumConstantsTree);

                memberList.add(writer.getMemberListItem(enumConstantsTree));
            }
            Content enumConstantDetails = writer.getEnumConstantsDetails(
                    enumConstantsDetailsTreeHeader, memberList);
            detailsList.add(enumConstantDetails);
        }
    }

    /**
     * Build the signature.
     *
     * @param enumConstantsTree the content tree to which the documentation will be added
     */
    protected void buildSignature(Content enumConstantsTree) {
        enumConstantsTree.add(writer.getSignature(currentElement));
    }

    /**
     * Build the deprecation information.
     *
     * @param enumConstantsTree the content tree to which the documentation will be added
     */
    protected void buildDeprecationInfo(Content enumConstantsTree) {
        writer.addDeprecated(currentElement, enumConstantsTree);
    }

    /**
     * Build the preview information.
     *
     * @param enumConstantsTree the content tree to which the documentation will be added
     */
    protected void buildPreviewInfo(Content enumConstantsTree) {
        writer.addPreview(currentElement, enumConstantsTree);
    }

    /**
     * Build the comments for the enum constant.  Do nothing if
     * {@link BaseOptions#noComment()} is set to true.
     *
     * @param enumConstantsTree the content tree to which the documentation will be added
     */
    protected void buildEnumConstantComments(Content enumConstantsTree) {
        if (!options.noComment()) {
            writer.addComments(currentElement, enumConstantsTree);
        }
    }

    /**
     * Build the tag information.
     *
     * @param enumConstantsTree the content tree to which the documentation will be added
     */
    protected void buildTagInfo(Content enumConstantsTree) {
        writer.addTags(currentElement, enumConstantsTree);
    }

    /**
     * Return the enum constant writer for this builder.
     *
     * @return the enum constant writer for this builder.
     */
    public EnumConstantWriter getWriter() {
        return writer;
    }
}
