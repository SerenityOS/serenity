/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary Basic tests for ObjectMethods
 * @run testng ObjectMethodsTest
 * @run testng/othervm/java.security.policy=empty.policy ObjectMethodsTest
 */

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import org.testng.annotations.Test;
import static java.lang.invoke.MethodType.methodType;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class ObjectMethodsTest {

    public static class C {
        static final MethodType EQUALS_DESC = methodType(boolean.class, C.class, Object.class);
        static final MethodType HASHCODE_DESC = methodType(int.class, C.class);
        static final MethodType TO_STRING_DESC = methodType(String.class, C.class);

        static final MethodHandle[] ACCESSORS = accessors();
        static final String NAME_LIST = "x;y";
        private static MethodHandle[] accessors() {
            try {
                return  new MethodHandle[]{
                        MethodHandles.lookup().findGetter(C.class, "x", int.class),
                        MethodHandles.lookup().findGetter(C.class, "y", int.class),
                };
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        private final int x;
        private final int y;
        C (int x, int y) { this.x = x; this.y = y; }
        public int x() { return x; }
        public int y() { return y; }
    }

    static class Empty {
        static final MethodType EQUALS_DESC = methodType(boolean.class, Empty.class, Object.class);
        static final MethodType HASHCODE_DESC = methodType(int.class, Empty.class);
        static final MethodType TO_STRING_DESC = methodType(String.class, Empty.class);
        static final MethodHandle[] ACCESSORS = new MethodHandle[] { };
        static final String NAME_LIST = "";
        Empty () {  }
    }

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public void testEqualsC() throws Throwable {
        CallSite cs = (CallSite)ObjectMethods.bootstrap(LOOKUP, "equals", C.EQUALS_DESC, C.class, C.NAME_LIST, C.ACCESSORS);
        MethodHandle handle = cs.dynamicInvoker();
        C c = new C(5, 5);
        assertTrue((boolean)handle.invokeExact(c, (Object)c));
        assertTrue((boolean)handle.invokeExact(c, (Object)new C(5, 5)));
        assertFalse((boolean)handle.invokeExact(c, (Object)new C(5, 4)));
        assertFalse((boolean)handle.invokeExact(c, (Object)new C(4, 5)));
        assertFalse((boolean)handle.invokeExact(c, (Object)null));
        assertFalse((boolean)handle.invokeExact(c, new Object()));
    }

    public void testEqualsEmpty() throws Throwable {
        CallSite cs = (CallSite)ObjectMethods.bootstrap(LOOKUP, "equals", Empty.EQUALS_DESC, Empty.class, Empty.NAME_LIST, Empty.ACCESSORS);
        MethodHandle handle = cs.dynamicInvoker();
        Empty e = new Empty();
        assertTrue((boolean)handle.invokeExact(e, (Object)e));
        assertTrue((boolean)handle.invokeExact(e, (Object)new Empty()));
        assertFalse((boolean)handle.invokeExact(e, (Object)null));
        assertFalse((boolean)handle.invokeExact(e, new Object()));
    }

    public void testHashCodeC() throws Throwable {
        CallSite cs = (CallSite)ObjectMethods.bootstrap(LOOKUP, "hashCode", C.HASHCODE_DESC, C.class, null, C.ACCESSORS);
        MethodHandle handle = cs.dynamicInvoker();
        C c = new C(6, 7);
        int hc = (int)handle.invokeExact(c);
        assertEquals(hc, hashCombiner(c.x(), c.y()));

        assertEquals((int)handle.invokeExact(new C(100, 1)),  hashCombiner(100, 1));
        assertEquals((int)handle.invokeExact(new C(0, 0)),    hashCombiner(0, 0));
        assertEquals((int)handle.invokeExact(new C(-1, 100)), hashCombiner(-1, 100));
        assertEquals((int)handle.invokeExact(new C(100, 1)),  hashCombiner(100, 1));
        assertEquals((int)handle.invokeExact(new C(100, -1)), hashCombiner(100, -1));
    }

    public void testHashCodeEmpty() throws Throwable {
        CallSite cs = (CallSite)ObjectMethods.bootstrap(LOOKUP, "hashCode", Empty.HASHCODE_DESC, Empty.class, "", Empty.ACCESSORS);
        MethodHandle handle = cs.dynamicInvoker();
        Empty e = new Empty();
        assertEquals((int)handle.invokeExact(e), 0);
    }

    public void testToStringC() throws Throwable {
        CallSite cs = (CallSite)ObjectMethods.bootstrap(LOOKUP, "toString", C.TO_STRING_DESC, C.class, C.NAME_LIST, C.ACCESSORS);
        MethodHandle handle = cs.dynamicInvoker();
        assertEquals((String)handle.invokeExact(new C(8, 9)),    "C[x=8, y=9]"   );
        assertEquals((String)handle.invokeExact(new C(10, 11)),  "C[x=10, y=11]" );
        assertEquals((String)handle.invokeExact(new C(100, -9)), "C[x=100, y=-9]");
        assertEquals((String)handle.invokeExact(new C(0, 0)),    "C[x=0, y=0]"   );
    }

    public void testToStringEmpty() throws Throwable {
        CallSite cs = (CallSite)ObjectMethods.bootstrap(LOOKUP, "toString", Empty.TO_STRING_DESC, Empty.class, Empty.NAME_LIST, Empty.ACCESSORS);
        MethodHandle handle = cs.dynamicInvoker();
        assertEquals((String)handle.invokeExact(new Empty()),    "Empty[]");
    }

    Class<NullPointerException> NPE = NullPointerException.class;
    Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    public void exceptions()  {
        assertThrows(IAE, () -> ObjectMethods.bootstrap(LOOKUP, "badName",  C.EQUALS_DESC,    C.class,         C.NAME_LIST, C.ACCESSORS));
        assertThrows(IAE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", C.TO_STRING_DESC, C.class,         "x;y;z",     C.ACCESSORS));
        assertThrows(IAE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", C.TO_STRING_DESC, C.class,         "x;y",       new MethodHandle[]{}));
        assertThrows(IAE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", C.TO_STRING_DESC, this.getClass(), "x;y",       C.ACCESSORS));

        assertThrows(IAE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", C.EQUALS_DESC,    C.class, "x;y", C.ACCESSORS));
        assertThrows(IAE, () -> ObjectMethods.bootstrap(LOOKUP, "hashCode", C.TO_STRING_DESC, C.class, "x;y", C.ACCESSORS));
        assertThrows(IAE, () -> ObjectMethods.bootstrap(LOOKUP, "equals",   C.HASHCODE_DESC,  C.class, "x;y", C.ACCESSORS));

        assertThrows(NPE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", C.TO_STRING_DESC, C.class, "x;y", null)       );
        assertThrows(NPE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", C.TO_STRING_DESC, C.class, null,  C.ACCESSORS));
        assertThrows(NPE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", C.TO_STRING_DESC, null,    "x;y", C.ACCESSORS));
        assertThrows(NPE, () -> ObjectMethods.bootstrap(LOOKUP, "equals",   C.EQUALS_DESC,    null,    "x;y", C.ACCESSORS));
        assertThrows(NPE, () -> ObjectMethods.bootstrap(LOOKUP, "hashCode", C.HASHCODE_DESC,  null,    "x;y", C.ACCESSORS));

        assertThrows(NPE, () -> ObjectMethods.bootstrap(LOOKUP, "toString", null,             C.class, "x;y", C.ACCESSORS));
        assertThrows(NPE, () -> ObjectMethods.bootstrap(LOOKUP, null,       C.TO_STRING_DESC, C.class, "x;y", C.ACCESSORS));
      //assertThrows(NPE, () -> ObjectMethods.bootstrap(null,   "toString", C.TO_STRING_DESC, C.class, "x;y", C.ACCESSORS));
    }

    // Based on the ObjectMethods internal implementation
    private static int hashCombiner(int x, int y) {
        return x*31 + y;
    }
}
