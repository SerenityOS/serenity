/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
   @bug 8059739
   @summary Dragged and Dropped data is corrupted for two data types
   @author Anton Nashatyrev
*/

import javax.swing.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class bug8059739 {

    private static boolean passed = true;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    runTest();
                } catch (Exception e) {
                    e.printStackTrace();
                    passed = false;
                }
            }
        });

        if (!passed) {
            throw new RuntimeException("Test FAILED.");
        } else {
            System.out.println("Passed.");
        }
    }

    private static void runTest() throws Exception {
        String testString = "my string";
        JTextField tf = new JTextField(testString);
        tf.selectAll();
        Clipboard clipboard = new Clipboard("clip");
        tf.getTransferHandler().exportToClipboard(tf, clipboard, TransferHandler.COPY);
        DataFlavor[] dfs = clipboard.getAvailableDataFlavors();
        for (DataFlavor df: dfs) {
            String charset = df.getParameter("charset");
            if (InputStream.class.isAssignableFrom(df.getRepresentationClass()) &&
                    charset != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (InputStream) clipboard.getData(df), charset));
                String s = br.readLine();
                System.out.println("Content: '" + s + "'");
                passed &= s.contains(testString);
            }
        }
    }
}
