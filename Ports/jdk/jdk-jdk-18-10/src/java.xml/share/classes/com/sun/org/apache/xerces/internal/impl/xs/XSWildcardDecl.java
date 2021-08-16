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

package com.sun.org.apache.xerces.internal.impl.xs;

import com.sun.org.apache.xerces.internal.impl.xs.util.StringListImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.org.apache.xerces.internal.xs.XSAnnotation;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSNamespaceItem;
import com.sun.org.apache.xerces.internal.xs.XSWildcard;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;

/**
 * The XML representation for a wildcard declaration
 * schema component is an <any> or <anyAttribute> element information item
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 * @author Rahul Srivastava, Sun Microsystems Inc.
 *
 */
public class XSWildcardDecl implements XSWildcard {

    public static final String ABSENT = null;

    // the type of wildcard: any, other, or list
    public short fType = NSCONSTRAINT_ANY;
    // the type of process contents: strict, lax, or skip
    public short fProcessContents = PC_STRICT;
    // the namespace list:
    // for NSCONSTRAINT_LIST, it means one of the namespaces in the list
    // for NSCONSTRAINT_NOT, it means not any of the namespaces in the list
    public String[] fNamespaceList;

    // optional annotation
    public XSObjectList fAnnotations = null;

    // I'm trying to implement the following constraint exactly as what the
    // spec describes. Sometimes it seems redundant, and sometimes there seems
    // to be much easier solutions. But it makes it easy to understand,
    // easy to maintain, and easy to find a bug (either in the code, or in the
    // spec). -SG
    //
    // NOTE: Schema spec only requires that ##other not(tNS,absent).
    //       The way we store ##other is not(NS1,NS2,...,NSN), which covers
    //       what's required by Schema, and allows future enhanced features.
    //
    // In the following in-line comments:
    // - Bullet removed from w3c specification.
    // + Bullet added as proposed by Sandy Gao, IBM.
    // / Since we store ##other as not(NS1,NS2,...,NSN), we need to put some
    //   comments on where we didn't follow the spec exactly.
    // * When we really support not(NS1,NS2,...,NSN), we need to revisit these items.

    /**
     * Validation Rule: Wildcard allows Namespace Name
     */
    public boolean allowNamespace(String namespace) {
        // For a value which is either a namespace name or absent to be valid with respect to a wildcard constraint (the value of a {namespace constraint}) one of the following must be true:

        // 1 The constraint must be any.
        if (fType == NSCONSTRAINT_ANY)
            return true;

        // 2 All of the following must be true:
        // 2.1 The constraint is a pair of not and a namespace name or absent ([Definition:]  call this the namespace test).
        // 2.2 The value must not be identical to the namespace test.
        // 2.3 The value must not be absent.
        // / we store ##other as not(list), so our actual rule is
        // / 2 The constraint is a pair of not and a set, and the value is not in such set.
        if (fType == NSCONSTRAINT_NOT) {
            boolean found = false;
            int listNum = fNamespaceList.length;
            for (int i = 0; i < listNum && !found; i++) {
                if (namespace == fNamespaceList[i])
                    found = true;
            }

            if (!found)
                return true;
        }

        // 3 The constraint is a set, and the value is identical to one of the members of the set.
        if (fType == NSCONSTRAINT_LIST) {
            int listNum = fNamespaceList.length;
            for (int i = 0; i < listNum; i++) {
                if (namespace == fNamespaceList[i])
                    return true;
            }
        }

        // none of the above conditions applied, so return false.
        return false;
    }

    /**
     *  Schema Component Constraint: Wildcard Subset
     */
    public boolean isSubsetOf(XSWildcardDecl superWildcard) {
        // if the super is null (not expressible), return false
        if (superWildcard == null)
            return false;

        // For a namespace constraint (call it sub) to be an intensional subset of another
        // namespace constraint (call it super) one of the following must be true:

        // 1 super must be any.
        if (superWildcard.fType == NSCONSTRAINT_ANY) {
            return true;
        }

        // 2 All of the following must be true:
        //   2.1 sub must be a pair of not and a namespace name or absent.
        //   2.2 super must be a pair of not and the same value.
        //   * we can't just compare whether the namespace are the same value
        //     since we store other as not(list)
        if (fType == NSCONSTRAINT_NOT) {
            if (superWildcard.fType == NSCONSTRAINT_NOT &&
                fNamespaceList[0] == superWildcard.fNamespaceList[0]) {
                return true;
            }
        }

        // 3 All of the following must be true:
        //   3.1 sub must be a set whose members are either namespace names or absent.
        //   3.2 One of the following must be true:
        //       3.2.1 super must be the same set or a superset thereof.
        //       -3.2.2 super must be a pair of not and a namespace name or absent and
        //              that value must not be in sub's set.
        //       +3.2.2 super must be a pair of not and a namespace name or absent and
        //              either that value or absent must not be in sub's set.
        //       * since we store ##other as not(list), we acturally need to make sure
        //         that none of the namespaces in super.list is in sub.list.
        if (fType == NSCONSTRAINT_LIST) {
            if (superWildcard.fType == NSCONSTRAINT_LIST &&
                subset2sets(fNamespaceList, superWildcard.fNamespaceList)) {
                return true;
            }

            if (superWildcard.fType == NSCONSTRAINT_NOT &&
                !elementInSet(superWildcard.fNamespaceList[0], fNamespaceList) &&
                !elementInSet(ABSENT, fNamespaceList)) {
                return true;
            }
        }

        // none of the above conditions applied, so return false.
        return false;

    } // isSubsetOf

    /**
     * Check whether this wildcard has a weaker process contents than the super.
     */
    public boolean weakerProcessContents(XSWildcardDecl superWildcard) {
        return fProcessContents == XSWildcardDecl.PC_LAX &&
               superWildcard.fProcessContents == XSWildcardDecl.PC_STRICT ||
               fProcessContents == XSWildcardDecl.PC_SKIP &&
               superWildcard.fProcessContents != XSWildcardDecl.PC_SKIP;
    }

    /**
     * Schema Component Constraint: Attribute Wildcard Union
     */
    public XSWildcardDecl performUnionWith(XSWildcardDecl wildcard,
                                           short processContents) {
        // if the other wildcard is not expressible, the result is still not expressible
        if (wildcard == null)
            return null;

        // For a wildcard's {namespace constraint} value to be the intensional union of two
        // other such values (call them O1 and O2): the appropriate case among the following
        // must be true:

        XSWildcardDecl unionWildcard = new XSWildcardDecl();
        unionWildcard.fProcessContents = processContents;

        // 1 If O1 and O2 are the same value, then that value must be the value.
        if (areSame(wildcard)) {
            unionWildcard.fType = fType;
            unionWildcard.fNamespaceList = fNamespaceList;
        }

        // 2 If either O1 or O2 is any, then any must be the value.
        else if ( (fType == NSCONSTRAINT_ANY) || (wildcard.fType == NSCONSTRAINT_ANY) ) {
            unionWildcard.fType = NSCONSTRAINT_ANY;
        }

        // 3 If both O1 and O2 are sets of (namespace names or absent), then the union of
        //   those sets must be the value.
        else if ( (fType == NSCONSTRAINT_LIST) && (wildcard.fType == NSCONSTRAINT_LIST) ) {
            unionWildcard.fType = NSCONSTRAINT_LIST;
            unionWildcard.fNamespaceList = union2sets(fNamespaceList, wildcard.fNamespaceList);
        }

        // -4 If the two are negations of different namespace names, then the intersection
        //    is not expressible.
        // +4 If the two are negations of different namespace names or absent, then
        //    a pair of not and absent must be the value.
        // * now we store ##other as not(list), the result should be
        //   not(intersection of two lists).
        else if (fType == NSCONSTRAINT_NOT && wildcard.fType == NSCONSTRAINT_NOT) {
            unionWildcard.fType = NSCONSTRAINT_NOT;
            unionWildcard.fNamespaceList = new String[2];
            unionWildcard.fNamespaceList[0] = ABSENT;
            unionWildcard.fNamespaceList[1] = ABSENT;
        }

        // 5 If either O1 or O2 is a pair of not and a namespace name and the other is a set of
        //   (namespace names or absent), then The appropriate case among the following must be true:
        //      -5.1 If the set includes the negated namespace name, then any must be the value.
        //      -5.2 If the set does not include the negated namespace name, then whichever of O1 or O2
        //           is a pair of not and a namespace name must be the value.
        //    +5.1 If the negated value is a namespace name, then The appropriate case
        //         among the following must be true:
        //        +5.1.1 If the set includes both the namespace name and absent, then any
        //               must be the value.
        //        +5.1.2 If the set includes the namespace name but does not include
        //               absent, then a pair of not and absent must be the value.
        //        +5.1.3 If the set does not include the namespace name but includes
        //               absent, then the union is not expressible.
        //        +5.1.4 If the set does not include either the namespace name or absent,
        //               then whichever of O1 or O2 is a pair of not and a namespace name must be
        //               the value.
        //    +5.2 If the negated value is absent, then The appropriate case among the
        //         following must be true:
        //        +5.2.1 If the set includes absent, then any must be the value.
        //        +5.2.2 If the set does not include absent, then whichever of O1 or O2 is
        //               a pair of not and a namespace name must be the value.
        // * when we have not(list), the operation is just not(otherlist-list)
        else if ( ((fType == NSCONSTRAINT_NOT) && (wildcard.fType == NSCONSTRAINT_LIST)) ||
                  ((fType == NSCONSTRAINT_LIST) && (wildcard.fType == NSCONSTRAINT_NOT)) ) {
            String[] other = null;
            String[] list = null;

            if (fType == NSCONSTRAINT_NOT) {
                other = fNamespaceList;
                list = wildcard.fNamespaceList;
            }
            else {
                other = wildcard.fNamespaceList;
                list = fNamespaceList;
            }

            boolean foundAbsent = elementInSet(ABSENT, list);

            if (other[0] != ABSENT) {
                boolean foundNS = elementInSet(other[0], list);
                if (foundNS && foundAbsent) {
                    unionWildcard.fType = NSCONSTRAINT_ANY;
                } else if (foundNS && !foundAbsent) {
                    unionWildcard.fType = NSCONSTRAINT_NOT;
                    unionWildcard.fNamespaceList = new String[2];
                    unionWildcard.fNamespaceList[0] = ABSENT;
                    unionWildcard.fNamespaceList[1] = ABSENT;
                } else if (!foundNS && foundAbsent) {
                    return null;
                } else { // !foundNS && !foundAbsent
                    unionWildcard.fType = NSCONSTRAINT_NOT;
                    unionWildcard.fNamespaceList = other;
                }
            } else { // other[0] == ABSENT
                if (foundAbsent) {
                    unionWildcard.fType = NSCONSTRAINT_ANY;
                } else { // !foundAbsent
                    unionWildcard.fType = NSCONSTRAINT_NOT;
                    unionWildcard.fNamespaceList = other;
                }
            }
        }

        return unionWildcard;

    } // performUnionWith

    /**
     * Schema Component Constraint: Attribute Wildcard Intersection
     */
    public XSWildcardDecl performIntersectionWith(XSWildcardDecl wildcard,
                                                  short processContents) {
        // if the other wildcard is not expressible, the result is still not expressible
        if (wildcard == null)
            return null;

        // For a wildcard's {namespace constraint} value to be the intensional intersection of
        // two other such values (call them O1 and O2): the appropriate case among the following
        // must be true:

        XSWildcardDecl intersectWildcard = new XSWildcardDecl();
        intersectWildcard.fProcessContents = processContents;

        // 1 If O1 and O2 are the same value, then that value must be the value.
        if (areSame(wildcard)) {
            intersectWildcard.fType = fType;
            intersectWildcard.fNamespaceList = fNamespaceList;
        }

        // 2 If either O1 or O2 is any, then the other must be the value.
        else if ( (fType == NSCONSTRAINT_ANY) || (wildcard.fType == NSCONSTRAINT_ANY) ) {
            // both cannot be ANY, if we have reached here.
            XSWildcardDecl other = this;

            if (fType == NSCONSTRAINT_ANY)
                other = wildcard;

            intersectWildcard.fType = other.fType;
            intersectWildcard.fNamespaceList = other.fNamespaceList;
        }

        // -3 If either O1 or O2 is a pair of not and a namespace name and the other is a set of
        //    (namespace names or absent), then that set, minus the negated namespace name if
        //    it was in the set, must be the value.
        // +3 If either O1 or O2 is a pair of not and a namespace name and the other
        //    is a set of (namespace names or absent), then that set, minus the negated
        //    namespace name if it was in the set, then minus absent if it was in the
        //    set, must be the value.
        // * when we have not(list), the operation is just list-otherlist
        else if ( ((fType == NSCONSTRAINT_NOT) && (wildcard.fType == NSCONSTRAINT_LIST)) ||
                  ((fType == NSCONSTRAINT_LIST) && (wildcard.fType == NSCONSTRAINT_NOT)) ) {
            String[] list = null;
            String[] other = null;

            if (fType == NSCONSTRAINT_NOT) {
                other = fNamespaceList;
                list = wildcard.fNamespaceList;
            }
            else {
                other = wildcard.fNamespaceList;
                list = fNamespaceList;
            }

            int listSize = list.length;
            String[] intersect = new String[listSize];
            int newSize = 0;
            for (int i = 0; i < listSize; i++) {
                if (list[i] != other[0] && list[i] != ABSENT)
                    intersect[newSize++] = list[i];
            }

            intersectWildcard.fType = NSCONSTRAINT_LIST;
            intersectWildcard.fNamespaceList = new String[newSize];
            System.arraycopy(intersect, 0, intersectWildcard.fNamespaceList, 0, newSize);
        }

        // 4 If both O1 and O2 are sets of (namespace names or absent), then the intersection of those
        //   sets must be the value.
        else if ( (fType == NSCONSTRAINT_LIST) && (wildcard.fType == NSCONSTRAINT_LIST) ) {
            intersectWildcard.fType = NSCONSTRAINT_LIST;
            intersectWildcard.fNamespaceList = intersect2sets(fNamespaceList, wildcard.fNamespaceList);
        }

        // -5 If the two are negations of different namespace names, then the intersection is not expressible.
        // +5 If the two are negations of namespace names or absent, then The
        //    appropriate case among the following must be true:
        //    +5.1 If the two are negations of different namespace names, then the
        //         intersection is not expressible.
        //    +5.2 If one of the two is a pair of not and absent, the other must be
        //         the value.
        // * when we have not(list), the operation is just not(onelist+otherlist)
        else if (fType == NSCONSTRAINT_NOT && wildcard.fType == NSCONSTRAINT_NOT) {
            if (fNamespaceList[0] != ABSENT && wildcard.fNamespaceList[0] != ABSENT)
                return null;

            XSWildcardDecl other = this;
            if (fNamespaceList[0] == ABSENT)
                other = wildcard;

            intersectWildcard.fType = other.fType;
            intersectWildcard.fNamespaceList = other.fNamespaceList;
        }

        return intersectWildcard;

    } // performIntersectionWith

    private boolean areSame(XSWildcardDecl wildcard) {
        if (fType == wildcard.fType) {
            // ##any, true
            if (fType == NSCONSTRAINT_ANY)
                return true;

            // ##other, only check the negated value
            // * when we support not(list), we need to check in the same way
            //   as for NSCONSTRAINT_LIST.
            if (fType == NSCONSTRAINT_NOT)
                return fNamespaceList[0] == wildcard.fNamespaceList[0];

            // ## list, must have the same length,
            // and each item in one list must appear in the other one
            // (we are assuming that there are no duplicate items in a list)
            if (fNamespaceList.length == wildcard.fNamespaceList.length) {
                for (int i=0; i<fNamespaceList.length; i++) {
                    if (!elementInSet(fNamespaceList[i], wildcard.fNamespaceList))
                        return false;
                }
                return true;
            }
        }

        return false;
    } // areSame

    String[] intersect2sets(String[] one, String[] theOther){
        String[] result = new String[Math.min(one.length,theOther.length)];

        // simple implemention,
        int count = 0;
        for (int i=0; i<one.length; i++) {
            if (elementInSet(one[i], theOther))
                result[count++] = one[i];
        }

        String[] result2 = new String[count];
        System.arraycopy(result, 0, result2, 0, count);

        return result2;
    }

    String[] union2sets(String[] one, String[] theOther){
        String[] result1 = new String[one.length];

        // simple implemention,
        int count = 0;
        for (int i=0; i<one.length; i++) {
            if (!elementInSet(one[i], theOther))
                result1[count++] = one[i];
        }

        String[] result2 = new String[count+theOther.length];
        System.arraycopy(result1, 0, result2, 0, count);
        System.arraycopy(theOther, 0, result2, count, theOther.length);

        return result2;
    }

    boolean subset2sets(String[] subSet, String[] superSet){
        for (int i=0; i<subSet.length; i++) {
            if (!elementInSet(subSet[i], superSet))
                return false;
        }

        return true;
    }

    boolean elementInSet(String ele, String[] set){
        boolean found = false;
        for (int i=0; i<set.length && !found; i++) {
            if (ele==set[i])
                found = true;
        }

        return found;
    }

    /**
     * get the string description of this wildcard
     */
    private String fDescription = null;
    public String toString() {
        if (fDescription == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("WC[");
            switch (fType) {
            case NSCONSTRAINT_ANY:
                buffer.append(SchemaSymbols.ATTVAL_TWOPOUNDANY);
                break;
            case NSCONSTRAINT_NOT:
                buffer.append(SchemaSymbols.ATTVAL_TWOPOUNDOTHER);
                buffer.append(":\"");
                if (fNamespaceList[0] != null)
                    buffer.append(fNamespaceList[0]);
                buffer.append("\"");
                break;
            case NSCONSTRAINT_LIST:
                if (fNamespaceList.length == 0)
                    break;
                buffer.append("\"");
                if (fNamespaceList[0] != null)
                    buffer.append(fNamespaceList[0]);
                buffer.append("\"");
                for (int i = 1; i < fNamespaceList.length; i++) {
                    buffer.append(",\"");
                    if (fNamespaceList[i] != null)
                        buffer.append(fNamespaceList[i]);
                    buffer.append("\"");
                }
                break;
            }
            buffer.append(']');
            fDescription = buffer.toString();
        }

        return fDescription;
    }

    /**
     * Get the type of the object, i.e ELEMENT_DECLARATION.
     */
    public short getType() {
        return XSConstants.WILDCARD;
    }

    /**
     * The <code>name</code> of this <code>XSObject</code> depending on the
     * <code>XSObject</code> type.
     */
    public String getName() {
        return null;
    }

    /**
     * The namespace URI of this node, or <code>null</code> if it is
     * unspecified.  defines how a namespace URI is attached to schema
     * components.
     */
    public String getNamespace() {
        return null;
    }

    /**
     * Namespace constraint: A constraint type: any, not, list.
     */
    public short getConstraintType() {
        return fType;
    }

    /**
     * Namespace constraint. For <code>constraintType</code>
     * LIST_NSCONSTRAINT, the list contains allowed namespaces. For
     * <code>constraintType</code> NOT_NSCONSTRAINT, the list contains
     * disallowed namespaces.
     */
    public StringList getNsConstraintList() {
        return new StringListImpl(fNamespaceList, fNamespaceList == null ? 0 : fNamespaceList.length);
    }

    /**
     * {process contents} One of skip, lax or strict. Valid constants values
     * are: PC_SKIP, PC_LAX, PC_STRICT.
     */
    public short getProcessContents() {
        return fProcessContents;
    }

    /**
     * String valid of {process contents}. One of "skip", "lax" or "strict".
     */
    public String getProcessContentsAsString() {
        switch (fProcessContents) {
            case XSWildcardDecl.PC_SKIP: return "skip";
            case XSWildcardDecl.PC_LAX: return "lax";
            case XSWildcardDecl.PC_STRICT: return "strict";
            default: return "invalid value";
        }
    }

    /**
     * Optional. Annotation.
     */
    public XSAnnotation getAnnotation() {
        return (fAnnotations != null) ? (XSAnnotation) fAnnotations.item(0) : null;
    }

    /**
     * Optional. Annotations.
     */
    public XSObjectList getAnnotations() {
        return (fAnnotations != null) ? fAnnotations : XSObjectListImpl.EMPTY_LIST;
    }

    /**
     * @see org.apache.xerces.xs.XSObject#getNamespaceItem()
     */
    public XSNamespaceItem getNamespaceItem() {
        return null;
    }

} // class XSWildcardDecl
