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

import java.awt.EventQueue;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

/**
 * @test
 * @bug 6206439
 */
public final class SetLayerNPE {

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeAndWait(() -> {
            try {
                // JInternalFrame without parent
                new JInternalFrame("My Frame").setLayer(null);
                throw new AssertionError("expected NPE was not thrown");
            } catch (final NullPointerException ignored) {
            }
        });
        EventQueue.invokeAndWait(() -> {
            try {
                // JInternalFrame with parent
                JInternalFrame jif = new JInternalFrame("My Frame");
                new JDesktopPane().add(jif);
                jif.setLayer(null);
                throw new AssertionError("expected NPE was not thrown");
            } catch (final NullPointerException ignored) {
            }
        });
    }
}
