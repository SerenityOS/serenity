/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8064934
 * @key headful
 * @requires (os.family == "windows")
 * @summary Incorrect Exception message from java.awt.Desktop.open()
 * @author Dmitry Markov
 * @library /test/lib
 * @modules java.desktop/sun.awt
 * @build jdk.test.lib.Platform
 * @run main bug8064934
 */
import jdk.test.lib.Platform;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class bug8064934 {
    private static final String NO_ASSOCIATION_ERROR_MESSAGE = "Error message: No application is associated with" +
            " the specified file for this operation.";

    public static void main(String[] args) {
        // This test is intended only for Windows
        if (!AccessController.doPrivileged((PrivilegedAction<Boolean>) Platform::isWindows)) {
            System.out.println("The test is for Windows platform only");
            return;
        }

        // Test whether Desktop is supported of not
        if (!Desktop.isDesktopSupported()) {
            System.out.println("Desktop is not supported");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        // Test whether open action is supported or not
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            System.out.println("Desktop.Action.OPEN is not supported");
            return;
        }

        File file = null;
        try {
            file = File.createTempFile("test", ".foo");
            if (!file.exists()) {
                throw new RuntimeException("Can not create temp file");
            }
            desktop.open(file);
        } catch (IOException ioe) {
            String errorMessage = ioe.getMessage().trim();
            if (errorMessage != null && !errorMessage.endsWith(NO_ASSOCIATION_ERROR_MESSAGE)) {
                throw new RuntimeException("Test FAILED! Wrong Error message: \n" +
                        "Actual " + errorMessage.substring(errorMessage.indexOf("Error message:")) + "\n" +
                        "Expected " + NO_ASSOCIATION_ERROR_MESSAGE);
            }
        } finally {
            if (file != null) {
                file.delete();
            }
        }

        System.out.println("Test PASSED!");
    }
}
