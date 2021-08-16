/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4819194
 * @summary doPrivileged should preserve DomainCombiner
 */

import java.security.*;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

public class PreserveCombiner {

    public static void main(String[] args) throws Exception {

        Subject s = new Subject();
        s.getPrincipals().add(new X500Principal("cn=duke"));

        String result = (String)Subject.doAs(s, new PrivilegedAction() {
            public Object run() {

                // get subject from current ACC - this always worked
                Subject doAsSubject =
                        Subject.getSubject(AccessController.getContext());
                if (doAsSubject == null) {
                    return "test 1 failed";
                } else {
                    System.out.println(doAsSubject);
                    System.out.println("test 1 passed");
                }

                // try doPriv (PrivilegedAction) test
                String result = AccessController.doPrivilegedWithCombiner
                    (new PrivilegedAction<String>() {
                    public String run() {
                        // get subject after doPriv
                        Subject doPrivSubject =
                            Subject.getSubject(AccessController.getContext());
                        if (doPrivSubject == null) {
                            return "test 2 failed";
                        } else {
                            System.out.println(doPrivSubject);
                            return "test 2 passed";
                        }
                    }
                });

                if ("test 2 failed".equals(result)) {
                    return result;
                } else {
                    System.out.println(result);
                }

                // try doPriv (PrivilegedExceptionAction) test
                try {
                    result = AccessController.doPrivilegedWithCombiner
                        (new PrivilegedExceptionAction<String>() {
                        public String run() throws PrivilegedActionException {
                            // get subject after doPriv
                            Subject doPrivSubject = Subject.getSubject
                                (AccessController.getContext());
                            if (doPrivSubject == null) {
                                return "test 3 failed";
                            } else {
                                System.out.println(doPrivSubject);
                                return "test 3 passed";
                            }
                        }
                    });
                } catch (PrivilegedActionException pae) {
                    result = "test 3 failed";
                }

                if ("test 3 failed".equals(result)) {
                    return result;
                } else {
                    System.out.println(result);
                }

                // tests passed
                return result;
            }
        });

        if (result.indexOf("passed") <= 0) {
            throw new SecurityException("overall test failed");
        }
    }
}
