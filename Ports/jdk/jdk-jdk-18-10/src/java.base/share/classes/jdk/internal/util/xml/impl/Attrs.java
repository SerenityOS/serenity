/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util.xml.impl;

import jdk.internal.org.xml.sax.Attributes;

public class Attrs implements Attributes {

    /**
     * Attributes string array. Each individual attribute is represented by four
     * strings: namespace URL(+0), qname(+1), local name(+2), value(+3),
     * type(+4), declared["d"] and default["D"](+5). In order to find attribute
     * by the attrubute index, the attribute index MUST be multiplied by 8. The
     * result will point to the attribute namespace URL.
     */
    /* pkg */ String[] mItems;
    /**
     * Number of attributes in the attributes string array.
     */
    private char mLength;
    /**
     * current index
     */
    private char mAttrIdx = 0;

    /**
     * Constructor.
     */
    public Attrs() {
        //              The default number of attributies capacity is 8.
        mItems = new String[(8 << 3)];
    }

    /**
     * Sets up the number of attributes and ensure the capacity of the attribute
     * string array.
     *
     * @param length The number of attributes in the object.
     */
    public void setLength(char length) {
        if (length > ((char) (mItems.length >> 3))) {
            mItems = new String[length << 3];
        }
        mLength = length;
    }

    /**
     * Return the number of attributes in the list.
     *
     * <p>Once you know the number of attributes, you can iterate through the
     * list.</p>
     *
     * @return The number of attributes in the list.
     * @see #getURI(int)
     * @see #getLocalName(int)
     * @see #getQName(int)
     * @see #getType(int)
     * @see #getValue(int)
     */
    public int getLength() {
        return mLength;
    }

    /**
     * Look up an attribute's Namespace URI by index.
     *
     * @param index The attribute index (zero-based).
     * @return The Namespace URI, or the empty string if none is available, or
     * null if the index is out of range.
     * @see #getLength
     */
    public String getURI(int index) {
        return ((index >= 0) && (index < mLength))
                ? (mItems[index << 3])
                : null;
    }

    /**
     * Look up an attribute's local name by index.
     *
     * @param index The attribute index (zero-based).
     * @return The local name, or the empty string if Namespace processing is
     * not being performed, or null if the index is out of range.
     * @see #getLength
     */
    public String getLocalName(int index) {
        return ((index >= 0) && (index < mLength))
                ? (mItems[(index << 3) + 2])
                : null;
    }

    /**
     * Look up an attribute's XML 1.0 qualified name by index.
     *
     * @param index The attribute index (zero-based).
     * @return The XML 1.0 qualified name, or the empty string if none is
     * available, or null if the index is out of range.
     * @see #getLength
     */
    public String getQName(int index) {
        if ((index < 0) || (index >= mLength)) {
            return null;
        }
        return mItems[(index << 3) + 1];
    }

    /**
     * Look up an attribute's type by index.
     *
     * <p>The attribute type is one of the strings "CDATA", "ID", "IDREF",
     * "IDREFS", "NMTOKEN", "NMTOKENS", "ENTITY", "ENTITIES", or "NOTATION"
     * (always in upper case).</p>
     *
     * <p>If the parser has not read a declaration for the attribute, or if the
     * parser does not report attribute types, then it must return the value
     * "CDATA" as stated in the XML 1.0 Recommentation (clause 3.3.3,
     * "Attribute-Value Normalization").</p>
     *
     * <p>For an enumerated attribute that is not a notation, the parser will
     * report the type as "NMTOKEN".</p>
     *
     * @param index The attribute index (zero-based).
     * @return The attribute's type as a string, or null if the index is out of
     * range.
     * @see #getLength
     */
    public String getType(int index) {
        return ((index >= 0) && (index < (mItems.length >> 3)))
                ? (mItems[(index << 3) + 4])
                : null;
    }

    /**
     * Look up an attribute's value by index.
     *
     * <p>If the attribute value is a list of tokens (IDREFS, ENTITIES, or
     * NMTOKENS), the tokens will be concatenated into a single string with each
     * token separated by a single space.</p>
     *
     * @param index The attribute index (zero-based).
     * @return The attribute's value as a string, or null if the index is out of
     * range.
     * @see #getLength
     */
    public String getValue(int index) {
        return ((index >= 0) && (index < mLength))
                ? (mItems[(index << 3) + 3])
                : null;
    }

    /**
     * Look up the index of an attribute by Namespace name.
     *
     * @param uri The Namespace URI, or the empty string if the name has no
     * Namespace URI.
     * @param localName The attribute's local name.
     * @return The index of the attribute, or -1 if it does not appear in the
     * list.
     */
    public int getIndex(String uri, String localName) {
        char len = mLength;
        for (char idx = 0; idx < len; idx++) {
            if ((mItems[idx << 3]).equals(uri)
                    && mItems[(idx << 3) + 2].equals(localName)) {
                return idx;
            }
        }
        return -1;
    }

    /**
     * Look up the index of an attribute by Namespace name.
     *
     * @param uri The Namespace URI, or the empty string if the name has no
     * Namespace URI. <code>null</code> value enforce the search by the local
     * name only.
     * @param localName The attribute's local name.
     * @return The index of the attribute, or -1 if it does not appear in the
     * list.
     */
    /* pkg */ int getIndexNullNS(String uri, String localName) {
        char len = mLength;
        if (uri != null) {
            for (char idx = 0; idx < len; idx++) {
                if ((mItems[idx << 3]).equals(uri)
                        && mItems[(idx << 3) + 2].equals(localName)) {
                    return idx;
                }
            }
        } else {
            for (char idx = 0; idx < len; idx++) {
                if (mItems[(idx << 3) + 2].equals(localName)) {
                    return idx;
                }
            }
        }
        return -1;
    }

    /**
     * Look up the index of an attribute by XML 1.0 qualified name.
     *
     * @param qName The qualified (prefixed) name.
     * @return The index of the attribute, or -1 if it does not appear in the
     * list.
     */
    public int getIndex(String qName) {
        char len = mLength;
        for (char idx = 0; idx < len; idx++) {
            if (mItems[(idx << 3) + 1].equals(qName)) {
                return idx;
            }
        }
        return -1;
    }

    /**
     * Look up an attribute's type by Namespace name.
     *
     * <p>See {@link #getType(int) getType(int)} for a description of the
     * possible types.</p>
     *
     * @param uri The Namespace URI, or the empty String if the name has no
     * Namespace URI.
     * @param localName The local name of the attribute.
     * @return The attribute type as a string, or null if the attribute is not
     * in the list or if Namespace processing is not being performed.
     */
    public String getType(String uri, String localName) {
        int idx = getIndex(uri, localName);
        return (idx >= 0) ? (mItems[(idx << 3) + 4]) : null;
    }

    /**
     * Look up an attribute's type by XML 1.0 qualified name.
     *
     * <p>See {@link #getType(int) getType(int)} for a description of the
     * possible types.</p>
     *
     * @param qName The XML 1.0 qualified name.
     * @return The attribute type as a string, or null if the attribute is not
     * in the list or if qualified names are not available.
     */
    public String getType(String qName) {
        int idx = getIndex(qName);
        return (idx >= 0) ? (mItems[(idx << 3) + 4]) : null;
    }

    /**
     * Look up an attribute's value by Namespace name.
     *
     * <p>See {@link #getValue(int) getValue(int)} for a description of the
     * possible values.</p>
     *
     * @param uri The Namespace URI, or the empty String if the name has no
     * Namespace URI.
     * @param localName The local name of the attribute.
     * @return The attribute value as a string, or null if the attribute is not
     * in the list.
     */
    public String getValue(String uri, String localName) {
        int idx = getIndex(uri, localName);
        return (idx >= 0) ? (mItems[(idx << 3) + 3]) : null;
    }

    /**
     * Look up an attribute's value by XML 1.0 qualified name.
     *
     * <p>See {@link #getValue(int) getValue(int)} for a description of the
     * possible values.</p>
     *
     * @param qName The XML 1.0 qualified name.
     * @return The attribute value as a string, or null if the attribute is not
     * in the list or if qualified names are not available.
     */
    public String getValue(String qName) {
        int idx = getIndex(qName);
        return (idx >= 0) ? (mItems[(idx << 3) + 3]) : null;
    }

    /**
     * Returns false unless the attribute was declared in the DTD. This helps
     * distinguish two kinds of attributes that SAX reports as CDATA: ones that
     * were declared (and hence are usually valid), and those that were not (and
     * which are never valid).
     *
     * @param index The attribute index (zero-based).
     * @return true if the attribute was declared in the DTD, false otherwise.
     * @exception java.lang.ArrayIndexOutOfBoundsException When the supplied
     * index does not identify an attribute.
     */
    public boolean isDeclared(int index) {
        if ((index < 0) || (index >= mLength)) {
            throw new ArrayIndexOutOfBoundsException("");
        }

        return ((mItems[(index << 3) + 5]) != null);
    }

    /**
     * Returns false unless the attribute was declared in the DTD. This helps
     * distinguish two kinds of attributes that SAX reports as CDATA: ones that
     * were declared (and hence are usually valid), and those that were not (and
     * which are never valid).
     *
     * @param qName The XML qualified (prefixed) name.
     * @return true if the attribute was declared in the DTD, false otherwise.
     * @exception java.lang.IllegalArgumentException When the supplied name does
     * not identify an attribute.
     */
    public boolean isDeclared(String qName) {
        int idx = getIndex(qName);
        if (idx < 0) {
            throw new IllegalArgumentException("");
        }

        return ((mItems[(idx << 3) + 5]) != null);
    }

    /**
     * Returns false unless the attribute was declared in the DTD. This helps
     * distinguish two kinds of attributes that SAX reports as CDATA: ones that
     * were declared (and hence are usually valid), and those that were not (and
     * which are never valid).
     *
     * <p>Remember that since DTDs do not "understand" namespaces, the namespace
     * URI associated with an attribute may not have come from the DTD. The
     * declaration will have applied to the attribute's <em>qName</em>.
     *
     * @param uri The Namespace URI, or the empty string if the name has no
     * Namespace URI.
     * @param localName The attribute's local name.
     * @return true if the attribute was declared in the DTD, false otherwise.
     * @exception java.lang.IllegalArgumentException When the supplied names do
     * not identify an attribute.
     */
    public boolean isDeclared(String uri, String localName) {
        int idx = getIndex(uri, localName);
        if (idx < 0) {
            throw new IllegalArgumentException("");
        }

        return ((mItems[(idx << 3) + 5]) != null);
    }

    /**
     * Returns true unless the attribute value was provided by DTD defaulting.
     *
     * @param index The attribute index (zero-based).
     * @return true if the value was found in the XML text, false if the value
     * was provided by DTD defaulting.
     * @exception java.lang.ArrayIndexOutOfBoundsException When the supplied
     * index does not identify an attribute.
     */
    public boolean isSpecified(int index) {
        if ((index < 0) || (index >= mLength)) {
            throw new ArrayIndexOutOfBoundsException("");
        }

        String str = mItems[(index << 3) + 5];
        return ((str != null) ? (str.charAt(0) == 'd') : true);
    }

    /**
     * Returns true unless the attribute value was provided by DTD defaulting.
     *
     * <p>Remember that since DTDs do not "understand" namespaces, the namespace
     * URI associated with an attribute may not have come from the DTD. The
     * declaration will have applied to the attribute's <em>qName</em>.
     *
     * @param uri The Namespace URI, or the empty string if the name has no
     * Namespace URI.
     * @param localName The attribute's local name.
     * @return true if the value was found in the XML text, false if the value
     * was provided by DTD defaulting.
     * @exception java.lang.IllegalArgumentException When the supplied names do
     * not identify an attribute.
     */
    public boolean isSpecified(String uri, String localName) {
        int idx = getIndex(uri, localName);
        if (idx < 0) {
            throw new IllegalArgumentException("");
        }

        String str = mItems[(idx << 3) + 5];
        return ((str != null) ? (str.charAt(0) == 'd') : true);
    }

    /**
     * Returns true unless the attribute value was provided by DTD defaulting.
     *
     * @param qName The XML qualified (prefixed) name.
     * @return true if the value was found in the XML text, false if the value
     * was provided by DTD defaulting.
     * @exception java.lang.IllegalArgumentException When the supplied name does
     * not identify an attribute.
     */
    public boolean isSpecified(String qName) {
        int idx = getIndex(qName);
        if (idx < 0) {
            throw new IllegalArgumentException("");
        }

        String str = mItems[(idx << 3) + 5];
        return ((str != null) ? (str.charAt(0) == 'd') : true);
    }
}
