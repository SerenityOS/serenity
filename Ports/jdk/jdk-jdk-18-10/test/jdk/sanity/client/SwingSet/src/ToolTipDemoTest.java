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

import static com.sun.swingset3.demos.tooltip.ToolTipDemo.DEMO_TITLE;
import static com.sun.swingset3.demos.tooltip.ToolTipDemo.HTML_TOOLTIP_COMP_TITLE;
import static com.sun.swingset3.demos.tooltip.ToolTipDemo.HTML_TOOLTIP_TEXT;
import static com.sun.swingset3.demos.tooltip.ToolTipDemo.PLAIN_TOOLTIP_COMP_TITLE;
import static com.sun.swingset3.demos.tooltip.ToolTipDemo.PLAIN_TOOLTIP_TEXT;
import static com.sun.swingset3.demos.tooltip.ToolTipDemo.STYLE_TOOLTIP_COMP_TITLE;
import static com.sun.swingset3.demos.tooltip.ToolTipDemo.STYLE_TOOLTIP_TEXT;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JComponentOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JToolTipOperator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.swingset3.demos.tooltip.ToolTipDemo;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 ToolTipDemo page by checking whether tooltip
 *  shown or removed on user actions, tooltip text, location, number of
 *  tooltips shown at a time, with different tooltip texts plain, html and
 *  styled.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *         java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.tooltip.ToolTipDemo
 * @run testng/timeout=600 ToolTipDemoTest
 */
@Listeners(GuiTestListener.class)
public class ToolTipDemoTest {

    private static int TOOLTIP_DISMISS_DELAY = 60000;

    /**
     * Testing whether tooltip shown while keeping the mouse on label, removed
     * on mouse press, tooltip text, location, number of tooltips shown at a
     * time with different tooltip texts plain, html and styled.
     *
     * @throws Exception
     */
    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(ToolTipDemo.class.getCanonicalName()).startApplication();
        JFrameOperator frameOperator = new JFrameOperator(DEMO_TITLE);
        frameOperator.setComparator(EXACT_STRING_COMPARATOR);
        // Setting the tooltip dismiss delay
        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISS_DELAY);

        // Verifying the plain tooltip properties
        checkToolTip(frameOperator, PLAIN_TOOLTIP_COMP_TITLE,
                PLAIN_TOOLTIP_TEXT);
        // Verifying the html tooltip properties
        checkToolTip(frameOperator, HTML_TOOLTIP_COMP_TITLE,
                HTML_TOOLTIP_TEXT);
        // Verifying the styled tooltip properties
        checkToolTip(frameOperator, STYLE_TOOLTIP_COMP_TITLE,
                STYLE_TOOLTIP_TEXT);

        // Reducing the frame size to half and verifying that tooltip shown
        // even it goes out of the window
        Dimension newSize = new Dimension(frameOperator.getWidth() / 2,
                frameOperator.getHeight() / 2);
        frameOperator.resize(newSize.width, newSize.height);
        frameOperator.waitComponentSize(newSize);
        checkToolTip(frameOperator, HTML_TOOLTIP_COMP_TITLE,
                HTML_TOOLTIP_TEXT);
    }

    /**
     * Shows the tooltip on specified component and verifies the properties
     * tooltip text, location. And dismisses thetooltip after verification of
     * the properties.
     *
     * @param frameOperator
     * @param compTitle
     * @param toolTipText
     */
    private void checkToolTip(JFrameOperator frameOperator, String compTitle,
            String toolTipText) {

        JLabelOperator toolTipHostComp =
                new JLabelOperator(frameOperator, compTitle);
        JToolTipOperator toolTipOperator =
                new JToolTipOperator(toolTipHostComp.showToolTip());
        toolTipOperator.waitTipText(toolTipText);
        checkToolTipLocation(toolTipHostComp, toolTipOperator);

        // Dismissing the tooltip by mouse click
        toolTipHostComp.clickMouse();
        toolTipOperator.waitComponentShowing(false);

    }

    private void checkToolTipLocation(JComponentOperator componentOpertor,
                JToolTipOperator toolTipOperator) {
        Point labelStartPoint = componentOpertor.getLocationOnScreen();
        Dimension labelSize = componentOpertor.getSize();
        Point labelEndPoint = new Point((labelStartPoint.x + labelSize.width),
                (labelStartPoint.y + labelSize.height));
        toolTipOperator.waitComponentLocationOnScreen(
                labelStartPoint, labelEndPoint);
    }
}
