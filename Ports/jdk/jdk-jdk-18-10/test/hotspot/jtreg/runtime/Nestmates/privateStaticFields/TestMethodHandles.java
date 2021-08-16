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
 * @summary Test access to private static fields between nestmates and nest-host
 *          using different flavours of named nested types using MethodHandles
 * @run main TestMethodHandles
 */

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;

import java.lang.reflect.Field;

public class TestMethodHandles {

    // private static field of nest-host for nestmates to access
    private static int priv_field;

    // public constructor so we aren't relying on private access
    public TestMethodHandles() {}

    // Methods that will access private static fields of nestmates

    // NOTE: No InnerNested calls in this test because non-static nested types
    // can't have static fields. Also no StaticIface calls as static interface
    // fields must be public (and final)

    void access_priv(TestMethodHandles o) throws Throwable {
        MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
        priv_field = (int) mh.invoke();
        priv_field = (int) mh.invokeExact();
        mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
        mh.invoke(2);
        mh.invokeExact(3);
    }
    void access_priv(StaticNested o) throws Throwable {
        MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
        priv_field = (int) mh.invoke();
        priv_field = (int) mh.invokeExact();
        mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
        mh.invoke(2);
        mh.invokeExact(3);
    }

    // The various nestmates

    static interface StaticIface {

        // Methods that will access private static fields of nestmates

        default void access_priv(TestMethodHandles o) throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
            int priv_field = (int) mh.invoke();
            priv_field = (int) mh.invokeExact();
            mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
            mh.invoke(2);
            mh.invokeExact(3);
        }
        default void access_priv(StaticNested o) throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
            int priv_field = (int) mh.invoke();
            priv_field = (int) mh.invokeExact();
            mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
            mh.invoke(2);
            mh.invokeExact(3);
        }
    }

    static class StaticNested {

        private static int priv_field;

        // public constructor so we aren't relying on private access
        public StaticNested() {}

        // Methods that will access private static fields of nestmates

        void access_priv(TestMethodHandles o) throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
            priv_field = (int) mh.invoke();
            priv_field = (int) mh.invokeExact();
            mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
            mh.invoke(2);
            mh.invokeExact(3);
        }
        void access_priv(StaticNested o) throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
            priv_field = (int) mh.invoke();
            priv_field = (int) mh.invokeExact();
            mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
            mh.invoke(2);
            mh.invokeExact(3);
        }
    }

    class InnerNested {

        // public constructor so we aren't relying on private access
        public InnerNested() {}

        void access_priv(TestMethodHandles o) throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
            priv_field = (int) mh.invoke();
            priv_field = (int) mh.invokeExact();
            mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
            mh.invoke(2);
            mh.invokeExact(3);
        }
        void access_priv(StaticNested o) throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(o.getClass(), "priv_field", int.class);
            priv_field = (int) mh.invoke();
            priv_field = (int) mh.invokeExact();
            mh = lookup().findStaticSetter(o.getClass(), "priv_field", int.class);
            mh.invoke(2);
            mh.invokeExact(3);
        }
    }

    public static void main(String[] args) throws Throwable {
        TestMethodHandles o = new TestMethodHandles();
        StaticNested s = new StaticNested();
        InnerNested i = o.new InnerNested();
        StaticIface intf = new StaticIface() {};

        o.access_priv(new TestMethodHandles());
        o.access_priv(s);

        s.access_priv(o);
        s.access_priv(new StaticNested());

        i.access_priv(o);
        i.access_priv(s);

        intf.access_priv(o);
        intf.access_priv(s);
    }
}
