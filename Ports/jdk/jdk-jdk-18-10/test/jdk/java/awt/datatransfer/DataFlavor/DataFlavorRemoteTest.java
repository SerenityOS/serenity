/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 8051636
  @summary DataTransferer optional dependency on RMI
  @author Semyon Sadetsky
  @library ../../regtesthelpers/process
  @build ProcessResults ProcessCommunicator
  @run main DataFlavorRemoteTest
*/

import test.java.awt.regtesthelpers.process.ProcessCommunicator;
import test.java.awt.regtesthelpers.process.ProcessResults;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serializable;

interface Hello extends java.rmi.Remote {
    String sayHello();
}

public class DataFlavorRemoteTest {

    public static void main(String[] args) throws Exception {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Producer contents = new Producer();
        clipboard.setContents(contents, null);
        ProcessResults processResults =
                ProcessCommunicator
                        .executeChildProcess(Consumer.class, new String[0]);
        if (!"Hello".equals(processResults.getStdOut())) {
            throw new RuntimeException("transfer of remote object failed");
        }
        System.out.println("ok");
    }

    static class Consumer {
        public static void main(String[] args) throws Exception {
            Clipboard clipboard =
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor dataFlavor = new DataFlavor(DataFlavor.javaRemoteObjectMimeType +
                    ";class=Hello" );
            Object data = clipboard.getData(dataFlavor);
            System.out.print(((Hello) data).sayHello());
        }

    }
}

class Producer implements Transferable {

    private final DataFlavor dataFlavor;
    private final HelloImpl impl;

    private static class HelloImpl implements Hello, Serializable {
        @Override
        public String sayHello() {
            return "Hello";
        }
    }

    public Producer() throws Exception {
        dataFlavor = new DataFlavor(DataFlavor.javaRemoteObjectMimeType +
                ";class=Hello" );
        impl = new HelloImpl();
        System.out.println(impl.hashCode());
    }

    Hello getImpl() {
        return impl;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{dataFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(dataFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        return impl;
    }

}
