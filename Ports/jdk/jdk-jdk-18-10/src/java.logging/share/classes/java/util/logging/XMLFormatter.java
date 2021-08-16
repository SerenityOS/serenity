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

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Format a LogRecord into a standard XML format.
 * <p>
 * The DTD specification is provided as Appendix A to the
 * Java Logging APIs specification.
 * <p>
 * The XMLFormatter can be used with arbitrary character encodings,
 * but it is recommended that it normally be used with UTF-8.  The
 * character encoding can be set on the output Handler.
 *
 * @implSpec Since JDK 9, instances of {@linkplain LogRecord} contain
 * an {@link LogRecord#getInstant() Instant} which can have nanoseconds below
 * the millisecond resolution.
 * The DTD specification has been updated to allow for an optional
 * {@code <nanos>} element. By default, the XMLFormatter will compute the
 * nanosecond adjustment below the millisecond resolution (using
 * {@code LogRecord.getInstant().getNano() % 1000_000}) - and if this is not 0,
 * this adjustment value will be printed in the new {@code <nanos>} element.
 * The event instant can then be reconstructed using
 * {@code Instant.ofEpochSecond(millis/1000L, (millis % 1000L) * 1000_000L + nanos)}
 * where {@code millis} and {@code nanos} represent the numbers serialized in
 * the {@code <millis>} and {@code <nanos>} elements, respectively.
 * <br>
 * The {@code <date>} element will now contain the whole instant as formatted
 * by the {@link DateTimeFormatter#ISO_INSTANT DateTimeFormatter.ISO_INSTANT}
 * formatter.
 * <p>
 * For compatibility with old parsers, XMLFormatters can
 * be configured to revert to the old format by specifying a
 * {@code <xml-formatter-fully-qualified-class-name>.useInstant = false}
 * {@linkplain LogManager#getProperty(java.lang.String) property} in the
 * logging configuration. When {@code useInstant} is {@code false}, the old
 * formatting will be preserved. When {@code useInstant} is {@code true}
 * (the default), the {@code <nanos>} element will be printed and the
 * {@code <date>} element will contain the {@linkplain
 * DateTimeFormatter#ISO_INSTANT formatted} instant.
 * <p>
 * For instance, in order to configure plain instances of XMLFormatter to omit
 * the new {@code <nano>} element,
 * {@code java.util.logging.XMLFormatter.useInstant = false} can be specified
 * in the logging configuration.
 *
 * @since 1.4
 */

public class XMLFormatter extends Formatter {
    private final LogManager manager = LogManager.getLogManager();
    private final boolean useInstant;

    /**
     * Creates a new instance of XMLFormatter.
     *
     * @implSpec
     *    Since JDK 9, the XMLFormatter will print out the record {@linkplain
     *    LogRecord#getInstant() event time} as an Instant. This instant
     *    has the best resolution available on the system. The {@code <date>}
     *    element will contain the instant as formatted by the {@link
     *    DateTimeFormatter#ISO_INSTANT}.
     *    In addition, an optional {@code <nanos>} element containing a
     *    nanosecond adjustment will be printed if the instant contains some
     *    nanoseconds below the millisecond resolution.
     *    <p>
     *    This new behavior can be turned off, and the old formatting restored,
     *    by specifying a property in the {@linkplain
     *    LogManager#getProperty(java.lang.String) logging configuration}.
     *    If {@code LogManager.getLogManager().getProperty(
     *    this.getClass().getName()+".useInstant")} is {@code "false"} or
     *    {@code "0"}, the old formatting will be restored.
     */
    public XMLFormatter() {
        useInstant = (manager == null)
            || manager.getBooleanProperty(
                    this.getClass().getName()+".useInstant", true);
    }

    // Append a two digit number.
    private void a2(StringBuilder sb, int x) {
        if (x < 10) {
            sb.append('0');
        }
        sb.append(x);
    }

    // Append the time and date in ISO 8601 format
    private void appendISO8601(StringBuilder sb, long millis) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);
        sb.append(cal.get(Calendar.YEAR));
        sb.append('-');
        a2(sb, cal.get(Calendar.MONTH) + 1);
        sb.append('-');
        a2(sb, cal.get(Calendar.DAY_OF_MONTH));
        sb.append('T');
        a2(sb, cal.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        a2(sb, cal.get(Calendar.MINUTE));
        sb.append(':');
        a2(sb, cal.get(Calendar.SECOND));
    }

    // Append to the given StringBuilder an escaped version of the
    // given text string where XML special characters have been escaped.
    // For a null string we append "<null>"
    private void escape(StringBuilder sb, String text) {
        if (text == null) {
            text = "<null>";
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '<') {
                sb.append("&lt;");
            } else if (ch == '>') {
                sb.append("&gt;");
            } else if (ch == '&') {
                sb.append("&amp;");
            } else {
                sb.append(ch);
            }
        }
    }

    /**
     * Format the given message to XML.
     * <p>
     * This method can be overridden in a subclass.
     * It is recommended to use the {@link Formatter#formatMessage}
     * convenience method to localize and format the message field.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder(500);
        sb.append("<record>\n");

        final Instant instant = record.getInstant();

        sb.append("  <date>");
        if (useInstant) {
            // If useInstant is true - we will print the instant in the
            // date field, using the ISO_INSTANT formatter.
            DateTimeFormatter.ISO_INSTANT.formatTo(instant, sb);
        } else {
            // If useInstant is false - we will keep the 'old' formating
            appendISO8601(sb, instant.toEpochMilli());
        }
        sb.append("</date>\n");

        sb.append("  <millis>");
        sb.append(instant.toEpochMilli());
        sb.append("</millis>\n");

        final int nanoAdjustment = instant.getNano() % 1000_000;
        if (useInstant && nanoAdjustment != 0) {
            sb.append("  <nanos>");
            sb.append(nanoAdjustment);
            sb.append("</nanos>\n");
        }

        sb.append("  <sequence>");
        sb.append(record.getSequenceNumber());
        sb.append("</sequence>\n");

        String name = record.getLoggerName();
        if (name != null) {
            sb.append("  <logger>");
            escape(sb, name);
            sb.append("</logger>\n");
        }

        sb.append("  <level>");
        escape(sb, record.getLevel().toString());
        sb.append("</level>\n");

        if (record.getSourceClassName() != null) {
            sb.append("  <class>");
            escape(sb, record.getSourceClassName());
            sb.append("</class>\n");
        }

        if (record.getSourceMethodName() != null) {
            sb.append("  <method>");
            escape(sb, record.getSourceMethodName());
            sb.append("</method>\n");
        }

        sb.append("  <thread>");
        sb.append(record.getLongThreadID());
        sb.append("</thread>\n");

        if (record.getMessage() != null) {
            // Format the message string and its accompanying parameters.
            String message = formatMessage(record);
            sb.append("  <message>");
            escape(sb, message);
            sb.append("</message>");
            sb.append("\n");
        }

        // If the message is being localized, output the key, resource
        // bundle name, and params.
        ResourceBundle bundle = record.getResourceBundle();
        try {
            if (bundle != null && bundle.getString(record.getMessage()) != null) {
                sb.append("  <key>");
                escape(sb, record.getMessage());
                sb.append("</key>\n");
                sb.append("  <catalog>");
                escape(sb, record.getResourceBundleName());
                sb.append("</catalog>\n");
            }
        } catch (Exception ex) {
            // The message is not in the catalog.  Drop through.
        }

        Object parameters[] = record.getParameters();
        //  Check to see if the parameter was not a messagetext format
        //  or was not null or empty
        if (parameters != null && parameters.length != 0
                && record.getMessage().indexOf('{') == -1 ) {
            for (Object parameter : parameters) {
                sb.append("  <param>");
                try {
                    escape(sb, parameter.toString());
                } catch (Exception ex) {
                    sb.append("???");
                }
                sb.append("</param>\n");
            }
        }

        if (record.getThrown() != null) {
            // Report on the state of the throwable.
            Throwable th = record.getThrown();
            sb.append("  <exception>\n");
            sb.append("    <message>");
            escape(sb, th.toString());
            sb.append("</message>\n");
            StackTraceElement trace[] = th.getStackTrace();
            for (StackTraceElement frame : trace) {
                sb.append("    <frame>\n");
                sb.append("      <class>");
                escape(sb, frame.getClassName());
                sb.append("</class>\n");
                sb.append("      <method>");
                escape(sb, frame.getMethodName());
                sb.append("</method>\n");
                // Check for a line number.
                if (frame.getLineNumber() >= 0) {
                    sb.append("      <line>");
                    sb.append(frame.getLineNumber());
                    sb.append("</line>\n");
                }
                sb.append("    </frame>\n");
            }
            sb.append("  </exception>\n");
        }

        sb.append("</record>\n");
        return sb.toString();
    }

    /**
     * Return the header string for a set of XML formatted records.
     *
     * @param   h  The target handler (can be null)
     * @return  a valid XML string
     */
    @Override
    public String getHead(Handler h) {
        StringBuilder sb = new StringBuilder();
        String encoding;
        sb.append("<?xml version=\"1.0\"");

        if (h != null) {
            encoding = h.getEncoding();
        } else {
            encoding = null;
        }

        if (encoding == null) {
            // Figure out the default encoding.
            encoding = java.nio.charset.Charset.defaultCharset().name();
        }
        // Try to map the encoding name to a canonical name.
        try {
            Charset cs = Charset.forName(encoding);
            encoding = cs.name();
        } catch (Exception ex) {
            // We hit problems finding a canonical name.
            // Just use the raw encoding name.
        }

        sb.append(" encoding=\"");
        sb.append(encoding);
        sb.append("\"");
        sb.append(" standalone=\"no\"?>\n");

        sb.append("<!DOCTYPE log SYSTEM \"logger.dtd\">\n");
        sb.append("<log>\n");
        return sb.toString();
    }

    /**
     * Return the tail string for a set of XML formatted records.
     *
     * @param   h  The target handler (can be null)
     * @return  a valid XML string
     */
    @Override
    public String getTail(Handler h) {
        return "</log>\n";
    }
}
