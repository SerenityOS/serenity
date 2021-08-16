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
package com.sun.imageio.plugins.tiff;

import java.util.Arrays;
import java.util.List;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;

/**
 * The {@code Node} representation of a {@code TIFFField}
 * wherein the child node is procedural rather than buffered.
 */
public class TIFFFieldNode extends IIOMetadataNode {
    private static boolean isIFD(TIFFField f) {
        int type = f.getType();
        return f.hasDirectory() &&
            (type == TIFFTag.TIFF_LONG || type == TIFFTag.TIFF_IFD_POINTER);
    }

    private static String getNodeName(TIFFField f) {
        return isIFD(f) ? "TIFFIFD" : "TIFFField";
    }

    private boolean isIFD;

    private Boolean isInitialized = Boolean.FALSE;

    private TIFFField field;

    public TIFFFieldNode(TIFFField field) {
        super(getNodeName(field));

        isIFD = isIFD(field);

        this.field = field;

        TIFFTag tag = field.getTag();
        int tagNumber = tag.getNumber();
        String tagName = tag.getName();

        if(isIFD) {
            if(tagNumber != 0) {
                setAttribute("parentTagNumber", Integer.toString(tagNumber));
            }
            if(tagName != null) {
                setAttribute("parentTagName", tagName);
            }

            TIFFDirectory dir = field.hasDirectory() ?
                field.getDirectory() : (TIFFDirectory)field.getData();
            TIFFTagSet[] tagSets = dir.getTagSets();
            if(tagSets != null) {
                StringBuilder tagSetNames = new StringBuilder();
                for(int i = 0; i < tagSets.length; i++) {
                    tagSetNames.append(tagSets[i].getClass().getName());
                    if(i != tagSets.length - 1) {
                        tagSetNames.append(",");
                    }
                }
                setAttribute("tagSets", tagSetNames.toString());
            }
        } else {
            setAttribute("number", Integer.toString(tagNumber));
            setAttribute("name", tagName);
        }
    }

    private synchronized void initialize() {
        if(isInitialized) return;

        if(isIFD) {
            TIFFDirectory dir = field.hasDirectory() ?
                field.getDirectory() : (TIFFDirectory)field.getData();
            TIFFField[] fields = dir.getTIFFFields();
            if(fields != null) {
                TIFFTagSet[] tagSets = dir.getTagSets();
                List<TIFFTagSet> tagSetList = Arrays.asList(tagSets);
                int numFields = fields.length;
                for(int i = 0; i < numFields; i++) {
                    TIFFField f = fields[i];
                    int tagNumber = f.getTagNumber();
                    TIFFTag tag = TIFFIFD.getTag(tagNumber, tagSetList);

                    Node node = f.getAsNativeNode();

                    if (node != null) {
                        appendChild(node);
                    }
                }
            }
        } else {
            IIOMetadataNode child;
            int count = field.getCount();
            if (field.getType() == TIFFTag.TIFF_UNDEFINED) {
                child = new IIOMetadataNode("TIFFUndefined");

                byte[] data = field.getAsBytes();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    sb.append(data[i] & 0xff);
                    if (i < count - 1) {
                        sb.append(",");
                    }
                }
                child.setAttribute("value", sb.toString());
            } else {
                child = new IIOMetadataNode("TIFF" +
                                            TIFFField.getTypeName(field.getType()) +
                                            "s");

                TIFFTag tag = field.getTag();

                for (int i = 0; i < count; i++) {
                    IIOMetadataNode cchild =
                        new IIOMetadataNode("TIFF" +
                                            TIFFField.getTypeName(field.getType()));

                    cchild.setAttribute("value", field.getValueAsString(i));
                    if (tag.hasValueNames() && field.isIntegral()) {
                        int value = field.getAsInt(i);
                        String name = tag.getValueName(value);
                        if (name != null) {
                            cchild.setAttribute("description", name);
                        }
                    }

                    child.appendChild(cchild);
                }
            }
            appendChild(child);
        }

        isInitialized = Boolean.TRUE;
    }

    // Need to override this method to avoid a stack overflow exception
    // which will occur if super.appendChild is called from initialize().
    public Node appendChild(Node newChild) {
        if (newChild == null) {
            throw new NullPointerException("newChild == null!");
        }

        return super.insertBefore(newChild, null);
    }

    // Override all methods which refer to child nodes.

    public boolean hasChildNodes() {
        initialize();
        return super.hasChildNodes();
    }

    public int getLength() {
        initialize();
        return super.getLength();
    }

    public Node getFirstChild() {
        initialize();
        return super.getFirstChild();
    }

    public Node getLastChild() {
        initialize();
        return super.getLastChild();
    }

    public Node getPreviousSibling() {
        initialize();
        return super.getPreviousSibling();
    }

    public Node getNextSibling() {
        initialize();
        return super.getNextSibling();
    }

    public Node insertBefore(Node newChild,
                             Node refChild) {
        initialize();
        return super.insertBefore(newChild, refChild);
    }

    public Node replaceChild(Node newChild,
                             Node oldChild) {
        initialize();
        return super.replaceChild(newChild, oldChild);
    }

    public Node removeChild(Node oldChild) {
        initialize();
        return super.removeChild(oldChild);
    }

    public Node cloneNode(boolean deep) {
        initialize();
        return super.cloneNode(deep);
    }
}
