/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8182041
 * @summary Tests if the files(Shortcut Panle files, FileChooser ComboBox files)
 * are filtered out when run with SecurityManager enabled.
 * @run main/othervm/policy=shellfolderqueries.policy ShellFolderQueriesSecurityManagerTest
 */

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.Arrays;

public class ShellFolderQueriesSecurityManagerTest {
    static final FileSystemView fsv = FileSystemView.getFileSystemView();

    public static void main(String[] args) throws Exception {
        try {
            File[] shortcuts = fsv.getChooserShortcutPanelFiles();
            Arrays.asList(shortcuts).forEach(System.out::println);

            if (shortcuts.length != 0) {
                throw new RuntimeException("Shortcut panel files leaked from SecurityManager.");
            }

            File[] cbFiles = fsv.getChooserComboBoxFiles();
            Arrays.asList(cbFiles).forEach(System.out::println);
            if (cbFiles.length != 0) {
                throw new RuntimeException("Combobox Files leaked from SecurityManager.");
            }

            System.out.println("ok");
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
