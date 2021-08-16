/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4184274
 * @summary Make sure bad distinguished names (without keywords) don't
 * cause out-of-memory condition
 * @modules java.base/sun.security.x509
 */
import java.io.IOException;
import sun.security.x509.X500Name;

public class BadName {

    public static void main(String args[]) throws Exception {
        try {
            // This used to throw java.lang.OutOfMemoryError, from which no
            // recovery is possible.
            // In the example below, the correct DN would be: "CN=John Doe"
            X500Name name = new X500Name("John Doe");
            System.out.println(name.toString());
        } catch (IOException ioe) {
        }
    }
}
