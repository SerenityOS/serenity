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

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * @test
 * @bug 4339584
 */
public final class GetUI {

    public static void main(final String[] args) {
        CustomJComponent component = new CustomJComponent();
        ComponentUI ui = new ComponentUI() {
        };
        component.setUI(ui);
        ComponentUI actual = component.getUI();
        if (actual != ui) {
            System.err.println("Expected: " + ui);
            System.err.println("Actual: " + actual);
            throw new RuntimeException("Test failed");
        }
    }

    private static class CustomJComponent extends JComponent {

        @Override
        public ComponentUI getUI() {
            return super.getUI();
        }

        @Override
        public void setUI(ComponentUI ui) {
            super.setUI(ui);
        }
    }
}
