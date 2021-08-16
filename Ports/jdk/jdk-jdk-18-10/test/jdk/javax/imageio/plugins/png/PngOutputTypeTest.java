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
 * @bug     6549882
 * @summary Test verifies that PNG image reader creates buffered image
 *          of standart type for 8 bpp images with color type RGB or RGBAlpha
 *
 * @run     main PngOutputTypeTest
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PngOutputTypeTest {

    public static void main(String[] args) throws IOException {

        new PngOutputTypeTest(BufferedImage.TYPE_INT_RGB).doTest();

        new PngOutputTypeTest(BufferedImage.TYPE_INT_ARGB).doTest();

    }

    ImageInputStream iis;

    ImageReader reader;

    public PngOutputTypeTest(int type) throws IOException {
        this(createTestImage(type));
    }

    public PngOutputTypeTest(File f) throws IOException {
        this(ImageIO.createImageInputStream(f));
    }

    public PngOutputTypeTest(ImageInputStream iis) throws IOException {
        this.iis = iis;
        reader = ImageIO.getImageReaders(iis).next();
        reader.setInput(iis);
    }

    BufferedImage def;
    BufferedImage raw;

    ImageTypeSpecifier raw_type;

    public void doTest() throws IOException {
        /*
         * This test verifies that png images with color type RGB or RGBA
         * are decoded as buffered  image of some standard type.
         *
         * So we need to be sure that image provided by
         * user has required color type - RGB or RGBA
         */
        if (!checkImageType()) {
            System.out.println("Test IGNORED!");
            return;
        }

        def = reader.read(0);
        System.out.println("Default image type: " + def.getType());
        if (def == null || def.getType() == BufferedImage.TYPE_CUSTOM) {
            throw new RuntimeException("Test FAILED!");
        }

        raw_type = reader.getRawImageType(0);
        ImageReadParam param = reader.getDefaultReadParam();
        param.setDestinationType(raw_type);

        System.out.println("Reading with raw image type...");
        raw = reader.read(0, param);
        System.out.println("Type of raw image is " + raw.getType());

        compare(def, raw);

        Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
        while (types.hasNext()) {
            ImageTypeSpecifier t = types.next();
            System.out.println("Test type: " + t);
            param.setDestinationType(t);

            BufferedImage img = reader.read(0, param);
            System.out.println("Result type: " + img.getType());

            compare(def, img);
            System.out.println("Done.\n");
        }

        System.out.println("Test PASSED.");
    }

    private boolean checkImageType() throws IOException {
        IIOMetadata md  = null;
        try {
            md = reader.getImageMetadata(0);
        } catch (IOException e) {
            return false;
        }

        String format = md.getNativeMetadataFormatName();

        Node root = md.getAsTree(format);

        Node ihdr = getNode(root, "IHDR");
        if (ihdr == null) {
            throw new RuntimeException("No ihdr node: invalid png image!");
        }

        String colorType = getAttributeValue(ihdr, "colorType");
        System.out.println("ColorType: " + colorType);
        if ("RGB".equals(colorType) || "RGBAlpha".equals(colorType)) {
            // we shuld chek bitDepth
            System.out.println("Good color type!");
            String bitDepthStr = getAttributeValue(ihdr, "bitDepth");
            System.out.println("bitDepth: " + bitDepthStr);
            int bitDepth = -1;
            try {
                bitDepth = Integer.parseInt(bitDepthStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid bitDepth!");
            }
            if (bitDepth == 8) {
                /*
                 * This image is RGB or RGBA color type and
                 * 8 bit tepth. so it can be used for test
                 */
                return true;
            }
        }

        return false;
    }

    private String getAttributeValue(Node n, String attrname) {
        NamedNodeMap attrs = n.getAttributes();
        if (attrs == null) {
            return null;
        } else {
            Node a = attrs.getNamedItem(attrname);
            if (a == null) {
                return null;
            } else {
                return a.getNodeValue();
            }
        }
    }

    private Node getNode(Node root, String name) {
        Node n = root;
        return lookupNode(n, name);
    }

    private Node lookupNode(Node n, String name) {
        if (n == null) {
            return null;
        }
        if (name.equals(n.getNodeName())) {
            return n;
        } else {
            // may be next on this level?
            Node res = lookupNode(n.getNextSibling(), name);

            if (res != null) {
                return res;
            } else {
                /// try children then
                return lookupNode(n.getFirstChild(), name);
            }
        }
    }

    private static void compare(BufferedImage a, BufferedImage b) {
        int w = a.getWidth();
        int h = a.getHeight();

        if (w != b.getWidth() || h != b.getHeight()) {
            throw new RuntimeException("Test FAILED!");
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    throw new RuntimeException("Test FAILED!");
                }
            }
        }
    }

    static Color[] colors = new Color[] { Color.red, Color.green, Color.blue };

    private static ImageInputStream createTestImage(int type) throws IOException  {
        int w = 100;
        int h = 100;

        BufferedImage img = new BufferedImage(w, h, type);

        int dx = w / colors.length;

        for (int i = 0; i < colors.length; i++) {
            for (int x = i *dx; (x < (i + 1) * dx) && (x < w) ; x++) {
                for (int y = 0; y < h; y++) {
                    img.setRGB(x, y, colors[i].getRGB());
                }
            }
        }

        File pwd = new File(".");
        File out = File.createTempFile("rgba_", ".png", pwd);
        System.out.println("Create file: " + out.getAbsolutePath());
        ImageIO.write(img, "PNG", out);
        return ImageIO.createImageInputStream(out);
    }
}
