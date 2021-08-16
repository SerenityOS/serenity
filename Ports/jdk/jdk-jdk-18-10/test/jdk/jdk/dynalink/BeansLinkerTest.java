/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.dynalink.StandardNamespace.ELEMENT;
import static jdk.dynalink.StandardNamespace.METHOD;
import static jdk.dynalink.StandardNamespace.PROPERTY;
import static jdk.dynalink.StandardOperation.CALL;
import static jdk.dynalink.StandardOperation.GET;
import static jdk.dynalink.StandardOperation.SET;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.Namespace;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.NoSuchDynamicMethodException;
import jdk.dynalink.Operation;
import jdk.dynalink.beans.StaticClass;
import jdk.dynalink.support.SimpleRelinkableCallSite;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @test
 * @run testng BeansLinkerTest
 */
public class BeansLinkerTest {
    public static class Bean1 {
        public final int answer = 42;

        public String getName() {
            return "bean1";
        }

        public String someMethod(final String x) {
            return x + "-foo";
        }
    }

    @Test
    public static void testPublicFieldPropertyUnnamedGetter() {
        testGetterPermutations(PROPERTY, (op) -> Assert.assertEquals(42, call(op, new Bean1(), "answer")));
    }

    @Test
    public static void testPublicFieldPropertyNamedGetter() {
        testGetterPermutations(PROPERTY, (op) -> Assert.assertEquals(42, call(op.named("answer"), new Bean1())));
    }

    @Test
    public static void testGetterPropertyUnnamedGetter() {
        testGetterPermutations(PROPERTY, (op) -> Assert.assertEquals("bean1", call(op, new Bean1(), "name")));
    }

    @Test
    public static void testGetterPropertyNamedGetter() {
        testGetterPermutations(PROPERTY, (op) -> Assert.assertEquals("bean1", call(op.named("name"), new Bean1())));
    }

    @Test
    public static void testMethodUnnamedGetter() {
        testGetterPermutations(METHOD, (op) -> Assert.assertEquals("bar-foo", call(call(op, new Bean1(), "someMethod"), new Bean1(), "bar")));
    }

    @Test
    public static void testMethodNamedGetter() {
        testGetterPermutations(METHOD, (op) -> Assert.assertEquals("bar-foo", call(call(op.named("someMethod"), new Bean1()), new Bean1(), "bar")));
    }

    private static final Map<String, String> MAP1 = new HashMap<>();
    static {
        MAP1.put("foo", "bar");
    }

    @Test
    public static void testElementUnnamedGetter() {
        testGetterPermutations(ELEMENT, (op) -> Assert.assertEquals("bar", call(op, MAP1, "foo")));
    }

    @Test
    public static void testElementNamedGetter() {
        testGetterPermutations(ELEMENT, (op) -> Assert.assertEquals("bar", call(op.named("foo"), MAP1)));
    }

    public static class Bean2 {
        public int answer;
        private String name;

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public static void testUnnamedFieldSetter() {
        testSetterPermutations(PROPERTY, (op) -> {
            final Bean2 bean2 = new Bean2();
            call(op, bean2, "answer", 12);
            Assert.assertEquals(bean2.answer, 12);
        });
    }

    @Test
    public static void testNamedFieldSetter() {
        testSetterPermutations(PROPERTY, (op) -> {
            final Bean2 bean2 = new Bean2();
            call(op.named("answer"), bean2, 14);
            Assert.assertEquals(bean2.answer, 14);
        });
    }

    @Test
    public static void testUnnamedPropertySetter() {
        testSetterPermutations(PROPERTY, (op) -> {
            final Bean2 bean2 = new Bean2();
            call(op, bean2, "name", "boo");
            Assert.assertEquals(bean2.name, "boo");
        });
    }

    @Test
    public static void testNamedPropertySetter() {
        testSetterPermutations(PROPERTY, (op) -> {
            final Bean2 bean2 = new Bean2();
            call(op.named("name"), bean2, "blah");
            Assert.assertEquals(bean2.name, "blah");
        });
    }

    private static final Pattern GET_ELEMENT_THEN_PROPERTY_PATTERN = Pattern.compile(".*ELEMENT.*PROPERTY.*");

    @Test
    public static void testUnnamedElementAndPropertyGetter() {
        final Map<String, Object> map = new HashMap<>();
        map.put("empty", true);
        testGetterPermutations(GET_ELEMENT_THEN_PROPERTY_PATTERN, 4, (op) -> Assert.assertEquals(true, call(op, map, "empty")));
    }

    @Test
    public static void testNamedElementAndPropertyGetter() {
        final Map<String, Object> map = new HashMap<>();
        map.put("empty", true);
        testGetterPermutations(GET_ELEMENT_THEN_PROPERTY_PATTERN, 4, (op) -> Assert.assertEquals(true, call(op.named("empty"), map)));
    }

    private static final Pattern GET_PROPERTY_THEN_ELEMENT_PATTERN = Pattern.compile(".*PROPERTY.*ELEMENT.*");

    @Test
    public static void testUnnamedPropertyAndElementGetter() {
        final Map<String, Object> map = new HashMap<>();
        map.put("empty", true);
        testGetterPermutations(GET_PROPERTY_THEN_ELEMENT_PATTERN, 4, (op) -> Assert.assertEquals(false, call(op, map, "empty")));
    }

    @Test
    public static void testNamedPropertyAndElementGetter() {
        final Map<String, Object> map = new HashMap<>();
        map.put("empty", true);
        testGetterPermutations(GET_PROPERTY_THEN_ELEMENT_PATTERN, 4, (op) -> Assert.assertEquals(false, call(op.named("empty"), map)));
    }

    public static class MapWithProperty extends HashMap<String, Object> {
        private String name;

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public static void testUnnamedPropertyAndElementSetter() {
        final MapWithProperty map = new MapWithProperty();
        map.put("name", "element");

        call(SET.withNamespaces(PROPERTY, ELEMENT), map, "name", "property");
        Assert.assertEquals("property", map.name);
        Assert.assertEquals("element", map.get("name"));

        call(SET.withNamespaces(ELEMENT, PROPERTY), map, "name", "element2");
        Assert.assertEquals("property", map.name);
        Assert.assertEquals("element2", map.get("name"));
    }

    @Test
    public static void testMissingMembersAtLinkTime() {
        testPermutations(GETTER_PERMUTATIONS, (op) -> expectNoSuchDynamicMethodException(()-> call(op.named("foo"), new Object())));
        testPermutations(SETTER_PERMUTATIONS, (op) -> expectNoSuchDynamicMethodException(()-> call(op.named("foo"), new Object(), "newValue")));
    }

    @Test
    public static void testMissingMembersAtRunTime() {
        call(GET.withNamespace(ELEMENT), new ArrayList<>(), "foo");
        Stream.of(new HashMap(), new ArrayList(), new Object[0]).forEach((receiver) -> {
            testPermutations(GETTER_PERMUTATIONS, (op) -> { System.err.println(op + " " + receiver.getClass().getName()); Assert.assertNull(call(op, receiver, "foo"));});
            // No assertion for the setter; we just expect it to silently succeed
            testPermutations(SETTER_PERMUTATIONS, (op) -> call(op, receiver, "foo", "newValue"));
        });
    }

    public static class A {
        public static class Inner {}
    }

    public static class B extends A {
        public static class Inner {}
    }

    @Test
    public static void testInnerClassGetter() {
        Object inner1 = call(GET.withNamespace(PROPERTY), StaticClass.forClass(A.class), "Inner");
        Assert.assertTrue(inner1 instanceof StaticClass);
        Assert.assertEquals(A.Inner.class, ((StaticClass) inner1).getRepresentedClass());

        Object inner2 = call(GET.withNamespace(PROPERTY), StaticClass.forClass(B.class), "Inner");
        Assert.assertTrue(inner2 instanceof StaticClass);
        Assert.assertEquals(B.Inner.class, ((StaticClass) inner2).getRepresentedClass());
    }

    private static void expectNoSuchDynamicMethodException(final Runnable r) {
        try {
            r.run();
            Assert.fail("Should've thrown NoSuchDynamicMethodException");
        } catch(final NoSuchDynamicMethodException e) {
        }
    }

    private static final NamespaceOperation[] GETTER_PERMUTATIONS = new NamespaceOperation[] {
        GET.withNamespaces(PROPERTY),
        GET.withNamespaces(METHOD),
        GET.withNamespaces(ELEMENT),
        GET.withNamespaces(PROPERTY, ELEMENT),
        GET.withNamespaces(PROPERTY, METHOD),
        GET.withNamespaces(ELEMENT,  PROPERTY),
        GET.withNamespaces(ELEMENT,  METHOD),
        GET.withNamespaces(METHOD,   PROPERTY),
        GET.withNamespaces(METHOD,   ELEMENT),
        GET.withNamespaces(PROPERTY, ELEMENT,  METHOD),
        GET.withNamespaces(PROPERTY, METHOD,   ELEMENT),
        GET.withNamespaces(ELEMENT,  PROPERTY, METHOD),
        GET.withNamespaces(ELEMENT,  METHOD,   PROPERTY),
        GET.withNamespaces(METHOD,   PROPERTY, ELEMENT),
        GET.withNamespaces(METHOD,   ELEMENT,  PROPERTY)
    };

    private static final NamespaceOperation[] SETTER_PERMUTATIONS = new NamespaceOperation[] {
        SET.withNamespaces(PROPERTY),
        SET.withNamespaces(ELEMENT),
        SET.withNamespaces(PROPERTY, ELEMENT),
        SET.withNamespaces(ELEMENT, PROPERTY)
    };

    private static void testPermutations(final NamespaceOperation[] ops, final Operation requiredOp, final Namespace requiredNamespace, final int expectedCount, final Consumer<NamespaceOperation> test) {
        testPermutationsWithFilter(ops, (op)->NamespaceOperation.contains(op, requiredOp, requiredNamespace), expectedCount, test);
    }

    private static void testPermutations(final NamespaceOperation[] ops, final Pattern regex, final int expectedCount, final Consumer<NamespaceOperation> test) {
        testPermutationsWithFilter(ops, (op)->regex.matcher(op.toString()).matches(), expectedCount, test);
    }

    private static void testPermutations(final NamespaceOperation[] ops, final Consumer<NamespaceOperation> test) {
        testPermutationsWithFilter(ops, (op)->true, ops.length, test);
    }

    private static void testPermutationsWithFilter(final NamespaceOperation[] ops, final Predicate<NamespaceOperation> filter, final int expectedCount, final Consumer<NamespaceOperation> test) {
        final int[] counter = new int[1];
        Stream.of(ops).filter(filter).forEach((op)-> { counter[0]++; test.accept(op); });
        Assert.assertEquals(counter[0], expectedCount);
    }

    private static void testGetterPermutations(final Namespace requiredNamespace, final Consumer<NamespaceOperation> test) {
        testPermutations(GETTER_PERMUTATIONS, GET, requiredNamespace, 11, test);
    }

    private static void testGetterPermutations(final Pattern regex, final int expectedCount, final Consumer<NamespaceOperation> test) {
        testPermutations(GETTER_PERMUTATIONS, regex, expectedCount, test);
    }

    private static void testSetterPermutations(final Namespace requiredNamespace, final Consumer<NamespaceOperation> test) {
        testPermutations(SETTER_PERMUTATIONS, SET, requiredNamespace, 3, test);
    }

    private static Object call(final Operation op, final Object... args) {
        try {
            return new DynamicLinkerFactory().createLinker().link(
                    new SimpleRelinkableCallSite(new CallSiteDescriptor(
                            MethodHandles.publicLookup(), op, t(args.length))))
                            .dynamicInvoker().invokeWithArguments(args);
        } catch (final Error|RuntimeException e) {
            throw e;
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Object call(final Object... args) {
        return call(CALL, args);
    }

    private static MethodType t(final int argCount) {
        return MethodType.methodType(Object.class, Collections.nCopies(argCount, Object.class));
    }
}
