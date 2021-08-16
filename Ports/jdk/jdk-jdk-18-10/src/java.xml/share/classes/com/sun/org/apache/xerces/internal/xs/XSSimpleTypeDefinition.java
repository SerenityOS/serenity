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

package com.sun.org.apache.xerces.internal.xs;

/**
 * This interface represents the Simple Type Definition schema component. This
 * interface provides several query operations for facet components. Users
 * can either retrieve the defined facets as XML Schema components, using
 * the <code>facets</code> and the <code>multiValueFacets</code> attributes;
 * or users can separately query a facet's properties using methods such as
 * <code>getLexicalFacetValue</code>, <code>isFixedFacet</code>, etc.
 */
public interface XSSimpleTypeDefinition extends XSTypeDefinition {
    // Variety definitions
    /**
     * The variety is absent for the anySimpleType definition.
     */
    public static final short VARIETY_ABSENT            = 0;
    /**
     * <code>Atomic</code> type.
     */
    public static final short VARIETY_ATOMIC            = 1;
    /**
     * <code>List</code> type.
     */
    public static final short VARIETY_LIST              = 2;
    /**
     * <code>Union</code> type.
     */
    public static final short VARIETY_UNION             = 3;

    // Facets
    /**
     * No facets defined.
     */
    public static final short FACET_NONE                = 0;
    /**
     * 4.3.1 Length
     */
    public static final short FACET_LENGTH              = 1;
    /**
     * 4.3.2 minLength.
     */
    public static final short FACET_MINLENGTH           = 2;
    /**
     * 4.3.3 maxLength.
     */
    public static final short FACET_MAXLENGTH           = 4;
    /**
     * 4.3.4 pattern.
     */
    public static final short FACET_PATTERN             = 8;
    /**
     * 4.3.5 whitespace.
     */
    public static final short FACET_WHITESPACE          = 16;
    /**
     * 4.3.7 maxInclusive.
     */
    public static final short FACET_MAXINCLUSIVE        = 32;
    /**
     * 4.3.9 maxExclusive.
     */
    public static final short FACET_MAXEXCLUSIVE        = 64;
    /**
     * 4.3.9 minExclusive.
     */
    public static final short FACET_MINEXCLUSIVE        = 128;
    /**
     * 4.3.10 minInclusive.
     */
    public static final short FACET_MININCLUSIVE        = 256;
    /**
     * 4.3.11 totalDigits .
     */
    public static final short FACET_TOTALDIGITS         = 512;
    /**
     * 4.3.12 fractionDigits.
     */
    public static final short FACET_FRACTIONDIGITS      = 1024;
    /**
     * 4.3.5 enumeration.
     */
    public static final short FACET_ENUMERATION         = 2048;

    /**
     * A constant defined for the 'ordered' fundamental facet: not ordered.
     */
    public static final short ORDERED_FALSE             = 0;
    /**
     * A constant defined for the 'ordered' fundamental facet: partially
     * ordered.
     */
    public static final short ORDERED_PARTIAL           = 1;
    /**
     * A constant defined for the 'ordered' fundamental facet: total ordered.
     */
    public static final short ORDERED_TOTAL             = 2;
    /**
     * [variety]: one of {atomic, list, union} or absent.
     */
    public short getVariety();

    /**
     * If variety is <code>atomic</code> the primitive type definition (a
     * built-in primitive datatype definition or the simple ur-type
     * definition) is available, otherwise <code>null</code>.
     */
    public XSSimpleTypeDefinition getPrimitiveType();

    /**
     * Returns the closest built-in type category this type represents or
     * derived from. For example, if this simple type is a built-in derived
     * type integer the <code>INTEGER_DV</code> is returned.
     */
    public short getBuiltInKind();

    /**
     * If variety is <code>list</code> the item type definition (an atomic or
     * union simple type definition) is available, otherwise
     * <code>null</code>.
     */
    public XSSimpleTypeDefinition getItemType();

    /**
     * If variety is <code>union</code> the list of member type definitions (a
     * non-empty sequence of simple type definitions) is available,
     * otherwise an empty <code>XSObjectList</code>.
     */
    public XSObjectList getMemberTypes();

    /**
     * [facets]: all facets defined on this type. The value is a bit
     * combination of FACET_XXX constants of all defined facets.
     */
    public short getDefinedFacets();

    /**
     * Convenience method. [Facets]: check whether a facet is defined on this
     * type.
     * @param facetName  The name of the facet.
     * @return  True if the facet is defined, false otherwise.
     */
    public boolean isDefinedFacet(short facetName);

    /**
     * [facets]: all defined facets for this type which are fixed.
     */
    public short getFixedFacets();

    /**
     * Convenience method. [Facets]: check whether a facet is defined and
     * fixed on this type.
     * @param facetName  The name of the facet.
     * @return  True if the facet is fixed, false otherwise.
     */
    public boolean isFixedFacet(short facetName);

    /**
     * Convenience method. Returns a value of a single constraining facet for
     * this simple type definition. This method must not be used to retrieve
     * values for <code>enumeration</code> and <code>pattern</code> facets.
     * @param facetName The name of the facet, i.e.
     *   <code>FACET_LENGTH, FACET_TOTALDIGITS</code>.
     *   To retrieve the value for a pattern or
     *   an enumeration, see <code>enumeration</code> and
     *   <code>pattern</code>.
     * @return A value of the facet specified in <code>facetName</code> for
     *   this simple type definition or <code>null</code>.
     */
    public String getLexicalFacetValue(short facetName);

    /**
     * A list of enumeration values if it exists, otherwise an empty
     * <code>StringList</code>.
     */
    public StringList getLexicalEnumeration();

    /**
     * A list of pattern values if it exists, otherwise an empty
     * <code>StringList</code>.
     */
    public StringList getLexicalPattern();

    /**
     *  Fundamental Facet: ordered.
     */
    public short getOrdered();

    /**
     * Fundamental Facet: cardinality.
     */
    public boolean getFinite();

    /**
     * Fundamental Facet: bounded.
     */
    public boolean getBounded();

    /**
     * Fundamental Facet: numeric.
     */
    public boolean getNumeric();

    /**
     *  A list of constraining facets if it exists, otherwise an empty
     * <code>XSObjectList</code>. Note: This method must not be used to
     * retrieve values for <code>enumeration</code> and <code>pattern</code>
     * facets.
     */
    public XSObjectList getFacets();

    /**
     *  A list of enumeration and pattern constraining facets if it exists,
     * otherwise an empty <code>XSObjectList</code>.
     */
    public XSObjectList getMultiValueFacets();

    /**
     * A constraining facet object. An instance of XSFacet or XSMultiValueFacet.
     */
    public XSObject getFacet(int facetType);

    /**
     * A sequence of [annotations] or an empty <code>XSObjectList</code>.
     */
    public XSObjectList getAnnotations();

}
