/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 7036669
  @summary Test Component.revalidate() method
  @author anthony.petrov@oracle.com: area=awt.component
  @run main/othervm -Djava.awt.smartInvalidate=true Revalidate
*/

import java.awt.*;

public class Revalidate {
    private static Frame frame = new Frame();
    private static Panel panel = new Panel() {
        @Override
        public boolean isValidateRoot() {
            return true;
        }
    };
    private static Button button = new Button("Test");

    private static void sleep() {
        try { Thread.sleep(500); } catch (Exception e) {}
    }

    private static void printState(String str) {
        System.out.println(str + " isValid state: ");
        System.out.println("         frame: " + frame.isValid());
        System.out.println("         panel: " + panel.isValid());
        System.out.println("        button: " + button.isValid());
    }

    private static void fail(String msg) {
        frame.dispose();
        throw new RuntimeException(msg);
    }

    private static void check(String n, Component c, boolean v) {
        if (c.isValid() != v) {
            fail(n + ".isValid() = " + c.isValid() + ";   expected: " + v);
        }
    }
    private static void check(String str, boolean f, boolean p, boolean b) {
        printState(str);

        check("frame", frame, f);
        check("panel", panel, p);
        check("button", button, b);
    }

    public static void main(String[] args) {
        // setup
        frame.add(panel);
        panel.add(button);
        frame.setBounds(200, 200, 300, 200);
        frame.setVisible(true);
        sleep();
        check("Upon showing", true, true, true);

        button.setBounds(1, 1, 30, 30);
        sleep();
        check("button.setBounds():", true, false, false);

        button.revalidate();
        sleep();
        check("button.revalidate():", true, true, true);

        button.setBounds(1, 1, 30, 30);
        sleep();
        check("button.setBounds():", true, false, false);

        panel.revalidate();
        sleep();
        // because the panel's validate root is actually OK
        check("panel.revalidate():", true, false, false);

        button.revalidate();
        sleep();
        check("button.revalidate():", true, true, true);

        panel.setBounds(2, 2, 125, 130);
        sleep();
        check("panel.setBounds():", false, false, true);

        button.revalidate();
        sleep();
        check("button.revalidate():", false, true, true);

        panel.setBounds(3, 3, 152, 121);
        sleep();
        check("panel.setBounds():", false, false, true);

        panel.revalidate();
        sleep();
        check("panel.revalidate():", true, true, true);

        // cleanup
        frame.dispose();
    }
}
