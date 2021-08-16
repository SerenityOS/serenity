/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246077
 * @summary Ensure a cloneable digest can be accepted/used by HmacCore
 */
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.*;
import javax.crypto.spec.*;

public class DigestCloneabilityTest {

    private static String ALGO = "HmacSHA512";

    public static void main(String[] args) throws Exception {
        Provider p = new SampleProvider();
        // make SampleProvider the most preferred, so its digest impl is picked
        int status = Security.insertProviderAt(p, 1);
        try {
            Mac mac = Mac.getInstance(ALGO, "SunJCE");
            // do a complete mac generation and check if the supplied
            // digest is used
            mac.init(new SecretKeySpec(new byte[512>>3], ALGO));
            mac.update((byte)0x12);
            byte[] macBytes = mac.doFinal();
            if (!SampleProvider.CloneableDigest.isUsed) {
                throw new RuntimeException("Expected Digest impl not used");
            }
        } finally {
            if (status != -1) {
                Security.removeProvider(p.getName());
            }
        }
        System.out.println("Test Passed");
    }

    public static class SampleProvider extends Provider {

        public SampleProvider() {
            super("Sample", "1.0", "test provider");
            putService(new Provider.Service(this, "MessageDigest", "SHA-512",
                    "DigestCloneabilityTest$SampleProvider$CloneableDigest",
                    null, null));
        }
        public static class CloneableDigest extends MessageDigestSpi
                implements Cloneable {
            private MessageDigest md;
            static boolean isUsed = false;

            public CloneableDigest() throws NoSuchAlgorithmException {
                try {
                    md = MessageDigest.getInstance("SHA-512", "SUN");
                } catch (NoSuchProviderException nspe) {
                    // should never happen
                }
            }

            public byte[] engineDigest() {
                isUsed = true;
                return md.digest();
            }

            public void engineReset() {
                isUsed = true;
                md.reset();
            }

            public void engineUpdate(byte input) {
                isUsed = true;
                md.update(input);
            }

            public void engineUpdate(byte[] b, int ofs, int len) {
                isUsed = true;
                md.update(b, ofs, len);
            }

            public Object clone() throws CloneNotSupportedException {
                return this;
            }
        }
    }
}
