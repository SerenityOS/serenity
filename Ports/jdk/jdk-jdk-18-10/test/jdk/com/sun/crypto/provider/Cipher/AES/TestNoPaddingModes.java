/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8180392
 * @summary Ensure SunJCE provider throws exception for unsupported modes
 *          and padding combinations
 */
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.*;

public class TestNoPaddingModes {

    // SunJCE only supports NoPadding with following modes
    private static final String[] MODES = {
        "CTR", "CTS", "GCM"
    };
    private static final String[] PADDINGS = {
        "PKCS5Padding", "ISO10126Padding"
    };

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SunJCE");
        String transformation;
        for (String mode : MODES) {
            for (String padding : PADDINGS) {
                transformation = "AES/" + mode + "/" + padding;

                System.out.println("Test using " + transformation);
                try {
                    Cipher c = Cipher.getInstance(transformation, "SunJCE");
                    throw new RuntimeException("=> Fail, no exception thrown");
                } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                    System.out.println("=> Expected ex: " + ex);
                }
                try {
                    Cipher c = Cipher.getInstance(transformation, p);
                    throw new RuntimeException("=> Fail, no exception thrown");
                } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
                    System.out.println("=> Expected ex: " + ex);
                }
            }
        }
        System.out.println("Test Passed");
    }
}
