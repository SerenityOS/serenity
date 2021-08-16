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
import java.security.*;
import java.security.spec.*;

/**
 * @test
 * @bug 8205445
 * @summary Make sure old state is cleared when init is called again
 */
public class InitAgain {

    public static void main(String[] args) throws Exception {

        byte[] msg = "hello".getBytes();

        Signature s1 = Signature.getInstance("RSASSA-PSS");
        Signature s2 = Signature.getInstance("RSASSA-PSS");

        s1.setParameter(PSSParameterSpec.DEFAULT);
        s2.setParameter(PSSParameterSpec.DEFAULT);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
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
