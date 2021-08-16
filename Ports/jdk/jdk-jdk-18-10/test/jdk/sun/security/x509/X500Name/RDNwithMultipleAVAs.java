/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4197911
 * @summary Make sure RDN with multiple AVAs can be parsed.
 * @modules java.base/sun.security.x509
 */

import java.io.*;
import sun.security.x509.*;

public class RDNwithMultipleAVAs {

    public static void main(String[] args) throws Exception {

        X500Name name = null;

        try {
            name = new X500Name("cn=john doe +   , c=us");
            throw new Exception("Expected IOException not thrown");
        } catch (IOException ioe) {
        }

        try {
            name = new X500Name("cn=john doe + l=ca\\++liformia, c=us");
            throw new Exception("Expected IOException not thrown");
        } catch (IOException ioe) {
        }

        name = new X500Name("cn=john \\+doe   , c=us");
        System.out.println(name);

        name = new X500Name("cn=john doe + l=ca\\+lifornia + l =sf, c=us");
        System.out.println(name);

        // single AVA w/ multiple "+" separators
        name = new X500Name("cn=john \\+doe + l=mpk, c=us");
        String loc = name.getLocality();
        if (loc == null || !loc.endsWith("mpk"))
            throw new Exception("AVA has been ignored");
    }
}
