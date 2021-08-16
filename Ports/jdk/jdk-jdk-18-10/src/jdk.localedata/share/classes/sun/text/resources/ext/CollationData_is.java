/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.text.resources.ext;

import java.util.ListResourceBundle;

public class CollationData_is extends ListResourceBundle {

    protected final Object[][] getContents() {
        return new Object[][] {
            { "Rule",
                /* for is, accents sorted backwards plus the following: */

                "@"                                           /* sort accents bkwd */
                /* assuming that in the default collation we add:                   */
                /*  thorn, ae ligature, o-diaeresis, and o-slash                    */
                /*  ....in this order...and ditto for the uppercase of these....    */
                /* to be treated as characters (not accented characters) after z    */
                /* then we don't have to add anything here. I've just added it here */
                /* just in case it gets overlooked.                                 */
                + "& A < a\u0301, A\u0301 "       // nt : A < a-acute
                + "& D < \u00f0, \u00d0"          // nt : d < eth
                + "& E < e\u0301, E\u0301 "       // nt : e < e-acute
                + "& I < i\u0301, I\u0301 "       // nt : i < i-acute
                + "& O < o\u0301, O\u0301 "       // nt : o < o-acute
                + "& U < u\u0301, U\u0301 "       // nt : u < u-acute
                + "& Y < y\u0301, Y\u0301 "       // nt : y < y-acute
                + "& Z < \u00fe, \u00de < \u00e6, \u00c6" // nt : z < thron < a-e-ligature
                + "< o\u0308, O\u0308 ; \u00f8, \u00d8" // nt : o-umlaut ; o-stroke
            }
        };
    }
}
