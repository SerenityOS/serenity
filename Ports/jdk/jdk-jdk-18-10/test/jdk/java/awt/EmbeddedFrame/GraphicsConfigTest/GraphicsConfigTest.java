/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6356322
 * @key headful
 * @summary Tests that embedded frame's graphics configuration is updated
 *          correctly when it is moved to another screen in multiscreen system,
 *          XToolkit
 * @author artem.ananiev@sun.com: area=awt.multiscreen
 * @requires os.family == "linux"
 * @modules java.desktop/sun.awt
 *          java.desktop/sun.awt.X11
 *          java.desktop/java.awt.peer
 * @run main GraphicsConfigTest
 */

import java.awt.*;
import java.awt.peer.*;
import java.lang.reflect.*;
import java.util.*;
import sun.awt.*;

public class GraphicsConfigTest {

    private static void init()
        throws InterruptedException, AWTException {
        if (!isXToolkit()) {
            System.err.println("The test should be run only on XToolkit");
            return;
        }

        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        if (gds.length < 2) {
            System.err.println("The test should be run only in"
                + " multiscreen configuration");
            return;
        }

        boolean xinerama = Arrays.stream(gds)
            .map((gd) -> gd.getDefaultConfiguration().getBounds())
            .filter((r) -> r.x != 0 || r.y != 0).findFirst().isPresent();

        if (!xinerama) {
            System.err.println("The test should be run only with Xinerama ON");
            return;
        }

        Rectangle r0 = gds[0].getDefaultConfiguration().getBounds();
        Rectangle r1 = gds[1].getDefaultConfiguration().getBounds();

        System.setProperty("sun.awt.xembedserver", "true");
        Frame f = new Frame("F");
        try {
            final Robot robot = new Robot();

            f.setBounds(r0.x + 100, r0.y + 100, 200, 200);
            f.setVisible(true);
            robot.waitForIdle();
            Thread.sleep(1000);

            Canvas c = new Canvas();
            f.add(c);
            AWTAccessor.ComponentAccessor acc =
                        AWTAccessor.getComponentAccessor();
            WindowIDProvider wip = acc.getPeer(c);
            long h = wip.getWindow();

            EmbeddedFrame e = createEmbeddedFrame(h);
            acc.<FramePeer>getPeer(e).setBoundsPrivate(0, 0, 100,
                100); // triggers XConfigureEvent
            e.registerListeners();
            e.setVisible(true);
            robot.waitForIdle();
            Thread.sleep(1000);

            if (!checkGC(f, e)) {
                throw new RuntimeException("Failed at checkpoint 1");
            }

            f.setLocation(r1.x + 100, r1.y + 100);
            Thread.sleep(100);
            acc.<FramePeer>getPeer(e).setBoundsPrivate(0, 0, 101,
                101); // triggers XConfigureEvent
            robot.waitForIdle();
            Thread.sleep(1000);

            if (!checkGC(f, e)) {
                throw new RuntimeException("Failed at checkpoint 2");
            }

            f.setLocation(r0.x + 100, r0.y + 100);
            Thread.sleep(100);
            acc.<FramePeer>getPeer(e).setBoundsPrivate(0, 0, 102,
                102); // triggers XConfigureEvent
            robot.waitForIdle();
            Thread.sleep(1000);

            if (!checkGC(f, e)) {
                throw new RuntimeException("Failed at checkpoint 3");
            }

        } finally {
            f.dispose();
        }
    }

    private static boolean isXToolkit() {
        return Toolkit.getDefaultToolkit().getClass()
                        .getName().equals("sun.awt.X11.XToolkit");
    }

    private static EmbeddedFrame createEmbeddedFrame(long window) {
        try {
            Class cl = Class.forName("sun.awt.X11.XEmbeddedFrame");
            Constructor cons = cl.getConstructor(
                new Class[]{Long.TYPE, Boolean.TYPE});
            return (EmbeddedFrame) cons.newInstance(new Object[]{window, true});
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Can't create embedded frame");
        }
    }

    private static boolean checkGC(Component c, Component d) {
        GraphicsConfiguration g1 = c.getGraphicsConfiguration();
        System.err.println(g1);
        GraphicsConfiguration g2 = d.getGraphicsConfiguration();
        System.err.println(g2);

        return g1.equals(g2);
    }

    public static void main(String args[]) throws InterruptedException, AWTException {
        init();
    }
}
