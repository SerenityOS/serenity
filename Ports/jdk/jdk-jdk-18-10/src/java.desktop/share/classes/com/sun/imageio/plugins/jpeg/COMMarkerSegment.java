/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.jpeg;

import java.io.IOException;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.Node;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * A Comment marker segment.  Retains an array of bytes representing the
 * comment data as it is read from the stream.  If the marker segment is
 * constructed from a String, then local default encoding is assumed
 * when creating the byte array.  If the marker segment is created from
 * an {@code IIOMetadataNode}, the user object, if present is
 * assumed to be a byte array containing the comment data.  If there is
 * no user object then the comment attribute is used to create the
 * byte array, again assuming the default local encoding.
 */
class COMMarkerSegment extends MarkerSegment {

    /**
     * Constructs a marker segment from the given buffer, which contains
     * data from an {@code ImageInputStream}.  This is used when
     * reading metadata from a stream.
     */
    COMMarkerSegment(JPEGBuffer buffer) throws IOException {
        super(buffer);
        loadData(buffer);
    }

    /**
     * Constructs a marker segment from a String.  This is used when
     * modifying metadata from a non-native tree and when transcoding.
     * The default encoding is used to construct the byte array.
     */
    COMMarkerSegment(String comment) {
        super(JPEG.COM);
        data = comment.getBytes(); // Default encoding
    }

    /**
     * Constructs a marker segment from a native tree node.  If the node
     * is an {@code IIOMetadataNode} and contains a user object,
     * that object is used rather than the string attribute.  If the
     * string attribute is used, the default encoding is used.
     */
    COMMarkerSegment(Node node) throws IIOInvalidTreeException{
        super(JPEG.COM);
        if (node instanceof IIOMetadataNode) {
            IIOMetadataNode ourNode = (IIOMetadataNode) node;
            data = (byte []) ourNode.getUserObject();
        }
        if (data == null) {
            String comment =
                node.getAttributes().getNamedItem("comment").getNodeValue();
            if (comment != null) {
                data = comment.getBytes(); // Default encoding
            } else {
                throw new IIOInvalidTreeException("Empty comment node!", node);
            }
        }
    }

    /**
     * Returns the array encoded as a String, using ISO-Latin-1 encoding.
     * If an application needs another encoding, the data array must be
     * consulted directly.
     */
    String getComment() {
        return new String(data, ISO_8859_1);
    }

    /**
     * Returns an {@code IIOMetadataNode} containing the data array
     * as a user object and a string encoded using ISO-8895-1, as an
     * attribute.
     */
    IIOMetadataNode getNativeNode() {
        IIOMetadataNode node = new IIOMetadataNode("com");
        node.setAttribute("comment", getComment());
        if (data != null) {
            node.setUserObject(data.clone());
        }
        return node;
    }

    /**
     * Writes the data for this segment to the stream in
     * valid JPEG format, directly from the data array.
     */
    void write(ImageOutputStream ios) throws IOException {
        length = 2 + data.length;
        writeTag(ios);
        ios.write(data);
    }

    void print() {
        printTag("COM");
        System.out.println("<" + getComment() + ">");
    }
}
