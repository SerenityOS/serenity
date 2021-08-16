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

package jdk.javadoc.internal.doclets.toolkit;

import javax.lang.model.element.ExecutableElement;

/**
 * The interface for writing property output.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public interface PropertyWriter extends MemberWriter {

    /**
     * Get the property details tree header.
     *
     * @param memberDetailsTree the content tree representing member details
     * @return content tree for the property details header
     */
    Content getPropertyDetailsTreeHeader(Content memberDetailsTree);

    /**
     * Get the property documentation tree header.
     *
     * @param property the property being documented
     * @return content tree for the property documentation header
     */
    Content getPropertyDocTreeHeader(ExecutableElement property);

    /**
     * Get the signature for the given property.
     *
     * @param property the property being documented
     * @return content tree for the property signature
     */
    Content getSignature(ExecutableElement property);

    /**
     * Add the deprecated output for the given property.
     *
     * @param property the property being documented
     * @param propertyDocTree content tree to which the deprecated information will be added
     */
    void addDeprecated(ExecutableElement property, Content propertyDocTree);

    /**
     * Add the preview output for the given member.
     *
     * @param member the member being documented
     * @param annotationDocTree content tree to which the preview information will be added
     */
    void addPreview(ExecutableElement member, Content contentTree);

    /**
     * Add the comments for the given property.
     *
     * @param property the property being documented
     * @param propertyDocTree the content tree to which the comments will be added
     */
    void addComments(ExecutableElement property, Content propertyDocTree);

    /**
     * Add the tags for the given property.
     *
     * @param property the property being documented
     * @param propertyDocTree the content tree to which the tags will be added
     */
    void addTags(ExecutableElement property, Content propertyDocTree);

    /**
     * Get the property details tree.
     *
     * @param memberDetailsTreeHeader the content tree representing member details header
     * @param memberDetailsTree the content tree representing member details
     * @return content tree for the property details
     */
    Content getPropertyDetails(Content memberDetailsTreeHeader, Content memberDetailsTree);

    /**
     * Gets the member header tree.
     *
     * @return a content tree for the member header
     */
    Content getMemberTreeHeader();
}
