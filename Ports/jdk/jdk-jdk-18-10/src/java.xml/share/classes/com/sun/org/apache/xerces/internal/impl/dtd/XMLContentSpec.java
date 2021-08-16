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

package com.sun.org.apache.xerces.internal.impl.dtd;

/**
 * ContentSpec really exists to aid the parser classes in implementing
 * access to the grammar.
 * <p>
 * This class is used by the DTD scanner and the validator classes,
 * allowing them to be used separately or together.  This "struct"
 * class is used to build content models for validation, where it
 * is more efficient to fetch all of the information for each of
 * these content model "fragments" than to fetch each field one at
 * a time.  Since configurations are allowed to have validators
 * without a DTD scanner (i.e. a schema validator) and a DTD scanner
 * without a validator (non-validating processor), this class can be
 * used by each without requiring the presence of the other.
 * <p>
 * When processing element declarations, the DTD scanner will build
 * up a representation of the content model using the node types that
 * are defined here.  Since a non-validating processor only needs to
 * remember the type of content model declared (i.e. ANY, EMPTY, MIXED,
 * or CHILDREN), it is free to discard the specific details of the
 * MIXED and CHILDREN content models described using this class.
 * <p>
 * In the typical case of a validating processor reading the grammar
 * of the document from a DTD, the information about the content model
 * declared will be preserved and later "compiled" into an efficient
 * form for use during element validation.  Each content spec node
 * that is saved is assigned a unique index that is used as a handle
 * for the "value" or "otherValue" fields of other content spec nodes.
 * A leaf node has a "value" that is either an index in the string
 * pool of the element type of that leaf, or a value of -1 to indicate
 * the special "#PCDATA" leaf type used in a mixed content model.
 * <p>
 * For a mixed content model, the content spec will be made up of
 * leaf and choice content spec nodes, with an optional "zero or more"
 * node.  For example, the mixed content declaration "(#PCDATA)" would
 * contain a single leaf node with a node value of -1.  A mixed content
 * declaration of "(#PCDATA|foo)*" would have a content spec consisting
 * of two leaf nodes, for the "#PCDATA" and "foo" choices, a choice node
 * with the "value" set to the index of the "#PCDATA" leaf node and the
 * "otherValue" set to the index of the "foo" leaf node, and a "zero or
 * more" node with the "value" set to the index of the choice node.  If
 * the content model has more choices, for example "(#PCDATA|a|b)*", then
 * there will be more corresponding choice and leaf nodes, the choice
 * nodes will be chained together through the "value" field with each
 * leaf node referenced by the "otherValue" field.
 * <p>
 * For element content models, there are sequence nodes and also "zero or
 * one" and "one or more" nodes.  The leaf nodes would always have a valid
 * string pool index, as the "#PCDATA" leaf is not used in the declarations
 * for element content models.
 *
 * @xerces.internal
 *
 */
public class XMLContentSpec {

    //
    // Constants
    //

    /**
     * Name or #PCDATA. Leaf nodes that represent parsed character
     * data (#PCDATA) have values of -1.
     */
    public static final short CONTENTSPECNODE_LEAF = 0;

    /** Represents a zero or one occurence count, '?'. */
    public static final short CONTENTSPECNODE_ZERO_OR_ONE = 1;

    /** Represents a zero or more occurence count, '*'. */
    public static final short CONTENTSPECNODE_ZERO_OR_MORE = 2;

    /** Represents a one or more occurence count, '+'. */
    public static final short CONTENTSPECNODE_ONE_OR_MORE = 3;

    /** Represents choice, '|'. */
    public static final short CONTENTSPECNODE_CHOICE = 4;

    /** Represents sequence, ','. */
    public static final short CONTENTSPECNODE_SEQ = 5;

    /**
     * Represents any namespace specified namespace. When the element
     * found in the document must belong to a specific namespace,
     * <code>otherValue</code> will contain the name of the namespace.
     * If <code>otherValue</code> is <code>-1</code> then the element
     * can be from any namespace.
     * <p>
     * Lists of valid namespaces are created from choice content spec
     * nodes that have any content spec nodes as children.
     */
    public static final short CONTENTSPECNODE_ANY = 6;

    /**
     * Represents any other namespace (XML Schema: ##other).
     * <p>
     * When the content spec node type is set to CONTENTSPECNODE_ANY_OTHER,
     * <code>value</code> will contain the namespace that <em>cannot</em>
     * occur.
     */
    public static final short CONTENTSPECNODE_ANY_OTHER = 7;

    /** Represents any local element (XML Schema: ##local). */
    public static final short CONTENTSPECNODE_ANY_LOCAL = 8;

    /** prcessContent is 'lax' **/
    public static final short CONTENTSPECNODE_ANY_LAX = 22;

    public static final short CONTENTSPECNODE_ANY_OTHER_LAX = 23;

    public static final short CONTENTSPECNODE_ANY_LOCAL_LAX = 24;

    /** processContent is 'skip' **/

    public static final short CONTENTSPECNODE_ANY_SKIP = 38;

    public static final short CONTENTSPECNODE_ANY_OTHER_SKIP = 39;

    public static final short CONTENTSPECNODE_ANY_LOCAL_SKIP = 40;
    //
    // Data
    //

    /**
     * The content spec node type.
     *
     * @see #CONTENTSPECNODE_LEAF
     * @see #CONTENTSPECNODE_ZERO_OR_ONE
     * @see #CONTENTSPECNODE_ZERO_OR_MORE
     * @see #CONTENTSPECNODE_ONE_OR_MORE
     * @see #CONTENTSPECNODE_CHOICE
     * @see #CONTENTSPECNODE_SEQ
     */
    public short type;

    /**
     * The "left hand" value object of the content spec node.
     * leaf name.localpart, single child for unary ops, left child for binary ops.
     */
    public Object value;

    /**
     * The "right hand" value of the content spec node.
     *  leaf name.uri, right child for binary ops
     */
    public Object otherValue;

    //
    // Constructors
    //

    /** Default constructor. */
    public XMLContentSpec() {
        clear();
    }

    /** Constructs a content spec with the specified values. */
    public XMLContentSpec(short type, Object value, Object otherValue) {
        setValues(type, value, otherValue);
    }

    /**
     * Constructs a content spec from the values in the specified content spec.
     */
    public XMLContentSpec(XMLContentSpec contentSpec) {
        setValues(contentSpec);
    }

    /**
     * Constructs a content spec from the values specified by the given
     * content spec provider and identifier.
     */
    public XMLContentSpec(XMLContentSpec.Provider provider,
                          int contentSpecIndex) {
        setValues(provider, contentSpecIndex);
    }

    //
    // Public methods
    //

    /** Clears the values. */
    public void clear() {
        type = -1;
        value = null;
        otherValue = null;
    }

    /** Sets the values. */
    public void setValues(short type, Object value, Object otherValue) {
        this.type = type;
        this.value = value;
        this.otherValue = otherValue;
    }

    /** Sets the values of the specified content spec. */
    public void setValues(XMLContentSpec contentSpec) {
        type = contentSpec.type;
        value = contentSpec.value;
        otherValue = contentSpec.otherValue;
    }

    /**
     * Sets the values from the values specified by the given content spec
     * provider and identifier. If the specified content spec cannot be
     * provided, the values of this content spec are cleared.
     */
    public void setValues(XMLContentSpec.Provider provider,
                          int contentSpecIndex) {
        if (!provider.getContentSpec(contentSpecIndex, this)) {
            clear();
        }
    }


    //
    // Object methods
    //

    /** Returns a hash code for this node. */
    public int hashCode() {
        return type << 16 |
               value.hashCode() << 8 |
               otherValue.hashCode();
    }

    /** Returns true if the two objects are equal. */
    public boolean equals(Object object) {
        if (object != null && object instanceof XMLContentSpec) {
            XMLContentSpec contentSpec = (XMLContentSpec)object;
            return type == contentSpec.type &&
                   value == contentSpec.value &&
                   otherValue == contentSpec.otherValue;
        }
        return false;
    }


    //
    // Interfaces
    //

    /**
     * Provides a means for walking the structure built out of
     * content spec "nodes". The user of this provider interface is
     * responsible for knowing what the content spec node values
     * "mean". If those values refer to content spec identifiers,
     * then the user can call back into the provider to get the
     * next content spec node in the structure.
     *
     * @xerces.internal
     */
    public interface Provider {

        //
        // XMLContentSpec.Provider methods
        //

        /**
         * Fills in the provided content spec structure with content spec
         * information for a unique identifier.
         *
         * @param contentSpecIndex The content spec identifier. All content
         *                         spec "nodes" have a unique identifier.
         * @param contentSpec      The content spec struct to fill in with
         *                         the information.
         *
         * @return Returns true if the contentSpecIndex was found.
         */
        public boolean getContentSpec(int contentSpecIndex, XMLContentSpec contentSpec);

    } // interface Provider

} // class XMLContentSpec
