/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.awt.datatransfer.*;

/*
 * @test
 * @summary To test the basic Clipboard functions
 * @author Kanishk Jethi (kanishk.jethi@sun.com) area=Clipboard
 * @modules java.datatransfer
 * @run main BasicClipboardTest
 */

public class BasicClipboardTest implements ClipboardOwner {

    StringSelection strSelect = new StringSelection("Transferable String Selection");
    StringSelection strCheck;
    String clipName = "Test Clipboard";
    Clipboard clip = new Clipboard(clipName);
    DataFlavor dataFlavor, testDataFlavor ;
    DataFlavor dataFlavorArray[];
    Object testObject;
    String strTest = null;

    public static void main (String[] args) throws Exception {
        new BasicClipboardTest().doTest();
    }

    public void doTest() throws Exception {
        dataFlavor = new DataFlavor(DataFlavor.javaRemoteObjectMimeType, null, this.getClass().getClassLoader());
        // test for null return of selectBestTextFlavor if input is null or
        // of zero length
        testDataFlavor = DataFlavor.selectBestTextFlavor(dataFlavorArray);
        if (testDataFlavor != null)
            throw new RuntimeException("\n***Error in selectBestTextFlavor");

        dataFlavorArray = new DataFlavor[0];

        testDataFlavor = DataFlavor.selectBestTextFlavor(dataFlavorArray);
        if (testDataFlavor != null)
            throw new RuntimeException("\n***Error in selectBestTextFlavor");

        // test for null return when there are no text flavors in array
        dataFlavorArray = new DataFlavor[1];
        dataFlavorArray[0] = new DataFlavor(DataFlavor.javaSerializedObjectMimeType + ";class=java.io.Serializable");

        testDataFlavor = DataFlavor.selectBestTextFlavor(dataFlavorArray);
        if (testDataFlavor != null)
            throw new RuntimeException("\n***Error in selectBestTextFlavor");

        if (clip.getName() != clipName)
            throw new RuntimeException("\n*** Error in Clipboard.getName()");

        // set the owner of the clipboard to null to check branch coverage
        // of the setContents method
        clip.setContents(null, null);

        //set the owner of the clipboard to something valid to check branch
        //coverage of the setContents method
        clip.setContents(null, new BasicClipboardTest());

        //set the owner of the clipboard to this to check branch coverage
        // of the setContents method
        clip.setContents(null, this);

        //set the contents of the clipboard
        clip.setContents(strSelect, this);

        //get the contents of the clipboard
        strCheck = (StringSelection)clip.getContents(this);
        if (!strCheck.equals(strSelect))
            throw new RuntimeException("\n***The contents of the clipboard are "
            + "not the same as those that were set");

        //Check if getReaderForText throws IAE when the Transferable has
        //null TransferData
        dataFlavor = DataFlavor.stringFlavor;
        strSelect = new StringSelection(null);
        try {
            testObject = dataFlavor.getReaderForText(strSelect);
            throw new RuntimeException("\n***Error in getReaderForText. An IAE should have been thrown");
        } catch (IllegalArgumentException iae) {
            // do nothing as this is expected
        }

        //Check getParameter
        dataFlavor.setHumanPresentableName("String Flavor");
        if (!(dataFlavor.getParameter("humanPresentableName")).equals("String Flavor"))
            throw new RuntimeException("\n***Error in getParameter");

        //Check equals
        try {
            if (dataFlavor.isMimeTypeEqual(strTest))
                throw new RuntimeException("\n***Error in DataFlavor.equals(String s)");
        } catch (NullPointerException e) {
            //do nothing as it is expected
        }

        if (!(dataFlavor.isMimeTypeEqual(dataFlavor.getMimeType())))
            throw new RuntimeException("\n***Error in DataFlavor.equals(String s)");

        //Check isMimeTypeSerializedObject
        if (!dataFlavorArray[0].isMimeTypeSerializedObject())
            throw new RuntimeException("\n***Error in isMimeTypeSerializedObject()");
        System.out.println(dataFlavorArray[0].getDefaultRepresentationClass());
        System.out.println(dataFlavorArray[0].getDefaultRepresentationClassAsString());
        //Check isFlavorRemoteObjectType
        if (dataFlavor.isFlavorRemoteObjectType())
            System.out.println("The DataFlavor is a remote object type");

        //Check clone()
        testDataFlavor = (DataFlavor)dataFlavor.clone();
    }

    public void lostOwnership (Clipboard clipboard, Transferable contents) { }
}

