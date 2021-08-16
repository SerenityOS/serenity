/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serializer;

import com.sun.org.apache.xml.internal.serializer.utils.StringToIntTable;

/**
 * This class has a series of flags (bit values) that describe an HTML element
 *
 * This class is public because XSLTC uses it, it is not a public API.
 *
 * @xsl.usage internal
 */
public final class ElemDesc
{
    /** Bit flags to tell about this element type. */
    private int m_flags;

    /**
     * Table of attribute names to integers, which contain bit flags telling about
     *  the attributes.
     */
    private StringToIntTable m_attrs = null;

    /** Bit position if this element type is empty. */
    static final int EMPTY = (1 << 1);

    /** Bit position if this element type is a flow. */
    private static final int FLOW = (1 << 2);

    /** Bit position if this element type is a block. */
    static final int BLOCK = (1 << 3);

    /** Bit position if this element type is a block form. */
    static final int BLOCKFORM = (1 << 4);

    /** Bit position if this element type is a block form field set. */
    static final int BLOCKFORMFIELDSET = (1 << 5);

    /** Bit position if this element type is CDATA. */
    private static final int CDATA = (1 << 6);

    /** Bit position if this element type is PCDATA. */
    private static final int PCDATA = (1 << 7);

    /** Bit position if this element type is should be raw characters. */
    static final int RAW = (1 << 8);

    /** Bit position if this element type should be inlined. */
    private static final int INLINE = (1 << 9);

    /** Bit position if this element type is INLINEA. */
    private static final int INLINEA = (1 << 10);

    /** Bit position if this element type is an inline label. */
    static final int INLINELABEL = (1 << 11);

    /** Bit position if this element type is a font style. */
    static final int FONTSTYLE = (1 << 12);

    /** Bit position if this element type is a phrase. */
    static final int PHRASE = (1 << 13);

    /** Bit position if this element type is a form control. */
    static final int FORMCTRL = (1 << 14);

    /** Bit position if this element type is ???. */
    static final int SPECIAL = (1 << 15);

    /** Bit position if this element type is ???. */
    static final int ASPECIAL = (1 << 16);

    /** Bit position if this element type is an odd header element. */
    static final int HEADMISC = (1 << 17);

    /** Bit position if this element type is a head element (i.e. H1, H2, etc.) */
    static final int HEAD = (1 << 18);

    /** Bit position if this element type is a list. */
    static final int LIST = (1 << 19);

    /** Bit position if this element type is a preformatted type. */
    static final int PREFORMATTED = (1 << 20);

    /** Bit position if this element type is whitespace sensitive. */
    static final int WHITESPACESENSITIVE = (1 << 21);

    /** Bit position if this element type is a header element (i.e. HEAD). */
    static final int HEADELEM = (1 << 22);

    /** Bit position if this element is the "HTML" element */
    private static final int HTMLELEM = (1 << 23);

    /** Bit position if this attribute type is a URL. */
    public static final int ATTRURL = (1 << 1);

    /** Bit position if this attribute type is an empty type. */
    public static final int ATTREMPTY = (1 << 2);

    /**
     * Construct an ElemDesc from a set of bit flags.
     *
     *
     * @param flags Bit flags that describe the basic properties of this element type.
     */
    ElemDesc(int flags)
    {
        m_flags = flags;
    }

    /**
     * Tell if this element type has the basic bit properties that are passed
     * as an argument.
     *
     * @param flags Bit flags that describe the basic properties of interest.
     *
     * @return true if any of the flag bits are true.
     */
    private boolean is(int flags)
    {

        // int which = (m_flags & flags);
        return (m_flags & flags) != 0;
    }

    int getFlags() {
        return m_flags;
    }

    /**
     * Set an attribute name and it's bit properties.
     *
     *
     * @param name non-null name of attribute, in upper case.
     * @param flags flag bits.
     */
    void setAttr(String name, int flags)
    {

        if (null == m_attrs)
            m_attrs = new StringToIntTable();

        m_attrs.put(name, flags);
    }

    /**
     * Tell if any of the bits of interest are set for a named attribute type.
     *
     * @param name non-null reference to attribute name, in any case.
     * @param flags flag mask.
     *
     * @return true if any of the flags are set for the named attribute.
     */
    public boolean isAttrFlagSet(String name, int flags)
    {
        return (null != m_attrs)
            ? ((m_attrs.getIgnoreCase(name) & flags) != 0)
            : false;
    }
}
