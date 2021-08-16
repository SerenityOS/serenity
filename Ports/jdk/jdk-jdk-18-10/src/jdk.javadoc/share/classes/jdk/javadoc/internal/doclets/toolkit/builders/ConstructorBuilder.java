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
import jdk.javadoc.internal.doclets.toolkit.ConstructorWriter;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocletException;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Builds documentation for a constructor.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ConstructorBuilder extends AbstractMemberBuilder {

    /**
     * The current constructor that is being documented at this point in time.
     */
    private ExecutableElement currentConstructor;

    /**
     * The writer to output the constructor documentation.
     */
    private final ConstructorWriter writer;

    /**
     * The constructors being documented.
     */
    private final List<? extends Element> constructors;

    /**
     * Construct a new ConstructorBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     */
    private ConstructorBuilder(Context context,
            TypeElement typeElement,
            ConstructorWriter writer) {
        super(context, typeElement);
        this.writer = Objects.requireNonNull(writer);
        constructors = getVisibleMembers(CONSTRUCTORS);
        for (Element ctor : constructors) {
            if (utils.isProtected(ctor) || utils.isPrivate(ctor)) {
                writer.setFoundNonPubConstructor(true);
            }
        }
    }

    /**
     * Construct a new ConstructorBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     * @return the new ConstructorBuilder
     */
    public static ConstructorBuilder getInstance(Context context,
            TypeElement typeElement, ConstructorWriter writer) {
        return new ConstructorBuilder(context, typeElement, writer);
    }

    @Override
    public boolean hasMembersToDocument() {
        return !constructors.isEmpty();
    }

    /**
     * Return the constructor writer for this builder.
     *
     * @return the constructor writer for this builder.
     */
    public ConstructorWriter getWriter() {
        return writer;
    }

    @Override
    public void build(Content contentTree) throws DocletException {
        buildConstructorDoc(contentTree);
    }

    /**
     * Build the constructor documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildConstructorDoc(Content detailsList) throws DocletException {
        if (hasMembersToDocument()) {
            Content constructorDetailsTreeHeader = writer.getConstructorDetailsTreeHeader(detailsList);
            Content memberList = writer.getMemberList();

            for (Element constructor : constructors) {
                currentConstructor = (ExecutableElement)constructor;
                Content constructorDocTree = writer.getConstructorDocTreeHeader(currentConstructor);

                buildSignature(constructorDocTree);
                buildDeprecationInfo(constructorDocTree);
                buildPreviewInfo(constructorDocTree);
                buildConstructorComments(constructorDocTree);
                buildTagInfo(constructorDocTree);

                memberList.add(writer.getMemberListItem(constructorDocTree));
            }
            Content constructorDetails = writer.getConstructorDetails(constructorDetailsTreeHeader, memberList);
            detailsList.add(constructorDetails);
        }
    }

    /**
     * Build the signature.
     *
     * @param constructorDocTree the content tree to which the documentation will be added
     */
    protected void buildSignature(Content constructorDocTree) {
        constructorDocTree.add(writer.getSignature(currentConstructor));
    }

    /**
     * Build the deprecation information.
     *
     * @param constructorDocTree the content tree to which the documentation will be added
     */
    protected void buildDeprecationInfo(Content constructorDocTree) {
        writer.addDeprecated(currentConstructor, constructorDocTree);
    }

    /**
     * Build the preview information.
     *
     * @param constructorDocTree the content tree to which the documentation will be added
     */
    protected void buildPreviewInfo(Content constructorDocTree) {
        writer.addPreview(currentConstructor, constructorDocTree);
    }

    /**
     * Build the comments for the constructor.  Do nothing if
     * {@link BaseOptions#noComment()} is set to true.
     *
     * @param constructorDocTree the content tree to which the documentation will be added
     */
    protected void buildConstructorComments(Content constructorDocTree) {
        if (!options.noComment()) {
            writer.addComments(currentConstructor, constructorDocTree);
        }
    }

    /**
     * Build the tag information.
     *
     * @param constructorDocTree the content tree to which the documentation will be added
     */
    protected void buildTagInfo(Content constructorDocTree) {
        writer.addTags(currentConstructor, constructorDocTree);
    }
}
