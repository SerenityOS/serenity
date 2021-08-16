/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @test
 * @key headful
 * @bug 4411534 4517274
 * @summary ensures that user's requestFocus() during applet initialization
 *          is not ignored
 */
public class AppletInitialFocusTest1 extends Frame implements FocusListener {

    Button button1 = new Button("Button1");
    Button button2 = new Button("Button2");
    private static volatile Object focused;

    public static void main(final String[] args) throws Exception {
        AppletInitialFocusTest1 app = new AppletInitialFocusTest1();
        try {
            app.setSize(200, 200);
            app.setLocationRelativeTo(null);
            app.setLayout(new FlowLayout());

            app.button1.addFocusListener(app);
            app.button2.addFocusListener(app);
            app.add(app.button1);
            app.add(app.button2);
            app.setVisible(true);
            app.button2.requestFocus();
            // wait for the very very last focus event
            Thread.sleep(10000);
            if (app.button2 != focused) {
                throw new RuntimeException("Wrong focus owner: " + focused);
            }
        } finally {
            app.dispose();
        }
    }

    public void focusGained(FocusEvent e) {
        focused = e.getSource();
        System.out.println("focused = " + focused);
    }

    public void focusLost(FocusEvent e) {
    }
}
