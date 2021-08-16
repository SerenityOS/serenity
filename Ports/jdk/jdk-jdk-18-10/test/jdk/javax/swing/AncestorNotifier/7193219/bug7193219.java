/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7193219
 * @summary JComboBox serialization fails in JDK 1.7
 * @author Anton Litvinov
 */

import java.io.*;

import javax.swing.*;
import javax.swing.plaf.metal.*;

public class bug7193219 {
    private static byte[] serializeGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("Serialization");
        JPanel mainPanel = new JPanel();

        /**
         * If JComboBox is replaced with other component like JLabel
         * The issue does not happen.
         */
        JComboBox status = new JComboBox();
        status.addItem("123");
        mainPanel.add(status);
        frame.getContentPane().add(mainPanel);
        frame.pack();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(mainPanel);
            oos.flush();
            frame.dispose();
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static void deserializeGUI(byte[] serializedData) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedData));
            JPanel mainPanel = (JPanel)ois.readObject();
            JFrame frame = new JFrame("Deserialization");
            frame.getContentPane().add(mainPanel);
            frame.pack();
            frame.dispose();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                deserializeGUI(serializeGUI());
            }
        });
    }
}
