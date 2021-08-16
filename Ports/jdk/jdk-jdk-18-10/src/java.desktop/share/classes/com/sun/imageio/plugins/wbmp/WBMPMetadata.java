/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.wbmp;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import com.sun.imageio.plugins.common.I18N;
import com.sun.imageio.plugins.common.ImageUtil;
import org.w3c.dom.Node;

public class WBMPMetadata extends IIOMetadata {

    static final String nativeMetadataFormatName =
        "javax_imageio_wbmp_1.0";

    public int wbmpType;

    public int width;
    public int height;

    public WBMPMetadata() {
        super(true,
              nativeMetadataFormatName,
              "com.sun.imageio.plugins.wbmp.WBMPMetadataFormat",
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
            throw new IllegalArgumentException(I18N.getString("WBMPMetadata0"));
        }
    }

    private Node getNativeTree() {
        IIOMetadataNode root =
            new IIOMetadataNode(nativeMetadataFormatName);

        addChildNode(root, "WBMPType", wbmpType);
        addChildNode(root, "Width", width);
        addChildNode(root, "Height", height);

        return root;
    }

    public void setFromTree(String formatName, Node root) {
        throw new IllegalStateException(I18N.getString("WBMPMetadata1"));
    }

    public void mergeTree(String formatName, Node root) {
        throw new IllegalStateException(I18N.getString("WBMPMetadata1"));
    }

    public void reset() {
        throw new IllegalStateException(I18N.getString("WBMPMetadata1"));
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


    protected IIOMetadataNode getStandardChromaNode() {

        IIOMetadataNode node = new IIOMetadataNode("Chroma");
        IIOMetadataNode subNode = new IIOMetadataNode("BlackIsZero");
        subNode.setAttribute("value", "TRUE");

        node.appendChild(subNode);
        return node;
    }


    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
        IIOMetadataNode node = null; // scratch node

        // PixelAspectRatio not in image

        node = new IIOMetadataNode("ImageOrientation");
        node.setAttribute("value", "Normal");
        dimension_node.appendChild(node);

        return dimension_node;
    }

}
