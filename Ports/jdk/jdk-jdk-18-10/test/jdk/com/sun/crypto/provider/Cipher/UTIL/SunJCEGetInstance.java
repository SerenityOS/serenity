/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7171982
 * @summary Test that SunJCE.getInstance() is retrieving a provider when
 * SunJCE has been removed from the provider list.
 * @run main/othervm SunJCEGetInstance
 */

import java.security.Security;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class SunJCEGetInstance {
    public static void main(String[] args) throws Exception {
        Cipher jce;

        try{
            // Remove SunJCE from Provider list
            Provider prov = Security.getProvider("SunJCE");
            Security.removeProvider("SunJCE");
            // Create our own instance of SunJCE provider.  Purposefully not
            // using SunJCE.getInstance() so we can have our own instance
            // for the test.
            jce = Cipher.getInstance("AES/CBC/PKCS5Padding", prov);

            jce.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec("1234567890abcedf".getBytes(), "AES"));
            jce.doFinal("PlainText".getBytes());
        } catch (Exception e) {
            System.err.println("Setup failure:  ");
            throw e;
        }

        // Get parameters which will call SunJCE.getInstance().  Failure
        // would occur on this line.
        try {
            jce.getParameters().getEncoded();

        } catch (Exception e) {
            System.err.println("Test Failure");
            throw e;
        }
        System.out.println("Passed");
    }
}
