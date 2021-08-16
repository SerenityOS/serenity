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

public class FormatData_ar extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    @Override
    protected final Object[][] getContents() {
        final String[] rocEras = {
            "Before R.O.C.",
            "\u062c\u0645\u0647\u0648\u0631\u064a\u0629 \u0627\u0644\u0635\u064a",
        };
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "\u064a\u0646\u0627\u064a\u0631", // january
                    "\u0641\u0628\u0631\u0627\u064a\u0631", // february
                    "\u0645\u0627\u0631\u0633", // march
                    "\u0623\u0628\u0631\u064a\u0644", // april
                    "\u0645\u0627\u064a\u0648", // may
                    "\u064a\u0648\u0646\u064a\u0648", // june
                    "\u064a\u0648\u0644\u064a\u0648", // july
                    "\u0623\u063a\u0633\u0637\u0633", // august
                    "\u0633\u0628\u062a\u0645\u0628\u0631", // september
                    "\u0623\u0643\u062a\u0648\u0628\u0631", // october
                    "\u0646\u0648\u0641\u0645\u0628\u0631", // november
                    "\u062f\u064a\u0633\u0645\u0628\u0631", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "\u064a\u0646\u0627", // abb january
                    "\u0641\u0628\u0631", // abb february
                    "\u0645\u0627\u0631", // abb march
                    "\u0623\u0628\u0631", // abb april
                    "\u0645\u0627\u064a", // abb may
                    "\u064a\u0648\u0646", // abb june
                    "\u064a\u0648\u0644", // abb july
                    "\u0623\u063a\u0633", // abb august
                    "\u0633\u0628\u062a", // abb september
                    "\u0623\u0643\u062a", // abb october
                    "\u0646\u0648\u0641", // abb november
                    "\u062f\u064a\u0633", // abb december
                    "" // abb month 13 if applicable
                }
            },
            { "MonthNarrows",
                new String[] {
                    "\u064a",
                    "\u0641",
                    "\u0645",
                    "\u0623",
                    "\u0648",
                    "\u0646",
                    "\u0644",
                    "\u063a",
                    "\u0633",
                    "\u0643",
                    "\u0628",
                    "\u062f",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "\u0627\u0644\u0623\u062d\u062f", // Sunday
                    "\u0627\u0644\u0627\u062b\u0646\u064a\u0646", // Monday
                    "\u0627\u0644\u062b\u0644\u0627\u062b\u0627\u0621", // Tuesday
                    "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", // Wednesday
                    "\u0627\u0644\u062e\u0645\u064a\u0633", // Thursday
                    "\u0627\u0644\u062c\u0645\u0639\u0629", // Friday
                    "\u0627\u0644\u0633\u0628\u062a" // Saturday
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u062d", // abb Sunday
                    "\u0646", // abb Monday
                    "\u062b", // abb Tuesday
                    "\u0631", // abb Wednesday
                    "\u062e", // abb Thursday
                    "\u062c", // abb Friday
                    "\u0633" // abb Saturday
                }
            },
            { "standalone.DayAbbreviations",
                new String[] {
                    "\u0627\u0644\u0623\u062d\u062f",
                    "\u0627\u0644\u0627\u062b\u0646\u064a\u0646",
                    "\u0627\u0644\u062b\u0644\u0627\u062b\u0627\u0621",
                    "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621",
                    "\u0627\u0644\u062e\u0645\u064a\u0633",
                    "\u0627\u0644\u062c\u0645\u0639\u0629",
                    "\u0627\u0644\u0633\u0628\u062a",
                }
            },
            { "DayNarrows",
                new String[] {
                    "\u062d",
                    "\u0646",
                    "\u062b",
                    "\u0631",
                    "\u062e",
                    "\u062c",
                    "\u0633",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "\u0635", // am marker
                    "\u0645" // pm marker
                }
            },
            { "Eras",
                new String[] { // era strings
                    "\u0642.\u0645",
                    "\u0645"
                }
            },
            { "short.Eras",
                new String[] {
                    "\u0642.\u0645",
                    "\u0645",
                }
            },
            { "japanese.Eras",
                new String[] {
                    "\u0645",
                    "\u0645\u064a\u062c\u064a",
                    "\u062a\u064a\u0634\u0648",
                    "\u0634\u0648\u0648\u0627",
                    "\u0647\u064a\u0633\u064a",
                    "\u0631\u064a\u0648\u0627",
                }
            },
            { "japanese.short.Eras",
                new String[] {
                    "\u0645",
                    "\u0645\u064a\u062c\u064a",
                    "\u062a\u064a\u0634\u0648",
                    "\u0634\u0648\u0648\u0627",
                    "\u0647\u064a\u0633\u064a",
                    "\u0631\u064a\u0648\u0627",
                }
            },
            { "buddhist.Eras",
                new String[] {
                    "BC",
                    "\u0627\u0644\u062a\u0642\u0648\u064a\u0645 \u0627\u0644\u0628\u0648\u0630\u064a",
                }
            },
            { "buddhist.short.Eras",
                new String[] {
                    "BC",
                    "\u0627\u0644\u062a\u0642\u0648\u064a\u0645 \u0627\u0644\u0628\u0648\u0630\u064a",
                }
            },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###;#,##0.###-", // decimal pattern
                    "\u00A4 #,##0.###;\u00A4 #,##0.###-", // currency pattern
                    "#,##0%" // percent pattern
                }
            },
            { "TimePatterns",
                new String[] {
                    "z hh:mm:ss a", // full time pattern
                    "z hh:mm:ss a", // long time pattern
                    "hh:mm:ss a", // medium time pattern
                    "hh:mm a", // short time pattern
                }
            },
            { "DatePatterns",
                new String[] {
                    "dd MMMM, yyyy", // full date pattern
                    "dd MMMM, yyyy", // long date pattern
                    "dd/MM/yyyy", // medium date pattern
                    "dd/MM/yy", // short date pattern
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
