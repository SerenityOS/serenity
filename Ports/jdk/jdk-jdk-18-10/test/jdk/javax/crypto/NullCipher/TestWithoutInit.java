/*
 * Copyright (c) 2004, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4991790
 * @summary Make sure NullCipher can be used without initialization.
 * @author Valerie Peng
 */
import java.io.PrintStream;
import java.util.Random;
import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class TestWithoutInit {

    public static void main(String argv[]) throws Exception {
        // Initialization
        Cipher ci = new NullCipher();

        byte[] in = new byte[8];

        // try calling doFinal() directly
        ci.doFinal(in);

        // try calling update() directly
        ci.update(in);
        ci.doFinal(); // reset cipher state

        // try calling wrap() and unwrap() directly
        Key key = new SecretKeySpec(in, "any");
        try {
            ci.wrap(key);
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
        try {
            ci.unwrap(in, "any", Cipher.SECRET_KEY);
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
        System.out.println("Test Passed");
    }
}
