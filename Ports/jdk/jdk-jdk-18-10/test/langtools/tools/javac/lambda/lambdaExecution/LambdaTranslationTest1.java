/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8003639
 * @summary convert lambda testng tests to jtreg and add them
 * @run testng LambdaTranslationTest1
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class LambdaTranslationTest1 extends LT1Sub {

    String cntxt = "blah";

    private static final ThreadLocal<Object> result = new ThreadLocal<>();

    private static void setResult(Object s) { result.set(s); }
    private static void appendResult(Object s) { result.set(result.get().toString() + s); }

    private static void assertResult(String expected) {
        assertEquals(result.get().toString(), expected);
    }

    static Integer count(String s) {
        return s.length();
    }

    static int icount(String s) {
        return s.length();
    }

    static void eye(Integer i) {
        setResult(String.format("I:%d", i));
    }

    static void ieye(int i) {
        setResult(String.format("i:%d", i));
    }

    static void deye(double d) {
        setResult(String.format("d:%f", d));
    }

    public void testLambdas() {
        TBlock<Object> b = t -> {setResult("Sink0::" + t);};
        b.apply("Howdy");
        assertResult("Sink0::Howdy");

        TBlock<String> b1 = t -> {setResult("Sink1::" + t);};
        b1.apply("Rowdy");
        assertResult("Sink1::Rowdy");

        for (int i = 5; i < 10; ++i) {
            TBlock<Integer> b2 = t -> {setResult("Sink2::" + t);};
            b2.apply(i);
            assertResult("Sink2::" + i);
        }

        TBlock<Integer> b3 = t -> {setResult("Sink3::" + t);};
        for (int i = 900; i > 0; i -= 100) {
            b3.apply(i);
            assertResult("Sink3::" + i);
        }

        cntxt = "blah";
        TBlock<String> b4 = t -> {setResult(String.format("b4: %s .. %s", cntxt, t));};
        b4.apply("Yor");
        assertResult("b4: blah .. Yor");

        String flaw = "flaw";
        TBlock<String> b5 = t -> {setResult(String.format("b5: %s .. %s", flaw, t));};
        b5.apply("BB");
        assertResult("b5: flaw .. BB");

        cntxt = "flew";
        TBlock<String> b6 = t -> {setResult(String.format("b6: %s .. %s .. %s", t, cntxt, flaw));};
        b6.apply("flee");
        assertResult("b6: flee .. flew .. flaw");

        TBlock<String> b7 = t -> {setResult(String.format("b7: %s %s", t, this.protectedSuperclassMethod()));};
        b7.apply("this:");
        assertResult("b7: this: instance:flew");

        TBlock<String> b8 = t -> {setResult(String.format("b8: %s %s", t, super.protectedSuperclassMethod()));};
        b8.apply("super:");
        assertResult("b8: super: I'm the sub");

        TBlock<String> b7b = t -> {setResult(String.format("b9: %s %s", t, protectedSuperclassMethod()));};
        b7b.apply("implicit this:");
        assertResult("b9: implicit this: instance:flew");

        TBlock<Object> b10 = t -> {setResult(String.format("b10: new LT1Thing: %s", (new LT1Thing(t)).str));};
        b10.apply("thing");
        assertResult("b10: new LT1Thing: thing");

        TBlock<Object> b11 = t -> {setResult(String.format("b11: %s", (new LT1Thing(t) {
            String get() {
                return "*" + str.toString() + "*";
            }
        }).get()));};
        b11.apply(999);
        assertResult("b11: *999*");
    }

    public void testMethodRefs() {
        LT1IA ia = LambdaTranslationTest1::eye;
        ia.doit(1234);
        assertResult("I:1234");

        LT1IIA iia = LambdaTranslationTest1::ieye;
        iia.doit(1234);
        assertResult("i:1234");

        LT1IA da = LambdaTranslationTest1::deye;
        da.doit(1234);
        assertResult("d:1234.000000");

        LT1SA a = LambdaTranslationTest1::count;
        assertEquals((Integer) 5, a.doit("howdy"));

        a = LambdaTranslationTest1::icount;
        assertEquals((Integer) 6, a.doit("shower"));
    }

    public void testInner() throws Exception {
        (new In()).doInner();
    }

    protected String protectedSuperclassMethod() {
        return "instance:" + cntxt;
    }

    private class In {

        private int that = 1234;

        void doInner() {
            TBlock<String> i4 = t -> {setResult(String.format("i4: %d .. %s", that, t));};
            i4.apply("=1234");
            assertResult("i4: 1234 .. =1234");

            TBlock<String> i5 = t -> {setResult(""); appendResult(t); appendResult(t);};
            i5.apply("fruit");
            assertResult("fruitfruit");

            cntxt = "human";
            TBlock<String> b4 = t -> {setResult(String.format("b4: %s .. %s", cntxt, t));};
            b4.apply("bin");
            assertResult("b4: human .. bin");

            final String flaw = "flaw";

/**
 Callable<String> c5 = () ->  "["+flaw+"]" ;
 System.out.printf("c5: %s\n", c5.call() );
 **/

            TBlock<String> b5 = t -> {setResult(String.format("b5: %s .. %s", flaw, t));};
            b5.apply("BB");
            assertResult("b5: flaw .. BB");

            cntxt = "borg";
            TBlock<String> b6 = t -> {setResult(String.format("b6: %s .. %s .. %s", t, cntxt, flaw));};
            b6.apply("flee");
            assertResult("b6: flee .. borg .. flaw");

            TBlock<String> b7b = t -> {setResult(String.format("b7b: %s %s", t, protectedSuperclassMethod()));};
            b7b.apply("implicit outer this");
            assertResult("b7b: implicit outer this instance:borg");

            /**
             TBlock<Object> b9 = t -> { System.out.printf("New: %s\n", (new LT1Thing(t)).str); };
             b9.apply("thing");

             TBlock<Object> ba = t -> { System.out.printf("Def: %s\n", (new LT1Thing(t) { String get() { return "*" + str.toString() +"*";}}).get() ); };
             ba.apply(999);

             */
        }
    }
}

class LT1Sub {
    protected String protectedSuperclassMethod() {
        return "I'm the sub";
    }
}

class LT1Thing {
    final Object str;

    LT1Thing(Object s) {
        str = s;
    }
}

interface LT1SA {
    Integer doit(String s);
}

interface LT1IA {
    void doit(int i);
}

interface LT1IIA {
    void doit(Integer i);
}
