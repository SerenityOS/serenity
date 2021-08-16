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

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGHuffmanTable;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import com.sun.imageio.plugins.common.ImageUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Metadata for the JPEG plug-in.
 */
public class JPEGMetadata extends IIOMetadata implements Cloneable {

    //////// Private variables

    private static final boolean debug = false;

    /**
     * A copy of {@code markerSequence}, created the first time the
     * {@code markerSequence} is modified.  This is used by reset
     * to restore the original state.
     */
    private List<MarkerSegment> resetSequence = null;

    /**
     * Set to {@code true} when reading a thumbnail stored as
     * JPEG.  This is used to enforce the prohibition of JFIF thumbnails
     * containing any JFIF marker segments, and to ensure generation of
     * a correct native subtree during {@code getAsTree}.
     */
    private boolean inThumb = false;

    /**
     * Set by the chroma node construction method to signal the
     * presence or absence of an alpha channel to the transparency
     * node construction method.  Used only when constructing a
     * standard metadata tree.
     */
    private boolean hasAlpha;

    //////// end of private variables

    /////// Package-access variables

    /**
     * All data is a list of {@code MarkerSegment} objects.
     * When accessing the list, use the tag to identify the particular
     * subclass.  Any JFIF marker segment must be the first element
     * of the list if it is present, and any JFXX or APP2ICC marker
     * segments are subordinate to the JFIF marker segment.  This
     * list is package visible so that the writer can access it.
     * @see MarkerSegment
     */
    List<MarkerSegment> markerSequence = new ArrayList<>();

    /**
     * Indicates whether this object represents stream or image
     * metadata.  Package-visible so the writer can see it.
     */
    final boolean isStream;

    /////// End of package-access variables

    /////// Constructors

    /**
     * Constructor containing code shared by other constructors.
     */
    JPEGMetadata(boolean isStream, boolean inThumb) {
        super(true,  // Supports standard format
              JPEG.nativeImageMetadataFormatName,  // and a native format
              JPEG.nativeImageMetadataFormatClassName,
              null, null);  // No other formats
        this.inThumb = inThumb;
        // But if we are stream metadata, adjust the variables
        this.isStream = isStream;
        if (isStream) {
            nativeMetadataFormatName = JPEG.nativeStreamMetadataFormatName;
            nativeMetadataFormatClassName =
                JPEG.nativeStreamMetadataFormatClassName;
        }
    }

    /*
     * Constructs a {@code JPEGMetadata} object by reading the
     * contents of an {@code ImageInputStream}.  Has package-only
     * access.
     *
     * @param isStream A boolean indicating whether this object will be
     * stream or image metadata.
     * @param isThumb A boolean indicating whether this metadata object
     * is for an image or for a thumbnail stored as JPEG.
     * @param iis An {@code ImageInputStream} from which to read
     * the metadata.
     * @param reader The {@code JPEGImageReader} calling this
     * constructor, to which warnings should be sent.
     */
    JPEGMetadata(boolean isStream,
                 boolean isThumb,
                 ImageInputStream iis,
                 JPEGImageReader reader) throws IOException {
        this(isStream, isThumb);

        JPEGBuffer buffer = new JPEGBuffer(iis);

        buffer.loadBuf(0);

        // The first three bytes should be FF, SOI, FF
        if (((buffer.buf[0] & 0xff) != 0xff)
            || ((buffer.buf[1] & 0xff) != JPEG.SOI)
            || ((buffer.buf[2] & 0xff) != 0xff)) {
            throw new IIOException ("Image format error");
        }

        boolean done = false;
        buffer.bufAvail -=2;  // Next byte should be the ff before a marker
        buffer.bufPtr = 2;
        MarkerSegment newGuy = null;
        while (!done) {
            byte [] buf;
            int ptr;
            buffer.loadBuf(1);
            if (debug) {
                System.out.println("top of loop");
                buffer.print(10);
            }
            buffer.scanForFF(reader);
            switch (buffer.buf[buffer.bufPtr] & 0xff) {
            case 0:
                if (debug) {
                    System.out.println("Skipping 0");
                }
                buffer.bufAvail--;
                buffer.bufPtr++;
                break;
            case JPEG.SOF0:
            case JPEG.SOF1:
            case JPEG.SOF2:
                if (isStream) {
                    throw new IIOException
                        ("SOF not permitted in stream metadata");
                }
                newGuy = new SOFMarkerSegment(buffer);
                break;
            case JPEG.DQT:
                newGuy = new DQTMarkerSegment(buffer);
                break;
            case JPEG.DHT:
                newGuy = new DHTMarkerSegment(buffer);
                break;
            case JPEG.DRI:
                newGuy = new DRIMarkerSegment(buffer);
                break;
            case JPEG.APP0:
                // Either JFIF, JFXX, or unknown APP0
                buffer.loadBuf(8); // tag, length, id
                buf = buffer.buf;
                ptr = buffer.bufPtr;
                if ((buf[ptr+3] == 'J')
                    && (buf[ptr+4] == 'F')
                    && (buf[ptr+5] == 'I')
                    && (buf[ptr+6] == 'F')
                    && (buf[ptr+7] == 0)) {
                    if (inThumb) {
                        reader.warningOccurred
                            (JPEGImageReader.WARNING_NO_JFIF_IN_THUMB);
                        // Leave newGuy null
                        // Read a dummy to skip the segment
                        JFIFMarkerSegment dummy =
                            new JFIFMarkerSegment(buffer);
                    } else if (isStream) {
                        throw new IIOException
                            ("JFIF not permitted in stream metadata");
                    } else if (markerSequence.isEmpty() == false) {
                        throw new IIOException
                            ("JFIF APP0 must be first marker after SOI");
                    } else {
                        newGuy = new JFIFMarkerSegment(buffer);
                    }
                } else if ((buf[ptr+3] == 'J')
                           && (buf[ptr+4] == 'F')
                           && (buf[ptr+5] == 'X')
                           && (buf[ptr+6] == 'X')
                           && (buf[ptr+7] == 0)) {
                    if (isStream) {
                        throw new IIOException
                            ("JFXX not permitted in stream metadata");
                    }
                    if (inThumb) {
                        throw new IIOException
                          ("JFXX markers not allowed in JFIF JPEG thumbnail");
                    }
                    JFIFMarkerSegment jfif =
                        (JFIFMarkerSegment) findMarkerSegment
                               (JFIFMarkerSegment.class, true);
                    if (jfif == null) {
                        throw new IIOException
                            ("JFXX encountered without prior JFIF!");
                    }
                    jfif.addJFXX(buffer, reader);
                    // newGuy remains null
                } else {
                    newGuy = new MarkerSegment(buffer);
                    newGuy.loadData(buffer);
                }
                break;
            case JPEG.APP2:
                // Either an ICC profile or unknown APP2
                buffer.loadBuf(15); // tag, length, id
                if ((buffer.buf[buffer.bufPtr+3] == 'I')
                    && (buffer.buf[buffer.bufPtr+4] == 'C')
                    && (buffer.buf[buffer.bufPtr+5] == 'C')
                    && (buffer.buf[buffer.bufPtr+6] == '_')
                    && (buffer.buf[buffer.bufPtr+7] == 'P')
                    && (buffer.buf[buffer.bufPtr+8] == 'R')
                    && (buffer.buf[buffer.bufPtr+9] == 'O')
                    && (buffer.buf[buffer.bufPtr+10] == 'F')
                    && (buffer.buf[buffer.bufPtr+11] == 'I')
                    && (buffer.buf[buffer.bufPtr+12] == 'L')
                    && (buffer.buf[buffer.bufPtr+13] == 'E')
                    && (buffer.buf[buffer.bufPtr+14] == 0)
                    ) {
                    if (isStream) {
                        throw new IIOException
                            ("ICC profiles not permitted in stream metadata");
                    }

                    JFIFMarkerSegment jfif =
                        (JFIFMarkerSegment) findMarkerSegment
                        (JFIFMarkerSegment.class, true);
                    if (jfif == null) {
                        newGuy = new MarkerSegment(buffer);
                        newGuy.loadData(buffer);
                    } else {
                        jfif.addICC(buffer);
                    }
                    // newGuy remains null
                } else {
                    newGuy = new MarkerSegment(buffer);
                    newGuy.loadData(buffer);
                }
                break;
            case JPEG.APP14:
                // Either Adobe or unknown APP14
                buffer.loadBuf(8); // tag, length, id
                if ((buffer.buf[buffer.bufPtr+3] == 'A')
                    && (buffer.buf[buffer.bufPtr+4] == 'd')
                    && (buffer.buf[buffer.bufPtr+5] == 'o')
                    && (buffer.buf[buffer.bufPtr+6] == 'b')
                    && (buffer.buf[buffer.bufPtr+7] == 'e')) {
                    if (isStream) {
                        throw new IIOException
                      ("Adobe APP14 markers not permitted in stream metadata");
                    }
                    newGuy = new AdobeMarkerSegment(buffer);
                } else {
                    newGuy = new MarkerSegment(buffer);
                    newGuy.loadData(buffer);
                }

                break;
            case JPEG.COM:
                newGuy = new COMMarkerSegment(buffer);
                break;
            case JPEG.SOS:
                if (isStream) {
                    throw new IIOException
                        ("SOS not permitted in stream metadata");
                }
                newGuy = new SOSMarkerSegment(buffer);
                break;
            case JPEG.RST0:
            case JPEG.RST1:
            case JPEG.RST2:
            case JPEG.RST3:
            case JPEG.RST4:
            case JPEG.RST5:
            case JPEG.RST6:
            case JPEG.RST7:
                if (debug) {
                    System.out.println("Restart Marker");
                }
                buffer.bufPtr++; // Just skip it
                buffer.bufAvail--;
                break;
            case JPEG.EOI:
                done = true;
                buffer.bufPtr++;
                buffer.bufAvail--;
                break;
            default:
                newGuy = new MarkerSegment(buffer);
                newGuy.loadData(buffer);
                newGuy.unknown = true;
                break;
            }
            if (newGuy != null) {
                markerSequence.add(newGuy);
                if (debug) {
                    newGuy.print();
                }
                newGuy = null;
            }
        }

        // Now that we've read up to the EOI, we need to push back
        // whatever is left in the buffer, so that the next read
        // in the native code will work.

        buffer.pushBack();

        if (!isConsistent()) {
            throw new IIOException("Inconsistent metadata read from stream");
        }
    }

    /**
     * Constructs a default stream {@code JPEGMetadata} object appropriate
     * for the given write parameters.
     */
    JPEGMetadata(ImageWriteParam param, JPEGImageWriter writer) {
        this(true, false);

        JPEGImageWriteParam jparam = null;

        if ((param != null) && (param instanceof JPEGImageWriteParam)) {
            jparam = (JPEGImageWriteParam) param;
            if (!jparam.areTablesSet()) {
                jparam = null;
            }
        }
        if (jparam != null) {
            markerSequence.add(new DQTMarkerSegment(jparam.getQTables()));
            markerSequence.add
                (new DHTMarkerSegment(jparam.getDCHuffmanTables(),
                                      jparam.getACHuffmanTables()));
        } else {
            // default tables.
            markerSequence.add(new DQTMarkerSegment(JPEG.getDefaultQTables()));
            markerSequence.add(new DHTMarkerSegment(JPEG.getDefaultHuffmanTables(true),
                                                    JPEG.getDefaultHuffmanTables(false)));
        }

        // Defensive programming
        if (!isConsistent()) {
            throw new InternalError("Default stream metadata is inconsistent");
        }
    }

    /**
     * Constructs a default image {@code JPEGMetadata} object appropriate
     * for the given image type and write parameters.
     */
    JPEGMetadata(ImageTypeSpecifier imageType,
                 ImageWriteParam param,
                 JPEGImageWriter writer) {
        this(false, false);

        boolean wantJFIF = true;
        boolean wantAdobe = false;
        int transform = JPEG.ADOBE_UNKNOWN;
        boolean willSubsample = true;
        boolean wantICC = false;
        boolean wantProg = false;
        boolean wantOptimized = false;
        boolean wantExtended = false;
        boolean wantQTables = true;
        boolean wantHTables = true;
        float quality = JPEG.DEFAULT_QUALITY;
        byte[] componentIDs = { 1, 2, 3, 4};
        int numComponents = 0;

        ImageTypeSpecifier destType = null;

        if (param != null) {
            destType = param.getDestinationType();
            if (destType != null) {
                if (imageType != null) {
                    // Ignore the destination type.
                    writer.warningOccurred
                        (JPEGImageWriter.WARNING_DEST_IGNORED);
                    destType = null;
                }
            }
            // The only progressive mode that makes sense here is MODE_DEFAULT
            if (param.canWriteProgressive()) {
                // the param may not be one of ours, so it may return false.
                // If so, the following would throw an exception
                if (param.getProgressiveMode() == ImageWriteParam.MODE_DEFAULT) {
                    wantProg = true;
                    wantOptimized = true;
                    wantHTables = false;
                }
            }

            if (param instanceof JPEGImageWriteParam) {
                JPEGImageWriteParam jparam = (JPEGImageWriteParam) param;
                if (jparam.areTablesSet()) {
                    wantQTables = false;  // If the param has them, metadata shouldn't
                    wantHTables = false;
                    if ((jparam.getDCHuffmanTables().length > 2)
                            || (jparam.getACHuffmanTables().length > 2)) {
                        wantExtended = true;
                    }
                }
                // Progressive forces optimized, regardless of param setting
                // so consult the param re optimized only if not progressive
                if (!wantProg) {
                    wantOptimized = jparam.getOptimizeHuffmanTables();
                    if (wantOptimized) {
                        wantHTables = false;
                    }
                }
            }

            // compression quality should determine the q tables.  Note that this
            // will be ignored if we already decided not to create any.
            // Again, the param may not be one of ours, so we must check that it
            // supports compression settings
            if (param.canWriteCompressed()) {
                if (param.getCompressionMode() == ImageWriteParam.MODE_EXPLICIT) {
                    quality = param.getCompressionQuality();
                }
            }
        }

        // We are done with the param, now for the image types

        ColorSpace cs = null;
        if (destType != null) {
            ColorModel cm = destType.getColorModel();
            numComponents = cm.getNumComponents();
            boolean hasExtraComponents = (cm.getNumColorComponents() != numComponents);
            boolean hasAlpha = cm.hasAlpha();
            cs = cm.getColorSpace();
            int type = cs.getType();
            switch(type) {
            case ColorSpace.TYPE_GRAY:
                willSubsample = false;
                if (hasExtraComponents) {  // e.g. alpha
                    wantJFIF = false;
                }
                break;
            case ColorSpace.TYPE_YCbCr:
                if (hasExtraComponents) { // e.g. K or alpha
                    wantJFIF = false;
                    if (!hasAlpha) { // Not alpha, so must be K
                        wantAdobe = true;
                        transform = JPEG.ADOBE_YCCK;
                    }
                }
                break;
            case ColorSpace.TYPE_RGB:  // with or without alpha
                wantJFIF = false;
                wantAdobe = true;
                willSubsample = false;
                componentIDs[0] = (byte) 'R';
                componentIDs[1] = (byte) 'G';
                componentIDs[2] = (byte) 'B';
                if (hasAlpha) {
                    componentIDs[3] = (byte) 'A';
                }
                break;
            default:
                // Everything else is not subsampled, gets no special marker,
                // and component ids are 1 - N
                wantJFIF = false;
                willSubsample = false;
            }
        } else if (imageType != null) {
            ColorModel cm = imageType.getColorModel();
            numComponents = cm.getNumComponents();
            boolean hasExtraComponents = (cm.getNumColorComponents() != numComponents);
            boolean hasAlpha = cm.hasAlpha();
            cs = cm.getColorSpace();
            int type = cs.getType();
            switch(type) {
            case ColorSpace.TYPE_GRAY:
                willSubsample = false;
                if (hasExtraComponents) {  // e.g. alpha
                    wantJFIF = false;
                }
                break;
            case ColorSpace.TYPE_RGB:  // with or without alpha
                // without alpha we just accept the JFIF defaults
                if (hasAlpha) {
                    wantJFIF = false;
                }
                break;
            case ColorSpace.TYPE_3CLR:
                wantJFIF = false;
                willSubsample = false;
                if (cs.equals(ColorSpace.getInstance(ColorSpace.CS_PYCC))) {
                    willSubsample = true;
                    wantAdobe = true;
                    componentIDs[0] = (byte) 'Y';
                    componentIDs[1] = (byte) 'C';
                    componentIDs[2] = (byte) 'c';
                    if (hasAlpha) {
                        componentIDs[3] = (byte) 'A';
                    }
                }
                break;
            case ColorSpace.TYPE_YCbCr:
                if (hasExtraComponents) { // e.g. K or alpha
                    wantJFIF = false;
                    if (!hasAlpha) {  // then it must be K
                        wantAdobe = true;
                        transform = JPEG.ADOBE_YCCK;
                    }
                }
                break;
            case ColorSpace.TYPE_CMYK:
                wantJFIF = false;
                wantAdobe = true;
                transform = JPEG.ADOBE_YCCK;
                break;

            default:
                // Everything else is not subsampled, gets no special marker,
                // and component ids are 0 - N
                wantJFIF = false;
                willSubsample = false;
            }

        }

        // do we want an ICC profile?
        if (wantJFIF && ImageUtil.isNonStandardICCColorSpace(cs)) {
            wantICC = true;
        }

        // Now step through the markers, consulting our variables.
        if (wantJFIF) {
            JFIFMarkerSegment jfif = new JFIFMarkerSegment();
            markerSequence.add(jfif);
            if (wantICC) {
                try {
                    jfif.addICC((ICC_ColorSpace)cs);
                } catch (IOException e) {} // Can't happen here
            }
        }
        // Adobe
        if (wantAdobe) {
            markerSequence.add(new AdobeMarkerSegment(transform));
        }

        // dqt
        if (wantQTables) {
            markerSequence.add(new DQTMarkerSegment(quality, willSubsample));
        }

        // dht
        if (wantHTables) {
            markerSequence.add(new DHTMarkerSegment(willSubsample));
        }

        // sof
        markerSequence.add(new SOFMarkerSegment(wantProg,
                                                wantExtended,
                                                willSubsample,
                                                componentIDs,
                                                numComponents));

        // sos
        if (!wantProg) {  // Default progression scans are done in the writer
            markerSequence.add(new SOSMarkerSegment(willSubsample,
                                                    componentIDs,
                                                    numComponents));
        }

        // Defensive programming
        if (!isConsistent()) {
            throw new InternalError("Default image metadata is inconsistent");
        }
    }

    ////// End of constructors

    // Utilities for dealing with the marker sequence.
    // The first ones have package access for access from the writer.

    /**
     * Returns the first MarkerSegment object in the list
     * with the given tag, or null if none is found.
     */
    MarkerSegment findMarkerSegment(int tag) {
        for (MarkerSegment seg : markerSequence) {
            if (seg.tag == tag) {
                return seg;
            }
        }
        return null;
    }

    /**
     * Returns the first or last MarkerSegment object in the list
     * of the given class, or null if none is found.
     */
    MarkerSegment findMarkerSegment(Class<? extends MarkerSegment> cls, boolean first) {
        if (first) {
            for (MarkerSegment seg : markerSequence) {
                if (cls.isInstance(seg)) {
                    return seg;
                }
            }
        } else {
            ListIterator<MarkerSegment> iter =
                markerSequence.listIterator(markerSequence.size());
            while (iter.hasPrevious()) {
                MarkerSegment seg = iter.previous();
                if (cls.isInstance(seg)) {
                    return seg;
                }
            }
        }
        return null;
    }

    /**
     * Returns the index of the first or last MarkerSegment in the list
     * of the given class, or -1 if none is found.
     */
    private int findMarkerSegmentPosition(Class<? extends MarkerSegment> cls,
                                          boolean first) {
        if (first) {
            ListIterator<MarkerSegment> iter = markerSequence.listIterator();
            for (int i = 0; iter.hasNext(); i++) {
                MarkerSegment seg = iter.next();
                if (cls.isInstance(seg)) {
                    return i;
                }
            }
        } else {
            ListIterator<MarkerSegment> iter =
                    markerSequence.listIterator(markerSequence.size());
            for (int i = markerSequence.size()-1; iter.hasPrevious(); i--) {
                MarkerSegment seg = iter.previous();
                if (cls.isInstance(seg)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findLastUnknownMarkerSegmentPosition() {
        ListIterator<MarkerSegment> iter =
            markerSequence.listIterator(markerSequence.size());
        for (int i = markerSequence.size()-1; iter.hasPrevious(); i--) {
            MarkerSegment seg = iter.previous();
            if (seg.unknown == true) {
                return i;
            }
        }
        return -1;
    }

    // Implement Cloneable, but restrict access

    protected Object clone() {
        JPEGMetadata newGuy = null;
        try {
            newGuy = (JPEGMetadata) super.clone();
        } catch (CloneNotSupportedException e) {} // won't happen
        if (markerSequence != null) {
            newGuy.markerSequence = cloneSequence();
        }
        newGuy.resetSequence = null;
        return newGuy;
    }

    /**
     * Returns a deep copy of the current marker sequence.
     */
    private List<MarkerSegment> cloneSequence() {
        if (markerSequence == null) {
            return null;
        }
        List<MarkerSegment> retval = new ArrayList<>(markerSequence.size());
        for (MarkerSegment seg : markerSequence) {
            retval.add((MarkerSegment) seg.clone());
        }

        return retval;
    }


    // Tree methods

    public Node getAsTree(String formatName) {
        if (formatName == null) {
            throw new IllegalArgumentException("null formatName!");
        }
        if (isStream) {
            if (formatName.equals(JPEG.nativeStreamMetadataFormatName)) {
                return getNativeTree();
            }
        } else {
            if (formatName.equals(JPEG.nativeImageMetadataFormatName)) {
                return getNativeTree();
            }
            if (formatName.equals
                    (IIOMetadataFormatImpl.standardMetadataFormatName)) {
                return getStandardTree();
            }
        }
        throw  new IllegalArgumentException("Unsupported format name: "
                                                + formatName);
    }

    IIOMetadataNode getNativeTree() {
        IIOMetadataNode root;
        IIOMetadataNode top;
        Iterator<MarkerSegment> iter = markerSequence.iterator();
        if (isStream) {
            root = new IIOMetadataNode(JPEG.nativeStreamMetadataFormatName);
            top = root;
        } else {
            IIOMetadataNode sequence = new IIOMetadataNode("markerSequence");
            if (!inThumb) {
                root = new IIOMetadataNode(JPEG.nativeImageMetadataFormatName);
                IIOMetadataNode header = new IIOMetadataNode("JPEGvariety");
                root.appendChild(header);
                JFIFMarkerSegment jfif = (JFIFMarkerSegment)
                    findMarkerSegment(JFIFMarkerSegment.class, true);
                if (jfif != null) {
                    iter.next();  // JFIF must be first, so this skips it
                    header.appendChild(jfif.getNativeNode());
                }
                root.appendChild(sequence);
            } else {
                root = sequence;
            }
            top = sequence;
        }
        while(iter.hasNext()) {
            MarkerSegment seg = iter.next();
            top.appendChild(seg.getNativeNode());
        }
        return root;
    }

    // Standard tree node methods

    protected IIOMetadataNode getStandardChromaNode() {
        hasAlpha = false;  // Unless we find otherwise

        // Colorspace type - follow the rules in the spec
        // First get the SOF marker segment, if there is one
        SOFMarkerSegment sof = (SOFMarkerSegment)
            findMarkerSegment(SOFMarkerSegment.class, true);
        if (sof == null) {
            // No image, so no chroma
            return null;
        }

        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");
        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        chroma.appendChild(csType);

        // get the number of channels
        int numChannels = sof.componentSpecs.length;

        IIOMetadataNode numChanNode = new IIOMetadataNode("NumChannels");
        chroma.appendChild(numChanNode);
        numChanNode.setAttribute("value", Integer.toString(numChannels));

        // is there a JFIF marker segment?
        if (findMarkerSegment(JFIFMarkerSegment.class, true) != null) {
            if (numChannels == 1) {
                csType.setAttribute("name", "GRAY");
            } else {
                csType.setAttribute("name", "YCbCr");
            }
            return chroma;
        }

        // How about an Adobe marker segment?
        AdobeMarkerSegment adobe =
            (AdobeMarkerSegment) findMarkerSegment(AdobeMarkerSegment.class, true);
        if (adobe != null){
            switch (adobe.transform) {
            case JPEG.ADOBE_YCCK:
                csType.setAttribute("name", "YCCK");
                break;
            case JPEG.ADOBE_YCC:
                csType.setAttribute("name", "YCbCr");
                break;
            case JPEG.ADOBE_UNKNOWN:
                if (numChannels == 3) {
                    csType.setAttribute("name", "RGB");
                } else if (numChannels == 4) {
                    csType.setAttribute("name", "CMYK");
                }
                break;
            }
            return chroma;
        }

        // Neither marker.  Check components
        if (numChannels < 3) {
            csType.setAttribute("name", "GRAY");
            if (numChannels == 2) {
                hasAlpha = true;
            }
            return chroma;
        }

        boolean idsAreJFIF = false;

        int cid0 = sof.componentSpecs[0].componentId;
        int cid1 = sof.componentSpecs[1].componentId;
        int cid2 = sof.componentSpecs[2].componentId;
        if ((cid0 == 1) && (cid1 == 2) && (cid2 == 3)) {
            idsAreJFIF = true;
        }

        if (idsAreJFIF) {
            csType.setAttribute("name", "YCbCr");
            if (numChannels == 4) {
                hasAlpha = true;
            }
            return chroma;
        }

        // Check against the letters
        if ((sof.componentSpecs[0].componentId == 'R')
            && (sof.componentSpecs[1].componentId == 'G')
            && (sof.componentSpecs[2].componentId == 'B')){

            csType.setAttribute("name", "RGB");
            if ((numChannels == 4)
                && (sof.componentSpecs[3].componentId == 'A')) {
                hasAlpha = true;
            }
            return chroma;
        }

        if ((sof.componentSpecs[0].componentId == 'Y')
            && (sof.componentSpecs[1].componentId == 'C')
            && (sof.componentSpecs[2].componentId == 'c')){

            csType.setAttribute("name", "PhotoYCC");
            if ((numChannels == 4)
                && (sof.componentSpecs[3].componentId == 'A')) {
                hasAlpha = true;
            }
            return chroma;
        }

        // Finally, 3-channel subsampled are YCbCr, unsubsampled are RGB
        // 4-channel subsampled are YCbCrA, unsubsampled are CMYK

        boolean subsampled = false;

        int hfactor = sof.componentSpecs[0].HsamplingFactor;
        int vfactor = sof.componentSpecs[0].VsamplingFactor;

        for (int i = 1; i<sof.componentSpecs.length; i++) {
            if ((sof.componentSpecs[i].HsamplingFactor != hfactor)
                || (sof.componentSpecs[i].VsamplingFactor != vfactor)){
                subsampled = true;
                break;
            }
        }

        if (subsampled) {
            csType.setAttribute("name", "YCbCr");
            if (numChannels == 4) {
                hasAlpha = true;
            }
            return chroma;
        }

        // Not subsampled.  numChannels < 3 is taken care of above
        if (numChannels == 3) {
            csType.setAttribute("name", "RGB");
        } else {
            csType.setAttribute("name", "CMYK");
        }

        return chroma;
    }

    protected IIOMetadataNode getStandardCompressionNode() {

        IIOMetadataNode compression = new IIOMetadataNode("Compression");

        // CompressionTypeName
        IIOMetadataNode name = new IIOMetadataNode("CompressionTypeName");
        name.setAttribute("value", "JPEG");
        compression.appendChild(name);

        // Lossless - false
        IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
        lossless.setAttribute("value", "FALSE");
        compression.appendChild(lossless);

        // NumProgressiveScans - count sos segments
        int sosCount = 0;
        for (MarkerSegment ms : markerSequence) {
            if (ms.tag == JPEG.SOS) {
                sosCount++;
            }
        }
        if (sosCount != 0) {
            IIOMetadataNode prog = new IIOMetadataNode("NumProgressiveScans");
            prog.setAttribute("value", Integer.toString(sosCount));
            compression.appendChild(prog);
        }

        return compression;
    }

    protected IIOMetadataNode getStandardDimensionNode() {
        // If we have a JFIF marker segment, we know a little
        // otherwise all we know is the orientation, which is always normal
        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        IIOMetadataNode orient = new IIOMetadataNode("ImageOrientation");
        orient.setAttribute("value", "normal");
        dim.appendChild(orient);

        JFIFMarkerSegment jfif =
            (JFIFMarkerSegment) findMarkerSegment(JFIFMarkerSegment.class, true);
        if (jfif != null) {

            // Aspect Ratio is width of pixel / height of pixel
            float aspectRatio;
            if (jfif.resUnits == 0) {
                // In this case they just encode aspect ratio directly
                aspectRatio = ((float) jfif.Xdensity)/jfif.Ydensity;
            } else {
                // They are true densities (e.g. dpi) and must be inverted
                aspectRatio = ((float) jfif.Ydensity)/jfif.Xdensity;
            }
            IIOMetadataNode aspect = new IIOMetadataNode("PixelAspectRatio");
            aspect.setAttribute("value", Float.toString(aspectRatio));
            dim.insertBefore(aspect, orient);

            // Pixel size
            if (jfif.resUnits != 0) {
                // 1 == dpi, 2 == dpc
                float scale = (jfif.resUnits == 1) ? 25.4F : 10.0F;

                IIOMetadataNode horiz =
                    new IIOMetadataNode("HorizontalPixelSize");
                horiz.setAttribute("value",
                                   Float.toString(scale/jfif.Xdensity));
                dim.appendChild(horiz);

                IIOMetadataNode vert =
                    new IIOMetadataNode("VerticalPixelSize");
                vert.setAttribute("value",
                                  Float.toString(scale/jfif.Ydensity));
                dim.appendChild(vert);
            }
        }
        return dim;
    }

    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = null;
        // Add a text entry for each COM Marker Segment
        if (findMarkerSegment(JPEG.COM) != null) {
            text = new IIOMetadataNode("Text");
            for (MarkerSegment seg : markerSequence) {
                if (seg.tag == JPEG.COM) {
                    COMMarkerSegment com = (COMMarkerSegment) seg;
                    IIOMetadataNode entry = new IIOMetadataNode("TextEntry");
                    entry.setAttribute("keyword", "comment");
                    entry.setAttribute("value", com.getComment());
                text.appendChild(entry);
                }
            }
        }
        return text;
    }

    protected IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode trans = null;
        if (hasAlpha == true) {
            trans = new IIOMetadataNode("Transparency");
            IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
            alpha.setAttribute("value", "nonpremultiplied"); // Always assume
            trans.appendChild(alpha);
        }
        return trans;
    }

    // Editing

    public boolean isReadOnly() {
        return false;
    }

    public void mergeTree(String formatName, Node root)
        throws IIOInvalidTreeException {
        if (formatName == null) {
            throw new IllegalArgumentException("null formatName!");
        }
        if (root == null) {
            throw new IllegalArgumentException("null root!");
        }
        List<MarkerSegment> copy = null;
        if (resetSequence == null) {
            resetSequence = cloneSequence();  // Deep copy
            copy = resetSequence;  // Avoid cloning twice
        } else {
            copy = cloneSequence();
        }
        if (isStream &&
            (formatName.equals(JPEG.nativeStreamMetadataFormatName))) {
                mergeNativeTree(root);
        } else if (!isStream &&
                   (formatName.equals(JPEG.nativeImageMetadataFormatName))) {
            mergeNativeTree(root);
        } else if (!isStream &&
                   (formatName.equals
                    (IIOMetadataFormatImpl.standardMetadataFormatName))) {
            mergeStandardTree(root);
        } else {
            throw  new IllegalArgumentException("Unsupported format name: "
                                                + formatName);
        }
        if (!isConsistent()) {
            markerSequence = copy;
            throw new IIOInvalidTreeException
                ("Merged tree is invalid; original restored", root);
        }
    }

    private void mergeNativeTree(Node root) throws IIOInvalidTreeException {
        String name = root.getNodeName();
        if (name != ((isStream) ? JPEG.nativeStreamMetadataFormatName
                                : JPEG.nativeImageMetadataFormatName)) {
            throw new IIOInvalidTreeException("Invalid root node name: " + name,
                                              root);
        }
        if (root.getChildNodes().getLength() != 2) { // JPEGvariety and markerSequence
            throw new IIOInvalidTreeException(
                "JPEGvariety and markerSequence nodes must be present", root);
        }
        mergeJFIFsubtree(root.getFirstChild());
        mergeSequenceSubtree(root.getLastChild());
    }

    /**
     * Merge a JFIF subtree into the marker sequence, if the subtree
     * is non-empty.
     * If a JFIF marker exists, update it from the subtree.
     * If none exists, create one from the subtree and insert it at the
     * beginning of the marker sequence.
     */
    private void mergeJFIFsubtree(Node JPEGvariety)
        throws IIOInvalidTreeException {
        if (JPEGvariety.getChildNodes().getLength() != 0) {
            Node jfifNode = JPEGvariety.getFirstChild();
            // is there already a jfif marker segment?
            JFIFMarkerSegment jfifSeg =
                (JFIFMarkerSegment) findMarkerSegment(JFIFMarkerSegment.class, true);
            if (jfifSeg != null) {
                jfifSeg.updateFromNativeNode(jfifNode, false);
            } else {
                // Add it as the first element in the list.
                markerSequence.add(0, new JFIFMarkerSegment(jfifNode));
            }
        }
    }

    private void mergeSequenceSubtree(Node sequenceTree)
        throws IIOInvalidTreeException {
        NodeList children = sequenceTree.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            String name = node.getNodeName();
            if (name.equals("dqt")) {
                mergeDQTNode(node);
            } else if (name.equals("dht")) {
                mergeDHTNode(node);
            } else if (name.equals("dri")) {
                mergeDRINode(node);
            } else if (name.equals("com")) {
                mergeCOMNode(node);
            } else if (name.equals("app14Adobe")) {
                mergeAdobeNode(node);
            } else if (name.equals("unknown")) {
                mergeUnknownNode(node);
            } else if (name.equals("sof")) {
                mergeSOFNode(node);
            } else if (name.equals("sos")) {
                mergeSOSNode(node);
            } else {
                throw new IIOInvalidTreeException("Invalid node: " + name, node);
            }
        }
    }

    /**
     * Merge the given DQT node into the marker sequence.  If there already
     * exist DQT marker segments in the sequence, then each table in the
     * node replaces the first table, in any DQT segment, with the same
     * table id.  If none of the existing DQT segments contain a table with
     * the same id, then the table is added to the last existing DQT segment.
     * If there are no DQT segments, then a new one is created and added
     * as follows:
     * If there are DHT segments, the new DQT segment is inserted before the
     * first one.
     * If there are no DHT segments, the new DQT segment is inserted before
     * an SOF segment, if there is one.
     * If there is no SOF segment, the new DQT segment is inserted before
     * the first SOS segment, if there is one.
     * If there is no SOS segment, the new DQT segment is added to the end
     * of the sequence.
     */
    private void mergeDQTNode(Node node) throws IIOInvalidTreeException {
        // First collect any existing DQT nodes into a local list
        ArrayList<DQTMarkerSegment> oldDQTs = new ArrayList<>();
        for (MarkerSegment seg : markerSequence) {
            if (seg instanceof DQTMarkerSegment) {
                oldDQTs.add((DQTMarkerSegment) seg);
            }
        }
        if (!oldDQTs.isEmpty()) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                int childID = MarkerSegment.getAttributeValue(child,
                                                              null,
                                                              "qtableId",
                                                              0, 3,
                                                              true);
                DQTMarkerSegment dqt = null;
                int tableIndex = -1;
                for (int j = 0; j < oldDQTs.size(); j++) {
                    DQTMarkerSegment testDQT = oldDQTs.get(j);
                    for (int k = 0; k < testDQT.tables.size(); k++) {
                        DQTMarkerSegment.Qtable testTable = testDQT.tables.get(k);
                        if (childID == testTable.tableID) {
                            dqt = testDQT;
                            tableIndex = k;
                            break;
                        }
                    }
                    if (dqt != null) break;
                }
                if (dqt != null) {
                    dqt.tables.set(tableIndex, dqt.getQtableFromNode(child));
                } else {
                    dqt = oldDQTs.get(oldDQTs.size()-1);
                    dqt.tables.add(dqt.getQtableFromNode(child));
                }
            }
        } else {
            DQTMarkerSegment newGuy = new DQTMarkerSegment(node);
            int firstDHT = findMarkerSegmentPosition(DHTMarkerSegment.class, true);
            int firstSOF = findMarkerSegmentPosition(SOFMarkerSegment.class, true);
            int firstSOS = findMarkerSegmentPosition(SOSMarkerSegment.class, true);
            if (firstDHT != -1) {
                markerSequence.add(firstDHT, newGuy);
            } else if (firstSOF != -1) {
                markerSequence.add(firstSOF, newGuy);
            } else if (firstSOS != -1) {
                markerSequence.add(firstSOS, newGuy);
            } else {
                markerSequence.add(newGuy);
            }
        }
    }

    /**
     * Merge the given DHT node into the marker sequence.  If there already
     * exist DHT marker segments in the sequence, then each table in the
     * node replaces the first table, in any DHT segment, with the same
     * table class and table id.  If none of the existing DHT segments contain
     * a table with the same class and id, then the table is added to the last
     * existing DHT segment.
     * If there are no DHT segments, then a new one is created and added
     * as follows:
     * If there are DQT segments, the new DHT segment is inserted immediately
     * following the last DQT segment.
     * If there are no DQT segments, the new DHT segment is inserted before
     * an SOF segment, if there is one.
     * If there is no SOF segment, the new DHT segment is inserted before
     * the first SOS segment, if there is one.
     * If there is no SOS segment, the new DHT segment is added to the end
     * of the sequence.
     */
    private void mergeDHTNode(Node node) throws IIOInvalidTreeException {
        // First collect any existing DQT nodes into a local list
        ArrayList<DHTMarkerSegment> oldDHTs = new ArrayList<>();
        for (MarkerSegment seg : markerSequence) {
            if (seg instanceof DHTMarkerSegment) {
                oldDHTs.add((DHTMarkerSegment) seg);
            }
        }
        if (!oldDHTs.isEmpty()) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                NamedNodeMap attrs = child.getAttributes();
                int childID = MarkerSegment.getAttributeValue(child,
                                                              attrs,
                                                              "htableId",
                                                              0, 3,
                                                              true);
                int childClass = MarkerSegment.getAttributeValue(child,
                                                                 attrs,
                                                                 "class",
                                                                 0, 1,
                                                                 true);
                DHTMarkerSegment dht = null;
                int tableIndex = -1;
                for (int j = 0; j < oldDHTs.size(); j++) {
                    DHTMarkerSegment testDHT = oldDHTs.get(j);
                    for (int k = 0; k < testDHT.tables.size(); k++) {
                        DHTMarkerSegment.Htable testTable = testDHT.tables.get(k);
                        if ((childID == testTable.tableID) &&
                            (childClass == testTable.tableClass)) {
                            dht = testDHT;
                            tableIndex = k;
                            break;
                        }
                    }
                    if (dht != null) break;
                }
                if (dht != null) {
                    dht.tables.set(tableIndex, dht.getHtableFromNode(child));
                } else {
                    dht = oldDHTs.get(oldDHTs.size()-1);
                    dht.tables.add(dht.getHtableFromNode(child));
                }
            }
        } else {
            DHTMarkerSegment newGuy = new DHTMarkerSegment(node);
            int lastDQT = findMarkerSegmentPosition(DQTMarkerSegment.class, false);
            int firstSOF = findMarkerSegmentPosition(SOFMarkerSegment.class, true);
            int firstSOS = findMarkerSegmentPosition(SOSMarkerSegment.class, true);
            if (lastDQT != -1) {
                markerSequence.add(lastDQT+1, newGuy);
            } else if (firstSOF != -1) {
                markerSequence.add(firstSOF, newGuy);
            } else if (firstSOS != -1) {
                markerSequence.add(firstSOS, newGuy);
            } else {
                markerSequence.add(newGuy);
            }
        }
    }

    /**
     * Merge the given DRI node into the marker sequence.
     * If there already exists a DRI marker segment, the restart interval
     * value is updated.
     * If there is no DRI segment, then a new one is created and added as
     * follows:
     * If there is an SOF segment, the new DRI segment is inserted before
     * it.
     * If there is no SOF segment, the new DRI segment is inserted before
     * the first SOS segment, if there is one.
     * If there is no SOS segment, the new DRI segment is added to the end
     * of the sequence.
     */
    private void mergeDRINode(Node node) throws IIOInvalidTreeException {
        DRIMarkerSegment dri =
            (DRIMarkerSegment) findMarkerSegment(DRIMarkerSegment.class, true);
        if (dri != null) {
            dri.updateFromNativeNode(node, false);
        } else {
            DRIMarkerSegment newGuy = new DRIMarkerSegment(node);
            int firstSOF = findMarkerSegmentPosition(SOFMarkerSegment.class, true);
            int firstSOS = findMarkerSegmentPosition(SOSMarkerSegment.class, true);
            if (firstSOF != -1) {
                markerSequence.add(firstSOF, newGuy);
            } else if (firstSOS != -1) {
                markerSequence.add(firstSOS, newGuy);
            } else {
                markerSequence.add(newGuy);
            }
        }
    }

    /**
     * Merge the given COM node into the marker sequence.
     * A new COM marker segment is created and added to the sequence
     * using insertCOMMarkerSegment.
     */
    private void mergeCOMNode(Node node) throws IIOInvalidTreeException {
        COMMarkerSegment newGuy = new COMMarkerSegment(node);
        insertCOMMarkerSegment(newGuy);
    }

     /**
      * Insert a new COM marker segment into an appropriate place in the
      * marker sequence, as follows:
      * If there already exist COM marker segments, the new one is inserted
      * after the last one.
      * If there are no COM segments, the new COM segment is inserted after the
      * JFIF segment, if there is one.
      * If there is no JFIF segment, the new COM segment is inserted after the
      * Adobe marker segment, if there is one.
      * If there is no Adobe segment, the new COM segment is inserted
      * at the beginning of the sequence.
      */
    private void insertCOMMarkerSegment(COMMarkerSegment newGuy) {
        int lastCOM = findMarkerSegmentPosition(COMMarkerSegment.class, false);
        boolean hasJFIF = (findMarkerSegment(JFIFMarkerSegment.class, true) != null);
        int firstAdobe = findMarkerSegmentPosition(AdobeMarkerSegment.class, true);
        if (lastCOM != -1) {
            markerSequence.add(lastCOM+1, newGuy);
        } else if (hasJFIF) {
            markerSequence.add(1, newGuy);  // JFIF is always 0
        } else if (firstAdobe != -1) {
            markerSequence.add(firstAdobe+1, newGuy);
        } else {
            markerSequence.add(0, newGuy);
        }
    }

    /**
     * Merge the given Adobe APP14 node into the marker sequence.
     * If there already exists an Adobe marker segment, then its attributes
     * are updated from the node.
     * If there is no Adobe segment, then a new one is created and added
     * using insertAdobeMarkerSegment.
     */
    private void mergeAdobeNode(Node node) throws IIOInvalidTreeException {
        AdobeMarkerSegment adobe =
            (AdobeMarkerSegment) findMarkerSegment(AdobeMarkerSegment.class, true);
        if (adobe != null) {
            adobe.updateFromNativeNode(node, false);
        } else {
            AdobeMarkerSegment newGuy = new AdobeMarkerSegment(node);
            insertAdobeMarkerSegment(newGuy);
        }
    }

    /**
     * Insert the given AdobeMarkerSegment into the marker sequence, as
     * follows (we assume there is no Adobe segment yet):
     * If there is a JFIF segment, then the new Adobe segment is inserted
     * after it.
     * If there is no JFIF segment, the new Adobe segment is inserted after the
     * last Unknown segment, if there are any.
     * If there are no Unknown segments, the new Adobe segment is inserted
     * at the beginning of the sequence.
     */
    private void insertAdobeMarkerSegment(AdobeMarkerSegment newGuy) {
        boolean hasJFIF =
            (findMarkerSegment(JFIFMarkerSegment.class, true) != null);
        int lastUnknown = findLastUnknownMarkerSegmentPosition();
        if (hasJFIF) {
            markerSequence.add(1, newGuy);  // JFIF is always 0
        } else if (lastUnknown != -1) {
            markerSequence.add(lastUnknown+1, newGuy);
        } else {
            markerSequence.add(0, newGuy);
        }
    }

    /**
     * Merge the given Unknown node into the marker sequence.
     * A new Unknown marker segment is created and added to the sequence as
     * follows:
     * If there already exist Unknown marker segments, the new one is inserted
     * after the last one.
     * If there are no Unknown marker segments, the new Unknown marker segment
     * is inserted after the JFIF segment, if there is one.
     * If there is no JFIF segment, the new Unknown segment is inserted before
     * the Adobe marker segment, if there is one.
     * If there is no Adobe segment, the new Unknown segment is inserted
     * at the beginning of the sequence.
     */
    private void mergeUnknownNode(Node node) throws IIOInvalidTreeException {
        MarkerSegment newGuy = new MarkerSegment(node);
        int lastUnknown = findLastUnknownMarkerSegmentPosition();
        boolean hasJFIF = (findMarkerSegment(JFIFMarkerSegment.class, true) != null);
        int firstAdobe = findMarkerSegmentPosition(AdobeMarkerSegment.class, true);
        if (lastUnknown != -1) {
            markerSequence.add(lastUnknown+1, newGuy);
        } else if (hasJFIF) {
            markerSequence.add(1, newGuy);  // JFIF is always 0
        } if (firstAdobe != -1) {
            markerSequence.add(firstAdobe, newGuy);
        } else {
            markerSequence.add(0, newGuy);
        }
    }

    /**
     * Merge the given SOF node into the marker sequence.
     * If there already exists an SOF marker segment in the sequence, then
     * its values are updated from the node.
     * If there is no SOF segment, then a new one is created and added as
     * follows:
     * If there are any SOS segments, the new SOF segment is inserted before
     * the first one.
     * If there is no SOS segment, the new SOF segment is added to the end
     * of the sequence.
     *
     */
    private void mergeSOFNode(Node node) throws IIOInvalidTreeException {
        SOFMarkerSegment sof =
            (SOFMarkerSegment) findMarkerSegment(SOFMarkerSegment.class, true);
        if (sof != null) {
            sof.updateFromNativeNode(node, false);
        } else {
            SOFMarkerSegment newGuy = new SOFMarkerSegment(node);
            int firstSOS = findMarkerSegmentPosition(SOSMarkerSegment.class, true);
            if (firstSOS != -1) {
                markerSequence.add(firstSOS, newGuy);
            } else {
                markerSequence.add(newGuy);
            }
        }
    }

    /**
     * Merge the given SOS node into the marker sequence.
     * If there already exists a single SOS marker segment, then the values
     * are updated from the node.
     * If there are more than one existing SOS marker segments, then an
     * IIOInvalidTreeException is thrown, as SOS segments cannot be merged
     * into a set of progressive scans.
     * If there are no SOS marker segments, a new one is created and added
     * to the end of the sequence.
     */
    private void mergeSOSNode(Node node) throws IIOInvalidTreeException {
        SOSMarkerSegment firstSOS =
            (SOSMarkerSegment) findMarkerSegment(SOSMarkerSegment.class, true);
        SOSMarkerSegment lastSOS =
            (SOSMarkerSegment) findMarkerSegment(SOSMarkerSegment.class, false);
        if (firstSOS != null) {
            if (firstSOS != lastSOS) {
                throw new IIOInvalidTreeException
                    ("Can't merge SOS node into a tree with > 1 SOS node", node);
            }
            firstSOS.updateFromNativeNode(node, false);
        } else {
            markerSequence.add(new SOSMarkerSegment(node));
        }
    }

    private boolean transparencyDone;

    private void mergeStandardTree(Node root) throws IIOInvalidTreeException {
        transparencyDone = false;
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            String name = node.getNodeName();
            if (name.equals("Chroma")) {
                mergeStandardChromaNode(node, children);
            } else if (name.equals("Compression")) {
                mergeStandardCompressionNode(node);
            } else if (name.equals("Data")) {
                mergeStandardDataNode(node);
            } else if (name.equals("Dimension")) {
                mergeStandardDimensionNode(node);
            } else if (name.equals("Document")) {
                mergeStandardDocumentNode(node);
            } else if (name.equals("Text")) {
                mergeStandardTextNode(node);
            } else if (name.equals("Transparency")) {
                mergeStandardTransparencyNode(node);
            } else {
                throw new IIOInvalidTreeException("Invalid node: " + name, node);
            }
        }
    }

    /*
     * In general, it could be possible to convert all non-pixel data to some
     * textual form and include it in comments, but then this would create the
     * expectation that these comment forms be recognized by the reader, thus
     * creating a defacto extension to JPEG metadata capabilities.  This is
     * probably best avoided, so the following convert only text nodes to
     * comments, and lose the keywords as well.
     */

    private void mergeStandardChromaNode(Node node, NodeList siblings)
        throws IIOInvalidTreeException {
        // ColorSpaceType can change the target colorspace for compression
        // This must take any transparency node into account as well, as
        // that affects the number of channels (if alpha is present).  If
        // a transparency node is dealt with here, set a flag to indicate
        // this to the transparency processor below.  If we discover that
        // the nodes are not in order, throw an exception as the tree is
        // invalid.

        if (transparencyDone) {
            throw new IIOInvalidTreeException
                ("Transparency node must follow Chroma node", node);
        }

        Node csType = node.getFirstChild();
        if ((csType == null) || !csType.getNodeName().equals("ColorSpaceType")) {
            // If there is no ColorSpaceType node, we have nothing to do
            return;
        }

        String csName = csType.getAttributes().getNamedItem("name").getNodeValue();

        int numChannels = 0;
        boolean wantJFIF = false;
        boolean wantAdobe = false;
        int transform = 0;
        boolean willSubsample = false;
        byte [] ids = {1, 2, 3, 4};  // JFIF compatible
        if (csName.equals("GRAY")) {
            numChannels = 1;
            wantJFIF = true;
        } else if (csName.equals("YCbCr")) {
            numChannels = 3;
            wantJFIF = true;
            willSubsample = true;
        } else if (csName.equals("PhotoYCC")) {
            numChannels = 3;
            wantAdobe = true;
            transform = JPEG.ADOBE_YCC;
            ids[0] = (byte) 'Y';
            ids[1] = (byte) 'C';
            ids[2] = (byte) 'c';
        } else if (csName.equals("RGB")) {
            numChannels = 3;
            wantAdobe = true;
            transform = JPEG.ADOBE_UNKNOWN;
            ids[0] = (byte) 'R';
            ids[1] = (byte) 'G';
            ids[2] = (byte) 'B';
        } else if ((csName.equals("XYZ"))
                   || (csName.equals("Lab"))
                   || (csName.equals("Luv"))
                   || (csName.equals("YxY"))
                   || (csName.equals("HSV"))
                   || (csName.equals("HLS"))
                   || (csName.equals("CMY"))
                   || (csName.equals("3CLR"))) {
            numChannels = 3;
        } else if (csName.equals("YCCK")) {
            numChannels = 4;
            wantAdobe = true;
            transform = JPEG.ADOBE_YCCK;
            willSubsample = true;
        } else if (csName.equals("CMYK")) {
            numChannels = 4;
            wantAdobe = true;
            transform = JPEG.ADOBE_UNKNOWN;
        } else if (csName.equals("4CLR")) {
            numChannels = 4;
        } else { // We can't handle them, so don't modify any metadata
            return;
        }

        boolean wantAlpha = false;
        for (int i = 0; i < siblings.getLength(); i++) {
            Node trans = siblings.item(i);
            if (trans.getNodeName().equals("Transparency")) {
                wantAlpha = wantAlpha(trans);
                break;  // out of for
            }
        }

        if (wantAlpha) {
            numChannels++;
            wantJFIF = false;
            if (ids[0] == (byte) 'R') {
                ids[3] = (byte) 'A';
                wantAdobe = false;
            }
        }

        JFIFMarkerSegment jfif =
            (JFIFMarkerSegment) findMarkerSegment(JFIFMarkerSegment.class, true);
        AdobeMarkerSegment adobe =
            (AdobeMarkerSegment) findMarkerSegment(AdobeMarkerSegment.class, true);
        SOFMarkerSegment sof =
            (SOFMarkerSegment) findMarkerSegment(SOFMarkerSegment.class, true);
        SOSMarkerSegment sos =
            (SOSMarkerSegment) findMarkerSegment(SOSMarkerSegment.class, true);

        // If the metadata specifies progressive, then the number of channels
        // must match, so that we can modify all the existing SOS marker segments.
        // If they don't match, we don't know what to do with SOS so we can't do
        // the merge.  We then just return silently.
        // An exception would not be appropriate.  A warning might, but we have
        // nowhere to send it to.
        if ((sof != null) && (sof.tag == JPEG.SOF2)) { // Progressive
            if ((sof.componentSpecs.length != numChannels) && (sos != null)) {
                return;
            }
        }

        // JFIF header might be removed
        if (!wantJFIF && (jfif != null)) {
            markerSequence.remove(jfif);
        }

        // Now add a JFIF if we do want one, but only if it isn't stream metadata
        if (wantJFIF && !isStream) {
            markerSequence.add(0, new JFIFMarkerSegment());
        }

        // Adobe header might be removed or the transform modified, if it isn't
        // stream metadata
        if (wantAdobe) {
            if ((adobe == null) && !isStream) {
                adobe = new AdobeMarkerSegment(transform);
                insertAdobeMarkerSegment(adobe);
            } else {
                adobe.transform = transform;
            }
        } else if (adobe != null) {
            markerSequence.remove(adobe);
        }

        boolean updateQtables = false;
        boolean updateHtables = false;

        boolean progressive = false;

        int [] subsampledSelectors = {0, 1, 1, 0 } ;
        int [] nonSubsampledSelectors = { 0, 0, 0, 0};

        int [] newTableSelectors = willSubsample
                                   ? subsampledSelectors
                                   : nonSubsampledSelectors;

        // Keep the old componentSpecs array
        SOFMarkerSegment.ComponentSpec [] oldCompSpecs = null;
        // SOF might be modified
        if (sof != null) {
            oldCompSpecs = sof.componentSpecs;
            progressive = (sof.tag == JPEG.SOF2);
            // Now replace the SOF with a new one; it might be the same, but
            // this is easier.
            markerSequence.set(markerSequence.indexOf(sof),
                               new SOFMarkerSegment(progressive,
                                                    false, // we never need extended
                                                    willSubsample,
                                                    ids,
                                                    numChannels));

            // Now suss out if subsampling changed and set the boolean for
            // updating the q tables
            // if the old componentSpec q table selectors don't match
            // the new ones, update the qtables.  The new selectors are already
            // in place in the new SOF segment above.
            for (int i = 0; i < oldCompSpecs.length; i++) {
                if (oldCompSpecs[i].QtableSelector != newTableSelectors[i]) {
                    updateQtables = true;
                }
            }

            if (progressive) {
                // if the component ids are different, update all the existing scans
                // ignore Huffman tables
                boolean idsDiffer = false;
                for (int i = 0; i < oldCompSpecs.length; i++) {
                    if (ids[i] != oldCompSpecs[i].componentId) {
                        idsDiffer = true;
                    }
                }
                if (idsDiffer) {
                    // update the ids in each SOS marker segment
                    for (Iterator<MarkerSegment> iter = markerSequence.iterator();
                            iter.hasNext();) {
                        MarkerSegment seg = iter.next();
                        if (seg instanceof SOSMarkerSegment) {
                            SOSMarkerSegment target = (SOSMarkerSegment) seg;
                            for (int i = 0; i < target.componentSpecs.length; i++) {
                                int oldSelector =
                                    target.componentSpecs[i].componentSelector;
                                // Find the position in the old componentSpecs array
                                // of the old component with the old selector
                                // and replace the component selector with the
                                // new id at the same position, as these match
                                // the new component specs array in the SOF created
                                // above.
                                for (int j = 0; j < oldCompSpecs.length; j++) {
                                    if (oldCompSpecs[j].componentId == oldSelector) {
                                        target.componentSpecs[i].componentSelector =
                                            ids[j];
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (sos != null) {
                    // htables - if the old htable selectors don't match the new ones,
                    // update the tables.
                    for (int i = 0; i < sos.componentSpecs.length; i++) {
                        if ((sos.componentSpecs[i].dcHuffTable
                             != newTableSelectors[i])
                            || (sos.componentSpecs[i].acHuffTable
                                != newTableSelectors[i])) {
                            updateHtables = true;
                        }
                    }

                    // Might be the same as the old one, but this is easier.
                    markerSequence.set(markerSequence.indexOf(sos),
                               new SOSMarkerSegment(willSubsample,
                                                    ids,
                                                    numChannels));
                }
            }
        } else {
            // should be stream metadata if there isn't an SOF, but check it anyway
            if (isStream) {
                // update tables - routines below check if it's really necessary
                updateQtables = true;
                updateHtables = true;
            }
        }

        if (updateQtables) {
            List<DQTMarkerSegment> tableSegments = new ArrayList<>();
            for (Iterator<MarkerSegment> iter = markerSequence.iterator();
                    iter.hasNext();) {
                MarkerSegment seg = iter.next();
                if (seg instanceof DQTMarkerSegment) {
                    tableSegments.add((DQTMarkerSegment) seg);
                }
            }
            // If there are no tables, don't add them, as the metadata encodes an
            // abbreviated stream.
            // If we are not subsampling, we just need one, so don't do anything
            if (!tableSegments.isEmpty() && willSubsample) {
                // Is it really necessary?  There should be at least 2 tables.
                // If there is only one, assume it's a scaled "standard"
                // luminance table, extract the scaling factor, and generate a
                // scaled "standard" chrominance table.

                // Find the table with selector 1.
                boolean found = false;
                for (Iterator<DQTMarkerSegment> iter = tableSegments.iterator();
                        iter.hasNext();) {
                    DQTMarkerSegment testdqt = iter.next();
                    for (Iterator<DQTMarkerSegment.Qtable> tabiter =
                            testdqt.tables.iterator(); tabiter.hasNext();) {
                        DQTMarkerSegment.Qtable tab = tabiter.next();
                        if (tab.tableID == 1) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    //    find the table with selector 0.  There should be one.
                    DQTMarkerSegment.Qtable table0 = null;
                    for (Iterator<DQTMarkerSegment> iter =
                            tableSegments.iterator(); iter.hasNext();) {
                        DQTMarkerSegment testdqt = iter.next();
                        for (Iterator<DQTMarkerSegment.Qtable> tabiter =
                                testdqt.tables.iterator(); tabiter.hasNext();) {
                            DQTMarkerSegment.Qtable tab = tabiter.next();
                            if (tab.tableID == 0) {
                                table0 = tab;
                            }
                        }
                    }

                    // Assuming that the table with id 0 is a luminance table,
                    // compute a new chrominance table of the same quality and
                    // add it to the last DQT segment
                    DQTMarkerSegment dqt = tableSegments.get(tableSegments.size()-1);
                    dqt.tables.add(dqt.getChromaForLuma(table0));
                }
            }
        }

        if (updateHtables) {
            List<DHTMarkerSegment> tableSegments = new ArrayList<>();
            for (Iterator<MarkerSegment> iter = markerSequence.iterator();
                    iter.hasNext();) {
                MarkerSegment seg = iter.next();
                if (seg instanceof DHTMarkerSegment) {
                    tableSegments.add((DHTMarkerSegment) seg);
                }
            }
            // If there are no tables, don't add them, as the metadata encodes an
            // abbreviated stream.
            // If we are not subsampling, we just need one, so don't do anything
            if (!tableSegments.isEmpty() && willSubsample) {
                // Is it really necessary?  There should be at least 2 dc and 2 ac
                // tables.  If there is only one, add a
                // "standard " chrominance table.

                // find a table with selector 1. AC/DC is irrelevant
                boolean found = false;
                for (Iterator<DHTMarkerSegment> iter = tableSegments.iterator();
                        iter.hasNext();) {
                    DHTMarkerSegment testdht = iter.next();
                    for (Iterator<DHTMarkerSegment.Htable> tabiter =
                            testdht.tables.iterator(); tabiter.hasNext();) {
                        DHTMarkerSegment.Htable tab = tabiter.next();
                        if (tab.tableID == 1) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    // Create new standard dc and ac chrominance tables and add them
                    // to the last DHT segment
                    DHTMarkerSegment lastDHT =
                        tableSegments.get(tableSegments.size()-1);
                    lastDHT.addHtable(JPEGHuffmanTable.StdDCLuminance, true, 1);
                    lastDHT.addHtable(JPEGHuffmanTable.StdACLuminance, true, 1);
                }
            }
        }
    }

    private boolean wantAlpha(Node transparency) {
        boolean returnValue = false;
        Node alpha = transparency.getFirstChild();  // Alpha must be first if present
        if (alpha.getNodeName().equals("Alpha")) {
            if (alpha.hasAttributes()) {
                String value =
                    alpha.getAttributes().getNamedItem("value").getNodeValue();
                if (!value.equals("none")) {
                    returnValue = true;
                }
            }
        }
        transparencyDone = true;
        return returnValue;
    }

    private void mergeStandardCompressionNode(Node node)
        throws IIOInvalidTreeException {
        // NumProgressiveScans is ignored.  Progression must be enabled on the
        // ImageWriteParam.
        // No-op
    }

    private void mergeStandardDataNode(Node node)
        throws IIOInvalidTreeException {
        // No-op
    }

    private void mergeStandardDimensionNode(Node node)
        throws IIOInvalidTreeException {
        // Pixel Aspect Ratio or pixel size can be incorporated if there is,
        // or can be, a JFIF segment
        JFIFMarkerSegment jfif =
            (JFIFMarkerSegment) findMarkerSegment(JFIFMarkerSegment.class, true);
        if (jfif == null) {
            // Can there be one?
            // Criteria:
            // SOF must be present with 1 or 3 channels, (stream metadata fails this)
            //     Component ids must be JFIF compatible.
            boolean canHaveJFIF = false;
            SOFMarkerSegment sof =
                (SOFMarkerSegment) findMarkerSegment(SOFMarkerSegment.class, true);
            if (sof != null) {
                int numChannels = sof.componentSpecs.length;
                if ((numChannels == 1) || (numChannels == 3)) {
                    canHaveJFIF = true; // remaining tests are negative
                    for (int i = 0; i < sof.componentSpecs.length; i++) {
                        if (sof.componentSpecs[i].componentId != i+1)
                            canHaveJFIF = false;
                    }
                    // if Adobe present, transform = ADOBE_UNKNOWN for 1-channel,
                    //     ADOBE_YCC for 3-channel.
                    AdobeMarkerSegment adobe =
                        (AdobeMarkerSegment) findMarkerSegment(AdobeMarkerSegment.class,
                                                               true);
                    if (adobe != null) {
                        if (adobe.transform != ((numChannels == 1)
                                                ? JPEG.ADOBE_UNKNOWN
                                                : JPEG.ADOBE_YCC)) {
                            canHaveJFIF = false;
                        }
                    }
                }
            }
            // If so, create one and insert it into the sequence.  Note that
            // default is just pixel ratio at 1:1
            if (canHaveJFIF) {
                jfif = new JFIFMarkerSegment();
                markerSequence.add(0, jfif);
            }
        }
        if (jfif != null) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                NamedNodeMap attrs = child.getAttributes();
                String name = child.getNodeName();
                if (name.equals("PixelAspectRatio")) {
                    String valueString = attrs.getNamedItem("value").getNodeValue();
                    float value = Float.parseFloat(valueString);
                    Point p = findIntegerRatio(value);
                    jfif.resUnits = JPEG.DENSITY_UNIT_ASPECT_RATIO;
                    jfif.Xdensity = p.x;
                    jfif.Xdensity = p.y;
                } else if (name.equals("HorizontalPixelSize")) {
                    String valueString = attrs.getNamedItem("value").getNodeValue();
                    float value = Float.parseFloat(valueString);
                    // Convert from mm/dot to dots/cm
                    int dpcm = (int) Math.round(1.0/(value*10.0));
                    jfif.resUnits = JPEG.DENSITY_UNIT_DOTS_CM;
                    jfif.Xdensity = dpcm;
                } else if (name.equals("VerticalPixelSize")) {
                    String valueString = attrs.getNamedItem("value").getNodeValue();
                    float value = Float.parseFloat(valueString);
                    // Convert from mm/dot to dots/cm
                    int dpcm = (int) Math.round(1.0/(value*10.0));
                    jfif.resUnits = JPEG.DENSITY_UNIT_DOTS_CM;
                    jfif.Ydensity = dpcm;
                }

            }
        }
    }

    /*
     * Return a pair of integers whose ratio (x/y) approximates the given
     * float value.
     */
    private static Point findIntegerRatio(float value) {
        float epsilon = 0.005F;

        // Normalize
        value = Math.abs(value);

        // Deal with min case
        if (value <= epsilon) {
            return new Point(1, 255);
        }

        // Deal with max case
        if (value >= 255) {
            return new Point(255, 1);
        }

        // Remember if we invert
        boolean inverted = false;
        if (value < 1.0) {
            value = 1.0F/value;
            inverted = true;
        }

        // First approximation
        int y = 1;
        int x = Math.round(value);

        float ratio = (float) x;
        float delta = Math.abs(value - ratio);
        while (delta > epsilon) { // not close enough
            // Increment y and compute a new x
            y++;
            x = Math.round(y*value);
            ratio = (float)x/(float)y;
            delta = Math.abs(value - ratio);
        }
        return inverted ? new Point(y, x) : new Point(x, y);
    }

    private void mergeStandardDocumentNode(Node node)
        throws IIOInvalidTreeException {
        // No-op
    }

    private void mergeStandardTextNode(Node node)
        throws IIOInvalidTreeException {
        // Convert to comments.  For the moment ignore the encoding issue.
        // Ignore keywords, language, and encoding (for the moment).
        // If compression tag is present, use only entries with "none".
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            NamedNodeMap attrs = child.getAttributes();
            Node comp = attrs.getNamedItem("compression");
            boolean copyIt = true;
            if (comp != null) {
                String compString = comp.getNodeValue();
                if (!compString.equals("none")) {
                    copyIt = false;
                }
            }
            if (copyIt) {
                String value = attrs.getNamedItem("value").getNodeValue();
                COMMarkerSegment com = new COMMarkerSegment(value);
                insertCOMMarkerSegment(com);
            }
        }
    }

    private void mergeStandardTransparencyNode(Node node)
        throws IIOInvalidTreeException {
        // This might indicate that an alpha channel is being added or removed.
        // The nodes must appear in order, and a Chroma node will process any
        // transparency, so process it here only if there was no Chroma node
        // Do nothing for stream metadata
        if (!transparencyDone && !isStream) {
            boolean wantAlpha = wantAlpha(node);
            // do we have alpha already?  If the number of channels is 2 or 4,
            // we do, as we don't support CMYK, nor can we add alpha to it
            // The number of channels can be determined from the SOF
            JFIFMarkerSegment jfif = (JFIFMarkerSegment) findMarkerSegment
                (JFIFMarkerSegment.class, true);
            AdobeMarkerSegment adobe = (AdobeMarkerSegment) findMarkerSegment
                (AdobeMarkerSegment.class, true);
            SOFMarkerSegment sof = (SOFMarkerSegment) findMarkerSegment
                (SOFMarkerSegment.class, true);
            SOSMarkerSegment sos = (SOSMarkerSegment) findMarkerSegment
                (SOSMarkerSegment.class, true);

            // We can do nothing for progressive, as we don't know how to
            // modify the scans.
            if ((sof != null) && (sof.tag == JPEG.SOF2)) { // Progressive
                return;
            }

            // Do we already have alpha?  We can tell by the number of channels
            // We must have an sof, or we can't do anything further
            if (sof != null) {
                int numChannels = sof.componentSpecs.length;
                boolean hadAlpha = (numChannels == 2) || (numChannels == 4);
                // proceed only if the old state and the new state differ
                if (hadAlpha != wantAlpha) {
                    if (wantAlpha) {  // Adding alpha
                        numChannels++;
                        if (jfif != null) {
                            markerSequence.remove(jfif);
                        }

                        // If an adobe marker is present, transform must be UNKNOWN
                        if (adobe != null) {
                            adobe.transform = JPEG.ADOBE_UNKNOWN;
                        }

                        // Add a component spec with appropriate parameters to SOF
                        SOFMarkerSegment.ComponentSpec [] newSpecs =
                            new SOFMarkerSegment.ComponentSpec[numChannels];
                        for (int i = 0; i < sof.componentSpecs.length; i++) {
                            newSpecs[i] = sof.componentSpecs[i];
                        }
                        byte oldFirstID = (byte) sof.componentSpecs[0].componentId;
                        byte newID = (byte) ((oldFirstID > 1) ? 'A' : 4);
                        newSpecs[numChannels-1] =
                            sof.getComponentSpec(newID,
                                sof.componentSpecs[0].HsamplingFactor,
                                sof.componentSpecs[0].QtableSelector);

                        sof.componentSpecs = newSpecs;

                        // Add a component spec with appropriate parameters to SOS
                        SOSMarkerSegment.ScanComponentSpec [] newScanSpecs =
                            new SOSMarkerSegment.ScanComponentSpec [numChannels];
                        for (int i = 0; i < sos.componentSpecs.length; i++) {
                            newScanSpecs[i] = sos.componentSpecs[i];
                        }
                        newScanSpecs[numChannels-1] =
                            sos.getScanComponentSpec (newID, 0);
                        sos.componentSpecs = newScanSpecs;
                    } else {  // Removing alpha
                        numChannels--;
                        // Remove a component spec from SOF
                        SOFMarkerSegment.ComponentSpec [] newSpecs =
                            new SOFMarkerSegment.ComponentSpec[numChannels];
                        for (int i = 0; i < numChannels; i++) {
                            newSpecs[i] = sof.componentSpecs[i];
                        }
                        sof.componentSpecs = newSpecs;

                        // Remove a component spec from SOS
                        SOSMarkerSegment.ScanComponentSpec [] newScanSpecs =
                            new SOSMarkerSegment.ScanComponentSpec [numChannels];
                        for (int i = 0; i < numChannels; i++) {
                            newScanSpecs[i] = sos.componentSpecs[i];
                        }
                        sos.componentSpecs = newScanSpecs;
                    }
                }
            }
        }
    }


    public void setFromTree(String formatName, Node root)
        throws IIOInvalidTreeException {
        if (formatName == null) {
            throw new IllegalArgumentException("null formatName!");
        }
        if (root == null) {
            throw new IllegalArgumentException("null root!");
        }
        if (isStream &&
            (formatName.equals(JPEG.nativeStreamMetadataFormatName))) {
            setFromNativeTree(root);
        } else if (!isStream &&
                   (formatName.equals(JPEG.nativeImageMetadataFormatName))) {
            setFromNativeTree(root);
        } else if (!isStream &&
                   (formatName.equals
                    (IIOMetadataFormatImpl.standardMetadataFormatName))) {
            // In this case a reset followed by a merge is correct
            super.setFromTree(formatName, root);
        } else {
            throw  new IllegalArgumentException("Unsupported format name: "
                                                + formatName);
        }
    }

    private void setFromNativeTree(Node root) throws IIOInvalidTreeException {
        if (resetSequence == null) {
            resetSequence = markerSequence;
        }
        markerSequence = new ArrayList<>();

        // Build a whole new marker sequence from the tree

        String name = root.getNodeName();
        if (name != ((isStream) ? JPEG.nativeStreamMetadataFormatName
                                : JPEG.nativeImageMetadataFormatName)) {
            throw new IIOInvalidTreeException("Invalid root node name: " + name,
                                              root);
        }
        if (!isStream) {
            if (root.getChildNodes().getLength() != 2) { // JPEGvariety and markerSequence
                throw new IIOInvalidTreeException(
                    "JPEGvariety and markerSequence nodes must be present", root);
            }

            Node JPEGvariety = root.getFirstChild();

            if (JPEGvariety.getChildNodes().getLength() != 0) {
                markerSequence.add(new JFIFMarkerSegment(JPEGvariety.getFirstChild()));
            }
        }

        Node markerSequenceNode = isStream ? root : root.getLastChild();
        setFromMarkerSequenceNode(markerSequenceNode);

    }

    void setFromMarkerSequenceNode(Node markerSequenceNode)
        throws IIOInvalidTreeException{

        NodeList children = markerSequenceNode.getChildNodes();
        // for all the children, add a marker segment
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            String childName = node.getNodeName();
            if (childName.equals("dqt")) {
                markerSequence.add(new DQTMarkerSegment(node));
            } else if (childName.equals("dht")) {
                markerSequence.add(new DHTMarkerSegment(node));
            } else if (childName.equals("dri")) {
                markerSequence.add(new DRIMarkerSegment(node));
            } else if (childName.equals("com")) {
                markerSequence.add(new COMMarkerSegment(node));
            } else if (childName.equals("app14Adobe")) {
                markerSequence.add(new AdobeMarkerSegment(node));
            } else if (childName.equals("unknown")) {
                markerSequence.add(new MarkerSegment(node));
            } else if (childName.equals("sof")) {
                markerSequence.add(new SOFMarkerSegment(node));
            } else if (childName.equals("sos")) {
                markerSequence.add(new SOSMarkerSegment(node));
            } else {
                throw new IIOInvalidTreeException("Invalid "
                    + (isStream ? "stream " : "image ") + "child: "
                    + childName, node);
            }
        }
    }

    /**
     * Check that this metadata object is in a consistent state and
     * return {@code true} if it is or {@code false}
     * otherwise.  All the constructors and modifiers should call
     * this method at the end to guarantee that the data is always
     * consistent, as the writer relies on this.
     */
    private boolean isConsistent() {
        SOFMarkerSegment sof =
            (SOFMarkerSegment) findMarkerSegment(SOFMarkerSegment.class,
                                                 true);
        JFIFMarkerSegment jfif =
            (JFIFMarkerSegment) findMarkerSegment(JFIFMarkerSegment.class,
                                                  true);
        AdobeMarkerSegment adobe =
            (AdobeMarkerSegment) findMarkerSegment(AdobeMarkerSegment.class,
                                                   true);
        boolean retval = true;
        if (!isStream) {
            if (sof != null) {
                // SOF numBands = total scan bands
                int numSOFBands = sof.componentSpecs.length;
                int numScanBands = countScanBands();
                if (numScanBands != 0) {  // No SOS is OK
                    if (numScanBands != numSOFBands) {
                        retval = false;
                    }
                }
                // If JFIF is present, component ids are 1-3, bands are 1 or 3
                if (jfif != null) {
                    if ((numSOFBands != 1) && (numSOFBands != 3)) {
                        retval = false;
                    }
                    for (int i = 0; i < numSOFBands; i++) {
                        if (sof.componentSpecs[i].componentId != i+1) {
                            retval = false;
                        }
                    }

                    // If both JFIF and Adobe are present,
                    // Adobe transform == unknown for gray,
                    // YCC for 3-chan.
                    if ((adobe != null)
                        && (((numSOFBands == 1)
                             && (adobe.transform != JPEG.ADOBE_UNKNOWN))
                            || ((numSOFBands == 3)
                                && (adobe.transform != JPEG.ADOBE_YCC)))) {
                        retval = false;
                    }
                }
            } else {
                // stream can't have jfif, adobe, sof, or sos
                SOSMarkerSegment sos =
                    (SOSMarkerSegment) findMarkerSegment(SOSMarkerSegment.class,
                                                         true);
                if ((jfif != null) || (adobe != null)
                    || (sof != null) || (sos != null)) {
                    retval = false;
                }
            }
        }
        return retval;
    }

    /**
     * Returns the total number of bands referenced in all SOS marker
     * segments, including 0 if there are no SOS marker segments.
     */
    private int countScanBands() {
        List<Integer> ids = new ArrayList<>();
        for (MarkerSegment seg : markerSequence) {
            if (seg instanceof SOSMarkerSegment) {
                SOSMarkerSegment sos = (SOSMarkerSegment) seg;
                SOSMarkerSegment.ScanComponentSpec [] specs = sos.componentSpecs;
                for (int i = 0; i < specs.length; i++) {
                    Integer id = specs[i].componentSelector;
                    if (!ids.contains(id)) {
                        ids.add(id);
                    }
                }
            }
        }

        return ids.size();
    }

    ///// Writer support

    void writeToStream(ImageOutputStream ios,
                       boolean ignoreJFIF,
                       boolean forceJFIF,
                       List<? extends BufferedImage> thumbnails,
                       ICC_Profile iccProfile,
                       boolean ignoreAdobe,
                       int newAdobeTransform,
                       JPEGImageWriter writer)
        throws IOException {
        if (forceJFIF) {
            // Write a default JFIF segment, including thumbnails
            // This won't be duplicated below because forceJFIF will be
            // set only if there is no JFIF present already.
            JFIFMarkerSegment.writeDefaultJFIF(ios,
                                               thumbnails,
                                               iccProfile,
                                               writer);
            if ((ignoreAdobe == false)
                && (newAdobeTransform != JPEG.ADOBE_IMPOSSIBLE)) {
                if ((newAdobeTransform != JPEG.ADOBE_UNKNOWN)
                    && (newAdobeTransform != JPEG.ADOBE_YCC)) {
                    // Not compatible, so ignore Adobe.
                    ignoreAdobe = true;
                    writer.warningOccurred
                        (JPEGImageWriter.WARNING_METADATA_ADJUSTED_FOR_THUMB);
                }
            }
        }
        // Iterate over each MarkerSegment
        for (MarkerSegment seg : markerSequence) {
            if (seg instanceof JFIFMarkerSegment) {
                if (ignoreJFIF == false) {
                    JFIFMarkerSegment jfif = (JFIFMarkerSegment) seg;
                    jfif.writeWithThumbs(ios, thumbnails, writer);
                    if (iccProfile != null) {
                        JFIFMarkerSegment.writeICC(iccProfile, ios);
                    }
                } // Otherwise ignore it, as requested
            } else if (seg instanceof AdobeMarkerSegment) {
                if (ignoreAdobe == false) {
                    if (newAdobeTransform != JPEG.ADOBE_IMPOSSIBLE) {
                        AdobeMarkerSegment newAdobe =
                            (AdobeMarkerSegment) seg.clone();
                        newAdobe.transform = newAdobeTransform;
                        newAdobe.write(ios);
                    } else if (forceJFIF) {
                        // If adobe isn't JFIF compatible, ignore it
                        AdobeMarkerSegment adobe = (AdobeMarkerSegment) seg;
                        if ((adobe.transform == JPEG.ADOBE_UNKNOWN)
                            || (adobe.transform == JPEG.ADOBE_YCC)) {
                            adobe.write(ios);
                        } else {
                            writer.warningOccurred
                         (JPEGImageWriter.WARNING_METADATA_ADJUSTED_FOR_THUMB);
                        }
                    } else {
                        seg.write(ios);
                    }
                } // Otherwise ignore it, as requested
            } else {
                seg.write(ios);
            }
        }
    }

    //// End of writer support

    public void reset() {
        if (resetSequence != null) {  // Otherwise no need to reset
            markerSequence = resetSequence;
            resetSequence = null;
        }
    }

    public void print() {
        for (int i = 0; i < markerSequence.size(); i++) {
            MarkerSegment seg = markerSequence.get(i);
            seg.print();
        }
    }

}
