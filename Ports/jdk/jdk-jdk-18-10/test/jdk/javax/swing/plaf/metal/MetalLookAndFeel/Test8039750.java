/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.UIDefaults;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;

/*
 * @test
 * @bug 8039750
 * @summary Tests MetalLazyValue removing
 * @author Sergey Malenkov
 */
public class Test8039750 {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UIDefaults table= new MetalLookAndFeel().getDefaults();
            test(table.get("ToolBar.rolloverBorder"),
                    "javax.swing.plaf.metal.MetalBorders$ButtonBorder",
                    "javax.swing.plaf.metal.MetalBorders$RolloverMarginBorder");
            test(table.get("ToolBar.nonrolloverBorder"),
                    "javax.swing.plaf.metal.MetalBorders$ButtonBorder",
                    "javax.swing.plaf.metal.MetalBorders$RolloverMarginBorder");
            test(table.get("RootPane.frameBorder"),
                    "javax.swing.plaf.metal.MetalBorders$FrameBorder");
            test(table.get("RootPane.plainDialogBorder"),
                    "javax.swing.plaf.metal.MetalBorders$DialogBorder");
            test(table.get("RootPane.informationDialogBorder"),
                    "javax.swing.plaf.metal.MetalBorders$DialogBorder");
            test(table.get("RootPane.errorDialogBorder"),
                    "javax.swing.plaf.metal.MetalBorders$ErrorDialogBorder");
            test(table.get("RootPane.colorChooserDialogBorder"),
                    "javax.swing.plaf.metal.MetalBorders$QuestionDialogBorder");
            test(table.get("RootPane.fileChooserDialogBorder"),
                    "javax.swing.plaf.metal.MetalBorders$QuestionDialogBorder");
            test(table.get("RootPane.questionDialogBorder"),
                    "javax.swing.plaf.metal.MetalBorders$QuestionDialogBorder");
            test(table.get("RootPane.warningDialogBorder"),
                    "javax.swing.plaf.metal.MetalBorders$WarningDialogBorder");
        });
    }

    private static void test(Object value, String name) {
        if (!value.getClass().getName().equals(name)) {
            throw new RuntimeException(name);
        }
    }

    private static void test(Object value, String one, String two) {
        if (value instanceof CompoundBorder) {
            CompoundBorder border = (CompoundBorder) value;
            test(border.getOutsideBorder(), one);
            test(border.getInsideBorder(), two);
        } else {
            throw new RuntimeException("CompoundBorder");
        }
    }
}
