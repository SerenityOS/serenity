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
 * @summary Test method selection process for private/public nestmate invocation
 * @compile TestMethodSelection.java
 * @compile PB_A.jcod \
 *          PC_B_A.jcod \
 *          PC_B_PA.jcod \
 *          PC_PB_A.jcod
 * @run main/othervm TestMethodSelection
 * @run main/othervm -Dsun.reflect.noInflation=true TestMethodSelection
 */

// The first run will use NativeMethodAccessor and due to the limited number
// of calls we will not reach the inflation threshold.
// The second run disables inflation so we will use the GeneratedMethodAccessor
// instead. In this way both sets of Reflection classes are tested.

/*
We are setting up a basic test structure as follows:

class A {
  ?? String m() { return "A::m"; }
}
class B extends A {
  ?? String m() { return "B::m"; }
}
class C extends B {
  ?? String m() { return "C::m"; }
}

where the access modifier of m() is either public or private in all combinations.
The only cases of interest here are private and non-private, so we use public for
the non-private case.

We then have a test function:

void test(B target, String expected) {
  check(target.m() == expected);
}

where the call to target.m() is expressed as an invokevirtual B::m on target. We
then pass either a B instance or a C instance and check that the expected method
is invoked. In all cases the resolved method is B::m, so we are effectively
testing the method selection rules. We are not testing resolution here.

The expected behaviour is as follows (where P means m() is private and - means
m() is public).

Target  A.m  B.m  C.m     Result   Reason
------------------------------------------
 B       P    P   n/a      B.m       [1]
 B       P    -   n/a      B.m       [2]
 B       -    P   n/a      B.m       [1]
 B       -    -   n/a      B.m       [2]
 C       P    P    P       B.m       [1]
 C       P    P    -       B.m       [1]
 C       P    -    P       B.m       [3]
 C       P    -    -       C.m       [2]
 c       -    P    P       B.m       [1]
 C       -    P    -       B.m       [1]
 C       -    -    P       B.m       [3]
 C       -    -    -       C.m       [2]

[1] Resolved method is private => selected method == resolved method
[2] target-type.m() can override B.m => selected method == target-type.m()
[3] private C.m does not override resolved public method B.m, but
    C has a superclass B, with B.m that (trivially) overrides resolved B.m
    => selected method = B.m

To allow us to do this in source code we encode the inheritance hierarchy in the
class name, and we use plain A (for example) when m() is public and PA when m()
is private. So class C_B_A defines a public m() and inherits public m() from
both B and A. While PC_PB_PA defines a private m() and also has private m()
defined in its superclasses PB and PA.

For cases where the subclass makes a public method private we can't write this
directly in Java source code so we have to have jcod versions that change
the access modifier to private. This occurs for:

- PC_B_A
- PB_A
- PC_B_PA
- PC_PB_A

We test direct invocation from Java source, MethodHandle invocation and core
reflection invocation. For MH and reflection we look for the method in "B" to
maintain the same resolution process as in the direct case.
*/

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import java.lang.reflect.InvocationTargetException;

public class TestMethodSelection {

    static final MethodType M_T = MethodType.methodType(String.class);

    static class A {
        public String m() { return "A::m"; }
    }
    static class PA {
        private String m() { return "PA::m"; }
    }

    static class B_A extends A {
        public String m() { return "B_A::m"; }
    }
    static class B_PA extends PA {
        public String m() { return "B_PA::m"; }
    }
    // jcod version will rewrite this to have private m()
    static class PB_A extends A {
        public String m() { return "PB_A::m"; }
    }
    static class PB_PA extends PA {
        private String m() { return "PB_PA::m"; }
    }

    static class C_B_A extends B_A {
        public String m() { return "C_B_A::m"; }
    }
    // jcod version will rewrite this to have private m()
    static class PC_B_A extends B_A {
        public String m() { return "PC_B_A"; }
    }
    static class C_PB_A extends PB_A {
        public String m() { return "C_PB_A::m"; }
    }
    // jcod version will rewrite this to have private m()
    static class PC_PB_A extends PB_A {
        public String m() { return "PC_PB_A"; }
    }
    static class C_B_PA extends B_PA {
        public String m() { return "C_B_PA::m"; }
    }
    // jcod version will rewrite this to have private m()
    static class PC_B_PA extends B_PA {
        public String m() { return "PC_B_PA"; }
    }
    static class C_PB_PA extends PB_PA {
        public String m() { return "C_PB_PA::m"; }
    }
    static class PC_PB_PA extends PB_PA {
        private String m() { return "PC_PB_PA::m"; }
    }

    // Need a test function for each of the "B" classes

    static void doInvoke(B_A target, String expected) throws Throwable {
        // Direct
        check(target.m(), expected);
        // MethodHandle
        MethodHandle mh = lookup().findVirtual(B_A.class, "m", M_T);
        check((String)mh.invoke(target), expected);
        // Reflection
        check((String)B_A.class.getDeclaredMethod("m", new Class<?>[0]).
              invoke(target, new Object[0]), expected);
    }
    static void doInvoke(B_PA target, String expected) throws Throwable {
        // Direct
        check(target.m(), expected);
        // MethodHandle
        MethodHandle mh = lookup().findVirtual(B_PA.class, "m", M_T);
        check((String)mh.invoke(target), expected);
        // Reflection
        check((String)B_PA.class.getDeclaredMethod("m", new Class<?>[0]).
              invoke(target, new Object[0]), expected);
    }
    static void doInvoke(PB_A target, String expected) throws Throwable {
        // Direct
        check(target.m(), expected);
        // MethodHandle
        MethodHandle mh = lookup().findVirtual(PB_A.class, "m", M_T);
        check((String)mh.invoke(target), expected);
        // Reflection
        check((String)PB_A.class.getDeclaredMethod("m", new Class<?>[0]).
              invoke(target, new Object[0]), expected);
    }
    static void doInvoke(PB_PA target, String expected) throws Throwable {
        // Direct
        check(target.m(), expected);
        // MethodHandle
        MethodHandle mh = lookup().findVirtual(PB_PA.class, "m", M_T);
        check((String)mh.invoke(target), expected);
        // Reflection
        check((String)PB_PA.class.getDeclaredMethod("m", new Class<?>[0]).
              invoke(target, new Object[0]), expected);
    }

    static void check(String actual, String expected) {
        if (!actual.equals(expected)) {
                throw new Error("Selection error: expected " + expected +
                                " but got " + actual);
        }
    }

    public static void main(String[] args) throws Throwable {
        // First pass a suitable "B" instance
        doInvoke(new PB_PA(), "PB_PA::m");
        doInvoke(new B_PA(),  "B_PA::m");
        doInvoke(new PB_A(),  "PB_A::m");
        doInvoke(new B_A(),   "B_A::m");
        // Now a "C" instance
        doInvoke(new PC_PB_PA(), "PB_PA::m");
        doInvoke(new C_PB_PA(),  "PB_PA::m");
        doInvoke(new PC_B_PA(),  "B_PA::m");
        doInvoke(new C_B_PA(),   "C_B_PA::m");
        doInvoke(new PC_PB_A(),  "PB_A::m");
        doInvoke(new C_PB_A(),   "PB_A::m");
        doInvoke(new PC_B_A(),   "B_A::m");
        doInvoke(new C_B_A(),    "C_B_A::m");
    }
}




