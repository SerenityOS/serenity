/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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


package javax.naming.directory;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Locale;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

/**
  * This class provides a basic implementation
  * of the Attributes interface.
  *<p>
  * BasicAttributes is either case-sensitive or case-insensitive (case-ignore).
  * This property is determined at the time the BasicAttributes constructor
  * is called.
  * In a case-insensitive BasicAttributes, the case of its attribute identifiers
  * is ignored when searching for an attribute, or adding attributes.
  * In a case-sensitive BasicAttributes, the case is significant.
  *<p>
  * When the BasicAttributes class needs to create an Attribute, it
  * uses BasicAttribute. There is no other dependency on BasicAttribute.
  *<p>
  * Note that updates to BasicAttributes (such as adding or removing an attribute)
  * does not affect the corresponding representation in the directory.
  * Updates to the directory can only be effected
  * using operations in the DirContext interface.
  *<p>
  * A BasicAttributes instance is not synchronized against concurrent
  * multithreaded access. Multiple threads trying to access and modify
  * a single BasicAttributes instance should lock the object.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see DirContext#getAttributes
  * @see DirContext#modifyAttributes
  * @see DirContext#bind
  * @see DirContext#rebind
  * @see DirContext#createSubcontext
  * @see DirContext#search
  * @since 1.3
  */

public class BasicAttributes implements Attributes {
    /**
     * Indicates whether case of attribute ids is ignored.
     * @serial
     */
    private boolean ignoreCase = false;

    // The 'key' in attrs is stored in the 'right case'.
    // If ignoreCase is true, key is aways lowercase.
    // If ignoreCase is false, key is stored as supplied by put().
    // %%% Not declared "private" due to bug 4064984.
    transient Hashtable<String,Attribute> attrs = new Hashtable<>(11);

    /**
      * Constructs a new instance of Attributes.
      * The character case of attribute identifiers
      * is significant when subsequently retrieving or adding attributes.
      */
    public BasicAttributes() {
    }

    /**
      * Constructs a new instance of Attributes.
      * If <code>ignoreCase</code> is true, the character case of attribute
      * identifiers is ignored; otherwise the case is significant.
      * @param ignoreCase true means this attribute set will ignore
      *                   the case of its attribute identifiers
      *                   when retrieving or adding attributes;
      *                   false means case is respected.
      */
    public BasicAttributes(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
      * Constructs a new instance of Attributes with one attribute.
      * The attribute specified by attrID and val are added to the newly
      * created attribute.
      * The character case of attribute identifiers
      * is significant when subsequently retrieving or adding attributes.
      * @param attrID   non-null The id of the attribute to add.
      * @param val The value of the attribute to add. If null, a null
      *        value is added to the attribute.
      */
    public BasicAttributes(String attrID, Object val) {
        this();
        this.put(new BasicAttribute(attrID, val));
    }

    /**
      * Constructs a new instance of Attributes with one attribute.
      * The attribute specified by attrID and val are added to the newly
      * created attribute.
      * If <code>ignoreCase</code> is true, the character case of attribute
      * identifiers is ignored; otherwise the case is significant.
      * @param attrID   non-null The id of the attribute to add.
      *           If this attribute set ignores the character
      *           case of its attribute ids, the case of attrID
      *           is ignored.
      * @param val The value of the attribute to add. If null, a null
      *        value is added to the attribute.
      * @param ignoreCase true means this attribute set will ignore
      *                   the case of its attribute identifiers
      *                   when retrieving or adding attributes;
      *                   false means case is respected.
      */
    public BasicAttributes(String attrID, Object val, boolean ignoreCase) {
        this(ignoreCase);
        this.put(new BasicAttribute(attrID, val));
    }

    @SuppressWarnings("unchecked")
    public Object clone() {
        BasicAttributes attrset;
        try {
            attrset = (BasicAttributes)super.clone();
        } catch (CloneNotSupportedException e) {
            attrset = new BasicAttributes(ignoreCase);
        }
        attrset.attrs = (Hashtable<String,Attribute>)attrs.clone();
        return attrset;
    }

    public boolean isCaseIgnored() {
        return ignoreCase;
    }

    public int size() {
        return attrs.size();
    }

    public Attribute get(String attrID) {
        Attribute attr = attrs.get(
                ignoreCase ? attrID.toLowerCase(Locale.ENGLISH) : attrID);
        return (attr);
    }

    public NamingEnumeration<Attribute> getAll() {
        return new AttrEnumImpl();
    }

    public NamingEnumeration<String> getIDs() {
        return new IDEnumImpl();
    }

    public Attribute put(String attrID, Object val) {
        return this.put(new BasicAttribute(attrID, val));
    }

    public Attribute put(Attribute attr) {
        String id = attr.getID();
        if (ignoreCase) {
            id = id.toLowerCase(Locale.ENGLISH);
        }
        return attrs.put(id, attr);
    }

    public Attribute remove(String attrID) {
        String id = (ignoreCase ? attrID.toLowerCase(Locale.ENGLISH) : attrID);
        return attrs.remove(id);
    }

    /**
     * Generates the string representation of this attribute set.
     * The string consists of each attribute identifier and the contents
     * of each attribute. The contents of this string is useful
     * for debugging and is not meant to be interpreted programmatically.
     *
     * @return A non-null string listing the contents of this attribute set.
     */
    public String toString() {
        if (attrs.size() == 0) {
            return("No attributes");
        } else {
            return attrs.toString();
        }
    }

    /**
     * Determines whether this {@code BasicAttributes} is equal to another
     * {@code Attributes}
     * Two {@code Attributes} are equal if they are both instances of
     * {@code Attributes},
     * treat the case of attribute IDs the same way, and contain the
     * same attributes. Each {@code Attribute} in this {@code BasicAttributes}
     * is checked for equality using {@code Object.equals()}, which may have
     * be overridden by implementations of {@code Attribute}).
     * If a subclass overrides {@code equals()},
     * it should override {@code hashCode()}
     * as well so that two {@code Attributes} instances that are equal
     * have the same hash code.
     * @param obj the possibly null object to compare against.
     *
     * @return true If obj is equal to this BasicAttributes.
     * @see #hashCode
     */
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof Attributes)) {
            Attributes target = (Attributes)obj;

            // Check case first
            if (ignoreCase != target.isCaseIgnored()) {
                return false;
            }

            if (size() == target.size()) {
                Attribute their, mine;
                try {
                    NamingEnumeration<?> theirs = target.getAll();
                    while (theirs.hasMore()) {
                        their = (Attribute)theirs.next();
                        mine = get(their.getID());
                        if (!their.equals(mine)) {
                            return false;
                        }
                    }
                } catch (NamingException e) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the hash code of this BasicAttributes.
     *<p>
     * The hash code is computed by adding the hash code of
     * the attributes of this object. If this BasicAttributes
     * ignores case of its attribute IDs, one is added to the hash code.
     * If a subclass overrides {@code hashCode()},
     * it should override {@code equals()}
     * as well so that two {@code Attributes} instances that are equal
     * have the same hash code.
     *
     * @return an int representing the hash code of this BasicAttributes instance.
     * @see #equals
     */
    public int hashCode() {
        int hash = (ignoreCase ? 1 : 0);
        try {
            NamingEnumeration<?> all = getAll();
            while (all.hasMore()) {
                hash += all.next().hashCode();
            }
        } catch (NamingException e) {}
        return hash;
    }

    /**
     * The writeObject method is called to save the state of the
     * {@code BasicAttributes} to a stream.
     *
     * @serialData Default field (ignoreCase flag - a {@code boolean}), followed by
     * the number of attributes in the set
     * (an {@code int}), and then the individual {@code Attribute} objects.
     *
     * @param s the {@code ObjectOutputStream} to write to
     * @throws java.io.IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Overridden to avoid exposing implementation details
        s.defaultWriteObject(); // write out the ignoreCase flag
        s.writeInt(attrs.size());
        Enumeration<Attribute> attrEnum = attrs.elements();
        while (attrEnum.hasMoreElements()) {
            s.writeObject(attrEnum.nextElement());
        }
    }

    /**
     * The readObject method is called to restore the state of
     * the {@code BasicAttributes} from a stream.
     *
     * See {@code writeObject} for a description of the serial form.
     *
     * @param s the {@code ObjectInputStream} to read from
     * @throws java.io.IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Overridden to avoid exposing implementation details.
        s.defaultReadObject();  // read in the ignoreCase flag
        int n = s.readInt();    // number of attributes
        attrs = (n >= 1)
                ? new Hashtable<>(1 + (int) (Math.min(768, n) / .75f))
                : new Hashtable<>(2); // can't have initial size of 0 (grrr...)
        while (--n >= 0) {
            put((Attribute)s.readObject());
        }
    }


class AttrEnumImpl implements NamingEnumeration<Attribute> {

    Enumeration<Attribute> elements;

    public AttrEnumImpl() {
        this.elements = attrs.elements();
    }

    public boolean hasMoreElements() {
        return elements.hasMoreElements();
    }

    public Attribute nextElement() {
        return elements.nextElement();
    }

    public boolean hasMore() throws NamingException {
        return hasMoreElements();
    }

    public Attribute next() throws NamingException {
        return nextElement();
    }

    public void close() throws NamingException {
        elements = null;
    }
}

class IDEnumImpl implements NamingEnumeration<String> {

    Enumeration<Attribute> elements;

    public IDEnumImpl() {
        // Walking through the elements, rather than the keys, gives
        // us attribute IDs that have not been converted to lowercase.
        this.elements = attrs.elements();
    }

    public boolean hasMoreElements() {
        return elements.hasMoreElements();
    }

    public String nextElement() {
        Attribute attr = elements.nextElement();
        return attr.getID();
    }

    public boolean hasMore() throws NamingException {
        return hasMoreElements();
    }

    public String next() throws NamingException {
        return nextElement();
    }

    public void close() throws NamingException {
        elements = null;
    }
}

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability.
     */
    @java.io.Serial
    private static final long serialVersionUID = 4980164073184639448L;
}
