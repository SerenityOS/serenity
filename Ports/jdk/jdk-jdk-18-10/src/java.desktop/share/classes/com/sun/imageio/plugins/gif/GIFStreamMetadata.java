/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.gif;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import org.w3c.dom.Node;

// TODO - document elimination of globalColorTableFlag

public class GIFStreamMetadata extends GIFMetadata {

    // package scope
    static final String
        nativeMetadataFormatName = "javax_imageio_gif_stream_1.0";

    static final String[] versionStrings = { "87a", "89a" };

    public String version; // 87a or 89a
    public int logicalScreenWidth;
    public int logicalScreenHeight;
    public int colorResolution; // 1 to 8
    public int pixelAspectRatio;

    public int backgroundColorIndex; // Valid if globalColorTable != null
    public boolean sortFlag; // Valid if globalColorTable != null

    static final String[] colorTableSizes = {
        "2", "4", "8", "16", "32", "64", "128", "256"
    };

    // Set global color table flag in header to 0 if null, 1 otherwise
    public byte[] globalColorTable = null;

    protected GIFStreamMetadata(boolean standardMetadataFormatSupported,
                                String nativeMetadataFormatName,
                                String nativeMetadataFormatClassName,
                                String[] extraMetadataFormatNames,
                                String[] extraMetadataFormatClassNames)
    {
        super(standardMetadataFormatSupported,
              nativeMetadataFormatName,
              nativeMetadataFormatClassName,
              extraMetadataFormatNames,
              extraMetadataFormatClassNames);
    }

    public GIFStreamMetadata() {
        this(true,
              nativeMetadataFormatName,
              "com.sun.imageio.plugins.gif.GIFStreamMetadataFormat",
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
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    private Node getNativeTree() {
        IIOMetadataNode node; // scratch node
        IIOMetadataNode root =
            new IIOMetadataNode(nativeMetadataFormatName);

        node = new IIOMetadataNode("Version");
        node.setAttribute("value", version);
        root.appendChild(node);

        // Image descriptor
        node = new IIOMetadataNode("LogicalScreenDescriptor");
        /* NB: At the moment we use empty strings to support undefined
         * integer values in tree representation.
         * We need to add better support for undefined/default values later.
         */
        node.setAttribute("logicalScreenWidth",
                          logicalScreenWidth == UNDEFINED_INTEGER_VALUE ?
                          "" : Integer.toString(logicalScreenWidth));
        node.setAttribute("logicalScreenHeight",
                          logicalScreenHeight == UNDEFINED_INTEGER_VALUE ?
                          "" : Integer.toString(logicalScreenHeight));
        // Stored value plus one
        node.setAttribute("colorResolution",
                          colorResolution == UNDEFINED_INTEGER_VALUE ?
                          "" : Integer.toString(colorResolution));
        node.setAttribute("pixelAspectRatio",
                          Integer.toString(pixelAspectRatio));
        root.appendChild(node);

        if (globalColorTable != null) {
            node = new IIOMetadataNode("GlobalColorTable");
            int numEntries = globalColorTable.length/3;
            node.setAttribute("sizeOfGlobalColorTable",
                              Integer.toString(numEntries));
            node.setAttribute("backgroundColorIndex",
                              Integer.toString(backgroundColorIndex));
            node.setAttribute("sortFlag",
                              sortFlag ? "TRUE" : "FALSE");

            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("ColorTableEntry");
                entry.setAttribute("index", Integer.toString(i));
                int r = globalColorTable[3*i] & 0xff;
                int g = globalColorTable[3*i + 1] & 0xff;
                int b = globalColorTable[3*i + 2] & 0xff;
                entry.setAttribute("red", Integer.toString(r));
                entry.setAttribute("green", Integer.toString(g));
                entry.setAttribute("blue", Integer.toString(b));
                node.appendChild(entry);
            }
            root.appendChild(node);
        }

        return root;
    }

    public IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma_node = new IIOMetadataNode("Chroma");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("ColorSpaceType");
        node.setAttribute("name", "RGB");
        chroma_node.appendChild(node);

        node = new IIOMetadataNode("BlackIsZero");
        node.setAttribute("value", "TRUE");
        chroma_node.appendChild(node);

        // NumChannels not in stream
        // Gamma not in format

        if (globalColorTable != null) {
            node = new IIOMetadataNode("Palette");
            int numEntries = globalColorTable.length/3;
            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("PaletteEntry");
                entry.setAttribute("index", Integer.toString(i));
                entry.setAttribute("red",
                           Integer.toString(globalColorTable[3*i] & 0xff));
                entry.setAttribute("green",
                           Integer.toString(globalColorTable[3*i + 1] & 0xff));
                entry.setAttribute("blue",
                           Integer.toString(globalColorTable[3*i + 2] & 0xff));
                node.appendChild(entry);
            }
            chroma_node.appendChild(node);

            // backgroundColorIndex is valid iff there is a color table
            node = new IIOMetadataNode("BackgroundIndex");
            node.setAttribute("value", Integer.toString(backgroundColorIndex));
            chroma_node.appendChild(node);
        }

        return chroma_node;
    }

    public IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("CompressionTypeName");
        node.setAttribute("value", "lzw");
        compression_node.appendChild(node);

        node = new IIOMetadataNode("Lossless");
        node.setAttribute("value", "TRUE");
        compression_node.appendChild(node);

        // NumProgressiveScans not in stream
        // BitRate not in format

        return compression_node;
    }

    public IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data_node = new IIOMetadataNode("Data");
        IIOMetadataNode node = null; // scratch node

        // PlanarConfiguration

        node = new IIOMetadataNode("SampleFormat");
        node.setAttribute("value", "Index");
        data_node.appendChild(node);

        node = new IIOMetadataNode("BitsPerSample");
        node.setAttribute("value",
                          colorResolution == UNDEFINED_INTEGER_VALUE ?
                          "" : Integer.toString(colorResolution));
        data_node.appendChild(node);

        // SignificantBitsPerSample
        // SampleMSB

        return data_node;
    }

    public IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("PixelAspectRatio");
        float aspectRatio = 1.0F;
        if (pixelAspectRatio != 0) {
            aspectRatio = (pixelAspectRatio + 15)/64.0F;
        }
        node.setAttribute("value", Float.toString(aspectRatio));
        dimension_node.appendChild(node);

        node = new IIOMetadataNode("ImageOrientation");
        node.setAttribute("value", "Normal");
        dimension_node.appendChild(node);

        // HorizontalPixelSize not in format
        // VerticalPixelSize not in format
        // HorizontalPhysicalPixelSpacing not in format
        // VerticalPhysicalPixelSpacing not in format
        // HorizontalPosition not in format
        // VerticalPosition not in format
        // HorizontalPixelOffset not in stream
        // VerticalPixelOffset not in stream

        node = new IIOMetadataNode("HorizontalScreenSize");
        node.setAttribute("value",
                          logicalScreenWidth == UNDEFINED_INTEGER_VALUE ?
                          "" : Integer.toString(logicalScreenWidth));
        dimension_node.appendChild(node);

        node = new IIOMetadataNode("VerticalScreenSize");
        node.setAttribute("value",
                          logicalScreenHeight == UNDEFINED_INTEGER_VALUE ?
                          "" : Integer.toString(logicalScreenHeight));
        dimension_node.appendChild(node);

        return dimension_node;
    }

    public IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document_node = new IIOMetadataNode("Document");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("FormatVersion");
        node.setAttribute("value", version);
        document_node.appendChild(node);

        // SubimageInterpretation not in format
        // ImageCreationTime not in format
        // ImageModificationTime not in format

        return document_node;
    }

    public IIOMetadataNode getStandardTextNode() {
        // Not in stream
        return null;
    }

    public IIOMetadataNode getStandardTransparencyNode() {
        // Not in stream
        return null;
    }

    public void setFromTree(String formatName, Node root)
        throws IIOInvalidTreeException
    {
        throw new IllegalStateException("Metadata is read-only!");
    }

    protected void mergeNativeTree(Node root) throws IIOInvalidTreeException
    {
        throw new IllegalStateException("Metadata is read-only!");
    }

    protected void mergeStandardTree(Node root) throws IIOInvalidTreeException
    {
        throw new IllegalStateException("Metadata is read-only!");
    }

    public void reset() {
        throw new IllegalStateException("Metadata is read-only!");
    }
}
