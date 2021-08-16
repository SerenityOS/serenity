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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.BaseOptions;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.PropertyWriter;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Builds documentation for a property.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class PropertyBuilder extends AbstractMemberBuilder {

    /**
     * The writer to output the property documentation.
     */
    private final PropertyWriter writer;

    /**
     * The list of properties being documented.
     */
    private final List<? extends Element> properties;

    /**
     * The index of the current property that is being documented at this point
     * in time.
     */
    private ExecutableElement currentProperty;

    /**
     * Construct a new PropertyBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     */
    private PropertyBuilder(Context context,
            TypeElement typeElement,
            PropertyWriter writer) {
        super(context, typeElement);
        this.writer = Objects.requireNonNull(writer);
        properties = getVisibleMembers(PROPERTIES);
    }

    /**
     * Construct a new PropertyBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     * @return the new PropertyBuilder
     */
    public static PropertyBuilder getInstance(Context context,
            TypeElement typeElement,
            PropertyWriter writer) {
        return new PropertyBuilder(context, typeElement, writer);
    }

    /**
     * Returns whether or not there are members to document.
     *
     * @return whether or not there are members to document
     */
    @Override
    public boolean hasMembersToDocument() {
        return !properties.isEmpty();
    }

    @Override
    public void build(Content contentTree) throws DocletException {
        buildPropertyDoc(contentTree);
    }

    /**
     * Build the property documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildPropertyDoc(Content detailsList) throws DocletException {
        if (hasMembersToDocument()) {
            Content propertyDetailsTreeHeader = writer.getPropertyDetailsTreeHeader(detailsList);
            Content memberList = writer.getMemberList();

            for (Element property : properties) {
                currentProperty = (ExecutableElement)property;
                Content propertyDocTree = writer.getPropertyDocTreeHeader(currentProperty);

                buildSignature(propertyDocTree);
                buildPropertyComments(propertyDocTree);
                buildTagInfo(propertyDocTree);

                memberList.add(writer.getMemberListItem(propertyDocTree));
            }
            Content propertyDetails = writer.getPropertyDetails(propertyDetailsTreeHeader, memberList);
            detailsList.add(propertyDetails);
        }
    }

    /**
     * Build the signature.
     *
     * @param propertyDocTree the content tree to which the documentation will be added
     */
    protected void buildSignature(Content propertyDocTree) {
        propertyDocTree.add(writer.getSignature(currentProperty));
    }

    /**
     * Build the deprecation information.
     *
     * @param propertyDocTree the content tree to which the documentation will be added
     */
    protected void buildDeprecationInfo(Content propertyDocTree) {
        writer.addDeprecated(currentProperty, propertyDocTree);
    }

    /**
     * Build the preview information.
     *
     * @param propertyDocTree the content tree to which the documentation will be added
     */
    protected void buildPreviewInfo(Content propertyDocTree) {
        writer.addPreview(currentProperty, propertyDocTree);
    }

    /**
     * Build the comments for the property.  Do nothing if
     * {@link BaseOptions#noComment()} is set to true.
     *
     * @param propertyDocTree the content tree to which the documentation will be added
     */
    protected void buildPropertyComments(Content propertyDocTree) {
        if (!options.noComment()) {
            writer.addComments(currentProperty, propertyDocTree);
        }
    }

    /**
     * Build the tag information.
     *
     * @param propertyDocTree the content tree to which the documentation will be added
     */
    protected void buildTagInfo(Content propertyDocTree) {
        writer.addTags(currentProperty, propertyDocTree);
    }

    /**
     * Return the property writer for this builder.
     *
     * @return the property writer for this builder.
     */
    public PropertyWriter getWriter() {
        return writer;
    }
}
