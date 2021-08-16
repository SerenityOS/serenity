/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Vector;

/*
 * @test
 * @key headful
 * @summary To check Whether java.awt.Image can be transferred through the
 *          system clipboard
 * @author Jitender(jitender.singh@eng.sun.com) area=AWT
 * @author dmitriy.ermashov@oracle.com
 * @library /lib/client
 * @build ExtendedRobot
 * @run main ImageTransferTest
 */

public class ImageTransferTest  {

    TestFrame frame1, frame2;
    Image image1, image2;
    DataFlavor imageFlavor;

    public static void main(String[] args) throws Exception {
        new ImageTransferTest().doTest();
    }

    class TestFrame extends Frame {
        boolean show;
        Image img;
        public TestFrame(boolean show, Image img){
            this.show = show;
            this.img = img;
        }

        public void paint( Graphics g ){
            if (show)
                g.drawImage( this.img, 30, 30, this );
        }
    }

    class TransferableObject implements Transferable, ClipboardOwner {

        Object data;
        DataFlavor[] dfs;

        public TransferableObject(Object data) throws Exception {
            Vector v = new Vector();
            imageFlavor = new DataFlavor("image/x-java-Image;class=java.awt.Image");
            imageFlavor.setHumanPresentableName("Java Image Flavor Object");

            if( data instanceof java.awt.Image )
                v.addElement(imageFlavor);

            dfs = new DataFlavor[v.size()];
            v.copyInto(dfs);

            for (int j = 0; j < dfs.length; j++)
                System.out.println(" in constructor flavor : " + dfs[j]);

            this.data = data;
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException,IOException {

            System.out.println(" **************");
            System.out.println(" The flavor passed to retrive the data :-" + flavor);
            System.out.println(" The flavors supported");
            for (int j = 0; j < dfs.length; j++)
                System.out.println(" jitu flavor : " + dfs[j]);

            System.out.println(" **************");

            if(!isDataFlavorSupported(flavor))
                throw new UnsupportedFlavorException(flavor);

            if(imageFlavor.equals(flavor)){
                if( data instanceof java.awt.Image )
                    return data;
            }
            return null;

        }

        public DataFlavor[] getTransferDataFlavors() { return dfs; }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            for (int i = 0 ; i < dfs.length; i++)
                if (dfs[i].match(flavor)) return true;
            return false;
        }

        public void lostOwnership(Clipboard clip,Transferable contents) {
            System.out.println(" LostOwnership is invoked");
        }
    }

    ImageTransferTest() throws Exception {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.RED);
        g.fillRect(20, 20, 60, 60);
        g.dispose();

        image1 = img;
        frame1 = new TestFrame(true, image1);
        frame2 = new TestFrame(false, null);

        System.out.println("Inside contrucor...");

        imageFlavor = new DataFlavor("image/x-java-Image;class=java.awt.Image");
        imageFlavor.setHumanPresentableName("Java Image Flavor Object");

        frame1.setBounds(10,350,200,200);
        frame2.setBounds(250,350,200,200);
        frame1.setVisible(true);
        frame2.setVisible(true);
        frame1.repaint();
    }

    public void compareImages() {
        if (image2 == image1)
            System.out.println("Pasted Image is same as that one copied !");
        else
            if (image2 != image1)
                throw new RuntimeException("Image retreived from the Clipboard is not the same " +
                        "image that was set to the Clipboard");
    }


    public void doTest() throws Exception {
        ExtendedRobot robot = new ExtendedRobot();
        robot.waitForIdle(2000);
        TransferableObject imagedata = new TransferableObject(image1);

        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        System.out.println("copying image...");
        if (imagedata != null) {
            clip.setContents(imagedata, imagedata);
        } else {
            System.out.println("Transferable object is null");
        }

        Transferable content = clip.getContents(this);
        if (content != null && (content.isDataFlavorSupported(imageFlavor))) {
            System.out.println("jitu after paste" + content);
            image2 = (Image) content.getTransferData(imageFlavor);
        } else {
            System.out.println("Contents of the clipboard is null");
        }

        frame2.img = image2;
        frame2.show = true;
        frame2.repaint();

        robot.waitForIdle(2000);

        compareImages();
    }
}

