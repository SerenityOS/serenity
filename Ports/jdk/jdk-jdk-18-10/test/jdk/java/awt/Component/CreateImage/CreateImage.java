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

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;

import javax.swing.JButton;

/**
 * @test
 * @bug 6815345
 * @run main CreateImage
 * @run main/othervm -Djava.awt.headless=true CreateImage
 */
public final class CreateImage {

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeAndWait(CreateImage::test);
    }

    private static void test() {
        final JButton jbutton1 = new JButton();
        final JButton jbutton2 = new JButton();

        if (GraphicsEnvironment.isHeadless()) {
            checkCreateImage(jbutton1, true);
            checkCreateImage(jbutton2, true);
            return;
        }

        final Frame frame = new Frame();
        final Button button1 = new Button();
        final Button button2 = new Button();
        try {
            // all components are not displayable
            checkCreateImage(frame, true);
            checkCreateImage(button1, true);
            checkCreateImage(button2, true);
            checkCreateImage(jbutton1, true);
            checkCreateImage(jbutton2, true);

            // some components added to the non-displayable frame
            frame.add(button1);
            frame.add(jbutton1);
            checkCreateImage(button1, true);
            checkCreateImage(jbutton1, true);
            frame.pack();

            // tests previously added components when the frame is displayable
            checkCreateImage(frame, false);
            checkCreateImage(button1, false);
            checkCreateImage(jbutton1, false);

            // some components added to the displayable frame
            frame.add(button2);
            frame.add(jbutton2);
            checkCreateImage(button2, false);
            checkCreateImage(jbutton2, false);

        } finally {
            frame.dispose();
        }
        // tests all components after the frame became non-displayable again
        checkCreateImage(frame, true);
        checkCreateImage(button1, true);
        checkCreateImage(button2, true);
        checkCreateImage(jbutton1, true);
        checkCreateImage(jbutton2, true);
    }

    private static void checkCreateImage(final Component comp,
                                         final boolean isNull) {
        if ((comp.createImage(10, 10) != null) == isNull) {
            throw new RuntimeException("Image is wrong");
        }
        if ((comp.createVolatileImage(10, 10) != null) == isNull) {
            throw new RuntimeException("Image is wrong");
        }
        try {
            if ((comp.createVolatileImage(10, 10, null) != null) == isNull) {
                throw new RuntimeException("Image is wrong");
            }
        } catch (final AWTException ignored) {
            // this check is not applicable
        }
    }
}
