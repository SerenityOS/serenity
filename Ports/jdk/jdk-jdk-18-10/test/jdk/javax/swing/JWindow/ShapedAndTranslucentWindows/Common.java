/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;

public abstract class Common {

    ExtendedRobot robot;
    Class<? extends JFrame> windowClass;
    JFrame background;
    BufferedImage foreground;
    Window window;
    volatile boolean gradientBackgroundEnabled = false;
    volatile int gradientWidth = 255;
    volatile int gradientHeight = 255;

    float opacity = 1.0f;
    float perPixelTranslucency = 1.0f;
    static Color BG_COLOR = Color.BLUE;
    static Color FG_COLOR = Color.RED;
    static final int delay = 1000;
    static final SecureRandom random = new SecureRandom();
    static final int dl = 100;
    static final Class[] WINDOWS_TO_TEST = { JWindow.class, JFrame.class, JDialog.class };

    volatile int clicked;

    public Common(Class windowClass, float opacity, float perPixelTranslucency, boolean gradient) throws Exception {
        this.gradientBackgroundEnabled = gradient;
        this.perPixelTranslucency = perPixelTranslucency;
        this.opacity = opacity;
        robot = new ExtendedRobot();
        this.windowClass = windowClass;
        EventQueue.invokeAndWait(this::initBackgroundFrame);
        EventQueue.invokeAndWait(this::initGUI);
    }

    public Common(Class windowClass) throws Exception {
        this(windowClass, 1.0f, 1.0f, false);
    }

    public Common(Class windowClass, boolean gradient) throws Exception {
        this(windowClass, 1.0f, 1.0f, gradient);
    }

    public abstract void doTest() throws Exception;

    public void dispose() {
        window.dispose();
        background.dispose();
    }

    public void applyShape() {};

    public void applyDynamicShape() {
        final Area a = new Area();
        Dimension size = window.getSize();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                a.add(new Area(new Rectangle2D.Double(
                        x * size.getWidth() / 17*6, y * size.getHeight() / 17*6,
                        size.getWidth() / 17*5, size.getHeight() / 17*5)));
            }
        }
        window.setShape(a);
    }

    public BufferedImage getForegroundWindow() throws Exception {
        final BufferedImage f[] = new BufferedImage[1];
        EventQueue.invokeAndWait( () -> {
            f[0] = new BufferedImage(window.getWidth(),
                    window.getHeight(), BufferedImage.TYPE_INT_RGB);
            window.printAll(f[0].createGraphics());
        });
        robot.waitForIdle(delay);
        return f[0];
    }

    public static boolean checkTranslucencyMode(GraphicsDevice.WindowTranslucency mode) {

        if (!GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .isWindowTranslucencySupported(mode)){
            System.out.println(mode+" translucency mode isn't supported");
            return false;
        } else {
            return true;
        }

    }

    public void applyAppDragNResizeSupport() {
        MouseAdapter m = new MouseAdapter() {

            private Point dragOrigin = null;
            private Dimension origSize = null;
            private Point origLoc = null;
            private boolean left = false;
            private boolean top = false;
            private boolean bottom = false;
            private boolean right = false;

            public void mousePressed(MouseEvent e) {
                dragOrigin = e.getLocationOnScreen();
                origSize = window.getSize();
                origLoc = window.getLocationOnScreen();
                right = (origLoc.x + window.getWidth() - dragOrigin.x) < 5;
                left = !right && dragOrigin.x - origLoc.x < 5;
                bottom = (origLoc.y + window.getHeight() - dragOrigin.y) < 5;
                top = !bottom && dragOrigin.y - origLoc.y < 5;
            }

            public void mouseReleased(MouseEvent e) { resize(e); }
            public void mouseDragged(MouseEvent e) { resize(e); }

            void resize(MouseEvent e) {
                Point dragDelta = e.getLocationOnScreen();
                dragDelta.translate(-dragOrigin.x, -dragOrigin.y);
                Point newLoc = new Point(origLoc);
                newLoc.translate(dragDelta.x, dragDelta.y);
                Dimension newSize = new Dimension(origSize);
                if (left || right) {
                    newSize.width += right ? dragDelta.x : -dragDelta.x;
                }
                if (top || bottom) {
                    newSize.height += bottom ? dragDelta.y : -dragDelta.y;
                }
                if (right || (top || bottom) && !left) {
                    newLoc.x = origLoc.x;
                }
                if (bottom || (left || right) && !top) {
                    newLoc.y = origLoc.y;
                }
                window.setBounds(newLoc.x, newLoc.y, newSize.width, newSize.height);
            }
        };
        for (Component comp : window.getComponents()) {
            comp.addMouseListener(m);
            comp.addMouseMotionListener(m);
        }

        window.addMouseListener(m);
        window.addMouseMotionListener(m);
    }

    public void checkTranslucentShape() throws Exception {
        foreground = getForegroundWindow();
        Point[] points = new Point[4];

        Dimension size = window.getSize();
        Point location = window.getLocationOnScreen();

        points[0] = new Point(20, 20);
        points[1] = new Point(20, size.height-20);
        points[2] = new Point(size.width-20, 20);
        points[3] = new Point(size.width-20, size.height-20);

        for (Point p : points){
            p.translate(location.x, location.y);
            Color actual = robot.getPixelColor(p.x, p.y);
            if (actual.equals(BG_COLOR)|| actual.equals(FG_COLOR))
                throw new RuntimeException("Error in point "+p+": "+actual+" equals to foreground or background color");
            else
                System.out.println("OK with foreground point "+p);
        }
    }

    public void checkDynamicShape() throws Exception {
        Point[] points = new Point[4];

        Dimension size = window.getSize();

        int blockSizeX = (int) (size.getWidth() / 17);
        int blockSizeY = (int) (size.getHeight() / 17);

        // background
        points[0] = new Point((int) (blockSizeX * 5.5), (int) (blockSizeY * 5.5));
        points[1] = new Point((int) (size.getWidth() - blockSizeX * 5.5), (int) (size.getHeight() - blockSizeY * 5.5));
        points[2] = new Point((int) (blockSizeX * 5.5), (int) (size.getHeight() - blockSizeY * 5.5));
        points[3] = new Point((int) (size.getWidth() - blockSizeX * 5.5), (int) (blockSizeY * 5.5));
        checkShape(points, true);

        // foreground
        if (opacity < 1.0f){
            checkTranslucentShape();
        } else {
            points[0] = new Point(3 * blockSizeX, 3 * blockSizeY);
            points[1] = new Point(14 * blockSizeX, 14 * blockSizeY);
            points[2] = new Point(3 * blockSizeX, 14 * blockSizeY);
            points[3] = new Point(14 * blockSizeX, 3 * blockSizeY);
            checkShape(points, false);
        }
    }

    public void checkShape(Point[] points, boolean areBackgroundPoints) throws Exception {

        Point location = window.getLocationOnScreen();

        for (Point p : points) {
            p.translate(location.x, location.y);
            Color pixel = robot.getPixelColor(p.x, p.y);
            if (areBackgroundPoints) {
                if (pixel.getRed() != 0
                    || pixel.getGreen() != 0 )
                    throw new RuntimeException("Background point " + p +
                            " color " + pixel +
                            " does not equal to background color " + BG_COLOR);
                else
                    System.out.println("OK with background point " + p);
            } else {
                if (pixel.equals(BG_COLOR))
                    throw new RuntimeException("Foreground point " + p +
                            " color " + pixel +
                            " equals to background color " + BG_COLOR);
                else
                    System.out.println("OK with foreground point " + p);
            }
        }
    }

    public void initBackgroundFrame() {
        background = new JFrame();
        background.setUndecorated(true);
        background.getContentPane().setBackground(BG_COLOR);
        background.setSize(500, 500);
        background.setLocation(dl, dl);
        background.setVisible(true);
    }

    public void initGUI() {
        Container contentPane;
        if (windowClass.equals(Frame.class)) {
            window = new JFrame();
            ((JFrame) window).setUndecorated(true);
            contentPane = ((JFrame) window).getContentPane();
        } else  if (windowClass.equals(Dialog.class)) {
            window = new JDialog(background);
            ((JDialog) window).setUndecorated(true);
            contentPane = ((JDialog) window).getContentPane();
        } else {
            window = new JWindow(background);
            contentPane = ((JWindow) window).getContentPane();
        }

        if (perPixelTranslucency < 1.0f) {
            contentPane.setBackground(colorWithOpacity(FG_COLOR, perPixelTranslucency));
            window.setBackground(colorWithOpacity(FG_COLOR, perPixelTranslucency));
        } else {
            contentPane.setBackground(FG_COLOR);
            window.setBackground(FG_COLOR);
        }

        window.setLocation(2 * dl, 2 * dl);
        window.setSize(255, 255);
        window.setPreferredSize(new Dimension(255, 255));
        createSwingComponents();
        if (opacity < 1.0f)
            window.setOpacity(opacity);

        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyShape();
            }
        });
        applyShape();
        window.setVisible(true);
        applyAppDragNResizeSupport();
        window.toFront();
    }

    public void createSwingComponents() {
        Container contentPane;
        if (gradientBackgroundEnabled) {
            JPanel jPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    if (g instanceof Graphics2D) {
                        Color background = Color.RED;
                        Paint p = new GradientPaint(0.0f, 0.0f, colorWithOpacity(background, 0),
                                0.0f, gradientHeight - 3, colorWithOpacity(background, 1), true);
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setPaint(p);
                        g2d.fillRect(0, 3, gradientWidth, gradientHeight - 3);
                    } else {
                        super.paintComponent(g);
                    }
                }
            };
            jPanel.setBorder(new EmptyBorder(15, 5, 5, 5));
            jPanel.setOpaque(false);

            contentPane = jPanel;

            RootPaneContainer.class.cast(window).setContentPane(contentPane);
        } else {
            contentPane = RootPaneContainer.class.cast(window).getContentPane();
        }
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JButton button = new JButton("JButton");
        window.add(button);

        JTextArea textArea = new JTextArea("JTextArea");
        window.add(textArea);

        JCheckBox checkbox = new JCheckBox("JCheckBox");
        checkbox.setOpaque(false);
        window.add(checkbox);

        JComboBox comboBox = new JComboBox(new String[]{"JComboBox", "Some item"});
        window.add(comboBox);

        JLabel label = new JLabel("JLabel");
        window.add(label);

        JTextField textField = new JTextField("JTextField");
        window.add(textField);

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        window.add(panel);

        JComboBox comboBox2 = new JComboBox(new String[]{"JComboBox2", "Another item"});
        window.add(comboBox2);

        JRadioButton radioButton = new JRadioButton("JRadioButton");
        radioButton.setOpaque(false);
        window.add(radioButton);
    }

    Color colorWithOpacity(Color color, float opacity) {
        return new Color(color.getColorSpace(), color.getColorComponents(null), opacity);
    }

    public void checkTranslucent() throws Exception {
        checkTranslucentShape();

        // Drag
        Point location = window.getLocationOnScreen();
        robot.dragAndDrop(location.x + 30, location.y + 5, location.x + dl + random.nextInt(dl), location.y + random.nextInt(dl));
        robot.waitForIdle(2*delay);
        checkTranslucentShape();

        // Resize
        location = window.getLocationOnScreen();
        robot.dragAndDrop(location.x + 4, location.y + 4, location.x + random.nextInt(2*dl)-dl, location.y + random.nextInt(2*dl)-dl);
        robot.waitForIdle(2*delay);
        checkTranslucentShape();

        EventQueue.invokeAndWait(this::dispose);
    }

    public void checkDynamic() throws Exception {
        checkDynamicShape();

        // Drag
        Point location = window.getLocationOnScreen();
        robot.dragAndDrop(location.x + 30, location.y + 5, location.x + dl + random.nextInt(dl), location.y + random.nextInt(dl));
        robot.waitForIdle(2*delay);
        checkDynamicShape();

        // Resize
        location = window.getLocationOnScreen();
        robot.dragAndDrop(location.x + 4, location.y + 4, location.x + random.nextInt(2*dl)-dl, location.y + random.nextInt(2*dl)-dl);
        robot.waitForIdle(2*delay);
        checkDynamicShape();

        EventQueue.invokeAndWait(this::dispose);
    }

    void checkClick(int x, int y, int flag) throws Exception {

        System.out.println("Trying to click point " + x + ", " + y + ", looking for " + flag + " flag to trigger.");

        clicked = 0;
        robot.mouseMove(x, y);
        robot.click();

        for (int i = 0; i < 100; i++)
            if ((clicked & (1 << flag)) == 0)
                robot.delay(50);
            else
                break;

        if ((clicked & (1 << flag)) == 0)
            throw new RuntimeException("FAIL: Flag " + flag + " is not triggered for point " + x + ", " + y + "!");
    }
}
