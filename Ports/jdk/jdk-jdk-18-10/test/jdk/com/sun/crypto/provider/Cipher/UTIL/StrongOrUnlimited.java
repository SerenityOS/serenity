/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Simple test to see if Strong or Unlimited Crypto Policy
 * files are installed.
 * @author Brad R. Wetmore
 */

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class StrongOrUnlimited {

    public static void main(String[] args) throws Exception {
        // decide if the installed jurisdiction policy file is the
        // unlimited version
        boolean isUnlimited = true;
        Cipher c = Cipher.getInstance("AES", "SunJCE");

        try {
            c.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(new byte[16], "AES"));
        } catch (InvalidKeyException ike) {
            throw new Exception("128 bit AES not available.");
        }

        try {
            c.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(new byte[32], "AES"));
            System.out.println("Unlimited Crypto *IS* Installed");
        } catch (InvalidKeyException ike) {
            System.out.println("Unlimited Crypto *IS NOT* Installed");
        }
    }
}
