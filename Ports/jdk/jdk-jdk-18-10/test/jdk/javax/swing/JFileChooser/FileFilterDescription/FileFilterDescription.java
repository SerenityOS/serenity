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

import java.applet.Applet;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

public final class FileFilterDescription extends Applet {

    @Override
    public void init() {
    }

    @Override
    public void start() {
        try {
            test();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void test() throws Exception {
        final UIManager.LookAndFeelInfo[] infos = UIManager
                .getInstalledLookAndFeels();
        for (final UIManager.LookAndFeelInfo info : infos) {
            SwingUtilities.invokeAndWait(() -> {
                final JFileChooser chooser = new JFileChooser();
                setLookAndFeel(info);
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setFileFilter(new CustomFileFilter());
                SwingUtilities.updateComponentTreeUI(chooser);
                chooser.showDialog(null, "Open");
            });
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo info) {
        try {
            UIManager.setLookAndFeel(info.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class CustomFileFilter extends FileFilter {

        @Override
        public boolean accept(final File f) {
            return false;
        }

        @Override
        public String getDescription() {
            return "CustomFileFilter";
        }
    }
}
