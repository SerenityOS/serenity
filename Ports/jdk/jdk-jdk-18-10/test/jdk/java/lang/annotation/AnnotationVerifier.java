/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Create class file using ASM, slightly modified the ASMifier output
 */

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * @test
 * @bug 8158510
 * @summary Verify valid annotation
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @modules java.base/sun.reflect.annotation
 * @clean AnnotationWithVoidReturn AnnotationWithParameter
 *        AnnotationWithExtraInterface AnnotationWithException
 *        AnnotationWithHashCode AnnotationWithDefaultMember
 *        AnnotationWithoutAnnotationAccessModifier HolderX
 * @compile -XDignore.symbol.file ClassFileGenerator.java GoodAnnotation.java
 * @run main ClassFileGenerator
 * @run testng AnnotationVerifier
 */

public class AnnotationVerifier {

    //=======================================================
    // GoodAnnotation...

    @GoodAnnotation
    static class HolderA {
    }

    @Test
    public void holderA_goodAnnotation() {
        testGetAnnotation(HolderA.class, GoodAnnotation.class, true);
    }

    @Test
    public void holderA_annotations() {
        testGetAnnotations(HolderA.class, GoodAnnotation.class);
    }

    //=======================================================
    // AnnotationWithParameter...

    /*
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithParameter {
        int m(int x) default -1;
    }
    */

    @GoodAnnotation
    @AnnotationWithParameter
    static class HolderB {
    }

    @Test
    public void holderB_annotationWithParameter() {
        testGetAnnotation(HolderB.class, AnnotationWithParameter.class, false);
    }

    @Test
    public void holderB_goodAnnotation() {
        testGetAnnotation(HolderB.class, GoodAnnotation.class, true);
    }

    @Test
    public void holderB_annotations() {
        testGetAnnotations(HolderB.class, GoodAnnotation.class);
    }

    //=======================================================
    // AnnotationWithVoidReturn...

    /*
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithVoidReturn {
        void m() default 1;
    }
    */

    @GoodAnnotation
    @AnnotationWithVoidReturn
    static class HolderC {
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderC_annotationWithVoidReturn() {
        testGetAnnotation(HolderC.class, AnnotationWithVoidReturn.class, false);
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderC_goodAnnotation() {
        testGetAnnotation(HolderC.class, GoodAnnotation.class, false);
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderC_annotations() {
        testGetAnnotations(HolderC.class);
    }

    //=======================================================
    // AnnotationWithExtraInterface...

    /*
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithExtraInterface extends java.io.Serializable {
        int m() default 1;
    }
    */

    @GoodAnnotation
    @AnnotationWithExtraInterface
    static class HolderD {
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderD_annotationWithExtraInterface() {
        testGetAnnotation(HolderD.class, AnnotationWithExtraInterface.class, false);
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderD_goodAnnotation() {
        testGetAnnotation(HolderD.class, GoodAnnotation.class, false);
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderD_annotations() {
        testGetAnnotations(HolderD.class);
    }

    //=======================================================
    // AnnotationWithException...

    /*
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithException {
        int m() throws Exception default 1;
    }
    */

    @GoodAnnotation
    @AnnotationWithException
    static class HolderE {
    }

    @AnnotationWithException
    static class HolderE2 {
    }

    @Test
    public void holderE_annotationWithException() {
        testGetAnnotation(HolderE.class, AnnotationWithException.class, true);
    }

    @Test
    public void holderE_goodAnnotation() {
        testGetAnnotation(HolderE.class, GoodAnnotation.class, true);
    }

    @Test
    public void holderE_annotations() {
        testGetAnnotations(HolderE.class, GoodAnnotation.class, AnnotationWithException.class);
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderE_annotationWithException_equals() {
        AnnotationWithException ann1, ann2;
        try {
            ann1 = HolderE.class.getAnnotation(AnnotationWithException.class);
            ann2 = HolderE2.class.getAnnotation(AnnotationWithException.class);
        } catch (Throwable t) {
            throw new AssertionError("Unexpected exception", t);
        }
        Assert.assertNotNull(ann1);
        Assert.assertNotNull(ann2);

        testEquals(ann1, ann2, true); // this throws AnnotationFormatError
    }

    //=======================================================
    // AnnotationWithHashCode...

    /*
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithHashCode {
        int hashCode() default 1;
    }
    */

    @GoodAnnotation
    @AnnotationWithHashCode
    static class HolderF {
    }

    @AnnotationWithHashCode
    static class HolderF2 {
    }

    @Test
    public void holderF_annotationWithHashCode() {
        testGetAnnotation(HolderF.class, AnnotationWithHashCode.class, true);
    }

    @Test
    public void holderF_goodAnnotation() {
        testGetAnnotation(HolderF.class, GoodAnnotation.class, true);
    }

    @Test
    public void holderF_annotations() {
        testGetAnnotations(HolderF.class, GoodAnnotation.class, AnnotationWithHashCode.class);
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderF_annotationWithHashCode_equals() {
        AnnotationWithHashCode ann1, ann2;
        try {
            ann1 = HolderF.class.getAnnotation(AnnotationWithHashCode.class);
            ann2 = HolderF2.class.getAnnotation(AnnotationWithHashCode.class);
        } catch (Throwable t) {
            throw new AssertionError("Unexpected exception", t);
        }
        Assert.assertNotNull(ann1);
        Assert.assertNotNull(ann2);

        testEquals(ann1, ann2, true); // this throws AnnotationFormatError
    }

    //=======================================================
    // AnnotationWithDefaultMember...

    /*
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnnotationWithDefaultMember {
        int m() default 1;
        default int d() default 2 { return 2; }
    }
    */

    @GoodAnnotation
    @AnnotationWithDefaultMember
    static class HolderG {
    }

    @AnnotationWithDefaultMember
    static class HolderG2 {
    }

    @Test
    public void holderG_annotationWithDefaultMember() {
        testGetAnnotation(HolderG.class, AnnotationWithDefaultMember.class, true);
    }

    @Test
    public void holderG_goodAnnotation() {
        testGetAnnotation(HolderG.class, GoodAnnotation.class, true);
    }

    @Test
    public void holderG_annotations() {
        testGetAnnotations(HolderG.class, GoodAnnotation.class, AnnotationWithDefaultMember.class);
    }

    @Test(expectedExceptions = AnnotationFormatError.class)
    public void holderG_annotationWithDefaultMember_equals() {
        AnnotationWithDefaultMember ann1, ann2;
        try {
            ann1 = HolderG.class.getAnnotation(AnnotationWithDefaultMember.class);
            ann2 = HolderG2.class.getAnnotation(AnnotationWithDefaultMember.class);
        } catch (Throwable t) {
            throw new AssertionError("Unexpected exception", t);
        }
        Assert.assertNotNull(ann1);
        Assert.assertNotNull(ann2);

        testEquals(ann1, ann2, true); // this throws AnnotationFormatError
    }

    //=======================================================
    // AnnotationWithoutAnnotationAccessModifier...

    /*

    @Retention(RetentionPolicy.RUNTIME)
    public interface AnnotationWithoutAnnotationAccessModifier extends Annotation {
        int m() default 1;
    }

    @GoodAnnotation
    @AnnotationWithoutAnnotationAccessModifier
    static class HolderX {
    }

    */

    @Test
    public void holderX_annotationWithoutAnnotationAccessModifier() {
        testGetAnnotation(HolderX.class, AnnotationWithoutAnnotationAccessModifier.class, false);
    }

    @Test
    public void holderX_goodAnnotation() {
        testGetAnnotation(HolderX.class, GoodAnnotation.class, true);
    }

    @Test
    public void holderX_annotations() {
        testGetAnnotations(HolderX.class, GoodAnnotation.class);
    }

    //=======================================================
    // utils
    //

    private static void testGetAnnotation(Class<?> holderClass,
                                          Class<? extends Annotation> annType,
                                          boolean expectedPresent) {
        Object result = null;
        try {
            try {
                result = holderClass.getAnnotation(annType);
                if (expectedPresent != (result != null)) {
                    throw new AssertionError("Expected " +
                                             (expectedPresent ? "non-null" : "null") +
                                             " result, but got: " + result);
                }
            } catch (Throwable t) {
                result = t;
                throw t;
            }
        } finally {
            System.out.println("\n" +
                               holderClass.getSimpleName() +
                               ".class.getAnnotation(" +
                               annType.getSimpleName() +
                               ".class) = " +
                               result);
        }
    }

    private static void testGetAnnotations(Class<?> holderClass,
                                           Class<? extends Annotation> ... expectedTypes) {
        Object result = null;
        try {
            try {
                Annotation[] anns = holderClass.getAnnotations();

                Set<Class<? extends Annotation>> gotTypes =
                    Stream.of(anns)
                          .map(Annotation::annotationType)
                          .collect(Collectors.toSet());

                Set<Class<? extends Annotation>> expTypes =
                    Stream.of(expectedTypes)
                          .collect(Collectors.toSet());

                if (!expTypes.equals(gotTypes)) {
                    throw new AssertionError("Expected annotation types: " + expTypes +
                                             " but got: " + Arrays.toString(anns));
                }
                result = Arrays.toString(anns);
            } catch (Throwable t) {
                result = t;
                throw t;
            }
        } finally {
            System.out.println("\n" +
                               holderClass.getSimpleName() +
                               ".class.getAnnotations() = " +
                               result);
        }
    }

    private static void testEquals(Annotation ann1, Annotation ann2, boolean expectedEquals) {
        Object result = null;
        try {
            try {
                boolean gotEquals = ann1.equals(ann2);
                Assert.assertEquals(gotEquals, expectedEquals);
                result = gotEquals;
            } catch (Throwable t) {
                result = t;
                throw t;
            }
        } finally {
            System.out.println("\n" + ann1 + ".equals(" + ann2 + ") = " + result);
        }
    }
}
