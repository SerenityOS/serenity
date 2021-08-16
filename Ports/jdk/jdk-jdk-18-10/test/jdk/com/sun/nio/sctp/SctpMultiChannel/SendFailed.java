/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8067846
 * @summary Test for send failed notification
 */

import com.sun.nio.sctp.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import static java.lang.System.out;
import static java.nio.ByteBuffer.*;

public class SendFailed {

    static final SocketAddress remoteAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 3000);

    static final int[] bufferSizes =
            { 20, 49, 50, 51, 100, 101, 1024, 1025, 4095, 4096, 4097, 8191, 8192, 8193};

    void test(String[] args) throws IOException {
        SocketAddress address = null;

        if (!Util.isSCTPSupported()) {
            out.println("SCTP protocol is not supported");
            out.println("Test cannot be run");
            return;
        }

        System.out.println("remote address: " + remoteAddress);
        System.out.println("Note, remote address should not be up");

        /* combinations with various buffer sizes, and offsets */
        for (int send=0; send < bufferSizes.length; send++) {
            for (int recv=0; recv < bufferSizes.length; recv++) {
                for (boolean direct : new boolean[] {true, false})
                    runWithManyOffsets(bufferSizes[send], bufferSizes[recv], direct);
            }
        }
    }

    void runWithManyOffsets(int sendBufferSize, int recvBufferSize, boolean direct)
        throws IOException
    {
        doTest(sendBufferSize, recvBufferSize, direct, 0);
        doTest(sendBufferSize, recvBufferSize, direct, 1);
        doTest(sendBufferSize, recvBufferSize, direct, 3);
        doTest(sendBufferSize, recvBufferSize, direct, 7);
        doTest(sendBufferSize, recvBufferSize, direct, 9);
        doTest(sendBufferSize, recvBufferSize, direct, 13);
        doTest(sendBufferSize, recvBufferSize, direct, 15);
    }

    void doTest(int sendBufferSize, int recvBufferSize, boolean direct, int offset)
        throws IOException
    {
        debug("%n--- Testing with send size:[%d], recv size:[%d], offset:[%d] "
                + ", direct [%s]. ", sendBufferSize, recvBufferSize, offset, direct);

        try (SctpMultiChannel channel = SctpMultiChannel.open()) {
            MessageInfo messageInfo = MessageInfo.createOutgoing(remoteAddress, 0);
            ByteBuffer sendBuffer = filledBuffer(sendBufferSize, direct);

            debug("%nAttempting to send to %s. ", remoteAddress);
            int sent = channel.send(sendBuffer, messageInfo);
            sendBuffer.flip();

            SendFailedNotificationHandler handler =
                    new SendFailedNotificationHandler();
            ByteBuffer recvBuffer = direct ? allocateDirect(recvBufferSize)
                                           : allocate((recvBufferSize));
            MessageInfo info = channel.receive(recvBuffer, null, handler);
            debug("receive returned info:" + info);

            if (handler.receivedSendFailed) {
                // verify sent buffer received by send failed notification
                ByteBuffer buffer = handler.getSendFailedByteBuffer();
                check(buffer.remaining() == sent);
                check(buffer.position() == 0);
                check(buffer.limit() == sent);
                assertSameContent(sendBuffer, handler.getSendFailedByteBuffer());
            } else {
                debug("Unexpected event or received data. Check output.");
            }
        }
    }

    class SendFailedNotificationHandler extends AbstractNotificationHandler<Object>
    {
        /** Reference to the buffer captured in send failed notification */
        private ByteBuffer sentBuffer;
        boolean receivedSendFailed;

        @Override
        public HandlerResult handleNotification(
                Notification notification, Object attachment) {
            fail("Unknown notification type");
            return HandlerResult.CONTINUE;
        }

        @Override
        public HandlerResult handleNotification(
                AssociationChangeNotification notification, Object attachment) {
            AssociationChangeNotification.AssocChangeEvent event = notification.event();
            debug("%nAssociationChangeNotification");
            debug("%n  Association: %s. ", notification.association());
            debug("%n  Event: %s. ", event);
            return HandlerResult.CONTINUE;
        }

        @Override
        public HandlerResult handleNotification(
                SendFailedNotification notification, Object attachment) {
            debug("%nSendFailedNotification: %s. ", notification);
            receivedSendFailed = true;
            sentBuffer = notification.buffer();
            return HandlerResult.RETURN;
        }

        public ByteBuffer getSendFailedByteBuffer() {
            return sentBuffer;
        }

        @Override
        public HandlerResult handleNotification(
                PeerAddressChangeNotification pacn, Object attachment)
        {
            debug("%nPeerAddressChangeNotification: %s", pacn);
            return HandlerResult.CONTINUE;
        }

        @Override
        public HandlerResult handleNotification(
                ShutdownNotification notification, Object attachment) {
            debug("%nShutdownNotification");
            debug("%n  Association: %s. ", notification.association());
            return HandlerResult.CONTINUE;
        }
    }

    static ByteBuffer filledBuffer(int size, boolean direct) {
        ByteBuffer buffer = direct ? allocateDirect(size) : allocate((size));
        for (int i=0; i< size; i++)
            buffer.put((byte)i);
        buffer.flip();
        return buffer;
    }

    static void assertSameContent(ByteBuffer bb1, ByteBuffer bb2) {
        if (!bb1.equals(bb2))
            throw new RuntimeException("Buffers are not equal; bb1: " + bb1 + ", bb2: " + bb2);
    }

    //--------------------- Infrastructure ---------------------------
    boolean debug = true;
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void check(boolean cond, String failMessage) {if (cond) pass(); else fail(failMessage);}
    void debug(String message, Object... args) {if(debug) { out.printf(message, args); } }
    public static void main(String[] args) throws Throwable {
        Class<?> k = new Object(){}.getClass().getEnclosingClass();
        try {k.getMethod("instanceMain",String[].class)
                .invoke( k.newInstance(), (Object) args);}
        catch (Throwable e) {throw e.getCause();}}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
