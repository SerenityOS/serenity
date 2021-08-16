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
 * @summary Testing DTLS records sequence number property support in application
 *          data exchange.
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
 *      -Dtest.mode=norm DTLSSequenceNumberTest
 * @run main/othervm -Dtest.security.protocol=DTLS
 *      -Dtest.mode=norm_sni DTLSSequenceNumberTest
 * @run main/othervm -Dtest.security.protocol=DTLS
 *      -Dtest.mode=krb DTLSSequenceNumberTest
 */

import java.nio.ByteBuffer;
import java.util.TreeMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.util.Random;
import jdk.test.lib.RandomFactory;

/**
 * Testing DTLS records sequence number property support in application data
 * exchange.
 */
public class DTLSSequenceNumberTest extends SSLEngineTestCase {

    private final String BIG_MESSAGE = "Very very big message. One two three"
            + " four five six seven eight nine ten eleven twelve thirteen"
            + " fourteen fifteen sixteen seventeen eighteen nineteen twenty.";
    private final byte[] BIG_MESSAGE_BYTES = BIG_MESSAGE.getBytes();
    private final int PIECES_NUMBER = 15;

    public static void main(String[] args) {
        DTLSSequenceNumberTest test = new DTLSSequenceNumberTest();
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
        checkSeqNumPropertyWithAppDataSend(clientEngine, serverEngine);
        checkSeqNumPropertyWithAppDataSend(serverEngine, clientEngine);
    }

    private void checkSeqNumPropertyWithAppDataSend(SSLEngine sendEngine,
            SSLEngine recvEngine) throws SSLException {
        String sender, reciever;
        if (sendEngine.getUseClientMode() && !recvEngine.getUseClientMode()) {
            sender = "Client";
            reciever = "Server";
        } else if (recvEngine.getUseClientMode() && !sendEngine.getUseClientMode()) {
            sender = "Server";
            reciever = "Client";
        } else {
            throw new Error("Both engines are in the same mode");
        }
        System.out.println("================================================="
                + "===========");
        System.out.println("Checking DTLS sequence number support"
                + " by sending data from " + sender + " to " + reciever);
        ByteBuffer[] sentMessages = new ByteBuffer[PIECES_NUMBER];
        ByteBuffer[] netBuffers = new ByteBuffer[PIECES_NUMBER];
        TreeMap<Long, ByteBuffer> recvMap = new TreeMap<>(Long::compareUnsigned);
        int symbolsInAMessage;
        int symbolsInTheLastMessage;
        int[] recievingSequence = new int[PIECES_NUMBER];
        for (int i = 0; i < PIECES_NUMBER; i++) {
            recievingSequence[i] = i;
        }
        shuffleArray(recievingSequence);
        if (BIG_MESSAGE.length() % PIECES_NUMBER == 0) {
            symbolsInAMessage = BIG_MESSAGE.length() / PIECES_NUMBER;
            symbolsInTheLastMessage = symbolsInAMessage;
        } else {
            symbolsInAMessage = BIG_MESSAGE.length() / (PIECES_NUMBER - 1);
            symbolsInTheLastMessage = BIG_MESSAGE.length() % (PIECES_NUMBER - 1);
        }
        for (int i = 0; i < PIECES_NUMBER - 1; i++) {
            sentMessages[i] = ByteBuffer.wrap(BIG_MESSAGE_BYTES,
                    i * symbolsInAMessage, symbolsInAMessage);
        }
        sentMessages[PIECES_NUMBER - 1] = ByteBuffer.wrap(BIG_MESSAGE_BYTES,
                (PIECES_NUMBER - 1) * symbolsInAMessage, symbolsInTheLastMessage);
        long prevSeqNum = 0L;
        //Wrapping massages in direct order
        for (int i = 0; i < PIECES_NUMBER; i++) {
            netBuffers[i] = ByteBuffer.allocate(sendEngine.getSession()
                    .getPacketBufferSize());
            SSLEngineResult[] r = new SSLEngineResult[1];
            netBuffers[i] = doWrap(sendEngine, sender, 0, sentMessages[i], r);
            long seqNum = r[0].sequenceNumber();
            if (Long.compareUnsigned(seqNum, prevSeqNum) <= 0) {
                throw new AssertionError("Sequence number of the wrapped "
                        + "message is less or equal than that of the"
                        + " previous one! "
                        + "Was " + prevSeqNum + ", now " + seqNum + ".");
            }
            prevSeqNum = seqNum;
        }
        //Unwrapping messages in random order and trying to reconstruct order
        //from sequence number.
        for (int i = 0; i < PIECES_NUMBER; i++) {
            int recvNow = recievingSequence[i];
            SSLEngineResult[] r = new SSLEngineResult[1];
            ByteBuffer recvMassage = doUnWrap(recvEngine, reciever,
                    netBuffers[recvNow], r);
            long seqNum = r[0].sequenceNumber();
            recvMap.put(seqNum, recvMassage);
        }
        int mapSize = recvMap.size();
        if (mapSize != PIECES_NUMBER) {
            throw new AssertionError("The number of received massages "
                    + mapSize + " is not equal to the number of sent messages "
                    + PIECES_NUMBER + "!");
        }
        byte[] recvBigMsgBytes = new byte[BIG_MESSAGE_BYTES.length];
        int counter = 0;
        for (ByteBuffer msg : recvMap.values()) {
            System.arraycopy(msg.array(), 0, recvBigMsgBytes,
                    counter * symbolsInAMessage, msg.remaining());
            counter++;
        }
        String recvBigMsg = new String(recvBigMsgBytes);
        if (!recvBigMsg.equals(BIG_MESSAGE)) {
            throw new AssertionError("Received big message is not equal to"
                    + " one that was sent! Received message is: " + recvBigMsg);
        }
    }

    private static void shuffleArray(int[] ar) {
        final Random RNG = RandomFactory.getRandom();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = RNG.nextInt(i + 1);
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }
}
