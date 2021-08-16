/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6639507
 * @summary Title of javax.swing.JDialog is null while spec says it's empty
 * @author Pavel Porvatov
 */

import javax.swing.*;
import java.awt.*;

public class bug6639507 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                assertEmptyTitle(new Dialog((Frame) null), "new Dialog((Frame) null)");
                assertEmptyTitle(new Dialog((Frame) null, true), "new Dialog((Frame) null, true)");
                assertEmptyTitle(new Dialog((Dialog) null), "new Dialog((Dialog) null)");
                assertEmptyTitle(new Dialog((Window) null), "new Dialog((Window) null)");
                assertEmptyTitle(new Dialog(new Dialog((Window) null), Dialog.ModalityType.APPLICATION_MODAL),
                        "new Dialog((Window) null), Dialog.ModalityType.APPLICATION_MODAL");

                assertEmptyTitle(new JDialog((Frame) null), "new JDialog((Frame) null)");
                assertEmptyTitle(new JDialog((Frame) null, true), "new JDialog((Frame) null, true)");
                assertEmptyTitle(new JDialog((Dialog) null), "new JDialog((Dialog) null)");
                assertEmptyTitle(new JDialog((Dialog) null, true), "new JDialog((Dialog) null, true)");
                assertEmptyTitle(new JDialog((Window) null), "new JDialog((Window) null)");
                assertEmptyTitle(new JDialog((Window) null, Dialog.ModalityType.APPLICATION_MODAL),
                        "new JDialog((Window) null, Dialog.ModalityType.APPLICATION_MODAL)");
            }
        });
    }

    private static void assertEmptyTitle(Dialog dialog, String ctr) {
        String title = dialog.getTitle();

        if (title == null || title.length() > 0) {
            throw new RuntimeException("Title is not empty for constructor " + ctr);
        }
    }
}
