/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.xml.sax.helpers;

import org.xml.sax.Attributes;


/**
 * Default implementation of the Attributes interface.
 *
 * <p>This class provides a default implementation of the SAX2
 * {@link org.xml.sax.Attributes Attributes} interface, with the
 * addition of manipulators so that the list can be modified or
 * reused.</p>
 *
 * <p>There are two typical uses of this class:</p>
 *
 * <ol>
 * <li>to take a persistent snapshot of an Attributes object
 *  in a {@link org.xml.sax.ContentHandler#startElement startElement} event; or</li>
 * <li>to construct or modify an Attributes object in a SAX2 driver or filter.</li>
 * </ol>
 *
 * <p>This class replaces the now-deprecated SAX1 {@link
 * org.xml.sax.helpers.AttributeListImpl AttributeListImpl}
 * class; in addition to supporting the updated Attributes
 * interface rather than the deprecated {@link org.xml.sax.AttributeList
 * AttributeList} interface, it also includes a much more efficient
 * implementation using a single array rather than a set of Vectors.</p>
 *
 * @since 1.4, SAX 2.0
 * @author David Megginson
 */
public class AttributesImpl implements Attributes
{


    ////////////////////////////////////////////////////////////////////
    // Constructors.
    ////////////////////////////////////////////////////////////////////


    /**
     * Construct a new, empty AttributesImpl object.
     */
    public AttributesImpl ()
    {
        length = 0;
        data = null;
    }


    /**
     * Copy an existing Attributes object.
     *
     * <p>This constructor is especially useful inside a
     * {@link org.xml.sax.ContentHandler#startElement startElement} event.</p>
     *
     * @param atts The existing Attributes object.
     */
    public AttributesImpl (Attributes atts)
    {
        setAttributes(atts);
    }



    ////////////////////////////////////////////////////////////////////
    // Implementation of org.xml.sax.Attributes.
    ////////////////////////////////////////////////////////////////////


    /**
     * Return the number of attributes in the list.
     *
     * @return The number of attributes in the list.
     * @see org.xml.sax.Attributes#getLength
     */
    public int getLength ()
    {
        return length;
    }


    /**
     * Return an attribute's Namespace URI.
     *
     * @param index The attribute's index (zero-based).
     * @return The Namespace URI, the empty string if none is
     *         available, or null if the index is out of range.
     * @see org.xml.sax.Attributes#getURI
     */
    public String getURI (int index)
    {
        if (index >= 0 && index < length) {
            return data[index*5];
        } else {
            return null;
        }
    }


    /**
     * Return an attribute's local name.
     *
     * @param index The attribute's index (zero-based).
     * @return The attribute's local name, the empty string if
     *         none is available, or null if the index if out of range.
     * @see org.xml.sax.Attributes#getLocalName
     */
    public String getLocalName (int index)
    {
        if (index >= 0 && index < length) {
            return data[index*5+1];
        } else {
            return null;
        }
    }


    /**
     * Return an attribute's qualified (prefixed) name.
     *
     * @param index The attribute's index (zero-based).
     * @return The attribute's qualified name, the empty string if
     *         none is available, or null if the index is out of bounds.
     * @see org.xml.sax.Attributes#getQName
     */
    public String getQName (int index)
    {
        if (index >= 0 && index < length) {
            return data[index*5+2];
        } else {
            return null;
        }
    }


    /**
     * Return an attribute's type by index.
     *
     * @param index The attribute's index (zero-based).
     * @return The attribute's type, "CDATA" if the type is unknown, or null
     *         if the index is out of bounds.
     * @see org.xml.sax.Attributes#getType(int)
     */
    public String getType (int index)
    {
        if (index >= 0 && index < length) {
            return data[index*5+3];
        } else {
            return null;
        }
    }


    /**
     * Return an attribute's value by index.
     *
     * @param index The attribute's index (zero-based).
     * @return The attribute's value or null if the index is out of bounds.
     * @see org.xml.sax.Attributes#getValue(int)
     */
    public String getValue (int index)
    {
        if (index >= 0 && index < length) {
            return data[index*5+4];
        } else {
            return null;
        }
    }


    /**
     * Look up an attribute's index by Namespace name.
     *
     * <p>In many cases, it will be more efficient to look up the name once and
     * use the index query methods rather than using the name query methods
     * repeatedly.</p>
     *
     * @param uri The attribute's Namespace URI, or the empty
     *        string if none is available.
     * @param localName The attribute's local name.
     * @return The attribute's index, or -1 if none matches.
     * @see org.xml.sax.Attributes#getIndex(java.lang.String,java.lang.String)
     */
    public int getIndex (String uri, String localName)
    {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i+1].equals(localName)) {
                return i / 5;
            }
        }
        return -1;
    }


    /**
     * Look up an attribute's index by qualified (prefixed) name.
     *
     * @param qName The qualified name.
     * @return The attribute's index, or -1 if none matches.
     * @see org.xml.sax.Attributes#getIndex(java.lang.String)
     */
    public int getIndex (String qName)
    {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i+2].equals(qName)) {
                return i / 5;
            }
        }
        return -1;
    }


    /**
     * Look up an attribute's type by Namespace-qualified name.
     *
     * @param uri The Namespace URI, or the empty string for a name
     *        with no explicit Namespace URI.
     * @param localName The local name.
     * @return The attribute's type, or null if there is no
     *         matching attribute.
     * @see org.xml.sax.Attributes#getType(java.lang.String,java.lang.String)
     */
    public String getType (String uri, String localName)
    {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i+1].equals(localName)) {
                return data[i+3];
            }
        }
        return null;
    }


    /**
     * Look up an attribute's type by qualified (prefixed) name.
     *
     * @param qName The qualified name.
     * @return The attribute's type, or null if there is no
     *         matching attribute.
     * @see org.xml.sax.Attributes#getType(java.lang.String)
     */
    public String getType (String qName)
    {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i+2].equals(qName)) {
                return data[i+3];
            }
        }
        return null;
    }


    /**
     * Look up an attribute's value by Namespace-qualified name.
     *
     * @param uri The Namespace URI, or the empty string for a name
     *        with no explicit Namespace URI.
     * @param localName The local name.
     * @return The attribute's value, or null if there is no
     *         matching attribute.
     * @see org.xml.sax.Attributes#getValue(java.lang.String,java.lang.String)
     */
    public String getValue (String uri, String localName)
    {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i].equals(uri) && data[i+1].equals(localName)) {
                return data[i+4];
            }
        }
        return null;
    }


    /**
     * Look up an attribute's value by qualified (prefixed) name.
     *
     * @param qName The qualified name.
     * @return The attribute's value, or null if there is no
     *         matching attribute.
     * @see org.xml.sax.Attributes#getValue(java.lang.String)
     */
    public String getValue (String qName)
    {
        int max = length * 5;
        for (int i = 0; i < max; i += 5) {
            if (data[i+2].equals(qName)) {
                return data[i+4];
            }
        }
        return null;
    }



    ////////////////////////////////////////////////////////////////////
    // Manipulators.
    ////////////////////////////////////////////////////////////////////


    /**
     * Clear the attribute list for reuse.
     *
     * <p>Note that little memory is freed by this call:
     * the current array is kept so it can be
     * reused.</p>
     */
    public void clear ()
    {
        if (data != null) {
            for (int i = 0; i < (length * 5); i++)
                data [i] = null;
        }
        length = 0;
    }


    /**
     * Copy an entire Attributes object.
     *
     * <p>It may be more efficient to reuse an existing object
     * rather than constantly allocating new ones.</p>
     *
     * @param atts The attributes to copy.
     */
    public void setAttributes (Attributes atts)
    {
        clear();
        length = atts.getLength();
        if (length > 0) {
            data = new String[length*5];
            for (int i = 0; i < length; i++) {
                data[i*5] = atts.getURI(i);
                data[i*5+1] = atts.getLocalName(i);
                data[i*5+2] = atts.getQName(i);
                data[i*5+3] = atts.getType(i);
                data[i*5+4] = atts.getValue(i);
            }
        }
    }


    /**
     * Add an attribute to the end of the list.
     *
     * <p>For the sake of speed, this method does no checking
     * to see if the attribute is already in the list: that is
     * the responsibility of the application.</p>
     *
     * @param uri The Namespace URI, or the empty string if
     *        none is available or Namespace processing is not
     *        being performed.
     * @param localName The local name, or the empty string if
     *        Namespace processing is not being performed.
     * @param qName The qualified (prefixed) name, or the empty string
     *        if qualified names are not available.
     * @param type The attribute type as a string.
     * @param value The attribute value.
     */
    public void addAttribute (String uri, String localName, String qName,
                              String type, String value)
    {
        ensureCapacity(length+1);
        data[length*5] = uri;
        data[length*5+1] = localName;
        data[length*5+2] = qName;
        data[length*5+3] = type;
        data[length*5+4] = value;
        length++;
    }


    /**
     * Set an attribute in the list.
     *
     * <p>For the sake of speed, this method does no checking
     * for name conflicts or well-formedness: such checks are the
     * responsibility of the application.</p>
     *
     * @param index The index of the attribute (zero-based).
     * @param uri The Namespace URI, or the empty string if
     *        none is available or Namespace processing is not
     *        being performed.
     * @param localName The local name, or the empty string if
     *        Namespace processing is not being performed.
     * @param qName The qualified name, or the empty string
     *        if qualified names are not available.
     * @param type The attribute type as a string.
     * @param value The attribute value.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not point to an attribute
     *            in the list.
     */
    public void setAttribute (int index, String uri, String localName,
                              String qName, String type, String value)
    {
        if (index >= 0 && index < length) {
            data[index*5] = uri;
            data[index*5+1] = localName;
            data[index*5+2] = qName;
            data[index*5+3] = type;
            data[index*5+4] = value;
        } else {
            badIndex(index);
        }
    }


    /**
     * Remove an attribute from the list.
     *
     * @param index The index of the attribute (zero-based).
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not point to an attribute
     *            in the list.
     */
    public void removeAttribute (int index)
    {
        if (index >= 0 && index < length) {
            if (index < length - 1) {
                System.arraycopy(data, (index+1)*5, data, index*5,
                                 (length-index-1)*5);
            }
            index = (length - 1) * 5;
            data [index++] = null;
            data [index++] = null;
            data [index++] = null;
            data [index++] = null;
            data [index] = null;
            length--;
        } else {
            badIndex(index);
        }
    }


    /**
     * Set the Namespace URI of a specific attribute.
     *
     * @param index The index of the attribute (zero-based).
     * @param uri The attribute's Namespace URI, or the empty
     *        string for none.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not point to an attribute
     *            in the list.
     */
    public void setURI (int index, String uri)
    {
        if (index >= 0 && index < length) {
            data[index*5] = uri;
        } else {
            badIndex(index);
        }
    }


    /**
     * Set the local name of a specific attribute.
     *
     * @param index The index of the attribute (zero-based).
     * @param localName The attribute's local name, or the empty
     *        string for none.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not point to an attribute
     *            in the list.
     */
    public void setLocalName (int index, String localName)
    {
        if (index >= 0 && index < length) {
            data[index*5+1] = localName;
        } else {
            badIndex(index);
        }
    }


    /**
     * Set the qualified name of a specific attribute.
     *
     * @param index The index of the attribute (zero-based).
     * @param qName The attribute's qualified name, or the empty
     *        string for none.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not point to an attribute
     *            in the list.
     */
    public void setQName (int index, String qName)
    {
        if (index >= 0 && index < length) {
            data[index*5+2] = qName;
        } else {
            badIndex(index);
        }
    }


    /**
     * Set the type of a specific attribute.
     *
     * @param index The index of the attribute (zero-based).
     * @param type The attribute's type.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not point to an attribute
     *            in the list.
     */
    public void setType (int index, String type)
    {
        if (index >= 0 && index < length) {
            data[index*5+3] = type;
        } else {
            badIndex(index);
        }
    }


    /**
     * Set the value of a specific attribute.
     *
     * @param index The index of the attribute (zero-based).
     * @param value The attribute's value.
     * @throws java.lang.ArrayIndexOutOfBoundsException When the
     *            supplied index does not point to an attribute
     *            in the list.
     */
    public void setValue (int index, String value)
    {
        if (index >= 0 && index < length) {
            data[index*5+4] = value;
        } else {
            badIndex(index);
        }
    }



    ////////////////////////////////////////////////////////////////////
    // Internal methods.
    ////////////////////////////////////////////////////////////////////


    /**
     * Ensure the internal array's capacity.
     *
     * @param n The minimum number of attributes that the array must
     *        be able to hold.
     */
    private void ensureCapacity (int n)    {
        if (n <= 0) {
            return;
        }
        int max;
        if (data == null || data.length == 0) {
            max = 25;
        }
        else if (data.length >= n * 5) {
            return;
        }
        else {
            max = data.length;
        }
        while (max < n * 5) {
            max *= 2;
        }

        String newData[] = new String[max];
        if (length > 0) {
            System.arraycopy(data, 0, newData, 0, length*5);
        }
        data = newData;
    }


    /**
     * Report a bad array index in a manipulator.
     *
     * @param index The index to report.
     * @throws java.lang.ArrayIndexOutOfBoundsException Always.
     */
    private void badIndex (int index)
        throws ArrayIndexOutOfBoundsException
    {
        String msg =
            "Attempt to modify attribute at illegal index: " + index;
        throw new ArrayIndexOutOfBoundsException(msg);
    }



    ////////////////////////////////////////////////////////////////////
    // Internal state.
    ////////////////////////////////////////////////////////////////////

    int length;
    String data [];

}

// end of AttributesImpl.java
