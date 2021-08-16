/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2004 World Wide Web Consortium,
 *
 * (Massachusetts Institute of Technology, European Research Consortium for
 * Informatics and Mathematics, Keio University). All Rights Reserved. This
 * work is distributed under the W3C(r) Software License [1] in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

package org.w3c.dom;

/**
 * The <code>DOMImplementation</code> interface provides a number of methods
 * for performing operations that are independent of any particular instance
 * of the document object model.
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 */
public interface DOMImplementation {
    /**
     * Test if the DOM implementation implements a specific feature and
     * version, as specified in <a href="http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#DOMFeatures">DOM Features</a>.
     * @param feature  The name of the feature to test.
     * @param version  This is the version number of the feature to test.
     * @return <code>true</code> if the feature is implemented in the
     *   specified version, <code>false</code> otherwise.
     */
    public boolean hasFeature(String feature,
                              String version);

    /**
     * Creates an empty <code>DocumentType</code> node. Entity declarations
     * and notations are not made available. Entity reference expansions and
     * default attribute additions do not occur..
     * @param qualifiedName The qualified name of the document type to be
     *   created.
     * @param publicId The external subset public identifier.
     * @param systemId The external subset system identifier.
     * @return A new <code>DocumentType</code> node with
     *   <code>Node.ownerDocument</code> set to <code>null</code>.
     * @exception DOMException
     *   INVALID_CHARACTER_ERR: Raised if the specified qualified name is not
     *   an XML name according to [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>].
     *   <br>NAMESPACE_ERR: Raised if the <code>qualifiedName</code> is
     *   malformed.
     *   <br>NOT_SUPPORTED_ERR: May be raised if the implementation does not
     *   support the feature "XML" and the language exposed through the
     *   Document does not support XML Namespaces (such as [<a href='http://www.w3.org/TR/1999/REC-html401-19991224/'>HTML 4.01</a>]).
     * @since 1.4, DOM Level 2
     */
    public DocumentType createDocumentType(String qualifiedName,
                                           String publicId,
                                           String systemId)
                                           throws DOMException;

    /**
     * Creates a DOM Document object of the specified type with its document
     * element.
     * <br>Note that based on the <code>DocumentType</code> given to create
     * the document, the implementation may instantiate specialized
     * <code>Document</code> objects that support additional features than
     * the "Core", such as "HTML" [<a href='http://www.w3.org/TR/2003/REC-DOM-Level-2-HTML-20030109'>DOM Level 2 HTML</a>]
     * . On the other hand, setting the <code>DocumentType</code> after the
     * document was created makes this very unlikely to happen.
     * Alternatively, specialized <code>Document</code> creation methods,
     * such as <code>createHTMLDocument</code> [<a href='http://www.w3.org/TR/2003/REC-DOM-Level-2-HTML-20030109'>DOM Level 2 HTML</a>]
     * , can be used to obtain specific types of <code>Document</code>
     * objects.
     * @param namespaceURI The namespace URI of the document element to
     *   create or <code>null</code>.
     * @param qualifiedName The qualified name of the document element to be
     *   created or <code>null</code>.
     * @param doctype The type of document to be created or <code>null</code>.
     *   When <code>doctype</code> is not <code>null</code>, its
     *   <code>Node.ownerDocument</code> attribute is set to the document
     *   being created.
     * @return A new <code>Document</code> object with its document element.
     *   If the <code>NamespaceURI</code>, <code>qualifiedName</code>, and
     *   <code>doctype</code> are <code>null</code>, the returned
     *   <code>Document</code> is empty with no document element.
     * @exception DOMException
     *   INVALID_CHARACTER_ERR: Raised if the specified qualified name is not
     *   an XML name according to [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>].
     *   <br>NAMESPACE_ERR: Raised if the <code>qualifiedName</code> is
     *   malformed, if the <code>qualifiedName</code> has a prefix and the
     *   <code>namespaceURI</code> is <code>null</code>, or if the
     *   <code>qualifiedName</code> is <code>null</code> and the
     *   <code>namespaceURI</code> is different from <code>null</code>, or
     *   if the <code>qualifiedName</code> has a prefix that is "xml" and
     *   the <code>namespaceURI</code> is different from "<a href='http://www.w3.org/XML/1998/namespace'>
     *   http://www.w3.org/XML/1998/namespace</a>" [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     *   , or if the DOM implementation does not support the
     *   <code>"XML"</code> feature but a non-null namespace URI was
     *   provided, since namespaces were defined by XML.
     *   <br>WRONG_DOCUMENT_ERR: Raised if <code>doctype</code> has already
     *   been used with a different document or was created from a different
     *   implementation.
     *   <br>NOT_SUPPORTED_ERR: May be raised if the implementation does not
     *   support the feature "XML" and the language exposed through the
     *   Document does not support XML Namespaces (such as [<a href='http://www.w3.org/TR/1999/REC-html401-19991224/'>HTML 4.01</a>]).
     * @since 1.4, DOM Level 2
     */
    public Document createDocument(String namespaceURI,
                                   String qualifiedName,
                                   DocumentType doctype)
                                   throws DOMException;

    /**
     *  This method returns a specialized object which implements the
     * specialized APIs of the specified feature and version, as specified
     * in <a href="http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#DOMFeatures">DOM Features</a>. The specialized object may also be obtained by using
     * binding-specific casting methods but is not necessarily expected to,
     * as discussed in . This method also allow the implementation to
     * provide specialized objects which do not support the
     * <code>DOMImplementation</code> interface.
     * @param feature  The name of the feature requested. Note that any plus
     *   sign "+" prepended to the name of the feature will be ignored since
     *   it is not significant in the context of this method.
     * @param version  This is the version number of the feature to test.
     * @return  Returns an object which implements the specialized APIs of
     *   the specified feature and version, if any, or <code>null</code> if
     *   there is no object which implements interfaces associated with that
     *   feature. If the <code>DOMObject</code> returned by this method
     *   implements the <code>DOMImplementation</code> interface, it must
     *   delegate to the primary core <code>DOMImplementation</code> and not
     *   return results inconsistent with the primary core
     *   <code>DOMImplementation</code> such as <code>hasFeature</code>,
     *   <code>getFeature</code>, etc.
     * @since 1.5, DOM Level 3
     */
    public Object getFeature(String feature,
                             String version);

}
