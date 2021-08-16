/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8071707 6243376
 * @summary Test verifies that EXIF images with differing sampling factors
 *          are written correctly
 *
 * @run     main MagentaEXIFTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class MagentaEXIFTest {

    public static void main(final String[] argv) throws Exception {

        IIOMetadata jpegmetadata = null;
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
        jpegmetadata = createJPEGMetadata(jpgWriter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream output = ImageIO.createImageOutputStream(baos);
        jpgWriter.setOutput(output);
        int w=100, h=100;
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, w, h);
        IIOImage image = new IIOImage(bi, null, jpegmetadata);
        jpgWriter.write(null, image, null);
        jpgWriter.dispose();

        baos.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ImageInputStream iis = ImageIO.createImageInputStream(bais);
        bi = ImageIO.read(iis);
        for (int i=0; i<bi.getWidth(); i++) {
            for(int j=0; j<bi.getHeight(); j++) {
               if (bi.getRGB(i, j) != Color.white.getRGB()) {
                   throw new RuntimeException("Wrong color : " + Integer.toHexString(bi.getRGB(i, j)));
               }
            }
        }

    }


    static void displayMetadata(Node node, int level) {
        for (int i = 0; i < level; i++) System.out.print("  ");
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
        if (child != null) {
            System.out.println(">"); // close current tag
            while (child != null) { // emit child tags recursively
                displayMetadata(child, level + 1);
                child = child.getNextSibling();
            }
            for (int i = 0; i < level; i++) System.out.print("  ");
            System.out.println("</" + node.getNodeName() + ">");
        } else {
            System.out.println("/>");
        }
    }

    /*
     * Construct a JPEG IIOMetadata that has had the JFIF marker removed and
     * an APP1 EXIF marker added, and further massaged so that we have differing
     * horizontal and vertical sampling factors for one channel.
     */
    static IIOMetadata createJPEGMetadata(ImageWriter iw) throws IIOInvalidTreeException {
        String jpegMDName = "javax_imageio_jpeg_image_1.0";
        ImageWriter imgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ImageTypeSpecifier ist = new ImageTypeSpecifier(bi);
        IIOMetadata metadata = imgWriter.getDefaultImageMetadata(ist, null);

        IIOMetadataNode root = new IIOMetadataNode(jpegMDName);
        IIOMetadataNode header = new IIOMetadataNode("JPEGvariety");
        IIOMetadataNode sequence = new IIOMetadataNode("markerSequence");

        root.appendChild(header);
        root.appendChild(sequence);

        IIOMetadataNode app1 = new IIOMetadataNode("unknown");
        app1.setUserObject(new byte[255]);
        app1.setAttribute("MarkerTag", "255");
        sequence.appendChild(app1);

        IIOMetadataNode sof = new IIOMetadataNode("sof");
        sof.setAttribute("process", "0");
        sof.setAttribute("samplePrecision", "8");
        sof.setAttribute("numLines", "100");
        sof.setAttribute("samplesPerLine", "100");
        sof.setAttribute("numFrameComponents", "3");
        IIOMetadataNode c1 = new IIOMetadataNode("componentSpec");
        c1.setAttribute("componentId", "1");
        c1.setAttribute("HsamplingFactor", "1");
        c1.setAttribute("VsamplingFactor", "2");
        c1.setAttribute("QtableSelector", "1");
        sof.appendChild(c1);
        IIOMetadataNode c2 = new IIOMetadataNode("componentSpec");
        c2.setAttribute("componentId", "2");
        c2.setAttribute("HsamplingFactor", "1");
        c2.setAttribute("VsamplingFactor", "1");
        c2.setAttribute("QtableSelector", "1");
        sof.appendChild(c2);
        IIOMetadataNode c3 = new IIOMetadataNode("componentSpec");
        c3.setAttribute("componentId", "3");
        c3.setAttribute("HsamplingFactor", "1");
        c3.setAttribute("VsamplingFactor", "1");
        c3.setAttribute("QtableSelector", "1");
        sof.appendChild(c3);
        sequence.appendChild(sof);
        metadata.setFromTree(jpegMDName, root);
        IIOMetadata def = imgWriter.getDefaultImageMetadata(ist, null);
        metadata.mergeTree(jpegMDName, def.getAsTree(jpegMDName));
        Node tree = metadata.getAsTree(jpegMDName);
        Node variety = tree.getFirstChild();
        Node jfif = variety.getFirstChild();
        variety.removeChild(jfif);
        sequence = (IIOMetadataNode)tree.getLastChild();
        NodeList markers = sequence.getChildNodes();
        IIOMetadataNode n, sofNode=null;
        for (int i=0;i<markers.getLength();i++) {
           n = (IIOMetadataNode)markers.item(i);
           if (n.getNodeName().equals("sof")) {
                sofNode = n;
                break;
           }
        }
        IIOMetadataNode componentSpec = (IIOMetadataNode)sofNode.getFirstChild();
        Attr attr = componentSpec.getAttributeNode("HsamplingFactor");
        attr.setValue("1");
        attr = componentSpec.getAttributeNode("VsamplingFactor");
        attr.setValue("2");
        metadata.setFromTree(jpegMDName, tree);
        String[] names = metadata.getMetadataFormatNames();
        int length = names.length;
        for (int i = 0; i < length; i++) {
            System.out.println( "Format name: " + names[ i ] );
            displayMetadata(metadata.getAsTree(names[i]), 0);
        }

        return metadata;
    }
}
