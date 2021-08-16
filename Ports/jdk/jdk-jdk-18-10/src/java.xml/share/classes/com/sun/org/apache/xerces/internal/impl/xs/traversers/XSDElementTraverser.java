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

import java.util.Locale;

import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSConstraints;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * The element declaration schema component traverser.
 * <element
 *   abstract = boolean : false
 *   block = (#all | List of (extension | restriction | substitution))
 *   default = string
 *   final = (#all | List of (extension | restriction))
 *   fixed = string
 *   form = (qualified | unqualified)
 *   id = ID
 *   maxOccurs = (nonNegativeInteger | unbounded)  : 1
 *   minOccurs = nonNegativeInteger : 1
 *   name = NCName
 *   nillable = boolean : false
 *   ref = QName
 *   substitutionGroup = QName
 *   type = QName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, ((simpleType | complexType)?, (unique | key | keyref)*))
 * </element>
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: Oct 2017
 */
class XSDElementTraverser extends XSDAbstractTraverser {

    protected final XSElementDecl  fTempElementDecl  = new XSElementDecl();

    // this controls what happens when a local element is encountered.
    // We may not encounter all local elements when first parsing.
    boolean fDeferTraversingLocalElements;

    XSDElementTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    /**
     * Traverse a locally declared element (or an element reference).
     *
     * To handle the recursive cases efficiently, we delay the traversal
     * and return an empty particle node. We'll fill in this particle node
     * later after we've done with all the global declarations.
     * This method causes a number of data structures in the schema handler to be filled in.
     *
     * @param  elmDecl
     * @param  schemaDoc
     * @param  grammar
     * @return the particle
     */
    XSParticleDecl traverseLocal(Element elmDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            int allContextFlags,
            XSObject parent) {

        XSParticleDecl particle = null;
        if (fSchemaHandler.fDeclPool !=null) {
            particle = fSchemaHandler.fDeclPool.getParticleDecl();
        } else {
            particle = new XSParticleDecl();
        }
        if (fDeferTraversingLocalElements) {
            // The only thing we care about now is whether this element has
            // minOccurs=0. This affects (if the element appears in a complex
            // type) whether a type has emptiable content.
            particle.fType = XSParticleDecl.PARTICLE_ELEMENT;
            Attr attr = elmDecl.getAttributeNode(SchemaSymbols.ATT_MINOCCURS);
            if (attr != null) {
                String min = attr.getValue();
                try {
                    int m = Integer.parseInt(XMLChar.trim(min));
                    if (m >= 0)
                        particle.fMinOccurs = m;
                }
                catch (NumberFormatException ex) {
                }
            }
            fSchemaHandler.fillInLocalElemInfo(elmDecl, schemaDoc, allContextFlags, parent, particle);
        } else {
            traverseLocal(particle, elmDecl, schemaDoc, grammar, allContextFlags, parent, null);
            // If it's an empty particle, return null.
            if (particle.fType == XSParticleDecl.PARTICLE_EMPTY)
                particle = null;
        }

        return particle;
    }

    /**
     * Traverse a locally declared element (or an element reference).
     *
     * This is the real traversal method. It's called after we've done with
     * all the global declarations.
     *
     * @param  index
     */
    protected void traverseLocal(XSParticleDecl particle,
            Element elmDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            int allContextFlags,
            XSObject parent,
            String[] localNSDecls) {

        if (localNSDecls != null) {
            schemaDoc.fNamespaceSupport.setEffectiveContext(localNSDecls);
        }

        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(elmDecl, false, schemaDoc);

        QName refAtt = (QName) attrValues[XSAttributeChecker.ATTIDX_REF];
        XInt  minAtt = (XInt)  attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt  maxAtt = (XInt)  attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];

        XSElementDecl element = null;
        XSAnnotationImpl annotation = null;
        if (elmDecl.getAttributeNode(SchemaSymbols.ATT_REF) != null) {
            if (refAtt != null) {
                element = (XSElementDecl)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.ELEMENT_TYPE, refAtt, elmDecl);

                Element child = DOMUtil.getFirstChildElement(elmDecl);
                if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(elmDecl);
                    if (text != null) {
                        annotation = traverseSyntheticAnnotation(elmDecl, text, attrValues, false, schemaDoc);
                    }
                }
                // Element Declaration Representation OK
                // 2 If the item's parent is not <schema>, then all of the following must be true:
                // 2.1 One of ref or name must be present, but not both.
                // 2.2 If ref is present, then all of <complexType>, <simpleType>, <key>, <keyref>, <unique>, nillable, default, fixed, form, block and type must be absent, i.e. only minOccurs, maxOccurs, id are allowed in addition to ref, along with <annotation>.
                if (child != null) {
                    reportSchemaError("src-element.2.2", new Object[]{refAtt.rawname, DOMUtil.getLocalName(child)}, child);
                }
            } else {
                element = null;
            }
        } else {
            element = traverseNamedElement(elmDecl, attrValues, schemaDoc, grammar, false, parent);
        }

        particle.fMinOccurs = minAtt.intValue();
        particle.fMaxOccurs = maxAtt.intValue();
        if (element != null) {
            particle.fType = XSParticleDecl.PARTICLE_ELEMENT;
            particle.fValue = element;
        }
        else {
            particle.fType = XSParticleDecl.PARTICLE_EMPTY;
        }
        if (refAtt != null) {
            XSObjectList annotations;
            if (annotation != null) {
                annotations = new XSObjectListImpl();
                ((XSObjectListImpl) annotations).addXSObject(annotation);
            } else {
                annotations = XSObjectListImpl.EMPTY_LIST;
            }
            particle.fAnnotations = annotations;
        } else {
            particle.fAnnotations = ((element != null) ? element.fAnnotations
                    : XSObjectListImpl.EMPTY_LIST);
        }
        Long defaultVals = (Long)attrValues[XSAttributeChecker.ATTIDX_FROMDEFAULT];
        checkOccurrences(particle, SchemaSymbols.ELT_ELEMENT,
                (Element)elmDecl.getParentNode(), allContextFlags,
                defaultVals.longValue());

        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
    }

    /**
     * Traverse a globally declared element.
     *
     * @param  elmDecl
     * @param  schemaDoc
     * @param  grammar
     * @return the element declaration
     */
    XSElementDecl traverseGlobal(Element elmDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        // General Attribute Checking'

        Object[] attrValues = fAttrChecker.checkAttributes(elmDecl, true, schemaDoc);
        XSElementDecl element = traverseNamedElement(elmDecl, attrValues, schemaDoc, grammar, true, null);
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return element;

    }

    /**
     * Traverse a globally declared element.
     *
     * @param  elmDecl
     * @param  attrValues
     * @param  schemaDoc
     * @param  grammar
     * @param  isGlobal
     * @return the element declaration
     */
    XSElementDecl traverseNamedElement(Element elmDecl,
            Object[] attrValues,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            boolean isGlobal,
            XSObject parent) {

        Boolean abstractAtt  = (Boolean) attrValues[XSAttributeChecker.ATTIDX_ABSTRACT];
        XInt    blockAtt     = (XInt)    attrValues[XSAttributeChecker.ATTIDX_BLOCK];
        String  defaultAtt   = (String)  attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        XInt    finalAtt     = (XInt)    attrValues[XSAttributeChecker.ATTIDX_FINAL];
        String  fixedAtt     = (String)  attrValues[XSAttributeChecker.ATTIDX_FIXED];
        XInt    formAtt      = (XInt)    attrValues[XSAttributeChecker.ATTIDX_FORM];
        String  nameAtt      = (String)  attrValues[XSAttributeChecker.ATTIDX_NAME];
        Boolean nillableAtt  = (Boolean) attrValues[XSAttributeChecker.ATTIDX_NILLABLE];
        QName   subGroupAtt  = (QName)   attrValues[XSAttributeChecker.ATTIDX_SUBSGROUP];
        QName   typeAtt      = (QName)   attrValues[XSAttributeChecker.ATTIDX_TYPE];

        // Step 1: get declaration information

        XSElementDecl element = null;
        if (fSchemaHandler.fDeclPool !=null) {
            element = fSchemaHandler.fDeclPool.getElementDecl();
        } else {
            element = new XSElementDecl();
        }
        // get 'name'
        if (nameAtt != null)
            element.fName = fSymbolTable.addSymbol(nameAtt);

        // get 'target namespace'
        if (isGlobal) {
            element.fTargetNamespace = schemaDoc.fTargetNamespace;
            element.setIsGlobal();
        }
        else {
            if (parent instanceof XSComplexTypeDecl)
                element.setIsLocal((XSComplexTypeDecl)parent);

            if (formAtt != null) {
                if (formAtt.intValue() == SchemaSymbols.FORM_QUALIFIED)
                    element.fTargetNamespace = schemaDoc.fTargetNamespace;
                else
                    element.fTargetNamespace = null;
            } else if (schemaDoc.fAreLocalElementsQualified) {
                element.fTargetNamespace = schemaDoc.fTargetNamespace;
            } else {
                element.fTargetNamespace = null;
            }
        }

        // get 'block', 'final', 'nillable', 'abstract'
         if (blockAtt == null) {
             // use defaults
             element.fBlock = schemaDoc.fBlockDefault;
             // discard valid Block 'Default' values that are invalid for Block
             // respect #all
             if (element.fBlock != XSConstants.DERIVATION_ALL) {
                 element.fBlock &= (XSConstants.DERIVATION_EXTENSION | XSConstants.DERIVATION_RESTRICTION | XSConstants.DERIVATION_SUBSTITUTION);
             }
         } else {
             // use specified values
             element.fBlock = blockAtt.shortValue();
             // check for valid values
             if ((element.fBlock != XSConstants.DERIVATION_ALL)
                 &&
                 ((element.fBlock | XSConstants.DERIVATION_EXTENSION_RESTRICTION_SUBSTITION)
                     != XSConstants.DERIVATION_EXTENSION_RESTRICTION_SUBSTITION)) {
                 reportSchemaError(
                         "s4s-att-invalid-value",
                         new Object[]{element.fName, "block", "must be (#all | List of (extension | restriction | substitution))"},
                         elmDecl);
             }
        }

        element.fFinal = finalAtt == null ? schemaDoc.fFinalDefault : finalAtt.shortValue();
        // discard valid Final 'Default' values that are invalid for Final
        element.fFinal &= (XSConstants.DERIVATION_EXTENSION | XSConstants.DERIVATION_RESTRICTION);

        if (nillableAtt.booleanValue())
            element.setIsNillable();
        if (abstractAtt != null && abstractAtt.booleanValue())
            element.setIsAbstract();

        // get 'value constraint'
        if (fixedAtt != null) {
            element.fDefault = new ValidatedInfo();
            element.fDefault.normalizedValue = fixedAtt;
            element.setConstraintType(XSConstants.VC_FIXED);
        } else if (defaultAtt != null) {
            element.fDefault = new ValidatedInfo();
            element.fDefault.normalizedValue = defaultAtt;
            element.setConstraintType(XSConstants.VC_DEFAULT);
        } else {
            element.setConstraintType(XSConstants.VC_NONE);
        }

        // get 'substitutionGroup affiliation'
        if (subGroupAtt != null) {
            element.fSubGroup = (XSElementDecl)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.ELEMENT_TYPE, subGroupAtt, elmDecl);
        }

        // get 'annotation'
        Element child = DOMUtil.getFirstChildElement(elmDecl);
        XSAnnotationImpl annotation = null;
        if(child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(elmDecl);
            if (text != null) {
                annotation = traverseSyntheticAnnotation(elmDecl, text, attrValues, false, schemaDoc);
            }
        }

        XSObjectList annotations;
        if (annotation != null) {
            annotations = new XSObjectListImpl();
            ((XSObjectListImpl)annotations).addXSObject (annotation);
        } else {
            annotations = XSObjectListImpl.EMPTY_LIST;
        }
        element.fAnnotations = annotations;

        // get 'type definition'
        XSTypeDefinition elementType = null;
        boolean haveAnonType = false;

        // Handle Anonymous type if there is one
        if (child != null) {
            String childName = DOMUtil.getLocalName(child);

            if (childName.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                elementType = fSchemaHandler.fComplexTypeTraverser.traverseLocal(child, schemaDoc, grammar);
                haveAnonType = true;
                child = DOMUtil.getNextSiblingElement(child);
            }
            else if (childName.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                elementType = fSchemaHandler.fSimpleTypeTraverser.traverseLocal(child, schemaDoc, grammar);
                haveAnonType = true;
                child = DOMUtil.getNextSiblingElement(child);
            }
        }

        // Handler type attribute
        if (elementType == null && typeAtt != null) {
            elementType = (XSTypeDefinition)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.TYPEDECL_TYPE, typeAtt, elmDecl);
            if (elementType == null) {
                element.fUnresolvedTypeName = typeAtt;
            }
        }

        // Get it from the substitutionGroup declaration
        if (elementType == null && element.fSubGroup != null) {
            elementType = element.fSubGroup.fType;
        }

        if (elementType == null) {
            elementType = SchemaGrammar.fAnyType;
        }

        element.fType = elementType;

        // get 'identity constraint'

        // see if there's something here; it had better be key, keyref or unique.
        if (child != null) {
            String childName = DOMUtil.getLocalName(child);
            while (child != null &&
                    (childName.equals(SchemaSymbols.ELT_KEY) ||
                            childName.equals(SchemaSymbols.ELT_KEYREF) ||
                            childName.equals(SchemaSymbols.ELT_UNIQUE))) {

                if (childName.equals(SchemaSymbols.ELT_KEY) ||
                        childName.equals(SchemaSymbols.ELT_UNIQUE)) {
                    // need to set <key>/<unique> to hidden before traversing it,
                    // because it has global scope
                    DOMUtil.setHidden(child, fSchemaHandler.fHiddenNodes);
                    fSchemaHandler.fUniqueOrKeyTraverser.traverse(child, element, schemaDoc, grammar);
                    if(DOMUtil.getAttrValue(child, SchemaSymbols.ATT_NAME).length() != 0 ) {
                        fSchemaHandler.checkForDuplicateNames(
                                (schemaDoc.fTargetNamespace == null) ? ","+DOMUtil.getAttrValue(child, SchemaSymbols.ATT_NAME)
                                        : schemaDoc.fTargetNamespace+","+ DOMUtil.getAttrValue(child, SchemaSymbols.ATT_NAME),
                                        XSDHandler.ATTRIBUTE_TYPE, fSchemaHandler.getIDRegistry(), fSchemaHandler.getIDRegistry_sub(),
                                        child, schemaDoc);
                    }
                } else if (childName.equals(SchemaSymbols.ELT_KEYREF)) {
                    fSchemaHandler.storeKeyRef(child, schemaDoc, element);
                }
                child = DOMUtil.getNextSiblingElement(child);
                if (child != null) {
                    childName = DOMUtil.getLocalName(child);
                }
            }
        }

        // Step 3: check against schema for schemas

        // required attributes
        if (nameAtt == null) {
            if (isGlobal)
                reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_ELEMENT, SchemaSymbols.ATT_NAME}, elmDecl);
            else
                reportSchemaError("src-element.2.1", null, elmDecl);
            nameAtt = NO_NAME;
        }

        // element
        if (child != null) {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{nameAtt, "(annotation?, (simpleType | complexType)?, (unique | key | keyref)*))", DOMUtil.getLocalName(child)}, child);
        }

        // Step 4: check 3.3.3 constraints

        // src-element

        // 1 default and fixed must not both be present.
        if (defaultAtt != null && fixedAtt != null) {
            reportSchemaError("src-element.1", new Object[]{nameAtt}, elmDecl);
        }

        // 2 If the item's parent is not <schema>, then all of the following must be true:
        // 2.1 One of ref or name must be present, but not both.
        // This is checked in XSAttributeChecker

        // 2.2 If ref is present, then all of <complexType>, <simpleType>, <key>, <keyref>, <unique>, nillable, default, fixed, form, block and type must be absent, i.e. only minOccurs, maxOccurs, id are allowed in addition to ref, along with <annotation>.
        // Attributes are checked in XSAttributeChecker, elements are checked in "traverse" method

        // 3 type and either <simpleType> or <complexType> are mutually exclusive.
        if (haveAnonType && (typeAtt != null)) {
            reportSchemaError("src-element.3", new Object[]{nameAtt}, elmDecl);
        }

        // Step 5: check 3.3.6 constraints
        // check for NOTATION type
        checkNotationType(nameAtt, elementType, elmDecl);

        // e-props-correct

        // 2 If there is a {value constraint}, the canonical lexical representation of its value must be valid with respect to the {type definition} as defined in Element Default Valid (Immediate) (3.3.6).
        if (element.fDefault != null) {
            fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
            if (XSConstraints.ElementDefaultValidImmediate(element.fType, element.fDefault.normalizedValue, fValidationState, element.fDefault) == null) {
                reportSchemaError ("e-props-correct.2", new Object[]{nameAtt, element.fDefault.normalizedValue}, elmDecl);
                element.fDefault = null;
                element.setConstraintType(XSConstants.VC_NONE);
            }
        }

        // 4 If there is an {substitution group affiliation}, the {type definition} of the element declaration must be validly derived from the {type definition} of the {substitution group affiliation}, given the value of the {substitution group exclusions} of the {substitution group affiliation}, as defined in Type Derivation OK (Complex) (3.4.6) (if the {type definition} is complex) or as defined in Type Derivation OK (Simple) (3.14.6) (if the {type definition} is simple).
        if (element.fSubGroup != null) {
            if (!XSConstraints.checkTypeDerivationOk(element.fType, element.fSubGroup.fType, element.fSubGroup.fFinal)) {
                reportSchemaError ("e-props-correct.4", new Object[]{nameAtt, subGroupAtt.prefix+":"+subGroupAtt.localpart}, elmDecl);
                element.fSubGroup = null;
            }
        }

        // 5 If the {type definition} or {type definition}'s {content type} is or is derived from ID then there must not be a {value constraint}.
        if (element.fDefault != null) {
            if ((elementType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE &&
                    ((XSSimpleType)elementType).isIDType()) ||
                    (elementType.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE &&
                            ((XSComplexTypeDecl)elementType).containsTypeID())) {
                reportSchemaError ("e-props-correct.5", new Object[]{element.fName}, elmDecl);
                element.fDefault = null;
                element.setConstraintType(XSConstants.VC_NONE);
            }
        }

        // Element without a name. Return null.
        if (element.fName == null)
            return null;

        // Step 5: register the element decl to the grammar
        if (isGlobal) {
            grammar.addGlobalElementDeclAll(element);

            if (grammar.getGlobalElementDecl(element.fName) == null) {
                grammar.addGlobalElementDecl(element);
            }

            // we also add the element to the tolerate duplicates list as well
            final String loc = fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            final XSElementDecl element2 = grammar.getGlobalElementDecl(element.fName, loc);
            if (element2 == null) {
                grammar.addGlobalElementDecl(element, loc);
            }

            // if we are tolerating duplicates, and we found a duplicate declaration
            // use the duplicate one instead
            if (fSchemaHandler.fTolerateDuplicates) {
                if (element2 != null) {
                    element = element2;
                }
                fSchemaHandler.addGlobalElementDecl(element);
            }
        }

        return element;
    }

    void reset(SymbolTable symbolTable, boolean validateAnnotations, Locale locale) {
        super.reset(symbolTable, validateAnnotations, locale);
        fDeferTraversingLocalElements = true;
    } // reset()

}
