/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * This class describes a higher-contrast Metal Theme.
 *
 * @author Michael C. Albers
 */

public class ContrastTheme extends DefaultMetalTheme {

    public static String NAME = "Contrast";

    public String getName() { return NAME; }

    private final ColorUIResource primary1 = new ColorUIResource(0, 0, 0);
    private final ColorUIResource primary2 = new ColorUIResource(204, 204, 204);
    private final ColorUIResource primary3 = new ColorUIResource(255, 255, 255);
    private final ColorUIResource primaryHighlight = new ColorUIResource(102,102,102);

    private final ColorUIResource secondary2 = new ColorUIResource(204, 204, 204);
    private final ColorUIResource secondary3 = new ColorUIResource(255, 255, 255);

    protected ColorUIResource getPrimary1() { return primary1; }
    protected ColorUIResource getPrimary2() { return primary2; }
    protected ColorUIResource getPrimary3() { return primary3; }
    public ColorUIResource getPrimaryControlHighlight() { return primaryHighlight;}

    protected ColorUIResource getSecondary2() { return secondary2; }
    protected ColorUIResource getSecondary3() { return secondary3; }
    public ColorUIResource getControlHighlight() { return super.getSecondary3(); }

    public ColorUIResource getFocusColor() { return getBlack(); }

    public ColorUIResource getTextHighlightColor() { return getBlack(); }
    public ColorUIResource getHighlightedTextColor() { return getWhite(); }

    public ColorUIResource getMenuSelectedBackground() { return getBlack(); }
    public ColorUIResource getMenuSelectedForeground() { return getWhite(); }
    public ColorUIResource getAcceleratorForeground() { return getBlack(); }
    public ColorUIResource getAcceleratorSelectedForeground() { return getWhite(); }


    public void addCustomEntriesToTable(UIDefaults table) {

        Border blackLineBorder = new BorderUIResource(new LineBorder( getBlack() ));

        Object textBorder = new BorderUIResource( new CompoundBorder(
                                                       blackLineBorder,
                                                       new BasicBorders.MarginBorder()));

        table.put( "ToolTip.border", blackLineBorder);
        table.put( "TitledBorder.border", blackLineBorder);

        table.put( "TextField.border", textBorder);
        table.put( "PasswordField.border", textBorder);
        table.put( "TextArea.border", textBorder);
        table.put( "TextPane.border", textBorder);
        table.put( "EditorPane.border", textBorder);


    }

}