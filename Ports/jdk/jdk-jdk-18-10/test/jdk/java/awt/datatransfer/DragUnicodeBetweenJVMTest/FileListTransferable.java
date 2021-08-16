/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.List;

class FileListTransferable implements Transferable {

    public static File [] files = new File [] {
        new File ("\u042f\u0020\u0441\u0440\u0430\u0437\u0443\u0020\u0441\u043c\u0430\u0437\u0430\u043b" +
                "\u0020\u043a\u0430\u0440\u0442\u0443\u0020\u0431\u0443\u0434\u043d\u044f"),
        new File ("\u043f\u043b\u0435\u0441\u043d\u0443\u0432\u0448\u0438\u0020\u043a\u0440\u0430\u0441" +
                "\u043a\u0443\u0020\u0438\u0437\u0020\u0441\u0442\u0430\u043a\u0430\u043d\u0430"),
        new File ("\u044f\u0020\u043f\u043e\u043a\u0430\u0437\u0430\u043b\u0020\u043d\u0430\u0020\u0431" +
                "\u043b\u044e\u0434\u0435\u0020\u0441\u0442\u0443\u0434\u043d\u044f"),
        new File ("\u043a\u043e\u0441\u044b\u0435\u0020\u0441\u043a\u0443\u043b\u044b\u0020\u043e\u043a" +
                "\u0435\u0430\u043d\u0430"),
        new File ("\u041d\u0430\u0020\u0447\u0435\u0448\u0443\u0435\u0020\u0436\u0435\u0441\u0442\u044f" +
                "\u043d\u043e\u0439\u0020\u0440\u044b\u0431\u044b"),
        new File ("\u043f\u0440\u043e\u0447\u0435\u043b\u0020\u044f\u0020\u0437\u043e\u0432\u044b\u0020" +
                "\u043d\u043e\u0432\u044b\u0445\u0020\u0433\u0443\u0431"),
        new File ("\u0410\u0020\u0432\u044b"),
        new File ("\u043d\u043e\u043a\u0442\u044e\u0440\u043d\u0020\u0441\u044b\u0433\u0440\u0430\u0442" +
                "\u044c"),
        new File ("\u043c\u043e\u0433\u043b\u0438\u0020\u0431\u044b"),
        new File ("\u043d\u0430\u0020\u0444\u043b\u0435\u0439\u0442\u0435\u0020\u0432\u043e\u0434\u043e" +
                "\u0441\u0442\u043e\u0447\u043d\u044b\u0445\u0020\u0442\u0440\u0443\u0431"),
    };

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor [] {DataFlavor.javaFileListFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.javaFileListFlavor) ;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        List<File> list = Arrays.asList(files);
        return list;

    }
}
