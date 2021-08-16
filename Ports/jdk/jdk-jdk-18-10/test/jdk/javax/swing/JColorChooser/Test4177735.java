/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4177735
 * @summary Tests that JColorChooser leaves no threads when disposed
 * @author Shannon Hickey
 */

import java.awt.Point;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.colorchooser.AbstractColorChooserPanel;

public class Test4177735 implements Runnable {
    private static final long DELAY = 1000L;

    public static void main(String[] args) throws Exception {
        int hsvIndex = 0;
        int panelsLength;
        int finalIndex;
        JColorChooser chooser = new JColorChooser();
        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        panelsLength = panels.length;

        for(int i = 0; i < panelsLength; i++) {
            if(panels[i].getDisplayName().equals("HSV")) {
                hsvIndex = i;
            }
        }
        finalIndex = Math.min(hsvIndex, panelsLength - 1);
        chooser.setChooserPanels(new AbstractColorChooserPanel[] { panels[finalIndex] });

        JDialog dialog = show(chooser);
        pause(DELAY);

        dialog.dispose();
        pause(DELAY);

        Test4177735 test = new Test4177735();
        SwingUtilities.invokeAndWait(test);
        if (test.count != 0) {
            throw new Error("JColorChooser leaves " + test.count + " threads running");
        }
    }

    static JDialog show(JColorChooser chooser) {
        JDialog dialog = JColorChooser.createDialog(null, null, false, chooser, null, null);
        dialog.setVisible(true);
        // block till displayed
        Point point = null;
        while (point == null) {
            try {
                point = dialog.getLocationOnScreen();
            }
            catch (IllegalStateException exception) {
                pause(DELAY);
            }
        }
        return dialog;
    }

    private static void pause(long delay) {
        try {
            Thread.sleep(delay);
        }
        catch (InterruptedException exception) {
        }
    }

    private int count;

    public void run() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        Thread[] threads = new Thread[group.activeCount()];
        int count = group.enumerate(threads, false);
        for (int i = 0; i < count; i++) {
            String name = threads[i].getName();
            if ("SyntheticImageGenerator".equals(name)) { // NON-NLS: thread name
                this.count++;
            }
        }
    }
}
