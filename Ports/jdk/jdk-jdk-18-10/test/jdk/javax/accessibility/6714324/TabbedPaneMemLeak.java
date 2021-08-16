/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @key headful
 * @bug 6714324
 * @summary tests if removing a Tab from JTabbedComponent, clears the reference
 * to the Page (AccessibleContext) object.
 * @modules java.desktop/java.awt:open
 * @run main TabbedPaneMemLeak
 */
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JTabbedPane;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.lang.reflect.Field;
import java.util.Hashtable;

public class TabbedPaneMemLeak
{
    private static void checkAccessibleParent(Component component) {
        //Use reflection to check the value of accessibleContext, since directly calling getAccessibleContext()
        //creates one, if not already present.
        try {
            Field field =
                    component.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField(
                            "accessibleContext");
            field.setAccessible(true);
            AccessibleContext ctx = (AccessibleContext)field.get(component);
            if (ctx != null) {
                Field accessibleParentField = field.getType().getDeclaredField("accessibleParent");
                accessibleParentField.setAccessible(true);
                Accessible parent = (Accessible)accessibleParentField.get(ctx);
                if (parent != null) {
                    throw new RuntimeException("Test failed: AccessibleContext added on the wrong codepath.");
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Test failed: Unable to fetch AccessibleContext");
        }

    }

    public static void main(String[] args) throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = new JTabbedPane();
            if (tabbedPane.getAccessibleContext() != null) {  // Ensure that the JTabbedPane has an AccessibleContext
                JComponent component = new JPanel();
                System.out.println(component.getAccessibleContext().getAccessibleParent()); // null
                tabbedPane.addTab("Component", component);
                System.out.println(component.getAccessibleContext().getAccessibleParent()); // JTabbedPane$Page

                JComponent component1 = new JPanel();
                JComponent component2 = new JPanel();

                tabbedPane.addTab("Component1", component1);
                tabbedPane.setComponentAt(1, component2);

                if (component1.getAccessibleContext().getAccessibleParent() != null) {
                    throw new RuntimeException("Test failed: Parent AccessibleContext not cleared from the child component");
                }

                tabbedPane.removeAll(); // Could also be tabbedPane.remove(component) or tabbedPane.removeTabAt(0)
                if (component.getAccessibleContext().getAccessibleParent() != null) {
                    throw new RuntimeException("Test failed: Parent AccessibleContext not cleared from the child " +
                            "component");
                }

                JSlider slider = new JSlider(0, 10);
                Hashtable<Integer, JComponent> labels = slider.createStandardLabels(5, 2);

                JComponent labelComp = labels.get(labels.keys().nextElement());

                tabbedPane.add(labelComp);

                checkAccessibleParent(labelComp);

                tabbedPane.remove(labelComp);

                checkAccessibleParent(labelComp);
            }
        });
    }
}
