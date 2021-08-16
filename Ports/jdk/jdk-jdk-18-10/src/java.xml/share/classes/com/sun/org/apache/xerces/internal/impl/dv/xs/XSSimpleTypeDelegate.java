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

package com.sun.org.apache.xerces.internal.impl.dv.xs;

import com.sun.org.apache.xerces.internal.impl.dv.DatatypeException;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeFacetException;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import com.sun.org.apache.xerces.internal.impl.dv.XSFacets;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSNamespaceItem;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;

/**
 * Base class for XSSimpleType wrapper implementations.
 *
 * @xerces.internal
 *
 */
public class XSSimpleTypeDelegate
    implements XSSimpleType {

    protected final XSSimpleType type;

    public XSSimpleTypeDelegate(XSSimpleType type) {
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
    }

    public XSSimpleType getWrappedXSSimpleType() {
        return type;
    }

    public XSObjectList getAnnotations() {
        return type.getAnnotations();
    }

    public boolean getBounded() {
        return type.getBounded();
    }

    public short getBuiltInKind() {
        return type.getBuiltInKind();
    }

    public short getDefinedFacets() {
        return type.getDefinedFacets();
    }

    public XSObjectList getFacets() {
        return type.getFacets();
    }

    public XSObject getFacet(int facetType) {
        return type.getFacet(facetType);
    }

    public boolean getFinite() {
        return type.getFinite();
    }

    public short getFixedFacets() {
        return type.getFixedFacets();
    }

    public XSSimpleTypeDefinition getItemType() {
        return type.getItemType();
    }

    public StringList getLexicalEnumeration() {
        return type.getLexicalEnumeration();
    }

    public String getLexicalFacetValue(short facetName) {
        return type.getLexicalFacetValue(facetName);
    }

    public StringList getLexicalPattern() {
        return type.getLexicalPattern();
    }

    public XSObjectList getMemberTypes() {
        return type.getMemberTypes();
    }

    public XSObjectList getMultiValueFacets() {
        return type.getMultiValueFacets();
    }

    public boolean getNumeric() {
        return type.getNumeric();
    }

    public short getOrdered() {
        return type.getOrdered();
    }

    public XSSimpleTypeDefinition getPrimitiveType() {
        return type.getPrimitiveType();
    }

    public short getVariety() {
        return type.getVariety();
    }

    public boolean isDefinedFacet(short facetName) {
        return type.isDefinedFacet(facetName);
    }

    public boolean isFixedFacet(short facetName) {
        return type.isFixedFacet(facetName);
    }

    public boolean derivedFrom(String namespace, String name, short derivationMethod) {
        return type.derivedFrom(namespace, name, derivationMethod);
    }

    public boolean derivedFromType(XSTypeDefinition ancestorType, short derivationMethod) {
        return type.derivedFromType(ancestorType, derivationMethod);
    }

    public boolean getAnonymous() {
        return type.getAnonymous();
    }

    public XSTypeDefinition getBaseType() {
        return type.getBaseType();
    }

    public short getFinal() {
        return type.getFinal();
    }

    public short getTypeCategory() {
        return type.getTypeCategory();
    }

    public boolean isFinal(short restriction) {
        return type.isFinal(restriction);
    }

    public String getName() {
        return type.getName();
    }

    public String getNamespace() {
        return type.getNamespace();
    }

    public XSNamespaceItem getNamespaceItem() {
        return type.getNamespaceItem();
    }

    public short getType() {
        return type.getType();
    }

    public void applyFacets(XSFacets facets, short presentFacet, short fixedFacet, ValidationContext context)
        throws InvalidDatatypeFacetException {
        type.applyFacets(facets, presentFacet, fixedFacet, context);
    }

    public short getPrimitiveKind() {
        return type.getPrimitiveKind();
    }

    public short getWhitespace() throws DatatypeException {
        return type.getWhitespace();
    }

    public boolean isEqual(Object value1, Object value2) {
        return type.isEqual(value1, value2);
    }

    public boolean isIDType() {
        return type.isIDType();
    }

    public void validate(ValidationContext context, ValidatedInfo validatedInfo)
        throws InvalidDatatypeValueException {
        type.validate(context, validatedInfo);
    }

    public Object validate(String content, ValidationContext context, ValidatedInfo validatedInfo)
        throws InvalidDatatypeValueException {
        return type.validate(content, context, validatedInfo);
    }

    public Object validate(Object content, ValidationContext context, ValidatedInfo validatedInfo)
        throws InvalidDatatypeValueException {
        return type.validate(content, context, validatedInfo);
    }

    public String toString() {
        return type.toString();
    }
}
