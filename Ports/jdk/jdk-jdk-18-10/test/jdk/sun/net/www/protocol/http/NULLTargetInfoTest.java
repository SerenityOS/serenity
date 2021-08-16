/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8151788
 * @summary NullPointerException from ntlm.Client.type3
 * @modules java.base/com.sun.security.ntlm
 * @run main NULLTargetInfoTest
 */
import com.sun.security.ntlm.Client;

public class NULLTargetInfoTest {

    public static void main(String[] args) throws Exception {
        Client c = new Client(null, "host", "user", "domain", "pass".toCharArray());
        c.type1();
        // this input does have the 0x800000 bit(NTLMSSP_NEGOTIATE_TARGET_INFO) set
        // but after offset 40 all eight bytes are all zero which means there is no
        // security buffer for target info.
        byte[] type2 = hex(
                "4E 54 4C 4D 53 53 50 00 02 00 00 00 00 00 00 00"
                + "00 00 00 00 05 82 89 00 0B 87 81 B6 2D 6E 8B C1"
                + "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
        byte[] nonce = new byte[10];
        c.type3(type2, nonce);
    }

    private static byte[] hex(String str) {
        str = str.replaceAll("\\s", "");
        byte[] response = new byte[str.length() / 2];
        int index = 0;
        for (int i = 0; i < str.length(); i += 2) {
            response[index++] = Integer.valueOf(str.substring(i, i + 2), 16).byteValue();
        }
        return response;
    }
}
