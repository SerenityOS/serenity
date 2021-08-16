/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.dom;

import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.xs.AttributePSVImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.StringListImpl;
import com.sun.org.apache.xerces.internal.xs.*;
import com.sun.org.apache.xerces.internal.xs.AttributePSVI;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Attribute namespace implementation; stores PSVI attribute items.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 */
public class PSVIAttrNSImpl extends AttrNSImpl implements AttributePSVI {

    /** Serialization version. */
    static final long serialVersionUID = -3241738699421018889L;

    /**
     * Construct an attribute node.
     */
    public PSVIAttrNSImpl(CoreDocumentImpl ownerDocument, String namespaceURI,
                          String qualifiedName, String localName) {
        super(ownerDocument, namespaceURI, qualifiedName, localName);
    }

    /**
     * Construct an attribute node.
     */
    public PSVIAttrNSImpl(CoreDocumentImpl ownerDocument, String namespaceURI,
                          String qualifiedName) {
        super(ownerDocument, namespaceURI, qualifiedName);
    }

    /** attribute declaration */
    protected XSAttributeDeclaration fDeclaration = null;

    /** type of attribute, simpleType */
    protected XSTypeDefinition fTypeDecl = null;

    /** If this attribute was explicitly given a
     * value in the original document, this is true; otherwise, it is false  */
    protected boolean fSpecified = true;

    /** Schema value */
    protected ValidatedInfo fValue = new ValidatedInfo();

    /** validation attempted: none, partial, full */
    protected short fValidationAttempted = AttributePSVI.VALIDATION_NONE;

    /** validity: valid, invalid, unknown */
    protected short fValidity = AttributePSVI.VALIDITY_NOTKNOWN;

    /** error codes */
    protected StringList fErrorCodes = null;

    /** error messages */
    protected StringList fErrorMessages = null;

    /** validation context: could be QName or XPath expression*/
    protected String fValidationContext = null;

    //
    // AttributePSVI methods
    //

    /* (non-Javadoc)
     * @see org.apache.xerces.xs.ItemPSVI#constant()
     */
    public ItemPSVI constant() {
        return new AttributePSVImpl(true, this);
    }

    /* (non-Javadoc)
     * @see org.apache.xerces.xs.ItemPSVI#isConstant()
     */
    public boolean isConstant() {
        return false;
    }

    /**
     * [schema default]
     *
     * @return The canonical lexical representation of the declaration's {value constraint} value.
     * @see <a href="http://www.w3.org/TR/xmlschema-1/#e-schema_default>XML Schema Part 1: Structures [schema default]</a>
     */
    @SuppressWarnings("deprecation")
    public String getSchemaDefault() {
        return fDeclaration == null ? null : fDeclaration.getConstraintValue();
    }

    /**
     * [schema normalized value]
     *
     *
     * @see <a href="http://www.w3.org/TR/xmlschema-1/#e-schema_normalized_value>XML Schema Part 1: Structures [schema normalized value]</a>
     * @return the normalized value of this item after validation
     */
    @Deprecated
    public String getSchemaNormalizedValue() {
        return fValue.getNormalizedValue();
    }

    /**
     * [schema specified]
     * @see <a href="http://www.w3.org/TR/xmlschema-1/#e-schema_specified">XML Schema Part 1: Structures [schema specified]</a>
     * @return false value was specified in schema, true value comes from the infoset
     */
    public boolean getIsSchemaSpecified() {
        return fSpecified;
    }


    /**
     * Determines the extent to which the document has been validated
     *
     * @return return the [validation attempted] property. The possible values are
     *         NO_VALIDATION, PARTIAL_VALIDATION and FULL_VALIDATION
     */
    public short getValidationAttempted() {
        return fValidationAttempted;
    }

    /**
     * Determine the validity of the node with respect
     * to the validation being attempted
     *
     * @return return the [validity] property. Possible values are:
     *         UNKNOWN_VALIDITY, INVALID_VALIDITY, VALID_VALIDITY
     */
    public short getValidity() {
        return fValidity;
    }

    /**
     * A list of error codes generated from validation attempts.
     * Need to find all the possible subclause reports that need reporting
     *
     * @return list of error codes
     */
    public StringList getErrorCodes() {
        if (fErrorCodes != null) {
            return fErrorCodes;
        }
        return StringListImpl.EMPTY_LIST;
    }

    /**
     * A list of error messages generated from the validation attempt or
     * an empty <code>StringList</code> if no errors occurred during the
     * validation attempt. The indices of error messages in this list are
     * aligned with those in the <code>[schema error code]</code> list.
     */
    public StringList getErrorMessages() {
        if (fErrorMessages != null) {
            return fErrorMessages;
        }
        return StringListImpl.EMPTY_LIST;
    }

    // This is the only information we can provide in a pipeline.
    public String getValidationContext() {
        return fValidationContext;
    }

    /**
     * An item isomorphic to the type definition used to validate this element.
     *
     * @return  a type declaration
     */
    public XSTypeDefinition getTypeDefinition() {
        return fTypeDecl;
    }

    /**
     * If and only if that type definition is a simple type definition
     * with {variety} union, or a complex type definition whose {content type}
     * is a simple type definition with {variety} union, then an item isomorphic
     * to that member of the union's {member type definitions} which actually
     * validated the element item's normalized value.
     *
     * @return  a simple type declaration
     */
    public XSSimpleTypeDefinition getMemberTypeDefinition() {
        return fValue.getMemberTypeDefinition();
    }

    /**
     * An item isomorphic to the attribute declaration used to validate
     * this attribute.
     *
     * @return  an attribute declaration
     */
    public XSAttributeDeclaration getAttributeDeclaration() {
        return fDeclaration;
    }

    /**
     * Copy PSVI properties from another psvi item.
     *
     * @param attr  the source of attribute PSVI items
     */
    public void setPSVI(AttributePSVI attr) {
        this.fDeclaration = attr.getAttributeDeclaration();
        this.fValidationContext = attr.getValidationContext();
        this.fValidity = attr.getValidity();
        this.fValidationAttempted = attr.getValidationAttempted();
        this.fErrorCodes = attr.getErrorCodes();
        this.fErrorMessages = attr.getErrorMessages();
        this.fValue.copyFrom(attr.getSchemaValue());
        this.fTypeDecl = attr.getTypeDefinition();
        this.fSpecified = attr.getIsSchemaSpecified();
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.ItemPSVI#getActualNormalizedValue()
     */
    @Deprecated
    public Object getActualNormalizedValue() {
        return fValue.getActualValue();
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.ItemPSVI#getActualNormalizedValueType()
     */
    @Deprecated
    public short getActualNormalizedValueType() {
        return fValue.getActualValueType();
    }

    /* (non-Javadoc)
     * @see com.sun.org.apache.xerces.internal.xs.ItemPSVI#getItemValueTypes()
     */
    @Deprecated
    public ShortList getItemValueTypes() {
        return fValue.getListValueTypes();
    }

    /* (non-Javadoc)
     * @see org.apache.xerces.xs.ItemPSVI#getSchemaValue()
     */
    public XSValue getSchemaValue() {
        return fValue;
    }

    // REVISIT: Forbid serialization of PSVI DOM until
    // we support object serialization of grammars -- mrglavas

    private void writeObject(ObjectOutputStream out)
        throws IOException {
        throw new NotSerializableException(getClass().getName());
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        throw new NotSerializableException(getClass().getName());
    }
}
