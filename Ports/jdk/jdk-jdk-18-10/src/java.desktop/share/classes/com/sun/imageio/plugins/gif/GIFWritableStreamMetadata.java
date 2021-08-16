/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/*
 * The source for this class was copied verbatim from the source for
 * package com.sun.imageio.plugins.gif.GIFImageMetadata and then modified
 * to make the class read-write capable.
 */

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import org.w3c.dom.Node;

class GIFWritableStreamMetadata extends GIFStreamMetadata {

    // package scope
    static final String
    NATIVE_FORMAT_NAME = "javax_imageio_gif_stream_1.0";

    public GIFWritableStreamMetadata() {
        super(true,
              NATIVE_FORMAT_NAME,
              "com.sun.imageio.plugins.gif.GIFStreamMetadataFormat", // XXX J2SE
              null, null);

        // initialize metadata fields by default values
        reset();
    }

    public boolean isReadOnly() {
        return false;
    }

    public void mergeTree(String formatName, Node root)
      throws IIOInvalidTreeException {
        if (formatName.equals(nativeMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeNativeTree(root);
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeStandardTree(root);
        } else {
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    public void reset() {
        version = null;

        logicalScreenWidth = UNDEFINED_INTEGER_VALUE;
        logicalScreenHeight = UNDEFINED_INTEGER_VALUE;
        colorResolution = UNDEFINED_INTEGER_VALUE;
        pixelAspectRatio = 0;

        backgroundColorIndex = 0;
        sortFlag = false;
        globalColorTable = null;
    }

    protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName().equals(nativeMetadataFormatName)) {
            fatal(node, "Root must be " + nativeMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();

            if (name.equals("Version")) {
                version = getStringAttribute(node, "value", null,
                                             true, versionStrings);
            } else if (name.equals("LogicalScreenDescriptor")) {
                /* NB: At the moment we use empty strings to support undefined
                 * integer values in tree representation.
                 * We need to add better support for undefined/default values
                 * later.
                 */
                logicalScreenWidth = getIntAttribute(node,
                                                     "logicalScreenWidth",
                                                     UNDEFINED_INTEGER_VALUE,
                                                     true,
                                                     true, 1, 65535);

                logicalScreenHeight = getIntAttribute(node,
                                                      "logicalScreenHeight",
                                                      UNDEFINED_INTEGER_VALUE,
                                                      true,
                                                      true, 1, 65535);

                colorResolution = getIntAttribute(node,
                                                  "colorResolution",
                                                  UNDEFINED_INTEGER_VALUE,
                                                  true,
                                                  true, 1, 8);

                pixelAspectRatio = getIntAttribute(node,
                                                   "pixelAspectRatio",
                                                   0, true,
                                                   true, 0, 255);
            } else if (name.equals("GlobalColorTable")) {
                int sizeOfGlobalColorTable =
                    getIntAttribute(node, "sizeOfGlobalColorTable",
                                    true, 2, 256);
                if (sizeOfGlobalColorTable != 2 &&
                   sizeOfGlobalColorTable != 4 &&
                   sizeOfGlobalColorTable != 8 &&
                   sizeOfGlobalColorTable != 16 &&
                   sizeOfGlobalColorTable != 32 &&
                   sizeOfGlobalColorTable != 64 &&
                   sizeOfGlobalColorTable != 128 &&
                   sizeOfGlobalColorTable != 256) {
                    fatal(node,
                          "Bad value for GlobalColorTable attribute sizeOfGlobalColorTable!");
                }

                backgroundColorIndex = getIntAttribute(node,
                                                       "backgroundColorIndex",
                                                       0, true,
                                                       true, 0, 255);

                sortFlag = getBooleanAttribute(node, "sortFlag", false, true);

                globalColorTable = getColorTable(node, "ColorTableEntry",
                                                 true, sizeOfGlobalColorTable);
            } else {
                fatal(node, "Unknown child of root node!");
            }

            node = node.getNextSibling();
        }
    }

    protected void mergeStandardTree(Node root)
      throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName()
            .equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            fatal(node, "Root must be " +
                  IIOMetadataFormatImpl.standardMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();

            if (name.equals("Chroma")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("Palette")) {
                        globalColorTable = getColorTable(childNode,
                                                         "PaletteEntry",
                                                         false, -1);

                    } else if (childName.equals("BackgroundIndex")) {
                        backgroundColorIndex = getIntAttribute(childNode,
                                                               "value",
                                                               -1, true,
                                                               true, 0, 255);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if (name.equals("Data")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("BitsPerSample")) {
                        colorResolution = getIntAttribute(childNode,
                                                          "value",
                                                          -1, true,
                                                          true, 1, 8);
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if (name.equals("Dimension")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("PixelAspectRatio")) {
                        float aspectRatio = getFloatAttribute(childNode,
                                                              "value");
                        if (aspectRatio == 1.0F) {
                            pixelAspectRatio = 0;
                        } else {
                            int ratio = (int)(aspectRatio*64.0F - 15.0F);
                            pixelAspectRatio =
                                Math.max(Math.min(ratio, 255), 0);
                        }
                    } else if (childName.equals("HorizontalScreenSize")) {
                        logicalScreenWidth = getIntAttribute(childNode,
                                                             "value",
                                                             -1, true,
                                                             true, 1, 65535);
                    } else if (childName.equals("VerticalScreenSize")) {
                        logicalScreenHeight = getIntAttribute(childNode,
                                                              "value",
                                                              -1, true,
                                                              true, 1, 65535);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if (name.equals("Document")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("FormatVersion")) {
                        String formatVersion =
                            getStringAttribute(childNode, "value", null,
                                               true, null);
                        for (int i = 0; i < versionStrings.length; i++) {
                            if (formatVersion.equals(versionStrings[i])) {
                                version = formatVersion;
                                break;
                            }
                        }
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            }

            node = node.getNextSibling();
        }
    }

    public void setFromTree(String formatName, Node root)
        throws IIOInvalidTreeException
    {
        reset();
        mergeTree(formatName, root);
    }
}
