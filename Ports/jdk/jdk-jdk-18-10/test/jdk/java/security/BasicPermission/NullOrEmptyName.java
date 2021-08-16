/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4240252
 * @summary Make sure BasicPermission constructor raises
 * NullPointerException if permission name is null, and
 * IllegalArgumentException is permission name is empty.
 * @run main/othervm -Djava.security.manager=allow NullOrEmptyName
 */

public class NullOrEmptyName {

    public static void main(String[]args) throws Exception {
        NullOrEmptyName noe = new NullOrEmptyName();

        // run without sm installed
        noe.run();

        // run with sm installed
        SecurityManager sm = new SecurityManager();
        System.setSecurityManager(sm);
        noe.run();

        try {
            // called by System.getProperty()
            sm.checkPropertyAccess(null);
            throw new Exception("Expected NullPointerException not thrown");
        } catch (NullPointerException npe) {
            // expected exception thrown
        }

        try {
            // called by System.getProperty()
            sm.checkPropertyAccess("");
            throw new Exception("Expected IllegalArgumentException not " +
                                "thrown");
        } catch (IllegalArgumentException iae) {
            // expected exception thrown
        }
    }

    void run() throws Exception {

        try {
            System.getProperty(null);
            throw new Exception("Expected NullPointerException not " +
                                "thrown");
        } catch (NullPointerException npe) {
            // expected exception thrown
        }

        try {
            System.getProperty(null, "value");
            throw new Exception("Expected NullPointerException not " +
                                "thrown");
        } catch (NullPointerException npe) {
            // expected exception thrown
        }

        try {
            System.getProperty("");
            throw new Exception("Expected IllegalArgumentException not " +
                                "thrown");
        } catch (IllegalArgumentException iae) {
            // expected exception thrown
        }

        try {
            System.getProperty("", "value");
            throw new Exception("Expected IllegalArgumentException not " +
                                "thrown");
        } catch (IllegalArgumentException iae) {
            // expected exception thrown
        }

        try {
            System.setProperty(null, "value");
            throw new Exception("Expected NullPointerException not " +
                                "thrown");
        } catch (NullPointerException npe) {
            // expected exception thrown
        }

        try {
            System.setProperty("", "value");
            throw new Exception("Expected IllegalArgumentException not " +
                                "thrown");
        } catch (IllegalArgumentException iae) {
            // expected exception thrown
        }
    }
}
