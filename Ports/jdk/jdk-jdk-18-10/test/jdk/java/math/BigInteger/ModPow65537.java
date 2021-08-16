/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main ModPow65537
 * @bug 4891312 8074460 8078672
 * @summary verify that modPow() not broken by the special case for 65537 (use -Dseed=X to set PRNG seed)
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.math.BigInteger;

import java.security.*;
import java.security.spec.*;
import java.util.Random;
import jdk.test.lib.RandomFactory;

public class ModPow65537 {

    public static void main(String[] args) throws Exception {
        // SunRsaSign uses BigInteger internally
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        kpg.initialize(new RSAKeyGenParameterSpec(512, BigInteger.valueOf(65537)));
        KeyPair kp = kpg.generateKeyPair();
        testSigning(kp);

        kpg.initialize(new RSAKeyGenParameterSpec(512, BigInteger.valueOf(65539)));
        kp = kpg.generateKeyPair();
        testSigning(kp);

        kpg.initialize(new RSAKeyGenParameterSpec(512, BigInteger.valueOf(3)));
        kp = kpg.generateKeyPair();
        testSigning(kp);

        // basic known answer test
        BigInteger base = new BigInteger("19058071224156864789844466979330892664777520457048234786139035643344145635582");
        BigInteger mod  = new BigInteger("75554098474976067521257305210610421240510163914613117319380559667371251381587");
        BigInteger exp1 = BigInteger.valueOf(65537);
        BigInteger exp2 = BigInteger.valueOf(75537);
        BigInteger exp3 = new BigInteger("13456870775607312149");

        BigInteger res1 = new BigInteger("5770048609366563851320890693196148833634112303472168971638730461010114147506");
        BigInteger res2 = new BigInteger("63446979364051087123350579021875958137036620431381329472348116892915461751531");
        BigInteger res3 = new BigInteger("39016891919893878823999350081191675846357272199067075794096200770872982089502");

        if (base.modPow(exp1, mod).equals(res1) == false) {
            throw new Exception("Error using " + exp1);
        }
        if (base.modPow(exp2, mod).equals(res2) == false) {
            throw new Exception("Error using " + exp2);
        }
        if (base.modPow(exp3, mod).equals(res3) == false) {
            throw new Exception("Error using " + exp3);
        }

        System.out.println("Passed");
    }

    private static void testSigning(KeyPair kp) throws Exception {
        System.out.println(kp.getPublic());
        byte[] data = new byte[1024];
        Random random = RandomFactory.getRandom();
        random.nextBytes(data);

        Signature sig = Signature.getInstance("SHA1withRSA", "SunRsaSign");
        sig.initSign(kp.getPrivate());
        sig.update(data);
        byte[] sigBytes = sig.sign();

        sig.initVerify(kp.getPublic());
        sig.update(data);
        if (sig.verify(sigBytes) == false) {
            throw new Exception("signature verification failed");
        }
        System.out.println("OK");
    }

}
