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

import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.xs.models.CMBuilder;
import com.sun.org.apache.xerces.internal.impl.xs.models.XSCMValidator;
import com.sun.org.apache.xerces.internal.impl.xs.util.SimpleLocator;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.SymbolHash;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Constaints shared by traversers and validator
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: Nov 2017
 */
public class XSConstraints {

    // IHR: Visited on 2006-11-17
    // Added a boolean return value to particleValidRestriction (it was a void function)
    // to help the checkRecurseLax to know when expansion has happened and no order is required
    // (IHR@xbrl.org) (Ignacio@Hernandez-Ros.com)

    static final int OCCURRENCE_UNKNOWN = SchemaSymbols.OCCURRENCE_UNBOUNDED-1;
    static final XSSimpleType STRING_TYPE =
            (XSSimpleType)SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(SchemaSymbols.ATTVAL_STRING);

    private static XSParticleDecl fEmptyParticle = null;
    public static XSParticleDecl getEmptySequence() {
        if (fEmptyParticle == null) {
            XSModelGroupImpl group = new XSModelGroupImpl();
            group.fCompositor = XSModelGroupImpl.MODELGROUP_SEQUENCE;
            group.fParticleCount = 0;
            group.fParticles = null;
            group.fAnnotations = XSObjectListImpl.EMPTY_LIST;
            XSParticleDecl particle = new XSParticleDecl();
            particle.fType = XSParticleDecl.PARTICLE_MODELGROUP;
            particle.fValue = group;
            particle.fAnnotations = XSObjectListImpl.EMPTY_LIST;
            fEmptyParticle = particle;
        }
        return fEmptyParticle;
    }

    private static final Comparator<XSParticleDecl> ELEMENT_PARTICLE_COMPARATOR =
            new Comparator<XSParticleDecl>() {

        public int compare(XSParticleDecl o1, XSParticleDecl o2) {
            XSParticleDecl pDecl1 = o1;
            XSParticleDecl pDecl2 = o2;
            XSElementDecl decl1 = (XSElementDecl) pDecl1.fValue;
            XSElementDecl decl2 = (XSElementDecl) pDecl2.fValue;

            String namespace1 = decl1.getNamespace();
            String namespace2 = decl2.getNamespace();
            String name1 = decl1.getName();
            String name2 = decl2.getName();

            boolean sameNamespace = (namespace1 == namespace2);
            int namespaceComparison = 0;

            if (!sameNamespace) {
                if (namespace1 != null) {
                    if (namespace2 != null){
                        namespaceComparison = namespace1.compareTo(namespace2);
                    }
                    else {
                        namespaceComparison = 1;
                    }
                }
                else {
                    namespaceComparison = -1;
                }
            }
            //This assumes that the names are never null.
            return namespaceComparison != 0 ? namespaceComparison : name1.compareTo(name2);
        }
    };

    /**
     * check whether derived is valid derived from base, given a subset
     * of {restriction, extension}.B
     */
    public static boolean checkTypeDerivationOk(XSTypeDefinition derived, XSTypeDefinition base, short block) {
        // if derived is anyType, then it's valid only if base is anyType too
        if (derived == SchemaGrammar.fAnyType)
            return derived == base;
        // if derived is anySimpleType, then it's valid only if the base
        // is ur-type
        if (derived == SchemaGrammar.fAnySimpleType) {
            return (base == SchemaGrammar.fAnyType ||
                    base == SchemaGrammar.fAnySimpleType);
        }

        // if derived is simple type
        if (derived.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            // if base is complex type
            if (base.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                // if base is anyType, change base to anySimpleType,
                // otherwise, not valid
                if (base == SchemaGrammar.fAnyType)
                    base = SchemaGrammar.fAnySimpleType;
                else
                    return false;
            }
            return checkSimpleDerivation((XSSimpleType)derived,
                    (XSSimpleType)base, block);
        }
        else {
            return checkComplexDerivation((XSComplexTypeDecl)derived, base, block);
        }
    }

    /**
     * check whether simple type derived is valid derived from base,
     * given a subset of {restriction, extension}.
     */
    public static boolean checkSimpleDerivationOk(XSSimpleType derived, XSTypeDefinition base, short block) {
        // if derived is anySimpleType, then it's valid only if the base
        // is ur-type
        if (derived == SchemaGrammar.fAnySimpleType) {
            return (base == SchemaGrammar.fAnyType ||
                    base == SchemaGrammar.fAnySimpleType);
        }

        // if base is complex type
        if (base.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
            // if base is anyType, change base to anySimpleType,
            // otherwise, not valid
            if (base == SchemaGrammar.fAnyType)
                base = SchemaGrammar.fAnySimpleType;
            else
                return false;
        }
        return checkSimpleDerivation(derived, (XSSimpleType)base, block);
    }

    /**
     * check whether complex type derived is valid derived from base,
     * given a subset of {restriction, extension}.
     */
    public static boolean checkComplexDerivationOk(XSComplexTypeDecl derived, XSTypeDefinition base, short block) {
        // if derived is anyType, then it's valid only if base is anyType too
        if (derived == SchemaGrammar.fAnyType)
            return derived == base;
        return checkComplexDerivation(derived, base, block);
    }

    /**
     * Note: this will be a private method, and it assumes that derived is not
     *       anySimpleType, and base is not anyType. Another method will be
     *       introduced for public use, which will call this method.
     */
    private static boolean checkSimpleDerivation(XSSimpleType derived, XSSimpleType base, short block) {
        // 1 They are the same type definition.
        if (derived == base)
            return true;

        // 2 All of the following must be true:
        // 2.1 restriction is not in the subset, or in the {final} of its own {base type definition};
        if ((block & XSConstants.DERIVATION_RESTRICTION) != 0 ||
                (derived.getBaseType().getFinal() & XSConstants.DERIVATION_RESTRICTION) != 0) {
            return false;
        }

        // 2.2 One of the following must be true:
        // 2.2.1 D's base type definition is B.
        XSSimpleType directBase = (XSSimpleType)derived.getBaseType();
        if (directBase == base)
            return true;

        // 2.2.2 D's base type definition is not the simple ur-type definition and is validly derived from B given the subset, as defined by this constraint.
        if (directBase != SchemaGrammar.fAnySimpleType &&
                checkSimpleDerivation(directBase, base, block)) {
            return true;
        }

        // 2.2.3 D's {variety} is list or union and B is the simple ur-type definition.
        if ((derived.getVariety() == XSSimpleType.VARIETY_LIST ||
                derived.getVariety() == XSSimpleType.VARIETY_UNION) &&
                base == SchemaGrammar.fAnySimpleType) {
            return true;
        }

        // 2.2.4 B's {variety} is union and D is validly derived from a type definition in B's {member type definitions} given the subset, as defined by this constraint.
        if (base.getVariety() == XSSimpleType.VARIETY_UNION) {
            XSObjectList subUnionMemberDV = base.getMemberTypes();
            int subUnionSize = subUnionMemberDV.getLength();
            for (int i=0; i<subUnionSize; i++) {
                base = (XSSimpleType)subUnionMemberDV.item(i);
                if (checkSimpleDerivation(derived, base, block))
                    return true;
            }
        }

        return false;
    }

    /**
     * Note: this will be a private method, and it assumes that derived is not
     *       anyType. Another method will be introduced for public use,
     *       which will call this method.
     */
    private static boolean checkComplexDerivation(XSComplexTypeDecl derived, XSTypeDefinition base, short block) {
        // 2.1 B and D must be the same type definition.
        if (derived == base)
            return true;

        // 1 If B and D are not the same type definition, then the {derivation method} of D must not be in the subset.
        if ((derived.fDerivedBy & block) != 0)
            return false;

        // 2 One of the following must be true:
        XSTypeDefinition directBase = derived.fBaseType;
        // 2.2 B must be D's {base type definition}.
        if (directBase == base)
            return true;

        // 2.3 All of the following must be true:
        // 2.3.1 D's {base type definition} must not be the ur-type definition.
        if (directBase == SchemaGrammar.fAnyType ||
                directBase == SchemaGrammar.fAnySimpleType) {
            return false;
        }

        // 2.3.2 The appropriate case among the following must be true:
        // 2.3.2.1 If D's {base type definition} is complex, then it must be validly derived from B given the subset as defined by this constraint.
        if (directBase.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
            return checkComplexDerivation((XSComplexTypeDecl)directBase, base, block);

        // 2.3.2.2 If D's {base type definition} is simple, then it must be validly derived from B given the subset as defined in Type Derivation OK (Simple) (3.14.6).
        if (directBase.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            // if base is complex type
            if (base.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                // if base is anyType, change base to anySimpleType,
                // otherwise, not valid
                if (base == SchemaGrammar.fAnyType)
                    base = SchemaGrammar.fAnySimpleType;
                else
                    return false;
            }
            return checkSimpleDerivation((XSSimpleType)directBase,
                    (XSSimpleType)base, block);
        }

        return false;
    }

    /**
     * check whether a value is a valid default for some type
     * returns the compiled form of the value
     * The parameter value could be either a String or a ValidatedInfo object
     */
    public static Object ElementDefaultValidImmediate(XSTypeDefinition type, String value, ValidationContext context, ValidatedInfo vinfo) {

        XSSimpleType dv = null;

        // e-props-correct
        // For a string to be a valid default with respect to a type definition the appropriate case among the following must be true:
        // 1 If the type definition is a simple type definition, then the string must be valid with respect to that definition as defined by String Valid (3.14.4).
        if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            dv = (XSSimpleType)type;
        }

        // 2 If the type definition is a complex type definition, then all of the following must be true:
        else {
            // 2.1 its {content type} must be a simple type definition or mixed.
            XSComplexTypeDecl ctype = (XSComplexTypeDecl)type;
            // 2.2 The appropriate case among the following must be true:
            // 2.2.1 If the {content type} is a simple type definition, then the string must be valid with respect to that simple type definition as defined by String Valid (3.14.4).
            if (ctype.fContentType == XSComplexTypeDecl.CONTENTTYPE_SIMPLE) {
                dv = ctype.fXSSimpleType;
            }
            // 2.2.2 If the {content type} is mixed, then the {content type}'s particle must be emptiable as defined by Particle Emptiable (3.9.6).
            else if (ctype.fContentType == XSComplexTypeDecl.CONTENTTYPE_MIXED) {
                if (!((XSParticleDecl)ctype.getParticle()).emptiable())
                    return null;
            }
            else {
                return null;
            }
        }

        // get the simple type declaration, and validate
        Object actualValue = null;
        if (dv == null) {
            // complex type with mixed. to make sure that we store correct
            // information in vinfo and return the correct value, we use
            // "string" type for validation
            dv = STRING_TYPE;
        }
        try {
            // validate the original lexical rep, and set the actual value
            actualValue = dv.validate(value, context, vinfo);
            // validate the canonical lexical rep
            if (vinfo != null)
                actualValue = dv.validate(vinfo.stringValue(), context, vinfo);
        } catch (InvalidDatatypeValueException ide) {
            return null;
        }

        return actualValue;
    }

    static void reportSchemaError(XMLErrorReporter errorReporter,
            SimpleLocator loc,
            String key, Object[] args) {
        if (loc != null) {
            errorReporter.reportError(loc, XSMessageFormatter.SCHEMA_DOMAIN,
                    key, args, XMLErrorReporter.SEVERITY_ERROR);
        }
        else {
            errorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN,
                    key, args, XMLErrorReporter.SEVERITY_ERROR);
        }
    }

    /**
     * used to check the 3 constraints against each complex type
     * (should be each model group):
     * Unique Particle Attribution, Particle Derivation (Restriction),
     * Element Declrations Consistent.
     */
    public static void fullSchemaChecking(XSGrammarBucket grammarBucket,
            SubstitutionGroupHandler SGHandler,
            CMBuilder cmBuilder,
            XMLErrorReporter errorReporter) {
        // get all grammars, and put all substitution group information
        // in the substitution group handler
        SchemaGrammar[] grammars = grammarBucket.getGrammars();
        for (int i = grammars.length-1; i >= 0; i--) {
            SGHandler.addSubstitutionGroup(grammars[i].getSubstitutionGroups());
        }

        XSParticleDecl fakeDerived = new XSParticleDecl();
        XSParticleDecl fakeBase = new XSParticleDecl();
        fakeDerived.fType = XSParticleDecl.PARTICLE_MODELGROUP;
        fakeBase.fType = XSParticleDecl.PARTICLE_MODELGROUP;
        // before worrying about complexTypes, let's get
        // groups redefined by restriction out of the way.
        for (int g = grammars.length-1; g >= 0; g--) {
            XSGroupDecl [] redefinedGroups = grammars[g].getRedefinedGroupDecls();
            SimpleLocator [] rgLocators = grammars[g].getRGLocators();
            for(int i=0; i<redefinedGroups.length; ) {
                XSGroupDecl derivedGrp = redefinedGroups[i++];
                XSModelGroupImpl derivedMG = derivedGrp.fModelGroup;
                XSGroupDecl baseGrp = redefinedGroups[i++];
                XSModelGroupImpl baseMG = baseGrp.fModelGroup;
                fakeDerived.fValue = derivedMG;
                fakeBase.fValue = baseMG;
                if(baseMG == null) {
                    if(derivedMG != null) { // can't be a restriction!
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                "src-redefine.6.2.2",
                                new Object[]{derivedGrp.fName, "rcase-Recurse.2"});
                    }
                } else if (derivedMG == null) {
                    if (!fakeBase.emptiable()) {
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                "src-redefine.6.2.2",
                                new Object[]{derivedGrp.fName, "rcase-Recurse.2"});
                    }
                } else {
                    try {
                        particleValidRestriction(fakeDerived, SGHandler, fakeBase, SGHandler);
                    } catch (XMLSchemaException e) {
                        String key = e.getKey();
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                key,
                                e.getArgs());
                        reportSchemaError(errorReporter, rgLocators[i/2-1],
                                "src-redefine.6.2.2",
                                new Object[]{derivedGrp.fName, key});
                    }
                }
            }
        }

        // for each complex type, check the 3 constraints.
        // types need to be checked
        XSComplexTypeDecl[] types;
        SimpleLocator [] ctLocators;
        // to hold the errors
        // REVISIT: do we want to report all errors? or just one?
        //XMLSchemaError1D errors = new XMLSchemaError1D();
        // whether need to check this type again;
        // whether only do UPA checking
        boolean further, fullChecked;
        // if do all checkings, how many need to be checked again.
        int keepType;
        // i: grammar; j: type; k: error
        // for all grammars
        SymbolHash elemTable = new SymbolHash();
        for (int i = grammars.length-1, j; i >= 0; i--) {
            // get whether to skip EDC, and types need to be checked
            keepType = 0;
            fullChecked = grammars[i].fFullChecked;
            types = grammars[i].getUncheckedComplexTypeDecls();
            ctLocators = grammars[i].getUncheckedCTLocators();
            // for each type
            for (j = 0; j < types.length; j++) {
                // if we've already full-checked this grammar, then
                // skip the EDC constraint
                if (!fullChecked) {
                    // 1. Element Decl Consistent
                    if (types[j].fParticle!=null) {
                        elemTable.clear();
                        try {
                            checkElementDeclsConsistent(types[j], types[j].fParticle,
                                    elemTable, SGHandler);
                        }
                        catch (XMLSchemaException e) {
                            reportSchemaError(errorReporter, ctLocators[j],
                                    e.getKey(),
                                    e.getArgs());
                        }
                    }
                }

                // 2. Particle Derivation

                if (types[j].fBaseType != null &&
                        types[j].fBaseType != SchemaGrammar.fAnyType &&
                        types[j].fDerivedBy == XSConstants.DERIVATION_RESTRICTION &&
                        (types[j].fBaseType instanceof XSComplexTypeDecl)) {

                    XSParticleDecl derivedParticle=types[j].fParticle;
                    XSParticleDecl baseParticle=
                        ((XSComplexTypeDecl)(types[j].fBaseType)).fParticle;
                    if (derivedParticle==null) {
                        if (baseParticle!=null && !baseParticle.emptiable()) {
                            reportSchemaError(errorReporter,ctLocators[j],
                                    "derivation-ok-restriction.5.3.2",
                                    new Object[]{types[j].fName, types[j].fBaseType.getName()});
                        }
                    }
                    else if (baseParticle!=null) {
                        try {
                            particleValidRestriction(types[j].fParticle,
                                    SGHandler,
                                    ((XSComplexTypeDecl)(types[j].fBaseType)).fParticle,
                                    SGHandler);
                        } catch (XMLSchemaException e) {
                            reportSchemaError(errorReporter, ctLocators[j],
                                    e.getKey(),
                                    e.getArgs());
                            reportSchemaError(errorReporter, ctLocators[j],
                                    "derivation-ok-restriction.5.4.2",
                                    new Object[]{types[j].fName});
                        }
                    }
                    else {
                        reportSchemaError(errorReporter, ctLocators[j],
                                "derivation-ok-restriction.5.4.2",
                                new Object[]{types[j].fName});
                    }
                }
                // 3. UPA
                // get the content model and check UPA
                XSCMValidator cm = types[j].getContentModel(cmBuilder, true);
                further = false;
                if (cm != null) {
                    try {
                        further = cm.checkUniqueParticleAttribution(SGHandler);
                    } catch (XMLSchemaException e) {
                        reportSchemaError(errorReporter, ctLocators[j],
                                e.getKey(),
                                e.getArgs());
                    }
                }
                // now report all errors
                // REVISIT: do we want to report all errors? or just one?
                /*for (k = errors.getErrorCodeNum()-1; k >= 0; k--) {
                    reportSchemaError(errorReporter, ctLocators[j],
                                      errors.getErrorCode(k),
                                      errors.getArgs(k));
                }*/

                // if we are doing all checkings, and this one needs further
                // checking, store it in the type array.
                if (!fullChecked && further)
                    types[keepType++] = types[j];

                // clear errors for the next type.
                // REVISIT: do we want to report all errors? or just one?
                //errors.clear();
            }
            // we've done with the types in this grammar. if we are checking
            // all constraints, need to trim type array to a proper size:
            // only contain those need further checking.
            // and mark this grammar that it only needs UPA checking.
            if (!fullChecked) {
                grammars[i].setUncheckedTypeNum(keepType);
                grammars[i].fFullChecked = true;
            }
        }
    }

    /*
       Check that a given particle is a valid restriction of a base particle.
     */

    public static void checkElementDeclsConsistent(XSComplexTypeDecl type,
            XSParticleDecl particle,
            SymbolHash elemDeclHash,
            SubstitutionGroupHandler sgHandler)
        throws XMLSchemaException {

        // check for elements in the tree with the same name and namespace

        int pType = particle.fType;

        if (pType == XSParticleDecl.PARTICLE_WILDCARD)
            return;

        if (pType == XSParticleDecl.PARTICLE_ELEMENT) {
            XSElementDecl elem = (XSElementDecl)(particle.fValue);
            findElemInTable(type, elem, elemDeclHash);

            if (elem.fScope == XSConstants.SCOPE_GLOBAL) {
                // Check for subsitution groups.
                XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(elem);
                for (int i = 0; i < subGroup.length; i++) {
                    findElemInTable(type, subGroup[i], elemDeclHash);
                }
            }
            return;
        }

        XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
        for (int i = 0; i < group.fParticleCount; i++)
            checkElementDeclsConsistent(type, group.fParticles[i], elemDeclHash, sgHandler);
    }

    public static void findElemInTable(XSComplexTypeDecl type, XSElementDecl elem,
            SymbolHash elemDeclHash)
        throws XMLSchemaException {

        // How can we avoid this concat?  LM.
        String name = elem.fName + "," + elem.fTargetNamespace;

        XSElementDecl existingElem = null;
        if ((existingElem = (XSElementDecl)(elemDeclHash.get(name))) == null) {
            // just add it in
            elemDeclHash.put(name, elem);
        }
        else {
            // If this is the same check element, we're O.K.
            if (elem == existingElem)
                return;

            if (elem.fType != existingElem.fType) {
                // Types are not the same
                throw new XMLSchemaException("cos-element-consistent",
                        new Object[] {type.fName, elem.fName});

            }
        }
    }

    // Check that a given particle is a valid restriction of a base particle.
    //
    // IHR: 2006/11/17
    // Returns a boolean indicating if there has been expansion of substitution group
    // in the bParticle.
    // With this information the checkRecurseLax function knows when is
    // to keep the order and when to ignore it.
    private static boolean particleValidRestriction(XSParticleDecl dParticle,
            SubstitutionGroupHandler dSGHandler,
            XSParticleDecl bParticle,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {
        return particleValidRestriction(dParticle, dSGHandler, bParticle, bSGHandler, true);
    }

    private static boolean particleValidRestriction(XSParticleDecl dParticle,
            SubstitutionGroupHandler dSGHandler,
            XSParticleDecl bParticle,
            SubstitutionGroupHandler bSGHandler,
            boolean checkWCOccurrence)
        throws XMLSchemaException {

        List<XSParticleDecl> dChildren = null;
        List<XSParticleDecl> bChildren = null;
        int dMinEffectiveTotalRange=OCCURRENCE_UNKNOWN;
        int dMaxEffectiveTotalRange=OCCURRENCE_UNKNOWN;

        // By default there has been no expansion
        boolean bExpansionHappened = false;

        // Check for empty particles.   If either base or derived particle is empty,
        // (and the other isn't) it's an error.
        if (dParticle.isEmpty() && !bParticle.emptiable()) {
            throw new XMLSchemaException("cos-particle-restrict.a", null);
        }
        else if (!dParticle.isEmpty() && bParticle.isEmpty()) {
            throw new XMLSchemaException("cos-particle-restrict.b", null);
        }

        //
        // Do setup prior to invoking the Particle (Restriction) cases.
        // This involves:
        //   - removing pointless occurrences for groups, and retrieving a vector of
        //     non-pointless children
        //   - turning top-level elements with substitution groups into CHOICE groups.
        //

        short dType = dParticle.fType;
        //
        // Handle pointless groups for the derived particle
        //
        if (dType == XSParticleDecl.PARTICLE_MODELGROUP) {
            dType = ((XSModelGroupImpl)dParticle.fValue).fCompositor;

            // Find a group, starting with this particle, with more than 1 child.   There
            // may be none, and the particle of interest trivially becomes an element or
            // wildcard.
            XSParticleDecl dtmp = getNonUnaryGroup(dParticle);
            if (dtmp != dParticle) {
                // Particle has been replaced.   Retrieve new type info.
                dParticle = dtmp;
                dType = dParticle.fType;
                if (dType == XSParticleDecl.PARTICLE_MODELGROUP)
                    dType = ((XSModelGroupImpl)dParticle.fValue).fCompositor;
            }

            // Fill in a vector with the children of the particle, removing any
            // pointless model groups in the process.
            dChildren = removePointlessChildren(dParticle);
        }

        int dMinOccurs = dParticle.fMinOccurs;
        int dMaxOccurs = dParticle.fMaxOccurs;

        //
        // For elements which are the heads of substitution groups, treat as CHOICE
        //
        if (dSGHandler != null && dType == XSParticleDecl.PARTICLE_ELEMENT) {
            XSElementDecl dElement = (XSElementDecl)dParticle.fValue;

            if (dElement.fScope == XSConstants.SCOPE_GLOBAL) {
                // Check for subsitution groups.   Treat any element that has a
                // subsitution group as a choice.   Fill in the children vector with the
                // members of the substitution group
                XSElementDecl[] subGroup = dSGHandler.getSubstitutionGroup(dElement);
                if (subGroup.length >0 ) {
                    // Now, set the type to be CHOICE.  The "group" will have the same
                    // occurrence information as the original particle.
                    dType = XSModelGroupImpl.MODELGROUP_CHOICE;
                    dMinEffectiveTotalRange = dMinOccurs;
                    dMaxEffectiveTotalRange = dMaxOccurs;

                    // Fill in the vector of children
                    dChildren = new ArrayList<>(subGroup.length+1);
                    for (int i = 0; i < subGroup.length; i++) {
                        addElementToParticleVector(dChildren, subGroup[i]);
                    }
                    addElementToParticleVector(dChildren, dElement);
                    Collections.sort(dChildren, ELEMENT_PARTICLE_COMPARATOR);

                    // Set the handler to null, to indicate that we've finished handling
                    // substitution groups for this particle.
                    dSGHandler = null;
                }
            }
        }

        short bType = bParticle.fType;
        //
        // Handle pointless groups for the base particle
        //
        if (bType == XSParticleDecl.PARTICLE_MODELGROUP) {
            bType = ((XSModelGroupImpl)bParticle.fValue).fCompositor;

            // Find a group, starting with this particle, with more than 1 child.   There
            // may be none, and the particle of interest trivially becomes an element or
            // wildcard.
            XSParticleDecl btmp = getNonUnaryGroup(bParticle);
            if (btmp != bParticle) {
                // Particle has been replaced.   Retrieve new type info.
                bParticle = btmp;
                bType = bParticle.fType;
                if (bType == XSParticleDecl.PARTICLE_MODELGROUP)
                    bType = ((XSModelGroupImpl)bParticle.fValue).fCompositor;
            }

            // Fill in a vector with the children of the particle, removing any
            // pointless model groups in the process.
            bChildren = removePointlessChildren(bParticle);
        }

        int bMinOccurs = bParticle.fMinOccurs;
        int bMaxOccurs = bParticle.fMaxOccurs;

        if (bSGHandler != null && bType == XSParticleDecl.PARTICLE_ELEMENT) {
            XSElementDecl bElement = (XSElementDecl)bParticle.fValue;

            if (bElement.fScope == XSConstants.SCOPE_GLOBAL) {
                // Check for subsitution groups.   Treat any element that has a
                // subsitution group as a choice.   Fill in the children vector with the
                // members of the substitution group
                XSElementDecl[] bsubGroup = bSGHandler.getSubstitutionGroup(bElement);
                if (bsubGroup.length >0 ) {
                    // Now, set the type to be CHOICE
                    bType = XSModelGroupImpl.MODELGROUP_CHOICE;

                    bChildren = new ArrayList<>(bsubGroup.length+1);
                    for (int i = 0; i < bsubGroup.length; i++) {
                        addElementToParticleVector(bChildren, bsubGroup[i]);
                    }
                    addElementToParticleVector(bChildren, bElement);
                    Collections.sort(bChildren, ELEMENT_PARTICLE_COMPARATOR);
                    // Set the handler to null, to indicate that we've finished handling
                    // substitution groups for this particle.
                    bSGHandler = null;

                    // if we are here expansion of bParticle happened
                    bExpansionHappened = true;
                }
            }
        }

        //
        // O.K. - Figure out which particle derivation rule applies and call it
        //
        switch (dType) {
            case XSParticleDecl.PARTICLE_ELEMENT:
            {
                switch (bType) {

                    // Elt:Elt NameAndTypeOK
                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        checkNameAndTypeOK((XSElementDecl)dParticle.fValue,dMinOccurs,dMaxOccurs,
                                (XSElementDecl)bParticle.fValue,bMinOccurs,bMaxOccurs);
                        return bExpansionHappened;
                    }

                    // Elt:Any NSCompat
                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        checkNSCompat((XSElementDecl)dParticle.fValue,dMinOccurs,dMaxOccurs,
                                (XSWildcardDecl)bParticle.fValue,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);
                        return bExpansionHappened;
                    }

                    // Elt:All RecurseAsIfGroup
                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    {
                        // Treat the element as if it were in a group of the same type
                        // as the base Particle
                        dChildren = new ArrayList<>();
                        dChildren.add(dParticle);

                        checkRecurseLax(dChildren, 1, 1, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSModelGroupImpl.MODELGROUP_ALL:
                    {
                        // Treat the element as if it were in a group of the same type
                        // as the base Particle
                        dChildren = new ArrayList<>();
                        dChildren.add(dParticle);

                        checkRecurse(dChildren, 1, 1, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

            case XSParticleDecl.PARTICLE_WILDCARD:
            {
                switch (bType) {

                    // Any:Any NSSubset
                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        checkNSSubset((XSWildcardDecl)dParticle.fValue, dMinOccurs, dMaxOccurs,
                                (XSWildcardDecl)bParticle.fValue, bMinOccurs, bMaxOccurs);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSModelGroupImpl.MODELGROUP_ALL:
                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"any:choice,sequence,all,elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

            case XSModelGroupImpl.MODELGROUP_ALL:
            {
                switch (bType) {

                    // All:Any NSRecurseCheckCardinality
                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        if (dMinEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMinEffectiveTotalRange = dParticle.minEffectiveTotalRange();
                        if (dMaxEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMaxEffectiveTotalRange = dParticle.maxEffectiveTotalRange();

                        checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange,
                                dMaxEffectiveTotalRange,
                                dSGHandler,
                                bParticle,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);

                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_ALL:
                    {
                        checkRecurse(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"all:choice,sequence,elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

            case XSModelGroupImpl.MODELGROUP_CHOICE:
            {
                switch (bType) {

                    // Choice:Any NSRecurseCheckCardinality
                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        if (dMinEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMinEffectiveTotalRange = dParticle.minEffectiveTotalRange();
                        if (dMaxEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMaxEffectiveTotalRange = dParticle.maxEffectiveTotalRange();

                        checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange,
                                dMaxEffectiveTotalRange,
                                dSGHandler,
                                bParticle,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    {
                        checkRecurseLax(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_ALL:
                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"choice:all,sequence,elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }


            case XSModelGroupImpl.MODELGROUP_SEQUENCE:
            {
                switch (bType) {

                    // Choice:Any NSRecurseCheckCardinality
                    case XSParticleDecl.PARTICLE_WILDCARD:
                    {
                        if (dMinEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMinEffectiveTotalRange = dParticle.minEffectiveTotalRange();
                        if (dMaxEffectiveTotalRange == OCCURRENCE_UNKNOWN)
                            dMaxEffectiveTotalRange = dParticle.maxEffectiveTotalRange();

                        checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange,
                                dMaxEffectiveTotalRange,
                                dSGHandler,
                                bParticle,bMinOccurs,bMaxOccurs,
                                checkWCOccurrence);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_ALL:
                    {
                        checkRecurseUnordered(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_SEQUENCE:
                    {
                        checkRecurse(dChildren, dMinOccurs, dMaxOccurs, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSModelGroupImpl.MODELGROUP_CHOICE:
                    {
                        int min1 = dMinOccurs * dChildren.size();
                        int max1 = (dMaxOccurs == SchemaSymbols.OCCURRENCE_UNBOUNDED)?
                                dMaxOccurs : dMaxOccurs * dChildren.size();
                        checkMapAndSum(dChildren, min1, max1, dSGHandler,
                                bChildren, bMinOccurs, bMaxOccurs, bSGHandler);
                        return bExpansionHappened;
                    }

                    case XSParticleDecl.PARTICLE_ELEMENT:
                    {
                        throw new XMLSchemaException("cos-particle-restrict.2",
                                new Object[]{"seq:elt"});
                    }

                    default:
                    {
                        throw new XMLSchemaException("Internal-Error",
                                new Object[]{"in particleValidRestriction"});
                    }
                }
            }

        }

        return bExpansionHappened;
    }

    private static void addElementToParticleVector (List<XSParticleDecl> v, XSElementDecl d)  {

        XSParticleDecl p = new XSParticleDecl();
        p.fValue = d;
        p.fType = XSParticleDecl.PARTICLE_ELEMENT;
        v.add(p);

    }

    private static XSParticleDecl getNonUnaryGroup(XSParticleDecl p) {

        if (p.fType == XSParticleDecl.PARTICLE_ELEMENT ||
                p.fType == XSParticleDecl.PARTICLE_WILDCARD)
            return p;

        if (p.fMinOccurs==1 && p.fMaxOccurs==1 &&
                p.fValue!=null && ((XSModelGroupImpl)p.fValue).fParticleCount == 1)
            return getNonUnaryGroup(((XSModelGroupImpl)p.fValue).fParticles[0]);
        else
            return p;
    }

    private static List<XSParticleDecl> removePointlessChildren(XSParticleDecl p)  {

        if (p.fType == XSParticleDecl.PARTICLE_ELEMENT ||
                p.fType == XSParticleDecl.PARTICLE_WILDCARD)
            return null;

        List<XSParticleDecl> children = new ArrayList<>();

        XSModelGroupImpl group = (XSModelGroupImpl)p.fValue;
        for (int i = 0; i < group.fParticleCount; i++)
            gatherChildren(group.fCompositor, group.fParticles[i], children);

        return children;
    }


    private static void gatherChildren(int parentType, XSParticleDecl p, List<XSParticleDecl> children) {

        int min = p.fMinOccurs;
        int max = p.fMaxOccurs;
        int type = p.fType;
        if (type == XSParticleDecl.PARTICLE_MODELGROUP)
            type = ((XSModelGroupImpl)p.fValue).fCompositor;

        if (type == XSParticleDecl.PARTICLE_ELEMENT ||
                type== XSParticleDecl.PARTICLE_WILDCARD) {
            children.add(p);
            return;
        }

        if (! (min==1 && max==1)) {
            children.add(p);
        }
        else if (parentType == type) {
            XSModelGroupImpl group = (XSModelGroupImpl)p.fValue;
            for (int i = 0; i < group.fParticleCount; i++)
                gatherChildren(type, group.fParticles[i], children);
        }
        else if (!p.isEmpty()) {
            children.add(p);
        }

    }

    private static void checkNameAndTypeOK(XSElementDecl dElement, int dMin, int dMax,
            XSElementDecl bElement, int bMin, int bMax)
        throws XMLSchemaException {


        //
        // Check that the names are the same
        //
        if (dElement.fName != bElement.fName ||
                dElement.fTargetNamespace != bElement.fTargetNamespace) {
            throw new XMLSchemaException(
                    "rcase-NameAndTypeOK.1",new Object[]{dElement.fName,
                            dElement.fTargetNamespace, bElement.fName, bElement.fTargetNamespace});
        }

        //
        // Check nillable
        //
        if (!bElement.getNillable() && dElement.getNillable()) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.2",
                    new Object[]{dElement.fName});
        }

        //
        // Check occurrence range
        //
        if (!checkOccurrenceRange(dMin, dMax, bMin, bMax)) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.3",
                    new Object[]{
                    dElement.fName,
                    Integer.toString(dMin),
                    dMax==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(dMax),
                            Integer.toString(bMin),
                            bMax==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(bMax)});
        }

        //
        // Check for consistent fixed values
        //
        if (bElement.getConstraintType() == XSConstants.VC_FIXED) {
            // derived one has to have a fixed value
            if (dElement.getConstraintType() != XSConstants.VC_FIXED) {
                throw new XMLSchemaException("rcase-NameAndTypeOK.4.a",
                        new Object[]{dElement.fName, bElement.fDefault.stringValue()});
            }

            // get simple type
            boolean isSimple = false;
            if (dElement.fType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE ||
                    ((XSComplexTypeDecl)dElement.fType).fContentType == XSComplexTypeDecl.CONTENTTYPE_SIMPLE) {
                isSimple = true;
            }

            // if there is no simple type, then compare based on string
            if (!isSimple && !bElement.fDefault.normalizedValue.equals(dElement.fDefault.normalizedValue) ||
                    isSimple && !bElement.fDefault.actualValue.equals(dElement.fDefault.actualValue)) {
                throw new XMLSchemaException("rcase-NameAndTypeOK.4.b",
                        new Object[]{dElement.fName,
                        dElement.fDefault.stringValue(),
                        bElement.fDefault.stringValue()});
            }
        }

        //
        // Check identity constraints
        //
        checkIDConstraintRestriction(dElement, bElement);

        //
        // Check for disallowed substitutions
        //
        int blockSet1 = dElement.fBlock;
        int blockSet2 = bElement.fBlock;
        if (((blockSet1 & blockSet2)!=blockSet2) ||
                (blockSet1==XSConstants.DERIVATION_NONE && blockSet2!=XSConstants.DERIVATION_NONE))
            throw new XMLSchemaException("rcase-NameAndTypeOK.6",
                    new Object[]{dElement.fName});


        //
        // Check that the derived element's type is derived from the base's.
        //
        if (!checkTypeDerivationOk(dElement.fType, bElement.fType,
                (short)(XSConstants.DERIVATION_EXTENSION|XSConstants.DERIVATION_LIST|XSConstants.DERIVATION_UNION))) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.7",
                    new Object[]{dElement.fName, dElement.fType.getName(), bElement.fType.getName()});
        }

    }


    private static void checkIDConstraintRestriction(XSElementDecl derivedElemDecl,
            XSElementDecl baseElemDecl)
        throws XMLSchemaException {
        // TODO
    } // checkIDConstraintRestriction


    private static boolean checkOccurrenceRange(int min1, int max1, int min2, int max2) {

        if ((min1 >= min2) &&
                ((max2==SchemaSymbols.OCCURRENCE_UNBOUNDED) ||
                        (max1!=SchemaSymbols.OCCURRENCE_UNBOUNDED && max1<=max2)))
            return true;
        else
            return false;
    }

    private static void checkNSCompat(XSElementDecl elem, int min1, int max1,
            XSWildcardDecl wildcard, int min2, int max2,
            boolean checkWCOccurrence)
        throws XMLSchemaException {

        // check Occurrence ranges
        if (checkWCOccurrence && !checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-NSCompat.2",
                    new Object[]{
                    elem.fName,
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        // check wildcard allows namespace of element
        if (!wildcard.allowNamespace(elem.fTargetNamespace))  {
            throw new XMLSchemaException("rcase-NSCompat.1",
                    new Object[]{elem.fName,elem.fTargetNamespace});
        }

    }

    private static void checkNSSubset(XSWildcardDecl dWildcard, int min1, int max1,
            XSWildcardDecl bWildcard, int min2, int max2)
        throws XMLSchemaException {

        // check Occurrence ranges
        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-NSSubset.2", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        // check wildcard subset
        if (!dWildcard.isSubsetOf(bWildcard)) {
            throw new XMLSchemaException("rcase-NSSubset.1", null);
        }

        if (dWildcard.weakerProcessContents(bWildcard)) {
            throw new XMLSchemaException("rcase-NSSubset.3",
                    new Object[]{dWildcard.getProcessContentsAsString(),
                    bWildcard.getProcessContentsAsString()});
        }

    }


    private static void checkNSRecurseCheckCardinality(List<XSParticleDecl> children, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            XSParticleDecl wildcard, int min2, int max2,
            boolean checkWCOccurrence)
        throws XMLSchemaException {


        // check Occurrence ranges
        if (checkWCOccurrence && !checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-NSRecurseCheckCardinality.2", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        // Check that each member of the group is a valid restriction of the wildcard
        int count = children.size();
        try {
            for (int i = 0; i < count; i++) {
                XSParticleDecl particle1 = children.get(i);
                particleValidRestriction(particle1, dSGHandler, wildcard, null, false);

            }
        }
        // REVISIT: should we really just ignore original cause of this error?
        //          how can we report it?
        catch (XMLSchemaException e) {
            throw new XMLSchemaException("rcase-NSRecurseCheckCardinality.1", null);
        }

    }

    private static void checkRecurse(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {

        // check Occurrence ranges
        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-Recurse.1", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                    Integer.toString(min2),
                    max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1= dChildren.size();
        int count2= bChildren.size();

        int current = 0;
        label: for (int i = 0; i<count1; i++) {

            XSParticleDecl particle1 = dChildren.get(i);
            for (int j = current; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                current +=1;
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    continue label;
                }
                catch (XMLSchemaException e) {
                    if (!particle2.emptiable())
                        throw new XMLSchemaException("rcase-Recurse.2", null);
                }
            }
            throw new XMLSchemaException("rcase-Recurse.2", null);
        }

        // Now, see if there are some elements in the base we didn't match up
        for (int j=current; j < count2; j++) {
            XSParticleDecl particle2 = bChildren.get(j);
            if (!particle2.emptiable()) {
                throw new XMLSchemaException("rcase-Recurse.2", null);
            }
        }

    }

    private static void checkRecurseUnordered(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {


        // check Occurrence ranges
        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-RecurseUnordered.1", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                    Integer.toString(min2),
                    max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1= dChildren.size();
        int count2 = bChildren.size();

        boolean foundIt[] = new boolean[count2];

        label: for (int i = 0; i<count1; i++) {
            XSParticleDecl particle1 = dChildren.get(i);

            for (int j = 0; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    if (foundIt[j])
                        throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
                    else
                        foundIt[j]=true;

                    continue label;
                }
                catch (XMLSchemaException e) {
                }
            }
            // didn't find a match.  Detect an error
            throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
        }

        // Now, see if there are some elements in the base we didn't match up
        for (int j=0; j < count2; j++) {
            XSParticleDecl particle2 = bChildren.get(j);
            if (!foundIt[j] && !particle2.emptiable()) {
                throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
            }
        }

    }

    private static void checkRecurseLax(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler  bSGHandler)
        throws XMLSchemaException {

        // check Occurrence ranges
        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-RecurseLax.1", new Object[]{
                    Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1= dChildren.size();
        int count2 = bChildren.size();

        int current = 0;
        label: for (int i = 0; i<count1; i++) {

            XSParticleDecl particle1 = dChildren.get(i);
            for (int j = current; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                current +=1;
                try {
                    // IHR: go back one element on b list because the next element may match
                    // this as well.
                    if (particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler))
                        current--;
                    continue label;
                }
                catch (XMLSchemaException e) {
                }
            }
            // didn't find a match.  Detect an error
            throw new XMLSchemaException("rcase-RecurseLax.2", null);

        }

    }

    private static void checkMapAndSum(List<XSParticleDecl> dChildren, int min1, int max1,
            SubstitutionGroupHandler dSGHandler,
            List<XSParticleDecl> bChildren, int min2, int max2,
            SubstitutionGroupHandler bSGHandler)
        throws XMLSchemaException {

        // See if the sequence group is a valid restriction of the choice

        // Here is an example of a valid restriction:
        //   <choice minOccurs="2">
        //       <a/>
        //       <b/>
        //       <c/>
        //   </choice>
        //
        //   <sequence>
        //        <b/>
        //        <a/>
        //   </sequence>

        // check Occurrence ranges
        if (!checkOccurrenceRange(min1,max1,min2,max2)) {
            throw new XMLSchemaException("rcase-MapAndSum.2",
                    new Object[]{Integer.toString(min1),
                    max1==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max1),
                            Integer.toString(min2),
                            max2==SchemaSymbols.OCCURRENCE_UNBOUNDED?"unbounded":Integer.toString(max2)});
        }

        int count1 = dChildren.size();
        int count2 = bChildren.size();

        label: for (int i = 0; i<count1; i++) {

            XSParticleDecl particle1 = dChildren.get(i);
            for (int j = 0; j<count2; j++) {
                XSParticleDecl particle2 = bChildren.get(j);
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    continue label;
                }
                catch (XMLSchemaException e) {
                }
            }
            // didn't find a match.  Detect an error
            throw new XMLSchemaException("rcase-MapAndSum.1", null);
        }
    }
    // to check whether two element overlap, as defined in constraint UPA
    public static boolean overlapUPA(XSElementDecl element1,
            XSElementDecl element2,
            SubstitutionGroupHandler sgHandler) {
        // if the two element have the same name and namespace,
        if (element1.fName == element2.fName &&
                element1.fTargetNamespace == element2.fTargetNamespace) {
            return true;
        }

        // or if there is an element decl in element1's substitution group,
        // who has the same name/namespace with element2
        XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(element1);
        for (int i = subGroup.length-1; i >= 0; i--) {
            if (subGroup[i].fName == element2.fName &&
                    subGroup[i].fTargetNamespace == element2.fTargetNamespace) {
                return true;
            }
        }

        // or if there is an element decl in element2's substitution group,
        // who has the same name/namespace with element1
        subGroup = sgHandler.getSubstitutionGroup(element2);
        for (int i = subGroup.length-1; i >= 0; i--) {
            if (subGroup[i].fName == element1.fName &&
                    subGroup[i].fTargetNamespace == element1.fTargetNamespace) {
                return true;
            }
        }

        return false;
    }

    // to check whether an element overlaps with a wildcard,
    // as defined in constraint UPA
    public static boolean overlapUPA(XSElementDecl element,
            XSWildcardDecl wildcard,
            SubstitutionGroupHandler sgHandler) {
        // if the wildcard allows the element
        if (wildcard.allowNamespace(element.fTargetNamespace))
            return true;

        // or if the wildcard allows any element in the substitution group
        XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(element);
        for (int i = subGroup.length-1; i >= 0; i--) {
            if (wildcard.allowNamespace(subGroup[i].fTargetNamespace))
                return true;
        }

        return false;
    }

    public static boolean overlapUPA(XSWildcardDecl wildcard1,
            XSWildcardDecl wildcard2) {
        // if the intersection of the two wildcard is not empty list
        XSWildcardDecl intersect = wildcard1.performIntersectionWith(wildcard2, wildcard1.fProcessContents);
        if (intersect == null ||
                intersect.fType != XSWildcardDecl.NSCONSTRAINT_LIST ||
                intersect.fNamespaceList.length != 0) {
            return true;
        }

        return false;
    }

    // call one of the above methods according to the type of decls
    public static boolean overlapUPA(Object decl1, Object decl2,
            SubstitutionGroupHandler sgHandler) {
        if (decl1 instanceof XSElementDecl) {
            if (decl2 instanceof XSElementDecl) {
                return overlapUPA((XSElementDecl)decl1,
                        (XSElementDecl)decl2,
                        sgHandler);
            }
            else {
                return overlapUPA((XSElementDecl)decl1,
                        (XSWildcardDecl)decl2,
                        sgHandler);
            }
        }
        else {
            if (decl2 instanceof XSElementDecl) {
                return overlapUPA((XSElementDecl)decl2,
                        (XSWildcardDecl)decl1,
                        sgHandler);
            }
            else {
                return overlapUPA((XSWildcardDecl)decl1,
                        (XSWildcardDecl)decl2);
            }
        }
    }

} // class XSContraints
