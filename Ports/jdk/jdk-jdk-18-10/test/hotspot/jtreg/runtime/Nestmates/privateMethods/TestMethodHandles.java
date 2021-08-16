/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046171
 * @summary Test access to private methods between nestmates and nest-host
 *          using different flavours of named nested types using MethodHandles
 * @run main TestMethodHandles
 */


import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class TestMethodHandles {

    static final MethodType M_T = MethodType.methodType(void.class);

    // Private method of nest-host for nestmates to access
    private void priv_invoke() {
        System.out.println("TestMethodHandles::priv_invoke");
    }

    // public constructor so we aren't relying on private access
    public TestMethodHandles() {}

    // Methods that will access private methods of nestmates

    void access_priv(TestMethodHandles o) throws Throwable {
        MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
        mh.invoke(o);
        mh.invokeExact(o);
        checkBadInvoke(mh, new StaticNested()); // wrong nestmate
        checkBadInvoke(mh, mh); // completely wrong type
        // findSpecial also works when this and o are the same class
        mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
        mh.invoke(o);
        mh.invokeExact(o);
        checkBadInvoke(mh, new StaticNested()); // wrong nestmate
        checkBadInvoke(mh, mh); // completely wrong type
    }
    void access_priv(InnerNested o) throws Throwable {
        MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
        mh.invoke(o);
        mh.invokeExact(o);
        checkBadInvoke(mh, this); // wrong nestmate
        checkBadInvoke(mh, mh); // completely wrong type
        try {
            mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
            throw new Error("findSpecial() succeeded unexpectedly");
        }
        catch (IllegalAccessException expected) {}
    }
    void access_priv(StaticNested o) throws Throwable {
        MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
        mh.invoke(o);
        mh.invokeExact(o);
        checkBadInvoke(mh, this); // wrong nestmate
        checkBadInvoke(mh, mh); // completely wrong type
        try {
            mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
            throw new Error("findSpecial() succeeded unexpectedly");
        }
        catch (IllegalAccessException expected) {}
    }
    void access_priv(StaticIface o) throws Throwable {
        MethodHandle mh = lookup().findVirtual(StaticIface.class, "priv_invoke", M_T);
        mh.invoke(o);
        mh.invokeExact(o);
        checkBadInvoke(mh, this); // wrong nestmate
        checkBadInvoke(mh, mh); // completely wrong type
        try {
            mh = lookup().findSpecial(StaticIface.class, "priv_invoke", M_T, this.getClass());
            throw new Error("findSpecial() succeeded unexpectedly");
        }
        catch (IllegalAccessException expected) {}
    }

    // The various nestmates

    static interface StaticIface {

        private void priv_invoke() {
            System.out.println("StaticIface::priv_invoke");
        }

        // Methods that will access private methods of nestmates

        default void access_priv(TestMethodHandles o) throws Throwable {
            MethodHandle mh =
              lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
        default void access_priv(InnerNested o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
        default void access_priv(StaticNested o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
        default void access_priv(StaticIface o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(StaticIface.class, "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, new StaticNested()); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            // findSpecial also works when this and o are the same interface
            mh = lookup().findSpecial(StaticIface.class, "priv_invoke", M_T, StaticIface.class);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, new StaticNested()); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
        }
    }

    static class StaticNested {

        private void priv_invoke() {
            System.out.println("StaticNested::priv_invoke");
        }

        // public constructor so we aren't relying on private access
        public StaticNested() {}

        // Methods that will access private methods of nestmates

        void access_priv(TestMethodHandles o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
        void access_priv(InnerNested o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
        void access_priv(StaticNested o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, new TestMethodHandles()); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            // findSpecial also works when this and o are the same class
            mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, new TestMethodHandles()); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
        }
        void access_priv(StaticIface o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(StaticIface.class, "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(StaticIface.class, "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
    }

    class InnerNested {

        private void priv_invoke() {
            System.out.println("InnerNested::priv_invoke");
        }

        // public constructor so we aren't relying on private access
        public InnerNested() {}

        void access_priv(TestMethodHandles o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
        void access_priv(InnerNested o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, new StaticNested()); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            // findSpecial also works when this and o are the same class
            mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, new StaticNested()); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
        }
        void access_priv(StaticNested o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(o.getClass(), "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(o.getClass(), "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
        void access_priv(StaticIface o) throws Throwable {
            MethodHandle mh = lookup().findVirtual(StaticIface.class, "priv_invoke", M_T);
            mh.invoke(o);
            mh.invokeExact(o);
            checkBadInvoke(mh, this); // wrong nestmate
            checkBadInvoke(mh, mh); // completely wrong type
            try {
                mh = lookup().findSpecial(StaticIface.class, "priv_invoke", M_T, this.getClass());
                throw new Error("findSpecial() succeeded unexpectedly");
            }
            catch (IllegalAccessException expected) {}
        }
    }

    static void checkBadInvoke(MethodHandle mh, Object o) throws Throwable {
        try {
            mh.invoke(o);
            throw new Error("Invoke on MethodHandle " + mh + " with receiver "
                            + o + "should have failed with ClassCastException!");
         }
         catch (ClassCastException expected) {
             System.out.println("invoke got expected exception: " + expected);
         }
    }

    public static void main(String[] args) throws Throwable {
        TestMethodHandles o = new TestMethodHandles();
        StaticNested s = new StaticNested();
        InnerNested i = o.new InnerNested();
        StaticIface intf = new StaticIface() {};

        o.access_priv(new TestMethodHandles());
        o.access_priv(i);
        o.access_priv(s);
        o.access_priv(intf);

        s.access_priv(o);
        s.access_priv(i);
        s.access_priv(new StaticNested());
        s.access_priv(intf);

        i.access_priv(o);
        i.access_priv(o.new InnerNested());
        i.access_priv(s);
        i.access_priv(intf);

        intf.access_priv(o);
        intf.access_priv(i);
        intf.access_priv(s);
        intf.access_priv(new StaticIface(){});
    }
}
