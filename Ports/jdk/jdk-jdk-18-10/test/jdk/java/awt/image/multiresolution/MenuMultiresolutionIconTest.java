/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8150258
 * @author a.stepanov
 * @summary Check that correct resolution variants are chosen for menu icons
 *          when multiresolution image is used for their construction.
 *
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main/othervm -Dsun.java2d.uiScale=1 MenuMultiresolutionIconTest
 * @run main/othervm -Dsun.java2d.uiScale=2 MenuMultiresolutionIconTest
 */


import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

public class MenuMultiresolutionIconTest extends JPanel {

    private final static int DELAY = 1000;
    private final static int SZ = 50;
    private final static String SCALE = "sun.java2d.uiScale";
    private final static Color C1X = Color.RED, C2X = Color.BLUE;
    private final ExtendedRobot r;

    private static BufferedImage generateImage(int scale, Color c) {

        int x = SZ * scale;
        BufferedImage img = new BufferedImage(x, x, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(c);
        g.fillRect(0, 0, x, x);
        return img;
    }

    private static BaseMultiResolutionImage createIcon() {

        return new BaseMultiResolutionImage(new BufferedImage[] {
            generateImage(1, C1X), generateImage(2, C2X)});
    }

    private JFrame     frame;
    private JPopupMenu popup;
    private JMenuItem  popupItem;
    private JMenu      menu;

    public MenuMultiresolutionIconTest() throws Exception {

        r = new ExtendedRobot();
        SwingUtilities.invokeAndWait(this::createUI);
    }

    private void createUI() {

        ImageIcon ii = new ImageIcon(createIcon());

        popup = new JPopupMenu();
        popupItem = new JMenuItem("test", ii);
        popup.add(popupItem);
        popupItem.setHorizontalTextPosition(JMenuItem.RIGHT);
        addMouseListener(new MousePopupListener());

        frame = new JFrame();
        JMenuBar menuBar = new JMenuBar();
        menu = new JMenu("test");
        menuBar.add(menu);
        menu.add(new JMenuItem("test", ii));
        menu.add(new JRadioButtonMenuItem("test", ii, true));
        menu.add(new JCheckBoxMenuItem("test", ii, true));

        frame.setJMenuBar(menuBar);
        frame.setContentPane(this);
        frame.setSize(300, 300);
        frame.setVisible(true);
    }

    private class MousePopupListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e)  { showPopup(e); }
        @Override
        public void mouseClicked(MouseEvent e)  { showPopup(e); }
        @Override
        public void mouseReleased(MouseEvent e) { showPopup(e); }

        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(MenuMultiresolutionIconTest.this, e.getX(), e.getY());
            }
        }
    }

    private boolean eqColors(Color c1, Color c2) {

        int tol = 15;
        return (
            Math.abs(c2.getRed()   - c1.getRed()  ) < tol &&
            Math.abs(c2.getGreen() - c1.getGreen()) < tol &&
            Math.abs(c2.getBlue()  - c1.getBlue() ) < tol);
    }

    private void checkIconColor(Point p, String what) {

        String scale = System.getProperty(SCALE);
        Color expected = "2".equals(scale) ? C2X : C1X;
        Color c = r.getPixelColor(p.x + SZ / 2, p.y + SZ / 2);
        if (!eqColors(c, expected)) {
            frame.dispose();
            throw new RuntimeException("invalid " + what + "menu item icon " +
                "color, expected: " + expected + ", got: " + c);
        }
        System.out.println(what + "item icon check passed");
    }

    private void doTest() {

        r.waitForIdle(2 * DELAY);

        Point p = getLocationOnScreen();
        r.mouseMove(p.x + getWidth() / 4, p.y + getHeight() / 4);
        r.waitForIdle(DELAY);
        r.click(InputEvent.BUTTON3_DOWN_MASK);
        r.waitForIdle(DELAY);
        p = popupItem.getLocationOnScreen();
        checkIconColor(p, "popup ");
        r.waitForIdle(DELAY);

        p = menu.getLocationOnScreen();
        r.mouseMove(p.x + menu.getWidth() / 2, p.y + menu.getHeight() / 2);
        r.waitForIdle(DELAY);
        r.click();
        p = menu.getItem(0).getLocationOnScreen();
        checkIconColor(p, "");
        r.waitForIdle(DELAY);

        p = menu.getItem(1).getLocationOnScreen();
        checkIconColor(p, "radiobutton ");
        r.waitForIdle(DELAY);

        p = menu.getItem(2).getLocationOnScreen();
        checkIconColor(p, "checkbox ");
        r.waitForIdle(DELAY);

        frame.dispose();
    }

    public static void main(String s[]) throws Exception {

        (new MenuMultiresolutionIconTest()).doTest();
    }
}
