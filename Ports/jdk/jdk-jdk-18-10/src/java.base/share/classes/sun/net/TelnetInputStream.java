/*
 * Copyright (c) 1994, 1995, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This class provides input and output streams for telnet clients.
 * This class overrides read to do CRLF processing as specified in
 * RFC 854. The class assumes it is running on a system where lines
 * are terminated with a single newline {@literal <LF>} character.
 *
 * This is the relevant section of RFC 824 regarding CRLF processing:
 *
 * <pre>
 * The sequence "CR LF", as defined, will cause the NVT to be
 * positioned at the left margin of the next print line (as would,
 * for example, the sequence "LF CR").  However, many systems and
 * terminals do not treat CR and LF independently, and will have to
 * go to some effort to simulate their effect.  (For example, some
 * terminals do not have a CR independent of the LF, but on such
 * terminals it may be possible to simulate a CR by backspacing.)
 * Therefore, the sequence "CR LF" must be treated as a single "new
 * line" character and used whenever their combined action is
 * intended; the sequence "CR NUL" must be used where a carriage
 * return alone is actually desired; and the CR character must be
 * avoided in other contexts.  This rule gives assurance to systems
 * which must decide whether to perform a "new line" function or a
 * multiple-backspace that the TELNET stream contains a character
 * following a CR that will allow a rational decision.
 *
 *    Note that "CR LF" or "CR NUL" is required in both directions
 *    (in the default ASCII mode), to preserve the symmetry of the
 *    NVT model.  Even though it may be known in some situations
 *    (e.g., with remote echo and suppress go ahead options in
 *    effect) that characters are not being sent to an actual
 *    printer, nonetheless, for the sake of consistency, the protocol
 *    requires that a NUL be inserted following a CR not followed by
 *    a LF in the data stream.  The converse of this is that a NUL
 *    received in the data stream after a CR (in the absence of
 *    options negotiations which explicitly specify otherwise) should
 *    be stripped out prior to applying the NVT to local character
 *    set mapping.
 * </pre>
 *
 * @author      Jonathan Payne
 */

public class TelnetInputStream extends FilterInputStream {
    /** If stickyCRLF is true, then we're a machine, like an IBM PC,
        where a Newline is a CR followed by LF.  On UNIX, this is false
        because Newline is represented with just a LF character. */
    boolean         stickyCRLF = false;
    boolean         seenCR = false;

    public boolean  binaryMode = false;

    public TelnetInputStream(InputStream fd, boolean binary) {
        super(fd);
        binaryMode = binary;
    }

    public void setStickyCRLF(boolean on) {
        stickyCRLF = on;
    }

    public int read() throws IOException {
        if (binaryMode)
            return super.read();

        int c;

        /* If last time we determined we saw a CRLF pair, and we're
           not turning that into just a Newline (that is, we're
           stickyCRLF), then return the LF part of that sticky
           pair now. */

        if (seenCR) {
            seenCR = false;
            return '\n';
        }

        if ((c = super.read()) == '\r') {    /* CR */
            switch (c = super.read()) {
            default:
            case -1:                        /* this is an error */
                throw new TelnetProtocolException("misplaced CR in input");

            case 0:                         /* NUL - treat CR as CR */
                return '\r';

            case '\n':                      /* CRLF - treat as NL */
                if (stickyCRLF) {
                    seenCR = true;
                    return '\r';
                } else {
                    return '\n';
                }
            }
        }
        return c;
    }

    /** read into a byte array */
    public int read(byte bytes[]) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    /**
     * Read into a byte array at offset <i>off</i> for length <i>length</i>
     * bytes.
     */
    public int read(byte bytes[], int off, int length) throws IOException {
        if (binaryMode)
            return super.read(bytes, off, length);

        int c;
        int offStart = off;

        while (--length >= 0) {
            c = read();
            if (c == -1)
                break;
            bytes[off++] = (byte)c;
        }
        return (off > offStart) ? off - offStart : -1;
    }
}
