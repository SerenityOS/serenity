/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug 8146319
 * @summary JEditorPane function setPage leaves a file lock
 * @run main JEditorPaneTest
 */
import java.awt.Robot;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

public class JEditorPaneTest {

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        try {
            File file = File.createTempFile("Temp_", ".txt");
            file.deleteOnExit();
            writeFile(file);
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    JEditorPane editorPane = new JEditorPane();
                    try {
                        editorPane.setPage(file.toURI().toURL());
                    } catch (IOException ex) {
                        file.delete();
                        throw new RuntimeException("Test Failed" + ex);
                    }
                }
            });
            robot.waitForIdle();
            if (!file.renameTo(file)) {
                file.delete();
                throw new RuntimeException("Test Failed");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create File" + ex);
        }
    }

    private static void writeFile(File file) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("Test Text");
            bw.close();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write File" + ex);
        }

    }
}
