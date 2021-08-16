/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.IIOException;

import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

/**
 * All metadata is stored in MarkerSegments.  Marker segments
 * that we know about are stored in subclasses of this
 * basic class, which used for unrecognized APPn marker
 * segments.  XXX break out UnknownMarkerSegment as a subclass
 * and make this abstract, avoiding unused data field.
 */
class MarkerSegment implements Cloneable {
    protected static final int LENGTH_SIZE = 2; // length is 2 bytes
    int tag;      // See JPEG.java
    int length;    /* Sometimes needed by subclasses; doesn't include
                      itself.  Meaningful only if constructed from a stream */
    byte [] data = null;  // Raw segment data, used for unrecognized segments
    boolean unknown = false; // Set to true if the tag is not recognized

    /**
     * Constructor for creating {@code MarkerSegment}s by reading
     * from an {@code ImageInputStream}.
     */
    MarkerSegment(JPEGBuffer buffer) throws IOException {

        buffer.loadBuf(3);  // tag plus length
        tag = buffer.buf[buffer.bufPtr++] & 0xff;
        length = (buffer.buf[buffer.bufPtr++] & 0xff) << 8;
        length |= buffer.buf[buffer.bufPtr++] & 0xff;
        length -= 2;  // JPEG length includes itself, we don't

        if (length < 0) {
            throw new IIOException("Invalid segment length: " + length);
        }
        buffer.bufAvail -= 3;
        // Now that we know the true length, ensure that we've got it,
        // or at least a bufferful if length is too big.
        buffer.loadBuf(length);
    }

    /**
     * Constructor used when creating segments other than by
     * reading them from a stream.
     */
    MarkerSegment(int tag) {
        this.tag = tag;
        length = 0;
    }

    /**
     * Construct a MarkerSegment from an "unknown" DOM Node.
     */
    MarkerSegment(Node node) throws IIOInvalidTreeException {
        // The type of node should have been verified already.
        // get the attribute and assign it to the tag
        tag = getAttributeValue(node,
                                null,
                                "MarkerTag",
                                0, 255,
                                true);
        length = 0;
        // get the user object and clone it to the data
        if (node instanceof IIOMetadataNode) {
            IIOMetadataNode iioNode = (IIOMetadataNode) node;
            try {
                data = (byte []) iioNode.getUserObject();
            } catch (Exception e) {
                IIOInvalidTreeException newGuy =
                    new IIOInvalidTreeException
                    ("Can't get User Object", node);
                newGuy.initCause(e);
                throw newGuy;
            }
        } else {
            throw new IIOInvalidTreeException
                ("Node must have User Object", node);
        }
    }

    /**
     * Deep copy of data array.
     */
    protected Object clone() {
        MarkerSegment newGuy = null;
        try {
            newGuy = (MarkerSegment) super.clone();
        } catch (CloneNotSupportedException e) {} // won't happen
        if (this.data != null) {
            newGuy.data = data.clone();
        }
        return newGuy;
    }

    /**
     * We have determined that we don't know the type, so load
     * the data using the length parameter.
     */
    void loadData(JPEGBuffer buffer) throws IOException {
        data = new byte[length];
        buffer.readData(data);
    }

    IIOMetadataNode getNativeNode() {
        IIOMetadataNode node = new IIOMetadataNode("unknown");
        node.setAttribute("MarkerTag", Integer.toString(tag));
        node.setUserObject(data);

        return node;
    }

    static int getAttributeValue(Node node,
                                 NamedNodeMap attrs,
                                 String name,
                                 int min,
                                 int max,
                                 boolean required)
        throws IIOInvalidTreeException {
        if (attrs == null) {
            attrs = node.getAttributes();
        }
        String valueString = attrs.getNamedItem(name).getNodeValue();
        int value = -1;
        if (valueString == null) {
            if (required) {
                throw new IIOInvalidTreeException
                    (name + " attribute not found", node);
            }
        } else {
              value = Integer.parseInt(valueString);
              if ((value < min) || (value > max)) {
                  throw new IIOInvalidTreeException
                      (name + " attribute out of range", node);
              }
        }
        return value;
    }

    /**
     * Writes the marker, tag, and length.  Note that length
     * should be verified by the caller as a correct JPEG
     * length, i.e it includes itself.
     */
    void writeTag(ImageOutputStream ios) throws IOException {
        ios.write(0xff);
        ios.write(tag);
        write2bytes(ios, length);
    }

    /**
     * Writes the data for this segment to the stream in
     * valid JPEG format.
     */
    void write(ImageOutputStream ios) throws IOException {
        length = 2 + ((data != null) ? data.length : 0);
        writeTag(ios);
        if (data != null) {
            ios.write(data);
        }
    }

    static void write2bytes(ImageOutputStream ios,
                            int value) throws IOException {
        ios.write((value >> 8) & 0xff);
        ios.write(value & 0xff);

    }

    void printTag(String prefix) {
        System.out.println(prefix + " marker segment - marker = 0x"
                           + Integer.toHexString(tag));
        System.out.println("length: " + length);
    }

    void print() {
        printTag("Unknown");
        if (length > 10) {
            System.out.print("First 5 bytes:");
            for (int i=0;i<5;i++) {
                System.out.print(" Ox"
                                 + Integer.toHexString((int)data[i]));
            }
            System.out.print("\nLast 5 bytes:");
            for (int i=data.length-5;i<data.length;i++) {
                System.out.print(" Ox"
                                 + Integer.toHexString((int)data[i]));
            }
        } else {
            System.out.print("Data:");
            for (int i=0;i<data.length;i++) {
                System.out.print(" Ox"
                                 + Integer.toHexString((int)data[i]));
            }
        }
        System.out.println();
    }
}
