/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.image.BufferStrategy;
import java.util.List;

import javax.swing.JFrame;

/**
 * @test
 * @bug 6933331
 * @key headful
 * @summary Tests that BufferStrategy works after the peer is recreated
 */
public final class ExceptionAfterComponentDispose {

    public static void main(final String[] args) throws Exception {
        // Will check Canvas on the main thread
        for (int size = -3; size < 3; ++size) {
            for (int numBuffers = 1; numBuffers < 3; ++numBuffers) {
                testCanvas(size, numBuffers);
            }
        }
        // Will check JFrame on EDT
        EventQueue.invokeAndWait(() -> {
            for (int size = -3; size < 3; ++size) {
                for (int numBuffers = 1; numBuffers < 3; ++numBuffers) {
                    testJFrame(size, numBuffers);
                }
            }
        });
    }

    private static void testCanvas(final int size, final int numBuffers) {
        final Frame frame = new Frame();
        try {
            final Canvas canvas = new Canvas();
            frame.setLayout(null); // we would like to control the size
            frame.add(canvas);
            frame.pack(); // creates a peer
            canvas.setSize(size, size);

            // BufferStrategy works when the peer just created
            canvas.createBufferStrategy(numBuffers);
            final BufferStrategy bs = canvas.getBufferStrategy();
            if (bs.getDrawGraphics() == null) {
                throw new RuntimeException("DrawGraphics is null");
            }

            // BufferStrategy should work when the peer is recreated
            frame.dispose();
            frame.pack();
            if (canvas.getBufferStrategy() != bs) {
                throw new RuntimeException("BS was changed");
            }
            if (bs.getDrawGraphics() == null) {
                throw new RuntimeException("DrawGraphics is null");
            }
        } finally {
            frame.dispose();
        }
    }

    private static void testJFrame(final int size, final int numBuffers) {
        for (final boolean undecorated : List.of(true, false)) {
            final JFrame frame = new JFrame();
            try {
                frame.setUndecorated(undecorated);
                frame.pack(); // creates a peer
                frame.setSize(size, size);

                // BufferStrategy works when the peer just created
                frame.createBufferStrategy(numBuffers);
                final BufferStrategy bs = frame.getBufferStrategy();
                if (bs.getDrawGraphics() == null) {
                    throw new RuntimeException("DrawGraphics is null");
                }

                // BufferStrategy should work when the peer is recreated
                frame.dispose();
                frame.pack();
                if (frame.getBufferStrategy() != bs) {
                    throw new RuntimeException("BS was changed");
                }
                if (bs.getDrawGraphics() == null) {
                    throw new RuntimeException("DrawGraphics is null");
                }
            } finally {
                frame.dispose();
            }
        }
    }
}
