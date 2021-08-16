/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4894801
 * @summary Test that ObjectName.getInstance(ObjectName) preserves key order
 * @author Eamonn McManus
 *
 * @run clean ObjectNameGetInstanceTest
 * @run build ObjectNameGetInstanceTest
 * @run main ObjectNameGetInstanceTest
 */

import javax.management.*;

public class ObjectNameGetInstanceTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Test that ObjectName.getInstance(ObjectName) " +
                           "preserves the order of keys in its input");

        final String nonCanonical = "d:x=y,a=b";
        ObjectName nice = new ObjectName(nonCanonical);

        // Check that we are in fact using an ObjectName whose canonical
        // form is not the same as the input
        if (nice.getCanonicalName().equals(nonCanonical)) {
            System.err.println("TEST IS BROKEN: INPUT ALREADY CANONICAL");
            System.exit(1);
        }

        ObjectName evil = new EvilObjectName(nonCanonical);
        ObjectName unEvil = ObjectName.getInstance(evil);
        if (unEvil instanceof EvilObjectName) {
            System.err.println("FAILS: getInstance did not banish evil");
            System.exit(1);
        }

        if (nice.equals(unEvil))
            System.out.println("Test passes: original key order preserved");
        else {
            System.out.println("FAILS: key order changed");
            System.exit(1);
        }
    }

    public static class EvilObjectName extends ObjectName {
        public EvilObjectName(String s) throws MalformedObjectNameException {
            super(s);
        }

        public String getCanonicalName() {
            return "bogus:canonical=name";
        }
    }
}
