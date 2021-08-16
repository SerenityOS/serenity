/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * This class overrides write to do CRLF processing as specified in
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

public class TelnetOutputStream extends BufferedOutputStream {
    boolean         stickyCRLF = false;
    boolean         seenCR = false;

    public boolean  binaryMode = false;

    public TelnetOutputStream(OutputStream fd, boolean binary) {
        super(fd);
        binaryMode = binary;
    }

    /**
     * set the stickyCRLF flag. Tells whether the terminal considers CRLF as a single
     * char.
     *
     * @param   on      the <code>boolean</code> to set the flag to.
     */
    public void setStickyCRLF(boolean on) {
        stickyCRLF = on;
    }

    /**
     * Writes the int to the stream and does CR LF processing if necessary.
     */
    public void write(int c) throws IOException {
        if (binaryMode) {
            super.write(c);
            return;
        }

        if (seenCR) {
            if (c != '\n')
                super.write(0);
            super.write(c);
            if (c != '\r')
                seenCR = false;
        } else { // !seenCR
            if (c == '\n') {
                super.write('\r');
                super.write('\n');
                return;
            }
            if (c == '\r') {
                if (stickyCRLF)
                    seenCR = true;
                else {
                    super.write('\r');
                    c = 0;
                }
            }
            super.write(c);
        }
    }

    /**
     * Write the bytes at offset <i>off</i> in buffer <i>bytes</i> for
     * <i>length</i> bytes.
     */
    public void write(byte bytes[], int off, int length) throws IOException {
        if (binaryMode) {
            super.write(bytes, off, length);
            return;
        }

        while (--length >= 0) {
            write(bytes[off++]);
        }
    }
}
