/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.bmp;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import com.sun.imageio.plugins.common.I18N;
import com.sun.imageio.plugins.common.ImageUtil;
import org.w3c.dom.Node;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class BMPMetadata extends IIOMetadata implements BMPConstants {
    public static final String nativeMetadataFormatName =
        "javax_imageio_bmp_1.0";

    // Fields for Image Descriptor
    public String bmpVersion;
    public int width ;
    public int height;
    public short bitsPerPixel;
    public int compression;
    public int imageSize;

    // Fields for PixelsPerMeter
    public int xPixelsPerMeter;
    public int yPixelsPerMeter;

    public int colorsUsed;
    public int colorsImportant;

    // Fields for BI_BITFIELDS compression(Mask)
    public int redMask;
    public int greenMask;
    public int blueMask;
    public int alphaMask;

    public int colorSpace;

    // Fields for CIE XYZ for the LCS_CALIBRATED_RGB color space
    public double redX;
    public double redY;
    public double redZ;
    public double greenX;
    public double greenY;
    public double greenZ;
    public double blueX;
    public double blueY;
    public double blueZ;

    // Fields for Gamma values for the LCS_CALIBRATED_RGB color space
    public int gammaRed;
    public int gammaGreen;
    public int gammaBlue;

    public int intent;

    // Fields for the Palette and Entries
    public byte[] palette = null;
    public int paletteSize;
    public int red;
    public int green;
    public int blue;

    public BMPMetadata() {
        super(true,
              nativeMetadataFormatName,
              "com.sun.imageio.plugins.bmp.BMPMetadataFormat",
              null, null);
    }

    public boolean isReadOnly() {
        return true;
    }

    public Node getAsTree(String formatName) {
        if (formatName.equals(nativeMetadataFormatName)) {
            return getNativeTree();
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        } else {
            throw new IllegalArgumentException(I18N.getString("BMPMetadata0"));
        }
    }

    private String toISO8859(byte[] data) {
        return new String(data, ISO_8859_1);
    }

    private Node getNativeTree() {
        IIOMetadataNode root =
            new IIOMetadataNode(nativeMetadataFormatName);

        addChildNode(root, "BMPVersion", bmpVersion);
        addChildNode(root, "Width", width);
        addChildNode(root, "Height", height);
        addChildNode(root, "BitsPerPixel", Short.valueOf(bitsPerPixel));
        addChildNode(root, "Compression", compression);
        addChildNode(root, "ImageSize", imageSize);

        IIOMetadataNode node = addChildNode(root, "PixelsPerMeter", null);
        addChildNode(node, "X", xPixelsPerMeter);
        addChildNode(node, "Y", yPixelsPerMeter);

        addChildNode(root, "ColorsUsed", colorsUsed);
        addChildNode(root, "ColorsImportant", colorsImportant);

        int version = 0;
        for (int i = 0; i < bmpVersion.length(); i++)
            if (Character.isDigit(bmpVersion.charAt(i)))
                version = bmpVersion.charAt(i) -'0';

        if (version >= 4) {
            node = addChildNode(root, "Mask", null);
            addChildNode(node, "Red", redMask);
            addChildNode(node, "Green", greenMask);
            addChildNode(node, "Blue", blueMask);
            addChildNode(node, "Alpha", alphaMask);

            addChildNode(root, "ColorSpaceType", colorSpace);

            node = addChildNode(root, "CIEXYZEndPoints", null);
            addXYZPoints(node, "Red", redX, redY, redZ);
            addXYZPoints(node, "Green", greenX, greenY, greenZ);
            addXYZPoints(node, "Blue", blueX, blueY, blueZ);

            node = addChildNode(root, "Intent", intent);
        }

        // Palette
        if ((palette != null) && (paletteSize > 0)) {
            node = addChildNode(root, "Palette", null);
            int numComps = palette.length / paletteSize;

            for (int i = 0, j = 0; i < paletteSize; i++) {
                IIOMetadataNode entry =
                    addChildNode(node, "PaletteEntry", null);
                red = palette[j++] & 0xff;
                green = palette[j++] & 0xff;
                blue = palette[j++] & 0xff;
                addChildNode(entry, "Red", Byte.valueOf((byte)red));
                addChildNode(entry, "Green", Byte.valueOf((byte)green));
                addChildNode(entry, "Blue", Byte.valueOf((byte)blue));
                if (numComps == 4)
                    addChildNode(entry, "Alpha",
                                 Byte.valueOf((byte)(palette[j++] & 0xff)));
            }
        }

        return root;
    }

    // Standard tree node methods
    protected IIOMetadataNode getStandardChromaNode() {

        if ((palette != null) && (paletteSize > 0)) {
            IIOMetadataNode node = new IIOMetadataNode("Chroma");
            IIOMetadataNode subNode = new IIOMetadataNode("Palette");
            int numComps = palette.length / paletteSize;
            subNode.setAttribute("value", "" + numComps);

            for (int i = 0, j = 0; i < paletteSize; i++) {
                IIOMetadataNode subNode1 = new IIOMetadataNode("PaletteEntry");
                subNode1.setAttribute("index", ""+i);
                subNode1.setAttribute("red", "" + palette[j++]);
                subNode1.setAttribute("green", "" + palette[j++]);
                subNode1.setAttribute("blue", "" + palette[j++]);
                if (numComps == 4 && palette[j] != 0)
                    subNode1.setAttribute("alpha", "" + palette[j++]);
                subNode.appendChild(subNode1);
            }
            node.appendChild(subNode);
            return node;
        }

        return null;
    }

    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode node = new IIOMetadataNode("Compression");

        // CompressionTypeName
        IIOMetadataNode subNode = new IIOMetadataNode("CompressionTypeName");
        subNode.setAttribute("value", BMPCompressionTypes.getName(compression));
        node.appendChild(subNode);
        return node;
    }

    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        String bits = "";
        if (bitsPerPixel == 24)
            bits = "8 8 8 ";
        else if (bitsPerPixel == 16 || bitsPerPixel == 32) {
            bits = "" + countBits(redMask) + " " + countBits(greenMask) +
                  countBits(blueMask) + "" + countBits(alphaMask);
        }

        IIOMetadataNode subNode = new IIOMetadataNode("BitsPerSample");
        subNode.setAttribute("value", bits);
        node.appendChild(subNode);

        return node;
    }

    protected IIOMetadataNode getStandardDimensionNode() {
        if (yPixelsPerMeter > 0.0F && xPixelsPerMeter > 0.0F) {
            IIOMetadataNode node = new IIOMetadataNode("Dimension");
            float ratio = yPixelsPerMeter / xPixelsPerMeter;
            IIOMetadataNode subNode = new IIOMetadataNode("PixelAspectRatio");
            subNode.setAttribute("value", "" + ratio);
            node.appendChild(subNode);

            subNode = new IIOMetadataNode("HorizontalPhysicalPixelSpacing");
            subNode.setAttribute("value", "" + (1000.0F / xPixelsPerMeter));
            node.appendChild(subNode);

            subNode = new IIOMetadataNode("VerticalPhysicalPixelSpacing");
            subNode.setAttribute("value", "" + (1000.0F / yPixelsPerMeter));
            node.appendChild(subNode);

            return node;
        }
        return null;
    }

    public void setFromTree(String formatName, Node root) {
        throw new IllegalStateException(I18N.getString("BMPMetadata1"));
    }

    public void mergeTree(String formatName, Node root) {
        throw new IllegalStateException(I18N.getString("BMPMetadata1"));
    }

    public void reset() {
        throw new IllegalStateException(I18N.getString("BMPMetadata1"));
    }

    private String countBits(int num) {
        int count = 0;
        while(num > 0) {
            if ((num & 1) == 1)
                count++;
            num >>>= 1;
        }

        return count == 0 ? "" : "" + count;
    }

    private void addXYZPoints(IIOMetadataNode root, String name, double x, double y, double z) {
        IIOMetadataNode node = addChildNode(root, name, null);
        addChildNode(node, "X", Double.valueOf(x));
        addChildNode(node, "Y", Double.valueOf(y));
        addChildNode(node, "Z", Double.valueOf(z));
    }

    private IIOMetadataNode addChildNode(IIOMetadataNode root,
                                         String name,
                                         Object object) {
        IIOMetadataNode child = new IIOMetadataNode(name);
        if (object != null) {
            child.setUserObject(object);
            child.setNodeValue(ImageUtil.convertObjectToString(object));
        }
        root.appendChild(child);
        return child;
    }
}
