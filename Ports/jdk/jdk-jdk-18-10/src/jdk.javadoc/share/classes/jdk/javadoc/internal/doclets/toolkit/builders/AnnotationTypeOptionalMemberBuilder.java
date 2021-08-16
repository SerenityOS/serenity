/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.AnnotationTypeOptionalMemberWriter;
import jdk.javadoc.internal.doclets.toolkit.AnnotationTypeRequiredMemberWriter;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.DocletException;

import static jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable.Kind.*;

/**
 * Builds documentation for optional annotation type members.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class AnnotationTypeOptionalMemberBuilder extends AnnotationTypeRequiredMemberBuilder {

    /**
     * Construct a new AnnotationTypeMemberBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     */
    private AnnotationTypeOptionalMemberBuilder(Context context,
            TypeElement typeElement,
            AnnotationTypeOptionalMemberWriter writer) {
        super(context, typeElement, writer, ANNOTATION_TYPE_MEMBER_OPTIONAL);
    }


    /**
     * Construct a new AnnotationTypeMemberBuilder.
     *
     * @param context  the build context.
     * @param typeElement the class whose members are being documented.
     * @param writer the doclet specific writer.
     * @return the new AnnotationTypeMemberBuilder
     */
    public static AnnotationTypeOptionalMemberBuilder getInstance(
            Context context, TypeElement typeElement,
            AnnotationTypeOptionalMemberWriter writer) {
        return new AnnotationTypeOptionalMemberBuilder(context,
                typeElement, writer);
    }

    @Override
    public void build(Content contentTree) throws DocletException {
        buildAnnotationTypeOptionalMember(contentTree);
    }

    /**
     * Build the annotation type optional member documentation.
     *
     * @param memberDetailsTree the content tree to which the documentation will be added
     * @throws DocletException if there is a problem while building the documentation
     */
    protected void buildAnnotationTypeOptionalMember(Content memberDetailsTree)
                throws DocletException {
        buildAnnotationTypeMember(memberDetailsTree);
    }

    @Override
    protected void buildAnnotationTypeMemberChildren(Content annotationDocTree) {
        super.buildAnnotationTypeMemberChildren(annotationDocTree);
        buildDefaultValueInfo(annotationDocTree);
    }

    /**
     * Build the default value for this optional member.
     *
     * @param annotationDocTree the content tree to which the documentation will be added
     */
    protected void buildDefaultValueInfo(Content annotationDocTree) {
        ((AnnotationTypeOptionalMemberWriter) writer).addDefaultValueInfo(currentMember,
                annotationDocTree);
    }

    @Override
    public AnnotationTypeRequiredMemberWriter getWriter() {
        return writer;
    }
}
