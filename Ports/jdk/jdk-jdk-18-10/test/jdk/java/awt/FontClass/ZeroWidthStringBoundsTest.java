/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8245159 8239725
 * @summary Ensure no exception getting bounds of an empty string when the font has
 * layout attributes.
 * @run main/othervm ZeroWidthStringBoundsTest
 */

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;

public class ZeroWidthStringBoundsTest {

   public static void main(String[] args) {
       FontRenderContext frc = new FontRenderContext(null, false, false);
       Font f1 = new Font(Font.MONOSPACED, Font.PLAIN, 12);
       f1.getStringBounds("", frc);
       HashMap<AttributedCharacterIterator.Attribute, Object> attrs = new HashMap<>();
       attrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
       Font f2 = f1.deriveFont(attrs);
       f2.getStringBounds("", frc);
   }
}
