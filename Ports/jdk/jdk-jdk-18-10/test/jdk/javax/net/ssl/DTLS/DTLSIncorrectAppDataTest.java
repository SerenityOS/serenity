/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043758
 * @summary Testing DTLS incorrect app data packages unwrapping.
 * @key randomness
 * @library /sun/security/krb5/auto /test/lib /javax/net/ssl/TLSCommon
 * @modules java.security.jgss
 *          jdk.security.auth
 *          java.security.jgss/sun.security.jgss.krb5
 *          java.security.jgss/sun.security.krb5:+open
 *          java.security.jgss/sun.security.krb5.internal:+open
 *          java.security.jgss/sun.security.krb5.internal.ccache
 *          java.security.jgss/sun.security.krb5.internal.crypto
 *          java.security.jgss/sun.security.krb5.internal.ktab
 *          java.base/sun.security.util
 * @build jdk.test.lib.RandomFactory
 * @run main/othervm -Dtest.security.protocol=DTLS
 *      -Dtest.mode=norm DTLSIncorrectAppDataTest
 * @run main/othervm -Dtest.security.protocol=DTLS
 *      -Dtest.mode=norm_sni DTLSIncorrectAppDataTest
 * @run main/othervm -Dtest.security.protocol=DTLS
 *      -Dtest.mode=krb DTLSIncorrectAppDataTest
 */

import java.nio.ByteBuffer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.util.Random;
import jdk.test.lib.RandomFactory;

/**
 * Testing DTLS incorrect app data packages unwrapping. Incorrect application
 * data packages should be ignored by DTLS SSLEngine.
 */
public class DTLSIncorrectAppDataTest extends SSLEngineTestCase {

    private final String MESSAGE = "Hello peer!";

    public static void main(String[] s) {
        DTLSIncorrectAppDataTest test = new DTLSIncorrectAppDataTest();
        setUpAndStartKDCIfNeeded();
        test.runTests();
    }

    @Override
    protected void testOneCipher(String cipher) {
        SSLContext context = getContext();
        int maxPacketSize = getMaxPacketSize();
        boolean useSNI = !TEST_MODE.equals("norm");
        SSLEngine clientEngine = getClientSSLEngine(context, useSNI);
        SSLEngine serverEngine = getServerSSLEngine(context, useSNI);
        clientEngine.setEnabledCipherSuites(new String[]{cipher});
        serverEngine.setEnabledCipherSuites(new String[]{cipher});
        serverEngine.setNeedClientAuth(!cipher.contains("anon"));
        try {
            doHandshake(clientEngine, serverEngine, maxPacketSize,
                    HandshakeMode.INITIAL_HANDSHAKE);
            checkIncorrectAppDataUnwrap(clientEngine, serverEngine);
            checkIncorrectAppDataUnwrap(serverEngine, clientEngine);
        } catch (SSLException ssle) {
            throw new AssertionError("Error during handshake or sending app data",
                    ssle);
        }
    }

    private void checkIncorrectAppDataUnwrap(SSLEngine sendEngine,
            SSLEngine recvEngine) throws SSLException {
        String direction = sendEngine.getUseClientMode() ? "client"
                : "server";
        System.out.println("================================================="
                + "===========");
        System.out.println("Testing DTLS incorrect app data packages unwrapping"
                + " by sending data from " + direction);
        ByteBuffer app = ByteBuffer.wrap(MESSAGE.getBytes());
        ByteBuffer net = doWrap(sendEngine, direction, 0, app);
        final Random RNG = RandomFactory.getRandom();
        int randomPlace = RNG.nextInt(net.remaining());
        net.array()[randomPlace] += 1;
        app = ByteBuffer.allocate(recvEngine.getSession()
                .getApplicationBufferSize());
        recvEngine.unwrap(net, app);
        app.flip();
        int length = app.remaining();
        System.out.println("Unwrapped " + length + " bytes.");
    }
}
