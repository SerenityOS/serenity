/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7006109
 * @summary Add test library to simplify the task of writing automated type-system tests
 * @author mcimadamore
 * @library /tools/lib/types
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 * @build TypeHarness
 * @run main BoxingConversionTest
 */

import com.sun.tools.javac.code.Type;
import java.lang.reflect.Array;
import java.util.EnumSet;

/**
 * Check invariants in assignment/method conversion involving boxing conversions
 */
public class BoxingConversionTest extends TypeHarness {

    Type[] types1;
    Type[] types2;
    Type[] types3;

    enum Result {
        OK_BOTH(true),
        FAIL_BOTH(false),
        OK_ASSIGN_ONLY(true);

        boolean value;

        Result(boolean value) {
            this.value = value;
        }
    }

    enum ConversionKind {
        ASSIGNMENT_CONVERSION(EnumSet.of(Result.OK_BOTH, Result.OK_ASSIGN_ONLY)) {
            @Override
            void check(TypeHarness harness, Type from, Type to, Result expected) {
                harness.assertAssignable(from, to, resSet.contains(expected));
            }
        },
        METHOD_CONVERSION(EnumSet.of(Result.OK_BOTH)) {
            @Override
            void check(TypeHarness harness, Type from, Type to, Result expected) {
                harness.assertConvertible(from, to, resSet.contains(expected));
            }
        };

        EnumSet<Result> resSet;

        private ConversionKind(EnumSet<Result> resSet) {
            this.resSet = resSet;
        }

        abstract void check(TypeHarness harness, Type from, Type to, Result expected);
    }

    enum TestKind {
        SIMPLE {
            @Override
            Type[] getFromTypes(BoxingConversionTest harness) {
                return harness.types1;
            }
            @Override
            Type[] getToTypes(BoxingConversionTest harness) {
                return harness.types1;
            }
            @Override
            Result[][] getResults(BoxingConversionTest harness) {
                return harness.results1;
            }
        },
        CONSTANT_TYPES {
            @Override
            Type[] getFromTypes(BoxingConversionTest harness) {
                return harness.types2;
            }
            @Override
            Type[] getToTypes(BoxingConversionTest harness) {
                return harness.types3;
            }
            @Override
            Result[][] getResults(BoxingConversionTest harness) {
                return harness.results2;
            }
        };

        abstract Type[] getFromTypes(BoxingConversionTest harness);
        abstract Type[] getToTypes(BoxingConversionTest harness);
        abstract Result[][] getResults(BoxingConversionTest harness);
    }

    static final Result T = Result.OK_BOTH;
    static final Result F = Result.FAIL_BOTH;
    static final Result A = Result.OK_ASSIGN_ONLY;

    Result[][] results1 = {
                   //byte, short, int, long, float, double, char, bool, Byte, Short, Integer, Long, Float, Double, Character, Boolean
    /*byte*/       { T   , T    , T  , T   , T    , T     , F   , F   , T   , F    , F      , F   , F    , F     , F        , F },
    /*short*/      { F   , T    , T  , T   , T    , T     , F   , F   , F   , T    , F      , F   , F    , F     , F        , F },
    /*int*/        { F   , F    , T  , T   , T    , T     , F   , F   , F   , F    , T      , F   , F    , F     , F        , F },
    /*long*/       { F   , F    , F  , T   , T    , T     , F   , F   , F   , F    , F      , T   , F    , F     , F        , F },
    /*float*/      { F   , F    , F  , F   , T    , T     , F   , F   , F   , F    , F      , F   , T    , F     , F        , F },
    /*double*/     { F   , F    , F  , F   , F    , T     , F   , F   , F   , F    , F      , F   , F    , T     , F        , F },
    /*char*/       { F   , F    , T  , T   , T    , T     , T   , F   , F   , F    , F      , F   , F    , F     , T        , F },
    /*bool*/       { F   , F    , F  , F   , F    , F     , F   , T   , F   , F    , F      , F   , F    , F     , F        , T },
    /*Byte*/       { T   , T    , T  , T   , T    , T     , F   , F   , T   , F    , F      , F   , F    , F     , F        , F },
    /*Short*/      { F   , T    , T  , T   , T    , T     , F   , F   , F   , T    , F      , F   , F    , F     , F        , F },
    /*Integer*/    { F   , F    , T  , T   , T    , T     , F   , F   , F   , F    , T      , F   , F    , F     , F        , F },
    /*Long*/       { F   , F    , F  , T   , T    , T     , F   , F   , F   , F    , F      , T   , F    , F     , F        , F },
    /*Float*/      { F   , F    , F  , F   , T    , T     , F   , F   , F   , F    , F      , F   , T    , F     , F        , F },
    /*Double*/     { F   , F    , F  , F   , F    , T     , F   , F   , F   , F    , F      , F   , F    , T     , F        , F },
    /*Character*/  { F   , F    , T  , T   , T    , T     , T   , F   , F   , F    , F      , F   , F    , F     , T        , F },
    /*Boolean*/    { F   , F    , F  , F   , F    , F     , F   , T   , F   , F    , F      , F   , F    , F     , F        , T }};

    Result[][] results2 = {
                //Byte, Short, Integer, Long, Float, Double, Chararacter, Boolean
    /*byte*/    { T   , F    , F      , F   , F    , F     , F          , F },
    /*short*/   { F   , T    , F      , F   , F    , F     , F          , F },
    /*short1*/  { A   , T    , F      , F   , F    , F     , A          , F },
    /*short2*/  { F   , T    , F      , F   , F    , F     , A          , F },
    /*int*/     { F   , F    , T      , F   , F    , F     , F          , F },
    /*int1*/    { A   , A    , T      , F   , F    , F     , A          , F },
    /*int2*/    { F   , A    , T      , F   , F    , F     , A          , F },
    /*int4*/    { F   , F    , T      , F   , F    , F     , F          , F },
    /*long*/    { F   , F    , F      , T   , F    , F     , F          , F },
    /*long1*/   { F   , F    , F      , T   , F    , F     , F          , F },
    /*long2*/   { F   , F    , F      , T   , F    , F     , F          , F },
    /*long4*/   { F   , F    , F      , T   , F    , F     , F          , F },
    /*long8*/   { F   , F    , F      , T   , F    , F     , F          , F },
    /*float*/   { F   , F    , F      , F   , T    , F     , F          , F },
    /*float1*/  { F   , F    , F      , F   , T    , F     , F          , F },
    /*float2*/  { F   , F    , F      , F   , T    , F     , F          , F },
    /*float4*/  { F   , F    , F      , F   , T    , F     , F          , F },
    /*double*/  { F   , F    , F      , F   , F    , T     , F          , F },
    /*double1*/ { F   , F    , F      , F   , F    , T     , F          , F },
    /*double2*/ { F   , F    , F      , F   , F    , T     , F          , F },
    /*double4*/ { F   , F    , F      , F   , F    , T     , F          , F },
    /*double8*/ { F   , F    , F      , F   , F    , T     , F          , F },
    /*char*/    { F   , F    , F      , F   , F    , F     , T          , F },
    /*char1*/   { A   , A    , F      , F   , F    , F     , T          , F },
    /*char2*/   { F   , A    , F      , F   , F    , F     , T          , F },
    /*bool*/    { F   , F    , F      , F   , F    , F     , F          , T }};

    BoxingConversionTest() {
        Type[] primitiveTypes = new Type[] {
            predef.byteType,
            predef.shortType,
            predef.intType,
            predef.longType,
            predef.floatType,
            predef.doubleType,
            predef.charType,
            predef.booleanType };

        Type[] boxedTypes = new Type[primitiveTypes.length];
        for (int i = 0 ; i < primitiveTypes.length ; i++) {
            boxedTypes[i] = box(primitiveTypes[i]);
        }

        types1 = join(Type.class, primitiveTypes, boxedTypes);

        types2 = new Type[] {
            predef.byteType,
            predef.shortType,
            fac.Constant((short)0x0001),
            fac.Constant((short)0x0100),
            predef.intType,
            fac.Constant((int)0x0000_0001),
            fac.Constant((int)0x0000_0100),
            fac.Constant((int)0x0001_0000),
            predef.longType,
            fac.Constant((long)0x0000_0000_0000_0001L),
            fac.Constant((long)0x0000_0000_0000_0100L),
            fac.Constant((long)0x0000_0000_0001_0000L),
            fac.Constant((long)0x0001_0000_0000_0000L),
            predef.floatType,
            fac.Constant((float)0x0000_0001),
            fac.Constant((float)0x0000_0100),
            fac.Constant((float)0x0001_0000),
            predef.doubleType,
            fac.Constant((double)0x0000_0000_0000_0001L),
            fac.Constant((double)0x0000_0000_0000_0100L),
            fac.Constant((double)0x0000_0000_0001_0000L),
            fac.Constant((double)0x0001_0000_0000_0000L),
            predef.charType,
            fac.Constant((char)0x0001),
            fac.Constant((char)0x0100),
            predef.booleanType
        };

        types3 = boxedTypes;
    }

    void testConversion(ConversionKind convKind, TestKind testKind) {
        Type[] rows = testKind.getFromTypes(this);
        Type[] cols = testKind.getToTypes(this);
        for (int i = 0; i < rows.length ; i++) {
            for (int j = 0; j < cols.length ; j++) {
                convKind.check(this, rows[i], cols[j], testKind.getResults(this)[i][j]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> T[] join(Class<T> type, T[]... args) {
        int totalLength = 0;
        for (T[] arr : args) {
            totalLength += arr.length;
        }
        T[] new_arr = (T[])Array.newInstance(type, totalLength);
        int idx = 0;
        for (T[] arr : args) {
            System.arraycopy(arr, 0, new_arr, idx, arr.length);
            idx += arr.length;
        }
        return new_arr;
    }

    public static void main(String[] args) {
        BoxingConversionTest harness = new BoxingConversionTest();
        for (ConversionKind convKind : ConversionKind.values()) {
            for (TestKind testKind : TestKind.values()) {
                harness.testConversion(convKind, testKind);
            }
        }
    }
}
