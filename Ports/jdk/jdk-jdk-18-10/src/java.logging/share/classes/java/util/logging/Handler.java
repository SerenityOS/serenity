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

import java.util.Objects;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A {@code Handler} object takes log messages from a {@code Logger} and
 * exports them.  It might for example, write them to a console
 * or write them to a file, or send them to a network logging service,
 * or forward them to an OS log, or whatever.
 * <p>
 * A {@code Handler} can be disabled by doing a {@code setLevel(Level.OFF)}
 * and can  be re-enabled by doing a {@code setLevel} with an appropriate level.
 * <p>
 * {@code Handler} classes typically use {@code LogManager} properties to set
 * default values for the {@code Handler}'s {@code Filter}, {@code Formatter},
 * and {@code Level}.  See the specific documentation for each concrete
 * {@code Handler} class.
 *
 *
 * @since 1.4
 */

public abstract class Handler {
    private static final int offValue = Level.OFF.intValue();
    private final LogManager manager = LogManager.getLogManager();

    // We're using volatile here to avoid synchronizing getters, which
    // would prevent other threads from calling isLoggable()
    // while publish() is executing.
    // On the other hand, setters will be synchronized to exclude concurrent
    // execution with more complex methods, such as StreamHandler.publish().
    // We wouldn't want 'level' to be changed by another thread in the middle
    // of the execution of a 'publish' call.
    private volatile Filter filter;
    private volatile Formatter formatter;
    private volatile Level logLevel = Level.ALL;
    private volatile ErrorManager errorManager = new ErrorManager();
    private volatile String encoding;

    /**
     * Default constructor.  The resulting {@code Handler} has a log
     * level of {@code Level.ALL}, no {@code Formatter}, and no
     * {@code Filter}.  A default {@code ErrorManager} instance is installed
     * as the {@code ErrorManager}.
     */
    protected Handler() {
    }

    /**
     * Package-private constructor for chaining from subclass constructors
     * that wish to configure the handler with specific default and/or
     * specified values.
     *
     * @param defaultLevel       a default {@link Level} to configure if one is
     *                           not found in LogManager configuration properties
     * @param defaultFormatter   a default {@link Formatter} to configure if one is
     *                           not specified by {@code specifiedFormatter} parameter
     *                           nor found in LogManager configuration properties
     * @param specifiedFormatter if not null, this is the formatter to configure
     */
    @SuppressWarnings("removal")
    Handler(Level defaultLevel, Formatter defaultFormatter,
            Formatter specifiedFormatter) {

        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();

        final Level level = manager.getLevelProperty(cname + ".level", defaultLevel);
        final Filter filter = manager.getFilterProperty(cname + ".filter", null);
        final Formatter formatter = specifiedFormatter == null
                                    ? manager.getFormatterProperty(cname + ".formatter", defaultFormatter)
                                    : specifiedFormatter;
        final String encoding = manager.getStringProperty(cname + ".encoding", null);

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                setLevel(level);
                setFilter(filter);
                setFormatter(formatter);
                try {
                    setEncoding(encoding);
                } catch (Exception ex) {
                    try {
                        setEncoding(null);
                    } catch (Exception ex2) {
                        // doing a setEncoding with null should always work.
                        // assert false;
                    }
                }
                return null;
            }
        }, null, LogManager.controlPermission);
    }

    /**
     * Publish a {@code LogRecord}.
     * <p>
     * The logging request was made initially to a {@code Logger} object,
     * which initialized the {@code LogRecord} and forwarded it here.
     * <p>
     * The {@code Handler}  is responsible for formatting the message, when and
     * if necessary.  The formatting should include localization.
     *
     * @param  record  description of the log event. A null record is
     *                 silently ignored and is not published
     */
    public abstract void publish(LogRecord record);

    /**
     * Flush any buffered output.
     */
    public abstract void flush();

    /**
     * Close the {@code Handler} and free all associated resources.
     * <p>
     * The close method will perform a {@code flush} and then close the
     * {@code Handler}.   After close has been called this {@code Handler}
     * should no longer be used.  Method calls may either be silently
     * ignored or may throw runtime exceptions.
     *
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    public abstract void close() throws SecurityException;

    /**
     * Set a {@code Formatter}.  This {@code Formatter} will be used
     * to format {@code LogRecords} for this {@code Handler}.
     * <p>
     * Some {@code Handlers} may not use {@code Formatters}, in
     * which case the {@code Formatter} will be remembered, but not used.
     *
     * @param newFormatter the {@code Formatter} to use (may not be null)
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    public synchronized void setFormatter(Formatter newFormatter) throws SecurityException {
        checkPermission();
        formatter = Objects.requireNonNull(newFormatter);
    }

    /**
     * Return the {@code Formatter} for this {@code Handler}.
     * @return the {@code Formatter} (may be null).
     */
    public Formatter getFormatter() {
        return formatter;
    }

    /**
     * Set the character encoding used by this {@code Handler}.
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
    public synchronized void setEncoding(String encoding)
                        throws SecurityException, java.io.UnsupportedEncodingException {
        checkPermission();
        if (encoding != null) {
            try {
                if(!java.nio.charset.Charset.isSupported(encoding)) {
                    throw new UnsupportedEncodingException(encoding);
                }
            } catch (java.nio.charset.IllegalCharsetNameException e) {
                throw new UnsupportedEncodingException(encoding);
            }
        }
        this.encoding = encoding;
    }

    /**
     * Return the character encoding for this {@code Handler}.
     *
     * @return  The encoding name.  May be null, which indicates the
     *          default encoding should be used.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Set a {@code Filter} to control output on this {@code Handler}.
     * <P>
     * For each call of {@code publish} the {@code Handler} will call
     * this {@code Filter} (if it is non-null) to check if the
     * {@code LogRecord} should be published or discarded.
     *
     * @param   newFilter  a {@code Filter} object (may be null)
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    public synchronized void setFilter(Filter newFilter) throws SecurityException {
        checkPermission();
        filter = newFilter;
    }

    /**
     * Get the current {@code Filter} for this {@code Handler}.
     *
     * @return  a {@code Filter} object (may be null)
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Define an ErrorManager for this Handler.
     * <p>
     * The ErrorManager's "error" method will be invoked if any
     * errors occur while using this Handler.
     *
     * @param em  the new ErrorManager
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    public synchronized void setErrorManager(ErrorManager em) {
        checkPermission();
        if (em == null) {
           throw new NullPointerException();
        }
        errorManager = em;
    }

    /**
     * Retrieves the ErrorManager for this Handler.
     *
     * @return the ErrorManager for this Handler
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    public ErrorManager getErrorManager() {
        checkPermission();
        return errorManager;
    }

   /**
     * Protected convenience method to report an error to this Handler's
     * ErrorManager.  Note that this method retrieves and uses the ErrorManager
     * without doing a security check.  It can therefore be used in
     * environments where the caller may be non-privileged.
     *
     * @param msg    a descriptive string (may be null)
     * @param ex     an exception (may be null)
     * @param code   an error code defined in ErrorManager
     */
    protected void reportError(String msg, Exception ex, int code) {
        try {
            errorManager.error(msg, ex, code);
        } catch (Exception ex2) {
            System.err.println("Handler.reportError caught:");
            ex2.printStackTrace();
        }
    }

    /**
     * Set the log level specifying which message levels will be
     * logged by this {@code Handler}.  Message levels lower than this
     * value will be discarded.
     * <p>
     * The intention is to allow developers to turn on voluminous
     * logging, but to limit the messages that are sent to certain
     * {@code Handlers}.
     *
     * @param newLevel   the new value for the log level
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    public synchronized void setLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException();
        }
        checkPermission();
        logLevel = newLevel;
    }

    /**
     * Get the log level specifying which messages will be
     * logged by this {@code Handler}.  Message levels lower
     * than this level will be discarded.
     * @return  the level of messages being logged.
     */
    public Level getLevel() {
        return logLevel;
    }

    /**
     * Check if this {@code Handler} would actually log a given {@code LogRecord}.
     * <p>
     * This method checks if the {@code LogRecord} has an appropriate
     * {@code Level} and  whether it satisfies any {@code Filter}.  It also
     * may make other {@code Handler} specific checks that might prevent a
     * handler from logging the {@code LogRecord}. It will return false if
     * the {@code LogRecord} is null.
     *
     * @param record  a {@code LogRecord} (may be null).
     * @return true if the {@code LogRecord} would be logged.
     *
     */
    public boolean isLoggable(LogRecord record) {
        final int levelValue = getLevel().intValue();
        if (record == null) return false;
        if (record.getLevel().intValue() < levelValue || levelValue == offValue) {
            return false;
        }
        final Filter filter = getFilter();
        if (filter == null) {
            return true;
        }
        return filter.isLoggable(record);
    }

    // Package-private support method for security checks.
    // We check that the caller has appropriate security privileges
    // to update Handler state and if not throw a SecurityException.
    void checkPermission() throws SecurityException {
        manager.checkPermission();
    }
}
