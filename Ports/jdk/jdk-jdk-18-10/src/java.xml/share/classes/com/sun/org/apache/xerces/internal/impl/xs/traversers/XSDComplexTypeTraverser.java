/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeFacetException;
import com.sun.org.apache.xerces.internal.impl.dv.SchemaDVFactory;
import com.sun.org.apache.xerces.internal.impl.dv.XSFacets;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeGroupDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeUseImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSConstraints;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSWildcardDecl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSAttributeUse;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import org.w3c.dom.Element;

/**
 * A complex type definition schema component traverser.
 *
 * <complexType
 *   abstract = boolean : false
 *   block = (#all | List of (extension | restriction))
 *   final = (#all | List of (extension | restriction))
 *   id = ID
 *   mixed = boolean : false
 *   name = NCName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleContent | complexContent |
 *            ((group | all | choice | sequence)?,
 *            ((attribute | attributeGroup)*, anyAttribute?))))
 * </complexType>
 *
 * @xerces.internal
 * @LastModified: Apr 2019
 */

class  XSDComplexTypeTraverser extends XSDAbstractParticleTraverser {

    // size of stack to hold globals:
    private final static int GLOBAL_NUM = 11;

    private static XSParticleDecl fErrorContent = null;
    private static XSWildcardDecl fErrorWildcard = null;
    private static XSParticleDecl getErrorContent() {
        if (fErrorContent == null) {
            XSParticleDecl particle = new XSParticleDecl();
            particle.fType = XSParticleDecl.PARTICLE_WILDCARD;
            particle.fValue = getErrorWildcard();
            particle.fMinOccurs = 0;
            particle.fMaxOccurs = SchemaSymbols.OCCURRENCE_UNBOUNDED;
            XSModelGroupImpl group = new XSModelGroupImpl();
            group.fCompositor = XSModelGroupImpl.MODELGROUP_SEQUENCE;
            group.fParticleCount = 1;
            group.fParticles = new XSParticleDecl[1];
            group.fParticles[0] = particle;
            XSParticleDecl errorContent = new XSParticleDecl();
            errorContent.fType = XSParticleDecl.PARTICLE_MODELGROUP;
            errorContent.fValue = group;
            fErrorContent = errorContent;
        }
        return fErrorContent;
    }
    private static XSWildcardDecl getErrorWildcard() {
        if (fErrorWildcard == null) {
            XSWildcardDecl wildcard = new XSWildcardDecl();
            wildcard.fProcessContents = XSWildcardDecl.PC_SKIP;
            fErrorWildcard = wildcard;
        }
        return fErrorWildcard;
    }

    // globals for building XSComplexTypeDecls
    private String fName = null;
    private String fTargetNamespace = null;
    private short fDerivedBy = XSConstants.DERIVATION_RESTRICTION;
    private short fFinal = XSConstants.DERIVATION_NONE;
    private short fBlock = XSConstants.DERIVATION_NONE;
    private short fContentType = XSComplexTypeDecl.CONTENTTYPE_EMPTY;
    private XSTypeDefinition fBaseType = null;
    private XSAttributeGroupDecl fAttrGrp = null;
    private XSSimpleType fXSSimpleType = null;
    private XSParticleDecl fParticle = null;
    private boolean fIsAbstract = false;
    private XSComplexTypeDecl fComplexTypeDecl = null;
    private XSAnnotationImpl [] fAnnotations = null;

    // our own little stack to retain state when getGlobalDecls is called:
    private Object [] fGlobalStore = null;
    private int fGlobalStorePos = 0;

    XSDComplexTypeTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }


    private static final boolean DEBUG=false;

    private static final class ComplexTypeRecoverableError extends Exception {

        private static final long serialVersionUID = 6802729912091130335L;

        Object[] errorSubstText=null;
        Element  errorElem = null;
        ComplexTypeRecoverableError() {
            super();
        }
        ComplexTypeRecoverableError(String msgKey, Object[] args, Element e) {
            super(msgKey);
            errorSubstText=args;
            errorElem = e;
        }

    }

    /**
     * Traverse local complexType declarations
     *
     * @param Element
     * @param XSDocumentInfo
     * @param SchemaGrammar
     * @return XSComplexTypeDecl
     */
    XSComplexTypeDecl traverseLocal(Element complexTypeNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {


        Object[] attrValues = fAttrChecker.checkAttributes(complexTypeNode, false,
                schemaDoc);
        String complexTypeName = genAnonTypeName(complexTypeNode);
        contentBackup();
        XSComplexTypeDecl type = traverseComplexTypeDecl (complexTypeNode,
                complexTypeName, attrValues, schemaDoc, grammar);
        contentRestore();
        // need to add the type to the grammar for later constraint checking
        grammar.addComplexTypeDecl(type, fSchemaHandler.element2Locator(complexTypeNode));
        type.setIsAnonymous();
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        return type;
    }

    /**
     * Traverse global complexType declarations
     *
     * @param Element
     * @param XSDocumentInfo
     * @param SchemaGrammar
     * @return XSComplexTypeDecXSComplexTypeDecl
     */
    XSComplexTypeDecl traverseGlobal (Element complexTypeNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        Object[] attrValues = fAttrChecker.checkAttributes(complexTypeNode, true,
                schemaDoc);
        String complexTypeName = (String)  attrValues[XSAttributeChecker.ATTIDX_NAME];
        contentBackup();
        XSComplexTypeDecl type = traverseComplexTypeDecl (complexTypeNode,
                complexTypeName, attrValues, schemaDoc, grammar);
        contentRestore();
        // need to add the type to the grammar for later constraint checking
        grammar.addComplexTypeDecl(type, fSchemaHandler.element2Locator(complexTypeNode));

        if (complexTypeName == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_COMPLEXTYPE, SchemaSymbols.ATT_NAME}, complexTypeNode);
            type = null;
        } else {
            if (grammar.getGlobalTypeDecl(type.getName()) == null) {
                grammar.addGlobalComplexTypeDecl(type);
            }

            // also add it to extended map
            final String loc = fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            final XSTypeDefinition type2 = grammar.getGlobalTypeDecl(type.getName(), loc);
            if (type2 == null) {
                grammar.addGlobalComplexTypeDecl(type, loc);
            }

            // handle duplicates
            if (fSchemaHandler.fTolerateDuplicates) {
                if (type2 != null) {
                    if (type2 instanceof XSComplexTypeDecl) {
                        type = (XSComplexTypeDecl) type2;
                    }
                }
                fSchemaHandler.addGlobalTypeDecl(type);
            }
        }

        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        return type;
    }


    private XSComplexTypeDecl traverseComplexTypeDecl(Element complexTypeDecl,
            String complexTypeName,
            Object[] attrValues,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        fComplexTypeDecl = new XSComplexTypeDecl();
        fAttrGrp = new XSAttributeGroupDecl();
        Boolean abstractAtt  = (Boolean) attrValues[XSAttributeChecker.ATTIDX_ABSTRACT];
        XInt    blockAtt     = (XInt)    attrValues[XSAttributeChecker.ATTIDX_BLOCK];
        Boolean mixedAtt     = (Boolean) attrValues[XSAttributeChecker.ATTIDX_MIXED];
        XInt    finalAtt     = (XInt)    attrValues[XSAttributeChecker.ATTIDX_FINAL];

        fName = complexTypeName;
        fComplexTypeDecl.setName(fName);
        fTargetNamespace = schemaDoc.fTargetNamespace;

        fBlock = blockAtt == null ? schemaDoc.fBlockDefault : blockAtt.shortValue();
        fFinal = finalAtt == null ? schemaDoc.fFinalDefault : finalAtt.shortValue();
        //discard valid Block/Final 'Default' values that are invalid for Block/Final
        fBlock &= (XSConstants.DERIVATION_EXTENSION | XSConstants.DERIVATION_RESTRICTION);
        fFinal &= (XSConstants.DERIVATION_EXTENSION | XSConstants.DERIVATION_RESTRICTION);

        fIsAbstract = (abstractAtt != null && abstractAtt.booleanValue());
        fAnnotations = null;

        Element child = null;

        try {
            // ---------------------------------------------------------------
            // First, handle any ANNOTATION declaration and get next child
            // ---------------------------------------------------------------
            child = DOMUtil.getFirstChildElement(complexTypeDecl);
            if(child != null) {
                if (DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    addAnnotation(traverseAnnotationDecl(child, attrValues, false, schemaDoc));
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(complexTypeDecl);
                    if (text != null) {
                        addAnnotation(traverseSyntheticAnnotation(complexTypeDecl, text, attrValues, false, schemaDoc));
                    }
                }
                if (child !=null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                            new Object[]{fName,SchemaSymbols.ELT_ANNOTATION},
                            child);
                }
            }
            else {
                String text = DOMUtil.getSyntheticAnnotation(complexTypeDecl);
                if (text != null) {
                    addAnnotation(traverseSyntheticAnnotation(complexTypeDecl, text, attrValues, false, schemaDoc));
                }
            }
            // ---------------------------------------------------------------
            // Process the content of the complex type definition
            // ---------------------------------------------------------------
            if (child==null) {
                //
                // EMPTY complexType with complexContent
                //

                // set the base to the anyType
                fBaseType = SchemaGrammar.fAnyType;
                fDerivedBy = XSConstants.DERIVATION_RESTRICTION;
                processComplexContent(child, mixedAtt.booleanValue(), false,
                        schemaDoc, grammar);
            }
            else if (DOMUtil.getLocalName(child).equals
                    (SchemaSymbols.ELT_SIMPLECONTENT)) {
                //
                // SIMPLE CONTENT
                //
                traverseSimpleContent(child, schemaDoc, grammar);
                Element elemTmp = DOMUtil.getNextSiblingElement(child);
                if (elemTmp != null) {
                    String siblingName = DOMUtil.getLocalName(elemTmp);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                            new Object[]{fName,siblingName},
                            elemTmp);
                }
            }
            else if (DOMUtil.getLocalName(child).equals
                    (SchemaSymbols.ELT_COMPLEXCONTENT)) {
                traverseComplexContent(child, mixedAtt.booleanValue(),
                        schemaDoc, grammar);
                Element elemTmp = DOMUtil.getNextSiblingElement(child);
                if (elemTmp != null) {
                    String siblingName = DOMUtil.getLocalName(elemTmp);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                            new Object[]{fName,siblingName},
                            elemTmp);
                }
            }
            else {
                //
                // We must have ....
                // GROUP, ALL, SEQUENCE or CHOICE, followed by optional attributes
                // Note that it's possible that only attributes are specified.
                //

                // set the base to the anyType
                fBaseType = SchemaGrammar.fAnyType;
                fDerivedBy = XSConstants.DERIVATION_RESTRICTION;
                processComplexContent(child, mixedAtt.booleanValue(), false,
                        schemaDoc, grammar);
            }

        }
        catch (ComplexTypeRecoverableError e) {
            handleComplexTypeError(e.getMessage(), e.errorSubstText,
                    e.errorElem);
        }

        if (DEBUG) {
            System.out.println(fName);
        }
        fComplexTypeDecl.setValues(fName, fTargetNamespace, fBaseType,
                fDerivedBy, fFinal, fBlock, fContentType, fIsAbstract,
                fAttrGrp, fXSSimpleType, fParticle, new XSObjectListImpl(fAnnotations,
                        fAnnotations == null? 0 : fAnnotations.length));
        return fComplexTypeDecl;
    }


    private void traverseSimpleContent(Element simpleContentElement,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar)
    throws ComplexTypeRecoverableError {


        Object[] simpleContentAttrValues = fAttrChecker.checkAttributes(simpleContentElement, false,
                schemaDoc);

        // -----------------------------------------------------------------------
        // Set content type
        // -----------------------------------------------------------------------
        fContentType = XSComplexTypeDecl.CONTENTTYPE_SIMPLE;
        fParticle = null;

        Element simpleContent = DOMUtil.getFirstChildElement(simpleContentElement);
        if (simpleContent != null && DOMUtil.getLocalName(simpleContent).equals(SchemaSymbols.ELT_ANNOTATION)) {
            addAnnotation(traverseAnnotationDecl(simpleContent, simpleContentAttrValues, false, schemaDoc));
            simpleContent = DOMUtil.getNextSiblingElement(simpleContent);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(simpleContentElement);
            if (text != null) {
                addAnnotation(traverseSyntheticAnnotation(simpleContentElement, text, simpleContentAttrValues, false, schemaDoc));
            }
        }

        // If there are no children, return
        if (simpleContent==null) {
            fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.2",
                    new Object[]{fName,SchemaSymbols.ELT_SIMPLECONTENT},
                    simpleContentElement);
        }

        // -----------------------------------------------------------------------
        // The content should be either "restriction" or "extension"
        // -----------------------------------------------------------------------
        String simpleContentName = DOMUtil.getLocalName(simpleContent);
        if (simpleContentName.equals(SchemaSymbols.ELT_RESTRICTION))
            fDerivedBy = XSConstants.DERIVATION_RESTRICTION;
        else if (simpleContentName.equals(SchemaSymbols.ELT_EXTENSION))
            fDerivedBy = XSConstants.DERIVATION_EXTENSION;
        else {
            fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                    new Object[]{fName,simpleContentName},
                    simpleContent);
        }
        Element elemTmp = DOMUtil.getNextSiblingElement(simpleContent);
        if (elemTmp != null) {
            fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            String siblingName = DOMUtil.getLocalName(elemTmp);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                    new Object[]{fName,siblingName},
                    elemTmp);
        }

        Object [] derivationTypeAttrValues = fAttrChecker.checkAttributes(simpleContent, false,
                schemaDoc);
        QName baseTypeName = (QName)  derivationTypeAttrValues[XSAttributeChecker.ATTIDX_BASE];


        // -----------------------------------------------------------------------
        // Need a base type.
        // -----------------------------------------------------------------------
        if (baseTypeName==null) {
            fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-att-must-appear",
                    new Object[]{simpleContentName, "base"}, simpleContent);
        }

        XSTypeDefinition type = (XSTypeDefinition)fSchemaHandler.getGlobalDecl(schemaDoc,
                XSDHandler.TYPEDECL_TYPE, baseTypeName,
                simpleContent);
        if (type==null) {
            fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError();
        }

        fBaseType = type;

        XSSimpleType baseValidator = null;
        XSComplexTypeDecl baseComplexType = null;
        int baseFinalSet = 0;

        // If the base type is complex, it must have simpleContent
        if ((type.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)) {

            baseComplexType = (XSComplexTypeDecl)type;
            baseFinalSet = baseComplexType.getFinal();
            // base is a CT with simple content (both restriction and extension are OK)
            if (baseComplexType.getContentType() == XSComplexTypeDecl.CONTENTTYPE_SIMPLE) {
                baseValidator = (XSSimpleType)baseComplexType.getSimpleType();
            }
            // base is a CT with mixed/emptiable content (only restriction is OK)
            else if (fDerivedBy == XSConstants.DERIVATION_RESTRICTION &&
                    baseComplexType.getContentType() == XSComplexTypeDecl.CONTENTTYPE_MIXED &&
                    ((XSParticleDecl)baseComplexType.getParticle()).emptiable()) {
            }
            else {
                fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("src-ct.2.1",
                        new Object[]{fName, baseComplexType.getName()}, simpleContent);
            }
        }
        else {
            baseValidator = (XSSimpleType)type;
            // base is a ST (only extension is OK)
            if (fDerivedBy == XSConstants.DERIVATION_RESTRICTION) {
                fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("src-ct.2.1",
                        new Object[]{fName, baseValidator.getName()}, simpleContent);
            }
            baseFinalSet=baseValidator.getFinal();
        }

        // -----------------------------------------------------------------------
        // Check that the base permits the derivation
        // -----------------------------------------------------------------------
        if ((baseFinalSet & fDerivedBy)!=0) {
            fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            String errorKey = (fDerivedBy==XSConstants.DERIVATION_EXTENSION) ?
                    "cos-ct-extends.1.1" : "derivation-ok-restriction.1";
            throw new ComplexTypeRecoverableError(errorKey,
                    new Object[]{fName, fBaseType.getName()}, simpleContent);
        }

        // -----------------------------------------------------------------------
        // Skip over any potential annotations
        // -----------------------------------------------------------------------
        Element scElement = simpleContent;
        simpleContent = DOMUtil.getFirstChildElement(simpleContent);
        if (simpleContent != null) {
            // traverse annotation if any

            if (DOMUtil.getLocalName(simpleContent).equals(SchemaSymbols.ELT_ANNOTATION)) {
                addAnnotation(traverseAnnotationDecl(simpleContent, derivationTypeAttrValues, false, schemaDoc));
                simpleContent = DOMUtil.getNextSiblingElement(simpleContent);
            }
            else {
                String text = DOMUtil.getSyntheticAnnotation(scElement);
                if (text != null) {
                    addAnnotation(traverseSyntheticAnnotation(scElement, text, derivationTypeAttrValues, false, schemaDoc));
                }
            }

            if (simpleContent !=null &&
                    DOMUtil.getLocalName(simpleContent).equals(SchemaSymbols.ELT_ANNOTATION)){
                fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                        new Object[]{fName,SchemaSymbols.ELT_ANNOTATION},
                        simpleContent);
            }
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(scElement);
            if (text != null) {
                addAnnotation(traverseSyntheticAnnotation(scElement, text, derivationTypeAttrValues, false, schemaDoc));
            }
        }

        // -----------------------------------------------------------------------
        // Process a RESTRICTION
        // -----------------------------------------------------------------------
        if (fDerivedBy == XSConstants.DERIVATION_RESTRICTION) {

            // -----------------------------------------------------------------------
            // There may be a simple type definition in the restriction element
            // The data type validator will be based on it, if specified
            // -----------------------------------------------------------------------
            if (simpleContent !=null &&
                    DOMUtil.getLocalName(simpleContent).equals(SchemaSymbols.ELT_SIMPLETYPE )) {

                XSSimpleType dv = fSchemaHandler.fSimpleTypeTraverser.traverseLocal(
                        simpleContent, schemaDoc, grammar);
                if (dv == null) {
                    fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError();
                }
                //check that this datatype validator is validly derived from the base
                //according to derivation-ok-restriction 5.1.2.1

                if (baseValidator != null &&
                        !XSConstraints.checkSimpleDerivationOk(dv, baseValidator,
                                baseValidator.getFinal())) {
                    fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("derivation-ok-restriction.5.2.2.1",
                            new Object[]{fName, dv.getName(), baseValidator.getName()},
                            simpleContent);
                }
                baseValidator = dv;
                simpleContent = DOMUtil.getNextSiblingElement(simpleContent);
            }

            // this only happens when restricting a mixed/emptiable CT
            // but there is no <simpleType>, which is required
            if (baseValidator == null) {
                fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("src-ct.2.2",
                        new Object[]{fName}, simpleContent);
            }

            // -----------------------------------------------------------------------
            // Traverse any facets
            // -----------------------------------------------------------------------
            Element attrNode = null;
            XSFacets facetData = null;
            short presentFacets = 0 ;
            short fixedFacets = 0 ;

            if (simpleContent!=null) {
                FacetInfo fi = traverseFacets(simpleContent, fComplexTypeDecl, baseValidator, schemaDoc);
                attrNode = fi.nodeAfterFacets;
                facetData = fi.facetdata;
                presentFacets = fi.fPresentFacets;
                fixedFacets = fi.fFixedFacets;
            }

            String name = genAnonTypeName(simpleContentElement);
            fXSSimpleType = fSchemaHandler.fDVFactory.createTypeRestriction(name,schemaDoc.fTargetNamespace,(short)0,baseValidator,null);
            try{
                fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                fXSSimpleType.applyFacets(facetData, presentFacets, fixedFacets, fValidationState);
            }catch(InvalidDatatypeFacetException ex){
                reportSchemaError(ex.getKey(), ex.getArgs(), simpleContent);
                // Recreate the type, ignoring the facets
                fXSSimpleType = fSchemaHandler.fDVFactory.createTypeRestriction(name,schemaDoc.fTargetNamespace,(short)0,baseValidator,null);
            }
            if (fXSSimpleType instanceof XSSimpleTypeDecl) {
                ((XSSimpleTypeDecl)fXSSimpleType).setAnonymous(true);
            }

            // -----------------------------------------------------------------------
            // Traverse any attributes
            // -----------------------------------------------------------------------
            if (attrNode != null) {
                if (!isAttrOrAttrGroup(attrNode)) {
                    fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                            new Object[]{fName,DOMUtil.getLocalName(attrNode)},
                            attrNode);
                }
                Element node=traverseAttrsAndAttrGrps(attrNode,fAttrGrp,
                        schemaDoc,grammar,fComplexTypeDecl);
                if (node!=null) {
                    fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                            new Object[]{fName,DOMUtil.getLocalName(node)},
                            node);
                }
            }

            try {
                mergeAttributes(baseComplexType.getAttrGrp(), fAttrGrp, fName, false, simpleContentElement);
            } catch (ComplexTypeRecoverableError e) {
                fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw e;
            }
            // Prohibited uses must be removed after merge for RESTRICTION
            fAttrGrp.removeProhibitedAttrs();

            Object[] errArgs=fAttrGrp.validRestrictionOf(fName, baseComplexType.getAttrGrp());
            if (errArgs != null) {
                fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError((String)errArgs[errArgs.length-1],
                        errArgs, attrNode);
            }

        }
        // -----------------------------------------------------------------------
        // Process a EXTENSION
        // -----------------------------------------------------------------------
        else {
            fXSSimpleType = baseValidator;
            if (simpleContent != null) {
                // -----------------------------------------------------------------------
                // Traverse any attributes
                // -----------------------------------------------------------------------
                Element attrNode = simpleContent;
                if (!isAttrOrAttrGroup(attrNode)) {
                    fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                            new Object[]{fName,DOMUtil.getLocalName(attrNode)},
                            attrNode);
                }
                Element node=traverseAttrsAndAttrGrps(attrNode,fAttrGrp,
                        schemaDoc,grammar,fComplexTypeDecl);

                if (node!=null) {
                    fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                            new Object[]{fName,DOMUtil.getLocalName(node)},
                            node);
                }
                // Remove prohibited uses.   Should be done prior to any merge.
                fAttrGrp.removeProhibitedAttrs();
            }

            if (baseComplexType != null) {
                try {
                    mergeAttributes(baseComplexType.getAttrGrp(), fAttrGrp, fName, true, simpleContentElement);
                } catch (ComplexTypeRecoverableError e) {
                    fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw e;
                }
            }
        }
        // and finally, since we've nothing more to traverse, we can
        // return the attributes (and thereby reset the namespace support)
        fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
        fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
    }

    private void traverseComplexContent(Element complexContentElement,
            boolean mixedOnType, XSDocumentInfo schemaDoc,
            SchemaGrammar grammar)
    throws ComplexTypeRecoverableError {


        Object[] complexContentAttrValues = fAttrChecker.checkAttributes(complexContentElement, false,
                schemaDoc);


        // -----------------------------------------------------------------------
        // Determine if this is mixed content
        // -----------------------------------------------------------------------
        boolean mixedContent = mixedOnType;
        Boolean mixedAtt     = (Boolean) complexContentAttrValues[XSAttributeChecker.ATTIDX_MIXED];
        if (mixedAtt != null) {
            mixedContent = mixedAtt.booleanValue();
        }


        // -----------------------------------------------------------------------
        // Since the type must have complex content, set the simple type validators
        // to null
        // -----------------------------------------------------------------------
        fXSSimpleType = null;

        Element complexContent = DOMUtil.getFirstChildElement(complexContentElement);
        if (complexContent != null && DOMUtil.getLocalName(complexContent).equals(SchemaSymbols.ELT_ANNOTATION)) {
            addAnnotation(traverseAnnotationDecl(complexContent, complexContentAttrValues, false, schemaDoc));
            complexContent = DOMUtil.getNextSiblingElement(complexContent);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(complexContentElement);
            if (text != null) {
                addAnnotation(traverseSyntheticAnnotation(complexContentElement, text, complexContentAttrValues, false, schemaDoc));
            }
        }

        // If there are no children, return
        if (complexContent==null) {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.2",
                    new Object[]{fName,SchemaSymbols.ELT_COMPLEXCONTENT},
                    complexContentElement);
        }

        // -----------------------------------------------------------------------
        // The content should be either "restriction" or "extension"
        // -----------------------------------------------------------------------
        String complexContentName = DOMUtil.getLocalName(complexContent);
        if (complexContentName.equals(SchemaSymbols.ELT_RESTRICTION))
            fDerivedBy = XSConstants.DERIVATION_RESTRICTION;
        else if (complexContentName.equals(SchemaSymbols.ELT_EXTENSION))
            fDerivedBy = XSConstants.DERIVATION_EXTENSION;
        else {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                    new Object[]{fName, complexContentName}, complexContent);
        }
        Element elemTmp = DOMUtil.getNextSiblingElement(complexContent);
        if (elemTmp != null) {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            String siblingName = DOMUtil.getLocalName(elemTmp);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                    new Object[]{fName, siblingName}, elemTmp);
        }

        Object[] derivationTypeAttrValues = fAttrChecker.checkAttributes(complexContent, false,
                schemaDoc);
        QName baseTypeName = (QName)  derivationTypeAttrValues[XSAttributeChecker.ATTIDX_BASE];


        // -----------------------------------------------------------------------
        // Need a base type.  Check that it's a complex type
        // -----------------------------------------------------------------------
        if (baseTypeName==null) {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-att-must-appear",
                    new Object[]{complexContentName, "base"}, complexContent);
        }

        XSTypeDefinition type = (XSTypeDefinition)fSchemaHandler.getGlobalDecl(schemaDoc,
                XSDHandler.TYPEDECL_TYPE,
                baseTypeName,
                complexContent);

        if (type==null) {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError();
        }

        if (! (type instanceof XSComplexTypeDecl)) {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("src-ct.1",
                    new Object[]{fName, type.getName()}, complexContent);
        }
        XSComplexTypeDecl baseType = (XSComplexTypeDecl)type;
        fBaseType = baseType;

        // -----------------------------------------------------------------------
        // Check that the base permits the derivation
        // -----------------------------------------------------------------------
        if ((baseType.getFinal() & fDerivedBy)!=0) {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            String errorKey = (fDerivedBy==XSConstants.DERIVATION_EXTENSION) ?
                    "cos-ct-extends.1.1" : "derivation-ok-restriction.1";
            throw new ComplexTypeRecoverableError(errorKey,
                    new Object[]{fName, fBaseType.getName()}, complexContent);
        }

        // -----------------------------------------------------------------------
        // Skip over any potential annotations
        // -----------------------------------------------------------------------
        complexContent = DOMUtil.getFirstChildElement(complexContent);

        if (complexContent != null) {
            // traverse annotation if any
            if (DOMUtil.getLocalName(complexContent).equals(SchemaSymbols.ELT_ANNOTATION)) {
                addAnnotation(traverseAnnotationDecl(complexContent, derivationTypeAttrValues, false, schemaDoc));
                complexContent = DOMUtil.getNextSiblingElement(complexContent);
            }
            else {
                String text = DOMUtil.getSyntheticAnnotation(complexContent);
                if (text != null) {
                    addAnnotation(traverseSyntheticAnnotation(complexContent, text, derivationTypeAttrValues, false, schemaDoc));
                }
            }
            if (complexContent !=null &&
                    DOMUtil.getLocalName(complexContent).equals(SchemaSymbols.ELT_ANNOTATION)){
                fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                        new Object[]{fName,SchemaSymbols.ELT_ANNOTATION}, complexContent);
            }
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(complexContent);
            if (text != null) {
                addAnnotation(traverseSyntheticAnnotation(complexContent, text, derivationTypeAttrValues, false, schemaDoc));
            }
        }
        // -----------------------------------------------------------------------
        // Process the content.  Note:  should I try to catch any complexType errors
        // here in order to return the attr array?
        // -----------------------------------------------------------------------
        try {
            processComplexContent(complexContent, mixedContent, true, schemaDoc,
                    grammar);
        } catch (ComplexTypeRecoverableError e) {
            fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            throw e;
        }

        // -----------------------------------------------------------------------
        // Compose the final content and attribute uses
        // -----------------------------------------------------------------------
        XSParticleDecl baseContent = (XSParticleDecl)baseType.getParticle();
        if (fDerivedBy==XSConstants.DERIVATION_RESTRICTION) {

            // This is an RESTRICTION

            // N.B. derivation-ok-restriction.5.3 is checked under schema
            // full checking.   That's because we need to wait until locals are
            // traversed so that occurrence information is correct.


            if (fContentType == XSComplexTypeDecl.CONTENTTYPE_MIXED &&
                    baseType.getContentType() != XSComplexTypeDecl.CONTENTTYPE_MIXED) {
                fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("derivation-ok-restriction.5.4.1.2",
                        new Object[]{fName, baseType.getName()},
                        complexContent);
            }

            try {
                mergeAttributes(baseType.getAttrGrp(), fAttrGrp, fName, false, complexContent);
            } catch (ComplexTypeRecoverableError e) {
                fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw e;
            }
            // Remove prohibited uses.   Must be done after merge for RESTRICTION.
            fAttrGrp.removeProhibitedAttrs();

            if (baseType != SchemaGrammar.fAnyType) {
                Object[] errArgs = fAttrGrp.validRestrictionOf(fName, baseType.getAttrGrp());
                if (errArgs != null) {
                    fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError((String)errArgs[errArgs.length-1],
                            errArgs, complexContent);
                }
            }
        }
        else {

            // This is an EXTENSION

            // Create the particle
            if (fParticle == null) {
                fContentType = baseType.getContentType();
                fXSSimpleType = (XSSimpleType)baseType.getSimpleType();
                fParticle = baseContent;
            }
            else if (baseType.getContentType() == XSComplexTypeDecl.CONTENTTYPE_EMPTY) {
            }
            else {
                //
                // Check if the contentType of the base is consistent with the new type
                // cos-ct-extends.1.4.3.2
                if (fContentType == XSComplexTypeDecl.CONTENTTYPE_ELEMENT &&
                        baseType.getContentType() != XSComplexTypeDecl.CONTENTTYPE_ELEMENT) {
                    fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("cos-ct-extends.1.4.3.2.2.1.a",
                            new Object[]{fName}, complexContent);
                }
                else if (fContentType == XSComplexTypeDecl.CONTENTTYPE_MIXED &&
                        baseType.getContentType() != XSComplexTypeDecl.CONTENTTYPE_MIXED) {
                    fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("cos-ct-extends.1.4.3.2.2.1.b",
                            new Object[]{fName}, complexContent);
                }

                // if the content of either type is an "all" model group, error.
                if (fParticle.fType == XSParticleDecl.PARTICLE_MODELGROUP &&
                        ((XSModelGroupImpl)fParticle.fValue).fCompositor == XSModelGroupImpl.MODELGROUP_ALL ||
                        ((XSParticleDecl)baseType.getParticle()).fType == XSParticleDecl.PARTICLE_MODELGROUP &&
                        ((XSModelGroupImpl)(((XSParticleDecl)baseType.getParticle())).fValue).fCompositor == XSModelGroupImpl.MODELGROUP_ALL) {
                    fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                    fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("cos-all-limited.1.2",
                            new Object[]{}, complexContent);
                }
                // the "sequence" model group to contain both particles
                XSModelGroupImpl group = new XSModelGroupImpl();
                group.fCompositor = XSModelGroupImpl.MODELGROUP_SEQUENCE;
                group.fParticleCount = 2;
                group.fParticles = new XSParticleDecl[2];
                group.fParticles[0] = (XSParticleDecl)baseType.getParticle();
                group.fParticles[1] = fParticle;
                group.fAnnotations = XSObjectListImpl.EMPTY_LIST;
                // the particle to contain the above sequence
                XSParticleDecl particle = new XSParticleDecl();
                particle.fType = XSParticleDecl.PARTICLE_MODELGROUP;
                particle.fValue = group;
                particle.fAnnotations = XSObjectListImpl.EMPTY_LIST;

                fParticle = particle;
            }

            // Remove prohibited uses.   Must be done before merge for EXTENSION.
            fAttrGrp.removeProhibitedAttrs();
            try {
                mergeAttributes(baseType.getAttrGrp(), fAttrGrp, fName, true, complexContent);
            } catch (ComplexTypeRecoverableError e) {
                fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw e;
            }

        }

        // and *finally* we can legitimately return the attributes!
        fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
        fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);

    } // end of traverseComplexContent


    // This method merges attribute uses from the base, into the derived set.
    // LM: may want to merge with attributeGroup processing.
    private void mergeAttributes(XSAttributeGroupDecl fromAttrGrp,
            XSAttributeGroupDecl toAttrGrp,
            String typeName,
            boolean extension,
            Element elem)
    throws ComplexTypeRecoverableError {

        XSObjectList attrUseS = fromAttrGrp.getAttributeUses();
        XSAttributeUseImpl oneAttrUse = null;
        int attrCount = attrUseS.getLength();
        for (int i=0; i<attrCount; i++) {
            oneAttrUse = (XSAttributeUseImpl)attrUseS.item(i);
            XSAttributeUse existingAttrUse = toAttrGrp.getAttributeUse(oneAttrUse.fAttrDecl.getNamespace(),
                    oneAttrUse.fAttrDecl.getName());
            if (existingAttrUse == null) {

                String idName = toAttrGrp.addAttributeUse(oneAttrUse);
                if (idName != null) {
                    throw new ComplexTypeRecoverableError("ct-props-correct.5",
                            new Object[]{typeName, idName, oneAttrUse.fAttrDecl.getName()},
                            elem);
                }
            }
            else if (existingAttrUse != oneAttrUse) {
                if (extension) {
                    reportSchemaError("ct-props-correct.4",
                            new Object[]{typeName, oneAttrUse.fAttrDecl.getName()},
                            elem);
                    // Recover by using the attribute use from the base type,
                    // to make the resulting schema "more valid".
                    toAttrGrp.replaceAttributeUse(existingAttrUse, oneAttrUse);
                }
            }
        }
        // For extension, the wildcard must be formed by doing a union of the wildcards
        if (extension) {
            if (toAttrGrp.fAttributeWC==null) {
                toAttrGrp.fAttributeWC = fromAttrGrp.fAttributeWC;
            }
            else if (fromAttrGrp.fAttributeWC != null) {
                toAttrGrp.fAttributeWC = toAttrGrp.fAttributeWC.performUnionWith(fromAttrGrp.fAttributeWC, toAttrGrp.fAttributeWC.fProcessContents);
                if (toAttrGrp.fAttributeWC == null) {
                    // REVISIT: XML Schema 1.0 2nd edition doesn't actually specify this constraint. It's a bug in the spec
                    // which will eventually be fixed. We're just guessing what the error code will be. If it turns out to be
                    // something else we'll need to change it. -- mrglavas
                    throw new ComplexTypeRecoverableError("src-ct.5", new Object[]{typeName}, elem);
                }
            }

        }
    }

    private void processComplexContent(Element complexContentChild,
            boolean isMixed, boolean isDerivation,
            XSDocumentInfo schemaDoc, SchemaGrammar grammar)
    throws ComplexTypeRecoverableError {

        Element attrNode = null;
        XSParticleDecl particle = null;

        // whether there is a particle with empty model group
        boolean emptyParticle = false;
        if (complexContentChild != null) {
            // -------------------------------------------------------------
            // GROUP, ALL, SEQUENCE or CHOICE, followed by attributes, if specified.
            // Note that it's possible that only attributes are specified.
            // -------------------------------------------------------------


            String childName = DOMUtil.getLocalName(complexContentChild);

            if (childName.equals(SchemaSymbols.ELT_GROUP)) {

                particle = fSchemaHandler.fGroupTraverser.traverseLocal(complexContentChild,
                        schemaDoc, grammar);
                attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
            }
            else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
                particle = traverseSequence(complexContentChild,schemaDoc,grammar,
                        NOT_ALL_CONTEXT,fComplexTypeDecl);
                if (particle != null) {
                    XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
                    if (group.fParticleCount == 0)
                        emptyParticle = true;
                }
                attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
            }
            else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
                particle = traverseChoice(complexContentChild,schemaDoc,grammar,
                        NOT_ALL_CONTEXT,fComplexTypeDecl);
                if (particle != null && particle.fMinOccurs == 0) {
                    XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
                    if (group.fParticleCount == 0)
                        emptyParticle = true;
                }
                attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
            }
            else if (childName.equals(SchemaSymbols.ELT_ALL)) {
                particle = traverseAll(complexContentChild,schemaDoc,grammar,
                        PROCESSING_ALL_GP,fComplexTypeDecl);
                if (particle != null) {
                    XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
                    if (group.fParticleCount == 0)
                        emptyParticle = true;
                }
                attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
            }
            else {
                // Should be attributes here - will check below...
                attrNode = complexContentChild;
            }
        }

        // if the particle is empty because there is no non-annotation chidren,
        // we need to make the particle itself null (so that the effective
        // content is empty).
        if (emptyParticle) {
            // get the first child
            Element child = DOMUtil.getFirstChildElement(complexContentChild);
            // if it's annotation, get the next one
            if (child != null) {
                if (DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    child = DOMUtil.getNextSiblingElement(child);
                }
            }
            // if there is no (non-annotation) children, mark particle empty
            if (child == null)
                particle = null;
            // child != null means we might have seen an element with
            // minOccurs == maxOccurs == 0
        }

        if (particle == null && isMixed) {
            particle = XSConstraints.getEmptySequence();
        }
        fParticle = particle;

        // -----------------------------------------------------------------------
        // Set the content type
        // -----------------------------------------------------------------------
        if (fParticle == null)
            fContentType = XSComplexTypeDecl.CONTENTTYPE_EMPTY;
        else if (isMixed)
            fContentType = XSComplexTypeDecl.CONTENTTYPE_MIXED;
        else
            fContentType = XSComplexTypeDecl.CONTENTTYPE_ELEMENT;


        // -------------------------------------------------------------
        // Now, process attributes
        // -------------------------------------------------------------
        if (attrNode != null) {
            if (!isAttrOrAttrGroup(attrNode)) {
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                        new Object[]{fName,DOMUtil.getLocalName(attrNode)},
                        attrNode);
            }
            Element node =
                traverseAttrsAndAttrGrps(attrNode,fAttrGrp,schemaDoc,grammar,fComplexTypeDecl);
            if (node!=null) {
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1",
                        new Object[]{fName,DOMUtil.getLocalName(node)},
                        node);
            }
            // Only remove prohibited attribute uses if this isn't a derived type
            // Derivation-specific code worries about this elsewhere
            if (!isDerivation) {
                fAttrGrp.removeProhibitedAttrs();
            }
        }



    } // end processComplexContent


    private boolean isAttrOrAttrGroup(Element e) {
        String elementName = DOMUtil.getLocalName(e);

        if (elementName.equals(SchemaSymbols.ELT_ATTRIBUTE) ||
                elementName.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP) ||
                elementName.equals(SchemaSymbols.ELT_ANYATTRIBUTE))
            return true;
        else
            return false;
    }

    private void traverseSimpleContentDecl(Element simpleContentDecl) {
    }

    private void traverseComplexContentDecl(Element complexContentDecl,
            boolean mixedOnComplexTypeDecl) {
    }

    /*
     * Generate a name for an anonymous type
     */
    private String genAnonTypeName(Element complexTypeDecl) {

        // Generate a unique name for the anonymous type by concatenating together the
        // names of parent nodes
        // The name is quite good for debugging/error purposes, but we may want to
        // revisit how this is done for performance reasons (LM).
        StringBuffer typeName = new StringBuffer("#AnonType_");
        Element node = DOMUtil.getParent(complexTypeDecl);
        while (node != null && (node != DOMUtil.getRoot(DOMUtil.getDocument(node)))) {
            typeName.append(node.getAttribute(SchemaSymbols.ATT_NAME));
            node = DOMUtil.getParent(node);
        }
        return typeName.toString();
    }


    private void handleComplexTypeError(String messageId,Object[] args,
            Element e) {

        if (messageId!=null) {
            reportSchemaError(messageId, args, e);
        }

        //
        //  Mock up the typeInfo structure so that there won't be problems during
        //  validation
        //
        fBaseType = SchemaGrammar.fAnyType;
        fContentType = XSComplexTypeDecl.CONTENTTYPE_MIXED;
        fXSSimpleType = null;
        fParticle = getErrorContent();
        // REVISIT: do we need to remove all attribute uses already added into
        // the attribute group? maybe it's ok to leave them there. -SG
        fAttrGrp.fAttributeWC = getErrorWildcard();
    }

    private void contentBackup() {
        if(fGlobalStore == null) {
            fGlobalStore = new Object [GLOBAL_NUM];
            fGlobalStorePos = 0;
        }
        if(fGlobalStorePos == fGlobalStore.length) {
            Object [] newArray = new Object[fGlobalStorePos+GLOBAL_NUM];
            System.arraycopy(fGlobalStore, 0, newArray, 0, fGlobalStorePos);
            fGlobalStore = newArray;
        }
        fGlobalStore[fGlobalStorePos++] = fComplexTypeDecl;
        fGlobalStore[fGlobalStorePos++] = fIsAbstract?Boolean.TRUE:Boolean.FALSE;
        fGlobalStore[fGlobalStorePos++] = fName ;
        fGlobalStore[fGlobalStorePos++] = fTargetNamespace;
        // let's save ourselves a couple of objects...
        fGlobalStore[fGlobalStorePos++] = (fDerivedBy << 16) + fFinal;
        fGlobalStore[fGlobalStorePos++] = (fBlock << 16) + fContentType;
        fGlobalStore[fGlobalStorePos++] = fBaseType;
        fGlobalStore[fGlobalStorePos++] = fAttrGrp;
        fGlobalStore[fGlobalStorePos++] = fParticle;
        fGlobalStore[fGlobalStorePos++] = fXSSimpleType;
        fGlobalStore[fGlobalStorePos++] = fAnnotations;
    }

    private void contentRestore() {
        fAnnotations = (XSAnnotationImpl [])fGlobalStore[--fGlobalStorePos];
        fXSSimpleType = (XSSimpleType)fGlobalStore[--fGlobalStorePos];
        fParticle = (XSParticleDecl)fGlobalStore[--fGlobalStorePos];
        fAttrGrp = (XSAttributeGroupDecl)fGlobalStore[--fGlobalStorePos];
        fBaseType = (XSTypeDefinition)fGlobalStore[--fGlobalStorePos];
        int i = ((Integer)(fGlobalStore[--fGlobalStorePos]));
        fBlock = (short)(i >> 16);
        fContentType = (short)i;
        i = ((Integer)(fGlobalStore[--fGlobalStorePos]));
        fDerivedBy = (short)(i >> 16);
        fFinal = (short)i;
        fTargetNamespace = (String)fGlobalStore[--fGlobalStorePos];
        fName = (String)fGlobalStore[--fGlobalStorePos];
        fIsAbstract = ((Boolean)fGlobalStore[--fGlobalStorePos]);
        fComplexTypeDecl = (XSComplexTypeDecl)fGlobalStore[--fGlobalStorePos];
    }

    private void addAnnotation(XSAnnotationImpl annotation) {
        if(annotation == null)
            return;
        // it isn't very likely that there will be more than one annotation
        // in a complexType decl.  This saves us fromhaving to push/pop
        // one more object from the fGlobalStore, and that's bound
        // to be a savings for most applications
        if(fAnnotations == null) {
            fAnnotations = new XSAnnotationImpl[1];
        } else {
            XSAnnotationImpl [] tempArray = new XSAnnotationImpl[fAnnotations.length + 1];
            System.arraycopy(fAnnotations, 0, tempArray, 0, fAnnotations.length);
            fAnnotations = tempArray;
        }
        fAnnotations[fAnnotations.length-1] = annotation;
    }
}
