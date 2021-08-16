/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4102566
 * @summary Test access methods for assignment targets.
 * @author William Maddox (maddox)
 *
 * @compile AccessMethodsLHS.java
 * @run main AccessMethodsLHS
 */

public class AccessMethodsLHS {

    static void fail(String msg) throws Exception {
        throw new Exception("FAIL: " + msg);
    }

    static int arg = 123456;

    private int i;
    private double d;

    private void m(int x) throws Exception {
        System.out.println("called AccessMethodsLHS.m");
        if (x != 123456)
            AccessMethodsLHS.fail("bad argument");
    }

    public static class Bar {
        private int i;
        private double d;
        private String s;

        private void m(int x) throws Exception {
            System.out.println("called AccessMethodsLHS.Bar.m");
            if (x != 123456)
                AccessMethodsLHS.fail("bad argument");
        }

        // Static members are permitted in a static inner class.

        static private int si;
        static private double sd;
        static private String ss;

        static private void sm(int x) throws Exception {
            System.out.println("called AccessMethodsLHS.Bar.sm");
            if (x != 123456)
                AccessMethodsLHS.fail("bad argument");
        }
    }

    public static class Baz {
        private int i;
        private double d;
        private String s;

        private void m(int x) throws Exception {
            System.out.println("called Baz.m");
            if (x != 123456)
                AccessMethodsLHS.fail("bad argument");
        }

        // Compiler rejects static members here correctly.

        // static private int si;
        // static private double sd;
        // static private String ss;
    }

    public class Quux {
        void DoIt () throws Exception {
            m(arg);
            i = 1;
            d = 1.0;
            i += 5;
            i--;
        }
        void DoMore(AccessMethodsLHS f) throws Exception {
            f.m(arg);
        }
    }

    public static class Quem {
        void DoIt () {
            // Illegal static refs to non-static vars
            // i = 1;
            // d = 1.0;
            // i += 5;
            // i--;
        }
    }

    static int effects = 0;

    static Bar iBar(Bar x) { effects++; return x; }

    static Baz iBaz(Baz x) { effects++; return x; }

    static void checkEffects(int i) throws Exception {
        if (effects != 1) {
            AccessMethodsLHS.fail("incorrect side-effect behavior (" + effects + "): " + i);
        }
        effects = 0;
    }

    static public void main(String args[]) throws Exception {

        Bar b = new Bar();
        Baz c = new Baz();

        System.out.println("testing assignment");

        AccessMethodsLHS f = new AccessMethodsLHS();

        Quux q1 = f.new Quux();
        q1.DoIt();
        q1.DoMore(f);

        Quem q2 = new Quem();
        q2.DoIt();

        // *** Static class, Non-static members ***

        b.m(arg);

        // Integer (1 word)

        b.i = 5;
        System.out.println(b.i);
        if (b.i != 5)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(b.i);


        if ((b.i += 10) != 15)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(b.i);

        if (b.i != 15)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(b.i);

        b.s = "foo";
        if (!(b.s += "bar").equals("foobar"))
            AccessMethodsLHS.fail("concat-assign result");
        System.out.println(b.s);

        if (!b.s.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect");
        System.out.println(b.s);

        b.s = "foo";
        b.s += "bar";
        if (!b.s.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect (novalue)");
        System.out.println(b.s);

        b.i = 0;
        b.i++;
        if (b.i != 1)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(b.i);

        b.i = 5;
        if (b.i++ != 5)
            AccessMethodsLHS.fail("post-increment result");
        if (b.i != 6)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(b.i);

        b.i = 1;
        --b.i;
        if (b.i != 0)
            AccessMethodsLHS.fail("pre-decrement effect");

        b.i = 5;
        if (--b.i != 4)
            AccessMethodsLHS.fail("pre-decrement result");
        if (b.i != 4)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(b.i);

        // Double (2 word)

        b.d = 5.0;
        System.out.println(b.d);
        if (b.d != 5.0)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(b.d);

        if ((b.d += 10) != 15.0)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(b.d);

        if (b.d != 15.0)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(b.d);

        b.d = 0.0;
        b.d++;
        if (b.d != 1.0)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(b.d);

        b.d = 5.0;
        if (b.d++ != 5.0)
            AccessMethodsLHS.fail("post-increment result");
        if (b.d != 6.0)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(b.d);

        b.d = 1.0;
        --b.d;
        if (b.d != 0.0)
            AccessMethodsLHS.fail("pre-decrement effect");

        b.d = 5.0;
        if (--b.d != 4.0)
            AccessMethodsLHS.fail("pre-decrement result");
        if (b.d != 4.0)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(b.d);

        // Integer (1 word) with side-effects in object reference

        iBar(b).i = 5;
        checkEffects(1);
        System.out.println(b.i);
        if (b.i != 5)
            AccessMethodsLHS.fail("simple assignment");

        System.out.println(b.i);

        if ((iBar(b).i += 10) != 15)
            AccessMethodsLHS.fail("add-assign result");
        checkEffects(2);
        System.out.println(b.i);

        if (b.i != 15)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(b.i);

        b.i = 0;
        iBar(b).i++;
        checkEffects(3);
        if (b.i != 1)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(b.i);

        b.i = 5;
        if (iBar(b).i++ != 5)
            AccessMethodsLHS.fail("post-increment result");
        checkEffects(4);
        if (b.i != 6)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(b.i);

        b.i = 1;
        --iBar(b).i;
        checkEffects(5);
        if (b.i != 0)
            AccessMethodsLHS.fail("pre-decrement effect");

        b.i = 5;
        if (--iBar(b).i != 4)
            AccessMethodsLHS.fail("pre-decrement result");
        checkEffects(6);
        if (b.i != 4)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(b.i);


        // *** Static class, Static members ***

        Bar.sm(arg);

        // Integer (1 word)

        Bar.si = 5;
        System.out.println(Bar.si);
        if (Bar.si != 5)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(Bar.si);

        if ((Bar.si += 10) != 15)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(Bar.si);

        if (Bar.si != 15)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(Bar.si);

        Bar.ss = "foo";
        if (!(Bar.ss += "bar").equals("foobar"))
            AccessMethodsLHS.fail("concat-assign result");
        System.out.println(Bar.ss);

        if (!Bar.ss.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect");
        System.out.println(Bar.ss);

        Bar.ss = "foo";
        Bar.ss += "bar";
        if (!Bar.ss.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect (novalue)");
        System.out.println(Bar.ss);

        Bar.si = 0;
        Bar.si++;
        if (Bar.si != 1)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(Bar.si);

        Bar.si = 5;
        if (Bar.si++ != 5)
            AccessMethodsLHS.fail("post-increment result");
        if (Bar.si != 6)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(Bar.si);

        Bar.si = 1;
        --Bar.si;
        if (Bar.si != 0)
            AccessMethodsLHS.fail("pre-decrement effect");

        Bar.si = 5;
        if (--Bar.si != 4)
            AccessMethodsLHS.fail("pre-decrement result");
        if (Bar.si != 4)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(Bar.si);

        // Double (2 word)

        Bar.sd = 5.0;
        System.out.println(Bar.sd);
        if (Bar.sd != 5.0)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(Bar.sd);

        if ((Bar.sd += 10) != 15.0)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(Bar.sd);

        if (Bar.sd != 15.0)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(Bar.sd);

        Bar.sd = 0.0;
        Bar.sd++;
        if (Bar.sd != 1.0)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(Bar.sd);

        Bar.sd = 5.0;
        if (Bar.sd++ != 5.0)
            AccessMethodsLHS.fail("post-increment result");
        if (Bar.sd != 6.0)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(Bar.sd);

        Bar.sd = 1.0;
        --Bar.sd;
        if (Bar.sd != 0.0)
            AccessMethodsLHS.fail("pre-decrement effect");

        Bar.sd = 5.0;
        if (--Bar.sd != 4.0)
            AccessMethodsLHS.fail("pre-decrement result");
        if (Bar.sd != 4.0)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(Bar.sd);


        // *** Static class, Static members (invoked via object reference) ***

        b.sm(arg);

        iBar(b).sm(arg);
        checkEffects(101);

        // Integer (1 word)

        b.si = 5;
        System.out.println(b.si);
        if (b.si != 5)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(b.si);

        if ((b.si += 10) != 15)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(b.si);

        if (b.si != 15)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(b.si);

        b.ss = "foo";
        if (!(b.ss += "bar").equals("foobar"))
            AccessMethodsLHS.fail("concat-assign result");
        System.out.println(b.ss);

        if (!b.ss.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect");
        System.out.println(b.ss);

        b.ss = "foo";
        b.ss += "bar";
        if (!b.ss.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect (novalue)");
        System.out.println(b.ss);

        b.si = 0;
        b.si++;
        if (b.si != 1)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(b.si);

        b.si = 5;
        if (b.si++ != 5)
            AccessMethodsLHS.fail("post-increment result");
        if (b.si != 6)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(b.si);

        b.si = 1;
        --b.si;
        if (b.si != 0)
            AccessMethodsLHS.fail("pre-decrement effect");

        b.si = 5;
        if (--b.si != 4)
            AccessMethodsLHS.fail("pre-decrement result");
        if (b.si != 4)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(b.si);

        // Double (2 word)

        b.sd = 5.0;
        System.out.println(b.sd);
        if (b.sd != 5.0)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(b.sd);

        if ((b.sd += 10) != 15.0)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(b.sd);

        if (b.sd != 15.0)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(b.sd);

        b.sd = 0.0;
        b.sd++;
        if (b.sd != 1.0)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(b.sd);

        b.sd = 5.0;
        if (b.sd++ != 5.0)
            AccessMethodsLHS.fail("post-increment result");
        if (b.sd != 6.0)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(b.sd);

        b.sd = 1.0;
        --b.sd;
        if (b.sd != 0.0)
            AccessMethodsLHS.fail("pre-decrement effect");

        b.sd = 5.0;
        if (--b.sd != 4.0)
            AccessMethodsLHS.fail("pre-decrement result");
        if (b.sd != 4.0)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(b.sd);

        // Integer (1 word) with side-effects in object reference

        iBar(b).si = 5;
        checkEffects(7);
        System.out.println(b.si);
        if (b.si != 5)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(b.si);

        if ((iBar(b).si += 10) != 15)
            AccessMethodsLHS.fail("add-assign result");
        checkEffects(8);
        System.out.println(b.si);

        if (b.si != 15)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(b.si);

        iBar(b).si = 0;
        checkEffects(9);
        iBar(b).si++;
        checkEffects(10);
        if (b.si != 1)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(b.si);

        b.si = 5;
        if (iBar(b).si++ != 5)
            AccessMethodsLHS.fail("post-increment result");
        checkEffects(11);
        if (b.si != 6)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(b.si);

        b.si = 1;
        --iBar(b).si;
        checkEffects(12);
        if (b.si != 0)
            AccessMethodsLHS.fail("pre-decrement effect");

        b.si = 5;
        if (--iBar(b).si != 4)
            AccessMethodsLHS.fail("pre-decrement result");
        checkEffects(13);
        if (b.si != 4)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(b.si);


        // *** Non-static class, Non-static members ***

        c.m(arg);

        iBaz(c).m(arg);
        checkEffects(102);

        // Integer (1 word)

        c.i = 5;
        System.out.println(c.i);
        if (c.i != 5)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(c.i);

        if ((c.i += 10) != 15)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(c.i);

        if (c.i != 15)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(c.i);

        c.s = "foo";
        if (!(c.s += "bar").equals("foobar"))
            AccessMethodsLHS.fail("concat-assign result");
        System.out.println(c.s);

        if (!c.s.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect");
        System.out.println(c.s);

        c.s = "foo";
        c.s += "bar";
        if (!c.s.equals("foobar"))
            AccessMethodsLHS.fail("concat-assign effect (novalue)");
        System.out.println(c.s);

        c.i = 0;
        c.i++;
        if (c.i != 1)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(c.i);

        c.i = 5;
        if (c.i++ != 5)
            AccessMethodsLHS.fail("post-increment result");
        if (c.i != 6)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(c.i);

        c.i = 1;
        --c.i;
        if (c.i != 0)
            AccessMethodsLHS.fail("pre-decrement effect");

        c.i = 5;
        if (--c.i != 4)
            AccessMethodsLHS.fail("pre-decrement result");
        if (c.i != 4)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(c.i);

        // Double (2 word)

        c.d = 5.0;
        System.out.println(c.d);
        if (c.d != 5.0)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(c.d);

        if ((c.d += 10) != 15.0)
            AccessMethodsLHS.fail("add-assign result");
        System.out.println(c.d);

        if (c.d != 15.0)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(c.d);

        c.d = 0.0;
        c.d++;
        if (c.d != 1.0)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(c.d);

        c.d = 5.0;
        if (c.d++ != 5.0)
            AccessMethodsLHS.fail("post-increment result");
        if (c.d != 6.0)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(c.d);

        c.d = 1.0;
        --c.d;
        if (c.d != 0.0)
            AccessMethodsLHS.fail("pre-decrement effect");

        c.d = 5.0;
        if (--c.d != 4.0)
            AccessMethodsLHS.fail("pre-decrement result");
        if (c.d != 4.0)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(c.d);

        // Double (2 word) with side-effects in object reference

        iBaz(c).d = 5.0;
        checkEffects(14);
        System.out.println(c.d);
        if (c.d != 5.0)
            AccessMethodsLHS.fail("simple assignment");
        System.out.println(c.d);

        if ((iBaz(c).d += 10) != 15.0)
            AccessMethodsLHS.fail("add-assign result");
        checkEffects(15);
        System.out.println(c.d);

        if (c.d != 15.0)
            AccessMethodsLHS.fail("add-assign effect");
        System.out.println(c.d);

        c.d = 0.0;
        iBaz(c).d++;
        checkEffects(16);
        if (c.d != 1.0)
            AccessMethodsLHS.fail("post-increment effect");
        System.out.println(c.d);

        c.d = 5.0;
        if (iBaz(c).d++ != 5.0)
            AccessMethodsLHS.fail("post-increment result");
        checkEffects(17);
        if (c.d != 6.0)
            AccessMethodsLHS.fail("post-increment effect (embedded)");
        System.out.println(c.d);

        c.d = 1.0;
        --iBaz(c).d;
        checkEffects(18);
        if (c.d != 0.0)
            AccessMethodsLHS.fail("pre-decrement effect");

        c.d = 5.0;
        if (--iBaz(c).d != 4.0)
            AccessMethodsLHS.fail("pre-decrement result");
        checkEffects(19);
        if (c.d != 4.0)
            AccessMethodsLHS.fail("pre-decrement effect (embedded)");
        System.out.println(c.d);

        // All done.

        System.out.println("done");
    }
}
