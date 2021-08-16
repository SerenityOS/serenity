/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6631361
 * @summary Test constructor when PD array is null or contains all null contexts
 */

import java.security.AccessControlContext;
import java.security.ProtectionDomain;

public class CheckCtor {

    public static void main(String[] args) throws Exception {

        // check that null PD array throws NPE
        try {
            new AccessControlContext(null);
            throw new Exception("Expected NullPointerException not thrown");
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                throw new Exception("Expected NullPointerException not thrown");
            }
        }

        // check that empty PD array equals PD array of one or more nulls
        ProtectionDomain zero[] = {};
        ProtectionDomain null1[] = {null};
        ProtectionDomain null2[] = {null, null};

        AccessControlContext accZero = new AccessControlContext(zero);
        AccessControlContext accNull1 = new AccessControlContext(null1);
        AccessControlContext accNull2 = new AccessControlContext(null2);

        testEquals(accZero, accNull1);
        testEquals(accZero, accNull2);
        testEquals(accNull1, accNull2);
        testEquals(accNull1, accZero);
        testEquals(accNull2, accZero);
        testEquals(accNull2, accNull1);
    }

    private static void testEquals(AccessControlContext acc1,
        AccessControlContext acc2) throws Exception {
        if (!acc1.equals(acc2)) {
            throw new Exception("AccessControlContexts should be equal");
        }
    }
}
