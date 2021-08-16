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

package sun.nio.cs;

import java.nio.charset.Charset;

abstract class Unicode extends Charset
    implements HistoricallyNamedCharset
{
    public Unicode(String name, String[] aliases) {
        super(name, aliases);
    }

    public boolean contains(Charset cs) {
        return ((cs instanceof US_ASCII)
                || (cs instanceof ISO_8859_1)
                || (cs instanceof ISO_8859_15)
                || (cs instanceof ISO_8859_16)
                || (cs instanceof MS1252)
                || (cs instanceof UTF_8)
                || (cs instanceof UTF_16)
                || (cs instanceof UTF_16BE)
                || (cs instanceof UTF_16LE)
                || (cs instanceof UTF_16LE_BOM)
                || (cs.name().equals("GBK"))
                || (cs.name().equals("GB18030"))
                || (cs.name().equals("ISO-8859-2"))
                || (cs.name().equals("ISO-8859-3"))
                || (cs.name().equals("ISO-8859-4"))
                || (cs.name().equals("ISO-8859-5"))
                || (cs.name().equals("ISO-8859-6"))
                || (cs.name().equals("ISO-8859-7"))
                || (cs.name().equals("ISO-8859-8"))
                || (cs.name().equals("ISO-8859-9"))
                || (cs.name().equals("ISO-8859-13"))
                || (cs.name().equals("JIS_X0201"))
                || (cs.name().equals("x-JIS0208"))
                || (cs.name().equals("JIS_X0212-1990"))
                || (cs.name().equals("GB2312"))
                || (cs.name().equals("EUC-KR"))
                || (cs.name().equals("x-EUC-TW"))
                || (cs.name().equals("EUC-JP"))
                || (cs.name().equals("x-euc-jp-linux"))
                || (cs.name().equals("KOI8-R"))
                || (cs.name().equals("TIS-620"))
                || (cs.name().equals("x-ISCII91"))
                || (cs.name().equals("windows-1251"))
                || (cs.name().equals("windows-1253"))
                || (cs.name().equals("windows-1254"))
                || (cs.name().equals("windows-1255"))
                || (cs.name().equals("windows-1256"))
                || (cs.name().equals("windows-1257"))
                || (cs.name().equals("windows-1258"))
                || (cs.name().equals("windows-932"))
                || (cs.name().equals("x-mswin-936"))
                || (cs.name().equals("x-windows-949"))
                || (cs.name().equals("x-windows-950"))
                || (cs.name().equals("windows-31j"))
                || (cs.name().equals("Big5"))
                || (cs.name().equals("Big5-HKSCS"))
                || (cs.name().equals("x-MS950-HKSCS"))
                || (cs.name().equals("ISO-2022-JP"))
                || (cs.name().equals("ISO-2022-KR"))
                || (cs.name().equals("x-ISO-2022-CN-CNS"))
                || (cs.name().equals("x-ISO-2022-CN-GB"))
                || (cs.name().equals("x-Johab"))
                || (cs.name().equals("Shift_JIS")));
    }
}
