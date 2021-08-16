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
 *          using different flavours of named nested types using MethodHandles
 * @run main TestMethodHandles
 */

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class TestMethodHandles {

  static final MethodType NOARG_T = MethodType.methodType(void.class);
  static final MethodType INNER_T = MethodType.methodType(void.class, TestMethodHandles.class);

    // All constructors are private to ensure nestmate access checks apply

    // All doConstruct methods are public so they don't involve invoke_special

    private TestMethodHandles() {}

    // The various nestmates

    // Note: No constructor on interfaces so no StaticIface variants

    static interface StaticIface {

        // Methods that will access private constructors of nestmates.
        // The arg is a dummy for overloading purposes

        default void doConstruct(TestMethodHandles o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(TestMethodHandles.class, NOARG_T);
            TestMethodHandles obj = (TestMethodHandles) mh.invoke();
            obj = (TestMethodHandles) mh.invokeExact();
        }
        default void doConstruct(TestMethodHandles outer, InnerNested o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(InnerNested.class, INNER_T);
            InnerNested obj = (InnerNested) mh.invoke(outer);
            obj = (InnerNested) mh.invokeExact(outer);
        }
        default void doConstruct(StaticNested o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(StaticNested.class, NOARG_T);
            StaticNested obj = (StaticNested) mh.invoke();
            obj = (StaticNested) mh.invokeExact();
        }
    }

    static class StaticNested {

        private StaticNested() {}

        // Methods that will access private constructors of nestmates.
        // The arg is a dummy for overloading purposes

        public void doConstruct(TestMethodHandles o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(TestMethodHandles.class, NOARG_T);
            TestMethodHandles obj = (TestMethodHandles) mh.invoke();
            obj = (TestMethodHandles) mh.invokeExact();
        }
        public  void doConstruct(TestMethodHandles outer, InnerNested o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(InnerNested.class, INNER_T);
            InnerNested obj = (InnerNested) mh.invoke(outer);
            obj = (InnerNested) mh.invokeExact(outer);
        }
        public void doConstruct(StaticNested o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(StaticNested.class, NOARG_T);
            StaticNested obj = (StaticNested) mh.invoke();
            obj = (StaticNested) mh.invokeExact();
        }
    }

    class InnerNested {

        private InnerNested() {}

        // Methods that will access private constructors of nestmates.
        // The arg is a dummy for overloading purposes

        public void doConstruct(TestMethodHandles o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(TestMethodHandles.class, NOARG_T);
            TestMethodHandles obj = (TestMethodHandles) mh.invoke();
            obj = (TestMethodHandles) mh.invokeExact();
        }
        public  void doConstruct(TestMethodHandles outer, InnerNested o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(InnerNested.class, INNER_T);
            InnerNested obj = (InnerNested) mh.invoke(outer);
            obj = (InnerNested) mh.invokeExact(outer);
        }
        public void doConstruct(StaticNested o) throws Throwable {
            MethodHandle mh =
              lookup().findConstructor(StaticNested.class, NOARG_T);
            StaticNested obj = (StaticNested) mh.invoke();
            obj = (StaticNested) mh.invokeExact();
        }
    }

    public static void main(String[] args) throws Throwable {
        // These initial constructions test nest-host access
        MethodHandle mh =
          lookup().findConstructor(TestMethodHandles.class, NOARG_T);
        TestMethodHandles o = (TestMethodHandles) mh.invoke();
        o = (TestMethodHandles) mh.invokeExact();

        mh = lookup().findConstructor(StaticNested.class, NOARG_T);
        StaticNested s = (StaticNested) mh.invoke();
        s = (StaticNested) mh.invokeExact();

        mh = lookup().findConstructor(InnerNested.class, INNER_T);
        InnerNested i = (InnerNested) mh.invoke(o);
        i = (InnerNested) mh.invokeExact(o);

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
