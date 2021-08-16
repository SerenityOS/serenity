/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.net.ProtocolException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.net.http.HttpHeaders;

import jdk.internal.net.http.common.Utils;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static jdk.internal.net.http.common.Utils.ACCEPT_ALL;

class Http1HeaderParser {

    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final char HT = '\t';
    private static final char SP = ' ';

    private StringBuilder sb = new StringBuilder();
    private String statusLine;
    private int responseCode;
    private HttpHeaders headers;
    private Map<String,List<String>> privateMap = new HashMap<>();

    enum State { INITIAL,
                 STATUS_LINE,
                 STATUS_LINE_FOUND_CR,
                 STATUS_LINE_FOUND_LF,
                 STATUS_LINE_END,
                 STATUS_LINE_END_CR,
                 STATUS_LINE_END_LF,
                 HEADER,
                 HEADER_FOUND_CR,
                 HEADER_FOUND_LF,
                 HEADER_FOUND_CR_LF,
                 HEADER_FOUND_CR_LF_CR,
                 FINISHED }

    private State state = State.INITIAL;

    /** Returns the status-line. */
    String statusLine() { return statusLine; }

    /** Returns the response code. */
    int responseCode() { return responseCode; }

    /** Returns the headers, possibly empty. */
    HttpHeaders headers() {
        assert state == State.FINISHED : "Unexpected state " + state;
        return headers;
    }

    /** A current-state message suitable for inclusion in an exception detail message. */
    public String currentStateMessage() {
        String stateName = state.name();
        String msg;
        if (stateName.contains("INITIAL")) {
            return format("HTTP/1.1 header parser received no bytes");
        } else if (stateName.contains("STATUS")) {
            msg = format("parsing HTTP/1.1 status line, receiving [%s]", sb.toString());
        } else if (stateName.contains("HEADER")) {
            String headerName = sb.toString();
            if (headerName.indexOf(':') != -1)
                headerName = headerName.substring(0, headerName.indexOf(':')+1) + "...";
            msg = format("parsing HTTP/1.1 header, receiving [%s]", headerName);
        } else {
            msg = format("HTTP/1.1 parser receiving [%s]", sb.toString());
        }
        return format("%s, parser state [%s]", msg, state);
    }

    /**
     * Parses HTTP/1.X status-line and headers from the given bytes. Must be
     * called successive times, with additional data, until returns true.
     *
     * All given ByteBuffers will be consumed, until ( possibly ) the last one
     * ( when true is returned ), which may not be fully consumed.
     *
     * @param input the ( partial ) header data
     * @return true iff the end of the headers block has been reached
     */
    boolean parse(ByteBuffer input) throws ProtocolException {
        requireNonNull(input, "null input");

        while (canContinueParsing(input)) {
            switch (state) {
                case INITIAL                                    ->  state = State.STATUS_LINE;
                case STATUS_LINE                                ->  readResumeStatusLine(input);
                case STATUS_LINE_FOUND_CR, STATUS_LINE_FOUND_LF ->  readStatusLineFeed(input);
                case STATUS_LINE_END                            ->  maybeStartHeaders(input);
                case STATUS_LINE_END_CR, STATUS_LINE_END_LF     ->  maybeEndHeaders(input);
                case HEADER                                     ->  readResumeHeader(input);
                case HEADER_FOUND_CR, HEADER_FOUND_LF           ->  resumeOrLF(input);
                case HEADER_FOUND_CR_LF                         ->  resumeOrSecondCR(input);
                case HEADER_FOUND_CR_LF_CR                      ->  resumeOrEndHeaders(input);

                default -> throw new InternalError("Unexpected state: " + state);
            }
        }

        return state == State.FINISHED;
    }

    private boolean canContinueParsing(ByteBuffer buffer) {
        // some states don't require any input to transition
        // to the next state.
        return switch (state) {
            case FINISHED -> false;
            case STATUS_LINE_FOUND_LF, STATUS_LINE_END_LF, HEADER_FOUND_LF -> true;
            default -> buffer.hasRemaining();
        };
    }

    /**
     * Returns a character (char) corresponding to the next byte in the
     * input, interpreted as an ISO-8859-1 encoded character.
     * <p>
     * The ISO-8859-1 encoding is a 8-bit character coding that
     * corresponds to the first 256 Unicode characters - from U+0000 to
     * U+00FF. UTF-16 is backward compatible with ISO-8859-1 - which
     * means each byte in the input should be interpreted as an unsigned
     * value from [0, 255] representing the character code.
     *
     * @param input a {@code ByteBuffer} containing a partial input
     * @return the next byte in the input, interpreted as an ISO-8859-1
     * encoded char
     * @throws BufferUnderflowException
     *          if the input buffer's current position is not smaller
     *          than its limit
     */
    private char get(ByteBuffer input) {
        return (char)(input.get() & 0xFF);
    }

    private void readResumeStatusLine(ByteBuffer input) {
        char c = 0;
        while (input.hasRemaining() && (c = get(input)) != CR) {
            if (c == LF) break;
            sb.append(c);
        }
        if (c == CR) {
            state = State.STATUS_LINE_FOUND_CR;
        } else if (c == LF) {
            state = State.STATUS_LINE_FOUND_LF;
        }
    }

    private void readStatusLineFeed(ByteBuffer input) throws ProtocolException {
        char c = state == State.STATUS_LINE_FOUND_LF ? LF : get(input);
        if (c != LF) {
            throw protocolException("Bad trailing char, \"%s\", when parsing status line, \"%s\"",
                                    c, sb.toString());
        }

        statusLine = sb.toString();
        sb = new StringBuilder();
        if (!statusLine.startsWith("HTTP/1.")) {
            throw protocolException("Invalid status line: \"%s\"", statusLine);
        }
        if (statusLine.length() < 12) {
            throw protocolException("Invalid status line: \"%s\"", statusLine);
        }
        try {
            responseCode = Integer.parseInt(statusLine.substring(9, 12));
        } catch (NumberFormatException nfe) {
            throw protocolException("Invalid status line: \"%s\"", statusLine);
        }
        // response code expected to be a 3-digit integer (RFC-2616, section 6.1.1)
        if (responseCode < 100) {
            throw protocolException("Invalid status line: \"%s\"", statusLine);
        }

        state = State.STATUS_LINE_END;
    }

    private void maybeStartHeaders(ByteBuffer input) {
        assert state == State.STATUS_LINE_END;
        assert sb.length() == 0;
        char c = get(input);
        if (c == CR) {
            state = State.STATUS_LINE_END_CR;
        } else if (c == LF) {
            state = State.STATUS_LINE_END_LF;
        } else {
            sb.append(c);
            state = State.HEADER;
        }
    }

    private void maybeEndHeaders(ByteBuffer input) throws ProtocolException {
        assert state == State.STATUS_LINE_END_CR || state == State.STATUS_LINE_END_LF;
        assert sb.length() == 0;
        char c = state == State.STATUS_LINE_END_LF ? LF : get(input);
        if (c == LF) {
            headers = HttpHeaders.of(privateMap, ACCEPT_ALL);
            privateMap = null;
            state = State.FINISHED;  // no headers
        } else {
            throw protocolException("Unexpected \"%s\", after status line CR", c);
        }
    }

    private void readResumeHeader(ByteBuffer input) {
        assert state == State.HEADER;
        assert input.hasRemaining();
        while (input.hasRemaining()) {
            char c = get(input);
            if (c == CR) {
                state = State.HEADER_FOUND_CR;
                break;
            } else if (c == LF) {
                state = State.HEADER_FOUND_LF;
                break;
            }

            if (c == HT)
                c = SP;
            sb.append(c);
        }
    }

    private void addHeaderFromString(String headerString) throws ProtocolException {
        assert sb.length() == 0;
        int idx = headerString.indexOf(':');
        if (idx == -1)
            return;
        String name = headerString.substring(0, idx);

        // compatibility with HttpURLConnection;
        if (name.isEmpty()) return;

        if (!Utils.isValidName(name)) {
            throw protocolException("Invalid header name \"%s\"", name);
        }
        String value = headerString.substring(idx + 1).trim();
        if (!Utils.isValidValue(value)) {
            throw protocolException("Invalid header value \"%s: %s\"", name, value);
        }

        privateMap.computeIfAbsent(name.toLowerCase(Locale.US),
                                   k -> new ArrayList<>()).add(value);
    }

    private void resumeOrLF(ByteBuffer input) {
        assert state == State.HEADER_FOUND_CR || state == State.HEADER_FOUND_LF;
        char c = state == State.HEADER_FOUND_LF ? LF : get(input);
        if (c == LF) {
            // header value will be flushed by
            // resumeOrSecondCR if next line does not
            // begin by SP or HT
            state = State.HEADER_FOUND_CR_LF;
        } else if (c == SP || c == HT) {
            sb.append(SP); // parity with MessageHeaders
            state = State.HEADER;
        } else {
            sb = new StringBuilder();
            sb.append(c);
            state = State.HEADER;
        }
    }

    private void resumeOrSecondCR(ByteBuffer input) throws ProtocolException {
        assert state == State.HEADER_FOUND_CR_LF;
        char c = get(input);
        if (c == CR || c == LF) {
            if (sb.length() > 0) {
                // no continuation line - flush
                // previous header value.
                String headerString = sb.toString();
                sb = new StringBuilder();
                addHeaderFromString(headerString);
            }
            if (c == CR) {
                state = State.HEADER_FOUND_CR_LF_CR;
            } else {
                state = State.FINISHED;
                headers = HttpHeaders.of(privateMap, ACCEPT_ALL);
                privateMap = null;
            }
        } else if (c == SP || c == HT) {
            assert sb.length() != 0;
            sb.append(SP); // continuation line
            state = State.HEADER;
        } else {
            if (sb.length() > 0) {
                // no continuation line - flush
                // previous header value.
                String headerString = sb.toString();
                sb = new StringBuilder();
                addHeaderFromString(headerString);
            }
            sb.append(c);
            state = State.HEADER;
        }
    }

    private void resumeOrEndHeaders(ByteBuffer input) throws ProtocolException {
        assert state == State.HEADER_FOUND_CR_LF_CR;
        char c = get(input);
        if (c == LF) {
            state = State.FINISHED;
            headers = HttpHeaders.of(privateMap, ACCEPT_ALL);
            privateMap = null;
        } else {
            throw protocolException("Unexpected \"%s\", after CR LF CR", c);
        }
    }

    private ProtocolException protocolException(String format, Object... args) {
        return new ProtocolException(format(format, args));
    }
}
