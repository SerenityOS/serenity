/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

public class FormatData_sr extends ParallelListResourceBundle {
    @Override
    protected final Object[][] getContents() {
        final String[] rocEras = {
            "\u041f\u0440\u0435 \u0420\u041a",
            "\u0420\u041a",
        };
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "\u0458\u0430\u043d\u0443\u0430\u0440",
                    "\u0444\u0435\u0431\u0440\u0443\u0430\u0440",
                    "\u043c\u0430\u0440\u0442",
                    "\u0430\u043f\u0440\u0438\u043b",
                    "\u043c\u0430\u0458",
                    "\u0458\u0443\u043d",
                    "\u0458\u0443\u043b",
                    "\u0430\u0432\u0433\u0443\u0441\u0442",
                    "\u0441\u0435\u043f\u0442\u0435\u043c\u0431\u0430\u0440",
                    "\u043e\u043a\u0442\u043e\u0431\u0430\u0440",
                    "\u043d\u043e\u0432\u0435\u043c\u0431\u0430\u0440",
                    "\u0434\u0435\u0446\u0435\u043c\u0431\u0430\u0440",
                    "",
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "\u0458\u0430\u043d",
                    "\u0444\u0435\u0431",
                    "\u043c\u0430\u0440",
                    "\u0430\u043f\u0440",
                    "\u043c\u0430\u0458",
                    "\u0458\u0443\u043d",
                    "\u0458\u0443\u043b",
                    "\u0430\u0432\u0433",
                    "\u0441\u0435\u043f",
                    "\u043e\u043a\u0442",
                    "\u043d\u043e\u0432",
                    "\u0434\u0435\u0446",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "\u0458",
                    "\u0444",
                    "\u043c",
                    "\u0430",
                    "\u043c",
                    "\u0458",
                    "\u0458",
                    "\u0430",
                    "\u0441",
                    "\u043e",
                    "\u043d",
                    "\u0434",
                    "",
                }
            },
            { "MonthNarrows",
                new String[] {
                    "\u0458",
                    "\u0444",
                    "\u043c",
                    "\u0430",
                    "\u043c",
                    "\u0458",
                    "\u0458",
                    "\u0430",
                    "\u0441",
                    "\u043e",
                    "\u043d",
                    "\u0434",
                    "",
                }
            },
            { "DayNames",
                new String[] {
                    "\u043d\u0435\u0434\u0435\u0459\u0430",
                    "\u043f\u043e\u043d\u0435\u0434\u0435\u0459\u0430\u043a",
                    "\u0443\u0442\u043e\u0440\u0430\u043a",
                    "\u0441\u0440\u0435\u0434\u0430",
                    "\u0447\u0435\u0442\u0432\u0440\u0442\u0430\u043a",
                    "\u043f\u0435\u0442\u0430\u043a",
                    "\u0441\u0443\u0431\u043e\u0442\u0430",
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u043d\u0435\u0434",
                    "\u043f\u043e\u043d",
                    "\u0443\u0442\u043e",
                    "\u0441\u0440\u0435",
                    "\u0447\u0435\u0442",
                    "\u043f\u0435\u0442",
                    "\u0441\u0443\u0431",
                }
            },
            { "DayNarrows",
                new String[] {
                    "\u043d",
                    "\u043f",
                    "\u0443",
                    "\u0441",
                    "\u0447",
                    "\u043f",
                    "\u0441",
                }
            },
            { "Eras",
                new String[] {
                    "\u043f. \u043d. \u0435.",
                    "\u043d. \u0435",
                }
            },
            { "short.Eras",
                new String[] {
                    "\u043f. \u043d. \u0435.",
                    "\u043d. \u0435.",
                }
            },
            { "narrow.Eras",
                new String[] {
                    "\u043f.\u043d.\u0435.",
                    "\u043d.\u0435.",
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
                    ",",
                    ".",
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
                    "HH.mm.ss z",
                    "HH.mm.ss z",
                    "HH.mm.ss",
                    "HH.mm",
                }
            },
            { "DatePatterns",
                new String[] {
                    "EEEE, dd.MMMM.yyyy.",
                    "dd.MM.yyyy.",
                    "dd.MM.yyyy.",
                    "d.M.yy.",
                }
            },
            { "DateTimePatterns",
                new String[] {
                    "{1} {0}",
                }
            },
            { "DateTimePatternChars", "GanjkHmsSEDFwWxhKzZ" },
        };
    }
}
