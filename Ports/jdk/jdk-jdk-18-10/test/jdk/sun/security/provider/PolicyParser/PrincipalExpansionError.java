/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4373996
 * @summary parser incorrectly ignores a principal if the principal name
 *      expands to nothing.
 * @run main/manual PrincipalExpansionError
 */

/*
 * This test is a bit complicated.
 * 1) PrincipalExpansionError.java
 *         the test itself.  this test creates a Subject with a
 *         UnixPrincipal("TestPrincipal") and calls doAs
 *         with a PrincipalExpansionErrorAction.
 * 2) PrincipalExpansionErrorAction
 *         this action tries to read the file, /testfile
 * 3) to run the test:
 *         a) jtreg -verbose:all -testjdk:<your_jdk>/build/sparc
 *                 PrincipalExpansionError.java
 *         b) PrincipalExpansionError is compiled and put into
 *                 the "test.classes" directory
 *         c) PrincipalExpansionErrorAction is compiled and put into
 *                 the "test.classes"/apackage directory
 *                 (since it belongs to the 'apackage' package
 *         d) the PrincipalExpansionError shell script moves
 *                 test.classes/apackage to test.src/apackage.
 *                 this guarantees that the test will run
 *                 with codebase test.classes, and the action
 *                 will run with codebase test.src.
 *         e) the test is executed.  permissions to read the file,
 *                 /testfile, were granted to the PrincipalExpansionError.
 *                 the policy entry for PrincipalExpansionErrorAction
 *                 running as UnixPrincipal("TestPrincipal")
 *                 was also granted the file permission,
 *                 but it has a bogus second UnixPrincipal with
 *                 a name that can't be property-expanded.
 *
 *                 the old behavior of the code would ignore the
 *                 bogus entry and incorrectly grants the file permission
 *                 to UnixPrincipal("TestPrincipal").
 *                 the new behavior correctly ignores the entire
 *                 policy entry.
 *                 Please note that the jtreg needs to be granted
 *                 allpermissions for this test to succeed. If the codebase
 *                 for jtreg changes, the PrincipalExpansionError.policy
 *                 needs to be updated.
 *         f) original @ tags:
 *                 compile PrincipalExpansionErrorAction.java
 *                 run shell PrincipalExpansionError.sh
 *                 run main/othervm/policy=PrincipalExpansionError.policy
 *                         -Djava.security.debug=access,domain,failure
 *                         PrincipalExpansionError
 */

import javax.security.auth.*;
import com.sun.security.auth.*;
import java.util.Set;
import apackage.PrincipalExpansionErrorAction;

public class PrincipalExpansionError {

    public static void main(String[] args) {

        Subject s = new Subject();

        try {
            Set principals = s.getPrincipals();
            principals.add(new UnixPrincipal("TestPrincipal"));
        } catch (SecurityException se) {
            // test incorrectly set up
            throw new SecurityException
                ("PrincipalExpansionError test incorrectly set up:" + se);
        }

        try {
            Subject.doAs(s, new PrincipalExpansionErrorAction());

            // test failed
            System.out.println("PrincipalExpansionError test failed");
            throw new SecurityException("PrincipalExpansionError test failed");

        } catch (java.security.PrivilegedActionException pae) {
            Exception e = pae.getException();

            if (e instanceof java.io.FileNotFoundException) {
                System.out.println
                    ("PrincipalExpansionError test failed (file not found)");
                java.io.FileNotFoundException fnfe =
                        (java.io.FileNotFoundException)e;
                throw new SecurityException("PrincipalExpansionError" +
                        "test failed (file not found)");
            } else {
                // i don't know???
                System.out.println("what happened?");
                pae.printStackTrace();
            }
        } catch (SecurityException se) {
                // good!  test succeeded
                System.out.println("PrincipalExpansionError test succeeded");
                se.printStackTrace();
        }
    }
}
