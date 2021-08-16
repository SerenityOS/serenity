/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests Component encoding (background, foreground and font)
 * @run main/othervm -Djava.security.manager=allow java_awt_Component
 * @author Sergey Malenkov
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public final class java_awt_Component extends AbstractTest<Component> {
    public static void main(String[] args) {
        new java_awt_Component().test(true);
    }

    @Override
    protected Component getObject() {
        Component component = new MyComponent();
        component.setBackground(Color.WHITE);
        component.setFont(new Font(null, Font.BOLD, 5));
        return component;
    }

    @Override
    protected Component getAnotherObject() {
        Component component = new MyComponent();
        component.setForeground(Color.BLACK);
        component.setFont(new Font(null, Font.ITALIC, 6));
        return component;
    }

    public static final class MyComponent extends Component {
    }
}

