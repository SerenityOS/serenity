/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8211049
 * @summary make sure the supplied SecureRandom object is used
 */

import java.security.*;
import java.security.interfaces.*;

public class TestKeyPairGeneratorInit {

    private static class MySecureRandom extends SecureRandom {
        boolean isUsed = false;
        public MySecureRandom() {
            super();
        }

        public void nextBytes(byte[] bytes) {
            isUsed = true;
            super.nextBytes(bytes);
        }
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg =
            KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        MySecureRandom rnd = new MySecureRandom();
        kpg.initialize(2048, rnd);
        System.out.println("Generate keypair then check");
        KeyPair kp = kpg.generateKeyPair();
        if (!rnd.isUsed) {
            throw new RuntimeException("ERROR: Supplied random not used");
        } else {
            System.out.println("=> Test passed");
        }
    }
}
