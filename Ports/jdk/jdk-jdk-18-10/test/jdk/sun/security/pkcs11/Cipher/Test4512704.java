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

/*
 * @test
 * @bug 8080462
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main Test4512704
 * @summary Verify that AES cipher can generate default IV in encrypt mode
 */
import java.io.PrintStream;
import java.security.*;
import java.security.spec.*;
import java.util.Random;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.Provider;

public class Test4512704 extends PKCS11Test {

    public void test(String mode, Provider p) throws Exception {
        Cipher c;
        String transformation = "AES/" + mode + "/NoPadding";

        try {
            transformation = "AES/" + mode + "/NoPadding";
            c = Cipher.getInstance(transformation, p);
        } catch (GeneralSecurityException e) {
            System.out.println("Skip testing " + p.getName() +
                    ", no support for " + mode);
            return;
        }
        SecretKey key = new SecretKeySpec(new byte[16], "AES");

        AlgorithmParameterSpec aps = null;
        Cipher ci = Cipher.getInstance(transformation, p);
        try {
            ci.init(Cipher.ENCRYPT_MODE, key, aps);
        } catch(InvalidAlgorithmParameterException ex) {
            throw new Exception("parameter should be generated when null is specified!");
        }
        System.out.println(transformation + ": Passed");
    }

    public static void main (String[] args) throws Exception {
        main(new Test4512704(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        test("GCM", p);
    }

}
