/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;

/*
 * @test
 * @summary Check that Cursor constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessCursor
 */

public class HeadlessCursor {
    public static void main(String args[]) {
        Cursor c;
        c = new Cursor(Cursor.CROSSHAIR_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.DEFAULT_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.E_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.HAND_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.N_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.NE_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.NW_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.S_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.SE_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.SW_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.TEXT_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.W_RESIZE_CURSOR);
        c.getType();
        c.getName();
        c = new Cursor(Cursor.WAIT_CURSOR);
        c.getType();
        c.getName();

        c = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        c = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        c = Cursor.getDefaultCursor();
        c.getType();
        c.getName();
    }
}
