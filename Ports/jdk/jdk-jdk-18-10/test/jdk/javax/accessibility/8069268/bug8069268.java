/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 8069268
  @summary Tests that only one ContainerListener exists for AccessibleJComponent of JRootPane
  @author Vivi An
*/
import javax.swing.*;
import java.awt.event.*;
import javax.accessibility.*;

public class bug8069268{
    public static void main(String[] args) throws Exception {
        TestableRootPane rootPane = new TestableRootPane();

        // Get accesibleContext and then AccessibleJComponent, call the function
        // addPropertyChangeListener to trigger container listener to be added
        AccessibleContext acc = rootPane.getAccessibleContext();
        JComponent.AccessibleJComponent accJ = (JComponent.AccessibleJComponent) acc;
        accJ.addPropertyChangeListener(null);

        // Test how many container listener(s) exist(s), should only have 1
        if (!rootPane.testContainerListener())
            throw new RuntimeException("Failed test for bug 8069268");
    }

    private static class TestableRootPane extends JRootPane {
        public boolean testContainerListener() {
            boolean result = false;
            ContainerListener[] listeners = getContainerListeners();
            System.out.println("ContainerListener number is " + listeners.length);
            result = (listeners.length == 1) ? true : false;
            return result;
        }
    }
}
