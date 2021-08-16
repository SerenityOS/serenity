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
 * @summary Test access to private constructors between nestmates and nest-host
 *          using different flavours of named nested types using core reflection
 * @run main TestReflection
 * @run main/othervm -Dsun.reflect.noInflation=true TestReflection
 */

// The first run will use NativeConstructorAccessor and due to the limited
// number of calls we will not reach the inflation threshold.
// The second run disables inflation so we will use the
// GeneratedConstructorAccessor instead.
// In this way both sets of Reflection classes are tested.

public class TestReflection {

    // All constructors are private to ensure nestmate access checks apply

    // All doConstruct methods are public so they don't involve invoke_special

    private TestReflection() {}

    // The various nestmates

    // Note: No constructor on interfaces so no StaticIface variants

    static interface StaticIface {

        // Methods that will access private constructors of nestmates.

        default void doConstruct(TestReflection o) throws Throwable {
          Object obj = o.getClass().getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
        default void doConstruct(TestReflection tr, InnerNested o) throws Throwable {
          Object obj = InnerNested.class.getDeclaredConstructor(new Class<?>[] {TestReflection.class}).newInstance(new Object[] { tr });
        }
        default void doConstruct(StaticNested o) throws Throwable {
          Object obj = o.getClass().getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
    }

    static class StaticNested {

        private StaticNested() {}

        // Methods that will access private constructors of nestmates.
        // The arg is a dummy for overloading purposes

        public void doConstruct(TestReflection o) throws Throwable {
          Object obj = o.getClass().getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
        public  void doConstruct(TestReflection tr, InnerNested o) throws Throwable {
          Object obj = InnerNested.class.getDeclaredConstructor(new Class<?>[] {TestReflection.class}).newInstance(new Object[] { tr });
        }
        public void doConstruct(StaticNested o) throws Throwable {
          Object obj = o.getClass().getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
    }

    class InnerNested {

        private InnerNested() {}

        // Methods that will access private constructors of nestmates.
        // The arg is a dummy for overloading purposes

        public void doConstruct(TestReflection o) throws Throwable {
          Object obj = o.getClass().getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
        public  void doConstruct(TestReflection tr, InnerNested o) throws Throwable {
          Object obj = InnerNested.class.getDeclaredConstructor(new Class<?>[] {TestReflection.class}).newInstance(new Object[] { tr });
        }
        public void doConstruct(StaticNested o) throws Throwable {
          Object obj = o.getClass().getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
    }

    public static void main(String[] args) throws Throwable {
        // These initial constructions test nest-host access
        TestReflection o = TestReflection.class.getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        StaticNested s = StaticNested.class.getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        InnerNested i = InnerNested.class.getDeclaredConstructor(new Class<?>[] {TestReflection.class}).newInstance(new Object[] { o });

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
}
