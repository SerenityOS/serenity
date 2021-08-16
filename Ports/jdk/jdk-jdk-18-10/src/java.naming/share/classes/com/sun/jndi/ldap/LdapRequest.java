/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.jndi.ldap;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.naming.CommunicationException;
import javax.naming.NamingException;
import java.util.concurrent.TimeUnit;

final class LdapRequest {

    private static final BerDecoder EOF = new BerDecoder(new byte[]{}, -1, 0);
    private static final String CLOSE_MSG = "LDAP connection has been closed";
    private static final String TIMEOUT_MSG_FMT = "LDAP response read timed out, timeout used: %d ms.";

    LdapRequest next;   // Set/read in synchronized Connection methods
    final int msgId;          // read-only

    private final BlockingQueue<BerDecoder> replies;
    private volatile boolean cancelled;
    private volatile boolean closed;
    private volatile boolean completed;
    private final boolean pauseAfterReceipt;

    LdapRequest(int msgId, boolean pause, int replyQueueCapacity) {
        this.msgId = msgId;
        this.pauseAfterReceipt = pause;
        if (replyQueueCapacity == -1) {
            this.replies = new LinkedBlockingQueue<>();
        } else {
            this.replies = new LinkedBlockingQueue<>(8 * replyQueueCapacity / 10);
        }
    }

    void cancel() {
        cancelled = true;
        replies.offer(EOF);
    }

    synchronized void close() {
        closed = true;
        replies.offer(EOF);
    }

    private boolean isClosed() {
        return closed && (replies.size() == 0 || replies.peek() == EOF);
    }

    synchronized boolean addReplyBer(BerDecoder ber) {
        // check the closed boolean value here as we don't want anything
        // to be added to the queue after close() has been called.
        if (cancelled || closed) {
            return false;
        }

        // peek at the BER buffer to check if it is a SearchResultDone PDU
        try {
            ber.parseSeq(null);
            ber.parseInt();
            completed = (ber.peekByte() == LdapClient.LDAP_REP_RESULT);
        } catch (IOException e) {
            // ignore
        }
        ber.reset();

        // Add a new reply to the queue of unprocessed replies.
        try {
            replies.put(ber);
        } catch (InterruptedException e) {
            // ignore
        }

        return pauseAfterReceipt;
    }

    /**
     * Read reply BER
     * @param millis timeout, infinite if the value is negative
     * @return BerDecoder if reply was read successfully
     * @throws CommunicationException request has been canceled and request does not need to be abandoned
     * @throws NamingException request has been closed or timed out. Request does need to be abandoned
     * @throws InterruptedException LDAP operation has been interrupted
     */
    BerDecoder getReplyBer(long millis) throws NamingException,
                                               InterruptedException {
        if (cancelled) {
            throw new CommunicationException("Request: " + msgId +
                " cancelled");
        }
        if (isClosed()) {
            throw new NamingException(CLOSE_MSG);
        }

        BerDecoder result = millis > 0 ?
                replies.poll(millis, TimeUnit.MILLISECONDS) : replies.take();

        if (cancelled) {
            throw new CommunicationException("Request: " + msgId +
                " cancelled");
        }

        // poll from 'replies' blocking queue ended-up with timeout
        if (result == null) {
            throw new NamingException(String.format(TIMEOUT_MSG_FMT, millis));
        }
        // Unexpected EOF can be caused by connection closure or cancellation
        if (result == EOF) {
            throw new NamingException(CLOSE_MSG);
        }
        return result;
    }

    boolean hasSearchCompleted() {
        return completed;
    }
}
