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
 * This interface represents the Complex Type Definition schema component.
 */
public interface XSComplexTypeDefinition extends XSTypeDefinition {
    // Content Model Types
    /**
     * Represents an empty content type. A content type with the distinguished
     * value empty validates elements with no character or element
     * information item children.
     */
    public static final short CONTENTTYPE_EMPTY         = 0;
    /**
     * Represents a simple content type. A content type which is simple
     * validates elements with character-only children.
     */
    public static final short CONTENTTYPE_SIMPLE        = 1;
    /**
     * Represents an element-only content type. An element-only content type
     * validates elements with children that conform to the supplied content
     * model.
     */
    public static final short CONTENTTYPE_ELEMENT       = 2;
    /**
     * Represents a mixed content type.
     */
    public static final short CONTENTTYPE_MIXED         = 3;

    /**
     * [derivation method]: either <code>DERIVATION_EXTENSION</code>,
     * <code>DERIVATION_RESTRICTION</code>, or <code>DERIVATION_NONE</code>
     * (see <code>XSConstants</code>).
     */
    public short getDerivationMethod();

    /**
     * [abstract]: a boolean. Complex types for which <code>abstract</code> is
     * true must not be used as the type definition for the validation of
     * element information items.
     */
    public boolean getAbstract();

    /**
     *  A set of attribute uses if it exists, otherwise an empty
     * <code>XSObjectList</code>.
     */
    public XSObjectList getAttributeUses();

    /**
     * An attribute wildcard if it exists, otherwise <code>null</code>.
     */
    public XSWildcard getAttributeWildcard();

    /**
     * [content type]: one of empty (<code>CONTENTTYPE_EMPTY</code>), a simple
     * type definition (<code>CONTENTTYPE_SIMPLE</code>), mixed (
     * <code>CONTENTTYPE_MIXED</code>), or element-only (
     * <code>CONTENTTYPE_ELEMENT</code>).
     */
    public short getContentType();

    /**
     * A simple type definition corresponding to a simple content model,
     * otherwise <code>null</code>.
     */
    public XSSimpleTypeDefinition getSimpleType();

    /**
     * A particle for a mixed or element-only content model, otherwise
     * <code>null</code>.
     */
    public XSParticle getParticle();

    /**
     * [prohibited substitutions]: a subset of {extension, restriction}
     * @param restriction  Extension or restriction constants (see
     *   <code>XSConstants</code>).
     * @return True if <code>restriction</code> is a prohibited substitution,
     *   otherwise false.
     */
    public boolean isProhibitedSubstitution(short restriction);

    /**
     *  [prohibited substitutions]: A subset of {extension, restriction} or
     * <code>DERIVATION_NONE</code> represented as a bit flag (see
     * <code>XSConstants</code>).
     */
    public short getProhibitedSubstitutions();

    /**
     * A sequence of [annotations] or an empty <code>XSObjectList</code>.
     */
    public XSObjectList getAnnotations();

}
