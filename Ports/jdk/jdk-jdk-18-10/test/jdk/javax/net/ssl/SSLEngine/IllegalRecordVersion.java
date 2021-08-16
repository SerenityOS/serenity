/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

// This test case relies on updated static security property, no way to re-use
// security property in samevm/agentvm mode.

/*
 * @test
 * @bug 8042449
 * @summary Issue for negative byte major record version
 * @ignore the dependent implementation details are changed
 * @run main/othervm IllegalRecordVersion
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

public class IllegalRecordVersion {

    public static void main(String args[]) throws Exception {
        SSLContext context = SSLContext.getDefault();

        SSLEngine cliEngine = context.createSSLEngine();
        cliEngine.setUseClientMode(true);
        SSLEngine srvEngine = context.createSSLEngine();
        srvEngine.setUseClientMode(false);

        SSLSession session = cliEngine.getSession();
        int netBufferMax = session.getPacketBufferSize();
        int appBufferMax = session.getApplicationBufferSize();

        ByteBuffer cliToSrv = ByteBuffer.allocateDirect(netBufferMax);
        ByteBuffer srvIBuff = ByteBuffer.allocateDirect(appBufferMax + 50);
        ByteBuffer cliOBuff = ByteBuffer.wrap("I'm client".getBytes());


        System.out.println("client hello (record version(0xa9, 0xa2))");
        SSLEngineResult cliRes = cliEngine.wrap(cliOBuff, cliToSrv);
        System.out.println("Client wrap result: " + cliRes);
        cliToSrv.flip();
        if (cliToSrv.limit() > 5) {
            cliToSrv.put(1, (byte)0xa9);
            cliToSrv.put(2, (byte)0xa2);
        }

        try {
            srvEngine.unwrap(cliToSrv, srvIBuff);
            throw new Exception(
                "Cannot catch the unsupported record version issue");
        } catch (SSLException e) {
            // get the expected exception
        }
    }
}
