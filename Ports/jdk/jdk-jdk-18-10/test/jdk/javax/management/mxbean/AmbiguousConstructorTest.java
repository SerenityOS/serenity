/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6175517 6278707
 * @summary Test that ambiguous ConstructorProperties annotations are detected.
 * @author Eamonn McManus
 *
 * @run clean AmbiguousConstructorTest
 * @run build AmbiguousConstructorTest
 * @run main AmbiguousConstructorTest
 */

import javax.management.ConstructorParameters;
import javax.management.*;

public class AmbiguousConstructorTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();

        System.out.println("Unambiguous case:");
        ObjectName unambigName = new ObjectName("d:type=Unambiguous");
        mbs.registerMBean(new UnambiguousImpl(), unambigName);
        System.out.println("...OK");

        System.out.println("Ambiguous case:");
        ObjectName ambigName = new ObjectName("d:type=Ambiguous");
        boolean exception = false;
        try {
            mbs.registerMBean(new AmbiguousImpl(), ambigName);
        } catch (Exception e) {
            // TODO - check for the specific exception we should get.
            // Currently exception happens in preRegister so we have
            // RuntimeMBeanException -> IllegalArgumentException ->
            // InvalidObjectException, where the IllegalArgumentException
            // is thrown by MXSupport.MBeanDispatcher.visitAttribute when
            // it calls ConvertingMethod.checkCallFromOpen
            System.out.println("...OK, got expected exception:");
            e.printStackTrace(System.out);
            exception = true;
        }
        if (!exception) {
            System.out.println("TEST FAILED: expected exception, got none");
            throw new Exception("Did not get expected exception");
        }

        System.out.println("TEST PASSED");
    }

    public static class Unambiguous {
        public byte getA() {return 0;}
        public short getB() {return 0;}
        public int getC() {return 0;}
        public long getD() {return 0;}

        @ConstructorParameters({"a", "b"})
        public Unambiguous(byte a, short b) {}

        @ConstructorParameters({"b", "c"})
        public Unambiguous(short b, int c) {}

        @ConstructorParameters({"a", "b", "c"})
        public Unambiguous(byte a, short b, int c) {}
    }

    public static class Ambiguous {
        public byte getA() {return 0;}
        public short getB() {return 0;}
        public int getC() {return 0;}
        public long getD() {return 0;}

        @ConstructorParameters({"a", "b"})
        public Ambiguous(byte a, short b) {}

        @ConstructorParameters({"b", "c"})
        public Ambiguous(short b, int c) {}

        @ConstructorParameters({"a", "b", "c", "d"})
        public Ambiguous(byte a, short b, int c, long d) {}
    }

    public static interface UnambiguousMXBean {
        public void setUnambiguous(Unambiguous x);
    }

    public static class UnambiguousImpl implements UnambiguousMXBean {
        public void setUnambiguous(Unambiguous x) {}
    }

    public static interface AmbiguousMXBean {
        public void setAmbiguous(Ambiguous x);
    }

    public static class AmbiguousImpl implements AmbiguousMXBean {
        public void setAmbiguous(Ambiguous x) {}
    }
}
