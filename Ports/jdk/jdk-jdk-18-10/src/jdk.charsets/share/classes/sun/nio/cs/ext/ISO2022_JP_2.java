/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.cs.ext;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import sun.nio.cs.DoubleByte;
import sun.nio.cs.*;

public class ISO2022_JP_2 extends ISO2022_JP
{
    public ISO2022_JP_2() {
        super("ISO-2022-JP-2",
              ExtendedCharsets.aliasesFor("ISO-2022-JP-2"));
    }

    public String historicalName() {
        return "ISO2022JP2";
    }

    public boolean contains(Charset cs) {
      return super.contains(cs) ||
             (cs instanceof JIS_X_0212) ||
             (cs instanceof ISO2022_JP_2);
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this, Decoder.DEC0208, CoderHolder.DEC0212);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this, Encoder.ENC0208, CoderHolder.ENC0212, true);
    }

    private static class CoderHolder {
        static final DoubleByte.Decoder DEC0212 =
            (DoubleByte.Decoder)new JIS_X_0212().newDecoder();
        static final DoubleByte.Encoder ENC0212 =
            (DoubleByte.Encoder)new JIS_X_0212().newEncoder();
    }
}
