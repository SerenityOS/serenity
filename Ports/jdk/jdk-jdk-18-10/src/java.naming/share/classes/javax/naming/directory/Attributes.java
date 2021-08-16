/*
 * Copyright (c) 1999, 2004, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

/**
  * This interface represents a collection of attributes.
  *<p>
  * In a directory, named objects can have associated with them
  * attributes.  The Attributes interface represents a collection of attributes.
  * For example, you can request from the directory the attributes
  * associated with an object.  Those attributes are returned in
  * an object that implements the Attributes interface.
  *<p>
  * Attributes in an object that implements the  Attributes interface are
  * unordered. The object can have zero or more attributes.
  * Attributes is either case-sensitive or case-insensitive (case-ignore).
  * This property is determined at the time the Attributes object is
  * created. (see BasicAttributes constructor for example).
  * In a case-insensitive Attributes, the case of its attribute identifiers
  * is ignored when searching for an attribute, or adding attributes.
  * In a case-sensitive Attributes, the case is significant.
  *<p>
  * Note that updates to Attributes (such as adding or removing an attribute)
  * do not affect the corresponding representation in the directory.
  * Updates to the directory can only be effected
  * using operations in the DirContext interface.
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
  * @see BasicAttributes
  * @since 1.3
  */

public interface Attributes extends Cloneable, java.io.Serializable {
    /**
      * Determines whether the attribute set ignores the case of
      * attribute identifiers when retrieving or adding attributes.
      * @return true if case is ignored; false otherwise.
      */
    boolean isCaseIgnored();

    /**
      * Retrieves the number of attributes in the attribute set.
      *
      * @return The nonnegative number of attributes in this attribute set.
      */
    int size();

    /**
      * Retrieves the attribute with the given attribute id from the
      * attribute set.
      *
      * @param attrID The non-null id of the attribute to retrieve.
      *           If this attribute set ignores the character
      *           case of its attribute ids, the case of attrID
      *           is ignored.
      * @return The attribute identified by attrID; null if not found.
      * @see #put
      * @see #remove
      */
    Attribute get(String attrID);

    /**
      * Retrieves an enumeration of the attributes in the attribute set.
      * The effects of updates to this attribute set on this enumeration
      * are undefined.
      *
      * @return A non-null enumeration of the attributes in this attribute set.
      *         Each element of the enumeration is of class {@code Attribute}.
      *         If attribute set has zero attributes, an empty enumeration
      *         is returned.
      */
    NamingEnumeration<? extends Attribute> getAll();

    /**
      * Retrieves an enumeration of the ids of the attributes in the
      * attribute set.
      * The effects of updates to this attribute set on this enumeration
      * are undefined.
      *
      * @return A non-null enumeration of the attributes' ids in
      *         this attribute set. Each element of the enumeration is
      *         of class String.
      *         If attribute set has zero attributes, an empty enumeration
      *         is returned.
      */
    NamingEnumeration<String> getIDs();

    /**
      * Adds a new attribute to the attribute set.
      *
      * @param attrID   non-null The id of the attribute to add.
      *           If the attribute set ignores the character
      *           case of its attribute ids, the case of attrID
      *           is ignored.
      * @param val      The possibly null value of the attribute to add.
      *                 If null, the attribute does not have any values.
      * @return The Attribute with attrID that was previous in this attribute set;
      *         null if no such attribute existed.
      * @see #remove
      */
    Attribute put(String attrID, Object val);

    /**
      * Adds a new attribute to the attribute set.
      *
      * @param attr     The non-null attribute to add.
      *                 If the attribute set ignores the character
      *                 case of its attribute ids, the case of
      *                 attr's identifier is ignored.
      * @return The Attribute with the same ID as attr that was previous
      *         in this attribute set;
      *         null if no such attribute existed.
      * @see #remove
      */
    Attribute put(Attribute attr);

    /**
      * Removes the attribute with the attribute id 'attrID' from
      * the attribute set. If the attribute does not exist, ignore.
      *
      * @param attrID   The non-null id of the attribute to remove.
      *                 If the attribute set ignores the character
      *                 case of its attribute ids, the case of
      *                 attrID is ignored.
      * @return The Attribute with the same ID as attrID that was previous
      *         in the attribute set;
      *         null if no such attribute existed.
      */
    Attribute remove(String attrID);

    /**
      * Makes a copy of the attribute set.
      * The new set contains the same attributes as the original set:
      * the attributes are not themselves cloned.
      * Changes to the copy will not affect the original and vice versa.
      *
      * @return A non-null copy of this attribute set.
      */
    Object clone();

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    // static final long serialVersionUID = -7247874645443605347L;
}
