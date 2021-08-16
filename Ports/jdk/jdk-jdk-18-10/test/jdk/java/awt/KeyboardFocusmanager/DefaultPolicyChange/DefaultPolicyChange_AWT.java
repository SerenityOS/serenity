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

/*
  @test
  @key headful
  @bug 6741526
  @summary KeyboardFocusManager.setDefaultFocusTraversalPolicy(FocusTraversalPolicy) affects created components
  @library ../../regtesthelpers
  @build Sysout
  @author Andrei Dmitriev : area=awt-focus
  @run main DefaultPolicyChange_AWT
*/

import java.awt.*;
import test.java.awt.regtesthelpers.Sysout;

public class DefaultPolicyChange_AWT {
    public static void main(String []s) {
        DefaultPolicyChange_AWT.runTestAWT();
    }

    private static void runTestAWT(){
        KeyboardFocusManager currentKFM = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        FocusTraversalPolicy defaultFTP = currentKFM.getDefaultFocusTraversalPolicy();
        ContainerOrderFocusTraversalPolicy newFTP = new ContainerOrderFocusTraversalPolicy();

        Frame frame = new Frame();
        Window window = new Window(frame);

        FocusTraversalPolicy resultFTP = window.getFocusTraversalPolicy();
        System.out.println("FocusTraversalPolicy on window = " + resultFTP);
        /**
         * Note: this call doesn't affect already created components as they have
         * their policy initialized. Only new components will use this policy as
         * their default policy.
         **/
        System.out.println("Now will set another policy.");
        currentKFM.setDefaultFocusTraversalPolicy(newFTP);
        resultFTP = window.getFocusTraversalPolicy();
        if (!resultFTP.equals(defaultFTP)) {
            System.out.println("Failure! FocusTraversalPolicy should not change");
            System.out.println("Was: " + defaultFTP);
            System.out.println("Become: " + resultFTP);
            throw new RuntimeException("Failure! FocusTraversalPolicy should not change");
        }
    }
}
