/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.applet.Applet;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.Frame;
import java.awt.ImageCapabilities;
import java.util.HashSet;
import java.util.Set;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;

import static java.awt.BufferCapabilities.FlipContents.BACKGROUND;
import static java.awt.BufferCapabilities.FlipContents.COPIED;
import static java.awt.BufferCapabilities.FlipContents.PRIOR;
import static java.awt.BufferCapabilities.FlipContents.UNDEFINED;

/**
 * @test
 * @key headful
 * @bug 8130390 8134732
 * @summary Applet fails to launch on virtual desktop
 * @modules java.desktop/sun.awt
 * @author Semyon Sadetsky
 */
public final class AppletFlipBuffer {

    static final ImageCapabilities[] ics = {new ImageCapabilities(true),
                                            new ImageCapabilities(false)};
    static final FlipContents[] cntx = {UNDEFINED, BACKGROUND, PRIOR, COPIED};
    static final Set<BufferCapabilities> bcs = new HashSet<>();

    static {
        for (final ImageCapabilities icFront : ics) {
            for (final ImageCapabilities icBack : ics) {
                for (final FlipContents cnt : cntx) {
                    bcs.add(new BufferCapabilities(icFront, icBack, cnt));
                }
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        Applet applet = new Applet();
        Frame frame = new Frame();
        try {
            frame.setSize(10, 10);
            frame.add(applet);
            frame.setUndecorated(true);
            frame.setVisible(true);
            test(applet);
            System.out.println("ok");
        } finally {
            frame.dispose();
        }
    }

    private static void test(final Applet applet) {
        ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        for (int i = 1; i < 10; ++i) {
            for (final BufferCapabilities caps : bcs) {
                try {
                    acc.createBufferStrategy(applet, i, caps);
                } catch (final AWTException ignored) {
                    // this kind of buffer strategy is not supported
                }
            }
        }
    }
}
