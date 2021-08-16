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

/*
 * @test
 * @bug 8131754
 * @summary Tests JComponent encoding
 * @run main/othervm -Djava.security.manager=allow javax_swing_JComponent
 */
public final class javax_swing_JComponent extends AbstractTest<JComponent> {

    public static void main(final String[] args) {
        new javax_swing_JComponent().test(true);
    }

    protected JComponent getObject() {
        return new SimpleJComponent();
    }

    protected JComponent getAnotherObject() {
        return new CustomJComponent();
    }

    public static final class SimpleJComponent extends JComponent {

    }

    public static final class CustomJComponent extends JComponent {

        public CustomJComponent() {
            ui = new CustomUI();
        }

        @Override
        public ComponentUI getUI() {
            return ui;
        }

        @Override
        public void setUI(final ComponentUI newUI) {
            ui = newUI;
        }
    }

    public static final class CustomUI extends ComponentUI {

        public boolean getFlag() {
            throw new Error();
        }

        public void setFlag(final boolean flag) {
            throw new Error();
        }
    }
}
