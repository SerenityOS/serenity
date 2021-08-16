/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides DOM specific transformation classes.
 * <p>
 * The {@link javax.xml.transform.dom.DOMSource} class allows the
 * client of the implementation of this API to specify a DOM
 * {@link org.w3c.dom.Node} as the source of the input tree. The model of
 * how the Transformer deals with the DOM tree in terms of mismatches with the
 * <A href="http://www.w3.org/TR/xslt#data-model">XSLT data model</A> or
 * other data models is beyond the scope of this document. Any of the nodes
 * derived from {@link org.w3c.dom.Node} are legal input.
 * <p>
 * The {@link javax.xml.transform.dom.DOMResult} class allows
 * a {@link org.w3c.dom.Node} to be specified to which result DOM nodes will
 * be appended. If an output node is not specified, the transformer will use
 * {@link javax.xml.parsers.DocumentBuilder#newDocument} to create an
 * output {@link org.w3c.dom.Document} node. If a node is specified, it
 * should be one of the following: {@link org.w3c.dom.Document},
 * {@link org.w3c.dom.Element}, or
 * {@link org.w3c.dom.DocumentFragment}. Specification of any other node
 * type is implementation dependent and undefined by this API. If the result is a
 * {@link org.w3c.dom.Document}, the output of the transformation must have
 * a single element root to set as the document element.
 * <p>
 * The {@link javax.xml.transform.dom.DOMLocator} node may be passed
 * to {@link javax.xml.transform.TransformerException} objects, and
 * retrieved by trying to cast the result of the
 * {@link javax.xml.transform.TransformerException#getLocator()} method.
 * The implementation has no responsibility to use a DOMLocator instead of a
 * {@link javax.xml.transform.SourceLocator} (though line numbers and the
 * like do not make much sense for a DOM), so the result of getLocator must always
 * be tested with an instanceof.
 *
 * @since 1.5
 */

package javax.xml.transform.dom;
