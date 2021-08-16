/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.spec.*;

/**
 * @test
 * @bug 8080462 8242332
 * @summary Make sure old state is cleared when init is called again
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 */
public class InitAgainPSS extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new InitAgainPSS(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        test("RSASSA-PSS", p);
    }

    private void test(String sigAlg, Provider p) throws Exception {
        Signature s1;
        try {
            s1 = Signature.getInstance(sigAlg, p);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Skip testing " + sigAlg +
                " due to no support");
            return;
        }

        byte[] msg = "hello".getBytes();

        Signature s2 = Signature.getInstance(sigAlg, p);

        PSSParameterSpec params = new PSSParameterSpec("SHA-256", "MGF1",
            new MGF1ParameterSpec("SHA-256"), 32,
            PSSParameterSpec.TRAILER_FIELD_BC);
        s1.setParameter(params);
        s2.setParameter(params);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        s1.initSign(kp.getPrivate());

        s1.update(msg);
        s1.initSign(kp.getPrivate());
        s1.update(msg);
        // Data digested in s1:
        // Before this fix, msg | msg
        // After this fix, msg

        s2.initVerify(kp.getPublic());
        s2.update(msg);
        s2.initVerify(kp.getPublic());
        s2.update(msg);
        s2.initVerify(kp.getPublic());
        s2.update(msg);
        // Data digested in s2:
        // Before this fix, msg | msg | msg
        // After this fix, msg

        if (!s2.verify(s1.sign())) {
            throw new Exception();
        }
    }
}
