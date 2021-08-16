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

package javax.net.ssl;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.List;
import java.util.function.BiFunction;


/**
 * A class which enables secure communications using protocols such as
 * the Secure Sockets Layer (SSL) or
 * <A HREF="http://www.ietf.org/rfc/rfc2246.txt"> IETF RFC 2246 "Transport
 * Layer Security" (TLS) </A> protocols, but is transport independent.
 * <P>
 * The secure communications modes include: <UL>
 *
 *      <LI> <em>Integrity Protection</em>.  SSL/TLS/DTLS protects against
 *      modification of messages by an active wiretapper.
 *
 *      <LI> <em>Authentication</em>.  In most modes, SSL/TLS/DTLS provides
 *      peer authentication.  Servers are usually authenticated, and
 *      clients may be authenticated as requested by servers.
 *
 *      <LI> <em>Confidentiality (Privacy Protection)</em>.  In most
 *      modes, SSL/TLS/DTLS encrypts data being sent between client and
 *      server.  This protects the confidentiality of data, so that
 *      passive wiretappers won't see sensitive data such as financial
 *      information or personal information of many kinds.
 *
 *      </UL>
 *
 * These kinds of protection are specified by a "cipher suite", which
 * is a combination of cryptographic algorithms used by a given SSL
 * connection.  During the negotiation process, the two endpoints must
 * agree on a cipher suite that is available in both environments.  If
 * there is no such suite in common, no SSL connection can be
 * established, and no data can be exchanged.
 * <P>
 * The cipher suite used is established by a negotiation process called
 * "handshaking".  The goal of this process is to create or rejoin a
 * "session", which may protect many connections over time.  After
 * handshaking has completed, you can access session attributes by
 * using the {@link #getSession()} method.
 * <P>
 * The {@code SSLSocket} class provides much of the same security
 * functionality, but all of the inbound and outbound data is
 * automatically transported using the underlying {@link
 * java.net.Socket Socket}, which by design uses a blocking model.
 * While this is appropriate for many applications, this model does not
 * provide the scalability required by large servers.
 * <P>
 * The primary distinction of an {@code SSLEngine} is that it
 * operates on inbound and outbound byte streams, independent of the
 * transport mechanism.  It is the responsibility of the
 * {@code SSLEngine} user to arrange for reliable I/O transport to
 * the peer.  By separating the SSL/TLS/DTLS abstraction from the I/O
 * transport mechanism, the {@code SSLEngine} can be used for a
 * wide variety of I/O types, such as {@link
 * java.nio.channels.spi.AbstractSelectableChannel#configureBlocking(boolean)
 * non-blocking I/O (polling)}, {@link java.nio.channels.Selector
 * selectable non-blocking I/O}, {@link java.net.Socket Socket} and the
 * traditional Input/OutputStreams, local {@link java.nio.ByteBuffer
 * ByteBuffers} or byte arrays, <A
 * HREF="http://www.jcp.org/en/jsr/detail?id=203"> future asynchronous
 * I/O models </A>, and so on.
 * <P>
 * At a high level, the {@code SSLEngine} appears thus:
 *
 * <pre>
 *                   app data
 *
 *                |           ^
 *                |     |     |
 *                v     |     |
 *           +----+-----|-----+----+
 *           |          |          |
 *           |       SSL|Engine    |
 *   wrap()  |          |          |  unwrap()
 *           | OUTBOUND | INBOUND  |
 *           |          |          |
 *           +----+-----|-----+----+
 *                |     |     ^
 *                |     |     |
 *                v           |
 *
 *                   net data
 * </pre>
 * Application data (also known as plaintext or cleartext) is data which
 * is produced or consumed by an application.  Its counterpart is
 * network data, which consists of either handshaking and/or ciphertext
 * (encrypted) data, and destined to be transported via an I/O
 * mechanism.  Inbound data is data which has been received from the
 * peer, and outbound data is destined for the peer.
 * <P>
 * (In the context of an {@code SSLEngine}, the term "handshake
 * data" is taken to mean any data exchanged to establish and control a
 * secure connection.  Handshake data includes the SSL/TLS/DTLS messages
 * "alert", "change_cipher_spec," and "handshake.")
 * <P>
 * There are five distinct phases to an {@code SSLEngine}.
 *
 * <OL>
 *     <li> Creation - The {@code SSLEngine} has been created and
 *     initialized, but has not yet been used.  During this phase, an
 *     application may set any {@code SSLEngine}-specific settings
 *     (enabled cipher suites, whether the {@code SSLEngine} should
 *     handshake in client or server mode, and so on).  Once
 *     handshaking has begun, though, any new settings (except
 *     client/server mode, see below) will be used for
 *     the next handshake.
 *
 *     <li> Initial Handshake - The initial handshake is a procedure by
 *     which the two peers exchange communication parameters until an
 *     SSLSession is established.  Application data can not be sent during
 *     this phase.
 *
 *     <li> Application Data - Once the communication parameters have
 *     been established and the handshake is complete, application data
 *     may flow through the {@code SSLEngine}.  Outbound
 *     application messages are encrypted and integrity protected,
 *     and inbound messages reverse the process.
 *
 *     <li> Rehandshaking - Either side may request a renegotiation of
 *     the session at any time during the Application Data phase.  New
 *     handshaking data can be intermixed among the application data.
 *     Before starting the rehandshake phase, the application may
 *     reset the SSL/TLS/DTLS communication parameters such as the list of
 *     enabled ciphersuites and whether to use client authentication,
 *     but can not change between client/server modes.  As before, once
 *     handshaking has begun, any new {@code SSLEngine}
 *     configuration settings will not be used until the next
 *     handshake.
 *
 *     <li> Closure - When the connection is no longer needed, the client
 *     and the server applications should each close both sides of their
 *     respective connections.  For {@code SSLEngine} objects, an
 *     application should call {@link SSLEngine#closeOutbound()} and
 *     send any remaining messages to the peer.  Likewise, an application
 *     should receive any remaining messages from the peer before calling
 *     {@link SSLEngine#closeInbound()}.  The underlying transport mechanism
 *     can then be closed after both sides of the {@code SSLEngine} have
 *     been closed.  If the connection is not closed in an orderly manner
 *     (for example {@link SSLEngine#closeInbound()} is called before the
 *     peer's write closure notification has been received), exceptions
 *     will be raised to indicate that an error has occurred.  Once an
 *     engine is closed, it is not reusable: a new {@code SSLEngine}
 *     must be created.
 * </OL>
 * An {@code SSLEngine} is created by calling {@link
 * SSLContext#createSSLEngine()} from an initialized
 * {@code SSLContext}.  Any configuration
 * parameters should be set before making the first call to
 * {@code wrap()}, {@code unwrap()}, or
 * {@code beginHandshake()}.  These methods all trigger the
 * initial handshake.
 * <P>
 * Data moves through the engine by calling {@link #wrap(ByteBuffer,
 * ByteBuffer) wrap()} or {@link #unwrap(ByteBuffer, ByteBuffer)
 * unwrap()} on outbound or inbound data, respectively.  Depending on
 * the state of the {@code SSLEngine}, a {@code wrap()} call
 * may consume application data from the source buffer and may produce
 * network data in the destination buffer.  The outbound data
 * may contain application and/or handshake data.  A call to
 * {@code unwrap()} will examine the source buffer and may
 * advance the handshake if the data is handshaking information, or
 * may place application data in the destination buffer if the data
 * is application.  The state of the underlying SSL/TLS/DTLS algorithm
 * will determine when data is consumed and produced.
 * <P>
 * Calls to {@code wrap()} and {@code unwrap()} return an
 * {@code SSLEngineResult} which indicates the status of the
 * operation, and (optionally) how to interact with the engine to make
 * progress.
 * <P>
 * The {@code SSLEngine} produces/consumes complete SSL/TLS/DTLS
 * packets only, and does not store application data internally between
 * calls to {@code wrap()/unwrap()}.  Thus input and output
 * {@code ByteBuffer}s must be sized appropriately to hold the
 * maximum record that can be produced.  Calls to {@link
 * SSLSession#getPacketBufferSize()} and {@link
 * SSLSession#getApplicationBufferSize()} should be used to determine
 * the appropriate buffer sizes.  The size of the outbound application
 * data buffer generally does not matter.  If buffer conditions do not
 * allow for the proper consumption/production of data, the application
 * must determine (via {@link SSLEngineResult}) and correct the
 * problem, and then try the call again.
 * <P>
 * For example, {@code unwrap()} will return a {@link
 * SSLEngineResult.Status#BUFFER_OVERFLOW} result if the engine
 * determines that there is not enough destination buffer space available.
 * Applications should call {@link SSLSession#getApplicationBufferSize()}
 * and compare that value with the space available in the destination buffer,
 * enlarging the buffer if necessary.  Similarly, if {@code unwrap()}
 * were to return a {@link SSLEngineResult.Status#BUFFER_UNDERFLOW}, the
 * application should call {@link SSLSession#getPacketBufferSize()} to ensure
 * that the source buffer has enough room to hold a record (enlarging if
 * necessary), and then obtain more inbound data.
 *
 * <pre>{@code
 *   SSLEngineResult r = engine.unwrap(src, dst);
 *   switch (r.getStatus()) {
 *   case BUFFER_OVERFLOW:
 *       // Could attempt to drain the dst buffer of any already obtained
 *       // data, but we'll just increase it to the size needed.
 *       int appSize = engine.getSession().getApplicationBufferSize();
 *       ByteBuffer b = ByteBuffer.allocate(appSize + dst.position());
 *       dst.flip();
 *       b.put(dst);
 *       dst = b;
 *       // retry the operation.
 *       break;
 *   case BUFFER_UNDERFLOW:
 *       int netSize = engine.getSession().getPacketBufferSize();
 *       // Resize buffer if needed.
 *       if (netSize > src.capacity()) {
 *           ByteBuffer b = ByteBuffer.allocate(netSize);
 *           src.flip();
 *           b.put(src);
 *           src = b;
 *       }
 *       // Obtain more inbound network data for src,
 *       // then retry the operation.
 *       break;
 *   // other cases: CLOSED, OK.
 *   }
 * }</pre>
 *
 * <P>
 * Unlike {@code SSLSocket}, all methods of SSLEngine are
 * non-blocking.  {@code SSLEngine} implementations may
 * require the results of tasks that may take an extended period of
 * time to complete, or may even block.  For example, a TrustManager
 * may need to connect to a remote certificate validation service,
 * or a KeyManager might need to prompt a user to determine which
 * certificate to use as part of client authentication.  Additionally,
 * creating cryptographic signatures and verifying them can be slow,
 * seemingly blocking.
 * <P>
 * For any operation which may potentially block, the
 * {@code SSLEngine} will create a {@link java.lang.Runnable}
 * delegated task.  When {@code SSLEngineResult} indicates that a
 * delegated task result is needed, the application must call {@link
 * #getDelegatedTask()} to obtain an outstanding delegated task and
 * call its {@link java.lang.Runnable#run() run()} method (possibly using
 * a different thread depending on the compute strategy).  The
 * application should continue obtaining delegated tasks until no more
 * exist, and try the original operation again.
 * <P>
 * At the end of a communication session, applications should properly
 * close the SSL/TLS/DTLS link.  The SSL/TLS/DTLS protocols have closure
 * handshake messages, and these messages should be communicated to the
 * peer before releasing the {@code SSLEngine} and closing the
 * underlying transport mechanism.  A close can be initiated by one of:
 * an SSLException, an inbound closure handshake message, or one of the
 * close methods.  In all cases, closure handshake messages are
 * generated by the engine, and {@code wrap()} should be repeatedly
 * called until the resulting {@code SSLEngineResult}'s status
 * returns "CLOSED", or {@link #isOutboundDone()} returns true.  All
 * data obtained from the {@code wrap()} method should be sent to the
 * peer.
 * <P>
 * {@link #closeOutbound()} is used to signal the engine that the
 * application will not be sending any more data.
 * <P>
 * A peer will signal its intent to close by sending its own closure
 * handshake message.  After this message has been received and
 * processed by the local {@code SSLEngine}'s {@code unwrap()}
 * call, the application can detect the close by calling
 * {@code unwrap()} and looking for a {@code SSLEngineResult}
 * with status "CLOSED", or if {@link #isInboundDone()} returns true.
 * If for some reason the peer closes the communication link without
 * sending the proper SSL/TLS/DTLS closure message, the application can
 * detect the end-of-stream and can signal the engine via {@link
 * #closeInbound()} that there will no more inbound messages to
 * process.  Some applications might choose to require orderly shutdown
 * messages from a peer, in which case they can check that the closure
 * was generated by a handshake message and not by an end-of-stream
 * condition.
 * <P>
 * There are two groups of cipher suites which you will need to know
 * about when managing cipher suites:
 *
 * <UL>
 *      <LI> <em>Supported</em> cipher suites:  all the suites which are
 *      supported by the SSL implementation.  This list is reported
 *      using {@link #getSupportedCipherSuites()}.
 *
 *      <LI> <em>Enabled</em> cipher suites, which may be fewer than
 *      the full set of supported suites.  This group is set using the
 *      {@link #setEnabledCipherSuites(String [])} method, and
 *      queried using the {@link #getEnabledCipherSuites()} method.
 *      Initially, a default set of cipher suites will be enabled on a
 *      new engine that represents the minimum suggested
 *      configuration.
 * </UL>
 *
 * Implementation defaults require that only cipher suites which
 * authenticate servers and provide confidentiality be enabled by
 * default.  Only if both sides explicitly agree to unauthenticated
 * and/or non-private (unencrypted) communications will such a
 * cipher suite be selected.
 * <P>
 * Each SSL/TLS/DTLS connection must have one client and one server, thus
 * each endpoint must decide which role to assume.  This choice determines
 * who begins the handshaking process as well as which type of messages
 * should be sent by each party.  The method {@link
 * #setUseClientMode(boolean)} configures the mode.  Note that the
 * default mode for a new {@code SSLEngine} is provider-specific.
 * Applications should set the mode explicitly before invoking other
 * methods of the {@code SSLEngine}.  Once the initial handshaking has
 * started, an {@code SSLEngine} can not switch between client and server
 * modes, even when performing renegotiations.
 * <P>
 * The ApplicationProtocol {@code String} values returned by the methods
 * in this class are in the network byte representation sent by the peer.
 * The bytes could be directly compared, or converted to its Unicode
 * {code String} format for comparison.
 *
 * <blockquote><pre>
 *     String networkString = sslEngine.getHandshakeApplicationProtocol();
 *     byte[] bytes = networkString.getBytes(StandardCharsets.ISO_8859_1);
 *
 *     //
 *     // Match using bytes:
 *     //
 *     //   "http/1.1"                       (7-bit ASCII values same in UTF-8)
 *     //   MEETEI MAYEK LETTERS "HUK UN I"  (Unicode 0xabcd->0xabcf)
 *     //
 *     String HTTP1_1 = "http/1.1";
 *     byte[] HTTP1_1_BYTES = HTTP1_1.getBytes(StandardCharsets.UTF_8);
 *
 *     byte[] HUK_UN_I_BYTES = new byte[] {
 *         (byte) 0xab, (byte) 0xcd,
 *         (byte) 0xab, (byte) 0xce,
 *         (byte) 0xab, (byte) 0xcf};
 *
 *     if ((Arrays.compare(bytes, HTTP1_1_BYTES) == 0 )
 *             || Arrays.compare(bytes, HUK_UN_I_BYTES) == 0) {
 *        ...
 *     }
 *
 *     //
 *     // Alternatively match using string.equals() if we know the ALPN value
 *     // was encoded from a {@code String} using a certain character set,
 *     // for example {@code UTF-8}.  The ALPN value must first be properly
 *     // decoded to a Unicode {@code String} before use.
 *     //
 *     String unicodeString = new String(bytes, StandardCharsets.UTF_8);
 *     if (unicodeString.equals(HTTP1_1)
 *             || unicodeString.equals("\u005cuabcd\u005cuabce\u005cuabcf")) {
 *         ...
 *     }
 * </pre></blockquote>
 *
 * <P>
 * Applications might choose to process delegated tasks in different
 * threads.  When an {@code SSLEngine}
 * is created, the current {@link java.security.AccessControlContext}
 * is saved.  All future delegated tasks will be processed using this
 * context:  that is, all access control decisions will be made using the
 * context captured at engine creation.
 *
 * <HR>
 *
 * <B>Concurrency Notes</B>:
 * There are two concurrency issues to be aware of:
 *
 * <OL>
 *      <li>The {@code wrap()} and {@code unwrap()} methods
 *      may execute concurrently of each other.
 *
 *      <li> The SSL/TLS/DTLS protocols employ ordered packets.
 *      Applications must take care to ensure that generated packets
 *      are delivered in sequence.  If packets arrive
 *      out-of-order, unexpected or fatal results may occur.
 * <P>
 *      For example:
 *
 *      <pre>
 *              synchronized (outboundLock) {
 *                  sslEngine.wrap(src, dst);
 *                  outboundQueue.put(dst);
 *              }
 *      </pre>
 *
 *      As a corollary, two threads must not attempt to call the same method
 *      (either {@code wrap()} or {@code unwrap()}) concurrently,
 *      because there is no way to guarantee the eventual packet ordering.
 * </OL>
 *
 * @see SSLContext
 * @see SSLSocket
 * @see SSLServerSocket
 * @see SSLSession
 * @see java.net.Socket
 *
 * @since 1.5
 * @author Brad R. Wetmore
 */

public abstract class SSLEngine {

    private String peerHost = null;
    private int peerPort = -1;

    /**
     * Constructor for an {@code SSLEngine} providing no hints
     * for an internal session reuse strategy.
     *
     * @see     SSLContext#createSSLEngine()
     * @see     SSLSessionContext
     */
    protected SSLEngine() {
    }

    /**
     * Constructor for an {@code SSLEngine}.
     * <P>
     * {@code SSLEngine} implementations may use the
     * {@code peerHost} and {@code peerPort} parameters as hints
     * for their internal session reuse strategy.
     * <P>
     * Some cipher suites (such as Kerberos) require remote hostname
     * information. Implementations of this class should use this
     * constructor to use Kerberos.
     * <P>
     * The parameters are not authenticated by the
     * {@code SSLEngine}.
     *
     * @param   peerHost the name of the peer host
     * @param   peerPort the port number of the peer
     * @see     SSLContext#createSSLEngine(String, int)
     * @see     SSLSessionContext
     */
    protected SSLEngine(String peerHost, int peerPort) {
        this.peerHost = peerHost;
        this.peerPort = peerPort;
    }

    /**
     * Returns the host name of the peer.
     * <P>
     * Note that the value is not authenticated, and should not be
     * relied upon.
     *
     * @return  the host name of the peer, or null if nothing is
     *          available.
     */
    public String getPeerHost() {
        return peerHost;
    }

    /**
     * Returns the port number of the peer.
     * <P>
     * Note that the value is not authenticated, and should not be
     * relied upon.
     *
     * @return  the port number of the peer, or -1 if nothing is
     *          available.
     */
    public int getPeerPort() {
        return peerPort;
    }

    /**
     * Attempts to encode a buffer of plaintext application data into
     * SSL/TLS/DTLS network data.
     * <P>
     * An invocation of this method behaves in exactly the same manner
     * as the invocation:
     * <blockquote><pre>
     * {@link #wrap(ByteBuffer [], int, int, ByteBuffer)
     *     engine.wrap(new ByteBuffer [] { src }, 0, 1, dst);}
     * </pre></blockquote>
     *
     * @param   src
     *          a {@code ByteBuffer} containing outbound application data
     * @param   dst
     *          a {@code ByteBuffer} to hold outbound network data
     * @return  an {@code SSLEngineResult} describing the result
     *          of this operation.
     * @throws  SSLException
     *          A problem was encountered while processing the
     *          data that caused the {@code SSLEngine} to abort.
     *          See the class description for more information on
     *          engine closure.
     * @throws  ReadOnlyBufferException
     *          if the {@code dst} buffer is read-only.
     * @throws  IllegalArgumentException
     *          if either {@code src} or {@code dst}
     *          is null.
     * @throws  IllegalStateException if the client/server mode
     *          has not yet been set.
     * @see     #wrap(ByteBuffer [], int, int, ByteBuffer)
     */
    public SSLEngineResult wrap(ByteBuffer src,
            ByteBuffer dst) throws SSLException {
        return wrap(new ByteBuffer [] { src }, 0, 1, dst);
    }

    /**
     * Attempts to encode plaintext bytes from a sequence of data
     * buffers into SSL/TLS/DTLS network data.
     * <P>
     * An invocation of this method behaves in exactly the same manner
     * as the invocation:
     * <blockquote><pre>
     * {@link #wrap(ByteBuffer [], int, int, ByteBuffer)
     *     engine.wrap(srcs, 0, srcs.length, dst);}
     * </pre></blockquote>
     *
     * @param   srcs
     *          an array of {@code ByteBuffers} containing the
     *          outbound application data
     * @param   dst
     *          a {@code ByteBuffer} to hold outbound network data
     * @return  an {@code SSLEngineResult} describing the result
     *          of this operation.
     * @throws  SSLException
     *          A problem was encountered while processing the
     *          data that caused the {@code SSLEngine} to abort.
     *          See the class description for more information on
     *          engine closure.
     * @throws  ReadOnlyBufferException
     *          if the {@code dst} buffer is read-only.
     * @throws  IllegalArgumentException
     *          if either {@code srcs} or {@code dst}
     *          is null, or if any element in {@code srcs} is null.
     * @throws  IllegalStateException if the client/server mode
     *          has not yet been set.
     * @see     #wrap(ByteBuffer [], int, int, ByteBuffer)
     */
    public SSLEngineResult wrap(ByteBuffer [] srcs,
            ByteBuffer dst) throws SSLException {
        if (srcs == null) {
            throw new IllegalArgumentException("src == null");
        }
        return wrap(srcs, 0, srcs.length, dst);
    }


    /**
     * Attempts to encode plaintext bytes from a subsequence of data
     * buffers into SSL/TLS/DTLS network data.  This <i>"gathering"</i>
     * operation encodes, in a single invocation, a sequence of bytes
     * from one or more of a given sequence of buffers.  Gathering
     * wraps are often useful when implementing network protocols or
     * file formats that, for example, group data into segments
     * consisting of one or more fixed-length headers followed by a
     * variable-length body.  See
     * {@link java.nio.channels.GatheringByteChannel} for more
     * information on gathering, and {@link
     * java.nio.channels.GatheringByteChannel#write(ByteBuffer[],
     * int, int)} for more information on the subsequence
     * behavior.
     * <P>
     * Depending on the state of the SSLEngine, this method may produce
     * network data without consuming any application data (for example,
     * it may generate handshake data.)
     * <P>
     * The application is responsible for reliably transporting the
     * network data to the peer, and for ensuring that data created by
     * multiple calls to wrap() is transported in the same order in which
     * it was generated.  The application must properly synchronize
     * multiple calls to this method.
     * <P>
     * If this {@code SSLEngine} has not yet started its initial
     * handshake, this method will automatically start the handshake.
     * <P>
     * This method will attempt to produce SSL/TLS/DTLS records, and will
     * consume as much source data as possible, but will never consume
     * more than the sum of the bytes remaining in each buffer.  Each
     * {@code ByteBuffer}'s position is updated to reflect the
     * amount of data consumed or produced.  The limits remain the
     * same.
     * <P>
     * The underlying memory used by the {@code srcs} and
     * {@code dst ByteBuffer}s must not be the same.
     * <P>
     * See the class description for more information on engine closure.
     *
     * @param   srcs
     *          an array of {@code ByteBuffers} containing the
     *          outbound application data
     * @param   offset
     *          The offset within the buffer array of the first buffer from
     *          which bytes are to be retrieved; it must be non-negative
     *          and no larger than {@code srcs.length}
     * @param   length
     *          The maximum number of buffers to be accessed; it must be
     *          non-negative and no larger than
     *          {@code srcs.length}&nbsp;-&nbsp;{@code offset}
     * @param   dst
     *          a {@code ByteBuffer} to hold outbound network data
     * @return  an {@code SSLEngineResult} describing the result
     *          of this operation.
     * @throws  SSLException
     *          A problem was encountered while processing the
     *          data that caused the {@code SSLEngine} to abort.
     *          See the class description for more information on
     *          engine closure.
     * @throws  IndexOutOfBoundsException
     *          if the preconditions on the {@code offset} and
     *          {@code length} parameters do not hold.
     * @throws  ReadOnlyBufferException
     *          if the {@code dst} buffer is read-only.
     * @throws  IllegalArgumentException
     *          if either {@code srcs} or {@code dst}
     *          is null, or if any element in the {@code srcs}
     *          subsequence specified is null.
     * @throws  IllegalStateException if the client/server mode
     *          has not yet been set.
     * @see     java.nio.channels.GatheringByteChannel
     * @see     java.nio.channels.GatheringByteChannel#write(
     *              ByteBuffer[], int, int)
     */
    public abstract SSLEngineResult wrap(ByteBuffer [] srcs, int offset,
            int length, ByteBuffer dst) throws SSLException;

    /**
     * Attempts to decode SSL/TLS/DTLS network data into a plaintext
     * application data buffer.
     * <P>
     * An invocation of this method behaves in exactly the same manner
     * as the invocation:
     * <blockquote><pre>
     * {@link #unwrap(ByteBuffer, ByteBuffer [], int, int)
     *     engine.unwrap(src, new ByteBuffer [] { dst }, 0, 1);}
     * </pre></blockquote>
     *
     * @param   src
     *          a {@code ByteBuffer} containing inbound network data.
     * @param   dst
     *          a {@code ByteBuffer} to hold inbound application data.
     * @return  an {@code SSLEngineResult} describing the result
     *          of this operation.
     * @throws  SSLException
     *          A problem was encountered while processing the
     *          data that caused the {@code SSLEngine} to abort.
     *          See the class description for more information on
     *          engine closure.
     * @throws  ReadOnlyBufferException
     *          if the {@code dst} buffer is read-only.
     * @throws  IllegalArgumentException
     *          if either {@code src} or {@code dst}
     *          is null.
     * @throws  IllegalStateException if the client/server mode
     *          has not yet been set.
     * @see     #unwrap(ByteBuffer, ByteBuffer [], int, int)
     */
    public SSLEngineResult unwrap(ByteBuffer src,
            ByteBuffer dst) throws SSLException {
        return unwrap(src, new ByteBuffer [] { dst }, 0, 1);
    }

    /**
     * Attempts to decode SSL/TLS/DTLS network data into a sequence of plaintext
     * application data buffers.
     * <P>
     * An invocation of this method behaves in exactly the same manner
     * as the invocation:
     * <blockquote><pre>
     * {@link #unwrap(ByteBuffer, ByteBuffer [], int, int)
     *     engine.unwrap(src, dsts, 0, dsts.length);}
     * </pre></blockquote>
     *
     * @param   src
     *          a {@code ByteBuffer} containing inbound network data.
     * @param   dsts
     *          an array of {@code ByteBuffer}s to hold inbound
     *          application data.
     * @return  an {@code SSLEngineResult} describing the result
     *          of this operation.
     * @throws  SSLException
     *          A problem was encountered while processing the
     *          data that caused the {@code SSLEngine} to abort.
     *          See the class description for more information on
     *          engine closure.
     * @throws  ReadOnlyBufferException
     *          if any of the {@code dst} buffers are read-only.
     * @throws  IllegalArgumentException
     *          if either {@code src} or {@code dsts}
     *          is null, or if any element in {@code dsts} is null.
     * @throws  IllegalStateException if the client/server mode
     *          has not yet been set.
     * @see     #unwrap(ByteBuffer, ByteBuffer [], int, int)
     */
    public SSLEngineResult unwrap(ByteBuffer src,
            ByteBuffer [] dsts) throws SSLException {
        if (dsts == null) {
            throw new IllegalArgumentException("dsts == null");
        }
        return unwrap(src, dsts, 0, dsts.length);
    }

    /**
     * Attempts to decode SSL/TLS/DTLS network data into a subsequence of
     * plaintext application data buffers.  This <i>"scattering"</i>
     * operation decodes, in a single invocation, a sequence of bytes
     * into one or more of a given sequence of buffers.  Scattering
     * unwraps are often useful when implementing network protocols or
     * file formats that, for example, group data into segments
     * consisting of one or more fixed-length headers followed by a
     * variable-length body.  See
     * {@link java.nio.channels.ScatteringByteChannel} for more
     * information on scattering, and {@link
     * java.nio.channels.ScatteringByteChannel#read(ByteBuffer[],
     * int, int)} for more information on the subsequence
     * behavior.
     * <P>
     * Depending on the state of the SSLEngine, this method may consume
     * network data without producing any application data (for example,
     * it may consume handshake data.)
     * <P>
     * The application is responsible for reliably obtaining the network
     * data from the peer, and for invoking unwrap() on the data in the
     * order it was received.  The application must properly synchronize
     * multiple calls to this method.
     * <P>
     * If this {@code SSLEngine} has not yet started its initial
     * handshake, this method will automatically start the handshake.
     * <P>
     * This method will attempt to consume one complete SSL/TLS/DTLS network
     * packet, but will never consume more than the sum of the bytes
     * remaining in the buffers.  Each {@code ByteBuffer}'s
     * position is updated to reflect the amount of data consumed or
     * produced.  The limits remain the same.
     * <P>
     * The underlying memory used by the {@code src} and
     * {@code dsts ByteBuffer}s must not be the same.
     * <P>
     * The inbound network buffer may be modified as a result of this
     * call:  therefore if the network data packet is required for some
     * secondary purpose, the data should be duplicated before calling this
     * method.  Note:  the network data will not be useful to a second
     * SSLEngine, as each SSLEngine contains unique random state which
     * influences the SSL/TLS/DTLS messages.
     * <P>
     * See the class description for more information on engine closure.
     *
     * @param   src
     *          a {@code ByteBuffer} containing inbound network data.
     * @param   dsts
     *          an array of {@code ByteBuffer}s to hold inbound
     *          application data.
     * @param   offset
     *          The offset within the buffer array of the first buffer from
     *          which bytes are to be transferred; it must be non-negative
     *          and no larger than {@code dsts.length}.
     * @param   length
     *          The maximum number of buffers to be accessed; it must be
     *          non-negative and no larger than
     *          {@code dsts.length}&nbsp;-&nbsp;{@code offset}.
     * @return  an {@code SSLEngineResult} describing the result
     *          of this operation.
     * @throws  SSLException
     *          A problem was encountered while processing the
     *          data that caused the {@code SSLEngine} to abort.
     *          See the class description for more information on
     *          engine closure.
     * @throws  IndexOutOfBoundsException
     *          If the preconditions on the {@code offset} and
     *          {@code length} parameters do not hold.
     * @throws  ReadOnlyBufferException
     *          if any of the {@code dst} buffers are read-only.
     * @throws  IllegalArgumentException
     *          if either {@code src} or {@code dsts}
     *          is null, or if any element in the {@code dsts}
     *          subsequence specified is null.
     * @throws  IllegalStateException if the client/server mode
     *          has not yet been set.
     * @see     java.nio.channels.ScatteringByteChannel
     * @see     java.nio.channels.ScatteringByteChannel#read(
     *              ByteBuffer[], int, int)
     */
    public abstract SSLEngineResult unwrap(ByteBuffer src,
            ByteBuffer [] dsts, int offset, int length) throws SSLException;


    /**
     * Returns a delegated {@code Runnable} task for
     * this {@code SSLEngine}.
     * <P>
     * {@code SSLEngine} operations may require the results of
     * operations that block, or may take an extended period of time to
     * complete.  This method is used to obtain an outstanding {@link
     * java.lang.Runnable} operation (task).  Each task must be assigned
     * a thread (possibly the current) to perform the {@link
     * java.lang.Runnable#run() run} operation.  Once the
     * {@code run} method returns, the {@code Runnable} object
     * is no longer needed and may be discarded.
     * <P>
     * Delegated tasks run in the {@code AccessControlContext}
     * in place when this object was created.
     * <P>
     * A call to this method will return each outstanding task
     * exactly once.
     * <P>
     * Multiple delegated tasks can be run in parallel.
     *
     * @return  a delegated {@code Runnable} task, or null
     *          if none are available.
     */
    public abstract Runnable getDelegatedTask();


    /**
     * Signals that no more inbound network data will be sent
     * to this {@code SSLEngine}.
     * <P>
     * If the application initiated the closing process by calling
     * {@link #closeOutbound()}, under some circumstances it is not
     * required that the initiator wait for the peer's corresponding
     * close message.  (See section 7.2.1 of the TLS specification (<A
     * HREF="http://www.ietf.org/rfc/rfc2246.txt">RFC 2246</A>) for more
     * information on waiting for closure alerts.)  In such cases, this
     * method need not be called.
     * <P>
     * But if the application did not initiate the closure process, or
     * if the circumstances above do not apply, this method should be
     * called whenever the end of the SSL/TLS/DTLS data stream is reached.
     * This ensures closure of the inbound side, and checks that the
     * peer followed the SSL/TLS/DTLS close procedure properly, thus
     * detecting possible truncation attacks.
     * <P>
     * This method is idempotent:  if the inbound side has already
     * been closed, this method does not do anything.
     * <P>
     * {@link #wrap(ByteBuffer, ByteBuffer) wrap()} should be
     * called to flush any remaining handshake data.
     *
     * @throws  SSLException
     *          if this engine has not received the proper SSL/TLS/DTLS close
     *          notification message from the peer.
     *
     * @see     #isInboundDone()
     * @see     #isOutboundDone()
     */
    public abstract void closeInbound() throws SSLException;


    /**
     * Returns whether {@link #unwrap(ByteBuffer, ByteBuffer)} will
     * accept any more inbound data messages.
     *
     * @return  true if the {@code SSLEngine} will not
     *          consume anymore network data (and by implication,
     *          will not produce any more application data.)
     * @see     #closeInbound()
     */
    public abstract boolean isInboundDone();


    /**
     * Signals that no more outbound application data will be sent
     * on this {@code SSLEngine}.
     * <P>
     * This method is idempotent:  if the outbound side has already
     * been closed, this method does not do anything.
     * <P>
     * {@link #wrap(ByteBuffer, ByteBuffer)} should be
     * called to flush any remaining handshake data.
     *
     * @see     #isOutboundDone()
     */
    public abstract void closeOutbound();


    /**
     * Returns whether {@link #wrap(ByteBuffer, ByteBuffer)} will
     * produce any more outbound data messages.
     * <P>
     * Note that during the closure phase, a {@code SSLEngine} may
     * generate handshake closure data that must be sent to the peer.
     * {@code wrap()} must be called to generate this data.  When
     * this method returns true, no more outbound data will be created.
     *
     * @return  true if the {@code SSLEngine} will not produce
     *          any more network data
     *
     * @see     #closeOutbound()
     * @see     #closeInbound()
     */
    public abstract boolean isOutboundDone();


    /**
     * Returns the names of the cipher suites which could be enabled for use
     * on this engine.  Normally, only a subset of these will actually
     * be enabled by default, since this list may include cipher suites which
     * do not meet quality of service requirements for those defaults.  Such
     * cipher suites might be useful in specialized applications.
     * <P>
     * The returned array includes cipher suites from the list of standard
     * cipher suite names in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation, and may also
     * include other cipher suites that the provider supports.
     *
     * @return  an array of cipher suite names
     * @see     #getEnabledCipherSuites()
     * @see     #setEnabledCipherSuites(String [])
     */
    public abstract String [] getSupportedCipherSuites();


    /**
     * Returns the names of the SSL cipher suites which are currently
     * enabled for use on this engine.  When an SSLEngine is first
     * created, all enabled cipher suites support a minimum quality of
     * service.  Thus, in some environments this value might be empty.
     * <P>
     * Note that even if a suite is enabled, it may never be used. This
     * can occur if the peer does not support it, or its use is restricted,
     * or the requisite certificates (and private keys) for the suite are
     * not available, or an anonymous suite is enabled but authentication
     * is required.
     * <P>
     * The returned array includes cipher suites from the list of standard
     * cipher suite names in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation, and may also
     * include other cipher suites that the provider supports.
     *
     * @return  an array of cipher suite names
     * @see     #getSupportedCipherSuites()
     * @see     #setEnabledCipherSuites(String [])
     */
    public abstract String [] getEnabledCipherSuites();


    /**
     * Sets the cipher suites enabled for use on this engine.
     * <P>
     * Each cipher suite in the {@code suites} parameter must have
     * been listed by getSupportedCipherSuites(), or the method will
     * fail.  Following a successful call to this method, only suites
     * listed in the {@code suites} parameter are enabled for use.
     * <P>
     * Note that the standard list of cipher suite names may be found in the
     * <a href=
     * "{@docRoot}/../specs/security/standard-names.html#jsse-cipher-suite-names">
     * JSSE Cipher Suite Names</a> section of the Java Cryptography
     * Architecture Standard Algorithm Name Documentation.  Providers
     * may support cipher suite names not found in this list or might not
     * use the recommended name for a certain cipher suite.
     * <P>
     * See {@link #getEnabledCipherSuites()} for more information
     * on why a specific cipher suite may never be used on a engine.
     *
     * @param   suites Names of all the cipher suites to enable
     * @throws  IllegalArgumentException when one or more of the ciphers
     *          named by the parameter is not supported, or when the
     *          parameter is null.
     * @see     #getSupportedCipherSuites()
     * @see     #getEnabledCipherSuites()
     */
    public abstract void setEnabledCipherSuites(String suites []);


    /**
     * Returns the names of the protocols which could be enabled for use
     * with this {@code SSLEngine}.
     *
     * @return  an array of protocols supported
     */
    public abstract String [] getSupportedProtocols();


    /**
     * Returns the names of the protocol versions which are currently
     * enabled for use with this {@code SSLEngine}.
     * <P>
     * Note that even if a protocol is enabled, it may never be used.
     * This can occur if the peer does not support the protocol, or its
     * use is restricted, or there are no enabled cipher suites supported
     * by the protocol.
     *
     * @return  an array of protocols
     * @see     #setEnabledProtocols(String [])
     */
    public abstract String [] getEnabledProtocols();


    /**
     * Set the protocol versions enabled for use on this engine.
     * <P>
     * The protocols must have been listed by getSupportedProtocols()
     * as being supported.  Following a successful call to this method,
     * only protocols listed in the {@code protocols} parameter
     * are enabled for use.
     *
     * @param   protocols Names of all the protocols to enable.
     * @throws  IllegalArgumentException when one or more of
     *          the protocols named by the parameter is not supported or
     *          when the protocols parameter is null.
     * @see     #getEnabledProtocols()
     */
    public abstract void setEnabledProtocols(String protocols[]);


    /**
     * Returns the {@code SSLSession} in use in this
     * {@code SSLEngine}.
     * <P>
     * These can be long lived, and frequently correspond to an entire
     * login session for some user.  The session specifies a particular
     * cipher suite which is being actively used by all connections in
     * that session, as well as the identities of the session's client
     * and server.
     * <P>
     * Unlike {@link SSLSocket#getSession()}
     * this method does not block until handshaking is complete.
     * <P>
     * Until the initial handshake has completed, this method returns
     * a session object which reports an invalid cipher suite of
     * "SSL_NULL_WITH_NULL_NULL".
     *
     * @return  the {@code SSLSession} for this {@code SSLEngine}
     * @see     SSLSession
     */
    public abstract SSLSession getSession();


    /**
     * Returns the {@code SSLSession} being constructed during a SSL/TLS/DTLS
     * handshake.
     * <p>
     * TLS/DTLS protocols may negotiate parameters that are needed when using
     * an instance of this class, but before the {@code SSLSession} has
     * been completely initialized and made available via {@code getSession}.
     * For example, the list of valid signature algorithms may restrict
     * the type of certificates that can be used during TrustManager
     * decisions, or the maximum TLS/DTLS fragment packet sizes can be
     * resized to better support the network environment.
     * <p>
     * This method provides early access to the {@code SSLSession} being
     * constructed.  Depending on how far the handshake has progressed,
     * some data may not yet be available for use.  For example, if a
     * remote server will be sending a Certificate chain, but that chain
     * has yet not been processed, the {@code getPeerCertificates}
     * method of {@code SSLSession} will throw a
     * SSLPeerUnverifiedException.  Once that chain has been processed,
     * {@code getPeerCertificates} will return the proper value.
     *
     * @see SSLSocket
     * @see SSLSession
     * @see ExtendedSSLSession
     * @see X509ExtendedKeyManager
     * @see X509ExtendedTrustManager
     *
     * @return null if this instance is not currently handshaking, or
     *         if the current handshake has not progressed far enough to
     *         create a basic SSLSession.  Otherwise, this method returns the
     *         {@code SSLSession} currently being negotiated.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     *
     * @since 1.7
     */
    public SSLSession getHandshakeSession() {
        throw new UnsupportedOperationException();
    }


    /**
     * Initiates handshaking (initial or renegotiation) on this SSLEngine.
     * <P>
     * This method is not needed for the initial handshake, as the
     * {@code wrap()} and {@code unwrap()} methods will
     * implicitly call this method if handshaking has not already begun.
     * <P>
     * Note that the peer may also request a session renegotiation with
     * this {@code SSLEngine} by sending the appropriate
     * session renegotiate handshake message.
     * <P>
     * Unlike the {@link SSLSocket#startHandshake()
     * SSLSocket#startHandshake()} method, this method does not block
     * until handshaking is completed.
     * <P>
     * To force a complete SSL/TLS/DTLS session renegotiation, the current
     * session should be invalidated prior to calling this method.
     * <P>
     * Some protocols may not support multiple handshakes on an existing
     * engine and may throw an {@code SSLException}.
     *
     * @throws  SSLException
     *          if a problem was encountered while signaling the
     *          {@code SSLEngine} to begin a new handshake.
     *          See the class description for more information on
     *          engine closure.
     * @throws  IllegalStateException if the client/server mode
     *          has not yet been set.
     * @see     SSLSession#invalidate()
     */
    public abstract void beginHandshake() throws SSLException;


    /**
     * Returns the current handshake status for this {@code SSLEngine}.
     *
     * @return  the current {@code SSLEngineResult.HandshakeStatus}.
     */
    public abstract SSLEngineResult.HandshakeStatus getHandshakeStatus();


    /**
     * Configures the engine to use client (or server) mode when
     * handshaking.
     * <P>
     * This method must be called before any handshaking occurs.
     * Once handshaking has begun, the mode can not be reset for the
     * life of this engine.
     * <P>
     * Servers normally authenticate themselves, and clients
     * are not required to do so.
     *
     * @implNote
     * The JDK SunJSSE provider implementation default for this mode is false.
     *
     * @param   mode true if the engine should start its handshaking
     *          in "client" mode
     * @throws  IllegalArgumentException if a mode change is attempted
     *          after the initial handshake has begun.
     * @see     #getUseClientMode()
     */
    public abstract void setUseClientMode(boolean mode);


    /**
     * Returns true if the engine is set to use client mode when
     * handshaking.
     *
     * @implNote
     * The JDK SunJSSE provider implementation returns false unless
     * {@link setUseClientMode(boolean)} is used to change the mode to true.
     *
     * @return  true if the engine should do handshaking
     *          in "client" mode
     * @see     #setUseClientMode(boolean)
     */
    public abstract boolean getUseClientMode();


    /**
     * Configures the engine to <i>require</i> client authentication.  This
     * option is only useful for engines in the server mode.
     * <P>
     * An engine's client authentication setting is one of the following:
     * <ul>
     * <li> client authentication required
     * <li> client authentication requested
     * <li> no client authentication desired
     * </ul>
     * <P>
     * Unlike {@link #setWantClientAuth(boolean)}, if this option is set and
     * the client chooses not to provide authentication information
     * about itself, <i>the negotiations will stop and the engine will
     * begin its closure procedure</i>.
     * <P>
     * Calling this method overrides any previous setting made by
     * this method or {@link #setWantClientAuth(boolean)}.
     *
     * @param   need set to true if client authentication is required,
     *          or false if no client authentication is desired.
     * @see     #getNeedClientAuth()
     * @see     #setWantClientAuth(boolean)
     * @see     #getWantClientAuth()
     * @see     #setUseClientMode(boolean)
     */
    public abstract void setNeedClientAuth(boolean need);


    /**
     * Returns true if the engine will <i>require</i> client authentication.
     * This option is only useful to engines in the server mode.
     *
     * @return  true if client authentication is required,
     *          or false if no client authentication is desired.
     * @see     #setNeedClientAuth(boolean)
     * @see     #setWantClientAuth(boolean)
     * @see     #getWantClientAuth()
     * @see     #setUseClientMode(boolean)
     */
    public abstract boolean getNeedClientAuth();


    /**
     * Configures the engine to <i>request</i> client authentication.
     * This option is only useful for engines in the server mode.
     * <P>
     * An engine's client authentication setting is one of the following:
     * <ul>
     * <li> client authentication required
     * <li> client authentication requested
     * <li> no client authentication desired
     * </ul>
     * <P>
     * Unlike {@link #setNeedClientAuth(boolean)}, if this option is set and
     * the client chooses not to provide authentication information
     * about itself, <i>the negotiations will continue</i>.
     * <P>
     * Calling this method overrides any previous setting made by
     * this method or {@link #setNeedClientAuth(boolean)}.
     *
     * @param   want set to true if client authentication is requested,
     *          or false if no client authentication is desired.
     * @see     #getWantClientAuth()
     * @see     #setNeedClientAuth(boolean)
     * @see     #getNeedClientAuth()
     * @see     #setUseClientMode(boolean)
     */
    public abstract void setWantClientAuth(boolean want);


    /**
     * Returns true if the engine will <i>request</i> client authentication.
     * This option is only useful for engines in the server mode.
     *
     * @return  true if client authentication is requested,
     *          or false if no client authentication is desired.
     * @see     #setNeedClientAuth(boolean)
     * @see     #getNeedClientAuth()
     * @see     #setWantClientAuth(boolean)
     * @see     #setUseClientMode(boolean)
     */
    public abstract boolean getWantClientAuth();


    /**
     * Controls whether new SSL sessions may be established by this engine.
     * If session creations are not allowed, and there are no
     * existing sessions to resume, there will be no successful
     * handshaking.
     *
     * @param   flag true indicates that sessions may be created; this
     *          is the default.  false indicates that an existing session
     *          must be resumed
     * @see     #getEnableSessionCreation()
     */
    public abstract void setEnableSessionCreation(boolean flag);


    /**
     * Returns true if new SSL sessions may be established by this engine.
     *
     * @return  true indicates that sessions may be created; this
     *          is the default.  false indicates that an existing session
     *          must be resumed
     * @see     #setEnableSessionCreation(boolean)
     */
    public abstract boolean getEnableSessionCreation();

    /**
     * Returns the SSLParameters in effect for this SSLEngine.
     * The ciphersuites and protocols of the returned SSLParameters
     * are always non-null.
     *
     * @return the SSLParameters in effect for this SSLEngine.
     * @since 1.6
     */
    public SSLParameters getSSLParameters() {
        SSLParameters params = new SSLParameters();
        params.setCipherSuites(getEnabledCipherSuites());
        params.setProtocols(getEnabledProtocols());
        if (getNeedClientAuth()) {
            params.setNeedClientAuth(true);
        } else if (getWantClientAuth()) {
            params.setWantClientAuth(true);
        }
        return params;
    }

    /**
     * Applies SSLParameters to this engine.
     *
     * <p>This means:
     * <ul>
     * <li>If {@code params.getCipherSuites()} is non-null,
     *   {@code setEnabledCipherSuites()} is called with that value.</li>
     * <li>If {@code params.getProtocols()} is non-null,
     *   {@code setEnabledProtocols()} is called with that value.</li>
     * <li>If {@code params.getNeedClientAuth()} or
     *   {@code params.getWantClientAuth()} return {@code true},
     *   {@code setNeedClientAuth(true)} and
     *   {@code setWantClientAuth(true)} are called, respectively;
     *   otherwise {@code setWantClientAuth(false)} is called.</li>
     * <li>If {@code params.getServerNames()} is non-null, the engine will
     *   configure its server names with that value.</li>
     * <li>If {@code params.getSNIMatchers()} is non-null, the engine will
     *   configure its SNI matchers with that value.</li>
     * </ul>
     *
     * @param params the parameters
     * @throws IllegalArgumentException if the setEnabledCipherSuites() or
     *    the setEnabledProtocols() call fails
     * @since 1.6
     */
    public void setSSLParameters(SSLParameters params) {
        String[] s;
        s = params.getCipherSuites();
        if (s != null) {
            setEnabledCipherSuites(s);
        }
        s = params.getProtocols();
        if (s != null) {
            setEnabledProtocols(s);
        }
        if (params.getNeedClientAuth()) {
            setNeedClientAuth(true);
        } else if (params.getWantClientAuth()) {
            setWantClientAuth(true);
        } else {
            setWantClientAuth(false);
        }
    }

    /**
     * Returns the most recent application protocol value negotiated for this
     * connection.
     * <p>
     * If supported by the underlying SSL/TLS/DTLS implementation,
     * application name negotiation mechanisms such as <a
     * href="http://www.ietf.org/rfc/rfc7301.txt"> RFC 7301 </a>, the
     * Application-Layer Protocol Negotiation (ALPN), can negotiate
     * application-level values between peers.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @return null if it has not yet been determined if application
     *         protocols might be used for this connection, an empty
     *         {@code String} if application protocols values will not
     *         be used, or a non-empty application protocol {@code String}
     *         if a value was successfully negotiated.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public String getApplicationProtocol() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the application protocol value negotiated on a SSL/TLS
     * handshake currently in progress.
     * <p>
     * Like {@link #getHandshakeSession()},
     * a connection may be in the middle of a handshake. The
     * application protocol may or may not yet be available.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @return null if it has not yet been determined if application
     *         protocols might be used for this handshake, an empty
     *         {@code String} if application protocols values will not
     *         be used, or a non-empty application protocol {@code String}
     *         if a value was successfully negotiated.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public String getHandshakeApplicationProtocol() {
        throw new UnsupportedOperationException();
    }

    /**
     * Registers a callback function that selects an application protocol
     * value for a SSL/TLS/DTLS handshake.
     * The function overrides any values supplied using
     * {@link SSLParameters#setApplicationProtocols
     * SSLParameters.setApplicationProtocols} and it supports the following
     * type parameters:
     * <blockquote>
     * <dl>
     * <dt> {@code SSLEngine}
     * <dd> The function's first argument allows the current {@code SSLEngine}
     *      to be inspected, including the handshake session and configuration
     *      settings.
     * <dt> {@code List<String>}
     * <dd> The function's second argument lists the application protocol names
     *      advertised by the TLS peer.
     * <dt> {@code String}
     * <dd> The function's result is an application protocol name, or null to
     *      indicate that none of the advertised names are acceptable.
     *      If the return value is an empty {@code String} then application
     *      protocol indications will not be used.
     *      If the return value is null (no value chosen) or is a value that
     *      was not advertised by the peer, the underlying protocol will
     *      determine what action to take. (For example, ALPN will send a
     *      "no_application_protocol" alert and terminate the connection.)
     * </dl>
     * </blockquote>
     *
     * For example, the following call registers a callback function that
     * examines the TLS handshake parameters and selects an application protocol
     * name:
     * <pre>{@code
     *     serverEngine.setHandshakeApplicationProtocolSelector(
     *         (serverEngine, clientProtocols) -> {
     *             SSLSession session = serverEngine.getHandshakeSession();
     *             return chooseApplicationProtocol(
     *                 serverEngine,
     *                 clientProtocols,
     *                 session.getProtocol(),
     *                 session.getCipherSuite());
     *         });
     * }</pre>
     *
     * @apiNote
     * This method should be called by TLS server applications before the TLS
     * handshake begins. Also, this {@code SSLEngine} should be configured with
     * parameters that are compatible with the application protocol selected by
     * the callback function. For example, enabling a poor choice of cipher
     * suites could result in no suitable application protocol.
     * See {@link SSLParameters}.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @param selector the callback function, or null to disable the callback
     *         functionality.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLEngine, List<String>, String> selector) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the callback function that selects an application protocol
     * value during a SSL/TLS/DTLS handshake.
     * See {@link #setHandshakeApplicationProtocolSelector
     * setHandshakeApplicationProtocolSelector}
     * for the function's type parameters.
     *
     * @implSpec
     * The implementation in this class throws
     * {@code UnsupportedOperationException} and performs no other action.
     *
     * @return the callback function, or null if none has been set.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation.
     * @since 9
     */
    public BiFunction<SSLEngine, List<String>, String>
            getHandshakeApplicationProtocolSelector() {
        throw new UnsupportedOperationException();
    }
}
