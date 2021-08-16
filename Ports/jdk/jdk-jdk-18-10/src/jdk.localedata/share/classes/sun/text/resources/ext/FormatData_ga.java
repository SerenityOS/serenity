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

/*
 * COPYRIGHT AND PERMISSION NOTICE
 *
 * Copyright (C) 1991-2007 Unicode, Inc. All rights reserved.
 * Distributed under the Terms of Use in http://www.unicode.org/copyright.html.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of the Unicode data files and any associated documentation (the "Data
 * Files") or Unicode software and any associated documentation (the
 * "Software") to deal in the Data Files or Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, and/or sell copies of the Data Files or Software, and
 * to permit persons to whom the Data Files or Software are furnished to do
 * so, provided that (a) the above copyright notice(s) and this permission
 * notice appear with all copies of the Data Files or Software, (b) both the
 * above copyright notice(s) and this permission notice appear in associated
 * documentation, and (c) there is clear notice in each modified Data File or
 * in the Software as well as in the documentation associated with the Data
 * File(s) or Software that the data or software has been modified.
 *
 * THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
 * THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS
 * INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE DATA FILES OR SOFTWARE.
 *
 * Except as contained in this notice, the name of a copyright holder shall not
 * be used in advertising or otherwise to promote the sale, use or other
 * dealings in these Data Files or Software without prior written
 * authorization of the copyright holder.
 */

package sun.text.resources.ext;

import sun.util.resources.ParallelListResourceBundle;

public class FormatData_ga extends ParallelListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "Ean\u00e1ir",
                    "Feabhra",
                    "M\u00e1rta",
                    "Aibre\u00e1n",
                    "Bealtaine",
                    "Meitheamh",
                    "I\u00fail",
                    "L\u00fanasa",
                    "Me\u00e1n F\u00f3mhair",
                    "Deireadh F\u00f3mhair",
                    "Samhain",
                    "Nollaig",
                    "",
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "Ean",
                    "Feabh",
                    "M\u00e1rta",
                    "Aib",
                    "Beal",
                    "Meith",
                    "I\u00fail",
                    "L\u00fan",
                    "MF\u00f3mh",
                    "DF\u00f3mh",
                    "Samh",
                    "Noll",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "E",
                    "F",
                    "M",
                    "A",
                    "B",
                    "M",
                    "I",
                    "L",
                    "M",
                    "D",
                    "S",
                    "N",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "D\u00e9 Domhnaigh",
                    "D\u00e9 Luain",
                    "D\u00e9 M\u00e1irt",
                    "D\u00e9 C\u00e9adaoin",
                    "D\u00e9ardaoin",
                    "D\u00e9 hAoine",
                    "D\u00e9 Sathairn",
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "Domh",
                    "Luan",
                    "M\u00e1irt",
                    "C\u00e9ad",
                    "D\u00e9ar",
                    "Aoine",
                    "Sath",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "a.m.",
                    "p.m.",
                }
            },
            { "Eras",
                new String[] {
                    "RC",
                    "AD",
                }
            },
            { "short.Eras",
                new String[] {
                    "RC",
                    "AD",
                }
            },
            { "NumberPatterns",
                new String[] {
                    "#,##0.###",
                    "\u00a4 #,##0.00",
                    "#,##0%",
                }
            },
            { "NumberElements",
                new String[] {
                    ".",
                    ",",
                    ";",
                    "%",
                    "0",
                    "#",
                    "-",
                    "E",
                    "\u2030",
                    "\u221e",
                    "NaN",
                }
            },
            { "TimePatterns",
                new String[] {
                    "HH:mm:ss z",
                    "HH:mm:ss z",
                    "HH:mm:ss",
                    "HH:mm",
                }
            },
            { "DatePatterns",
                new String[] {
                    "EEEE, yyyy MMMM dd",
                    "yyyy MMMM d",
                    "yyyy MMM d",
                    "yy/MM/dd",
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}",
                }
            },
            { "DateTimePatternChars", "RbMLkUnsSElFtTauKcZ" },
        };
    }
}
