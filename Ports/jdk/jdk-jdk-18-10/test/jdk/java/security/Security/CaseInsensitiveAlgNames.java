/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4093855
 * @summary Make sure algorithm names provided to getInstance() are
 * treated case-insensitive
 */

import java.security.*;

public class CaseInsensitiveAlgNames {

    public static void main(String[] args)
        throws NoSuchAlgorithmException, NoSuchProviderException {
            // MessageDigest without provider
            MessageDigest md = MessageDigest.getInstance("SHA");
            md = MessageDigest.getInstance("sha");
            md = MessageDigest.getInstance("Sha-1");
            md = MessageDigest.getInstance("shA1");

            // MessageDigest with provider
            md = MessageDigest.getInstance("SHA", "SUN");
            md = MessageDigest.getInstance("sha", "SUN");
            md = MessageDigest.getInstance("Sha-1", "SUN");
            md = MessageDigest.getInstance("shA1", "SUN");

            // KeyPairGenerator without provider
            KeyPairGenerator kGen = KeyPairGenerator.getInstance("DSA");
            kGen = KeyPairGenerator.getInstance("dsa");
            kGen = KeyPairGenerator.getInstance("dSA");
            kGen = KeyPairGenerator.getInstance("OId.1.2.840.10040.4.1");
            kGen = KeyPairGenerator.getInstance("1.2.840.10040.4.1");

            // KeyPairGenerator with provider
            kGen = KeyPairGenerator.getInstance("DSA", "SUN");
            kGen = KeyPairGenerator.getInstance("dsa", "SUN");
            kGen = KeyPairGenerator.getInstance("dSA", "SUN");
            kGen = KeyPairGenerator.getInstance("OId.1.2.840.10040.4.1",
                                                "SUN");
            kGen = KeyPairGenerator.getInstance("1.2.840.10040.4.1", "SUN");
    }
}
