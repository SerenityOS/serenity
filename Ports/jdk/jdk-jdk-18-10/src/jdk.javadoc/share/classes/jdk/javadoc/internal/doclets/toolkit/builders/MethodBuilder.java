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
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.internal.doclets.toolkit.BaseOptions;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocletException;
import jdk.javadoc.internal.doclets.toolkit.MethodWriter;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Builds documentation for a method.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class MethodBuilder extends AbstractMemberBuilder {

    /**
     * The index of the current field that is being documented at this point
     * in time.
     */
    private ExecutableElement currentMethod;

    /**
     * The writer to output the method documentation.
     */
    private final MethodWriter writer;

    /**
     * The methods being documented.
     */
    private final List<? extends Element> methods;


    /**
     * Construct a new MethodBuilder.
     *
     * @param context       the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     */
    private MethodBuilder(Context context,
            TypeElement typeElement,
            MethodWriter writer) {
        super(context, typeElement);
        this.writer = Objects.requireNonNull(writer);
        methods = getVisibleMembers(METHODS);
    }

    /**
     * Construct a new MethodBuilder.
     *
     * @param context       the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     *
     * @return an instance of a MethodBuilder.
     */
    public static MethodBuilder getInstance(Context context,
            TypeElement typeElement, MethodWriter writer) {
        return new MethodBuilder(context, typeElement, writer);
    }

    @Override
    public boolean hasMembersToDocument() {
        return !methods.isEmpty();
    }

    @Override
    public void build(Content contentTree) throws DocletException {
        buildMethodDoc(contentTree);
    }

    /**
     * Build the method documentation.
     *
     * @param detailsList the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildMethodDoc(Content detailsList) throws DocletException {
        if (hasMembersToDocument()) {
            Content methodDetailsTreeHeader = writer.getMethodDetailsTreeHeader(detailsList);
            Content memberList = writer.getMemberList();

            for (Element method : methods) {
                currentMethod = (ExecutableElement)method;
                Content methodDocTree = writer.getMethodDocTreeHeader(currentMethod);

                buildSignature(methodDocTree);
                buildDeprecationInfo(methodDocTree);
                buildPreviewInfo(methodDocTree);
                buildMethodComments(methodDocTree);
                buildTagInfo(methodDocTree);

                memberList.add(writer.getMemberListItem(methodDocTree));
            }
            Content methodDetails = writer.getMethodDetails(methodDetailsTreeHeader, memberList);
            detailsList.add(methodDetails);
        }
    }

    /**
     * Build the signature.
     *
     * @param methodDocTree the content tree to which the documentation will be added
     */
    protected void buildSignature(Content methodDocTree) {
        methodDocTree.add(writer.getSignature(currentMethod));
    }

    /**
     * Build the deprecation information.
     *
     * @param methodDocTree the content tree to which the documentation will be added
     */
    protected void buildDeprecationInfo(Content methodDocTree) {
        writer.addDeprecated(currentMethod, methodDocTree);
    }

    /**
     * Build the preview information.
     *
     * @param methodDocTree the content tree to which the documentation will be added
     */
    protected void buildPreviewInfo(Content methodDocTree) {
        writer.addPreview(currentMethod, methodDocTree);
    }

    /**
     * Build the comments for the method.  Do nothing if
     * {@link BaseOptions#noComment()} is set to true.
     *
     * @param methodDocTree the content tree to which the documentation will be added
     */
    protected void buildMethodComments(Content methodDocTree) {
        if (!options.noComment()) {
            ExecutableElement method = currentMethod;
            if (utils.getFullBody(currentMethod).isEmpty()) {
                DocFinder.Output docs = DocFinder.search(configuration,
                        new DocFinder.Input(utils, currentMethod));
                if (docs.inlineTags != null && !docs.inlineTags.isEmpty())
                        method = (ExecutableElement)docs.holder;
            }
            TypeMirror containingType = method.getEnclosingElement().asType();
            writer.addComments(containingType, method, methodDocTree);
        }
    }

    /**
     * Build the tag information.
     *
     * @param methodDocTree the content tree to which the documentation will be added
     */
    protected void buildTagInfo(Content methodDocTree) {
        writer.addTags(currentMethod, methodDocTree);
    }

    /**
     * Return the method writer for this builder.
     *
     * @return the method writer for this builder.
     */
    public MethodWriter getWriter() {
        return writer;
    }
}
