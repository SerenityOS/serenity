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
 * @summary Checks Signature attribute for array return type of method.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules java.desktop
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build ReturnTypeTest Driver ExpectedSignature ExpectedSignatureContainer
 * @run main Driver ReturnTypeTest
 */

import java.awt.Frame;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@ExpectedSignature(descriptor = "ReturnTypeTest",
        signature = "<T:Ljava/awt/Frame;:Ljava/lang/Runnable;:Ljava/util/concurrent/Callable<[TT;>;>Ljava/lang/Object;")
public class ReturnTypeTest<T extends Frame & Runnable & Callable<T[]>> {

    @ExpectedSignature(descriptor = "byteArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[B")
    byte[] byteArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "shortArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[S")
    short[] shortArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "charArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[C")
    char[] charArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "intArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[I")
    int[] intArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "longArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[J")
    long[] longArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "booleanArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[Z")
    boolean[] booleanArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "floatArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[F")
    float[] floatArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "doubleArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[D")
    double[] doubleArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "objectArrayReturnType(java.awt.Frame)",
            signature = "(TT;)[Ljava/lang/Object;")
    Object[] objectArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticByteArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[B")
    static <T> byte[] staticByteArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticShortArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[S")
    static <T> short[] staticShortArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticCharArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[C")
    static <T> char[] staticCharArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticIntArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[I")
    static <T> int[] staticIntArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticLongArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[J")
    static <T> long[] staticLongArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticBooleanArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[Z")
    static <T> boolean[] staticBooleanArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticFloatArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[F")
    static <T> float[] staticFloatArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticDoubleArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[D")
    static <T> double[] staticDoubleArrayReturnType(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticObjectArrayReturnType(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)[Ljava/lang/Object;")
    static <T> Object[] staticObjectArrayReturnType(T a) {
        return null;
    }

    byte[] byteArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    short[] shortArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    char[] charArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    int[] intArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    long[] longArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    boolean[] booleanArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    float[] floatArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    double[] doubleArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    Object[] objectArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static byte[] staticByteArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static short[] staticShortArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static char[] staticCharArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static int[] staticIntArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static long[] staticLongArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static boolean[] staticBooleanArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static float[] staticFloatArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static double[] staticDoubleArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    static Object[] staticObjectArrayReturnTypeNoSignatureAttribute() {
        return null;
    }

    @ExpectedSignature(descriptor = "typeReturnType()",
            signature = "()TT;")
    T typeReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "typeArrayReturnType()",
            signature = "()[TT;")
    T[] typeArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "extendsReturnType()",
            signature = "<E:TT;>()TE;")
    <E extends T> E extendsReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "extendsArrayReturnType()",
            signature = "<E:TT;>()[TE;")
    <E extends T> E[] extendsArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "genericListReturnType()",
            signature = "()Ljava/util/List<TT;>;")
    List<T> genericListReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "genericListArrayReturnType()",
            signature = "()[Ljava/util/List<TT;>;")
    List<T>[] genericListArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "extendsBoundReturnType()",
            signature = "()Ljava/util/List<+TT;>;")
    List<? extends T> extendsBoundReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "extendsBoundArrayReturnType()",
            signature = "()[Ljava/util/List<+TT;>;")
    List<? extends T>[] extendsBoundArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "superBoundReturnType()",
            signature = "()Ljava/util/List<-TT;>;")
    List<? super T> superBoundReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "superBoundArrayReturnType()",
            signature = "()[Ljava/util/List<-TT;>;")
    List<? super T>[] superBoundArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "wildcardReturnType()",
            signature = "()Ljava/util/Map<**>;")
    Map<?, ?> wildcardReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "wildcardArrayReturnType()",
            signature = "()[Ljava/util/Map<**>;")
    Map<?, ?>[] wildcardArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticTypeReturnType()",
            signature = "<T:Ljava/lang/Object;>()TT;")
    static <T> T staticTypeReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticTypeArrayReturnType()",
            signature = "<T:Ljava/lang/Object;>()[TT;")
    static <T> T[] staticTypeArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticExtendsReturnType()",
            signature = "<T:Ljava/lang/Object;E:TT;>()TE;")
    static <T, E extends T> E staticExtendsReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticExtendsArrayReturnType()",
            signature = "<T:Ljava/lang/Object;E:TT;>()[TE;")
    static <T, E extends T> E[] staticExtendsArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticGenericListReturnType()",
            signature = "<T:Ljava/lang/Object;>()Ljava/util/List<TT;>;")
    static <T> List<T> staticGenericListReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticGenericListArrayReturnType()",
            signature = "<T:Ljava/lang/Object;>()[Ljava/util/List<TT;>;")
    static <T> List<T>[] staticGenericListArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticExtendsBoundReturnType()",
            signature = "<T:Ljava/lang/Object;>()Ljava/util/List<+TT;>;")
    static <T> List<? extends T> staticExtendsBoundReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticExtendsBoundArrayReturnType()",
            signature = "<T:Ljava/lang/Object;>()[Ljava/util/List<+TT;>;")
    static <T> List<? extends T>[] staticExtendsBoundArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticSuperBoundReturnType()",
            signature = "<T:Ljava/lang/Object;>()Ljava/util/List<-TT;>;")
    static <T> List<? super T> staticSuperBoundReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticSuperBoundArrayReturnType()",
            signature = "<T:Ljava/lang/Object;>()[Ljava/util/List<-TT;>;")
    static <T> List<? super T>[] staticSuperBoundArrayReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticWildcardReturnType()",
            signature = "()Ljava/util/Map<**>;")
    static Map<?, ?> staticWildcardReturnType() {
        return null;
    }

    @ExpectedSignature(descriptor = "staticWildcardArrayReturnType()",
            signature = "()[Ljava/util/Map<**>;")
    static Map<?, ?>[] staticWildcardArrayReturnType() {
        return null;
    }

    List noSignature() {
        return null;
    }

    static List staticNoSignatureAttribute() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsBooleanArray()",
            signature = "()Ljava/util/Map<+[Z-[Z>;")
    Map<? extends boolean[], ? super boolean[]> boundsBooleanArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsByteArray()",
            signature = "()Ljava/util/Map<+[B-[B>;")
    Map<? extends byte[], ? super byte[]> boundsByteArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsShortArray()",
            signature = "()Ljava/util/Map<+[S-[S>;")
    Map<? extends short[], ? super short[]> boundsShortArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsIntArray()",
            signature = "()Ljava/util/Map<+[I-[I>;")
    Map<? extends int[], ? super int[]> boundsIntArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsLongArray()",
            signature = "()Ljava/util/Map<+[J-[J>;")
    Map<? extends long[], ? super long[]> boundsLongArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsCharArray()",
            signature = "()Ljava/util/Map<+[C-[C>;")
    Map<? extends char[], ? super char[]> boundsCharArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsFloatArray()",
            signature = "()Ljava/util/Map<+[F-[F>;")
    Map<? extends float[], ? super float[]> boundsFloatArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsDoubleArray()",
            signature = "()Ljava/util/Map<+[D-[D>;")
    Map<? extends double[], ? super double[]> boundsDoubleArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "boundsObjectArray()",
            signature = "()Ljava/util/Map<+[Ljava/lang/Object;-[Ljava/lang/Object;>;")
    Map<? extends Object[], ? super Object[]> boundsObjectArray() {
        return null;
    }

    @ExpectedSignature(descriptor = "voidMethod(java.awt.Frame)", signature = "(TT;)V")
    void voidMethod(T a) {
    }

    @ExpectedSignature(descriptor = "byteMethod(java.awt.Frame)", signature = "(TT;)B")
    byte byteMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "shortMethod(java.awt.Frame)", signature = "(TT;)S")
    short shortMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "charMethod(java.awt.Frame)", signature = "(TT;)C")
    char charMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "intMethod(java.awt.Frame)", signature = "(TT;)I")
    int intMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "longMethod(java.awt.Frame)", signature = "(TT;)J")
    long longMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "booleanMethod(java.awt.Frame)", signature = "(TT;)Z")
    boolean booleanMethod(T a) {
        return false;
    }

    @ExpectedSignature(descriptor = "floatMethod(java.awt.Frame)", signature = "(TT;)F")
    float floatMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "doubleMethod(java.awt.Frame)", signature = "(TT;)D")
    double doubleMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "objectMethod(java.awt.Frame)", signature = "(TT;)Ljava/lang/Object;")
    Object objectMethod(T a) {
        return null;
    }

    @ExpectedSignature(descriptor = "staticVoidMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)V")
    static <T> void staticVoidMethod(T a) {
    }

    @ExpectedSignature(descriptor = "staticByteMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)B")
    static <T> byte staticByteMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "staticShortMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)S")
    static <T> short staticShortMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "staticCharMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)C")
    static <T> char staticCharMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "staticIntMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)I")
    static <T> int staticIntMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "staticLongMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)J")
    static <T> long staticLongMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "staticBooleanMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)Z")
    static <T> boolean staticBooleanMethod(T a) {
        return false;
    }

    @ExpectedSignature(descriptor = "staticFloatMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)F")
    static <T> float staticFloatMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "staticDoubleMethod(java.lang.Object)", signature = "<T:Ljava/lang/Object;>(TT;)D")
    static <T> double staticDoubleMethod(T a) {
        return 0;
    }

    @ExpectedSignature(descriptor = "staticObjectMethod(java.lang.Object)",
            signature = "<T:Ljava/lang/Object;>(TT;)Ljava/lang/Object;")
    static <T> Object staticObjectMethod(T a) {
        return null;
    }

    void voidReturnTypeNoSignatureAttribute() {
    }

    byte byteReturnTypeNoSignatureAttribute() {
        return 0;
    }

    Object objectReturnNoSignatureAttribute() {
        return null;
    }

    static void staticVoidReturnTypeNoSignatureAttribute() {
    }

    static byte staticByteReturnTypeNoSignatureAttribute() {
        return 0;
    }

    static Object staticObjectReturnTypeNoSignatureAttribute() {
        return null;
    }
}
