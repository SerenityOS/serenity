/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * @test
 * @bug 8048603
 * @summary Checks if MAC algorithms work fine with empty buffer
 * @author Alexander Fomin
 * @build Utils
 * @run main EmptyByteBufferTest
 */
public class EmptyByteBufferTest implements MacTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Utils.runTests(new EmptyByteBufferTest());
    }

    @Override
    public void doTest(String alg) throws NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException {
        SecretKey key = Utils.getSecretKeySpec();

        // instantiate Mac object and init it with a SecretKey
        Mac mac = Mac.getInstance(alg, "SunJCE");
        mac.init(key);

        // prepare buffer
        byte[] data = new byte[0];
        ByteBuffer buf = ByteBuffer.wrap(data);

        mac.update(buf);
        mac.doFinal();
    }

}
