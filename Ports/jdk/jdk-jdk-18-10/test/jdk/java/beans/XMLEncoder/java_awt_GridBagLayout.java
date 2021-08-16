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
 * @summary Tests GridBagLayout encoding
 * @modules java.desktop/java.awt:open
 * @run main/othervm -Djava.security.manager=allow java_awt_GridBagLayout
 * @author Sergey Malenkov
 */

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.JLabel;

public final class java_awt_GridBagLayout extends AbstractTest<GridBagLayout> {
    private static final Field HASHTABLE = getField("java.awt.GridBagLayout.comptable");

    public static void main(String[] args) {
        new java_awt_GridBagLayout().test(true);
    }

    @Override
    protected GridBagLayout getObject() {
        GridBagLayout layout = new GridBagLayout();
        update(layout, "1", 1, 1);
        update(layout, "2", 2, 2);
        update(layout, "3", 3, 3);
        return layout;
    }

    @Override
    protected GridBagLayout getAnotherObject() {
        GridBagLayout layout = new GridBagLayout();
        update(layout, "11", 1, 1);
        update(layout, "12", 1, 2);
        update(layout, "21", 2, 1);
        update(layout, "22", 2, 2);
        return layout;
    }

    @Override
    protected void validate(GridBagLayout before, GridBagLayout after) {
        super.validate(before, after);
        try {
            Hashtable a = (Hashtable) HASHTABLE.get(after);
            Hashtable b = (Hashtable) HASHTABLE.get(before);
            super.validator.validate(a, b);

//            for (int i = 0; i < size; i++) {
//                validator.validate(NAME.get(a.get(i)), NAME.get(b.get(i)));
//                validator.validate(COMP.get(a.get(i)), COMP.get(b.get(i)));
//            }
        }
        catch (Exception exception) {
            throw new Error(exception);
        }



//        for (String name : names) {
//            validator.validate(getConstraints(before, name), getConstraints(after, name));
//        }
    }

    private static void update(GridBagLayout layout, String id, int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        layout.addLayoutComponent(new JLabel(id), gbc);
    }

/*
    private static GridBagConstraints getConstraints(GridBagLayout layout, String id) {
        return (layout == null) ? null : ((MyGridBagLayout) layout).getConstraints(id);
    }
*/
}
