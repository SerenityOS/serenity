/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6794836
 * @summary BasicSliderUI throws NullPointerExc when JSlider maximum is Integer.MAX_VALUE
 * @author Pavel Porvatov
 * @modules java.desktop/javax.swing.plaf.basic:open
 * @run main bug6794836
 */

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.lang.reflect.Method;
import java.util.Hashtable;

public class bug6794836 {
    public static void main(String[] args) throws Exception {
        new bug6794836().run();
    }

    public void run() throws Exception {
        JSlider slider = new JSlider(0, Integer.MAX_VALUE);

        slider.setPaintLabels(true);

        JLabel minLabel = new JLabel("Min");
        JLabel maxLabel = new JLabel("Max");

        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();

        labelTable.put(Integer.MIN_VALUE, minLabel);
        labelTable.put(Integer.MAX_VALUE, maxLabel);

        slider.setLabelTable(labelTable);

        BasicSliderUI ui = (BasicSliderUI) slider.getUI();

        if (invokeMethod("getHighestValueLabel", ui) != maxLabel) {
            fail("invalid getHighestValueLabel result");
        }

        if (invokeMethod("getLowestValueLabel", ui) != minLabel) {
            fail("invalid getLowestValueLabel result");
        }

        System.out.println("The bug6794836 test passed");
    }

    private static Object invokeMethod(String name, BasicSliderUI ui) throws Exception {
        Method method = BasicSliderUI.class.getDeclaredMethod(name, null);

        method.setAccessible(true);

        return method.invoke(ui, null);
    }

    private static void fail(String s) {
        throw new RuntimeException("Test failed: " + s);
    }
}
