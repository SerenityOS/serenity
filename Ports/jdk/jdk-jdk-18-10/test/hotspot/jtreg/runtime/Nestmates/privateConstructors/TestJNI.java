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
 * @summary Test JNI access to private constructors between nestmates and nest-host
 *          using different flavours of named nested types using core reflection
 * @compile ../NestmatesJNI.java
 * @run main/othervm/native TestJNI
 * @run main/othervm/native -Xcheck:jni TestJNI
 */
public class TestJNI {

    // Unlike reflection, the calling context is not relevant to JNI
    // calls, but we keep the same structure as the reflection tests.


    // All constructors are private to ensure nestmate access checks apply

    // All doConstruct methods are public so they don't involve nestmate access

    private TestJNI() {}

    // The various nestmates

    // Note: No constructor on interfaces so no StaticIface variants

    static interface StaticIface {

        // Methods that will access private constructors of nestmates.

        default void doConstruct(TestJNI o) throws Throwable {
            Object obj = newInstance(o.getClass());
        }
        default void doConstruct(TestJNI outerThis, InnerNested o) throws Throwable {
            Object obj = newInstance(o.getClass(), outerThis);
        }
        default void doConstruct(StaticNested o) throws Throwable {
            Object obj = newInstance(o.getClass());
        }
    }

    static class StaticNested {

        private StaticNested() {}

        // Methods that will access private constructors of nestmates.
        // The arg is a dummy for overloading purposes

        public void doConstruct(TestJNI o) throws Throwable {
            Object obj = newInstance(o.getClass());
        }
        public  void doConstruct(TestJNI outerThis, InnerNested o) throws Throwable {
            Object obj = newInstance(o.getClass(), outerThis);
        }
        public void doConstruct(StaticNested o) throws Throwable {
            Object obj = newInstance(o.getClass());
        }
    }

    class InnerNested {

        private InnerNested() {}

        // Methods that will access private constructors of nestmates.
        // The arg is a dummy for overloading purposes

        public void doConstruct(TestJNI o) throws Throwable {
            Object obj = newInstance(o.getClass());
        }
        public  void doConstruct(TestJNI outerThis, InnerNested o) throws Throwable {
            Object obj = newInstance(o.getClass(), outerThis);
        }
        public void doConstruct(StaticNested o) throws Throwable {
            Object obj = newInstance(o.getClass());
        }
    }

    public static void main(String[] args) throws Throwable {
        // These initial constructions test nest-host access to members

        TestJNI o = newInstance(TestJNI.class);
        StaticNested s = (StaticNested) newInstance(StaticNested.class);
        InnerNested i = (InnerNested) newInstance(InnerNested.class, o);

        // We need a StaticIface instance to call doConstruct on
        StaticIface intf = new StaticIface() {};

        s.doConstruct(o);
        s.doConstruct(o, i);
        s.doConstruct(s);

        i.doConstruct(o);
        i.doConstruct(o, i);
        i.doConstruct(s);

        intf.doConstruct(o);
        intf.doConstruct(o, i);
        intf.doConstruct(s);
    }

    static <T> T newInstance(Class<T> klass) {
        return newInstance(klass, null);
    }

    static <T> T newInstance(Class<T> klass, Object outerThis) {
        String sig = (outerThis == null) ?
            "()V" :
            "(L" + outerThis.getClass().getName() + ";)V";
        String definingClass = klass.getName();
        String desc = " Invocation of constructor " + definingClass + sig;
        try {
            T ret = (T) NestmatesJNI.newInstance(definingClass, sig, outerThis);
            System.out.println(desc + " - passed");
            return ret;
        }
        catch (Throwable t) {
            throw new Error(desc + ": Unexpected exception: " + t, t);
        }
    }
}
