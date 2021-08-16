/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058595
 * @summary Test that AnnotatedType.getAnnotatedOwnerType() works as expected
 *
 * @library /test/lib
 * @build jdk.test.lib.Asserts
 * @run main GetAnnotatedOwnerType
 */

import java.lang.annotation.*;
import java.lang.reflect.*;

import jdk.test.lib.Asserts;

public class GetAnnotatedOwnerType<Dummy> {
    public @TA("generic") GetAnnotatedOwnerType<String> . @TB("generic") Nested<Integer> genericField;
    public @TA("raw") GetAnnotatedOwnerType . @TB("raw") Nested rawField;
    public @TA("non-generic") GetAnnotatedOwnerTypeAuxilliary . @TB("non-generic") Inner nonGeneric;
    public @TA("non-generic") GetAnnotatedOwnerTypeAuxilliary . @TB("generic") InnerGeneric<String> innerGeneric;
    public @TA("non-generic") GetAnnotatedOwnerTypeAuxilliary . @TB("raw") InnerGeneric innerRaw;
    public GetAnnotatedOwnerTypeAuxilliary . @TB("non-generic") Nested nestedNonGeneric;
    public GetAnnotatedOwnerTypeAuxilliary . @TB("generic") NestedGeneric<String> nestedGeneric;
    public GetAnnotatedOwnerTypeAuxilliary . @TB("raw") NestedGeneric nestedRaw;
    public Object anonymous = new Object() {};
    public @TA("array") Dummy[] dummy;
    public @TA("wildcard") GetAnnotatedOwnerType<?> wildcard;
    public @TA("typevariable") Dummy tv;
    public @TA("bad") GetAnnotatedOwnerType<@TA("good") GetAnnotatedOwnerType<String> . @TB("tb") Nested<Integer> >  typeArgument;
    public GetAnnotatedOwnerType< GetAnnotatedOwnerType<String> .
            B .
            C<Class<?>, ? extends @TA("complicated") Exception> .
            D<Number> > [] complicated;

    public static void main(String[] args) throws Exception {
        testGeneric();
        testRaw();
        testNonGeneric();
        testInnerGeneric();
        testInnerRaw();
        testNestedNonGeneric();
        testNestedGeneric();
        testNestedRaw();

        testLocalClass();
        testAnonymousClass();

        testArray();
        testWildcard();
        testTypeParameter();

        testTypeArgument();
        testComplicated();
    }

    public static void testGeneric() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("genericField");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "generic");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((ParameterizedType) f.getGenericType()).getOwnerType());
        Asserts.assertEquals(outer.getAnnotation(TA.class).value(), "generic");
        Asserts.assertTrue(outer.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + outer.getAnnotations().length);
    }

    public static void testRaw() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("rawField");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "raw");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((Class<?>)f.getGenericType()).getEnclosingClass());
        Asserts.assertEquals(outer.getAnnotation(TA.class).value(), "raw");
        Asserts.assertTrue(outer.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + outer.getAnnotations().length);
    }

    public static void testNonGeneric() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("nonGeneric");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "non-generic");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((Class<?>)f.getGenericType()).getEnclosingClass());
        Asserts.assertEquals(outer.getAnnotation(TA.class).value(), "non-generic");
        Asserts.assertTrue(outer.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + outer.getAnnotations().length);
    }

    public static void testInnerGeneric() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("innerGeneric");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "generic");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((ParameterizedType) f.getGenericType()).getOwnerType());
        Asserts.assertEquals(outer.getAnnotation(TA.class).value(), "non-generic");
        Asserts.assertTrue(outer.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + outer.getAnnotations().length);
    }

    public static void testInnerRaw() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("innerRaw");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "raw");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((Class<?>)f.getGenericType()).getEnclosingClass());
        Asserts.assertEquals(outer.getAnnotation(TA.class).value(), "non-generic");
        Asserts.assertTrue(outer.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + outer.getAnnotations().length);
    }

    public static void testNestedNonGeneric() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("nestedNonGeneric");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "non-generic");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((Class<?>)f.getGenericType()).getEnclosingClass());
        Asserts.assertTrue(outer.getAnnotations().length == 0, "expecting no annotations, got: "
                + outer.getAnnotations().length);
    }

    public static void testNestedGeneric() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("nestedGeneric");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "generic");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((ParameterizedType) f.getGenericType()).getOwnerType());
        Asserts.assertTrue(outer.getAnnotations().length == 0, "expecting no annotations, got: "
                + outer.getAnnotations().length);
    }

    public static void testNestedRaw() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("nestedRaw");

        // make sure inner is correctly annotated
        AnnotatedType inner = f.getAnnotatedType();
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "raw");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated, on the correct type
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getType(), ((Class<?>)f.getGenericType()).getEnclosingClass());
        Asserts.assertTrue(outer.getAnnotations().length == 0, "expecting no annotations, got: "
                + outer.getAnnotations().length);
    }

    public static void testLocalClass() throws Exception {
        class ALocalClass {}
        class OneMore {
            public @TA("null") ALocalClass c;
        }
        testNegative(OneMore.class.getField("c").getAnnotatedType(), "Local class should return null");
    }

    public static void testAnonymousClass() throws Exception {
        testNegative(GetAnnotatedOwnerType.class.getField("anonymous").getAnnotatedType(),
                "Anonymous class should return null");
    }

    public static void testArray() throws Exception {
        AnnotatedType t = GetAnnotatedOwnerType.class.getField("dummy").getAnnotatedType();
        Asserts.assertTrue((t instanceof AnnotatedArrayType),
                "Was expecting an AnnotatedArrayType " + t);
        testNegative(t, "" + t + " should not have an annotated owner type");
    }

    public static void testWildcard() throws Exception {
        AnnotatedType tt = GetAnnotatedOwnerType.class.getField("wildcard").getAnnotatedType();
        AnnotatedType t = ((AnnotatedParameterizedType)tt).getAnnotatedActualTypeArguments()[0];
        Asserts.assertTrue((t instanceof AnnotatedWildcardType),
                "Was expecting an AnnotatedWildcardType " + t);
        testNegative(t, "" + t + " should not have an annotated owner type");
    }

    public static void testTypeParameter() throws Exception {
        AnnotatedType t = GetAnnotatedOwnerType.class.getField("tv").getAnnotatedType();
        Asserts.assertTrue((t instanceof AnnotatedTypeVariable),
                "Was expecting an AnnotatedTypeVariable " + t);
        testNegative(t, "" + t + " should not have an annotated owner type");
    }

    public static void testTypeArgument() throws Exception {
        AnnotatedType tt = GetAnnotatedOwnerType.class.getField("typeArgument").getAnnotatedType();
        Asserts.assertEquals(tt.getAnnotation(TA.class).value(), "bad");
        Asserts.assertTrue(tt.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + tt.getAnnotations().length);

        // make sure inner is correctly annotated
        AnnotatedType inner = ((AnnotatedParameterizedType)tt).getAnnotatedActualTypeArguments()[0];
        Asserts.assertEquals(inner.getAnnotation(TB.class).value(), "tb");
        Asserts.assertTrue(inner.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + inner.getAnnotations().length);

        // make sure owner is correctly annotated
        AnnotatedType outer = inner.getAnnotatedOwnerType();
        Asserts.assertEquals(outer.getAnnotation(TA.class).value(), "good");
        Asserts.assertTrue(outer.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + outer.getAnnotations().length);
    }

    public static void testComplicated() throws Exception {
        Field f = GetAnnotatedOwnerType.class.getField("complicated");

        // Outermost level
        AnnotatedType t = f.getAnnotatedType();
        Asserts.assertTrue((t instanceof AnnotatedArrayType),
                "Was expecting an AnnotatedArrayType " + t);
        testNegative(t, "" + t + " should not have an annotated owner type");
        Asserts.assertTrue(t.getAnnotations().length == 0, "expecting zero annotation, got: "
                + t.getAnnotations().length);

        // Component type
        t = ((AnnotatedArrayType)t).getAnnotatedGenericComponentType();
        testNegative(t, "" + t + " should not have an annotated owner type");
        Asserts.assertTrue(t.getAnnotations().length == 0, "expecting zero annotation, got: "
                + t.getAnnotations().length);

        // Type arg GetAnnotatedOwnerType<String>...D<Number>
        t = ((AnnotatedParameterizedType)t).getAnnotatedActualTypeArguments()[0];
        Asserts.assertTrue(t.getAnnotations().length == 0, "expecting zero annotation, got: "
                + t.getAnnotations().length);

        // C<Class<?>, ? extends ...>
        t = t.getAnnotatedOwnerType();
        Asserts.assertTrue(t.getAnnotations().length == 0, "expecting zero annotation, got: "
                + t.getAnnotations().length);

        // ? extends
        t = ((AnnotatedParameterizedType)t).getAnnotatedActualTypeArguments()[1];
        testNegative(t, "" + t + " should not have an annotated owner type");
        Asserts.assertTrue(t.getAnnotations().length == 0, "expecting zero annotation, got: "
                + t.getAnnotations().length);

        // @TA("complicated") Exception
        t = ((AnnotatedWildcardType)t).getAnnotatedUpperBounds()[0];
        testNegative(t, "" + t + " should not have an annotated owner type");
        Asserts.assertEquals(t.getAnnotation(TA.class).value(), "complicated");
        Asserts.assertTrue(t.getAnnotations().length == 1, "expecting one (1) annotation, got: "
                + t.getAnnotations().length);
    }

    private static void testNegative(AnnotatedType t, String msg) {
        Asserts.assertNull(t.getAnnotatedOwnerType(), msg);
    }

    public class Nested<AlsoDummy> {}
    public class B {
        public class C<R, S> {
            public class D<T> {
            }
        }
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TA {
        String value();
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TB {
        String value();
    }
}

class GetAnnotatedOwnerTypeAuxilliary {
    class Inner {}

    class InnerGeneric<Dummy> {}

    static class Nested {}

    static class NestedGeneric<Dummy> {}
}
