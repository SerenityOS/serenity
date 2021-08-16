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

package org.w3c.dom.ls;

/**
 *  <code>LSResourceResolver</code> provides a way for applications to
 * redirect references to external resources.
 * <p> Applications needing to implement custom handling for external
 * resources can implement this interface and register their implementation
 * by setting the "resource-resolver" parameter of
 * <code>DOMConfiguration</code> objects attached to <code>LSParser</code>
 * and <code>LSSerializer</code>. It can also be register on
 * <code>DOMConfiguration</code> objects attached to <code>Document</code>
 * if the "LS" feature is supported.
 * <p> The <code>LSParser</code> will then allow the application to intercept
 * any external entities, including the external DTD subset and external
 * parameter entities, before including them. The top-level document entity
 * is never passed to the <code>resolveResource</code> method.
 * <p> Many DOM applications will not need to implement this interface, but it
 * will be especially useful for applications that build XML documents from
 * databases or other specialized input sources, or for applications that
 * use URNs.
 * <p ><b>Note:</b>  <code>LSResourceResolver</code> is based on the SAX2 [<a href='http://www.saxproject.org/'>SAX</a>] <code>EntityResolver</code>
 * interface.
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407'>Document Object Model (DOM) Level 3 Load
and Save Specification</a>.
 *
 * @since 1.5
 */
public interface LSResourceResolver {
    /**
     *  Allow the application to resolve external resources.
     * <br> The <code>LSParser</code> will call this method before opening any
     * external resource, including the external DTD subset, external
     * entities referenced within the DTD, and external entities referenced
     * within the document element (however, the top-level document entity
     * is not passed to this method). The application may then request that
     * the <code>LSParser</code> resolve the external resource itself, that
     * it use an alternative URI, or that it use an entirely different input
     * source.
     * <br> Application writers can use this method to redirect external
     * system identifiers to secure and/or local URI, to look up public
     * identifiers in a catalogue, or to read an entity from a database or
     * other input source (including, for example, a dialog box).
     * @param type  The type of the resource being resolved. For XML [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>] resources
     *   (i.e. entities), applications must use the value
     *   <code>"http://www.w3.org/TR/REC-xml"</code>. For XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     *   , applications must use the value
     *   <code>"http://www.w3.org/2001/XMLSchema"</code>. Other types of
     *   resources are outside the scope of this specification and therefore
     *   should recommend an absolute URI in order to use this method.
     * @param namespaceURI  The namespace of the resource being resolved,
     *   e.g. the target namespace of the XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     *    when resolving XML Schema resources.
     * @param publicId  The public identifier of the external entity being
     *   referenced, or <code>null</code> if no public identifier was
     *   supplied or if the resource is not an entity.
     * @param systemId  The system identifier, a URI reference [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>], of the
     *   external resource being referenced, or <code>null</code> if no
     *   system identifier was supplied.
     * @param baseURI  The absolute base URI of the resource being parsed, or
     *   <code>null</code> if there is no base URI.
     * @return  A <code>LSInput</code> object describing the new input
     *   source, or <code>null</code> to request that the parser open a
     *   regular URI connection to the resource.
     */
    public LSInput resolveResource(String type,
                                   String namespaceURI,
                                   String publicId,
                                   String systemId,
                                   String baseURI);

}
