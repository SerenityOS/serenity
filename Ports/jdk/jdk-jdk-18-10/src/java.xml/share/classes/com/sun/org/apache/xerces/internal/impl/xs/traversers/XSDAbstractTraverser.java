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

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.XSFacets;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationState;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeGroupDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeUseImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSWildcardDecl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.NamespaceSupport;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSAttributeUse;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.w3c.dom.Element;

/**
 * Class <code>XSDAbstractTraverser</code> serves as the base class for all
 * other <code>XSD???Traverser</code>s. It holds the common data and provide
 * a unified way to initialize these data.
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 * @author Rahul Srivastava, Sun Microsystems Inc.
 * @author Neeraj Bajaj, Sun Microsystems Inc.
 *
 * @LastModified: Oct 2017
 */
abstract class XSDAbstractTraverser {

    protected static final String NO_NAME      = "(no name)";

    // Flags for checkOccurrences to indicate any special
    // restrictions on minOccurs and maxOccurs relating to "all".
    //    NOT_ALL_CONTEXT    - not processing an <all>
    //    PROCESSING_ALL_EL  - processing an <element> in an <all>
    //    GROUP_REF_WITH_ALL - processing <group> reference that contained <all>
    //    CHILD_OF_GROUP     - processing a child of a model group definition
    //    PROCESSING_ALL_GP  - processing an <all> group itself

    protected static final int NOT_ALL_CONTEXT    = 0;
    protected static final int PROCESSING_ALL_EL  = 1;
    protected static final int GROUP_REF_WITH_ALL = 2;
    protected static final int CHILD_OF_GROUP     = 4;
    protected static final int PROCESSING_ALL_GP  = 8;

    //Shared data
    protected XSDHandler            fSchemaHandler = null;
    protected SymbolTable           fSymbolTable = null;
    protected XSAttributeChecker    fAttrChecker = null;
    protected boolean               fValidateAnnotations = false;

    // used to validate default/fixed attribute values
    ValidationState fValidationState = new ValidationState();

    XSDAbstractTraverser (XSDHandler handler,
            XSAttributeChecker attrChecker) {
        fSchemaHandler = handler;
        fAttrChecker = attrChecker;
    }

    void reset(SymbolTable symbolTable, boolean validateAnnotations, Locale locale) {
        fSymbolTable = symbolTable;
        fValidateAnnotations = validateAnnotations;
        fValidationState.setExtraChecking(false);
        fValidationState.setSymbolTable(symbolTable);
        fValidationState.setLocale(locale);
    }

    // traverse the annotation declaration
    // REVISIT: how to pass the parentAttrs? as DOM attributes?
    //          as name/value pairs (string)? in parsed form?
    // @return XSAnnotationImpl object
    XSAnnotationImpl traverseAnnotationDecl(Element annotationDecl, Object[] parentAttrs,
            boolean isGlobal, XSDocumentInfo schemaDoc) {
        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(annotationDecl, isGlobal, schemaDoc);
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        String contents = DOMUtil.getAnnotation(annotationDecl);
        Element child = DOMUtil.getFirstChildElement(annotationDecl);
        if (child != null) {
            do {
                String name = DOMUtil.getLocalName(child);

                // the only valid children of "annotation" are
                // "appinfo" and "documentation"
                if (!((name.equals(SchemaSymbols.ELT_APPINFO)) ||
                        (name.equals(SchemaSymbols.ELT_DOCUMENTATION)))) {
                    reportSchemaError("src-annotation", new Object[]{name}, child);
                }
                else {
                    // General Attribute Checking
                    // There is no difference between global or local appinfo/documentation,
                    // so we assume it's always global.
                    attrValues = fAttrChecker.checkAttributes(child, true, schemaDoc);
                    fAttrChecker.returnAttrArray(attrValues, schemaDoc);
                }

                child = DOMUtil.getNextSiblingElement(child);
            }
            while (child != null);
        }
        // if contents was null, must have been some kind of error;
        // nothing to contribute to PSVI
        if (contents == null) return null;

        // find the grammar; fSchemaHandler must be known!
        SchemaGrammar grammar = fSchemaHandler.getGrammar(schemaDoc.fTargetNamespace);
        // fish out local attributes passed from parent
        @SuppressWarnings("unchecked")
        List<String> annotationLocalAttrs = (ArrayList<String>)parentAttrs[XSAttributeChecker.ATTIDX_NONSCHEMA];
        // optimize for case where there are no local attributes
        if(annotationLocalAttrs != null && !annotationLocalAttrs.isEmpty()) {
            StringBuilder localStrBuffer = new StringBuilder(64);
            localStrBuffer.append(" ");
            //ArrayList<>should contain rawname value pairs
            int i = 0;
            while (i < annotationLocalAttrs.size()) {
                String rawname = annotationLocalAttrs.get(i++);
                int colonIndex = rawname.indexOf(':');
                String prefix, localpart;
                if (colonIndex == -1) {
                    prefix = "";
                    localpart = rawname;
                }
                else {
                    prefix = rawname.substring(0,colonIndex);
                    localpart = rawname.substring(colonIndex+1);
                }
                String uri = schemaDoc.fNamespaceSupport.getURI(fSymbolTable.addSymbol(prefix));
                if (annotationDecl.getAttributeNS(uri, localpart).length() != 0) {
                    i++; // skip the next value, too
                    continue;
                }
                localStrBuffer.append(rawname)
                .append("=\"");
                String value = annotationLocalAttrs.get(i++);
                // search for pesky "s and <s within attr value:
                value = processAttValue(value);
                localStrBuffer.append(value)
                .append("\" ");
            }
            // and now splice it into place; immediately after the annotation token, for simplicity's sake
            StringBuilder contentBuffer = new StringBuilder(contents.length() + localStrBuffer.length());
            int annotationTokenEnd = contents.indexOf(SchemaSymbols.ELT_ANNOTATION);
            // annotation must occur somewhere or we're in big trouble...
            if(annotationTokenEnd == -1) return null;
            annotationTokenEnd += SchemaSymbols.ELT_ANNOTATION.length();
            contentBuffer.append(contents.substring(0,annotationTokenEnd));
            contentBuffer.append(localStrBuffer.toString());
            contentBuffer.append(contents.substring(annotationTokenEnd, contents.length()));
            final String annotation = contentBuffer.toString();
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(annotation, annotationDecl));
            }
            return new XSAnnotationImpl(annotation, grammar);
        } else {
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(contents, annotationDecl));
            }
            return new XSAnnotationImpl(contents, grammar);
        }

    }

    XSAnnotationImpl traverseSyntheticAnnotation(Element annotationParent, String initialContent,
            Object[] parentAttrs, boolean isGlobal, XSDocumentInfo schemaDoc) {

        String contents = initialContent;

        // find the grammar; fSchemaHandler must be known!
        SchemaGrammar grammar = fSchemaHandler.getGrammar(schemaDoc.fTargetNamespace);
        // fish out local attributes passed from parent
        @SuppressWarnings("unchecked")
        List<String> annotationLocalAttrs = (ArrayList<String>)parentAttrs[XSAttributeChecker.ATTIDX_NONSCHEMA];
        // optimize for case where there are no local attributes
        if (annotationLocalAttrs != null && !annotationLocalAttrs.isEmpty()) {
            StringBuilder localStrBuffer = new StringBuilder(64);
            localStrBuffer.append(" ");
            //ArrayList<>should contain rawname value pairs
            int i = 0;
            while (i < annotationLocalAttrs.size()) {
                String rawname = annotationLocalAttrs.get(i++);
                int colonIndex = rawname.indexOf(':');
                String prefix, localpart;
                if (colonIndex == -1) {
                    prefix = "";
                    localpart = rawname;
                }
                else {
                    prefix = rawname.substring(0,colonIndex);
                    localpart = rawname.substring(colonIndex+1);
                }
                String uri = schemaDoc.fNamespaceSupport.getURI(fSymbolTable.addSymbol(prefix));
                localStrBuffer.append(rawname)
                .append("=\"");
                String value = annotationLocalAttrs.get(i++);
                // search for pesky "s and <s within attr value:
                value = processAttValue(value);
                localStrBuffer.append(value)
                .append("\" ");
            }
            // and now splice it into place; immediately after the annotation token, for simplicity's sake
            StringBuilder contentBuffer = new StringBuilder(contents.length() + localStrBuffer.length());
            int annotationTokenEnd = contents.indexOf(SchemaSymbols.ELT_ANNOTATION);
            // annotation must occur somewhere or we're in big trouble...
            if(annotationTokenEnd == -1) return null;
            annotationTokenEnd += SchemaSymbols.ELT_ANNOTATION.length();
            contentBuffer.append(contents.substring(0,annotationTokenEnd));
            contentBuffer.append(localStrBuffer.toString());
            contentBuffer.append(contents.substring(annotationTokenEnd, contents.length()));
            final String annotation = contentBuffer.toString();
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(annotation, annotationParent));
            }
            return new XSAnnotationImpl(annotation, grammar);
        } else {
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(contents, annotationParent));
            }
            return new XSAnnotationImpl(contents, grammar);
        }
    }

    // the QName simple type used to resolve qnames
    private static final XSSimpleType fQNameDV = (XSSimpleType)SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(SchemaSymbols.ATTVAL_QNAME);
    // Temp data structures to be re-used in traversing facets
    private StringBuilder fPattern = new StringBuilder();
    private final XSFacets xsFacets = new XSFacets();

    static final class FacetInfo {

        final XSFacets facetdata;
        final Element nodeAfterFacets;
        final short fPresentFacets;
        final short fFixedFacets;

        FacetInfo(XSFacets facets, Element nodeAfterFacets, short presentFacets, short fixedFacets) {
            facetdata = facets;
            this.nodeAfterFacets = nodeAfterFacets;
            fPresentFacets = presentFacets;
            fFixedFacets = fixedFacets;
        }
    }

    FacetInfo traverseFacets(Element content,
            XSTypeDefinition typeDef,
            XSSimpleType baseValidator,
            XSDocumentInfo schemaDoc) {

        short facetsPresent = 0 ;
        short facetsFixed = 0; // facets that have fixed="true"
        String facet;
        boolean hasQName = containsQName(baseValidator);
        List<String> enumData = null;
        XSObjectListImpl enumAnnotations = null;
        XSObjectListImpl patternAnnotations = null;
        List<NamespaceContext> enumNSDecls = hasQName ? new ArrayList<>() : null;
        int currentFacet = 0;
        xsFacets.reset();
        boolean seenPattern = false;
        Element contextNode = (Element)content.getParentNode();
        boolean hasLengthFacet = false, hasMinLengthFacet = false, hasMaxLengthFacet = false;
        while (content != null) {
            // General Attribute Checking
            Object[] attrs = null;
            facet = DOMUtil.getLocalName(content);
            if (facet.equals(SchemaSymbols.ELT_ENUMERATION)) {
                attrs = fAttrChecker.checkAttributes(content, false, schemaDoc, hasQName);
                String enumVal = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                // The facet can't be used if the value is missing. Ignore
                // this facet element.
                if (enumVal == null) {
                    reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_ENUMERATION, SchemaSymbols.ATT_VALUE}, content);
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }

                NamespaceSupport nsDecls = (NamespaceSupport)attrs[XSAttributeChecker.ATTIDX_ENUMNSDECLS];

                // for NOTATION types, need to check whether there is a notation
                // declared with the same name as the enumeration value.
                if (baseValidator.getVariety() == XSSimpleType.VARIETY_ATOMIC &&
                        baseValidator.getPrimitiveKind() == XSSimpleType.PRIMITIVE_NOTATION) {
                    // need to use the namespace context returned from checkAttributes
                    schemaDoc.fValidationContext.setNamespaceSupport(nsDecls);
                    Object notation = null;
                    try{
                        QName temp = (QName)fQNameDV.validate(enumVal, schemaDoc.fValidationContext, null);
                        // try to get the notation decl. if failed, getGlobalDecl
                        // reports an error, so we don't need to report one again.
                        notation = fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.NOTATION_TYPE, temp, content);
                    }catch(InvalidDatatypeValueException ex){
                        reportSchemaError(ex.getKey(), ex.getArgs(), content);
                    }
                    if (notation == null) {
                        // Either the QName value is invalid, or it doens't
                        // resolve to a notation declaration.
                        // Ignore this facet, to avoid instance validation problems
                        fAttrChecker.returnAttrArray (attrs, schemaDoc);
                        content = DOMUtil.getNextSiblingElement(content);
                        continue;
                    }
                    // restore to the normal namespace context
                    schemaDoc.fValidationContext.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                }
                if (enumData == null){
                    enumData = new ArrayList<>();
                    enumAnnotations = new XSObjectListImpl();
                }
                enumData.add(enumVal);
                enumAnnotations.addXSObject(null);
                if (hasQName)
                    enumNSDecls.add(nsDecls);
                Element child = DOMUtil.getFirstChildElement( content );

                if (child != null &&
                    DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    // traverse annotation if any
                    enumAnnotations.addXSObject(enumAnnotations.getLength()-1,traverseAnnotationDecl(child, attrs, false, schemaDoc));
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(content);
                    if (text != null) {
                        enumAnnotations.addXSObject(enumAnnotations.getLength()-1, traverseSyntheticAnnotation(content, text, attrs, false, schemaDoc));
                    }
                }
                if (child !=null) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{"enumeration", "(annotation?)", DOMUtil.getLocalName(child)}, child);
                }
            }
            else if (facet.equals(SchemaSymbols.ELT_PATTERN)) {
                attrs = fAttrChecker.checkAttributes(content, false, schemaDoc);
                String patternVal = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                // The facet can't be used if the value is missing. Ignore
                // this facet element.
                if (patternVal == null) {
                    reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_PATTERN, SchemaSymbols.ATT_VALUE}, content);
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }

                seenPattern = true;
                if (fPattern.length() == 0) {
                    fPattern.append(patternVal);
                } else {
                    // ---------------------------------------------
                    //datatypes: 5.2.4 pattern: src-multiple-pattern
                    // ---------------------------------------------
                    fPattern.append("|");
                    fPattern.append(patternVal);
                }
                Element child = DOMUtil.getFirstChildElement( content );
                if (child != null &&
                        DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    // traverse annotation if any
                    if (patternAnnotations == null){
                        patternAnnotations = new XSObjectListImpl();
                    }
                    patternAnnotations.addXSObject(traverseAnnotationDecl(child, attrs, false, schemaDoc));
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(content);
                    if (text != null) {
                        if (patternAnnotations == null){
                            patternAnnotations = new XSObjectListImpl();
                        }
                        patternAnnotations.addXSObject(traverseSyntheticAnnotation(content, text, attrs, false, schemaDoc));
                    }
                }
                if (child !=null) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{"pattern", "(annotation?)", DOMUtil.getLocalName(child)}, child);
                }
            }
            else {
                if (facet.equals(SchemaSymbols.ELT_MINLENGTH)) {
                    currentFacet = XSSimpleType.FACET_MINLENGTH;
                }
                else if (facet.equals(SchemaSymbols.ELT_MAXLENGTH)) {
                    currentFacet = XSSimpleType.FACET_MAXLENGTH;
                }
                else if (facet.equals(SchemaSymbols.ELT_MAXEXCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MAXEXCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_MAXINCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MAXINCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_MINEXCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MINEXCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_MININCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MININCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_TOTALDIGITS)) {
                    currentFacet = XSSimpleType.FACET_TOTALDIGITS;
                }
                else if (facet.equals(SchemaSymbols.ELT_FRACTIONDIGITS)) {
                    currentFacet = XSSimpleType.FACET_FRACTIONDIGITS;
                }
                else if (facet.equals(SchemaSymbols.ELT_WHITESPACE)) {
                    currentFacet = XSSimpleType.FACET_WHITESPACE;
                }
                else if (facet.equals(SchemaSymbols.ELT_LENGTH)) {
                    currentFacet = XSSimpleType.FACET_LENGTH;
                }
                else {
                    break;   // a non-facet
                }

                attrs = fAttrChecker.checkAttributes(content, false, schemaDoc);

                // check for duplicate facets
                if ((facetsPresent & currentFacet) != 0) {
                    // Ignore this facet, to avoid corrupting the previous facet
                    reportSchemaError("src-single-facet-value", new Object[]{facet}, content);
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }

                // The facet can't be used if the value is missing. Ignore
                // this facet element.
                if (attrs[XSAttributeChecker.ATTIDX_VALUE] == null) {
                    // Report an error if the "value" attribute is missing.
                    // If it's not missing, then its value is invalid, and an
                    // error should have already been reported by the
                    // attribute checker.
                    if (content.getAttributeNodeNS(null, "value") == null) {
                        reportSchemaError("s4s-att-must-appear", new Object[]{content.getLocalName(), SchemaSymbols.ATT_VALUE}, content);
                    }
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }

                facetsPresent |= currentFacet;
                // check for fixed facet
                if (((Boolean)attrs[XSAttributeChecker.ATTIDX_FIXED]).booleanValue()) {
                    facetsFixed |= currentFacet;
                }
                switch (currentFacet) {
                case XSSimpleType.FACET_MINLENGTH:
                    xsFacets.minLength = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    hasMinLengthFacet = true;
                    break;
                case XSSimpleType.FACET_MAXLENGTH:
                    xsFacets.maxLength = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    hasMaxLengthFacet = true;
                    break;
                case XSSimpleType.FACET_MAXEXCLUSIVE:
                    xsFacets.maxExclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_MAXINCLUSIVE:
                    xsFacets.maxInclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_MINEXCLUSIVE:
                    xsFacets.minExclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_MININCLUSIVE:
                    xsFacets.minInclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_TOTALDIGITS:
                    xsFacets.totalDigits = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    break;
                case XSSimpleType.FACET_FRACTIONDIGITS:
                    xsFacets.fractionDigits = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    break;
                case XSSimpleType.FACET_WHITESPACE:
                    xsFacets.whiteSpace = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).shortValue();
                    break;
                case XSSimpleType.FACET_LENGTH:
                    xsFacets.length = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    hasLengthFacet = true;
                    break;
                }

                Element child = DOMUtil.getFirstChildElement( content );
                XSAnnotationImpl annotation = null;
                if (child != null &&
                    DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    // traverse annotation if any
                    annotation = traverseAnnotationDecl(child, attrs, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(content);
                    if (text != null) {
                        annotation = traverseSyntheticAnnotation(content, text, attrs, false, schemaDoc);
                    }
               }
                switch (currentFacet) {
                case XSSimpleType.FACET_MINLENGTH:
                    xsFacets.minLengthAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MAXLENGTH:
                    xsFacets.maxLengthAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MAXEXCLUSIVE:
                    xsFacets.maxExclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MAXINCLUSIVE:
                    xsFacets.maxInclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MINEXCLUSIVE:
                    xsFacets.minExclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MININCLUSIVE:
                    xsFacets.minInclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_TOTALDIGITS:
                    xsFacets.totalDigitsAnnotation = annotation;
                break;
                case XSSimpleType.FACET_FRACTIONDIGITS:
                    xsFacets.fractionDigitsAnnotation = annotation;
                break;
                case XSSimpleType.FACET_WHITESPACE:
                    xsFacets.whiteSpaceAnnotation = annotation;
                break;
                case XSSimpleType.FACET_LENGTH:
                    xsFacets.lengthAnnotation = annotation;
                break;
                }
                if (child != null) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{facet, "(annotation?)", DOMUtil.getLocalName(child)}, child);
                }
            }
            fAttrChecker.returnAttrArray (attrs, schemaDoc);
            content = DOMUtil.getNextSiblingElement(content);
        }
        if (enumData !=null) {
            facetsPresent |= XSSimpleType.FACET_ENUMERATION;
            xsFacets.enumeration = enumData;
            xsFacets.enumNSDecls = enumNSDecls;
            xsFacets.enumAnnotations = enumAnnotations;
        }
        if (seenPattern) {
            facetsPresent |= XSSimpleType.FACET_PATTERN;
            xsFacets.pattern = fPattern.toString();
            xsFacets.patternAnnotations = patternAnnotations;
        }

        fPattern.setLength(0);

        // check if length, minLength and maxLength facets contradict with enumeration facets.
        // currently considers the case when the baseValidator is a built-in type.
        if (enumData != null) {
           if (hasLengthFacet) {
              checkEnumerationAndLengthInconsistency(baseValidator, enumData, contextNode, getSchemaTypeName(typeDef));
           }
           if (hasMinLengthFacet) {
              checkEnumerationAndMinLengthInconsistency(baseValidator, enumData, contextNode, getSchemaTypeName(typeDef));
           }
           if (hasMaxLengthFacet) {
              checkEnumerationAndMaxLengthInconsistency(baseValidator, enumData, contextNode, getSchemaTypeName(typeDef));
           }
        }

        return new FacetInfo(xsFacets, content, facetsPresent, facetsFixed);
    }

    /*
     * Get name of an XSD type definition as a string value (which will typically be the value of "name" attribute of a
     * type definition, or an internal name determined by the validator for anonymous types).
     */
    public static String getSchemaTypeName(XSTypeDefinition typeDefn) {

        String typeNameStr = "";
        if (typeDefn instanceof XSSimpleTypeDefinition) {
            typeNameStr = ((XSSimpleTypeDecl) typeDefn).getTypeName();
        }
        else {
            typeNameStr = ((XSComplexTypeDecl) typeDefn).getTypeName();
        }

        return typeNameStr;

    } // getSchemaTypeName

    /*
     * Check whether values of xs:maxLength and xs:enumeration are consistent. Report a warning message if they are not.
     */
    private void checkEnumerationAndMaxLengthInconsistency(XSSimpleType baseValidator, List<String> enumData, Element contextNode, String typeName) {
        if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(baseValidator.getNamespace()) &&
            SchemaSymbols.ATTVAL_HEXBINARY.equals(baseValidator.getName())) {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                if (enumVal.length() / 2 > xsFacets.maxLength) {
                    reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_MAXLENGTH, typeName}, contextNode);
                }
            }
        }
        else if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(baseValidator.getNamespace()) &&
                 SchemaSymbols.ATTVAL_BASE64BINARY.equals(baseValidator.getName())) {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                byte[] decodedVal = Base64.decode(enumVal);
                if (decodedVal != null && (new String(decodedVal)).length() > xsFacets.maxLength) {
                   reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_MAXLENGTH, typeName}, contextNode);
                }
            }
        }
        else {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                if (enumVal.length() > xsFacets.maxLength) {
                    reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_MAXLENGTH, typeName}, contextNode);
                }
            }
        }
    } // checkEnumerationAndMaxLengthInconsistency

    /*
     * Check whether values of xs:minLength and xs:enumeration are consistent. Report a warning message if they are not.
     */
    private void checkEnumerationAndMinLengthInconsistency(XSSimpleType baseValidator, List<String> enumData, Element contextNode, String typeName) {
        if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(baseValidator.getNamespace()) &&
            SchemaSymbols.ATTVAL_HEXBINARY.equals(baseValidator.getName())) {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                if (enumVal.length() / 2 < xsFacets.minLength) {
                    reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_MINLENGTH, typeName}, contextNode);
                }
            }
        }
        else if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(baseValidator.getNamespace()) &&
                 SchemaSymbols.ATTVAL_BASE64BINARY.equals(baseValidator.getName())) {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                byte[] decodedVal = Base64.decode(enumVal);
                if (decodedVal != null && (new String(decodedVal)).length() < xsFacets.minLength) {
                   reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_MINLENGTH, typeName}, contextNode);
                }
            }
        }
        else {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                if (enumVal.length() < xsFacets.minLength) {
                    reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_MINLENGTH, typeName}, contextNode);
                }
            }
        }
    } // checkEnumerationAndMinLengthInconsistency

    /*
     * Check whether values of xs:length and xs:enumeration are consistent. Report a warning message if they are not.
     */
    private void checkEnumerationAndLengthInconsistency(XSSimpleType baseValidator, List<String> enumData, Element contextNode, String typeName) {
        if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(baseValidator.getNamespace()) &&
            SchemaSymbols.ATTVAL_HEXBINARY.equals(baseValidator.getName())) {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                if (enumVal.length() / 2 != xsFacets.length) {
                    reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_LENGTH, typeName}, contextNode);
                }
            }
        }
        else if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(baseValidator.getNamespace()) &&
                 SchemaSymbols.ATTVAL_BASE64BINARY.equals(baseValidator.getName())) {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                byte[] decodedVal = Base64.decode(enumVal);
                if (decodedVal != null && (new String(decodedVal)).length() != xsFacets.length) {
                   reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_LENGTH, typeName}, contextNode);
                }
            }
        }
        else {
            for (int enumIdx = 0; enumIdx < enumData.size(); enumIdx++) {
                String enumVal = (enumData.get(enumIdx));
                if (enumVal.length() != xsFacets.length) {
                    reportSchemaWarning("FacetsContradict", new Object[]{enumVal, SchemaSymbols.ELT_LENGTH, typeName}, contextNode);
                }
            }
        }
    } // checkEnumerationAndLengthInconsistency


    // return whether QName/NOTATION is part of the given type
    private boolean containsQName(XSSimpleType type) {
        if (type.getVariety() == XSSimpleType.VARIETY_ATOMIC) {
            short primitive = type.getPrimitiveKind();
            return (primitive == XSSimpleType.PRIMITIVE_QNAME ||
                    primitive == XSSimpleType.PRIMITIVE_NOTATION);
        }
        else if (type.getVariety() == XSSimpleType.VARIETY_LIST) {
            return containsQName((XSSimpleType)type.getItemType());
        }
        else if (type.getVariety() == XSSimpleType.VARIETY_UNION) {
            XSObjectList members = type.getMemberTypes();
            for (int i = 0; i < members.getLength(); i++) {
                if (containsQName((XSSimpleType)members.item(i)))
                    return true;
            }
        }
        return false;
    }

    //
    // Traverse a set of attribute and attribute group elements
    // Needed by complexType and attributeGroup traversal
    // This method will return the first non-attribute/attrgrp found
    //
    Element traverseAttrsAndAttrGrps(Element firstAttr, XSAttributeGroupDecl attrGrp,
            XSDocumentInfo schemaDoc, SchemaGrammar grammar,
            XSComplexTypeDecl enclosingCT) {

        Element child=null;
        XSAttributeGroupDecl tempAttrGrp = null;
        XSAttributeUseImpl tempAttrUse = null;
        XSAttributeUse otherUse = null;
        String childName;

        for (child=firstAttr; child!=null; child=DOMUtil.getNextSiblingElement(child)) {
            childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                tempAttrUse = fSchemaHandler.fAttributeTraverser.traverseLocal(child,
                        schemaDoc,
                        grammar,
                        enclosingCT);
                if (tempAttrUse == null) continue;
                if (tempAttrUse.fUse == SchemaSymbols.USE_PROHIBITED) {
                    attrGrp.addAttributeUse(tempAttrUse);
                    continue;
                }
                otherUse = attrGrp.getAttributeUseNoProhibited(
                        tempAttrUse.fAttrDecl.getNamespace(),
                        tempAttrUse.fAttrDecl.getName());
                if (otherUse==null) {
                    String idName = attrGrp.addAttributeUse(tempAttrUse);
                    if (idName != null) {
                        String code = (enclosingCT == null) ? "ag-props-correct.3" : "ct-props-correct.5";
                        String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                        reportSchemaError(code, new Object[]{name, tempAttrUse.fAttrDecl.getName(), idName}, child);
                    }
                }
                else if (otherUse != tempAttrUse) {
                    String code = (enclosingCT == null) ? "ag-props-correct.2" : "ct-props-correct.4";
                    String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                    reportSchemaError(code, new Object[]{name, tempAttrUse.fAttrDecl.getName()}, child);
                }
            }
            else if (childName.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                //REVISIT: do we need to save some state at this point??
                tempAttrGrp = fSchemaHandler.fAttributeGroupTraverser.traverseLocal(
                        child, schemaDoc, grammar);
                if(tempAttrGrp == null ) continue;
                XSObjectList attrUseS = tempAttrGrp.getAttributeUses();
                XSAttributeUseImpl oneAttrUse;
                int attrCount = attrUseS.getLength();
                for (int i=0; i<attrCount; i++) {
                    oneAttrUse = (XSAttributeUseImpl)attrUseS.item(i);
                    if (oneAttrUse.fUse == SchemaSymbols.USE_PROHIBITED) {
                        attrGrp.addAttributeUse(oneAttrUse);
                        continue;
                    }
                    otherUse = attrGrp.getAttributeUseNoProhibited(
                            oneAttrUse.fAttrDecl.getNamespace(),
                            oneAttrUse.fAttrDecl.getName());
                    if (otherUse==null) {
                        String idName = attrGrp.addAttributeUse(oneAttrUse);
                        if (idName != null) {
                            String code = (enclosingCT == null) ? "ag-props-correct.3" : "ct-props-correct.5";
                            String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                            reportSchemaError(code, new Object[]{name, oneAttrUse.fAttrDecl.getName(), idName}, child);
                        }
                    }
                    else if (oneAttrUse != otherUse) {
                        String code = (enclosingCT == null) ? "ag-props-correct.2" : "ct-props-correct.4";
                        String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                        reportSchemaError(code, new Object[]{name, oneAttrUse.fAttrDecl.getName()}, child);
                    }
                }

                if (tempAttrGrp.fAttributeWC != null) {
                    if (attrGrp.fAttributeWC == null) {
                        attrGrp.fAttributeWC = tempAttrGrp.fAttributeWC;
                    }
                    // perform intersection of attribute wildcard
                    else {
                        attrGrp.fAttributeWC = attrGrp.fAttributeWC.
                        performIntersectionWith(tempAttrGrp.fAttributeWC, attrGrp.fAttributeWC.fProcessContents);
                        if (attrGrp.fAttributeWC == null) {
                            String code = (enclosingCT == null) ? "src-attribute_group.2" : "src-ct.4";
                            String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                            reportSchemaError(code, new Object[]{name}, child);
                        }
                    }
                }
            }
            else
                break;
        } // for

        if (child != null) {
            childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ANYATTRIBUTE)) {
                XSWildcardDecl tempAttrWC = fSchemaHandler.fWildCardTraverser.
                traverseAnyAttribute(child, schemaDoc, grammar);
                if (attrGrp.fAttributeWC == null) {
                    attrGrp.fAttributeWC = tempAttrWC;
                }
                // perform intersection of attribute wildcard
                else {
                    attrGrp.fAttributeWC = tempAttrWC.
                    performIntersectionWith(attrGrp.fAttributeWC, tempAttrWC.fProcessContents);
                    if (attrGrp.fAttributeWC == null) {
                        String code = (enclosingCT == null) ? "src-attribute_group.2" : "src-ct.4";
                        String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                        reportSchemaError(code, new Object[]{name}, child);
                    }
                }
                child = DOMUtil.getNextSiblingElement(child);
            }
        }

        // Success
        return child;

    }

    void reportSchemaError (String key, Object[] args, Element ele) {
        fSchemaHandler.reportSchemaError(key, args, ele);
    }

    void reportSchemaWarning (String key, Object[] args, Element ele) {
        fSchemaHandler.reportSchemaWarning(key, args, ele);
    }

    /**
     * Element/Attribute traversers call this method to check whether
     * the type is NOTATION without enumeration facet
     */
    void checkNotationType(String refName, XSTypeDefinition typeDecl, Element elem) {
        if (typeDecl.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE &&
                ((XSSimpleType)typeDecl).getVariety() == XSSimpleType.VARIETY_ATOMIC &&
                ((XSSimpleType)typeDecl).getPrimitiveKind() == XSSimpleType.PRIMITIVE_NOTATION) {
            if ((((XSSimpleType)typeDecl).getDefinedFacets() & XSSimpleType.FACET_ENUMERATION) == 0) {
                reportSchemaError("enumeration-required-notation", new Object[]{typeDecl.getName(), refName, DOMUtil.getLocalName(elem)}, elem);
            }
        }
    }

    // Checks constraints for minOccurs, maxOccurs
    protected XSParticleDecl checkOccurrences(XSParticleDecl particle,
            String particleName, Element parent,
            int allContextFlags,
            long defaultVals) {

        int min = particle.fMinOccurs;
        int max = particle.fMaxOccurs;
        boolean defaultMin = (defaultVals & (1 << XSAttributeChecker.ATTIDX_MINOCCURS)) != 0;
        boolean defaultMax = (defaultVals & (1 << XSAttributeChecker.ATTIDX_MAXOCCURS)) != 0;

        boolean processingAllEl = ((allContextFlags & PROCESSING_ALL_EL) != 0);
        boolean processingAllGP = ((allContextFlags & PROCESSING_ALL_GP) != 0);
        boolean groupRefWithAll = ((allContextFlags & GROUP_REF_WITH_ALL) != 0);
        boolean isGroupChild    = ((allContextFlags & CHILD_OF_GROUP) != 0);

        // Neither minOccurs nor maxOccurs may be specified
        // for the child of a model group definition.
        if (isGroupChild) {
            if (!defaultMin) {
                Object[] args = new Object[]{particleName, "minOccurs"};
                reportSchemaError("s4s-att-not-allowed", args, parent);
                min = 1;
            }
            if (!defaultMax) {
                Object[] args = new Object[]{particleName, "maxOccurs"};
                reportSchemaError("s4s-att-not-allowed", args, parent);
                max = 1;
            }
        }

        // If minOccurs=maxOccurs=0, no component is specified
        if (min == 0 && max== 0) {
            particle.fType = XSParticleDecl.PARTICLE_EMPTY;
            return null;
        }

        // For the elements referenced in an <all>, minOccurs attribute
        // must be zero or one, and maxOccurs attribute must be one.
        // For a complex type definition that contains an <all> or a
        // reference a <group> whose model group is an all model group,
        // minOccurs and maxOccurs must be one.
        if (processingAllEl) {
            if (max != 1) {
                reportSchemaError("cos-all-limited.2", new Object[]{
                        (max == SchemaSymbols.OCCURRENCE_UNBOUNDED) ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max),
                        ((XSElementDecl)particle.fValue).getName()}, parent);
                max = 1;
                if (min > 1)
                    min = 1;
            }
        }
        else if (processingAllGP || groupRefWithAll) {
            if (max != 1) {
                reportSchemaError("cos-all-limited.1.2", null, parent);
                if (min > 1)
                    min = 1;
                max = 1;
            }
        }

        particle.fMinOccurs = min;
        particle.fMaxOccurs = max;

        return particle;
    }

    private static String processAttValue(String original) {
        final int length = original.length();
        // normally, nothing will happen
        for (int i = 0; i < length; ++i) {
            char currChar = original.charAt(i);
            if (currChar == '"' || currChar == '<' || currChar == '&' ||
                    currChar == 0x09 || currChar == 0x0A || currChar == 0x0D) {
                return escapeAttValue(original, i);
            }
        }
        return original;
    }

    // this is not terribly performant!
    private static String escapeAttValue(String original, int from) {
        int i;
        final int length = original.length();
        StringBuilder newVal = new StringBuilder(length);
        newVal.append(original.substring(0, from));
        for (i = from; i < length; ++i) {
            char currChar = original.charAt(i);
            if (currChar == '"') {
                newVal.append("&quot;");
            }
            else if (currChar == '<') {
                newVal.append("&lt;");
            }
            else if (currChar == '&') {
                newVal.append("&amp;");
            }
            // Must escape 0x09, 0x0A and 0x0D if they appear in attribute
            // value so that they may be round-tripped. They would otherwise
            // be transformed to a 0x20 during attribute value normalization.
            else if (currChar == 0x09) {
                newVal.append("&#x9;");
            }
            else if (currChar == 0x0A) {
                newVal.append("&#xA;");
            }
            else if (currChar == 0x0D) {
                newVal.append("&#xD;");
            }
            else {
                newVal.append(currChar);
            }
        }
        return newVal.toString();
    }
}
