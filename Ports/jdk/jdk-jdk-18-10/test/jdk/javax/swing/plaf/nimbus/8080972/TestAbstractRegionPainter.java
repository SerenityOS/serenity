/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.nimbus.AbstractRegionPainter;
/*
 * @test
 * @bug 8080972
 * @run main/othervm -Djava.security.manager=allow TestAbstractRegionPainter
 * @summary Audit Core Reflection in module java.desktop for places that will
 *          require changes to work with modules
 * @author Alexander Scherbatiy
 */

public class TestAbstractRegionPainter {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TestAbstractRegionPainter::testAbstractRegionPainter);
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(TestAbstractRegionPainter::testAbstractRegionPainter);
    }

    private static void testAbstractRegionPainter() {
        UserAbstractRegionPainter painter = new UserAbstractRegionPainter();

        JComponent userComponent = new UserJComponent();

        Color color = painter.getUserComponentColor(userComponent, "UserColor",
                Color.yellow, 0, 0, 0);

        if (!UserJComponent.USER_COLOR.equals(color)) {
            throw new RuntimeException("Wrong color: " + color);
        }
    }

    public static class UserJComponent extends JComponent {

        public static final Color USER_COLOR = new Color(10, 20, 30);
        public static final Color TEST_COLOR = new Color(15, 25, 35);

        Color color = USER_COLOR;

        public UserJComponent() {

        }

        public Color getUserColor() {
            return color;
        }

        public void setUserColor(Color color) {
            this.color = color;
        }
    }

    public static class UserAbstractRegionPainter extends AbstractRegionPainter {

        public Color getUserComponentColor(JComponent c, String property,
                Color defaultColor,
                float saturationOffset,
                float brightnessOffset,
                int alphaOffset) {
            return getComponentColor(c, property, defaultColor, saturationOffset, brightnessOffset, alphaOffset);
        }

        @Override
        protected AbstractRegionPainter.PaintContext getPaintContext() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width, int height, Object[] extendedCacheKeys) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
