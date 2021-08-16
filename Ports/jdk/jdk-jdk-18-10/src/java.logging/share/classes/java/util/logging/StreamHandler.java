/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;

/**
 * Stream based logging {@code Handler}.
 * <p>
 * This is primarily intended as a base class or support class to
 * be used in implementing other logging {@code Handlers}.
 * <p>
 * {@code LogRecords} are published to a given {@code java.io.OutputStream}.
 * <p>
 * <b>Configuration:</b>
 * By default each {@code StreamHandler} is initialized using the following
 * {@code LogManager} configuration properties where {@code <handler-name>}
 * refers to the fully-qualified class name of the handler.
 * If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * <ul>
 * <li>   &lt;handler-name&gt;.level
 *        specifies the default level for the {@code Handler}
 *        (defaults to {@code Level.INFO}). </li>
 * <li>   &lt;handler-name&gt;.filter
 *        specifies the name of a {@code Filter} class to use
 *         (defaults to no {@code Filter}). </li>
 * <li>   &lt;handler-name&gt;.formatter
 *        specifies the name of a {@code Formatter} class to use
 *        (defaults to {@code java.util.logging.SimpleFormatter}). </li>
 * <li>   &lt;handler-name&gt;.encoding
 *        the name of the character set encoding to use (defaults to
 *        the default platform encoding). </li>
 * </ul>
 * <p>
 * For example, the properties for {@code StreamHandler} would be:
 * <ul>
 * <li>   java.util.logging.StreamHandler.level=INFO </li>
 * <li>   java.util.logging.StreamHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 * <p>
 * For a custom handler, e.g. com.foo.MyHandler, the properties would be:
 * <ul>
 * <li>   com.foo.MyHandler.level=INFO </li>
 * <li>   com.foo.MyHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 *
 * @since 1.4
 */

public class StreamHandler extends Handler {
    private OutputStream output;
    private boolean doneHeader;
    private volatile Writer writer;

    /**
     * Create a {@code StreamHandler}, with no current output stream.
     */
    public StreamHandler() {
        // configure with specific defaults for StreamHandler
        super(Level.INFO, new SimpleFormatter(), null);
    }

    /**
     * Create a {@code StreamHandler} with a given {@code Formatter}
     * and output stream.
     *
     * @param out         the target output stream
     * @param formatter   Formatter to be used to format output
     */
    public StreamHandler(OutputStream out, Formatter formatter) {
        // configure with default level but use specified formatter
        super(Level.INFO, null, Objects.requireNonNull(formatter));

        setOutputStreamPrivileged(out);
    }

    /**
     * @see Handler#Handler(Level, Formatter, Formatter)
     */
    StreamHandler(Level defaultLevel,
                  Formatter defaultFormatter,
                  Formatter specifiedFormatter) {
        super(defaultLevel, defaultFormatter, specifiedFormatter);
    }

    /**
     * Change the output stream.
     * <P>
     * If there is a current output stream then the {@code Formatter}'s
     * tail string is written and the stream is flushed and closed.
     * Then the output stream is replaced with the new output stream.
     *
     * @param out   New output stream.  May not be null.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
        if (out == null) {
            throw new NullPointerException();
        }
        flushAndClose();
        output = out;
        doneHeader = false;
        String encoding = getEncoding();
        if (encoding == null) {
            writer = new OutputStreamWriter(output);
        } else {
            try {
                writer = new OutputStreamWriter(output, encoding);
            } catch (UnsupportedEncodingException ex) {
                // This shouldn't happen.  The setEncoding method
                // should have validated that the encoding is OK.
                throw new Error("Unexpected exception " + ex);
            }
        }
    }

    /**
     * Set (or change) the character encoding used by this {@code Handler}.
     * <p>
     * The encoding should be set before any {@code LogRecords} are written
     * to the {@code Handler}.
     *
     * @param encoding  The name of a supported character encoding.
     *        May be null, to indicate the default platform encoding.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     * @throws  UnsupportedEncodingException if the named encoding is
     *          not supported.
     */
    @Override
    public synchronized void setEncoding(String encoding)
                        throws SecurityException, java.io.UnsupportedEncodingException {
        super.setEncoding(encoding);
        if (output == null) {
            return;
        }
        // Replace the current writer with a writer for the new encoding.
        flush();
        if (encoding == null) {
            writer = new OutputStreamWriter(output);
        } else {
            writer = new OutputStreamWriter(output, encoding);
        }
    }

    /**
     * Format and publish a {@code LogRecord}.
     * <p>
     * The {@code StreamHandler} first checks if there is an {@code OutputStream}
     * and if the given {@code LogRecord} has at least the required log level.
     * If not it silently returns.  If so, it calls any associated
     * {@code Filter} to check if the record should be published.  If so,
     * it calls its {@code Formatter} to format the record and then writes
     * the result to the current output stream.
     * <p>
     * If this is the first {@code LogRecord} to be written to a given
     * {@code OutputStream}, the {@code Formatter}'s "head" string is
     * written to the stream before the {@code LogRecord} is written.
     *
     * @param  record  description of the log event. A null record is
     *                 silently ignored and is not published
     */
    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String msg;
        try {
            msg = getFormatter().format(record);
        } catch (Exception ex) {
            // We don't want to throw an exception here, but we
            // report the exception to any registered ErrorManager.
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }

        try {
            if (!doneHeader) {
                writer.write(getFormatter().getHead(this));
                doneHeader = true;
            }
            writer.write(msg);
        } catch (Exception ex) {
            // We don't want to throw an exception here, but we
            // report the exception to any registered ErrorManager.
            reportError(null, ex, ErrorManager.WRITE_FAILURE);
        }
    }


    /**
     * Check if this {@code Handler} would actually log a given {@code LogRecord}.
     * <p>
     * This method checks if the {@code LogRecord} has an appropriate level and
     * whether it satisfies any {@code Filter}.  It will also return false if
     * no output stream has been assigned yet or the LogRecord is null.
     *
     * @param record  a {@code LogRecord} (may be null).
     * @return true if the {@code LogRecord} would be logged.
     *
     */
    @Override
    public boolean isLoggable(LogRecord record) {
        if (writer == null || record == null) {
            return false;
        }
        return super.isLoggable(record);
    }

    /**
     * Flush any buffered messages.
     */
    @Override
    public synchronized void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (Exception ex) {
                // We don't want to throw an exception here, but we
                // report the exception to any registered ErrorManager.
                reportError(null, ex, ErrorManager.FLUSH_FAILURE);
            }
        }
    }

    private synchronized void flushAndClose() throws SecurityException {
        checkPermission();
        if (writer != null) {
            try {
                if (!doneHeader) {
                    writer.write(getFormatter().getHead(this));
                    doneHeader = true;
                }
                writer.write(getFormatter().getTail(this));
                writer.flush();
                writer.close();
            } catch (Exception ex) {
                // We don't want to throw an exception here, but we
                // report the exception to any registered ErrorManager.
                reportError(null, ex, ErrorManager.CLOSE_FAILURE);
            }
            writer = null;
            output = null;
        }
    }

    /**
     * Close the current output stream.
     * <p>
     * The {@code Formatter}'s "tail" string is written to the stream before it
     * is closed.  In addition, if the {@code Formatter}'s "head" string has not
     * yet been written to the stream, it will be written before the
     * "tail" string.
     *
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    @Override
    public synchronized void close() throws SecurityException {
        flushAndClose();
    }

    // Package-private support for setting OutputStream
    // with elevated privilege.
    @SuppressWarnings("removal")
    final void setOutputStreamPrivileged(final OutputStream out) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                setOutputStream(out);
                return null;
            }
        }, null, LogManager.controlPermission);
    }
}
