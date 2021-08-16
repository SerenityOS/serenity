/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8261779
 * @summary Check that EncryptedPrivateKeyInfo.getEncoded() calls
 *          AlgorithmParameters.getEncoded() when first called
 */

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.AlgorithmParametersSpi;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import javax.crypto.EncryptedPrivateKeyInfo;

public class GetEncoded {

    public static void main(String[] argv) throws Exception {

        AlgorithmParameters params =
            AlgorithmParameters.getInstance("EC", new MyProvider());
        EncryptedPrivateKeyInfo epki =
            new EncryptedPrivateKeyInfo(params, new byte[] {1, 2, 3, 4});
        try {
            epki.getEncoded();
            throw new Exception("Should have thrown IOException");
        } catch (IOException ioe) {
            // test passed, expected exception
        }

        AlgorithmParameters ap1 = AlgorithmParameters.getInstance("EC");
        EncryptedPrivateKeyInfo epki1 =
            new EncryptedPrivateKeyInfo(ap1, new byte[] {1, 2, 3, 4});
        ap1.init(new ECGenParameterSpec("secp256r1"));

        EncryptedPrivateKeyInfo epki2 =
            new EncryptedPrivateKeyInfo(epki1.getEncoded());

        AlgorithmParameters ap2 = epki2.getAlgParameters();
        if (ap2 == null || !Arrays.equals(ap1.getEncoded(), ap2.getEncoded())) {
            throw new Exception("AlgorithmParameters are not equal");
        }
    }

    public static class MyProvider extends Provider {

        MyProvider() {
            super("MyProvider", "0.0", "My Provider");
            put("AlgorithmParameters.EC", UnsupportedParameters.class.getName());
        }
    }

    public static class UnsupportedParameters extends AlgorithmParametersSpi {

        protected void engineInit(AlgorithmParameterSpec paramSpec)
              throws InvalidParameterSpecException {
            throw new InvalidParameterSpecException("Not supported");
        }
        protected void engineInit(byte[] params) throws IOException {
            throw new IOException("Not supported");
        }
        protected void engineInit(byte[] params, String format) throws IOException {
            throw new IOException("Not supported");
        }
        protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(
            Class<T> paramSpec) throws InvalidParameterSpecException {
            throw new InvalidParameterSpecException("Not supported");
        }
        protected byte[] engineGetEncoded() throws IOException {
            throw new IOException("Not supported");
        }
        protected byte[] engineGetEncoded(String format) throws IOException {
            throw new IOException("Not supported");
        }
        protected String engineToString() {
            return null;
        }
    }
}
