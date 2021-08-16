/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6198111
 * @summary Test verifies that PNG image reader correctly handles
 *          hIST chunk if length of image palette in not power of two.
 *
 * @run     main ShortHistogramTest 15
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ShortHistogramTest {

    public static void main(String[] args) throws IOException {
        int numColors = 15;
        if (args.length > 0) {
            try {
                numColors = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number of colors: " + args[0]);
            }
        }
        System.out.println("Test number of colors: " + numColors);

        ShortHistogramTest t = new ShortHistogramTest(numColors);
        t.doTest();
    }

    int numColors;

    public ShortHistogramTest(int numColors) {
        this.numColors = numColors;
    }

    public void doTest() throws IOException {
        BufferedImage bi = createTestImage(numColors);

        File f = writeImageWithHist(bi);
        System.out.println("Test file is " + f.getCanonicalPath());

        try {
            ImageIO.read(f);
        } catch (IOException e) {
            throw new RuntimeException("Test FAILED!", e);
        }
        System.out.println("Test PASSED!");
    }

    protected File writeImageWithHist(BufferedImage bi) throws IOException {
        File f = File.createTempFile("hist_", ".png", new File("."));

        ImageWriter writer = ImageIO.getImageWritersByFormatName("PNG").next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(f);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        ImageTypeSpecifier type = new ImageTypeSpecifier(bi);

        IIOMetadata imgMetadata = writer.getDefaultImageMetadata(type, param);

        /* add hIST node to image metadata */
        imgMetadata = upgradeMetadata(imgMetadata, bi);

        IIOImage iio_img = new IIOImage(bi,
                                        null, // no thumbnails
                                        imgMetadata);

        writer.write(iio_img);
        ios.flush();
        ios.close();
        return f;
    }

    private IIOMetadata upgradeMetadata(IIOMetadata src, BufferedImage bi) {
        String format = src.getNativeMetadataFormatName();
        System.out.println("Native format: " + format);
        Node root = src.getAsTree(format);

        // add hIST node
        Node n = lookupChildNode(root, "hIST");
        if (n == null) {
            System.out.println("Appending new hIST node...");
            Node hIST = gethISTNode(bi);
            root.appendChild(hIST);
        }

        System.out.println("Upgraded metadata tree:");
        dump(root, "");

        System.out.println("Merging metadata...");
        try {
            src.mergeTree(format, root);
        } catch (IIOInvalidTreeException e) {
            throw new RuntimeException("Test FAILED!", e);
        }
        return src;
    }

    private IIOMetadataNode gethISTNode(BufferedImage bi) {
        IndexColorModel icm = (IndexColorModel)bi.getColorModel();
        int mapSize = icm.getMapSize();

        int[] hist = new int[mapSize];
        Arrays.fill(hist, 0);

        Raster r = bi.getData();
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int s = r.getSample(x, y, 0);
                hist[s] ++;
            }
        }

        IIOMetadataNode hIST = new IIOMetadataNode("hIST");
        for (int i = 0; i < hist.length; i++) {
            IIOMetadataNode n = new IIOMetadataNode("hISTEntry");
            n.setAttribute("index", "" + i);
            n.setAttribute("value", "" + hist[i]);
            hIST.appendChild(n);
        }

        return hIST;
    }

    private static Node lookupChildNode(Node root, String name) {
        Node n = root.getFirstChild();
        while (n != null && !name.equals(n.getNodeName())) {
            n = n.getNextSibling();
        }
        return n;
    }

    private static void dump(Node node, String ident) {
        if (node == null) {
            return;
        }

        System.out.printf("%s%s\n", ident, node.getNodeName());

        // dump node attributes...
        NamedNodeMap attribs = node.getAttributes();
        if (attribs != null) {
            for (int i = 0; i < attribs.getLength(); i++) {
                Node a = attribs.item(i);
                System.out.printf("%s  %s: %s\n", ident,
                        a.getNodeName(), a.getNodeValue());
            }
        }
        // dump node children...
        dump(node.getFirstChild(), ident + "    ");

        dump(node.getNextSibling(), ident);
    }

    protected BufferedImage createTestImage(int numColors) {

        IndexColorModel icm = createTestICM(numColors);
        int w = numColors * 10;
        int h = 20;

        BufferedImage img = new BufferedImage(w, h,
                BufferedImage.TYPE_BYTE_INDEXED, icm);

        Graphics2D g = img.createGraphics();
        for (int i = 0; i < numColors; i++) {
            int rgb = icm.getRGB(i);
            //System.out.printf("pixel %d, rgb %x\n", i, rgb);
            g.setColor(new Color(rgb));
            g.fillRect(i * 10, 0, w - i * 10, h);
        }
        g.dispose();

       return img;
    }

    protected IndexColorModel createTestICM(int numColors) {
        int[] palette = createTestPalette(numColors);

        int numBits = getNumBits(numColors);

        IndexColorModel icm = new IndexColorModel(numBits, numColors,
                palette, 0, false, -1,
                DataBuffer.TYPE_BYTE);
        return icm;
    }

    protected static int getNumBits(int numColors) {
        if (numColors < 0 || 256 < numColors) {
            throw new RuntimeException("Unsupported number of colors: " +
                                       numColors);
        }

        int numBits = 1;
        int limit = 1 << numBits;
        while (numColors > limit) {
            numBits++;
            limit = 1 << numBits;
        }
        return numBits;
    }

    private static Random rnd = new Random();

    protected static int[] createTestPalette(int numColors) {
        int[] palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
            int r = rnd.nextInt(256);
            int g = rnd.nextInt(256);
            int b = rnd.nextInt(256);

            palette[i] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
        return palette;
    }
}
