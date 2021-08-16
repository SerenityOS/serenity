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
 * @summary Test access to private fields between nestmates and nest-host
 *          using different flavours of named nested types using core reflection
 * @run main TestReflection
 */

import java.lang.reflect.Field;

public class TestReflection {

    // Private field of nest-host for nestmates to access
    private int priv_field;

    // public constructor so we aren't relying on private access
    public TestReflection() {}

    // Methods that will access private fields of nestmates

    // NOTE: No StaticIface variants as interfaces can't have instance fields

    void access_priv(TestReflection o) throws Throwable {
        Field f = o.getClass().getDeclaredField("priv_field");
        this.priv_field = f.getInt(o);
        f.setInt(o, 2);
    }
    void access_priv(InnerNested o) throws Throwable {
        Field f = o.getClass().getDeclaredField("priv_field");
        this.priv_field = f.getInt(o);
        f.setInt(o, 2);
    }
    void access_priv(StaticNested o) throws Throwable {
        Field f = o.getClass().getDeclaredField("priv_field");
        this.priv_field = f.getInt(o);
        f.setInt(o, 2);
    }

    // The various nestmates

    static interface StaticIface {

        // Methods that will access private fields of nestmates

        default void access_priv(TestReflection o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            int priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
        default void access_priv(InnerNested o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            int priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
        default void access_priv(StaticNested o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            int priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
    }

    static class StaticNested {

        private int priv_field;

        // public constructor so we aren't relying on private access
        public StaticNested() {}

        // Methods that will access private fields of nestmates

        void access_priv(TestReflection o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            this.priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
        void access_priv(InnerNested o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            this.priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
        void access_priv(StaticNested o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            this.priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
    }

    class InnerNested {

        private int priv_field;

        // public constructor so we aren't relying on private access
        public InnerNested() {}

        void access_priv(TestReflection o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            this.priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
        void access_priv(InnerNested o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            this.priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
        void access_priv(StaticNested o) throws Throwable {
            Field f = o.getClass().getDeclaredField("priv_field");
            this.priv_field = f.getInt(o);
            f.setInt(o, 2);
        }
    }

    public static void main(String[] args) throws Throwable {
        TestReflection o = new TestReflection();
        StaticNested s = new StaticNested();
        InnerNested i = o.new InnerNested();
        StaticIface intf = new StaticIface() {};

        o.access_priv(new TestReflection());
        o.access_priv(i);
        o.access_priv(s);

        s.access_priv(o);
        s.access_priv(i);
        s.access_priv(new StaticNested());

        i.access_priv(o);
        i.access_priv(o.new InnerNested());
        i.access_priv(s);

        intf.access_priv(o);
        intf.access_priv(i);
        intf.access_priv(s);
    }
}
