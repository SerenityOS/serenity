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
 * @summary Test interface method selection process for private/public nestmate invocation
 * @compile TestInterfaceMethodSelection.java
 * @compile PA_I.jcod \
 *          PB_A_I.jcod \
 *          PB_A_PI.jcod \
 *          PB_PA_I.jcod
 * @run main TestInterfaceMethodSelection
 * @run main/othervm -Dsun.reflect.noInflation=true TestInterfaceMethodSelection
 */

// The first run will use NativeMethodAccessor and due to the limited number
// of calls we will not reach the inflation threshold.
// The second run disables inflation so we will use the GeneratedMethodAccessor
// instead. In this way both sets of Reflection classes are tested.

/*
We are setting up a basic test structure as follows:

interface I {
  ?? String m() [ return "I::m"; // private case]
}
class A implements I {
  ?? String m() { return "A::m"; }
}
class B extends A {
  ?? String m() { return "B::m"; }
}

where the access modifier of m() is either public or private in all combinations.
The only cases of interest here are private and non-private, so we use public for
the non-private case. Obviously for an interface, only the private case defines
a method body for m() - we're not testing default methods here (but they would be
invoked in the cases where we get AME).

We then have a test function:

void test(I target, String expected) {
  check(target.m() == expected);
}

where the call to target.m() is expressed as an invokeinterface I::m on target. We
then pass either an A instance or a B instance and check the expected method
is invoked. In all cases the resolved method is I::m, so we are effectively
testing the method selection rules. We are not testing resolution here.

The expected behaviour is as follows (where P means m() is private and - means
m() is public).

Target  I.m  A.m  B.m     Result   Reason
------------------------------------------
 A       P    P   n/a      I.m       [1]
 A       P    -   n/a      I.m       [1]
 A       -    P   n/a      AME       [2]
 A       -    -   n/a      A.m       [3]
 B       P    P    P       I.m       [1]
 B       P    P    -       I.m       [1]
 B       P    -    P       I.m       [1]
 B       P    -    -       I.m       [1]
 B       -    P    P       AME       [2]
 B       -    P    -       B.m       [3]
 B       -    -    P       A.m       [4]
 B       -    -    -       B.m       [3]

[1] Resolved method is private => selected method == resolved method
[2] private A.m/B.m doesn't override abstract public I.m => AbstractMethodError
[3] Normal overriding: most specific method selected
[4] private B.m doesn't override public A.m/I.m so is ignored => A.m selected

To allow us to do this in source code we encode the inheritance hierarchy in the
class name, and we use plain I (for example) when m() is public and PI when m()
is private. So class B_A_I defines a public m() and inherits public m() from
both A and I. While PB_PA_PI defines a private m() and also has private m()
defined in its superclass PA and implemented interface PI.

For cases where the subclass makes a public method private we can't write this
directly in Java source code so we have to have jcod versions that change
the access modifier to private, but we also need to switch between having a
method implementation or not, so add fake_m() and then rename in the jcod file.
The affected cases are:

- PA_I
- PB_A_I
- PB_PA_I
- PB_A_PI

We test direct invocation from Java source, MethodHandle invocation and core
reflection invocation. For MH and reflection we look for the method in "I" to
maintain the same resolution process as in the direct case.
*/

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import java.lang.reflect.InvocationTargetException;

public class TestInterfaceMethodSelection {

    static final MethodType M_T = MethodType.methodType(String.class);

    static interface I {
        public String m();
    }
    static interface PI {
        private String m() { return "PI::m"; }
    }

    static class A_I implements I {
        public String m() { return "A_I::m"; }
    }
    static class A_PI implements PI {
        public String m() { return "A_PI::m"; }
    }
    // jcod version will edit method names
    static class PA_I implements I {
        private String real_m() { return "PA_I::m"; }
        public String m() { return "Should not see this"; }
    }
    static class PA_PI implements PI {
        private String m() { return "PA_PI::m"; }
    }

    static class B_A_I extends A_I {
        public String m() { return "B_A_I::m"; }
    }
    // jcod version will rewrite this to have private m()
    static class PB_A_I extends A_I {
        public String m() { return "PB_A_I::m"; }
    }
    static class B_PA_I extends PA_I {
        public String m() { return "B_PA_I::m"; }
    }
    // jcod version will edit method names
    static class PB_PA_I extends PA_I {
        public String m() { return "Should not see this"; }
        private String real_m() { return "PB_PA_I"; }
    }
    static class B_A_PI extends A_PI {
        public String m() { return "B_A_PI::m"; }
    }
    // jcod version will rewrite this to have private m()
    static class PB_A_PI extends A_PI {
        public String m() { return "PB_A_PI"; }
    }
    static class B_PA_PI extends PA_PI {
        public String m() { return "B_PA_PI::m"; }
    }
    static class PB_PA_PI extends PA_PI {
        private String m() { return "PB_PA_PI::m"; }
    }

    // Need a test function for each of the "I" interfaces

    static void doInvoke(I target, String expected) throws Throwable {
        // Direct
        check(target.m(), expected);
        // MethodHandle
        MethodHandle mh = lookup().findVirtual(I.class, "m", M_T);
        check((String)mh.invoke(target), expected);
        // Reflection
        check((String)I.class.getDeclaredMethod("m", new Class<?>[0]).
              invoke(target, new Object[0]), expected);
    }
    static void doInvoke(PI target, String expected) throws Throwable {
        // Direct
        check(target.m(), expected);
        // MethodHandle
        MethodHandle mh = lookup().findVirtual(PI.class, "m", M_T);
        check((String)mh.invoke(target), expected);
        // Reflection
        check((String)PI.class.getDeclaredMethod("m", new Class<?>[0]).
              invoke(target, new Object[0]), expected);
    }

    static void badInvoke(I target) {
        badDirectInvoke(target);
        badMHInvoke(target);
        badReflectInvoke(target);
    }

    static void badDirectInvoke(I target) {
        try {
            target.m();
            throw new Error("Unexpected success directly invoking " +
                            target.getClass().getSimpleName() +
                            ".m() - expected AbstractMethodError");
        }
        catch (AbstractMethodError expected) {
        }
        catch (Throwable t) {
            throw new Error("Unexpected exception directly invoking " +
                            target.getClass().getSimpleName() +
                            ".m() - expected AbstractMethodError got: " + t);
        }
    }

    static void badMHInvoke(I target) {
        try {
            lookup().findVirtual(I.class, "m", M_T).invoke(target);
            throw new Error("Unexpected success for MH invoke of" +
                            target.getClass().getSimpleName() +
                            ".m() - expected AbstractMethodError");
        }
        catch (AbstractMethodError expected) {
        }
        catch (Throwable t) {
            throw new Error("Unexpected exception for MH invoke of " +
                            target.getClass().getSimpleName() +
                            ".m() - expected AbstractMethodError got: " + t);
        }
    }

    static void badReflectInvoke(I target) {
        try {
            I.class.getDeclaredMethod("m", new Class<?>[0]).
                invoke(target, new Object[0]);
            throw new Error("Unexpected success for Method invoke of" +
                            target.getClass().getSimpleName() +
                            ".m() - expected AbstractMethodError");
        }
        catch (InvocationTargetException expected) {
            Throwable t = expected.getCause();
            if (!(t instanceof AbstractMethodError)) {
                throw new Error("Unexpected exception for Method invoke of " +
                                target.getClass().getSimpleName() +
                                ".m() - expected AbstractMethodError got: " + t);

            }
        }
        catch (Throwable t) {
            throw new Error("Unexpected exception for Method invoke of " +
                            target.getClass().getSimpleName() +
                            ".m() - expected AbstractMethodError got: " + t);
        }
    }

    static void check(String actual, String expected) {
        if (!actual.equals(expected)) {
                throw new Error("Selection error: expected " + expected +
                                " but got " + actual);
        }
    }

    public static void main(String[] args) throws Throwable {
        // First pass a suitable "A" instance
        doInvoke(new PA_PI(), "PI::m");
        doInvoke(new A_PI(),  "PI::m");
        badInvoke(new PA_I());
        doInvoke(new A_I(),   "A_I::m");
        // Now a "B" instance
        doInvoke(new PB_PA_PI(), "PI::m");
        doInvoke(new B_PA_PI(),  "PI::m");
        doInvoke(new PB_A_PI(),  "PI::m");
        doInvoke(new B_A_PI(),   "PI::m");
        badInvoke(new PB_PA_I());
        doInvoke(new B_PA_I(),   "B_PA_I::m");
        doInvoke(new PB_A_I(),   "A_I::m");
        doInvoke(new B_A_I(),    "B_A_I::m");
    }
}




