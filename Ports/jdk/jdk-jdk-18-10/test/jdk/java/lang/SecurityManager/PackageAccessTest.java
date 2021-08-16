/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4256530 4136245
 * @summary Check that restricted packages that are supposed to be restricted
 *          and explicit grants accessClassInPackage permission overridden in
 *          privileged block
 * @modules java.base/sun.security.x509
 * @run main/othervm PackageAccessTest
 * @run main/othervm/java.security.policy=test.policy PackageAccessTest access
 * @run main/othervm/java.security.policy=empty.policy PackageAccessTest deny
 */

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

public class PackageAccessTest {

    public static void main(String[] args) {
        boolean access = true;

        if (args != null && args.length > 0) {
            switch (args[0]) {
                case "access":
                    access = true;
                    break;
                case "deny":
                    access = false;
                    break;
                default:
                    throw new RuntimeException(
                            "Invalid input parameter " + args[0]);
            }
        }

        testPkgAccess(access);
        testPkgAccessWithPrivileged(access);
    }

    private static void testPkgAccess(boolean access) {
        try {
            sun.security.x509.X509CertInfo x = new sun.security.x509.X509CertInfo();
            if (!access) {
                throw new RuntimeException(
                        "application unexpectedly able to access the internal package");
            }
        } catch (SecurityException se) {
            if (access) {
                throw new RuntimeException("Unexpected security exception", se);
            }
        }
    }

    private static void testPkgAccessWithPrivileged(boolean access) {
        sun.security.x509.X509CertInfo o = null;
        try {
            o = (sun.security.x509.X509CertInfo) AccessController.doPrivileged(
                    (PrivilegedExceptionAction) () -> new sun.security.x509.X509CertInfo());
            if (!access) {
                throw new RuntimeException(
                        "application unexpectedly able to access the internal package");
            }
        } catch (AccessControlException ace) {
            if (access) {
                throw new RuntimeException("Unexpected AccessControlException", ace);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Test failed with unexpected exception", ex);
        }
        if (access && o == null)
            throw new RuntimeException(
                    "Test failed: unable to instantiate object");
    }

}
