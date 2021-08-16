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

package simp;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import org.w3c.dom.Node;

public class SIMPMetadata extends IIOMetadata {

   static final String nativeMetadataFormatName = "simp_metadata_1.0";

   private int width, height;

   public SIMPMetadata() {
       super(true,
             nativeMetadataFormatName, "simp.SIMPMetadataFormat", null, null);
   }

   public SIMPMetadata(int width, int height) {
       this();
       this.width = width;
       this.height = height;
   }

   public boolean isReadOnly() {
        return true;
   }

   public void setFromTree(String formatName, Node root) {
    }

    public void mergeTree(String formatName, Node root) {
        throw new IllegalStateException("read only metadata");
    }

    public void reset() {
        throw new IllegalStateException("read only metadata");
    }

    private IIOMetadataNode addChildNode(IIOMetadataNode root,
                                         String name,
                                         Object object) {
        IIOMetadataNode child = new IIOMetadataNode(name);
        if (object != null) {
            child.setUserObject(object);
            child.setNodeValue(object.toString());
        }
        root.appendChild(child);
        return child;
    }

    private Node getNativeTree() {
        IIOMetadataNode root =
            new IIOMetadataNode(nativeMetadataFormatName);
        addChildNode(root, "width", width);
        addChildNode(root, "weight", height);
        return root;
    }

    public Node getAsTree(String formatName) {
        if (formatName.equals(nativeMetadataFormatName)) {
            return getNativeTree();
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        } else {
            throw new IllegalArgumentException("unsupported format");
        }
    }
}

