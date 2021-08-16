/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
import javax.lang.model.type.TypeMirror;

/**
 * The interface for writing method output.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public interface MethodWriter extends MemberWriter {

    /**
     * Get the method details tree header.
     *
     * @param memberDetailsTree the content tree representing member details
     * @return content tree for the method details header
     */
    Content getMethodDetailsTreeHeader(Content memberDetailsTree);

    /**
     * Get the method documentation tree header.
     *
     * @param method the method being documented
     * @return content tree for the method documentation header
     */
    Content getMethodDocTreeHeader(ExecutableElement method);

    /**
     * Get the signature for the given method.
     *
     * @param method the method being documented
     * @return content tree for the method signature
     */
    Content getSignature(ExecutableElement method);

    /**
     * Add the deprecated output for the given method.
     *
     * @param method the method being documented
     * @param methodDocTree content tree to which the deprecated information will be added
     */
    void addDeprecated(ExecutableElement method, Content methodDocTree);

    /**
     * Adds the preview output for the given member.
     *
     * @param member the member being documented
     * @param annotationDocTree content tree to which the preview information will be added
     */
    void addPreview(ExecutableElement member, Content contentTree);

    /**
     * Add the comments for the given method.
     *
     * @param holder the holder type (not erasure) of the method
     * @param method the method being documented
     * @param methodDocTree the content tree to which the comments will be added
     */
    void addComments(TypeMirror holder, ExecutableElement method, Content methodDocTree);

    /**
     * Add the tags for the given method.
     *
     * @param method the method being documented
     * @param methodDocTree the content tree to which the tags will be added
     */
    void addTags(ExecutableElement method, Content methodDocTree);

    /**
     * Get the method details tree.
     *
     * @param methodDetailsTreeHeader the content tree representing method details header
     * @param methodDetailsTree the content tree representing method details
     * @return content tree for the method details
     */
    Content getMethodDetails(Content methodDetailsTreeHeader, Content methodDetailsTree);

    /**
     * Gets the member header tree.
     *
     * @return a content tree for the member header
     */
    Content getMemberTreeHeader();
}
