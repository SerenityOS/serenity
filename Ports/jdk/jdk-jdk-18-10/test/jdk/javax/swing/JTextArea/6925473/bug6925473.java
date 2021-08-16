/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6925473
 * @summary REGRESSION: JOptionPane in dialog is full-screen height
 * @author Pavel Porvatov
 * @run main bug6925473
 */

import javax.swing.*;
import java.awt.*;

public class bug6925473 {
    private static final String LONG_TEXT = "Copyright 2010 Sun Microsystems, Inc.  All Rights Reserved. " +
            "DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER. " +
            "This code is free software; you can redistribute it and/or modify it " +
            "under the terms of the GNU General Public License version 2 only, as " +
            "published by the Free Software Foundation. ";

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JTextArea textArea = new JTextArea(LONG_TEXT);

                Dimension preferredSize = textArea.getPreferredSize();

                if (preferredSize.width <= 0 || preferredSize.height <= 0) {
                    throw new RuntimeException("Invalid preferred size " + preferredSize);
                }

                JTextArea textAreaLW = new JTextArea(LONG_TEXT);

                textAreaLW.setLineWrap(true);

                Dimension preferredSizeLW = textAreaLW.getPreferredSize();

                if (preferredSizeLW.width <= 0 || preferredSizeLW.height <= 0) {
                    throw new RuntimeException("Invalid preferred size " + preferredSizeLW);
                }
            }
        });
    }
}
