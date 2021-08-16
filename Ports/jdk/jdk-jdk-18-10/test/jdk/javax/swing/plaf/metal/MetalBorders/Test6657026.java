/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6657026
 * @summary Tests constancy of borders
 * @author Sergey Malenkov
 */

import java.awt.Insets;
import javax.swing.border.Border;
import javax.swing.plaf.metal.MetalBorders.ButtonBorder;
import javax.swing.plaf.metal.MetalBorders.MenuBarBorder;
import javax.swing.plaf.metal.MetalBorders.MenuItemBorder;
import javax.swing.plaf.metal.MetalBorders.PopupMenuBorder;

public class Test6657026 {

    private static final Insets NEGATIVE = new Insets(Integer.MIN_VALUE,
                                                      Integer.MIN_VALUE,
                                                      Integer.MIN_VALUE,
                                                      Integer.MIN_VALUE);

    public static void main(String[] args) {
        new ButtonBorder() {{borderInsets = NEGATIVE;}};
        new MenuBarBorder() {{borderInsets = NEGATIVE;}};
        new MenuItemBorder() {{borderInsets = NEGATIVE;}};
        new PopupMenuBorder() {{borderInsets = NEGATIVE;}};

        test(create("ButtonBorder"));
        test(create("MenuBarBorder"));
        test(create("MenuItemBorder"));
        test(create("PopupMenuBorder"));

        test(create("Flush3DBorder"));
        test(create("InternalFrameBorder"));
        // NOT USED: test(create("FrameBorder"));
        // NOT USED: test(create("DialogBorder"));
        test(create("PaletteBorder"));
        test(create("OptionDialogBorder"));
        test(create("ScrollPaneBorder"));
    }

    private static Border create(String name) {
        try {
            name = "javax.swing.plaf.metal.MetalBorders$" + name;
            return (Border) Class.forName(name).newInstance();
        }
        catch (Exception exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    private static void test(Border border) {
        Insets actual = border.getBorderInsets(null);
        if (NEGATIVE.equals(actual)) {
            throw new Error("unexpected insets in " + border.getClass());
        }
        Insets expected = (Insets) actual.clone();
        // modify
        actual.top++;
        actual.left++;
        actual.right++;
        actual.bottom++;
        // validate
        if (!expected.equals(border.getBorderInsets(null))) {
            throw new Error("shared insets in " + border.getClass());
        }
    }
}
