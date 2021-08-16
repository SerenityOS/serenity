/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8149631
 * @summary rgb(...) CSS color values are not parsed properly
 * @run main RGBColorValueTest
 */

import javax.swing.text.AttributeSet;
import javax.swing.text.html.StyleSheet;

import static javax.swing.text.html.CSS.Attribute.*;

public class RGBColorValueTest {

    public static void main(String[] args) {
        StyleSheet styleSheet = new StyleSheet();
        AttributeSet attributeSet = styleSheet.
             getDeclaration("border-color: rgb(1, 2, 3)    rgb(1, 2, 4);");
        if (!attributeSet.getAttribute(BORDER_TOP_COLOR).toString()
                                                  .equals("rgb(1, 2, 3)") ||
            !attributeSet.getAttribute(BORDER_BOTTOM_COLOR).toString()
                                                  .equals("rgb(1, 2, 3)") ||
            !attributeSet.getAttribute(BORDER_RIGHT_COLOR).toString()
                                                  .equals("rgb(1, 2, 4)") ||
            !attributeSet.getAttribute(BORDER_LEFT_COLOR).toString()
                                                  .equals("rgb(1, 2, 4)") ) {
            throw new RuntimeException("Failed");
        }
    }
}
