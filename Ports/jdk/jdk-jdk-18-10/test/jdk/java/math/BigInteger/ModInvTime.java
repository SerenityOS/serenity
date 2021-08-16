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
 * @bug 8225603
 * @summary Tests whether modInverse() completes in a reasonable time
 * @run main/othervm ModInvTime
 */
import java.math.BigInteger;

public class ModInvTime {
    public static void main(String[] args) throws InterruptedException {
        BigInteger prime = new BigInteger("39402006196394479212279040100143613805079739270465446667946905279627659399113263569398956308152294913554433653942643");
        BigInteger s = new BigInteger("9552729729729327851382626410162104591956625415831952158766936536163093322096473638446154604799898109762512409920799");
        System.out.format("int length: %d, modulus length: %d%n",
            s.bitLength(), prime.bitLength());

        System.out.println("Computing modular inverse ...");
        BigInteger mi = s.modInverse(prime);
        System.out.format("Modular inverse: %s%n", mi);
        check(s, prime, mi);

        BigInteger ns = s.negate();
        BigInteger nmi = ns.modInverse(prime);
        System.out.format("Modular inverse of negation: %s%n", nmi);
        check(ns, prime, nmi);
    }

    public static void check(BigInteger val, BigInteger mod, BigInteger inv) {
        BigInteger r = inv.multiply(val).remainder(mod);
        if (r.signum() == -1)
            r = r.add(mod);
        if (!r.equals(BigInteger.ONE))
            throw new RuntimeException("Numerically incorrect modular inverse");
    }
}
