/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;

/*
 * @test
 * @key headful
 * @bug 8033069
 * @summary Checks that JComboBox popup does not close when mouse wheel is
 *          rotated over combo box and over its popup. The case where
 *          popup has scroll bar.
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug8033069ScrollBar
 * @author Alexey Ivanov
 */
public class bug8033069ScrollBar extends bug8033069NoScrollBar {

    private static final String[] SCROLL_ITEMS = new String[] {
            "A", "B", "C", "D", "E", "F",
            "G", "H", "I", "J", "K", "L",
            "M", "N", "O", "P", "Q", "R"
    };

    public static void main(String[] args) throws Exception {
        iterateLookAndFeels(new bug8033069ScrollBar(SCROLL_ITEMS));
    }

    public bug8033069ScrollBar(String[] items) throws AWTException {
        super(items);
    }

}
