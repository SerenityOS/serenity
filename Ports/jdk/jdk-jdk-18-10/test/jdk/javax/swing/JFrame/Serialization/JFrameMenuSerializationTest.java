/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8189201
 * @summary [macosx] NotSerializableException during JFrame with MenuBar
 *          serialization
 * @run main JFrameMenuSerializationTest
 */

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

public class JFrameMenuSerializationTest {
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            frame.setJMenuBar(new JMenuBar());
            frame.setVisible(true);
            frame.getAccessibleContext().addPropertyChangeListener(evt -> {});
        });
        Robot robot = new Robot();
        robot.waitForIdle();
        robot.delay(200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        SwingUtilities.invokeAndWait(() -> {
            try {
                oos.writeObject(frame);
            } catch (NotSerializableException e) {
                throw new RuntimeException("Test failed", e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        SwingUtilities.invokeLater(frame::dispose);

    }
}
