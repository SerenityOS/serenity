/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * A class representing the set of tags found in the baseline TIFF
 * specification as well as some common additional tags.
 *
 * <p> The non-baseline tags included in this class are:
 * <ul>
 * <li> {@link #TAG_JPEG_TABLES JPEGTables}
 * <li> {@link #TAG_ICC_PROFILE ICC&nbsp;Profile}
 * </ul>
 *
 * <p> The non-baseline values of baseline tags included in this class are
 * <ul>
 * <li>{@link #TAG_COMPRESSION Compression} tag values:
 * <ul>
 * <li>{@link #COMPRESSION_JPEG JPEG-in-TIFF&nbsp;compression}</li>
 * <li>{@link #COMPRESSION_ZLIB Zlib-in-TIFF&nbsp;compression}</li>
 * <li>{@link #COMPRESSION_DEFLATE Deflate&nbsp;compression}</li>
 * </ul>
 * </li>
 * <li>{@link #TAG_PHOTOMETRIC_INTERPRETATION PhotometricInterpretation}
 * tag values:
 * <ul>
 * <li>{@link #PHOTOMETRIC_INTERPRETATION_ICCLAB ICCLAB&nbsp;
 * photometric&nbsp;interpretation}</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @since 9
 * @see   <a href="https://www.itu.int/itudoc/itu-t/com16/tiff-fx/docs/tiff6.pdf">  TIFF 6.0 Specification</a>
 */
public final class BaselineTIFFTagSet extends TIFFTagSet {

    private static BaselineTIFFTagSet theInstance = null;

    // Tags from TIFF 6.0 specification

    /**
     * Constant specifying the "NewSubfileType" tag.
     *
     * @see #NEW_SUBFILE_TYPE_REDUCED_RESOLUTION
     * @see #NEW_SUBFILE_TYPE_SINGLE_PAGE
     * @see #NEW_SUBFILE_TYPE_TRANSPARENCY
     */
    public static final int TAG_NEW_SUBFILE_TYPE = 254;

    /**
     * A mask to be used with the "NewSubfileType" tag.
     *
     * @see #TAG_NEW_SUBFILE_TYPE
     */
    public static final int NEW_SUBFILE_TYPE_REDUCED_RESOLUTION = 1;

    /**
     * A mask to be used with the "NewSubfileType" tag.
     *
     * @see #TAG_NEW_SUBFILE_TYPE
     */
    public static final int NEW_SUBFILE_TYPE_SINGLE_PAGE = 2;

    /**
     * A mask to be used with the "NewSubfileType" tag.
     *
     * @see #TAG_NEW_SUBFILE_TYPE
     */
    public static final int NEW_SUBFILE_TYPE_TRANSPARENCY = 4;

    /**
     * Constant specifying the "SubfileType" tag.
     *
     * @see #SUBFILE_TYPE_FULL_RESOLUTION
     * @see #SUBFILE_TYPE_REDUCED_RESOLUTION
     * @see #SUBFILE_TYPE_SINGLE_PAGE
     */
    public static final int TAG_SUBFILE_TYPE = 255;

    /**
     * A value to be used with the "SubfileType" tag.
     *
     * @see #TAG_SUBFILE_TYPE
     */
    public static final int SUBFILE_TYPE_FULL_RESOLUTION = 1;

    /**
     * A value to be used with the "SubfileType" tag.
     *
     * @see #TAG_SUBFILE_TYPE
     */
    public static final int SUBFILE_TYPE_REDUCED_RESOLUTION = 2;

    /**
     * A value to be used with the "SubfileType" tag.
     *
     * @see #TAG_SUBFILE_TYPE
     */
    public static final int SUBFILE_TYPE_SINGLE_PAGE = 3;

    /**
     * Constant specifying the "ImageWidth" tag.
     */
    public static final int TAG_IMAGE_WIDTH = 256;

    /**
     * Constant specifying the "ImageLength" tag.
     */
    public static final int TAG_IMAGE_LENGTH = 257;

    /**
     * Constant specifying the "BitsPerSample" tag.
     */
    public static final int TAG_BITS_PER_SAMPLE = 258;

    /**
     * Constant specifying the "Compression" tag.
     *
     * @see #COMPRESSION_NONE
     * @see #COMPRESSION_CCITT_RLE
     * @see #COMPRESSION_CCITT_T_4
     * @see #COMPRESSION_CCITT_T_6
     * @see #COMPRESSION_LZW
     * @see #COMPRESSION_OLD_JPEG
     * @see #COMPRESSION_JPEG
     * @see #COMPRESSION_ZLIB
     * @see #COMPRESSION_PACKBITS
     * @see #COMPRESSION_DEFLATE
     */
    public static final int TAG_COMPRESSION = 259;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_NONE = 1;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_CCITT_RLE = 2;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_CCITT_T_4 = 3;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_CCITT_T_6 = 4;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_LZW = 5;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_OLD_JPEG = 6;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_JPEG = 7;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_ZLIB = 8;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     */
    public static final int COMPRESSION_PACKBITS = 32773;

    /**
     * A value to be used with the "Compression" tag.
     *
     * @see #TAG_COMPRESSION
     * @see <a href="https://tools.ietf.org/html/rfc1951">DEFLATE specification</a>
     */
    public static final int COMPRESSION_DEFLATE = 32946;

    /**
     * Constant specifying the "PhotometricInterpretation" tag.
     *
     * @see #PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO
     * @see #PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO
     * @see #PHOTOMETRIC_INTERPRETATION_RGB
     * @see #PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR
     * @see #PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK
     * @see #PHOTOMETRIC_INTERPRETATION_Y_CB_CR
     * @see #PHOTOMETRIC_INTERPRETATION_CIELAB
     * @see #PHOTOMETRIC_INTERPRETATION_ICCLAB
     */
    public static final int TAG_PHOTOMETRIC_INTERPRETATION = 262;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO = 0;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO = 1;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_RGB = 2;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR = 3;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK = 4;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_CMYK = 5;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_Y_CB_CR = 6;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_CIELAB = 8;

    /**
     * A value to be used with the "PhotometricInterpretation" tag.
     *
     * @see #TAG_PHOTOMETRIC_INTERPRETATION
     */
    public static final int PHOTOMETRIC_INTERPRETATION_ICCLAB = 9;

    /**
     * Constant specifying the "Threshholding" tag.
     *
     * @see #THRESHHOLDING_NONE
     * @see #THRESHHOLDING_ORDERED_DITHER
     * @see #THRESHHOLDING_RANDOMIZED_DITHER
     */
    public static final int TAG_THRESHHOLDING = 263;

    /**
     * A value to be used with the "Thresholding" tag.
     *
     * @see #TAG_THRESHHOLDING
     */
    public static final int THRESHHOLDING_NONE = 1;

    /**
     * A value to be used with the "Thresholding" tag.
     *
     * @see #TAG_THRESHHOLDING
     */
    public static final int THRESHHOLDING_ORDERED_DITHER = 2;

    /**
     * A value to be used with the "Thresholding" tag.
     *
     * @see #TAG_THRESHHOLDING
     */
    public static final int THRESHHOLDING_RANDOMIZED_DITHER = 3;

    /**
     * Constant specifying the "Cell_Width" tag.
     */
    public static final int TAG_CELL_WIDTH = 264;

    /**
     * Constant specifying the "cell_length" tag.
     */
    public static final int TAG_CELL_LENGTH = 265;

    /**
     * Constant specifying the "fill_order" tag.
     *
     * @see #FILL_ORDER_LEFT_TO_RIGHT
     * @see #FILL_ORDER_RIGHT_TO_LEFT
     */
    public static final int TAG_FILL_ORDER = 266;

    /**
     * A value to be used with the "FillOrder" tag.
     *
     * @see #TAG_FILL_ORDER
     */
    public static final int FILL_ORDER_LEFT_TO_RIGHT = 1;

    /**
     * A value to be used with the "FillOrder" tag.
     *
     * @see #TAG_FILL_ORDER
     */
    public static final int FILL_ORDER_RIGHT_TO_LEFT = 2;

    /**
     * Constant specifying the "document_name" tag.
     */
    public static final int TAG_DOCUMENT_NAME = 269;

    /**
     * Constant specifying the "Image_description" tag.
     */
    public static final int TAG_IMAGE_DESCRIPTION = 270;

    /**
     * Constant specifying the "Make" tag.
     */
    public static final int TAG_MAKE = 271;

    /**
     * Constant specifying the "Model" tag.
     */
    public static final int TAG_MODEL = 272;

    /**
     * Constant specifying the "Strip_offsets" tag.
     */
    public static final int TAG_STRIP_OFFSETS = 273;

    /**
     * Constant specifying the "Orientation" tag.
     *
     * @see #ORIENTATION_ROW_0_TOP_COLUMN_0_LEFT
     * @see #ORIENTATION_ROW_0_TOP_COLUMN_0_RIGHT
     * @see #ORIENTATION_ROW_0_BOTTOM_COLUMN_0_RIGHT
     * @see #ORIENTATION_ROW_0_BOTTOM_COLUMN_0_LEFT
     * @see #ORIENTATION_ROW_0_LEFT_COLUMN_0_TOP
     * @see #ORIENTATION_ROW_0_RIGHT_COLUMN_0_TOP
     * @see #ORIENTATION_ROW_0_RIGHT_COLUMN_0_BOTTOM
     * @see #ORIENTATION_ROW_0_LEFT_COLUMN_0_BOTTOM
     */
    public static final int TAG_ORIENTATION = 274;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_LEFT = 1;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_RIGHT = 2;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_RIGHT = 3;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_LEFT = 4;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_TOP = 5;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_TOP = 6;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_BOTTOM = 7;

    /**
     * A value to be used with the "Orientation" tag.
     *
     * @see #TAG_ORIENTATION
     */
    public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_BOTTOM = 8;

    /**
     * Constant specifying the "Samples_per_pixel" tag.
     */
    public static final int TAG_SAMPLES_PER_PIXEL = 277;

    /**
     * Constant specifying the "Rows_per_strip" tag.
     */
    public static final int TAG_ROWS_PER_STRIP = 278;

    /**
     * Constant specifying the "Strip_byte_counts" tag.
     */
    public static final int TAG_STRIP_BYTE_COUNTS = 279;

    /**
     * Constant specifying the "Min_sample_value" tag.
     */
    public static final int TAG_MIN_SAMPLE_VALUE = 280;

    /**
     * Constant specifying the "Max_sample_value" tag.
     */
    public static final int TAG_MAX_SAMPLE_VALUE = 281;

    /**
     * Constant specifying the "XResolution" tag.
     */
    public static final int TAG_X_RESOLUTION = 282;

    /**
     * Constant specifying the "YResolution" tag.
     */
    public static final int TAG_Y_RESOLUTION = 283;

    /**
     * Constant specifying the "PlanarConfiguration" tag.
     *
     * @see #PLANAR_CONFIGURATION_CHUNKY
     * @see #PLANAR_CONFIGURATION_PLANAR
     */
    public static final int TAG_PLANAR_CONFIGURATION = 284;

    /**
     * A value to be used with the "PlanarConfiguration" tag.
     *
     * @see #TAG_PLANAR_CONFIGURATION
     */
    public static final int PLANAR_CONFIGURATION_CHUNKY = 1;

    /**
     * A value to be used with the "PlanarConfiguration" tag.
     *
     * @see #TAG_PLANAR_CONFIGURATION
     */
    public static final int PLANAR_CONFIGURATION_PLANAR = 2;

    /**
     * Constant specifying the "PageName" tag.
     */
    public static final int TAG_PAGE_NAME = 285;

    /**
     * Constant specifying the "XPosition" tag.
     */
    public static final int TAG_X_POSITION = 286;

    /**
     * Constant specifying the "YPosition" tag.
     */
    public static final int TAG_Y_POSITION = 287;

    /**
     * Constant specifying the "FreeOffsets" tag.
     */
    public static final int TAG_FREE_OFFSETS = 288;

    /**
     * Constant specifying the "FreeByteCounts" tag.
     */
    public static final int TAG_FREE_BYTE_COUNTS = 289;

    /**
     * Constant specifying the "GrayResponseUnit" tag.
     *
     * @see #GRAY_RESPONSE_UNIT_TENTHS
     * @see #GRAY_RESPONSE_UNIT_HUNDREDTHS
     * @see #GRAY_RESPONSE_UNIT_THOUSANDTHS
     * @see #GRAY_RESPONSE_UNIT_TEN_THOUSANDTHS
     * @see #GRAY_RESPONSE_UNIT_HUNDRED_THOUSANDTHS
     */
    public static final int TAG_GRAY_RESPONSE_UNIT = 290;

    /**
     * A value to be used with the "GrayResponseUnit" tag.
     *
     * @see #TAG_GRAY_RESPONSE_UNIT
     */
    public static final int GRAY_RESPONSE_UNIT_TENTHS = 1;

    /**
     * A value to be used with the "GrayResponseUnit" tag.
     *
     * @see #TAG_GRAY_RESPONSE_UNIT
     */
    public static final int GRAY_RESPONSE_UNIT_HUNDREDTHS = 2;

    /**
     * A value to be used with the "GrayResponseUnit" tag.
     *
     * @see #TAG_GRAY_RESPONSE_UNIT
     */
    public static final int GRAY_RESPONSE_UNIT_THOUSANDTHS = 3;

    /**
     * A value to be used with the "GrayResponseUnit" tag.
     *
     * @see #TAG_GRAY_RESPONSE_UNIT
     */
    public static final int GRAY_RESPONSE_UNIT_TEN_THOUSANDTHS = 4;

    /**
     * A value to be used with the "GrayResponseUnit" tag.
     *
     * @see #TAG_GRAY_RESPONSE_UNIT
     */
    public static final int GRAY_RESPONSE_UNIT_HUNDRED_THOUSANDTHS = 5;

    /**
     * Constant specifying the "GrayResponseCurve" tag.
     */
    public static final int TAG_GRAY_RESPONSE_CURVE = 291;

    /**
     * Constant specifying the "T4Options" tag.
     *
     * @see #T4_OPTIONS_2D_CODING
     * @see #T4_OPTIONS_UNCOMPRESSED
     * @see #T4_OPTIONS_EOL_BYTE_ALIGNED
     */
    public static final int TAG_T4_OPTIONS = 292;

    /**
     * A mask to be used with the "T4Options" tag.
     *
     * @see #TAG_T4_OPTIONS
     */
    public static final int T4_OPTIONS_2D_CODING = 1;

    /**
     * A mask to be used with the "T4Options" tag.
     *
     * @see #TAG_T4_OPTIONS
     */
    public static final int T4_OPTIONS_UNCOMPRESSED = 2;

    /**
     * A mask to be used with the "T4Options" tag.
     *
     * @see #TAG_T4_OPTIONS
     */
    public static final int T4_OPTIONS_EOL_BYTE_ALIGNED = 4;

    /**
     * Constant specifying the "T6Options" tag.
     *
     * @see #T6_OPTIONS_UNCOMPRESSED
     */
    public static final int TAG_T6_OPTIONS = 293;

    /**
     * A mask to be used with the "T6Options" tag.
     *
     * @see #TAG_T6_OPTIONS
     */
    public static final int T6_OPTIONS_UNCOMPRESSED = 2;

    /**
     * Constant specifying the "ResolutionUnit" tag.
     *
     * @see #RESOLUTION_UNIT_NONE
     * @see #RESOLUTION_UNIT_INCH
     * @see #RESOLUTION_UNIT_CENTIMETER
     */
    public static final int TAG_RESOLUTION_UNIT = 296;

    /**
     * A value to be used with the "ResolutionUnit" tag.
     *
     * @see #TAG_RESOLUTION_UNIT
     */
    public static final int RESOLUTION_UNIT_NONE = 1;

    /**
     * A value to be used with the "ResolutionUnit" tag.
     *
     * @see #TAG_RESOLUTION_UNIT
     */
    public static final int RESOLUTION_UNIT_INCH = 2;

    /**
     * A value to be used with the "ResolutionUnit" tag.
     *
     * @see #TAG_RESOLUTION_UNIT
     */
    public static final int RESOLUTION_UNIT_CENTIMETER = 3;


    /**
     * Constant specifying the "PageNumber" tag.
     */
    public static final int TAG_PAGE_NUMBER = 297;

    /**
     * Constant specifying the "TransferFunction" tag.
     */
    public static final int TAG_TRANSFER_FUNCTION = 301;

    /**
     * Constant specifying the "Software" tag.
     */
    public static final int TAG_SOFTWARE = 305;

    /**
     * Constant specifying the "DateTime" tag.
     */
    public static final int TAG_DATE_TIME = 306;

    /**
     * Constant specifying the "Artist" tag.
     */
    public static final int TAG_ARTIST = 315;

    /**
     * Constant specifying the "HostComputer" tag.
     */
    public static final int TAG_HOST_COMPUTER = 316;

    /**
     * Constant specifying the "Predictor" tag.
     *
     * @see #TAG_WHITE_POINT
     * @see #TAG_PRIMARY_CHROMATICITES
     * @see #TAG_COLOR_MAP
     * @see #TAG_HALFTONE_HINTS
     * @see #TAG_TILE_WIDTH
     * @see #TAG_TILE_LENGTH
     * @see #TAG_TILE_OFFSETS
     * @see #TAG_TILE_BYTE_COUNTS
     */
    public static final int TAG_PREDICTOR = 317;

    /**
     * A value to be used with the "Predictor" tag.
     *
     * @see #TAG_PREDICTOR
     */
    public static final int PREDICTOR_NONE = 1;

    /**
     * A value to be used with the "Predictor" tag.
     *
     * @see #TAG_PREDICTOR
     */
    public static final int PREDICTOR_HORIZONTAL_DIFFERENCING = 2;

    /**
     * Constant specifying the "WhitePoint" tag.
     */
    public static final int TAG_WHITE_POINT = 318;

    /**
     * Constant specifying the "PrimaryChromaticites" tag.
     */
    public static final int TAG_PRIMARY_CHROMATICITES = 319;

    /**
     * Constant specifying the "ColorMap" tag.
     */
    public static final int TAG_COLOR_MAP = 320;

    /**
     * Constant specifying the "HalftoneHints" tag.
     */
    public static final int TAG_HALFTONE_HINTS = 321;

    /**
     * Constant specifying the "TileWidth" tag.
     */
    public static final int TAG_TILE_WIDTH = 322;

    /**
     * Constant specifying the "TileLength" tag.
     */
    public static final int TAG_TILE_LENGTH = 323;

    /**
     * Constant specifying the "TileOffsets" tag.
     */
    public static final int TAG_TILE_OFFSETS = 324;

    /**
     * Constant specifying the "TileByteCounts" tag.
     */
    public static final int TAG_TILE_BYTE_COUNTS = 325;

    /**
     * Constant specifying the "InkSet" tag.
     *
     * @see #INK_SET_CMYK
     * @see #INK_SET_NOT_CMYK
     */
    public static final int TAG_INK_SET = 332;

    /**
     * A value to be used with the "InkSet" tag.
     *
     * @see #TAG_INK_SET
     */
    public static final int INK_SET_CMYK = 1;

    /**
     * A value to be used with the "InkSet" tag.
     *
     * @see #TAG_INK_SET
     */
    public static final int INK_SET_NOT_CMYK = 2;

    /**
     * Constant specifying the "InkNames" tag.
     */
    public static final int TAG_INK_NAMES = 333;

    /**
     * Constant specifying the "NumberOfInks" tag.
     */
    public static final int TAG_NUMBER_OF_INKS = 334;

    /**
     * Constant specifying the "DotRange" tag.
     */
    public static final int TAG_DOT_RANGE = 336;

    /**
     * Constant specifying the "TargetPrinter" tag.
     */
    public static final int TAG_TARGET_PRINTER = 337;

    /**
     * Constant specifying the "ExtraSamples" tag.
     *
     * @see #EXTRA_SAMPLES_UNSPECIFIED
     * @see #EXTRA_SAMPLES_ASSOCIATED_ALPHA
     * @see #EXTRA_SAMPLES_UNASSOCIATED_ALPHA
     */
    public static final int TAG_EXTRA_SAMPLES = 338;

    /**
     * A value to be used with the "ExtraSamples" tag.
     *
     * @see #TAG_EXTRA_SAMPLES
     */
    public static final int EXTRA_SAMPLES_UNSPECIFIED = 0;

    /**
     * A value to be used with the "ExtraSamples" tag.
     *
     * @see #TAG_EXTRA_SAMPLES
     */
    public static final int EXTRA_SAMPLES_ASSOCIATED_ALPHA = 1;

    /**
     * A value to be used with the "ExtraSamples" tag.
     *
     * @see #TAG_EXTRA_SAMPLES
     */
    public static final int EXTRA_SAMPLES_UNASSOCIATED_ALPHA = 2;

    /**
     * Constant specifying the "SampleFormat" tag.
     *
     * @see #SAMPLE_FORMAT_UNSIGNED_INTEGER
     * @see #SAMPLE_FORMAT_SIGNED_INTEGER
     * @see #SAMPLE_FORMAT_FLOATING_POINT
     * @see #SAMPLE_FORMAT_UNDEFINED
     */
    public static final int TAG_SAMPLE_FORMAT = 339;

    /**
     * A value to be used with the "SampleFormat" tag.
     *
     * @see #TAG_SAMPLE_FORMAT
     */
    public static final int SAMPLE_FORMAT_UNSIGNED_INTEGER = 1;

    /**
     * A value to be used with the "SampleFormat" tag.
     *
     * @see #TAG_SAMPLE_FORMAT
     */
    public static final int SAMPLE_FORMAT_SIGNED_INTEGER = 2;

    /**
     * A value to be used with the "SampleFormat" tag.
     *
     * @see #TAG_SAMPLE_FORMAT
     */
    public static final int SAMPLE_FORMAT_FLOATING_POINT = 3;

    /**
     * A value to be used with the "SampleFormat" tag.
     *
     * @see #TAG_SAMPLE_FORMAT
     */
    public static final int SAMPLE_FORMAT_UNDEFINED = 4;

    /**
     * Constant specifying the "SMinSampleValue" tag.
     */
    public static final int TAG_S_MIN_SAMPLE_VALUE = 340;

    /**
     * Constant specifying the "SMaxSampleValue" tag.
     */
    public static final int TAG_S_MAX_SAMPLE_VALUE = 341;

    /**
     * Constant specifying the "TransferRange" tag.
     */
    public static final int TAG_TRANSFER_RANGE = 342;

    /**
     * Constant specifying the "JPEGTables" tag for
     * "New style" JPEG-in-TIFF compression.
     */
    public static final int TAG_JPEG_TABLES = 347;

    /**
     * Constant specifying the "JPEGProc" tag.
     */
    public static final int TAG_JPEG_PROC = 512;

    /**
     * A value to be used with the "JPEGProc" tag.
     *
     * @see #TAG_JPEG_PROC
     */
    public static final int JPEG_PROC_BASELINE = 1;

    /**
     * A value to be used with the "JPEGProc" tag.
     *
     * @see #TAG_JPEG_PROC
     */
    public static final int JPEG_PROC_LOSSLESS = 14;

    /**
     * Constant specifying the "JPEGInterchangeFormat" tag.
     */
    public static final int TAG_JPEG_INTERCHANGE_FORMAT = 513;

    /**
     * Constant specifying the "JPEGInterchangeFormatLength" tag.
     */
    public static final int TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = 514;

    /**
     * Constant specifying the "JPEGRestartInterval" tag.
     */
    public static final int TAG_JPEG_RESTART_INTERVAL = 515;

    /**
     * Constant specifying the "JPEGLosslessPredictors" tag.
     */
    public static final int TAG_JPEG_LOSSLESS_PREDICTORS = 517;

    /**
     * Constant specifying the "JPEGPointTransforms" tag.
     */
    public static final int TAG_JPEG_POINT_TRANSFORMS = 518;

    /**
     * Constant specifying the "JPEGQTables" tag.
     */
    public static final int TAG_JPEG_Q_TABLES = 519;

    /**
     * Constant specifying the "JPEGDCTables" tag.
     */
    public static final int TAG_JPEG_DC_TABLES = 520;

    /**
     * Constant specifying the "JPEGACTables" tag.
     */
    public static final int TAG_JPEG_AC_TABLES = 521;

    /**
     * Constant specifying the "YCbCrCoefficients" tag.
     */
    public static final int TAG_Y_CB_CR_COEFFICIENTS = 529;

    /**
     * Constant specifying the "YCbCrSubsampling" tag.
     */
    public static final int TAG_Y_CB_CR_SUBSAMPLING = 530;

    /**
     * Constant specifying the "YCbCrPositioning" tag.
     *
     * @see #Y_CB_CR_POSITIONING_CENTERED
     * @see #Y_CB_CR_POSITIONING_COSITED
     */
    public static final int TAG_Y_CB_CR_POSITIONING = 531;

    /**
     * A value to be used with the "YCbCrPositioning" tag.
     *
     * @see #TAG_Y_CB_CR_POSITIONING
     */
    public static final int Y_CB_CR_POSITIONING_CENTERED = 1;

    /**
     * A value to be used with the "YCbCrPositioning" tag.
     *
     * @see #TAG_Y_CB_CR_POSITIONING
     */
    public static final int Y_CB_CR_POSITIONING_COSITED = 2;

    /**
     * Constant specifying the "ReferenceBlackWhite" tag.
     */
    public static final int TAG_REFERENCE_BLACK_WHITE = 532;

    /**
     * Constant specifying the "Copyright" tag.
     */
    public static final int TAG_COPYRIGHT = 33432;

    // Common non-baseline tags

    // ICC profiles (Spec ICC 1:2001-04, Appendix B)

    // 34675 - Embedded ICC Profile               (UNDEFINED/any)

    /**
     * Constant specifying the "ICC Profile" tag.
     *
     * @see <a href="http://www.color.org/ICC1V42.pdf">ICC Specification, section B.4: Embedding ICC profiles in TIFF files</a>
     */
    public static final int TAG_ICC_PROFILE = 34675;

    // Artist

    static class Artist extends TIFFTag {

        public Artist() {
            super("Artist",
                  TAG_ARTIST,
                  1 << TIFF_ASCII);
        }
    }

    // BitsPerSample

    static class BitsPerSample extends TIFFTag {

        public BitsPerSample() {
            super("BitsPerSample",
                  TAG_BITS_PER_SAMPLE,
                  1 << TIFF_SHORT);
        }
    }

    // CellLength

    static class CellLength extends TIFFTag {

        public CellLength() {
            super("CellLength",
                  TAG_CELL_LENGTH,
                  1 << TIFF_SHORT,
                  1);
        }
    }

    // CellWidth tag

    static class CellWidth extends TIFFTag {

        public CellWidth() {
            super("CellWidth",
                  TAG_CELL_WIDTH,
                  1 << TIFF_SHORT,
                  1);
        }
    }

    // ColorMap

    static class ColorMap extends TIFFTag {

        public ColorMap() {
            super("ColorMap",
                  TAG_COLOR_MAP,
                  1 << TIFF_SHORT);
        }
    }

    // Compression

    static class Compression extends TIFFTag {

        public Compression() {
            super("Compression",
                  TAG_COMPRESSION,
                  1 << TIFF_SHORT,
                  1);

            addValueName(COMPRESSION_NONE, "Uncompressed");
            addValueName(COMPRESSION_CCITT_RLE, "CCITT RLE");
            addValueName(COMPRESSION_CCITT_T_4, "CCITT T.4");
            addValueName(COMPRESSION_CCITT_T_6, "CCITT T.6");
            addValueName(COMPRESSION_LZW, "LZW");
            addValueName(COMPRESSION_OLD_JPEG, "Old JPEG");
            addValueName(COMPRESSION_JPEG, "JPEG");
            addValueName(COMPRESSION_ZLIB, "ZLib");
            addValueName(COMPRESSION_PACKBITS, "PackBits");
            addValueName(COMPRESSION_DEFLATE, "Deflate"); // Non-baseline

            // 32771 CCITT
            // 32809 ThunderScan
            // 32766 NeXT
            // 32909 Pixar
            // 34676 SGI
            // 34677 SGI
        }
    }

    // Copyright

    static class Copyright extends TIFFTag {

        public Copyright() {
            super("Copyright",
                  TAG_COPYRIGHT,
                  1 << TIFF_ASCII);
        }
    }

    // DateTime

    static class DateTime extends TIFFTag {

        public DateTime() {
            super("DateTime",
                  TAG_DATE_TIME,
                  1 << TIFF_ASCII,
                  20);
        }
    }

    // DocumentName

    static class DocumentName extends TIFFTag {

        public DocumentName() {
            super("DocumentName",
                  TAG_DOCUMENT_NAME,
                  1 << TIFF_ASCII);
        }
    }

    // DotRange

    static class DotRange extends TIFFTag {

        public DotRange() {
            super("DotRange",
                  TAG_DOT_RANGE,
                  (1 << TIFF_BYTE) |
                  (1 << TIFF_SHORT));
        }
    }

    // ExtraSamples

    static class ExtraSamples extends TIFFTag {

        public ExtraSamples() {
            super("ExtraSamples",
                  TAG_EXTRA_SAMPLES,
                  1 << TIFF_SHORT);

            addValueName(EXTRA_SAMPLES_UNSPECIFIED,
                         "Unspecified");
            addValueName(EXTRA_SAMPLES_ASSOCIATED_ALPHA,
                         "Associated Alpha");
            addValueName(EXTRA_SAMPLES_UNASSOCIATED_ALPHA,
                         "Unassociated Alpha");
        }
    }

    // FillOrder

    static class FillOrder extends TIFFTag {

        public FillOrder() {
            super("FillOrder",
                  TAG_FILL_ORDER,
                  1 << TIFF_SHORT,
                  1);

            addValueName(FILL_ORDER_LEFT_TO_RIGHT, "LeftToRight");
            addValueName(FILL_ORDER_RIGHT_TO_LEFT, "RightToLeft");
        }
    }

    // FreeByteCounts

    static class FreeByteCounts extends TIFFTag {

        public FreeByteCounts() {
            super("FreeByteCounts",
                  TAG_FREE_BYTE_COUNTS,
                  1 << TIFF_LONG);
        }
    }

    // FreeOffsets

    static class FreeOffsets extends TIFFTag {

        public FreeOffsets() {
            super("FreeOffsets",
                  TAG_FREE_OFFSETS,
                  1 << TIFF_LONG);
        }
    }

    // GrayResponseCurve

    static class GrayResponseCurve extends TIFFTag {

        public GrayResponseCurve() {
            super("GrayResponseCurve",
                  TAG_GRAY_RESPONSE_CURVE,
                  1 << TIFF_SHORT);
        }
    }

    // GrayResponseUnit

    static class GrayResponseUnit extends TIFFTag {

        public GrayResponseUnit() {
            super("GrayResponseUnit",
                  TAG_GRAY_RESPONSE_UNIT,
                  1 << TIFF_SHORT,
                  1);

            addValueName(GRAY_RESPONSE_UNIT_TENTHS,
                         "Tenths");
            addValueName(GRAY_RESPONSE_UNIT_HUNDREDTHS,
                         "Hundredths");
            addValueName(GRAY_RESPONSE_UNIT_THOUSANDTHS,
                         "Thousandths");
            addValueName(GRAY_RESPONSE_UNIT_TEN_THOUSANDTHS,
                         "Ten-Thousandths");
            addValueName(GRAY_RESPONSE_UNIT_HUNDRED_THOUSANDTHS,
                         "Hundred-Thousandths");
        }
    }

    // HalftoneHints

    static class HalftoneHints extends TIFFTag {

        public HalftoneHints() {
            super("HalftoneHints",
                  TAG_HALFTONE_HINTS,
                  1 << TIFF_SHORT,
                  2);
        }
    }

    // HostComputer

    static class HostComputer extends TIFFTag {

        public HostComputer() {
            super("HostComputer",
                  TAG_HOST_COMPUTER,
                  1 << TIFF_ASCII);
        }
    }

    // ImageDescription

    static class ImageDescription extends TIFFTag {

        public ImageDescription() {
            super("ImageDescription",
                  TAG_IMAGE_DESCRIPTION,
                  1 << TIFF_ASCII);
        }
    }

    // ImageLength tag

    static class ImageLength extends TIFFTag {

        public ImageLength() {
            super("ImageLength",
                  TAG_IMAGE_LENGTH,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG),
                  1);
        }
    }

    // ImageWidth tag

    static class ImageWidth extends TIFFTag {

        public ImageWidth() {
            super("ImageWidth",
                  TAG_IMAGE_WIDTH,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG),
                  1);
        }
    }

    // InkNames

    static class InkNames extends TIFFTag {

        public InkNames() {
            super("InkNames",
                  TAG_INK_NAMES,
                  1 << TIFF_ASCII);
        }
    }

    // InkSet

    static class InkSet extends TIFFTag {

        public InkSet() {
            super("InkSet",
                  TAG_INK_SET,
                  1 << TIFF_SHORT,
                  1);

            addValueName(INK_SET_CMYK, "CMYK");
            addValueName(INK_SET_NOT_CMYK, "Not CMYK");
        }
    }

    // JPEGTables (Tech note)

    static class JPEGTables extends TIFFTag {

        public JPEGTables() {
            super("JPEGTables",
                  TAG_JPEG_TABLES,
                  1 << TIFF_UNDEFINED);
        }
    }

    // JPEGACTables

    static class JPEGACTables extends TIFFTag {

        public JPEGACTables() {
            super("JPEGACTables",
                  TAG_JPEG_AC_TABLES,
                  1 << TIFF_LONG);
        }
    }

    // JPEGDCTables

    static class JPEGDCTables extends TIFFTag {

        public JPEGDCTables() {
            super("JPEGDCTables",
                  TAG_JPEG_DC_TABLES,
                  1 << TIFF_LONG);
        }
    }

    // JPEGInterchangeFormat

    static class JPEGInterchangeFormat extends TIFFTag {

        public JPEGInterchangeFormat() {
            super("JPEGInterchangeFormat",
                  TAG_JPEG_INTERCHANGE_FORMAT,
                  1 << TIFF_LONG,
                  1);
        }
    }

    // JPEGInterchangeFormatLength

    static class JPEGInterchangeFormatLength extends TIFFTag {

        public JPEGInterchangeFormatLength() {
            super("JPEGInterchangeFormatLength",
                  TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                  1 << TIFF_LONG,
                  1);
        }
    }

    // JPEGLosslessPredictors

    static class JPEGLosslessPredictors extends TIFFTag {

        public JPEGLosslessPredictors() {
            super("JPEGLosslessPredictors",
                  TAG_JPEG_LOSSLESS_PREDICTORS,
                  1 << TIFF_SHORT);

            addValueName(1, "A");
            addValueName(2, "B");
            addValueName(3, "C");
            addValueName(4, "A+B-C");
            addValueName(5, "A+((B-C)/2)");
            addValueName(6, "B+((A-C)/2)");
            addValueName(7, "(A+B)/2");
        }
    }

    // JPEGPointTransforms

    static class JPEGPointTransforms extends TIFFTag {

        public JPEGPointTransforms() {
            super("JPEGPointTransforms",
                  TAG_JPEG_POINT_TRANSFORMS,
                  1 << TIFF_SHORT);
        }
    }

    // JPEGProc

    static class JPEGProc extends TIFFTag {

        public JPEGProc() {
            super("JPEGProc",
                  TAG_JPEG_PROC,
                  1 << TIFF_SHORT,
                  1);

            addValueName(JPEG_PROC_BASELINE, "Baseline sequential process");
            addValueName(JPEG_PROC_LOSSLESS,
                         "Lossless process with Huffman coding");
        }
    }

    // JPEGQTables

    static class JPEGQTables extends TIFFTag {

        public JPEGQTables() {
            super("JPEGQTables",
                  TAG_JPEG_Q_TABLES,
                  1 << TIFF_LONG);
        }
    }

    // JPEGRestartInterval

    static class JPEGRestartInterval extends TIFFTag {

        public JPEGRestartInterval() {
            super("JPEGRestartInterval",
                  TAG_JPEG_RESTART_INTERVAL,
                  1 << TIFF_SHORT,
                  1);
        }
    }

    // Make

    static class Make extends TIFFTag {

        public Make() {
            super("Make",
                  TAG_MAKE,
                  1 << TIFF_ASCII);
        }
    }

    // MaxSampleValue

    static class MaxSampleValue extends TIFFTag {

        public MaxSampleValue() {
            super("MaxSampleValue",
                  TAG_MAX_SAMPLE_VALUE,
                  1 << TIFF_SHORT);
        }
    }

    // MinSampleValue

    static class MinSampleValue extends TIFFTag {

        public MinSampleValue() {
            super("MinSampleValue",
                  TAG_MIN_SAMPLE_VALUE,
                  1 << TIFF_SHORT);
        }
    }

    // Model

    static class Model extends TIFFTag {

        public Model() {
            super("Model",
                  TAG_MODEL,
                  1 << TIFF_ASCII);
        }
    }

    // NewSubfileType

    static class NewSubfileType extends TIFFTag {

        public NewSubfileType() {
            super("NewSubfileType",
                  TAG_NEW_SUBFILE_TYPE,
                  1 << TIFF_LONG,
                  1);

            addValueName(0,
                         "Default");
            addValueName(NEW_SUBFILE_TYPE_REDUCED_RESOLUTION,
                         "ReducedResolution");
            addValueName(NEW_SUBFILE_TYPE_SINGLE_PAGE,
                         "SinglePage");
            addValueName(NEW_SUBFILE_TYPE_SINGLE_PAGE |
                         NEW_SUBFILE_TYPE_REDUCED_RESOLUTION,
                         "SinglePage+ReducedResolution");
            addValueName(NEW_SUBFILE_TYPE_TRANSPARENCY,
                         "Transparency");
            addValueName(NEW_SUBFILE_TYPE_TRANSPARENCY |
                         NEW_SUBFILE_TYPE_REDUCED_RESOLUTION,
                         "Transparency+ReducedResolution");
            addValueName(NEW_SUBFILE_TYPE_TRANSPARENCY |
                         NEW_SUBFILE_TYPE_SINGLE_PAGE,
                         "Transparency+SinglePage");
            addValueName(NEW_SUBFILE_TYPE_TRANSPARENCY |
                         NEW_SUBFILE_TYPE_SINGLE_PAGE |
                         NEW_SUBFILE_TYPE_REDUCED_RESOLUTION,
                         "Transparency+SinglePage+ReducedResolution");
        }
    }

    // NumberOfInks

    static class NumberOfInks extends TIFFTag {

        public NumberOfInks() {
            super("NumberOfInks",
                  TAG_NUMBER_OF_INKS,
                  1 << TIFF_SHORT,
                  1);
        }
    }

    // Orientation

    static class Orientation extends TIFFTag {

        public Orientation() {
            super("Orientation",
                  TAG_ORIENTATION,
                  1 << TIFF_SHORT,
                  1);

            addValueName(ORIENTATION_ROW_0_TOP_COLUMN_0_LEFT,
                         "Row 0=Top, Column 0=Left");
            addValueName(ORIENTATION_ROW_0_TOP_COLUMN_0_RIGHT,
                         "Row 0=Top, Column 0=Right");
            addValueName(ORIENTATION_ROW_0_BOTTOM_COLUMN_0_RIGHT,
                         "Row 0=Bottom, Column 0=Right");
            addValueName(ORIENTATION_ROW_0_BOTTOM_COLUMN_0_LEFT,
                         "Row 0=Bottom, Column 0=Left");
            addValueName(ORIENTATION_ROW_0_LEFT_COLUMN_0_TOP,
                         "Row 0=Left, Column 0=Top");
            addValueName(ORIENTATION_ROW_0_RIGHT_COLUMN_0_TOP,
                         "Row 0=Right, Column 0=Top");
            addValueName(ORIENTATION_ROW_0_RIGHT_COLUMN_0_BOTTOM,
                         "Row 0=Right, Column 0=Bottom");
        }
    }

    // PageName

    static class PageName extends TIFFTag {

        public PageName() {
            super("PageName",
                  TAG_PAGE_NAME,
                  1 << TIFF_ASCII);
        }
    }

    // PageNumber

    static class PageNumber extends TIFFTag {

        public PageNumber() {
            super("PageNumber",
                  TAG_PAGE_NUMBER,
                  1 << TIFF_SHORT);
        }
    }

    // PhotometricInterpretation

    static class PhotometricInterpretation extends TIFFTag {

        public PhotometricInterpretation() {
            super("PhotometricInterpretation",
                  TAG_PHOTOMETRIC_INTERPRETATION,
                  1 << TIFF_SHORT,
                  1);

            addValueName(PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO,
                         "WhiteIsZero");
            addValueName(PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO,
                         "BlackIsZero");
            addValueName(PHOTOMETRIC_INTERPRETATION_RGB,
                         "RGB");
            addValueName(PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR,
                         "Palette Color");
            addValueName(PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK,
                         "Transparency Mask");
            addValueName(PHOTOMETRIC_INTERPRETATION_CMYK,
                         "CMYK");
            addValueName(PHOTOMETRIC_INTERPRETATION_Y_CB_CR,
                         "YCbCr");
            addValueName(PHOTOMETRIC_INTERPRETATION_CIELAB,
                         "CIELAB");
            addValueName(PHOTOMETRIC_INTERPRETATION_ICCLAB,
                         "ICCLAB"); // Non-baseline
        }
    }

    // PlanarConfiguration

    static class PlanarConfiguration extends TIFFTag {

        public PlanarConfiguration() {
            super("PlanarConfiguration",
                  TAG_PLANAR_CONFIGURATION,
                  1 << TIFF_SHORT,
                  1);

            addValueName(PLANAR_CONFIGURATION_CHUNKY, "Chunky");
            addValueName(PLANAR_CONFIGURATION_PLANAR, "Planar");
        }
    }

    // Predictor

    static class Predictor extends TIFFTag {

        public Predictor() {
            super("Predictor",
                  TAG_PREDICTOR,
                  1 << TIFF_SHORT,
                  1);

            addValueName(PREDICTOR_NONE,
                         "None");
            addValueName(PREDICTOR_HORIZONTAL_DIFFERENCING,
                         "Horizontal Differencing");
        }
    }

    // PrimaryChromaticities

    static class PrimaryChromaticities extends TIFFTag {

        public PrimaryChromaticities() {
            super("PrimaryChromaticities",
                  TAG_PRIMARY_CHROMATICITES,
                  1 << TIFF_RATIONAL,
                  6);
        }
    }

    // ReferenceBlackWhite

    static class ReferenceBlackWhite extends TIFFTag {

        public ReferenceBlackWhite() {
            super("ReferenceBlackWhite",
                  TAG_REFERENCE_BLACK_WHITE,
                  1 << TIFF_RATIONAL);
        }
    }

    // ResolutionUnit

    static class ResolutionUnit extends TIFFTag {

        public ResolutionUnit() {
            super("ResolutionUnit",
                  TAG_RESOLUTION_UNIT,
                  1 << TIFF_SHORT,
                  1);

            addValueName(RESOLUTION_UNIT_NONE, "None");
            addValueName(RESOLUTION_UNIT_INCH, "Inch");
            addValueName(RESOLUTION_UNIT_CENTIMETER, "Centimeter");
        }
    }

    // RowsPerStrip

    static class RowsPerStrip extends TIFFTag {

        public RowsPerStrip() {
            super("RowsPerStrip",
                  TAG_ROWS_PER_STRIP,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG),
                  1);
        }
    }

    // SampleFormat

    static class SampleFormat extends TIFFTag {

        public SampleFormat() {
            super("SampleFormat",
                  TAG_SAMPLE_FORMAT,
                  1 << TIFF_SHORT);

            addValueName(SAMPLE_FORMAT_UNSIGNED_INTEGER, "Unsigned Integer");
            addValueName(SAMPLE_FORMAT_SIGNED_INTEGER, "Signed Integer");
            addValueName(SAMPLE_FORMAT_FLOATING_POINT, "Floating Point");
            addValueName(SAMPLE_FORMAT_UNDEFINED, "Undefined");
        }
    }

    // SamplesPerPixel

    static class SamplesPerPixel extends TIFFTag {

        public SamplesPerPixel() {
            super("SamplesPerPixel",
                  TAG_SAMPLES_PER_PIXEL,
                  1 << TIFF_SHORT,
                  1);
        }
    }

    // SMaxSampleValue

    static class SMaxSampleValue extends TIFFTag {

        public SMaxSampleValue() {
            super("SMaxSampleValue",
                  TAG_S_MAX_SAMPLE_VALUE,
                  (1 << TIFF_BYTE) |
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG) |
                  (1 << TIFF_RATIONAL) |
                  (1 << TIFF_SBYTE) |
                  (1 << TIFF_SSHORT) |
                  (1 << TIFF_SLONG) |
                  (1 << TIFF_SRATIONAL) |
                  (1 << TIFF_FLOAT) |
                  (1 << TIFF_DOUBLE));
        }
    }

    // SMinSampleValue

    static class SMinSampleValue extends TIFFTag {

        public SMinSampleValue() {
            super("SMinSampleValue",
                  TAG_S_MIN_SAMPLE_VALUE,
                  (1 << TIFF_BYTE) |
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG) |
                  (1 << TIFF_RATIONAL) |
                  (1 << TIFF_SBYTE) |
                  (1 << TIFF_SSHORT) |
                  (1 << TIFF_SLONG) |
                  (1 << TIFF_SRATIONAL) |
                  (1 << TIFF_FLOAT) |
                  (1 << TIFF_DOUBLE));
        }
    }

    // Software

    static class Software extends TIFFTag {

        public Software() {
            super("Software",
                  TAG_SOFTWARE,
                  1 << TIFF_ASCII);
        }
    }

    // StripByteCounts

    static class StripByteCounts extends TIFFTag {

        public StripByteCounts() {
            super("StripByteCounts",
                  TAG_STRIP_BYTE_COUNTS,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG));
        }
    }

    // StripOffsets

    static class StripOffsets extends TIFFTag {

        public StripOffsets() {
            super("StripOffsets",
                  TAG_STRIP_OFFSETS,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG));
        }
    }

    // SubfileType (deprecated by TIFF but retained for backward compatibility)

    static class SubfileType extends TIFFTag {

        public SubfileType() {
            super("SubfileType",
                  TAG_SUBFILE_TYPE,
                  1 << TIFF_SHORT,
                  1);

            addValueName(SUBFILE_TYPE_FULL_RESOLUTION, "FullResolution");
            addValueName(SUBFILE_TYPE_REDUCED_RESOLUTION, "ReducedResolution");
            addValueName(SUBFILE_TYPE_SINGLE_PAGE, "SinglePage");
        }
    }

    // T4Options

    static class T4Options extends TIFFTag {

        public T4Options() {
            super("T4Options",
                  TAG_T4_OPTIONS,
                  1 << TIFF_LONG,
                  1);

            addValueName(0,
                         "Default 1DCoding"); // 0x00
            addValueName(T4_OPTIONS_2D_CODING,
                         "2DCoding"); // 0x01
            addValueName(T4_OPTIONS_UNCOMPRESSED,
                         "Uncompressed"); // 0x02
            addValueName(T4_OPTIONS_2D_CODING |
                         T4_OPTIONS_UNCOMPRESSED,
                         "2DCoding+Uncompressed"); // 0x03
            addValueName(T4_OPTIONS_EOL_BYTE_ALIGNED,
                         "EOLByteAligned"); // 0x04
            addValueName(T4_OPTIONS_2D_CODING |
                         T4_OPTIONS_EOL_BYTE_ALIGNED,
                         "2DCoding+EOLByteAligned"); // 0x05
            addValueName(T4_OPTIONS_UNCOMPRESSED |
                         T4_OPTIONS_EOL_BYTE_ALIGNED,
                         "Uncompressed+EOLByteAligned"); // 0x06
            addValueName(T4_OPTIONS_2D_CODING |
                         T4_OPTIONS_UNCOMPRESSED |
                         T4_OPTIONS_EOL_BYTE_ALIGNED,
                         "2DCoding+Uncompressed+EOLByteAligned"); // 0x07
        }
    }

    // T6Options

    static class T6Options extends TIFFTag {

        public T6Options() {
            super("T6Options",
                  TAG_T6_OPTIONS,
                  1 << TIFF_LONG,
                  1);

            addValueName(0,
                         "Default"); // 0x00
            // 0x01 is not possible as bit 0 is unused and always zero.
            addValueName(T6_OPTIONS_UNCOMPRESSED,
                         "Uncompressed"); // 0x02
        }
    }

    // TargetPrinter

    static class TargetPrinter extends TIFFTag {

        public TargetPrinter() {
            super("TargetPrinter",
                  TAG_TARGET_PRINTER,
                  1 << TIFF_ASCII);
        }
    }

    // Threshholding

    static class Threshholding extends TIFFTag {

        public Threshholding() {
            super("Threshholding",
                  TAG_THRESHHOLDING,
                  1 << TIFF_SHORT,
                  1);

            addValueName(1, "None");
            addValueName(2, "OrderedDither");
            addValueName(3, "RandomizedDither");
        }
    }

    // TileByteCounts

    static class TileByteCounts extends TIFFTag {

        public TileByteCounts() {
            super("TileByteCounts",
                  TAG_TILE_BYTE_COUNTS,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG));
        }
    }

    // TileOffsets

    static class TileOffsets extends TIFFTag {

        public TileOffsets() {
            super("TileOffsets",
                  TAG_TILE_OFFSETS,
                  1 << TIFF_LONG);
        }
    }

    // TileLength tag

    static class TileLength extends TIFFTag {

        public TileLength() {
            super("TileLength",
                  TAG_TILE_LENGTH,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG),
                  1);
        }
    }

    // TileWidth tag

    static class TileWidth extends TIFFTag {

        public TileWidth() {
            super("TileWidth",
                  TAG_TILE_WIDTH,
                  (1 << TIFF_SHORT) |
                  (1 << TIFF_LONG),
                  1);
        }
    }

    // TransferFunction

    static class TransferFunction extends TIFFTag {

        public TransferFunction() {
            super("TransferFunction",
                  TAG_TRANSFER_FUNCTION,
                  1 << TIFF_SHORT);
        }
    }

    // TransferRange

    static class TransferRange extends TIFFTag {

        public TransferRange() {
            super("TransferRange",
                  TAG_TRANSFER_RANGE,
                  1 << TIFF_SHORT,
                  6);
        }
    }

    // WhitePoint

    static class WhitePoint extends TIFFTag {

        public WhitePoint() {
            super("WhitePoint",
                  TAG_WHITE_POINT,
                  1 << TIFF_RATIONAL,
                  2);
        }
    }

    // XPosition

    static class XPosition extends TIFFTag {

        public XPosition() {
            super("XPosition",
                  TAG_X_POSITION,
                  1 << TIFF_RATIONAL,
                  1);
        }
    }

    // XResolution

    static class XResolution extends TIFFTag {

        public XResolution() {
            super("XResolution",
                  TAG_X_RESOLUTION,
                  1 << TIFF_RATIONAL,
                  1);
        }
    }

    // YCbCrCoefficients

    static class YCbCrCoefficients extends TIFFTag {

        public YCbCrCoefficients() {
            super("YCbCrCoefficients",
                  TAG_Y_CB_CR_COEFFICIENTS,
                  1 << TIFF_RATIONAL,
                  3);
        }
    }

    // YCbCrPositioning

    static class YCbCrPositioning extends TIFFTag {

        public YCbCrPositioning() {
            super("YCbCrPositioning",
                  TAG_Y_CB_CR_POSITIONING,
                  1 << TIFF_SHORT,
                  1);

            addValueName(Y_CB_CR_POSITIONING_CENTERED, "Centered");
            addValueName(Y_CB_CR_POSITIONING_COSITED, "Cosited");
        }
    }

    // YCbCrSubSampling

    static class YCbCrSubSampling extends TIFFTag {

        public YCbCrSubSampling() {
            super("YCbCrSubSampling",
                  TAG_Y_CB_CR_SUBSAMPLING,
                  1 << TIFF_SHORT,
                  2);
        }
    }

    // YPosition

    static class YPosition extends TIFFTag {

        public YPosition() {
            super("YPosition",
                  TAG_Y_POSITION,
                  1 << TIFF_RATIONAL,
                  1);
        }
    }

    // YResolution

    static class YResolution extends TIFFTag {

        public YResolution() {
            super("YResolution",
                  TAG_Y_RESOLUTION,
                  1 << TIFF_RATIONAL,
                  1);
        }
    }

    // Non-6.0 tags

    // ICC Profile (Spec. ICC.1:2001-12, File Format for Color Profiles)

    static class ICCProfile extends TIFFTag {

        public ICCProfile() {
            super("ICC Profile",
                  TAG_ICC_PROFILE,
                  1 << TIFF_UNDEFINED);
        }
    }

    private static List<TIFFTag> tags;

    private static void initTags() {
        tags = new ArrayList<TIFFTag>(76);

        tags.add(new BaselineTIFFTagSet.Artist());
        tags.add(new BaselineTIFFTagSet.BitsPerSample());
        tags.add(new BaselineTIFFTagSet.CellLength());
        tags.add(new BaselineTIFFTagSet.CellWidth());
        tags.add(new BaselineTIFFTagSet.ColorMap());
        tags.add(new BaselineTIFFTagSet.Compression());
        tags.add(new BaselineTIFFTagSet.Copyright());
        tags.add(new BaselineTIFFTagSet.DateTime());
        tags.add(new BaselineTIFFTagSet.DocumentName());
        tags.add(new BaselineTIFFTagSet.DotRange());
        tags.add(new BaselineTIFFTagSet.ExtraSamples());
        tags.add(new BaselineTIFFTagSet.FillOrder());
        tags.add(new BaselineTIFFTagSet.FreeByteCounts());
        tags.add(new BaselineTIFFTagSet.FreeOffsets());
        tags.add(new BaselineTIFFTagSet.GrayResponseCurve());
        tags.add(new BaselineTIFFTagSet.GrayResponseUnit());
        tags.add(new BaselineTIFFTagSet.HalftoneHints());
        tags.add(new BaselineTIFFTagSet.HostComputer());
        tags.add(new BaselineTIFFTagSet.ImageDescription());
        tags.add(new BaselineTIFFTagSet.ICCProfile());
        tags.add(new BaselineTIFFTagSet.ImageLength());
        tags.add(new BaselineTIFFTagSet.ImageWidth());
        tags.add(new BaselineTIFFTagSet.InkNames());
        tags.add(new BaselineTIFFTagSet.InkSet());
        tags.add(new BaselineTIFFTagSet.JPEGACTables());
        tags.add(new BaselineTIFFTagSet.JPEGDCTables());
        tags.add(new BaselineTIFFTagSet.JPEGInterchangeFormat());
        tags.add(new BaselineTIFFTagSet.JPEGInterchangeFormatLength());
        tags.add(new BaselineTIFFTagSet.JPEGLosslessPredictors());
        tags.add(new BaselineTIFFTagSet.JPEGPointTransforms());
        tags.add(new BaselineTIFFTagSet.JPEGProc());
        tags.add(new BaselineTIFFTagSet.JPEGQTables());
        tags.add(new BaselineTIFFTagSet.JPEGRestartInterval());
        tags.add(new BaselineTIFFTagSet.JPEGTables());
        tags.add(new BaselineTIFFTagSet.Make());
        tags.add(new BaselineTIFFTagSet.MaxSampleValue());
        tags.add(new BaselineTIFFTagSet.MinSampleValue());
        tags.add(new BaselineTIFFTagSet.Model());
        tags.add(new BaselineTIFFTagSet.NewSubfileType());
        tags.add(new BaselineTIFFTagSet.NumberOfInks());
        tags.add(new BaselineTIFFTagSet.Orientation());
        tags.add(new BaselineTIFFTagSet.PageName());
        tags.add(new BaselineTIFFTagSet.PageNumber());
        tags.add(new BaselineTIFFTagSet.PhotometricInterpretation());
        tags.add(new BaselineTIFFTagSet.PlanarConfiguration());
        tags.add(new BaselineTIFFTagSet.Predictor());
        tags.add(new BaselineTIFFTagSet.PrimaryChromaticities());
        tags.add(new BaselineTIFFTagSet.ReferenceBlackWhite());
        tags.add(new BaselineTIFFTagSet.ResolutionUnit());
        tags.add(new BaselineTIFFTagSet.RowsPerStrip());
        tags.add(new BaselineTIFFTagSet.SampleFormat());
        tags.add(new BaselineTIFFTagSet.SamplesPerPixel());
        tags.add(new BaselineTIFFTagSet.SMaxSampleValue());
        tags.add(new BaselineTIFFTagSet.SMinSampleValue());
        tags.add(new BaselineTIFFTagSet.Software());
        tags.add(new BaselineTIFFTagSet.StripByteCounts());
        tags.add(new BaselineTIFFTagSet.StripOffsets());
        tags.add(new BaselineTIFFTagSet.SubfileType());
        tags.add(new BaselineTIFFTagSet.T4Options());
        tags.add(new BaselineTIFFTagSet.T6Options());
        tags.add(new BaselineTIFFTagSet.TargetPrinter());
        tags.add(new BaselineTIFFTagSet.Threshholding());
        tags.add(new BaselineTIFFTagSet.TileByteCounts());
        tags.add(new BaselineTIFFTagSet.TileOffsets());
        tags.add(new BaselineTIFFTagSet.TileLength());
        tags.add(new BaselineTIFFTagSet.TileWidth());
        tags.add(new BaselineTIFFTagSet.TransferFunction());
        tags.add(new BaselineTIFFTagSet.TransferRange());
        tags.add(new BaselineTIFFTagSet.WhitePoint());
        tags.add(new BaselineTIFFTagSet.XPosition());
        tags.add(new BaselineTIFFTagSet.XResolution());
        tags.add(new BaselineTIFFTagSet.YCbCrCoefficients());
        tags.add(new BaselineTIFFTagSet.YCbCrPositioning());
        tags.add(new BaselineTIFFTagSet.YCbCrSubSampling());
        tags.add(new BaselineTIFFTagSet.YPosition());
        tags.add(new BaselineTIFFTagSet.YResolution());
    }

    private BaselineTIFFTagSet() {
        super(tags);
    }

    /**
     * Returns a shared instance of a {@code BaselineTIFFTagSet}.
     *
     * @return a {@code BaselineTIFFTagSet} instance.
     */
    public synchronized static BaselineTIFFTagSet getInstance() {
        if (theInstance == null) {
            initTags();
            theInstance = new BaselineTIFFTagSet();
            tags = null;
        }
        return theInstance;
    }
}
