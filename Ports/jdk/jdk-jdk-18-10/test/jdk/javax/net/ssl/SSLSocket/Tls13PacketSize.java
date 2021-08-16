/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
// Please run in othervm mode.  SunJSSE does not support dynamic system
// properties, no way to re-use system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8221253
 * @summary TLSv1.3 may generate TLSInnerPlainText longer than 2^14+1 bytes
 * @modules jdk.crypto.ec
 * @library /javax/net/ssl/templates
 * @run main/othervm Tls13PacketSize
 */
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;

public class Tls13PacketSize extends SSLSocketTemplate {
    private static final byte[] appData = new byte[16385];
    static {
        for (int i = 0; i < appData.length; i++) {
            appData[i] = (byte)('A' + (i % 26));
        }
    }

    // Run the test case.
    public static void main(String[] args) throws Exception {
        (new Tls13PacketSize()).run();
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        // Set SO_LINGER in case of slow socket
        socket.setSoLinger(true, 10);

        // here comes the test logic
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslIS.read();
        int extra = sslIS.available();
        System.out.println("Server input bytes: " + extra);
        // Considering the padding impact, the record plaintext is less
        // than the TLSPlaintext.fragment length (2^14).
        if (extra >= 16383) {    // 16383: 2^14 - 1 byte read above
            throw new Exception(
                    "Client record plaintext exceeds 2^14 octets: " + extra);
        }

        sslOS.write(appData);
        sslOS.flush();
    }

    /*
     * Define the client side application of the test for the specified socket.
     * This method is used if the returned value of
     * isCustomizedClientConnection() is false.
     *
     * @param socket may be null is no client socket is generated.
     *
     * @see #isCustomizedClientConnection()
     */
    protected void runClientApplication(SSLSocket socket) throws Exception {
        // Set SO_LINGER in case of slow socket
        socket.setSoLinger(true, 10);

        socket.setEnabledProtocols(new String[] {"TLSv1.3"});
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(appData);
        sslOS.flush();

        sslIS.read();
        int extra = sslIS.available();
        System.out.println("Client input bytes: " + extra);
        // Considering the padding impact, the record plaintext is less
        // than the TLSPlaintext.fragment length (2^14).
        if (extra >= 16383) {    // 16383: 2^14 - 1 byte read above
            throw new Exception(
                    "Server record plaintext exceeds 2^14 octets: " + extra);
        }
    }
}
