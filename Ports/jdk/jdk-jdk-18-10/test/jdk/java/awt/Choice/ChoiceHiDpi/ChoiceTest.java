/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8144594
 * @summary HiDPI: awt.Choice looks improperly (Win 8)
 * @run main ChoiceTest
 */
import java.awt.Frame;
import java.awt.Choice;
import java.awt.Font;
import java.util.stream.Stream;

public class ChoiceTest {

    private static void UI() {
        Frame frame = new Frame("Test frame");
        Choice choice = new Choice();

        Stream.of(new String[]{"item 1", "item 2", "item 3"}).forEach(choice::add);
        frame.add(choice);
        frame.setBounds(100, 100, 400, 200);

        frame.setVisible(true);
        Font font = choice.getFont();
        int size = font.getSize();
        int height = choice.getBounds().height;
        try {
            if (height < size) {
                throw new RuntimeException("Test failed");
            }
        } finally {
            frame.dispose();
        }
    }

    public static void main(String[] args) throws Exception {
        ChoiceTest.UI();
    }
}

