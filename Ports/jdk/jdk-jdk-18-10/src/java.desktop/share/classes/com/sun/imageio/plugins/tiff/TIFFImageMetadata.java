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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.ExifParentTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;

public class TIFFImageMetadata extends IIOMetadata {

    // package scope

    public static final String NATIVE_METADATA_FORMAT_NAME =
        "javax_imageio_tiff_image_1.0";

    public static final String NATIVE_METADATA_FORMAT_CLASS_NAME =
        "javax.imageio.plugins.tiff.TIFFImageMetadataFormat";

    private List<TIFFTagSet> tagSets;

    TIFFIFD rootIFD;

    public TIFFImageMetadata(List<TIFFTagSet> tagSets) {
        super(true,
              NATIVE_METADATA_FORMAT_NAME,
              NATIVE_METADATA_FORMAT_CLASS_NAME,
              null, null);

        this.tagSets = tagSets;
        this.rootIFD = new TIFFIFD(tagSets);
    }

    public TIFFImageMetadata(TIFFIFD ifd) {
        super(true,
              NATIVE_METADATA_FORMAT_NAME,
              NATIVE_METADATA_FORMAT_CLASS_NAME,
              null, null);
        this.tagSets = ifd.getTagSetList();
        this.rootIFD = ifd;
    }

    public void initializeFromStream(ImageInputStream stream,
                                     boolean ignoreMetadata,
                                     boolean readUnknownTags)
        throws IOException {
        rootIFD.initialize(stream, true, ignoreMetadata, readUnknownTags);
    }

    public void addShortOrLongField(int tagNumber, long value) {
        TIFFField field = new TIFFField(rootIFD.getTag(tagNumber), value);
        rootIFD.addTIFFField(field);
    }

    public boolean isReadOnly() {
        return false;
    }

    private Node getIFDAsTree(TIFFIFD ifd,
                              String parentTagName, int parentTagNumber) {
        IIOMetadataNode IFDRoot = new IIOMetadataNode("TIFFIFD");
        if (parentTagNumber != 0) {
            IFDRoot.setAttribute("parentTagNumber",
                                 Integer.toString(parentTagNumber));
        }
        if (parentTagName != null) {
            IFDRoot.setAttribute("parentTagName", parentTagName);
        }

        List<TIFFTagSet> tagSets = ifd.getTagSetList();
        if (tagSets.size() > 0) {
            Iterator<TIFFTagSet> iter = tagSets.iterator();
            StringBuilder tagSetNames = new StringBuilder();
            while (iter.hasNext()) {
                TIFFTagSet tagSet = iter.next();
                tagSetNames.append(tagSet.getClass().getName());
                if (iter.hasNext()) {
                    tagSetNames.append(",");
                }
            }

            IFDRoot.setAttribute("tagSets", tagSetNames.toString());
        }

        Iterator<TIFFField> iter = ifd.iterator();
        while (iter.hasNext()) {
            TIFFField f = iter.next();
            int tagNumber = f.getTagNumber();
            TIFFTag tag = TIFFIFD.getTag(tagNumber, tagSets);

            Node node = null;
            if (tag == null) {
                node = f.getAsNativeNode();
            } else if (tag.isIFDPointer() && f.hasDirectory()) {
                TIFFIFD subIFD = TIFFIFD.getDirectoryAsIFD(f.getDirectory());

                // Recurse
                node = getIFDAsTree(subIFD, tag.getName(), tag.getNumber());
            } else {
                node = f.getAsNativeNode();
            }

            if (node != null) {
                IFDRoot.appendChild(node);
            }
        }

        return IFDRoot;
    }

    public Node getAsTree(String formatName) {
        if (formatName.equals(nativeMetadataFormatName)) {
            return getNativeTree();
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        } else {
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    private Node getNativeTree() {
        IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);

        Node IFDNode = getIFDAsTree(rootIFD, null, 0);
        root.appendChild(IFDNode);

        return root;
    }

    private static final String[] colorSpaceNames = {
        "GRAY", // WhiteIsZero
        "GRAY", // BlackIsZero
        "RGB", // RGB
        "RGB", // PaletteColor
        "GRAY", // TransparencyMask
        "CMYK", // CMYK
        "YCbCr", // YCbCr
        "Lab", // CIELab
        "Lab", // ICCLab
    };

    public IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma_node = new IIOMetadataNode("Chroma");
        IIOMetadataNode node = null; // scratch node

        TIFFField f;

        // Set the PhotometricInterpretation and the palette color flag.
        int photometricInterpretation = -1;
        boolean isPaletteColor = false;
        f = getTIFFField(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION);
        if (f != null) {
            photometricInterpretation = f.getAsInt(0);

            isPaletteColor =
                photometricInterpretation ==
                BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR;
        }

        // Determine the number of channels.
        int numChannels = -1;
        if(isPaletteColor) {
            numChannels = 3;
        } else {
            f = getTIFFField(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL);
            if (f != null) {
                numChannels = f.getAsInt(0);
            } else { // f == null
                f = getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
                if(f != null) {
                    numChannels = f.getCount();
                }
            }
        }

        if(photometricInterpretation != -1) {
            if (photometricInterpretation >= 0 &&
                photometricInterpretation < colorSpaceNames.length) {
                node = new IIOMetadataNode("ColorSpaceType");
                String csName;
                if(photometricInterpretation ==
                   BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CMYK &&
                   numChannels == 3) {
                    csName = "CMY";
                } else {
                    csName = colorSpaceNames[photometricInterpretation];
                }
                node.setAttribute("name", csName);
                chroma_node.appendChild(node);
            }

            node = new IIOMetadataNode("BlackIsZero");
            node.setAttribute("value",
                              (photometricInterpretation ==
                   BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO)
                              ? "FALSE" : "TRUE");
            chroma_node.appendChild(node);
        }

        if(numChannels != -1) {
            node = new IIOMetadataNode("NumChannels");
            node.setAttribute("value", Integer.toString(numChannels));
            chroma_node.appendChild(node);
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_COLOR_MAP);
        if (f != null) {
            // NOTE: The presence of hasAlpha is vestigial: there is
            // no way in TIFF to represent an alpha component in a palette
            // color image. See bug 5086341.
            boolean hasAlpha = false;

            node = new IIOMetadataNode("Palette");
            int len = f.getCount()/(hasAlpha ? 4 : 3);
            for (int i = 0; i < len; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("PaletteEntry");
                entry.setAttribute("index", Integer.toString(i));

                int r = (f.getAsInt(i)*255)/65535;
                int g = (f.getAsInt(len + i)*255)/65535;
                int b = (f.getAsInt(2*len + i)*255)/65535;

                entry.setAttribute("red", Integer.toString(r));
                entry.setAttribute("green", Integer.toString(g));
                entry.setAttribute("blue", Integer.toString(b));
                if (hasAlpha) {
                    int alpha = 0;
                    entry.setAttribute("alpha", Integer.toString(alpha));
                }
                node.appendChild(entry);
            }
            chroma_node.appendChild(node);
        }

        return chroma_node;
    }

    public IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
        IIOMetadataNode node = null; // scratch node

        TIFFField f;

        f = getTIFFField(BaselineTIFFTagSet.TAG_COMPRESSION);
        if (f != null) {
            String compressionTypeName = null;
            int compression = f.getAsInt(0);
            boolean isLossless = true; // obligate initialization.
            if(compression == BaselineTIFFTagSet.COMPRESSION_NONE) {
                compressionTypeName = "None";
                isLossless = true;
            } else {
                int[] compressionNumbers = TIFFImageWriter.compressionNumbers;
                for(int i = 0; i < compressionNumbers.length; i++) {
                    if(compression == compressionNumbers[i]) {
                        compressionTypeName =
                            TIFFImageWriter.compressionTypes[i];
                        isLossless =
                            TIFFImageWriter.isCompressionLossless[i];
                        break;
                    }
                }
            }

            if (compressionTypeName != null) {
                node = new IIOMetadataNode("CompressionTypeName");
                node.setAttribute("value", compressionTypeName);
                compression_node.appendChild(node);

                node = new IIOMetadataNode("Lossless");
                node.setAttribute("value", isLossless ? "TRUE" : "FALSE");
                compression_node.appendChild(node);
            }
        }

        node = new IIOMetadataNode("NumProgressiveScans");
        node.setAttribute("value", "1");
        compression_node.appendChild(node);

        return compression_node;
    }

    private String repeat(String s, int times) {
        if (times == 1) {
            return s;
        }
        StringBuilder sb = new StringBuilder((s.length() + 1)*times - 1);
        sb.append(s);
        for (int i = 1; i < times; i++) {
            sb.append(" ");
            sb.append(s);
        }
        return sb.toString();
    }

    public IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data_node = new IIOMetadataNode("Data");
        IIOMetadataNode node = null; // scratch node

        TIFFField f;

        boolean isPaletteColor = false;
        f = getTIFFField(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION);
        if (f != null) {
            isPaletteColor =
                f.getAsInt(0) ==
                BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR;
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION);
        String planarConfiguration = "PixelInterleaved";
        if (f != null &&
            f.getAsInt(0) == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {
            planarConfiguration = "PlaneInterleaved";
        }

        node = new IIOMetadataNode("PlanarConfiguration");
        node.setAttribute("value", planarConfiguration);
        data_node.appendChild(node);

        f = getTIFFField(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION);
        if (f != null) {
            int photometricInterpretation = f.getAsInt(0);
            String sampleFormat = "UnsignedIntegral";

            if (photometricInterpretation ==
                BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR) {
                sampleFormat = "Index";
            } else {
                f = getTIFFField(BaselineTIFFTagSet.TAG_SAMPLE_FORMAT);
                if (f != null) {
                    int format = f.getAsInt(0);
                    if (format ==
                        BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER) {
                        sampleFormat = "SignedIntegral";
                    } else if (format ==
                        BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER) {
                        sampleFormat = "UnsignedIntegral";
                    } else if (format ==
                               BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {
                        sampleFormat = "Real";
                    } else {
                        sampleFormat = null; // don't know
                    }
                }
            }
            if (sampleFormat != null) {
                node = new IIOMetadataNode("SampleFormat");
                node.setAttribute("value", sampleFormat);
                data_node.appendChild(node);
            }
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
        int[] bitsPerSample = null;
        if(f != null) {
            bitsPerSample = f.getAsInts();
        } else {
            f = getTIFFField(BaselineTIFFTagSet.TAG_COMPRESSION);
            int compression = f != null ?
                f.getAsInt(0) : BaselineTIFFTagSet.COMPRESSION_NONE;
            if(getTIFFField(ExifParentTIFFTagSet.TAG_EXIF_IFD_POINTER) !=
               null ||
               compression == BaselineTIFFTagSet.COMPRESSION_JPEG ||
               compression == BaselineTIFFTagSet.COMPRESSION_OLD_JPEG ||
               getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT) !=
               null) {
                f = getTIFFField(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION);
                if(f != null &&
                   (f.getAsInt(0) ==
                    BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO ||
                    f.getAsInt(0) ==
                    BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO)) {
                    bitsPerSample = new int[] {8};
                } else {
                    bitsPerSample = new int[] {8, 8, 8};
                }
            } else {
                bitsPerSample = new int[] {1};
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bitsPerSample.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(bitsPerSample[i]);
        }
        node = new IIOMetadataNode("BitsPerSample");
        if(isPaletteColor) {
            node.setAttribute("value", repeat(sb.toString(), 3));
        } else {
            node.setAttribute("value", sb.toString());
        }
        data_node.appendChild(node);

            // SampleMSB
        f = getTIFFField(BaselineTIFFTagSet.TAG_FILL_ORDER);
        int fillOrder = f != null ?
            f.getAsInt(0) : BaselineTIFFTagSet.FILL_ORDER_LEFT_TO_RIGHT;
        sb = new StringBuilder();
        for (int i = 0; i < bitsPerSample.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            int maxBitIndex = bitsPerSample[i] == 1 ?
                7 : bitsPerSample[i] - 1;
            int msb =
                fillOrder == BaselineTIFFTagSet.FILL_ORDER_LEFT_TO_RIGHT ?
                maxBitIndex : 0;
            sb.append(msb);
        }
        node = new IIOMetadataNode("SampleMSB");
        if(isPaletteColor) {
            node.setAttribute("value", repeat(sb.toString(), 3));
        } else {
            node.setAttribute("value", sb.toString());
        }
        data_node.appendChild(node);

        return data_node;
    }

    private static final String[] orientationNames = {
        null,
        "Normal",
        "FlipH",
        "Rotate180",
        "FlipV",
        "FlipHRotate90",
        "Rotate270",
        "FlipVRotate90",
        "Rotate90",
    };

    public IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
        IIOMetadataNode node = null; // scratch node

        TIFFField f;

        long[] xres = null;
        long[] yres = null;

        f = getTIFFField(BaselineTIFFTagSet.TAG_X_RESOLUTION);
        if (f != null) {
            xres = f.getAsRational(0).clone();
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_Y_RESOLUTION);
        if (f != null) {
            yres = f.getAsRational(0).clone();
        }

        if (xres != null && yres != null) {
            node = new IIOMetadataNode("PixelAspectRatio");

            // Compute (1/xres)/(1/yres)
            // (xres_denom/xres_num)/(yres_denom/yres_num) =
            // (xres_denom/xres_num)*(yres_num/yres_denom) =
            // (xres_denom*yres_num)/(xres_num*yres_denom)
            float ratio = (float)((double)xres[1]*yres[0])/(xres[0]*yres[1]);
            node.setAttribute("value", Float.toString(ratio));
            dimension_node.appendChild(node);
        }

        if (xres != null || yres != null) {
            // Get unit field.
            f = getTIFFField(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT);

            // Set resolution unit.
            int resolutionUnit = f != null ?
                f.getAsInt(0) : BaselineTIFFTagSet.RESOLUTION_UNIT_INCH;

            // Have size if either centimeters or inches.
            boolean gotPixelSize =
                resolutionUnit != BaselineTIFFTagSet.RESOLUTION_UNIT_NONE;

            // Convert pixels/inch to pixels/centimeter.
            if (resolutionUnit == BaselineTIFFTagSet.RESOLUTION_UNIT_INCH) {
                // Divide xres by 2.54
                if (xres != null) {
                    xres[0] *= 100;
                    xres[1] *= 254;
                }

                // Divide yres by 2.54
                if (yres != null) {
                    yres[0] *= 100;
                    yres[1] *= 254;
                }
            }

            if (gotPixelSize) {
                if (xres != null) {
                    float horizontalPixelSize = (float)(10.0*xres[1]/xres[0]);
                    node = new IIOMetadataNode("HorizontalPixelSize");
                    node.setAttribute("value",
                                      Float.toString(horizontalPixelSize));
                    dimension_node.appendChild(node);
                }

                if (yres != null) {
                    float verticalPixelSize = (float)(10.0*yres[1]/yres[0]);
                    node = new IIOMetadataNode("VerticalPixelSize");
                    node.setAttribute("value",
                                      Float.toString(verticalPixelSize));
                    dimension_node.appendChild(node);
                }
            }
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT);
        int resolutionUnit = f != null ?
            f.getAsInt(0) : BaselineTIFFTagSet.RESOLUTION_UNIT_INCH;
        if(resolutionUnit == BaselineTIFFTagSet.RESOLUTION_UNIT_INCH ||
           resolutionUnit == BaselineTIFFTagSet.RESOLUTION_UNIT_CENTIMETER) {
            f = getTIFFField(BaselineTIFFTagSet.TAG_X_POSITION);
            if(f != null) {
                long[] xpos = f.getAsRational(0);
                float xPosition = (float)xpos[0]/(float)xpos[1];
                // Convert to millimeters.
                if(resolutionUnit == BaselineTIFFTagSet.RESOLUTION_UNIT_INCH) {
                    xPosition *= 254F;
                } else {
                    xPosition *= 10F;
                }
                node = new IIOMetadataNode("HorizontalPosition");
                node.setAttribute("value",
                                  Float.toString(xPosition));
                dimension_node.appendChild(node);
            }

            f = getTIFFField(BaselineTIFFTagSet.TAG_Y_POSITION);
            if(f != null) {
                long[] ypos = f.getAsRational(0);
                float yPosition = (float)ypos[0]/(float)ypos[1];
                // Convert to millimeters.
                if(resolutionUnit == BaselineTIFFTagSet.RESOLUTION_UNIT_INCH) {
                    yPosition *= 254F;
                } else {
                    yPosition *= 10F;
                }
                node = new IIOMetadataNode("VerticalPosition");
                node.setAttribute("value",
                                  Float.toString(yPosition));
                dimension_node.appendChild(node);
            }
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_ORIENTATION);
        if (f != null) {
            int o = f.getAsInt(0);
            if (o >= 0 && o < orientationNames.length) {
                node = new IIOMetadataNode("ImageOrientation");
                node.setAttribute("value", orientationNames[o]);
                dimension_node.appendChild(node);
            }
        }

        return dimension_node;
    }

    public IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document_node = new IIOMetadataNode("Document");
        IIOMetadataNode node = null; // scratch node

        TIFFField f;

        node = new IIOMetadataNode("FormatVersion");
        node.setAttribute("value", "6.0");
        document_node.appendChild(node);

        f = getTIFFField(BaselineTIFFTagSet.TAG_NEW_SUBFILE_TYPE);
        if(f != null) {
            int newSubFileType = f.getAsInt(0);
            String value = null;
            if((newSubFileType &
                BaselineTIFFTagSet.NEW_SUBFILE_TYPE_TRANSPARENCY) != 0) {
                value = "TransparencyMask";
            } else if((newSubFileType &
                       BaselineTIFFTagSet.NEW_SUBFILE_TYPE_REDUCED_RESOLUTION) != 0) {
                value = "ReducedResolution";
            } else if((newSubFileType &
                       BaselineTIFFTagSet.NEW_SUBFILE_TYPE_SINGLE_PAGE) != 0) {
                value = "SinglePage";
            }
            if(value != null) {
                node = new IIOMetadataNode("SubimageInterpretation");
                node.setAttribute("value", value);
                document_node.appendChild(node);
            }
        }

        f = getTIFFField(BaselineTIFFTagSet.TAG_DATE_TIME);
        if (f != null) {
            String s = f.getAsString(0);

            // DateTime should be formatted as "YYYY:MM:DD hh:mm:ss".
            if(s.length() == 19) {
                node = new IIOMetadataNode("ImageCreationTime");

                // Files with incorrect DateTime format have been
                // observed so anticipate an exception from substring()
                // and only add the node if the format is presumably
                // correct.
                boolean appendNode;
                try {
                    node.setAttribute("year", s.substring(0, 4));
                    node.setAttribute("month", s.substring(5, 7));
                    node.setAttribute("day", s.substring(8, 10));
                    node.setAttribute("hour", s.substring(11, 13));
                    node.setAttribute("minute", s.substring(14, 16));
                    node.setAttribute("second", s.substring(17, 19));
                    appendNode = true;
                } catch(IndexOutOfBoundsException e) {
                    appendNode = false;
                }

                if(appendNode) {
                    document_node.appendChild(node);
                }
            }
        }

        return document_node;
    }

    public IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text_node = null;
        IIOMetadataNode node = null; // scratch node

        TIFFField f;

        int[] textFieldTagNumbers = new int[] {
            BaselineTIFFTagSet.TAG_DOCUMENT_NAME,
            BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION,
            BaselineTIFFTagSet.TAG_MAKE,
            BaselineTIFFTagSet.TAG_MODEL,
            BaselineTIFFTagSet.TAG_PAGE_NAME,
            BaselineTIFFTagSet.TAG_SOFTWARE,
            BaselineTIFFTagSet.TAG_ARTIST,
            BaselineTIFFTagSet.TAG_HOST_COMPUTER,
            BaselineTIFFTagSet.TAG_INK_NAMES,
            BaselineTIFFTagSet.TAG_COPYRIGHT
        };

        for(int i = 0; i < textFieldTagNumbers.length; i++) {
            f = getTIFFField(textFieldTagNumbers[i]);
            if(f != null) {
                String value = f.getAsString(0);
                if(text_node == null) {
                    text_node = new IIOMetadataNode("Text");
                }
                node = new IIOMetadataNode("TextEntry");
                node.setAttribute("keyword", f.getTag().getName());
                node.setAttribute("value", value);
                text_node.appendChild(node);
            }
        }

        return text_node;
    }

    public IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparency_node =
            new IIOMetadataNode("Transparency");
        IIOMetadataNode node = null; // scratch node

        TIFFField f;

        node = new IIOMetadataNode("Alpha");
        String value = "none";

        f = getTIFFField(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES);
        if(f != null) {
            int[] extraSamples = f.getAsInts();
            for(int i = 0; i < extraSamples.length; i++) {
                if(extraSamples[i] ==
                   BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                    value = "premultiplied";
                    break;
                } else if(extraSamples[i] ==
                          BaselineTIFFTagSet.EXTRA_SAMPLES_UNASSOCIATED_ALPHA) {
                    value = "nonpremultiplied";
                    break;
                }
            }
        }

        node.setAttribute("value", value);
        transparency_node.appendChild(node);

        return transparency_node;
    }

    // Shorthand for throwing an IIOInvalidTreeException
    private static void fatal(Node node, String reason)
        throws IIOInvalidTreeException {
        throw new IIOInvalidTreeException(reason, node);
    }

    private int[] listToIntArray(String list) {
        StringTokenizer st = new StringTokenizer(list, " ");
        ArrayList<Integer> intList = new ArrayList<Integer>();
        while (st.hasMoreTokens()) {
            String nextInteger = st.nextToken();
            Integer nextInt = Integer.valueOf(nextInteger);
            intList.add(nextInt);
        }

        int[] intArray = new int[intList.size()];
        for(int i = 0; i < intArray.length; i++) {
            intArray[i] = intList.get(i);
        }

        return intArray;
    }

    private char[] listToCharArray(String list) {
        StringTokenizer st = new StringTokenizer(list, " ");
        ArrayList<Integer> intList = new ArrayList<Integer>();
        while (st.hasMoreTokens()) {
            String nextInteger = st.nextToken();
            Integer nextInt = Integer.valueOf(nextInteger);
            intList.add(nextInt);
        }

        char[] charArray = new char[intList.size()];
        for(int i = 0; i < charArray.length; i++) {
            charArray[i] = (char)(intList.get(i).intValue());
        }

        return charArray;
    }

    private void mergeStandardTree(Node root)
        throws IIOInvalidTreeException {
        TIFFField f;
        TIFFTag tag;

        Node node = root;
        if (!node.getNodeName()
            .equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            fatal(node, "Root must be " +
                  IIOMetadataFormatImpl.standardMetadataFormatName);
        }

        // Obtain the sample format and set the palette flag if appropriate.
        String sampleFormat = null;
        Node dataNode = getChildNode(root, "Data");
        boolean isPaletteColor = false;
        if(dataNode != null) {
            Node sampleFormatNode = getChildNode(dataNode, "SampleFormat");
            if(sampleFormatNode != null) {
                sampleFormat = getAttribute(sampleFormatNode, "value");
                isPaletteColor = sampleFormat.equals("Index");
            }
        }

        // If palette flag not set check for palette.
        if(!isPaletteColor) {
            Node chromaNode = getChildNode(root, "Chroma");
            if(chromaNode != null &&
               getChildNode(chromaNode, "Palette") != null) {
                isPaletteColor = true;
            }
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();

            if (name.equals("Chroma")) {
                String colorSpaceType = null;
                String blackIsZero = null;
                boolean gotPalette = false;
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("ColorSpaceType")) {
                        colorSpaceType = getAttribute(child, "name");
                    } else if (childName.equals("NumChannels")) {
                        tag = rootIFD.getTag(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL);
                        int samplesPerPixel = isPaletteColor ?
                            1 : Integer.parseInt(getAttribute(child, "value"));
                        f = new TIFFField(tag, samplesPerPixel);
                        rootIFD.addTIFFField(f);
                    } else if (childName.equals("BlackIsZero")) {
                        blackIsZero = getAttribute(child, "value");
                    } else if (childName.equals("Palette")) {
                        Node entry = child.getFirstChild();
                        HashMap<Integer,char[]> palette = new HashMap<>();
                        int maxIndex = -1;
                        while(entry != null) {
                            String entryName = entry.getNodeName();
                            if(entryName.equals("PaletteEntry")) {
                                String idx = getAttribute(entry, "index");
                                int id = Integer.parseInt(idx);
                                if(id > maxIndex) {
                                    maxIndex = id;
                                }
                                char red =
                                    (char)Integer.parseInt(getAttribute(entry,
                                                                        "red"));
                                char green =
                                    (char)Integer.parseInt(getAttribute(entry,
                                                                        "green"));
                                char blue =
                                    (char)Integer.parseInt(getAttribute(entry,
                                                                        "blue"));
                                palette.put(Integer.valueOf(id),
                                            new char[] {red, green, blue});

                                gotPalette = true;
                            }
                            entry = entry.getNextSibling();
                        }

                        if(gotPalette) {
                            int mapSize = maxIndex + 1;
                            int paletteLength = 3*mapSize;
                            char[] paletteEntries = new char[paletteLength];
                            for (Map.Entry<Integer,char[]> paletteEntry : palette.entrySet()) {
                                int index = paletteEntry.getKey();
                                char[] rgb = paletteEntry.getValue();
                                paletteEntries[index] =
                                    (char)((rgb[0]*65535)/255);
                                paletteEntries[mapSize + index] =
                                    (char)((rgb[1]*65535)/255);
                                paletteEntries[2*mapSize + index] =
                                    (char)((rgb[2]*65535)/255);
                            }

                            tag = rootIFD.getTag(BaselineTIFFTagSet.TAG_COLOR_MAP);
                            f = new TIFFField(tag, TIFFTag.TIFF_SHORT,
                                              paletteLength, paletteEntries);
                            rootIFD.addTIFFField(f);
                        }
                    }

                    child = child.getNextSibling();
                }

                int photometricInterpretation = -1;
                if((colorSpaceType == null || colorSpaceType.equals("GRAY")) &&
                   blackIsZero != null &&
                   blackIsZero.equalsIgnoreCase("FALSE")) {
                    photometricInterpretation =
                        BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO;
                } else if(colorSpaceType != null) {
                    if(colorSpaceType.equals("GRAY")) {
                        boolean isTransparency = false;
                        if(root instanceof IIOMetadataNode) {
                            IIOMetadataNode iioRoot = (IIOMetadataNode)root;
                            NodeList siNodeList =
                                iioRoot.getElementsByTagName("SubimageInterpretation");
                            if(siNodeList.getLength() == 1) {
                                Node siNode = siNodeList.item(0);
                                String value = getAttribute(siNode, "value");
                                if(value.equals("TransparencyMask")) {
                                    isTransparency = true;
                                }
                            }
                        }
                        if(isTransparency) {
                            photometricInterpretation =
                                BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK;
                        } else {
                            photometricInterpretation =
                                BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO;
                        }
                    } else if(colorSpaceType.equals("RGB")) {
                        photometricInterpretation =
                            gotPalette ?
                            BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR :
                            BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_RGB;
                    } else if(colorSpaceType.equals("YCbCr")) {
                        photometricInterpretation =
                            BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_Y_CB_CR;
                    } else if(colorSpaceType.equals("CMYK")) {
                        photometricInterpretation =
                            BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CMYK;
                    } else if(colorSpaceType.equals("Lab")) {
                        photometricInterpretation =
                            BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CIELAB;
                    }
                }

                if(photometricInterpretation != -1) {
                    tag = rootIFD.getTag(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION);
                    f = new TIFFField(tag, photometricInterpretation);
                    rootIFD.addTIFFField(f);
                }
            } else if (name.equals("Compression")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("CompressionTypeName")) {
                        int compression = -1;
                        String compressionTypeName =
                            getAttribute(child, "value");
                        if(compressionTypeName.equalsIgnoreCase("None")) {
                            compression =
                                BaselineTIFFTagSet.COMPRESSION_NONE;
                        } else {
                            String[] compressionNames =
                                TIFFImageWriter.compressionTypes;
                            for(int i = 0; i < compressionNames.length; i++) {
                                if(compressionNames[i].equalsIgnoreCase(compressionTypeName)) {
                                    compression =
                                        TIFFImageWriter.compressionNumbers[i];
                                    break;
                                }
                            }
                        }

                        if(compression != -1) {
                            tag = rootIFD.getTag(BaselineTIFFTagSet.TAG_COMPRESSION);
                            f = new TIFFField(tag, compression);
                            rootIFD.addTIFFField(f);

                            // Lossless is irrelevant.
                        }
                    }

                    child = child.getNextSibling();
                }
            } else if (name.equals("Data")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();

                    if (childName.equals("PlanarConfiguration")) {
                        String pc = getAttribute(child, "value");
                        int planarConfiguration = -1;
                        if(pc.equals("PixelInterleaved")) {
                            planarConfiguration =
                                BaselineTIFFTagSet.PLANAR_CONFIGURATION_CHUNKY;
                        } else if(pc.equals("PlaneInterleaved")) {
                            planarConfiguration =
                                BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR;
                        }
                        if(planarConfiguration != -1) {
                            tag = rootIFD.getTag(BaselineTIFFTagSet.TAG_PLANAR_CONFIGURATION);
                            f = new TIFFField(tag, planarConfiguration);
                            rootIFD.addTIFFField(f);
                        }
                    } else if (childName.equals("BitsPerSample")) {
                        String bps = getAttribute(child, "value");
                        char[] bitsPerSample = listToCharArray(bps);
                        tag = rootIFD.getTag(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
                        if(isPaletteColor) {
                            f = new TIFFField(tag, TIFFTag.TIFF_SHORT, 1,
                                              new char[] {bitsPerSample[0]});
                        } else {
                            f = new TIFFField(tag, TIFFTag.TIFF_SHORT,
                                              bitsPerSample.length,
                                              bitsPerSample);
                        }
                        rootIFD.addTIFFField(f);
                    } else if (childName.equals("SampleMSB")) {
                        // Add FillOrder only if lsb-to-msb (right to left)
                        // for all bands, i.e., SampleMSB is zero for all
                        // channels.
                        String sMSB = getAttribute(child, "value");
                        int[] sampleMSB = listToIntArray(sMSB);
                        boolean isRightToLeft = true;
                        for(int i = 0; i < sampleMSB.length; i++) {
                            if(sampleMSB[i] != 0) {
                                isRightToLeft = false;
                                break;
                            }
                        }
                        int fillOrder = isRightToLeft ?
                            BaselineTIFFTagSet.FILL_ORDER_RIGHT_TO_LEFT :
                            BaselineTIFFTagSet.FILL_ORDER_LEFT_TO_RIGHT;
                        tag =
                            rootIFD.getTag(BaselineTIFFTagSet.TAG_FILL_ORDER);
                        f = new TIFFField(tag, fillOrder);
                        rootIFD.addTIFFField(f);
                    }

                    child = child.getNextSibling();
                }
            } else if (name.equals("Dimension")) {
                float pixelAspectRatio = -1.0f;
                boolean gotPixelAspectRatio = false;

                float horizontalPixelSize = -1.0f;
                boolean gotHorizontalPixelSize = false;

                float verticalPixelSize = -1.0f;
                boolean gotVerticalPixelSize = false;

                boolean sizeIsAbsolute = false;

                float horizontalPosition = -1.0f;
                boolean gotHorizontalPosition = false;

                float verticalPosition = -1.0f;
                boolean gotVerticalPosition = false;

                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("PixelAspectRatio")) {
                        String par = getAttribute(child, "value");
                        pixelAspectRatio = Float.parseFloat(par);
                        gotPixelAspectRatio = true;
                    } else if (childName.equals("ImageOrientation")) {
                        String orientation = getAttribute(child, "value");
                        for (int i = 0; i < orientationNames.length; i++) {
                            if (orientation.equals(orientationNames[i])) {
                                char[] oData = new char[1];
                                oData[0] = (char)i;

                                f = new TIFFField(
                            rootIFD.getTag(BaselineTIFFTagSet.TAG_ORIENTATION),
                            TIFFTag.TIFF_SHORT,
                            1,
                            oData);

                                rootIFD.addTIFFField(f);
                                break;
                            }
                        }

                    } else if (childName.equals("HorizontalPixelSize")) {
                        String hps = getAttribute(child, "value");
                        horizontalPixelSize = Float.parseFloat(hps);
                        gotHorizontalPixelSize = true;
                    } else if (childName.equals("VerticalPixelSize")) {
                        String vps = getAttribute(child, "value");
                        verticalPixelSize = Float.parseFloat(vps);
                        gotVerticalPixelSize = true;
                    } else if (childName.equals("HorizontalPosition")) {
                        String hp = getAttribute(child, "value");
                        horizontalPosition = Float.parseFloat(hp);
                        gotHorizontalPosition = true;
                    } else if (childName.equals("VerticalPosition")) {
                        String vp = getAttribute(child, "value");
                        verticalPosition = Float.parseFloat(vp);
                        gotVerticalPosition = true;
                    }

                    child = child.getNextSibling();
                }

                sizeIsAbsolute = gotHorizontalPixelSize ||
                    gotVerticalPixelSize;

                // Fill in pixel size data from aspect ratio
                if (gotPixelAspectRatio) {
                    if (gotHorizontalPixelSize && !gotVerticalPixelSize) {
                        verticalPixelSize =
                            horizontalPixelSize/pixelAspectRatio;
                        gotVerticalPixelSize = true;
                    } else if (gotVerticalPixelSize &&
                               !gotHorizontalPixelSize) {
                        horizontalPixelSize =
                            verticalPixelSize*pixelAspectRatio;
                        gotHorizontalPixelSize = true;
                    } else if (!gotHorizontalPixelSize &&
                               !gotVerticalPixelSize) {
                        horizontalPixelSize = pixelAspectRatio;
                        verticalPixelSize = 1.0f;
                        gotHorizontalPixelSize = true;
                        gotVerticalPixelSize = true;
                    }
                }

                // Compute pixels/centimeter
                if (gotHorizontalPixelSize) {
                    float xResolution =
                        (sizeIsAbsolute ? 10.0f : 1.0f)/horizontalPixelSize;
                    long[][] hData = new long[1][2];
                    hData[0] = new long[2];
                    hData[0][0] = (long)(xResolution*10000.0f);
                    hData[0][1] = (long)10000;

                    f = new TIFFField(
                           rootIFD.getTag(BaselineTIFFTagSet.TAG_X_RESOLUTION),
                           TIFFTag.TIFF_RATIONAL,
                           1,
                           hData);
                    rootIFD.addTIFFField(f);
                }

                if (gotVerticalPixelSize) {
                    float yResolution =
                        (sizeIsAbsolute ? 10.0f : 1.0f)/verticalPixelSize;
                    long[][] vData = new long[1][2];
                    vData[0] = new long[2];
                    vData[0][0] = (long)(yResolution*10000.0f);
                    vData[0][1] = (long)10000;

                    f = new TIFFField(
                           rootIFD.getTag(BaselineTIFFTagSet.TAG_Y_RESOLUTION),
                           TIFFTag.TIFF_RATIONAL,
                           1,
                           vData);
                    rootIFD.addTIFFField(f);
                }

                // Emit ResolutionUnit tag
                char[] res = new char[1];
                res[0] = (char)(sizeIsAbsolute ?
                                BaselineTIFFTagSet.RESOLUTION_UNIT_CENTIMETER :
                                BaselineTIFFTagSet.RESOLUTION_UNIT_NONE);

                f = new TIFFField(
                        rootIFD.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
                        TIFFTag.TIFF_SHORT,
                        1,
                        res);
                rootIFD.addTIFFField(f);

                // Position
                if(sizeIsAbsolute) {
                    if(gotHorizontalPosition) {
                        // Convert from millimeters to centimeters via
                        // numerator multiplier = denominator/10.
                        long[][] hData = new long[1][2];
                        hData[0][0] = (long)(horizontalPosition*10000.0f);
                        hData[0][1] = (long)100000;

                        f = new TIFFField(
                           rootIFD.getTag(BaselineTIFFTagSet.TAG_X_POSITION),
                           TIFFTag.TIFF_RATIONAL,
                           1,
                           hData);
                        rootIFD.addTIFFField(f);
                    }

                    if(gotVerticalPosition) {
                        // Convert from millimeters to centimeters via
                        // numerator multiplier = denominator/10.
                        long[][] vData = new long[1][2];
                        vData[0][0] = (long)(verticalPosition*10000.0f);
                        vData[0][1] = (long)100000;

                        f = new TIFFField(
                           rootIFD.getTag(BaselineTIFFTagSet.TAG_Y_POSITION),
                           TIFFTag.TIFF_RATIONAL,
                           1,
                           vData);
                        rootIFD.addTIFFField(f);
                    }
                }
            } else if (name.equals("Document")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();

                    if (childName.equals("SubimageInterpretation")) {
                        String si = getAttribute(child, "value");
                        int newSubFileType = -1;
                        if(si.equals("TransparencyMask")) {
                            newSubFileType =
                                BaselineTIFFTagSet.NEW_SUBFILE_TYPE_TRANSPARENCY;
                        } else if(si.equals("ReducedResolution")) {
                            newSubFileType =
                                BaselineTIFFTagSet.NEW_SUBFILE_TYPE_REDUCED_RESOLUTION;
                        } else if(si.equals("SinglePage")) {
                            newSubFileType =
                                BaselineTIFFTagSet.NEW_SUBFILE_TYPE_SINGLE_PAGE;
                        }
                        if(newSubFileType != -1) {
                            tag =
                                rootIFD.getTag(BaselineTIFFTagSet.TAG_NEW_SUBFILE_TYPE);
                            f = new TIFFField(tag, newSubFileType);
                            rootIFD.addTIFFField(f);
                        }
                    }

                    if (childName.equals("ImageCreationTime")) {
                        String year = getAttribute(child, "year");
                        String month = getAttribute(child, "month");
                        String day = getAttribute(child, "day");
                        String hour = getAttribute(child, "hour");
                        String minute = getAttribute(child, "minute");
                        String second = getAttribute(child, "second");

                        StringBuilder sb = new StringBuilder();
                        sb.append(year);
                        sb.append(":");
                        if(month.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(month);
                        sb.append(":");
                        if(day.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(day);
                        sb.append(" ");
                        if(hour.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(hour);
                        sb.append(":");
                        if(minute.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(minute);
                        sb.append(":");
                        if(second.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(second);

                        String[] dt = new String[1];
                        dt[0] = sb.toString();

                        f = new TIFFField(
                              rootIFD.getTag(BaselineTIFFTagSet.TAG_DATE_TIME),
                              TIFFTag.TIFF_ASCII,
                              1,
                              dt);
                        rootIFD.addTIFFField(f);
                    }

                    child = child.getNextSibling();
                }
            } else if (name.equals("Text")) {
                Node child = node.getFirstChild();
                String theAuthor = null;
                String theDescription = null;
                String theTitle = null;
                while (child != null) {
                    String childName = child.getNodeName();
                    if(childName.equals("TextEntry")) {
                        int tagNumber = -1;
                        NamedNodeMap childAttrs = child.getAttributes();
                        Node keywordNode = childAttrs.getNamedItem("keyword");
                        if(keywordNode != null) {
                            String keyword = keywordNode.getNodeValue();
                            String value = getAttribute(child, "value");
                            if (!keyword.isEmpty() && !value.isEmpty()) {
                                if(keyword.equalsIgnoreCase("DocumentName")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_DOCUMENT_NAME;
                                } else if(keyword.equalsIgnoreCase("ImageDescription")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION;
                                } else if(keyword.equalsIgnoreCase("Make")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_MAKE;
                                } else if(keyword.equalsIgnoreCase("Model")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_MODEL;
                                } else if(keyword.equalsIgnoreCase("PageName")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_PAGE_NAME;
                                } else if(keyword.equalsIgnoreCase("Software")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_SOFTWARE;
                                } else if(keyword.equalsIgnoreCase("Artist")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_ARTIST;
                                } else if(keyword.equalsIgnoreCase("HostComputer")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_HOST_COMPUTER;
                                } else if(keyword.equalsIgnoreCase("InkNames")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_INK_NAMES;
                                } else if(keyword.equalsIgnoreCase("Copyright")) {
                                    tagNumber =
                                        BaselineTIFFTagSet.TAG_COPYRIGHT;
                                } else if(keyword.equalsIgnoreCase("author")) {
                                    theAuthor = value;
                                } else if(keyword.equalsIgnoreCase("description")) {
                                    theDescription = value;
                                } else if(keyword.equalsIgnoreCase("title")) {
                                    theTitle = value;
                                }
                                if(tagNumber != -1) {
                                    f = new TIFFField(rootIFD.getTag(tagNumber),
                                                      TIFFTag.TIFF_ASCII,
                                                      1,
                                                      new String[] {value});
                                    rootIFD.addTIFFField(f);
                                }
                            }
                        }
                    }
                    child = child.getNextSibling();
                } // child != null
                if(theAuthor != null &&
                   getTIFFField(BaselineTIFFTagSet.TAG_ARTIST) == null) {
                    f = new TIFFField(rootIFD.getTag(BaselineTIFFTagSet.TAG_ARTIST),
                                      TIFFTag.TIFF_ASCII,
                                      1,
                                      new String[] {theAuthor});
                    rootIFD.addTIFFField(f);
                }
                if(theDescription != null &&
                   getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION) == null) {
                    f = new TIFFField(rootIFD.getTag(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION),
                                      TIFFTag.TIFF_ASCII,
                                      1,
                                      new String[] {theDescription});
                    rootIFD.addTIFFField(f);
                }
                if(theTitle != null &&
                   getTIFFField(BaselineTIFFTagSet.TAG_DOCUMENT_NAME) == null) {
                    f = new TIFFField(rootIFD.getTag(BaselineTIFFTagSet.TAG_DOCUMENT_NAME),
                                      TIFFTag.TIFF_ASCII,
                                      1,
                                      new String[] {theTitle});
                    rootIFD.addTIFFField(f);
                }
            } else if (name.equals("Transparency")) {
                 Node child = node.getFirstChild();
                 while (child != null) {
                     String childName = child.getNodeName();

                     if (childName.equals("Alpha")) {
                         String alpha = getAttribute(child, "value");

                         f = null;
                         if (alpha.equals("premultiplied")) {
                             f = new TIFFField(
                          rootIFD.getTag(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES),
                          BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA);
                         } else if (alpha.equals("nonpremultiplied")) {
                             f = new TIFFField(
                          rootIFD.getTag(BaselineTIFFTagSet.TAG_EXTRA_SAMPLES),
                          BaselineTIFFTagSet.EXTRA_SAMPLES_UNASSOCIATED_ALPHA);
                         }
                         if (f != null) {
                             rootIFD.addTIFFField(f);
                         }
                     }

                    child = child.getNextSibling();
                 }
            }

            node = node.getNextSibling();
        }

        // Set SampleFormat.
        if(sampleFormat != null) {
            // Derive the value.
            int sf = -1;
            if(sampleFormat.equals("SignedIntegral")) {
                sf = BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER;
            } else if(sampleFormat.equals("UnsignedIntegral")) {
                sf = BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER;
            } else if(sampleFormat.equals("Real")) {
                sf = BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT;
            } else if(sampleFormat.equals("Index")) {
                sf = BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER;
            }

            if(sf != -1) {
                // Derive the count.
                int count = 1;

                // Try SamplesPerPixel first.
                f = getTIFFField(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL);
                if(f != null) {
                    count = f.getAsInt(0);
                } else {
                    // Try BitsPerSample.
                    f = getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
                    if(f != null) {
                        count = f.getCount();
                    }
                }

                char[] sampleFormatArray = new char[count];
                Arrays.fill(sampleFormatArray, (char)sf);

                // Add SampleFormat.
                tag = rootIFD.getTag(BaselineTIFFTagSet.TAG_SAMPLE_FORMAT);
                f = new TIFFField(tag, TIFFTag.TIFF_SHORT,
                                  sampleFormatArray.length, sampleFormatArray);
                rootIFD.addTIFFField(f);
            }
        }
    }

    private static String getAttribute(Node node, String attrName) {
        NamedNodeMap attrs = node.getAttributes();
        Node attr = attrs.getNamedItem(attrName);
        return attr != null ? attr.getNodeValue() : null;
    }

    private Node getChildNode(Node node, String childName) {
        Node childNode = null;
        if(node.hasChildNodes()) {
            NodeList childNodes = node.getChildNodes();
            int length = childNodes.getLength();
            for(int i = 0; i < length; i++) {
                Node item = childNodes.item(i);
                if(item.getNodeName().equals(childName)) {
                    childNode = item;
                    break;
                }
            }
        }
        return childNode;
    }

    public static TIFFIFD parseIFD(Node node) throws IIOInvalidTreeException {
        if (!node.getNodeName().equals("TIFFIFD")) {
            fatal(node, "Expected \"TIFFIFD\" node");
        }

        String tagSetNames = getAttribute(node, "tagSets");
        List<TIFFTagSet> tagSets = new ArrayList<TIFFTagSet>(5);

        if (tagSetNames != null) {
            StringTokenizer st = new StringTokenizer(tagSetNames, ",");
            while (st.hasMoreTokens()) {
                String className = st.nextToken();

                Object o = null;
                Class<?> setClass = null;
                try {
                    ClassLoader cl = TIFFImageMetadata.class.getClassLoader();
                    setClass = Class.forName(className, false, cl);
                    if (!TIFFTagSet.class.isAssignableFrom(setClass)) {
                        fatal(node, "TagSets in IFD must be subset of"
                                + " TIFFTagSet class");
                    }
                    Method getInstanceMethod =
                        setClass.getMethod("getInstance", (Class[])null);
                    o = getInstanceMethod.invoke(null, (Object[])null);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                if (!(o instanceof TIFFTagSet)) {
                    fatal(node, "Specified tag set class \"" +
                          className +
                          "\" is not an instance of TIFFTagSet");
                } else {
                    tagSets.add((TIFFTagSet)o);
                }
            }
        }

        TIFFIFD ifd = new TIFFIFD(tagSets);

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();

            TIFFField f = null;
            if (name.equals("TIFFIFD")) {
                TIFFIFD subIFD = parseIFD(node);
                String parentTagName = getAttribute(node, "parentTagName");
                String parentTagNumber = getAttribute(node, "parentTagNumber");
                TIFFTag tag = null;
                if(parentTagName != null) {
                    tag = TIFFIFD.getTag(parentTagName, tagSets);
                } else if(parentTagNumber != null) {
                    int tagNumber = Integer.parseUnsignedInt(parentTagNumber);
                    tag = TIFFIFD.getTag(tagNumber, tagSets);
                }

                int type;
                if (tag == null) {
                    type = TIFFTag.TIFF_LONG;
                    tag = new TIFFTag(TIFFTag.UNKNOWN_TAG_NAME, 0, 1 << type);
                } else {
                    if (tag.isDataTypeOK(TIFFTag.TIFF_IFD_POINTER)) {
                        type = TIFFTag.TIFF_IFD_POINTER;
                    } else if (tag.isDataTypeOK(TIFFTag.TIFF_LONG)) {
                        type = TIFFTag.TIFF_LONG;
                    } else {
                        for (type = TIFFTag.MAX_DATATYPE;
                            type >= TIFFTag.MIN_DATATYPE;
                            type--) {
                            if (tag.isDataTypeOK(type)) {
                                break;
                            }
                        }
                    }
                }

                f = new TIFFField(tag, type, 1L, subIFD);
            } else if (name.equals("TIFFField")) {
                int number = Integer.parseInt(getAttribute(node, "number"));

                TIFFTagSet tagSet = null;
                for (TIFFTagSet t : tagSets) {
                    if (t.getTag(number) != null) {
                        tagSet = t;
                        break;
                    }
                }

                f = TIFFField.createFromMetadataNode(tagSet, node);
            } else {
                fatal(node,
                      "Expected either \"TIFFIFD\" or \"TIFFField\" node, got "
                      + name);
            }

            ifd.addTIFFField(f);
            node = node.getNextSibling();
        }

        return ifd;
    }

    private void mergeNativeTree(Node root) throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName().equals(nativeMetadataFormatName)) {
            fatal(node, "Root must be " + nativeMetadataFormatName);
        }

        node = node.getFirstChild();
        if (node == null || !node.getNodeName().equals("TIFFIFD")) {
            fatal(root, "Root must have \"TIFFIFD\" child");
        }
        TIFFIFD ifd = parseIFD(node);

        List<TIFFTagSet> rootIFDTagSets = rootIFD.getTagSetList();
        for (Object o : ifd.getTagSetList()) {
            if(o instanceof TIFFTagSet && !rootIFDTagSets.contains(o)) {
                rootIFD.addTagSet((TIFFTagSet)o);
            }
        }

        Iterator<TIFFField> ifdIter = ifd.iterator();
        while(ifdIter.hasNext()) {
            TIFFField field = ifdIter.next();
            rootIFD.addTIFFField(field);
        }
    }

    public void mergeTree(String formatName, Node root)
        throws IIOInvalidTreeException{
        if (formatName.equals(nativeMetadataFormatName)) {
            if (root == null) {
                throw new NullPointerException("root == null!");
            }
            mergeNativeTree(root);
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            if (root == null) {
                throw new NullPointerException("root == null!");
            }
            mergeStandardTree(root);
        } else {
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    public void reset() {
        rootIFD = new TIFFIFD(tagSets);
    }

    public TIFFIFD getRootIFD() {
        return rootIFD;
    }

    public TIFFField getTIFFField(int tagNumber) {
        return rootIFD.getTIFFField(tagNumber);
    }

    public void removeTIFFField(int tagNumber) {
        rootIFD.removeTIFFField(tagNumber);
    }

    /**
     * Returns a {@code TIFFImageMetadata} wherein all fields in the
     * root IFD from the {@code BaselineTIFFTagSet} are copied by value
     * and all other fields copied by reference.
     */
    public TIFFImageMetadata getShallowClone() {
        return new TIFFImageMetadata(rootIFD.getShallowClone());
    }
}
