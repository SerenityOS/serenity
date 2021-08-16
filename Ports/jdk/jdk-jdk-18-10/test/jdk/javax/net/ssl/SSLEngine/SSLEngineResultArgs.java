/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4965868
 * @summary SSLEngineResult constructor needs null argument description
 *
 * @author Brad Wetmore
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;

public class SSLEngineResultArgs {

    private static void test(int i) throws Exception {
        SSLEngineResult result;
        try {
            switch (i) {
            case 0:
                result = new SSLEngineResult(
                    null, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
                return;
            case 1:
                result = new SSLEngineResult(
                    Status.OK, null, 0, 0);
                return;
            case 2:
                result = new SSLEngineResult(
                    Status.OK, HandshakeStatus.NOT_HANDSHAKING, -1, 0);
                return;
            case 3:
                result = new SSLEngineResult(
                    Status.OK, HandshakeStatus.NOT_HANDSHAKING, 0, -1);
                return;
            }
            throw new Exception("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("Threw right Exception");
        }
    }

    public static void main(String args[]) throws Exception {
        test(0);
        test(1);
        test(2);
        test(3);
    }
}
