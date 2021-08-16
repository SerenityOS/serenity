/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLKeyException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSession;

/**
 * Implementation of an non-blocking SSLEngine.
 *
 * @author Brad Wetmore
 */
final class SSLEngineImpl extends SSLEngine implements SSLTransport {
    private final SSLContextImpl        sslContext;
    final TransportContext              conContext;
    private final ReentrantLock         engineLock = new ReentrantLock();

    /**
     * Constructor for an SSLEngine from SSLContext, without
     * host/port hints.
     *
     * This Engine will not be able to cache sessions, but must renegotiate
     * everything by hand.
     */
    SSLEngineImpl(SSLContextImpl sslContext) {
        this(sslContext, null, -1);
    }

    /**
     * Constructor for an SSLEngine from SSLContext.
     */
    SSLEngineImpl(SSLContextImpl sslContext,
            String host, int port) {
        super(host, port);
        this.sslContext = sslContext;
        HandshakeHash handshakeHash = new HandshakeHash();
        if (sslContext.isDTLS()) {
            this.conContext = new TransportContext(sslContext, this,
                    new DTLSInputRecord(handshakeHash),
                    new DTLSOutputRecord(handshakeHash));
        } else {
            this.conContext = new TransportContext(sslContext, this,
                    new SSLEngineInputRecord(handshakeHash),
                    new SSLEngineOutputRecord(handshakeHash));
        }

        // Server name indication is a connection scope extension.
        if (host != null) {
            this.conContext.sslConfig.serverNames =
                    Utilities.addToSNIServerNameList(
                            conContext.sslConfig.serverNames, host);
        }
    }

    @Override
    public void beginHandshake() throws SSLException {
        engineLock.lock();
        try {
            if (conContext.isUnsureMode) {
                throw new IllegalStateException(
                        "Client/Server mode has not yet been set.");
            }

            try {
                conContext.kickstart();
            } catch (IOException ioe) {
                throw conContext.fatal(Alert.HANDSHAKE_FAILURE,
                    "Couldn't kickstart handshaking", ioe);
            } catch (Exception ex) {     // including RuntimeException
                throw conContext.fatal(Alert.INTERNAL_ERROR,
                    "Fail to begin handshake", ex);
            }
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] appData,
            int offset, int length, ByteBuffer netData) throws SSLException {
        return wrap(appData, offset, length, new ByteBuffer[]{ netData }, 0, 1);
    }

    // @Override
    public SSLEngineResult wrap(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws SSLException {

        engineLock.lock();
        try {
            if (conContext.isUnsureMode) {
                throw new IllegalStateException(
                        "Client/Server mode has not yet been set.");
            }

            // See if the handshaker needs to report back some SSLException.
            checkTaskThrown();

            // check parameters
            checkParams(srcs, srcsOffset, srcsLength,
                    dsts, dstsOffset, dstsLength);

            try {
                return writeRecord(
                    srcs, srcsOffset, srcsLength, dsts, dstsOffset, dstsLength);
            } catch (SSLProtocolException spe) {
                // may be an unexpected handshake message
                throw conContext.fatal(Alert.UNEXPECTED_MESSAGE, spe);
            } catch (IOException ioe) {
                throw conContext.fatal(Alert.INTERNAL_ERROR,
                    "problem wrapping app data", ioe);
            } catch (Exception ex) {     // including RuntimeException
                throw conContext.fatal(Alert.INTERNAL_ERROR,
                    "Fail to wrap application data", ex);
            }
        } finally {
            engineLock.unlock();
        }
    }

    private SSLEngineResult writeRecord(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws IOException {

        // See note on TransportContext.needHandshakeFinishedStatus.
        if (conContext.needHandshakeFinishedStatus) {
            conContext.needHandshakeFinishedStatus = false;
            return new SSLEngineResult(
                    Status.OK, HandshakeStatus.FINISHED, 0, 0);
        }

        // May need to deliver cached records.
        if (isOutboundDone()) {
            return new SSLEngineResult(
                    Status.CLOSED, conContext.getHandshakeStatus(), 0, 0);
        }

        HandshakeContext hc = conContext.handshakeContext;
        HandshakeStatus hsStatus = null;
        if (!conContext.isNegotiated && !conContext.isBroken &&
                !conContext.isInboundClosed() &&
                !conContext.isOutboundClosed()) {
            conContext.kickstart();

            hsStatus = conContext.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_UNWRAP) {
                /*
                 * For DTLS, if the handshake state is
                 * HandshakeStatus.NEED_UNWRAP, a call to SSLEngine.wrap()
                 * means that the previous handshake packets (if delivered)
                 * get lost, and need retransmit the handshake messages.
                 */
                if (!sslContext.isDTLS() || hc == null ||
                        !hc.sslConfig.enableRetransmissions ||
                        conContext.outputRecord.firstMessage) {

                    return new SSLEngineResult(Status.OK, hsStatus, 0, 0);
                }   // otherwise, need retransmission
            }
        }

        if (hsStatus == null) {
            hsStatus = conContext.getHandshakeStatus();
        }

        /*
         * If we have a task outstanding, this *MUST* be done before
         * doing any more wrapping, because we could be in the middle
         * of receiving a handshake message, for example, a finished
         * message which would change the ciphers.
         */
        if (hsStatus == HandshakeStatus.NEED_TASK) {
            return new SSLEngineResult(Status.OK, hsStatus, 0, 0);
        }

        int dstsRemains = 0;
        for (int i = dstsOffset; i < dstsOffset + dstsLength; i++) {
            dstsRemains += dsts[i].remaining();
        }

        // Check destination buffer size.
        //
        // We can be smarter about using smaller buffer sizes later.  For
        // now, force it to be large enough to handle any valid record.
        if (dstsRemains < conContext.conSession.getPacketBufferSize()) {
            return new SSLEngineResult(
                Status.BUFFER_OVERFLOW, conContext.getHandshakeStatus(), 0, 0);
        }

        int srcsRemains = 0;
        for (int i = srcsOffset; i < srcsOffset + srcsLength; i++) {
            srcsRemains += srcs[i].remaining();
        }

        Ciphertext ciphertext = null;
        try {
            // Acquire the buffered to-be-delivered records or retransmissions.
            //
            // May have buffered records, or need retransmission if handshaking.
            if (!conContext.outputRecord.isEmpty() || (hc != null &&
                    hc.sslConfig.enableRetransmissions &&
                    hc.sslContext.isDTLS() &&
                    hsStatus == HandshakeStatus.NEED_UNWRAP)) {
                ciphertext = encode(null, 0, 0,
                        dsts, dstsOffset, dstsLength);
            }

            if (ciphertext == null && srcsRemains != 0) {
                ciphertext = encode(srcs, srcsOffset, srcsLength,
                        dsts, dstsOffset, dstsLength);
            }
        } catch (IOException ioe) {
            if (ioe instanceof SSLException) {
                throw ioe;
            } else {
                throw new SSLException("Write problems", ioe);
            }
        }

        /*
         * Check for status.
         */
        Status status = (isOutboundDone() ? Status.CLOSED : Status.OK);
        if (ciphertext != null && ciphertext.handshakeStatus != null) {
            hsStatus = ciphertext.handshakeStatus;
        } else {
            hsStatus = conContext.getHandshakeStatus();
            if (ciphertext == null && !conContext.isNegotiated &&
                    conContext.isInboundClosed() &&
                    hsStatus == HandshakeStatus.NEED_WRAP) {
                // Even the outboud is open, no futher data could be wrapped as:
                //     1. the outbound is empty
                //     2. no negotiated connection
                //     3. the inbound has closed, cannot complete the handshake
                //
                // Mark the engine as closed if the handshake status is
                // NEED_WRAP. Otherwise, it could lead to dead loops in
                // applications.
                status = Status.CLOSED;
            }
        }

        int deltaSrcs = srcsRemains;
        for (int i = srcsOffset; i < srcsOffset + srcsLength; i++) {
            deltaSrcs -= srcs[i].remaining();
        }

        int deltaDsts = dstsRemains;
        for (int i = dstsOffset; i < dstsOffset + dstsLength; i++) {
            deltaDsts -= dsts[i].remaining();
        }

        return new SSLEngineResult(status, hsStatus, deltaSrcs, deltaDsts,
                ciphertext != null ? ciphertext.recordSN : -1L);
    }

    private Ciphertext encode(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws IOException {

        Ciphertext ciphertext;
        try {
            ciphertext = conContext.outputRecord.encode(
                srcs, srcsOffset, srcsLength, dsts, dstsOffset, dstsLength);
        } catch (SSLHandshakeException she) {
            // may be record sequence number overflow
            throw conContext.fatal(Alert.HANDSHAKE_FAILURE, she);
        } catch (IOException e) {
            throw conContext.fatal(Alert.UNEXPECTED_MESSAGE, e);
        }

        if (ciphertext == null) {
            return null;
        }

        // Is the handshake completed?
        boolean needRetransmission =
                conContext.sslContext.isDTLS() &&
                conContext.handshakeContext != null &&
                conContext.handshakeContext.sslConfig.enableRetransmissions;
        HandshakeStatus hsStatus =
                tryToFinishHandshake(ciphertext.contentType);
        if (needRetransmission &&
                hsStatus == HandshakeStatus.FINISHED &&
                conContext.sslContext.isDTLS() &&
                ciphertext.handshakeType == SSLHandshake.FINISHED.id) {
            // Retransmit the last flight for DTLS.
            //
            // The application data transactions may begin immediately
            // after the last flight.  If the last flight get lost, the
            // application data may be discarded accordingly.  As could
            // be an issue for some applications.  This impact can be
            // mitigated by sending the last fligth twice.
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,verbose")) {
                SSLLogger.finest("retransmit the last flight messages");
            }

            conContext.outputRecord.launchRetransmission();
            hsStatus = HandshakeStatus.NEED_WRAP;
        }

        if (hsStatus == null) {
            hsStatus = conContext.getHandshakeStatus();
        }

        // Is the sequence number is nearly overflow?
        if (conContext.outputRecord.seqNumIsHuge() ||
                conContext.outputRecord.writeCipher.atKeyLimit()) {
            hsStatus = tryKeyUpdate(hsStatus);
        }

        // Check if NewSessionTicket PostHandshake message needs to be sent
        if (conContext.conSession.updateNST &&
                !conContext.sslConfig.isClientMode) {
            hsStatus = tryNewSessionTicket(hsStatus);
        }

        // update context status
        ciphertext.handshakeStatus = hsStatus;

        return ciphertext;
    }

    private HandshakeStatus tryToFinishHandshake(byte contentType) {
        HandshakeStatus hsStatus = null;
        if ((contentType == ContentType.HANDSHAKE.id) &&
                conContext.outputRecord.isEmpty()) {
            if (conContext.handshakeContext == null) {
                hsStatus = HandshakeStatus.FINISHED;
            } else if (conContext.isPostHandshakeContext()) {
                // unlikely, but just in case.
                hsStatus = conContext.finishPostHandshake();
            } else if (conContext.handshakeContext.handshakeFinished) {
                hsStatus = conContext.finishHandshake();
            }
        }   // Otherwise, the followed call to getHSStatus() will help.

        return hsStatus;
    }

    /**
     * Try key update for sequence number wrap or key usage limit.
     *
     * Note that in order to maintain the handshake status properly, we check
     * the sequence number and key usage limit after the last record
     * reading/writing process.
     *
     * As we request renegotiation or close the connection for wrapped sequence
     * number when there is enough sequence number space left to handle a few
     * more records, so the sequence number of the last record cannot be
     * wrapped.
     */
    private HandshakeStatus tryKeyUpdate(
            HandshakeStatus currentHandshakeStatus) throws IOException {
        // Don't bother to kickstart if handshaking is in progress, or if the
        // connection is not duplex-open.
        if ((conContext.handshakeContext == null) &&
                !conContext.isOutboundClosed() &&
                !conContext.isInboundClosed() &&
                !conContext.isBroken) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("trigger key update");
            }
            beginHandshake();
            return conContext.getHandshakeStatus();
        }

        return currentHandshakeStatus;
    }

    // Try to generate a PostHandshake NewSessionTicket message.  This is
    // TLS 1.3 only.
    private HandshakeStatus tryNewSessionTicket(
            HandshakeStatus currentHandshakeStatus) throws IOException {
        // Don't bother to kickstart if handshaking is in progress, or if the
        // connection is not duplex-open.
        if ((conContext.handshakeContext == null) &&
                conContext.protocolVersion.useTLS13PlusSpec() &&
                !conContext.isOutboundClosed() &&
                !conContext.isInboundClosed() &&
                !conContext.isBroken) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("trigger NST");
            }
            conContext.conSession.updateNST = false;
            NewSessionTicket.t13PosthandshakeProducer.produce(
                    new PostHandshakeContext(conContext));
            return conContext.getHandshakeStatus();
        }

        return currentHandshakeStatus;
    }

    private static void checkParams(
            ByteBuffer[] srcs, int srcsOffset, int srcsLength,
            ByteBuffer[] dsts, int dstsOffset, int dstsLength) {

        if ((srcs == null) || (dsts == null)) {
            throw new IllegalArgumentException(
                    "source or destination buffer is null");
        }

        if ((dstsOffset < 0) || (dstsLength < 0) ||
                (dstsOffset > dsts.length - dstsLength)) {
            throw new IndexOutOfBoundsException(
                    "index out of bound of the destination buffers");
        }

        if ((srcsOffset < 0) || (srcsLength < 0) ||
                (srcsOffset > srcs.length - srcsLength)) {
            throw new IndexOutOfBoundsException(
                    "index out of bound of the source buffers");
        }

        for (int i = dstsOffset; i < dstsOffset + dstsLength; i++) {
            if (dsts[i] == null) {
                throw new IllegalArgumentException(
                        "destination buffer[" + i + "] == null");
            }

            /*
             * Make sure the destination bufffers are writable.
             */
            if (dsts[i].isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
        }

        for (int i = srcsOffset; i < srcsOffset + srcsLength; i++) {
            if (srcs[i] == null) {
                throw new IllegalArgumentException(
                        "source buffer[" + i + "] == null");
            }
        }
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src,
            ByteBuffer[] dsts, int offset, int length) throws SSLException {
        return unwrap(
                new ByteBuffer[]{src}, 0, 1, dsts, offset, length);
    }

    // @Override
    public SSLEngineResult unwrap(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws SSLException {

        engineLock.lock();
        try {
            if (conContext.isUnsureMode) {
                throw new IllegalStateException(
                        "Client/Server mode has not yet been set.");
            }

            // See if the handshaker needs to report back some SSLException.
            checkTaskThrown();

            // check parameters
            checkParams(srcs, srcsOffset, srcsLength,
                    dsts, dstsOffset, dstsLength);

            try {
                return readRecord(
                    srcs, srcsOffset, srcsLength, dsts, dstsOffset, dstsLength);
            } catch (SSLProtocolException spe) {
                // may be an unexpected handshake message
                throw conContext.fatal(Alert.UNEXPECTED_MESSAGE,
                        spe.getMessage(), spe);
            } catch (IOException ioe) {
                /*
                 * Don't reset position so it looks like we didn't
                 * consume anything.  We did consume something, and it
                 * got us into this situation, so report that much back.
                 * Our days of consuming are now over anyway.
                 */
                throw conContext.fatal(Alert.INTERNAL_ERROR,
                        "problem unwrapping net record", ioe);
            } catch (Exception ex) {     // including RuntimeException
                throw conContext.fatal(Alert.INTERNAL_ERROR,
                    "Fail to unwrap network record", ex);
            }
        } finally {
            engineLock.unlock();
        }
    }

    private SSLEngineResult readRecord(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws IOException {

        /*
         * Check if we are closing/closed.
         */
        if (isInboundDone()) {
            return new SSLEngineResult(
                    Status.CLOSED, conContext.getHandshakeStatus(), 0, 0);
        }

        HandshakeStatus hsStatus = null;
        if (!conContext.isNegotiated && !conContext.isBroken &&
                !conContext.isInboundClosed() &&
                !conContext.isOutboundClosed()) {
            conContext.kickstart();

            /*
             * If there's still outbound data to flush, we
             * can return without trying to unwrap anything.
             */
            hsStatus = conContext.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_WRAP) {
                return new SSLEngineResult(Status.OK, hsStatus, 0, 0);
            }
        }

        if (hsStatus == null) {
            hsStatus = conContext.getHandshakeStatus();
        }

        /*
         * If we have a task outstanding, this *MUST* be done before
         * doing any more unwrapping, because we could be in the middle
         * of receiving a handshake message, for example, a finished
         * message which would change the ciphers.
         */
        if (hsStatus == HandshakeStatus.NEED_TASK) {
            return new SSLEngineResult(Status.OK, hsStatus, 0, 0);
        }

        if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN) {
            Plaintext plainText;
            try {
                plainText = decode(null, 0, 0,
                        dsts, dstsOffset, dstsLength);
            } catch (IOException ioe) {
                if (ioe instanceof SSLException) {
                    throw ioe;
                } else {
                    throw new SSLException("readRecord", ioe);
                }
            }

            Status status = (isInboundDone() ? Status.CLOSED : Status.OK);
            if (plainText.handshakeStatus != null) {
                hsStatus = plainText.handshakeStatus;
            } else {
                hsStatus = conContext.getHandshakeStatus();
            }

            return new SSLEngineResult(
                    status, hsStatus, 0, 0, plainText.recordSN);
        }

        int srcsRemains = 0;
        for (int i = srcsOffset; i < srcsOffset + srcsLength; i++) {
            srcsRemains += srcs[i].remaining();
        }

        if (srcsRemains == 0) {
            return new SSLEngineResult(
                Status.BUFFER_UNDERFLOW, hsStatus, 0, 0);
        }

        /*
         * Check the packet to make sure enough is here.
         * This will also indirectly check for 0 len packets.
         */
        int packetLen;
        try {
            packetLen = conContext.inputRecord.bytesInCompletePacket(
                    srcs, srcsOffset, srcsLength);
        } catch (SSLException ssle) {
            // Need to discard invalid records for DTLS protocols.
            if (sslContext.isDTLS()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,verbose")) {
                    SSLLogger.finest("Discard invalid DTLS records", ssle);
                }

                // invalid, discard the entire data [section 4.1.2.7, RFC 6347]
                for (int i = srcsOffset; i < srcsOffset + srcsLength; i++) {
                    srcs[i].position(srcs[i].limit());
                }

                Status status = (isInboundDone() ? Status.CLOSED : Status.OK);
                if (hsStatus == null) {
                    hsStatus = conContext.getHandshakeStatus();
                }

                return new SSLEngineResult(status, hsStatus, srcsRemains, 0, -1L);
            } else {
                throw ssle;
            }
        }

        // Is this packet bigger than SSL/TLS normally allows?
        if (packetLen > conContext.conSession.getPacketBufferSize()) {
            int largestRecordSize = sslContext.isDTLS() ?
                    DTLSRecord.maxRecordSize : SSLRecord.maxLargeRecordSize;
            if ((packetLen <= largestRecordSize) && !sslContext.isDTLS()) {
                // Expand the expected maximum packet/application buffer
                // sizes.
                //
                // Only apply to SSL/TLS protocols.

                // Old behavior: shall we honor the System Property
                // "jsse.SSLEngine.acceptLargeFragments" if it is "false"?
                conContext.conSession.expandBufferSizes();
            }

            // check the packet again
            largestRecordSize = conContext.conSession.getPacketBufferSize();
            if (packetLen > largestRecordSize) {
                throw new SSLProtocolException(
                        "Input record too big: max = " +
                        largestRecordSize + " len = " + packetLen);
            }
        }

        /*
         * Check for OVERFLOW.
         *
         * Delay enforcing the application buffer free space requirement
         * until after the initial handshaking.
         */
        int dstsRemains = 0;
        for (int i = dstsOffset; i < dstsOffset + dstsLength; i++) {
            dstsRemains += dsts[i].remaining();
        }

        if (conContext.isNegotiated) {
            int FragLen =
                    conContext.inputRecord.estimateFragmentSize(packetLen);
            if (FragLen > dstsRemains) {
                return new SSLEngineResult(
                        Status.BUFFER_OVERFLOW, hsStatus, 0, 0);
            }
        }

        // check for UNDERFLOW.
        if ((packetLen == -1) || (srcsRemains < packetLen)) {
            return new SSLEngineResult(Status.BUFFER_UNDERFLOW, hsStatus, 0, 0);
        }

        /*
         * We're now ready to actually do the read.
         */
        Plaintext plainText;
        try {
            plainText = decode(srcs, srcsOffset, srcsLength,
                            dsts, dstsOffset, dstsLength);
        } catch (IOException ioe) {
            if (ioe instanceof SSLException) {
                throw ioe;
            } else {
                throw new SSLException("readRecord", ioe);
            }
        }

        /*
         * Check the various condition that we could be reporting.
         *
         * It's *possible* something might have happened between the
         * above and now, but it was better to minimally lock "this"
         * during the read process.  We'll return the current
         * status, which is more representative of the current state.
         *
         * status above should cover:  FINISHED, NEED_TASK
         */
        Status status = (isInboundDone() ? Status.CLOSED : Status.OK);
        if (plainText.handshakeStatus != null) {
            hsStatus = plainText.handshakeStatus;
        } else {
            hsStatus = conContext.getHandshakeStatus();
        }

        int deltaNet = srcsRemains;
        for (int i = srcsOffset; i < srcsOffset + srcsLength; i++) {
            deltaNet -= srcs[i].remaining();
        }

        int deltaApp = dstsRemains;
        for (int i = dstsOffset; i < dstsOffset + dstsLength; i++) {
            deltaApp -= dsts[i].remaining();
        }

        return new SSLEngineResult(
                status, hsStatus, deltaNet, deltaApp, plainText.recordSN);
    }

    private Plaintext decode(
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws IOException {

        Plaintext pt = SSLTransport.decode(conContext,
                            srcs, srcsOffset, srcsLength,
                            dsts, dstsOffset, dstsLength);

        // Is the handshake completed?
        if (pt != Plaintext.PLAINTEXT_NULL) {
            HandshakeStatus hsStatus = tryToFinishHandshake(pt.contentType);
            if (hsStatus == null) {
                pt.handshakeStatus = conContext.getHandshakeStatus();
            } else {
                pt.handshakeStatus = hsStatus;
            }

            // Is the sequence number is nearly overflow?
            if (conContext.inputRecord.seqNumIsHuge() ||
                    conContext.inputRecord.readCipher.atKeyLimit()) {
                pt.handshakeStatus =
                        tryKeyUpdate(pt.handshakeStatus);
            }
        }

        return pt;
    }

    @Override
    public Runnable getDelegatedTask() {
        engineLock.lock();
        try {
            if (conContext.handshakeContext != null && // PRE or POST handshake
                    !conContext.handshakeContext.taskDelegated &&
                    !conContext.handshakeContext.delegatedActions.isEmpty()) {
                conContext.handshakeContext.taskDelegated = true;
                return new DelegatedTask(this);
            }
        } finally {
            engineLock.unlock();
        }

        return null;
    }

    @Override
    public void closeInbound() throws SSLException {
        engineLock.lock();
        try {
            if (isInboundDone()) {
                return;
            }

            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("Closing inbound of SSLEngine");
            }

            // Is it ready to close inbound?
            //
            // No exception if the initial handshake is not started.
            if (!conContext.isInputCloseNotified &&
                (conContext.isNegotiated ||
                    conContext.handshakeContext != null)) {

                throw conContext.fatal(Alert.INTERNAL_ERROR,
                        "closing inbound before receiving peer's close_notify");
            }

            conContext.closeInbound();
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public boolean isInboundDone() {
        engineLock.lock();
        try {
            return conContext.isInboundClosed();
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void closeOutbound() {
        engineLock.lock();
        try {
            if (conContext.isOutboundClosed()) {
                return;
            }

            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.finest("Closing outbound of SSLEngine");
            }

            conContext.closeOutbound();
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public boolean isOutboundDone() {
        engineLock.lock();
        try {
            return conContext.isOutboundDone();
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return CipherSuite.namesOf(sslContext.getSupportedCipherSuites());
    }

    @Override
    public String[] getEnabledCipherSuites() {
        engineLock.lock();
        try {
            return CipherSuite.namesOf(conContext.sslConfig.enabledCipherSuites);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        engineLock.lock();
        try {
            conContext.sslConfig.enabledCipherSuites =
                    CipherSuite.validValuesOf(suites);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public String[] getSupportedProtocols() {
        return ProtocolVersion.toStringArray(
                sslContext.getSupportedProtocolVersions());
    }

    @Override
    public String[] getEnabledProtocols() {
        engineLock.lock();
        try {
            return ProtocolVersion.toStringArray(
                    conContext.sslConfig.enabledProtocols);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        engineLock.lock();
        try {
            if (protocols == null) {
                throw new IllegalArgumentException("Protocols cannot be null");
            }

            conContext.sslConfig.enabledProtocols =
                    ProtocolVersion.namesOf(protocols);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public SSLSession getSession() {
        engineLock.lock();
        try {
            return conContext.conSession;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public SSLSession getHandshakeSession() {
        engineLock.lock();
        try {
            return conContext.handshakeContext == null ?
                    null : conContext.handshakeContext.handshakeSession;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
        engineLock.lock();
        try {
            return conContext.getHandshakeStatus();
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void setUseClientMode(boolean mode) {
        engineLock.lock();
        try {
            conContext.setUseClientMode(mode);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public boolean getUseClientMode() {
        engineLock.lock();
        try {
            return conContext.sslConfig.isClientMode;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        engineLock.lock();
        try {
            conContext.sslConfig.clientAuthType =
                    (need ? ClientAuthType.CLIENT_AUTH_REQUIRED :
                            ClientAuthType.CLIENT_AUTH_NONE);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public boolean getNeedClientAuth() {
        engineLock.lock();
        try {
            return (conContext.sslConfig.clientAuthType ==
                            ClientAuthType.CLIENT_AUTH_REQUIRED);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void setWantClientAuth(boolean want) {
        engineLock.lock();
        try {
            conContext.sslConfig.clientAuthType =
                    (want ? ClientAuthType.CLIENT_AUTH_REQUESTED :
                            ClientAuthType.CLIENT_AUTH_NONE);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public boolean getWantClientAuth() {
        engineLock.lock();
        try {
            return (conContext.sslConfig.clientAuthType ==
                            ClientAuthType.CLIENT_AUTH_REQUESTED);
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        engineLock.lock();
        try {
            conContext.sslConfig.enableSessionCreation = flag;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public boolean getEnableSessionCreation() {
        engineLock.lock();
        try {
            return conContext.sslConfig.enableSessionCreation;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public SSLParameters getSSLParameters() {
        engineLock.lock();
        try {
            return conContext.sslConfig.getSSLParameters();
        } finally {
            engineLock.unlock();
        }
   }

    @Override
    public void setSSLParameters(SSLParameters params) {
        engineLock.lock();
        try {
            conContext.sslConfig.setSSLParameters(params);

            if (conContext.sslConfig.maximumPacketSize != 0) {
                conContext.outputRecord.changePacketSize(
                        conContext.sslConfig.maximumPacketSize);
            }
        } finally {
            engineLock.unlock();
        }
   }

    @Override
    public String getApplicationProtocol() {
        engineLock.lock();
        try {
            return conContext.applicationProtocol;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public String getHandshakeApplicationProtocol() {
        engineLock.lock();
        try {
            return conContext.handshakeContext == null ?
                    null : conContext.handshakeContext.applicationProtocol;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLEngine, List<String>, String> selector) {
        engineLock.lock();
        try {
            conContext.sslConfig.engineAPSelector = selector;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public BiFunction<SSLEngine, List<String>, String>
            getHandshakeApplicationProtocolSelector() {
        engineLock.lock();
        try {
            return conContext.sslConfig.engineAPSelector;
        } finally {
            engineLock.unlock();
        }
    }

    @Override
    public boolean useDelegatedTask() {
        return true;
    }

    @Override
    public String toString() {
        return "SSLEngine[" +
                "hostname=" + getPeerHost() +
                ", port=" + getPeerPort() +
                ", " + conContext.conSession +  // SSLSessionImpl.toString()
                "]";
    }

    /*
     * Depending on whether the error was just a warning and the
     * handshaker wasn't closed, or fatal and the handshaker is now
     * null, report back the Exception that happened in the delegated
     * task(s).
     */
    private void checkTaskThrown() throws SSLException {

        Exception exc = null;
        engineLock.lock();
        try {
            // First check the handshake context.
            HandshakeContext hc = conContext.handshakeContext;
            if ((hc != null) && (hc.delegatedThrown != null)) {
                exc = hc.delegatedThrown;
                hc.delegatedThrown = null;
            }

            /*
             * hc.delegatedThrown and conContext.delegatedThrown are most
             * likely the same, but it's possible we could have had a non-fatal
             * exception and thus the new HandshakeContext is still valid
             * (alert warning).  If so, then we may have a secondary exception
             * waiting to be reported from the TransportContext, so we will
             * need to clear that on a successive call. Otherwise, clear it now.
             */
            if (conContext.delegatedThrown != null) {
                if (exc != null) {
                    // hc object comparison
                    if (conContext.delegatedThrown == exc) {
                        // clear if/only if both are the same
                        conContext.delegatedThrown = null;
                    } // otherwise report the hc delegatedThrown
                } else {
                    // Nothing waiting in HandshakeContext, but one is in the
                    // TransportContext.
                    exc = conContext.delegatedThrown;
                    conContext.delegatedThrown = null;
                }
            }
        } finally {
            engineLock.unlock();
        }

        // Anything to report?
        if (exc == null) {
            return;
        }

        // If it wasn't a RuntimeException/SSLException, need to wrap it.
        if (exc instanceof SSLException) {
            throw (SSLException)exc;
        } else if (exc instanceof RuntimeException) {
            throw (RuntimeException)exc;
        } else {
            throw getTaskThrown(exc);
        }
    }

    private static SSLException getTaskThrown(Exception taskThrown) {
        String msg = taskThrown.getMessage();

        if (msg == null) {
            msg = "Delegated task threw Exception or Error";
        }

        if (taskThrown instanceof RuntimeException) {
            throw new RuntimeException(msg, taskThrown);
        } else if (taskThrown instanceof SSLHandshakeException) {
            return (SSLHandshakeException)
                new SSLHandshakeException(msg).initCause(taskThrown);
        } else if (taskThrown instanceof SSLKeyException) {
            return (SSLKeyException)
                new SSLKeyException(msg).initCause(taskThrown);
        } else if (taskThrown instanceof SSLPeerUnverifiedException) {
            return (SSLPeerUnverifiedException)
                new SSLPeerUnverifiedException(msg).initCause(taskThrown);
        } else if (taskThrown instanceof SSLProtocolException) {
            return (SSLProtocolException)
                new SSLProtocolException(msg).initCause(taskThrown);
        } else if (taskThrown instanceof SSLException) {
            return (SSLException)taskThrown;
        } else {
            return new SSLException(msg, taskThrown);
        }
    }

    /**
     * Implement a simple task delegator.
     */
    private static class DelegatedTask implements Runnable {
        private final SSLEngineImpl engine;

        DelegatedTask(SSLEngineImpl engineInstance) {
            this.engine = engineInstance;
        }

        @Override
        public void run() {
            engine.engineLock.lock();
            try {
                HandshakeContext hc = engine.conContext.handshakeContext;
                if (hc == null || hc.delegatedActions.isEmpty()) {
                    return;
                }

                try {
                    @SuppressWarnings("removal")
                    var dummy = AccessController.doPrivileged(
                            new DelegatedAction(hc), engine.conContext.acc);
                } catch (PrivilegedActionException pae) {
                    // Get the handshake context again in case the
                    // handshaking has completed.
                    Exception reportedException = pae.getException();

                    // Report to both the TransportContext...
                    if (engine.conContext.delegatedThrown == null) {
                        engine.conContext.delegatedThrown = reportedException;
                    }

                    // ...and the HandshakeContext in case condition
                    // wasn't fatal and the handshakeContext is still
                    // around.
                    hc = engine.conContext.handshakeContext;
                    if (hc != null) {
                        hc.delegatedThrown = reportedException;
                    } else if (engine.conContext.closeReason != null) {
                        // Update the reason in case there was a previous.
                        engine.conContext.closeReason =
                                getTaskThrown(reportedException);
                    }
                } catch (RuntimeException rte) {
                    // Get the handshake context again in case the
                    // handshaking has completed.

                    // Report to both the TransportContext...
                    if (engine.conContext.delegatedThrown == null) {
                        engine.conContext.delegatedThrown = rte;
                    }

                    // ...and the HandshakeContext in case condition
                    // wasn't fatal and the handshakeContext is still
                    // around.
                    hc = engine.conContext.handshakeContext;
                    if (hc != null) {
                        hc.delegatedThrown = rte;
                    } else if (engine.conContext.closeReason != null) {
                        // Update the reason in case there was a previous.
                        engine.conContext.closeReason = rte;
                    }
                }

                // Get the handshake context again in case the
                // handshaking has completed.
                hc = engine.conContext.handshakeContext;
                if (hc != null) {
                    hc.taskDelegated = false;
                }
            } finally {
                engine.engineLock.unlock();
            }
        }

        private static class DelegatedAction
                implements PrivilegedExceptionAction<Void> {
            final HandshakeContext context;
            DelegatedAction(HandshakeContext context) {
                this.context = context;
            }

            @Override
            public Void run() throws Exception {
                while (!context.delegatedActions.isEmpty()) {
                    Map.Entry<Byte, ByteBuffer> me =
                            context.delegatedActions.poll();
                    if (me != null) {
                        context.dispatch(me.getKey(), me.getValue());
                    }
                }
                return null;
            }
        }
    }
}
