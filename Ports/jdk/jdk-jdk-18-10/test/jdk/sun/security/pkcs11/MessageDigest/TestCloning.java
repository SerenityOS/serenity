/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6414899 8242332
 * @summary Ensure the cloning functionality works.
 * @author Valerie Peng
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestCloning
 * @run main/othervm -Djava.security.manager=allow TestCloning sm
 */

import java.security.MessageDigest;
import java.security.Provider;
import java.util.Arrays;
import java.util.Random;
import java.util.List;

public class TestCloning extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestCloning(), args);
    }

    private static final byte[] data1 = new byte[10];
    private static final byte[] data2 = new byte[10*1024];

    @Override
    public void main(Provider p) throws Exception {
        List<String> ALGS = getSupportedAlgorithms("MessageDigest", "SHA", p);
        Random r = new Random();
        byte[] data1 = new byte[10];
        byte[] data2 = new byte[2*1024];
        r.nextBytes(data1);
        r.nextBytes(data2);
        System.out.println("Testing against provider " + p.getName());
        for (String alg : ALGS) {
            System.out.println("Testing " + alg);
            MessageDigest md = MessageDigest.getInstance(alg, p);
            md = testCloning(md, p);
            // repeat the test again after generating digest once
            for (int j = 0; j < 10; j++) {
                md = testCloning(md, p);
            }
        }
    }

    private static MessageDigest testCloning(MessageDigest mdObj, Provider p)
            throws Exception {
        // copy#0: clone at state BLANK w/o any data
        MessageDigest mdCopy0 = (MessageDigest) mdObj.clone();

        // copy#1: clone again at state BUFFERED w/ very short data
        mdObj.update(data1);
        mdCopy0.update(data1);
        MessageDigest mdCopy1 = (MessageDigest) mdObj.clone();

        // copy#2: clone again after updating it w/ long data to trigger
        // the state into INIT
        mdObj.update(data2);
        mdCopy0.update(data2);
        mdCopy1.update(data2);
        MessageDigest mdCopy2 = (MessageDigest) mdObj.clone();

        // copy#3: clone again after updating it w/ very short data
        mdObj.update(data1);
        mdCopy0.update(data1);
        mdCopy1.update(data1);
        mdCopy2.update(data1);
        MessageDigest mdCopy3 = (MessageDigest) mdObj.clone();

        // copy#4: clone again after updating it w/ long data
        mdObj.update(data2);
        mdCopy0.update(data2);
        mdCopy1.update(data2);
        mdCopy2.update(data2);
        mdCopy3.update(data2);
        MessageDigest mdCopy4 = (MessageDigest) mdObj.clone();

        // check digest equalities
        byte[] answer = mdObj.digest();
        byte[] result0 = mdCopy0.digest();
        byte[] result1 = mdCopy1.digest();
        byte[] result2 = mdCopy2.digest();
        byte[] result3 = mdCopy3.digest();
        byte[] result4 = mdCopy4.digest();


        check(answer, result0, "copy0");
        check(answer, result1, "copy1");
        check(answer, result2, "copy2");
        check(answer, result3, "copy3");
        check(answer, result4, "copy4");

        return mdCopy3;
    }

    private static void check(byte[] d1, byte[] d2, String copyName)
            throws Exception {
        if (Arrays.equals(d1, d2) == false) {
            throw new RuntimeException(copyName + " digest mismatch!");
        }
    }
}

