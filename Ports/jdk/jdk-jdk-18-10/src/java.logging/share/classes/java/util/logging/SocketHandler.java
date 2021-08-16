/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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


package java.util.logging;

import java.io.*;
import java.net.*;

/**
 * Simple network logging {@code Handler}.
 * <p>
 * {@code LogRecords} are published to a network stream connection.  By default
 * the {@code XMLFormatter} class is used for formatting.
 * <p>
 * <b>Configuration:</b>
 * By default each {@code SocketHandler} is initialized using the following
 * {@code LogManager} configuration properties where {@code <handler-name>}
 * refers to the fully-qualified class name of the handler.
 * If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * <ul>
 * <li>   &lt;handler-name&gt;.level
 *        specifies the default level for the {@code Handler}
 *        (defaults to {@code Level.ALL}). </li>
 * <li>   &lt;handler-name&gt;.filter
 *        specifies the name of a {@code Filter} class to use
 *        (defaults to no {@code Filter}). </li>
 * <li>   &lt;handler-name&gt;.formatter
 *        specifies the name of a {@code Formatter} class to use
 *        (defaults to {@code java.util.logging.XMLFormatter}). </li>
 * <li>   &lt;handler-name&gt;.encoding
 *        the name of the character set encoding to use (defaults to
 *        the default platform encoding). </li>
 * <li>   &lt;handler-name&gt;.host
 *        specifies the target host name to connect to (no default). </li>
 * <li>   &lt;handler-name&gt;.port
 *        specifies the target TCP port to use (no default). </li>
 * </ul>
 * <p>
 * For example, the properties for {@code SocketHandler} would be:
 * <ul>
 * <li>   java.util.logging.SocketHandler.level=INFO </li>
 * <li>   java.util.logging.SocketHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 * <p>
 * For a custom handler, e.g. com.foo.MyHandler, the properties would be:
 * <ul>
 * <li>   com.foo.MyHandler.level=INFO </li>
 * <li>   com.foo.MyHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 * <p>
 * The output IO stream is buffered, but is flushed after each
 * {@code LogRecord} is written.
 *
 * @since 1.4
 */

public class SocketHandler extends StreamHandler {
    private Socket sock;
    private String host;
    private int port;

    /**
     * Create a {@code SocketHandler}, using only {@code LogManager} properties
     * (or their defaults).
     * @throws IllegalArgumentException if the host or port are invalid or
     *          are not specified as LogManager properties.
     * @throws IOException if we are unable to connect to the target
     *         host and port.
     */
    public SocketHandler() throws IOException {
        // configure with specific defaults for SocketHandler
        super(Level.ALL, new XMLFormatter(), null);

        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        port = manager.getIntProperty(cname + ".port", 0);
        host = manager.getStringProperty(cname + ".host", null);

        try {
            connect();
        } catch (IOException ix) {
            System.err.println("SocketHandler: connect failed to " + host + ":" + port);
            throw ix;
        }
    }

    /**
     * Construct a {@code SocketHandler} using a specified host and port.
     *
     * The {@code SocketHandler} is configured based on {@code LogManager}
     * properties (or their default values) except that the given target host
     * and port arguments are used. If the host argument is empty, but not
     * null String then the localhost is used.
     *
     * @param host target host.
     * @param port target port.
     *
     * @throws IllegalArgumentException if the host or port are invalid.
     * @throws IOException if we are unable to connect to the target
     *         host and port.
     */
    public SocketHandler(String host, int port) throws IOException {
        // configure with specific defaults for SocketHandler
        super(Level.ALL, new XMLFormatter(), null);

        this.port = port;
        this.host = host;

        connect();
    }

    private void connect() throws IOException {
        // Check the arguments are valid.
        if (port == 0) {
            throw new IllegalArgumentException("Bad port: " + port);
        }
        if (host == null) {
            throw new IllegalArgumentException("Null host name: " + host);
        }

        // Try to open a new socket.
        sock = new Socket(host, port);
        OutputStream out = sock.getOutputStream();
        BufferedOutputStream bout = new BufferedOutputStream(out);
        setOutputStreamPrivileged(bout);
    }

    /**
     * Close this output stream.
     *
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    @Override
    public synchronized void close() throws SecurityException {
        super.close();
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException ix) {
                // drop through.
            }
        }
        sock = null;
    }

    /**
     * Format and publish a {@code LogRecord}.
     *
     * @param  record  description of the log event. A null record is
     *                 silently ignored and is not published
     */
    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
    }
}
