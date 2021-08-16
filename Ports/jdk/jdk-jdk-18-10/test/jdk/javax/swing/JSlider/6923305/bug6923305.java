/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6923305
 * @summary SynthSliderUI paints the slider track when the slider's "paintTrack" property is set to false
 * @author Pavel Porvatov
 * @run main bug6923305
 */

import javax.swing.*;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthSliderUI;
import java.awt.*;
import java.awt.image.BufferedImage;

public class bug6923305 {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new SynthLookAndFeel());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JSlider slider = new JSlider();

                slider.setUI(new SynthSliderUI(slider) {
                    @Override
                    protected void paintTrack(SynthContext context, Graphics g, Rectangle trackBounds) {
                        throw new RuntimeException("Test failed: the SynthSliderUI.paintTrack was invoked");
                    }
                });

                slider.setPaintTrack(false);
                slider.setSize(slider.getPreferredSize());

                BufferedImage bufferedImage = new BufferedImage(slider.getWidth(), slider.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);

                slider.paint(bufferedImage.getGraphics());
            }
        });
    }
}
