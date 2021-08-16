/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.lang.invoke;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * SerializedLambdaTest
 *
 * @author Brian Goetz
 */
@Test
public class SerializedLambdaTest {
    public static final int REPS = 50;

    @SuppressWarnings("unchecked")
    private<T> void assertSerial(T p, Consumer<T> asserter) throws IOException, ClassNotFoundException {
        asserter.accept(p);

        for (int i=0; i<REPS; i++) {
            byte[] bytes = serialize(p);
            assertTrue(bytes.length > 0);

            asserter.accept((T) deserialize(bytes));
        }
    }

    private void assertNotSerial(Predicate<String> p, Consumer<Predicate<String>> asserter)
            throws IOException, ClassNotFoundException {
        asserter.accept(p);
        try {
            byte[] bytes = serialize(p);
            fail("Expected serialization failure");
        }
        catch (NotSerializableException e) {
            // success
        }
    }

    private byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.close();
        return bos.toByteArray();
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readObject();
        }
    }

    // Test instantiating against intersection type
    public void testSimpleSerializedInstantiation() throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Predicate<String> pred = (Predicate<String> & Serializable) s -> true;
        assertSerial(pred,
                     p -> {
                         assertTrue(p instanceof Predicate);
                         assertTrue(p instanceof Serializable);
                         assertTrue(p.test(""));
                     });
    }

    interface SerPredicate<T> extends Predicate<T>, Serializable { }

    // Test instantiating against derived type
    public void testSimpleSerializedInstantiation2() throws IOException, ClassNotFoundException {
        SerPredicate<String> serPred = (SerPredicate<String>) s -> true;
        assertSerial(serPred,
                     p -> {
                         assertTrue(p instanceof Predicate);
                         assertTrue(p instanceof Serializable);
                         assertTrue(p instanceof SerPredicate);
                         assertTrue(p.test(""));
                     });
    }

    // Negative test: non-serializable lambdas are in fact not serializable
    public void testNonserializableInstantiation() throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Predicate<String> pred = (Predicate<String>) s -> true;
        assertNotSerial(pred,
                        p -> {
                            assertTrue(p instanceof Predicate);
                            assertFalse(p instanceof Serializable);
                            assertTrue(p.test(""));
                        });
    }

    // Test lambda capturing int
    public void testSerializeCapturingInt() throws IOException, ClassNotFoundException {
        class Moo {
            @SuppressWarnings("unchecked")
            Predicate<String> foo(int x) {
                return (Predicate<String> & Serializable) s -> s.length() >= x;
            }
        }
        Predicate<String> pred = new Moo().foo(3);
        assertSerial(pred, p -> {
            assertTrue(p.test("yada"));
            assertFalse(p.test("no"));
        });
    }

    // Test lambda capturing String
    public void testSerializeCapturingString() throws IOException, ClassNotFoundException {
        class Moo {
            @SuppressWarnings("unchecked")
            Predicate<String> foo(String t) {
                return (Predicate<String> & Serializable) s -> s.equals(t);
            }
        }
        Predicate<String> pred = new Moo().foo("goo");
        assertSerial(pred, p -> {
            assertTrue(p.test("goo"));
            assertFalse(p.test("foo"));
        });
    }

    // Negative test: lambdas that capture a non-serializable var
    public void testSerializeCapturingNonSerializable() throws IOException, ClassNotFoundException {
        class Box {
            String s;

            Box(String s) { this.s = s; }
        }
        class Moo {
            @SuppressWarnings("unchecked")
            Predicate<String> foo(Box b) {
                return (Predicate<String> & Serializable) s -> s.equals(b.s);
            }
        }
        Predicate<String> pred = new Moo().foo(new Box("goo"));
        assertNotSerial(pred, p -> {
            assertTrue(p.test("goo"));
            assertFalse(p.test("foo"));
        });
    }

    static boolean startsWithA(String s) {
        return s.startsWith("a");
    }

    // Test static method ref
    public void testStaticMR() throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Predicate<String> mh1 = (Predicate<String> & Serializable) SerializedLambdaTest::startsWithA;
        @SuppressWarnings("unchecked")
        Predicate<String> mh2 = (SerPredicate<String>) SerializedLambdaTest::startsWithA;
        Consumer<Predicate<String>> b = p -> {
            assertTrue(p instanceof Serializable);
            assertTrue(p.test("arf"));
            assertFalse(p.test("barf"));
        };
        assertSerial(mh1, b);
        assertSerial(mh2, b);
    }

    // Test unbound method ref of nonserializable class -- should still succeed
    public void testUnboundMR() throws IOException, ClassNotFoundException {
        class Moo {
            public boolean startsWithB(String s) {
                return s.startsWith("b");
            }
        }
        @SuppressWarnings("unchecked")
        BiPredicate<Moo, String> mh1 = (BiPredicate<Moo, String> & Serializable) Moo::startsWithB;
        Consumer<BiPredicate<Moo, String>> b = p -> {
            assertTrue(p instanceof Serializable);
            assertTrue(p.test(new Moo(), "barf"));
            assertFalse(p.test(new Moo(), "arf"));
        };
        assertSerial(mh1, b);
    }

    // Negative test: test bound MR of nonserializable class
    public void testBoundMRNotSerReceiver() throws IOException, ClassNotFoundException {
        class Moo {
            public boolean startsWithB(String s) {
                return s.startsWith("b");
            }
        }
        Moo moo = new Moo();
        @SuppressWarnings("unchecked")
        Predicate<String> mh1 = (Predicate<String> & Serializable) moo::startsWithB;
        @SuppressWarnings("unchecked")
        Predicate<String> mh2 = (SerPredicate<String>) moo::startsWithB;
        Consumer<Predicate<String>> b = p -> {
            assertTrue(p instanceof Serializable);
            assertTrue(p.test("barf"));
            assertFalse(p.test("arf"));
        };
        assertNotSerial(mh1, b);
        assertNotSerial(mh2, b);
    }

    // Test bound MR of serializable class
    @SuppressWarnings("serial")
    static class ForBoundMRef implements Serializable {
        public boolean startsWithB(String s) {
            return s.startsWith("b");
        }
    }

    public void testBoundMR() throws IOException, ClassNotFoundException {
        ForBoundMRef moo = new ForBoundMRef();
        @SuppressWarnings("unchecked")
        Predicate<String> mh1 = (Predicate<String> & Serializable) moo::startsWithB;
        @SuppressWarnings("unchecked")
        Predicate<String> mh2 = (SerPredicate<String>) moo::startsWithB;
        Consumer<Predicate<String>> b = p -> {
            assertTrue(p instanceof Serializable);
            assertTrue(p.test("barf"));
            assertFalse(p.test("arf"));
        };
        assertSerial(mh1, b);
        assertSerial(mh2, b);
    }

    static class ForCtorRef {
        public boolean startsWithB(String s) {
            return s.startsWith("b");
        }
    }
    // Test ctor ref of nonserializable class
    public void testCtorRef() throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Supplier<ForCtorRef> ctor = (Supplier<ForCtorRef> & Serializable) ForCtorRef::new;
        Consumer<Supplier<ForCtorRef>> b = s -> {
            assertTrue(s instanceof Serializable);
            ForCtorRef m = s.get();
            assertTrue(m.startsWithB("barf"));
            assertFalse(m.startsWithB("arf"));
        };
        assertSerial(ctor, b);
    }

    //Test throwing away return type
    public void testDiscardReturnBound() throws IOException, ClassNotFoundException {
        List<String> list = new ArrayList<>();
        Consumer<String> c = (Consumer<String> & Serializable) list::add;
        assertSerial(c, cc -> { assertTrue(cc instanceof Consumer); });

        AtomicLong a = new AtomicLong();
        LongConsumer lc = (LongConsumer & Serializable) a::addAndGet;
        assertSerial(lc, plc -> { plc.accept(3); });
    }

    // Tests of direct use of metafactories

    private static boolean foo(Object s) { return s != null && ((String) s).length() > 0; }
    private static final MethodType predicateMT = MethodType.methodType(boolean.class, Object.class);
    private static final MethodType stringPredicateMT = MethodType.methodType(boolean.class, String.class);
    private static final Consumer<Predicate<String>> fooAsserter = x -> {
        assertTrue(x.test("foo"));
        assertFalse(x.test(""));
        assertFalse(x.test(null));
    };

    // standard MF: nonserializable supertype
    public void testDirectStdNonser() throws Throwable {
        MethodHandle fooMH = MethodHandles.lookup().findStatic(SerializedLambdaTest.class, "foo", predicateMT);

        // Standard metafactory, non-serializable target: not serializable
        CallSite cs = LambdaMetafactory.metafactory(MethodHandles.lookup(),
                                                    "test", MethodType.methodType(Predicate.class),
                                                    predicateMT, fooMH, stringPredicateMT);
        Predicate<String> p = (Predicate<String>) cs.getTarget().invokeExact();
        assertNotSerial(p, fooAsserter);
    }

    // standard MF: serializable supertype
    public void testDirectStdSer() throws Throwable {
        MethodHandle fooMH = MethodHandles.lookup().findStatic(SerializedLambdaTest.class, "foo", predicateMT);

        // Standard metafactory, serializable target: not serializable
        CallSite cs = LambdaMetafactory.metafactory(MethodHandles.lookup(),
                                                    "test", MethodType.methodType(SerPredicate.class),
                                                    predicateMT, fooMH, stringPredicateMT);
        assertNotSerial((SerPredicate<String>) cs.getTarget().invokeExact(), fooAsserter);
    }

    // alt MF: nonserializable supertype
    public void testAltStdNonser() throws Throwable {
        MethodHandle fooMH = MethodHandles.lookup().findStatic(SerializedLambdaTest.class, "foo", predicateMT);

        // Alt metafactory, non-serializable target: not serializable
        CallSite cs = LambdaMetafactory.altMetafactory(MethodHandles.lookup(),
                                                       "test", MethodType.methodType(Predicate.class),
                                                       predicateMT, fooMH, stringPredicateMT, 0);
        assertNotSerial((Predicate<String>) cs.getTarget().invokeExact(), fooAsserter);
    }

    // alt MF: serializable supertype
    public void testAltStdSer() throws Throwable {
        MethodHandle fooMH = MethodHandles.lookup().findStatic(SerializedLambdaTest.class, "foo", predicateMT);

        // Alt metafactory, serializable target, no FLAG_SERIALIZABLE: not serializable
        CallSite cs = LambdaMetafactory.altMetafactory(MethodHandles.lookup(),
                                                       "test", MethodType.methodType(SerPredicate.class),
                                                       predicateMT, fooMH, stringPredicateMT, 0);
        assertNotSerial((SerPredicate<String>) cs.getTarget().invokeExact(), fooAsserter);

        // Alt metafactory, serializable marker, no FLAG_SERIALIZABLE: not serializable
        cs = LambdaMetafactory.altMetafactory(MethodHandles.lookup(),
                                              "test", MethodType.methodType(Predicate.class),
                                              predicateMT, fooMH, stringPredicateMT, LambdaMetafactory.FLAG_MARKERS, 1, Serializable.class);
        assertNotSerial((Predicate<String>) cs.getTarget().invokeExact(), fooAsserter);
    }
}
