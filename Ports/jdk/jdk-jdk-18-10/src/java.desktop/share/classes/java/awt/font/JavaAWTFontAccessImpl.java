/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.font;

import jdk.internal.access.JavaAWTFontAccess;

class JavaAWTFontAccessImpl implements JavaAWTFontAccess {

    // java.awt.font.TextAttribute constants
    public Object getTextAttributeConstant(String name) {
        switch (name) {
            case "RUN_DIRECTION":
                return TextAttribute.RUN_DIRECTION;
            case "NUMERIC_SHAPING":
                return TextAttribute.NUMERIC_SHAPING;
            case "BIDI_EMBEDDING":
                return TextAttribute.BIDI_EMBEDDING;
            case "RUN_DIRECTION_LTR":
                return TextAttribute.RUN_DIRECTION_LTR;
            default:
                throw new AssertionError("Constant name is not recognized");
        }
    }

    // java.awt.font.NumericShaper
    public void shape(Object shaper, char[] text, int start, int count) {
        assert shaper instanceof NumericShaper;
        ((NumericShaper)shaper).shape(text, start,count);
    }

}
