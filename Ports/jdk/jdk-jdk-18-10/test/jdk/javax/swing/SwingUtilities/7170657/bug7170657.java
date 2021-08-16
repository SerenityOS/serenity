/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.SwingUtilities;
import javax.swing.event.MenuDragMouseEvent;

/**
 * @test
 * @key headful
 * @bug 7170657
 * @author Sergey Bylokhov
 */
public final class bug7170657 {

    private static boolean FAILED;

    public static void main(final String[] args) {
        final int mask = InputEvent.META_DOWN_MASK | InputEvent.CTRL_MASK;

        Frame f = new Frame();

        MouseEvent mwe = new MouseWheelEvent(f, 1, 1, mask, 1, 1, 1, 1, 1, true,
                                             1, 1, 1);
        MouseEvent mdme = new MenuDragMouseEvent(f, 1, 1, mask, 1, 1, 1, 1, 1,
                                                 true, null, null);
        MouseEvent me = new MouseEvent(f, 1, 1, mask, 1, 1, 1, 1, 1, true,
                                       MouseEvent.NOBUTTON);

        test(f, mwe);
        test(f, mdme);
        test(f, me);

        if (FAILED) {
            throw new RuntimeException("Wrong mouse event");
        }
    }


    private static void test(final Frame frame, final MouseEvent me) {
        MouseEvent newme = SwingUtilities.convertMouseEvent(frame, me, frame);
        if (me.getModifiersEx() != newme.getModifiersEx()
                || me.getModifiers() != newme.getModifiers()) {
            fail(me, newme);
        }
    }

    private static void fail(final MouseEvent exp, final MouseEvent act) {
        System.err.println("Expected: " + exp);
        System.err.println("Actual: " + act);
        FAILED = true;
    }
}
