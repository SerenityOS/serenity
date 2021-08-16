/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.net;

import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.Proxy;
import java.util.Arrays;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This is the base class for network clients.
 *
 * @author      Jonathan Payne
 */
@SuppressWarnings("removal")
public class NetworkClient {
    /* Default value of read timeout, if not specified (infinity) */
    public static final int DEFAULT_READ_TIMEOUT = -1;

    /* Default value of connect timeout, if not specified (infinity) */
    public static final int DEFAULT_CONNECT_TIMEOUT = -1;

    protected Proxy     proxy = Proxy.NO_PROXY;
    /** Socket for communicating with server. */
    protected Socket    serverSocket = null;

    /** Stream for printing to the server. */
    public PrintStream  serverOutput;

    /** Buffered stream for reading replies from server. */
    public InputStream  serverInput;

    protected static int defaultSoTimeout;
    protected static int defaultConnectTimeout;

    protected int readTimeout = DEFAULT_READ_TIMEOUT;
    protected int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    /* Name of encoding to use for output */
    protected static String encoding;

    static {
        final int vals[] = {0, 0};
        final String encs[] = { null };

        AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    public Void run() {
                        vals[0] = Integer.getInteger("sun.net.client.defaultReadTimeout", 0).intValue();
                        vals[1] = Integer.getInteger("sun.net.client.defaultConnectTimeout", 0).intValue();
                        encs[0] = System.getProperty("file.encoding", "ISO8859_1");
                        return null;
            }
        });
        if (vals[0] != 0) {
            defaultSoTimeout = vals[0];
        }
        if (vals[1] != 0) {
            defaultConnectTimeout = vals[1];
        }

        encoding = encs[0];
        try {
            if (!isASCIISuperset (encoding)) {
                encoding = "ISO8859_1";
            }
        } catch (Exception e) {
            encoding = "ISO8859_1";
        }
    }


    /**
     * Test the named character encoding to verify that it converts ASCII
     * characters correctly. We have to use an ASCII based encoding, or else
     * the NetworkClients will not work correctly in EBCDIC based systems.
     * However, we cannot just use ASCII or ISO8859_1 universally, because in
     * Asian locales, non-ASCII characters may be embedded in otherwise
     * ASCII based protocols (e.g. HTTP). The specifications (RFC2616, 2398)
     * are a little ambiguous in this matter. For instance, RFC2398 [part 2.1]
     * says that the HTTP request URI should be escaped using a defined
     * mechanism, but there is no way to specify in the escaped string what
     * the original character set is. It is not correct to assume that
     * UTF-8 is always used (as in URLs in HTML 4.0).  For this reason,
     * until the specifications are updated to deal with this issue more
     * comprehensively, and more importantly, HTTP servers are known to
     * support these mechanisms, we will maintain the current behavior
     * where it is possible to send non-ASCII characters in their original
     * unescaped form.
     */
    private static boolean isASCIISuperset (String encoding) throws Exception {
        String chkS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"+
                        "abcdefghijklmnopqrstuvwxyz-_.!~*'();/?:@&=+$,";

        // Expected byte sequence for string above
        byte[] chkB = { 48,49,50,51,52,53,54,55,56,57,65,66,67,68,69,70,71,72,
                73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,97,98,99,
                100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,
                115,116,117,118,119,120,121,122,45,95,46,33,126,42,39,40,41,59,
                47,63,58,64,38,61,43,36,44};

        byte[] b = chkS.getBytes (encoding);
        return Arrays.equals (b, chkB);
    }

    /** Open a connection to the server. */
    public void openServer(String server, int port)
        throws IOException, UnknownHostException {
        if (serverSocket != null)
            closeServer();
        serverSocket = doConnect (server, port);
        try {
            serverOutput = new PrintStream(new BufferedOutputStream(
                                        serverSocket.getOutputStream()),
                                        true, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding +"encoding not found", e);
        }
        serverInput = new BufferedInputStream(serverSocket.getInputStream());
    }

    /**
     * Return a socket connected to the server, with any
     * appropriate options pre-established
     */
    protected Socket doConnect (String server, int port)
    throws IOException, UnknownHostException {
        Socket s;
        if (proxy != null) {
            if (proxy.type() == Proxy.Type.SOCKS) {
                s = AccessController.doPrivileged(
                    new PrivilegedAction<>() {
                        public Socket run() {
                                       return new Socket(proxy);
                                   }});
            } else if (proxy.type() == Proxy.Type.DIRECT) {
                s = createSocket();
            } else {
                // Still connecting through a proxy
                // server & port will be the proxy address and port
                s = new Socket(Proxy.NO_PROXY);
            }
        } else {
            s = createSocket();
        }

        // Instance specific timeouts do have priority, that means
        // connectTimeout & readTimeout (-1 means not set)
        // Then global default timeouts
        // Then no timeout.
        if (connectTimeout >= 0) {
            s.connect(new InetSocketAddress(server, port), connectTimeout);
        } else {
            if (defaultConnectTimeout > 0) {
                s.connect(new InetSocketAddress(server, port), defaultConnectTimeout);
            } else {
                s.connect(new InetSocketAddress(server, port));
            }
        }
        if (readTimeout >= 0)
            s.setSoTimeout(readTimeout);
        else if (defaultSoTimeout > 0) {
            s.setSoTimeout(defaultSoTimeout);
        }
        return s;
    }

    /**
     * The following method, createSocket, is provided to allow the
     * https client to override it so that it may use its socket factory
     * to create the socket.
     */
    protected Socket createSocket() throws IOException {
        return new java.net.Socket(Proxy.NO_PROXY);  // direct connection
    }

    protected InetAddress getLocalAddress() throws IOException {
        if (serverSocket == null)
            throw new IOException("not connected");
        return  AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            public InetAddress run() {
                                return serverSocket.getLocalAddress();

                            }
                        });
    }

    /** Close an open connection to the server. */
    public void closeServer() throws IOException {
        if (! serverIsOpen()) {
            return;
        }
        serverSocket.close();
        serverSocket = null;
        serverInput = null;
        serverOutput = null;
    }

    /** Return server connection status */
    public boolean serverIsOpen() {
        return serverSocket != null;
    }

    /** Create connection with host <i>host</i> on port <i>port</i> */
    public NetworkClient(String host, int port) throws IOException {
        openServer(host, port);
    }

    public NetworkClient() {}

    public void setConnectTimeout(int timeout) {
        connectTimeout = timeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the read timeout.
     *
     * Note: Public URLConnection (and protocol specific implementations)
     * protect against negative timeout values being set. This implementation,
     * and protocol specific implementations, use -1 to represent the default
     * read timeout.
     *
     * This method may be invoked with the default timeout value when the
     * protocol handler is trying to reset the timeout after doing a
     * potentially blocking internal operation, e.g. cleaning up unread
     * response data, buffering error stream response data, etc
     */
    public void setReadTimeout(int timeout) {
        if (timeout == DEFAULT_READ_TIMEOUT)
            timeout = defaultSoTimeout;

        if (serverSocket != null && timeout >= 0) {
            try {
                serverSocket.setSoTimeout(timeout);
            } catch(IOException e) {
                // We tried...
            }
        }
        readTimeout = timeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }
}
