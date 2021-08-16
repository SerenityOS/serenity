/*
 * Copyright (C) 2019 JetBrains s.r.o.
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
 * @bug 8220231
 * @summary Cache HarfBuzz face object for same font's text layout calls
 * @comment Test layout operations for the same font performed simultaneously
 *          from multiple threads
 */

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

public class FontLayoutStressTest {
    private static final int NUMBER_OF_THREADS =
            Runtime.getRuntime().availableProcessors() * 2;
    private static final long TIME_TO_RUN_NS = 1_000_000_000; // 1 second
    private static final Font FONT = new Font(Font.SERIF, Font.PLAIN, 12);
    private static final FontRenderContext FRC = new FontRenderContext(null,
            false, false);
    private static final char[] TEXT = "Lorem ipsum dolor sit amet, ..."
            .toCharArray();

    private static double doLayout() {
        GlyphVector gv = FONT.layoutGlyphVector(FRC, TEXT, 0, TEXT.length,
                Font.LAYOUT_LEFT_TO_RIGHT);
        return gv.getGlyphPosition(gv.getNumGlyphs()).getX();
    }

    public static void main(String[] args) throws Throwable {
        double expectedWidth = doLayout();
        AtomicReference<Throwable> throwableRef = new AtomicReference<>();
        CyclicBarrier barrier = new CyclicBarrier(NUMBER_OF_THREADS);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    barrier.await();
                    long timeToStop = System.nanoTime() + TIME_TO_RUN_NS;
                    while (System.nanoTime() < timeToStop) {
                        double width = doLayout();
                        if (width != expectedWidth) {
                            throw new RuntimeException(
                                    "Unexpected layout result");
                        }
                    }
                } catch (Throwable e) {
                    throwableRef.set(e);
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        Throwable throwable = throwableRef.get();
        if (throwable != null) {
            throw throwable;
        }
    }
}
