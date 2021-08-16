/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4895512
 * @summary Test that WBMPImageWriter return non-null default image metadata
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class WbmpDefaultImageMetadataTest {
    ImageWriter writer = null;
    IIOMetadata imageData = null;
    ImageWriteParam writeParam = null;
    BufferedImage bimg = null;

    public WbmpDefaultImageMetadataTest(String format) {
        try {
            bimg = new BufferedImage(200, 200, bimg.TYPE_INT_RGB);
            Graphics gg = bimg.getGraphics();
            gg.setColor(Color.red);
            gg.fillRect(50, 50, 100, 100);

            Iterator it = ImageIO.getImageWritersByFormatName(format);
            if (it.hasNext()) {
                writer = (ImageWriter) it.next();
            }
            if (writer == null) {
                throw new RuntimeException("No writer available for the given format."
                                           + " Test failed.");
            }
            writeParam = writer.getDefaultWriteParam();

            System.out.println("Testing Image Metadata for "+format+"\n");
            imageData = writer.getDefaultImageMetadata(new ImageTypeSpecifier(bimg), writeParam);
            if (imageData == null) {
                System.out.println("return value is null. No default image metadata is associated with "+format+" writer");
                throw new RuntimeException("Default image metadata is null."
                                           + " Test failed.");
            }
            int j = 0;
            String imageDataNames[] = null;
            if(imageData != null) {
                System.out.println("Is standard metadata format supported (Image) ? "+
                                   imageData.isStandardMetadataFormatSupported() );
                imageDataNames = imageData.getMetadataFormatNames();
                System.out.println("\nAll supported Metadata Format Names\n");
                if(imageDataNames!=null){
                    for(j=0; j<imageDataNames.length; j++)  {
                        System.out.println("FORMAT NAME: "+imageDataNames[j]);
                        if (imageDataNames[j].equals(imageData.getNativeMetadataFormatName())) {
                            System.out.println("This is a Native Metadata format\n");
                        } else {
                            System.out.println("\n");
                        }
                        System.out.println("");
                        System.out.println("IIOImageMetadata DOM tree for "+imageDataNames[j]);
                        System.out.println("");
                        Node imageNode = imageData.getAsTree(imageDataNames[j]);
                        displayMetadata(imageNode);
                        System.out.println("\n\n");
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException("Exception was thrown."
                                       + " Test failed.");
        }
    }

    public void displayMetadata(Node root) {
        displayMetadata(root, 0);
    }

    void indent(int level) {
        for (int i = 0; i < level; i++) {
            System.out.print(" ");
        }
    }

    void displayMetadata(Node node, int level) {
        indent(level); // emit open tag
        System.out.print("<" + node.getNodeName());
        NamedNodeMap map = node.getAttributes();
        if (map != null) { // print attribute values
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                System.out.print(" " + attr.getNodeName() +
                                 "=\"" + attr.getNodeValue() + "\"");
            }
        }
        Node child = node.getFirstChild();

        if (node.getNodeValue() != null && !node.getNodeValue().equals("") ) {
            System.out.println(">");
            indent(level);
            System.out.println(node.getNodeValue());
            indent(level); // emit close tag
            System.out.println("</" + node.getNodeName() + ">");
        } else  if (child != null) {
            System.out.println(">"); // close current tag
            while (child != null) { // emit child tags recursively
                displayMetadata(child, level + 1);
                child = child.getNextSibling();
            }
            indent(level); // emit close tag
            System.out.println("</" + node.getNodeName() + ">");
        } else {
            System.out.println("/>");
        }
    }

    public static void main(String args[]) {
        WbmpDefaultImageMetadataTest test =
            new WbmpDefaultImageMetadataTest("wbmp");
    }
}
