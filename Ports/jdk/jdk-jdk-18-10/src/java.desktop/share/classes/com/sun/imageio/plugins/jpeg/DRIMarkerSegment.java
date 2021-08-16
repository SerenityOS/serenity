/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import org.w3c.dom.Node;

/**
     * A DRI (Define Restart Interval) marker segment.
     */
class DRIMarkerSegment extends MarkerSegment {
    /**
     * Restart interval, or 0 if none is specified.
     */
    int restartInterval = 0;

    DRIMarkerSegment(JPEGBuffer buffer)
        throws IOException {
        super(buffer);
        restartInterval = (buffer.buf[buffer.bufPtr++] & 0xff) << 8;
        restartInterval |= buffer.buf[buffer.bufPtr++] & 0xff;
        buffer.bufAvail -= length;
    }

    DRIMarkerSegment(Node node) throws IIOInvalidTreeException {
        super(JPEG.DRI);
        updateFromNativeNode(node, true);
    }

    IIOMetadataNode getNativeNode() {
        IIOMetadataNode node = new IIOMetadataNode("dri");
        node.setAttribute("interval", Integer.toString(restartInterval));
        return node;
    }

    void updateFromNativeNode(Node node, boolean fromScratch)
        throws IIOInvalidTreeException {
        restartInterval = getAttributeValue(node, null, "interval",
                                            0, 65535, true);
    }

    /**
     * Writes the data for this segment to the stream in
     * valid JPEG format.
     */
    void write(ImageOutputStream ios) throws IOException {
        // We don't write DRI segments; the IJG library does.
    }

    void print() {
        printTag("DRI");
        System.out.println("Interval: "
                           + Integer.toString(restartInterval));
    }
}
