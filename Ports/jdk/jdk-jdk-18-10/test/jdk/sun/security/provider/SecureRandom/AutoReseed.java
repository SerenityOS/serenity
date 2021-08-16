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

import java.security.SecureRandom;
import java.security.Security;

/**
 * @test
 * @bug 8051408
 * @summary make sure nextBytes etc can be called before setSeed
 * @run main/othervm -Djava.security.egd=file:/dev/urandom AutoReseed
 */
public class AutoReseed {

    public static void main(String[] args) throws Exception {
        SecureRandom sr;
        boolean pass = true;
        for (String mech : new String[]{"Hash_DRBG", "HMAC_DRBG", "CTR_DRBG"}) {
            try {
                System.out.println("Testing " + mech + "...");
                Security.setProperty("securerandom.drbg.config", mech);

                // Check auto reseed works
                sr = SecureRandom.getInstance("DRBG");
                sr.nextInt();
                sr = SecureRandom.getInstance("DRBG");
                sr.reseed();
                sr = SecureRandom.getInstance("DRBG");
                sr.generateSeed(10);
            } catch (Exception e) {
                pass = false;
                e.printStackTrace(System.out);
            }
        }
        if (!pass) {
            throw new RuntimeException("At least one test case failed");
        }
    }
}
