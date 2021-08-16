/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6293767
 * @summary Test for the CommandAPDU class
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.util.*;

import javax.smartcardio.*;

public class TestCommandAPDU {

    private static final Random random = new Random();

    private static void test(int nc, int ne) throws Exception {
        System.out.println("nc = " + nc + ", ne = " + ne);
        byte[] data = new byte[nc];
        random.nextBytes(data);
        CommandAPDU apdu = new CommandAPDU((byte)0, (byte)0, (byte)0, (byte)0, data, ne);
        //System.out.println(apdu);
        if (apdu.getNc() != nc) {
            throw new Exception("dataLength does not match");
        }
        if (Arrays.equals(data, apdu.getData()) == false) {
            throw new Exception("data does not match");
        }
        if (apdu.getNe() != ne) {
            throw new Exception("ne does not match");
        }
        byte[] apduBytes = apdu.getBytes();
        CommandAPDU apdu2 = new CommandAPDU(apduBytes);
        //System.out.println(apdu2);
        if (Arrays.equals(apduBytes, apdu2.getBytes()) == false) {
            throw new Exception("apduBytes do not match");
        }
        if (apdu2.getNc() != nc) {
            throw new Exception("dataLength does not match");
        }
        if (Arrays.equals(data, apdu2.getData()) == false) {
            throw new Exception("data does not match");
        }
        if (apdu2.getNe() != ne) {
            throw new Exception("ne does not match");
        }
    }

    public static void main(String[] args) throws Exception {
        int[] t = {0, 42, 0x81, 255, 256, 4242, 0x8181, 65535, 65536};
        for (int nc : t) {
            if (nc == 65536) {
                continue;
            }
            for (int ne : t) {
                test(nc, ne);
            }
        }
        System.out.println("OK");
    }
}
