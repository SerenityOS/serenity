/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6464022
 * @summary Memory leak in JOptionPane.createDialog
 * @author Pavel Porvatov
 * @library ../../regtesthelpers
 * @build Util
 * @run main/othervm -mx128m bug6464022
 */

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class bug6464022 {
    private static JOptionPane pane;

    public static void main(String[] args) throws Exception {
        final List<WeakReference<JDialog>> references = new ArrayList<WeakReference<JDialog>>();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                pane = new JOptionPane(null, JOptionPane.UNDEFINED_CONDITION);

                for (int i = 0; i < 10; i++) {
                    JDialog dialog = pane.createDialog(null, "Test " + i);

                    references.add(new WeakReference<JDialog>(dialog));

                    dialog.dispose();

                    System.out.println("Disposing Dialog:" + dialog.hashCode());
                }
            }
        });

        Util.generateOOME();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                int allocatedCount = 0;

                for (WeakReference<JDialog> ref : references) {
                    if (ref.get() != null) {
                        allocatedCount++;

                        System.out.println(ref.get().hashCode() + " is still allocated");
                    }
                }

                if (allocatedCount > 0) {
                    throw new RuntimeException("Some dialogs still exist in memory. Test failed");
                } else {
                    System.out.println("All dialogs were GCed. Test passed.");
                }
            }
        });
    }
}
