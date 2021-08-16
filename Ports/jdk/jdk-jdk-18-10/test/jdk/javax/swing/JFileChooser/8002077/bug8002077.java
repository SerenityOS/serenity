/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Robot;
import java.awt.event.KeyEvent;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * @test
 * @key headful
 * @bug 8002077
 * @author Alexander Scherbatiy
 * @summary Possible mnemonic issue on JFileChooser Save button on nimbus L&F
 * @library ../../regtesthelpers/
 * @build Util
 * @run main bug8002077
 */
public class bug8002077 {

    private static volatile int fileChooserState = JFileChooser.ERROR_OPTION;

    public static void main(String[] args) throws Exception {
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                UIManager.put("FileChooser.openButtonMnemonic", KeyEvent.VK_O);
                UIManager.put("FileChooser.saveButtonMnemonic", KeyEvent.VK_S);
                runTest();
                break;
            }
        }
    }

    private static void runTest() throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(50);

        SwingUtilities.invokeLater(() ->
                fileChooserState = new JFileChooser().showSaveDialog(null));
        robot.waitForIdle();
        robot.delay(100);

        Util.hitMnemonics(robot, KeyEvent.VK_N);
        robot.waitForIdle();
        robot.delay(100);

        Util.hitKeys(robot, KeyEvent.VK_A);
        robot.waitForIdle();
        robot.delay(100);

        Util.hitMnemonics(robot, KeyEvent.VK_S);
        robot.waitForIdle();
        robot.delay(100);

        if (fileChooserState != JFileChooser.APPROVE_OPTION) {
            // Close the dialog
            Util.hitKeys(robot, KeyEvent.VK_ESCAPE);
            robot.waitForIdle();
            robot.delay(100);

            throw new RuntimeException("Save button is not pressed!");
        }
    }
}
