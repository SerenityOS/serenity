/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

/*
 * COPYRIGHT AND PERMISSION NOTICE
 *
 * Copyright (C) 1991-2012 Unicode, Inc. All rights reserved. Distributed under
 * the Terms of Use in http://www.unicode.org/copyright.html.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of the Unicode data files and any associated documentation (the "Data
 * Files") or Unicode software and any associated documentation (the
 * "Software") to deal in the Data Files or Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, and/or sell copies of the Data Files or Software, and
 * to permit persons to whom the Data Files or Software are furnished to do so,
 * provided that (a) the above copyright notice(s) and this permission notice
 * appear with all copies of the Data Files or Software, (b) both the above
 * copyright notice(s) and this permission notice appear in associated
 * documentation, and (c) there is clear notice in each modified Data File or
 * in the Software as well as in the documentation associated with the Data
 * File(s) or Software that the data or software has been modified.
 *
 * THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
 * THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS
 * INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 * DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
 * OF THE DATA FILES OR SOFTWARE.
 *
 * Except as contained in this notice, the name of a copyright holder shall not
 * be used in advertising or otherwise to promote the sale, use or other
 * dealings in these Data Files or Software without prior written authorization
 * of the copyright holder.
 */

package sun.text.resources.ext;

import sun.util.resources.ParallelListResourceBundle;

public class FormatData_hu extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "janu\u00e1r", // january
                    "febru\u00e1r", // february
                    "m\u00e1rcius", // march
                    "\u00e1prilis", // april
                    "m\u00e1jus", // may
                    "j\u00fanius", // june
                    "j\u00falius", // july
                    "augusztus", // august
                    "szeptember", // september
                    "okt\u00f3ber", // october
                    "november", // november
                    "december", // december
                    "" // month 13 if applicable
                }
            },
            { "standalone.MonthNames",
                new String[] {
                    "janu\u00e1r",
                    "febru\u00e1r",
                    "m\u00e1rcius",
                    "\u00e1prilis",
                    "m\u00e1jus",
                    "j\u00fanius",
                    "j\u00falius",
                    "augusztus",
                    "szeptember",
                    "okt\u00f3ber",
                    "november",
                    "december",
                    "",
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "jan.", // abb january
                    "febr.", // abb february
                    "m\u00e1rc.", // abb march
                    "\u00e1pr.", // abb april
                    "m\u00e1j.", // abb may
                    "j\u00fan.", // abb june
                    "j\u00fal.", // abb july
                    "aug.", // abb august
                    "szept.", // abb september
                    "okt.", // abb october
                    "nov.", // abb november
                    "dec.", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "jan.",
                    "febr.",
                    "m\u00e1rc.",
                    "\u00e1pr.",
                    "m\u00e1j.",
                    "j\u00fan.",
                    "j\u00fal.",
                    "aug.",
                    "szept.",
                    "okt.",
                    "nov.",
                    "dec.",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "J",
                    "F",
                    "M",
                    "\u00c1",
                    "M",
                    "J",
                    "J",
                    "A",
                    "Sz",
                    "O",
                    "N",
                    "D",
                    "",
                }
            },
            { "standalone.MonthNarrows",
                new String[] {
                    "J",
                    "F",
                    "M",
                    "\u00c1",
                    "M",
                    "J",
                    "J",
                    "A",
                    "Sz",
                    "O",
                    "N",
                    "D",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "vas\u00e1rnap", // Sunday
                    "h\u00e9tf\u0151", // Monday
                    "kedd", // Tuesday
                    "szerda", // Wednesday
                    "cs\u00fct\u00f6rt\u00f6k", // Thursday
                    "p\u00e9ntek", // Friday
                    "szombat" // Saturday
                }
            },
            { "standalone.DayNames",
                new String[] {
                    "vas\u00e1rnap",
                    "h\u00e9tf\u0151",
                    "kedd",
                    "szerda",
                    "cs\u00fct\u00f6rt\u00f6k",
                    "p\u00e9ntek",
                    "szombat",
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "V", // abb Sunday
                    "H", // abb Monday
                    "K", // abb Tuesday
                    "Sze", // abb Wednesday
                    "Cs", // abb Thursday
                    "P", // abb Friday
                    "Szo" // abb Saturday
                }
            },
            { "standalone.DayAbbreviations",
                new String[] {
                    "V",
                    "H",
                    "K",
                    "Sze",
                    "Cs",
                    "P",
                    "Szo",
                }
            },
            { "DayNarrows",
                new String[] {
                    "V",
                    "H",
                    "K",
                    "Sz",
                    "Cs",
                    "P",
                    "Sz",
                }
            },
            { "standalone.DayNarrows",
                new String[] {
                    "V",
                    "H",
                    "K",
                    "Sz",
                    "Cs",
                    "P",
                    "Sz",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "DE", // am marker
                    "DU" // pm marker
                }
            },
            { "Eras",
                new String[] { // era strings
                    "i.e.",
                    "i.u."
                }
            },
            { "short.Eras",
                new String[] {
                    "i. e.",
                    "i. sz.",
                }
            },
            { "NumberElements",
                new String[] {
                    ",", // decimal separator
                    "\u00a0", // group (thousands) separator
                    ";", // list separator
                    "%", // percent sign
                    "0", // native 0 digit
                    "#", // pattern digit
                    "-", // minus sign
                    "E", // exponential
                    "\u2030", // per mille
                    "\u221e", // infinity
                    "\ufffd" // NaN
                }
            },
            { "TimePatterns",
                new String[] {
                    "H:mm:ss z", // full time pattern
                    "H:mm:ss z", // long time pattern
                    "H:mm:ss", // medium time pattern
                    "H:mm", // short time pattern
                }
            },
            { "DatePatterns",
                new String[] {
                    "yyyy. MMMM d.", // full date pattern
                    "yyyy. MMMM d.", // long date pattern
                    "yyyy.MM.dd.", // medium date pattern
                    "yyyy.MM.dd.", // short date pattern
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" // date-time pattern
                }
            },
            { "DateTimePatternChars", "GanjkHmsSEDFwWxhKzZ" },
            { "buddhist.Eras",
                new String[] {
                    "BC",
                    "BK",
                }
            },
            { "buddhist.short.Eras",
                new String[] {
                    "BC",
                    "BK",
                }
            },
        };
    }
}
