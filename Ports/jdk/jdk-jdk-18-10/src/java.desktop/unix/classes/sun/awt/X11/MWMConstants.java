/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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


package sun.awt.X11;

public final class MWMConstants {

    private MWMConstants(){}

/* bit definitions for MwmHints.flags */
    static final int MWM_HINTS_FUNCTIONS=       (1  << 0);
    static final int MWM_HINTS_DECORATIONS=     (1  << 1);
    static final int MWM_HINTS_INPUT_MODE=      (1  << 2);
    static final int MWM_HINTS_STATUS=          (1  << 3);

/* bit definitions for MwmHints.functions */
    static final int MWM_FUNC_ALL=              (1  << 0);
    static final int MWM_FUNC_RESIZE=           (1  << 1);
    static final int MWM_FUNC_MOVE=             (1  << 2);
    static final int MWM_FUNC_MINIMIZE=         (1  << 3);
    static final int MWM_FUNC_MAXIMIZE=         (1  << 4);
    static final int MWM_FUNC_CLOSE=            (1  << 5);

/* bit definitions for MwmHints.decorations */
    static final int MWM_DECOR_ALL=             (1  << 0);
    static final int MWM_DECOR_BORDER=          (1  << 1);
    static final int MWM_DECOR_RESIZEH=         (1  << 2);
    static final int MWM_DECOR_TITLE  =         (1  << 3);
    static final int MWM_DECOR_MENU     =       (1  << 4);
    static final int MWM_DECOR_MINIMIZE=        (1  << 5);
    static final int MWM_DECOR_MAXIMIZE=        (1  << 6);

    // Input modes
    static final int MWM_INPUT_MODELESS                 =0;
    static final int MWM_INPUT_PRIMARY_APPLICATION_MODAL=1;
    static final int MWM_INPUT_SYSTEM_MODAL             =2;
    static final int MWM_INPUT_FULL_APPLICATION_MODAL   =3;

/* number of elements of size 32 in _MWM_HINTS */
    static final int PROP_MWM_HINTS_ELEMENTS          = 5;
/* number of elements of size 32 in _MWM_INFO */
    static final int PROP_MOTIF_WM_INFO_ELEMENTS=       2;
    static final int PROP_MWM_INFO_ELEMENTS=            PROP_MOTIF_WM_INFO_ELEMENTS;

    static final String MWM_HINTS_ATOM_NAME = "_MOTIF_WM_HINTS";
}
