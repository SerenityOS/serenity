/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify no exception for unsupported code point.
 * @bug 8172967
 */
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

public class MissingCodePointLayoutTest {
    public static void main(String[] args) {
        Font font = new Font("Tahoma", Font.PLAIN, 12);
        String text = "\ude00";
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout layout = new TextLayout(text, font, frc);
        layout.getCaretShapes(0);
    }
}

