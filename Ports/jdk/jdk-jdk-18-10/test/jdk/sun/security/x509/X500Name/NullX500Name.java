/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4118818
 * @summary allow null X.500 Names
 * @library /test/lib
 * @modules java.base/sun.security.util
 *          java.base/sun.security.x509
 */

import java.util.Arrays;
import sun.security.util.DerOutputStream;
import sun.security.x509.*;
import jdk.test.lib.hexdump.HexPrinter;

public class NullX500Name {

    public static void main(String[] argv) throws Exception {
        X500Name subject;
        String name = "";

        subject = new X500Name(name);
        System.out.println("subject:" + subject.toString());

        System.out.println("getCN:" + subject.getCommonName());

        System.out.println("getC:" + subject.getCountry());

        System.out.println("getL:" + subject.getLocality());

        System.out.println("getST:" + subject.getState());

        System.out.println("getName:" + subject.getName());

        System.out.println("getO:" + subject.getOrganization());

        System.out.println("getOU:" + subject.getOrganizationalUnit());

        System.out.println("getType:" + subject.getType());

        // encode, getEncoded()
        DerOutputStream dos = new DerOutputStream();
        subject.encode(dos);
        byte[] out = dos.toByteArray();
        byte[] enc = subject.getEncoded();
        HexPrinter e = HexPrinter.simple();
        if (Arrays.equals(out, enc))
            System.out.println("Success: out:" + e.toString(out));
        else {
            System.out.println("Failed: encode:" + e.toString(out));
            System.out.println("getEncoded:" + e.toString(enc));
        }
        X500Name x = new X500Name(enc);
        if (x.equals(subject))
            System.out.println("Success: X500Name(byte[]):" + x.toString());
        else
            System.out.println("Failed: X500Name(byte[]):" + x.toString());
    }
}
