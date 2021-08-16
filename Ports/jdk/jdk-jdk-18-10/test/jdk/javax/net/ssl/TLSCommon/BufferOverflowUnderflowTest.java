/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

/**
 * Testing SSLEngine incorrect app data packages unwrapping.
 */
public class BufferOverflowUnderflowTest extends SSLEngineTestCase {

    private final String MESSAGE = "Hello peer!";

    public static void main(String[] args) {
        BufferOverflowUnderflowTest test = new BufferOverflowUnderflowTest();
        setUpAndStartKDCIfNeeded();
        test.runTests();
    }

    @Override
    protected void testOneCipher(String cipher) throws SSLException {
        SSLContext context = getContext();
        int maxPacketSize = getMaxPacketSize();
        boolean useSNI = !TEST_MODE.equals("norm");
        SSLEngine clientEngine = getClientSSLEngine(context, useSNI);
        SSLEngine serverEngine = getServerSSLEngine(context, useSNI);
        clientEngine.setEnabledCipherSuites(new String[]{cipher});
        serverEngine.setEnabledCipherSuites(new String[]{cipher});
        serverEngine.setNeedClientAuth(!cipher.contains("anon"));
        doHandshake(clientEngine, serverEngine, maxPacketSize,
                HandshakeMode.INITIAL_HANDSHAKE);
        checkBufferOverflowOnWrap(clientEngine);
        checkBufferOverflowOnWrap(serverEngine);
        checkBufferOverflowOnUnWrap(clientEngine, serverEngine);
        checkBufferOverflowOnUnWrap(serverEngine, clientEngine);
        checkBufferUnderflowOnUnWrap(serverEngine, clientEngine);
        checkBufferUnderflowOnUnWrap(clientEngine, serverEngine);
    }

    private void checkBufferOverflowOnWrap(SSLEngine engine)
            throws SSLException {
        String mode = engine.getUseClientMode() ? "client"
                : "server";
        System.out.println("================================================="
                + "===========");
        System.out.println("Testing SSLEngine buffer overflow"
                + " on wrap by " + mode);
        ByteBuffer app = ByteBuffer.wrap(MESSAGE.getBytes());
        //Making net buffer size less than required by 1 byte.
        ByteBuffer net = ByteBuffer
                .allocate(engine.getSession().getPacketBufferSize() - 1);
        SSLEngineResult r = engine.wrap(app, net);
        checkResult(r, SSLEngineResult.Status.BUFFER_OVERFLOW);
        System.out.println("Passed");
    }

    private void checkBufferOverflowOnUnWrap(SSLEngine wrappingEngine,
            SSLEngine unwrappingEngine)
            throws SSLException {
        String wrapperMode = wrappingEngine.getUseClientMode() ? "client"
                : "server";
        String unwrapperMode = unwrappingEngine.getUseClientMode() ? "client"
                : "server";
        if (wrapperMode.equals(unwrapperMode)) {
            throw new Error("Test error: both engines are in the same mode!");
        }
        System.out.println("================================================="
                + "===========");
        System.out.println("Testing SSLEngine buffer overflow"
                + " on unwrap by " + unwrapperMode);
        ByteBuffer app = ByteBuffer.wrap(MESSAGE.getBytes());
        ByteBuffer net = ByteBuffer
                .allocate(wrappingEngine.getSession().getPacketBufferSize());
        SSLEngineResult r = wrappingEngine.wrap(app, net);
        checkResult(r, SSLEngineResult.Status.OK);
        //Making app buffer size less than required by 1 byte.
        app = ByteBuffer.allocate(MESSAGE.length() - 1);
        net.flip();
        r = unwrappingEngine.unwrap(net, app);
        checkResult(r, SSLEngineResult.Status.BUFFER_OVERFLOW);
        System.out.println("Passed");
    }

    private void checkBufferUnderflowOnUnWrap(SSLEngine wrappingEngine,
            SSLEngine unwrappingEngine)
            throws SSLException {
        String wrapperMode = wrappingEngine.getUseClientMode() ? "client"
                : "server";
        String unwrapperMode = unwrappingEngine.getUseClientMode() ? "client"
                : "server";
        if (wrapperMode.equals(unwrapperMode)) {
            throw new Error("Test error: both engines are in the same mode!");
        }
        System.out.println("================================================="
                + "===========");
        System.out.println("Testing SSLEngine buffer underflow"
                + " on unwrap by " + unwrapperMode);
        ByteBuffer app = ByteBuffer.wrap(MESSAGE.getBytes());
        ByteBuffer net = ByteBuffer
                .allocate(wrappingEngine.getSession().getPacketBufferSize());
        SSLEngineResult r = wrappingEngine.wrap(app, net);
        checkResult(r, SSLEngineResult.Status.OK);
        app = ByteBuffer.allocate(unwrappingEngine.getSession()
                .getApplicationBufferSize());
        net.flip();
        //Making net buffer size less than size of dtls message.
        net.limit(net.limit() - 1);
        r = unwrappingEngine.unwrap(net, app);
        checkResult(r, SSLEngineResult.Status.BUFFER_UNDERFLOW);
        System.out.println("Passed");
    }
}
