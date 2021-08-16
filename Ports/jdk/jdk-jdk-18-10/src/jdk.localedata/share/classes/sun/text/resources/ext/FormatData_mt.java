/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

public class FormatData_mt extends ParallelListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "Jannar",
                    "Frar",
                    "Marzu",
                    "April",
                    "Mejju",
                    "\u0120unju",
                    "Lulju",
                    "Awwissu",
                    "Settembru",
                    "Ottubru",
                    "Novembru",
                    "Di\u010bembru",
                    "",
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "Jan",
                    "Fra",
                    "Mar",
                    "Apr",
                    "Mej",
                    "\u0120un",
                    "Lul",
                    "Aww",
                    "Set",
                    "Ott",
                    "Nov",
                    "Di\u010b",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "J",
                    "F",
                    "M",
                    "A",
                    "M",
                    "\u0120",
                    "L",
                    "A",
                    "S",
                    "O",
                    "N",
                    "D",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "Il-\u0126add",
                    "It-Tnejn",
                    "It-Tlieta",
                    "L-Erbg\u0127a",
                    "Il-\u0126amis",
                    "Il-\u0120img\u0127a",
                    "Is-Sibt",
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u0126ad",
                    "Tne",
                    "Tli",
                    "Erb",
                    "\u0126am",
                    "\u0120im",
                    "Sib",
                }
            },
            { "DayNarrows",
                new String[] {
                    "\u0126",
                    "T",
                    "T",
                    "E",
                    "\u0126",
                    "\u0120",
                    "S",
                }
            },
            { "AmPmMarkers",
                new String[] {
                    "QN",
                    "WN",
                }
            },
            { "Eras",
                new String[] {
                    "QK",
                    "WK",
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
                    "EEEE, d 'ta\u2019' MMMM yyyy",
                    "d 'ta\u2019' MMMM yyyy",
                    "dd MMM yyyy",
                    "dd/MM/yyyy",
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}",
                }
            },
        };
    }
}
