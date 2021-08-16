/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 7075105
 * @summary WIN: Provide a way to format HTML on drop
 * @author Denis Fokin: area=datatransfer
 * @requires (os.family == "windows")
 * @library /test/lib
 * @build HtmlTransferable PutAllHtmlFlavorsOnClipboard
 * @build PutOnlyAllHtmlFlavorOnClipboard PutSelectionAndFragmentHtmlFlavorsOnClipboard
 * @build jdk.test.lib.Platform
 * @run main HTMLDataFlavorTest
 */

import jdk.test.lib.Platform;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.HashMap;

public class HTMLDataFlavorTest {

    private static HashMap<DataFlavor, String> dataFlavors = new HashMap<DataFlavor, String>();


    public static void main(String[] args) throws IOException, UnsupportedFlavorException {

        if (!Platform.isWindows()) {
            System.err.println("This test is for MS Windows only. Considered passed.");
            return;
        }

        dataFlavors.put(DataFlavor.allHtmlFlavor, HtmlTransferable.ALL_HTML_AS_STRING);
        dataFlavors.put(DataFlavor.fragmentHtmlFlavor, HtmlTransferable.FRAGMENT_HTML_AS_STRING);
        dataFlavors.put(DataFlavor.selectionHtmlFlavor, HtmlTransferable.SELECTION_HTML_AS_STRING);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        resetClipboardContent(clipboard);

        // 1. Put all three html flavors on clipboard.
        //    Get the data within the same JVM
        //    Expect that the resulted html is the selection
        //    wrapped in all three types

        clipboard.setContents(new HtmlTransferable(HtmlTransferable.htmlDataFlavors),null);

        // Test local transfer
        testClipboardContent(clipboard, HtmlTransferable.htmlDataFlavors);

        resetClipboardContent(clipboard);

        // 2. Put only DataFlavor.allHtmlFlavor on clipboard.
        //    Expect that the resulted html is the all
        //    wrapped in all three types

        putHtmlInAnotherProcess("PutOnlyAllHtmlFlavorOnClipboard");

        for (DataFlavor df : HtmlTransferable.htmlDataFlavors) {
            if (!clipboard.isDataFlavorAvailable(df)) {
                throw new RuntimeException("The data should be available.");
            }
        }

        if (!clipboard.getData(DataFlavor.allHtmlFlavor).toString().
                equals(dataFlavors.get(DataFlavor.allHtmlFlavor).toString()))
        {
            throw new RuntimeException("DataFlavor.allHtmlFlavor data " +
                    "should be identical to the data put on the source side.");
        }

        resetClipboardContent(clipboard);

        // 3. Put all three html flavors on clipboard.
        //    Expect that the resulted html is the selection
        //    wrapped in all three types

        putHtmlInAnotherProcess("PutAllHtmlFlavorsOnClipboard");

        for (DataFlavor df : HtmlTransferable.htmlDataFlavors) {
            if (!clipboard.isDataFlavorAvailable(df)) {
                throw new RuntimeException("The data should be available.");
            }
        }

        if (!clipboard.getData(DataFlavor.selectionHtmlFlavor).toString().
                equals(dataFlavors.get(DataFlavor.selectionHtmlFlavor)))
        {
            throw new RuntimeException("DataFlavor.allHtmlFlavor data " +
                    "should be identical to the data put on the source side.");
        }

    }

    private static void resetClipboardContent(Clipboard clipboard) {
        clipboard.setContents(
                new StringSelection("The data is used to empty the clipboard content"
                ),null);
    }


    private static void putHtmlInAnotherProcess(String putterCommand) {
        try {

            String command = System.getProperty("java.home") + "/bin/java -cp " +
                    System.getProperty("test.classes", ".") + " "  +
                    putterCommand;

            System.out.println("Execute process : " + command);

            Process p = Runtime.getRuntime().exec(command);

            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("The data has been set remotely");

            try (BufferedReader stdstr = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String s;
                while ((s = stdstr.readLine()) != null) {
                    s = stdstr.readLine();
                    System.out.println(s);
                }
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String s;
                while ((s = br.readLine()) != null) {
                    s = br.readLine();
                    System.err.println(s);
                }
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testClipboardContent(Clipboard clipboard,
                                             DataFlavor [] expectedDataFlavors)
            throws UnsupportedFlavorException, IOException {

        for (DataFlavor df : clipboard.getAvailableDataFlavors()) {
            System.out.println("available df: " + df.getMimeType());
        }

        for (DataFlavor df : expectedDataFlavors) {

            if (!clipboard.isDataFlavorAvailable(df)) {
                throw new RuntimeException("The data should be available.");
            }


            System.out.println("Checking \"" + df.getParameter("document") + "\" for correspondence");

            if (!dataFlavors.get(df).toString().equals(clipboard.getData(df).toString())) {

                System.err.println("Expected data: " + dataFlavors.get(df).toString());
                System.err.println("Actual data: " + clipboard.getData(df).toString());


                throw new RuntimeException("An html flavor with parameter \"" +
                        df.getParameter("document") + "\" does not correspond to the transferred data.");


            }
        }
    }


}
