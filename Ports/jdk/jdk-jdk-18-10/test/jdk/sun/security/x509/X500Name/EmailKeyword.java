/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4531319
 * @summary Make sure AVA with EMAIL or EMAILADDRESS keyword is recognized
 *    in DEFAULT format
 * @modules java.base/sun.security.x509
 */
import sun.security.x509.X500Name;

public class EmailKeyword {

    public static void main(String[] arg) throws Exception {

        X500Name dN;
        dN = new X500Name("EMAIL=johndoe@example.com");
        System.out.println(dN.getName());
        dN = new X500Name("EMAILADDRESS=johndoe@example.com");
        System.out.println(dN.getName());
    }
}
