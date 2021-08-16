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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import jdk.internal.logger.SurrogateLogger;

/**
 * Print a brief summary of the {@code LogRecord} in a human readable
 * format.  The summary will typically be 1 or 2 lines.
 *
 * <p>
 * <a id="formatting">
 * <b>Configuration:</b></a>
 * The {@code SimpleFormatter} is initialized with the format string
 * specified in the {@systemProperty java.util.logging.SimpleFormatter.format}
 * property to {@linkplain #format(LogRecord) format} the log messages.
 * This property can be defined
 * in the {@linkplain LogManager#getProperty logging properties}
 * configuration file
 * or as a system property.  If this property is set in both
 * the logging properties and system properties,
 * the format string specified in the system property will be used.
 * If this property is not defined or the given format string
 * is {@linkplain java.util.IllegalFormatException illegal},
 * the default format is implementation-specific.
 *
 * @since 1.4
 * @see java.util.Formatter
 */

public class SimpleFormatter extends Formatter {

    // format string for printing the log record
    static String getLoggingProperty(String name) {
        return LogManager.getLogManager().getProperty(name);
    }

    private final String format =
        SurrogateLogger.getSimpleFormat(SimpleFormatter::getLoggingProperty);

    /**
     * Create a {@code SimpleFormatter}.
     */
    public SimpleFormatter() {}

    /**
     * Format the given LogRecord.
     * <p>
     * The formatting can be customized by specifying the format string
     * in the <a href="#formatting">
     * {@code java.util.logging.SimpleFormatter.format}</a> property.
     * The given {@code LogRecord} will be formatted as if by calling:
     * <pre>
     *    {@link String#format String.format}(format, date, source, logger, level, message, thrown);
     * </pre>
     * where the arguments are:<br>
     * <ol>
     * <li>{@code format} - the {@link java.util.Formatter
     *     java.util.Formatter} format string specified in the
     *     {@code java.util.logging.SimpleFormatter.format} property
     *     or the default format.</li>
     * <li>{@code date} - a {@link ZonedDateTime} object representing
     *     {@linkplain LogRecord#getInstant() event time} of the log record
     *      in the {@link ZoneId#systemDefault()} system time zone.</li>
     * <li>{@code source} - a string representing the caller, if available;
     *     otherwise, the logger's name.</li>
     * <li>{@code logger} - the logger's name.</li>
     * <li>{@code level} - the {@linkplain Level#getLocalizedName
     *     log level}.</li>
     * <li>{@code message} - the formatted log message
     *     returned from the {@link Formatter#formatMessage(LogRecord)}
     *     method.  It uses {@link java.text.MessageFormat java.text}
     *     formatting and does not use the {@code java.util.Formatter
     *     format} argument.</li>
     * <li>{@code thrown} - a string representing
     *     the {@linkplain LogRecord#getThrown throwable}
     *     associated with the log record and its backtrace
     *     beginning with a newline character, if any;
     *     otherwise, an empty string.</li>
     * </ol>
     *
     * <p>Some example formats:<br>
     * <ul>
     * <li> {@code java.util.logging.SimpleFormatter.format="%4$s: %5$s [%1$tc]%n"}
     *     <p>This prints 1 line with the log level ({@code 4$}),
     *     the log message ({@code 5$}) and the timestamp ({@code 1$}) in
     *     a square bracket.
     *     <pre>
     *     WARNING: warning message [Tue Mar 22 13:11:31 PDT 2011]
     *     </pre></li>
     * <li> {@code java.util.logging.SimpleFormatter.format="%1$tc %2$s%n%4$s: %5$s%6$s%n"}
     *     <p>This prints 2 lines where the first line includes
     *     the timestamp ({@code 1$}) and the source ({@code 2$});
     *     the second line includes the log level ({@code 4$}) and
     *     the log message ({@code 5$}) followed by the throwable
     *     and its backtrace ({@code 6$}), if any:
     *     <pre>
     *     Tue Mar 22 13:11:31 PDT 2011 MyClass fatal
     *     SEVERE: several message with an exception
     *     java.lang.IllegalArgumentException: invalid argument
     *             at MyClass.mash(MyClass.java:9)
     *             at MyClass.crunch(MyClass.java:6)
     *             at MyClass.main(MyClass.java:3)
     *     </pre></li>
     * <li> {@code java.util.logging.SimpleFormatter.format="%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%n"}
     *      <p>This prints 2 lines similar to the example above
     *         with a different date/time formatting and does not print
     *         the throwable and its backtrace:
     *     <pre>
     *     Mar 22, 2011 1:11:31 PM MyClass fatal
     *     SEVERE: several message with an exception
     *     </pre></li>
     * <li> {@code java.util.logging.SimpleFormatter.format="%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS.%1$tN %1$Tp %2$s%n%4$s: %5$s%6$s%n"}
     *      <p>Since JDK 9, {@code java.util.logging} uses {@link
     *         java.time.Clock#systemUTC() java.time} to create more precise time
     *         stamps.
     *         The format above can be used to add a {@code .%1$tN} to the
     *         date/time formatting so that nanoseconds will also be printed:
     *     <pre>
     *     Feb 06, 2015 5:33:10.279216000 PM example.Main main
     *     INFO: This is a test
     *     </pre></li>
     * </ul>
     * <p>This method can also be overridden in a subclass.
     * It is recommended to use the {@link Formatter#formatMessage}
     * convenience method to localize and format the message field.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public String format(LogRecord record) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                record.getInstant(), ZoneId.systemDefault());
        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
               source += " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(format,
                             zdt,
                             source,
                             record.getLoggerName(),
                             record.getLevel().getLocalizedLevelName(),
                             message,
                             throwable);
    }
}
