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
 * @bug 6918861
 * @summary SynthSliderUI.uninstallDefaults() is not called when UI is uninstalled
 * @author Pavel Porvatov
 * @run main bug6918861
 */

import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthSliderUI;

public class bug6918861 {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new SynthLookAndFeel());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JSlider slider = new JSlider();

                HackedSynthSliderUI ui = new HackedSynthSliderUI(slider);

                slider.setUI(ui);

                if (ui.counter != 111) {
                    throw new RuntimeException("Some installers of SynthSliderUI weren't invoked");
                }

                slider.setUI(null);

                if (ui.counter != 0) {
                    throw new RuntimeException("Some uninstallers of SynthSliderUI weren't invoked");
                }
            }
        });
    }

    private static class HackedSynthSliderUI extends SynthSliderUI {
        private int counter;

        protected HackedSynthSliderUI(JSlider c) {
            super(c);
        }

        protected void installDefaults(JSlider slider) {
            super.installDefaults(slider);

            counter += 1;
        }

        protected void uninstallDefaults(JSlider slider) {
            super.uninstallDefaults(slider);

            counter -= 1;
        }

        protected void installListeners(JSlider slider) {
            super.installListeners(slider);

            counter += 10;
        }

        protected void uninstallListeners(JSlider slider) {
            super.uninstallListeners(slider);

            counter -= 10;
        }

        protected void installKeyboardActions(JSlider slider) {
            super.installKeyboardActions(slider);

            counter += 100;
        }

        protected void uninstallKeyboardActions(JSlider slider) {
            super.uninstallKeyboardActions(slider);

            counter -= 100;
        }
    }
}
