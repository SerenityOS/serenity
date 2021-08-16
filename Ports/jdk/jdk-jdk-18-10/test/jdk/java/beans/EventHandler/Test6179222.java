/*
 * Copyright (c) 2004, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6179222 6271692
 * @summary Tests complex property name
 * @author Scott Violet
 */

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.beans.EventHandler;

public class Test6179222 {
    private Bar bar = new Bar();

    public static void main(String[] args) {
        Test6179222 test = new Test6179222();
        // test 6179222
        test(EventHandler.create(ActionListener.class, test, "foo", "source.icon"));
        // test 6265540
        test(EventHandler.create(ActionListener.class, test, "bar.doit"));
        if (!test.bar.invoked) {
            throw new Error("Bar was not set");
        }
    }

    private static void test(ActionListener listener) {
        JButton button = new JButton("hi");
        button.addActionListener(listener);
        button.doClick();
    }

    public void foo(Icon o) {
    }

    public Bar getBar() {
        return this.bar;
    }

    public static class Bar {
        private boolean invoked;

        public void doit() {
            this.invoked = true;
        }
    }
}
