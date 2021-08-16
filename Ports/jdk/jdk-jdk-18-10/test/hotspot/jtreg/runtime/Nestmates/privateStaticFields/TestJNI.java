/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test JNI access to private static fields between nestmates and nest-host
 *          using different flavours of named nested types
 * @compile ../NestmatesJNI.java
 * @run main/othervm/native TestJNI
 * @run main/othervm/native -Xcheck:jni TestJNI
 */
public class TestJNI {

    // Private field of nest-host for nestmates to access
    private static int priv_field;

    static final String FIELD = "priv_field";

    // public constructor so we aren't relying on private access
    public TestJNI() {}

    // Methods that will access private static fields of nestmates

    // NOTE: No InnerNested calls in this test because non-static nested types
    // can't have static fields. Also no StaticIface calls as static interface
    // fields must be public (and final)

    void access_priv(TestJNI o) {
        priv_field = getAndInc(o.getClass(), FIELD);
    }
    void access_priv(StaticNested o) {
        priv_field = getAndInc(o.getClass(), FIELD);
    }

    // The various nestmates

    static interface StaticIface {

        // Methods that will access private static fields of nestmates

        default void access_priv(TestJNI o) {
            int priv_field = getAndInc(o.getClass(), FIELD);
        }
        default void access_priv(StaticNested o) {
            int priv_field = getAndInc(o.getClass(), FIELD);
        }
    }

    static class StaticNested {

        private static int priv_field;

        // public constructor so we aren't relying on private access
        public StaticNested() {}

        // Methods that will access private static fields of nestmates

        void access_priv(TestJNI o) {
            priv_field = getAndInc(o.getClass(), FIELD);
        }
        void access_priv(StaticNested o) {
            priv_field = getAndInc(o.getClass(), FIELD);
        }
    }

    class InnerNested {

        // public constructor so we aren't relying on private access
        public InnerNested() {}

        void access_priv(TestJNI o) {
            int priv_field = getAndInc(o.getClass(), FIELD);
        }
        void access_priv(StaticNested o) {
            int priv_field = getAndInc(o.getClass(), FIELD);
        }
    }

    public static void main(String[] args) {
        TestJNI o = new TestJNI();
        StaticNested s = new StaticNested();
        InnerNested i = o.new InnerNested();
        StaticIface intf = new StaticIface() {};

        o.access_priv(new TestJNI());
        o.access_priv(s);

        s.access_priv(o);
        s.access_priv(new StaticNested());

        i.access_priv(o);
        i.access_priv(s);

        intf.access_priv(o);
        intf.access_priv(s);
    }

    static int getAndInc(Class<?> klass, String field) {
        String definingClass = klass.getName();
        String desc = "Access to static field " + definingClass + "." + field;
        int first, second;
        try {
            first = NestmatesJNI.getStaticIntField(definingClass, field);
            NestmatesJNI.setStaticIntField(definingClass, field, (first + 1));
            second = NestmatesJNI.getStaticIntField(definingClass, field);
        }
        catch (Throwable t) {
            throw new Error(desc + ": Unexpected exception: " + t, t);
        }
        if (second != first + 1) {
            throw new Error(desc + ": wrong field values: first=" + first +
                            ", second=" + second + " (should equal first+1)");
        }
        System.out.println(desc + " - passed");
        return first;
    }
}
