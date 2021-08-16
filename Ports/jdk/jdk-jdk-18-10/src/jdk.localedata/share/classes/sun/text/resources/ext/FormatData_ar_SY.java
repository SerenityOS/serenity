/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
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

package sun.text.resources.ext;

import sun.util.resources.ParallelListResourceBundle;

public class FormatData_ar_SY extends ParallelListResourceBundle {
    /**
     * Overrides ParallelListResourceBundle
     */
    protected final Object[][] getContents() {
        return new Object[][] {
            { "MonthNames",
                new String[] {
                    "\u0643\u0627\u0646\u0648\u0646\u0020\u0627\u0644\u062b\u0627\u0646\u064a", // january
                    "\u0634\u0628\u0627\u0637", // february
                    "\u0622\u0630\u0627\u0631", // march
                    "\u0646\u064a\u0633\u0627\u0646", // april
                    "\u0623\u064a\u0627\u0631", // may
                    "\u062d\u0632\u064a\u0631\u0627\u0646", // june
                    "\u062a\u0645\u0648\u0632", // july
                    "\u0622\u0628", // august
                    "\u0623\u064a\u0644\u0648\u0644", // september
                    "\u062a\u0634\u0631\u064a\u0646\u0020\u0627\u0644\u0623\u0648\u0644", // october
                    "\u062a\u0634\u0631\u064a\u0646\u0020\u0627\u0644\u062b\u0627\u0646\u064a", // november
                    "\u0643\u0627\u0646\u0648\u0646\u0020\u0627\u0644\u0623\u0648\u0644", // december
                    "" // month 13 if applicable
                }
            },
            { "MonthAbbreviations",
                new String[] {
                    "\u0643\u0627\u0646\u0648\u0646\u0020\u0627\u0644\u062b\u0627\u0646\u064a", // abb january
                    "\u0634\u0628\u0627\u0637", // abb february
                    "\u0622\u0630\u0627\u0631", // abb march
                    "\u0646\u064a\u0633\u0627\u0646", // abb april
                    "\u0623\u064a\u0627\u0631", // abb may
                    "\u062d\u0632\u064a\u0631\u0627\u0646", // abb june
                    "\u062a\u0645\u0648\u0632", // abb july
                    "\u0622\u0628", // abb august
                    "\u0623\u064a\u0644\u0648\u0644", // abb september
                    "\u062a\u0634\u0631\u064a\u0646\u0020\u0627\u0644\u0623\u0648\u0644", // abb october
                    "\u062a\u0634\u0631\u064a\u0646\u0020\u0627\u0644\u062b\u0627\u0646\u064a", // abb november
                    "\u0643\u0627\u0646\u0648\u0646\u0020\u0627\u0644\u0623\u0648\u0644", // abb december
                    "" // month 13 if applicable
                }
            },
            { "DayAbbreviations",
                new String[] {
                    "\u0627\u0644\u0623\u062d\u062f", // abb Sunday
                    "\u0627\u0644\u0627\u062b\u0646\u064a\u0646", // abb Monday
                    "\u0627\u0644\u062b\u0644\u0627\u062b\u0627\u0621", // abb Tuesday
                    "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", // abb Wednesday
                    "\u0627\u0644\u062e\u0645\u064a\u0633", // abb Thursday
                    "\u0627\u0644\u062c\u0645\u0639\u0629", // abb Friday
                    "\u0627\u0644\u0633\u0628\u062a" // abb Saturday
                }
            },
        };
    }
}
