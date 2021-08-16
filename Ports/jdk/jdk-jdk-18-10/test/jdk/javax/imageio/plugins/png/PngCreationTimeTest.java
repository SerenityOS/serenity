/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164971 8187113
 * @summary The test decodes a png file and checks if the metadata contains
 *          image creation time. In addition, the test also merges the custom
 *          metadata tree (both standard and native) and succeeds when the
 *          metadata contains expected image creation time.
 * @run main PngCreationTimeTest
 */
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

public class PngCreationTimeTest {
    // Members
    private static IIOMetadata pngMetadata = null;

    public static void initializeTest() throws IOException {
        Iterator<ImageReader> iterR = null;
        ImageReader pngImageReader = null;
        BufferedImage decImage = null;
        ImageInputStream imageStream = null;
        String fileName = "duke.png";
        String separator = System.getProperty("file.separator");
        String dirPath = System.getProperty("test.src", ".");
        String filePath = dirPath + separator + fileName;
        File file = null;

        try {
            // Open the required file and check if file exists.
            file = new File(filePath);
            if (file != null && !file.exists()) {
                reportExceptionAndFail("Test Failed. Required image file was"
                        + " not found.");
            }

            // Get PNG image reader
            iterR = ImageIO.getImageReadersBySuffix("PNG");
            if (iterR.hasNext()) {
                pngImageReader = iterR.next();
                ImageReadParam param = pngImageReader.getDefaultReadParam();
                imageStream = ImageIO.createImageInputStream(file);
                if (imageStream != null) {
                    // Last argument informs reader not to ignore metadata
                    pngImageReader.setInput(imageStream,
                                            false,
                                            false);
                    decImage = pngImageReader.read(0, param);
                    pngMetadata = pngImageReader.getImageMetadata(0);
                    if (pngMetadata != null) {
                        // Check if the metadata contains creation time
                        testImageMetadata(pngMetadata);
                    } else {
                        reportExceptionAndFail("Test Failed. Reader could not"
                                + " generate image metadata.");
                    }
                } else {
                    reportExceptionAndFail("Test Failed. Could not initialize"
                            + " image input stream.");
                }
            } else {
                reportExceptionAndFail("Test Failed. Required image reader"
                        + " was not found.");
            }
        } finally {
            // Release ther resources
            if (imageStream != null) {
                imageStream.close();
            }
            if (pngImageReader != null) {
                pngImageReader.dispose();
            }
        }
    }

    public static void testImageMetadata(IIOMetadata metadata) {
        /*
         * The source file contains Creation Time in its text chunk. Upon
         * successful decoding, the Standard/Document/ImageCreationTime
         * should exist in the metadata.
         */
        if (metadata != null) {
            Node keyNode = findNode(metadata.getAsTree("javax_imageio_1.0"),
                    "ImageCreationTime");
            if (keyNode == null) {
                reportExceptionAndFail("Test Failed: Could not find image"
                        + " creation time in the metadata.");
            }
        }
    }

    public static void testSaveCreationTime() throws IOException {
        File file = null;
        Iterator<ImageWriter> iterW = null;
        Iterator<ImageReader> iterR = null;
        ImageWriter pngImageWriter = null;
        ImageReader pngImageReader = null;
        ImageInputStream inputStream = null;
        ImageOutputStream outputStream = null;
        try {
            // Create a simple image and fill with a color
            int imageSize = 200;
            BufferedImage buffImage = new BufferedImage(imageSize, imageSize,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = buffImage.createGraphics();
            g2d.setColor(Color.red);
            g2d.fillRect(0, 0, imageSize, imageSize);

            // Create a temporary file for the output png image
            String fileName = "RoundTripTest";
            file = File.createTempFile(fileName, ".png");
            if (file == null) {
                reportExceptionAndFail("Test Failed. Could not create a"
                        + " temporary file for round trip test.");
            }

            // Create a PNG writer and write test image with metadata
            iterW = ImageIO.getImageWritersBySuffix("PNG");
            if (iterW.hasNext()) {
                pngImageWriter = iterW.next();
                outputStream = ImageIO.createImageOutputStream(file);
                if (outputStream != null) {
                    pngImageWriter.setOutput(outputStream);

                    // Get the default metadata & add image creation time to it.
                    ImageTypeSpecifier imgType =
                            ImageTypeSpecifier.createFromRenderedImage(buffImage);
                    IIOMetadata metadata =
                            pngImageWriter.getDefaultImageMetadata(imgType, null);
                    IIOMetadataNode root = createStandardMetadataNodeTree();
                    metadata.mergeTree("javax_imageio_1.0", root);

                    // Write a png image using buffImage & metadata
                    IIOImage iioImage = new IIOImage(buffImage, null, metadata);
                    pngImageWriter.write(iioImage);
                } else {
                    reportExceptionAndFail("Test Failed. Could not initialize"
                            + " image output stream for round trip test.");
                }
            } else {
                reportExceptionAndFail("Test Failed. Could not find required"
                        + " image writer for the round trip test.");
            }

            // Create a PNG reader and check if metadata was written
            iterR = ImageIO.getImageReadersBySuffix("PNG");
            if (iterR.hasNext()) {
                pngImageReader = iterR.next();
                inputStream = ImageIO.createImageInputStream(file);
                if (inputStream != null) {
                    // Read the image and get the metadata
                    pngImageReader.setInput(inputStream, false, false);
                    pngImageReader.read(0);
                    IIOMetadata imgMetadata =
                            pngImageReader.getImageMetadata(0);

                    // Test if the metadata contains creation time.
                    testImageMetadata(imgMetadata);
                } else {
                    reportExceptionAndFail("Test Failed. Could not initialize"
                            + " image input stream for round trip test.");
                }
            } else {
                reportExceptionAndFail("Test Failed. Cound not find the"
                        + " required image reader.");
            }
        } finally {
            // Release the resources held
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (pngImageWriter != null) {
                pngImageWriter.dispose();
            }
            if (pngImageReader != null) {
                pngImageReader.dispose();
            }
            // Delete the temp file as well
            if (file != null) {
                Files.delete(file.toPath());
            }
        }
    }

    public static void reportExceptionAndFail(String message) {
        // A common method to report exception.
        throw new RuntimeException(message);
    }

    public static void testMergeNativeTree() {
        // Merge a custom native metadata tree and inspect creation time
        if (pngMetadata != null) {
            try {
                IIOMetadataNode root = createNativeMetadataNodeTree();

                /*
                 * Merge the native metadata tree created. The data should
                 * reflect in Standard/Document/ImageCreationTime Node
                 */
                pngMetadata.mergeTree("javax_imageio_png_1.0", root);
                Node keyNode = findNode(pngMetadata.getAsTree("javax_imageio_1.0"),
                        "ImageCreationTime");
                if (keyNode != null) {
                    // Query the attributes of the node and check for the value
                    NamedNodeMap attrMap = keyNode.getAttributes();
                    String attrValue = attrMap.getNamedItem("year")
                                              .getNodeValue();
                    int decYear = Integer.parseInt(attrValue);
                    if (decYear != 2014) {
                        // Throw exception. Incorrect year value observed
                        reportExceptionAndFail("Test Failed: Incorrect"
                                + " creation time value observed.");
                    }
                } else {
                    // Throw exception.
                    reportExceptionAndFail("Test Failed: Image creation"
                            + " time doesn't exist in metadata.");
                }
            } catch (IOException ex) {
                // Throw exception.
                reportExceptionAndFail("Test Failed: While executing"
                        + " mergeTree on metadata.");
            }
        }
    }

    public static void testMergeStandardTree() {
        // Merge a standard metadata tree and inspect creation time
        if (pngMetadata != null) {
            try {
                IIOMetadataNode root = createStandardMetadataNodeTree();

                /*
                 * Merge the standard metadata tree created. The data should
                 * correctly reflect in the native tree
                 */
                pngMetadata.mergeTree("javax_imageio_1.0", root);
                Node keyNode = findNode(pngMetadata.getAsTree("javax_imageio_png_1.0"),
                        "tEXtEntry");
                // Last text entry would contain the merged information
                while (keyNode != null && keyNode.getNextSibling() != null) {
                    keyNode = keyNode.getNextSibling();
                }

                if (keyNode != null) {
                    // Query the attributes of the node and check for the value
                    NamedNodeMap attrMap = keyNode.getAttributes();
                    String attrValue = attrMap.getNamedItem("value")
                                              .getNodeValue();
                    if (!attrValue.contains("2016")) {
                        // Throw exception. Incorrect year value observed
                        throw new RuntimeException("Test Failed: Incorrect"
                                + " creation time value observed.");
                    }
                } else {
                    // Throw exception.
                    reportExceptionAndFail("Test Failed: Image creation"
                            + " time doesn't exist in metadata.");
                }
            } catch (IOException ex) {
                // Throw exception.
                reportExceptionAndFail("Test Failed: While executing"
                        + " mergeTree on metadata.");
            }
        }
    }

    public static IIOMetadataNode createNativeMetadataNodeTree() {
        // Create a text node to hold tEXtEntries
        IIOMetadataNode tEXtNode = new IIOMetadataNode("tEXt");

        // Create tEXt entry to hold random date time
        IIOMetadataNode randomTimeEntry = new IIOMetadataNode("tEXtEntry");
        randomTimeEntry.setAttribute("keyword", "Creation Time");
        randomTimeEntry.setAttribute("value", "21 Dec 2015,Monday");
        tEXtNode.appendChild(randomTimeEntry);

        // Create a tEXt entry to hold time in RFC1123 format
        IIOMetadataNode rfcTextEntry = new IIOMetadataNode("tEXtEntry");
        rfcTextEntry.setAttribute("keyword", "Creation Time");
        rfcTextEntry.setAttribute("value", "Mon, 21 Dec 2015 09:04:30 +0530");
        tEXtNode.appendChild(rfcTextEntry);

        // Create a tEXt entry to hold time in ISO format
        IIOMetadataNode isoTextEntry = new IIOMetadataNode("tEXtEntry");
        isoTextEntry.setAttribute("keyword", "Creation Time");
        isoTextEntry.setAttribute("value", "2014-12-21T09:04:30+05:30");
        tEXtNode.appendChild(isoTextEntry);

        // Create a root node append the text node
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(tEXtNode);

        return root;
    }

    public static IIOMetadataNode createStandardMetadataNodeTree() {
        /*
         * Create standard metadata tree with creation time in
         * Standard(Root)/Document/ImageCreationTime node
         */
        IIOMetadataNode createTimeNode = new IIOMetadataNode("ImageCreationTime");
        createTimeNode.setAttribute("year", "2016");
        createTimeNode.setAttribute("month", "12");
        createTimeNode.setAttribute("day", "21");
        createTimeNode.setAttribute("hour", "18");
        createTimeNode.setAttribute("minute", "30");
        createTimeNode.setAttribute("second", "00");

        // Create the Document node
        IIOMetadataNode documentNode = new IIOMetadataNode("Document");
        documentNode.appendChild(createTimeNode);

        // Create a root node append the Document node
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(documentNode);

        return root;
    }

    public static Node findNode(Node root, String nodeName) {
        // Return value
        Node retVal = null;

        if (root != null ) {
            // Check if the name of root node matches the key
            String name = root.getNodeName();
            if (name.equalsIgnoreCase(nodeName)) {
                return root;
            }

            // Process all children
            Node node = root.getFirstChild();
            while (node != null) {
                retVal = findNode(node, nodeName);
                if (retVal != null ) {
                    // We found the node. Stop the search
                    break;
                }
                node = node.getNextSibling();
            }
       }

        return retVal;
    }

    public static void main(String[] args) throws IOException {
        /*
         * Initialize the test by decoding a PNG image that has creation
         * time in one of its text chunks and check if the metadata returned
         * contains image creation time.
         */
        initializeTest();

        /*
         * Test the round trip usecase. Write a PNG file with "Creation Time"
         * in text chunk and decode the same to check if the creation time
         * was indeed written to the PNG file.
         */
        testSaveCreationTime();

        /*
         * Modify the metadata by merging a standard metadata tree and inspect
         * the value in the native tree
         */
        testMergeNativeTree();

        /*
         * Modify the metadata by merging a native metadata tree and inspect
         * the value in the standard tree.
         */
        testMergeStandardTree();
    }
}
