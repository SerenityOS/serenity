/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.*;


//This class delegates all invokes to the charset "javaCs" if
//its subclasses do not provide their own en/decode solution.

public class AWTCharset extends Charset {
    protected Charset awtCs;
    protected Charset javaCs;

    public AWTCharset(String awtCsName, Charset javaCs) {
        super(awtCsName, null);
        this.javaCs = javaCs;
        this.awtCs = this;
    }

    public boolean contains(Charset cs) {
        if (javaCs == null) return false;
        return javaCs.contains(cs);
    }

    public CharsetEncoder newEncoder() {
        if (javaCs == null)
            throw new Error("Encoder is not supported by this Charset");
        return new Encoder(javaCs.newEncoder());
    }

    public CharsetDecoder newDecoder() {
        if (javaCs == null)
            throw new Error("Decoder is not supported by this Charset");
        return new Decoder(javaCs.newDecoder());
    }

    public class Encoder extends CharsetEncoder {
        protected CharsetEncoder enc;
        protected Encoder () {
            this(javaCs.newEncoder());
        }
        protected Encoder (CharsetEncoder enc) {
            super(awtCs,
                  enc.averageBytesPerChar(),
                  enc.maxBytesPerChar());
            this.enc = enc;
        }
        public boolean canEncode(char c) {
            return enc.canEncode(c);
        }
        public boolean canEncode(CharSequence cs) {
            return enc.canEncode(cs);
        }
        protected CoderResult encodeLoop(CharBuffer src, ByteBuffer dst) {
            return enc.encode(src, dst, true);
        }
        protected CoderResult implFlush(ByteBuffer out) {
            return enc.flush(out);
        }
        protected void implReset() {
            enc.reset();
        }
        protected void implReplaceWith(byte[] newReplacement) {
            if (enc != null)
                enc.replaceWith(newReplacement);
        }
        protected void implOnMalformedInput(CodingErrorAction newAction) {
            enc.onMalformedInput(newAction);
        }
        protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
            enc.onUnmappableCharacter(newAction);
        }
        public boolean isLegalReplacement(byte[] repl) {
            return true;
        }
    }

    public class Decoder extends CharsetDecoder {
        protected CharsetDecoder dec;
        private String nr;

        protected Decoder () {
            this(javaCs.newDecoder());
        }

        protected Decoder (CharsetDecoder dec) {
            super(awtCs,
                  dec.averageCharsPerByte(),
                  dec.maxCharsPerByte());
            this.dec = dec;
        }
        protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
            return dec.decode(src, dst, true);
        }
        ByteBuffer fbb = ByteBuffer.allocate(0);
        protected CoderResult implFlush(CharBuffer out) {
            dec.decode(fbb, out, true);
            return dec.flush(out);
        }
        protected void implReset() {
            dec.reset();
        }
        protected void implReplaceWith(String newReplacement) {
            if (dec != null)
                dec.replaceWith(newReplacement);
        }
        protected void implOnMalformedInput(CodingErrorAction newAction) {
            dec.onMalformedInput(newAction);
        }
        protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
            dec.onUnmappableCharacter(newAction);
        }
    }
}
