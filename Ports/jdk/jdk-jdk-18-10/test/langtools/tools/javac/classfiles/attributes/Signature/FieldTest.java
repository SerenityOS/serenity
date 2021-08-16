/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049238
 * @summary Checks Signature attribute for fields.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build FieldTest Driver ExpectedSignature ExpectedSignatureContainer
 * @run main Driver FieldTest
 */

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@ExpectedSignature(descriptor = "FieldTest", signature = "<T:Ljava/lang/Object;>Ljava/lang/Object;")
public class FieldTest<T> {

    @ExpectedSignature(descriptor = "typeInList", signature = "Ljava/util/List<TT;>;")
    List<T> typeInList;

    @ExpectedSignature(descriptor = "boundsType", signature = "Ljava/util/List<Ljava/util/Map<+TT;-TT;>;>;")
    List<Map<? extends T, ? super T>> boundsType;

    @ExpectedSignature(descriptor = "type", signature = "TT;")
    T type;

    @ExpectedSignature(descriptor = "typeInArray", signature = "[TT;")
    T[] typeInArray;

    @ExpectedSignature(descriptor = "byteArrayInList", signature = "Ljava/util/List<[B>;")
    List<byte[]> byteArrayInList;

    @ExpectedSignature(descriptor = "shortArrayInList", signature = "Ljava/util/List<[S>;")
    List<short[]> shortArrayInList;

    @ExpectedSignature(descriptor = "intArrayInList", signature = "Ljava/util/List<[I>;")
    List<int[]> intArrayInList;

    @ExpectedSignature(descriptor = "longArrayInList", signature = "Ljava/util/List<[J>;")
    List<long[]> longArrayInList;

    @ExpectedSignature(descriptor = "charArrayInList", signature = "Ljava/util/List<[C>;")
    List<char[]> charArrayInList;

    @ExpectedSignature(descriptor = "booleanArrayInList", signature = "Ljava/util/List<[Z>;")
    List<boolean[]> booleanArrayInList;

    @ExpectedSignature(descriptor = "floatArrayInList", signature = "Ljava/util/List<[F>;")
    List<float[]> floatArrayInList;

    @ExpectedSignature(descriptor = "doubleArrayInList", signature = "Ljava/util/List<[D>;")
    List<double[]> doubleArrayInList;

    @ExpectedSignature(descriptor = "integerInList", signature = "Ljava/util/List<Ljava/lang/Integer;>;")
    List<Integer> integerInList;

    @ExpectedSignature(descriptor = "typeInMultiArray", signature = "[[TT;")
    T[][] typeInMultiArray;

    @ExpectedSignature(descriptor = "arrayOfClasses", signature = "[Ljava/util/List<TT;>;")
    List<T>[] arrayOfClasses;

    @ExpectedSignature(descriptor = "extendsWildCard", signature = "Ljava/util/List<+TT;>;")
    List<? extends T> extendsWildCard;

    @ExpectedSignature(descriptor = "superWildCard", signature = "Ljava/util/Comparator<-TT;>;")
    Comparator<? super T> superWildCard;

    @ExpectedSignature(descriptor = "extendsSuperWildCard",
            signature = "Ljava/util/List<+Ljava/util/Comparator<-TT;>;>;")
    List<? extends Comparator<? super T>> extendsSuperWildCard;

    @ExpectedSignature(descriptor = "wildCard", signature = "Ljava/util/Comparator<*>;")
    Comparator<?> wildCard;

    @ExpectedSignature(descriptor = "boundsBooleanArray", signature = "Ljava/util/Map<+[Z-[Z>;")
    Map<? extends boolean[], ? super boolean[]> boundsBooleanArray;

    @ExpectedSignature(descriptor = "boundsByteArray", signature = "Ljava/util/Map<+[B-[B>;")
    Map<? extends byte[], ? super byte[]> boundsByteArray;

    @ExpectedSignature(descriptor = "boundsShortArray", signature = "Ljava/util/Map<+[S-[S>;")
    Map<? extends short[], ? super short[]> boundsShortArray;

    @ExpectedSignature(descriptor = "boundsIntArray", signature = "Ljava/util/Map<+[I-[I>;")
    Map<? extends int[], ? super int[]> boundsIntArray;

    @ExpectedSignature(descriptor = "boundsLongArray", signature = "Ljava/util/Map<+[J-[J>;")
    Map<? extends long[], ? super long[]> boundsLongArray;

    @ExpectedSignature(descriptor = "boundsCharArray", signature = "Ljava/util/Map<+[C-[C>;")
    Map<? extends char[], ? super char[]> boundsCharArray;

    @ExpectedSignature(descriptor = "boundsFloatArray", signature = "Ljava/util/Map<+[F-[F>;")
    Map<? extends float[], ? super float[]> boundsFloatArray;

    @ExpectedSignature(descriptor = "boundsDoubleArray", signature = "Ljava/util/Map<+[D-[D>;")
    Map<? extends double[], ? super double[]> boundsDoubleArray;

    @ExpectedSignature(descriptor = "boundsObjectArray",
            signature = "Ljava/util/Map<+[Ljava/lang/Object;-[Ljava/lang/Object;>;")
    Map<? extends Object[], ? super Object[]> boundsObjectArray;

    boolean booleanNoSignatureAttribute;
    byte byteNoSignatureAttribute;
    char charNoSignatureAttribute;
    short shortNoSignatureAttribute;
    int intNoSignatureAttribute;
    long longNoSignatureAttribute;
    float floatNoSignatureAttribute;
    double doubleNoSignatureAttribute;

    List listNoSignatureAttribute;

    @ExpectedSignature(descriptor = "staticByteArrayInList", signature = "Ljava/util/List<[B>;")
    static List<byte[]> staticByteArrayInList;

    @ExpectedSignature(descriptor = "staticShortArrayInList", signature = "Ljava/util/List<[S>;")
    static List<short[]> staticShortArrayInList;

    @ExpectedSignature(descriptor = "staticIntArrayInList", signature = "Ljava/util/List<[I>;")
    static List<int[]> staticIntArrayInList;

    @ExpectedSignature(descriptor = "staticLongArrayInList", signature = "Ljava/util/List<[J>;")
    static List<long[]> staticLongArrayInList;

    @ExpectedSignature(descriptor = "staticCharArrayInList", signature = "Ljava/util/List<[C>;")
    static List<char[]> staticCharArrayInList;

    @ExpectedSignature(descriptor = "staticBooleanArrayInList", signature = "Ljava/util/List<[Z>;")
    static List<boolean[]> staticBooleanArrayInList;

    @ExpectedSignature(descriptor = "staticFloatArrayInList", signature = "Ljava/util/List<[F>;")
    static List<float[]> staticFloatArrayInList;

    @ExpectedSignature(descriptor = "staticDoubleArrayInList", signature = "Ljava/util/List<[D>;")
    static List<double[]> staticDoubleArrayInList;

    @ExpectedSignature(descriptor = "staticIntegerInList", signature = "Ljava/util/List<Ljava/lang/Integer;>;")
    static List<Integer> staticIntegerInList;

    @ExpectedSignature(descriptor = "staticWildCard", signature = "Ljava/util/Comparator<*>;")
    static Comparator<?> staticWildCard;

    @ExpectedSignature(descriptor = "staticBoundsBooleanArray", signature = "Ljava/util/Map<+[Z-[Z>;")
    static Map<? extends boolean[], ? super boolean[]> staticBoundsBooleanArray;

    @ExpectedSignature(descriptor = "staticBoundsByteArray", signature = "Ljava/util/Map<+[B-[B>;")
    static Map<? extends byte[], ? super byte[]> staticBoundsByteArray;

    @ExpectedSignature(descriptor = "staticBoundsShortArray", signature = "Ljava/util/Map<+[S-[S>;")
    static Map<? extends short[], ? super short[]> staticBoundsShortArray;

    @ExpectedSignature(descriptor = "staticBoundsIntArray", signature = "Ljava/util/Map<+[I-[I>;")
    static Map<? extends int[], ? super int[]> staticBoundsIntArray;

    @ExpectedSignature(descriptor = "staticBoundsLongArray", signature = "Ljava/util/Map<+[J-[J>;")
    static Map<? extends long[], ? super long[]> staticBoundsLongArray;

    @ExpectedSignature(descriptor = "staticBoundsCharArray", signature = "Ljava/util/Map<+[C-[C>;")
    static Map<? extends char[], ? super char[]> staticBoundsCharArray;

    @ExpectedSignature(descriptor = "staticBoundsFloatArray", signature = "Ljava/util/Map<+[F-[F>;")
    static Map<? extends float[], ? super float[]> staticBoundsFloatArray;

    @ExpectedSignature(descriptor = "staticBoundsDoubleArray", signature = "Ljava/util/Map<+[D-[D>;")
    static Map<? extends double[], ? super double[]> staticBoundsDoubleArray;

    @ExpectedSignature(descriptor = "staticBoundsObjectArray",
            signature = "Ljava/util/Map<+[Ljava/lang/Object;-[Ljava/lang/Object;>;")
    static Map<? extends Object[], ? super Object[]> staticBoundsObjectArray;

    static boolean staticBooleanNoSignatureAttribute;
    static byte staticByteNoSignatureAttribute;
    static char staticCharNoSignatureAttribute;
    static short staticShortNoSignatureAttribute;
    static int staticIntNoSignatureAttribute;
    static long staticLongNoSignatureAttribute;
    static float staticFloatNoSignatureAttribute;
    static double staticDoubleNoSignatureAttribute;

    static List staticListNoSignatureAttribute;
}
