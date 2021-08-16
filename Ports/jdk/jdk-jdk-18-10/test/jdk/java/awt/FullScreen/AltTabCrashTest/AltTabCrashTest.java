/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
 @test
 @key headful
 @bug 6275887 6429971 6459792 8198613
 @summary Test that we don't crash when alt+tabbing in and out of
         fullscreen app
 @author Dmitri.Trembovetski@sun.com: area=FullScreen
 @run main/othervm/timeout=100  AltTabCrashTest -auto -changedm
 @run main/othervm/timeout=100 -Dsun.java2d.d3d=True AltTabCrashTest -auto -changedm
 @run main/othervm/timeout=100 -Dsun.java2d.d3d=True AltTabCrashTest -auto -usebs -changedm
*/

import java.awt.AWTException;
import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.Random;
import java.util.Vector;

/**
 * Note that the alt+tabbing in and out part will most likely only work
 * on Windows, and only if there are no interventions.
 */

public class AltTabCrashTest extends Frame {
    public static int width;
    public static int height;
    public static volatile boolean autoMode;
    public static boolean useBS;
    public static final int NUM_OF_BALLS = 70;
    // number of times to alt+tab in and out of the app
    public static int altTabs = 5;
    private final Vector<Ball> balls = new Vector<>();
    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice();
    VolatileImage vimg = null;
    BufferStrategy bufferStrategy = null;
    volatile boolean timeToQuit = false;

    enum SpriteType {
        OVALS, VIMAGES, BIMAGES, AAOVALS, TEXT
    }

    private static boolean changeDM = false;
    private static SpriteType spriteType;
    static Random rnd = new Random();

    public AltTabCrashTest( ) {
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    timeToQuit = true;
                }
            }
        });
        setIgnoreRepaint(true);
        addMouseListener(new MouseHandler());
        for (int i = 0; i < NUM_OF_BALLS; i++) {
            int x = 50 + rnd.nextInt(550), y = 50 + rnd.nextInt(400);

            balls.addElement(createRandomBall(y, x));
        }
        setUndecorated(true);
        gd.setFullScreenWindow(this);
        GraphicsDevice gd = getGraphicsConfiguration().getDevice();
        if (gd.isDisplayChangeSupported() && changeDM) {
            DisplayMode dm = findDisplayMode();
            if (dm != null) {
                try {
                    gd.setDisplayMode(dm);
                } catch (IllegalArgumentException iae) {
                    System.err.println("Error setting display mode");
                }
            }
        }
        if (useBS) {
            createBufferStrategy(2);
            bufferStrategy = getBufferStrategy();
        } else {
            Graphics2D g = (Graphics2D) getGraphics();
            render(g);
            g.dispose();
        }
        Thread ballThread = new BallThread();
        ballThread.start();
        if (autoMode) {
            Thread tabberThread = new AltTabberThread();
            tabberThread.start();
            try {
                ballThread.join();
                tabberThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            dispose();
        }
    }

    private Ball createRandomBall(final int y, final int x) {
        Ball b;
        SpriteType type;

        if (spriteType == null) {
            int index = rnd.nextInt(SpriteType.values().length);
            type = SpriteType.values()[index];
        } else {
            type = spriteType;
        }
        switch (type) {
            case VIMAGES: b = new VISpriteBall(x, y); break;
            case AAOVALS: b = new AAOvalBall(x, y); break;
            case BIMAGES: b = new BISpriteBall(x, y); break;
            case TEXT: b = new TextBall(x,y, "Text Sprite!"); break;
            default: b = new Ball(x, y); break;
        }
        return b;
    }

    private class MouseHandler extends MouseAdapter  {
        public void mousePressed(MouseEvent e) {
            synchronized (balls) {
                balls.addElement(createRandomBall(e.getX(), e.getY()));
            }
        }
    }

    private class AltTabberThread extends Thread {
        Robot robot;

        void pressAltTab() {
            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_ALT);
        }
        void pressShiftAltTab() {
            robot.keyPress(KeyEvent.VK_SHIFT);
            pressAltTab();
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
        public void run() {
            try {
                robot = new Robot();
                robot.setAutoDelay(200);
            } catch (AWTException e) {
                throw new RuntimeException("Can't create robot");
            }
            boolean out = true;
            while (altTabs-- > 0 && !timeToQuit) {
                System.err.println("Alt+tabber Iteration: "+altTabs);
                try { Thread.sleep(2500); } catch (InterruptedException ex) {}

                if (out) {
                    System.err.println("Issuing alt+tab");
                    pressAltTab();
                } else {
                    System.err.println("Issuing shift ");
                    pressShiftAltTab();
                }
                out = !out;
            }
            System.err.println("Alt+tabber finished.");
            timeToQuit = true;
        }
    }

    private class BallThread extends Thread {
        public void run() {
            while (!timeToQuit) {
                if (useBS) {
                    renderToBS();
                    bufferStrategy.show();
                } else {
                    Graphics g = AltTabCrashTest.this.getGraphics();
                    render(g);
                    g.dispose();
                }
            }
            gd.setFullScreenWindow(null);
            AltTabCrashTest.this.dispose();
        }
    }

    static class Ball {

        int x, y;     // current location
        int dx, dy;   // motion delta
        int diameter = 40;
        Color color = Color.red;

        public Ball() {
        }

        public Ball(int x, int y) {
            this.x = x;
            this.y = y;
            dx = x % 20 + 1;
            dy = y % 20 + 1;
            color = new Color(rnd.nextInt(0x00ffffff));
        }

        public void move() {
            if (x < 10 || x >= AltTabCrashTest.width - 20)
                dx = -dx;
            if (y < 10 || y > AltTabCrashTest.height - 20)
                dy = -dy;
            x += dx;
            y += dy;
        }

        public void paint(Graphics g, Color c) {
            if (c == null) {
                g.setColor(color);
            } else {
                g.setColor(c);
            }
            g.fillOval(x, y, diameter, diameter);
        }

    }

    static class TextBall extends Ball {
        String text;
        public TextBall(int x, int y, String text) {
            super(x, y);
            this.text = text;
        }

        public void paint(Graphics g, Color c) {
            if (c == null) {
                g.setColor(color);
            } else {
                g.setColor(c);
            }
            g.drawString(text, x, y);
        }
    }

    static class AAOvalBall extends Ball {
        public AAOvalBall(int x, int y) {
            super(x, y);
        }
        public void paint(Graphics g, Color c) {
            if (c == null) {
                Graphics2D g2d = (Graphics2D)g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.fillOval(x, y, diameter, diameter);
            } else {
                g.setColor(c);
                g.fillOval(x-2, y-2, diameter+4, diameter+4);
            }
        }
    }

    static abstract class SpriteBall extends Ball {
        Image image;
        public SpriteBall(int x, int y) {
            super(x, y);
            image = createSprite();
            Graphics g = image.getGraphics();
            g.setColor(color);
            g.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        }
        public void paint(Graphics g, Color c) {
            if (c != null) {
                g.setColor(c);
                g.fillRect(x, y, image.getWidth(null), image.getHeight(null));
            } else do {
                validateSprite();
                g.drawImage(image, x, y, null);
            } while (renderingIncomplete());
        }
        public abstract Image createSprite();
        public void validateSprite() {}
        public boolean renderingIncomplete() { return false; }
    }
    class VISpriteBall extends SpriteBall {

        public VISpriteBall(int x, int y) {
            super(x, y);
        }
        public boolean renderingIncomplete() {
            return ((VolatileImage)image).contentsLost();
        }

        public Image createSprite() {
            return gd.getDefaultConfiguration().
                createCompatibleVolatileImage(20, 20);
        }
        public void validateSprite() {
            int result =
                ((VolatileImage)image).validate(getGraphicsConfiguration());
            if (result == VolatileImage.IMAGE_INCOMPATIBLE) {
                image = createSprite();
                result = VolatileImage.IMAGE_RESTORED;
            }
            if (result == VolatileImage.IMAGE_RESTORED) {
                Graphics g = image.getGraphics();
                g.setColor(color);
                g.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
            }
        }
    }
    class BISpriteBall extends SpriteBall {
        public BISpriteBall(int x, int y) {
            super(x, y);
        }
        public Image createSprite() {
            return new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        }
    }


    public void renderOffscreen() {
        Graphics2D g2d = (Graphics2D) vimg.getGraphics();
        synchronized (balls) {
            for (Ball b : balls) {
                b.paint(g2d, getBackground());
                b.move();
                b.paint(g2d, null);
            }
        }
        g2d.dispose();
    }

    public void renderToBS() {
        width = getWidth();
        height = getHeight();

        do {
            Graphics2D g2d = (Graphics2D)bufferStrategy.getDrawGraphics();

            g2d.clearRect(0, 0, width, height);
            synchronized (balls) {
                for (Ball b : balls) {
                    b.move();
                    b.paint(g2d, null);
                }
            }
            g2d.dispose();
        } while (bufferStrategy.contentsLost() ||
                bufferStrategy.contentsRestored());
    }

    public void render(Graphics g)  {
        do {
            height = getBounds().height;
            width = getBounds().width;
            if (vimg == null) {
                vimg = createVolatileImage(width, height);
                renderOffscreen();
            }
            int returnCode = vimg.validate(getGraphicsConfiguration());
            if (returnCode == VolatileImage.IMAGE_RESTORED) {
                renderOffscreen();
            } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                vimg = getGraphicsConfiguration().
                    createCompatibleVolatileImage(width, height);
                renderOffscreen();
            } else if (returnCode == VolatileImage.IMAGE_OK) {
                renderOffscreen();
            }
            g.drawImage(vimg, 0, 0, this);
        } while (vimg.contentsLost());
    }

    public static void main(String args[])  {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-auto")) {
                autoMode = true;
                System.err.println("Running in automatic mode using Robot");
            } else if (arg.equalsIgnoreCase("-usebs")) {
                useBS = true;
                System.err.println("Using BufferStrategy instead of VI");
            } else if (arg.equalsIgnoreCase("-changedm")) {
                changeDM= true;
                System.err.println("The test will change display mode");
            } else if (arg.equalsIgnoreCase("-vi")) {
                spriteType = SpriteType.VIMAGES;
            } else if (arg.equalsIgnoreCase("-bi")) {
                spriteType = SpriteType.BIMAGES;
            } else if (arg.equalsIgnoreCase("-ov")) {
                spriteType = SpriteType.OVALS;
            } else if (arg.equalsIgnoreCase("-aaov")) {
                spriteType = SpriteType.AAOVALS;
            } else if (arg.equalsIgnoreCase("-tx")) {
                spriteType = SpriteType.TEXT;
            } else {
                System.err.println("Usage: AltTabCrashTest [-usebs][-auto]" +
                                   "[-changedm][-vi|-bi|-ov|-aaov|-tx]");
                System.err.println(" -usebs: use BufferStrategy instead of VI");
                System.err.println(" -auto: automatically alt+tab in and out" +
                                   " of the application ");
                System.err.println(" -changedm: change display mode");
                System.err.println(" -(vi|bi|ov|tx|aaov) : use only VI, BI, " +
                                   "text or [AA] [draw]Oval sprites");
                System.exit(0);
            }
        }
        if (spriteType != null) {
            System.err.println("The test will only use "+spriteType+" sprites.");
        }
        new AltTabCrashTest();
    }

    private DisplayMode findDisplayMode() {
        GraphicsDevice gd = getGraphicsConfiguration().getDevice();
        DisplayMode dms[] = gd.getDisplayModes();
        DisplayMode currentDM = gd.getDisplayMode();
        for (DisplayMode dm : dms) {
            if (dm.getBitDepth() > 8 &&
                dm.getBitDepth() != DisplayMode.BIT_DEPTH_MULTI &&
                dm.getBitDepth() != currentDM.getBitDepth() &&
                dm.getWidth() == currentDM.getWidth() &&
                dm.getHeight() == currentDM.getHeight())
            {
                // found a mode which has the same dimensions but different
                // depth
                return dm;
            }
            if (dm.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI &&
                (dm.getWidth() != currentDM.getWidth() ||
                 dm.getHeight() != currentDM.getHeight()))
            {
                // found a mode which has the same depth but different
                // dimensions
                return dm;
            }
        }

        return null;
    }
}
