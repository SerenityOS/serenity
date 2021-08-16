/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.png;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.ListIterator;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;

public class PNGMetadata extends IIOMetadata implements Cloneable {

    // package scope
    public static final String
        nativeMetadataFormatName = "javax_imageio_png_1.0";

    protected static final String nativeMetadataFormatClassName
        = "com.sun.imageio.plugins.png.PNGMetadataFormat";

    // Color types for IHDR chunk
    static final String[] IHDR_colorTypeNames = {
        "Grayscale", null, "RGB", "Palette",
        "GrayAlpha", null, "RGBAlpha"
    };

    static final int[] IHDR_numChannels = {
        1, 0, 3, 3, 2, 0, 4
    };

    // Bit depths for IHDR chunk
    static final String[] IHDR_bitDepths = {
        "1", "2", "4", "8", "16"
    };

    // Compression methods for IHDR chunk
    static final String[] IHDR_compressionMethodNames = {
        "deflate"
    };

    // Filter methods for IHDR chunk
    static final String[] IHDR_filterMethodNames = {
        "adaptive"
    };

    // Interlace methods for IHDR chunk
    static final String[] IHDR_interlaceMethodNames = {
        "none", "adam7"
    };

    // Compression methods for iCCP chunk
    static final String[] iCCP_compressionMethodNames = {
        "deflate"
    };

    // Compression methods for zTXt chunk
    static final String[] zTXt_compressionMethodNames = {
        "deflate"
    };

    // "Unknown" unit for pHYs chunk
    public static final int PHYS_UNIT_UNKNOWN = 0;

    // "Meter" unit for pHYs chunk
    public static final int PHYS_UNIT_METER = 1;

    // Unit specifiers for pHYs chunk
    static final String[] unitSpecifierNames = {
        "unknown", "meter"
    };

    // Rendering intents for sRGB chunk
    static final String[] renderingIntentNames = {
        "Perceptual", // 0
        "Relative colorimetric", // 1
        "Saturation", // 2
        "Absolute colorimetric" // 3

    };

    // Color space types for Chroma->ColorSpaceType node
    static final String[] colorSpaceTypeNames = {
        "GRAY", null, "RGB", "RGB",
        "GRAY", null, "RGB"
    };

    // IHDR chunk
    public boolean IHDR_present;
    public int IHDR_width;
    public int IHDR_height;
    public int IHDR_bitDepth;
    public int IHDR_colorType;
    public int IHDR_compressionMethod;
    public int IHDR_filterMethod;
    public int IHDR_interlaceMethod; // 0 == none, 1 == adam7

    // PLTE chunk
    public boolean PLTE_present;
    public byte[] PLTE_red;
    public byte[] PLTE_green;
    public byte[] PLTE_blue;

    // If non-null, used to reorder palette entries during encoding in
    // order to minimize the size of the tRNS chunk.  Thus an index of
    // 'i' in the source should be encoded as index 'PLTE_order[i]'.
    // PLTE_order will be null unless 'initialize' is called with an
    // IndexColorModel image type.
    public int[] PLTE_order = null;

    // bKGD chunk
    // If external (non-PNG sourced) data has red = green = blue,
    // always store it as gray and promote when writing
    public boolean bKGD_present;
    public int bKGD_colorType; // PNG_COLOR_GRAY, _RGB, or _PALETTE
    public int bKGD_index;
    public int bKGD_gray;
    public int bKGD_red;
    public int bKGD_green;
    public int bKGD_blue;

    // cHRM chunk
    public boolean cHRM_present;
    public int cHRM_whitePointX;
    public int cHRM_whitePointY;
    public int cHRM_redX;
    public int cHRM_redY;
    public int cHRM_greenX;
    public int cHRM_greenY;
    public int cHRM_blueX;
    public int cHRM_blueY;

    // gAMA chunk
    public boolean gAMA_present;
    public int gAMA_gamma;

    // hIST chunk
    public boolean hIST_present;
    public char[] hIST_histogram;

    // iCCP chunk
    public boolean iCCP_present;
    public String iCCP_profileName;
    public int iCCP_compressionMethod;
    public byte[] iCCP_compressedProfile;

    // iTXt chunk
    public ArrayList<String> iTXt_keyword = new ArrayList<String>();
    public ArrayList<Boolean> iTXt_compressionFlag = new ArrayList<Boolean>();
    public ArrayList<Integer> iTXt_compressionMethod = new ArrayList<Integer>();
    public ArrayList<String> iTXt_languageTag = new ArrayList<String>();
    public ArrayList<String> iTXt_translatedKeyword = new ArrayList<String>();
    public ArrayList<String> iTXt_text = new ArrayList<String>();

    // pHYs chunk
    public boolean pHYs_present;
    public int pHYs_pixelsPerUnitXAxis;
    public int pHYs_pixelsPerUnitYAxis;
    public int pHYs_unitSpecifier; // 0 == unknown, 1 == meter

    // sBIT chunk
    public boolean sBIT_present;
    public int sBIT_colorType; // PNG_COLOR_GRAY, _GRAY_ALPHA, _RGB, _RGB_ALPHA
    public int sBIT_grayBits;
    public int sBIT_redBits;
    public int sBIT_greenBits;
    public int sBIT_blueBits;
    public int sBIT_alphaBits;

    // sPLT chunk
    public boolean sPLT_present;
    public String sPLT_paletteName; // 1-79 characters
    public int sPLT_sampleDepth; // 8 or 16
    public int[] sPLT_red;
    public int[] sPLT_green;
    public int[] sPLT_blue;
    public int[] sPLT_alpha;
    public int[] sPLT_frequency;

    // sRGB chunk
    public boolean sRGB_present;
    public int sRGB_renderingIntent;

    // tEXt chunk
    public ArrayList<String> tEXt_keyword = new ArrayList<String>(); // 1-79 characters
    public ArrayList<String> tEXt_text = new ArrayList<String>();

    // tIME chunk. Gives the image modification time.
    public boolean tIME_present;
    public int tIME_year;
    public int tIME_month;
    public int tIME_day;
    public int tIME_hour;
    public int tIME_minute;
    public int tIME_second;

    // Specifies whether metadata contains Standard/Document/ImageCreationTime
    public boolean creation_time_present;

    // Values that make up Standard/Document/ImageCreationTime
    public int creation_time_year;
    public int creation_time_month;
    public int creation_time_day;
    public int creation_time_hour;
    public int creation_time_minute;
    public int creation_time_second;
    public ZoneOffset creation_time_offset;

    /*
     * tEXt_creation_time_present- Specifies whether any text chunk (tEXt, iTXt,
     * zTXt) exists with image creation time. The data structure corresponding
     * to the last decoded text chunk with creation time is indicated by the
     * iterator- tEXt_creation_time_iter.
     *
     * Any update to the text chunks with creation time is reflected on
     * Standard/Document/ImageCreationTime after retrieving time from the text
     * chunk. If there are multiple text chunks with creation time, the time
     * retrieved from the last decoded text chunk will be used. A point to note
     * is that, retrieval of time from text chunks is possible only if the
     * encoded time in the chunk confirms to either the recommended RFC1123
     * format or ISO format.
     *
     * Similarly, any update to Standard/Document/ImageCreationTime is reflected
     * on the last decoded text chunk's data structure with time encoded in
     * RFC1123 format. By updating the text chunk's data structure, we also
     * ensure that PNGImageWriter will write image creation time on the output.
     */
    public boolean tEXt_creation_time_present;
    private ListIterator<String> tEXt_creation_time_iter = null;
    public static final String tEXt_creationTimeKey = "Creation Time";

    // tRNS chunk
    // If external (non-PNG sourced) data has red = green = blue,
    // always store it as gray and promote when writing
    public boolean tRNS_present;
    public int tRNS_colorType; // PNG_COLOR_GRAY, _RGB, or _PALETTE
    public byte[] tRNS_alpha; // May have fewer entries than PLTE_red, etc.
    public int tRNS_gray;
    public int tRNS_red;
    public int tRNS_green;
    public int tRNS_blue;

    // zTXt chunk
    public ArrayList<String> zTXt_keyword = new ArrayList<String>();
    public ArrayList<Integer> zTXt_compressionMethod = new ArrayList<Integer>();
    public ArrayList<String> zTXt_text = new ArrayList<String>();

    // Unknown chunks
    public ArrayList<String> unknownChunkType = new ArrayList<String>();
    public ArrayList<byte[]> unknownChunkData = new ArrayList<byte[]>();

    public PNGMetadata() {
        super(true,
              nativeMetadataFormatName,
              nativeMetadataFormatClassName,
              null, null);
    }

    public PNGMetadata(IIOMetadata metadata) {
        // TODO -- implement
    }

    /**
     * Sets the IHDR_bitDepth and IHDR_colorType variables.
     * The {@code numBands} parameter is necessary since
     * we may only be writing a subset of the image bands.
     */
    public void initialize(ImageTypeSpecifier imageType, int numBands) {
        ColorModel colorModel = imageType.getColorModel();
        SampleModel sampleModel = imageType.getSampleModel();

        // Initialize IHDR_bitDepth
        int[] sampleSize = sampleModel.getSampleSize();
        int bitDepth = sampleSize[0];
        // Choose max bit depth over all channels
        // Fixes bug 4413109
        for (int i = 1; i < sampleSize.length; i++) {
            if (sampleSize[i] > bitDepth) {
                bitDepth = sampleSize[i];
            }
        }
        // Multi-channel images must have a bit depth of 8 or 16
        if (sampleSize.length > 1 && bitDepth < 8) {
            bitDepth = 8;
        }

        // Round bit depth up to a power of 2
        if (bitDepth > 2 && bitDepth < 4) {
            bitDepth = 4;
        } else if (bitDepth > 4 && bitDepth < 8) {
            bitDepth = 8;
        } else if (bitDepth > 8 && bitDepth < 16) {
            bitDepth = 16;
        } else if (bitDepth > 16) {
            throw new RuntimeException("bitDepth > 16!");
        }
        IHDR_bitDepth = bitDepth;

        // Initialize IHDR_colorType
        if (colorModel instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel)colorModel;
            int size = icm.getMapSize();

            byte[] reds = new byte[size];
            icm.getReds(reds);
            byte[] greens = new byte[size];
            icm.getGreens(greens);
            byte[] blues = new byte[size];
            icm.getBlues(blues);

            // Determine whether the color tables are actually a gray ramp
            // if the color type has not been set previously
            boolean isGray = false;
            if (!IHDR_present ||
                (IHDR_colorType != PNGImageReader.PNG_COLOR_PALETTE)) {
                isGray = true;
                int scale = 255/((1 << IHDR_bitDepth) - 1);
                for (int i = 0; i < size; i++) {
                    byte red = reds[i];
                    if ((red != (byte)(i*scale)) ||
                        (red != greens[i]) ||
                        (red != blues[i])) {
                        isGray = false;
                        break;
                    }
                }
            }

            // Determine whether transparency exists
            boolean hasAlpha = colorModel.hasAlpha();

            byte[] alpha = null;
            if (hasAlpha) {
                alpha = new byte[size];
                icm.getAlphas(alpha);
            }

            /*
             * NB: PNG_COLOR_GRAY_ALPHA color type may be not optimal for images
             * contained more than 1024 pixels (or even than 768 pixels in case of
             * single transparent pixel in palette).
             * For such images alpha samples in raster will occupy more space than
             * it is required to store palette so it could be reasonable to
             * use PNG_COLOR_PALETTE color type for large images.
             */

            if (isGray && hasAlpha && (bitDepth == 8 || bitDepth == 16)) {
                IHDR_colorType = PNGImageReader.PNG_COLOR_GRAY_ALPHA;
            } else if (isGray && !hasAlpha) {
                IHDR_colorType = PNGImageReader.PNG_COLOR_GRAY;
            } else {
                IHDR_colorType = PNGImageReader.PNG_COLOR_PALETTE;
                PLTE_present = true;
                PLTE_order = null;
                PLTE_red = reds.clone();
                PLTE_green = greens.clone();
                PLTE_blue = blues.clone();

                if (hasAlpha) {
                    tRNS_present = true;
                    tRNS_colorType = PNGImageReader.PNG_COLOR_PALETTE;

                    PLTE_order = new int[alpha.length];

                    // Reorder the palette so that non-opaque entries
                    // come first.  Since the tRNS chunk does not have
                    // to store trailing 255's, this can save a
                    // considerable amount of space when encoding
                    // images with only one transparent pixel value,
                    // e.g., images from GIF sources.

                    byte[] newAlpha = new byte[alpha.length];

                    // Scan for non-opaque entries and assign them
                    // positions starting at 0.
                    int newIndex = 0;
                    for (int i = 0; i < alpha.length; i++) {
                        if (alpha[i] != (byte)255) {
                            PLTE_order[i] = newIndex;
                            newAlpha[newIndex] = alpha[i];
                            ++newIndex;
                        }
                    }
                    int numTransparent = newIndex;

                    // Scan for opaque entries and assign them
                    // positions following the non-opaque entries.
                    for (int i = 0; i < alpha.length; i++) {
                        if (alpha[i] == (byte)255) {
                            PLTE_order[i] = newIndex++;
                        }
                    }

                    // Reorder the palettes
                    byte[] oldRed = PLTE_red;
                    byte[] oldGreen = PLTE_green;
                    byte[] oldBlue = PLTE_blue;
                    int len = oldRed.length; // All have the same length
                    PLTE_red = new byte[len];
                    PLTE_green = new byte[len];
                    PLTE_blue = new byte[len];
                    for (int i = 0; i < len; i++) {
                        PLTE_red[PLTE_order[i]] = oldRed[i];
                        PLTE_green[PLTE_order[i]] = oldGreen[i];
                        PLTE_blue[PLTE_order[i]] = oldBlue[i];
                    }

                    // Copy only the transparent entries into tRNS_alpha
                    tRNS_alpha = new byte[numTransparent];
                    System.arraycopy(newAlpha, 0,
                                     tRNS_alpha, 0, numTransparent);
                }
            }
        } else {
            if (numBands == 1) {
                IHDR_colorType = PNGImageReader.PNG_COLOR_GRAY;
            } else if (numBands == 2) {
                IHDR_colorType = PNGImageReader.PNG_COLOR_GRAY_ALPHA;
            } else if (numBands == 3) {
                IHDR_colorType = PNGImageReader.PNG_COLOR_RGB;
            } else if (numBands == 4) {
                IHDR_colorType = PNGImageReader.PNG_COLOR_RGB_ALPHA;
            } else {
                throw new RuntimeException("Number of bands not 1-4!");
            }
        }

        IHDR_present = true;
    }

    public boolean isReadOnly() {
        return false;
    }

    private ArrayList<byte[]> cloneBytesArrayList(ArrayList<byte[]> in) {
        if (in == null) {
            return null;
        } else {
            ArrayList<byte[]> list = new ArrayList<byte[]>(in.size());
            for (byte[] b: in) {
                list.add((b == null) ? null : b.clone());
            }
            return list;
        }
    }

    // Deep clone
    public Object clone() {
        PNGMetadata metadata;
        try {
            metadata = (PNGMetadata)super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }

        // unknownChunkData needs deep clone
        metadata.unknownChunkData =
            cloneBytesArrayList(this.unknownChunkData);

        return metadata;
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
        IIOMetadataNode node = null; // scratch node
        IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);

        // IHDR
        if (IHDR_present) {
            IIOMetadataNode IHDR_node = new IIOMetadataNode("IHDR");
            IHDR_node.setAttribute("width", Integer.toString(IHDR_width));
            IHDR_node.setAttribute("height", Integer.toString(IHDR_height));
            IHDR_node.setAttribute("bitDepth",
                                   Integer.toString(IHDR_bitDepth));
            IHDR_node.setAttribute("colorType",
                                   IHDR_colorTypeNames[IHDR_colorType]);
            // IHDR_compressionMethod must be 0 in PNG 1.1
            IHDR_node.setAttribute("compressionMethod",
                          IHDR_compressionMethodNames[IHDR_compressionMethod]);
            // IHDR_filterMethod must be 0 in PNG 1.1
            IHDR_node.setAttribute("filterMethod",
                                    IHDR_filterMethodNames[IHDR_filterMethod]);
            IHDR_node.setAttribute("interlaceMethod",
                              IHDR_interlaceMethodNames[IHDR_interlaceMethod]);
            root.appendChild(IHDR_node);
        }

        // PLTE
        if (PLTE_present) {
            IIOMetadataNode PLTE_node = new IIOMetadataNode("PLTE");
            int numEntries = PLTE_red.length;
            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry = new IIOMetadataNode("PLTEEntry");
                entry.setAttribute("index", Integer.toString(i));
                entry.setAttribute("red",
                                   Integer.toString(PLTE_red[i] & 0xff));
                entry.setAttribute("green",
                                   Integer.toString(PLTE_green[i] & 0xff));
                entry.setAttribute("blue",
                                   Integer.toString(PLTE_blue[i] & 0xff));
                PLTE_node.appendChild(entry);
            }

            root.appendChild(PLTE_node);
        }

        // bKGD
        if (bKGD_present) {
            IIOMetadataNode bKGD_node = new IIOMetadataNode("bKGD");

            if (bKGD_colorType == PNGImageReader.PNG_COLOR_PALETTE) {
                node = new IIOMetadataNode("bKGD_Palette");
                node.setAttribute("index", Integer.toString(bKGD_index));
            } else if (bKGD_colorType == PNGImageReader.PNG_COLOR_GRAY) {
                node = new IIOMetadataNode("bKGD_Grayscale");
                node.setAttribute("gray", Integer.toString(bKGD_gray));
            } else if (bKGD_colorType == PNGImageReader.PNG_COLOR_RGB) {
                node = new IIOMetadataNode("bKGD_RGB");
                node.setAttribute("red", Integer.toString(bKGD_red));
                node.setAttribute("green", Integer.toString(bKGD_green));
                node.setAttribute("blue", Integer.toString(bKGD_blue));
            }
            bKGD_node.appendChild(node);

            root.appendChild(bKGD_node);
        }

        // cHRM
        if (cHRM_present) {
            IIOMetadataNode cHRM_node = new IIOMetadataNode("cHRM");
            cHRM_node.setAttribute("whitePointX",
                              Integer.toString(cHRM_whitePointX));
            cHRM_node.setAttribute("whitePointY",
                              Integer.toString(cHRM_whitePointY));
            cHRM_node.setAttribute("redX", Integer.toString(cHRM_redX));
            cHRM_node.setAttribute("redY", Integer.toString(cHRM_redY));
            cHRM_node.setAttribute("greenX", Integer.toString(cHRM_greenX));
            cHRM_node.setAttribute("greenY", Integer.toString(cHRM_greenY));
            cHRM_node.setAttribute("blueX", Integer.toString(cHRM_blueX));
            cHRM_node.setAttribute("blueY", Integer.toString(cHRM_blueY));

            root.appendChild(cHRM_node);
        }

        // gAMA
        if (gAMA_present) {
            IIOMetadataNode gAMA_node = new IIOMetadataNode("gAMA");
            gAMA_node.setAttribute("value", Integer.toString(gAMA_gamma));

            root.appendChild(gAMA_node);
        }

        // hIST
        if (hIST_present) {
            IIOMetadataNode hIST_node = new IIOMetadataNode("hIST");

            for (int i = 0; i < hIST_histogram.length; i++) {
                IIOMetadataNode hist =
                    new IIOMetadataNode("hISTEntry");
                hist.setAttribute("index", Integer.toString(i));
                hist.setAttribute("value",
                                  Integer.toString(hIST_histogram[i]));
                hIST_node.appendChild(hist);
            }

            root.appendChild(hIST_node);
        }

        // iCCP
        if (iCCP_present) {
            IIOMetadataNode iCCP_node = new IIOMetadataNode("iCCP");
            iCCP_node.setAttribute("profileName", iCCP_profileName);
            iCCP_node.setAttribute("compressionMethod",
                          iCCP_compressionMethodNames[iCCP_compressionMethod]);

            Object profile = iCCP_compressedProfile;
            if (profile != null) {
                profile = ((byte[])profile).clone();
            }
            iCCP_node.setUserObject(profile);

            root.appendChild(iCCP_node);
        }

        // iTXt
        if (iTXt_keyword.size() > 0) {
            IIOMetadataNode iTXt_parent = new IIOMetadataNode("iTXt");
            for (int i = 0; i < iTXt_keyword.size(); i++) {
                IIOMetadataNode iTXt_node = new IIOMetadataNode("iTXtEntry");
                iTXt_node.setAttribute("keyword", iTXt_keyword.get(i));
                iTXt_node.setAttribute("compressionFlag",
                        iTXt_compressionFlag.get(i) ? "TRUE" : "FALSE");
                iTXt_node.setAttribute("compressionMethod",
                        iTXt_compressionMethod.get(i).toString());
                iTXt_node.setAttribute("languageTag",
                                       iTXt_languageTag.get(i));
                iTXt_node.setAttribute("translatedKeyword",
                                       iTXt_translatedKeyword.get(i));
                iTXt_node.setAttribute("text", iTXt_text.get(i));

                iTXt_parent.appendChild(iTXt_node);
            }

            root.appendChild(iTXt_parent);
        }

        // pHYs
        if (pHYs_present) {
            IIOMetadataNode pHYs_node = new IIOMetadataNode("pHYs");
            pHYs_node.setAttribute("pixelsPerUnitXAxis",
                              Integer.toString(pHYs_pixelsPerUnitXAxis));
            pHYs_node.setAttribute("pixelsPerUnitYAxis",
                                   Integer.toString(pHYs_pixelsPerUnitYAxis));
            pHYs_node.setAttribute("unitSpecifier",
                                   unitSpecifierNames[pHYs_unitSpecifier]);

            root.appendChild(pHYs_node);
        }

        // sBIT
        if (sBIT_present) {
            IIOMetadataNode sBIT_node = new IIOMetadataNode("sBIT");

            if (sBIT_colorType == PNGImageReader.PNG_COLOR_GRAY) {
                node = new IIOMetadataNode("sBIT_Grayscale");
                node.setAttribute("gray",
                                  Integer.toString(sBIT_grayBits));
            } else if (sBIT_colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA) {
                node = new IIOMetadataNode("sBIT_GrayAlpha");
                node.setAttribute("gray",
                                  Integer.toString(sBIT_grayBits));
                node.setAttribute("alpha",
                                  Integer.toString(sBIT_alphaBits));
            } else if (sBIT_colorType == PNGImageReader.PNG_COLOR_RGB) {
                node = new IIOMetadataNode("sBIT_RGB");
                node.setAttribute("red",
                                  Integer.toString(sBIT_redBits));
                node.setAttribute("green",
                                  Integer.toString(sBIT_greenBits));
                node.setAttribute("blue",
                                  Integer.toString(sBIT_blueBits));
            } else if (sBIT_colorType == PNGImageReader.PNG_COLOR_RGB_ALPHA) {
                node = new IIOMetadataNode("sBIT_RGBAlpha");
                node.setAttribute("red",
                                  Integer.toString(sBIT_redBits));
                node.setAttribute("green",
                                  Integer.toString(sBIT_greenBits));
                node.setAttribute("blue",
                                  Integer.toString(sBIT_blueBits));
                node.setAttribute("alpha",
                                  Integer.toString(sBIT_alphaBits));
            } else if (sBIT_colorType == PNGImageReader.PNG_COLOR_PALETTE) {
                node = new IIOMetadataNode("sBIT_Palette");
                node.setAttribute("red",
                                  Integer.toString(sBIT_redBits));
                node.setAttribute("green",
                                  Integer.toString(sBIT_greenBits));
                node.setAttribute("blue",
                                  Integer.toString(sBIT_blueBits));
            }
            sBIT_node.appendChild(node);

            root.appendChild(sBIT_node);
        }

        // sPLT
        if (sPLT_present) {
            IIOMetadataNode sPLT_node = new IIOMetadataNode("sPLT");

            sPLT_node.setAttribute("name", sPLT_paletteName);
            sPLT_node.setAttribute("sampleDepth",
                                   Integer.toString(sPLT_sampleDepth));

            int numEntries = sPLT_red.length;
            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry = new IIOMetadataNode("sPLTEntry");
                entry.setAttribute("index", Integer.toString(i));
                entry.setAttribute("red", Integer.toString(sPLT_red[i]));
                entry.setAttribute("green", Integer.toString(sPLT_green[i]));
                entry.setAttribute("blue", Integer.toString(sPLT_blue[i]));
                entry.setAttribute("alpha", Integer.toString(sPLT_alpha[i]));
                entry.setAttribute("frequency",
                                  Integer.toString(sPLT_frequency[i]));
                sPLT_node.appendChild(entry);
            }

            root.appendChild(sPLT_node);
        }

        // sRGB
        if (sRGB_present) {
            IIOMetadataNode sRGB_node = new IIOMetadataNode("sRGB");
            sRGB_node.setAttribute("renderingIntent",
                                   renderingIntentNames[sRGB_renderingIntent]);

            root.appendChild(sRGB_node);
        }

        // tEXt
        if (tEXt_keyword.size() > 0) {
            IIOMetadataNode tEXt_parent = new IIOMetadataNode("tEXt");
            for (int i = 0; i < tEXt_keyword.size(); i++) {
                IIOMetadataNode tEXt_node = new IIOMetadataNode("tEXtEntry");
                tEXt_node.setAttribute("keyword" , tEXt_keyword.get(i));
                tEXt_node.setAttribute("value" , tEXt_text.get(i));

                tEXt_parent.appendChild(tEXt_node);
            }

            root.appendChild(tEXt_parent);
        }

        // tIME
        if (tIME_present) {
            IIOMetadataNode tIME_node = new IIOMetadataNode("tIME");
            tIME_node.setAttribute("year", Integer.toString(tIME_year));
            tIME_node.setAttribute("month", Integer.toString(tIME_month));
            tIME_node.setAttribute("day", Integer.toString(tIME_day));
            tIME_node.setAttribute("hour", Integer.toString(tIME_hour));
            tIME_node.setAttribute("minute", Integer.toString(tIME_minute));
            tIME_node.setAttribute("second", Integer.toString(tIME_second));

            root.appendChild(tIME_node);
        }

        // tRNS
        if (tRNS_present) {
            IIOMetadataNode tRNS_node = new IIOMetadataNode("tRNS");

            if (tRNS_colorType == PNGImageReader.PNG_COLOR_PALETTE) {
                node = new IIOMetadataNode("tRNS_Palette");

                for (int i = 0; i < tRNS_alpha.length; i++) {
                    IIOMetadataNode entry =
                        new IIOMetadataNode("tRNS_PaletteEntry");
                    entry.setAttribute("index", Integer.toString(i));
                    entry.setAttribute("alpha",
                                       Integer.toString(tRNS_alpha[i] & 0xff));
                    node.appendChild(entry);
                }
            } else if (tRNS_colorType == PNGImageReader.PNG_COLOR_GRAY) {
                node = new IIOMetadataNode("tRNS_Grayscale");
                node.setAttribute("gray", Integer.toString(tRNS_gray));
            } else if (tRNS_colorType == PNGImageReader.PNG_COLOR_RGB) {
                node = new IIOMetadataNode("tRNS_RGB");
                node.setAttribute("red", Integer.toString(tRNS_red));
                node.setAttribute("green", Integer.toString(tRNS_green));
                node.setAttribute("blue", Integer.toString(tRNS_blue));
            }
            tRNS_node.appendChild(node);

            root.appendChild(tRNS_node);
        }

        // zTXt
        if (zTXt_keyword.size() > 0) {
            IIOMetadataNode zTXt_parent = new IIOMetadataNode("zTXt");
            for (int i = 0; i < zTXt_keyword.size(); i++) {
                IIOMetadataNode zTXt_node = new IIOMetadataNode("zTXtEntry");
                zTXt_node.setAttribute("keyword", zTXt_keyword.get(i));

                int cm = (zTXt_compressionMethod.get(i)).intValue();
                zTXt_node.setAttribute("compressionMethod",
                                       zTXt_compressionMethodNames[cm]);

                zTXt_node.setAttribute("text", zTXt_text.get(i));

                zTXt_parent.appendChild(zTXt_node);
            }

            root.appendChild(zTXt_parent);
        }

        // Unknown chunks
        if (unknownChunkType.size() > 0) {
            IIOMetadataNode unknown_parent =
                new IIOMetadataNode("UnknownChunks");
            for (int i = 0; i < unknownChunkType.size(); i++) {
                IIOMetadataNode unknown_node =
                    new IIOMetadataNode("UnknownChunk");
                unknown_node.setAttribute("type",
                                          unknownChunkType.get(i));
                unknown_node.setUserObject(unknownChunkData.get(i));

                unknown_parent.appendChild(unknown_node);
            }

            root.appendChild(unknown_parent);
        }

        return root;
    }

    private int getNumChannels() {
        // Determine number of channels
        // Be careful about palette color with transparency
        int numChannels = IHDR_numChannels[IHDR_colorType];
        if (IHDR_colorType == PNGImageReader.PNG_COLOR_PALETTE &&
            tRNS_present && tRNS_colorType == IHDR_colorType) {
            numChannels = 4;
        }
        return numChannels;
    }

    public IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma_node = new IIOMetadataNode("Chroma");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("ColorSpaceType");
        node.setAttribute("name", colorSpaceTypeNames[IHDR_colorType]);
        chroma_node.appendChild(node);

        node = new IIOMetadataNode("NumChannels");
        node.setAttribute("value", Integer.toString(getNumChannels()));
        chroma_node.appendChild(node);

        if (gAMA_present) {
            node = new IIOMetadataNode("Gamma");
            node.setAttribute("value", Float.toString(gAMA_gamma*1.0e-5F));
            chroma_node.appendChild(node);
        }

        node = new IIOMetadataNode("BlackIsZero");
        node.setAttribute("value", "TRUE");
        chroma_node.appendChild(node);

        if (PLTE_present) {
            boolean hasAlpha = tRNS_present &&
                (tRNS_colorType == PNGImageReader.PNG_COLOR_PALETTE);

            node = new IIOMetadataNode("Palette");
            for (int i = 0; i < PLTE_red.length; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("PaletteEntry");
                entry.setAttribute("index", Integer.toString(i));
                entry.setAttribute("red",
                                   Integer.toString(PLTE_red[i] & 0xff));
                entry.setAttribute("green",
                                   Integer.toString(PLTE_green[i] & 0xff));
                entry.setAttribute("blue",
                                   Integer.toString(PLTE_blue[i] & 0xff));
                if (hasAlpha) {
                    int alpha = (i < tRNS_alpha.length) ?
                        (tRNS_alpha[i] & 0xff) : 255;
                    entry.setAttribute("alpha", Integer.toString(alpha));
                }
                node.appendChild(entry);
            }
            chroma_node.appendChild(node);
        }

        if (bKGD_present) {
            if (bKGD_colorType == PNGImageReader.PNG_COLOR_PALETTE) {
                node = new IIOMetadataNode("BackgroundIndex");
                node.setAttribute("value", Integer.toString(bKGD_index));
            } else {
                node = new IIOMetadataNode("BackgroundColor");
                int r, g, b;

                if (bKGD_colorType == PNGImageReader.PNG_COLOR_GRAY) {
                    r = g = b = bKGD_gray;
                } else {
                    r = bKGD_red;
                    g = bKGD_green;
                    b = bKGD_blue;
                }
                node.setAttribute("red", Integer.toString(r));
                node.setAttribute("green", Integer.toString(g));
                node.setAttribute("blue", Integer.toString(b));
            }
            chroma_node.appendChild(node);
        }

        return chroma_node;
    }

    public IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("CompressionTypeName");
        node.setAttribute("value", "deflate");
        compression_node.appendChild(node);

        node = new IIOMetadataNode("Lossless");
        node.setAttribute("value", "TRUE");
        compression_node.appendChild(node);

        node = new IIOMetadataNode("NumProgressiveScans");
        node.setAttribute("value",
                          (IHDR_interlaceMethod == 0) ? "1" : "7");
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

        node = new IIOMetadataNode("PlanarConfiguration");
        node.setAttribute("value", "PixelInterleaved");
        data_node.appendChild(node);

        node = new IIOMetadataNode("SampleFormat");
        node.setAttribute("value",
                          IHDR_colorType == PNGImageReader.PNG_COLOR_PALETTE ?
                          "Index" : "UnsignedIntegral");
        data_node.appendChild(node);

        String bitDepth = Integer.toString(IHDR_bitDepth);
        node = new IIOMetadataNode("BitsPerSample");
        node.setAttribute("value", repeat(bitDepth, getNumChannels()));
        data_node.appendChild(node);

        if (sBIT_present) {
            node = new IIOMetadataNode("SignificantBitsPerSample");
            String sbits;
            if (sBIT_colorType == PNGImageReader.PNG_COLOR_GRAY ||
                sBIT_colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA) {
                sbits = Integer.toString(sBIT_grayBits);
            } else { // sBIT_colorType == PNGImageReader.PNG_COLOR_RGB ||
                     // sBIT_colorType == PNGImageReader.PNG_COLOR_RGB_ALPHA
                sbits = Integer.toString(sBIT_redBits) + " " +
                    Integer.toString(sBIT_greenBits) + " " +
                    Integer.toString(sBIT_blueBits);
            }

            if (sBIT_colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA ||
                sBIT_colorType == PNGImageReader.PNG_COLOR_RGB_ALPHA) {
                sbits += " " + Integer.toString(sBIT_alphaBits);
            }

            node.setAttribute("value", sbits);
            data_node.appendChild(node);
        }

        // SampleMSB

        return data_node;
    }

    public IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("PixelAspectRatio");
        float ratio = pHYs_present ?
            (float)pHYs_pixelsPerUnitXAxis/pHYs_pixelsPerUnitYAxis : 1.0F;
        node.setAttribute("value", Float.toString(ratio));
        dimension_node.appendChild(node);

        node = new IIOMetadataNode("ImageOrientation");
        node.setAttribute("value", "Normal");
        dimension_node.appendChild(node);

        if (pHYs_present && pHYs_unitSpecifier == PHYS_UNIT_METER) {
            node = new IIOMetadataNode("HorizontalPixelSize");
            node.setAttribute("value",
                              Float.toString(1000.0F/pHYs_pixelsPerUnitXAxis));
            dimension_node.appendChild(node);

            node = new IIOMetadataNode("VerticalPixelSize");
            node.setAttribute("value",
                              Float.toString(1000.0F/pHYs_pixelsPerUnitYAxis));
            dimension_node.appendChild(node);
        }

        return dimension_node;
    }

    public IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document_node = null;

        // Check if image modification time exists
        if (tIME_present) {
            // Create new document node
            document_node = new IIOMetadataNode("Document");

            // Node to hold image modification time
            IIOMetadataNode node = new IIOMetadataNode("ImageModificationTime");
            node.setAttribute("year", Integer.toString(tIME_year));
            node.setAttribute("month", Integer.toString(tIME_month));
            node.setAttribute("day", Integer.toString(tIME_day));
            node.setAttribute("hour", Integer.toString(tIME_hour));
            node.setAttribute("minute", Integer.toString(tIME_minute));
            node.setAttribute("second", Integer.toString(tIME_second));
            document_node.appendChild(node);
        }

        // Check if image creation time exists
        if (creation_time_present) {
            if (document_node == null) {
                // Create new document node
                document_node = new IIOMetadataNode("Document");
            }

            // Node to hold image creation time
            IIOMetadataNode node = new IIOMetadataNode("ImageCreationTime");
            node.setAttribute("year", Integer.toString(creation_time_year));
            node.setAttribute("month", Integer.toString(creation_time_month));
            node.setAttribute("day", Integer.toString(creation_time_day));
            node.setAttribute("hour", Integer.toString(creation_time_hour));
            node.setAttribute("minute", Integer.toString(creation_time_minute));
            node.setAttribute("second", Integer.toString(creation_time_second));
            document_node.appendChild(node);
        }

        return document_node;
    }

    public IIOMetadataNode getStandardTextNode() {
        int numEntries = tEXt_keyword.size() +
            iTXt_keyword.size() + zTXt_keyword.size();
        if (numEntries == 0) {
            return null;
        }

        IIOMetadataNode text_node = new IIOMetadataNode("Text");
        IIOMetadataNode node = null; // scratch node

        for (int i = 0; i < tEXt_keyword.size(); i++) {
            node = new IIOMetadataNode("TextEntry");
            node.setAttribute("keyword", tEXt_keyword.get(i));
            node.setAttribute("value", tEXt_text.get(i));
            node.setAttribute("encoding", "ISO-8859-1");
            node.setAttribute("compression", "none");

            text_node.appendChild(node);
        }

        for (int i = 0; i < iTXt_keyword.size(); i++) {
            node = new IIOMetadataNode("TextEntry");
            node.setAttribute("keyword", iTXt_keyword.get(i));
            node.setAttribute("value", iTXt_text.get(i));
            node.setAttribute("language",
                              iTXt_languageTag.get(i));
            if (iTXt_compressionFlag.get(i)) {
                node.setAttribute("compression", "zip");
            } else {
                node.setAttribute("compression", "none");
            }

            text_node.appendChild(node);
        }

        for (int i = 0; i < zTXt_keyword.size(); i++) {
            node = new IIOMetadataNode("TextEntry");
            node.setAttribute("keyword", zTXt_keyword.get(i));
            node.setAttribute("value", zTXt_text.get(i));
            node.setAttribute("compression", "zip");

            text_node.appendChild(node);
        }

        return text_node;
    }

    public IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparency_node =
            new IIOMetadataNode("Transparency");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("Alpha");
        boolean hasAlpha =
            (IHDR_colorType == PNGImageReader.PNG_COLOR_RGB_ALPHA) ||
            (IHDR_colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA) ||
            (IHDR_colorType == PNGImageReader.PNG_COLOR_PALETTE &&
             tRNS_present &&
             (tRNS_colorType == IHDR_colorType) &&
             (tRNS_alpha != null));
        node.setAttribute("value", hasAlpha ? "nonpremultipled" : "none");
        transparency_node.appendChild(node);

        if (tRNS_present) {
            node = new IIOMetadataNode("TransparentColor");
            if (tRNS_colorType == PNGImageReader.PNG_COLOR_RGB) {
                node.setAttribute("value",
                                  Integer.toString(tRNS_red) + " " +
                                  Integer.toString(tRNS_green) + " " +
                                  Integer.toString(tRNS_blue));
            } else if (tRNS_colorType == PNGImageReader.PNG_COLOR_GRAY) {
                node.setAttribute("value", Integer.toString(tRNS_gray));
            }
            transparency_node.appendChild(node);
        }

        return transparency_node;
    }

    // Shorthand for throwing an IIOInvalidTreeException
    private void fatal(Node node, String reason)
        throws IIOInvalidTreeException {
        throw new IIOInvalidTreeException(reason, node);
    }

    // Get an integer-valued attribute
    private String getStringAttribute(Node node, String name,
                                      String defaultValue, boolean required)
        throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        return attr.getNodeValue();
    }


    // Get an integer-valued attribute
    private int getIntAttribute(Node node, String name,
                                int defaultValue, boolean required)
        throws IIOInvalidTreeException {
        String value = getStringAttribute(node, name, null, required);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    // Get a float-valued attribute
    private float getFloatAttribute(Node node, String name,
                                    float defaultValue, boolean required)
        throws IIOInvalidTreeException {
        String value = getStringAttribute(node, name, null, required);
        if (value == null) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    // Get a required integer-valued attribute
    private int getIntAttribute(Node node, String name)
        throws IIOInvalidTreeException {
        return getIntAttribute(node, name, -1, true);
    }

    // Get a required float-valued attribute
    private float getFloatAttribute(Node node, String name)
        throws IIOInvalidTreeException {
        return getFloatAttribute(node, name, -1.0F, true);
    }

    // Get a boolean-valued attribute
    private boolean getBooleanAttribute(Node node, String name,
                                        boolean defaultValue,
                                        boolean required)
        throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        String value = attr.getNodeValue();
        // Allow lower case booleans for backward compatibility, #5082756
        if (value.equals("TRUE") || value.equals("true")) {
            return true;
        } else if (value.equals("FALSE") || value.equals("false")) {
            return false;
        } else {
            fatal(node, "Attribute " + name + " must be 'TRUE' or 'FALSE'!");
            return false;
        }
    }

    // Get a required boolean-valued attribute
    private boolean getBooleanAttribute(Node node, String name)
        throws IIOInvalidTreeException {
        return getBooleanAttribute(node, name, false, true);
    }

    // Get an enumerated attribute as an index into a String array
    private int getEnumeratedAttribute(Node node,
                                       String name, String[] legalNames,
                                       int defaultValue, boolean required)
        throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        String value = attr.getNodeValue();
        for (int i = 0; i < legalNames.length; i++) {
            if (value.equals(legalNames[i])) {
                return i;
            }
        }

        fatal(node, "Illegal value for attribute " + name + "!");
        return -1;
    }

    // Get a required enumerated attribute as an index into a String array
    private int getEnumeratedAttribute(Node node,
                                       String name, String[] legalNames)
        throws IIOInvalidTreeException {
        return getEnumeratedAttribute(node, name, legalNames, -1, true);
    }

    // Get a String-valued attribute
    private String getAttribute(Node node, String name,
                                String defaultValue, boolean required)
        throws IIOInvalidTreeException {
        Node attr = node.getAttributes().getNamedItem(name);
        if (attr == null) {
            if (!required) {
                return defaultValue;
            } else {
                fatal(node, "Required attribute " + name + " not present!");
            }
        }
        return attr.getNodeValue();
    }

    // Get a required String-valued attribute
    private String getAttribute(Node node, String name)
        throws IIOInvalidTreeException {
            return getAttribute(node, name, null, true);
    }

    public void mergeTree(String formatName, Node root)
        throws IIOInvalidTreeException {
        if (formatName.equals(nativeMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeNativeTree(root);
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeStandardTree(root);
        } else {
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    private void mergeNativeTree(Node root)
        throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName().equals(nativeMetadataFormatName)) {
            fatal(node, "Root must be " + nativeMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();

            if (name.equals("IHDR")) {
                IHDR_width = getIntAttribute(node, "width");
                IHDR_height = getIntAttribute(node, "height");
                IHDR_bitDepth =
                        Integer.valueOf(IHDR_bitDepths[
                                getEnumeratedAttribute(node,
                                                    "bitDepth",
                                                    IHDR_bitDepths)]);
                IHDR_colorType = getEnumeratedAttribute(node, "colorType",
                                                        IHDR_colorTypeNames);
                IHDR_compressionMethod =
                    getEnumeratedAttribute(node, "compressionMethod",
                                           IHDR_compressionMethodNames);
                IHDR_filterMethod =
                    getEnumeratedAttribute(node,
                                           "filterMethod",
                                           IHDR_filterMethodNames);
                IHDR_interlaceMethod =
                    getEnumeratedAttribute(node, "interlaceMethod",
                                           IHDR_interlaceMethodNames);
                IHDR_present = true;
            } else if (name.equals("PLTE")) {
                byte[] red = new byte[256];
                byte[] green  = new byte[256];
                byte[] blue = new byte[256];
                int maxindex = -1;

                Node PLTE_entry = node.getFirstChild();
                if (PLTE_entry == null) {
                    fatal(node, "Palette has no entries!");
                }

                while (PLTE_entry != null) {
                    if (!PLTE_entry.getNodeName().equals("PLTEEntry")) {
                        fatal(node,
                              "Only a PLTEEntry may be a child of a PLTE!");
                    }

                    int index = getIntAttribute(PLTE_entry, "index");
                    if (index < 0 || index > 255) {
                        fatal(node,
                              "Bad value for PLTEEntry attribute index!");
                    }
                    if (index > maxindex) {
                        maxindex = index;
                    }
                    red[index] =
                        (byte)getIntAttribute(PLTE_entry, "red");
                    green[index] =
                        (byte)getIntAttribute(PLTE_entry, "green");
                    blue[index] =
                        (byte)getIntAttribute(PLTE_entry, "blue");

                    PLTE_entry = PLTE_entry.getNextSibling();
                }

                int numEntries = maxindex + 1;
                PLTE_red = new byte[numEntries];
                PLTE_green = new byte[numEntries];
                PLTE_blue = new byte[numEntries];
                System.arraycopy(red, 0, PLTE_red, 0, numEntries);
                System.arraycopy(green, 0, PLTE_green, 0, numEntries);
                System.arraycopy(blue, 0, PLTE_blue, 0, numEntries);
                PLTE_present = true;
            } else if (name.equals("bKGD")) {
                bKGD_present = false; // Guard against partial overwrite
                Node bKGD_node = node.getFirstChild();
                if (bKGD_node == null) {
                    fatal(node, "bKGD node has no children!");
                }
                String bKGD_name = bKGD_node.getNodeName();
                if (bKGD_name.equals("bKGD_Palette")) {
                    bKGD_index = getIntAttribute(bKGD_node, "index");
                    bKGD_colorType = PNGImageReader.PNG_COLOR_PALETTE;
                } else if (bKGD_name.equals("bKGD_Grayscale")) {
                    bKGD_gray = getIntAttribute(bKGD_node, "gray");
                    bKGD_colorType = PNGImageReader.PNG_COLOR_GRAY;
                } else if (bKGD_name.equals("bKGD_RGB")) {
                    bKGD_red = getIntAttribute(bKGD_node, "red");
                    bKGD_green = getIntAttribute(bKGD_node, "green");
                    bKGD_blue = getIntAttribute(bKGD_node, "blue");
                    bKGD_colorType = PNGImageReader.PNG_COLOR_RGB;
                } else {
                    fatal(node, "Bad child of a bKGD node!");
                }
                if (bKGD_node.getNextSibling() != null) {
                    fatal(node, "bKGD node has more than one child!");
                }

                bKGD_present = true;
            } else if (name.equals("cHRM")) {
                cHRM_whitePointX = getIntAttribute(node, "whitePointX");
                cHRM_whitePointY = getIntAttribute(node, "whitePointY");
                cHRM_redX = getIntAttribute(node, "redX");
                cHRM_redY = getIntAttribute(node, "redY");
                cHRM_greenX = getIntAttribute(node, "greenX");
                cHRM_greenY = getIntAttribute(node, "greenY");
                cHRM_blueX = getIntAttribute(node, "blueX");
                cHRM_blueY = getIntAttribute(node, "blueY");

                cHRM_present = true;
            } else if (name.equals("gAMA")) {
                gAMA_gamma = getIntAttribute(node, "value");
                gAMA_present = true;
            } else if (name.equals("hIST")) {
                char[] hist = new char[256];
                int maxindex = -1;

                Node hIST_entry = node.getFirstChild();
                if (hIST_entry == null) {
                    fatal(node, "hIST node has no children!");
                }

                while (hIST_entry != null) {
                    if (!hIST_entry.getNodeName().equals("hISTEntry")) {
                        fatal(node,
                              "Only a hISTEntry may be a child of a hIST!");
                    }

                    int index = getIntAttribute(hIST_entry, "index");
                    if (index < 0 || index > 255) {
                        fatal(node,
                              "Bad value for histEntry attribute index!");
                    }
                    if (index > maxindex) {
                        maxindex = index;
                    }
                    hist[index] =
                        (char)getIntAttribute(hIST_entry, "value");

                    hIST_entry = hIST_entry.getNextSibling();
                }

                int numEntries = maxindex + 1;
                hIST_histogram = new char[numEntries];
                System.arraycopy(hist, 0, hIST_histogram, 0, numEntries);

                hIST_present = true;
            } else if (name.equals("iCCP")) {
                iCCP_profileName = getAttribute(node, "profileName");
                iCCP_compressionMethod =
                    getEnumeratedAttribute(node, "compressionMethod",
                                           iCCP_compressionMethodNames);
                Object compressedProfile =
                    ((IIOMetadataNode)node).getUserObject();
                if (compressedProfile == null) {
                    fatal(node, "No ICCP profile present in user object!");
                }
                if (!(compressedProfile instanceof byte[])) {
                    fatal(node, "User object not a byte array!");
                }

                iCCP_compressedProfile = ((byte[])compressedProfile).clone();

                iCCP_present = true;
            } else if (name.equals("iTXt")) {
                Node iTXt_node = node.getFirstChild();
                while (iTXt_node != null) {
                    if (!iTXt_node.getNodeName().equals("iTXtEntry")) {
                        fatal(node,
                              "Only an iTXtEntry may be a child of an iTXt!");
                    }

                    String keyword = getAttribute(iTXt_node, "keyword");
                    if (isValidKeyword(keyword)) {
                        iTXt_keyword.add(keyword);

                        boolean compressionFlag =
                            getBooleanAttribute(iTXt_node, "compressionFlag");
                        iTXt_compressionFlag.add(Boolean.valueOf(compressionFlag));

                        String compressionMethod =
                            getAttribute(iTXt_node, "compressionMethod");
                        iTXt_compressionMethod.add(Integer.valueOf(compressionMethod));

                        String languageTag =
                            getAttribute(iTXt_node, "languageTag");
                        iTXt_languageTag.add(languageTag);

                        String translatedKeyword =
                            getAttribute(iTXt_node, "translatedKeyword");
                        iTXt_translatedKeyword.add(translatedKeyword);

                        String text = getAttribute(iTXt_node, "text");
                        iTXt_text.add(text);

                        // Check if the text chunk contains image creation time
                        if (keyword.equals(PNGMetadata.tEXt_creationTimeKey)) {
                            // Update Standard/Document/ImageCreationTime
                            int index = iTXt_text.size()-1;
                            decodeImageCreationTimeFromTextChunk(
                                    iTXt_text.listIterator(index));
                        }
                    }
                    // silently skip invalid text entry

                    iTXt_node = iTXt_node.getNextSibling();
                }
            } else if (name.equals("pHYs")) {
                pHYs_pixelsPerUnitXAxis =
                    getIntAttribute(node, "pixelsPerUnitXAxis");
                pHYs_pixelsPerUnitYAxis =
                    getIntAttribute(node, "pixelsPerUnitYAxis");
                pHYs_unitSpecifier =
                    getEnumeratedAttribute(node, "unitSpecifier",
                                           unitSpecifierNames);

                pHYs_present = true;
            } else if (name.equals("sBIT")) {
                sBIT_present = false; // Guard against partial overwrite
                Node sBIT_node = node.getFirstChild();
                if (sBIT_node == null) {
                    fatal(node, "sBIT node has no children!");
                }
                String sBIT_name = sBIT_node.getNodeName();
                if (sBIT_name.equals("sBIT_Grayscale")) {
                    sBIT_grayBits = getIntAttribute(sBIT_node, "gray");
                    sBIT_colorType = PNGImageReader.PNG_COLOR_GRAY;
                } else if (sBIT_name.equals("sBIT_GrayAlpha")) {
                    sBIT_grayBits = getIntAttribute(sBIT_node, "gray");
                    sBIT_alphaBits = getIntAttribute(sBIT_node, "alpha");
                    sBIT_colorType = PNGImageReader.PNG_COLOR_GRAY_ALPHA;
                } else if (sBIT_name.equals("sBIT_RGB")) {
                    sBIT_redBits = getIntAttribute(sBIT_node, "red");
                    sBIT_greenBits = getIntAttribute(sBIT_node, "green");
                    sBIT_blueBits = getIntAttribute(sBIT_node, "blue");
                    sBIT_colorType = PNGImageReader.PNG_COLOR_RGB;
                } else if (sBIT_name.equals("sBIT_RGBAlpha")) {
                    sBIT_redBits = getIntAttribute(sBIT_node, "red");
                    sBIT_greenBits = getIntAttribute(sBIT_node, "green");
                    sBIT_blueBits = getIntAttribute(sBIT_node, "blue");
                    sBIT_alphaBits = getIntAttribute(sBIT_node, "alpha");
                    sBIT_colorType = PNGImageReader.PNG_COLOR_RGB_ALPHA;
                } else if (sBIT_name.equals("sBIT_Palette")) {
                    sBIT_redBits = getIntAttribute(sBIT_node, "red");
                    sBIT_greenBits = getIntAttribute(sBIT_node, "green");
                    sBIT_blueBits = getIntAttribute(sBIT_node, "blue");
                    sBIT_colorType = PNGImageReader.PNG_COLOR_PALETTE;
                } else {
                    fatal(node, "Bad child of an sBIT node!");
                }
                if (sBIT_node.getNextSibling() != null) {
                    fatal(node, "sBIT node has more than one child!");
                }

                sBIT_present = true;
            } else if (name.equals("sPLT")) {
                sPLT_paletteName = getAttribute(node, "name");
                sPLT_sampleDepth = getIntAttribute(node, "sampleDepth");

                int[] red = new int[256];
                int[] green  = new int[256];
                int[] blue = new int[256];
                int[] alpha = new int[256];
                int[] frequency = new int[256];
                int maxindex = -1;

                Node sPLT_entry = node.getFirstChild();
                if (sPLT_entry == null) {
                    fatal(node, "sPLT node has no children!");
                }

                while (sPLT_entry != null) {
                    if (!sPLT_entry.getNodeName().equals("sPLTEntry")) {
                        fatal(node,
                              "Only an sPLTEntry may be a child of an sPLT!");
                    }

                    int index = getIntAttribute(sPLT_entry, "index");
                    if (index < 0 || index > 255) {
                        fatal(node,
                              "Bad value for PLTEEntry attribute index!");
                    }
                    if (index > maxindex) {
                        maxindex = index;
                    }
                    red[index] = getIntAttribute(sPLT_entry, "red");
                    green[index] = getIntAttribute(sPLT_entry, "green");
                    blue[index] = getIntAttribute(sPLT_entry, "blue");
                    alpha[index] = getIntAttribute(sPLT_entry, "alpha");
                    frequency[index] =
                        getIntAttribute(sPLT_entry, "frequency");

                    sPLT_entry = sPLT_entry.getNextSibling();
                }

                int numEntries = maxindex + 1;
                sPLT_red = new int[numEntries];
                sPLT_green = new int[numEntries];
                sPLT_blue = new int[numEntries];
                sPLT_alpha = new int[numEntries];
                sPLT_frequency = new int[numEntries];
                System.arraycopy(red, 0, sPLT_red, 0, numEntries);
                System.arraycopy(green, 0, sPLT_green, 0, numEntries);
                System.arraycopy(blue, 0, sPLT_blue, 0, numEntries);
                System.arraycopy(alpha, 0, sPLT_alpha, 0, numEntries);
                System.arraycopy(frequency, 0,
                                 sPLT_frequency, 0, numEntries);

                sPLT_present = true;
            } else if (name.equals("sRGB")) {
                sRGB_renderingIntent =
                    getEnumeratedAttribute(node, "renderingIntent",
                                           renderingIntentNames);

                sRGB_present = true;
            } else if (name.equals("tEXt")) {
                Node tEXt_node = node.getFirstChild();
                while (tEXt_node != null) {
                    if (!tEXt_node.getNodeName().equals("tEXtEntry")) {
                        fatal(node,
                              "Only an tEXtEntry may be a child of an tEXt!");
                    }

                    String keyword = getAttribute(tEXt_node, "keyword");
                    tEXt_keyword.add(keyword);

                    String text = getAttribute(tEXt_node, "value");
                    tEXt_text.add(text);

                    // Check if the text chunk contains image creation time
                    if (keyword.equals(PNGMetadata.tEXt_creationTimeKey)) {
                        // Update Standard/Document/ImageCreationTime
                        int index = tEXt_text.size()-1;
                        decodeImageCreationTimeFromTextChunk(
                                tEXt_text.listIterator(index));
                    }
                    tEXt_node = tEXt_node.getNextSibling();
                }
            } else if (name.equals("tIME")) {
                tIME_year = getIntAttribute(node, "year");
                tIME_month = getIntAttribute(node, "month");
                tIME_day = getIntAttribute(node, "day");
                tIME_hour = getIntAttribute(node, "hour");
                tIME_minute = getIntAttribute(node, "minute");
                tIME_second = getIntAttribute(node, "second");

                tIME_present = true;
            } else if (name.equals("tRNS")) {
                tRNS_present = false; // Guard against partial overwrite
                Node tRNS_node = node.getFirstChild();
                if (tRNS_node == null) {
                    fatal(node, "tRNS node has no children!");
                }
                String tRNS_name = tRNS_node.getNodeName();
                if (tRNS_name.equals("tRNS_Palette")) {
                    byte[] alpha = new byte[256];
                    int maxindex = -1;

                    Node tRNS_paletteEntry = tRNS_node.getFirstChild();
                    if (tRNS_paletteEntry == null) {
                        fatal(node, "tRNS_Palette node has no children!");
                    }
                    while (tRNS_paletteEntry != null) {
                        if (!tRNS_paletteEntry.getNodeName().equals(
                                                        "tRNS_PaletteEntry")) {
                            fatal(node,
                 "Only a tRNS_PaletteEntry may be a child of a tRNS_Palette!");
                        }
                        int index =
                            getIntAttribute(tRNS_paletteEntry, "index");
                        if (index < 0 || index > 255) {
                            fatal(node,
                           "Bad value for tRNS_PaletteEntry attribute index!");
                        }
                        if (index > maxindex) {
                            maxindex = index;
                        }
                        alpha[index] =
                            (byte)getIntAttribute(tRNS_paletteEntry,
                                                  "alpha");

                        tRNS_paletteEntry =
                            tRNS_paletteEntry.getNextSibling();
                    }

                    int numEntries = maxindex + 1;
                    tRNS_alpha = new byte[numEntries];
                    tRNS_colorType = PNGImageReader.PNG_COLOR_PALETTE;
                    System.arraycopy(alpha, 0, tRNS_alpha, 0, numEntries);
                } else if (tRNS_name.equals("tRNS_Grayscale")) {
                    tRNS_gray = getIntAttribute(tRNS_node, "gray");
                    tRNS_colorType = PNGImageReader.PNG_COLOR_GRAY;
                } else if (tRNS_name.equals("tRNS_RGB")) {
                    tRNS_red = getIntAttribute(tRNS_node, "red");
                    tRNS_green = getIntAttribute(tRNS_node, "green");
                    tRNS_blue = getIntAttribute(tRNS_node, "blue");
                    tRNS_colorType = PNGImageReader.PNG_COLOR_RGB;
                } else {
                    fatal(node, "Bad child of a tRNS node!");
                }
                if (tRNS_node.getNextSibling() != null) {
                    fatal(node, "tRNS node has more than one child!");
                }

                tRNS_present = true;
            } else if (name.equals("zTXt")) {
                Node zTXt_node = node.getFirstChild();
                while (zTXt_node != null) {
                    if (!zTXt_node.getNodeName().equals("zTXtEntry")) {
                        fatal(node,
                              "Only an zTXtEntry may be a child of an zTXt!");
                    }

                    String keyword = getAttribute(zTXt_node, "keyword");
                    zTXt_keyword.add(keyword);

                    int compressionMethod =
                        getEnumeratedAttribute(zTXt_node, "compressionMethod",
                                               zTXt_compressionMethodNames);
                    zTXt_compressionMethod.add(compressionMethod);

                    String text = getAttribute(zTXt_node, "text");
                    zTXt_text.add(text);

                    // Check if the text chunk contains image creation time
                    if (keyword.equals(PNGMetadata.tEXt_creationTimeKey)) {
                        // Update Standard/Document/ImageCreationTime
                        int index = zTXt_text.size()-1;
                        decodeImageCreationTimeFromTextChunk(
                                zTXt_text.listIterator(index));
                    }
                    zTXt_node = zTXt_node.getNextSibling();
                }
            } else if (name.equals("UnknownChunks")) {
                Node unknown_node = node.getFirstChild();
                while (unknown_node != null) {
                    if (!unknown_node.getNodeName().equals("UnknownChunk")) {
                        fatal(node,
                   "Only an UnknownChunk may be a child of an UnknownChunks!");
                    }
                    String chunkType = getAttribute(unknown_node, "type");
                    Object chunkData =
                        ((IIOMetadataNode)unknown_node).getUserObject();

                    if (chunkType.length() != 4) {
                        fatal(unknown_node,
                              "Chunk type must be 4 characters!");
                    }
                    if (chunkData == null) {
                        fatal(unknown_node,
                              "No chunk data present in user object!");
                    }
                    if (!(chunkData instanceof byte[])) {
                        fatal(unknown_node,
                              "User object not a byte array!");
                    }
                    unknownChunkType.add(chunkType);
                    unknownChunkData.add(((byte[])chunkData).clone());

                    unknown_node = unknown_node.getNextSibling();
                }
            } else {
                fatal(node, "Unknown child of root node!");
            }

            node = node.getNextSibling();
        }
    }

    /*
     * Accrding to PNG spec, keywords are restricted to 1 to 79 bytes
     * in length. Keywords shall contain only printable Latin-1 characters
     * and spaces; To reduce the chances for human misreading of a keyword,
     * leading spaces, trailing spaces, and consecutive spaces are not
     * permitted in keywords.
     *
     * See: http://www.w3.org/TR/PNG/#11keywords
     */
    private boolean isValidKeyword(String s) {
        int len = s.length();
        if (len < 1 || len >= 80) {
            return false;
        }
        if (s.startsWith(" ") || s.endsWith(" ") || s.contains("  ")) {
            return false;
        }
        return isISOLatin(s, false);
    }

    /*
     * According to PNG spec, keyword shall contain only printable
     * Latin-1 [ISO-8859-1] characters and spaces; that is, only
     * character codes 32-126 and 161-255 decimal are allowed.
     * For Latin-1 value fields the 0x10 (linefeed) control
     * character is aloowed too.
     *
     * See: http://www.w3.org/TR/PNG/#11keywords
     */
    private boolean isISOLatin(String s, boolean isLineFeedAllowed) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 32 || c > 255 || (c > 126 && c < 161)) {
                // not printable. Check whether this is an allowed
                // control char
                if (!isLineFeedAllowed || c != 0x10) {
                    return false;
                }
            }
        }
        return true;
    }

    private void mergeStandardTree(Node root)
        throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName()
            .equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            fatal(node, "Root must be " +
                  IIOMetadataFormatImpl.standardMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();

            if (name.equals("Chroma")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("Gamma")) {
                        float gamma = getFloatAttribute(child, "value");
                        gAMA_present = true;
                        gAMA_gamma = (int)(gamma*100000 + 0.5);
                    } else if (childName.equals("Palette")) {
                        byte[] red = new byte[256];
                        byte[] green = new byte[256];
                        byte[] blue = new byte[256];
                        int maxindex = -1;

                        Node entry = child.getFirstChild();
                        while (entry != null) {
                            int index = getIntAttribute(entry, "index");
                            if (index >= 0 && index <= 255) {
                                red[index] =
                                    (byte)getIntAttribute(entry, "red");
                                green[index] =
                                    (byte)getIntAttribute(entry, "green");
                                blue[index] =
                                    (byte)getIntAttribute(entry, "blue");
                                if (index > maxindex) {
                                    maxindex = index;
                                }
                            }
                            entry = entry.getNextSibling();
                        }

                        int numEntries = maxindex + 1;
                        PLTE_red = new byte[numEntries];
                        PLTE_green = new byte[numEntries];
                        PLTE_blue = new byte[numEntries];
                        System.arraycopy(red, 0, PLTE_red, 0, numEntries);
                        System.arraycopy(green, 0, PLTE_green, 0, numEntries);
                        System.arraycopy(blue, 0, PLTE_blue, 0, numEntries);
                        PLTE_present = true;
                    } else if (childName.equals("BackgroundIndex")) {
                        bKGD_present = true;
                        bKGD_colorType = PNGImageReader.PNG_COLOR_PALETTE;
                        bKGD_index = getIntAttribute(child, "value");
                    } else if (childName.equals("BackgroundColor")) {
                        int red = getIntAttribute(child, "red");
                        int green = getIntAttribute(child, "green");
                        int blue = getIntAttribute(child, "blue");
                        if (red == green && red == blue) {
                            bKGD_colorType = PNGImageReader.PNG_COLOR_GRAY;
                            bKGD_gray = red;
                        } else {
                            bKGD_colorType = PNGImageReader.PNG_COLOR_RGB;
                            bKGD_red = red;
                            bKGD_green = green;
                            bKGD_blue = blue;
                        }
                        bKGD_present = true;
                    }
//                  } else if (childName.equals("ColorSpaceType")) {
//                  } else if (childName.equals("NumChannels")) {

                    child = child.getNextSibling();
                }
            } else if (name.equals("Compression")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("NumProgressiveScans")) {
                        // Use Adam7 if NumProgressiveScans > 1
                        int scans = getIntAttribute(child, "value");
                        IHDR_interlaceMethod = (scans > 1) ? 1 : 0;
//                  } else if (childName.equals("CompressionTypeName")) {
//                  } else if (childName.equals("Lossless")) {
//                  } else if (childName.equals("BitRate")) {
                    }
                    child = child.getNextSibling();
                }
            } else if (name.equals("Data")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("BitsPerSample")) {
                        String s = getAttribute(child, "value");
                        StringTokenizer t = new StringTokenizer(s);
                        int maxBits = -1;
                        while (t.hasMoreTokens()) {
                            int bits = Integer.parseInt(t.nextToken());
                            if (bits > maxBits) {
                                maxBits = bits;
                            }
                        }
                        if (maxBits < 1) {
                            maxBits = 1;
                        }
                        if (maxBits == 3) maxBits = 4;
                        if (maxBits > 4 || maxBits < 8) {
                            maxBits = 8;
                        }
                        if (maxBits > 8) {
                            maxBits = 16;
                        }
                        IHDR_bitDepth = maxBits;
                    } else if (childName.equals("SignificantBitsPerSample")) {
                        String s = getAttribute(child, "value");
                        StringTokenizer t = new StringTokenizer(s);
                        int numTokens = t.countTokens();
                        if (numTokens == 1) {
                            sBIT_colorType = PNGImageReader.PNG_COLOR_GRAY;
                            sBIT_grayBits = Integer.parseInt(t.nextToken());
                        } else if (numTokens == 2) {
                            sBIT_colorType =
                              PNGImageReader.PNG_COLOR_GRAY_ALPHA;
                            sBIT_grayBits = Integer.parseInt(t.nextToken());
                            sBIT_alphaBits = Integer.parseInt(t.nextToken());
                        } else if (numTokens == 3) {
                            sBIT_colorType = PNGImageReader.PNG_COLOR_RGB;
                            sBIT_redBits = Integer.parseInt(t.nextToken());
                            sBIT_greenBits = Integer.parseInt(t.nextToken());
                            sBIT_blueBits = Integer.parseInt(t.nextToken());
                        } else if (numTokens == 4) {
                            sBIT_colorType =
                              PNGImageReader.PNG_COLOR_RGB_ALPHA;
                            sBIT_redBits = Integer.parseInt(t.nextToken());
                            sBIT_greenBits = Integer.parseInt(t.nextToken());
                            sBIT_blueBits = Integer.parseInt(t.nextToken());
                            sBIT_alphaBits = Integer.parseInt(t.nextToken());
                        }
                        if (numTokens >= 1 && numTokens <= 4) {
                            sBIT_present = true;
                        }
//                      } else if (childName.equals("PlanarConfiguration")) {
//                      } else if (childName.equals("SampleFormat")) {
//                      } else if (childName.equals("SampleMSB")) {
                    }
                    child = child.getNextSibling();
                }
            } else if (name.equals("Dimension")) {
                boolean gotWidth = false;
                boolean gotHeight = false;
                boolean gotAspectRatio = false;

                float width = -1.0F;
                float height = -1.0F;
                float aspectRatio = -1.0F;

                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("PixelAspectRatio")) {
                        aspectRatio = getFloatAttribute(child, "value");
                        gotAspectRatio = true;
                    } else if (childName.equals("HorizontalPixelSize")) {
                        width = getFloatAttribute(child, "value");
                        gotWidth = true;
                    } else if (childName.equals("VerticalPixelSize")) {
                        height = getFloatAttribute(child, "value");
                        gotHeight = true;
//                  } else if (childName.equals("ImageOrientation")) {
//                  } else if
//                      (childName.equals("HorizontalPhysicalPixelSpacing")) {
//                  } else if
//                      (childName.equals("VerticalPhysicalPixelSpacing")) {
//                  } else if (childName.equals("HorizontalPosition")) {
//                  } else if (childName.equals("VerticalPosition")) {
//                  } else if (childName.equals("HorizontalPixelOffset")) {
//                  } else if (childName.equals("VerticalPixelOffset")) {
                    }
                    child = child.getNextSibling();
                }

                if (gotWidth && gotHeight) {
                    pHYs_present = true;
                    pHYs_unitSpecifier = 1;
                    pHYs_pixelsPerUnitXAxis = (int)(width*1000 + 0.5F);
                    pHYs_pixelsPerUnitYAxis = (int)(height*1000 + 0.5F);
                } else if (gotAspectRatio) {
                    pHYs_present = true;
                    pHYs_unitSpecifier = 0;

                    // Find a reasonable rational approximation
                    int denom = 1;
                    for (; denom < 100; denom++) {
                        int num = (int)(aspectRatio*denom);
                        if (Math.abs(num/denom - aspectRatio) < 0.001) {
                            break;
                        }
                    }
                    pHYs_pixelsPerUnitXAxis = (int)(aspectRatio*denom);
                    pHYs_pixelsPerUnitYAxis = denom;
                }
            } else if (name.equals("Document")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("ImageModificationTime")) {
                        tIME_present = true;
                        tIME_year = getIntAttribute(child, "year");
                        tIME_month = getIntAttribute(child, "month");
                        tIME_day = getIntAttribute(child, "day");
                        tIME_hour =
                            getIntAttribute(child, "hour", 0, false);
                        tIME_minute =
                            getIntAttribute(child, "minute", 0, false);
                        tIME_second =
                            getIntAttribute(child, "second", 0, false);
//                  } else if (childName.equals("SubimageInterpretation")) {
                    } else if (childName.equals("ImageCreationTime")) {
                        // Extract the creation time values
                        int year  = getIntAttribute(child, "year");
                        int month = getIntAttribute(child, "month");
                        int day   = getIntAttribute(child, "day");
                        int hour  = getIntAttribute(child, "hour", 0, false);
                        int mins  = getIntAttribute(child, "minute", 0, false);
                        int sec   = getIntAttribute(child, "second", 0, false);

                        /*
                         * Update Standard/Document/ImageCreationTime and encode
                         * the same in the last decoded text chunk with creation
                         * time
                         */
                        initImageCreationTime(year, month, day, hour, mins, sec);
                        encodeImageCreationTimeToTextChunk();
                    }
                    child = child.getNextSibling();
                }
            } else if (name.equals("Text")) {
                Node child = node.getFirstChild();
                while (child != null) {
                    String childName = child.getNodeName();
                    if (childName.equals("TextEntry")) {
                        String keyword =
                            getAttribute(child, "keyword", "", false);
                        String value = getAttribute(child, "value");
                        String language =
                            getAttribute(child, "language", "", false);
                        String compression =
                            getAttribute(child, "compression", "none", false);

                        if (!isValidKeyword(keyword)) {
                            // Just ignore this node, PNG requires keywords
                        } else if (isISOLatin(value, true)) {
                            if (compression.equals("zip")) {
                                // Use a zTXt node
                                zTXt_keyword.add(keyword);
                                zTXt_text.add(value);
                                zTXt_compressionMethod.add(Integer.valueOf(0));
                            } else {
                                // Use a tEXt node
                                tEXt_keyword.add(keyword);
                                tEXt_text.add(value);
                            }
                        } else {
                            // Use an iTXt node
                            iTXt_keyword.add(keyword);
                            iTXt_compressionFlag.add(Boolean.valueOf(compression.equals("zip")));
                            iTXt_compressionMethod.add(Integer.valueOf(0));
                            iTXt_languageTag.add(language);
                            iTXt_translatedKeyword.add(keyword); // fake it
                            iTXt_text.add(value);
                        }
                    }
                    child = child.getNextSibling();
                }
//          } else if (name.equals("Transparency")) {
//              Node child = node.getFirstChild();
//              while (child != null) {
//                  String childName = child.getNodeName();
//                  if (childName.equals("Alpha")) {
//                  } else if (childName.equals("TransparentIndex")) {
//                  } else if (childName.equals("TransparentColor")) {
//                  } else if (childName.equals("TileTransparencies")) {
//                  } else if (childName.equals("TileOpacities")) {
//                  }
//                  child = child.getNextSibling();
//              }
//          } else {
//              // fatal(node, "Unknown child of root node!");
            }

            node = node.getNextSibling();
        }
    }

    void initImageCreationTime(OffsetDateTime offsetDateTime) {
        // Check for incoming arguments
        if (offsetDateTime != null) {
            // set values that make up Standard/Document/ImageCreationTime
            creation_time_present = true;
            creation_time_year    = offsetDateTime.getYear();
            creation_time_month   = offsetDateTime.getMonthValue();
            creation_time_day     = offsetDateTime.getDayOfMonth();
            creation_time_hour    = offsetDateTime.getHour();
            creation_time_minute  = offsetDateTime.getMinute();
            creation_time_second  = offsetDateTime.getSecond();
            creation_time_offset  = offsetDateTime.getOffset();
        }
    }

    void initImageCreationTime(int year, int month, int day,
            int hour, int min,int second) {
        /*
         * Though LocalDateTime suffices the need to store Standard/Document/
         * ImageCreationTime, we require the zone offset to encode the same
         * in the text chunk based on RFC1123 format.
         */
        LocalDateTime locDT = LocalDateTime.of(year, month, day, hour, min, second);
        ZoneOffset offset = ZoneId.systemDefault()
                                  .getRules()
                                  .getOffset(locDT);
        OffsetDateTime offDateTime = OffsetDateTime.of(locDT,offset);
        initImageCreationTime(offDateTime);
    }

    void decodeImageCreationTimeFromTextChunk(ListIterator<String> iterChunk) {
        // Check for incoming arguments
        if (iterChunk != null && iterChunk.hasNext()) {
            /*
             * Save the iterator to mark the last decoded text chunk with
             * creation time. The contents of this chunk will be updated when
             * user provides creation time by merging a standard tree with
             * Standard/Document/ImageCreationTime.
             */
            setCreationTimeChunk(iterChunk);

            // Parse encoded time and set Standard/Document/ImageCreationTime.
            String encodedTime = getEncodedTime();
            initImageCreationTime(parseEncodedTime(encodedTime));
        }
    }

    void encodeImageCreationTimeToTextChunk() {
        // Check if Standard/Document/ImageCreationTime exists.
        if (creation_time_present) {
            // Check if a text chunk with creation time exists.
            if (tEXt_creation_time_present == false) {
                // No text chunk exists with image creation time. Add an entry.
                this.tEXt_keyword.add(tEXt_creationTimeKey);
                this.tEXt_text.add("Creation Time Place Holder");

                // Update the iterator
                int index = tEXt_text.size() - 1;
                setCreationTimeChunk(tEXt_text.listIterator(index));
            }

            // Encode image creation time with RFC1123 formatter
            OffsetDateTime offDateTime = OffsetDateTime.of(creation_time_year,
                    creation_time_month, creation_time_day,
                    creation_time_hour, creation_time_minute,
                    creation_time_second, 0, creation_time_offset);
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            String encodedTime = offDateTime.format(formatter);
            setEncodedTime(encodedTime);
        }
    }

    private void setCreationTimeChunk(ListIterator<String> iter) {
        // Check for iterator's valid state
        if (iter != null && iter.hasNext()) {
            tEXt_creation_time_iter = iter;
            tEXt_creation_time_present = true;
        }
    }

    private void setEncodedTime(String encodedTime) {
        if (tEXt_creation_time_iter != null
                && tEXt_creation_time_iter.hasNext()
                && encodedTime != null) {
            // Set the value at the iterator and reset its state
            tEXt_creation_time_iter.next();
            tEXt_creation_time_iter.set(encodedTime);
            tEXt_creation_time_iter.previous();
        }
    }

    private String getEncodedTime() {
        String encodedTime = null;
        if (tEXt_creation_time_iter != null
                && tEXt_creation_time_iter.hasNext()) {
            // Get the value at iterator and reset its state
            encodedTime = tEXt_creation_time_iter.next();
            tEXt_creation_time_iter.previous();
        }
        return encodedTime;
    }

    private OffsetDateTime parseEncodedTime(String encodedTime) {
        OffsetDateTime retVal = null;
        boolean timeDecoded = false;

        /*
         * PNG specification recommends that image encoders use RFC1123 format
         * to represent time in String but doesn't mandate. Encoders could
         * use any convenient format. Hence, we extract time provided the
         * encoded time complies with either RFC1123 or ISO standards.
         */
        try {
            // Check if the encoded time complies with RFC1123
            retVal = OffsetDateTime.parse(encodedTime,
                                          DateTimeFormatter.RFC_1123_DATE_TIME);
            timeDecoded = true;
        } catch (DateTimeParseException exception) {
            // No Op. Encoded time did not comply with RFC1123 standard.
        }

        if (timeDecoded == false) {
            try {
                // Check if the encoded time complies with ISO standard.
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                TemporalAccessor dt = formatter.parseBest(encodedTime,
                        OffsetDateTime::from, LocalDateTime::from);

                if (dt instanceof OffsetDateTime) {
                    // Encoded time contains date time and zone offset
                    retVal = (OffsetDateTime) dt;
                } else if (dt instanceof LocalDateTime) {
                    /*
                     * Encoded time contains only date and time. Since zone
                     * offset information isn't available, we set to the default
                     */
                    LocalDateTime locDT = (LocalDateTime) dt;
                    retVal = OffsetDateTime.of(locDT, ZoneOffset.UTC);
                }
            }  catch (DateTimeParseException exception) {
                // No Op. Encoded time did not comply with ISO standard.
            }
        }
        return retVal;
    }

    boolean hasTransparentColor() {
        return tRNS_present &&
               (tRNS_colorType == PNGImageReader.PNG_COLOR_RGB ||
               tRNS_colorType == PNGImageReader.PNG_COLOR_GRAY);
    }

    // Reset all instance variables to their initial state
    public void reset() {
        IHDR_present = false;
        PLTE_present = false;
        bKGD_present = false;
        cHRM_present = false;
        gAMA_present = false;
        hIST_present = false;
        iCCP_present = false;
        iTXt_keyword = new ArrayList<String>();
        iTXt_compressionFlag = new ArrayList<Boolean>();
        iTXt_compressionMethod = new ArrayList<Integer>();
        iTXt_languageTag = new ArrayList<String>();
        iTXt_translatedKeyword = new ArrayList<String>();
        iTXt_text = new ArrayList<String>();
        pHYs_present = false;
        sBIT_present = false;
        sPLT_present = false;
        sRGB_present = false;
        tEXt_keyword = new ArrayList<String>();
        tEXt_text = new ArrayList<String>();
        // tIME chunk with Image modification time
        tIME_present = false;
        // Text chunk with Image creation time
        tEXt_creation_time_present = false;
        tEXt_creation_time_iter = null;
        creation_time_present = false;
        tRNS_present = false;
        zTXt_keyword = new ArrayList<String>();
        zTXt_compressionMethod = new ArrayList<Integer>();
        zTXt_text = new ArrayList<String>();
        unknownChunkType = new ArrayList<String>();
        unknownChunkData = new ArrayList<byte[]>();
    }
}
