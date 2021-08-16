/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8023651 8044629
 * @summary Test that the receiver annotations and the return annotations of
 *          constructors behave correctly.
 * @run testng ConstructorReceiverTest
 */

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.Arrays;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ConstructorReceiverTest {
    public static final Integer EMPTY_ANNOTATED_TYPE =  Integer.valueOf(-1);

    // Format is {
    //   { Class to get ctor for,
    //       ctor param class,
    //       value of anno of return type,
    //       value of anno for receiver,
    //              or null if there should be no receiver,
    //              or EMPTY_ANNOTATED_TYPE of there should be a receiver but
    //              no annotation
    //    },
    //    ...
    // }
    public static final Object[][] TESTS = {
        { ConstructorReceiverTest.class, null, Integer.valueOf(5), null },
        { ConstructorReceiverTest.Middle.class, ConstructorReceiverTest.class, Integer.valueOf(10), Integer.valueOf(15) },
        { ConstructorReceiverTest.Middle.Inner.class, ConstructorReceiverTest.Middle.class, Integer.valueOf(100), Integer.valueOf(150) },
        { ConstructorReceiverTest.Middle.Inner.Innermost.class, ConstructorReceiverTest.Middle.Inner.class, Integer.valueOf(1000), Integer.valueOf(1500) },
        { ConstructorReceiverTest.Middle.InnerNoReceiver.class, ConstructorReceiverTest.Middle.class, Integer.valueOf(300), EMPTY_ANNOTATED_TYPE },
        { ConstructorReceiverTest.Nested.class, null, Integer.valueOf(20), null },
        { ConstructorReceiverTest.Nested.NestedMiddle.class, ConstructorReceiverTest.Nested.class, Integer.valueOf(200), Integer.valueOf(250)},
        { ConstructorReceiverTest.Nested.NestedMiddle.NestedInner.class, ConstructorReceiverTest.Nested.NestedMiddle.class, Integer.valueOf(2000), Integer.valueOf(2500)},
        { ConstructorReceiverTest.Nested.NestedMiddle.NestedInnerNoReceiver.class, ConstructorReceiverTest.Nested.NestedMiddle.class, Integer.valueOf(4000), EMPTY_ANNOTATED_TYPE},
        { ConstructorReceiverTest.Nested.NestedMiddle.SecondNestedInnerNoReceiver.class, ConstructorReceiverTest.Nested.NestedMiddle.class, Integer.valueOf(5000), EMPTY_ANNOTATED_TYPE},
    };


    @DataProvider
    public Object[][] data() { return TESTS; }

    @Test(dataProvider = "data")
    public void testAnnotatedReciver(Class<?> toTest, Class<?> ctorParamType,
            Integer returnVal, Integer receiverVal) throws NoSuchMethodException {
        Constructor c;
        if (ctorParamType == null)
            c = toTest.getDeclaredConstructor();
        else
            c = toTest.getDeclaredConstructor(ctorParamType);

        AnnotatedType annotatedReceiverType = c.getAnnotatedReceiverType();

        // Some Constructors doesn't conceptually have a receiver, they should return null
        if (receiverVal == null) {
            assertNull(annotatedReceiverType, "getAnnotatedReciverType  should return null for Constructor: " + c);
            return;
        }

        // check that getType() matches the receiver (which can be parameterized)
        if (annotatedReceiverType.getType() instanceof ParameterizedType) {
            assertEquals(((ParameterizedType) annotatedReceiverType.getType()).getRawType(),
                    ctorParamType,
                    "getType() doesn't match receiver type: " + ctorParamType);
        } else {
            assertEquals(annotatedReceiverType.getType(),
                    ctorParamType,
                    "getType() doesn't match receiver type: " + ctorParamType);
        }

        Annotation[] receiverAnnotations = annotatedReceiverType.getAnnotations();

        // Some Constructors have no annotations on but in theory can have a receiver
        if (receiverVal.equals(EMPTY_ANNOTATED_TYPE)) {
            assertEquals(receiverAnnotations.length, 0, "expecting an empty annotated type for: " + c);
            return;
        }

        // The rest should have annotations
        assertEquals(receiverAnnotations.length, 1, "expecting a 1 element array. Looking at 'length': ");
        assertEquals(((Annot)receiverAnnotations[0]).value(), receiverVal.intValue(), " wrong annotation found. Found " +
                receiverAnnotations[0] +
                " should find @Annot with value=" +
                receiverVal);
    }

    @Test(dataProvider = "data")
    public void testAnnotatedReturn(Class<?> toTest, Class<?> ctorParamType,
            Integer returnVal, Integer receiverVal) throws NoSuchMethodException {
        Constructor c;
        if (ctorParamType == null)
            c = toTest.getDeclaredConstructor();
        else
            c = toTest.getDeclaredConstructor(ctorParamType);

        AnnotatedType annotatedReturnType = c.getAnnotatedReturnType();
        Annotation[] returnAnnotations = annotatedReturnType.getAnnotations();

        assertEquals(returnAnnotations.length, 1, "expecting a 1 element array. Looking at 'length': ");
        assertEquals(((Annot)returnAnnotations[0]).value(), returnVal.intValue(), " wrong annotation found. Found " +
                returnAnnotations[0] +
                " should find @Annot with value=" +
                returnVal);
    }

    @Annot(5) ConstructorReceiverTest() {}

    private class Middle {
        @Annot(10) public Middle(@Annot(15) ConstructorReceiverTest ConstructorReceiverTest.this) {}

        public class Inner {
            @Annot(100) Inner(@Annot(150) Middle Middle.this) {}

            class Innermost {
                @Annot(1000) private Innermost(@Annot(1500) Inner Inner.this) {}
            }
        }

        class InnerNoReceiver {
            @Annot(300) InnerNoReceiver(Middle Middle.this) {}
        }
    }

    public static class Nested {
        @Annot(20) public Nested() {}

        class NestedMiddle {
            @Annot(200) public NestedMiddle(@Annot(250) Nested Nested.this) {}

            class NestedInner {
                @Annot(2000) public NestedInner(@Annot(2500) NestedMiddle NestedMiddle.this) {}
            }

            class NestedInnerNoReceiver {
                @Annot(4000) public NestedInnerNoReceiver() {}
            }

            class SecondNestedInnerNoReceiver {
                @Annot(5000) public SecondNestedInnerNoReceiver(NestedMiddle NestedMiddle.this) {}
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public static @interface Annot {
        int value();
    }
}
