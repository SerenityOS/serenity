/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.String;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static java.lang.System.out;

/*
 * @test
 * @bug 4686632 8048610
 * @summary  To verify Cipher.init will throw InvalidKeyException with
 *  Non-empty message when create SecretKeySpec with invalid DES key
 * @author Kevin Liu
 */
public class Empty {
    public static void main(String[] args) throws Exception {
        try {
            byte master[] = {
                    0, 1, 2, 3, 4
            };
            SecretKey key = new SecretKeySpec(master, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            throw new RuntimeException("InvalidKeyException not thrown");
        } catch (java.security.InvalidKeyException ike) {
            ike.printStackTrace();
            if (ike.getMessage() != null) {
                out.println("Status -- Passed");
            } else {
                throw new RuntimeException("Error message is not expected when"
                        + " InvalidKeyException is thrown");
            }

        }
    }
}
