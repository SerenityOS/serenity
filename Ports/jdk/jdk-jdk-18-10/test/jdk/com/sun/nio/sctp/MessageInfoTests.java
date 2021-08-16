/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4927640
 * @summary Tests the SCTP protocol implementation
 * @author chegar
 */

import java.net.SocketAddress;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;

public class MessageInfoTests {
    static final int DEFAULT_STREAM_NUMBER = 14;
    static final int TEST_STREAM_NUMBER = 15;
    static final int TEST_PPID = 8;
    static final long TEST_TTL = 10000L;
    static final SocketAddress addr = new TestSocketAddress();
    static final Association assoc = new TestAssociation(1, 1, 1);

    void test(String[] args) {
        /* TEST 1 : createOutGoing(SocketAddress,int) */
        MessageInfo info = MessageInfo.createOutgoing(addr,
                                                      DEFAULT_STREAM_NUMBER);
        checkDefaults(info);
        checkGetterSetters(info);

        /* TEST 2 : createOutGoing(Association,SocketAddress,int) */
        info = MessageInfo.createOutgoing(assoc, addr, DEFAULT_STREAM_NUMBER);
        checkDefaults(info);
        check(info.association().equals(assoc), "incorrect association");
        checkGetterSetters(info);

        /* TEST 3: null values */
        info = MessageInfo.createOutgoing(null, 0);
        check(info.address() == null, "address should be null");
        check(info.association() == null, "association should be null");
        info = MessageInfo.createOutgoing(assoc, null, 0);
        check(info.address() == null, "address should be null");

        /* Test 4: IllegalArgumentException */
        testIAE(new Runnable() {
          public void run() { MessageInfo.createOutgoing(addr, -1); } });
        testIAE(new Runnable() {
          public void run() { MessageInfo.createOutgoing(addr, 65537); } });
        testIAE(new Runnable() {
          public void run() { MessageInfo.createOutgoing(null, addr, 0); } });
        testIAE(new Runnable() {
          public void run() { MessageInfo.createOutgoing(assoc, addr, -1); } });
        testIAE(new Runnable() {
          public void run() { MessageInfo.createOutgoing(assoc, addr, 65537);}});

        final MessageInfo iaeInfo = MessageInfo.createOutgoing(assoc, addr, 0);
        testIAE(new Runnable() {
          public void run() { iaeInfo.streamNumber(-1); } });
        testIAE(new Runnable() {
          public void run() { iaeInfo.streamNumber(65537); } });
    }

   /* TEST : unordered = false, timeToLive = 0, complete = true,
    *        payloadProtocolID = 0. */
    void checkDefaults(MessageInfo info) {
        check(info.isUnordered() == false, "default unordered value not false");
        check(info.timeToLive() == 0L, "timeToLive should be 0L");
        check(info.isComplete() == true, "default complete value not true");
        check(info.payloadProtocolID() == 0, "default PPID not 0");
        check(info.bytes() == 0, "default bytes value not 0");
        check(info.streamNumber() == DEFAULT_STREAM_NUMBER,
                "incorrect default stream number");
        check(info.address().equals(addr), "incorrect address");
    }

    void checkGetterSetters(MessageInfo info) {
        check(info.streamNumber(TEST_STREAM_NUMBER).streamNumber() ==
                TEST_STREAM_NUMBER, "stream number not being set correctly");

        check(info.complete(false).isComplete() == false,
                "complete not being set correctly");

        check(info.unordered(true).isUnordered() == true,
                "unordered not being set correctly");

        check(info.payloadProtocolID(TEST_PPID).payloadProtocolID() ==
                                  TEST_PPID, "PPID not being set correctly");

        check(info.timeToLive(TEST_TTL).timeToLive() == TEST_TTL,
                "TTL not being set correctly");
    }

    void testIAE(Runnable runnable) {
        try {
            runnable.run();
            fail("IllegalArgumentException should have been thrown");
        } catch(IllegalArgumentException iae) {
            pass();
        }
    }

    static class TestSocketAddress extends SocketAddress {}

    static class TestAssociation extends Association {
        TestAssociation(int assocID, int maxInStreams, int maxOutStreams) {
            super(assocID, maxInStreams, maxOutStreams);
        }
    }

          //--------------------- Infrastructure ---------------------------
    boolean debug = true;
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void check(boolean cond, String failMessage) {if (cond) pass();
          else fail(failMessage);}
    void debug(String message) {if(debug) { System.out.println(message); }  }
    public static void main(String[] args) throws Throwable {
        Class<?> k = new Object(){}.getClass().getEnclosingClass();
        try {k.getMethod("instanceMain",String[].class)
                .invoke( k.newInstance(), (Object) args);}
        catch (Throwable e) {throw e.getCause();}}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
