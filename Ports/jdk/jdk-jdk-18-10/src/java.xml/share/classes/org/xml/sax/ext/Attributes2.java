/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.xml.sax.ext;

import org.xml.sax.Attributes;


/**
 * SAX2 extension to augment the per-attribute information
 * provided through {@link Attributes}.
 * If an implementation supports this extension, the attributes
 * provided in {@link org.xml.sax.ContentHandler#startElement
 * ContentHandler.startElement() } will implement this interface,
 * and the <em>http://xml.org/sax/features/use-attributes2</em>
 * feature flag will have the value <em>true</em>.
 *
 * <p> XMLReader implementations are not required to support this
 * information, and it is not part of core-only SAX2 distributions.</p>
 *
 * <p>Note that if an attribute was defaulted (<em>!isSpecified()</em>)
 * it will of necessity also have been declared (<em>isDeclared()</em>)
 * in the DTD.
 * Similarly if an attribute's type is anything except CDATA, then it
 * must have been declared.
 * </p>
 *
 * @since 1.5, SAX 2.0 (extensions 1.1 alpha)
 * @author David Brownell
 */
public interface Attributes2 extends Attributes
{
    /**
     * Returns false unless the attribute was declared in the DTD.
     * This helps distinguish two kinds of attributes that SAX reports
     * as CDATA:  ones that were declared (and hence are usually valid),
     * and those that were not (and which are never valid).
     *
     * @param index The attribute index (zero-based).
     * @return true if the attribute was declared in the DTD,
     *          false otherwise.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not identify an attribute.
     */
    public boolean isDeclared (int index);

    /**
     * Returns false unless the attribute was declared in the DTD.
     * This helps distinguish two kinds of attributes that SAX reports
     * as CDATA:  ones that were declared (and hence are usually valid),
     * and those that were not (and which are never valid).
     *
     * @param qName The XML qualified (prefixed) name.
     * @return true if the attribute was declared in the DTD,
     *          false otherwise.
     * @throws java.lang.IllegalArgumentException When the
     *            supplied name does not identify an attribute.
     */
    public boolean isDeclared (String qName);

    /**
     * Returns false unless the attribute was declared in the DTD.
     * This helps distinguish two kinds of attributes that SAX reports
     * as CDATA:  ones that were declared (and hence are usually valid),
     * and those that were not (and which are never valid).
     *
     * <p>Remember that since DTDs do not "understand" namespaces, the
     * namespace URI associated with an attribute may not have come from
     * the DTD.  The declaration will have applied to the attribute's
     * <em>qName</em>.
     *
     * @param uri The Namespace URI, or the empty string if
     *        the name has no Namespace URI.
     * @param localName The attribute's local name.
     * @return true if the attribute was declared in the DTD,
     *          false otherwise.
     * @throws java.lang.IllegalArgumentException When the
     *            supplied names do not identify an attribute.
     */
    public boolean isDeclared (String uri, String localName);

    /**
     * Returns true unless the attribute value was provided
     * by DTD defaulting.
     *
     * @param index The attribute index (zero-based).
     * @return true if the value was found in the XML text,
     *          false if the value was provided by DTD defaulting.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not identify an attribute.
     */
    public boolean isSpecified (int index);

    /**
     * Returns true unless the attribute value was provided
     * by DTD defaulting.
     *
     * <p>Remember that since DTDs do not "understand" namespaces, the
     * namespace URI associated with an attribute may not have come from
     * the DTD.  The declaration will have applied to the attribute's
     * <em>qName</em>.
     *
     * @param uri The Namespace URI, or the empty string if
     *        the name has no Namespace URI.
     * @param localName The attribute's local name.
     * @return true if the value was found in the XML text,
     *          false if the value was provided by DTD defaulting.
     * @throws java.lang.IllegalArgumentException When the
     *            supplied names do not identify an attribute.
     */
    public boolean isSpecified (String uri, String localName);

    /**
     * Returns true unless the attribute value was provided
     * by DTD defaulting.
     *
     * @param qName The XML qualified (prefixed) name.
     * @return true if the value was found in the XML text,
     *          false if the value was provided by DTD defaulting.
     * @throws java.lang.IllegalArgumentException When the
     *            supplied name does not identify an attribute.
     */
    public boolean isSpecified (String qName);
}
