/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug     4906359 4963461 4965058 4965039 4986770
 * @summary Unit test for annotation reading
 * @author  Josh Bloch
 */

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.*;
import java.util.*;
import java.lang.reflect.*;
import java.io.*;

public class UnitTest {
    private static final Class[] X = new Class[0];
    private static final Class[] Y = { int.class };

    static int numTests = 0;

    public static void main(String[] args) throws Exception {

        // *** TESTS ON ANNOTATED METHODS ***

        // MULTIMEMBER SCALAR TYPES ON METHOD
        checkScalarTypes(UnitTest.class.getMethod("scalarTypesMethod", X));
        checkScalarTypesOverrideDefault(UnitTest.class.getMethod("scalarTypesOverrideDefaultMethod", X));
        checkScalarTypesAcceptDefault(UnitTest.class.getMethod("scalarTypesAcceptDefaultMethod", X));

        // MULTIMEMBER ARRAY TYPES ON METHOD
        checkArrayTypes0(UnitTest.class.getMethod("emptyArrayTypesMethod", X));
        checkArrayTypes1(UnitTest.class.getMethod("singleElementArrayTypesMethod", X));
        checkArrayTypes2(UnitTest.class.getMethod("twoElementArrayTypesMethod", X));
        checkArrayTypesAcceptDefault(UnitTest.class.getMethod("arrayTypesAcceptDefaultMethod", X));
        checkArrayTypesOverrideDefault(UnitTest.class.getMethod("arrayTypesOverrideDefaultMethod", X));

        // MARKER TYPE ON METHOD
        checkMarker(UnitTest.class.getMethod("markerMethod", X));

        // SINGLE-MEMBER SCALAR TYPES ON METHOD
        checkSingleMemberByte(UnitTest.class.getMethod("SingleMemberByte", X));
        checkSingleMemberShort(UnitTest.class.getMethod("SingleMemberShort", X));
        checkSingleMemberInt(UnitTest.class.getMethod("SingleMemberInt", X));
        checkSingleMemberLong(UnitTest.class.getMethod("SingleMemberLong", X));
        checkSingleMemberChar(UnitTest.class.getMethod("SingleMemberChar", X));
        checkSingleMemberFloat(UnitTest.class.getMethod("SingleMemberFloat", X));
        checkSingleMemberDouble(UnitTest.class.getMethod("SingleMemberDouble", X));
        checkSingleMemberBoolean(UnitTest.class.getMethod("SingleMemberBoolean", X));
        checkSingleMemberString(UnitTest.class.getMethod("SingleMemberString", X));
        checkSingleMemberClass(UnitTest.class.getMethod("SingleMemberClass", X));
        checkSingleMemberEnum(UnitTest.class.getMethod("SingleMemberEnum", X));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE ON METHOD
        checkSingleMemberByteOvrdDef(UnitTest.class.getMethod("SingleMemberByteOvrdDef", X));
        checkSingleMemberShortOvrdDef(UnitTest.class.getMethod("SingleMemberShortOvrdDef", X));
        checkSingleMemberIntOvrdDef(UnitTest.class.getMethod("SingleMemberIntOvrdDef", X));
        checkSingleMemberLongOvrdDef(UnitTest.class.getMethod("SingleMemberLongOvrdDef", X));
        checkSingleMemberCharOvrdDef(UnitTest.class.getMethod("SingleMemberCharOvrdDef", X));
        checkSingleMemberFloatOvrdDef(UnitTest.class.getMethod("SingleMemberFloatOvrdDef", X));
        checkSingleMemberDoubleOvrdDef(UnitTest.class.getMethod("SingleMemberDoubleOvrdDef", X));
        checkSingleMemberBooleanOvrdDef(UnitTest.class.getMethod("SingleMemberBooleanOvrdDef", X));
        checkSingleMemberStringOvrdDef(UnitTest.class.getMethod("SingleMemberStringOvrdDef", X));
        checkSingleMemberClassOvrdDef(UnitTest.class.getMethod("SingleMemberClassOvrdDef", X));
        checkSingleMemberEnumOvrdDef(UnitTest.class.getMethod("SingleMemberEnumOvrdDef", X));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT ON METHOD
        checkSingleMemberByteAcceptDef(UnitTest.class.getMethod("SingleMemberByteAcceptDef", X));
        checkSingleMemberShortAcceptDef(UnitTest.class.getMethod("SingleMemberShortAcceptDef", X));
        checkSingleMemberIntAcceptDef(UnitTest.class.getMethod("SingleMemberIntAcceptDef", X));
        checkSingleMemberLongAcceptDef(UnitTest.class.getMethod("SingleMemberLongAcceptDef", X));
        checkSingleMemberCharAcceptDef(UnitTest.class.getMethod("SingleMemberCharAcceptDef", X));
        checkSingleMemberFloatAcceptDef(UnitTest.class.getMethod("SingleMemberFloatAcceptDef", X));
        checkSingleMemberDoubleAcceptDef(UnitTest.class.getMethod("SingleMemberDoubleAcceptDef", X));
        checkSingleMemberBooleanAcceptDef(UnitTest.class.getMethod("SingleMemberBooleanAcceptDef", X));
        checkSingleMemberStringAcceptDef(UnitTest.class.getMethod("SingleMemberStringAcceptDef", X));
        checkSingleMemberClassAcceptDef(UnitTest.class.getMethod("SingleMemberClassAcceptDef", X));
        checkSingleMemberEnumAcceptDef(UnitTest.class.getMethod("SingleMemberEnumAcceptDef", X));

        // SINGLE-MEMBER ARRAY TYPES (EMPTY ARRAY) ON METHOD
        checkSingleMemberByteArrEmpty(UnitTest.class.getMethod("SingleMemberByteArrEmpty", X));
        checkSingleMemberShortArrEmpty(UnitTest.class.getMethod("SingleMemberShortArrEmpty", X));
        checkSingleMemberIntArrEmpty(UnitTest.class.getMethod("SingleMemberIntArrEmpty", X));
        checkSingleMemberLongArrEmpty(UnitTest.class.getMethod("SingleMemberLongArrEmpty", X));
        checkSingleMemberCharArrEmpty(UnitTest.class.getMethod("SingleMemberCharArrEmpty", X));
        checkSingleMemberFloatArrEmpty(UnitTest.class.getMethod("SingleMemberFloatArrEmpty", X));
        checkSingleMemberDoubleArrEmpty(UnitTest.class.getMethod("SingleMemberDoubleArrEmpty", X));
        checkSingleMemberBooleanArrEmpty(UnitTest.class.getMethod("SingleMemberBooleanArrEmpty", X));
        checkSingleMemberStringArrEmpty(UnitTest.class.getMethod("SingleMemberStringArrEmpty", X));
        checkSingleMemberClassArrEmpty(UnitTest.class.getMethod("SingleMemberClassArrEmpty", X));
        checkSingleMemberEnumArrEmpty(UnitTest.class.getMethod("SingleMemberEnumArrEmpty", X));

        // SINGLE-MEMBER ARRAY TYPES (ONE-ELEMENT ARRAY) ON METHOD
        checkSingleMemberByteArrOne(UnitTest.class.getMethod("SingleMemberByteArrOne", X));
        checkSingleMemberShortArrOne(UnitTest.class.getMethod("SingleMemberShortArrOne", X));
        checkSingleMemberIntArrOne(UnitTest.class.getMethod("SingleMemberIntArrOne", X));
        checkSingleMemberLongArrOne(UnitTest.class.getMethod("SingleMemberLongArrOne", X));
        checkSingleMemberCharArrOne(UnitTest.class.getMethod("SingleMemberCharArrOne", X));
        checkSingleMemberFloatArrOne(UnitTest.class.getMethod("SingleMemberFloatArrOne", X));
        checkSingleMemberDoubleArrOne(UnitTest.class.getMethod("SingleMemberDoubleArrOne", X));
        checkSingleMemberBooleanArrOne(UnitTest.class.getMethod("SingleMemberBooleanArrOne", X));
        checkSingleMemberStringArrOne(UnitTest.class.getMethod("SingleMemberStringArrOne", X));
        checkSingleMemberClassArrOne(UnitTest.class.getMethod("SingleMemberClassArrOne", X));
        checkSingleMemberEnumArrOne(UnitTest.class.getMethod("SingleMemberEnumArrOne", X));

        // SINGLE-MEMBER ARRAY TYPES (TWO-ELEMENT ARRAY) ON METHOD
        checkSingleMemberByteArrTwo(UnitTest.class.getMethod("SingleMemberByteArrTwo", X));
        checkSingleMemberShortArrTwo(UnitTest.class.getMethod("SingleMemberShortArrTwo", X));
        checkSingleMemberIntArrTwo(UnitTest.class.getMethod("SingleMemberIntArrTwo", X));
        checkSingleMemberLongArrTwo(UnitTest.class.getMethod("SingleMemberLongArrTwo", X));
        checkSingleMemberCharArrTwo(UnitTest.class.getMethod("SingleMemberCharArrTwo", X));
        checkSingleMemberFloatArrTwo(UnitTest.class.getMethod("SingleMemberFloatArrTwo", X));
        checkSingleMemberDoubleArrTwo(UnitTest.class.getMethod("SingleMemberDoubleArrTwo", X));
        checkSingleMemberBooleanArrTwo(UnitTest.class.getMethod("SingleMemberBooleanArrTwo", X));
        checkSingleMemberStringArrTwo(UnitTest.class.getMethod("SingleMemberStringArrTwo", X));
        checkSingleMemberClassArrTwo(UnitTest.class.getMethod("SingleMemberClassArrTwo", X));
        checkSingleMemberEnumArrTwo(UnitTest.class.getMethod("SingleMemberEnumArrTwo", X));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (OVERRIDE)ON METHOD
        checkSingleMemberByteArrOvrdDef(UnitTest.class.getMethod("SingleMemberByteArrOvrdDef", X));
        checkSingleMemberShortArrOvrdDef(UnitTest.class.getMethod("SingleMemberShortArrOvrdDef", X));
        checkSingleMemberIntArrOvrdDef(UnitTest.class.getMethod("SingleMemberIntArrOvrdDef", X));
        checkSingleMemberLongArrOvrdDef(UnitTest.class.getMethod("SingleMemberLongArrOvrdDef", X));
        checkSingleMemberCharArrOvrdDef(UnitTest.class.getMethod("SingleMemberCharArrOvrdDef", X));
        checkSingleMemberFloatArrOvrdDef(UnitTest.class.getMethod("SingleMemberFloatArrOvrdDef", X));
        checkSingleMemberDoubleArrOvrdDef(UnitTest.class.getMethod("SingleMemberDoubleArrOvrdDef", X));
        checkSingleMemberBooleanArrOvrdDef(UnitTest.class.getMethod("SingleMemberBooleanArrOvrdDef", X));
        checkSingleMemberStringArrOvrdDef(UnitTest.class.getMethod("SingleMemberStringArrOvrdDef", X));
        checkSingleMemberClassArrOvrdDef(UnitTest.class.getMethod("SingleMemberClassArrOvrdDef", X));
        checkSingleMemberEnumArrOvrdDef(UnitTest.class.getMethod("SingleMemberEnumArrOvrdDef", X));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (ACCEPT)ON METHOD
        checkSingleMemberByteArrAcceptDef(UnitTest.class.getMethod("SingleMemberByteArrAcceptDef", X));
        checkSingleMemberShortArrAcceptDef(UnitTest.class.getMethod("SingleMemberShortArrAcceptDef", X));
        checkSingleMemberIntArrAcceptDef(UnitTest.class.getMethod("SingleMemberIntArrAcceptDef", X));
        checkSingleMemberLongArrAcceptDef(UnitTest.class.getMethod("SingleMemberLongArrAcceptDef", X));
        checkSingleMemberCharArrAcceptDef(UnitTest.class.getMethod("SingleMemberCharArrAcceptDef", X));
        checkSingleMemberFloatArrAcceptDef(UnitTest.class.getMethod("SingleMemberFloatArrAcceptDef", X));
        checkSingleMemberDoubleArrAcceptDef(UnitTest.class.getMethod("SingleMemberDoubleArrAcceptDef", X));
        checkSingleMemberBooleanArrAcceptDef(UnitTest.class.getMethod("SingleMemberBooleanArrAcceptDef", X));
        checkSingleMemberStringArrAcceptDef(UnitTest.class.getMethod("SingleMemberStringArrAcceptDef", X));
        checkSingleMemberClassArrAcceptDef(UnitTest.class.getMethod("SingleMemberClassArrAcceptDef", X));
        checkSingleMemberEnumArrAcceptDef(UnitTest.class.getMethod("SingleMemberEnumArrAcceptDef", X));

        // *** TESTS ON ANNOTATED FIELDS ***

        // MULTIMEMBER SCALAR TYPES ON FIELD
        checkScalarTypes(UnitTest.class.getField("scalarTypesField"));
        checkScalarTypesAcceptDefault(UnitTest.class.getField("scalarTypesAcceptDefaultField"));
        checkScalarTypesOverrideDefault(UnitTest.class.getField("scalarTypesOverrideDefaultField"));

        // MULTIMEMBER ARRAY TYPES ON FIELD
        checkArrayTypes0(UnitTest.class.getField("emptyArrayTypesField"));
        checkArrayTypes1(UnitTest.class.getField("singleElementArrayTypesField"));
        checkArrayTypes2(UnitTest.class.getField("twoElementArrayTypesField"));
        checkArrayTypesAcceptDefault(UnitTest.class.getField("arrayTypesAcceptDefaultField"));
        checkArrayTypesOverrideDefault(UnitTest.class.getField("arrayTypesOverrideDefaultField"));

        // MARKER TYPE ON FIELD
        checkMarker(UnitTest.class.getField("markerField"));

        // SINGLE-MEMBER SCALAR TYPES ON FIELD
        checkSingleMemberByte(UnitTest.class.getField("SingleMemberByteField"));
        checkSingleMemberShort(UnitTest.class.getField("SingleMemberShortField"));
        checkSingleMemberInt(UnitTest.class.getField("SingleMemberIntField"));
        checkSingleMemberLong(UnitTest.class.getField("SingleMemberLongField"));
        checkSingleMemberChar(UnitTest.class.getField("SingleMemberCharField"));
        checkSingleMemberFloat(UnitTest.class.getField("SingleMemberFloatField"));
        checkSingleMemberDouble(UnitTest.class.getField("SingleMemberDoubleField"));
        checkSingleMemberBoolean(UnitTest.class.getField("SingleMemberBooleanField"));
        checkSingleMemberString(UnitTest.class.getField("SingleMemberStringField"));
        checkSingleMemberClass(UnitTest.class.getField("SingleMemberClassField"));
        checkSingleMemberEnum(UnitTest.class.getField("SingleMemberEnumField"));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE ON FIELD
        checkSingleMemberByteOvrdDef(UnitTest.class.getField("SingleMemberByteOvrdDefField"));
        checkSingleMemberShortOvrdDef(UnitTest.class.getField("SingleMemberShortOvrdDefField"));
        checkSingleMemberIntOvrdDef(UnitTest.class.getField("SingleMemberIntOvrdDefField"));
        checkSingleMemberLongOvrdDef(UnitTest.class.getField("SingleMemberLongOvrdDefField"));
        checkSingleMemberCharOvrdDef(UnitTest.class.getField("SingleMemberCharOvrdDefField"));
        checkSingleMemberFloatOvrdDef(UnitTest.class.getField("SingleMemberFloatOvrdDefField"));
        checkSingleMemberDoubleOvrdDef(UnitTest.class.getField("SingleMemberDoubleOvrdDefField"));
        checkSingleMemberBooleanOvrdDef(UnitTest.class.getField("SingleMemberBooleanOvrdDefField"));
        checkSingleMemberStringOvrdDef(UnitTest.class.getField("SingleMemberStringOvrdDefField"));
        checkSingleMemberClassOvrdDef(UnitTest.class.getField("SingleMemberClassOvrdDefField"));
        checkSingleMemberEnumOvrdDef(UnitTest.class.getField("SingleMemberEnumOvrdDefField"));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT ON FIELD
        checkSingleMemberByteAcceptDef(UnitTest.class.getField("SingleMemberByteAcceptDefField"));
        checkSingleMemberShortAcceptDef(UnitTest.class.getField("SingleMemberShortAcceptDefField"));
        checkSingleMemberIntAcceptDef(UnitTest.class.getField("SingleMemberIntAcceptDefField"));
        checkSingleMemberLongAcceptDef(UnitTest.class.getField("SingleMemberLongAcceptDefField"));
        checkSingleMemberCharAcceptDef(UnitTest.class.getField("SingleMemberCharAcceptDefField"));
        checkSingleMemberFloatAcceptDef(UnitTest.class.getField("SingleMemberFloatAcceptDefField"));
        checkSingleMemberDoubleAcceptDef(UnitTest.class.getField("SingleMemberDoubleAcceptDefField"));
        checkSingleMemberBooleanAcceptDef(UnitTest.class.getField("SingleMemberBooleanAcceptDefField"));
        checkSingleMemberStringAcceptDef(UnitTest.class.getField("SingleMemberStringAcceptDefField"));
        checkSingleMemberClassAcceptDef(UnitTest.class.getField("SingleMemberClassAcceptDefField"));
        checkSingleMemberEnumAcceptDef(UnitTest.class.getField("SingleMemberEnumAcceptDefField"));

        // SINGLE-MEMBER ARRAY TYPES (EMPTY ARRAY) ON FIELD
        checkSingleMemberByteArrEmpty(UnitTest.class.getField("SingleMemberByteArrEmptyField"));
        checkSingleMemberShortArrEmpty(UnitTest.class.getField("SingleMemberShortArrEmptyField"));
        checkSingleMemberIntArrEmpty(UnitTest.class.getField("SingleMemberIntArrEmptyField"));
        checkSingleMemberLongArrEmpty(UnitTest.class.getField("SingleMemberLongArrEmptyField"));
        checkSingleMemberCharArrEmpty(UnitTest.class.getField("SingleMemberCharArrEmptyField"));
        checkSingleMemberFloatArrEmpty(UnitTest.class.getField("SingleMemberFloatArrEmptyField"));
        checkSingleMemberDoubleArrEmpty(UnitTest.class.getField("SingleMemberDoubleArrEmptyField"));
        checkSingleMemberBooleanArrEmpty(UnitTest.class.getField("SingleMemberBooleanArrEmptyField"));
        checkSingleMemberStringArrEmpty(UnitTest.class.getField("SingleMemberStringArrEmptyField"));
        checkSingleMemberClassArrEmpty(UnitTest.class.getField("SingleMemberClassArrEmptyField"));
        checkSingleMemberEnumArrEmpty(UnitTest.class.getField("SingleMemberEnumArrEmptyField"));

        // SINGLE-MEMBER ARRAY TYPES (ONE-ELEMENT ARRAY) ON FIELD
        checkSingleMemberByteArrOne(UnitTest.class.getField("SingleMemberByteArrOneField"));
        checkSingleMemberShortArrOne(UnitTest.class.getField("SingleMemberShortArrOneField"));
        checkSingleMemberIntArrOne(UnitTest.class.getField("SingleMemberIntArrOneField"));
        checkSingleMemberLongArrOne(UnitTest.class.getField("SingleMemberLongArrOneField"));
        checkSingleMemberCharArrOne(UnitTest.class.getField("SingleMemberCharArrOneField"));
        checkSingleMemberFloatArrOne(UnitTest.class.getField("SingleMemberFloatArrOneField"));
        checkSingleMemberDoubleArrOne(UnitTest.class.getField("SingleMemberDoubleArrOneField"));
        checkSingleMemberBooleanArrOne(UnitTest.class.getField("SingleMemberBooleanArrOneField"));
        checkSingleMemberStringArrOne(UnitTest.class.getField("SingleMemberStringArrOneField"));
        checkSingleMemberClassArrOne(UnitTest.class.getField("SingleMemberClassArrOneField"));
        checkSingleMemberEnumArrOne(UnitTest.class.getField("SingleMemberEnumArrOneField"));

        // SINGLE-MEMBER ARRAY TYPES (TWO-ELEMENT ARRAY) ON FIELD
        checkSingleMemberByteArrTwo(UnitTest.class.getField("SingleMemberByteArrTwoField"));
        checkSingleMemberShortArrTwo(UnitTest.class.getField("SingleMemberShortArrTwoField"));
        checkSingleMemberIntArrTwo(UnitTest.class.getField("SingleMemberIntArrTwoField"));
        checkSingleMemberLongArrTwo(UnitTest.class.getField("SingleMemberLongArrTwoField"));
        checkSingleMemberCharArrTwo(UnitTest.class.getField("SingleMemberCharArrTwoField"));
        checkSingleMemberFloatArrTwo(UnitTest.class.getField("SingleMemberFloatArrTwoField"));
        checkSingleMemberDoubleArrTwo(UnitTest.class.getField("SingleMemberDoubleArrTwoField"));
        checkSingleMemberBooleanArrTwo(UnitTest.class.getField("SingleMemberBooleanArrTwoField"));
        checkSingleMemberStringArrTwo(UnitTest.class.getField("SingleMemberStringArrTwoField"));
        checkSingleMemberClassArrTwo(UnitTest.class.getField("SingleMemberClassArrTwoField"));
        checkSingleMemberEnumArrTwo(UnitTest.class.getField("SingleMemberEnumArrTwoField"));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (OVERRIDE)ON FIELD
        checkSingleMemberByteArrOvrdDef(UnitTest.class.getField("SingleMemberByteArrOvrdDefField"));
        checkSingleMemberShortArrOvrdDef(UnitTest.class.getField("SingleMemberShortArrOvrdDefField"));
        checkSingleMemberIntArrOvrdDef(UnitTest.class.getField("SingleMemberIntArrOvrdDefField"));
        checkSingleMemberLongArrOvrdDef(UnitTest.class.getField("SingleMemberLongArrOvrdDefField"));
        checkSingleMemberCharArrOvrdDef(UnitTest.class.getField("SingleMemberCharArrOvrdDefField"));
        checkSingleMemberFloatArrOvrdDef(UnitTest.class.getField("SingleMemberFloatArrOvrdDefField"));
        checkSingleMemberDoubleArrOvrdDef(UnitTest.class.getField("SingleMemberDoubleArrOvrdDefField"));
        checkSingleMemberBooleanArrOvrdDef(UnitTest.class.getField("SingleMemberBooleanArrOvrdDefField"));
        checkSingleMemberStringArrOvrdDef(UnitTest.class.getField("SingleMemberStringArrOvrdDefField"));
        checkSingleMemberClassArrOvrdDef(UnitTest.class.getField("SingleMemberClassArrOvrdDefField"));
        checkSingleMemberEnumArrOvrdDef(UnitTest.class.getField("SingleMemberEnumArrOvrdDefField"));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (ACCEPT)ON FIELD
        checkSingleMemberByteArrAcceptDef(UnitTest.class.getField("SingleMemberByteArrAcceptDefField"));
        checkSingleMemberShortArrAcceptDef(UnitTest.class.getField("SingleMemberShortArrAcceptDefField"));
        checkSingleMemberIntArrAcceptDef(UnitTest.class.getField("SingleMemberIntArrAcceptDefField"));
        checkSingleMemberLongArrAcceptDef(UnitTest.class.getField("SingleMemberLongArrAcceptDefField"));
        checkSingleMemberCharArrAcceptDef(UnitTest.class.getField("SingleMemberCharArrAcceptDefField"));
        checkSingleMemberFloatArrAcceptDef(UnitTest.class.getField("SingleMemberFloatArrAcceptDefField"));
        checkSingleMemberDoubleArrAcceptDef(UnitTest.class.getField("SingleMemberDoubleArrAcceptDefField"));
        checkSingleMemberBooleanArrAcceptDef(UnitTest.class.getField("SingleMemberBooleanArrAcceptDefField"));
        checkSingleMemberStringArrAcceptDef(UnitTest.class.getField("SingleMemberStringArrAcceptDefField"));
        checkSingleMemberClassArrAcceptDef(UnitTest.class.getField("SingleMemberClassArrAcceptDefField"));
        checkSingleMemberEnumArrAcceptDef(UnitTest.class.getField("SingleMemberEnumArrAcceptDefField"));

        // *** TESTS ON ANNOTATED ENUM CONSTS ***

        // MULTIMEMBER SCALAR TYPES ON ENUM CONST
        checkScalarTypes(TestType.class.getField("scalarTypesField"));
        checkScalarTypesAcceptDefault(TestType.class.getField("scalarTypesAcceptDefaultField"));
        checkScalarTypesOverrideDefault(TestType.class.getField("scalarTypesOverrideDefaultField"));

        // MULTIMEMBER ARRAY TYPES ON ENUM CONST
        checkArrayTypes0(TestType.class.getField("emptyArrayTypesField"));
        checkArrayTypes1(TestType.class.getField("singleElementArrayTypesField"));
        checkArrayTypes2(TestType.class.getField("twoElementArrayTypesField"));
        checkArrayTypesAcceptDefault(TestType.class.getField("arrayTypesAcceptDefaultField"));
        checkArrayTypesOverrideDefault(TestType.class.getField("arrayTypesOverrideDefaultField"));

        // MARKER TYPE ON CLASS
        checkMarker(TestType.class.getField("marker"));

        // SINGLE-MEMBER SCALAR TYPES ON CLASS
        checkSingleMemberByte(TestType.class.getField("SingleMemberByte"));
        checkSingleMemberShort(TestType.class.getField("SingleMemberShort"));
        checkSingleMemberInt(TestType.class.getField("SingleMemberInt"));
        checkSingleMemberLong(TestType.class.getField("SingleMemberLong"));
        checkSingleMemberChar(TestType.class.getField("SingleMemberChar"));
        checkSingleMemberFloat(TestType.class.getField("SingleMemberFloat"));
        checkSingleMemberDouble(TestType.class.getField("SingleMemberDouble"));
        checkSingleMemberBoolean(TestType.class.getField("SingleMemberBoolean"));
        checkSingleMemberString(TestType.class.getField("SingleMemberString"));
        checkSingleMemberClass(TestType.class.getField("SingleMemberClass"));
        checkSingleMemberEnum(TestType.class.getField("SingleMemberEnum"));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE ON CLASS
        checkSingleMemberByteOvrdDef(TestType.class.getField("SingleMemberByteOvrdDef"));
        checkSingleMemberShortOvrdDef(TestType.class.getField("SingleMemberShortOvrdDef"));
        checkSingleMemberIntOvrdDef(TestType.class.getField("SingleMemberIntOvrdDef"));
        checkSingleMemberLongOvrdDef(TestType.class.getField("SingleMemberLongOvrdDef"));
        checkSingleMemberCharOvrdDef(TestType.class.getField("SingleMemberCharOvrdDef"));
        checkSingleMemberFloatOvrdDef(TestType.class.getField("SingleMemberFloatOvrdDef"));
        checkSingleMemberDoubleOvrdDef(TestType.class.getField("SingleMemberDoubleOvrdDef"));
        checkSingleMemberBooleanOvrdDef(TestType.class.getField("SingleMemberBooleanOvrdDef"));
        checkSingleMemberStringOvrdDef(TestType.class.getField("SingleMemberStringOvrdDef"));
        checkSingleMemberClassOvrdDef(TestType.class.getField("SingleMemberClassOvrdDef"));
        checkSingleMemberEnumOvrdDef(TestType.class.getField("SingleMemberEnumOvrdDef"));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT ON CLASS
        checkSingleMemberByteAcceptDef(TestType.class.getField("SingleMemberByteAcceptDef"));
        checkSingleMemberShortAcceptDef(TestType.class.getField("SingleMemberShortAcceptDef"));
        checkSingleMemberIntAcceptDef(TestType.class.getField("SingleMemberIntAcceptDef"));
        checkSingleMemberLongAcceptDef(TestType.class.getField("SingleMemberLongAcceptDef"));
        checkSingleMemberCharAcceptDef(TestType.class.getField("SingleMemberCharAcceptDef"));
        checkSingleMemberFloatAcceptDef(TestType.class.getField("SingleMemberFloatAcceptDef"));
        checkSingleMemberDoubleAcceptDef(TestType.class.getField("SingleMemberDoubleAcceptDef"));
        checkSingleMemberBooleanAcceptDef(TestType.class.getField("SingleMemberBooleanAcceptDef"));
        checkSingleMemberStringAcceptDef(TestType.class.getField("SingleMemberStringAcceptDef"));
        checkSingleMemberClassAcceptDef(TestType.class.getField("SingleMemberClassAcceptDef"));
        checkSingleMemberEnumAcceptDef(TestType.class.getField("SingleMemberEnumAcceptDef"));

        // SINGLE-MEMBER ARRAY TYPES (TestType.class.getField("EMPTY ARRAY) ON CLASS
        checkSingleMemberByteArrEmpty(TestType.class.getField("SingleMemberByteArrEmpty"));
        checkSingleMemberShortArrEmpty(TestType.class.getField("SingleMemberShortArrEmpty"));
        checkSingleMemberIntArrEmpty(TestType.class.getField("SingleMemberIntArrEmpty"));
        checkSingleMemberLongArrEmpty(TestType.class.getField("SingleMemberLongArrEmpty"));
        checkSingleMemberCharArrEmpty(TestType.class.getField("SingleMemberCharArrEmpty"));
        checkSingleMemberFloatArrEmpty(TestType.class.getField("SingleMemberFloatArrEmpty"));
        checkSingleMemberDoubleArrEmpty(TestType.class.getField("SingleMemberDoubleArrEmpty"));
        checkSingleMemberBooleanArrEmpty(TestType.class.getField("SingleMemberBooleanArrEmpty"));
        checkSingleMemberStringArrEmpty(TestType.class.getField("SingleMemberStringArrEmpty"));
        checkSingleMemberClassArrEmpty(TestType.class.getField("SingleMemberClassArrEmpty"));
        checkSingleMemberEnumArrEmpty(TestType.class.getField("SingleMemberEnumArrEmpty"));

        // SINGLE-MEMBER ARRAY TYPES (TestType.class.getField("ONE-ELEMENT ARRAY) ON CLASS
        checkSingleMemberByteArrOne(TestType.class.getField("SingleMemberByteArrOne"));
        checkSingleMemberShortArrOne(TestType.class.getField("SingleMemberShortArrOne"));
        checkSingleMemberIntArrOne(TestType.class.getField("SingleMemberIntArrOne"));
        checkSingleMemberLongArrOne(TestType.class.getField("SingleMemberLongArrOne"));
        checkSingleMemberCharArrOne(TestType.class.getField("SingleMemberCharArrOne"));
        checkSingleMemberFloatArrOne(TestType.class.getField("SingleMemberFloatArrOne"));
        checkSingleMemberDoubleArrOne(TestType.class.getField("SingleMemberDoubleArrOne"));
        checkSingleMemberBooleanArrOne(TestType.class.getField("SingleMemberBooleanArrOne"));
        checkSingleMemberStringArrOne(TestType.class.getField("SingleMemberStringArrOne"));
        checkSingleMemberClassArrOne(TestType.class.getField("SingleMemberClassArrOne"));
        checkSingleMemberEnumArrOne(TestType.class.getField("SingleMemberEnumArrOne"));

        // SINGLE-MEMBER ARRAY TYPES (TestType.class.getField("TWO-ELEMENT ARRAY) ON CLASS
        checkSingleMemberByteArrTwo(TestType.class.getField("SingleMemberByteArrTwo"));
        checkSingleMemberShortArrTwo(TestType.class.getField("SingleMemberShortArrTwo"));
        checkSingleMemberIntArrTwo(TestType.class.getField("SingleMemberIntArrTwo"));
        checkSingleMemberLongArrTwo(TestType.class.getField("SingleMemberLongArrTwo"));
        checkSingleMemberCharArrTwo(TestType.class.getField("SingleMemberCharArrTwo"));
        checkSingleMemberFloatArrTwo(TestType.class.getField("SingleMemberFloatArrTwo"));
        checkSingleMemberDoubleArrTwo(TestType.class.getField("SingleMemberDoubleArrTwo"));
        checkSingleMemberBooleanArrTwo(TestType.class.getField("SingleMemberBooleanArrTwo"));
        checkSingleMemberStringArrTwo(TestType.class.getField("SingleMemberStringArrTwo"));
        checkSingleMemberClassArrTwo(TestType.class.getField("SingleMemberClassArrTwo"));
        checkSingleMemberEnumArrTwo(TestType.class.getField("SingleMemberEnumArrTwo"));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (TestType.class.getField("OVERRIDE)ON CLASS
        checkSingleMemberByteArrOvrdDef(TestType.class.getField("SingleMemberByteArrOvrdDef"));
        checkSingleMemberShortArrOvrdDef(TestType.class.getField("SingleMemberShortArrOvrdDef"));
        checkSingleMemberIntArrOvrdDef(TestType.class.getField("SingleMemberIntArrOvrdDef"));
        checkSingleMemberLongArrOvrdDef(TestType.class.getField("SingleMemberLongArrOvrdDef"));
        checkSingleMemberCharArrOvrdDef(TestType.class.getField("SingleMemberCharArrOvrdDef"));
        checkSingleMemberFloatArrOvrdDef(TestType.class.getField("SingleMemberFloatArrOvrdDef"));
        checkSingleMemberDoubleArrOvrdDef(TestType.class.getField("SingleMemberDoubleArrOvrdDef"));
        checkSingleMemberBooleanArrOvrdDef(TestType.class.getField("SingleMemberBooleanArrOvrdDef"));
        checkSingleMemberStringArrOvrdDef(TestType.class.getField("SingleMemberStringArrOvrdDef"));
        checkSingleMemberClassArrOvrdDef(TestType.class.getField("SingleMemberClassArrOvrdDef"));
        checkSingleMemberEnumArrOvrdDef(TestType.class.getField("SingleMemberEnumArrOvrdDef"));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (TestType.class.getField("ACCEPT)ON CLASS
        checkSingleMemberByteArrAcceptDef(TestType.class.getField("SingleMemberByteArrAcceptDef"));
        checkSingleMemberShortArrAcceptDef(TestType.class.getField("SingleMemberShortArrAcceptDef"));
        checkSingleMemberIntArrAcceptDef(TestType.class.getField("SingleMemberIntArrAcceptDef"));
        checkSingleMemberLongArrAcceptDef(TestType.class.getField("SingleMemberLongArrAcceptDef"));
        checkSingleMemberCharArrAcceptDef(TestType.class.getField("SingleMemberCharArrAcceptDef"));
        checkSingleMemberFloatArrAcceptDef(TestType.class.getField("SingleMemberFloatArrAcceptDef"));
        checkSingleMemberDoubleArrAcceptDef(TestType.class.getField("SingleMemberDoubleArrAcceptDef"));
        checkSingleMemberBooleanArrAcceptDef(TestType.class.getField("SingleMemberBooleanArrAcceptDef"));
        checkSingleMemberStringArrAcceptDef(TestType.class.getField("SingleMemberStringArrAcceptDef"));
        checkSingleMemberClassArrAcceptDef(TestType.class.getField("SingleMemberClassArrAcceptDef"));
        checkSingleMemberEnumArrAcceptDef(TestType.class.getField("SingleMemberEnumArrAcceptDef"));

        // *** TESTS ON ANNOTATED CONSTRUCTORS ***

        // MULTIMEMBER SCALAR TYPES ON CONSTRUCTOR
        checkScalarTypes(UnitTest.class.getConstructor(new Class[]{Iterator.class}));
        checkScalarTypesOverrideDefault(UnitTest.class.getConstructor(new Class[]{Map.class}));
        checkScalarTypesAcceptDefault(UnitTest.class.getConstructor(new Class[]{Set.class}));

        // MULTIMEMBER ARRAY TYPES ON CONSTRUCTOR
        checkArrayTypes0(UnitTest.class.getConstructor(new Class[]{List.class}));
        checkArrayTypes1(UnitTest.class.getConstructor(new Class[]{Collection.class}));
        checkArrayTypes2(UnitTest.class.getConstructor(new Class[]{SortedSet.class}));
        checkArrayTypesAcceptDefault(UnitTest.class.getConstructor(new Class[]{SortedMap.class}));
        checkArrayTypesOverrideDefault(UnitTest.class.getConstructor(new Class[]{RandomAccess.class}));

        // MARKER TYPE ON CONSTRUCTOR
        checkMarker(UnitTest.class.getConstructor(new Class[] { }));

        // SINGLE-MEMBER SCALAR TYPES ON CONSTRUCTOR
        checkSingleMemberByte(UnitTest.class.getConstructor(new Class[] { byte.class }));
        checkSingleMemberShort(UnitTest.class.getConstructor(new Class[] { short.class }));
        checkSingleMemberInt(UnitTest.class.getConstructor(new Class[] { int.class }));
        checkSingleMemberLong(UnitTest.class.getConstructor(new Class[] { long.class }));
        checkSingleMemberChar(UnitTest.class.getConstructor(new Class[] { char.class }));
        checkSingleMemberFloat(UnitTest.class.getConstructor(new Class[] { float.class }));
        checkSingleMemberDouble(UnitTest.class.getConstructor(new Class[] { double.class }));
        checkSingleMemberBoolean(UnitTest.class.getConstructor(new Class[] { boolean.class }));
        checkSingleMemberString(UnitTest.class.getConstructor(new Class[] { String.class }));
        checkSingleMemberClass(UnitTest.class.getConstructor(new Class[] { Class.class }));
        checkSingleMemberEnum(UnitTest.class.getConstructor(new Class[] { Enum.class }));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE ON CONSTRUCTOR
        checkSingleMemberByteOvrdDef(UnitTest.class.getConstructor(new Class[] { byte.class, Set.class }));
        checkSingleMemberShortOvrdDef(UnitTest.class.getConstructor(new Class[] { short.class, Set.class }));
        checkSingleMemberIntOvrdDef(UnitTest.class.getConstructor(new Class[] { int.class, Set.class }));
        checkSingleMemberLongOvrdDef(UnitTest.class.getConstructor(new Class[] { long.class, Set.class }));
        checkSingleMemberCharOvrdDef(UnitTest.class.getConstructor(new Class[] { char.class, Set.class }));
        checkSingleMemberFloatOvrdDef(UnitTest.class.getConstructor(new Class[] { float.class, Set.class }));
        checkSingleMemberDoubleOvrdDef(UnitTest.class.getConstructor(new Class[] { double.class, Set.class }));
        checkSingleMemberBooleanOvrdDef(UnitTest.class.getConstructor(new Class[] { boolean.class, Set.class }));
        checkSingleMemberStringOvrdDef(UnitTest.class.getConstructor(new Class[] { String.class, Set.class }));
        checkSingleMemberClassOvrdDef(UnitTest.class.getConstructor(new Class[] { Class.class, Set.class }));
        checkSingleMemberEnumOvrdDef(UnitTest.class.getConstructor(new Class[] { Enum.class, Set.class }));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT ON CONSTRUCTOR
        checkSingleMemberByteAcceptDef(UnitTest.class.getConstructor(new Class[] { byte.class, Map.class }));
        checkSingleMemberShortAcceptDef(UnitTest.class.getConstructor(new Class[] { short.class, Map.class }));
        checkSingleMemberIntAcceptDef(UnitTest.class.getConstructor(new Class[] { int.class, Map.class }));
        checkSingleMemberLongAcceptDef(UnitTest.class.getConstructor(new Class[] { long.class, Map.class }));
        checkSingleMemberCharAcceptDef(UnitTest.class.getConstructor(new Class[] { char.class, Map.class }));
        checkSingleMemberFloatAcceptDef(UnitTest.class.getConstructor(new Class[] { float.class, Map.class }));
        checkSingleMemberDoubleAcceptDef(UnitTest.class.getConstructor(new Class[] { double.class, Map.class }));
        checkSingleMemberBooleanAcceptDef(UnitTest.class.getConstructor(new Class[] { boolean.class, Map.class }));
        checkSingleMemberStringAcceptDef(UnitTest.class.getConstructor(new Class[] { String.class, Map.class }));
        checkSingleMemberClassAcceptDef(UnitTest.class.getConstructor(new Class[] { Class.class, Map.class }));
        checkSingleMemberEnumAcceptDef(UnitTest.class.getConstructor(new Class[] { Enum.class, Map.class }));

        // SINGLE-MEMBER ARRAY TYPES (EMPTY ARRAY) ON CONSTRUCTOR
        checkSingleMemberByteArrEmpty(UnitTest.class.getConstructor(new Class[] { byte[].class }));
        checkSingleMemberShortArrEmpty(UnitTest.class.getConstructor(new Class[] { short[].class }));
        checkSingleMemberIntArrEmpty(UnitTest.class.getConstructor(new Class[] { int[].class }));
        checkSingleMemberLongArrEmpty(UnitTest.class.getConstructor(new Class[] { long[].class }));
        checkSingleMemberCharArrEmpty(UnitTest.class.getConstructor(new Class[] { char[].class }));
        checkSingleMemberFloatArrEmpty(UnitTest.class.getConstructor(new Class[] { float[].class }));
        checkSingleMemberDoubleArrEmpty(UnitTest.class.getConstructor(new Class[] { double[].class }));
        checkSingleMemberBooleanArrEmpty(UnitTest.class.getConstructor(new Class[] { boolean[].class }));
        checkSingleMemberStringArrEmpty(UnitTest.class.getConstructor(new Class[] { String[].class }));
        checkSingleMemberClassArrEmpty(UnitTest.class.getConstructor(new Class[] { Class[].class }));
        checkSingleMemberEnumArrEmpty(UnitTest.class.getConstructor(new Class[] { Enum[].class }));

        // SINGLE-MEMBER ARRAY TYPES (ONE-ELEMENT ARRAY) ON CONSTRUCTOR
        checkSingleMemberByteArrOne(UnitTest.class.getConstructor(new Class[] { byte[].class, Set.class }));
        checkSingleMemberShortArrOne(UnitTest.class.getConstructor(new Class[] { short[].class, Set.class }));
        checkSingleMemberIntArrOne(UnitTest.class.getConstructor(new Class[] { int[].class, Set.class }));
        checkSingleMemberLongArrOne(UnitTest.class.getConstructor(new Class[] { long[].class, Set.class }));
        checkSingleMemberCharArrOne(UnitTest.class.getConstructor(new Class[] { char[].class, Set.class }));
        checkSingleMemberFloatArrOne(UnitTest.class.getConstructor(new Class[] { float[].class, Set.class }));
        checkSingleMemberDoubleArrOne(UnitTest.class.getConstructor(new Class[] { double[].class, Set.class }));
        checkSingleMemberBooleanArrOne(UnitTest.class.getConstructor(new Class[] { boolean[].class, Set.class }));
        checkSingleMemberStringArrOne(UnitTest.class.getConstructor(new Class[] { String[].class, Set.class }));
        checkSingleMemberClassArrOne(UnitTest.class.getConstructor(new Class[] { Class[].class, Set.class }));
        checkSingleMemberEnumArrOne(UnitTest.class.getConstructor(new Class[] { Enum[].class, Set.class }));

        // SINGLE-MEMBER ARRAY TYPES (TWO-ELEMENT ARRAY) ON CONSTRUCTOR
        checkSingleMemberByteArrTwo(UnitTest.class.getConstructor(new Class[] { byte[].class, Map.class }));
        checkSingleMemberShortArrTwo(UnitTest.class.getConstructor(new Class[] { short[].class, Map.class }));
        checkSingleMemberIntArrTwo(UnitTest.class.getConstructor(new Class[] { int[].class, Map.class }));
        checkSingleMemberLongArrTwo(UnitTest.class.getConstructor(new Class[] { long[].class, Map.class }));
        checkSingleMemberCharArrTwo(UnitTest.class.getConstructor(new Class[] { char[].class, Map.class }));
        checkSingleMemberFloatArrTwo(UnitTest.class.getConstructor(new Class[] { float[].class, Map.class }));
        checkSingleMemberDoubleArrTwo(UnitTest.class.getConstructor(new Class[] { double[].class, Map.class }));
        checkSingleMemberBooleanArrTwo(UnitTest.class.getConstructor(new Class[] { boolean[].class, Map.class }));
        checkSingleMemberStringArrTwo(UnitTest.class.getConstructor(new Class[] { String[].class, Map.class }));
        checkSingleMemberClassArrTwo(UnitTest.class.getConstructor(new Class[] { Class[].class, Map.class }));
        checkSingleMemberEnumArrTwo(UnitTest.class.getConstructor(new Class[] { Enum[].class, Map.class }));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (OVERRIDE)ON CONSTRUCTOR
        checkSingleMemberByteArrOvrdDef(UnitTest.class.getConstructor(new Class[] { byte[].class, List.class }));
        checkSingleMemberShortArrOvrdDef(UnitTest.class.getConstructor(new Class[] { short[].class, List.class }));
        checkSingleMemberIntArrOvrdDef(UnitTest.class.getConstructor(new Class[] { int[].class, List.class }));
        checkSingleMemberLongArrOvrdDef(UnitTest.class.getConstructor(new Class[] { long[].class, List.class }));
        checkSingleMemberCharArrOvrdDef(UnitTest.class.getConstructor(new Class[] { char[].class, List.class }));
        checkSingleMemberFloatArrOvrdDef(UnitTest.class.getConstructor(new Class[] { float[].class, List.class }));
        checkSingleMemberDoubleArrOvrdDef(UnitTest.class.getConstructor(new Class[] { double[].class, List.class }));
        checkSingleMemberBooleanArrOvrdDef(UnitTest.class.getConstructor(new Class[] { boolean[].class, List.class }));
        checkSingleMemberStringArrOvrdDef(UnitTest.class.getConstructor(new Class[] { String[].class, List.class }));
        checkSingleMemberClassArrOvrdDef(UnitTest.class.getConstructor(new Class[] { Class[].class, List.class }));
        checkSingleMemberEnumArrOvrdDef(UnitTest.class.getConstructor(new Class[] { Enum[].class, List.class }));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (ACCEPT)ON CONSTRUCTOR
        checkSingleMemberByteArrAcceptDef(UnitTest.class.getConstructor(new Class[] { byte[].class, Collection.class }));
        checkSingleMemberShortArrAcceptDef(UnitTest.class.getConstructor(new Class[] { short[].class, Collection.class }));
        checkSingleMemberIntArrAcceptDef(UnitTest.class.getConstructor(new Class[] { int[].class, Collection.class }));
        checkSingleMemberLongArrAcceptDef(UnitTest.class.getConstructor(new Class[] { long[].class, Collection.class }));
        checkSingleMemberCharArrAcceptDef(UnitTest.class.getConstructor(new Class[] { char[].class, Collection.class }));
        checkSingleMemberFloatArrAcceptDef(UnitTest.class.getConstructor(new Class[] { float[].class, Collection.class }));
        checkSingleMemberDoubleArrAcceptDef(UnitTest.class.getConstructor(new Class[] { double[].class, Collection.class }));
        checkSingleMemberBooleanArrAcceptDef(UnitTest.class.getConstructor(new Class[] { boolean[].class, Collection.class }));
        checkSingleMemberStringArrAcceptDef(UnitTest.class.getConstructor(new Class[] { String[].class, Collection.class }));
        checkSingleMemberClassArrAcceptDef(UnitTest.class.getConstructor(new Class[] { Class[].class, Collection.class }));
        checkSingleMemberEnumArrAcceptDef(UnitTest.class.getConstructor(new Class[] { Enum[].class, Collection.class }));

        // *** TESTS ON ANNOTATED PARAMETERS ***

        // MULTIMEMBER SCALAR TYPES ON PARAM
        checkScalarTypesParam(UnitTest.class.getMethod("scalarTypesParam", Y));
        checkScalarTypesOverrideDefaultParam(UnitTest.class.getMethod("scalarTypesOverrideDefaultParam", Y));
        checkScalarTypesAcceptDefaultParam(UnitTest.class.getMethod("scalarTypesAcceptDefaultParam", Y));

        // MULTIMEMBER ARRAY TYPES ON PARAM
        checkArrayTypes0Param(UnitTest.class.getMethod("emptyArrayTypesParam", Y));
        checkArrayTypes1Param(UnitTest.class.getMethod("singleElementArrayTypesParam", Y));
        checkArrayTypes2Param(UnitTest.class.getMethod("twoElementArrayTypesParam", Y));
        checkArrayTypesAcceptDefaultParam(UnitTest.class.getMethod("arrayTypesAcceptDefaultParam", Y));
        checkArrayTypesOverrideDefaultParam(UnitTest.class.getMethod("arrayTypesOverrideDefaultParam", Y));

        // MARKER TYPE ON PARAMETER
        checkMarkerParam(UnitTest.class.getMethod("markerParam", Y));

        // SINGLE-MEMBER SCALAR TYPES ON PARAMETER
        checkSingleMemberByteParam(UnitTest.class.getMethod("SingleMemberByteParam", Y));
        checkSingleMemberShortParam(UnitTest.class.getMethod("SingleMemberShortParam", Y));
        checkSingleMemberIntParam(UnitTest.class.getMethod("SingleMemberIntParam", Y));
        checkSingleMemberLongParam(UnitTest.class.getMethod("SingleMemberLongParam", Y));
        checkSingleMemberCharParam(UnitTest.class.getMethod("SingleMemberCharParam", Y));
        checkSingleMemberFloatParam(UnitTest.class.getMethod("SingleMemberFloatParam", Y));
        checkSingleMemberDoubleParam(UnitTest.class.getMethod("SingleMemberDoubleParam", Y));
        checkSingleMemberBooleanParam(UnitTest.class.getMethod("SingleMemberBooleanParam", Y));
        checkSingleMemberStringParam(UnitTest.class.getMethod("SingleMemberStringParam", Y));
        checkSingleMemberClassParam(UnitTest.class.getMethod("SingleMemberClassParam", Y));
        checkSingleMemberEnumParam(UnitTest.class.getMethod("SingleMemberEnumParam", Y));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE ON PARAMETER
        checkSingleMemberByteOvrdDefParam(UnitTest.class.getMethod("SingleMemberByteOvrdDefParam", Y));
        checkSingleMemberShortOvrdDefParam(UnitTest.class.getMethod("SingleMemberShortOvrdDefParam", Y));
        checkSingleMemberIntOvrdDefParam(UnitTest.class.getMethod("SingleMemberIntOvrdDefParam", Y));
        checkSingleMemberLongOvrdDefParam(UnitTest.class.getMethod("SingleMemberLongOvrdDefParam", Y));
        checkSingleMemberCharOvrdDefParam(UnitTest.class.getMethod("SingleMemberCharOvrdDefParam", Y));
        checkSingleMemberFloatOvrdDefParam(UnitTest.class.getMethod("SingleMemberFloatOvrdDefParam", Y));
        checkSingleMemberDoubleOvrdDefParam(UnitTest.class.getMethod("SingleMemberDoubleOvrdDefParam", Y));
        checkSingleMemberBooleanOvrdDefParam(UnitTest.class.getMethod("SingleMemberBooleanOvrdDefParam", Y));
        checkSingleMemberStringOvrdDefParam(UnitTest.class.getMethod("SingleMemberStringOvrdDefParam", Y));
        checkSingleMemberClassOvrdDefParam(UnitTest.class.getMethod("SingleMemberClassOvrdDefParam", Y));
        checkSingleMemberEnumOvrdDefParam(UnitTest.class.getMethod("SingleMemberEnumOvrdDefParam", Y));

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT ON PARAMETER
        checkSingleMemberByteAcceptDefParam(UnitTest.class.getMethod("SingleMemberByteAcceptDefParam", Y));
        checkSingleMemberShortAcceptDefParam(UnitTest.class.getMethod("SingleMemberShortAcceptDefParam", Y));
        checkSingleMemberIntAcceptDefParam(UnitTest.class.getMethod("SingleMemberIntAcceptDefParam", Y));
        checkSingleMemberLongAcceptDefParam(UnitTest.class.getMethod("SingleMemberLongAcceptDefParam", Y));
        checkSingleMemberCharAcceptDefParam(UnitTest.class.getMethod("SingleMemberCharAcceptDefParam", Y));
        checkSingleMemberFloatAcceptDefParam(UnitTest.class.getMethod("SingleMemberFloatAcceptDefParam", Y));
        checkSingleMemberDoubleAcceptDefParam(UnitTest.class.getMethod("SingleMemberDoubleAcceptDefParam", Y));
        checkSingleMemberBooleanAcceptDefParam(UnitTest.class.getMethod("SingleMemberBooleanAcceptDefParam", Y));
        checkSingleMemberStringAcceptDefParam(UnitTest.class.getMethod("SingleMemberStringAcceptDefParam", Y));
        checkSingleMemberClassAcceptDefParam(UnitTest.class.getMethod("SingleMemberClassAcceptDefParam", Y));
        checkSingleMemberEnumAcceptDefParam(UnitTest.class.getMethod("SingleMemberEnumAcceptDefParam", Y));

        // SINGLE-MEMBER ARRAY TYPES Param(UnitTest.class.getMethod("EMPTY ARRAY) ON PARAMETER
        checkSingleMemberByteArrEmptyParam(UnitTest.class.getMethod("SingleMemberByteArrEmptyParam", Y));
        checkSingleMemberShortArrEmptyParam(UnitTest.class.getMethod("SingleMemberShortArrEmptyParam", Y));
        checkSingleMemberIntArrEmptyParam(UnitTest.class.getMethod("SingleMemberIntArrEmptyParam", Y));
        checkSingleMemberLongArrEmptyParam(UnitTest.class.getMethod("SingleMemberLongArrEmptyParam", Y));
        checkSingleMemberCharArrEmptyParam(UnitTest.class.getMethod("SingleMemberCharArrEmptyParam", Y));
        checkSingleMemberFloatArrEmptyParam(UnitTest.class.getMethod("SingleMemberFloatArrEmptyParam", Y));
        checkSingleMemberDoubleArrEmptyParam(UnitTest.class.getMethod("SingleMemberDoubleArrEmptyParam", Y));
        checkSingleMemberBooleanArrEmptyParam(UnitTest.class.getMethod("SingleMemberBooleanArrEmptyParam", Y));
        checkSingleMemberStringArrEmptyParam(UnitTest.class.getMethod("SingleMemberStringArrEmptyParam", Y));
        checkSingleMemberClassArrEmptyParam(UnitTest.class.getMethod("SingleMemberClassArrEmptyParam", Y));
        checkSingleMemberEnumArrEmptyParam(UnitTest.class.getMethod("SingleMemberEnumArrEmptyParam", Y));

        // SINGLE-MEMBER ARRAY TYPES Param(UnitTest.class.getMethod("ONE-ELEMENT ARRAY) ON PARAMETER
        checkSingleMemberByteArrOneParam(UnitTest.class.getMethod("SingleMemberByteArrOneParam", Y));
        checkSingleMemberShortArrOneParam(UnitTest.class.getMethod("SingleMemberShortArrOneParam", Y));
        checkSingleMemberIntArrOneParam(UnitTest.class.getMethod("SingleMemberIntArrOneParam", Y));
        checkSingleMemberLongArrOneParam(UnitTest.class.getMethod("SingleMemberLongArrOneParam", Y));
        checkSingleMemberCharArrOneParam(UnitTest.class.getMethod("SingleMemberCharArrOneParam", Y));
        checkSingleMemberFloatArrOneParam(UnitTest.class.getMethod("SingleMemberFloatArrOneParam", Y));
        checkSingleMemberDoubleArrOneParam(UnitTest.class.getMethod("SingleMemberDoubleArrOneParam", Y));
        checkSingleMemberBooleanArrOneParam(UnitTest.class.getMethod("SingleMemberBooleanArrOneParam", Y));
        checkSingleMemberStringArrOneParam(UnitTest.class.getMethod("SingleMemberStringArrOneParam", Y));
        checkSingleMemberClassArrOneParam(UnitTest.class.getMethod("SingleMemberClassArrOneParam", Y));
        checkSingleMemberEnumArrOneParam(UnitTest.class.getMethod("SingleMemberEnumArrOneParam", Y));

        // SINGLE-MEMBER ARRAY TYPES Param(UnitTest.class.getMethod("TWO-ELEMENT ARRAY) ON PARAMETER
        checkSingleMemberByteArrTwoParam(UnitTest.class.getMethod("SingleMemberByteArrTwoParam", Y));
        checkSingleMemberShortArrTwoParam(UnitTest.class.getMethod("SingleMemberShortArrTwoParam", Y));
        checkSingleMemberIntArrTwoParam(UnitTest.class.getMethod("SingleMemberIntArrTwoParam", Y));
        checkSingleMemberLongArrTwoParam(UnitTest.class.getMethod("SingleMemberLongArrTwoParam", Y));
        checkSingleMemberCharArrTwoParam(UnitTest.class.getMethod("SingleMemberCharArrTwoParam", Y));
        checkSingleMemberFloatArrTwoParam(UnitTest.class.getMethod("SingleMemberFloatArrTwoParam", Y));
        checkSingleMemberDoubleArrTwoParam(UnitTest.class.getMethod("SingleMemberDoubleArrTwoParam", Y));
        checkSingleMemberBooleanArrTwoParam(UnitTest.class.getMethod("SingleMemberBooleanArrTwoParam", Y));
        checkSingleMemberStringArrTwoParam(UnitTest.class.getMethod("SingleMemberStringArrTwoParam", Y));
        checkSingleMemberClassArrTwoParam(UnitTest.class.getMethod("SingleMemberClassArrTwoParam", Y));
        checkSingleMemberEnumArrTwoParam(UnitTest.class.getMethod("SingleMemberEnumArrTwoParam", Y));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT Param(UnitTest.class.getMethod("OVERRIDE)ON PARAMETER
        checkSingleMemberByteArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberByteArrOvrdDefParam", Y));
        checkSingleMemberShortArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberShortArrOvrdDefParam", Y));
        checkSingleMemberIntArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberIntArrOvrdDefParam", Y));
        checkSingleMemberLongArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberLongArrOvrdDefParam", Y));
        checkSingleMemberCharArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberCharArrOvrdDefParam", Y));
        checkSingleMemberFloatArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberFloatArrOvrdDefParam", Y));
        checkSingleMemberDoubleArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberDoubleArrOvrdDefParam", Y));
        checkSingleMemberBooleanArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberBooleanArrOvrdDefParam", Y));
        checkSingleMemberStringArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberStringArrOvrdDefParam", Y));
        checkSingleMemberClassArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberClassArrOvrdDefParam", Y));
        checkSingleMemberEnumArrOvrdDefParam(UnitTest.class.getMethod("SingleMemberEnumArrOvrdDefParam", Y));

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT Param(UnitTest.class.getMethod("ACCEPT)ON PARAMETER
        checkSingleMemberByteArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberByteArrAcceptDefParam", Y));
        checkSingleMemberShortArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberShortArrAcceptDefParam", Y));
        checkSingleMemberIntArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberIntArrAcceptDefParam", Y));
        checkSingleMemberLongArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberLongArrAcceptDefParam", Y));
        checkSingleMemberCharArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberCharArrAcceptDefParam", Y));
        checkSingleMemberFloatArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberFloatArrAcceptDefParam", Y));
        checkSingleMemberDoubleArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberDoubleArrAcceptDefParam", Y));
        checkSingleMemberBooleanArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberBooleanArrAcceptDefParam", Y));
        checkSingleMemberStringArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberStringArrAcceptDefParam", Y));
        checkSingleMemberClassArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberClassArrAcceptDefParam", Y));
        checkSingleMemberEnumArrAcceptDefParam(UnitTest.class.getMethod("SingleMemberEnumArrAcceptDefParam", Y));

        // *** TESTS ON ANNOTATED CLASSES ***

        // MULTIMEMBER SCALAR TYPES ON CLASS
        checkScalarTypes(scalarTypesClass.class);
        checkScalarTypesOverrideDefault(scalarTypesOverrideDefaultClass.class);
        checkScalarTypesAcceptDefault(scalarTypesAcceptDefaultClass.class);

        // MULTIMEMBER ARRAY TYPES ON CLASS
        checkArrayTypes0(emptyArrayTypesClass.class);
        checkArrayTypes1(singleElementArrayTypesClass.class);
        checkArrayTypes2(twoElementArrayTypesClass.class);
        checkArrayTypesOverrideDefault(arrayTypesOverrideDefaultClass.class);
        checkArrayTypesAcceptDefault(arrayTypesAcceptDefaultClass.class);

        // MARKER TYPE ON CLASS
        checkMarker(markerClass.class);

        // SINGLE-MEMBER SCALAR TYPES ON CLASS
        checkSingleMemberByte(SingleMemberByteClass.class);
        checkSingleMemberShort(SingleMemberShortClass.class);
        checkSingleMemberInt(SingleMemberIntClass.class);
        checkSingleMemberLong(SingleMemberLongClass.class);
        checkSingleMemberChar(SingleMemberCharClass.class);
        checkSingleMemberFloat(SingleMemberFloatClass.class);
        checkSingleMemberDouble(SingleMemberDoubleClass.class);
        checkSingleMemberBoolean(SingleMemberBooleanClass.class);
        checkSingleMemberString(SingleMemberStringClass.class);
        checkSingleMemberClass(SingleMemberClassClass.class);
        checkSingleMemberEnum(SingleMemberEnumClass.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE ON CLASS
        checkSingleMemberByteOvrdDef(SingleMemberByteOvrdDefClass.class);
        checkSingleMemberShortOvrdDef(SingleMemberShortOvrdDefClass.class);
        checkSingleMemberIntOvrdDef(SingleMemberIntOvrdDefClass.class);
        checkSingleMemberLongOvrdDef(SingleMemberLongOvrdDefClass.class);
        checkSingleMemberCharOvrdDef(SingleMemberCharOvrdDefClass.class);
        checkSingleMemberFloatOvrdDef(SingleMemberFloatOvrdDefClass.class);
        checkSingleMemberDoubleOvrdDef(SingleMemberDoubleOvrdDefClass.class);
        checkSingleMemberBooleanOvrdDef(SingleMemberBooleanOvrdDefClass.class);
        checkSingleMemberStringOvrdDef(SingleMemberStringOvrdDefClass.class);
        checkSingleMemberClassOvrdDef(SingleMemberClassOvrdDefClass.class);
        checkSingleMemberEnumOvrdDef(SingleMemberEnumOvrdDefClass.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT ON CLASS
        checkSingleMemberByteAcceptDef(SingleMemberByteAcceptDefClass.class);
        checkSingleMemberShortAcceptDef(SingleMemberShortAcceptDefClass.class);
        checkSingleMemberIntAcceptDef(SingleMemberIntAcceptDefClass.class);
        checkSingleMemberLongAcceptDef(SingleMemberLongAcceptDefClass.class);
        checkSingleMemberCharAcceptDef(SingleMemberCharAcceptDefClass.class);
        checkSingleMemberFloatAcceptDef(SingleMemberFloatAcceptDefClass.class);
        checkSingleMemberDoubleAcceptDef(SingleMemberDoubleAcceptDefClass.class);
        checkSingleMemberBooleanAcceptDef(SingleMemberBooleanAcceptDefClass.class);
        checkSingleMemberStringAcceptDef(SingleMemberStringAcceptDefClass.class);
        checkSingleMemberClassAcceptDef(SingleMemberClassAcceptDefClass.class);
        checkSingleMemberEnumAcceptDef(SingleMemberEnumAcceptDefClass.class);

        // SINGLE-MEMBER ARRAY TYPES (EMPTY ARRAY) ON CLASS
        checkSingleMemberByteArrEmpty(SingleMemberByteArrEmptyClass.class);
        checkSingleMemberShortArrEmpty(SingleMemberShortArrEmptyClass.class);
        checkSingleMemberIntArrEmpty(SingleMemberIntArrEmptyClass.class);
        checkSingleMemberLongArrEmpty(SingleMemberLongArrEmptyClass.class);
        checkSingleMemberCharArrEmpty(SingleMemberCharArrEmptyClass.class);
        checkSingleMemberFloatArrEmpty(SingleMemberFloatArrEmptyClass.class);
        checkSingleMemberDoubleArrEmpty(SingleMemberDoubleArrEmptyClass.class);
        checkSingleMemberBooleanArrEmpty(SingleMemberBooleanArrEmptyClass.class);
        checkSingleMemberStringArrEmpty(SingleMemberStringArrEmptyClass.class);
        checkSingleMemberClassArrEmpty(SingleMemberClassArrEmptyClass.class);
        checkSingleMemberEnumArrEmpty(SingleMemberEnumArrEmptyClass.class);

        // SINGLE-MEMBER ARRAY TYPES (ONE-ELEMENT ARRAY) ON CLASS
        checkSingleMemberByteArrOne(SingleMemberByteArrOneClass.class);
        checkSingleMemberShortArrOne(SingleMemberShortArrOneClass.class);
        checkSingleMemberIntArrOne(SingleMemberIntArrOneClass.class);
        checkSingleMemberLongArrOne(SingleMemberLongArrOneClass.class);
        checkSingleMemberCharArrOne(SingleMemberCharArrOneClass.class);
        checkSingleMemberFloatArrOne(SingleMemberFloatArrOneClass.class);
        checkSingleMemberDoubleArrOne(SingleMemberDoubleArrOneClass.class);
        checkSingleMemberBooleanArrOne(SingleMemberBooleanArrOneClass.class);
        checkSingleMemberStringArrOne(SingleMemberStringArrOneClass.class);
        checkSingleMemberClassArrOne(SingleMemberClassArrOneClass.class);
        checkSingleMemberEnumArrOne(SingleMemberEnumArrOneClass.class);

        // SINGLE-MEMBER ARRAY TYPES (TWO-ELEMENT ARRAY) ON CLASS
        checkSingleMemberByteArrTwo(SingleMemberByteArrTwoClass.class);
        checkSingleMemberShortArrTwo(SingleMemberShortArrTwoClass.class);
        checkSingleMemberIntArrTwo(SingleMemberIntArrTwoClass.class);
        checkSingleMemberLongArrTwo(SingleMemberLongArrTwoClass.class);
        checkSingleMemberCharArrTwo(SingleMemberCharArrTwoClass.class);
        checkSingleMemberFloatArrTwo(SingleMemberFloatArrTwoClass.class);
        checkSingleMemberDoubleArrTwo(SingleMemberDoubleArrTwoClass.class);
        checkSingleMemberBooleanArrTwo(SingleMemberBooleanArrTwoClass.class);
        checkSingleMemberStringArrTwo(SingleMemberStringArrTwoClass.class);
        checkSingleMemberClassArrTwo(SingleMemberClassArrTwoClass.class);
        checkSingleMemberEnumArrTwo(SingleMemberEnumArrTwoClass.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (OVERRIDE)ON CLASS
        checkSingleMemberByteArrOvrdDef(SingleMemberByteArrOvrdDefClass.class);
        checkSingleMemberShortArrOvrdDef(SingleMemberShortArrOvrdDefClass.class);
        checkSingleMemberIntArrOvrdDef(SingleMemberIntArrOvrdDefClass.class);
        checkSingleMemberLongArrOvrdDef(SingleMemberLongArrOvrdDefClass.class);
        checkSingleMemberCharArrOvrdDef(SingleMemberCharArrOvrdDefClass.class);
        checkSingleMemberFloatArrOvrdDef(SingleMemberFloatArrOvrdDefClass.class);
        checkSingleMemberDoubleArrOvrdDef(SingleMemberDoubleArrOvrdDefClass.class);
        checkSingleMemberBooleanArrOvrdDef(SingleMemberBooleanArrOvrdDefClass.class);
        checkSingleMemberStringArrOvrdDef(SingleMemberStringArrOvrdDefClass.class);
        checkSingleMemberClassArrOvrdDef(SingleMemberClassArrOvrdDefClass.class);
        checkSingleMemberEnumArrOvrdDef(SingleMemberEnumArrOvrdDefClass.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (ACCEPT)ON CLASS
        checkSingleMemberByteArrAcceptDef(SingleMemberByteArrAcceptDefClass.class);
        checkSingleMemberShortArrAcceptDef(SingleMemberShortArrAcceptDefClass.class);
        checkSingleMemberIntArrAcceptDef(SingleMemberIntArrAcceptDefClass.class);
        checkSingleMemberLongArrAcceptDef(SingleMemberLongArrAcceptDefClass.class);
        checkSingleMemberCharArrAcceptDef(SingleMemberCharArrAcceptDefClass.class);
        checkSingleMemberFloatArrAcceptDef(SingleMemberFloatArrAcceptDefClass.class);
        checkSingleMemberDoubleArrAcceptDef(SingleMemberDoubleArrAcceptDefClass.class);
        checkSingleMemberBooleanArrAcceptDef(SingleMemberBooleanArrAcceptDefClass.class);
        checkSingleMemberStringArrAcceptDef(SingleMemberStringArrAcceptDefClass.class);
        checkSingleMemberClassArrAcceptDef(SingleMemberClassArrAcceptDefClass.class);
        checkSingleMemberEnumArrAcceptDef(SingleMemberEnumArrAcceptDefClass.class);

        // *** TESTS FOR EQUALS AND HASHCODE - POSITIVE

        // MULTIMEMBER SCALAR TYPES
        checkEquals(scalarTypesClass.class, UnitTest.class.getField("scalarTypesField"),
                    ScalarTypes.class);
        checkEquals(scalarTypesOverrideDefaultClass.class, UnitTest.class.getField("scalarTypesOverrideDefaultField"),
                    ScalarTypesWithDefault.class);
        checkEquals(scalarTypesAcceptDefaultClass.class, UnitTest.class.getField("scalarTypesAcceptDefaultField"),
                    ScalarTypesWithDefault.class);

        // MULTIMEMBER ARRAY TYPES
        checkEquals(emptyArrayTypesClass.class, UnitTest.class.getField("emptyArrayTypesField"),
                    ArrayTypes.class);
        checkEquals(singleElementArrayTypesClass.class, UnitTest.class.getField("singleElementArrayTypesField"),
                    ArrayTypes.class);
        checkEquals(twoElementArrayTypesClass.class, UnitTest.class.getField("twoElementArrayTypesField"),
                    ArrayTypes.class);
        checkEquals(arrayTypesOverrideDefaultClass.class, UnitTest.class.getField("arrayTypesOverrideDefaultField"),
                    ArrayTypesWithDefault.class);
        checkEquals(arrayTypesAcceptDefaultClass.class, UnitTest.class.getField("arrayTypesAcceptDefaultField"),
                    ArrayTypesWithDefault.class);

        // MARKER TYPE
        checkEquals(markerClass.class, UnitTest.class.getField("markerField"),
                    Marker.class);

        // SINGLE-MEMBER SCALAR TYPES
        checkEquals(SingleMemberByteClass.class, UnitTest.class.getField("SingleMemberByteField"),
                    SingleMemberByte.class);
        checkEquals(SingleMemberShortClass.class, UnitTest.class.getField("SingleMemberShortField"),
                    SingleMemberShort.class);
        checkEquals(SingleMemberIntClass.class, UnitTest.class.getField("SingleMemberIntField"),
                    SingleMemberInt.class);
        checkEquals(SingleMemberLongClass.class, UnitTest.class.getField("SingleMemberLongField"),
                    SingleMemberLong.class);
        checkEquals(SingleMemberCharClass.class, UnitTest.class.getField("SingleMemberCharField"),
                    SingleMemberChar.class);
        checkEquals(SingleMemberFloatClass.class, UnitTest.class.getField("SingleMemberFloatField"),
                    SingleMemberFloat.class);
        checkEquals(SingleMemberDoubleClass.class, UnitTest.class.getField("SingleMemberDoubleField"),
                    SingleMemberDouble.class);
        checkEquals(SingleMemberBooleanClass.class, UnitTest.class.getField("SingleMemberBooleanField"),
                    SingleMemberBoolean.class);
        checkEquals(SingleMemberStringClass.class, UnitTest.class.getField("SingleMemberStringField"),
                    SingleMemberString.class);
        checkEquals(SingleMemberClassClass.class, UnitTest.class.getField("SingleMemberClassField"),
                    SingleMemberClass.class);
        checkEquals(SingleMemberEnumClass.class, UnitTest.class.getField("SingleMemberEnumField"),
                    SingleMemberEnum.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE
        checkEquals(SingleMemberByteOvrdDefClass.class, UnitTest.class.getField("SingleMemberByteOvrdDefField"),
                    SingleMemberByteWithDef.class);
        checkEquals(SingleMemberShortOvrdDefClass.class, UnitTest.class.getField("SingleMemberShortOvrdDefField"),
                    SingleMemberShortWithDef.class);
        checkEquals(SingleMemberIntOvrdDefClass.class, UnitTest.class.getField("SingleMemberIntOvrdDefField"),
                    SingleMemberIntWithDef.class);
        checkEquals(SingleMemberLongOvrdDefClass.class, UnitTest.class.getField("SingleMemberLongOvrdDefField"),
                    SingleMemberLongWithDef.class);
        checkEquals(SingleMemberCharOvrdDefClass.class, UnitTest.class.getField("SingleMemberCharOvrdDefField"),
                    SingleMemberCharWithDef.class);
        checkEquals(SingleMemberFloatOvrdDefClass.class, UnitTest.class.getField("SingleMemberFloatOvrdDefField"),
                    SingleMemberFloatWithDef.class);
        checkEquals(SingleMemberDoubleOvrdDefClass.class, UnitTest.class.getField("SingleMemberDoubleOvrdDefField"),
                    SingleMemberDoubleWithDef.class);
        checkEquals(SingleMemberBooleanOvrdDefClass.class, UnitTest.class.getField("SingleMemberBooleanOvrdDefField"),
                    SingleMemberBooleanWithDef.class);
        checkEquals(SingleMemberStringOvrdDefClass.class, UnitTest.class.getField("SingleMemberStringOvrdDefField"),
                    SingleMemberStringWithDef.class);
        checkEquals(SingleMemberClassOvrdDefClass.class, UnitTest.class.getField("SingleMemberClassOvrdDefField"),
                    SingleMemberClassWithDef.class);
        checkEquals(SingleMemberEnumOvrdDefClass.class, UnitTest.class.getField("SingleMemberEnumOvrdDefField"),
                    SingleMemberEnumWithDef.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT
        checkEquals(SingleMemberByteAcceptDefClass.class, UnitTest.class.getField("SingleMemberByteAcceptDefField"),
                    SingleMemberByteWithDef.class);
        checkEquals(SingleMemberShortAcceptDefClass.class, UnitTest.class.getField("SingleMemberShortAcceptDefField"),
                    SingleMemberShortWithDef.class);
        checkEquals(SingleMemberIntAcceptDefClass.class, UnitTest.class.getField("SingleMemberIntAcceptDefField"),
                    SingleMemberIntWithDef.class);
        checkEquals(SingleMemberLongAcceptDefClass.class, UnitTest.class.getField("SingleMemberLongAcceptDefField"),
                    SingleMemberLongWithDef.class);
        checkEquals(SingleMemberCharAcceptDefClass.class, UnitTest.class.getField("SingleMemberCharAcceptDefField"),
                    SingleMemberCharWithDef.class);
        checkEquals(SingleMemberFloatAcceptDefClass.class, UnitTest.class.getField("SingleMemberFloatAcceptDefField"),
                    SingleMemberFloatWithDef.class);
        checkEquals(SingleMemberDoubleAcceptDefClass.class, UnitTest.class.getField("SingleMemberDoubleAcceptDefField"),
                    SingleMemberDoubleWithDef.class);
        checkEquals(SingleMemberBooleanAcceptDefClass.class, UnitTest.class.getField("SingleMemberBooleanAcceptDefField"),
                    SingleMemberBooleanWithDef.class);
        checkEquals(SingleMemberStringAcceptDefClass.class, UnitTest.class.getField("SingleMemberStringAcceptDefField"),
                    SingleMemberStringWithDef.class);
        checkEquals(SingleMemberClassAcceptDefClass.class, UnitTest.class.getField("SingleMemberClassAcceptDefField"),
                    SingleMemberClassWithDef.class);
        checkEquals(SingleMemberEnumAcceptDefClass.class, UnitTest.class.getField("SingleMemberEnumAcceptDefField"),
                    SingleMemberEnumWithDef.class);

        // SINGLE-MEMBER ARRAY TYPES (EMPTY ARRAY)
        checkEquals(SingleMemberByteArrEmptyClass.class, UnitTest.class.getField("SingleMemberByteArrEmptyField"),
                    SingleMemberByteArray.class);
        checkEquals(SingleMemberShortArrEmptyClass.class, UnitTest.class.getField("SingleMemberShortArrEmptyField"),
                    SingleMemberShortArray.class);
        checkEquals(SingleMemberIntArrEmptyClass.class, UnitTest.class.getField("SingleMemberIntArrEmptyField"),
                    SingleMemberIntArray.class);
        checkEquals(SingleMemberLongArrEmptyClass.class, UnitTest.class.getField("SingleMemberLongArrEmptyField"),
                    SingleMemberLongArray.class);
        checkEquals(SingleMemberCharArrEmptyClass.class, UnitTest.class.getField("SingleMemberCharArrEmptyField"),
                    SingleMemberCharArray.class);
        checkEquals(SingleMemberFloatArrEmptyClass.class, UnitTest.class.getField("SingleMemberFloatArrEmptyField"),
                    SingleMemberFloatArray.class);
        checkEquals(SingleMemberDoubleArrEmptyClass.class, UnitTest.class.getField("SingleMemberDoubleArrEmptyField"),
                    SingleMemberDoubleArray.class);
        checkEquals(SingleMemberBooleanArrEmptyClass.class, UnitTest.class.getField("SingleMemberBooleanArrEmptyField"),
                    SingleMemberBooleanArray.class);
        checkEquals(SingleMemberStringArrEmptyClass.class, UnitTest.class.getField("SingleMemberStringArrEmptyField"),
                    SingleMemberStringArray.class);
        checkEquals(SingleMemberClassArrEmptyClass.class, UnitTest.class.getField("SingleMemberClassArrEmptyField"),
                    SingleMemberClassArray.class);
        checkEquals(SingleMemberEnumArrEmptyClass.class, UnitTest.class.getField("SingleMemberEnumArrEmptyField"),
                    SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES (ONE-ELEMENT ARRAY)
        checkEquals(SingleMemberByteArrOneClass.class, UnitTest.class.getField("SingleMemberByteArrOneField"),
                    SingleMemberByteArray.class);
        checkEquals(SingleMemberShortArrOneClass.class, UnitTest.class.getField("SingleMemberShortArrOneField"),
                    SingleMemberShortArray.class);
        checkEquals(SingleMemberIntArrOneClass.class, UnitTest.class.getField("SingleMemberIntArrOneField"),
                    SingleMemberIntArray.class);
        checkEquals(SingleMemberLongArrOneClass.class, UnitTest.class.getField("SingleMemberLongArrOneField"),
                    SingleMemberLongArray.class);
        checkEquals(SingleMemberCharArrOneClass.class, UnitTest.class.getField("SingleMemberCharArrOneField"),
                    SingleMemberCharArray.class);
        checkEquals(SingleMemberFloatArrOneClass.class, UnitTest.class.getField("SingleMemberFloatArrOneField"),
                    SingleMemberFloatArray.class);
        checkEquals(SingleMemberDoubleArrOneClass.class, UnitTest.class.getField("SingleMemberDoubleArrOneField"),
                    SingleMemberDoubleArray.class);
        checkEquals(SingleMemberBooleanArrOneClass.class, UnitTest.class.getField("SingleMemberBooleanArrOneField"),
                    SingleMemberBooleanArray.class);
        checkEquals(SingleMemberStringArrOneClass.class, UnitTest.class.getField("SingleMemberStringArrOneField"),
                    SingleMemberStringArray.class);
        checkEquals(SingleMemberClassArrOneClass.class, UnitTest.class.getField("SingleMemberClassArrOneField"),
                    SingleMemberClassArray.class);
        checkEquals(SingleMemberEnumArrOneClass.class, UnitTest.class.getField("SingleMemberEnumArrOneField"),
                    SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES (TWO-ELEMENT ARRAY)
        checkEquals(SingleMemberByteArrTwoClass.class, UnitTest.class.getField("SingleMemberByteArrTwoField"),
                    SingleMemberByteArray.class);
        checkEquals(SingleMemberShortArrTwoClass.class, UnitTest.class.getField("SingleMemberShortArrTwoField"),
                    SingleMemberShortArray.class);
        checkEquals(SingleMemberIntArrTwoClass.class, UnitTest.class.getField("SingleMemberIntArrTwoField"),
                    SingleMemberIntArray.class);
        checkEquals(SingleMemberLongArrTwoClass.class, UnitTest.class.getField("SingleMemberLongArrTwoField"),
                    SingleMemberLongArray.class);
        checkEquals(SingleMemberCharArrTwoClass.class, UnitTest.class.getField("SingleMemberCharArrTwoField"),
                    SingleMemberCharArray.class);
        checkEquals(SingleMemberFloatArrTwoClass.class, UnitTest.class.getField("SingleMemberFloatArrTwoField"),
                    SingleMemberFloatArray.class);
        checkEquals(SingleMemberDoubleArrTwoClass.class, UnitTest.class.getField("SingleMemberDoubleArrTwoField"),
                    SingleMemberDoubleArray.class);
        checkEquals(SingleMemberBooleanArrTwoClass.class, UnitTest.class.getField("SingleMemberBooleanArrTwoField"),
                    SingleMemberBooleanArray.class);
        checkEquals(SingleMemberStringArrTwoClass.class, UnitTest.class.getField("SingleMemberStringArrTwoField"),
                    SingleMemberStringArray.class);
        checkEquals(SingleMemberClassArrTwoClass.class, UnitTest.class.getField("SingleMemberClassArrTwoField"),
                    SingleMemberClassArray.class);
        checkEquals(SingleMemberEnumArrTwoClass.class, UnitTest.class.getField("SingleMemberEnumArrTwoField"),
                    SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (OVERRIDE)
        checkEquals(SingleMemberByteArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberByteArrOvrdDefField"),
                    SingleMemberByteArrayDef.class);
        checkEquals(SingleMemberShortArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberShortArrOvrdDefField"),
                    SingleMemberShortArrayDef.class);
        checkEquals(SingleMemberIntArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberIntArrOvrdDefField"),
                    SingleMemberIntArrayDef.class);
        checkEquals(SingleMemberLongArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberLongArrOvrdDefField"),
                    SingleMemberLongArrayDef.class);
        checkEquals(SingleMemberCharArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberCharArrOvrdDefField"),
                    SingleMemberCharArrayDef.class);
        checkEquals(SingleMemberFloatArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberFloatArrOvrdDefField"),
                    SingleMemberFloatArrayDef.class);
        checkEquals(SingleMemberDoubleArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberDoubleArrOvrdDefField"),
                    SingleMemberDoubleArrayDef.class);
        checkEquals(SingleMemberBooleanArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberBooleanArrOvrdDefField"),
                    SingleMemberBooleanArrayDef.class);
        checkEquals(SingleMemberStringArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberStringArrOvrdDefField"),
                    SingleMemberStringArrayDef.class);
        checkEquals(SingleMemberClassArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberClassArrOvrdDefField"),
                    SingleMemberClassArrayDef.class);
        checkEquals(SingleMemberEnumArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberEnumArrOvrdDefField"),
                    SingleMemberEnumArrayDef.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (ACCEPT)
        checkEquals(SingleMemberByteArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberByteArrAcceptDefField"),
                    SingleMemberByteArrayDef.class);
        checkEquals(SingleMemberShortArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberShortArrAcceptDefField"),
                    SingleMemberShortArrayDef.class);
        checkEquals(SingleMemberIntArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberIntArrAcceptDefField"),
                    SingleMemberIntArrayDef.class);
        checkEquals(SingleMemberLongArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberLongArrAcceptDefField"),
                    SingleMemberLongArrayDef.class);
        checkEquals(SingleMemberCharArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberCharArrAcceptDefField"),
                    SingleMemberCharArrayDef.class);
        checkEquals(SingleMemberFloatArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberFloatArrAcceptDefField"),
                    SingleMemberFloatArrayDef.class);
        checkEquals(SingleMemberDoubleArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberDoubleArrAcceptDefField"),
                    SingleMemberDoubleArrayDef.class);
        checkEquals(SingleMemberBooleanArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberBooleanArrAcceptDefField"),
                    SingleMemberBooleanArrayDef.class);
        checkEquals(SingleMemberStringArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberStringArrAcceptDefField"),
                    SingleMemberStringArrayDef.class);
        checkEquals(SingleMemberClassArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberClassArrAcceptDefField"),
                    SingleMemberClassArrayDef.class);
        checkEquals(SingleMemberEnumArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberEnumArrAcceptDefField"),
                    SingleMemberEnumArrayDef.class);

        // *** TESTS FOR EQUALS AND HASHCODE - NEGATIVE

        // MULTIMEMBER SCALAR TYPES
        checkUnequals(scalarTypesOverrideDefaultClass.class, UnitTest.class.getField("scalarTypesAcceptDefaultField"),
                    ScalarTypesWithDefault.class);
        checkUnequals(scalarTypesAcceptDefaultClass.class, UnitTest.class.getField("scalarTypesOverrideDefaultField"),
                    ScalarTypesWithDefault.class);

        // MULTIMEMBER ARRAY TYPES
        checkUnequals(emptyArrayTypesClass.class, UnitTest.class.getField("singleElementArrayTypesField"),
                    ArrayTypes.class);
        checkUnequals(singleElementArrayTypesClass.class, UnitTest.class.getField("twoElementArrayTypesField"),
                    ArrayTypes.class);
        checkUnequals(twoElementArrayTypesClass.class, UnitTest.class.getField("singleElementArrayTypesField"),
                    ArrayTypes.class);
        checkUnequals(arrayTypesOverrideDefaultClass.class, UnitTest.class.getField("arrayTypesAcceptDefaultField"),
                    ArrayTypesWithDefault.class);
        checkUnequals(arrayTypesAcceptDefaultClass.class, UnitTest.class.getField("arrayTypesOverrideDefaultField"),
                    ArrayTypesWithDefault.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE
        checkUnequals(SingleMemberByteOvrdDefClass.class, UnitTest.class.getField("SingleMemberByteAcceptDefField"),
                    SingleMemberByteWithDef.class);
        checkUnequals(SingleMemberShortOvrdDefClass.class, UnitTest.class.getField("SingleMemberShortAcceptDefField"),
                    SingleMemberShortWithDef.class);
        checkUnequals(SingleMemberIntOvrdDefClass.class, UnitTest.class.getField("SingleMemberIntAcceptDefField"),
                    SingleMemberIntWithDef.class);
        checkUnequals(SingleMemberLongOvrdDefClass.class, UnitTest.class.getField("SingleMemberLongAcceptDefField"),
                    SingleMemberLongWithDef.class);
        checkUnequals(SingleMemberCharOvrdDefClass.class, UnitTest.class.getField("SingleMemberCharAcceptDefField"),
                    SingleMemberCharWithDef.class);
        checkUnequals(SingleMemberFloatOvrdDefClass.class, UnitTest.class.getField("SingleMemberFloatAcceptDefField"),
                    SingleMemberFloatWithDef.class);
        checkUnequals(SingleMemberDoubleOvrdDefClass.class, UnitTest.class.getField("SingleMemberDoubleAcceptDefField"),
                    SingleMemberDoubleWithDef.class);
        checkUnequals(SingleMemberBooleanOvrdDefClass.class, UnitTest.class.getField("SingleMemberBooleanAcceptDefField"),
                    SingleMemberBooleanWithDef.class);
        checkUnequals(SingleMemberStringOvrdDefClass.class, UnitTest.class.getField("SingleMemberStringAcceptDefField"),
                    SingleMemberStringWithDef.class);
        checkUnequals(SingleMemberClassOvrdDefClass.class, UnitTest.class.getField("SingleMemberClassAcceptDefField"),
                    SingleMemberClassWithDef.class);
        checkUnequals(SingleMemberEnumOvrdDefClass.class, UnitTest.class.getField("SingleMemberEnumAcceptDefField"),
                    SingleMemberEnumWithDef.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT
        checkUnequals(SingleMemberByteAcceptDefClass.class, UnitTest.class.getField("SingleMemberByteOvrdDefField"),
                    SingleMemberByteWithDef.class);
        checkUnequals(SingleMemberShortAcceptDefClass.class, UnitTest.class.getField("SingleMemberShortOvrdDefField"),
                    SingleMemberShortWithDef.class);
        checkUnequals(SingleMemberIntAcceptDefClass.class, UnitTest.class.getField("SingleMemberIntOvrdDefField"),
                    SingleMemberIntWithDef.class);
        checkUnequals(SingleMemberLongAcceptDefClass.class, UnitTest.class.getField("SingleMemberLongOvrdDefField"),
                    SingleMemberLongWithDef.class);
        checkUnequals(SingleMemberCharAcceptDefClass.class, UnitTest.class.getField("SingleMemberCharOvrdDefField"),
                    SingleMemberCharWithDef.class);
        checkUnequals(SingleMemberFloatAcceptDefClass.class, UnitTest.class.getField("SingleMemberFloatOvrdDefField"),
                    SingleMemberFloatWithDef.class);
        checkUnequals(SingleMemberDoubleAcceptDefClass.class, UnitTest.class.getField("SingleMemberDoubleOvrdDefField"),
                    SingleMemberDoubleWithDef.class);
        checkUnequals(SingleMemberBooleanAcceptDefClass.class, UnitTest.class.getField("SingleMemberBooleanOvrdDefField"),
                    SingleMemberBooleanWithDef.class);
        checkUnequals(SingleMemberStringAcceptDefClass.class, UnitTest.class.getField("SingleMemberStringOvrdDefField"),
                    SingleMemberStringWithDef.class);
        checkUnequals(SingleMemberClassAcceptDefClass.class, UnitTest.class.getField("SingleMemberClassOvrdDefField"),
                    SingleMemberClassWithDef.class);
        checkUnequals(SingleMemberEnumAcceptDefClass.class, UnitTest.class.getField("SingleMemberEnumOvrdDefField"),
                    SingleMemberEnumWithDef.class);

        // SINGLE-MEMBER ARRAY TYPES (EMPTY ARRAY)
        checkUnequals(SingleMemberByteArrEmptyClass.class, UnitTest.class.getField("SingleMemberByteArrOneField"),
                    SingleMemberByteArray.class);
        checkUnequals(SingleMemberShortArrEmptyClass.class, UnitTest.class.getField("SingleMemberShortArrOneField"),
                    SingleMemberShortArray.class);
        checkUnequals(SingleMemberIntArrEmptyClass.class, UnitTest.class.getField("SingleMemberIntArrOneField"),
                    SingleMemberIntArray.class);
        checkUnequals(SingleMemberLongArrEmptyClass.class, UnitTest.class.getField("SingleMemberLongArrOneField"),
                    SingleMemberLongArray.class);
        checkUnequals(SingleMemberCharArrEmptyClass.class, UnitTest.class.getField("SingleMemberCharArrOneField"),
                    SingleMemberCharArray.class);
        checkUnequals(SingleMemberFloatArrEmptyClass.class, UnitTest.class.getField("SingleMemberFloatArrOneField"),
                    SingleMemberFloatArray.class);
        checkUnequals(SingleMemberDoubleArrEmptyClass.class, UnitTest.class.getField("SingleMemberDoubleArrOneField"),
                    SingleMemberDoubleArray.class);
        checkUnequals(SingleMemberBooleanArrEmptyClass.class, UnitTest.class.getField("SingleMemberBooleanArrOneField"),
                    SingleMemberBooleanArray.class);
        checkUnequals(SingleMemberStringArrEmptyClass.class, UnitTest.class.getField("SingleMemberStringArrOneField"),
                    SingleMemberStringArray.class);
        checkUnequals(SingleMemberClassArrEmptyClass.class, UnitTest.class.getField("SingleMemberClassArrOneField"),
                    SingleMemberClassArray.class);
        checkUnequals(SingleMemberEnumArrEmptyClass.class, UnitTest.class.getField("SingleMemberEnumArrOneField"),
                    SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES (ONE-ELEMENT ARRAY)
        checkUnequals(SingleMemberByteArrOneClass.class, UnitTest.class.getField("SingleMemberByteArrTwoField"),
                    SingleMemberByteArray.class);
        checkUnequals(SingleMemberShortArrOneClass.class, UnitTest.class.getField("SingleMemberShortArrTwoField"),
                    SingleMemberShortArray.class);
        checkUnequals(SingleMemberIntArrOneClass.class, UnitTest.class.getField("SingleMemberIntArrTwoField"),
                    SingleMemberIntArray.class);
        checkUnequals(SingleMemberLongArrOneClass.class, UnitTest.class.getField("SingleMemberLongArrTwoField"),
                    SingleMemberLongArray.class);
        checkUnequals(SingleMemberCharArrOneClass.class, UnitTest.class.getField("SingleMemberCharArrTwoField"),
                    SingleMemberCharArray.class);
        checkUnequals(SingleMemberFloatArrOneClass.class, UnitTest.class.getField("SingleMemberFloatArrTwoField"),
                    SingleMemberFloatArray.class);
        checkUnequals(SingleMemberDoubleArrOneClass.class, UnitTest.class.getField("SingleMemberDoubleArrTwoField"),
                    SingleMemberDoubleArray.class);
        checkUnequals(SingleMemberBooleanArrOneClass.class, UnitTest.class.getField("SingleMemberBooleanArrTwoField"),
                    SingleMemberBooleanArray.class);
        checkUnequals(SingleMemberStringArrOneClass.class, UnitTest.class.getField("SingleMemberStringArrTwoField"),
                    SingleMemberStringArray.class);
        checkUnequals(SingleMemberClassArrOneClass.class, UnitTest.class.getField("SingleMemberClassArrTwoField"),
                    SingleMemberClassArray.class);
        checkUnequals(SingleMemberEnumArrOneClass.class, UnitTest.class.getField("SingleMemberEnumArrTwoField"),
                    SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES (TWO-ELEMENT ARRAY)
        checkUnequals(SingleMemberByteArrTwoClass.class, UnitTest.class.getField("SingleMemberByteArrOneField"),
                    SingleMemberByteArray.class);
        checkUnequals(SingleMemberShortArrTwoClass.class, UnitTest.class.getField("SingleMemberShortArrOneField"),
                    SingleMemberShortArray.class);
        checkUnequals(SingleMemberIntArrTwoClass.class, UnitTest.class.getField("SingleMemberIntArrOneField"),
                    SingleMemberIntArray.class);
        checkUnequals(SingleMemberLongArrTwoClass.class, UnitTest.class.getField("SingleMemberLongArrOneField"),
                    SingleMemberLongArray.class);
        checkUnequals(SingleMemberCharArrTwoClass.class, UnitTest.class.getField("SingleMemberCharArrOneField"),
                    SingleMemberCharArray.class);
        checkUnequals(SingleMemberFloatArrTwoClass.class, UnitTest.class.getField("SingleMemberFloatArrOneField"),
                    SingleMemberFloatArray.class);
        checkUnequals(SingleMemberDoubleArrTwoClass.class, UnitTest.class.getField("SingleMemberDoubleArrOneField"),
                    SingleMemberDoubleArray.class);
        checkUnequals(SingleMemberBooleanArrTwoClass.class, UnitTest.class.getField("SingleMemberBooleanArrOneField"),
                    SingleMemberBooleanArray.class);
        checkUnequals(SingleMemberStringArrTwoClass.class, UnitTest.class.getField("SingleMemberStringArrOneField"),
                    SingleMemberStringArray.class);
        checkUnequals(SingleMemberClassArrTwoClass.class, UnitTest.class.getField("SingleMemberClassArrOneField"),
                    SingleMemberClassArray.class);
        checkUnequals(SingleMemberEnumArrTwoClass.class, UnitTest.class.getField("SingleMemberEnumArrOneField"),
                    SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (OVERRIDE)
        checkUnequals(SingleMemberByteArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberByteArrAcceptDefField"),
                    SingleMemberByteArrayDef.class);
        checkUnequals(SingleMemberShortArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberShortArrAcceptDefField"),
                    SingleMemberShortArrayDef.class);
        checkUnequals(SingleMemberIntArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberIntArrAcceptDefField"),
                    SingleMemberIntArrayDef.class);
        checkUnequals(SingleMemberLongArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberLongArrAcceptDefField"),
                    SingleMemberLongArrayDef.class);
        checkUnequals(SingleMemberCharArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberCharArrAcceptDefField"),
                    SingleMemberCharArrayDef.class);
        checkUnequals(SingleMemberFloatArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberFloatArrAcceptDefField"),
                    SingleMemberFloatArrayDef.class);
        checkUnequals(SingleMemberDoubleArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberDoubleArrAcceptDefField"),
                    SingleMemberDoubleArrayDef.class);
        checkUnequals(SingleMemberBooleanArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberBooleanArrAcceptDefField"),
                    SingleMemberBooleanArrayDef.class);
        checkUnequals(SingleMemberStringArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberStringArrAcceptDefField"),
                    SingleMemberStringArrayDef.class);
        checkUnequals(SingleMemberClassArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberClassArrAcceptDefField"),
                    SingleMemberClassArrayDef.class);
        checkUnequals(SingleMemberEnumArrOvrdDefClass.class, UnitTest.class.getField("SingleMemberEnumArrAcceptDefField"),
                    SingleMemberEnumArrayDef.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (ACCEPT)
        checkUnequals(SingleMemberByteArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberByteArrOvrdDefField"),
                    SingleMemberByteArrayDef.class);
        checkUnequals(SingleMemberShortArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberShortArrOvrdDefField"),
                    SingleMemberShortArrayDef.class);
        checkUnequals(SingleMemberIntArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberIntArrOvrdDefField"),
                    SingleMemberIntArrayDef.class);
        checkUnequals(SingleMemberLongArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberLongArrOvrdDefField"),
                    SingleMemberLongArrayDef.class);
        checkUnequals(SingleMemberCharArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberCharArrOvrdDefField"),
                    SingleMemberCharArrayDef.class);
        checkUnequals(SingleMemberFloatArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberFloatArrOvrdDefField"),
                    SingleMemberFloatArrayDef.class);
        checkUnequals(SingleMemberDoubleArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberDoubleArrOvrdDefField"),
                    SingleMemberDoubleArrayDef.class);
        checkUnequals(SingleMemberBooleanArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberBooleanArrOvrdDefField"),
                    SingleMemberBooleanArrayDef.class);
        checkUnequals(SingleMemberStringArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberStringArrOvrdDefField"),
                    SingleMemberStringArrayDef.class);
        checkUnequals(SingleMemberClassArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberClassArrOvrdDefField"),
                    SingleMemberClassArrayDef.class);
        checkUnequals(SingleMemberEnumArrAcceptDefClass.class, UnitTest.class.getField("SingleMemberEnumArrOvrdDefField"),
                    SingleMemberEnumArrayDef.class);

        // *** TESTS FOR SERIALIZATION AND DESERIALIZATION

        // MULTIMEMBER SCALAR TYPES
        checkSerialization(scalarTypesClass.class, ScalarTypes.class);
        checkSerialization(scalarTypesOverrideDefaultClass.class, ScalarTypesWithDefault.class);
        checkSerialization(scalarTypesAcceptDefaultClass.class, ScalarTypesWithDefault.class);

        // MULTIMEMBER ARRAY TYPES
        checkSerialization(emptyArrayTypesClass.class, ArrayTypes.class);
        checkSerialization(singleElementArrayTypesClass.class, ArrayTypes.class);
        checkSerialization(twoElementArrayTypesClass.class, ArrayTypes.class);
        checkSerialization(arrayTypesOverrideDefaultClass.class, ArrayTypesWithDefault.class);
        checkSerialization(arrayTypesAcceptDefaultClass.class, ArrayTypesWithDefault.class);

        // MARKER TYPE
        checkSerialization(markerClass.class, Marker.class);

        // SINGLE-MEMBER SCALAR TYPES
        checkSerialization(SingleMemberByteClass.class, SingleMemberByte.class);
        checkSerialization(SingleMemberShortClass.class, SingleMemberShort.class);
        checkSerialization(SingleMemberIntClass.class, SingleMemberInt.class);
        checkSerialization(SingleMemberLongClass.class, SingleMemberLong.class);
        checkSerialization(SingleMemberCharClass.class, SingleMemberChar.class);
        checkSerialization(SingleMemberFloatClass.class, SingleMemberFloat.class);
        checkSerialization(SingleMemberDoubleClass.class, SingleMemberDouble.class);
        checkSerialization(SingleMemberBooleanClass.class, SingleMemberBoolean.class);
        checkSerialization(SingleMemberStringClass.class, SingleMemberString.class);
        checkSerialization(SingleMemberClassClass.class, SingleMemberClass.class);
        checkSerialization(SingleMemberEnumClass.class, SingleMemberEnum.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-OVERRIDE
        checkSerialization(SingleMemberByteOvrdDefClass.class, SingleMemberByteWithDef.class);
        checkSerialization(SingleMemberShortOvrdDefClass.class, SingleMemberShortWithDef.class);
        checkSerialization(SingleMemberIntOvrdDefClass.class, SingleMemberIntWithDef.class);
        checkSerialization(SingleMemberLongOvrdDefClass.class, SingleMemberLongWithDef.class);
        checkSerialization(SingleMemberCharOvrdDefClass.class, SingleMemberCharWithDef.class);
        checkSerialization(SingleMemberFloatOvrdDefClass.class, SingleMemberFloatWithDef.class);
        checkSerialization(SingleMemberDoubleOvrdDefClass.class, SingleMemberDoubleWithDef.class);
        checkSerialization(SingleMemberBooleanOvrdDefClass.class, SingleMemberBooleanWithDef.class);
        checkSerialization(SingleMemberStringOvrdDefClass.class, SingleMemberStringWithDef.class);
        checkSerialization(SingleMemberClassOvrdDefClass.class, SingleMemberClassWithDef.class);
        checkSerialization(SingleMemberEnumOvrdDefClass.class, SingleMemberEnumWithDef.class);

        // SINGLE-MEMBER SCALAR TYPES WITH DEFAULT-ACCEPT
        checkSerialization(SingleMemberByteAcceptDefClass.class, SingleMemberByteWithDef.class);
        checkSerialization(SingleMemberShortAcceptDefClass.class, SingleMemberShortWithDef.class);
        checkSerialization(SingleMemberIntAcceptDefClass.class, SingleMemberIntWithDef.class);
        checkSerialization(SingleMemberLongAcceptDefClass.class, SingleMemberLongWithDef.class);
        checkSerialization(SingleMemberCharAcceptDefClass.class, SingleMemberCharWithDef.class);
        checkSerialization(SingleMemberFloatAcceptDefClass.class, SingleMemberFloatWithDef.class);
        checkSerialization(SingleMemberDoubleAcceptDefClass.class, SingleMemberDoubleWithDef.class);
        checkSerialization(SingleMemberBooleanAcceptDefClass.class, SingleMemberBooleanWithDef.class);
        checkSerialization(SingleMemberStringAcceptDefClass.class, SingleMemberStringWithDef.class);
        checkSerialization(SingleMemberClassAcceptDefClass.class, SingleMemberClassWithDef.class);
        checkSerialization(SingleMemberEnumAcceptDefClass.class, SingleMemberEnumWithDef.class);

        // SINGLE-MEMBER ARRAY TYPES (EMPTY ARRAY)
        checkSerialization(SingleMemberByteArrEmptyClass.class, SingleMemberByteArray.class);
        checkSerialization(SingleMemberShortArrEmptyClass.class, SingleMemberShortArray.class);
        checkSerialization(SingleMemberIntArrEmptyClass.class, SingleMemberIntArray.class);
        checkSerialization(SingleMemberLongArrEmptyClass.class, SingleMemberLongArray.class);
        checkSerialization(SingleMemberCharArrEmptyClass.class, SingleMemberCharArray.class);
        checkSerialization(SingleMemberFloatArrEmptyClass.class, SingleMemberFloatArray.class);
        checkSerialization(SingleMemberDoubleArrEmptyClass.class, SingleMemberDoubleArray.class);
        checkSerialization(SingleMemberBooleanArrEmptyClass.class, SingleMemberBooleanArray.class);
        checkSerialization(SingleMemberStringArrEmptyClass.class, SingleMemberStringArray.class);
        checkSerialization(SingleMemberClassArrEmptyClass.class, SingleMemberClassArray.class);
        checkSerialization(SingleMemberEnumArrEmptyClass.class, SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES (ONE-ELEMENT ARRAY)
        checkSerialization(SingleMemberByteArrOneClass.class, SingleMemberByteArray.class);
        checkSerialization(SingleMemberShortArrOneClass.class, SingleMemberShortArray.class);
        checkSerialization(SingleMemberIntArrOneClass.class, SingleMemberIntArray.class);
        checkSerialization(SingleMemberLongArrOneClass.class, SingleMemberLongArray.class);
        checkSerialization(SingleMemberCharArrOneClass.class, SingleMemberCharArray.class);
        checkSerialization(SingleMemberFloatArrOneClass.class, SingleMemberFloatArray.class);
        checkSerialization(SingleMemberDoubleArrOneClass.class, SingleMemberDoubleArray.class);
        checkSerialization(SingleMemberBooleanArrOneClass.class, SingleMemberBooleanArray.class);
        checkSerialization(SingleMemberStringArrOneClass.class, SingleMemberStringArray.class);
        checkSerialization(SingleMemberClassArrOneClass.class, SingleMemberClassArray.class);
        checkSerialization(SingleMemberEnumArrOneClass.class, SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES (TWO-ELEMENT ARRAY)
        checkSerialization(SingleMemberByteArrTwoClass.class, SingleMemberByteArray.class);
        checkSerialization(SingleMemberShortArrTwoClass.class, SingleMemberShortArray.class);
        checkSerialization(SingleMemberIntArrTwoClass.class, SingleMemberIntArray.class);
        checkSerialization(SingleMemberLongArrTwoClass.class, SingleMemberLongArray.class);
        checkSerialization(SingleMemberCharArrTwoClass.class, SingleMemberCharArray.class);
        checkSerialization(SingleMemberFloatArrTwoClass.class, SingleMemberFloatArray.class);
        checkSerialization(SingleMemberDoubleArrTwoClass.class, SingleMemberDoubleArray.class);
        checkSerialization(SingleMemberBooleanArrTwoClass.class, SingleMemberBooleanArray.class);
        checkSerialization(SingleMemberStringArrTwoClass.class, SingleMemberStringArray.class);
        checkSerialization(SingleMemberClassArrTwoClass.class, SingleMemberClassArray.class);
        checkSerialization(SingleMemberEnumArrTwoClass.class, SingleMemberEnumArray.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (OVERRIDE)
        checkSerialization(SingleMemberByteArrOvrdDefClass.class, SingleMemberByteArrayDef.class);
        checkSerialization(SingleMemberShortArrOvrdDefClass.class, SingleMemberShortArrayDef.class);
        checkSerialization(SingleMemberIntArrOvrdDefClass.class, SingleMemberIntArrayDef.class);
        checkSerialization(SingleMemberLongArrOvrdDefClass.class, SingleMemberLongArrayDef.class);
        checkSerialization(SingleMemberCharArrOvrdDefClass.class, SingleMemberCharArrayDef.class);
        checkSerialization(SingleMemberFloatArrOvrdDefClass.class, SingleMemberFloatArrayDef.class);
        checkSerialization(SingleMemberDoubleArrOvrdDefClass.class, SingleMemberDoubleArrayDef.class);
        checkSerialization(SingleMemberBooleanArrOvrdDefClass.class, SingleMemberBooleanArrayDef.class);
        checkSerialization(SingleMemberStringArrOvrdDefClass.class, SingleMemberStringArrayDef.class);
        checkSerialization(SingleMemberClassArrOvrdDefClass.class, SingleMemberClassArrayDef.class);
        checkSerialization(SingleMemberEnumArrOvrdDefClass.class, SingleMemberEnumArrayDef.class);

        // SINGLE-MEMBER ARRAY TYPES WITH DEFAULT (ACCEPT)
        checkSerialization(SingleMemberByteArrAcceptDefClass.class, SingleMemberByteArrayDef.class);
        checkSerialization(SingleMemberShortArrAcceptDefClass.class, SingleMemberShortArrayDef.class);
        checkSerialization(SingleMemberIntArrAcceptDefClass.class, SingleMemberIntArrayDef.class);
        checkSerialization(SingleMemberLongArrAcceptDefClass.class, SingleMemberLongArrayDef.class);
        checkSerialization(SingleMemberCharArrAcceptDefClass.class, SingleMemberCharArrayDef.class);
        checkSerialization(SingleMemberFloatArrAcceptDefClass.class, SingleMemberFloatArrayDef.class);
        checkSerialization(SingleMemberDoubleArrAcceptDefClass.class, SingleMemberDoubleArrayDef.class);
        checkSerialization(SingleMemberBooleanArrAcceptDefClass.class, SingleMemberBooleanArrayDef.class);
        checkSerialization(SingleMemberStringArrAcceptDefClass.class, SingleMemberStringArrayDef.class);
        checkSerialization(SingleMemberClassArrAcceptDefClass.class, SingleMemberClassArrayDef.class);
        checkSerialization(SingleMemberEnumArrAcceptDefClass.class, SingleMemberEnumArrayDef.class);

        // *** TESTS FOR ANNOTATION INHERITANCE AND ENUMERATING DECLARED ANNOTATIONS

        // Inheritance tests
        checkInheritence(Grandpa.class, true, true);
        checkInheritence(Dad.class,     true, false);
        checkInheritence(Son.class,     true, true);

        // Declared annotations tests
        checkDeclaredAnnotations(Grandpa.class, true, true);
        checkDeclaredAnnotations(Dad.class,     false, false);
        checkDeclaredAnnotations(Son.class,     false, true);

        // Generate summary
        System.out.println("\n" + numTests + " tests completed");
        if (failCount != 0)
            throw new Exception("Failure count: " + failCount);
        else
            System.out.println("Success.");
    }

    static int failCount = 0;

    private static void fail(String test) {
        System.out.println("Failure: " + test);
        failCount++;
    }

    // ANNOTATION-VERIFICATION METHODS

    // Scalar multi-member

    static void checkScalarTypes(AnnotatedElement e) {
        try {
            checkScalarTypes(e.getAnnotation(ScalarTypes.class), e);
        } catch(Throwable t) {
            fail("ScalarTypes " + e + ": " + t);
            t.printStackTrace();
        }
    }

    static void checkScalarTypes(ScalarTypes st, AnnotatedElement e) throws Exception {
        numTests++;
        if (!(st.b()    == 1            &&
              st.s()    == 2            &&
              st.i()    == 3            &&
              st.l()    == 4L           &&
              st.c()    == '5'          &&
              st.f()    == 6.0f         &&
              st.d()    == 7.0          &&
              st.bool() == true         &&
              st.str().equals("custom") &&
              st.cls()  == Map.class    &&
              st.e()    == Stooge.MOE   &&
              st.a().x() == 1 && st.a().y() == 2))
            fail("ScalarTypes" + e);
    }

    static void checkScalarTypesOverrideDefault(AnnotatedElement e) {
        try {
            checkScalarTypesOverrideDefault(e.getAnnotation(ScalarTypesWithDefault.class), e);
        } catch(Throwable t) {
            fail("ScalarTypesOverrideDefaults" + e + ": " + t);
        }
    }

    static void checkScalarTypesOverrideDefault(ScalarTypesWithDefault st, AnnotatedElement e) {
        numTests++;
        if (!(st.b()    == 1            &&
              st.s()    == 2            &&
              st.i()    == 3            &&
              st.l()    == 4L           &&
              st.c()    == '5'          &&
              st.f()    == 6.0f         &&
              st.d()    == 7.0          &&
              st.bool() == true         &&
              st.str().equals("custom") &&
              st.cls()  == Map.class    &&
              st.e()    == Stooge.MOE))
            fail("ScalarTypesOverrideDefaults" + e);
    }

    static void checkScalarTypesAcceptDefault(AnnotatedElement e) {
        try {
            checkScalarTypesAcceptDefault(e.getAnnotation(ScalarTypesWithDefault.class), e);
        } catch(Throwable t) {
            fail("ScalarTypesAcceptDefaults" + e + ": " + t);
        }
    }

    static void checkScalarTypesAcceptDefault(ScalarTypesWithDefault st, AnnotatedElement e) {
        numTests++;
        if (!(st.b()    == 11            &&
              st.s()    == 12            &&
              st.i()    == 13            &&
              st.l()    == 14L           &&
              st.c()    == 'V'           &&
              st.f()    == 16.0f         &&
              st.d()    == 17.0          &&
              st.bool() == false         &&
              st.str().equals("default") &&
              st.cls()   == Class.class  &&
              st.e()    == Stooge.LARRY  &&
              st.a().x() == 11 && st.a().y() == 12))
            fail("ScalarTypesAcceptDefaults" + e);
    }

    // Array multi-member

    static void checkArrayTypes0(AnnotatedElement e) {
        try {
            checkArrayTypes0(e.getAnnotation(ArrayTypes.class), e);
        } catch(Throwable t) {
            fail("ArrayTypes(Empty)" + e + ": " + t);
        }
    }

    static void checkArrayTypes0(ArrayTypes at, AnnotatedElement e) {
        numTests++;
        if (!(at.b().length == 0 &&
              at.s().length == 0 &&
              at.i().length == 0 &&
              at.l().length == 0 &&
              at.c().length == 0 &&
              at.f().length == 0 &&
              at.d().length == 0 &&
              at.bool().length == 0 &&
              at.str().length == 0 &&
              at.cls().length == 0 &&
              at.e().length == 0 &&
              at.a().length == 0)) {
            fail("ArrayTypes(Empty)" + e);
        }
    }

    static void checkArrayTypes1(AnnotatedElement e) {
        try {
            checkArrayTypes1(e.getAnnotation(ArrayTypes.class), e);
        } catch(Throwable t) {
            fail("ArrayTypes(One element)" + e + ": " + t);
        }
    }

    static void checkArrayTypes1(ArrayTypes at, AnnotatedElement e) {
        numTests++;
        if (!(at.b()[0]    == 1            &&
              at.s()[0]    == 2            &&
              at.i()[0]    == 3            &&
              at.l()[0]    == 4L           &&
              at.c()[0]    == '5'          &&
              at.f()[0]    == 6.0f         &&
              at.d()[0]    == 7.0          &&
              at.bool()[0] == true         &&
              at.str()[0].equals("custom") &&
              at.cls()[0]  == Map.class    &&
              at.e()[0]    == Stooge.MOE   &&
              at.a()[0].x() == 1 && at.a()[0].y() == 2 &&

              at.b().length==1    && at.s().length==1   && at.i().length==1 &&
              at.l().length==1    && at.c().length==1   && at.d().length==1 &&
              at.bool().length==1 && at.str().length==1 &&
              at.cls().length==1  && at.cls().length==1 && at.a().length==1))
            fail("ArrayTypes(One element)" + e);
    }

    static void checkArrayTypes2(AnnotatedElement e) {
        try {
            checkArrayTypes2(e.getAnnotation(ArrayTypes.class), e);
        } catch(Throwable t) {
            fail("ArrayTypes(Two element)" + e + ": " + t);
        }
    }

    static void checkArrayTypes2(ArrayTypes at, AnnotatedElement e) {
        numTests++;
        if (!(at.b()[0]    == 1            && at.b()[1]    == 2            &&
              at.s()[0]    == 2            && at.s()[1]    == 3            &&
              at.i()[0]    == 3            && at.i()[1]    == 4            &&
              at.l()[0]    == 4L           && at.l()[1]    == 5L           &&
              at.c()[0]    == '5'          && at.c()[1]    == '6'          &&
              at.f()[0]    == 6.0f         && at.f()[1]    == 7.0f         &&
              at.d()[0]    == 7.0          && at.d()[1]    == 8.0          &&
              at.bool()[0] == true         && at.bool()[1] == false        &&
              at.str()[0].equals("custom") && at.str()[1].equals("paint")  &&
              at.cls()[0]  == Map.class    && at.cls()[1]  == Set.class    &&
              at.e()[0]    == Stooge.MOE   && at.e()[1]    == Stooge.CURLY &&
              at.a()[0].x() == 1 && at.a()[0].y() == 2 && at.a()[1].x() == 3 && at.a()[1].y() == 4 &&

              at.b().length==2    && at.s().length==2   && at.i().length==2 &&
              at.l().length==2    && at.c().length==2   && at.d().length==2 &&
              at.bool().length==2 && at.str().length==2 &&
              at.cls().length==2  && at.cls().length==2 && at.a().length==2))
            fail("ArrayTypes(Two element)" + e);
    }

    static void checkArrayTypesOverrideDefault(AnnotatedElement e) {
        try {
            checkArrayTypesOverrideDefault(e.getAnnotation(ArrayTypesWithDefault.class), e);
        } catch(Throwable t) {
            fail("ArrayTypesOverrideDefault" + e + ": " + t);
        }
    }

    static void checkArrayTypesOverrideDefault(ArrayTypesWithDefault at, AnnotatedElement e) {
        numTests++;
        if (!(at.b()[0]    == 1            &&
              at.s()[0]    == 2            &&
              at.i()[0]    == 3            &&
              at.l()[0]    == 4L           &&
              at.c()[0]    == '5'          &&
              at.f()[0]    == 6.0f         &&
              at.d()[0]    == 7.0          &&
              at.bool()[0] == true         &&
              at.str()[0].equals("custom") &&
              at.cls()[0]  == Map.class    &&
              at.e()[0]    == Stooge.MOE   &&
              at.a()[0].x() == 1 && at.a()[0].y() == 2 &&

              at.b().length==1    && at.s().length==1   && at.i().length==1 &&
              at.l().length==1    && at.c().length==1   && at.d().length==1 &&
              at.bool().length==1 && at.str().length==1 &&
              at.cls().length==1  && at.cls().length==1))
            fail("ArrayTypesOverrideDefault" + e);
    }

    static void checkArrayTypesAcceptDefault(AnnotatedElement e) {
        try {
            checkArrayTypesAcceptDefault(e.getAnnotation(ArrayTypesWithDefault.class), e);
        } catch(Throwable t) {
            fail("ArrayTypesAcceptDefault" + e + ": " + t);
        }
    }

    static void checkArrayTypesAcceptDefault(ArrayTypesWithDefault at, AnnotatedElement e) {
        numTests++;
        if (!(at.b()[0]    == 11            &&
              at.s()[0]    == 12            &&
              at.i()[0]    == 13            &&
              at.l()[0]    == 14L           &&
              at.c()[0]    == 'V'           &&
              at.f()[0]    == 16.0f         &&
              at.d()[0]    == 17.0          &&
              at.bool()[0] == false         &&
              at.str()[0].equals("default") &&
              at.cls()[0]  == Class.class   &&
              at.e()[0]    == Stooge.LARRY  &&
              at.a()[0].x() == 11 && at.a()[0].y() == 12 &&

              at.b().length==1    && at.s().length==1   && at.i().length==1 &&
              at.l().length==1    && at.c().length==1   && at.d().length==1 &&
              at.bool().length==1 && at.str().length==1 &&
              at.cls().length==1  && at.cls().length==1))
            fail("ArrayTypesAcceptDefault" + e);
    }

    // Scalar multi-member for parameters

    static void checkScalarTypesParam(Method m) {
        try {
            checkScalarTypes((ScalarTypes) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ScalarTypes" + m + ": " + t);
        }
    }

    static void checkScalarTypesOverrideDefaultParam(Method m) {
        try {
            checkScalarTypesOverrideDefault((ScalarTypesWithDefault) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ScalarTypesOverrideDefaults" + m + ": " + t);
        }
    }

    static void checkScalarTypesAcceptDefaultParam(Method m) {
        try {
            checkScalarTypesAcceptDefault((ScalarTypesWithDefault) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ScalarTypesAcceptDefaults" + m + ": " + t);
        }
    }

    // Array multi-member for parameters

    static void checkArrayTypes0Param(Method m) {
        try {
            checkArrayTypes0((ArrayTypes) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ArrayTypes(Empty)" + m + ": " + t);
        }
    }

    static void checkArrayTypes1Param(Method m) {
        try {
            checkArrayTypes1((ArrayTypes) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ArrayTypes(One Element)" + m + ": " + t);
        }
    }

    static void checkArrayTypes2Param(Method m) {
        try {
            checkArrayTypes2((ArrayTypes) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ArrayTypes(Two Elements)" + m + ": " + t);
        }
    }

    static void checkArrayTypesOverrideDefaultParam(Method m) {
        try {
            checkArrayTypesOverrideDefault((ArrayTypesWithDefault) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ArrayTypesOverrideDefault" + m + ": " + t);
        }
    }

    static void checkArrayTypesAcceptDefaultParam(Method m) {
        try {
            checkArrayTypesAcceptDefault((ArrayTypesWithDefault) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("ArrayTypesAcceptDefault" + m + ": " + t);
        }
    }

    // marker type on parameter
    static void checkMarkerParam(Method m) {
        try {
            checkMarker((Marker) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("Marker" + m + ": " + t);
        }
    }

    // single-member scalar types on parameter
    static void checkSingleMemberByteParam(Method m) {
        try {
            checkSingleMemberByte((SingleMemberByte) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByte" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortParam(Method m) {
        try {
            checkSingleMemberShort((SingleMemberShort) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShort" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntParam(Method m) {
        try {
            checkSingleMemberInt((SingleMemberInt) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberInt" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongParam(Method m) {
        try {
            checkSingleMemberLong((SingleMemberLong) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLong" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharParam(Method m) {
        try {
            checkSingleMemberChar((SingleMemberChar) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberChar" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatParam(Method m) {
        try {
            checkSingleMemberFloat((SingleMemberFloat) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloat" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleParam(Method m) {
        try {
            checkSingleMemberDouble((SingleMemberDouble) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDouble" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanParam(Method m) {
        try {
            checkSingleMemberBoolean((SingleMemberBoolean) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBoolean" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringParam(Method m) {
        try {
            checkSingleMemberString((SingleMemberString) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberString" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassParam(Method m) {
        try {
            checkSingleMemberClass((SingleMemberClass) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClass" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumParam(Method m) {
        try {
            checkSingleMemberEnum((SingleMemberEnum) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnum" + m + ": " + t);
        }
    }

    // single-member scalar types with default-override on parameter
    static void checkSingleMemberByteOvrdDefParam(Method m) {
        try {
            checkSingleMemberByteOvrdDef((SingleMemberByteWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByteOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortOvrdDefParam(Method m) {
        try {
            checkSingleMemberShortOvrdDef((SingleMemberShortWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShortOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntOvrdDefParam(Method m) {
        try {
            checkSingleMemberIntOvrdDef((SingleMemberIntWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberIntOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongOvrdDefParam(Method m) {
        try {
            checkSingleMemberLongOvrdDef((SingleMemberLongWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLongOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharOvrdDefParam(Method m) {
        try {
            checkSingleMemberCharOvrdDef((SingleMemberCharWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberCharOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatOvrdDefParam(Method m) {
        try {
            checkSingleMemberFloatOvrdDef((SingleMemberFloatWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloatOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleOvrdDefParam(Method m) {
        try {
            checkSingleMemberDoubleOvrdDef((SingleMemberDoubleWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDoubleOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanOvrdDefParam(Method m) {
        try {
            checkSingleMemberBooleanOvrdDef((SingleMemberBooleanWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBooleanOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringOvrdDefParam(Method m) {
        try {
            checkSingleMemberStringOvrdDef((SingleMemberStringWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberStringOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassOvrdDefParam(Method m) {
        try {
            checkSingleMemberClassOvrdDef((SingleMemberClassWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClassOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumOvrdDefParam(Method m) {
        try {
            checkSingleMemberEnumOvrdDef((SingleMemberEnumWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnumOvrdDef" + m + ": " + t);
        }
    }

    // single-member scalar types with default-accept on PARAMETER
    static void checkSingleMemberByteAcceptDefParam(Method m) {
        try {
            checkSingleMemberByteAcceptDef((SingleMemberByteWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByteAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortAcceptDefParam(Method m) {
        try {
            checkSingleMemberShortAcceptDef((SingleMemberShortWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShortAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntAcceptDefParam(Method m) {
        try {
            checkSingleMemberIntAcceptDef((SingleMemberIntWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberIntAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongAcceptDefParam(Method m) {
        try {
            checkSingleMemberLongAcceptDef((SingleMemberLongWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLongAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharAcceptDefParam(Method m) {
        try {
            checkSingleMemberCharAcceptDef((SingleMemberCharWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberCharAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatAcceptDefParam(Method m) {
        try {
            checkSingleMemberFloatAcceptDef((SingleMemberFloatWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloatAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleAcceptDefParam(Method m) {
        try {
            checkSingleMemberDoubleAcceptDef((SingleMemberDoubleWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDoubleAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanAcceptDefParam(Method m) {
        try {
            checkSingleMemberBooleanAcceptDef((SingleMemberBooleanWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBooleanAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringAcceptDefParam(Method m) {
        try {
            checkSingleMemberStringAcceptDef((SingleMemberStringWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberStringAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassAcceptDefParam(Method m) {
        try {
            checkSingleMemberClassAcceptDef((SingleMemberClassWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClassAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumAcceptDefParam(Method m) {
        try {
            checkSingleMemberEnumAcceptDef((SingleMemberEnumWithDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnumAcceptDef" + m + ": " + t);
        }
    }

    // single-member array types (empty array) parameter
    static void checkSingleMemberByteArrEmptyParam(Method m) {
        try {
            checkSingleMemberByteArrEmpty((SingleMemberByteArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByteArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortArrEmptyParam(Method m) {
        try {
            checkSingleMemberShortArrEmpty((SingleMemberShortArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShortArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntArrEmptyParam(Method m) {
        try {
            checkSingleMemberIntArrEmpty((SingleMemberIntArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberIntArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongArrEmptyParam(Method m) {
        try {
            checkSingleMemberLongArrEmpty((SingleMemberLongArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLongArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharArrEmptyParam(Method m) {
        try {
            checkSingleMemberCharArrEmpty((SingleMemberCharArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberCharArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrEmptyParam(Method m) {
        try {
            checkSingleMemberFloatArrEmpty((SingleMemberFloatArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloatArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrEmptyParam(Method m) {
        try {
            checkSingleMemberDoubleArrEmpty((SingleMemberDoubleArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrEmptyParam(Method m) {
        try {
            checkSingleMemberBooleanArrEmpty((SingleMemberBooleanArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringArrEmptyParam(Method m) {
        try {
            checkSingleMemberStringArrEmpty((SingleMemberStringArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberStringArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassArrEmptyParam(Method m) {
        try {
            checkSingleMemberClassArrEmpty((SingleMemberClassArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClassArrEmpty" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrEmptyParam(Method m) {
        try {
            checkSingleMemberEnumArrEmpty((SingleMemberEnumArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnumArrEmpty" + m + ": " + t);
        }
    }

    // single-member array types (one-element array) on parameter
    static void checkSingleMemberByteArrOneParam(Method m) {
        try {
            checkSingleMemberByteArrOne((SingleMemberByteArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByteArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortArrOneParam(Method m) {
        try {
            checkSingleMemberShortArrOne((SingleMemberShortArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShortArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntArrOneParam(Method m) {
        try {
            checkSingleMemberIntArrOne((SingleMemberIntArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberIntArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongArrOneParam(Method m) {
        try {
            checkSingleMemberLongArrOne((SingleMemberLongArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLongArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharArrOneParam(Method m) {
        try {
            checkSingleMemberCharArrOne((SingleMemberCharArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberCharArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrOneParam(Method m) {
        try {
            checkSingleMemberFloatArrOne((SingleMemberFloatArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloatArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrOneParam(Method m) {
        try {
            checkSingleMemberDoubleArrOne((SingleMemberDoubleArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrOneParam(Method m) {
        try {
            checkSingleMemberBooleanArrOne((SingleMemberBooleanArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringArrOneParam(Method m) {
        try {
            checkSingleMemberStringArrOne((SingleMemberStringArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberStringArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassArrOneParam(Method m) {
        try {
            checkSingleMemberClassArrOne((SingleMemberClassArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClassArrOne" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrOneParam(Method m) {
        try {
            checkSingleMemberEnumArrOne((SingleMemberEnumArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnumArrOne" + m + ": " + t);
        }
    }

    // single-member array types (two-element array) on parameter
    static void checkSingleMemberByteArrTwoParam(Method m) {
        try {
            checkSingleMemberByteArrTwo((SingleMemberByteArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByteArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortArrTwoParam(Method m) {
        try {
            checkSingleMemberShortArrTwo((SingleMemberShortArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShortArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntArrTwoParam(Method m) {
        try {
            checkSingleMemberIntArrTwo((SingleMemberIntArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberIntArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongArrTwoParam(Method m) {
        try {
            checkSingleMemberLongArrTwo((SingleMemberLongArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLongArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharArrTwoParam(Method m) {
        try {
            checkSingleMemberCharArrTwo((SingleMemberCharArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberCharArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrTwoParam(Method m) {
        try {
            checkSingleMemberFloatArrTwo((SingleMemberFloatArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloatArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrTwoParam(Method m) {
        try {
            checkSingleMemberDoubleArrTwo((SingleMemberDoubleArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrTwoParam(Method m) {
        try {
            checkSingleMemberBooleanArrTwo((SingleMemberBooleanArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringArrTwoParam(Method m) {
        try {
            checkSingleMemberStringArrTwo((SingleMemberStringArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberStringArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassArrTwoParam(Method m) {
        try {
            checkSingleMemberClassArrTwo((SingleMemberClassArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClassArrTwo" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrTwoParam(Method m) {
        try {
            checkSingleMemberEnumArrTwo((SingleMemberEnumArray) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnumArrTwo" + m + ": " + t);
        }
    }

    // single-member array types with default (override)on parameter
    static void checkSingleMemberByteArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberByteArrOvrdDef((SingleMemberByteArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByteArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberShortArrOvrdDef((SingleMemberShortArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShortArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberIntArrOvrdDef((SingleMemberIntArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberIntArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberLongArrOvrdDef((SingleMemberLongArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLongArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberCharArrOvrdDef((SingleMemberCharArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberCharArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberFloatArrOvrdDef((SingleMemberFloatArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloatArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberDoubleArrOvrdDef((SingleMemberDoubleArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberBooleanArrOvrdDef((SingleMemberBooleanArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberStringArrOvrdDef((SingleMemberStringArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberStringArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberClassArrOvrdDef((SingleMemberClassArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClassArrOvrdDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrOvrdDefParam(Method m) {
        try {
            checkSingleMemberEnumArrOvrdDef((SingleMemberEnumArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnumArrOvrdDef" + m + ": " + t);
        }
    }

    // single-member array types with default (accept)on parameter
    static void checkSingleMemberByteArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberByteArrAcceptDef((SingleMemberByteArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberByteArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberShortArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberShortArrAcceptDef((SingleMemberShortArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberShortArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberIntArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberIntArrAcceptDef((SingleMemberIntArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberIntArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberLongArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberLongArrAcceptDef((SingleMemberLongArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberLongArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberCharArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberCharArrAcceptDef((SingleMemberCharArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberCharArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberFloatArrAcceptDef((SingleMemberFloatArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberFloatArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberDoubleArrAcceptDef((SingleMemberDoubleArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberBooleanArrAcceptDef((SingleMemberBooleanArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberStringArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberStringArrAcceptDef((SingleMemberStringArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberStringArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberClassArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberClassArrAcceptDef((SingleMemberClassArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberClassArrAcceptDef" + m + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrAcceptDefParam(Method m) {
        try {
            checkSingleMemberEnumArrAcceptDef((SingleMemberEnumArrayDef) m.getParameterAnnotations()[0][0], m);
        } catch(Throwable t) {
            fail("SingleMemberEnumArrAcceptDef" + m + ": " + t);
        }
    }

    // Marker
    static void checkMarker(AnnotatedElement e) {
        checkMarker(e.getAnnotation(Marker.class), e);
    }
    static void checkMarker(Marker m, AnnotatedElement e) {
        numTests++;
        try {
            if (m == null) fail("Marker " + e);
        } catch(Throwable t) {
            fail("Marker " + e + ": " + t);
        }
    }

    // Single-member

    static void checkSingleMemberByte(AnnotatedElement e) {
        checkSingleMemberByte(e.getAnnotation(SingleMemberByte.class), e);
    }
    static void checkSingleMemberByte(SingleMemberByte a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 1) fail("SingleMemberByte " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByte " + e + ": " + t);
        }
    }

    static void checkSingleMemberShort(AnnotatedElement e) {
        checkSingleMemberShort(e.getAnnotation(SingleMemberShort.class), e);
    }
    static void checkSingleMemberShort(SingleMemberShort a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 2) fail("SingleMemberShort " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShort " + e + ": " + t);
        }
    }

    static void checkSingleMemberInt(AnnotatedElement e) {
        checkSingleMemberInt(e.getAnnotation(SingleMemberInt.class), e);
    }
    static void checkSingleMemberInt(SingleMemberInt a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 3) fail("SingleMemberInt " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberInt " + e + ": " + t);
        }
    }

    static void checkSingleMemberLong(AnnotatedElement e) {
        checkSingleMemberLong(e.getAnnotation(SingleMemberLong.class), e);
    }
    static void checkSingleMemberLong(SingleMemberLong a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 4L) fail("SingleMemberLong " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLong " + e + ": " + t);
        }
    }

    static void checkSingleMemberChar(AnnotatedElement e) {
        checkSingleMemberChar(e.getAnnotation(SingleMemberChar.class), e);
    }
    static void checkSingleMemberChar(SingleMemberChar a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != '5') fail("SingleMemberChar " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberChar " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloat(AnnotatedElement e) {
        checkSingleMemberFloat(e.getAnnotation(SingleMemberFloat.class), e);
    }
    static void checkSingleMemberFloat(SingleMemberFloat a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 6.0f) fail("SingleMemberFloat " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloat " + e + ": " + t);
        }
    }

    static void checkSingleMemberDouble(AnnotatedElement e) {
        checkSingleMemberDouble(e.getAnnotation(SingleMemberDouble.class), e);
    }
    static void checkSingleMemberDouble(SingleMemberDouble a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 7.0) fail("SingleMemberDouble " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDouble " + e + ": " + t);
        }
    }

    static void checkSingleMemberBoolean(AnnotatedElement e) {
        checkSingleMemberBoolean(e.getAnnotation(SingleMemberBoolean.class), e);
    }
    static void checkSingleMemberBoolean(SingleMemberBoolean a, AnnotatedElement e) {
        numTests++;
        try {
            if (!a.value()) fail("SingleMemberBoolean " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBoolean " + e + ": " + t);
        }
    }

    static void checkSingleMemberString(AnnotatedElement e) {
        checkSingleMemberString(e.getAnnotation(SingleMemberString.class), e);
    }
    static void checkSingleMemberString(SingleMemberString a, AnnotatedElement e) {
        numTests++;
        try {
            if (!(a.value().equals("custom"))) fail("SingleMemberString " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberString " + e + ": " + t);
        }
    }

    static void checkSingleMemberClass(AnnotatedElement e) {
        checkSingleMemberClass(e.getAnnotation(SingleMemberClass.class), e);
    }
    static void checkSingleMemberClass(SingleMemberClass a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != Map.class) fail("SingleMemberClass " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClass " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnum(AnnotatedElement e) {
        checkSingleMemberEnum(e.getAnnotation(SingleMemberEnum.class), e);
    }
    static void checkSingleMemberEnum(SingleMemberEnum a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != Stooge.MOE) fail("SingleMemberEnum " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnum " + e + ": " + t);
        }
    }

    // Single-member with default (Override)

    static void checkSingleMemberByteOvrdDef(AnnotatedElement e) {
        checkSingleMemberByteOvrdDef(e.getAnnotation(SingleMemberByteWithDef.class), e);
    }
    static void checkSingleMemberByteOvrdDef(SingleMemberByteWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 1) fail("SingleMemberByteOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByteOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberShortOvrdDef(AnnotatedElement e) {
        checkSingleMemberShortOvrdDef(e.getAnnotation(SingleMemberShortWithDef.class), e);
    }
    static void checkSingleMemberShortOvrdDef(SingleMemberShortWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 2) fail("SingleMemberShortOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShortOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberIntOvrdDef(AnnotatedElement e) {
        checkSingleMemberIntOvrdDef(e.getAnnotation(SingleMemberIntWithDef.class), e);
    }
    static void checkSingleMemberIntOvrdDef(SingleMemberIntWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 3) fail("SingleMemberIntOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberIntOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberLongOvrdDef(AnnotatedElement e) {
        checkSingleMemberLongOvrdDef(e.getAnnotation(SingleMemberLongWithDef.class), e);
    }
    static void checkSingleMemberLongOvrdDef(SingleMemberLongWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 4L) fail("SingleMemberLongOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLongOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberCharOvrdDef(AnnotatedElement e) {
        checkSingleMemberCharOvrdDef(e.getAnnotation(SingleMemberCharWithDef.class), e);
    }
    static void checkSingleMemberCharOvrdDef(SingleMemberCharWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != '5') fail("SingleMemberCharOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberCharOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloatOvrdDef(AnnotatedElement e) {
        checkSingleMemberFloatOvrdDef(e.getAnnotation(SingleMemberFloatWithDef.class), e);
    }
    static void checkSingleMemberFloatOvrdDef(SingleMemberFloatWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 6.0f) fail("SingleMemberFloatOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloatOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberDoubleOvrdDef(AnnotatedElement e) {
        checkSingleMemberDoubleOvrdDef(e.getAnnotation(SingleMemberDoubleWithDef.class), e);
    }
    static void checkSingleMemberDoubleOvrdDef(SingleMemberDoubleWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 7.0) fail("SingleMemberDoubleOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDoubleOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberBooleanOvrdDef(AnnotatedElement e) {
        checkSingleMemberBooleanOvrdDef(e.getAnnotation(SingleMemberBooleanWithDef.class), e);
    }
    static void checkSingleMemberBooleanOvrdDef(SingleMemberBooleanWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (!a.value()) fail("SingleMemberBooleanOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBooleanOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberStringOvrdDef(AnnotatedElement e) {
        checkSingleMemberStringOvrdDef(e.getAnnotation(SingleMemberStringWithDef.class), e);
    }
    static void checkSingleMemberStringOvrdDef(SingleMemberStringWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (!(a.value().equals("custom"))) fail("SingleMemberStringOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberStringOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberClassOvrdDef(AnnotatedElement e) {
        checkSingleMemberClassOvrdDef(e.getAnnotation(SingleMemberClassWithDef.class), e);
    }
    static void checkSingleMemberClassOvrdDef(SingleMemberClassWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != Map.class) fail("SingleMemberClassOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClassOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnumOvrdDef(AnnotatedElement e) {
        checkSingleMemberEnumOvrdDef(e.getAnnotation(SingleMemberEnumWithDef.class), e);
    }
    static void checkSingleMemberEnumOvrdDef(SingleMemberEnumWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != Stooge.MOE) fail("SingleMemberEnumOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnumOvrdDef " + e + ": " + t);
        }
    }

    // Single-member with default (Accept)

    static void checkSingleMemberByteAcceptDef(AnnotatedElement e) {
        checkSingleMemberByteAcceptDef(e.getAnnotation(SingleMemberByteWithDef.class), e);
    }
    static void checkSingleMemberByteAcceptDef(SingleMemberByteWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 11) fail("SingleMemberByteAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByteAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberShortAcceptDef(AnnotatedElement e) {
        checkSingleMemberShortAcceptDef(e.getAnnotation(SingleMemberShortWithDef.class), e);
    }
    static void checkSingleMemberShortAcceptDef(SingleMemberShortWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 12) fail("SingleMemberShortAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShortAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberIntAcceptDef(AnnotatedElement e) {
        checkSingleMemberIntAcceptDef(e.getAnnotation(SingleMemberIntWithDef.class), e);
    }
    static void checkSingleMemberIntAcceptDef(SingleMemberIntWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 13) fail("SingleMemberIntAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberIntAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberLongAcceptDef(AnnotatedElement e) {
        checkSingleMemberLongAcceptDef(e.getAnnotation(SingleMemberLongWithDef.class), e);
    }
    static void checkSingleMemberLongAcceptDef(SingleMemberLongWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 14L) fail("SingleMemberLongAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLongAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberCharAcceptDef(AnnotatedElement e) {
        checkSingleMemberCharAcceptDef(e.getAnnotation(SingleMemberCharWithDef.class), e);
    }
    static void checkSingleMemberCharAcceptDef(SingleMemberCharWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 'V') fail("SingleMemberCharAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberCharAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloatAcceptDef(AnnotatedElement e) {
        checkSingleMemberFloatAcceptDef(e.getAnnotation(SingleMemberFloatWithDef.class), e);
    }
    static void checkSingleMemberFloatAcceptDef(SingleMemberFloatWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 16.0f) fail("SingleMemberFloatAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloatAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberDoubleAcceptDef(AnnotatedElement e) {
        checkSingleMemberDoubleAcceptDef(e.getAnnotation(SingleMemberDoubleWithDef.class), e);
    }
    static void checkSingleMemberDoubleAcceptDef(SingleMemberDoubleWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != 17.0) fail("SingleMemberDoubleAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDoubleAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberBooleanAcceptDef(AnnotatedElement e) {
        checkSingleMemberBooleanAcceptDef(e.getAnnotation(SingleMemberBooleanWithDef.class), e);
    }
    static void checkSingleMemberBooleanAcceptDef(SingleMemberBooleanWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value()) fail("SingleMemberBooleanAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBooleanAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberStringAcceptDef(AnnotatedElement e) {
        checkSingleMemberStringAcceptDef(e.getAnnotation(SingleMemberStringWithDef.class), e);
    }
    static void checkSingleMemberStringAcceptDef(SingleMemberStringWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (!(a.value().equals("default"))) fail("SingleMemberStringAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberStringAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberClassAcceptDef(AnnotatedElement e) {
        checkSingleMemberClassAcceptDef(e.getAnnotation(SingleMemberClassWithDef.class), e);
    }
    static void checkSingleMemberClassAcceptDef(SingleMemberClassWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != Class.class) fail("SingleMemberClassAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClassAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnumAcceptDef(AnnotatedElement e) {
        checkSingleMemberEnumAcceptDef(e.getAnnotation(SingleMemberEnumWithDef.class), e);
    }
    static void checkSingleMemberEnumAcceptDef(SingleMemberEnumWithDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value() != Stooge.LARRY) fail("SingleMemberEnumAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnumAcceptDef " + e + ": " + t);
        }
    }

    // Single member array (empty array)
    static void checkSingleMemberByteArrEmpty(AnnotatedElement e) {
        checkSingleMemberByteArrEmpty(e.getAnnotation(SingleMemberByteArray.class), e);
    }
    static void checkSingleMemberByteArrEmpty(SingleMemberByteArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberByteArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByteArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberShortArrEmpty(AnnotatedElement e) {
        checkSingleMemberShortArrEmpty(e.getAnnotation(SingleMemberShortArray.class), e);
    }
    static void checkSingleMemberShortArrEmpty(SingleMemberShortArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberShortArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShortArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberIntArrEmpty(AnnotatedElement e) {
        checkSingleMemberIntArrEmpty(e.getAnnotation(SingleMemberIntArray.class), e);
    }
    static void checkSingleMemberIntArrEmpty(SingleMemberIntArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberIntArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberIntArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberLongArrEmpty(AnnotatedElement e) {
        checkSingleMemberLongArrEmpty(e.getAnnotation(SingleMemberLongArray.class), e);
    }
    static void checkSingleMemberLongArrEmpty(SingleMemberLongArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberLongArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLongArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberCharArrEmpty(AnnotatedElement e) {
        checkSingleMemberCharArrEmpty(e.getAnnotation(SingleMemberCharArray.class), e);
    }
    static void checkSingleMemberCharArrEmpty(SingleMemberCharArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberCharArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberCharArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrEmpty(AnnotatedElement e) {
        checkSingleMemberFloatArrEmpty(e.getAnnotation(SingleMemberFloatArray.class), e);
    }
    static void checkSingleMemberFloatArrEmpty(SingleMemberFloatArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberFloatArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloatArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrEmpty(AnnotatedElement e) {
        checkSingleMemberDoubleArrEmpty(e.getAnnotation(SingleMemberDoubleArray.class), e);
    }
    static void checkSingleMemberDoubleArrEmpty(SingleMemberDoubleArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberDoubleArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrEmpty(AnnotatedElement e) {
        checkSingleMemberBooleanArrEmpty(e.getAnnotation(SingleMemberBooleanArray.class), e);
    }
    static void checkSingleMemberBooleanArrEmpty(SingleMemberBooleanArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberBooleanArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberStringArrEmpty(AnnotatedElement e) {
        checkSingleMemberStringArrEmpty(e.getAnnotation(SingleMemberStringArray.class), e);
    }
    static void checkSingleMemberStringArrEmpty(SingleMemberStringArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberStringArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberStringArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberClassArrEmpty(AnnotatedElement e) {
        checkSingleMemberClassArrEmpty(e.getAnnotation(SingleMemberClassArray.class), e);
    }
    static void checkSingleMemberClassArrEmpty(SingleMemberClassArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberClassArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClassArrEmpty " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrEmpty(AnnotatedElement e) {
        checkSingleMemberEnumArrEmpty(e.getAnnotation(SingleMemberEnumArray.class), e);
    }
    static void checkSingleMemberEnumArrEmpty(SingleMemberEnumArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 0) fail("SingleMemberEnumArrEmpty " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnumArrEmpty " + e + ": " + t);
        }
    }

    // Single member array (one element array)
    static void checkSingleMemberByteArrOne(AnnotatedElement e) {
        checkSingleMemberByteArrOne(e.getAnnotation(SingleMemberByteArray.class), e);
    }
    static void checkSingleMemberByteArrOne(SingleMemberByteArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != (byte)1)
                fail("SingleMemberByteArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByteArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberShortArrOne(AnnotatedElement e) {
        checkSingleMemberShortArrOne(e.getAnnotation(SingleMemberShortArray.class), e);
    }
    static void checkSingleMemberShortArrOne(SingleMemberShortArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != (short)2)
                fail("SingleMemberShortArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShortArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberIntArrOne(AnnotatedElement e) {
        checkSingleMemberIntArrOne(e.getAnnotation(SingleMemberIntArray.class), e);
    }
    static void checkSingleMemberIntArrOne(SingleMemberIntArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 3)
                fail("SingleMemberIntArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberIntArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberLongArrOne(AnnotatedElement e) {
        checkSingleMemberLongArrOne(e.getAnnotation(SingleMemberLongArray.class), e);
    }
    static void checkSingleMemberLongArrOne(SingleMemberLongArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 4L)
                fail("SingleMemberLongArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLongArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberCharArrOne(AnnotatedElement e) {
        checkSingleMemberCharArrOne(e.getAnnotation(SingleMemberCharArray.class), e);
    }
    static void checkSingleMemberCharArrOne(SingleMemberCharArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != '5')
                fail("SingleMemberCharArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberCharArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrOne(AnnotatedElement e) {
        checkSingleMemberFloatArrOne(e.getAnnotation(SingleMemberFloatArray.class), e);
    }
    static void checkSingleMemberFloatArrOne(SingleMemberFloatArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 6.0f)
                fail("SingleMemberFloatArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloatArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrOne(AnnotatedElement e) {
        checkSingleMemberDoubleArrOne(e.getAnnotation(SingleMemberDoubleArray.class), e);
    }
    static void checkSingleMemberDoubleArrOne(SingleMemberDoubleArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 7.0)
                fail("SingleMemberDoubleArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrOne(AnnotatedElement e) {
        checkSingleMemberBooleanArrOne(e.getAnnotation(SingleMemberBooleanArray.class), e);
    }
    static void checkSingleMemberBooleanArrOne(SingleMemberBooleanArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || !a.value()[0])
                fail("SingleMemberBooleanArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberStringArrOne(AnnotatedElement e) {
        checkSingleMemberStringArrOne(e.getAnnotation(SingleMemberStringArray.class), e);
    }
    static void checkSingleMemberStringArrOne(SingleMemberStringArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || !(a.value()[0].equals("custom")))
                fail("SingleMemberStringArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberStringArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberClassArrOne(AnnotatedElement e) {
        checkSingleMemberClassArrOne(e.getAnnotation(SingleMemberClassArray.class), e);
    }
    static void checkSingleMemberClassArrOne(SingleMemberClassArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != Map.class)
                fail("SingleMemberClassArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClassArrOne " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrOne(AnnotatedElement e) {
        checkSingleMemberEnumArrOne(e.getAnnotation(SingleMemberEnumArray.class), e);
    }
    static void checkSingleMemberEnumArrOne(SingleMemberEnumArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != Stooge.MOE)
                fail("SingleMemberEnumArrOne " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnumArrOne " + e + ": " + t);
        }
    }

    // Single member array (two element array)
    static void checkSingleMemberByteArrTwo(AnnotatedElement e) {
        checkSingleMemberByteArrTwo(e.getAnnotation(SingleMemberByteArray.class), e);
    }
    static void checkSingleMemberByteArrTwo(SingleMemberByteArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != (byte)1 || a.value()[1] != (byte)2)
                fail("SingleMemberByteArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByteArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberShortArrTwo(AnnotatedElement e) {
        checkSingleMemberShortArrTwo(e.getAnnotation(SingleMemberShortArray.class), e);
    }
    static void checkSingleMemberShortArrTwo(SingleMemberShortArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != (short)2 || a.value()[1] != (short)3)
                fail("SingleMemberShortArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShortArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberIntArrTwo(AnnotatedElement e) {
        checkSingleMemberIntArrTwo(e.getAnnotation(SingleMemberIntArray.class), e);
    }
    static void checkSingleMemberIntArrTwo(SingleMemberIntArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != 3 || a.value()[1] != 4)
                fail("SingleMemberIntArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberIntArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberLongArrTwo(AnnotatedElement e) {
        checkSingleMemberLongArrTwo(e.getAnnotation(SingleMemberLongArray.class), e);
    }
    static void checkSingleMemberLongArrTwo(SingleMemberLongArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != 4L || a.value()[1] != 5L)
                fail("SingleMemberLongArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLongArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberCharArrTwo(AnnotatedElement e) {
        checkSingleMemberCharArrTwo(e.getAnnotation(SingleMemberCharArray.class), e);
    }
    static void checkSingleMemberCharArrTwo(SingleMemberCharArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != '5' || a.value()[1] != '6')
                fail("SingleMemberCharArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberCharArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrTwo(AnnotatedElement e) {
        checkSingleMemberFloatArrTwo(e.getAnnotation(SingleMemberFloatArray.class), e);
    }
    static void checkSingleMemberFloatArrTwo(SingleMemberFloatArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != 6.0f || a.value()[1] != 7.0f)
                fail("SingleMemberFloatArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloatArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrTwo(AnnotatedElement e) {
        checkSingleMemberDoubleArrTwo(e.getAnnotation(SingleMemberDoubleArray.class), e);
    }
    static void checkSingleMemberDoubleArrTwo(SingleMemberDoubleArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != 7.0 || a.value()[1] != 8.0)
                fail("SingleMemberDoubleArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrTwo(AnnotatedElement e) {
        checkSingleMemberBooleanArrTwo(e.getAnnotation(SingleMemberBooleanArray.class), e);
    }
    static void checkSingleMemberBooleanArrTwo(SingleMemberBooleanArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || !a.value()[0] || a.value()[1])
                fail("SingleMemberBooleanArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberStringArrTwo(AnnotatedElement e) {
        checkSingleMemberStringArrTwo(e.getAnnotation(SingleMemberStringArray.class), e);
    }
    static void checkSingleMemberStringArrTwo(SingleMemberStringArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || !(a.value()[0].equals("custom")) || !(a.value()[1].equals("paint")))
                fail("SingleMemberStringArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberStringArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberClassArrTwo(AnnotatedElement e) {
        checkSingleMemberClassArrTwo(e.getAnnotation(SingleMemberClassArray.class), e);
    }
    static void checkSingleMemberClassArrTwo(SingleMemberClassArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != Map.class || a.value()[1] != Set.class)
                fail("SingleMemberClassArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClassArrTwo " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrTwo(AnnotatedElement e) {
        checkSingleMemberEnumArrTwo(e.getAnnotation(SingleMemberEnumArray.class), e);
    }
    static void checkSingleMemberEnumArrTwo(SingleMemberEnumArray a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 2 || a.value()[0] != Stooge.MOE || a.value()[1] != Stooge.CURLY)
                fail("SingleMemberEnumArrTwo " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnumArrTwo " + e + ": " + t);
        }
    }

    // Single member array with default (override)
    static void checkSingleMemberByteArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberByteArrOvrdDef(e.getAnnotation(SingleMemberByteArrayDef.class), e);
    }
    static void checkSingleMemberByteArrOvrdDef(SingleMemberByteArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != (byte)1)
                fail("SingleMemberByteArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByteArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberShortArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberShortArrOvrdDef(e.getAnnotation(SingleMemberShortArrayDef.class), e);
    }
    static void checkSingleMemberShortArrOvrdDef(SingleMemberShortArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != (short)2)
                fail("SingleMemberShortArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShortArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberIntArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberIntArrOvrdDef(e.getAnnotation(SingleMemberIntArrayDef.class), e);
    }
    static void checkSingleMemberIntArrOvrdDef(SingleMemberIntArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 3)
                fail("SingleMemberIntArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberIntArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberLongArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberLongArrOvrdDef(e.getAnnotation(SingleMemberLongArrayDef.class), e);
    }
    static void checkSingleMemberLongArrOvrdDef(SingleMemberLongArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 4L)
                fail("SingleMemberLongArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLongArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberCharArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberCharArrOvrdDef(e.getAnnotation(SingleMemberCharArrayDef.class), e);
    }
    static void checkSingleMemberCharArrOvrdDef(SingleMemberCharArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != '5')
                fail("SingleMemberCharArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberCharArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberFloatArrOvrdDef(e.getAnnotation(SingleMemberFloatArrayDef.class), e);
    }
    static void checkSingleMemberFloatArrOvrdDef(SingleMemberFloatArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 6.0f)
                fail("SingleMemberFloatArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloatArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberDoubleArrOvrdDef(e.getAnnotation(SingleMemberDoubleArrayDef.class), e);
    }
    static void checkSingleMemberDoubleArrOvrdDef(SingleMemberDoubleArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 7.0)
                fail("SingleMemberDoubleArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberBooleanArrOvrdDef(e.getAnnotation(SingleMemberBooleanArrayDef.class), e);
    }
    static void checkSingleMemberBooleanArrOvrdDef(SingleMemberBooleanArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || !a.value()[0])
                fail("SingleMemberBooleanArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberStringArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberStringArrOvrdDef(e.getAnnotation(SingleMemberStringArrayDef.class), e);
    }
    static void checkSingleMemberStringArrOvrdDef(SingleMemberStringArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || !(a.value()[0].equals("custom")))
                fail("SingleMemberStringArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberStringArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberClassArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberClassArrOvrdDef(e.getAnnotation(SingleMemberClassArrayDef.class), e);
    }
    static void checkSingleMemberClassArrOvrdDef(SingleMemberClassArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != Map.class)
                fail("SingleMemberClassArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClassArrOvrdDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrOvrdDef(AnnotatedElement e) {
        checkSingleMemberEnumArrOvrdDef(e.getAnnotation(SingleMemberEnumArrayDef.class), e);
    }
    static void checkSingleMemberEnumArrOvrdDef(SingleMemberEnumArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != Stooge.MOE)
                fail("SingleMemberEnumArrOvrdDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnumArrOvrdDef " + e + ": " + t);
        }
    }

    // Single member array with default (accept)
    static void checkSingleMemberByteArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberByteArrAcceptDef(e.getAnnotation(SingleMemberByteArrayDef.class), e);
    }
    static void checkSingleMemberByteArrAcceptDef(SingleMemberByteArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != (byte)11)
                fail("SingleMemberByteArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberByteArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberShortArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberShortArrAcceptDef(e.getAnnotation(SingleMemberShortArrayDef.class), e);
    }
    static void checkSingleMemberShortArrAcceptDef(SingleMemberShortArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != (short)12)
                fail("SingleMemberShortArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberShortArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberIntArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberIntArrAcceptDef(e.getAnnotation(SingleMemberIntArrayDef.class), e);
    }
    static void checkSingleMemberIntArrAcceptDef(SingleMemberIntArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 13)
                fail("SingleMemberIntArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberIntArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberLongArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberLongArrAcceptDef(e.getAnnotation(SingleMemberLongArrayDef.class), e);
    }
    static void checkSingleMemberLongArrAcceptDef(SingleMemberLongArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 14L)
                fail("SingleMemberLongArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberLongArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberCharArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberCharArrAcceptDef(e.getAnnotation(SingleMemberCharArrayDef.class), e);
    }
    static void checkSingleMemberCharArrAcceptDef(SingleMemberCharArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 'V')
                fail("SingleMemberCharArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberCharArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberFloatArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberFloatArrAcceptDef(e.getAnnotation(SingleMemberFloatArrayDef.class), e);
    }
    static void checkSingleMemberFloatArrAcceptDef(SingleMemberFloatArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 16.0f)
                fail("SingleMemberFloatArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberFloatArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberDoubleArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberDoubleArrAcceptDef(e.getAnnotation(SingleMemberDoubleArrayDef.class), e);
    }
    static void checkSingleMemberDoubleArrAcceptDef(SingleMemberDoubleArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != 17.0)
                fail("SingleMemberDoubleArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberDoubleArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberBooleanArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberBooleanArrAcceptDef(e.getAnnotation(SingleMemberBooleanArrayDef.class), e);
    }
    static void checkSingleMemberBooleanArrAcceptDef(SingleMemberBooleanArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0])
                fail("SingleMemberBooleanArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberBooleanArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberStringArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberStringArrAcceptDef(e.getAnnotation(SingleMemberStringArrayDef.class), e);
    }
    static void checkSingleMemberStringArrAcceptDef(SingleMemberStringArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || !(a.value()[0].equals("default")))
                fail("SingleMemberStringArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberStringArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberClassArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberClassArrAcceptDef(e.getAnnotation(SingleMemberClassArrayDef.class), e);
    }
    static void checkSingleMemberClassArrAcceptDef(SingleMemberClassArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != Class.class)
                fail("SingleMemberClassArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberClassArrAcceptDef " + e + ": " + t);
        }
    }

    static void checkSingleMemberEnumArrAcceptDef(AnnotatedElement e) {
        checkSingleMemberEnumArrAcceptDef(e.getAnnotation(SingleMemberEnumArrayDef.class), e);
    }
    static void checkSingleMemberEnumArrAcceptDef(SingleMemberEnumArrayDef a, AnnotatedElement e) {
        numTests++;
        try {
            if (a.value().length != 1 || a.value()[0] != Stooge.LARRY)
                fail("SingleMemberEnumArrAcceptDef " + e + " = " + a.value());
        } catch(Throwable t) {
            fail("SingleMemberEnumArrAcceptDef " + e + ": " + t);
        }
    }

    // Verfification methods for equals/hashCode/serialization

    static <T extends Annotation> void checkEquals(AnnotatedElement e1, AnnotatedElement e2, Class<T> annoType) {
        numTests++;
        T a1 = e1.getAnnotation(annoType);
        T a2 = e2.getAnnotation(annoType);
        try {
            if (!a1.equals(a2))
                fail(a1 + " != " + a2);
            if (a1.hashCode() != a2.hashCode())
                fail(a1 + ".hashCode() [" + a1.hashCode() + "] != " + a2 + " .hashCode()["+ a2.hashCode()+"]");
            if (!(a1.toString().equals(a2.toString())))
                fail(a1 + ".toString() != " + a2 + ".toString()");
        } catch(Throwable t) {
            fail(a1 + " == " + a2 + ": " + t);
        }
    }

    static <T extends Annotation> void checkUnequals(AnnotatedElement e1, AnnotatedElement e2, Class<T> annoType) {
        numTests++;
        T a1 = e1.getAnnotation(annoType);
        T a2 = e2.getAnnotation(annoType);
        try {
            if (a1.equals(a2))
                fail(a1 + " == " + a2);
            if (a1.hashCode() == a2.hashCode())
                fail(a1 + ".hashCode() [" + a1.hashCode() + "] == " + a2 + " .hashCode()[" + a2.hashCode() + "]");
            if (a1.toString().equals(a2.toString()))
                fail(a1 + ".toString() == " + a2 + ".toString()");
        } catch(Throwable t) {
            fail(a1 + " != " + a2 + ": " + t);
        }
    }

    // Verfification method for serialization/deserialization

    static <T extends Annotation> void checkSerialization(AnnotatedElement e, Class<T> annoType) {
        numTests++;
        T a1 = e.getAnnotation(annoType);
        Object a2 = deepCopy(a1);
        try {
            if (!a1.equals(a2))
                fail("Serialization: " + a1 + " != " + a2);
            if (a1.hashCode() != a2.hashCode())
                fail("Serialization: " + a1 + ".hashCode() [" + a1.hashCode() + "] != " + a2 + " .hashCode()["+a2.hashCode()+"]");
            if (!(a1.toString().equals(a2.toString())))
                fail("Serialization: " + a1 + ".toString() != " + a2 + ".toString()");
        } catch(Throwable t) {
            fail("Serialization: " + a1 + " == " + a2 + ": " + t);
        }
    }

    private static Object deepCopy(Object original) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(original);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(
                bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            return ois.readObject();
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Verification method for inheritance test
    static void checkInheritence(AnnotatedElement e, boolean shouldHaveFoo, boolean shouldHaveBar) {
        numTests++;
        try {
            boolean hasFoo = e.isAnnotationPresent(Foo.class);
            boolean hasBar = e.isAnnotationPresent(Bar.class);
            if (hasFoo != shouldHaveFoo || hasBar != shouldHaveBar)
                fail("Inheritance(1): " + e +" - Foo: " + hasFoo + ", Bar: " + hasBar);

            // Now test getAnnotations
            hasFoo = hasBar = false;
            Annotation[] allAnnotations = e.getAnnotations();
            for (Annotation a : allAnnotations) {
                if (a instanceof Foo)
                    hasFoo = true;
                else if (a instanceof Bar)
                    hasBar = true;
            }
            if (hasFoo != shouldHaveFoo ||hasBar != shouldHaveBar)
                fail("Inheritance(2): " + e +" - Foo: " + hasFoo + ", Bar: " + hasBar);
        } catch(Throwable t) {
            fail("Inheritance: " + e +": " + t);
        }
    }

    // Verification method for declared annotations test
    static void checkDeclaredAnnotations(AnnotatedElement e, boolean shouldHaveFoo, boolean shouldHaveBar) {
        numTests++;
        try {
            boolean hasFoo = false;
            boolean hasBar = false;
            Annotation[] declaredAnnotations = e.getDeclaredAnnotations();
            for (Annotation a : declaredAnnotations) {
                if (a instanceof Foo)
                    hasFoo = true;
                else if (a instanceof Bar)
                    hasBar = true;
            }
            if (hasFoo != shouldHaveFoo ||hasBar != shouldHaveBar)
                fail("Declared annotations: " + e +" - Foo: " + hasFoo + ", Bar: " + hasBar);
        } catch(Throwable t) {
            fail("Declared annotations: " + e +": " + t);
        }
    }


    // ANNOTATED METHODS

    @ScalarTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    public void scalarTypesMethod() { }

    @ScalarTypesWithDefault ( )
    public void scalarTypesAcceptDefaultMethod() { }

    @ScalarTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE
    )
    public void scalarTypesOverrideDefaultMethod() { }

    @ArrayTypes (
        b =    { },
        s =    { },
        i =    { },
        l =    { },
        c =    { },
        f =    { },
        d =    { },
        bool = { },
        str =  { },
        cls =  { },
        e =    { },
        a =    { }
    )
    public void emptyArrayTypesMethod() { }

    @ArrayTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    public void singleElementArrayTypesMethod() { }

    @ArrayTypes (
        b =    { 1, 2 },
        s =    { 2, 3 },
        i =    { 3, 4 },
        l =    { 4L, 5L },
        c =    { '5', '6' },
        f =    { 6.0f, 7.0f },
        d =    { 7.0, 8.0 },
        bool = { true, false },
        str =  { "custom", "paint" },
        cls =  { Map.class, Set.class },
        e =    { Stooge.MOE, Stooge.CURLY },
        a =    { @Point(x = 1, y = 2),  @Point(x = 3, y = 4) }
    )
    public void twoElementArrayTypesMethod() { }

    @ArrayTypesWithDefault (
    )
    public void arrayTypesAcceptDefaultMethod() { }

    @ArrayTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    public void arrayTypesOverrideDefaultMethod() { }

    // Marker
    @Marker public void markerMethod() { }

    // Single-member (shorthand)
    @SingleMemberByte(1)          public void SingleMemberByte()    {}
    @SingleMemberShort(2)         public void SingleMemberShort()   {}
    @SingleMemberInt(3)           public void SingleMemberInt()     {}
    @SingleMemberLong(4L)         public void SingleMemberLong()    {}
    @SingleMemberChar('5')        public void SingleMemberChar()    {}
    @SingleMemberFloat(6.0f)      public void SingleMemberFloat()   {}
    @SingleMemberDouble(7.0)      public void SingleMemberDouble()  {}
    @SingleMemberBoolean(true)    public void SingleMemberBoolean() {}
    @SingleMemberString("custom") public void SingleMemberString()  {}
    @SingleMemberClass(Map.class) public void SingleMemberClass()   {}
    @SingleMemberEnum(Stooge.MOE)        public void SingleMemberEnum()    {}

    // Single-member with default (Override)
    @SingleMemberByteWithDef(1)          public void SingleMemberByteOvrdDef()    {}
    @SingleMemberShortWithDef(2)         public void SingleMemberShortOvrdDef()   {}
    @SingleMemberIntWithDef(3)           public void SingleMemberIntOvrdDef()     {}
    @SingleMemberLongWithDef(4L)         public void SingleMemberLongOvrdDef()    {}
    @SingleMemberCharWithDef('5')        public void SingleMemberCharOvrdDef()    {}
    @SingleMemberFloatWithDef(6.0f)      public void SingleMemberFloatOvrdDef()   {}
    @SingleMemberDoubleWithDef(7.0)      public void SingleMemberDoubleOvrdDef()  {}
    @SingleMemberBooleanWithDef(true)    public void SingleMemberBooleanOvrdDef() {}
    @SingleMemberStringWithDef("custom") public void SingleMemberStringOvrdDef()  {}
    @SingleMemberClassWithDef(Map.class) public void SingleMemberClassOvrdDef()   {}
    @SingleMemberEnumWithDef(Stooge.MOE)        public void SingleMemberEnumOvrdDef()    {}

    // Single-member with default (Accept)
    @SingleMemberByteWithDef    public void SingleMemberByteAcceptDef()    {}
    @SingleMemberShortWithDef   public void SingleMemberShortAcceptDef()   {}
    @SingleMemberIntWithDef     public void SingleMemberIntAcceptDef()     {}
    @SingleMemberLongWithDef    public void SingleMemberLongAcceptDef()    {}
    @SingleMemberCharWithDef    public void SingleMemberCharAcceptDef()    {}
    @SingleMemberFloatWithDef   public void SingleMemberFloatAcceptDef()   {}
    @SingleMemberDoubleWithDef  public void SingleMemberDoubleAcceptDef()  {}
    @SingleMemberBooleanWithDef public void SingleMemberBooleanAcceptDef() {}
    @SingleMemberStringWithDef  public void SingleMemberStringAcceptDef()  {}
    @SingleMemberClassWithDef   public void SingleMemberClassAcceptDef()   {}
    @SingleMemberEnumWithDef    public void SingleMemberEnumAcceptDef()    {}

    // Single member array (empty array)
    @SingleMemberByteArray({})   public void SingleMemberByteArrEmpty()    {}
    @SingleMemberShortArray({})  public void SingleMemberShortArrEmpty()   {}
    @SingleMemberIntArray({})    public void SingleMemberIntArrEmpty()     {}
    @SingleMemberLongArray({})   public void SingleMemberLongArrEmpty()    {}
    @SingleMemberCharArray({})   public void SingleMemberCharArrEmpty()    {}
    @SingleMemberFloatArray({})  public void SingleMemberFloatArrEmpty()   {}
    @SingleMemberDoubleArray({}) public void SingleMemberDoubleArrEmpty()  {}
    @SingleMemberBooleanArray({})public void SingleMemberBooleanArrEmpty() {}
    @SingleMemberStringArray({}) public void SingleMemberStringArrEmpty()  {}
    @SingleMemberClassArray({})  public void SingleMemberClassArrEmpty()   {}
    @SingleMemberEnumArray({})   public void SingleMemberEnumArrEmpty()    {}

    // Single member array (one-element shorthand)
    @SingleMemberByteArray(1)          public void SingleMemberByteArrOne()    {}
    @SingleMemberShortArray(2)         public void SingleMemberShortArrOne()   {}
    @SingleMemberIntArray(3)           public void SingleMemberIntArrOne()     {}
    @SingleMemberLongArray(4L)         public void SingleMemberLongArrOne()    {}
    @SingleMemberCharArray('5')        public void SingleMemberCharArrOne()    {}
    @SingleMemberFloatArray(6.0f)      public void SingleMemberFloatArrOne()   {}
    @SingleMemberDoubleArray(7.0)      public void SingleMemberDoubleArrOne()  {}
    @SingleMemberBooleanArray(true)    public void SingleMemberBooleanArrOne() {}
    @SingleMemberStringArray("custom") public void SingleMemberStringArrOne()  {}
    @SingleMemberClassArray(Map.class) public void SingleMemberClassArrOne()   {}
    @SingleMemberEnumArray(Stooge.MOE)        public void SingleMemberEnumArrOne()    {}

    // Single member array (two elements)
    @SingleMemberByteArray({1, 2})           public void SingleMemberByteArrTwo()   {}
    @SingleMemberShortArray({2, 3})          public void SingleMemberShortArrTwo()  {}
    @SingleMemberIntArray({3, 4})            public void SingleMemberIntArrTwo()    {}
    @SingleMemberLongArray({4L, 5L})         public void SingleMemberLongArrTwo()   {}
    @SingleMemberCharArray({'5', '6'})       public void SingleMemberCharArrTwo()   {}
    @SingleMemberFloatArray({6.0f, 7.0f})    public void SingleMemberFloatArrTwo()  {}
    @SingleMemberDoubleArray({7.0, 8.0})     public void SingleMemberDoubleArrTwo() {}
    @SingleMemberBooleanArray({true, false}) public void SingleMemberBooleanArrTwo(){}
    @SingleMemberStringArray({"custom", "paint"})      public void SingleMemberStringArrTwo(){}
    @SingleMemberClassArray({Map.class, Set.class})    public void SingleMemberClassArrTwo() {}
    @SingleMemberEnumArray({Stooge.MOE, Stooge.CURLY})               public void SingleMemberEnumArrTwo()  {}

    // Single member array with default (override)
    @SingleMemberByteArrayDef(1)          public void SingleMemberByteArrOvrdDef()   {}
    @SingleMemberShortArrayDef(2)         public void SingleMemberShortArrOvrdDef()  {}
    @SingleMemberIntArrayDef(3)           public void SingleMemberIntArrOvrdDef()    {}
    @SingleMemberLongArrayDef(4L)         public void SingleMemberLongArrOvrdDef()   {}
    @SingleMemberCharArrayDef('5')        public void SingleMemberCharArrOvrdDef()   {}
    @SingleMemberFloatArrayDef(6.0f)      public void SingleMemberFloatArrOvrdDef()  {}
    @SingleMemberDoubleArrayDef(7.0)      public void SingleMemberDoubleArrOvrdDef() {}
    @SingleMemberBooleanArrayDef(true)    public void SingleMemberBooleanArrOvrdDef(){}
    @SingleMemberStringArrayDef("custom") public void SingleMemberStringArrOvrdDef() {}
    @SingleMemberClassArrayDef(Map.class) public void SingleMemberClassArrOvrdDef()  {}
    @SingleMemberEnumArrayDef(Stooge.MOE)        public void SingleMemberEnumArrOvrdDef()   {}

    // Single member array with default - accept
    @SingleMemberByteArrayDef    public void SingleMemberByteArrAcceptDef()    {}
    @SingleMemberShortArrayDef   public void SingleMemberShortArrAcceptDef()   {}
    @SingleMemberIntArrayDef     public void SingleMemberIntArrAcceptDef()     {}
    @SingleMemberLongArrayDef    public void SingleMemberLongArrAcceptDef()    {}
    @SingleMemberCharArrayDef    public void SingleMemberCharArrAcceptDef()    {}
    @SingleMemberFloatArrayDef   public void SingleMemberFloatArrAcceptDef()   {}
    @SingleMemberDoubleArrayDef  public void SingleMemberDoubleArrAcceptDef()  {}
    @SingleMemberBooleanArrayDef public void SingleMemberBooleanArrAcceptDef() {}
    @SingleMemberStringArrayDef  public void SingleMemberStringArrAcceptDef()  {}
    @SingleMemberClassArrayDef   public void SingleMemberClassArrAcceptDef()   {}
    @SingleMemberEnumArrayDef    public void SingleMemberEnumArrAcceptDef()    {}

    // ANNOTATED FIELDS
    @ScalarTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    public int scalarTypesField;

    @ScalarTypesWithDefault ( )
    public int scalarTypesAcceptDefaultField;

    @ScalarTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE
    )
    public int scalarTypesOverrideDefaultField;

    @ArrayTypes (
        b =    { },
        s =    { },
        i =    { },
        l =    { },
        c =    { },
        f =    { },
        d =    { },
        bool = { },
        str =  { },
        cls =  { },
        e =    { },
        a =    { }
    )
    public int emptyArrayTypesField;

    @ArrayTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    public int singleElementArrayTypesField;

    @ArrayTypes (
        b =    { 1, 2 },
        s =    { 2, 3 },
        i =    { 3, 4 },
        l =    { 4L, 5L },
        c =    { '5', '6' },
        f =    { 6.0f, 7.0f },
        d =    { 7.0, 8.0 },
        bool = { true, false },
        str =  { "custom", "paint" },
        cls =  { Map.class, Set.class },
        e =    { Stooge.MOE, Stooge.CURLY },
        a =    { @Point(x = 1, y = 2), @Point(x = 3, y = 4) }
    )
    public int twoElementArrayTypesField;

    @ArrayTypesWithDefault ( )
    public int arrayTypesAcceptDefaultField;

    @ArrayTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    public int arrayTypesOverrideDefaultField;

    @Marker public int markerField;

    // Single-member (shorthand)
    @SingleMemberByte(1)          public int SingleMemberByteField;
    @SingleMemberShort(2)         public int SingleMemberShortField;
    @SingleMemberInt(3)           public int SingleMemberIntField;
    @SingleMemberLong(4L)         public int SingleMemberLongField;
    @SingleMemberChar('5')        public int SingleMemberCharField;
    @SingleMemberFloat(6.0f)      public int SingleMemberFloatField;
    @SingleMemberDouble(7.0)      public int SingleMemberDoubleField;
    @SingleMemberBoolean(true)    public int SingleMemberBooleanField;
    @SingleMemberString("custom") public int SingleMemberStringField;
    @SingleMemberClass(Map.class) public int SingleMemberClassField;
    @SingleMemberEnum(Stooge.MOE)        public int SingleMemberEnumField;

    // Single-member with default (Override)
    @SingleMemberByteWithDef(1)          public int SingleMemberByteOvrdDefField;
    @SingleMemberShortWithDef(2)         public int SingleMemberShortOvrdDefField;
    @SingleMemberIntWithDef(3)           public int SingleMemberIntOvrdDefField;
    @SingleMemberLongWithDef(4L)         public int SingleMemberLongOvrdDefField;
    @SingleMemberCharWithDef('5')        public int SingleMemberCharOvrdDefField;
    @SingleMemberFloatWithDef(6.0f)      public int SingleMemberFloatOvrdDefField;
    @SingleMemberDoubleWithDef(7.0)      public int SingleMemberDoubleOvrdDefField;
    @SingleMemberBooleanWithDef(true)    public int SingleMemberBooleanOvrdDefField;
    @SingleMemberStringWithDef("custom") public int SingleMemberStringOvrdDefField;
    @SingleMemberClassWithDef(Map.class) public int SingleMemberClassOvrdDefField;
    @SingleMemberEnumWithDef(Stooge.MOE)        public int SingleMemberEnumOvrdDefField;

    // Single-member with default (Accept)
    @SingleMemberByteWithDef    public int SingleMemberByteAcceptDefField;
    @SingleMemberShortWithDef   public int SingleMemberShortAcceptDefField;
    @SingleMemberIntWithDef     public int SingleMemberIntAcceptDefField;
    @SingleMemberLongWithDef    public int SingleMemberLongAcceptDefField;
    @SingleMemberCharWithDef    public int SingleMemberCharAcceptDefField;
    @SingleMemberFloatWithDef   public int SingleMemberFloatAcceptDefField;
    @SingleMemberDoubleWithDef  public int SingleMemberDoubleAcceptDefField;
    @SingleMemberBooleanWithDef public int SingleMemberBooleanAcceptDefField;
    @SingleMemberStringWithDef  public int SingleMemberStringAcceptDefField;
    @SingleMemberClassWithDef   public int SingleMemberClassAcceptDefField;
    @SingleMemberEnumWithDef    public int SingleMemberEnumAcceptDefField;

    // Single member array (empty array)
    @SingleMemberByteArray({})   public int SingleMemberByteArrEmptyField;
    @SingleMemberShortArray({})  public int SingleMemberShortArrEmptyField;
    @SingleMemberIntArray({})    public int SingleMemberIntArrEmptyField;
    @SingleMemberLongArray({})   public int SingleMemberLongArrEmptyField;
    @SingleMemberCharArray({})   public int SingleMemberCharArrEmptyField;
    @SingleMemberFloatArray({})  public int SingleMemberFloatArrEmptyField;
    @SingleMemberDoubleArray({}) public int SingleMemberDoubleArrEmptyField;
    @SingleMemberBooleanArray({})public int SingleMemberBooleanArrEmptyField;
    @SingleMemberStringArray({}) public int SingleMemberStringArrEmptyField;
    @SingleMemberClassArray({})  public int SingleMemberClassArrEmptyField;
    @SingleMemberEnumArray({})   public int SingleMemberEnumArrEmptyField;

    // Single member array (one-element shorthand)
    @SingleMemberByteArray(1)          public int SingleMemberByteArrOneField;
    @SingleMemberShortArray(2)         public int SingleMemberShortArrOneField;
    @SingleMemberIntArray(3)           public int SingleMemberIntArrOneField;
    @SingleMemberLongArray(4L)         public int SingleMemberLongArrOneField;
    @SingleMemberCharArray('5')        public int SingleMemberCharArrOneField;
    @SingleMemberFloatArray(6.0f)      public int SingleMemberFloatArrOneField;
    @SingleMemberDoubleArray(7.0)      public int SingleMemberDoubleArrOneField;
    @SingleMemberBooleanArray(true)    public int SingleMemberBooleanArrOneField;
    @SingleMemberStringArray("custom") public int SingleMemberStringArrOneField;
    @SingleMemberClassArray(Map.class) public int SingleMemberClassArrOneField;
    @SingleMemberEnumArray(Stooge.MOE)        public int SingleMemberEnumArrOneField;

    // Single member array (two elements)
    @SingleMemberByteArray({1, 2})         public int SingleMemberByteArrTwoField;
    @SingleMemberShortArray({2, 3})        public int SingleMemberShortArrTwoField;
    @SingleMemberIntArray({3, 4})          public int SingleMemberIntArrTwoField;
    @SingleMemberLongArray({4L, 5L})       public int SingleMemberLongArrTwoField;
    @SingleMemberCharArray({'5', '6'})     public int SingleMemberCharArrTwoField;
    @SingleMemberFloatArray({6.0f, 7.0f})  public int SingleMemberFloatArrTwoField;
    @SingleMemberDoubleArray({7.0, 8.0})   public int SingleMemberDoubleArrTwoField;
    @SingleMemberBooleanArray({true,false})         public int SingleMemberBooleanArrTwoField;
    @SingleMemberStringArray({"custom", "paint"})   public int SingleMemberStringArrTwoField;
    @SingleMemberClassArray({Map.class, Set.class}) public int SingleMemberClassArrTwoField;
    @SingleMemberEnumArray({Stooge.MOE, Stooge.CURLY})            public int SingleMemberEnumArrTwoField;

    // Single member array with default (override)
    @SingleMemberByteArrayDef(1)        public int SingleMemberByteArrOvrdDefField;
    @SingleMemberShortArrayDef(2)       public int SingleMemberShortArrOvrdDefField;
    @SingleMemberIntArrayDef(3)         public int SingleMemberIntArrOvrdDefField;
    @SingleMemberLongArrayDef(4L)       public int SingleMemberLongArrOvrdDefField;
    @SingleMemberCharArrayDef('5')      public int SingleMemberCharArrOvrdDefField;
    @SingleMemberFloatArrayDef(6.0f)    public int SingleMemberFloatArrOvrdDefField;
    @SingleMemberDoubleArrayDef(7.0)    public int SingleMemberDoubleArrOvrdDefField;
    @SingleMemberBooleanArrayDef(true)  public int SingleMemberBooleanArrOvrdDefField;
    @SingleMemberStringArrayDef("custom") public int SingleMemberStringArrOvrdDefField;
    @SingleMemberClassArrayDef(Map.class) public int SingleMemberClassArrOvrdDefField;
    @SingleMemberEnumArrayDef(Stooge.MOE)        public int SingleMemberEnumArrOvrdDefField;

    // Single member array with default - accept
    @SingleMemberByteArrayDef    public int SingleMemberByteArrAcceptDefField;
    @SingleMemberShortArrayDef   public int SingleMemberShortArrAcceptDefField;
    @SingleMemberIntArrayDef     public int SingleMemberIntArrAcceptDefField;
    @SingleMemberLongArrayDef    public int SingleMemberLongArrAcceptDefField;
    @SingleMemberCharArrayDef    public int SingleMemberCharArrAcceptDefField;
    @SingleMemberFloatArrayDef   public int SingleMemberFloatArrAcceptDefField;
    @SingleMemberDoubleArrayDef  public int SingleMemberDoubleArrAcceptDefField;
    @SingleMemberBooleanArrayDef public int SingleMemberBooleanArrAcceptDefField;
    @SingleMemberStringArrayDef  public int SingleMemberStringArrAcceptDefField;
    @SingleMemberClassArrayDef   public int SingleMemberClassArrAcceptDefField;
    @SingleMemberEnumArrayDef    public int SingleMemberEnumArrAcceptDefField;

    // ANNOTATED ENUM CONSTANTS
    enum TestType {
    @ScalarTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    scalarTypesField,

    @ScalarTypesWithDefault ( )
    scalarTypesAcceptDefaultField,

    @ScalarTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE
    )
    scalarTypesOverrideDefaultField,

    @ArrayTypes (
        b =    { },
        s =    { },
        i =    { },
        l =    { },
        c =    { },
        f =    { },
        d =    { },
        bool = { },
        str =  { },
        cls =  { },
        e =    { },
        a  =   { }
    )
    emptyArrayTypesField,

    @ArrayTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    singleElementArrayTypesField,

    @ArrayTypes (
        b =    { 1, 2 },
        s =    { 2, 3 },
        i =    { 3, 4 },
        l =    { 4L, 5L },
        c =    { '5', '6' },
        f =    { 6.0f, 7.0f },
        d =    { 7.0, 8.0 },
        bool = { true, false },
        str =  { "custom", "paint" },
        cls =  { Map.class, Set.class },
        e =    { Stooge.MOE, Stooge.CURLY },
        a =    { @Point(x = 1, y = 2),  @Point(x = 3, y = 4) }
    )
    twoElementArrayTypesField,

    @ArrayTypesWithDefault ( )
    arrayTypesAcceptDefaultField,

    @ArrayTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    arrayTypesOverrideDefaultField,

    // marker
    @Marker marker,

    // Single-member (shorthand)
    @SingleMemberByte(1)          SingleMemberByte,
    @SingleMemberShort(2)         SingleMemberShort,
    @SingleMemberInt(3)           SingleMemberInt,
    @SingleMemberLong(4L)         SingleMemberLong,
    @SingleMemberChar('5')        SingleMemberChar,
    @SingleMemberFloat(6.0f)      SingleMemberFloat,
    @SingleMemberDouble(7.0)      SingleMemberDouble,
    @SingleMemberBoolean(true)    SingleMemberBoolean,
    @SingleMemberString("custom") SingleMemberString,
    @SingleMemberClass(Map.class) SingleMemberClass,
    @SingleMemberEnum(Stooge.MOE)        SingleMemberEnum,

    // Single-member with default (Override)
    @SingleMemberByteWithDef(1)          SingleMemberByteOvrdDef,
    @SingleMemberShortWithDef(2)         SingleMemberShortOvrdDef,
    @SingleMemberIntWithDef(3)           SingleMemberIntOvrdDef,
    @SingleMemberLongWithDef(4L)         SingleMemberLongOvrdDef,
    @SingleMemberCharWithDef('5')        SingleMemberCharOvrdDef,
    @SingleMemberFloatWithDef(6.0f)      SingleMemberFloatOvrdDef,
    @SingleMemberDoubleWithDef(7.0)      SingleMemberDoubleOvrdDef,
    @SingleMemberBooleanWithDef(true)    SingleMemberBooleanOvrdDef,
    @SingleMemberStringWithDef("custom") SingleMemberStringOvrdDef,
    @SingleMemberClassWithDef(Map.class) SingleMemberClassOvrdDef,
    @SingleMemberEnumWithDef(Stooge.MOE)        SingleMemberEnumOvrdDef,

    // Single-member with default (Accept)
    @SingleMemberByteWithDef    SingleMemberByteAcceptDef,
    @SingleMemberShortWithDef   SingleMemberShortAcceptDef,
    @SingleMemberIntWithDef     SingleMemberIntAcceptDef,
    @SingleMemberLongWithDef    SingleMemberLongAcceptDef,
    @SingleMemberCharWithDef    SingleMemberCharAcceptDef,
    @SingleMemberFloatWithDef   SingleMemberFloatAcceptDef,
    @SingleMemberDoubleWithDef  SingleMemberDoubleAcceptDef,
    @SingleMemberBooleanWithDef SingleMemberBooleanAcceptDef,
    @SingleMemberStringWithDef  SingleMemberStringAcceptDef,
    @SingleMemberClassWithDef   SingleMemberClassAcceptDef,
    @SingleMemberEnumWithDef    SingleMemberEnumAcceptDef,

    // Single member array (empty array)
    @SingleMemberByteArray({})   SingleMemberByteArrEmpty,
    @SingleMemberShortArray({})  SingleMemberShortArrEmpty,
    @SingleMemberIntArray({})    SingleMemberIntArrEmpty,
    @SingleMemberLongArray({})   SingleMemberLongArrEmpty,
    @SingleMemberCharArray({})   SingleMemberCharArrEmpty,
    @SingleMemberFloatArray({})  SingleMemberFloatArrEmpty,
    @SingleMemberDoubleArray({}) SingleMemberDoubleArrEmpty,
    @SingleMemberBooleanArray({})SingleMemberBooleanArrEmpty,
    @SingleMemberStringArray({}) SingleMemberStringArrEmpty,
    @SingleMemberClassArray({})  SingleMemberClassArrEmpty,
    @SingleMemberEnumArray({})   SingleMemberEnumArrEmpty,

    // Single member array (one-element shorthand)
    @SingleMemberByteArray(1)          SingleMemberByteArrOne,
    @SingleMemberShortArray(2)         SingleMemberShortArrOne,
    @SingleMemberIntArray(3)           SingleMemberIntArrOne,
    @SingleMemberLongArray(4L)         SingleMemberLongArrOne,
    @SingleMemberCharArray('5')        SingleMemberCharArrOne,
    @SingleMemberFloatArray(6.0f)      SingleMemberFloatArrOne,
    @SingleMemberDoubleArray(7.0)      SingleMemberDoubleArrOne,
    @SingleMemberBooleanArray(true)    SingleMemberBooleanArrOne,
    @SingleMemberStringArray("custom") SingleMemberStringArrOne,
    @SingleMemberClassArray(Map.class) SingleMemberClassArrOne,
    @SingleMemberEnumArray(Stooge.MOE)        SingleMemberEnumArrOne,

    // Single member array (two elements)
    @SingleMemberByteArray({1, 2})         SingleMemberByteArrTwo,
    @SingleMemberShortArray({2, 3})        SingleMemberShortArrTwo,
    @SingleMemberIntArray({3, 4})          SingleMemberIntArrTwo,
    @SingleMemberLongArray({4L, 5L})       SingleMemberLongArrTwo,
    @SingleMemberCharArray({'5', '6'})     SingleMemberCharArrTwo,
    @SingleMemberFloatArray({6.0f, 7.0f})  SingleMemberFloatArrTwo,
    @SingleMemberDoubleArray({7.0, 8.0})   SingleMemberDoubleArrTwo,
    @SingleMemberBooleanArray({true,false})         SingleMemberBooleanArrTwo,
    @SingleMemberStringArray({"custom", "paint"})   SingleMemberStringArrTwo,
    @SingleMemberClassArray({Map.class, Set.class}) SingleMemberClassArrTwo,
    @SingleMemberEnumArray({Stooge.MOE, Stooge.CURLY})            SingleMemberEnumArrTwo,

    // Single member array with default (override)
    @SingleMemberByteArrayDef(1)        SingleMemberByteArrOvrdDef,
    @SingleMemberShortArrayDef(2)       SingleMemberShortArrOvrdDef,
    @SingleMemberIntArrayDef(3)         SingleMemberIntArrOvrdDef,
    @SingleMemberLongArrayDef(4L)       SingleMemberLongArrOvrdDef,
    @SingleMemberCharArrayDef('5')      SingleMemberCharArrOvrdDef,
    @SingleMemberFloatArrayDef(6.0f)    SingleMemberFloatArrOvrdDef,
    @SingleMemberDoubleArrayDef(7.0)    SingleMemberDoubleArrOvrdDef,
    @SingleMemberBooleanArrayDef(true)  SingleMemberBooleanArrOvrdDef,
    @SingleMemberStringArrayDef("custom") SingleMemberStringArrOvrdDef,
    @SingleMemberClassArrayDef(Map.class) SingleMemberClassArrOvrdDef,
    @SingleMemberEnumArrayDef(Stooge.MOE)        SingleMemberEnumArrOvrdDef,

    // Single member array with default - accept
    @SingleMemberByteArrayDef    SingleMemberByteArrAcceptDef,
    @SingleMemberShortArrayDef   SingleMemberShortArrAcceptDef,
    @SingleMemberIntArrayDef     SingleMemberIntArrAcceptDef,
    @SingleMemberLongArrayDef    SingleMemberLongArrAcceptDef,
    @SingleMemberCharArrayDef    SingleMemberCharArrAcceptDef,
    @SingleMemberFloatArrayDef   SingleMemberFloatArrAcceptDef,
    @SingleMemberDoubleArrayDef  SingleMemberDoubleArrAcceptDef,
    @SingleMemberBooleanArrayDef SingleMemberBooleanArrAcceptDef,
    @SingleMemberStringArrayDef  SingleMemberStringArrAcceptDef,
    @SingleMemberClassArrayDef   SingleMemberClassArrAcceptDef,
    @SingleMemberEnumArrayDef    SingleMemberEnumArrAcceptDef,
    }

    // ANNOTATED CONSTRUCTORS

    @ScalarTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    public UnitTest(Iterator it) { } // scalar types

    @ScalarTypesWithDefault ( )
    public UnitTest(Set s) { } // scalarTypesAcceptDefault

    @ScalarTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE
    )
    public UnitTest(Map s) { } // scalarTypesOverrideDefault

    @ArrayTypes (
        b =    { },
        s =    { },
        i =    { },
        l =    { },
        c =    { },
        f =    { },
        d =    { },
        bool = { },
        str =  { },
        cls =  { },
        e =    { },
        a =    { }
    )
    public UnitTest(List l){ } // emptyArrayTypes

    @ArrayTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    public UnitTest(Collection c) { } // singleElementArrayTypes

    @ArrayTypes (
        b =    { 1, 2 },
        s =    { 2, 3 },
        i =    { 3, 4 },
        l =    { 4L, 5L },
        c =    { '5', '6' },
        f =    { 6.0f, 7.0f },
        d =    { 7.0, 8.0 },
        bool = { true, false },
        str =  { "custom", "paint" },
        cls =  { Map.class, Set.class },
        e =    { Stooge.MOE, Stooge.CURLY },
        a =    { @Point(x = 1, y = 2),  @Point(x = 3, y = 4) }
    )
    public UnitTest(SortedSet ss) { } // twoElementArrayTypes

    @ArrayTypesWithDefault ( )
    public UnitTest(SortedMap sm) { } // arrayTypesAcceptDefault

    @ArrayTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    public UnitTest(RandomAccess r) { } // arrayTypesOverrideDefault

    // Marker
    @Marker public UnitTest() { } // marker

    // Single-member (shorthand)
    @SingleMemberByte(1)          public UnitTest(byte b)    { }
    @SingleMemberShort(2)         public UnitTest(short s)   { }
    @SingleMemberInt(3)           public UnitTest(int i)     { }
    @SingleMemberLong(4L)         public UnitTest(long l)    { }
    @SingleMemberChar('5')        public UnitTest(char c)    { }
    @SingleMemberFloat(6.0f)      public UnitTest(float f)   { }
    @SingleMemberDouble(7.0)      public UnitTest(double d)  { }
    @SingleMemberBoolean(true)    public UnitTest(boolean b) { }
    @SingleMemberString("custom") public UnitTest(String s)  { }
    @SingleMemberClass(Map.class) public UnitTest(Class c)   { }
    @SingleMemberEnum(Stooge.MOE)        public UnitTest(Enum e)    { }

    // Single-member with default (Override)
    @SingleMemberByteWithDef(1)          public UnitTest(byte b, Set s)    { }
    @SingleMemberShortWithDef(2)         public UnitTest(short s, Set x)   { }
    @SingleMemberIntWithDef(3)           public UnitTest(int i, Set s)     { }
    @SingleMemberLongWithDef(4L)         public UnitTest(long l, Set s)    { }
    @SingleMemberCharWithDef('5')        public UnitTest(char c, Set s)    { }
    @SingleMemberFloatWithDef(6.0f)      public UnitTest(float f, Set s)   { }
    @SingleMemberDoubleWithDef(7.0)      public UnitTest(double d, Set s)  { }
    @SingleMemberBooleanWithDef(true)    public UnitTest(boolean b, Set s) { }
    @SingleMemberStringWithDef("custom") public UnitTest(String s, Set x)  { }
    @SingleMemberClassWithDef(Map.class) public UnitTest(Class c, Set s)   { }
    @SingleMemberEnumWithDef(Stooge.MOE)        public UnitTest(Enum e, Set s)    { }

    // Single-member with default (Accept)
    @SingleMemberByteWithDef    public UnitTest(byte b, Map m)    { }
    @SingleMemberShortWithDef   public UnitTest(short s, Map m)   { }
    @SingleMemberIntWithDef     public UnitTest(int i, Map m)     { }
    @SingleMemberLongWithDef    public UnitTest(long l, Map m)    { }
    @SingleMemberCharWithDef    public UnitTest(char c, Map m)    { }
    @SingleMemberFloatWithDef   public UnitTest(float f, Map m)   { }
    @SingleMemberDoubleWithDef  public UnitTest(double d, Map m)  { }
    @SingleMemberBooleanWithDef public UnitTest(boolean b, Map m) { }
    @SingleMemberStringWithDef  public UnitTest(String s, Map m)  { }
    @SingleMemberClassWithDef   public UnitTest(Class c, Map m)   { }
    @SingleMemberEnumWithDef    public UnitTest(Enum e, Map m)    { }

    // Single member array (empty array)
    @SingleMemberByteArray({})   public UnitTest(byte[] b)    { }
    @SingleMemberShortArray({})  public UnitTest(short[] s)   { }
    @SingleMemberIntArray({})    public UnitTest(int[] i)     { }
    @SingleMemberLongArray({})   public UnitTest(long[] l)    { }
    @SingleMemberCharArray({})   public UnitTest(char[] c)    { }
    @SingleMemberFloatArray({})  public UnitTest(float[] f)   { }
    @SingleMemberDoubleArray({}) public UnitTest(double[] d)  { }
    @SingleMemberBooleanArray({})public UnitTest(boolean[] b) { }
    @SingleMemberStringArray({}) public UnitTest(String[] s)  { }
    @SingleMemberClassArray({})  public UnitTest(Class[] c)   { }
    @SingleMemberEnumArray({})   public UnitTest(Enum[] e)    { }

    // Single member array (one-element shorthand)
    @SingleMemberByteArray(1)          public UnitTest(byte[] b, Set s)    { }
    @SingleMemberShortArray(2)         public UnitTest(short[] s, Set x)   { }
    @SingleMemberIntArray(3)           public UnitTest(int[] i, Set s)     { }
    @SingleMemberLongArray(4L)         public UnitTest(long[] l, Set s)    { }
    @SingleMemberCharArray('5')        public UnitTest(char[] c, Set s)    { }
    @SingleMemberFloatArray(6.0f)      public UnitTest(float[] f, Set s)   { }
    @SingleMemberDoubleArray(7.0)      public UnitTest(double[] d, Set s)  { }
    @SingleMemberBooleanArray(true)    public UnitTest(boolean[] b, Set s) { }
    @SingleMemberStringArray("custom") public UnitTest(String[] s, Set x)  { }
    @SingleMemberClassArray(Map.class) public UnitTest(Class[] c, Set s)   { }
    @SingleMemberEnumArray(Stooge.MOE)        public UnitTest(Enum[] e, Set s)    { }

    // Single member array (two elements)
    @SingleMemberByteArray({1, 2})           public UnitTest(byte[] b, Map m)    { }
    @SingleMemberShortArray({2, 3})          public UnitTest(short[] s, Map m)   { }
    @SingleMemberIntArray({3, 4})            public UnitTest(int[] i, Map m)     { }
    @SingleMemberLongArray({4L, 5L})         public UnitTest(long[] l, Map m)    { }
    @SingleMemberCharArray({'5', '6'})       public UnitTest(char[] c, Map m)    { }
    @SingleMemberFloatArray({6.0f, 7.0f})    public UnitTest(float[] f, Map m)   { }
    @SingleMemberDoubleArray({7.0, 8.0})     public UnitTest(double[] d, Map m)  { }
    @SingleMemberBooleanArray({true, false}) public UnitTest(boolean[] b, Map m) { }
    @SingleMemberStringArray({"custom", "paint"})  public UnitTest(String[] s, Map m)  { }
    @SingleMemberClassArray({Map.class,Set.class}) public UnitTest(Class[] c, Map m) { }
    @SingleMemberEnumArray({Stooge.MOE, Stooge.CURLY})           public UnitTest(Enum[] e, Map m)  { }


    // Single member array with default (override)
    @SingleMemberByteArrayDef(1)          public UnitTest(byte[] b, List l)    { }
    @SingleMemberShortArrayDef(2)         public UnitTest(short[] s, List l)   { }
    @SingleMemberIntArrayDef(3)           public UnitTest(int[] i, List l)     { }
    @SingleMemberLongArrayDef(4L)         public UnitTest(long[] l, List x)    { }
    @SingleMemberCharArrayDef('5')        public UnitTest(char[] c, List l)    { }
    @SingleMemberFloatArrayDef(6.0f)      public UnitTest(float[] f, List l)   { }
    @SingleMemberDoubleArrayDef(7.0)      public UnitTest(double[] d, List l)  { }
    @SingleMemberBooleanArrayDef(true)    public UnitTest(boolean[] b, List l) { }
    @SingleMemberStringArrayDef("custom") public UnitTest(String[] s, List l)  { }
    @SingleMemberClassArrayDef(Map.class) public UnitTest(Class[] c, List l)   { }
    @SingleMemberEnumArrayDef(Stooge.MOE)        public UnitTest(Enum[] e, List l)    { }

    // Single member array with default - accept
    @SingleMemberByteArrayDef    public UnitTest(byte[] b, Collection c)    { }
    @SingleMemberShortArrayDef   public UnitTest(short[] s, Collection c)   { }
    @SingleMemberIntArrayDef     public UnitTest(int[] i, Collection c)     { }
    @SingleMemberLongArrayDef    public UnitTest(long[] l, Collection c)    { }
    @SingleMemberCharArrayDef    public UnitTest(char[] c, Collection x)    { }
    @SingleMemberFloatArrayDef   public UnitTest(float[] f, Collection c)   { }
    @SingleMemberDoubleArrayDef  public UnitTest(double[] d, Collection c)  { }
    @SingleMemberBooleanArrayDef public UnitTest(boolean[] b, Collection c) { }
    @SingleMemberStringArrayDef  public UnitTest(String[] s, Collection c)  { }
    @SingleMemberClassArrayDef   public UnitTest(Class[] c, Collection x)   { }
    @SingleMemberEnumArrayDef    public UnitTest(Enum[] e, Collection c)    { }

    // ANNOTATED PARAMETERS

    public void scalarTypesParam(
    @ScalarTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    int x) { }


    public void scalarTypesAcceptDefaultParam(
    @ScalarTypesWithDefault int x) { }

    public void scalarTypesOverrideDefaultParam(
    @ScalarTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE
    )
    int x) { }

    public void emptyArrayTypesParam(
    @ArrayTypes (
        b = { },
        s = { },
        i = { },
        l = { },
        c = { },
        f = { },
        d = { },
        bool = { },
        str = { },
        cls = { },
        e = { },
        a = { }
    )
    int x) { }

    public void singleElementArrayTypesParam(
    @ArrayTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    int x) { }

    public void twoElementArrayTypesParam(
    @ArrayTypes (
        b = { 1, 2 },
        s = { 2, 3 },
        i = { 3, 4 },
        l = { 4L, 5L },
        c = { '5', '6' },
        f = { 6.0f, 7.0f },
        d = { 7.0, 8.0 },
        bool = { true, false },
        str = { "custom", "paint" },
        cls = { Map.class, Set.class },
        e = { Stooge.MOE, Stooge.CURLY },
        a = { @Point(x = 1, y = 2),  @Point(x = 3, y = 4) }
    )
    int x) { }

    public void arrayTypesAcceptDefaultParam(
    @ArrayTypesWithDefault
    int x) { }

    public void arrayTypesOverrideDefaultParam(
    @ArrayTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    int x) { }

    // Marker
    public void markerParam(@Marker int x) { }

    // Single-member (shorthand)
    public void SingleMemberByteParam(@SingleMemberByte(1) int x) {}
    public void SingleMemberShortParam(@SingleMemberShort(2) int x) {}
    public void SingleMemberIntParam(@SingleMemberInt(3) int x) {}
    public void SingleMemberLongParam(@SingleMemberLong(4L) int x) {}
    public void SingleMemberCharParam(@SingleMemberChar('5') int x) {}
    public void SingleMemberFloatParam(@SingleMemberFloat(6.0f) int x) {}
    public void SingleMemberDoubleParam(@SingleMemberDouble(7.0) int x) {}
    public void SingleMemberBooleanParam(@SingleMemberBoolean(true) int x) {}
    public void SingleMemberStringParam(@SingleMemberString("custom") int x) {}
    public void SingleMemberClassParam(@SingleMemberClass(Map.class) int x) {}
    public void SingleMemberEnumParam(@SingleMemberEnum(Stooge.MOE) int x) {}

    // Single-member with default (Override)
    public void SingleMemberByteOvrdDefParam(@SingleMemberByteWithDef(1) int x) {}
    public void SingleMemberShortOvrdDefParam(@SingleMemberShortWithDef(2) int x) {}
    public void SingleMemberIntOvrdDefParam(@SingleMemberIntWithDef(3) int x) {}
    public void SingleMemberLongOvrdDefParam(@SingleMemberLongWithDef(4L) int x) {}
    public void SingleMemberCharOvrdDefParam(@SingleMemberCharWithDef('5') int x) {}
    public void SingleMemberFloatOvrdDefParam(@SingleMemberFloatWithDef(6.0f) int x) {}
    public void SingleMemberDoubleOvrdDefParam(@SingleMemberDoubleWithDef(7.0) int x) {}
    public void SingleMemberBooleanOvrdDefParam(@SingleMemberBooleanWithDef(true) int x) {}
    public void SingleMemberStringOvrdDefParam(@SingleMemberStringWithDef("custom") int x) {}
    public void SingleMemberClassOvrdDefParam(@SingleMemberClassWithDef(Map.class)  int x) {}
    public void SingleMemberEnumOvrdDefParam(@SingleMemberEnumWithDef(Stooge.MOE) int x) {}

    // Single-member with default (Accept)
    public void SingleMemberByteAcceptDefParam(@SingleMemberByteWithDef int x) {}
    public void SingleMemberShortAcceptDefParam(@SingleMemberShortWithDef int x) {}
    public void SingleMemberIntAcceptDefParam(@SingleMemberIntWithDef int x) {}
    public void SingleMemberLongAcceptDefParam(@SingleMemberLongWithDef int x) {}
    public void SingleMemberCharAcceptDefParam(@SingleMemberCharWithDef int x) {}
    public void SingleMemberFloatAcceptDefParam(@SingleMemberFloatWithDef int x) {}
    public void SingleMemberDoubleAcceptDefParam(@SingleMemberDoubleWithDef int x) {}
    public void SingleMemberBooleanAcceptDefParam(@SingleMemberBooleanWithDef int x){}
    public void SingleMemberStringAcceptDefParam(@SingleMemberStringWithDef int x) {}
    public void SingleMemberClassAcceptDefParam(@SingleMemberClassWithDef int x) {}
    public void SingleMemberEnumAcceptDefParam(@SingleMemberEnumWithDef int x) {}

    // Single member array (empty array)
    public void SingleMemberByteArrEmptyParam(@SingleMemberByteArray({}) int x) {}
    public void SingleMemberShortArrEmptyParam(@SingleMemberShortArray({}) int x) {}
    public void SingleMemberIntArrEmptyParam(@SingleMemberIntArray({}) int x) {}
    public void SingleMemberLongArrEmptyParam(@SingleMemberLongArray({}) int x) {}
    public void SingleMemberCharArrEmptyParam(@SingleMemberCharArray({}) int x) {}
    public void SingleMemberFloatArrEmptyParam(@SingleMemberFloatArray({}) int x) {}
    public void SingleMemberDoubleArrEmptyParam(@SingleMemberDoubleArray({}) int x) {}
    public void SingleMemberBooleanArrEmptyParam(@SingleMemberBooleanArray({}) int x) {}
    public void SingleMemberStringArrEmptyParam(@SingleMemberStringArray({}) int x) {}
    public void SingleMemberClassArrEmptyParam(@SingleMemberClassArray({}) int x) {}
    public void SingleMemberEnumArrEmptyParam(@SingleMemberEnumArray({}) int x) {}

    // Single member array (one-element shorthand)
    public void SingleMemberByteArrOneParam(@SingleMemberByteArray(1) int x) {}
    public void SingleMemberShortArrOneParam(@SingleMemberShortArray(2) int x) {}
    public void SingleMemberIntArrOneParam(@SingleMemberIntArray(3) int x) {}
    public void SingleMemberLongArrOneParam(@SingleMemberLongArray(4L) int x) {}
    public void SingleMemberCharArrOneParam(@SingleMemberCharArray('5') int x) {}
    public void SingleMemberFloatArrOneParam(@SingleMemberFloatArray(6.0f) int x) {}
    public void SingleMemberDoubleArrOneParam(@SingleMemberDoubleArray(7.0) int x) {}
    public void SingleMemberBooleanArrOneParam(@SingleMemberBooleanArray(true) int x) {}
    public void SingleMemberStringArrOneParam(@SingleMemberStringArray("custom") int x) {}
    public void SingleMemberClassArrOneParam(@SingleMemberClassArray(Map.class) int x) {}
    public void SingleMemberEnumArrOneParam(@SingleMemberEnumArray(Stooge.MOE) int x) {}

    // Single member array (two elements)
    public void SingleMemberByteArrTwoParam(@SingleMemberByteArray({1, 2}) int x) {}
    public void SingleMemberShortArrTwoParam(@SingleMemberShortArray({2, 3}) int x) {}
    public void SingleMemberIntArrTwoParam(@SingleMemberIntArray({3, 4}) int x) {}
    public void SingleMemberLongArrTwoParam(@SingleMemberLongArray({4L, 5L}) int x) {}
    public void SingleMemberCharArrTwoParam(@SingleMemberCharArray({'5', '6'}) int x) {}
    public void SingleMemberFloatArrTwoParam(@SingleMemberFloatArray({6.0f, 7.0f}) int x) {}
    public void SingleMemberDoubleArrTwoParam(@SingleMemberDoubleArray({7.0, 8.0}) int x) {}
    public void SingleMemberBooleanArrTwoParam(@SingleMemberBooleanArray({true, false}) int x){}
    public void SingleMemberStringArrTwoParam(@SingleMemberStringArray({"custom", "paint"}) int x) {}
    public void SingleMemberClassArrTwoParam(@SingleMemberClassArray({Map.class, Set.class}) int x) {}
    public void SingleMemberEnumArrTwoParam(@SingleMemberEnumArray({Stooge.MOE, Stooge.CURLY}) int x) {}

    // Single member array with default (override)
    public void SingleMemberByteArrOvrdDefParam(@SingleMemberByteArrayDef(1) int x) {}
    public void SingleMemberShortArrOvrdDefParam(@SingleMemberShortArrayDef(2) int x) {}
    public void SingleMemberIntArrOvrdDefParam(@SingleMemberIntArrayDef(3) int x) {}
    public void SingleMemberLongArrOvrdDefParam(@SingleMemberLongArrayDef(4L) int x) {}
    public void SingleMemberCharArrOvrdDefParam(@SingleMemberCharArrayDef('5') int x) {}
    public void SingleMemberFloatArrOvrdDefParam(@SingleMemberFloatArrayDef(6.0f) int x) {}
    public void SingleMemberDoubleArrOvrdDefParam(@SingleMemberDoubleArrayDef(7.0) int x) {}
    public void SingleMemberBooleanArrOvrdDefParam(@SingleMemberBooleanArrayDef(true) int x){}
    public void SingleMemberStringArrOvrdDefParam(@SingleMemberStringArrayDef("custom") int x) {}
    public void SingleMemberClassArrOvrdDefParam(@SingleMemberClassArrayDef(Map.class) int x) {}
    public void SingleMemberEnumArrOvrdDefParam(@SingleMemberEnumArrayDef(Stooge.MOE) int x) {}

    // Single member array with default - accept
    public void SingleMemberByteArrAcceptDefParam(@SingleMemberByteArrayDef int x) {}
    public void SingleMemberShortArrAcceptDefParam(@SingleMemberShortArrayDef int x) {}
    public void SingleMemberIntArrAcceptDefParam(@SingleMemberIntArrayDef int x) {}
    public void SingleMemberLongArrAcceptDefParam(@SingleMemberLongArrayDef int x) {}
    public void SingleMemberCharArrAcceptDefParam(@SingleMemberCharArrayDef int x) {}
    public void SingleMemberFloatArrAcceptDefParam(@SingleMemberFloatArrayDef int x) {}
    public void SingleMemberDoubleArrAcceptDefParam(@SingleMemberDoubleArrayDef int x) {}
    public void SingleMemberBooleanArrAcceptDefParam(@SingleMemberBooleanArrayDef int x){}
    public void SingleMemberStringArrAcceptDefParam(@SingleMemberStringArrayDef int x) {}
    public void SingleMemberClassArrAcceptDefParam(@SingleMemberClassArrayDef int x) {}
    public void SingleMemberEnumArrAcceptDefParam(@SingleMemberEnumArrayDef int x) {}
}

// Helper types

enum Stooge { LARRY, MOE, CURLY }

@Target({}) @interface Point { int x(); int y(); }

// ANNOTATION TYPES

@Retention(RUNTIME) @interface ScalarTypes {
    byte     b();
    short    s();
    int      i();
    long     l();
    char     c();
    float    f();
    double   d();
    boolean  bool();
    String   str();
    Class    cls();
    Stooge   e();
    Point    a();
}

@Retention(RUNTIME) @interface ScalarTypesWithDefault {
    byte     b()    default 11;
    short    s()    default 12;
    int      i()    default 13;
    long     l()    default 14;
    char     c()    default 'V';
    float    f()    default 16.0f;
    double   d()    default 17.0;
    boolean  bool() default false;
    String   str()  default "default";
    Class    cls()  default Class.class;
    Stooge   e()    default Stooge.LARRY;
    Point    a()    default @Point(x = 11, y = 12);
}

@Retention(RUNTIME) @interface ArrayTypes {
    byte[]     b();
    short[]    s();
    int[]      i();
    long[]     l();
    char[]     c();
    float[]    f();
    double[]   d();
    boolean[]  bool();
    String[]   str();
    Class[]    cls();
    Stooge[]   e();
    Point[]    a();
}

@Retention(RUNTIME) @interface ArrayTypesWithDefault {
    byte[]    b()    default { 11 };
    short[]   s()    default { 12 };
    int[]     i()    default { 13 };
    long[]    l()    default { 14L };
    char[]    c()    default { 'V' };
    float[]   f()    default { 16.0f };
    double[]  d()    default { 17.0 };
    boolean[] bool() default { false };
    String[]  str()  default { "default" };
    Class[]   cls()  default { Class.class };
    Stooge[]  e()    default { Stooge.LARRY };
    Point[]   a()    default { @Point(x = 11, y = 12) };
}

@Retention(RUNTIME) @interface Marker { }

@Retention(RUNTIME) @interface SingleMemberByte    { byte     value(); }
@Retention(RUNTIME) @interface SingleMemberShort   { short    value(); }
@Retention(RUNTIME) @interface SingleMemberInt     { int      value(); }
@Retention(RUNTIME) @interface SingleMemberLong    { long     value(); }
@Retention(RUNTIME) @interface SingleMemberChar    { char     value(); }
@Retention(RUNTIME) @interface SingleMemberFloat   { float    value(); }
@Retention(RUNTIME) @interface SingleMemberDouble  { double   value(); }
@Retention(RUNTIME) @interface SingleMemberBoolean { boolean  value(); }
@Retention(RUNTIME) @interface SingleMemberString  { String   value(); }
@Retention(RUNTIME) @interface SingleMemberClass   { Class    value(); }
@Retention(RUNTIME) @interface SingleMemberEnum    { Stooge   value(); }

@Retention(RUNTIME) @interface SingleMemberByteWithDef    { byte     value() default 11; }
@Retention(RUNTIME) @interface SingleMemberShortWithDef   { short    value() default 12; }
@Retention(RUNTIME) @interface SingleMemberIntWithDef     { int      value() default 13; }
@Retention(RUNTIME) @interface SingleMemberLongWithDef    { long     value() default 14; }
@Retention(RUNTIME) @interface SingleMemberCharWithDef    { char     value() default 'V'; }
@Retention(RUNTIME) @interface SingleMemberFloatWithDef   { float    value() default 16.0f; }
@Retention(RUNTIME) @interface SingleMemberDoubleWithDef  { double   value() default 17.0; }
@Retention(RUNTIME) @interface SingleMemberBooleanWithDef { boolean  value() default false; }
@Retention(RUNTIME) @interface SingleMemberStringWithDef  { String   value() default "default"; }
@Retention(RUNTIME) @interface SingleMemberClassWithDef   { Class    value() default Class.class; }
@Retention(RUNTIME) @interface SingleMemberEnumWithDef    { Stooge   value() default Stooge.LARRY; }

@Retention(RUNTIME) @interface SingleMemberByteArray    { byte[]     value(); }
@Retention(RUNTIME) @interface SingleMemberShortArray   { short[]    value(); }
@Retention(RUNTIME) @interface SingleMemberIntArray     { int[]      value(); }
@Retention(RUNTIME) @interface SingleMemberLongArray    { long[]     value(); }
@Retention(RUNTIME) @interface SingleMemberCharArray    { char[]     value(); }
@Retention(RUNTIME) @interface SingleMemberFloatArray   { float[]    value(); }
@Retention(RUNTIME) @interface SingleMemberDoubleArray  { double[]   value(); }
@Retention(RUNTIME) @interface SingleMemberBooleanArray { boolean[]  value(); }
@Retention(RUNTIME) @interface SingleMemberStringArray  { String[]   value(); }
@Retention(RUNTIME) @interface SingleMemberClassArray   { Class[]    value(); }
@Retention(RUNTIME) @interface SingleMemberEnumArray    { Stooge[]   value(); }

@Retention(RUNTIME) @interface SingleMemberByteArrayDef    { byte[]     value() default { 11 }; }
@Retention(RUNTIME) @interface SingleMemberShortArrayDef   { short[]    value() default { 12 }; }
@Retention(RUNTIME) @interface SingleMemberIntArrayDef     { int[]      value() default { 13 }; }
@Retention(RUNTIME) @interface SingleMemberLongArrayDef    { long[]     value() default { 14 }; }
@Retention(RUNTIME) @interface SingleMemberCharArrayDef    { char[]     value() default { 'V' }; }
@Retention(RUNTIME) @interface SingleMemberFloatArrayDef   { float[]    value() default { 16.0f };}
@Retention(RUNTIME) @interface SingleMemberDoubleArrayDef  { double[]   value() default { 17.0 }; }
@Retention(RUNTIME) @interface SingleMemberBooleanArrayDef { boolean[]  value() default { false };}
@Retention(RUNTIME) @interface SingleMemberStringArrayDef  {
    String[]  value() default {"default"};
}
@Retention(RUNTIME) @interface SingleMemberClassArrayDef {
    Class[]   value() default {Class.class};
}
@Retention(RUNTIME) @interface SingleMemberEnumArrayDef {
    Stooge[]   value() default {Stooge.LARRY};
}

// Annotation types for inheritance and declared-annotations tests
@Inherited @Retention(RUNTIME) @interface Foo { }
           @Retention(RUNTIME) @interface Bar { }


    // ANNOTATED CLASSES

    @ScalarTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    class scalarTypesClass { }

    @ScalarTypesWithDefault ( )
    class scalarTypesAcceptDefaultClass { }

    @ScalarTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE
    )
    class scalarTypesOverrideDefaultClass { }

    @ArrayTypes (
        b =    { },
        s =    { },
        i =    { },
        l =    { },
        c =    { },
        f =    { },
        d =    { },
        bool = { },
        str =  { },
        cls =  { },
        e =    { },
        a =    { }
    )
    class emptyArrayTypesClass { }

    @ArrayTypes (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    @Point(x = 1, y = 2)
    )
    class singleElementArrayTypesClass { }

    @ArrayTypes (
        b =    { 1, 2 },
        s =    { 2, 3 },
        i =    { 3, 4 },
        l =    { 4L, 5L },
        c =    { '5', '6' },
        f =    { 6.0f, 7.0f },
        d =    { 7.0, 8.0 },
        bool = { true, false },
        str =  { "custom", "paint" },
        cls =  { Map.class, Set.class },
        e =    { Stooge.MOE, Stooge.CURLY },
        a =    { @Point(x = 1, y = 2),  @Point(x = 3, y = 4) }
    )
    class twoElementArrayTypesClass { }

    @ArrayTypesWithDefault (
    )
    class arrayTypesAcceptDefaultClass { }

    @ArrayTypesWithDefault (
        b =    1,
        s =    2,
        i =    3,
        l =    4L,
        c =    '5',
        f =    6.0f,
        d =    7.0,
        bool = true,
        str =  "custom",
        cls =  Map.class,
        e =    Stooge.MOE,
        a =    { @Point(x = 1, y = 2) }
    )
    class arrayTypesOverrideDefaultClass { }

    @Marker class markerClass { }

    // Single-member (shorthand)
    @SingleMemberByte(1)          class SingleMemberByteClass { }
    @SingleMemberShort(2)         class SingleMemberShortClass { }
    @SingleMemberInt(3)           class SingleMemberIntClass { }
    @SingleMemberLong(4L)         class SingleMemberLongClass { }
    @SingleMemberChar('5')        class SingleMemberCharClass { }
    @SingleMemberFloat(6.0f)      class SingleMemberFloatClass { }
    @SingleMemberDouble(7.0)      class SingleMemberDoubleClass { }
    @SingleMemberBoolean(true)    class SingleMemberBooleanClass { }
    @SingleMemberString("custom") class SingleMemberStringClass { }
    @SingleMemberClass(Map.class) class SingleMemberClassClass { }
    @SingleMemberEnum(Stooge.MOE)        class SingleMemberEnumClass { }

    // Single-member with default (Override)
    @SingleMemberByteWithDef(1)          class SingleMemberByteOvrdDefClass { }
    @SingleMemberShortWithDef(2)         class SingleMemberShortOvrdDefClass { }
    @SingleMemberIntWithDef(3)           class SingleMemberIntOvrdDefClass { }
    @SingleMemberLongWithDef(4L)         class SingleMemberLongOvrdDefClass { }
    @SingleMemberCharWithDef('5')        class SingleMemberCharOvrdDefClass { }
    @SingleMemberFloatWithDef(6.0f)      class SingleMemberFloatOvrdDefClass { }
    @SingleMemberDoubleWithDef(7.0)      class SingleMemberDoubleOvrdDefClass { }
    @SingleMemberBooleanWithDef(true)    class SingleMemberBooleanOvrdDefClass { }
    @SingleMemberStringWithDef("custom") class SingleMemberStringOvrdDefClass { }
    @SingleMemberClassWithDef(Map.class) class SingleMemberClassOvrdDefClass { }
    @SingleMemberEnumWithDef(Stooge.MOE)        class SingleMemberEnumOvrdDefClass { }

    // Single-member with default (Accept)
    @SingleMemberByteWithDef    class SingleMemberByteAcceptDefClass { }
    @SingleMemberShortWithDef   class SingleMemberShortAcceptDefClass { }
    @SingleMemberIntWithDef     class SingleMemberIntAcceptDefClass { }
    @SingleMemberLongWithDef    class SingleMemberLongAcceptDefClass { }
    @SingleMemberCharWithDef    class SingleMemberCharAcceptDefClass { }
    @SingleMemberFloatWithDef   class SingleMemberFloatAcceptDefClass { }
    @SingleMemberDoubleWithDef  class SingleMemberDoubleAcceptDefClass { }
    @SingleMemberBooleanWithDef class SingleMemberBooleanAcceptDefClass { }
    @SingleMemberStringWithDef  class SingleMemberStringAcceptDefClass { }
    @SingleMemberClassWithDef   class SingleMemberClassAcceptDefClass { }
    @SingleMemberEnumWithDef    class SingleMemberEnumAcceptDefClass { }

    // Single member array (empty array)
    @SingleMemberByteArray({})   class SingleMemberByteArrEmptyClass { }
    @SingleMemberShortArray({})  class SingleMemberShortArrEmptyClass { }
    @SingleMemberIntArray({})    class SingleMemberIntArrEmptyClass { }
    @SingleMemberLongArray({})   class SingleMemberLongArrEmptyClass { }
    @SingleMemberCharArray({})   class SingleMemberCharArrEmptyClass { }
    @SingleMemberFloatArray({})  class SingleMemberFloatArrEmptyClass { }
    @SingleMemberDoubleArray({}) class SingleMemberDoubleArrEmptyClass { }
    @SingleMemberBooleanArray({})class SingleMemberBooleanArrEmptyClass { }
    @SingleMemberStringArray({}) class SingleMemberStringArrEmptyClass { }
    @SingleMemberClassArray({})  class SingleMemberClassArrEmptyClass { }
    @SingleMemberEnumArray({})   class SingleMemberEnumArrEmptyClass { }

    // Single member array (one-element shorthand)
    @SingleMemberByteArray(1)          class SingleMemberByteArrOneClass { }
    @SingleMemberShortArray(2)         class SingleMemberShortArrOneClass { }
    @SingleMemberIntArray(3)           class SingleMemberIntArrOneClass { }
    @SingleMemberLongArray(4L)         class SingleMemberLongArrOneClass { }
    @SingleMemberCharArray('5')        class SingleMemberCharArrOneClass { }
    @SingleMemberFloatArray(6.0f)      class SingleMemberFloatArrOneClass { }
    @SingleMemberDoubleArray(7.0)      class SingleMemberDoubleArrOneClass { }
    @SingleMemberBooleanArray(true)    class SingleMemberBooleanArrOneClass { }
    @SingleMemberStringArray("custom") class SingleMemberStringArrOneClass { }
    @SingleMemberClassArray(Map.class) class SingleMemberClassArrOneClass { }
    @SingleMemberEnumArray(Stooge.MOE)        class SingleMemberEnumArrOneClass { }

    // Single member array (two elements)
    @SingleMemberByteArray({1, 2})         class SingleMemberByteArrTwoClass { }
    @SingleMemberShortArray({2, 3})        class SingleMemberShortArrTwoClass { }
    @SingleMemberIntArray({3, 4})          class SingleMemberIntArrTwoClass { }
    @SingleMemberLongArray({4L, 5L})       class SingleMemberLongArrTwoClass { }
    @SingleMemberCharArray({'5', '6'})     class SingleMemberCharArrTwoClass { }
    @SingleMemberFloatArray({6.0f, 7.0f})  class SingleMemberFloatArrTwoClass { }
    @SingleMemberDoubleArray({7.0, 8.0})   class SingleMemberDoubleArrTwoClass { }
    @SingleMemberBooleanArray({true,false})         class SingleMemberBooleanArrTwoClass { }
    @SingleMemberStringArray({"custom", "paint"})   class SingleMemberStringArrTwoClass { }
    @SingleMemberClassArray({Map.class, Set.class}) class SingleMemberClassArrTwoClass { }
    @SingleMemberEnumArray({Stooge.MOE, Stooge.CURLY})            class SingleMemberEnumArrTwoClass { }

    // Single member array with default (override)
    @SingleMemberByteArrayDef(1)        class SingleMemberByteArrOvrdDefClass { }
    @SingleMemberShortArrayDef(2)       class SingleMemberShortArrOvrdDefClass { }
    @SingleMemberIntArrayDef(3)         class SingleMemberIntArrOvrdDefClass { }
    @SingleMemberLongArrayDef(4L)       class SingleMemberLongArrOvrdDefClass { }
    @SingleMemberCharArrayDef('5')      class SingleMemberCharArrOvrdDefClass { }
    @SingleMemberFloatArrayDef(6.0f)    class SingleMemberFloatArrOvrdDefClass { }
    @SingleMemberDoubleArrayDef(7.0)    class SingleMemberDoubleArrOvrdDefClass { }
    @SingleMemberBooleanArrayDef(true)  class SingleMemberBooleanArrOvrdDefClass { }
    @SingleMemberStringArrayDef("custom") class SingleMemberStringArrOvrdDefClass { }
    @SingleMemberClassArrayDef(Map.class) class SingleMemberClassArrOvrdDefClass { }
    @SingleMemberEnumArrayDef(Stooge.MOE)        class SingleMemberEnumArrOvrdDefClass { }

    // Single member array with default - accept
    @SingleMemberByteArrayDef    class SingleMemberByteArrAcceptDefClass { }
    @SingleMemberShortArrayDef   class SingleMemberShortArrAcceptDefClass { }
    @SingleMemberIntArrayDef     class SingleMemberIntArrAcceptDefClass { }
    @SingleMemberLongArrayDef    class SingleMemberLongArrAcceptDefClass { }
    @SingleMemberCharArrayDef    class SingleMemberCharArrAcceptDefClass { }
    @SingleMemberFloatArrayDef   class SingleMemberFloatArrAcceptDefClass { }
    @SingleMemberDoubleArrayDef  class SingleMemberDoubleArrAcceptDefClass { }
    @SingleMemberBooleanArrayDef class SingleMemberBooleanArrAcceptDefClass { }
    @SingleMemberStringArrayDef  class SingleMemberStringArrAcceptDefClass { }
    @SingleMemberClassArrayDef   class SingleMemberClassArrAcceptDefClass { }
    @SingleMemberEnumArrayDef    class SingleMemberEnumArrAcceptDefClass { }

    // Annotated classes for inheritance and declared-annotations tests
    @Foo @Bar class Grandpa     { }
    class Dad extends Grandpa   { }
    @Bar class Son extends Dad  { }
