/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.swingset3.demos.window.WindowDemo;
import static com.sun.swingset3.demos.window.WindowDemo.*;
import static org.jemmy2ext.JemmyExt.*;
import static org.testng.AssertJUnit.*;
import javax.swing.UIManager;
import org.testng.annotations.Test;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.WindowOperator;
import org.testng.annotations.Listeners;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 WindowDemo by checking that separate JWindow is
 *          shown, it contains predefined label and no new windows are opened
 *          when the "Show JWindow..." button is clicked.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.window.WindowDemo
 * @run testng/timeout=600 WindowDemoTest
 */
@Listeners(GuiTestListener.class)
public class WindowDemoTest {

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        new ClassReference(WindowDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frame = new JFrameOperator();

        assertEquals("Only one JWindow is shown", 1, getJWindowCount());

        WindowOperator window = new WindowOperator(getJWindow());

        assertTrue("JFrame is showing", frame.isShowing());
        assertFalse("JFrame is not iconified", isIconified(frame));
        assertTrue("JWindow is showing", window.isShowing());

        final String labelText = I_HAVE_NO_SYSTEM_BORDER;
        JLabelOperator jLabelOperator = new JLabelOperator(window, labelText);
        assertEquals("JWindow contains the label with corresponding text", labelText, jLabelOperator.getText());

        new JButtonOperator(frame, SHOW_J_WINDOW).push();

        assertEquals("Only one JWindow is shown", 1, getJWindowCount());
    }

}
