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

import java.security.*;
import java.util.Iterator;
import java.security.Provider.Service;

/*
 * @test
 * @bug 8220016
 * @summary This test checks the RSA-related services in SunJSSE provider
 */
public class CheckProviderEntries {

    private static boolean testResult = true;

    private static void error(String msg) {
        testResult = false;
        System.out.println(msg);
    }
    public static void main(String[] args) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        Provider p = Security.getProvider("SunJSSE");
        Iterator<Provider.Service> iter = p.getServices().iterator();
        while (iter.hasNext()) {
            Service s = iter.next();
            String type = s.getType();
            String algo = s.getAlgorithm();
            System.out.println("Type: " + type + " " + algo);
            try {
                if (algo.indexOf("RSA") != -1) {
                    // only MD5andSHA1withRSA signature support
                    // error out on any other RSA support
                    if (type.equals("Signature") &&
                        algo.equals("MD5andSHA1withRSA")) {
                        s.newInstance(null);
                        continue;
                    }
                    error("Error: unexpected RSA services");
                }
            } catch (NoSuchAlgorithmException | InvalidParameterException e) {
                error("Error: cannot create obj " + e);
            }
        }
        if (testResult) {
            System.out.println("Test Passed");
        } else {
            throw new RuntimeException("One or more tests failed");
        }
    }
}
