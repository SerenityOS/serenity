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

import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;
import static org.testng.Assert.assertTrue;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxMenuItemOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JMenuOperator;
import org.netbeans.jemmy.operators.JRadioButtonMenuItemOperator;
import org.netbeans.jemmy.operators.JToggleButtonOperator;
import org.netbeans.jemmy.util.NameComponentChooser;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @key headful
 * @summary Verifies check box menu item, radio button menu item, nested menus
 * and themes using SwingSet2 main window. Testing of other swing components
 * are covered in SwingSet3 demo tests.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet2/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build SwingSet2
 * @run testng/timeout=600 SwingSet2DemoTest
 */
@Listeners(GuiTestListener.class)
public class SwingSet2DemoTest {

    private static final String OCEAN_THEME_NAME = "Ocean";
    private static final String STEEL_THEME_NAME = "Steel";
    private static final int TOOLTIP_DISMISS_DELAY = 60000;
    private final static long TOOLTIP_TIMEOUT = 5000;

    /**
     * Testing check box menu item, radio button menu item, nested menus and
     * themes. Testing of all other main swing components are covered in
     * SwingSet3 demo tests.
     *
     * @throws Exception
     */
    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        new ClassReference(SwingSet2.class.getCanonicalName()).startApplication();
        JFrameOperator frameOperator = new JFrameOperator(SwingSet2.FRAME_TITLE);
        frameOperator.setComparator(EXACT_STRING_COMPARATOR);

        checkCheckBoxMenuItem(frameOperator);
        JMenuOperator themesMenu = new JMenuOperator(frameOperator, SwingSet2.THEMES_MENU_TEXT);
        // Themes menu will be enabled only on MetalLookAndFeel
        if(themesMenu.isEnabled()) {
            checkMenuOnMenuAndRadioButtonMenuItem(frameOperator, themesMenu);
            checkThemes(frameOperator, themesMenu);
        }
    }

    /**
     * Verifies the properties of nested menu and radio button menu item
     * @param frameOperator
     * @param themesMenu
     */
    private void checkMenuOnMenuAndRadioButtonMenuItem(
            JFrameOperator frameOperator, JMenuOperator themesMenu) {

        themesMenu.push();
        themesMenu.waitSelected(true);
        // Font menu is a nested menu, selecting that and verifying its
        // sub menu items are launched
        JMenuOperator fontMenu = new JMenuOperator(frameOperator, SwingSet2.FONT_MENU_TEXT);
        JRadioButtonMenuItemOperator fontPlainMenuItem = new JRadioButtonMenuItemOperator(
                (JRadioButtonMenuItem)fontMenu.showMenuItem(
                        SwingSet2.FONT_PLAIN_MENU_TEXT, "/").getSource());
        JRadioButtonMenuItemOperator fontBoldMenuItem = new JRadioButtonMenuItemOperator(
                (JRadioButtonMenuItem)fontMenu.showMenuItem(
                        SwingSet2.FONT_BOLD_MENU_TEXT, "/").getSource());
        JButtonOperator testComp =
                new JButtonOperator(frameOperator, OptionPaneDemo.INPUT_BUTTON);
        checkRadioButtonMenuItem(fontBoldMenuItem, fontPlainMenuItem, testComp,
                component -> component.getFont().isBold());
        checkRadioButtonMenuItem(fontPlainMenuItem, fontBoldMenuItem, testComp,
                component -> component.getFont().isPlain());
    }

    /**
     * Verifies the properties of radio button menu item
     * @param pressMenuItem
     * @param unPressMenuItem
     * @param testComp
     * @param validator
     */
    private void checkRadioButtonMenuItem(JRadioButtonMenuItemOperator pressMenuItem,
            JRadioButtonMenuItemOperator unPressMenuItem,
            ComponentOperator testComp, ComponentChooser validator){

        // Selecting a radio button menu item and verifying the pressed menu
        // item is selected, other one is unselected and corresponding ui
        // changes also happened
        pressMenuItem.push();
        pressMenuItem.waitSelected(true);
        unPressMenuItem.waitSelected(false);
        testComp.waitStateOnQueue(validator);
    }

    /**
     * Verifies the properties of check box menu item
     * @param frameOperator
     */
    private void checkCheckBoxMenuItem(JFrameOperator frameOperator) {

        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISS_DELAY);
        JToggleButtonOperator testComp = new JToggleButtonOperator(
                frameOperator, new NameComponentChooser(SwingSet2.getString(
                        OptionPaneDemo.DEMO_NAME + SwingSet2.NAME_PROP_SUFFIX)));
        JMenuOperator optionsMenu = new JMenuOperator(frameOperator, SwingSet2.OPTIONS_MENU_TEXT);
        JCheckBoxMenuItemOperator toolTipMenuItem = new JCheckBoxMenuItemOperator(
                (JCheckBoxMenuItem)optionsMenu.showMenuItem(
                        SwingSet2.TOOLTIP_MENU_TEXT, "/").getSource());
        // Selecting and deselecting tooltip checkbox menu item and verifying
        // tooltip is showing for demo toggle button
        toolTipMenuItem.push();
        toolTipMenuItem.waitSelected(false);
        // Set tooltip timeout as 5 seconds
        testComp.getTimeouts().setTimeout("JToolTipOperator.WaitToolTipTimeout", TOOLTIP_TIMEOUT);
        boolean isToolTipTimeout = false;
        try {
            testComp.showToolTip();
        } catch (TimeoutExpiredException e) {
            isToolTipTimeout = true;
        }
        assertTrue(isToolTipTimeout, "Tooltip is showing even after unchecking the checkbox menu"
                + " item 'Enable Tool Tips'");
        toolTipMenuItem.push();
        toolTipMenuItem.waitSelected(true);
        testComp.showToolTip();
    }

    /**
     * Verifies the different themes by applying different themes
     * @param frameOperator
     * @param themesMenu
     */
    private void checkThemes(JFrameOperator frameOperator, JMenuOperator themesMenu) {
        String themeMenuNames [] = {SwingSet2.OCEAN_MENU_TEXT, SwingSet2.AQUA_MENU_TEXT,
                SwingSet2.STEEL_MENU_TEXT, SwingSet2.CHARCOAL_MENU_TEXT,
                SwingSet2.CONTRAST_MENU_TEXT, SwingSet2.EMERALD_MENU_TEXT, SwingSet2.RUBY_MENU_TEXT};
        String themeNames [] = {OCEAN_THEME_NAME, AquaTheme.NAME, STEEL_THEME_NAME,
                CharcoalTheme.NAME, ContrastTheme.NAME, EmeraldTheme.NAME, RubyTheme.NAME};

        for (int i = 0; i < themeMenuNames.length; i++) {
            int themeIndex = i;
            JRadioButtonMenuItemOperator menuItem = new JRadioButtonMenuItemOperator(
                    (JRadioButtonMenuItem)themesMenu.showMenuItem(
                            themeMenuNames[themeIndex], "/").getSource());
            menuItem.push();
            menuItem.waitSelected(true);
            menuItem.waitStateOnQueue(comp -> themeNames[themeIndex].
                    equals(MetalLookAndFeel.getCurrentTheme().getName()));
        }
    }

}
