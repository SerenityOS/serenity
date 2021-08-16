/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import TVJar.TVPermission;
import java.security.AccessController;

/**
 * @test @bug 8050402
 * @summary Check policy is extensible with user defined permissions
 * @run main/othervm/policy=ExtensiblePolicyTest1.policy
 *      ExtensiblePolicyTest false
 * @run main/othervm/policy=ExtensiblePolicyTest2.policy
 *      ExtensiblePolicyTest true
 * @run main/othervm/policy=ExtensiblePolicyTest3.policy
 *      ExtensiblePolicyTest true
 */
public class ExtensiblePolicyTest {

    public static void main(String args[]) throws Throwable {
        // ExtensiblePolicyTest1.policy: policy file grants permission to
        // watch TVChannel 3-6
        // ExtensiblePolicyTest2.policy: policy file grants permission to
        // watch TVChanel 4
        // ExtensiblePolicyTest3.policy: policy file grants permission signed
        // by duke2 to watch TVChanel 5

        TVPermission perm = new TVPermission("channel:5", "watch");
        boolean getException = false;
        String exceptionMessage = null;
        boolean expectException = Boolean.parseBoolean(args[0]);
        try {
            AccessController.checkPermission(perm);
        } catch (SecurityException se) {
            getException = true;
            exceptionMessage = se.getMessage();
        }

        if (expectException ^ getException) {
            throw new RuntimeException("Test Failed: expectException = "
                    + expectException + " getException = " + getException
                    + "\n" + exceptionMessage);
        }
    }

}
