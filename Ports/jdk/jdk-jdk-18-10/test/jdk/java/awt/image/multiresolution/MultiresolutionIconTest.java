/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8150724 8151303
 * @author a.stepanov
 * @summary Check that correct resolution variants are chosen for icons
 *          when multiresolution image is used for their construction.
 *
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main/othervm/timeout=240 -Dsun.java2d.uiScale=1 MultiresolutionIconTest
 * @run main/othervm/timeout=240 -Dsun.java2d.uiScale=2 MultiresolutionIconTest
 */


// TODO: please remove the "@requires" tag after 8151303 fix


import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class MultiresolutionIconTest extends JFrame {

    private final static int SZ = 100;
    private final static int N = 5; // number of components

    private final static String SCALE = "sun.java2d.uiScale";
    private final static Color C1X = Color.RED;
    private final static Color C2X = Color.BLUE;

    private JLabel lbl;
    private JTabbedPane tabbedPane;

    private final ExtendedRobot r;

    private static BufferedImage generateImage(int sz, Color c) {

        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(c);
        g.fillRect(0, 0, sz, sz);
        return img;
    }

    public MultiresolutionIconTest(UIManager.LookAndFeelInfo lf) throws Exception {

        UIManager.setLookAndFeel(lf.getClassName());
        r = new ExtendedRobot();
        SwingUtilities.invokeAndWait(this::UI);
    }

    private void UI() {

        setUndecorated(true);

        BufferedImage img1x = generateImage(SZ / 2, C1X);
        BufferedImage img2x = generateImage(SZ, C2X);
        BaseMultiResolutionImage mri = new BaseMultiResolutionImage(
            new BufferedImage[]{img1x, img2x});
        Icon icon = new ImageIcon(mri);

        // hardcoded icon size for OS X (Mac OS X L&F) - see JDK-8151060
        BufferedImage tab1x = generateImage(16, C1X);
        BufferedImage tab2x = generateImage(32, C2X);
        BaseMultiResolutionImage tabMRI = new BaseMultiResolutionImage(
            new BufferedImage[]{tab1x, tab2x});
        Icon tabIcon = new ImageIcon(tabMRI);

        setSize((N + 1) * SZ, SZ);
        setLocation(50, 50);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new GridLayout(1, 1));

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(1, N));

        JButton btn = new JButton(icon);
        p.add(btn);

        JToggleButton tbn = new JToggleButton(icon);
        p.add(tbn);

        JRadioButton rbn = new JRadioButton(icon);
        rbn.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(rbn);

        JCheckBox cbx = new JCheckBox(icon);
        cbx.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(cbx);

        lbl = new JLabel(icon);
        p.add(lbl);

        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.addTab("", tabIcon, p);
        getContentPane().add(tabbedPane);

        setResizable(false);
        setVisible(true);
    }

    private boolean checkPressedColor(int x, int y, Color ok) {

        r.mouseMove(x, y);
        r.waitForIdle();
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        r.waitForIdle(100);
        Color c = r.getPixelColor(x, y);
        r.waitForIdle(100);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        r.waitForIdle(100);
        if (!c.equals(ok)) { return false; }
        // check the icon's color hasn't changed
        // after the mouse was released
        c = r.getPixelColor(x, y);
        return c.equals(ok);
    }

    private boolean checkTabIcon(
        int xStart, int xEnd, int yStart, int yEnd, Color ok, Color nok) {

        for (int y = yStart; y < yEnd; y += 2) {
            for (int x = xStart; x < xEnd; x += 2) {
                Color c = r.getPixelColor(x, y);
                if (c.equals(nok)) { return false; }
                else if (c.equals(ok)) {
                    // shift a bit to avoid the selection effects
                    return checkPressedColor(x + 5, y + 5, ok);
                }
            }
        }

        return false; // didn't find the icon
    }


    private void doTest() {

        r.waitForIdle(2000);
        String scale = System.getProperty(SCALE);
        boolean is2x = "2".equals(scale);
        Color expected = is2x ? C2X : C1X;
        Color unexpected = is2x ? C1X : C2X;

        Point p = lbl.getLocationOnScreen();
        int x = p.x + lbl.getWidth() / 2;
        int y = p.y + lbl.getHeight() / 2;
        int w = lbl.getWidth();

        boolean ok = true, curr;
        Color c;
        String components[] = new String[]{
            "JLabel", "JCheckBox", "JRadioButton", "JToggleButton", "JButton"};
        for (int i = 0; i < N; i++) {

            curr = true;
            int t = x - i * w;

            // check icon color
            c = r.getPixelColor(t, y);
            System.out.print(components[i] + " icon: ");
            if (!c.equals(expected)) {
                curr = false;
            } else {
                // check icon color when mouse button pressed - see JDK-8151303
                curr = checkPressedColor(t, y, expected);
            }

            System.out.println(curr ? "ok" : "nok");
            ok = ok && curr;

            r.waitForIdle();
        }

        int x0 = tabbedPane.getLocationOnScreen().x;
        int x1 = x - ((N - 1) * w + w / 2);
        int y0 = getLocationOnScreen().y;
        int y1 = y0 + getHeight();
        curr = checkTabIcon(x0, x1, y0, y1, expected, unexpected);

        System.out.println("JTabbedPane icon: " + (curr ? "ok" : "nok"));
        ok = ok && curr;

        if (!ok) { throw new RuntimeException("test failed"); }

        r.waitForIdle();
        dispose();
    }

    public static void main(String[] args) throws Exception {

        for (UIManager.LookAndFeelInfo LF: UIManager.getInstalledLookAndFeels()) {
            System.out.println("\nL&F: " + LF.getName());
            (new MultiresolutionIconTest(LF)).doTest();
        }
    }
}
