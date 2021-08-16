/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015334
 * @summary Memory leak with kerning.
 */

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class KerningLeak {

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                leak();
            }
        });
    }

    private static void leak() {
        Map<TextAttribute, Object> textAttributes = new HashMap<>();
        textAttributes.put(TextAttribute.FAMILY, "Sans Serif");
        textAttributes.put(TextAttribute.SIZE, 12);
        textAttributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        Font font = Font.getFont(textAttributes);
        JLabel label = new JLabel();
        int dummy = 0;
        for (int i = 0; i < 500; i++) {
            if (i % 10 == 0) System.out.println("Starting iter " + (i+1));
            for (int j = 0; j <1000; j++) {
                FontMetrics fm = label.getFontMetrics(font);
                dummy += SwingUtilities.computeStringWidth(fm, Integer.toString(j));
            }
        }
        System.out.println("done " + dummy);
    }
}
