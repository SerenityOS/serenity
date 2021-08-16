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

package com.sun.org.apache.xerces.internal.impl.dv;

import com.sun.org.apache.xerces.internal.impl.xs.util.ShortListImpl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.xs.ShortList;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSValue;

/**
 * Class to get the information back after content is validated. This info
 * would be filled by validate().
 *
 * @xerces.internal
 *
 * @author Neeraj Bajaj, Sun Microsystems, inc.
 *
 */
public class ValidatedInfo implements XSValue {

    /**
     * The normalized value of a string value
     */
    public String normalizedValue;

    /**
     * The actual value from a string value (QName, Boolean, etc.)
     * An array of Objects if the type is a list.
     */
    public Object actualValue;

    /**
     * The type of the actual value. It's one of the _DT constants
     * defined in XSConstants.java. The value is used to indicate
     * the most specific built-in type.
     * (i.e. short instead of decimal or integer).
     */
    public short actualValueType;

    /**
     * The declared type of the value.
     */
    public XSSimpleType actualType;

    /**
     * If the type is a union type, then the member type which
     * actually validated the string value.
     */
    public XSSimpleType memberType;

    /**
     * If
     * 1. the type is a union type where one of the member types is a list, or
     *    if the type is a list; and
     * 2. the item type of the list is a union type
     * then an array of member types used to validate the values.
     */
    public XSSimpleType[] memberTypes;

    /**
     * In the case the value is a list or a list of unions, this value
     * indicates the type(s) of the items in the list.
     * For a normal list, the length of the array is 1; for list of unions,
     * the length of the array is the same as the length of the list.
     */
    public ShortList itemValueTypes;

    /**
     * reset the state of this object
     */
    public void reset() {
        this.normalizedValue = null;
        this.actualValue = null;
        this.actualValueType = XSConstants.UNAVAILABLE_DT;
        this.actualType = null;
        this.memberType = null;
        this.memberTypes = null;
        this.itemValueTypes = null;
    }

    /**
     * Return a string representation of the value. If there is an actual
     * value, use toString; otherwise, use the normalized value.
     */
    public String stringValue() {
        if (actualValue == null) {
            return normalizedValue;
        }
        else {
            return actualValue.toString();
        }
    }

    /**
     * Returns true if the two ValidatedInfo objects can be compared in the same
     * value space.
     */
    public static boolean isComparable(ValidatedInfo info1, ValidatedInfo info2) {
        final short primitiveType1 = convertToPrimitiveKind(info1.actualValueType);
        final short primitiveType2 = convertToPrimitiveKind(info2.actualValueType);
        if (primitiveType1 != primitiveType2) {
            return (primitiveType1 == XSConstants.ANYSIMPLETYPE_DT && primitiveType2 == XSConstants.STRING_DT ||
                    primitiveType1 == XSConstants.STRING_DT && primitiveType2 == XSConstants.ANYSIMPLETYPE_DT);
        }
        else if (primitiveType1 == XSConstants.LIST_DT || primitiveType1 == XSConstants.LISTOFUNION_DT) {
            final ShortList typeList1 = info1.itemValueTypes;
            final ShortList typeList2 = info2.itemValueTypes;
            final int typeList1Length = typeList1 != null ? typeList1.getLength() : 0;
            final int typeList2Length = typeList2 != null ? typeList2.getLength() : 0;
            if (typeList1Length != typeList2Length) {
                return false;
            }
            for (int i = 0; i < typeList1Length; ++i) {
                final short primitiveItem1 = convertToPrimitiveKind(typeList1.item(i));
                final short primitiveItem2 = convertToPrimitiveKind(typeList2.item(i));
                if (primitiveItem1 != primitiveItem2) {
                    if (primitiveItem1 == XSConstants.ANYSIMPLETYPE_DT && primitiveItem2 == XSConstants.STRING_DT ||
                        primitiveItem1 == XSConstants.STRING_DT && primitiveItem2 == XSConstants.ANYSIMPLETYPE_DT) {
                        continue;
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the primitive type of the given type.
     * @param valueType A value type as defined in XSConstants.
     * @return The primitive type from which valueType was derived.
     */
    private static short convertToPrimitiveKind(short valueType) {
        /** Primitive datatypes. */
        if (valueType <= XSConstants.NOTATION_DT) {
            return valueType;
        }
        /** Types derived from string. */
        if (valueType <= XSConstants.ENTITY_DT) {
            return XSConstants.STRING_DT;
        }
        /** Types derived from decimal. */
        if (valueType <= XSConstants.POSITIVEINTEGER_DT) {
            return XSConstants.DECIMAL_DT;
        }
        /** Other types. */
        return valueType;
    }

    // XSValue methods

    public Object getActualValue() {
        return actualValue;
    }

    public short getActualValueType() {
        return actualValueType;
    }

    public ShortList getListValueTypes() {
        return itemValueTypes == null ? ShortListImpl.EMPTY_LIST : itemValueTypes;
    }

    public XSObjectList getMemberTypeDefinitions() {
        if (memberTypes == null) {
            return XSObjectListImpl.EMPTY_LIST;
        }
        return new XSObjectListImpl(memberTypes, memberTypes.length);
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public XSSimpleTypeDefinition getTypeDefinition() {
        return actualType;
    }

    public XSSimpleTypeDefinition getMemberTypeDefinition() {
        return memberType;
    }

    public void copyFrom(XSValue o) {
        if (o == null) {
            reset();
        }
        else if (o instanceof ValidatedInfo) {
            ValidatedInfo other = (ValidatedInfo)o;
            normalizedValue = other.normalizedValue;
            actualValue = other.actualValue;
            actualValueType = other.actualValueType;
            actualType = other.actualType;
            memberType = other.memberType;
            memberTypes = other.memberTypes;
            itemValueTypes = other.itemValueTypes;
        }
        else {
            normalizedValue = o.getNormalizedValue();
            actualValue = o.getActualValue();
            actualValueType = o.getActualValueType();
            actualType = (XSSimpleType)o.getTypeDefinition();
            memberType = (XSSimpleType)o.getMemberTypeDefinition();
            XSSimpleType realType = memberType == null ? actualType : memberType;
            if (realType != null && realType.getBuiltInKind() == XSConstants.LISTOFUNION_DT) {
                XSObjectList members = o.getMemberTypeDefinitions();
                memberTypes = new XSSimpleType[members.getLength()];
                for (int i = 0; i < members.getLength(); i++) {
                    memberTypes[i] = (XSSimpleType)members.get(i);
                }
            }
            else {
                memberTypes = null;
            }
            itemValueTypes = o.getListValueTypes();
        }
    }
}
