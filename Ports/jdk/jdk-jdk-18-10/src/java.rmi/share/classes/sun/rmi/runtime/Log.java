/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.lang.StackWalker.StackFrame;
import java.rmi.server.LogStream;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Utility which provides an abstract "logger" like RMI internal API
 * which can be directed to use one of two types of logging
 * infrastructure: the java.util.logging API or the
 * java.rmi.server.LogStream API.  The default behavior is to use the
 * java.util.logging API.  The LogStream API may be used instead by
 * setting the system property sun.rmi.log.useOld to true.
 *
 * For backwards compatibility, supports the RMI system logging
 * properties which pre-1.4 comprised the only way to configure RMI
 * logging.  If the java.util.logging API is used and RMI system log
 * properties are set, the system properties override initial RMI
 * logger values as appropriate. If the java.util.logging API is
 * turned off, pre-1.4 logging behavior is used.
 *
 * @author Laird Dornin
 * @since 1.4
 */
@SuppressWarnings("deprecation")
public abstract class Log {

    /** Logger re-definition of old RMI log values */
    public static final Level BRIEF = Level.FINE;
    public static final Level VERBOSE = Level.FINER;

    private static final StackWalker WALKER = StackWalker.getInstance(Set.of(), 4);

    /* selects log implementation */
    private static final LogFactory logFactory;
    static {
        @SuppressWarnings("removal")
        boolean useOld = java.security.AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> Boolean.getBoolean("sun.rmi.log.useOld"));

        /* set factory to select the logging facility to use */
        logFactory = (useOld ? (LogFactory) new LogStreamLogFactory() :
                      (LogFactory) new LoggerLogFactory());
    }

    /** "logger like" API to be used by RMI implementation */
    public abstract boolean isLoggable(Level level);
    public abstract void log(Level level, String message);
    public abstract void log(Level level, String message, Throwable thrown);

    /** get and set the RMI server call output stream */
    public abstract void setOutputStream(OutputStream stream);
    public abstract PrintStream getPrintStream();

    /** factory interface enables Logger and LogStream implementations */
    private static interface LogFactory {
        Log createLog(String loggerName, String oldLogName, Level level);
    }

    /* access log objects */

    /**
     * Access log for a tri-state system property.
     *
     * Need to first convert override value to a log level, taking
     * care to interpret a range of values between BRIEF, VERBOSE and
     * SILENT.
     *
     * An override {@literal <} 0 is interpreted to mean that the logging
     * configuration should not be overridden. The level passed to the
     * factories createLog method will be null in this case.
     *
     * Note that if oldLogName is null and old logging is on, the
     * returned LogStreamLog will ignore the override parameter - the
     * log will never log messages.  This permits new logs that only
     * write to Loggers to do nothing when old logging is active.
     *
     * Do not call getLog multiple times on the same logger name.
     * Since this is an internal API, no checks are made to ensure
     * that multiple logs do not exist for the same logger.
     */
    public static Log getLog(String loggerName, String oldLogName,
                             int override)
    {
        Level level;

        if (override < 0) {
            level = null;
        } else if (override == LogStream.SILENT) {
            level = Level.OFF;
        } else if ((override > LogStream.SILENT) &&
                   (override <= LogStream.BRIEF)) {
            level = BRIEF;
        } else if ((override > LogStream.BRIEF) &&
                   (override <= LogStream.VERBOSE))
        {
            level = VERBOSE;
        } else {
            level = Level.FINEST;
        }
        return logFactory.createLog(loggerName, oldLogName, level);
    }

    /**
     * Access logs associated with boolean properties
     *
     * Do not call getLog multiple times on the same logger name.
     * Since this is an internal API, no checks are made to ensure
     * that multiple logs do not exist for the same logger.
     */
    public static Log getLog(String loggerName, String oldLogName,
                             boolean override)
    {
        Level level = (override ? VERBOSE : null);
        return logFactory.createLog(loggerName, oldLogName, level);
    }

    /**
     * Factory to create Log objects which deliver log messages to the
     * java.util.logging API.
     */
    private static class LoggerLogFactory implements LogFactory {
        LoggerLogFactory() {}

        /*
         * Accessor to obtain an arbitrary RMI logger with name
         * loggerName.  If the level of the logger is greater than the
         * level for the system property with name, the logger level
         * will be set to the value of system property.
         */
        public Log createLog(final String loggerName, String oldLogName,
                             final Level level)
        {
            Logger logger = Logger.getLogger(loggerName);
            return new LoggerLog(logger, level);
        }
    }

    /**
     * Class specialized to log messages to the java.util.logging API
     */
    private static class LoggerLog extends Log {

        /* alternate console handler for RMI loggers */
        @SuppressWarnings("removal")
        private static final Handler alternateConsole =
                java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Handler>() {
                    public Handler run() {
                            InternalStreamHandler alternate =
                                new InternalStreamHandler(System.err);
                            alternate.setLevel(Level.ALL);
                            return alternate;
                        }
                });

        /** handler to which messages are copied */
        private InternalStreamHandler copyHandler = null;

        /* logger to which log messages are written */
        private final Logger logger;

        /* used as return value of RemoteServer.getLog */
        private LoggerPrintStream loggerSandwich;

        /** creates a Log which will delegate to the given logger */
        @SuppressWarnings("removal")
        private LoggerLog(final Logger logger, final Level level) {
            this.logger = logger;

            if (level != null){
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Void>() {
                        public Void run() {
                            if (!logger.isLoggable(level)) {
                                logger.setLevel(level);
                            }
                            logger.addHandler(alternateConsole);
                            return null;
                        }
                    }
                );
            }
        }

        public boolean isLoggable(Level level) {
            return logger.isLoggable(level);
        }

        public void log(Level level, String message) {
            if (isLoggable(level)) {
                StackFrame sourceFrame = getSource();
                logger.logp(level, sourceFrame.getClassName(), sourceFrame.getMethodName(),
                           Thread.currentThread().getName() + ": " + message);
            }
        }

        public void log(Level level, String message, Throwable thrown) {
            if (isLoggable(level)) {
                StackFrame sourceFrame = getSource();
                logger.logp(level, sourceFrame.getClassName(), sourceFrame.getMethodName(),
                    Thread.currentThread().getName() + ": " +
                           message, thrown);
            }
        }

        public String toString() {
            return logger.toString() + ", level: " + logger.getLevel() +
                    ", name: " + logger.getName();
        }

        /**
         * Set the output stream associated with the RMI server call
         * logger.
         *
         * Calling code needs LoggingPermission "control".
         */
        public synchronized void setOutputStream(OutputStream out) {
            if (out != null) {
                if (!logger.isLoggable(VERBOSE)) {
                    logger.setLevel(VERBOSE);
                }
                copyHandler = new InternalStreamHandler(out);
                copyHandler.setLevel(Log.VERBOSE);
                logger.addHandler(copyHandler);
            } else {
                /* ensure that messages are not logged */
                if (copyHandler != null) {
                    logger.removeHandler(copyHandler);
                }
                copyHandler = null;
            }
        }

        public synchronized PrintStream getPrintStream() {
            if (loggerSandwich == null) {
                loggerSandwich = new LoggerPrintStream(logger);
            }
            return loggerSandwich;
        }
    }

    /**
     * Subclass of StreamHandler for redirecting log output.  flush
     * must be called in the publish and close methods.
     */
    private static class InternalStreamHandler extends StreamHandler {
        InternalStreamHandler(OutputStream out) {
            super(out, new SimpleFormatter());
        }

        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        public void close() {
            flush();
        }
    }

    /**
     * PrintStream which forwards log messages to the logger.  Class
     * is needed to maintain backwards compatibility with
     * RemoteServer.{set|get}Log().
     */
    private static class LoggerPrintStream extends PrintStream {

        /** logger where output of this log is sent */
        private final Logger logger;

        /** record the last character written to this stream */
        private int last = -1;

        /** stream used for buffering lines */
        private final ByteArrayOutputStream bufOut;

        private LoggerPrintStream(Logger logger)
        {
            super(new ByteArrayOutputStream());
            bufOut = (ByteArrayOutputStream) super.out;
            this.logger = logger;
        }

        public void write(int b) {
            if ((last == '\r') && (b == '\n')) {
                last = -1;
                return;
            } else if ((b == '\n') || (b == '\r')) {
                try {
                    /* write the converted bytes of the log message */
                    String message =
                        Thread.currentThread().getName() + ": " +
                        bufOut.toString();
                    logger.logp(Level.INFO, "LogStream", "print", message);
                } finally {
                    bufOut.reset();
                }
            } else {
                super.write(b);
            }
            last = b;
        }

        public void write(byte b[], int off, int len) {
            if (len < 0) {
                throw new ArrayIndexOutOfBoundsException(len);
            }
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }

        public String toString() {
            return "RMI";
        }
    }

    /**
     * Factory to create Log objects which deliver log messages to the
     * java.rmi.server.LogStream API
     */
    private static class LogStreamLogFactory implements LogFactory {
        LogStreamLogFactory() {}

        /* create a new LogStreamLog for the specified log */
        public Log createLog(String loggerName, String oldLogName,
                             Level level)
        {
            LogStream stream = null;
            if (oldLogName != null) {
                stream = LogStream.log(oldLogName);
            }
            return new LogStreamLog(stream, level);
        }
    }

    /**
     * Class specialized to log messages to the
     * java.rmi.server.LogStream API
     */
    private static class LogStreamLog extends Log {
        /** Log stream to which log messages are written */
        private final LogStream stream;

        /** the level of the log as set by associated property */
        private int levelValue = Level.OFF.intValue();

        private LogStreamLog(LogStream stream, Level level) {
            if ((stream != null) && (level != null)) {
                /* if the stream or level is null, don't log any
                 * messages
                 */
                levelValue = level.intValue();
            }
            this.stream = stream;
        }

        public synchronized boolean isLoggable(Level level) {
            return (level.intValue() >= levelValue);
        }

        public void log(Level messageLevel, String message) {
            if (isLoggable(messageLevel)) {
                StackFrame sourceFrame = getSource();
                stream.println(unqualifiedName(sourceFrame.getClassName()) +
                               "." + sourceFrame.getMethodName() + ": " + message);
            }
        }

        public void log(Level level, String message, Throwable thrown) {
            if (isLoggable(level)) {
                /*
                 * keep output contiguous and maintain the contract of
                 * RemoteServer.getLog
                 */
                synchronized (stream) {
                    StackFrame sourceFrame = getSource();
                    stream.println(unqualifiedName(sourceFrame.getClassName()) + "." +
                                    sourceFrame.getMethodName() + ": " + message);
                    thrown.printStackTrace(stream);
                }
            }
        }

        public PrintStream getPrintStream() {
            return stream;
        }

        public synchronized void setOutputStream(OutputStream out) {
            if (out != null) {
                if (VERBOSE.intValue() < levelValue) {
                    levelValue = VERBOSE.intValue();
                }
                stream.setOutputStream(out);
            } else {
                /* ensure that messages are not logged */
                levelValue = Level.OFF.intValue();
            }
        }

        /*
         * Mimic old log messages that only contain unqualified names.
         */
        private static String unqualifiedName(String name) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) {
                name = name.substring(lastDot + 1);
            }
            name = name.replace('$', '.');
            return name;
        }
    }

    /**
     * Obtain stack frame of code calling a log method.
     */
    private static StackFrame getSource() {
        return WALKER.walk(s -> s
                                 .skip(3)
                                 .findFirst()
                                 .get());
    }
}
