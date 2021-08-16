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
import org.xml.sax.helpers.AttributesImpl;


/**
 * SAX2 extension helper for additional Attributes information,
 * implementing the {@link Attributes2} interface.
 *
 * <p>This is not part of core-only SAX2 distributions.</p>
 *
 * <p>The <em>specified</em> flag for each attribute will always
 * be true, unless it has been set to false in the copy constructor
 * or using {@link #setSpecified}.
 * Similarly, the <em>declared</em> flag for each attribute will
 * always be false, except for defaulted attributes (<em>specified</em>
 * is false), non-CDATA attributes, or when it is set to true using
 * {@link #setDeclared}.
 * If you change an attribute's type by hand, you may need to modify
 * its <em>declared</em> flag to match.
 * </p>
 *
 * @since 1.5, SAX 2.0 (extensions 1.1 alpha)
 * @author David Brownell
 */
public class Attributes2Impl extends AttributesImpl implements Attributes2
{
    private boolean     declared [];
    private boolean     specified [];


    /**
     * Construct a new, empty Attributes2Impl object.
     */
    public Attributes2Impl () {
        specified = null;
        declared = null;
    }


    /**
     * Copy an existing Attributes or Attributes2 object.
     * If the object implements Attributes2, values of the
     * <em>specified</em> and <em>declared</em> flags for each
     * attribute are copied.
     * Otherwise the flag values are defaulted to assume no DTD was used,
     * unless there is evidence to the contrary (such as attributes with
     * type other than CDATA, which must have been <em>declared</em>).
     *
     * <p>This constructor is especially useful inside a
     * {@link org.xml.sax.ContentHandler#startElement startElement} event.</p>
     *
     * @param atts The existing Attributes object.
     */
    public Attributes2Impl (Attributes atts)
    {
        super (atts);
    }


    ////////////////////////////////////////////////////////////////////
    // Implementation of Attributes2
    ////////////////////////////////////////////////////////////////////


    /**
     * Returns the current value of the attribute's "declared" flag.
     */
    // javadoc mostly from interface
    public boolean isDeclared (int index)
    {
        if (index < 0 || index >= getLength ())
            throw new ArrayIndexOutOfBoundsException (
                "No attribute at index: " + index);
        return declared [index];
    }


    /**
     * Returns the current value of the attribute's "declared" flag.
     */
    // javadoc mostly from interface
    public boolean isDeclared (String uri, String localName)
    {
        int index = getIndex (uri, localName);

        if (index < 0)
            throw new IllegalArgumentException (
                "No such attribute: local=" + localName
                + ", namespace=" + uri);
        return declared [index];
    }


    /**
     * Returns the current value of the attribute's "declared" flag.
     */
    // javadoc mostly from interface
    public boolean isDeclared (String qName)
    {
        int index = getIndex (qName);

        if (index < 0)
            throw new IllegalArgumentException (
                "No such attribute: " + qName);
        return declared [index];
    }


    /**
     * Returns the current value of an attribute's "specified" flag.
     *
     * @param index The attribute index (zero-based).
     * @return current flag value
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not identify an attribute.
     */
    public boolean isSpecified (int index)
    {
        if (index < 0 || index >= getLength ())
            throw new ArrayIndexOutOfBoundsException (
                "No attribute at index: " + index);
        return specified [index];
    }


    /**
     * Returns the current value of an attribute's "specified" flag.
     *
     * @param uri The Namespace URI, or the empty string if
     *        the name has no Namespace URI.
     * @param localName The attribute's local name.
     * @return current flag value
     * @throws java.lang.IllegalArgumentException When the
     *            supplied names do not identify an attribute.
     */
    public boolean isSpecified (String uri, String localName)
    {
        int index = getIndex (uri, localName);

        if (index < 0)
            throw new IllegalArgumentException (
                "No such attribute: local=" + localName
                + ", namespace=" + uri);
        return specified [index];
    }


    /**
     * Returns the current value of an attribute's "specified" flag.
     *
     * @param qName The XML qualified (prefixed) name.
     * @return current flag value
     * @throws java.lang.IllegalArgumentException When the
     *            supplied name does not identify an attribute.
     */
    public boolean isSpecified (String qName)
    {
        int index = getIndex (qName);

        if (index < 0)
            throw new IllegalArgumentException (
                "No such attribute: " + qName);
        return specified [index];
    }


    ////////////////////////////////////////////////////////////////////
    // Manipulators
    ////////////////////////////////////////////////////////////////////


    /**
     * Copy an entire Attributes object.  The "specified" flags are
     * assigned as true, and "declared" flags as false (except when
     * an attribute's type is not CDATA),
     * unless the object is an Attributes2 object.
     * In that case those flag values are all copied.
     *
     * @see AttributesImpl#setAttributes
     */
    public void setAttributes (Attributes atts)
    {
        int length = atts.getLength ();

        super.setAttributes (atts);
        declared = new boolean [length];
        specified = new boolean [length];

        if (atts instanceof Attributes2) {
            Attributes2 a2 = (Attributes2) atts;
            for (int i = 0; i < length; i++) {
                declared [i] = a2.isDeclared (i);
                specified [i] = a2.isSpecified (i);
            }
        } else {
            for (int i = 0; i < length; i++) {
                declared [i] = !"CDATA".equals (atts.getType (i));
                specified [i] = true;
            }
        }
    }


    /**
     * Add an attribute to the end of the list, setting its
     * "specified" flag to true.  To set that flag's value
     * to false, use {@link #setSpecified}.
     *
     * <p>Unless the attribute <em>type</em> is CDATA, this attribute
     * is marked as being declared in the DTD.  To set that flag's value
     * to true for CDATA attributes, use {@link #setDeclared}.
     *
     * @see AttributesImpl#addAttribute
     */
    public void addAttribute (String uri, String localName, String qName,
                              String type, String value)
    {
        super.addAttribute (uri, localName, qName, type, value);


        int length = getLength ();
        if(specified==null)
        {
            specified = new boolean[length];
            declared = new boolean[length];
        } else if (length > specified.length) {
            boolean     newFlags [];

            newFlags = new boolean [length];
            System.arraycopy (declared, 0, newFlags, 0, declared.length);
            declared = newFlags;

            newFlags = new boolean [length];
            System.arraycopy (specified, 0, newFlags, 0, specified.length);
            specified = newFlags;
        }

        specified [length - 1] = true;
        declared [length - 1] = !"CDATA".equals (type);
    }


    // javadoc entirely from superclass
    public void removeAttribute (int index)
    {
        int origMax = getLength () - 1;

        super.removeAttribute (index);
        if (index != origMax) {
            System.arraycopy (declared, index + 1, declared, index,
                    origMax - index);
            System.arraycopy (specified, index + 1, specified, index,
                    origMax - index);
        }
    }


    /**
     * Assign a value to the "declared" flag of a specific attribute.
     * This is normally needed only for attributes of type CDATA,
     * including attributes whose type is changed to or from CDATA.
     *
     * @param index The index of the attribute (zero-based).
     * @param value The desired flag value.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not identify an attribute.
     * @see #setType
     */
    public void setDeclared (int index, boolean value)
    {
        if (index < 0 || index >= getLength ())
            throw new ArrayIndexOutOfBoundsException (
                "No attribute at index: " + index);
        declared [index] = value;
    }


    /**
     * Assign a value to the "specified" flag of a specific attribute.
     * This is the only way this flag can be cleared, except clearing
     * by initialization with the copy constructor.
     *
     * @param index The index of the attribute (zero-based).
     * @param value The desired flag value.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not identify an attribute.
     */
    public void setSpecified (int index, boolean value)
    {
        if (index < 0 || index >= getLength ())
            throw new ArrayIndexOutOfBoundsException (
                "No attribute at index: " + index);
        specified [index] = value;
    }
}
