/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4925226
 * @summary ensure IllegalArgumentException is thrown if in == out
 * @author Andreas Sterbenz
 */

import java.nio.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class InOutBuffers {

    public static void main(String[] args) throws Exception {
        Cipher c = Cipher.getInstance("RC4");
        SecretKey key = new SecretKeySpec(new byte[16], "RC4");
        c.init(Cipher.ENCRYPT_MODE, key);
        ByteBuffer b = ByteBuffer.allocate(16);
        b.putInt(0x12345678);
        try {
            c.update(b, b);
            throw new Exception("Unexpectedly completed call");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
        b.flip();
        try {
            c.doFinal(b, b);
            throw new Exception("Unexpectedly completed call");
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
        System.out.println("Passed");
    }
}
