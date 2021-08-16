/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007458
 * @summary Tests CardLayout encoding
 * @modules java.desktop/java.awt:open
 * @run main/othervm -Djava.security.manager=allow java_awt_CardLayout
 * @author Sergey Malenkov
 */

import java.awt.CardLayout;
import java.lang.reflect.Field;
import java.util.Vector;
import javax.swing.JLabel;

public final class java_awt_CardLayout extends AbstractTest<CardLayout> {
    private static final Field VECTOR = getField("java.awt.CardLayout.vector");
    private static final Field NAME = getField("java.awt.CardLayout$Card.name");
    private static final Field COMP = getField("java.awt.CardLayout$Card.comp");

    public static void main(String[] args) throws Exception {
        new java_awt_CardLayout().test(true);
    }

    @Override
    protected CardLayout getObject() {
        CardLayout layout = new CardLayout();
        layout.addLayoutComponent(new JLabel("a"), "a");
        layout.addLayoutComponent(new JLabel("b"), "b");
        layout.addLayoutComponent(new JLabel("c"), "c");
        return layout;
    }

    @Override
    protected CardLayout getAnotherObject() {
        CardLayout layout = new CardLayout();
        layout.addLayoutComponent(new JLabel("a"), "a");
        layout.addLayoutComponent(new JLabel("b"), "b");
        layout.addLayoutComponent(new JLabel("c"), "c");
        layout.addLayoutComponent(new JLabel("d"), "d");
        return layout;
    }

    @Override
    protected void validate(CardLayout before, CardLayout after) {
        super.validate(before, after);
        try {
            Vector a = (Vector) VECTOR.get(after);
            Vector b = (Vector) VECTOR.get(before);
            int size = a.size();
            if (size != b.size()) {
                throw new Error("different content");
            }
            for (int i = 0; i < size; i++) {
                super.validator.validate(NAME.get(a.get(i)), NAME.get(b.get(i)));
                super.validator.validate(COMP.get(a.get(i)), COMP.get(b.get(i)));
            }
        }
        catch (Exception exception) {
            throw new Error(exception);
        }
    }
}
