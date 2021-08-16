/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Robot;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @test
 * @key headful
 * @bug 8152677
 * @requires (os.family == "mac")
 * @summary [macosx] All files filter can't be selected in JFileChooser
 * @run main SelectAllFilesFilterTest
 */

public class SelectAllFilesFilterTest {

    private static final String LABEL_TEXT = "File Format:";
    private static volatile JFileChooser fileChooser;
    private static JComboBox comboBox;

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeLater(SelectAllFilesFilterTest::createAndShowGUI);

        while (fileChooser == null) {
            Thread.sleep(100);
        }

        Robot robot = new Robot();
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            comboBox = findComboBox(fileChooser);
            comboBox.setSelectedIndex(0);
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            int selectedIndex = comboBox.getSelectedIndex();
            fileChooser.setVisible(false);

            if (selectedIndex != 0) {
                throw new RuntimeException("Select All file filter is not selected!");
            }
        });
    }

    private static void createAndShowGUI() {
        fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

        FileFilter txtFilter = new FileNameExtensionFilter("Text files", "txt");
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setFileFilter(txtFilter);
        fileChooser.showOpenDialog(null);
    }

    private static JComboBox findComboBox(Component comp) {

        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            if (LABEL_TEXT.equals(label.getText())) {
                return (JComboBox) label.getLabelFor();
            }
        }

        if (comp instanceof Container) {
            Container cont = (Container) comp;
            for (int i = 0; i < cont.getComponentCount(); i++) {

                JComboBox result = findComboBox(cont.getComponent(i));
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}
