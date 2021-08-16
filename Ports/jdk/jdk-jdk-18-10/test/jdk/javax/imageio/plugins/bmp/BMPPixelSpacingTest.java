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
 * @bug     7182758
 * @summary Test verifies whether we are getting correct Horizontal
 *          & Vertical Physical pixel spacing for active BMP image
 *          through stored metadata or not.
 * @run     main BMPPixelSpacingTest
 */

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BMPPixelSpacingTest {

    public static void main(String[] args) throws Exception {
        // Header contaning X & Y pixels-per-meter more than value 1
        byte[] bmpHeaderData = { (byte) 0x42, (byte) 0x4d, (byte) 0x7e,
            (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x3e, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff };

        ImageInputStream imageInput = ImageIO.
            createImageInputStream(new ByteArrayInputStream(bmpHeaderData));

        for (Iterator<ImageReader> it = ImageIO.getImageReaders(imageInput);
            it.hasNext(); ) {
            ImageReader reader = it.next();
            reader.setInput(imageInput);
            IIOMetadata metadata = reader.getImageMetadata(0);

            Node rootNode = metadata.getAsTree("javax_imageio_1.0");
            NodeList nl = rootNode.getChildNodes();

            //Parse until you get Dimension child node
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if ((node.getNodeName()).equals("Dimension")) {
                    //get childnode list under Dimension node
                    NodeList cl = node.getChildNodes();
                    //Corresponding node indices under Dimension node
                    int horizontalNodeIndex = 1;
                    int verticalNodeIndex = 2;
                    Node horizontalNode = cl.item(horizontalNodeIndex);
                    Node verticalNode = cl.item(verticalNodeIndex);

                    //get attributes for horizontal and vertical nodes
                    NamedNodeMap horizontalAttr = horizontalNode.
                        getAttributes();
                    NamedNodeMap verticalAttr = verticalNode.getAttributes();

                    //since they have only one attribute index is 0
                    int attributeIndex = 0;
                    Node horizontalValue = horizontalAttr.item(attributeIndex);
                    Node verticalValue = verticalAttr.item(attributeIndex);
                    float horizontalNodeValue = Float.
                        parseFloat((horizontalValue.getNodeValue()));
                    float verticalNodeValue = Float.
                        parseFloat((verticalValue.getNodeValue()));

                    float expectedHorizontalValue, expectedVerticalValue;
                    // in test metadata xPixelsPerMeter & yPixelsPerMeter is 2
                    expectedHorizontalValue = expectedVerticalValue =
                        1000.0F / 2;
                    //expected and returned values should be same
                    if ((Float.compare(horizontalNodeValue,
                        expectedHorizontalValue) != 0) ||
                        (Float.compare(verticalNodeValue,
                        expectedVerticalValue) != 0)) {
                        throw new RuntimeException("Invalid pixel spacing");
                    }
                }
            }
    }
    }
}
