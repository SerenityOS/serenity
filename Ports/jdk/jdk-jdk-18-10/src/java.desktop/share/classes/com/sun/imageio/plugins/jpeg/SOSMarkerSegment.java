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

//import javax.imageio.IIOException;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

/**
 * An SOS (Start Of Scan) marker segment.
 */
class SOSMarkerSegment extends MarkerSegment {
    int startSpectralSelection;
    int endSpectralSelection;
    int approxHigh;
    int approxLow;
    ScanComponentSpec [] componentSpecs; // Array size is numScanComponents

    SOSMarkerSegment(boolean willSubsample,
                     byte[] componentIDs,
                     int numComponents) {
        super(JPEG.SOS);
        startSpectralSelection = 0;
        endSpectralSelection = 63;
        approxHigh = 0;
        approxLow = 0;
        componentSpecs = new ScanComponentSpec[numComponents];
        for (int i = 0; i < numComponents; i++) {
            int tableSel = 0;
            if (willSubsample) {
                if ((i == 1) || (i == 2)) {
                    tableSel = 1;
                }
            }
            componentSpecs[i] = new ScanComponentSpec(componentIDs[i],
                                                      tableSel);
        }
    }

    SOSMarkerSegment(JPEGBuffer buffer) throws IOException {
        super(buffer);
        int numComponents = buffer.buf[buffer.bufPtr++];
        componentSpecs = new ScanComponentSpec[numComponents];
        for (int i = 0; i < numComponents; i++) {
            componentSpecs[i] = new ScanComponentSpec(buffer);
        }
        startSpectralSelection = buffer.buf[buffer.bufPtr++];
        endSpectralSelection = buffer.buf[buffer.bufPtr++];
        approxHigh = buffer.buf[buffer.bufPtr] >> 4;
        approxLow = buffer.buf[buffer.bufPtr++] &0xf;
        buffer.bufAvail -= length;
    }

    SOSMarkerSegment(Node node) throws IIOInvalidTreeException {
        super(JPEG.SOS);
        startSpectralSelection = 0;
        endSpectralSelection = 63;
        approxHigh = 0;
        approxLow = 0;
        updateFromNativeNode(node, true);
    }

    protected Object clone () {
        SOSMarkerSegment newGuy = (SOSMarkerSegment) super.clone();
        if (componentSpecs != null) {
            newGuy.componentSpecs = componentSpecs.clone();
            for (int i = 0; i < componentSpecs.length; i++) {
                newGuy.componentSpecs[i] =
                    (ScanComponentSpec) componentSpecs[i].clone();
            }
        }
        return newGuy;
    }

    IIOMetadataNode getNativeNode() {
        IIOMetadataNode node = new IIOMetadataNode("sos");
        node.setAttribute("numScanComponents",
                          Integer.toString(componentSpecs.length));
        node.setAttribute("startSpectralSelection",
                          Integer.toString(startSpectralSelection));
        node.setAttribute("endSpectralSelection",
                          Integer.toString(endSpectralSelection));
        node.setAttribute("approxHigh",
                          Integer.toString(approxHigh));
        node.setAttribute("approxLow",
                          Integer.toString(approxLow));
        for (int i = 0; i < componentSpecs.length; i++) {
            node.appendChild(componentSpecs[i].getNativeNode());
        }

        return node;
    }

    void updateFromNativeNode(Node node, boolean fromScratch)
        throws IIOInvalidTreeException {
        NamedNodeMap attrs = node.getAttributes();
        int numComponents = getAttributeValue(node, attrs, "numScanComponents",
                                              1, 4, true);
        int value = getAttributeValue(node, attrs, "startSpectralSelection",
                                      0, 63, false);
        startSpectralSelection = (value != -1) ? value : startSpectralSelection;
        value = getAttributeValue(node, attrs, "endSpectralSelection",
                                  0, 63, false);
        endSpectralSelection = (value != -1) ? value : endSpectralSelection;
        value = getAttributeValue(node, attrs, "approxHigh", 0, 15, false);
        approxHigh = (value != -1) ? value : approxHigh;
        value = getAttributeValue(node, attrs, "approxLow", 0, 15, false);
        approxLow = (value != -1) ? value : approxLow;

        // Now the children
        NodeList children = node.getChildNodes();
        if (children.getLength() != numComponents) {
            throw new IIOInvalidTreeException
                ("numScanComponents must match the number of children", node);
        }
        componentSpecs = new ScanComponentSpec[numComponents];
        for (int i = 0; i < numComponents; i++) {
            componentSpecs[i] = new ScanComponentSpec(children.item(i));
        }
    }

    /**
     * Writes the data for this segment to the stream in
     * valid JPEG format.
     */
    void write(ImageOutputStream ios) throws IOException {
        // We don't write SOS segments; the IJG library does.
    }

    void print () {
        printTag("SOS");
        System.out.print("Start spectral selection: ");
        System.out.println(startSpectralSelection);
        System.out.print("End spectral selection: ");
        System.out.println(endSpectralSelection);
        System.out.print("Approx high: ");
        System.out.println(approxHigh);
        System.out.print("Approx low: ");
        System.out.println(approxLow);
        System.out.print("Num scan components: ");
        System.out.println(componentSpecs.length);
        for (int i = 0; i< componentSpecs.length; i++) {
            componentSpecs[i].print();
        }
    }

    ScanComponentSpec getScanComponentSpec(byte componentSel, int tableSel) {
        return new ScanComponentSpec(componentSel, tableSel);
    }

    /**
     * A scan component spec within an SOS marker segment.
     */
    class ScanComponentSpec implements Cloneable {
        int componentSelector;
        int dcHuffTable;
        int acHuffTable;

        ScanComponentSpec(byte componentSel, int tableSel) {
            componentSelector = componentSel;
            dcHuffTable = tableSel;
            acHuffTable = tableSel;
        }

        ScanComponentSpec(JPEGBuffer buffer) {
            // Parent already loaded the buffer
            componentSelector = buffer.buf[buffer.bufPtr++];
            dcHuffTable = buffer.buf[buffer.bufPtr] >> 4;
            acHuffTable = buffer.buf[buffer.bufPtr++] & 0xf;
        }

        ScanComponentSpec(Node node) throws IIOInvalidTreeException {
            NamedNodeMap attrs = node.getAttributes();
            componentSelector = getAttributeValue(node, attrs, "componentSelector",
                                                  0, 255, true);
            dcHuffTable = getAttributeValue(node, attrs, "dcHuffTable",
                                            0, 3, true);
            acHuffTable = getAttributeValue(node, attrs, "acHuffTable",
                                            0, 3, true);
        }

        protected Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {} // won't happen
            return null;
        }

        IIOMetadataNode getNativeNode() {
            IIOMetadataNode node = new IIOMetadataNode("scanComponentSpec");
            node.setAttribute("componentSelector",
                              Integer.toString(componentSelector));
            node.setAttribute("dcHuffTable",
                              Integer.toString(dcHuffTable));
            node.setAttribute("acHuffTable",
                              Integer.toString(acHuffTable));
            return node;
        }

        void print () {
            System.out.print("Component Selector: ");
            System.out.println(componentSelector);
            System.out.print("DC huffman table: ");
            System.out.println(dcHuffTable);
            System.out.print("AC huffman table: ");
            System.out.println(acHuffTable);
        }
    }

}
