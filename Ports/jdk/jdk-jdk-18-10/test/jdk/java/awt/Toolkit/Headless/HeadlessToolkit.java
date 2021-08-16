/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.im.InputMethodHighlight;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/*
 * @test
 * @summary Check that Toolkit methods do not throw unexpected exceptions
 *          in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessToolkit
 */

public class HeadlessToolkit {

    class awtEventListener implements AWTEventListener {
        public void eventDispatched(AWTEvent e) {
        }
    }

    class propChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
        }
    }

    public static void main(String args[]) throws IOException {
        new HeadlessToolkit().doTest();
    }

    void doTest() throws IOException {
        Toolkit tk = Toolkit.getDefaultToolkit();
        String[] fl = tk.getFontList();
        FontMetrics fm = tk.getFontMetrics(new Font(fl[0], Font.PLAIN, 10));
        tk.sync();
        tk.beep();

        boolean exceptions = false;
        try {
            Dimension d = tk.getScreenSize();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            int res = tk.getScreenResolution();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Graphics2D gd = ge.createGraphics(new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR));
            GraphicsConfiguration gc = gd.getDeviceConfiguration();
            Insets res = tk.getScreenInsets(gc);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            ColorModel cm = tk.getColorModel();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            int km = tk.getMenuShortcutKeyMask();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            int km = tk.getMenuShortcutKeyMaskEx();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            boolean state = tk.getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            boolean state = tk.getLockingKeyState(KeyEvent.VK_NUM_LOCK);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            boolean state = tk.getLockingKeyState(KeyEvent.VK_KANA_LOCK);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            boolean state = tk.getLockingKeyState(KeyEvent.VK_SCROLL_LOCK);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_CAPS_LOCK, true);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_NUM_LOCK, true);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_KANA_LOCK, true);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_SCROLL_LOCK, true);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_CAPS_LOCK, false);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_NUM_LOCK, false);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_KANA_LOCK, false);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            tk.setLockingKeyState(KeyEvent.VK_SCROLL_LOCK, false);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            Dimension d = tk.getBestCursorSize(32, 32);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            int n = tk.getMaximumCursorColors();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        EventQueue eq = tk.getSystemEventQueue();
        awtEventListener el = new awtEventListener();
        tk.addAWTEventListener(el, 0xffffffff);
        tk.removeAWTEventListener(el);

        File[] images = new File[]{new File("image.png"), new File("image.jpg"), new File("image.gif")};
        Image im;
        for (File image : images) {
            String path = image.getCanonicalPath();
            ImageIO.write(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB), path.substring(path.lastIndexOf('.')+1), image);

            im = tk.getImage(image.getAbsolutePath());
            im.flush();

            FileInputStream fis = new FileInputStream(image);
            byte[] b = new byte[(int) (image.length())];
            fis.read(b);
            fis.close();
            im = tk.createImage(b);
            im.flush();

            im = tk.createImage(image.getAbsolutePath());
            im.flush();

        }

        im = tk.getImage(new URL("http://openjdk.java.net/images/openjdk.png"));
        im.flush();

        im = tk.createImage(new URL("http://openjdk.java.net/images/openjdk.png"));
        im.flush();

        MemoryImageSource mis;
        int pixels[] = new int[50 * 50];
        int index = 0;
        for (int y = 0; y < 50; y++) {
            int red = (y * 255) / 49;
            for (int x = 0; x < 50; x++) {
                int blue = (x * 255) / 49;
                pixels[index++] = (255 << 24) | (red << 16) | blue;
            }
        }
        mis = new MemoryImageSource(50, 50, pixels, 0, 50);
        im = tk.createImage(mis);
        im.flush();


        exceptions = false;
        try {
            Cursor cur = tk.createCustomCursor(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), new Point(0, 0), "Stop");
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            Cursor cur = tk.createCustomCursor(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), new Point(0, 0), "Stop");
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            InputMethodHighlight imh = new InputMethodHighlight(true, InputMethodHighlight.CONVERTED_TEXT);
            Map m = tk.mapInputMethodHighlight(imh);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            Clipboard cl = tk.getSystemClipboard();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");
    }
}
