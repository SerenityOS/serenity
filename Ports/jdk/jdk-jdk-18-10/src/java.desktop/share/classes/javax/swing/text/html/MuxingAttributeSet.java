/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import javax.swing.text.*;
import java.io.Serializable;
import java.util.*;

/**
 * An implementation of <code>AttributeSet</code> that can multiplex
 * across a set of <code>AttributeSet</code>s.
 *
 */
@SuppressWarnings("serial") // Same-version serialization only
class MuxingAttributeSet implements AttributeSet, Serializable {
    /**
     * Creates a <code>MuxingAttributeSet</code> with the passed in
     * attributes.
     */
    public MuxingAttributeSet(AttributeSet[] attrs) {
        this.attrs = attrs;
    }

    /**
     * Creates an empty <code>MuxingAttributeSet</code>. This is intended for
     * use by subclasses only, and it is also intended that subclasses will
     * set the constituent <code>AttributeSet</code>s before invoking any
     * of the <code>AttributeSet</code> methods.
     */
    protected MuxingAttributeSet() {
    }

    /**
     * Directly sets the <code>AttributeSet</code>s that comprise this
     * <code>MuxingAttributeSet</code>.
     */
    protected synchronized void setAttributes(AttributeSet[] attrs) {
        this.attrs = attrs;
    }

    /**
     * Returns the <code>AttributeSet</code>s multiplexing too. When the
     * <code>AttributeSet</code>s need to be referenced, this should be called.
     */
    protected synchronized AttributeSet[] getAttributes() {
        return attrs;
    }

    /**
     * Inserts <code>as</code> at <code>index</code>. This assumes
     * the value of <code>index</code> is between 0 and attrs.length,
     * inclusive.
     */
    protected synchronized void insertAttributeSetAt(AttributeSet as,
                                                     int index) {
        int numAttrs = attrs.length;
        AttributeSet[] newAttrs = new AttributeSet[numAttrs + 1];
        if (index < numAttrs) {
            if (index > 0) {
                System.arraycopy(attrs, 0, newAttrs, 0, index);
                System.arraycopy(attrs, index, newAttrs, index + 1,
                                 numAttrs - index);
            }
            else {
                System.arraycopy(attrs, 0, newAttrs, 1, numAttrs);
            }
        }
        else {
            System.arraycopy(attrs, 0, newAttrs, 0, numAttrs);
        }
        newAttrs[index] = as;
        attrs = newAttrs;
    }

    /**
     * Removes the AttributeSet at <code>index</code>. This assumes
     * the value of <code>index</code> is greater than or equal to 0,
     * and less than attrs.length.
     */
    protected synchronized void removeAttributeSetAt(int index) {
        int numAttrs = attrs.length;
        AttributeSet[] newAttrs = new AttributeSet[numAttrs - 1];
        if (numAttrs > 0) {
            if (index == 0) {
                // FIRST
                System.arraycopy(attrs, 1, newAttrs, 0, numAttrs - 1);
            }
            else if (index < (numAttrs - 1)) {
                // MIDDLE
                System.arraycopy(attrs, 0, newAttrs, 0, index);
                System.arraycopy(attrs, index + 1, newAttrs, index,
                                 numAttrs - index - 1);
            }
            else {
                // END
                System.arraycopy(attrs, 0, newAttrs, 0, numAttrs - 1);
            }
        }
        attrs = newAttrs;
    }

    //  --- AttributeSet methods ----------------------------

    /**
     * Gets the number of attributes that are defined.
     *
     * @return the number of attributes
     * @see AttributeSet#getAttributeCount
     */
    public int getAttributeCount() {
        AttributeSet[] as = getAttributes();
        int n = 0;
        for (int i = 0; i < as.length; i++) {
            n += as[i].getAttributeCount();
        }
        return n;
    }

    /**
     * Checks whether a given attribute is defined.
     * This will convert the key over to CSS if the
     * key is a StyleConstants key that has a CSS
     * mapping.
     *
     * @param key the attribute key
     * @return true if the attribute is defined
     * @see AttributeSet#isDefined
     */
    public boolean isDefined(Object key) {
        AttributeSet[] as = getAttributes();
        for (int i = 0; i < as.length; i++) {
            if (as[i].isDefined(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether two attribute sets are equal.
     *
     * @param attr the attribute set to check against
     * @return true if the same
     * @see AttributeSet#isEqual
     */
    public boolean isEqual(AttributeSet attr) {
        return ((getAttributeCount() == attr.getAttributeCount()) &&
                containsAttributes(attr));
    }

    /**
     * Copies a set of attributes.
     *
     * @return the copy
     * @see AttributeSet#copyAttributes
     */
    public AttributeSet copyAttributes() {
        AttributeSet[] as = getAttributes();
        MutableAttributeSet a = new SimpleAttributeSet();
        int n = 0;
        for (int i = as.length - 1; i >= 0; i--) {
            a.addAttributes(as[i]);
        }
        return a;
    }

    /**
     * Gets the value of an attribute.  If the requested
     * attribute is a StyleConstants attribute that has
     * a CSS mapping, the request will be converted.
     *
     * @param key the attribute name
     * @return the attribute value
     * @see AttributeSet#getAttribute
     */
    public Object getAttribute(Object key) {
        AttributeSet[] as = getAttributes();
        int n = as.length;
        for (int i = 0; i < n; i++) {
            Object o = as[i].getAttribute(key);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    /**
     * Gets the names of all attributes.
     *
     * @return the attribute names
     * @see AttributeSet#getAttributeNames
     */
    public Enumeration<?> getAttributeNames() {
        return new MuxingAttributeNameEnumeration();
    }

    /**
     * Checks whether a given attribute name/value is defined.
     *
     * @param name the attribute name
     * @param value the attribute value
     * @return true if the name/value is defined
     * @see AttributeSet#containsAttribute
     */
    public boolean containsAttribute(Object name, Object value) {
        return value.equals(getAttribute(name));
    }

    /**
     * Checks whether the attribute set contains all of
     * the given attributes.
     *
     * @param attrs the attributes to check
     * @return true if the element contains all the attributes
     * @see AttributeSet#containsAttributes
     */
    public boolean containsAttributes(AttributeSet attrs) {
        boolean result = true;

        Enumeration<?> names = attrs.getAttributeNames();
        while (result && names.hasMoreElements()) {
            Object name = names.nextElement();
            result = attrs.getAttribute(name).equals(getAttribute(name));
        }

        return result;
    }

    /**
     * Returns null, subclasses may wish to do something more
     * intelligent with this.
     */
    public AttributeSet getResolveParent() {
        return null;
    }

    /**
     * The <code>AttributeSet</code>s that make up the resulting
     * <code>AttributeSet</code>.
     */
    private AttributeSet[] attrs;


    /**
     * An Enumeration of the Attribute names in a MuxingAttributeSet.
     * This may return the same name more than once.
     */
    private class MuxingAttributeNameEnumeration implements Enumeration<Object> {

        MuxingAttributeNameEnumeration() {
            updateEnum();
        }

        public boolean hasMoreElements() {
            if (currentEnum == null) {
                return false;
            }
            return currentEnum.hasMoreElements();
        }

        public Object nextElement() {
            if (currentEnum == null) {
                throw new NoSuchElementException("No more names");
            }
            Object retObject = currentEnum.nextElement();
            if (!currentEnum.hasMoreElements()) {
                updateEnum();
            }
            return retObject;
        }

        void updateEnum() {
            AttributeSet[] as = getAttributes();
            currentEnum = null;
            while (currentEnum == null && attrIndex < as.length) {
                currentEnum = as[attrIndex++].getAttributeNames();
                if (!currentEnum.hasMoreElements()) {
                    currentEnum = null;
                }
            }
        }


        /** Index into attrs the current Enumeration came from. */
        private int attrIndex;
        /** Enumeration from attrs. */
        private Enumeration<?> currentEnum;
    }
}
