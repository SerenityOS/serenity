/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * @test
 * @bug 8051408
 * @library /test/lib
 * @summary testing HmacSHA512/224 and HmacSHA512/256.
 */
public class HmacSHA512 {
    public static void main(String[] args) throws Exception {

        Mac mac;

        // Test vectors obtained from
        // https://groups.google.com/d/msg/sci.crypt/OolWgsgQD-8/IUR2KhCcfEkJ
        mac = Mac.getInstance("HmacSHA512/224");
        mac.init(new SecretKeySpec(xeh("4a656665"), "HmacSHA512/224"));
        mac.update("what do ya want for nothing?".getBytes());
        Asserts.assertTrue(Arrays.equals(mac.doFinal(),
                xeh("4a530b31a79ebcce36916546317c45f247d83241dfb818fd37254bde")));

        mac = Mac.getInstance("HmacSHA512/256");
        mac.init(new SecretKeySpec(xeh("4a656665"), "HmacSHA512/256"));
        mac.update("what do ya want for nothing?".getBytes());
        Asserts.assertTrue(Arrays.equals(mac.doFinal(),
                xeh("6df7b24630d5ccb2ee335407081a87188c221489768fa2020513b2d593359456")));

        mac = Mac.getInstance("HmacSHA512/224");
        mac.init(new SecretKeySpec(xeh("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
                "HmacSHA512/224"));
        mac.update("Hi There".getBytes());
        Asserts.assertTrue(Arrays.equals(mac.doFinal(),
                xeh("b244ba01307c0e7a8ccaad13b1067a4cf6b961fe0c6a20bda3d92039")));

        mac = Mac.getInstance("HmacSHA512/256");
        mac.init(new SecretKeySpec(xeh("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
                "HmacSHA512/256"));
        mac.update("Hi There".getBytes());
        Asserts.assertTrue(Arrays.equals(mac.doFinal(),
                xeh("9f9126c3d9c3c330d760425ca8a217e31feae31bfe70196ff81642b868402eab")));
    }

    static byte[] xeh(String in) {
        in = in.replaceAll(" ", "");
        int len = in.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte)Integer.parseInt(in.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
