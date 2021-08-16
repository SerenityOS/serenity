/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.jtregext.GuiTestListener;
import com.sun.swingset3.demos.scrollpane.ScrollPaneDemo;
import static com.sun.swingset3.demos.scrollpane.ScrollPaneDemo.DEMO_TITLE;
import static org.testng.AssertJUnit.*;
import javax.swing.UIManager;
import org.testng.annotations.Test;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JScrollPaneOperator;
import org.testng.annotations.Listeners;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 ScrollPaneDemo by scrolling to bottom, to top,
 *          to left and to right and checking scroll bar values.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.scrollpane.ScrollPaneDemo
 * @run testng/timeout=600 ScrollPaneDemoTest
 */
@Listeners(GuiTestListener.class)
public class ScrollPaneDemoTest {

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        new ClassReference(ScrollPaneDemo.class.getName()).startApplication();

        JFrameOperator frame = new JFrameOperator(DEMO_TITLE);
        JScrollPaneOperator jspo = new JScrollPaneOperator(frame);

        // Set initial scrollbar positions
        int initialVerticalValue = jspo.getVerticalScrollBar().getValue();
        int initialHorizontalValue = jspo.getHorizontalScrollBar().getValue();

        System.out.println("Initial Vertical Value = " + jspo.getVerticalScrollBar().getValue());
        System.out.println("Initial HoriZontal Value = " + jspo.getHorizontalScrollBar().getValue());

        // Check scroll to Bottom
        {
            jspo.scrollToBottom();
            int currentValue = jspo.getVerticalScrollBar().getValue();
            System.out.println("Final Value = " + currentValue);
            assertTrue("Scroll to Bottom of Pane "
                    + "(initialVerticalValue, actual value: " + initialVerticalValue + " "
                    + "< currentValue, actual value = " + currentValue + ")",
                    initialVerticalValue < currentValue);
        }

        // Check scroll to Top
        {
            jspo.scrollToTop();
            int currentValue = jspo.getVerticalScrollBar().getValue();
            System.out.println("Top Scroll Final Value = " + currentValue);
            assertTrue("Scroll to Top of Pane "
                    + "(initialVerticalValue, actual value: " + initialVerticalValue + " "
                    + "> currentValue, actual value = " + currentValue + ")",
                    initialVerticalValue > currentValue);
        }

        // Check scroll to Left
        {
            jspo.scrollToLeft();
            int currentValue = jspo.getHorizontalScrollBar().getValue();
            System.out.println("Scroll to Left Final Value = " + currentValue);
            assertTrue("Scroll to Left of Pane "
                    + "(initialHorizontalValue, actual value: " + initialHorizontalValue + " "
                    + "> currentValue, actual value = " + currentValue + ")",
                    initialHorizontalValue > currentValue);
        }

        // Check scroll to Right
        {
            jspo.scrollToRight();
            int currentValue = jspo.getHorizontalScrollBar().getValue();
            System.out.println("Scroll to Right Final Value = " + currentValue);
            assertTrue("Scroll to Right of Pane "
                    + "(initialHorizontalValue, actual value: " + initialHorizontalValue + " "
                    + "< currentValue, actual value = " + currentValue + ")",
                    initialHorizontalValue < currentValue);
        }
    }

}
