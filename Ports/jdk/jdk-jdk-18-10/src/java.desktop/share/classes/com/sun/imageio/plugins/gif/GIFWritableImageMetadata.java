/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import org.w3c.dom.Node;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

class GIFWritableImageMetadata extends GIFImageMetadata {

    // package scope
    static final String
    NATIVE_FORMAT_NAME = "javax_imageio_gif_image_1.0";

    GIFWritableImageMetadata() {
        super(true,
              NATIVE_FORMAT_NAME,
              "com.sun.imageio.plugins.gif.GIFImageMetadataFormat",
              null, null);
    }

    public boolean isReadOnly() {
        return false;
    }

    public void reset() {
        // Fields from Image Descriptor
        imageLeftPosition = 0;
        imageTopPosition = 0;
        imageWidth = 0;
        imageHeight = 0;
        interlaceFlag = false;
        sortFlag = false;
        localColorTable = null;

        // Fields from Graphic Control Extension
        disposalMethod = 0;
        userInputFlag = false;
        transparentColorFlag = false;
        delayTime = 0;
        transparentColorIndex = 0;

        // Fields from Plain Text Extension
        hasPlainTextExtension = false;
        textGridLeft = 0;
        textGridTop = 0;
        textGridWidth = 0;
        textGridHeight = 0;
        characterCellWidth = 0;
        characterCellHeight = 0;
        textForegroundColor = 0;
        textBackgroundColor = 0;
        text = null;

        // Fields from ApplicationExtension
        applicationIDs = null;
        authenticationCodes = null;
        applicationData = null;

        // Fields from CommentExtension
        // List of byte[]
        comments = null;
    }

    private byte[] fromISO8859(String data) {
        return data.getBytes(ISO_8859_1);
    }

    protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName().equals(nativeMetadataFormatName)) {
            fatal(node, "Root must be " + nativeMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();

            if (name.equals("ImageDescriptor")) {
                imageLeftPosition = getIntAttribute(node,
                                                    "imageLeftPosition",
                                                    -1, true,
                                                    true, 0, 65535);

                imageTopPosition = getIntAttribute(node,
                                                   "imageTopPosition",
                                                   -1, true,
                                                   true, 0, 65535);

                imageWidth = getIntAttribute(node,
                                             "imageWidth",
                                             -1, true,
                                             true, 1, 65535);

                imageHeight = getIntAttribute(node,
                                              "imageHeight",
                                              -1, true,
                                              true, 1, 65535);

                interlaceFlag = getBooleanAttribute(node, "interlaceFlag",
                                                    false, true);
            } else if (name.equals("LocalColorTable")) {
                int sizeOfLocalColorTable =
                    getIntAttribute(node, "sizeOfLocalColorTable",
                                    true, 2, 256);
                if (sizeOfLocalColorTable != 2 &&
                    sizeOfLocalColorTable != 4 &&
                    sizeOfLocalColorTable != 8 &&
                    sizeOfLocalColorTable != 16 &&
                    sizeOfLocalColorTable != 32 &&
                    sizeOfLocalColorTable != 64 &&
                    sizeOfLocalColorTable != 128 &&
                    sizeOfLocalColorTable != 256) {
                    fatal(node,
                          "Bad value for LocalColorTable attribute sizeOfLocalColorTable!");
                }

                sortFlag = getBooleanAttribute(node, "sortFlag", false, true);

                localColorTable = getColorTable(node, "ColorTableEntry",
                                                true, sizeOfLocalColorTable);
            } else if (name.equals("GraphicControlExtension")) {
                String disposalMethodName =
                    getStringAttribute(node, "disposalMethod", null,
                                       true, disposalMethodNames);
                disposalMethod = 0;
                while(!disposalMethodName.equals(disposalMethodNames[disposalMethod])) {
                    disposalMethod++;
                }

                userInputFlag = getBooleanAttribute(node, "userInputFlag",
                                                    false, true);

                transparentColorFlag =
                    getBooleanAttribute(node, "transparentColorFlag",
                                        false, true);

                delayTime = getIntAttribute(node,
                                            "delayTime",
                                            -1, true,
                                            true, 0, 65535);

                transparentColorIndex =
                    getIntAttribute(node, "transparentColorIndex",
                                    -1, true,
                                    true, 0, 65535);
            } else if (name.equals("PlainTextExtension")) {
                hasPlainTextExtension = true;

                textGridLeft = getIntAttribute(node,
                                               "textGridLeft",
                                               -1, true,
                                               true, 0, 65535);

                textGridTop = getIntAttribute(node,
                                              "textGridTop",
                                              -1, true,
                                              true, 0, 65535);

                textGridWidth = getIntAttribute(node,
                                                "textGridWidth",
                                                -1, true,
                                                true, 1, 65535);

                textGridHeight = getIntAttribute(node,
                                                 "textGridHeight",
                                                 -1, true,
                                                 true, 1, 65535);

                // As per the specification (89a), character cell width
                // and character cell height occupy one byte each
                // in the Plain Text Extension block.
                characterCellWidth = getIntAttribute(node,
                                                     "characterCellWidth",
                                                     -1, true,
                                                     true, 1, 255);

                characterCellHeight = getIntAttribute(node,
                                                      "characterCellHeight",
                                                      -1, true,
                                                      true, 1, 255);

                textForegroundColor = getIntAttribute(node,
                                                      "textForegroundColor",
                                                      -1, true,
                                                      true, 0, 255);

                textBackgroundColor = getIntAttribute(node,
                                                      "textBackgroundColor",
                                                      -1, true,
                                                      true, 0, 255);

                // XXX The "text" attribute of the PlainTextExtension element
                // is not defined in the GIF image metadata format but it is
                // present in the GIFImageMetadata class. Consequently it is
                // used here but not required and with a default of "". See
                // bug 5082763.

                String textString =
                    getStringAttribute(node, "text", "", false, null);
                text = fromISO8859(textString);
            } else if (name.equals("ApplicationExtensions")) {
                IIOMetadataNode applicationExtension =
                    (IIOMetadataNode)node.getFirstChild();

                if (!applicationExtension.getNodeName().equals("ApplicationExtension")) {
                    fatal(node,
                          "Only a ApplicationExtension may be a child of a ApplicationExtensions!");
                }

                String applicationIDString =
                    getStringAttribute(applicationExtension, "applicationID",
                                       null, true, null);

                String authenticationCodeString =
                    getStringAttribute(applicationExtension, "authenticationCode",
                                       null, true, null);

                Object applicationExtensionData =
                    applicationExtension.getUserObject();
                if (applicationExtensionData == null ||
                    !(applicationExtensionData instanceof byte[])) {
                    fatal(applicationExtension,
                          "Bad user object in ApplicationExtension!");
                }

                if (applicationIDs == null) {
                    applicationIDs = new ArrayList<>();
                    authenticationCodes = new ArrayList<>();
                    applicationData = new ArrayList<>();
                }

                applicationIDs.add(fromISO8859(applicationIDString));
                authenticationCodes.add(fromISO8859(authenticationCodeString));
                applicationData.add((byte[]) applicationExtensionData);
            } else if (name.equals("CommentExtensions")) {
                Node commentExtension = node.getFirstChild();
                if (commentExtension != null) {
                    while(commentExtension != null) {
                        if (!commentExtension.getNodeName().equals("CommentExtension")) {
                            fatal(node,
                                  "Only a CommentExtension may be a child of a CommentExtensions!");
                        }

                        if (comments == null) {
                            comments = new ArrayList<>();
                        }

                        String comment =
                            getStringAttribute(commentExtension, "value", null,
                                               true, null);

                        comments.add(fromISO8859(comment));

                        commentExtension = commentExtension.getNextSibling();
                    }
                }
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
                        localColorTable = getColorTable(childNode,
                                                        "PaletteEntry",
                                                        false, -1);
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if (name.equals("Compression")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("NumProgressiveScans")) {
                        int numProgressiveScans =
                            getIntAttribute(childNode, "value", 4, false,
                                            true, 1, Integer.MAX_VALUE);
                        if (numProgressiveScans > 1) {
                            interlaceFlag = true;
                        }
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if (name.equals("Dimension")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("HorizontalPixelOffset")) {
                        imageLeftPosition = getIntAttribute(childNode,
                                                            "value",
                                                            -1, true,
                                                            true, 0, 65535);
                    } else if (childName.equals("VerticalPixelOffset")) {
                        imageTopPosition = getIntAttribute(childNode,
                                                           "value",
                                                           -1, true,
                                                           true, 0, 65535);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if (name.equals("Text")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("TextEntry") &&
                        getAttribute(childNode, "compression",
                                     "none", false).equals("none") &&
                        Charset.isSupported(getAttribute(childNode,
                                                         "encoding",
                                                         "ISO-8859-1",
                                                         false))) {
                        String value = getAttribute(childNode, "value");
                        byte[] comment = fromISO8859(value);
                        if (comments == null) {
                            comments = new ArrayList<>();
                        }
                        comments.add(comment);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if (name.equals("Transparency")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if (childName.equals("TransparentIndex")) {
                        transparentColorIndex = getIntAttribute(childNode,
                                                                "value",
                                                                -1, true,
                                                                true, 0, 255);
                        transparentColorFlag = true;
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
