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

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Robot;

/**
 * @test
 * @key headful
 * @bug 8074500
 * @summary Checkbox.setState() call should not post ItemEvent
 * @author Sergey Bylokhov
 */
public final class SetStateExcessEvent {

    private static boolean failed;

    public static void main(final String[] args) throws Exception {
        final Robot robot = new Robot();
        final CheckboxGroup group = new CheckboxGroup();
        final Checkbox[] cbs = {new Checkbox("checkbox1", true, group),
                                new Checkbox("checkbox2", false, group),
                                new Checkbox("checkbox3", true, group),

                                new Checkbox("checkbox4", true),
                                new Checkbox("checkbox5", false),
                                new Checkbox("checkbox6", true)};
        final Frame frame = new Frame();
        frame.setLayout(new GridBagLayout());
        try {
            for (final Checkbox cb : cbs) {
                cb.addItemListener(e -> {
                    failed = true;
                });
            }
            for (final Checkbox cb : cbs) {
                frame.add(cb);
            }
            frame.pack();

            for (final Checkbox cb : cbs) {
                cb.setState(!cb.getState());
            }

            for (final Checkbox cb : cbs) {
                group.setSelectedCheckbox(cb);
            }
            robot.waitForIdle();
        } finally {
            frame.dispose();
        }
        if (failed) {
            throw new RuntimeException("Listener should not be called");
        }
        System.out.println("Test passed");
    }
}
