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

package com.sun.org.apache.xerces.internal.impl.xs;

import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.xs.util.StringListImpl;
import com.sun.org.apache.xerces.internal.xs.ElementPSVI;
import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import com.sun.org.apache.xerces.internal.xs.ShortList;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSNotationDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSValue;

/**
 * Element PSV infoset augmentations implementation.
 * The following information will be available at the startElement call:
 * name, namespace, type, notation, validation context
 *
 * The following information will be available at the endElement call:
 * nil, specified, normalized value, member type, validity, error codes,
 * default
 *
 * @xerces.internal
 *
 * @author Elena Litani IBM
 * @LastModified: Nov 2017
 */
public class ElementPSVImpl implements ElementPSVI {

    /** element declaration */
    protected XSElementDeclaration fDeclaration = null;

    /** type of element, could be xsi:type */
    protected XSTypeDefinition fTypeDecl = null;

    /** true if clause 3.2 of Element Locally Valid (Element) (3.3.4)
      * is satisfied, otherwise false
      */
    protected boolean fNil = false;

    /** true if the element value was provided by the schema; false otherwise.
     */
    protected boolean fSpecified = false;

    /** Schema value */
    protected ValidatedInfo fValue = new ValidatedInfo();

    /** http://www.w3.org/TR/xmlschema-1/#e-notation*/
    protected XSNotationDeclaration fNotation = null;

    /** validation attempted: none, partial, full */
    protected short fValidationAttempted = ElementPSVI.VALIDATION_NONE;

    /** validity: valid, invalid, unknown */
    protected short fValidity = ElementPSVI.VALIDITY_NOTKNOWN;

    /** error codes and error messages */
    protected String[] fErrors = null;

    /** validation context: could be QName or XPath expression*/
    protected String fValidationContext = null;

    /** deferred XSModel **/
    protected SchemaGrammar[] fGrammars = null;

    /** the schema information property */
    protected XSModel fSchemaInformation = null;

    /** true if this object is immutable **/
    protected boolean fIsConstant;

    public ElementPSVImpl() {}

    public ElementPSVImpl(boolean isConstant, ElementPSVI elementPSVI) {
        fDeclaration = elementPSVI.getElementDeclaration();
        fTypeDecl = elementPSVI.getTypeDefinition();
        fNil = elementPSVI.getNil();
        fSpecified = elementPSVI.getIsSchemaSpecified();
        fValue.copyFrom(elementPSVI.getSchemaValue());
        fNotation = elementPSVI.getNotation();
        fValidationAttempted = elementPSVI.getValidationAttempted();
        fValidity = elementPSVI.getValidity();
        fValidationContext = elementPSVI.getValidationContext();
        if (elementPSVI instanceof ElementPSVImpl) {
            final ElementPSVImpl elementPSVIImpl = (ElementPSVImpl) elementPSVI;
            fErrors = (elementPSVIImpl.fErrors != null) ? elementPSVIImpl.fErrors.clone() : null;
            elementPSVIImpl.copySchemaInformationTo(this);
        }
        else {
            final StringList errorCodes = elementPSVI.getErrorCodes();
            final int length = errorCodes.getLength();
            if (length > 0) {
                final StringList errorMessages = elementPSVI.getErrorMessages();
                final String[] errors = new String[length << 1];
                for (int i = 0, j = 0; i < length; ++i) {
                    errors[j++] = errorCodes.item(i);
                    errors[j++] = errorMessages.item(i);
                }
                fErrors = errors;
            }
            fSchemaInformation = elementPSVI.getSchemaInformation();
        }
        fIsConstant = isConstant;
    }

    //
    // ElementPSVI methods
    //

    /* (non-Javadoc)
     * @see org.apache.xerces.xs.ItemPSVI#constant()
     */
    public ItemPSVI constant() {
        if (isConstant()) {
            return this;
        }
        return new ElementPSVImpl(true, this);
    }

    /* (non-Javadoc)
     * @see org.apache.xerces.xs.ItemPSVI#isConstant()
     */
    public boolean isConstant() {
        return fIsConstant;
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
     * @return true - value was specified in schema, false - value comes from the infoset
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
     * @return Array of error codes
     */
    public StringList getErrorCodes() {
        if (fErrors == null || fErrors.length == 0) {
            return StringListImpl.EMPTY_LIST;
        }
        return new PSVIErrorList(fErrors, true);
    }

    /**
     * A list of error messages generated from the validation attempt or
     * an empty <code>StringList</code> if no errors occurred during the
     * validation attempt. The indices of error messages in this list are
     * aligned with those in the <code>[schema error code]</code> list.
     */
    public StringList getErrorMessages() {
        if (fErrors == null || fErrors.length == 0) {
            return StringListImpl.EMPTY_LIST;
        }
        return new PSVIErrorList(fErrors, false);
    }

    // This is the only information we can provide in a pipeline.
    public String getValidationContext() {
        return fValidationContext;
    }

    /**
     * [nil]
     * @see <a href="http://www.w3.org/TR/xmlschema-1/#e-nil>XML Schema Part 1: Structures [nil]</a>
     * @return true if clause 3.2 of Element Locally Valid (Element) (3.3.4) above is satisfied, otherwise false
     */
    public boolean getNil() {
        return fNil;
    }

    /**
     * [notation]
     * @see <a href="http://www.w3.org/TR/xmlschema-1/#e-notation>XML Schema Part 1: Structures [notation]</a>
     * @return The notation declaration.
     */
    public XSNotationDeclaration getNotation() {
        return fNotation;
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
     * is a simple thype definition with {variety} union, then an item isomorphic
     * to that member of the union's {member type definitions} which actually
     * validated the element item's normalized value.
     *
     * @return  a simple type declaration
     */
    public XSSimpleTypeDefinition getMemberTypeDefinition() {
        return fValue.getMemberTypeDefinition();
    }

    /**
     * An item isomorphic to the element declaration used to validate
     * this element.
     *
     * @return  an element declaration
     */
    public XSElementDeclaration getElementDeclaration() {
        return fDeclaration;
    }

    /**
     * [schema information]
     * @see <a href="http://www.w3.org/TR/xmlschema-1/#e-schema_information">XML Schema Part 1: Structures [schema information]</a>
     * @return The schema information property if it's the validation root,
     *         null otherwise.
     */
    public synchronized XSModel getSchemaInformation() {
        if (fSchemaInformation == null && fGrammars != null) {
            fSchemaInformation = new XSModelImpl(fGrammars);
        }
        return fSchemaInformation;
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
     * @see com.sun.org.apache.xerces.internal.xs.ItemPSVI#getSchemaValue()
     */
    public XSValue getSchemaValue() {
        return fValue;
    }

    /**
     * Reset() should be called in validator startElement(..) method.
     */
    public void reset() {
        fDeclaration = null;
        fTypeDecl = null;
        fNil = false;
        fSpecified = false;
        fNotation = null;
        fValidationAttempted = ElementPSVI.VALIDATION_NONE;
        fValidity = ElementPSVI.VALIDITY_NOTKNOWN;
        fErrors = null;
        fValidationContext = null;
        fValue.reset();
    }

    public void copySchemaInformationTo(ElementPSVImpl target) {
        target.fGrammars = fGrammars;
        target.fSchemaInformation = fSchemaInformation;
    }
}
