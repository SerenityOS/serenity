/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6344646
 * @summary tests that WinNTFileSystem.hashCode() uses
 *    locale independent case mapping.
 */

import java.io.*;
import java.util.*;

public class bug6344646 {
    public static void main(String[] s) {
        Locale reservedLocale = Locale.getDefault();
        try {
            /* This test is only valid on win32 systems */
            if (File.separatorChar != '\\') {
                return;
            }

            Locale.setDefault(new Locale("lt"));
            File f1 = new File("J\u0301");
            File f2 = new File("j\u0301");

            if (f1.hashCode() != f2.hashCode()) {
                throw new RuntimeException("File.hashCode() for \"J\u0301\" " +
                        "and \"j\u0301\" should be the same");
            }
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }
}
