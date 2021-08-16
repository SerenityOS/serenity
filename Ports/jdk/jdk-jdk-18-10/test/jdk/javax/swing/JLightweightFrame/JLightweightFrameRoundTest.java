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

/*
 * @test
 * @key headful
 * @bug 8170387
 * @summary JLightweightFrame#syncCopyBuffer() may throw IOOBE
 * @modules java.desktop/sun.swing
 * @run main JLightweightFrameRoundTest
 */

import sun.swing.JLightweightFrame;
import sun.swing.LightweightContent;

import javax.swing.*;

public class JLightweightFrameRoundTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JLightweightFrame jLightweightFrame = new JLightweightFrame();
            jLightweightFrame.setContent(new XLightweightContent());
            jLightweightFrame.setSize(600, 600);
            jLightweightFrame.notifyDisplayChanged(1.0001, 1.0001);
        });
    }

    static class XLightweightContent implements LightweightContent {
        @Override
        public JComponent getComponent() {
            return new JPanel();
        }

        @Override
        public void paintLock() {}

        @Override
        public void paintUnlock() {}

        @Override
        public void imageBufferReset(int[] data, int x, int y, int width,
                                     int height, int linestride,
                                     double scaleX,
                                     double scaleY) {}

        @Override
        public void imageReshaped(int x, int y, int width, int height) {}

        @Override
        public void imageUpdated(int dirtyX, int dirtyY, int dirtyWidth,
                                 int dirtyHeight) {}

        @Override
        public void focusGrabbed() {}

        @Override
        public void focusUngrabbed() {}

        @Override
        public void preferredSizeChanged(int width, int height) {}

        @Override
        public void maximumSizeChanged(int width, int height) {}

        @Override
        public void minimumSizeChanged(int width, int height) {}
    }
}
