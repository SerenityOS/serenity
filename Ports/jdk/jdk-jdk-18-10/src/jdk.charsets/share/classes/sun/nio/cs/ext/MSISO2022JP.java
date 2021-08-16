/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

public class MSISO2022JP extends ISO2022_JP
{
    public MSISO2022JP() {
        super("x-windows-iso2022jp",
              ExtendedCharsets.aliasesFor("x-windows-iso2022jp"));
    }

    public String historicalName() {
        return "windows-iso2022jp";
    }

    public boolean contains(Charset cs) {
      return super.contains(cs) ||
             (cs instanceof MSISO2022JP);
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this, CoderHolder.DEC0208, null);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this, CoderHolder.ENC0208, null, true);
    }

    private static class CoderHolder {
        static final DoubleByte.Decoder DEC0208 =
            (DoubleByte.Decoder)new JIS_X_0208_MS932().newDecoder();
        static final DoubleByte.Encoder ENC0208 =
            (DoubleByte.Encoder)new JIS_X_0208_MS932().newEncoder();
    }
}
