/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;

import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

/**
 * @test
 * @bug 8175968 8198342
 * @summary FileSystemView should clean listeners in UIManager before removal
 * @library /javax/swing/regtesthelpers
 * @build Util
 * @run main/othervm -Xmx8m -Djava.awt.headless=true FileSystemViewListenerLeak
*/
public final class FileSystemViewListenerLeak {

    public static void main(final String[] args) {
        checkListenersCount();
        new CustomFileSystemView();
        Util.generateOOME();
        checkListenersCount();
    }

    private static void checkListenersCount() {
        int length = UIManager.getPropertyChangeListeners().length;
        if (length != 0) {
            throw new RuntimeException("The count of listeners is: " + length);
        }
    }

    private static final class CustomFileSystemView extends FileSystemView {

        public File createNewFolder(File containingDir) throws IOException {
            return null;
        }
    }
}
