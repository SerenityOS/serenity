/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4856008 7025987
 * @requires (os.family == "windows")
 * @summary Tests border insets
 * @author Sergey Malenkov
 * @modules java.desktop/com.sun.java.swing.plaf.motif
 *          java.desktop/com.sun.java.swing.plaf.windows
 *          java.desktop/sun.swing.plaf.synth
 */

import com.sun.java.swing.plaf.motif.MotifBorders;
import com.sun.java.swing.plaf.windows.WindowsBorders;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.AbstractBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicToolBarUI;
import javax.swing.plaf.metal.MetalBorders;
import javax.swing.plaf.metal.MetalComboBoxEditor;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import sun.swing.plaf.synth.SynthFileChooserUI;

public class Test4856008 {
    private static final JLabel LABEL = new JLabel();
    private static final JPopupMenu POPUP = new JPopupMenu();
    private static final JToolBar TOOLBAR = new JToolBar();

    private static final Border[] BORDERS = {
            new MotifBorders.BevelBorder(true, Color.BLACK, Color.WHITE),
            new MotifBorders.ButtonBorder(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),
            new MotifBorders.FocusBorder(Color.BLACK, Color.WHITE),
            new MotifBorders.FrameBorder(LABEL),
            new MotifBorders.MenuBarBorder(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),
            new MotifBorders.MotifPopupMenuBorder(new Font(null, Font.PLAIN, 10), Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),
            new MotifBorders.ToggleButtonBorder(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),

            new WindowsBorders.ProgressBarBorder(Color.BLACK, Color.WHITE),
            new WindowsBorders.ToolBarBorder(Color.BLACK, Color.WHITE),
            //- WindowsInternalFrameUI.XPBorder is not accessible: check it visually
            //? WindowsTableHeaderUI.IconBorder is not accessible: check it visually
            //- XPStyle.XPEmptyBorder is not accessible: check it visually
            //- XPStyle.XPFillBorder is not accessible: check it visually
            //- XPStyle.XPImageBorder is not accessible: check it visually

            new BevelBorder(BevelBorder.RAISED),
            new CompoundBorder(),
            new EmptyBorder(1, 2, 3, 4),
            new EtchedBorder(),
            new LineBorder(Color.BLACK, 2, true),
            new MatteBorder(1, 2, 3, 4, Color.BLACK),
            new SoftBevelBorder(BevelBorder.LOWERED),
            new TitledBorder("4856008"),

            new BorderUIResource(new EmptyBorder(1, 2, 3, 4)),

            new BasicBorders.ButtonBorder(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),
            new BasicBorders.FieldBorder(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),
            new BasicBorders.MarginBorder(),
            new BasicBorders.MenuBarBorder(Color.BLACK, Color.WHITE),
            new BasicBorders.RadioButtonBorder(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),
            //+ BasicBorders.RolloverMarginBorder:
            new ToolBar().getRolloverMarginBorder(),
            new BasicBorders.SplitPaneBorder(Color.BLACK, Color.WHITE),
            //+ BasicBorders.SplitPaneDividerBorder:
            BasicBorders.getSplitPaneDividerBorder(),
            new BasicBorders.ToggleButtonBorder(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK),

            new MetalBorders.ButtonBorder(),
            //- MetalBorders.DialogBorder is not accessible: check it visually
            new MetalBorders.Flush3DBorder(),
            //- MetalBorders.FrameBorder is not accessible: check it visually
            new MetalBorders.InternalFrameBorder(),
            new MetalBorders.MenuBarBorder(),
            new MetalBorders.MenuItemBorder(),
            new MetalBorders.OptionDialogBorder(),
            new MetalBorders.PaletteBorder(),
            new MetalBorders.PopupMenuBorder(),
            //- MetalBorders.RolloverMarginBorder is not accessible: check it visually
            new MetalBorders.ScrollPaneBorder(),
            new MetalBorders.TableHeaderBorder(),
            new MetalBorders.ToolBarBorder(),
            //+ MetalComboBoxEditor.EditorBorder:
            new MetalEditor().getEditorBorder(),

            //- SynthBorder is not accessible: check it visually
            //- SynthScrollPaneUI.ViewportBorder is not accessible: check it visually

            //? CSSBorder is not accessible: check it visually
            //? CommentView.CommentBorder is not accessible: check it visually
            //- HiddenTagView.EndTagBorder is not accessible: check it visually
            //- HiddenTagView.StartTagBorder is not accessible: check it visually

            //+ SynthFileChooserUI.UIBorder:
            new SynthFileChooser().getUIBorder(),

            //+ LoweredBorder:
            new NimbusLookAndFeel().getDefaults().getBorder("TitledBorder.border"),
    };

    public static void main(String[] args) {
        for (Border border : BORDERS) {
            System.out.println(border.getClass());
            test(border, border.getBorderInsets(getComponent(border)));
            if (border instanceof AbstractBorder) {
                test((AbstractBorder) border);
            }
        }
    }

    private static void test(AbstractBorder border) {
        Insets insets = new Insets(0, 0, 0, 0);
        if (insets != border.getBorderInsets(getComponent(border), insets)) {
            throw new Error("both instances are differ for " + border.getClass());
        }
        test(border, insets);
    }

    private static void test(Border border, Insets insets) {
        Insets result = border.getBorderInsets(getComponent(border));
        if (insets == result) {
            throw new Error("both instances are the same for " + border.getClass());
        }
        if (!insets.equals(result)) {
            throw new Error("both insets are not equal for " + border.getClass());
        }
    }

    private static JComponent getComponent(Border border) {
        Class type = border.getClass();
        if (type.equals(MotifBorders.MotifPopupMenuBorder.class)) {
            return POPUP;
        }
        if (type.equals(WindowsBorders.ToolBarBorder.class)) {
            return TOOLBAR;
        }
        if (type.equals(MetalBorders.ToolBarBorder.class)) {
            return TOOLBAR;
        }
        return LABEL;
    }

    // This class is used to get the instance of BasicBorders.RolloverMarginBorder
    private static class ToolBar extends BasicToolBarUI {
        private Border getRolloverMarginBorder() {
            JToggleButton button = new JToggleButton();
            CompoundBorder border = (CompoundBorder) getNonRolloverBorder(button);
            return border.getInsideBorder();
        }
    }

    // This class is used to get the instance of MetalComboBoxEditor.EditorBorder
    private static class MetalEditor extends MetalComboBoxEditor {
        private Border getEditorBorder() {
            return editor.getBorder();
        }
    }

    // This class is used to get the instance of SynthFileChooserUI.UIBorder
    private static class SynthFileChooser extends SynthFileChooserUI {
        private static final JFileChooser CHOOSER = new JFileChooser();
        private String name;

        private SynthFileChooser() {
            super(CHOOSER);
        }

        private Border getUIBorder() {
            new SynthLookAndFeel().initialize();
            CHOOSER.setBorder(null);
            installDefaults(CHOOSER);
            return CHOOSER.getBorder();
        }

        @Override
        protected ActionMap createActionMap() {
            return new ActionMapUIResource();
        }

        @Override
        public String getFileName() {
            return this.name;
        }

        @Override
        public void setFileName(String name) {
            this.name = name;
        }
    }
}
