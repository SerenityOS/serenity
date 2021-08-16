/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import org.testng.annotations.Test;
import static jdk.internal.net.http.frame.SettingsFrame.DEFAULT_INITIAL_WINDOW_SIZE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class WindowControllerTest {

    @Test
    public void testConnectionWindowOverflow() {
        WindowController wc = new WindowController();
        assertEquals(wc.connectionWindowSize(), DEFAULT_INITIAL_WINDOW_SIZE);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.connectionWindowSize(), DEFAULT_INITIAL_WINDOW_SIZE);

        wc.registerStream(1, DEFAULT_INITIAL_WINDOW_SIZE);
        wc.tryAcquire(DEFAULT_INITIAL_WINDOW_SIZE - 1, 1, null);
        assertEquals(wc.connectionWindowSize(), 1);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.connectionWindowSize(), 1);

        wc.increaseConnectionWindow(Integer.MAX_VALUE - 1 -1);
        assertEquals(wc.connectionWindowSize(), Integer.MAX_VALUE - 1);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.connectionWindowSize(), Integer.MAX_VALUE - 1);

        wc.increaseConnectionWindow(1);
        assertEquals(wc.connectionWindowSize(), Integer.MAX_VALUE);
        assertEquals(wc.increaseConnectionWindow(1), false);
        assertEquals(wc.increaseConnectionWindow(100), false);
        assertEquals(wc.increaseConnectionWindow(Integer.MAX_VALUE), false);
        assertEquals(wc.connectionWindowSize(), Integer.MAX_VALUE);
    }

    @Test
    public void testStreamWindowOverflow() {
        WindowController wc = new WindowController();
        wc.registerStream(1, DEFAULT_INITIAL_WINDOW_SIZE);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 1), false);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 1), false);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 1), false);

        wc.registerStream(3, DEFAULT_INITIAL_WINDOW_SIZE);
        assertEquals(wc.increaseStreamWindow(100, 3), true);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 3), false);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 3), false);

        wc.registerStream(5, 0);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 5), true);
        assertEquals(wc.increaseStreamWindow(1, 5), false);
        assertEquals(wc.increaseStreamWindow(1, 5), false);
        assertEquals(wc.increaseStreamWindow(10, 5), false);

        wc.registerStream(7, -1);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 7), true);
        assertEquals(wc.increaseStreamWindow(1, 7), true);
        assertEquals(wc.increaseStreamWindow(1, 7), false);
        assertEquals(wc.increaseStreamWindow(10, 7), false);

        wc.registerStream(9, -1);
        assertEquals(wc.increaseStreamWindow(1, 9), true);
        assertEquals(wc.increaseStreamWindow(1, 9), true);
        assertEquals(wc.increaseStreamWindow(1, 9), true);
        assertEquals(wc.increaseStreamWindow(10, 9), true);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 9), false);

        wc.registerStream(11, -10);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.increaseStreamWindow(1, 11), true);
        assertEquals(wc.streamWindowSize(11), 1);
        assertEquals(wc.increaseStreamWindow(Integer.MAX_VALUE, 11), false);
        assertEquals(wc.streamWindowSize(11), 1);
    }

    @Test
    public void testStreamAdjustment() {
        WindowController wc = new WindowController();
        wc.registerStream(1, 100);
        wc.registerStream(3, 100);
        wc.registerStream(5, 100);

        // simulate some stream send activity before receiving the server's
        // SETTINGS frame, and staying within the connection window size
        wc.tryAcquire(49, 1 , null);
        wc.tryAcquire(50, 3 , null);
        wc.tryAcquire(51, 5 , null);

        wc.adjustActiveStreams(-200);
        assertEquals(wc.streamWindowSize(1), -149);
        assertEquals(wc.streamWindowSize(3), -150);
        assertEquals(wc.streamWindowSize(5), -151);
    }

    static final Class<InternalError> IE = InternalError.class;

    @Test
    public void testRemoveStream() {
        WindowController wc = new WindowController();
        wc.registerStream(1, 999);
        wc.removeStream(1);
        assertThrows(IE, () -> wc.tryAcquire(5, 1, null));

        wc.registerStream(3, 999);
        wc.tryAcquire(998, 3, null);
        wc.removeStream(3);
        assertThrows(IE, () -> wc.tryAcquire(5, 1, null));
    }
}
