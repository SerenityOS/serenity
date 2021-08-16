/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.util.Enumeration;

/**
 * A collection of unique attributes.  This is a read-only,
 * immutable interface.  An attribute is basically a key and
 * a value assigned to the key.  The collection may represent
 * something like a style run, a logical style, etc.  These
 * are generally used to describe features that will contribute
 * to some graphical representation such as a font.  The
 * set of possible keys is unbounded and can be anything.
 * Typically View implementations will respond to attribute
 * definitions and render something to represent the attributes.
 * <p>
 * Attributes can potentially resolve in a hierarchy.  If a
 * key doesn't resolve locally, and a resolving parent
 * exists, the key will be resolved through the parent.
 *
 * @author  Timothy Prinzing
 * @see MutableAttributeSet
 */
public interface AttributeSet {

    /**
     * This interface is the type signature that is expected
     * to be present on any attribute key that contributes to
     * the determination of what font to use to render some
     * text.  This is not considered to be a closed set, the
     * definition can change across version of the platform and can
     * be amended by additional user added entries that
     * correspond to logical settings that are specific to
     * some type of content.
     */
    public interface FontAttribute {
    }

    /**
     * This interface is the type signature that is expected
     * to be present on any attribute key that contributes to
     * presentation of color.
     */
    public interface ColorAttribute {
    }

    /**
     * This interface is the type signature that is expected
     * to be present on any attribute key that contributes to
     * character level presentation.  This would be any attribute
     * that applies to a so-called <i>run</i> of
     * style.
     */
    public interface CharacterAttribute {
    }

    /**
     * This interface is the type signature that is expected
     * to be present on any attribute key that contributes to
     * the paragraph level presentation.
     */
    public interface ParagraphAttribute {
    }

    /**
     * Returns the number of attributes that are defined locally in this set.
     * Attributes that are defined in the parent set are not included.
     *
     * @return the number of attributes &gt;= 0
     */
    public int getAttributeCount();

    /**
     * Checks whether the named attribute has a value specified in
     * the set without resolving through another attribute
     * set.
     *
     * @param attrName the attribute name
     * @return true if the attribute has a value specified
     */
    public boolean isDefined(Object attrName);

    /**
     * Determines if the two attribute sets are equivalent.
     *
     * @param attr an attribute set
     * @return true if the sets are equivalent
     */
    public boolean isEqual(AttributeSet attr);

    /**
     * Returns an attribute set that is guaranteed not
     * to change over time.
     *
     * @return a copy of the attribute set
     */
    public AttributeSet copyAttributes();

    /**
     * Fetches the value of the given attribute. If the value is not found
     * locally, the search is continued upward through the resolving
     * parent (if one exists) until the value is either
     * found or there are no more parents.  If the value is not found,
     * null is returned.
     *
     * @param key the non-null key of the attribute binding
     * @return the value of the attribute, or {@code null} if not found
     */
    public Object getAttribute(Object key);

    /**
     * Returns an enumeration over the names of the attributes that are
     * defined locally in the set. Names of attributes defined in the
     * resolving parent, if any, are not included. The values of the
     * <code>Enumeration</code> may be anything and are not constrained to
     * a particular <code>Object</code> type.
     * <p>
     * This method never returns {@code null}. For a set with no attributes, it
     * returns an empty {@code Enumeration}.
     *
     * @return the names
     */
    public Enumeration<?> getAttributeNames();

    /**
     * Returns {@code true} if this set defines an attribute with the same
     * name and an equal value. If such an attribute is not found locally,
     * it is searched through in the resolving parent hierarchy.
     *
     * @param name the non-null attribute name
     * @param value the value
     * @return {@code true} if the set defines the attribute with an
     *     equal value, either locally or through its resolving parent
     * @throws NullPointerException if either {@code name} or
     *      {@code value} is {@code null}
     */
    public boolean containsAttribute(Object name, Object value);

    /**
     * Returns {@code true} if this set defines all the attributes from the
     * given set with equal values. If an attribute is not found locally,
     * it is searched through in the resolving parent hierarchy.
     *
     * @param attributes the set of attributes to check against
     * @return {@code true} if this set defines all the attributes with equal
     *              values, either locally or through its resolving parent
     * @throws NullPointerException if {@code attributes} is {@code null}
     */
    public boolean containsAttributes(AttributeSet attributes);

    /**
     * Gets the resolving parent.
     *
     * @return the parent
     */
    public AttributeSet getResolveParent();

    /**
     * Attribute name used to name the collection of
     * attributes.
     */
    public static final Object NameAttribute = StyleConstants.NameAttribute;

    /**
     * Attribute name used to identify the resolving parent
     * set of attributes, if one is defined.
     */
    public static final Object ResolveAttribute = StyleConstants.ResolveAttribute;

}
