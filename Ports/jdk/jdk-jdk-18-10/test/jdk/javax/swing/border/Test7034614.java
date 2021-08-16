/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7034614
 * @summary Tests that TitledBorder does not modify Insets
 * @author Sergey Malenkov
 */

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class Test7034614 {

    public static void main(String[] args) {
        Graphics g = new BufferedImage(9, 9, 9).getGraphics();

        BrokenBorder broken = new BrokenBorder();
        TitledBorder titled = new TitledBorder(broken, broken.getClass().getName());

        Insets insets = (Insets) broken.getBorderInsets(broken).clone();
        titled.getBorderInsets(broken);
        broken.validate(insets);
        for (int i = 0; i < 10; i++) {
            titled.paintBorder(broken, g, 0, 0, i, i);
            broken.validate(insets);
            titled.getBaseline(broken, i, i);
            broken.validate(insets);
        }
    }

    private static class BrokenBorder extends Component implements Border {
        private Insets insets = new Insets(1, 2, 3, 4);

        private void validate(Insets insets) {
            if (!this.insets.equals(insets)) {
                throw new Error("unexpected change");
            }
        }

        public Insets getBorderInsets(Component c) {
            return this.insets;
        }

        public boolean isBorderOpaque() {
            return false;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        }
    }
}
