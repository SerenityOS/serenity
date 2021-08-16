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

import javax.lang.model.element.TypeElement;

import jdk.javadoc.internal.doclets.toolkit.util.DocFileIOException;

/**
 * The interface for writing class output.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public interface ClassWriter {

    /**
     * Get the header of the page.
     *
     * @param header the header string to write
     * @return header content that needs to be added to the documentation
     */
    Content getHeader(String header);

    /**
     * Get the class content header.
     *
     * @return class content header that needs to be added to the documentation
     */
    Content getClassContentHeader();

    /**
     * Add the class tree documentation.
     *
     * @param classContentTree class content tree to which the documentation will be added
     */
    void addClassTree(Content classContentTree);

    /**
     * Get the class information tree header.
     *
     * @return class information tree header that needs to be added to the documentation
     */
    Content getClassInfoTreeHeader();

    /**
     * Add the type parameter and state component information.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addParamInfo(Content classInfoTree);

    /**
     * Add all super interfaces if this is an interface.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addSuperInterfacesInfo(Content classInfoTree);

    /**
     * Add all implemented interfaces if this is a class.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addImplementedInterfacesInfo(Content classInfoTree);

    /**
     * Add all the classes that extend this one.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addSubClassInfo(Content classInfoTree);

    /**
     * Add all the interfaces that extend this one.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addSubInterfacesInfo(Content classInfoTree);

    /**
     * If this is an interface, add all classes that implement this
     * interface.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addInterfaceUsageInfo(Content classInfoTree);

    /**
     * If this is an functional interface, display appropriate message.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addFunctionalInterfaceInfo(Content classInfoTree);

    /**
     * If this is an inner class or interface, add the enclosing class or
     * interface.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addNestedClassInfo(Content classInfoTree);

    /**
     * Get the class information.
     *
     * @param classInfoTree content tree containing the class information
     * @return a content tree for the class
     */
    Content getClassInfo(Content classInfoTree);

    /**
     * If this class is deprecated, add the appropriate information.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addClassDeprecationInfo(Content classInfoTree);

    /**
     * Add the signature of the current class content tree.
     *
     * @param classInfoTree the class content tree to which the signature will be added
     */
    void addClassSignature(Content classInfoTree);

    /**
     * Build the class description.
     *
     * @param classInfoTree content tree to which the documentation will be added
     */
    void addClassDescription(Content classInfoTree);

    /**
     * Add the tag information for the current class.
     *
     * @param classInfoTree content tree to which the tag information will be added
     */
    void addClassTagInfo(Content classInfoTree);

    /**
     * Get the member tree header for the class.
     *
     * @return a content tree for the member tree header
     */
    Content getMemberTreeHeader();

    /**
     * Returns a list to be used for the list of summaries for members of a given kind.
     *
     * @return a list to be used for the list of summaries for members of a given kind
     */
    Content getSummariesList();

    /**
     * Returns an item for the list of summaries for members of a given kind.
     *
     * @param content content for the item
     * @return an item for the list of summaries for members of a given kind
     */
    Content getSummariesListItem(Content content);

    /**
     * Returns a list to be used for the list of details for members of a given kind.
     *
     * @return a list to be used for the list of details for members of a given kind
     */
    Content getDetailsList();

    /**
     * Returns an item for the list of details for members of a given kind.
     *
     * @param content content for the item
     * @return an item for the list of details for members of a given kind
     */
    Content getDetailsListItem(Content content);

    /**
     * Add the class content tree.
     *
     * @param classContentTree class content tree which will be added to the content tree
     */
    void addClassContentTree(Content classContentTree);

    /**
     * Add the footer of the page.
     */
    void addFooter();

    /**
     * Print the document.
     *
     * @param contentTree content tree that will be printed as a document
     * @throws DocFileIOException if there is a problem while writing the document
     */
    void printDocument(Content contentTree) throws DocFileIOException;

    /**
     * Return the TypeElement being documented.
     *
     * @return the TypeElement being documented.
     */
    TypeElement getTypeElement();

    /**
     * Get the member summary tree.
     *
     * @param memberTree the content tree used to build the summary tree
     * @return a content tree for the member summary
     */
    Content getMemberSummaryTree(Content memberTree);

    /**
     * Get the member details tree.
     *
     * @param memberTree the content tree used to build the details tree
     * @return a content tree for the member details
     */
    Content getMemberDetailsTree(Content memberTree);
}
