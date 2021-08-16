/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.metadata;

import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;

/**
 * An object describing the structure of metadata documents returned
 * from {@code IIOMetadata.getAsTree} and passed to
 * {@code IIOMetadata.setFromTree} and {@code mergeTree}.
 * Document structures are described by a set of constraints on the
 * type and number of child elements that may belong to a given parent
 * element type, the names, types, and values of attributes that may
 * belong to an element, and the type and values of
 * {@code Object} reference that may be stored at a node.
 *
 * <p> N.B: classes that implement this interface should contain a
 * method declared as {@code public static getInstance()} which
 * returns an instance of the class.  Commonly, an implementation will
 * construct only a single instance and cache it for future
 * invocations of {@code getInstance}.
 * <p> In the event that the plugin is provided as part of a named module,
 * that module must export the package containing the implementation class
 * to the <pre>java.desktop</pre> module via a qualified export.
 * An unqualified export is not recommended unless also needed for
 * some other reason. Failing to export the package will result in
 * access failure at runtime.
 *
 * <p> The structures that may be described by this class are a subset
 * of those expressible using XML document type definitions (DTDs),
 * with the addition of some basic information on the datatypes of
 * attributes and the ability to store an {@code Object}
 * reference within a node.  In the future, XML Schemas could be used
 * to represent these structures, and many others.
 *
 * <p> The differences between
 * {@code IIOMetadataFormat}-described structures and DTDs are as
 * follows:
 *
 * <ul>
 * <li> Elements may not contain text or mix text with embedded
 * tags.
 *
 * <li> The children of an element must conform to one of a few simple
 * patterns, described in the documentation for the
 * {@code CHILD_*} constants;
 *
 * <li> The in-memory representation of an elements may contain a
 * reference to an {@code Object}.  There is no provision for
 * representing such objects textually.
 * </ul>
 *
 */
public interface IIOMetadataFormat {

    // Child policies

    /**
     * A constant returned by {@code getChildPolicy} to indicate
     * that an element may not have any children.  In other words, it
     * is required to be a leaf node.
     */
    int CHILD_POLICY_EMPTY = 0;

    /**
     * A constant returned by {@code getChildPolicy} to indicate
     * that an element must have a single instance of each of its
     * legal child elements, in order.  In DTD terms, the contents of
     * the element are defined by a sequence {@code a,b,c,d,...}.
     */
    int CHILD_POLICY_ALL = 1;

    /**
     * A constant returned by {@code getChildPolicy} to indicate
     * that an element must have zero or one instance of each of its
     * legal child elements, in order.  In DTD terms, the contents of
     * the element are defined by a sequence
     * {@code a?,b?,c?,d?,...}.
     */
    int CHILD_POLICY_SOME = 2;

    /**
     * A constant returned by {@code getChildPolicy} to indicate
     * that an element must have zero or one children, selected from
     * among its legal child elements.  In DTD terms, the contents of
     * the element are defined by a selection
     * {@code a|b|c|d|...}.
     */
    int CHILD_POLICY_CHOICE = 3;

    /**
     * A constant returned by {@code getChildPolicy} to indicate
     * that an element must have a sequence of instances of any of its
     * legal child elements.  In DTD terms, the contents of the
     * element are defined by a sequence {@code (a|b|c|d|...)*}.
     */
    int CHILD_POLICY_SEQUENCE = 4;

    /**
     * A constant returned by {@code getChildPolicy} to indicate
     * that an element must have zero or more instances of its unique
     * legal child element.  In DTD terms, the contents of the element
     * are defined by a starred expression {@code a*}.
     */
    int CHILD_POLICY_REPEAT = 5;

    /**
     * The largest valid {@code CHILD_POLICY_*} constant,
     * to be used for range checks.
     */
    int CHILD_POLICY_MAX = CHILD_POLICY_REPEAT;

    /**
     * A constant returned by {@code getObjectValueType} to
     * indicate the absence of a user object.
     */
    int VALUE_NONE = 0;

    /**
     * A constant returned by {@code getAttributeValueType} and
     * {@code getObjectValueType} to indicate that the attribute
     * or user object may be set a single, arbitrary value.
     */
    int VALUE_ARBITRARY = 1;

    /**
     * A constant returned by {@code getAttributeValueType} and
     * {@code getObjectValueType} to indicate that the attribute
     * or user object may be set a range of values.  Both the minimum
     * and maximum values of the range are exclusive.  It is
     * recommended that ranges of integers be inclusive on both ends,
     * and that exclusive ranges be used only for floating-point data.
     *
     * @see #VALUE_RANGE_MIN_MAX_INCLUSIVE
     */
    int VALUE_RANGE = 2;

    /**
     * A value that may be or'ed with {@code VALUE_RANGE} to
     * obtain {@code VALUE_RANGE_MIN_INCLUSIVE}, and with
     * {@code VALUE_RANGE_MAX_INCLUSIVE} to obtain
     * {@code VALUE_RANGE_MIN_MAX_INCLUSIVE}.
     *
     * <p> Similarly, the value may be and'ed with the value of
     * {@code getAttributeValueType} or
     * {@code getObjectValueType} to determine if the minimum
     * value of the range is inclusive.
     */
    int VALUE_RANGE_MIN_INCLUSIVE_MASK = 4;

    /**
     * A value that may be or'ed with {@code VALUE_RANGE} to
     * obtain {@code VALUE_RANGE_MAX_INCLUSIVE}, and with
     * {@code VALUE_RANGE_MIN_INCLUSIVE} to obtain
     * {@code VALUE_RANGE_MIN_MAX_INCLUSIVE}.
     *
     * <p> Similarly, the value may be and'ed with the value of
     * {@code getAttributeValueType} or
     * {@code getObjectValueType} to determine if the maximum
     * value of the range is inclusive.
     */
    int VALUE_RANGE_MAX_INCLUSIVE_MASK = 8;

    /**
     * A constant returned by {@code getAttributeValueType} and
     * {@code getObjectValueType} to indicate that the attribute
     * or user object may be set to a range of values.  The minimum
     * (but not the maximum) value of the range is inclusive.
     */
    int VALUE_RANGE_MIN_INCLUSIVE = VALUE_RANGE |
        VALUE_RANGE_MIN_INCLUSIVE_MASK;

    /**
     * A constant returned by {@code getAttributeValueType} and
     * {@code getObjectValueType} to indicate that the attribute
     * or user object may be set to a range of values.  The maximum
     * (but not the minimum) value of the range is inclusive.
     */
    int VALUE_RANGE_MAX_INCLUSIVE = VALUE_RANGE |
        VALUE_RANGE_MAX_INCLUSIVE_MASK;

    /**
     * A constant returned by {@code getAttributeValueType} and
     * {@code getObjectValueType} to indicate that the attribute
     * or user object may be set a range of values.  Both the minimum
     * and maximum values of the range are inclusive.  It is
     * recommended that ranges of integers be inclusive on both ends,
     * and that exclusive ranges be used only for floating-point data.
     */
    int VALUE_RANGE_MIN_MAX_INCLUSIVE =
        VALUE_RANGE |
        VALUE_RANGE_MIN_INCLUSIVE_MASK |
        VALUE_RANGE_MAX_INCLUSIVE_MASK;

    /**
     * A constant returned by {@code getAttributeValueType} and
     * {@code getObjectValueType} to indicate that the attribute
     * or user object may be set one of a number of enumerated values.
     * In the case of attributes, these values are
     * {@code String}s; for objects, they are
     * {@code Object}s implementing a given class or interface.
     *
     * <p> Attribute values of type {@code DATATYPE_BOOLEAN}
     * should be marked as enumerations.
     */
    int VALUE_ENUMERATION = 16;

    /**
     * A constant returned by {@code getAttributeValueType} and
     * {@code getObjectValueType} to indicate that the attribute
     * or user object may be set to a list or array of values.  In the
     * case of attributes, the list will consist of
     * whitespace-separated values within a {@code String}; for
     * objects, an array will be used.
     */
    int VALUE_LIST = 32;

    /**
     * A constant returned by {@code getAttributeDataType}
     * indicating that the value of an attribute is a general Unicode
     * string.
     */
    int DATATYPE_STRING = 0;

    /**
     * A constant returned by {@code getAttributeDataType}
     * indicating that the value of an attribute is one of the boolean
     * values 'true' or 'false'.
     * Attribute values of type DATATYPE_BOOLEAN should be marked as
     * enumerations, and the permitted values should be the string
     * literal values "TRUE" or "FALSE", although a plugin may also
     * recognise lower or mixed case equivalents.
     */
    int DATATYPE_BOOLEAN = 1;

    /**
     * A constant returned by {@code getAttributeDataType}
     * indicating that the value of an attribute is a string
     * representation of an integer.
     */
    int DATATYPE_INTEGER = 2;

    /**
     * A constant returned by {@code getAttributeDataType}
     * indicating that the value of an attribute is a string
     * representation of a decimal floating-point number.
     */
    int DATATYPE_FLOAT = 3;

    /**
     * A constant returned by {@code getAttributeDataType}
     * indicating that the value of an attribute is a string
     * representation of a double-precision decimal floating-point
     * number.
     */
    int DATATYPE_DOUBLE = 4;

    // Root

    /**
     * Returns the name of the root element of the format.
     *
     * @return a {@code String}.
     */
    String getRootName();

    // Multiplicity

    /**
     * Returns {@code true} if the element (and the subtree below
     * it) is allowed to appear in a metadata document for an image of
     * the given type, defined by an {@code ImageTypeSpecifier}.
     * For example, a metadata document format might contain an
     * element that describes the primary colors of the image, which
     * would not be allowed when writing a grayscale image.
     *
     * @param elementName the name of the element being queried.
     * @param imageType an {@code ImageTypeSpecifier} indicating
     * the type of the image that will be associated with the
     * metadata.
     *
     * @return {@code true} if the node is meaningful for images
     * of the given type.
     */
    boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType);

    /**
     * Returns the minimum number of children of the named element
     * with child policy {@code CHILD_POLICY_REPEAT}.  For
     * example, an element representing color primary information
     * might be required to have at least 3 children, one for each
     * primary.
     *
     * @param elementName the name of the element being queried.
     *
     * @return an {@code int}.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element does
     * not have a child policy of {@code CHILD_POLICY_REPEAT}.
     */
    int getElementMinChildren(String elementName);

    /**
     * Returns the maximum number of children of the named element
     * with child policy {@code CHILD_POLICY_REPEAT}.  For
     * example, an element representing an entry in an 8-bit color
     * palette might be allowed to repeat up to 256 times.  A value of
     * {@code Integer.MAX_VALUE} may be used to specify that
     * there is no upper bound.
     *
     * @param elementName the name of the element being queried.
     *
     * @return an {@code int}.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element does
     * not have a child policy of {@code CHILD_POLICY_REPEAT}.
     */
    int getElementMaxChildren(String elementName);

    /**
     * Returns a {@code String} containing a description of the
     * named element, or {@code null}.  The description will be
     * localized for the supplied {@code Locale} if possible.
     *
     * <p> If {@code locale} is {@code null}, the current
     * default {@code Locale} returned by {@code Locale.getLocale}
     * will be used.
     *
     * @param elementName the name of the element.
     * @param locale the {@code Locale} for which localization
     * will be attempted.
     *
     * @return the element description.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null}, or is not a legal element name for this format.
     */
    String getElementDescription(String elementName, Locale locale);

    // Children

    /**
     * Returns one of the constants starting with
     * {@code CHILD_POLICY_}, indicating the legal pattern of
     * children for the named element.
     *
     * @param elementName the name of the element being queried.
     *
     * @return one of the {@code CHILD_POLICY_*} constants.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     */
    int getChildPolicy(String elementName);

    /**
     * Returns an array of {@code String}s indicating the names
     * of the element which are allowed to be children of the named
     * element, in the order in which they should appear.  If the
     * element cannot have children, {@code null} is returned.
     *
     * @param elementName the name of the element being queried.
     *
     * @return an array of {@code String}s, or null.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     */
    String[] getChildNames(String elementName);

    // Attributes

    /**
     * Returns an array of {@code String}s listing the names of
     * the attributes that may be associated with the named element.
     *
     * @param elementName the name of the element being queried.
     *
     * @return an array of {@code String}s.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     */
    String[] getAttributeNames(String elementName);

    /**
     * Returns one of the constants starting with {@code VALUE_},
     * indicating whether the values of the given attribute within the
     * named element are arbitrary, constrained to lie within a
     * specified range, constrained to be one of a set of enumerated
     * values, or are a whitespace-separated list of arbitrary values.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return one of the {@code VALUE_*} constants.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     */
    int getAttributeValueType(String elementName, String attrName);

    /**
     * Returns one of the constants starting with
     * {@code DATATYPE_}, indicating the format and
     * interpretation of the value of the given attribute within the
     * named element.  If {@code getAttributeValueType} returns
     * {@code VALUE_LIST}, then the legal value is a
     * whitespace-spearated list of values of the returned datatype.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return one of the {@code DATATYPE_*} constants.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     */
    int getAttributeDataType(String elementName, String attrName);

    /**
     * Returns {@code true} if the named attribute must be
     * present within the named element.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return {@code true} if the attribute must be present.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     */
    boolean isAttributeRequired(String elementName, String attrName);

    /**
     * Returns the default value of the named attribute, if it is not
     * explicitly present within the named element, as a
     * {@code String}, or {@code null} if no default value
     * is available.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return a {@code String} containing the default value, or
     * {@code null}.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     */
    String getAttributeDefaultValue(String elementName, String attrName);

    /**
     * Returns an array of {@code String}s containing the legal
     * enumerated values for the given attribute within the named
     * element.  This method should only be called if
     * {@code getAttributeValueType} returns
     * {@code VALUE_ENUMERATION}.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return an array of {@code String}s.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     * @exception IllegalArgumentException if the given attribute is
     * not defined as an enumeration.
     */
    String[] getAttributeEnumerations(String elementName, String attrName);

    /**
     * Returns the minimum legal value for the attribute.  Whether
     * this value is inclusive or exclusive may be determined by the
     * value of {@code getAttributeValueType}.  The value is
     * returned as a {@code String}; its interpretation is
     * dependent on the value of {@code getAttributeDataType}.
     * This method should only be called if
     * {@code getAttributeValueType} returns
     * {@code VALUE_RANGE_*}.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return a {@code String} containing the smallest legal
     * value for the attribute.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     * @exception IllegalArgumentException if the given attribute is
     * not defined as a range.
     */
    String getAttributeMinValue(String elementName, String attrName);

    /**
     * Returns the maximum legal value for the attribute.  Whether
     * this value is inclusive or exclusive may be determined by the
     * value of {@code getAttributeValueType}.  The value is
     * returned as a {@code String}; its interpretation is
     * dependent on the value of {@code getAttributeDataType}.
     * This method should only be called if
     * {@code getAttributeValueType} returns
     * {@code VALUE_RANGE_*}.
     *
     * @param elementName the name of the element being queried, as a
     * {@code String}.
     * @param attrName the name of the attribute being queried.
     *
     * @return a {@code String} containing the largest legal
     * value for the attribute.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     * @exception IllegalArgumentException if the given attribute is
     * not defined as a range.
     */
    String getAttributeMaxValue(String elementName, String attrName);

    /**
     * Returns the minimum number of list items that may be used to
     * define this attribute.  The attribute itself is defined as a
     * {@code String} containing multiple whitespace-separated
     * items.  This method should only be called if
     * {@code getAttributeValueType} returns
     * {@code VALUE_LIST}.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return the smallest legal number of list items for the
     * attribute.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     * @exception IllegalArgumentException if the given attribute is
     * not defined as a list.
     */
    int getAttributeListMinLength(String elementName, String attrName);

    /**
     * Returns the maximum number of list items that may be used to
     * define this attribute.  A value of
     * {@code Integer.MAX_VALUE} may be used to specify that
     * there is no upper bound.  The attribute itself is defined as a
     * {@code String} containing multiple whitespace-separated
     * items.  This method should only be called if
     * {@code getAttributeValueType} returns
     * {@code VALUE_LIST}.
     *
     * @param elementName the name of the element being queried.
     * @param attrName the name of the attribute being queried.
     *
     * @return the largest legal number of list items for the
     * attribute.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     * @exception IllegalArgumentException if the given attribute is
     * not defined as a list.
     */
    int getAttributeListMaxLength(String elementName, String attrName);

    /**
     * Returns a {@code String} containing a description of the
     * named attribute, or {@code null}.  The description will be
     * localized for the supplied {@code Locale} if possible.
     *
     * <p> If {@code locale} is {@code null}, the current
     * default {@code Locale} returned by {@code Locale.getLocale}
     * will be used.
     *
     * @param elementName the name of the element.
     * @param attrName the name of the attribute.
     * @param locale the {@code Locale} for which localization
     * will be attempted.
     *
     * @return the attribute description.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null}, or is not a legal element name for this format.
     * @exception IllegalArgumentException if {@code attrName} is
     * {@code null} or is not a legal attribute name for this
     * element.
     */
    String getAttributeDescription(String elementName, String attrName,
                                   Locale locale);

    // Object value

    /**
     * Returns one of the enumerated values starting with
     * {@code VALUE_}, indicating the type of values
     * (enumeration, range, or array) that are allowed for the
     * {@code Object} reference.  If no object value can be
     * stored within the given element, the result of this method will
     * be {@code VALUE_NONE}.
     *
     * <p> {@code Object} references whose legal values are
     * defined as a range must implement the {@code Comparable}
     * interface.
     *
     * @param elementName the name of the element being queried.
     *
     * @return one of the {@code VALUE_*} constants.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     *
     * @see Comparable
     */
    int getObjectValueType(String elementName);

    /**
     * Returns the {@code Class} type of the {@code Object}
     * reference stored within the element.  If this element may not
     * contain an {@code Object} reference, an
     * {@code IllegalArgumentException} will be thrown.  If the
     * class type is an array, this field indicates the underlying
     * class type (<i>e.g</i>, for an array of {@code int}s, this
     * method would return {@code int.class}).
     *
     * <p> {@code Object} references whose legal values are
     * defined as a range must implement the {@code Comparable}
     * interface.
     *
     * @param elementName the name of the element being queried.
     *
     * @return a {@code Class} object.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element cannot
     * contain an object value (<i>i.e.</i>, if
     * {@code getObjectValueType(elementName) == VALUE_NONE}).
     */
    Class<?> getObjectClass(String elementName);

    /**
     * Returns an {@code Object}s containing the default
     * value for the {@code Object} reference within
     * the named element.
     *
     * @param elementName the name of the element being queried.
     *
     * @return an {@code Object}.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element cannot
     * contain an object value (<i>i.e.</i>, if
     * {@code getObjectValueType(elementName) == VALUE_NONE}).
     */
    Object getObjectDefaultValue(String elementName);

    /**
     * Returns an array of {@code Object}s containing the legal
     * enumerated values for the {@code Object} reference within
     * the named element.  This method should only be called if
     * {@code getObjectValueType} returns
     * {@code VALUE_ENUMERATION}.
     *
     * <p> The {@code Object} associated with a node that accepts
     * enumerated values must be equal to one of the values returned by
     * this method, as defined by the {@code ==} operator (as
     * opposed to the {@code Object.equals} method).
     *
     * @param elementName the name of the element being queried.
     *
     * @return an array of {@code Object}s.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element cannot
     * contain an object value (<i>i.e.</i>, if
     * {@code getObjectValueType(elementName) == VALUE_NONE}).
     * @exception IllegalArgumentException if the {@code Object}
     * is not defined as an enumeration.
     */
    Object[] getObjectEnumerations(String elementName);

    /**
     * Returns the minimum legal value for the {@code Object}
     * reference within the named element.  Whether this value is
     * inclusive or exclusive may be determined by the value of
     * {@code getObjectValueType}.  This method should only be
     * called if {@code getObjectValueType} returns one of the
     * constants starting with {@code VALUE_RANGE}.
     *
     * @param elementName the name of the element being queried.
     *
     * @return the smallest legal value for the attribute.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element cannot
     * contain an object value (<i>i.e.</i>, if
     * {@code getObjectValueType(elementName) == VALUE_NONE}).
     * @exception IllegalArgumentException if the {@code Object}
     * is not defined as a range.
     */
    Comparable<?> getObjectMinValue(String elementName);

    /**
     * Returns the maximum legal value for the {@code Object}
     * reference within the named element.  Whether this value is
     * inclusive or exclusive may be determined by the value of
     * {@code getObjectValueType}.  This method should only be
     * called if {@code getObjectValueType} returns one of the
     * constants starting with {@code VALUE_RANGE}.
     *
     * @return the smallest legal value for the attribute.
     *
     * @param elementName the name of the element being queried.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element cannot
     * contain an object value (<i>i.e.</i>, if
     * {@code getObjectValueType(elementName) == VALUE_NONE}).
     * @exception IllegalArgumentException if the {@code Object}
     * is not defined as a range.
     */
    Comparable<?> getObjectMaxValue(String elementName);

    /**
     * Returns the minimum number of array elements that may be used
     * to define the {@code Object} reference within the named
     * element.  This method should only be called if
     * {@code getObjectValueType} returns
     * {@code VALUE_LIST}.
     *
     * @param elementName the name of the element being queried.
     *
     * @return the smallest valid array length for the
     * {@code Object} reference.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element cannot
     * contain an object value (<i>i.e.</i>, if
     * {@code getObjectValueType(elementName) == VALUE_NONE}).
     * @exception IllegalArgumentException if the {@code Object} is not
     * an array.
     */
    int getObjectArrayMinLength(String elementName);

    /**
     * Returns the maximum number of array elements that may be used
     * to define the {@code Object} reference within the named
     * element.  A value of {@code Integer.MAX_VALUE} may be used
     * to specify that there is no upper bound.  This method should
     * only be called if {@code getObjectValueType} returns
     * {@code VALUE_LIST}.
     *
     * @param elementName the name of the element being queried.
     *
     * @return the largest valid array length for the
     * {@code Object} reference.
     *
     * @exception IllegalArgumentException if {@code elementName}
     * is {@code null} or is not a legal element name for this
     * format.
     * @exception IllegalArgumentException if the named element cannot
     * contain an object value (<i>i.e.</i>, if
     * {@code getObjectValueType(elementName) == VALUE_NONE}).
     * @exception IllegalArgumentException if the {@code Object} is not
     * an array.
     */
    int getObjectArrayMaxLength(String elementName);
}
