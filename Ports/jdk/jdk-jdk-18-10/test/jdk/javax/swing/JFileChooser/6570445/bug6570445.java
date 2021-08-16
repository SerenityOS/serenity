/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6570445
 * @run main/othervm -Djava.security.manager=allow  bug6570445
 * @summary Checks if Win32ShellFolder2's COM-using methods work under a security manager
 * @author Leonid Popov
 */

import javax.swing.filechooser.FileSystemView;

public class bug6570445 {
    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());

        // The next line of code forces FileSystemView to request data from Win32ShellFolder2,
        // what causes an exception if a security manager installed (see the bug 6570445 description)
        FileSystemView.getFileSystemView().getRoots();

        System.out.println("Passed.");
    }
}
