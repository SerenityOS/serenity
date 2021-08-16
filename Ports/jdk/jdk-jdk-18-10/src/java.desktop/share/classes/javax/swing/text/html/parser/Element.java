/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text.html.parser;

import java.util.Hashtable;
import java.util.BitSet;
import java.io.*;
import sun.awt.AppContext;

/**
 * An element as described in a DTD using the ELEMENT construct.
 * This is essential the description of a tag. It describes the
 * type, content model, attributes, attribute types etc. It is used
 * to correctly parse a document by the Parser.
 *
 * @see DTD
 * @see AttributeList
 * @author Arthur van Hoff
 */
@SuppressWarnings("serial") // Same-version serialization only
public final
class Element implements DTDConstants, Serializable {

    /**
     * The element index
     */
    public int index;

    /**
     * The name of the element
     */
    public String name;

    /**
     * {@code true} if the start tag can be omitted
     */
    public boolean oStart;

    /**
     * {@code true} if the end tag can be omitted
     */
    public boolean oEnd;

    /**
     * The set of elements that can occur inside the element
     */
    public BitSet inclusions;

    /**
     * The set of elements that must not occur inside the element
     */
    public BitSet exclusions;

    /**
     * The element type
     */
    public int type = ANY;

    /**
     * The content model
     */
    public ContentModel content;

    /**
     * The attributes
     */
    public AttributeList atts;

    /**
     * A field to store user data. Mostly used to store
     * style sheets.
     */
    public Object data;

    Element() {
    }

    /**
     * Create a new element.
     *
     * @param name   the name of the element
     * @param index  the index
     */
    Element(String name, int index) {
        this.name = name;
        this.index = index;
        if (index > getMaxIndex()) {
            AppContext.getAppContext().put(MAX_INDEX_KEY, index);
        }
    }

    private static final Object MAX_INDEX_KEY = new Object();

    static int getMaxIndex() {
        Integer value = (Integer) AppContext.getAppContext().get(MAX_INDEX_KEY);
        return (value != null)
                ? value.intValue()
                : 0;
    }

    /**
     * Get the name of the element.
     *
     * @return  the name of the element
     */
    public String getName() {
        return name;
    }

    /**
     * Return true if the start tag can be omitted.
     *
     * @return  {@code true} if the start tag can be omitted
     */
    public boolean omitStart() {
        return oStart;
    }

    /**
     * Return true if the end tag can be omitted.
     *
     * @return  {@code true} if the end tag can be omitted
     */
    public boolean omitEnd() {
        return oEnd;
    }

    /**
     * Get type.
     *
     * @return  the type of the element
     */
    public int getType() {
        return type;
    }

    /**
     * Get content model
     *
     * @return  the content model
     */
    public ContentModel getContent() {
        return content;
    }

    /**
     * Get the attributes.
     *
     * @return  the {@code AttributeList} specifying the element
     */
    public AttributeList getAttributes() {
        return atts;
    }

    /**
     * Get index.
     *
     * @return the element index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Check if empty
     *
     * @return  true if the current element is empty
     */
    public boolean isEmpty() {
        return type == EMPTY;
    }

    /**
     * Convert to a string.
     *
     * @return  a string representation for the given {@code Element} instance
     */
    public String toString() {
        return name;
    }

    /**
     * Get an attribute by name.
     *
     * @param name  the attribute name
     *
     * @return the {@code AttributeList} for the given {@code name}
     */
    public AttributeList getAttribute(String name) {
        for (AttributeList a = atts ; a != null ; a = a.next) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Get an attribute by value.
     *
     * @param value  the string representation of value
     *
     * @return  the {@code AttributeList} for the given {@code value}
     */
    public AttributeList getAttributeByValue(String value) {
        for (AttributeList a = atts ; a != null ; a = a.next) {
            if ((a.values != null) && a.values.contains(value)) {
                return a;
            }
        }
        return null;
    }


    static Hashtable<String, Integer> contentTypes = new Hashtable<String, Integer>();

    static {
        contentTypes.put("CDATA", Integer.valueOf(CDATA));
        contentTypes.put("RCDATA", Integer.valueOf(RCDATA));
        contentTypes.put("EMPTY", Integer.valueOf(EMPTY));
        contentTypes.put("ANY", Integer.valueOf(ANY));
    }

    /**
     * Converts {@code nm} to type. Returns appropriate DTDConstants
     * if the {@code nm} is equal to CDATA, RCDATA, EMPTY or ANY, 0 otherwise.
     *
     * @param nm a name
     * @return appropriate DTDConstants if the {@code nm} is equal to
     * CDATA, RCDATA, EMPTY or ANY, 0 otherwise.
     */
    public static int name2type(String nm) {
        Integer val = contentTypes.get(nm);
        return (val != null) ? val.intValue() : 0;
    }
}
