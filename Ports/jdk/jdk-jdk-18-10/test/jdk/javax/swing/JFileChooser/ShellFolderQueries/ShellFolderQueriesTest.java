/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8081722 8182041
 * @summary Provide public API for file hierarchy provided by
 * sun.awt.shell.ShellFolder
 * @author Semyon Sadetsky
 * @run main ShellFolderQueriesTest
 */


import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShellFolderQueriesTest {
    static final String HOME = System.getProperty("user.home");
    static final FileSystemView fsv = FileSystemView.getFileSystemView();


    static String scriptBeg =
            "set WshShell = WScript.CreateObject(\"WScript.Shell\")\n" +
            "set oShellLink = WshShell.CreateShortcut(\"shortcut.lnk\")\n" +
            "oShellLink.TargetPath = \"";
    static String scriptEnd = "\"\noShellLink.WindowStyle = 1\noShellLink.Save";

    public static void main(String[] args) throws Exception {
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.out.println("Windows detected: will run shortcut test");
            testGet();
            testLink();
            testShortcutPanelFiles();
        } else {
            testGet();
        }
        System.out.println("ok");
    }

    private static void testLink() throws IOException, InterruptedException {
        // Create and execute VBS script to create a link
        File file = createVbsScript(scriptBeg + HOME + scriptEnd);
        Runtime.getRuntime().exec("cscript " + file.getName(), null,
                file.getParentFile()).waitFor();
        file.delete();

        File link = new File(file.getParentFile(), "shortcut.lnk");
        if (!fsv.isLink(link)) {
            link.delete();
            throw new RuntimeException("Link is not detected");
        }

        File location = fsv.getLinkLocation(link);
        if (!location.getAbsolutePath().equals(HOME)) {
            link.delete();
            throw new RuntimeException("Link location " + location +
                    " is wrong");
        }
        link.delete();


        link = File.createTempFile("test", ".tst");

        if (fsv.isLink(link)) {
            link.delete();
            throw new RuntimeException("File is not a link");
        }

        try {
            location = fsv.getLinkLocation(link);
            if (location != null) {
                link.delete();
                throw new RuntimeException("Not a link, should return null");
            }
        }
        catch (FileNotFoundException e) {
        }
        link.delete();
    }

    private static File createVbsScript(String script) throws IOException {
        File file = File.createTempFile("test", ".vbs");
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(script.getBytes());
        fos.close();
        return file;
    }

    private static void testGet() {
        File[] files = fsv.getChooserComboBoxFiles();
        for (File file : files) {
            if (fsv.isLink(file)) {
                throw new RuntimeException(
                        "Link shouldn't be in FileChooser combobox, "
                                + file.getPath());
            }
        }
    }

    private static void testShortcutPanelFiles() {
        File[] shortcuts = fsv.getChooserShortcutPanelFiles();
        if (shortcuts.length == 0) {
            throw new RuntimeException("No shortcut panel files found.");
        }
    }
}
