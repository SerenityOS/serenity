/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6657026 7077259
 * @summary Tests shared MetalSliderUI in different application contexts
 * @author Sergey Malenkov
 * @modules java.desktop/sun.awt
 * @run main/othervm -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel Test6657026
 */

import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalSliderUI;
import sun.awt.SunToolkit;

public class Test6657026 extends MetalSliderUI implements Runnable {

    public static void main(String[] args) throws Exception {
        JSlider slider = new JSlider();
        test(slider);

        ThreadGroup group = new ThreadGroup("$$$");
        Thread thread = new Thread(group, new Test6657026());
        thread.start();
        thread.join();

        test(slider);
    }

    public void run() {
        SunToolkit.createNewAppContext();
        JSlider slider = new JSlider();
        test(slider);
        tickLength = -10000;
    }

    private static void test(JSlider slider) {
        MetalSliderUI ui = (MetalSliderUI) slider.getUI();
        int actual = ui.getTickLength();
        if (actual != 11) {
            throw new Error(actual + ", but expected 11");
        }
    }
}
