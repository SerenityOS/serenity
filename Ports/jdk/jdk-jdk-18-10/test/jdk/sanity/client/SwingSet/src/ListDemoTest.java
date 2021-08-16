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

import com.sun.swingset3.demos.list.ListDemo;
import static com.sun.swingset3.demos.list.ListDemo.DEMO_TITLE;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.UIManager;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JListOperator;

import static org.jemmy2ext.JemmyExt.*;

import org.jtregext.GuiTestListener;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 ListDemo page by checking and unchecking all
 *          the checkboxes on the page and verifying the number of items in the
 *          list.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.list.ListDemo
 * @run testng/timeout=600 ListDemoTest
 */
@Listeners(GuiTestListener.class)
public class ListDemoTest {

    private static final int CHECKBOX_COUNT = 50;

    private void waitModelSize(JListOperator listOp, int size) {
        listOp.waitState(new ComponentChooser() {
            public boolean checkComponent(Component comp) {
                return getUIValue(listOp, (JList list) -> list.getModel().getSize()) == size;
            }

            public String getDescription() {
                return "Model size to be equal to " + size;
            }
        });
    }

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        new ClassReference(ListDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frame = new JFrameOperator(DEMO_TITLE);
        JListOperator listOp = new JListOperator(frame);

        // Check *NO* Prefix and Suffixes Marked
        for (int i = 0; i < CHECKBOX_COUNT; i++) {
            JCheckBoxOperator checkBox = getJCheckBoxOperator(frame, i);
            checkBox.changeSelection(false);
        }
        waitModelSize(listOp, 0);

        // Check *ALL* Prefix and Suffixes Marked
        for (int i = 0; i < CHECKBOX_COUNT; i++) {
            JCheckBoxOperator checkBox = getJCheckBoxOperator(frame, i);
            checkBox.changeSelection(true);
        }
        waitModelSize(listOp, CHECKBOX_COUNT * CHECKBOX_COUNT / 4);

        // Check *ALL* Prefix and *NO* Suffixes Marked
        for (int i = 0; i < CHECKBOX_COUNT; i++) {
            JCheckBoxOperator checkBox = getJCheckBoxOperator(frame, i);
            if (i < CHECKBOX_COUNT / 2) {
                checkBox.changeSelection(true);
            } else {
                checkBox.changeSelection(false);
            }
        }
        waitModelSize(listOp, 0);

        // Check *NO* Prefix and *ALL* Suffixes Marked
        for (int i = 0; i < CHECKBOX_COUNT; i++) {
            JCheckBoxOperator checkBox = getJCheckBoxOperator(frame, i);
            if (i < CHECKBOX_COUNT / 2) {
                checkBox.changeSelection(false);
            } else {
                checkBox.changeSelection(true);
            }
        }
        waitModelSize(listOp, 0);
    }

    private JCheckBoxOperator getJCheckBoxOperator(JFrameOperator frame, int index) {

        // We map first half of indexes to the Prefixes panel and the second half
        // to the Suffixes panel
        String labelText;
        int subindex;
        if (index < CHECKBOX_COUNT / 2) {
            labelText = "Prefixes";
            subindex = index;
        } else {
            labelText = "Suffixes";
            subindex = index - CHECKBOX_COUNT / 2;
        }

        JCheckBoxOperator result = new JCheckBoxOperator(getLabeledContainerOperator(frame, labelText), subindex);
        result.setVerification(true);
        return result;
    }

}
