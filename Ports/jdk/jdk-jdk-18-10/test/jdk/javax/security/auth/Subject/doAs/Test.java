/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4399067 8179880
 * @summary Subject.doAs(null, action) does not clear the executing subject
 * @run main/othervm/policy=policy Test
 */
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.util.*;

public class Test {

    public static Subject get(String name) {

        return new Subject(true,
                Collections.singleton(new X500Principal(name)),
                new HashSet(),
                Collections.singleton(Boolean.TRUE));
    }

    public static void main(String[] args) {

        System.setSecurityManager(new SecurityManager());
        try {
            Subject.doAsPrivileged(get("CN=joe"), new PrivilegedAction() {
                public Object run() {
                    return Subject.doAs(null, new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty("foobar");
                        }
                    });
                }
            }, null);
            throw new RuntimeException(
                    "AccessControlException should have occcured");
        } catch (java.security.AccessControlException e) {
            // Expected exception occurred
            e.printStackTrace(System.out);
            System.out.println("Expected exception occurred");
        }
    }
}
