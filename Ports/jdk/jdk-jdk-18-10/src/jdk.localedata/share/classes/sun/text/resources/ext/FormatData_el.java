/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

public class FormatData_el extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    @Override
    protected final Object[][] getContents() {
        final String[] rocEras = {
            "\u03a0\u03c1\u03b9\u03bd R.O.C.",
            "R.O.C.",
        };
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "\u0399\u03b1\u03bd\u03bf\u03c5\u03b1\u03c1\u03af\u03bf\u03c5",
                    "\u03a6\u03b5\u03b2\u03c1\u03bf\u03c5\u03b1\u03c1\u03af\u03bf\u03c5",
                    "\u039c\u03b1\u03c1\u03c4\u03af\u03bf\u03c5",
                    "\u0391\u03c0\u03c1\u03b9\u03bb\u03af\u03bf\u03c5",
                    "\u039c\u03b1\u0390\u03bf\u03c5",
                    "\u0399\u03bf\u03c5\u03bd\u03af\u03bf\u03c5",
                    "\u0399\u03bf\u03c5\u03bb\u03af\u03bf\u03c5",
                    "\u0391\u03c5\u03b3\u03bf\u03cd\u03c3\u03c4\u03bf\u03c5",
                    "\u03a3\u03b5\u03c0\u03c4\u03b5\u03bc\u03b2\u03c1\u03af\u03bf\u03c5",
                    "\u039f\u03ba\u03c4\u03c9\u03b2\u03c1\u03af\u03bf\u03c5",
                    "\u039d\u03bf\u03b5\u03bc\u03b2\u03c1\u03af\u03bf\u03c5",
                    "\u0394\u03b5\u03ba\u03b5\u03bc\u03b2\u03c1\u03af\u03bf\u03c5",
                    "",
                }
            },
            { "standalone.MonthNames",
                new String[] {
                    "\u0399\u03b1\u03bd\u03bf\u03c5\u03ac\u03c1\u03b9\u03bf\u03c2", // january
                    "\u03a6\u03b5\u03b2\u03c1\u03bf\u03c5\u03ac\u03c1\u03b9\u03bf\u03c2", // february
                    "\u039c\u03ac\u03c1\u03c4\u03b9\u03bf\u03c2", // march
                    "\u0391\u03c0\u03c1\u03af\u03bb\u03b9\u03bf\u03c2", // april
                    "\u039c\u03ac\u03ca\u03bf\u03c2", // may
                    "\u0399\u03bf\u03cd\u03bd\u03b9\u03bf\u03c2", // june
                    "\u0399\u03bf\u03cd\u03bb\u03b9\u03bf\u03c2", // july
                    "\u0391\u03cd\u03b3\u03bf\u03c5\u03c3\u03c4\u03bf\u03c2", // august
                    "\u03a3\u03b5\u03c0\u03c4\u03ad\u03bc\u03b2\u03c1\u03b9\u03bf\u03c2", // september
                    "\u039f\u03ba\u03c4\u03ce\u03b2\u03c1\u03b9\u03bf\u03c2", // october
                    "\u039d\u03bf\u03ad\u03bc\u03b2\u03c1\u03b9\u03bf\u03c2", // november
                    "\u0394\u03b5\u03ba\u03ad\u03bc\u03b2\u03c1\u03b9\u03bf\u03c2", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "\u0399\u03b1\u03bd", // abb january
                    "\u03a6\u03b5\u03b2", // abb february
                    "\u039c\u03b1\u03c1", // abb march
                    "\u0391\u03c0\u03c1", // abb april
                    "\u039c\u03b1\u03ca", // abb may
                    "\u0399\u03bf\u03c5\u03bd", // abb june
                    "\u0399\u03bf\u03c5\u03bb", // abb july
                    "\u0391\u03c5\u03b3", // abb august
                    "\u03a3\u03b5\u03c0", // abb september
                    "\u039f\u03ba\u03c4", // abb october
                    "\u039d\u03bf\u03b5", // abb november
                    "\u0394\u03b5\u03ba", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "standalone.MonthAbbreviations",
                new String[] {
                    "\u0399\u03b1\u03bd",
                    "\u03a6\u03b5\u03b2",
                    "\u039c\u03ac\u03c1",
                    "\u0391\u03c0\u03c1",
                    "\u039c\u03ac\u03b9",
                    "\u0399\u03bf\u03cd\u03bd",
                    "\u0399\u03bf\u03cd\u03bb",
                    "\u0391\u03c5\u03b3",
                    "\u03a3\u03b5\u03c0",
                    "\u039f\u03ba\u03c4",
                    "\u039d\u03bf\u03ad",
                    "\u0394\u03b5\u03ba",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "\u0399",
                    "\u03a6",
                    "\u039c",
                    "\u0391",
                    "\u039c",
                    "\u0399",
                    "\u0399",
                    "\u0391",
                    "\u03a3",
                    "\u039f",
                    "\u039d",
                    "\u0394",
                    "",
                }
            },
            { "standalone.MonthNarrows",
                new String[] {
                    "\u0399",
                    "\u03a6",
                    "\u039c",
                    "\u0391",
                    "\u039c",
                    "\u0399",
                    "\u0399",
                    "\u0391",
                    "\u03a3",
                    "\u039f",
                    "\u039d",
                    "\u0394",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "\u039a\u03c5\u03c1\u03b9\u03b1\u03ba\u03ae", // Sunday
                    "\u0394\u03b5\u03c5\u03c4\u03ad\u03c1\u03b1", // Monday
                    "\u03a4\u03c1\u03af\u03c4\u03b7", // Tuesday
                    "\u03a4\u03b5\u03c4\u03ac\u03c1\u03c4\u03b7", // Wednesday
                    "\u03a0\u03ad\u03bc\u03c0\u03c4\u03b7", // Thursday
                    "\u03a0\u03b1\u03c1\u03b1\u03c3\u03ba\u03b5\u03c5\u03ae", // Friday
                    "\u03a3\u03ac\u03b2\u03b2\u03b1\u03c4\u03bf" // Saturday
                }
            },
            { "standalone.DayNames",
                new String[] {
                    "\u039a\u03c5\u03c1\u03b9\u03b1\u03ba\u03ae",
                    "\u0394\u03b5\u03c5\u03c4\u03ad\u03c1\u03b1",
                    "\u03a4\u03c1\u03af\u03c4\u03b7",
                    "\u03a4\u03b5\u03c4\u03ac\u03c1\u03c4\u03b7",
                    "\u03a0\u03ad\u03bc\u03c0\u03c4\u03b7",
                    "\u03a0\u03b1\u03c1\u03b1\u03c3\u03ba\u03b5\u03c5\u03ae",
                    "\u03a3\u03ac\u03b2\u03b2\u03b1\u03c4\u03bf",
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u039a\u03c5\u03c1", // abb Sunday
                    "\u0394\u03b5\u03c5", // abb Monday
                    "\u03a4\u03c1\u03b9", // abb Tuesday
                    "\u03a4\u03b5\u03c4", // abb Wednesday
                    "\u03a0\u03b5\u03bc", // abb Thursday
                    "\u03a0\u03b1\u03c1", // abb Friday
                    "\u03a3\u03b1\u03b2" // abb Saturday
                }
            },
            { "standalone.DayAbbreviations",
                new String[] {
                    "\u039a\u03c5\u03c1",
                    "\u0394\u03b5\u03c5",
                    "\u03a4\u03c1\u03af",
                    "\u03a4\u03b5\u03c4",
                    "\u03a0\u03ad\u03bc",
                    "\u03a0\u03b1\u03c1",
                    "\u03a3\u03ac\u03b2",
                }
            },
            { "DayNarrows",
                new String[] {
                    "\u039a",
                    "\u0394",
                    "\u03a4",
                    "\u03a4",
                    "\u03a0",
                    "\u03a0",
                    "\u03a3",
                }
            },
            { "standalone.DayNarrows",
                new String[] {
                    "\u039a",
                    "\u0394",
                    "\u03a4",
                    "\u03a4",
                    "\u03a0",
                    "\u03a0",
                    "\u03a3",
                }
            },
            { "short.Eras",
                new String[] {
                    "\u03c0.\u03a7.",
                    "\u03bc.\u03a7.",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "\u03c0\u03bc", // am marker
                    "\u03bc\u03bc" // pm marker
                }
            },
            { "NumberElements",
                new String[] {
                    ",", // decimal separator
                    ".", // group (thousands) separator
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
                    "h:mm:ss a z", // full time pattern
                    "h:mm:ss a z", // long time pattern
                    "h:mm:ss a", // medium time pattern
                    "h:mm a", // short time pattern
                }
            },
            { "DatePatterns",
                new String[] {
                    "EEEE, d MMMM yyyy", // full date pattern
                    "d MMMM yyyy", // long date pattern
                    "d MMM yyyy", // medium date pattern
                    "d/M/yyyy", // short date pattern
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}" // date-time pattern
                }
            },
            { "DateTimePatternChars", "GanjkHmsSEDFwWxhKzZ" },
        };
    }
}
