/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * To store and validate information about substitutionGroup
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: July 2019
 */
public class SubstitutionGroupHandler {

    private static final XSElementDecl[] EMPTY_GROUP = new XSElementDecl[0];

    // global element declaration resolver
    private final XSElementDeclHelper fXSElementDeclHelper;

    /**
     * Default constructor
     */
    public SubstitutionGroupHandler(XSElementDeclHelper elementDeclHelper) {
        fXSElementDeclHelper = elementDeclHelper;
    }

    // 3.9.4 Element Sequence Locally Valid (Particle) 2.3.3
    // check whether one element decl matches an element with the given qname
    public XSElementDecl getMatchingElemDecl(QName element, XSElementDecl exemplar) {
        if (Objects.equals(element.localpart, exemplar.fName) &&
            Objects.equals(element.uri, exemplar.fTargetNamespace)) {
            return exemplar;
        }

        // if the exemplar is not a global element decl, then it's not possible
        // to be substituted by another element.
        if (exemplar.fScope != XSConstants.SCOPE_GLOBAL) {
            return null;
        }

        // if the decl blocks substitution, return false
        if ((exemplar.fBlock & XSConstants.DERIVATION_SUBSTITUTION) != 0) {
            return null;
        }

        // get the decl for the element
        XSElementDecl eDecl = fXSElementDeclHelper.getGlobalElementDecl(element);
        if (eDecl == null) {
            return null;
        }

        // and check by using substitutionGroup information
        if (substitutionGroupOK(eDecl, exemplar, exemplar.fBlock)) {
            return eDecl;
        }

        return null;
    }

    // 3.3.6 Substitution Group OK (Transitive)
    // check whether element can substitute exemplar
    protected boolean substitutionGroupOK(XSElementDecl element, XSElementDecl exemplar, short blockingConstraint) {
        // For an element declaration (call it D) to be validly substitutable for another element declaration (call it C) subject to a blocking constraint (a subset of {substitution, extension, restriction}, the value of a {disallowed substitutions}) one of the following must be true:
        // 1. D and C are the same element declaration.
        if (element == exemplar) {
            return true;
        }

        // 2 All of the following must be true:
        // 2.1 The blocking constraint does not contain substitution.
        if ((blockingConstraint & XSConstants.DERIVATION_SUBSTITUTION) != 0) {
            return false;
        }

        // 2.2 There is a chain of {substitution group affiliation}s from D to C, that is, either D's {substitution group affiliation} is C, or D's {substitution group affiliation}'s {substitution group affiliation} is C, or . . .
        XSElementDecl subGroup = element.fSubGroup;
        while (subGroup != null && subGroup != exemplar) {
            subGroup = subGroup.fSubGroup;
        }

        if (subGroup == null) {
            return false;
        }

        // 2.3 The set of all {derivation method}s involved in the derivation of D's {type definition} from C's {type definition} does not intersect with the union of the blocking constraint, C's {prohibited substitutions} (if C is complex, otherwise the empty set) and the {prohibited substitutions} (respectively the empty set) of any intermediate {type definition}s in the derivation of D's {type definition} from C's {type definition}.
        // prepare the combination of {derivation method} and
        // {disallowed substitution}
        return typeDerivationOK(element.fType, exemplar.fType, blockingConstraint);
    }

    private boolean typeDerivationOK(XSTypeDefinition derived, XSTypeDefinition base, short blockingConstraint) {

        short devMethod = 0, blockConstraint = blockingConstraint;

        // "derived" should be derived from "base"
        // add derivation methods of derived types to devMethod;
        // add block of base types to blockConstraint.
        XSTypeDefinition type = derived;
        while (type != base && type != SchemaGrammar.fAnyType) {
            if (type.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                devMethod |= ((XSComplexTypeDecl)type).fDerivedBy;
            }
            else {
                devMethod |= XSConstants.DERIVATION_RESTRICTION;
            }
            type = type.getBaseType();
            // type == null means the current type is anySimpleType,
            // whose base type should be anyType
            if (type == null) {
                type = SchemaGrammar.fAnyType;
            }
            if (type.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                blockConstraint |= ((XSComplexTypeDecl)type).fBlock;
            }
        }
        if (type != base) {
            // If the base is a union, check if "derived" is allowed through any of the member types.
            if (base.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                XSSimpleTypeDefinition st = (XSSimpleTypeDefinition) base;
                if (st.getVariety() ==  XSSimpleTypeDefinition.VARIETY_UNION) {
                    XSObjectList memberTypes = st.getMemberTypes();
                    final int length = memberTypes.getLength();
                    for (int i = 0; i < length; ++i) {
                        if (typeDerivationOK(derived, (XSTypeDefinition) memberTypes.item(i), blockingConstraint)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        if ((devMethod & blockConstraint) != 0) {
            return false;
        }
        return true;
    }

    // check whether element is in exemplar's substitution group
    public boolean inSubstitutionGroup(XSElementDecl element, XSElementDecl exemplar) {
        // [Definition:]  Every element declaration (call this HEAD) in the {element declarations} of a schema defines a substitution group, a subset of those {element declarations}, as follows:
        // Define PSG, the potential substitution group for HEAD, as follows:
        // 1 The element declaration itself is in PSG;
        // 2 PSG is closed with respect to {substitution group affiliation}, that is, if any element declaration in the {element declarations} has a {substitution group affiliation} in PSG, then it is also in PSG itself.
        // HEAD's actual substitution group is then the set consisting of each member of PSG such that all of the following must be true:
        // 1 Its {abstract} is false.
        // 2 It is validly substitutable for HEAD subject to an empty blocking constraint, as defined in Substitution Group OK (Transitive) (3.3.6).
        return substitutionGroupOK(element, exemplar, exemplar.fBlock);
    }

    // to store substitution group information
    // the key to the map is an element decl, and the value is
    // - a Vector, which contains all elements that has this element as their
    //   substitution group affilication
    // - an array of OneSubGroup, which contains its substitution group before block.
    Map<XSElementDecl, Object> fSubGroupsB = new HashMap<>();
    private static final OneSubGroup[] EMPTY_VECTOR = new OneSubGroup[0];
    // The real substitution groups (after "block")
    Map<XSElementDecl, XSElementDecl[]> fSubGroups = new HashMap<>();

    /**
     * clear the internal registry of substitutionGroup information
     */
    public void reset() {
        fSubGroupsB.clear();
        fSubGroups.clear();
    }

    /**
     * add a list of substitution group information.
     */
    @SuppressWarnings("unchecked")
    public void addSubstitutionGroup(XSElementDecl[] elements) {
        XSElementDecl subHead, element;
        List<XSElementDecl> subGroup;
        // for all elements with substitution group affiliation
        for (int i = elements.length-1; i >= 0; i--) {
            element = elements[i];
            subHead = element.fSubGroup;
            // check whether this an entry for this element
            subGroup = (List<XSElementDecl>)fSubGroupsB.get(subHead);
            if (subGroup == null) {
                // if not, create a new one
                subGroup = new ArrayList<>();
                fSubGroupsB.put(subHead, subGroup);
            }
            // add to the vactor
            subGroup.add(element);
        }
    }

    /**
     * get all elements that can substitute the given element,
     * according to the spec, we shouldn't consider the {block} constraints.
     *
     * from the spec, substitution group of a given element decl also contains
     * the element itself. but the array returned from this method doesn't
     * containt this element.
     */
    public XSElementDecl[] getSubstitutionGroup(XSElementDecl element) {
        // If we already have sub group for this element, just return it.
        XSElementDecl[] subGroup = fSubGroups.get(element);
        if (subGroup != null)
            return subGroup;

        if ((element.fBlock & XSConstants.DERIVATION_SUBSTITUTION) != 0) {
            fSubGroups.put(element, EMPTY_GROUP);
            return EMPTY_GROUP;
        }

        // Otherwise, get all potential sub group elements
        // (without considering "block" on this element
        OneSubGroup[] groupB = getSubGroupB(element, new OneSubGroup());
        int len = groupB.length, rlen = 0;
        XSElementDecl[] ret = new XSElementDecl[len];
        // For each of such elements, check whether the derivation methods
        // overlap with "block". If not, add it to the sub group
        for (int i = 0 ; i < len; i++) {
            if ((element.fBlock & groupB[i].dMethod) == 0)
                ret[rlen++] = groupB[i].sub;
        }
        // Resize the array if necessary
        if (rlen < len) {
            XSElementDecl[] ret1 = new XSElementDecl[rlen];
            System.arraycopy(ret, 0, ret1, 0, rlen);
            ret = ret1;
        }
        // Store the subgroup
        fSubGroups.put(element, ret);

        return ret;
    }

    // Get potential sub group element (without considering "block")
    private OneSubGroup[] getSubGroupB(XSElementDecl element, OneSubGroup methods) {
        Object subGroup = fSubGroupsB.get(element);

        // substitution group for this one is empty
        if (subGroup == null) {
            fSubGroupsB.put(element, EMPTY_VECTOR);
            return EMPTY_VECTOR;
        }

        // we've already calculated the element, just return.
        if (subGroup instanceof OneSubGroup[])
            return (OneSubGroup[])subGroup;

        // we only have the *direct* substitutions
        @SuppressWarnings("unchecked")
        List<XSElementDecl> group = (ArrayList<XSElementDecl>)subGroup;
        List<OneSubGroup> newGroup = new ArrayList<>();
        OneSubGroup[] group1;
        // then for each of the direct substitutions, get its substitution
        // group, and combine the groups together.
        short dMethod, bMethod, dSubMethod, bSubMethod;
        for (int i = group.size()-1, j; i >= 0; i--) {
            // Check whether this element is blocked. If so, ignore it.
            XSElementDecl sub = group.get(i);
            if (!getDBMethods(sub.fType, element.fType, methods))
                continue;
            // Remember derivation methods and blocks from the types
            dMethod = methods.dMethod;
            bMethod = methods.bMethod;
            // Add this one to potential group
            newGroup.add(new OneSubGroup(sub, methods.dMethod, methods.bMethod));
            // Get potential group for this element
            group1 = getSubGroupB(sub, methods);
            for (j = group1.length-1; j >= 0; j--) {
                // For each of them, check whether it's blocked (by type)
                dSubMethod = (short)(dMethod | group1[j].dMethod);
                bSubMethod = (short)(bMethod | group1[j].bMethod);
                // Ignore it if it's blocked
                if ((dSubMethod & bSubMethod) != 0)
                    continue;
                newGroup.add(new OneSubGroup(group1[j].sub, dSubMethod, bSubMethod));
            }
        }
        // Convert to an array
        OneSubGroup[] ret = new OneSubGroup[newGroup.size()];
        for (int i = newGroup.size()-1; i >= 0; i--) {
            ret[i] = newGroup.get(i);
        }
        // Store the potential sub group
        fSubGroupsB.put(element, ret);

        return ret;
    }

    private boolean getDBMethods(XSTypeDefinition typed, XSTypeDefinition typeb,
                                 OneSubGroup methods) {
        short dMethod = 0, bMethod = 0;
        while (typed != typeb && typed != SchemaGrammar.fAnyType) {
            if (typed.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
                dMethod |= ((XSComplexTypeDecl)typed).fDerivedBy;
            else
                dMethod |= XSConstants.DERIVATION_RESTRICTION;
            typed = typed.getBaseType();
            // type == null means the current type is anySimpleType,
            // whose base type should be anyType
            if (typed == null)
                typed = SchemaGrammar.fAnyType;
            if (typed.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
                bMethod |= ((XSComplexTypeDecl)typed).fBlock;
        }
        // No derivation relation, or blocked, return false
        if (typed != typeb || (dMethod & bMethod) != 0)
            return false;

        // Remember the derivation methods and blocks, return true.
        methods.dMethod = dMethod;
        methods.bMethod = bMethod;
        return true;
    }

    // Record the information about how one element substitute another one
    private static final class OneSubGroup {
        OneSubGroup() {}
        OneSubGroup(XSElementDecl sub, short dMethod, short bMethod) {
            this.sub = sub;
            this.dMethod = dMethod;
            this.bMethod = bMethod;
        }
        // The element that substitutes another one
        XSElementDecl sub;
        // The combination of all derivation methods from sub's type to
        // the head's type
        short dMethod;
        // The combination of {block} of the types in the derivation chain
        // excluding sub's type
        short bMethod;
    }
} // class SubstitutionGroupHandler
