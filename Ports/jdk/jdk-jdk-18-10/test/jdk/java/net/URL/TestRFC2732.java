/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4440955
 * @summary toExternalForm() , toString() 's results format is
 *          not proper with IPv6 host nam
 */

import java.net.*;

public class TestRFC2732 {

    public static void main(String[] args) {
        URL u = null;
        try {
            u = new URL("http", "10:100::1234", 99, "/index");

            if (!u.toString().equals("http://[10:100::1234]:99/index") ||
                !u.toExternalForm().equals("http://[10:100::1234]:99/index")) {
                throw new Exception("Failed test for RFC 2732");
            }
            System.out.println("Test passed!");
        } catch(Exception exp) {
            throw new RuntimeException(exp.getMessage() +
                                       "Expect: " +
                                       "http://[10:100::1234]:99/index" +
                                       " Got: " + u.toExternalForm());
        }
    }
}
