/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046977 8065303
 * @summary ClassCastException: typing information needed for method reference bridging not preserved
 * @author Srikanth
 * @run main MethodReferenceGenericTarget
 */

public class MethodReferenceGenericTarget {
    static String result = "";

    interface ISi { int m(Short a); }

    public static void main(String[] args) {
      (new MethodReferenceGenericTarget()).testUnboxObjectToNumberWiden();
      if (!result.equals("7775"))
          throw new AssertionError("Incorrect result");
        MethodReferenceTestPrivateTypeConversion.main(null);
        new InferenceHookTest().test();
    }

    void foo(ISi q) {
        result += q.m((short)75);
    }

    public void testUnboxObjectToNumberWiden() {
        ISi q = (new E<Short>())::xI;
        result += q.m((short)77);
        // Verify poly invocation context to confirm we handle
        // deferred/speculative attribution paths adequately.
        foo((new E<Short>())::xI);
    }

    class E<T> {
        private T xI(T t) { return t; }
    }
}

// snippet from https://bugs.openjdk.java.net/browse/JDK-8065303
class MethodReferenceTestPrivateTypeConversion {

    class MethodReferenceTestTypeConversion_E<T> {
        private T xI(T t) { return t; }
    }

    interface ISi { int m(Short a); }

    interface ICc { char m(Character a); }

    public void testUnboxObjectToNumberWiden() {
        ISi q = (new MethodReferenceTestTypeConversion_E<Short>())::xI;
        if ((q.m((short)77) != (short)77))
            throw new AssertionError("Incorrect result");
    }

    public void testUnboxObjectToChar() {
        ICc q = (new MethodReferenceTestTypeConversion_E<Character>())::xI;
        if (q.m('@') != '@')
            throw new AssertionError("Incorrect result");
    }

    public static void main(String[] args) {
        new MethodReferenceTestPrivateTypeConversion().testUnboxObjectToNumberWiden();
        new MethodReferenceTestPrivateTypeConversion().testUnboxObjectToChar();
    }
}

class InferenceHookTestBase {
    <X> X m(Integer i) { return null; }
}

class InferenceHookTest extends InferenceHookTestBase {
    interface SAM1<R> {
        R m(Integer i);
    }

    <Z> Z g(SAM1<Z> o) { return null; }

    void test() {
        String s = g(super::m);
        if (s != null)
            throw new AssertionError("Incorrect result");
    }
}
