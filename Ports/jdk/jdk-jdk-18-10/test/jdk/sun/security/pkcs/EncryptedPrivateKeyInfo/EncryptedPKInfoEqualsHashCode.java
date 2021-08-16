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
 * @author Gary Ellison
 * @bug 4170635
 * @summary Verify equals()/hashCode() contract honored
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.x509
 */

import java.io.*;

import sun.security.x509.*;
import sun.security.pkcs.*;


public class EncryptedPKInfoEqualsHashCode {

    public static void main(String[] args) throws Exception {

        EncryptedPrivateKeyInfo ev1;
        EncryptedPrivateKeyInfo ev2;

        AlgorithmId dh = AlgorithmId.get("DH");

        byte key1[] = {
            (byte)0xD4,(byte)0xA0,(byte)0xBA,(byte)0x02,
            (byte)0x50,(byte)0xB6,(byte)0xFD,(byte)0x2E,
            (byte)0xC6,(byte)0x26,(byte)0xE7,(byte)0xEF,
            (byte)0xD6,(byte)0x37,(byte)0xDF,(byte)0x76,
            (byte)0xC7,(byte)0x16,(byte)0xE2,(byte)0x2D,
            (byte)0x09,(byte)0x44,(byte)0xB8,(byte)0x8B,
        };

        ev1 = new  EncryptedPrivateKeyInfo(dh, key1);
        ev2 = new  EncryptedPrivateKeyInfo(dh, key1);

        // the test
        if ( (ev1.equals(ev2)) == (ev1.hashCode()==ev2.hashCode()) )
            System.out.println("PASSED");
        else
            throw new Exception("Failed equals()/hashCode() contract");

    }
}
