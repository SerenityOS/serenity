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
  @bug 7027013
  @summary Dialog.show() should validate the window unconditionally
  @author anthony.petrov@oracle.com: area=awt.toplevel
  @run main ValidateOnShow
*/

import java.awt.*;

public class ValidateOnShow {
    private static Dialog dialog = new Dialog((Frame)null);
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

    private static void test() {
        System.out.println("Before showing: panel.isValid=" + panel.isValid() + "      dialog.isValid=" + dialog.isValid());
        dialog.setVisible(true);
        sleep();
        System.out.println("After showing:  panel.isValid=" + panel.isValid() + "      dialog.isValid=" + dialog.isValid());

        if (!panel.isValid()) {
            dialog.dispose();
            throw new RuntimeException("The panel hasn't been validated upon showing the dialog");
        }

        dialog.setVisible(false);
        sleep();
    }

    public static void main(String[] args) {
        // setup
        dialog.add(panel);
        panel.add(button);

        dialog.setBounds(200, 200, 300, 200);

        // The first test should always succeed since the dialog is invalid initially
        test();

        // now invalidate the button and the panel
        button.setBounds(1, 1, 30, 30);
        sleep();
        // since the panel is a validate root, the dialog is still valid

        // w/o a fix this would fail
        test();

        // cleanup
        dialog.dispose();
    }
}
