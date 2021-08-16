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
 * An SOF (Start Of Frame)  marker segment.
 */
class SOFMarkerSegment extends MarkerSegment {
    int samplePrecision;
    int numLines;
    int samplesPerLine;
    ComponentSpec [] componentSpecs;  // Array size is num components

    SOFMarkerSegment(boolean wantProg,
                     boolean wantExtended,
                     boolean willSubsample,
                     byte[] componentIDs,
                     int numComponents) {
        super(wantProg ? JPEG.SOF2
              : wantExtended ? JPEG.SOF1
              : JPEG.SOF0);
        samplePrecision = 8;
        numLines = 0;
        samplesPerLine = 0;
        componentSpecs = new ComponentSpec[numComponents];
        for(int i = 0; i < numComponents; i++) {
            int factor = 1;
            int qsel = 0;
            if (willSubsample) {
                factor = 2;
                if ((i == 1) || (i == 2)) {
                    factor = 1;
                    qsel = 1;
                }
            }
            componentSpecs[i] = new ComponentSpec(componentIDs[i], factor, qsel);
        }
    }

    SOFMarkerSegment(JPEGBuffer buffer) throws IOException{
        super(buffer);
        samplePrecision = buffer.buf[buffer.bufPtr++];
        numLines = (buffer.buf[buffer.bufPtr++] & 0xff) << 8;
        numLines |= buffer.buf[buffer.bufPtr++] & 0xff;
        samplesPerLine = (buffer.buf[buffer.bufPtr++] & 0xff) << 8;
        samplesPerLine |= buffer.buf[buffer.bufPtr++] & 0xff;
        int numComponents = buffer.buf[buffer.bufPtr++] & 0xff;
        componentSpecs = new ComponentSpec [numComponents];
        for (int i = 0; i < numComponents; i++) {
            componentSpecs[i] = new ComponentSpec(buffer);
        }
        buffer.bufAvail -= length;
    }

    SOFMarkerSegment(Node node) throws IIOInvalidTreeException {
        // All attributes are optional, so setup defaults first
        super(JPEG.SOF0);
        samplePrecision = 8;
        numLines = 0;
        samplesPerLine = 0;
        updateFromNativeNode(node, true);
    }

    protected Object clone() {
        SOFMarkerSegment newGuy = (SOFMarkerSegment) super.clone();
        if (componentSpecs != null) {
            newGuy.componentSpecs = componentSpecs.clone();
            for (int i = 0; i < componentSpecs.length; i++) {
                newGuy.componentSpecs[i] =
                    (ComponentSpec) componentSpecs[i].clone();
            }
        }
        return newGuy;
    }

    IIOMetadataNode getNativeNode() {
        IIOMetadataNode node = new IIOMetadataNode("sof");
        node.setAttribute("process", Integer.toString(tag-JPEG.SOF0));
        node.setAttribute("samplePrecision",
                          Integer.toString(samplePrecision));
        node.setAttribute("numLines",
                          Integer.toString(numLines));
        node.setAttribute("samplesPerLine",
                          Integer.toString(samplesPerLine));
        node.setAttribute("numFrameComponents",
                          Integer.toString(componentSpecs.length));
        for (int i = 0; i < componentSpecs.length; i++) {
            node.appendChild(componentSpecs[i].getNativeNode());
        }

        return node;
    }

    void updateFromNativeNode(Node node, boolean fromScratch)
        throws IIOInvalidTreeException {
        NamedNodeMap attrs = node.getAttributes();
        int value = getAttributeValue(node, attrs, "process", 0, 2, false);
        tag = (value != -1) ? value+JPEG.SOF0 : tag;
        // If samplePrecision is present, it must be 8.
        // This just checks.  We don't bother to assign the value.
        value = getAttributeValue(node, attrs, "samplePrecision", 8, 8, false);
        value = getAttributeValue(node, attrs, "numLines", 0, 65535, false);
        numLines = (value != -1) ? value : numLines;
        value = getAttributeValue(node, attrs, "samplesPerLine", 0, 65535, false);
        samplesPerLine = (value != -1) ? value : samplesPerLine;
        int numComponents = getAttributeValue(node, attrs, "numFrameComponents",
                                              1, 4, false);
        NodeList children = node.getChildNodes();
        if (children.getLength() != numComponents) {
            throw new IIOInvalidTreeException
                ("numFrameComponents must match number of children", node);
        }
        componentSpecs = new ComponentSpec [numComponents];
        for (int i = 0; i < numComponents; i++) {
            componentSpecs[i] = new ComponentSpec(children.item(i));
        }
    }

    /**
     * Writes the data for this segment to the stream in
     * valid JPEG format.
     */
    void write(ImageOutputStream ios) throws IOException {
        // We don't write SOF segments; the IJG library does.
    }

    void print () {
        printTag("SOF");
        System.out.print("Sample precision: ");
        System.out.println(samplePrecision);
        System.out.print("Number of lines: ");
        System.out.println(numLines);
        System.out.print("Samples per line: ");
        System.out.println(samplesPerLine);
        System.out.print("Number of components: ");
        System.out.println(componentSpecs.length);
        for(int i = 0; i<componentSpecs.length; i++) {
            componentSpecs[i].print();
        }
    }

    int getIDencodedCSType () {
        for (int i = 0; i < componentSpecs.length; i++) {
            if (componentSpecs[i].componentId < 'A') {
                return JPEG.JCS_UNKNOWN;
            }
        }
        switch(componentSpecs.length) {
        case 3:
            if ((componentSpecs[0].componentId == 'R')
                &&(componentSpecs[1].componentId == 'G')
                &&(componentSpecs[2].componentId == 'B')) {
                return JPEG.JCS_RGB;
            }
            break;
        }

        return JPEG.JCS_UNKNOWN;
    }

    ComponentSpec getComponentSpec(byte id, int factor, int qSelector) {
        return new ComponentSpec(id, factor, qSelector);
    }

    /**
     * A component spec within an SOF marker segment.
     */
    class ComponentSpec implements Cloneable {
        int componentId;
        int HsamplingFactor;
        int VsamplingFactor;
        int QtableSelector;

        ComponentSpec(byte id, int factor, int qSelector) {
            componentId = id;
            HsamplingFactor = factor;
            VsamplingFactor = factor;
            QtableSelector = qSelector;
        }

        ComponentSpec(JPEGBuffer buffer) {
            // Parent already did a loadBuf
            componentId = buffer.buf[buffer.bufPtr++];
            HsamplingFactor = buffer.buf[buffer.bufPtr] >>> 4;
            VsamplingFactor = buffer.buf[buffer.bufPtr++] & 0xf;
            QtableSelector = buffer.buf[buffer.bufPtr++];
        }

        ComponentSpec(Node node) throws IIOInvalidTreeException {
            NamedNodeMap attrs = node.getAttributes();
            componentId = getAttributeValue(node, attrs, "componentId", 0, 255, true);
            HsamplingFactor = getAttributeValue(node, attrs, "HsamplingFactor",
                                                1, 255, true);
            VsamplingFactor = getAttributeValue(node, attrs, "VsamplingFactor",
                                                1, 255, true);
            QtableSelector = getAttributeValue(node, attrs, "QtableSelector",
                                               0, 3, true);
        }

        protected Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {} // won't happen
            return null;
        }

        IIOMetadataNode getNativeNode() {
            IIOMetadataNode node = new IIOMetadataNode("componentSpec");
            node.setAttribute("componentId",
                              Integer.toString(componentId));
            node.setAttribute("HsamplingFactor",
                              Integer.toString(HsamplingFactor));
            node.setAttribute("VsamplingFactor",
                              Integer.toString(VsamplingFactor));
            node.setAttribute("QtableSelector",
                              Integer.toString(QtableSelector));
            return node;
        }

        void print () {
            System.out.print("Component ID: ");
            System.out.println(componentId);
            System.out.print("H sampling factor: ");
            System.out.println(HsamplingFactor);
            System.out.print("V sampling factor: ");
            System.out.println(VsamplingFactor);
            System.out.print("Q table selector: ");
            System.out.println(QtableSelector);
        }
    }

}
