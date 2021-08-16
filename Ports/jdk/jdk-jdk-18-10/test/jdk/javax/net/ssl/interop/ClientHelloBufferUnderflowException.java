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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8215790 8219389
 * @summary Verify exception
 * @library /test/lib
 * @modules java.base/sun.security.util
 * @run main/othervm ClientHelloBufferUnderflowException
 */

import javax.net.ssl.SSLHandshakeException;

import jdk.test.lib.hexdump.HexPrinter;

public class ClientHelloBufferUnderflowException extends ClientHelloInterOp {
    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        try {
            (new ClientHelloBufferUnderflowException()).run();
        } catch (SSLHandshakeException e) {
            System.out.println("Correct exception thrown: " + e);
            return;
        } catch (Exception e) {
            System.out.println("Failed: Exception not SSLHandShakeException");
            System.out.println(e.getMessage());
            throw e;
        }

        throw new Exception("No expected exception");
    }

    @Override
    protected byte[] createClientHelloMessage() {
        // The ClientHello message in hex: 16 03 01 00 05 01 00 00 01 03
        // Record Header:
        // 16 - type is 0x16 (handshake record)
        // 03 01 - protocol version is 3.1 (also known as TLS 1.0)
        // 00 05 - 0x05 (5) bytes of handshake message follows
        // Handshake Header:
        // 01 - handshake message type 0x01 (client hello)
        // 00 00 01 - 0x01 (1) bytes of client hello follows
        // Client Version:
        // 03 - incomplete client version
        //
        // (Based on https://tls.ulfheim.net)
        byte[] bytes = {
            0x16, 0x03, 0x01, 0x00, 0x05, 0x01, 0x00, 0x00, 0x01, 0x03};

        System.out.println("The ClientHello message used");
        try {
            HexPrinter.simple().format(bytes);
        } catch (Exception e) {
            // ignore
        }

        return bytes;
    }
}
