/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
 * @summary Test getGlyphCharIndex() results from layout
 * @bug 8152680
 */

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

public class GetGlyphCharIndexTest {
    public static void main(String[] args) {
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        FontRenderContext frc = new FontRenderContext(null, false, false);
        GlyphVector gv = font.layoutGlyphVector(frc, "abc".toCharArray(), 1, 3,
                                                Font.LAYOUT_LEFT_TO_RIGHT);
        int idx0 = gv.getGlyphCharIndex(0);
        if (idx0 != 0) {
           throw new RuntimeException("Expected 0, got " + idx0);
        }
    }
}
