/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4916852
 * @summary Tests BorderLayout encoding
 * @run main/othervm -Djava.security.manager=allow java_awt_BorderLayout
 * @author Sergey Malenkov
 */

import java.awt.BorderLayout;
import javax.swing.JLabel;

public final class java_awt_BorderLayout extends AbstractTest<BorderLayout> {
    private static final String[] CONSTRAINTS = {
            BorderLayout.NORTH,
            BorderLayout.SOUTH,
            BorderLayout.EAST,
            BorderLayout.WEST,
            BorderLayout.CENTER,
            BorderLayout.PAGE_START,
            BorderLayout.PAGE_END,
            BorderLayout.LINE_START,
            BorderLayout.LINE_END,
    };

    public static void main(String[] args) {
        new java_awt_BorderLayout().test(true);
    }

    @Override
    protected BorderLayout getObject() {
        BorderLayout layout = new BorderLayout();
        update(layout, BorderLayout.EAST);
        update(layout, BorderLayout.WEST);
        update(layout, BorderLayout.NORTH);
        update(layout, BorderLayout.SOUTH);
        return layout;
    }

    @Override
    protected BorderLayout getAnotherObject() {
        BorderLayout layout = getObject();
        update(layout, BorderLayout.CENTER);
        return layout;
    }

    @Override
    protected void validate(BorderLayout before, BorderLayout after) {
        super.validate(before, after);
        for (String constraint : CONSTRAINTS) {
            super.validator.validate(before.getLayoutComponent(constraint),
                                     after.getLayoutComponent(constraint));
        }
    }

    private static void update(BorderLayout layout, String constraints) {
        layout.addLayoutComponent(new JLabel(constraints), constraints);
    }
}
