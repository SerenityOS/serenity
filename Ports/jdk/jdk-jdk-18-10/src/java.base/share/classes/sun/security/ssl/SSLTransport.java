/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLHandshakeException;

/**
 * Interface for SSL/(D)TLS transportation.
 */
interface SSLTransport {

    /**
     * Returns the host name of the peer.
     *
     * @return  the host name of the peer, or null if nothing is
     *          available.
     */
    String getPeerHost();

    /**
     * Returns the port number of the peer.
     *
     * @return  the port number of the peer, or -1 if nothing is
     *          available.
     */
    int getPeerPort();

    /**
     * Shutdown the transport.
     */
    default void shutdown() throws IOException {
        // blank
    }

    /**
     * Return true if delegated tasks used for handshaking operations.
     *
     * @return true if delegated tasks used for handshaking operations.
     */
    boolean useDelegatedTask();

    /**
     * Decodes an array of SSL/(D)TLS network source data into the
     * destination application data buffers.
     *
     * For SSL/TLS connections, if no source data, the network data may be
     * received from the underlying SSL/TLS input stream.
     *
     * @param context      the transportation context
     * @param srcs         an array of {@code ByteBuffers} containing the
     *                      inbound network data
     * @param srcsOffset   The offset within the {@code srcs} buffer array
     *                      of the first buffer from which bytes are to be
     *                      retrieved; it must be non-negative and no larger
     *                      than {@code srcs.length}.
     * @param srcsLength   The maximum number of {@code srcs} buffers to be
     *                      accessed; it must be non-negative and no larger than
     *                      {@code srcs.length}&nbsp;-&nbsp;{@code srcsOffset}.
     * @param dsts         an array of {@code ByteBuffers} to hold inbound
     *                      application data
     * @param dstsOffset   The offset within the {@code dsts} buffer array
     *                      of the first buffer from which bytes are to be
     *                      placed; it must be non-negative and no larger
     *                      than {@code dsts.length}.
     * @param dstsLength   The maximum number of {@code dsts} buffers to be
     *                      accessed; it must be non-negative and no larger than
     *                      {@code dsts.length}&nbsp;-&nbsp;{@code dstsOffset}.
     *
     * @return             a {@code Plaintext} describing the result of
     *                      the operation
     * @throws IOException if a problem was encountered while receiving or
     *                      decoding networking data
     */
    static Plaintext decode(TransportContext context,
        ByteBuffer[] srcs, int srcsOffset, int srcsLength,
        ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws IOException {

        Plaintext[] plaintexts;
        try {
            plaintexts =
                    context.inputRecord.decode(srcs, srcsOffset, srcsLength);
        } catch (UnsupportedOperationException unsoe) {         // SSLv2Hello
            // Code to deliver SSLv2 error message for SSL/TLS connections.
            if (!context.sslContext.isDTLS()) {
                context.outputRecord.encodeV2NoCipher();
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.finest("may be talking to SSLv2");
                }
            }

            throw context.fatal(Alert.UNEXPECTED_MESSAGE, unsoe);
        } catch (AEADBadTagException bte) {
            throw context.fatal(Alert.BAD_RECORD_MAC, bte);
        } catch (BadPaddingException bpe) {
            /*
             * The basic SSLv3 record protection involves (optional)
             * encryption for privacy, and an integrity check ensuring
             * data origin authentication.  We do them both here, and
             * throw a fatal alert if the integrity check fails.
             */
             Alert alert = (context.handshakeContext != null) ?
                     Alert.HANDSHAKE_FAILURE :
                     Alert.BAD_RECORD_MAC;
            throw context.fatal(alert, bpe);
        } catch (SSLHandshakeException she) {
            // may be record sequence number overflow
            throw context.fatal(Alert.HANDSHAKE_FAILURE, she);
        } catch (EOFException eofe) {
            // rethrow EOFException, the call will handle it if neede.
            throw eofe;
        } catch (InterruptedIOException | SocketException se) {
            // don't close the Socket in case of timeouts or interrupts or SocketException.
            throw se;
        } catch (IOException ioe) {
            throw context.fatal(Alert.UNEXPECTED_MESSAGE, ioe);
        }

        if (plaintexts == null || plaintexts.length == 0) {
            // Connection closed or record should be discarded.
            return Plaintext.PLAINTEXT_NULL;
        }

        Plaintext finalPlaintext = Plaintext.PLAINTEXT_NULL;
        for (Plaintext plainText : plaintexts) {
            // plainText should never be null for TLS protocols
            if (plainText == Plaintext.PLAINTEXT_NULL) {
                // Only happens for DTLS protocols.
                //
                // Received a retransmitted flight, and need to retransmit the
                // previous delivered handshake flight messages.
                if (context.handshakeContext != null &&
                    context.handshakeContext.sslConfig.enableRetransmissions &&
                    context.sslContext.isDTLS()) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,verbose")) {
                        SSLLogger.finest("retransmited handshake flight");
                    }

                    context.outputRecord.launchRetransmission();
                }   // Otherwise, discard the retransmitted flight.
            } else if (plainText != null &&
                    plainText.contentType != ContentType.APPLICATION_DATA.id) {
                context.dispatch(plainText);
            }

            if (plainText == null) {
                plainText = Plaintext.PLAINTEXT_NULL;
            } else if (plainText.contentType ==
                            ContentType.APPLICATION_DATA.id) {
                // check handshake status
                //
                // Note that JDK does not support 0-RTT yet.  Otherwise, it is
                // needed to check early_data.
                if (!context.isNegotiated) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,verbose")) {
                        SSLLogger.warning("unexpected application data " +
                            "before handshake completion");
                    }

                    throw context.fatal(Alert.UNEXPECTED_MESSAGE,
                        "Receiving application data before handshake complete");
                }

                // Fill the destination buffers.
                if ((dsts != null) && (dstsLength > 0)) {
                    ByteBuffer fragment = plainText.fragment;
                    int remains = fragment.remaining();

                    // Should have enough room in the destination buffers.
                    int limit = dstsOffset + dstsLength;
                    for (int i = dstsOffset;
                            ((i < limit) && (remains > 0)); i++) {

                        int amount = Math.min(dsts[i].remaining(), remains);
                        fragment.limit(fragment.position() + amount);
                        dsts[i].put(fragment);
                        remains -= amount;

                        if (!dsts[i].hasRemaining()) {
                            dstsOffset++;
                        }
                    }

                    if (remains > 0) {
                        throw context.fatal(Alert.INTERNAL_ERROR,
                            "no sufficient room in the destination buffers");
                    }
                }
            }

            finalPlaintext = plainText;
        }

        return finalPlaintext;
    }
}
