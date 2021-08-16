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

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeFacetException;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 * The simple type definition schema component traverser.
 *
 * <simpleType
 *   final = (#all | (list | union | restriction))
 *   id = ID
 *   name = NCName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (restriction | list | union))
 * </simpleType>
 *
 * <restriction
 *   base = QName
 *   id = ID
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleType?, (minExclusive | minInclusive | maxExclusive | maxInclusive | totalDigits | fractionDigits | length | minLength | maxLength | enumeration | whiteSpace | pattern)*))
 * </restriction>
 *
 * <list
 *   id = ID
 *   itemType = QName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleType?))
 * </list>
 *
 * <union
 *   id = ID
 *   memberTypes = List of QName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, (simpleType*))
 * </union>
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 * @author Neeraj Bajaj, Sun Microsystems, Inc.
 * @author Sandy Gao, IBM
 *
 * @LastModified: Nov 2017
 */
class XSDSimpleTypeTraverser extends XSDAbstractTraverser {

    // whether the type being parsed is a S4S built-in type.
    private boolean fIsBuiltIn = false;

    XSDSimpleTypeTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    //return qualified name of simpleType or empty string if error occured
    XSSimpleType traverseGlobal(Element elmNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(elmNode, true, schemaDoc);
        String nameAtt = (String)attrValues[XSAttributeChecker.ATTIDX_NAME];
        if (nameAtt == null) {
            attrValues[XSAttributeChecker.ATTIDX_NAME] = NO_NAME;
        }
        XSSimpleType type = traverseSimpleTypeDecl(elmNode, attrValues, schemaDoc, grammar);
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        // if it's a global type without a name, return null
        if (nameAtt == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, SchemaSymbols.ATT_NAME}, elmNode);
            type = null;
        }

        // don't add global components without name to the grammar
        if (type != null) {
            if (grammar.getGlobalTypeDecl(type.getName()) == null) {
                grammar.addGlobalSimpleTypeDecl(type);
            }

            // also add it to extended map
            final String loc = fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            final XSTypeDefinition type2 = grammar.getGlobalTypeDecl(type.getName(), loc);
            if (type2 == null) {
                grammar.addGlobalSimpleTypeDecl(type, loc);
            }

            // handle duplicates
            if (fSchemaHandler.fTolerateDuplicates) {
                if (type2 != null) {
                    if (type2 instanceof XSSimpleType) {
                        type = (XSSimpleType) type2;
                    }
                }
                fSchemaHandler.addGlobalTypeDecl(type);
            }
        }

        return type;
    }

    XSSimpleType traverseLocal(Element elmNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(elmNode, false, schemaDoc);
        String name = genAnonTypeName(elmNode);
        XSSimpleType type = getSimpleType (name, elmNode, attrValues, schemaDoc, grammar);
        if (type instanceof XSSimpleTypeDecl) {
            ((XSSimpleTypeDecl)type).setAnonymous(true);
        }
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        return type;
    }

    private XSSimpleType traverseSimpleTypeDecl(Element simpleTypeDecl,
            Object[] attrValues,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        // get name and final values
        String name = (String)attrValues[XSAttributeChecker.ATTIDX_NAME];
        return getSimpleType(name, simpleTypeDecl, attrValues, schemaDoc, grammar);
    }

    /*
     * Generate a name for an anonymous type
     */
    private String genAnonTypeName(Element simpleTypeDecl) {

        // Generate a unique name for the anonymous type by concatenating together the
        // names of parent nodes
        // The name is quite good for debugging/error purposes, but we may want to
        // revisit how this is done for performance reasons (LM).
        StringBuffer typeName = new StringBuffer("#AnonType_");
        Element node = DOMUtil.getParent(simpleTypeDecl);
        while (node != null && (node != DOMUtil.getRoot(DOMUtil.getDocument(node)))) {
            typeName.append(node.getAttribute(SchemaSymbols.ATT_NAME));
            node = DOMUtil.getParent(node);
        }
        return typeName.toString();
    }

    /**
     * @param name
     * @param simpleTypeDecl
     * @param attrValues
     * @param schemaDoc
     * @param grammar
     * @return
     */
    private XSSimpleType getSimpleType(String name, Element simpleTypeDecl, Object[] attrValues, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        XInt finalAttr = (XInt)attrValues[XSAttributeChecker.ATTIDX_FINAL];
        int finalProperty = finalAttr == null ? schemaDoc.fFinalDefault : finalAttr.intValue();
        // annotation?,(list|restriction|union)
        Element child = DOMUtil.getFirstChildElement(simpleTypeDecl);
        XSAnnotationImpl [] annotations = null;
        if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            XSAnnotationImpl annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            if (annotation != null)
                annotations = new XSAnnotationImpl [] {annotation};
            child = DOMUtil.getNextSiblingElement(child);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(simpleTypeDecl);
            if (text != null) {
                XSAnnotationImpl annotation = traverseSyntheticAnnotation(simpleTypeDecl, text, attrValues, false, schemaDoc);
                annotations = new XSAnnotationImpl[] {annotation};
            }
        }
        // (list|restriction|union)
        if (child == null) {
            reportSchemaError("s4s-elt-must-match.2", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))"}, simpleTypeDecl);
            return errorType(name, schemaDoc.fTargetNamespace, XSConstants.DERIVATION_RESTRICTION);
        }
        // derivation type: restriction/list/union
        String varietyProperty = DOMUtil.getLocalName(child);
        short refType = XSConstants.DERIVATION_RESTRICTION;
        boolean restriction = false, list = false, union = false;
        if (varietyProperty.equals(SchemaSymbols.ELT_RESTRICTION)) {
            refType = XSConstants.DERIVATION_RESTRICTION;
            restriction = true;
        }
        else if (varietyProperty.equals(SchemaSymbols.ELT_LIST)) {
            refType = XSConstants.DERIVATION_LIST;
            list = true;
        }
        else if (varietyProperty.equals(SchemaSymbols.ELT_UNION)) {
            refType = XSConstants.DERIVATION_UNION;
            union = true;
        }
        else {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))", varietyProperty}, simpleTypeDecl);
            return errorType(name, schemaDoc.fTargetNamespace, XSConstants.DERIVATION_RESTRICTION);
        }
        // nothing should follow this element
        Element nextChild = DOMUtil.getNextSiblingElement(child);
        if (nextChild != null) {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))", DOMUtil.getLocalName(nextChild)}, nextChild);
        }
        // General Attribute Checking: get base/item/member types
        Object[] contentAttrs = fAttrChecker.checkAttributes(child, false, schemaDoc);
        QName baseTypeName = (QName)contentAttrs[restriction ?
                XSAttributeChecker.ATTIDX_BASE :
                    XSAttributeChecker.ATTIDX_ITEMTYPE];
        @SuppressWarnings("unchecked")
        List<QName> memberTypes = (ArrayList<QName>)contentAttrs[XSAttributeChecker.ATTIDX_MEMBERTYPES];
        //content = {annotation?,simpleType?...}
        Element content = DOMUtil.getFirstChildElement(child);
        //check content (annotation?, ...)
        if (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_ANNOTATION)) {
            XSAnnotationImpl annotation = traverseAnnotationDecl(content, contentAttrs, false, schemaDoc);
            if (annotation != null ) {
                if(annotations == null) {
                    annotations = new XSAnnotationImpl [] {annotation};
                }
                else {
                    XSAnnotationImpl [] tempArray = new XSAnnotationImpl[2];
                    tempArray[0] = annotations[0];
                    annotations = tempArray;
                    annotations[1] = annotation;
                }
            }
            content = DOMUtil.getNextSiblingElement(content);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(child);
            if (text != null) {
                XSAnnotationImpl annotation = traverseSyntheticAnnotation(child, text, contentAttrs, false, schemaDoc);
                if (annotations == null) {
                    annotations = new XSAnnotationImpl [] {annotation};
                }
                else {
                    XSAnnotationImpl [] tempArray = new XSAnnotationImpl[2];
                    tempArray[0] = annotations[0];
                    annotations = tempArray;
                    annotations[1] = annotation;
                }
            }
        }
        // get base type from "base" attribute
        XSSimpleType baseValidator = null;
        if ((restriction || list) && baseTypeName != null) {
            baseValidator = findDTValidator(child, name, baseTypeName, refType, schemaDoc);
            // if its the built-in type, return null from here
            if (baseValidator == null && fIsBuiltIn) {
                fIsBuiltIn = false;
                return null;
            }
        }
        // get types from "memberTypes" attribute
        List<XSObject> dTValidators = null;
        XSSimpleType dv = null;
        XSObjectList dvs;
        if (union && memberTypes != null && memberTypes.size() > 0) {
            int size = memberTypes.size();
            dTValidators = new ArrayList<>(size);
            // for each qname in the list
            for (int i = 0; i < size; i++) {
                // get the type decl
                dv = findDTValidator(child, name, memberTypes.get(i),
                        XSConstants.DERIVATION_UNION, schemaDoc);
                if (dv != null) {
                    // if it's a union, expand it
                    if (dv.getVariety() == XSSimpleType.VARIETY_UNION) {
                        dvs = dv.getMemberTypes();
                        for (int j = 0; j < dvs.getLength(); j++)
                            dTValidators.add(dvs.item(j));
                    } else {
                        dTValidators.add(dv);
                    }
                }
            }
        }

        // check if there is a child "simpleType"
        if (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_SIMPLETYPE)) {
            if (restriction || list) {
                // it's an error for both "base" and "simpleType" to appear
                if (baseTypeName != null) {
                    reportSchemaError(list ? "src-simple-type.3.a" : "src-simple-type.2.a", null, content);
                }
                if (baseValidator == null) {
                    // traverse this child to get the base type
                    baseValidator = traverseLocal(content, schemaDoc, grammar);
                }
                // get the next element
                content = DOMUtil.getNextSiblingElement(content);
            }
            else if (union) {
                if (dTValidators == null) {
                    dTValidators = new ArrayList<>(2);
                }
                do {
                    // traverse this child to get the member type
                    dv = traverseLocal(content, schemaDoc, grammar);
                    if (dv != null) {
                        // if it's a union, expand it
                        if (dv.getVariety() == XSSimpleType.VARIETY_UNION) {
                            dvs = dv.getMemberTypes();
                            for (int j = 0; j < dvs.getLength(); j++) {
                                dTValidators.add(dvs.item(j));
                            }
                        }
                        else {
                            dTValidators.add(dv);
                        }
                    }
                    // get the next element
                    content = DOMUtil.getNextSiblingElement(content);
                } while (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_SIMPLETYPE));
            }
        }
        else if ((restriction || list) && baseTypeName == null) {
            // it's an error if neither "base/itemType" nor "simpleType" appears
            reportSchemaError(list ? "src-simple-type.3.b" : "src-simple-type.2.b", null, child);
        }
        else if (union && (memberTypes == null || memberTypes.size() == 0)) {
            // it's an error if "memberTypes" is empty and no "simpleType" appears
            reportSchemaError("src-union-memberTypes-or-simpleTypes", null, child);
        }
        // error finding "base" or error traversing "simpleType".
        // don't need to report an error, since some error has been reported.
        if ((restriction || list) && baseValidator == null) {
            fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
            return errorType(name, schemaDoc.fTargetNamespace,
                    restriction ? XSConstants.DERIVATION_RESTRICTION : XSConstants.DERIVATION_LIST);
        }
        // error finding "memberTypes" or error traversing "simpleType".
        // don't need to report an error, since some error has been reported.
        if (union && (dTValidators == null || dTValidators.size() == 0)) {
            fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
            return errorType(name, schemaDoc.fTargetNamespace,
                    XSConstants.DERIVATION_UNION);
        }
        // item type of list types can't have list content
        if (list && isListDatatype(baseValidator)) {
            reportSchemaError("cos-st-restricts.2.1", new Object[]{name, baseValidator.getName()}, child);
            fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
            return errorType(name, schemaDoc.fTargetNamespace,
                    XSConstants.DERIVATION_LIST);
        }
        // create the simple type based on the "base" type
        XSSimpleType newDecl = null;
        if (restriction) {
            newDecl = fSchemaHandler.fDVFactory.createTypeRestriction(name, schemaDoc.fTargetNamespace, (short)finalProperty, baseValidator,
                    annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
        }
        else if (list) {
            newDecl = fSchemaHandler.fDVFactory.createTypeList(name, schemaDoc.fTargetNamespace, (short)finalProperty, baseValidator,
                    annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
        }
        else if (union) {
            XSSimpleType[] memberDecls = dTValidators.toArray(new XSSimpleType[dTValidators.size()]);
            newDecl = fSchemaHandler.fDVFactory.createTypeUnion(name, schemaDoc.fTargetNamespace, (short)finalProperty, memberDecls,
                    annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
        }
        // now traverse facets, if it's derived by restriction
        if (restriction && content != null) {
            FacetInfo fi = traverseFacets(content, newDecl, baseValidator, schemaDoc);
            content = fi.nodeAfterFacets;

            try {
                fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                newDecl.applyFacets(fi.facetdata, fi.fPresentFacets, fi.fFixedFacets, fValidationState);
            } catch (InvalidDatatypeFacetException ex) {
                reportSchemaError(ex.getKey(), ex.getArgs(), child);
                // Recreate the type, ignoring the facets
                newDecl = fSchemaHandler.fDVFactory.createTypeRestriction(name, schemaDoc.fTargetNamespace, (short)finalProperty, baseValidator,
                        annotations == null? null : new XSObjectListImpl(annotations, annotations.length));
            }
        }
        // no element should appear after this point
        if (content != null) {
            if (restriction) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_RESTRICTION, "(annotation?, (simpleType?, (minExclusive | minInclusive | maxExclusive | maxInclusive | totalDigits | fractionDigits | length | minLength | maxLength | enumeration | whiteSpace | pattern)*))", DOMUtil.getLocalName(content)}, content);
            }
            else if (list) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_LIST, "(annotation?, (simpleType?))", DOMUtil.getLocalName(content)}, content);
            }
            else if (union) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_UNION, "(annotation?, (simpleType*))", DOMUtil.getLocalName(content)}, content);
            }
        }
        fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
        // return the new type
        return newDecl;
    }

    //@param: elm - top element
    //@param: baseTypeStr - type (base/itemType/memberTypes)
    //@param: baseRefContext:  whether the caller is using this type as a base for restriction, union or list
    //return XSSimpleType available for the baseTypeStr, null if not found or disallowed.
    // also throws an error if the base type won't allow itself to be used in this context.
    // REVISIT: can this code be re-used?
    private XSSimpleType findDTValidator(Element elm, String refName,
            QName baseTypeStr, short baseRefContext,
            XSDocumentInfo schemaDoc) {
        if (baseTypeStr == null)
            return null;

        XSTypeDefinition baseType = (XSTypeDefinition)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.TYPEDECL_TYPE, baseTypeStr, elm);
        if (baseType == null) {
            return null;
        }
        if (baseType.getTypeCategory() != XSTypeDefinition.SIMPLE_TYPE) {
            reportSchemaError("cos-st-restricts.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            return null;
        }

        // if it's a complex type, or if its restriction of anySimpleType
        if (baseType == SchemaGrammar.fAnySimpleType &&
            baseRefContext == XSConstants.DERIVATION_RESTRICTION) {
            // if the base type is anySimpleType and the current type is
            // a S4S built-in type, return null. (not an error).
            if (checkBuiltIn(refName, schemaDoc.fTargetNamespace)) {
                return null;
            }
            reportSchemaError("cos-st-restricts.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            return null;
        }

        if ((baseType.getFinal() & baseRefContext) != 0) {
            if (baseRefContext == XSConstants.DERIVATION_RESTRICTION) {
                reportSchemaError("st-props-correct.3", new Object[]{refName, baseTypeStr.rawname}, elm);
            }
            else if (baseRefContext == XSConstants.DERIVATION_LIST) {
                reportSchemaError("cos-st-restricts.2.3.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            }
            else if (baseRefContext == XSConstants.DERIVATION_UNION) {
                reportSchemaError("cos-st-restricts.3.3.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            }
            return null;
        }

        return (XSSimpleType)baseType;
    }

    // check whethe the type denoted by the name and namespace is a S4S
    // built-in type. update fIsBuiltIn at the same time.
    private final boolean checkBuiltIn(String name, String namespace) {
        if (namespace != SchemaSymbols.URI_SCHEMAFORSCHEMA)
            return false;
        if (SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(name) != null)
            fIsBuiltIn = true;
        return fIsBuiltIn;
    }

    // find if a datatype validator is a list or has list datatype member.
    private boolean isListDatatype(XSSimpleType validator) {
        if (validator.getVariety() == XSSimpleType.VARIETY_LIST)
            return true;

        if (validator.getVariety() == XSSimpleType.VARIETY_UNION) {
            XSObjectList temp = validator.getMemberTypes();
            for (int i = 0; i < temp.getLength(); i++) {
                if (((XSSimpleType)temp.item(i)).getVariety() == XSSimpleType.VARIETY_LIST) {
                    return true;
                }
            }
        }

        return false;
    }//isListDatatype(XSSimpleTypeDecl):boolean

    private XSSimpleType errorType(String name, String namespace, short refType) {
        XSSimpleType stringType = (XSSimpleType)SchemaGrammar.SG_SchemaNS.getTypeDefinition("string");
        switch (refType) {
        case XSConstants.DERIVATION_RESTRICTION:
            return fSchemaHandler.fDVFactory.createTypeRestriction(name, namespace, (short)0,
                    stringType, null);
        case XSConstants.DERIVATION_LIST:
            return fSchemaHandler.fDVFactory.createTypeList(name, namespace, (short)0,
                    stringType, null);
        case XSConstants.DERIVATION_UNION:
            return fSchemaHandler.fDVFactory.createTypeUnion(name, namespace, (short)0,
                    new XSSimpleType[]{stringType}, null);
        }

        return null;
    }

}//class XSDSimpleTypeTraverser
