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

public class FormatData_cs extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "ledna",
                    "\u00fanora",
                    "b\u0159ezna",
                    "dubna",
                    "kv\u011btna",
                    "\u010dervna",
                    "\u010dervence",
                    "srpna",
                    "z\u00e1\u0159\u00ed",
                    "\u0159\u00edjna",
                    "listopadu",
                    "prosince",
                    "",
                }
            },
            { "standalone.MonthNames",
                new String[] {
                    "leden", // january
                    "\u00fanor", // february
                    "b\u0159ezen", // march
                    "duben", // april
                    "kv\u011bten", // may
                    "\u010derven", // june
                    "\u010dervenec", // july
                    "srpen", // august
                    "z\u00e1\u0159\u00ed", // september
                    "\u0159\u00edjen", // october
                    "listopad", // november
                    "prosinec", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "Led",
                    "\u00dano",
                    "B\u0159e",
                    "Dub",
                    "Kv\u011b",
                    "\u010cer",
                    "\u010cvc",
                    "Srp",
                    "Z\u00e1\u0159",
                    "\u0158\u00edj",
                    "Lis",
                    "Pro",
                    "",
                }
            },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "I", // abb january
                    "II", // abb february
                    "III", // abb march
                    "IV", // abb april
                    "V", // abb may
                    "VI", // abb june
                    "VII", // abb july
                    "VIII", // abb august
                    "IX", // abb september
                    "X", // abb october
                    "XI", // abb november
                    "XII", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "MonthNarrows",
                new String[] {
                    "l",
                    "\u00fa",
                    "b",
                    "d",
                    "k",
                    "\u010d",
                    "\u010d",
                    "s",
                    "z",
                    "\u0159",
                    "l",
                    "p",
                    "",
                }
            },
            { "standalone.MonthNarrows",
                new String[] {
                    "l",
                    "\u00fa",
                    "b",
                    "d",
                    "k",
                    "\u010d",
                    "\u010d",
                    "s",
                    "z",
                    "\u0159",
                    "l",
                    "p",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "Ned\u011ble", // Sunday
                    "Pond\u011bl\u00ed", // Monday
                    "\u00dater\u00fd", // Tuesday
                    "St\u0159eda", // Wednesday
                    "\u010ctvrtek", // Thursday
                    "P\u00e1tek", // Friday
                    "Sobota" // Saturday
                }
            },
            { "standalone.DayNames",
                new String[] {
                    "ned\u011ble",
                    "pond\u011bl\u00ed",
                    "\u00fater\u00fd",
                    "st\u0159eda",
                    "\u010dtvrtek",
                    "p\u00e1tek",
                    "sobota",
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "Ne", // abb Sunday
                    "Po", // abb Monday
                    "\u00dat", // abb Tuesday
                    "St", // abb Wednesday
                    "\u010ct", // abb Thursday
                    "P\u00e1", // abb Friday
                    "So" // abb Saturday
                }
            },
            { "standalone.DayAbbreviations",
                new String[] {
                    "ne",
                    "po",
                    "\u00fat",
                    "st",
                    "\u010dt",
                    "p\u00e1",
                    "so",
                }
            },
            { "DayNarrows",
                new String[] {
                    "N",
                    "P",
                    "\u00da",
                    "S",
                    "\u010c",
                    "P",
                    "S",
                }
            },
            { "standalone.DayNarrows",
                new String[] {
                    "N",
                    "P",
                    "\u00da",
                    "S",
                    "\u010c",
                    "P",
                    "S",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "dop.", // am marker
                    "odp." // pm marker
                }
            },
            { "Eras",
                new String[] { // era strings
                    "p\u0159.Kr.",
                    "po Kr."
                }
            },
            { "short.Eras",
                new String[] {
                    "p\u0159. n. l.",
                    "n. l.",
                }
            },
            { "narrow.Eras",
                new String[] {
                    "p\u0159.n.l.",
                    "n. l.",
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
                    "EEEE, d. MMMM yyyy", // full date pattern
                    "d. MMMM yyyy", // long date pattern
                    "d.M.yyyy", // medium date pattern
                    "d.M.yy", // short date pattern
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" // date-time pattern
                }
            },
            { "DateTimePatternChars", "GuMtkHmsSEDFwWahKzZ" },
        };
    }
}
