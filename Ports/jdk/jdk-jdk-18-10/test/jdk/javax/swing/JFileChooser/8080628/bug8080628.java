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

import java.util.Locale;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import sun.swing.SwingUtilities2;

/*
 * @test
 * @bug 8080628
 * @summary No mnemonics on Open and Save buttons in JFileChooser.
 * @author Alexey Ivanov
 * @modules java.desktop/sun.swing
 * @run main bug8080628
 */
public class bug8080628 {
    public static final String[] MNEMONIC_KEYS = new String[] {
            "FileChooser.saveButtonMnemonic",
            "FileChooser.openButtonMnemonic",
            "FileChooser.cancelButtonMnemonic",
            "FileChooser.directoryOpenButtonMnemonic"
    };

    public static final Locale[] LOCALES = new Locale[] {
            new Locale("en"),
            new Locale("de"),
            new Locale("es"),
            new Locale("fr"),
            new Locale("it"),
            new Locale("ja"),
            new Locale("ko"),
            new Locale("pt", "BR"),
            new Locale("sv"),
            new Locale("zh", "CN"),
            new Locale("zh", "TW")
    };

    private static volatile Exception exception;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                runTest();
            }
        });

        if (exception != null) {
            throw exception;
        }
    }

    private static void runTest() {
        try {
            LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
            for (LookAndFeelInfo info : lafInfo) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                } catch (final UnsupportedLookAndFeelException ignored) {
                    continue;
                }

                for (Locale locale : LOCALES) {
                    for (String key : MNEMONIC_KEYS) {
                        int mnemonic = SwingUtilities2.getUIDefaultsInt(key, locale);
                        if (mnemonic != 0) {
                            throw new RuntimeException("No mnemonic expected (" + mnemonic + ") " +
                                    "for '" + key + "' " +
                                    "in locale '" + locale + "' " +
                                    "in Look-and-Feel '"
                                        + UIManager.getLookAndFeel().getClass().getName() + "'");
                        }
                    }
                }
            }
            System.out.println("Test passed");
        } catch (Exception e) {
            exception = e;
        }
    }

}
