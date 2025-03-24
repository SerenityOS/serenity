/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/Debug.h>
#include <AK/Enumerate.h>
#include <AK/MemoryStream.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ImageFormats/ISOBMFF/JPEG2000Boxes.h>
#include <LibGfx/ImageFormats/ISOBMFF/Reader.h>
#include <LibGfx/ImageFormats/JPEG2000BitplaneDecoding.h>
#include <LibGfx/ImageFormats/JPEG2000InverseDiscreteWaveletTransform.h>
#include <LibGfx/ImageFormats/JPEG2000Loader.h>
#include <LibGfx/ImageFormats/JPEG2000ProgressionIterators.h>
#include <LibGfx/ImageFormats/JPEG2000TagTree.h>
#include <LibTextCodec/Decoder.h>

// Core coding system spec (.jp2 format): T-REC-T.800-201511-S!!PDF-E.pdf available here:
// https://www.itu.int/rec/dologin_pub.asp?lang=e&id=T-REC-T.800-201511-S!!PDF-E&type=items

// There is a useful example bitstream in the spec in:
// J.10 An example of decoding showing intermediate

// Extensions (.jpx format): T-REC-T.801-202106-S!!PDF-E.pdf available here:
// https://handle.itu.int/11.1002/1000/14666-en?locatt=format:pdf&auth

// rfc3745 lists the MIME type. It only mentions the jp2_id_string as magic number.

// A short overview of the JPEG2000 format:
//
// Image Decomposition
// -------------------
//
// 1. An image is first divided into independent tiles
// 2. Each tile is split into tile components (one each for R, G, B, A)
// 3. Each tile component undergoes Discrete Wavelet Transform (DWT)
//
// Resolution Levels and Subbands
// ------------------------------
//
// The DWT produces hierarchical resolution levels with these subbands:
// - Level 0: Single LL (Lowpass-Lowpass) subband
// - Level 1+: HL (Highpass-Lowpass), LH (Lowpass-Highpass), and HH (Highpass-Highpass) subbands
//
// Subband Layout:
// +-----+-----+----------+
// | LL0 | HL1 |          |
// +-----+-----+   HL2    |
// | LH1 | HH1 |          |
// +-----+-----+----------+
// |           |          |
// |    LH2    |    HH2   |
// |           |          |
// +-----------+----------+
//
// Precinct Structure
// ------------------
// - Precincts are rectangular regions that span all subbands within a resolution level
// - Typical size: 512k × 512k pixels
// - Most images contain only a single precinct due to this large size
// - "Precinct limited to a subband": portion of precinct covering one subband
//
// Layer System
// -----------
// - Coefficients' bitplanes can be stored separately
// - Groups of bitplanes form "layers"
// - For example, for an 8bpp image, layer 0 might contain the first two bitplanes, layer 1 the next two, etc.
// - Enables progressive refinement of image color resolution
//
// Codeblock Organization
// ----------------------
// - Each precinct is divided into codeblocks
// - A codeblock is the smallest coded unit in JPEG2000
// - Typical codeblock size: 64×64 pixels
// - Codeblocks store coefficient bitplanes from wavelet transformation
// - Independent arithmetic decoder contexts enable parallel decoding
// - A codeblock can be split into segments. A segment is a group of bytes
//   that are fed into the arithmetic decoder as one unit. Most files use one segment,
//   but the code block styles "termination on each coding pass" and
//   "selective arithmetic coding bypass" use multiple segments.
//
// Packets
// -------
// "All compressed image data representing a specific tile, layer, component, resolution level and precinct appears in the
//  codestream in a contiguous segment called a packet."
// A packet contains a packet header, and information about all codeblocks in the packet.

namespace Gfx {

// A JPEG2000 image can be stored in a codestream with markers, similar to a JPEG image,
// or in a JP2 file, which is a container format based on boxes similar to ISOBMFF.

// This is the marker for the codestream version.
// T.800 Annex A, Codestream syntax, A.2 Information in the marker segments and A.3 Construction of the codestream
static constexpr u8 marker_id_string[] = { 0xFF, 0x4F, 0xFF, 0x51 };

// This is the marker for the box version.
// T.800 Annex I, JP2 file format syntax, I.5.1 JPEG 2000 Signature box
static constexpr u8 jp2_id_string[] = { 0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20, 0x0D, 0x0A, 0x87, 0x0A };

// Table A.2 – List of markers and marker segments
// "Delimiting markers and marker segments"
#define J2K_SOC 0xFF4F // "Start of codestream"
#define J2K_SOT 0xFF90 // "Start of tile-part"
#define J2K_SOD 0xFF93 // "Start of data"
#define J2K_EOC 0xFFD9 // "End of codestream"
// "Fixed information marker segments"
#define J2K_SIZ 0xFF51 // "Image and tile size"
// "Functional marker segments"
#define J2K_COD 0xFF52 // "Coding style default"
#define J2K_COC 0xFF53 // "Coding style component"
#define J2K_RGN 0xFF5E // "Region-of-interest"
#define J2K_QCD 0xFF5C // "Quantization default"
#define J2K_QCC 0xFF5D // "Quantization component"
#define J2K_POC 0xFF5F // "Progression order change"
// "Pointer marker segments"
#define J2K_TLM 0xFF55 // "Tile-part lengths"
#define J2K_PLM 0xFF57 // "Packet length, main header"
#define J2K_PLT 0xFF58 // "Packet length, tile-part header"
#define J2K_PPM 0xFF60 // "Packed packet headers, main header"
#define J2K_PPT 0xFF61 // "Packed packet headers, tile-part header"
// "In-bit-stream markers and marker segments"
#define J2K_SOP 0xFF91 // "Start of packet"
#define J2K_EPH 0xFF92 // "End of packet header"
// "Informational marker segments"
#define J2K_CRG 0xFF63 // "Component registration"
#define J2K_COM 0xFF64 // "Comment"

// A.4.2 Start of tile-part (SOT)
struct StartOfTilePart {
    // "Tile index. This number refers to the tiles in raster order starting at the number 0."
    u16 tile_index { 0 }; // "Isot" in spec.

    // "Length, in bytes, from the beginning of the first byte of this SOT marker segment of the tile-part to
    //  the end of the data of that tile-part. Figure A.16 shows this alignment. Only the last tile-part in the
    //  codestream may contain a 0 for Psot. If the Psot is 0, this tile-part is assumed to contain all data until
    //  the EOC marker."
    u32 tile_part_length { 0 }; // "Psot" in spec.

    // "Tile-part index. There is a specific order required for decoding tile-parts; this index denotes the order
    //  from 0. If there is only one tile-part for a tile, then this value is zero. The tile-parts of this tile shall
    //  appear in the codestream in this order, although not necessarily consecutively."
    u8 tile_part_index { 0 }; // "TPsot" in spec.

    // "Number of tile-parts of a tile in the codestream. Two values are allowed: the correct number of tile-
    //  parts for that tile and zero. A zero value indicates that the number of tile-parts of this tile is not
    //  specified in this tile-part.
    u8 number_of_tile_parts { 0 }; // "TNsot" in spec.
};

static ErrorOr<StartOfTilePart> read_start_of_tile_part(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };

    StartOfTilePart sot;
    sot.tile_index = TRY(stream.read_value<BigEndian<u16>>());
    sot.tile_part_length = TRY(stream.read_value<BigEndian<u32>>());
    sot.tile_part_index = TRY(stream.read_value<u8>());
    sot.number_of_tile_parts = TRY(stream.read_value<u8>());

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: SOT marker segment: tile_index={}, tile_part_length={}, tile_part_index={}, number_of_tile_parts={}", sot.tile_index, sot.tile_part_length, sot.tile_part_index, sot.number_of_tile_parts);

    return sot;
}

// A.5.1 Image and tile size (SIZ)
struct ImageAndTileSize {
    // "Denotes capabilities that a decoder needs to properly decode the codestream."
    u16 needed_decoder_capabilities { 0 }; // "Rsiz" in spec.

    // "Width of the reference grid."
    u32 width { 0 }; // "Xsiz" in spec.

    // "Height of the reference grid."
    u32 height { 0 }; // "Ysiz" in spec.

    // "Horizontal offset from the origin of the reference grid to the left side of the image area."
    u32 x_offset { 0 }; // "XOsiz" in spec.

    // "Vertical offset from the origin of the reference grid to the top side of the image area."
    u32 y_offset { 0 }; // "YOsiz" in spec.

    // "Width of one reference tile with respect to the reference grid."
    u32 tile_width { 0 }; // "XTsiz" in spec.

    // "Height of one reference tile with respect to the reference grid."
    u32 tile_height { 0 }; // "YTsiz" in spec.

    // "Horizontal offset from the origin of the reference grid to the left side of the first tile."
    u32 tile_x_offset { 0 }; // "XTOsiz" in spec.

    // "Vertical offset from the origin of the reference grid to the top side of the first tile."
    u32 tile_y_offset { 0 }; // "YTOsiz" in spec.

    // "Csiz" isn't stored in this struct. It corresponds to `components.size()`.

    struct ComponentInformation {
        // "Precision (depth) in bits and sign of the ith component samples."
        u8 depth_and_sign { 0 }; // "Ssiz" in spec.

        // Table A.11 – Component Ssiz parameter
        u8 bit_depth() const { return (depth_and_sign & 0x7F) + 1; }
        bool is_signed() const { return depth_and_sign & 0x80; }

        // "Horizontal separation of a sample of the ith component with respect to the reference grid."
        u8 horizontal_separation { 0 }; // "XRsiz" in spec.

        // "Vertical separation of a sample of the ith component with respect to the reference grid."
        u8 vertical_separation { 0 }; // "YRsiz" in spec.
    };
    Vector<ComponentInformation> components;

    // (B-5)
    u32 number_of_x_tiles() const { return ceil_div(width - x_offset, tile_width); }
    u32 number_of_y_tiles() const { return ceil_div(height - y_offset, tile_height); }

    IntPoint tile_2d_index_from_1d_index(u32 tile_index) const
    {
        // (B-6)
        return { tile_index % number_of_x_tiles(), tile_index / number_of_x_tiles() };
    }

    IntRect reference_grid_coordinates_for_tile(IntPoint tile_2d_index) const
    {
        int p = tile_2d_index.x();
        int q = tile_2d_index.y();
        int tx0 = max(tile_x_offset + p * tile_width, x_offset);      // (B-7)
        int ty0 = max(tile_y_offset + q * tile_height, y_offset);     // (B-8)
        int tx1 = min(tile_x_offset + (p + 1) * tile_width, width);   // (B-9)
        int ty1 = min(tile_y_offset + (q + 1) * tile_height, height); // (B-10)
        return { tx0, ty0, tx1 - tx0, ty1 - ty0 };                    // (B-11)
    }

    IntRect reference_grid_coordinates_for_tile_component(IntRect tile_rect, int component_index) const
    {
        // (B-12)
        int tcx0 = ceil_div(tile_rect.left(), static_cast<int>(components[component_index].horizontal_separation));
        int tcx1 = ceil_div(tile_rect.right(), static_cast<int>(components[component_index].horizontal_separation));
        int tcy0 = ceil_div(tile_rect.top(), static_cast<int>(components[component_index].vertical_separation));
        int tcy1 = ceil_div(tile_rect.bottom(), static_cast<int>(components[component_index].vertical_separation));
        return { tcx0, tcy0, tcx1 - tcx0, tcy1 - tcy0 }; // (B-13)
    }

    IntRect reference_grid_coordinates_for_tile_component(IntPoint tile_2d_index, int component_index) const
    {
        auto tile_rect = reference_grid_coordinates_for_tile(tile_2d_index);
        return reference_grid_coordinates_for_tile_component(tile_rect, component_index);
    }

    IntRect reference_grid_coordinates_for_ll_band(IntRect tile_rect, int component_index, int r, int N_L) const
    {
        // B.5
        // (B-14)
        auto component_rect = reference_grid_coordinates_for_tile_component(tile_rect, component_index);
        int denominator = 1 << (N_L - r);
        int trx0 = ceil_div(component_rect.left(), denominator);
        int try0 = ceil_div(component_rect.top(), denominator);
        int trx1 = ceil_div(component_rect.right(), denominator);
        int try1 = ceil_div(component_rect.bottom(), denominator);

        return { trx0, try0, trx1 - trx0, try1 - try0 };
    }

    IntRect reference_grid_coordinates_for_sub_band(IntRect tile_rect, int component_index, int n_b, JPEG2000::SubBand sub_band) const
    {
        // B.5
        // Table B.1 – Quantities (xob, yob) for sub-band b
        int xob = 0;
        int yob = 0;
        if (sub_band == JPEG2000::SubBand::HorizontalHighpassVerticalLowpass || sub_band == JPEG2000::SubBand::HorizontalHighpassVerticalHighpass)
            xob = 1;
        if (sub_band == JPEG2000::SubBand::HorizontalLowpassVerticalHighpass || sub_band == JPEG2000::SubBand::HorizontalHighpassVerticalHighpass)
            yob = 1;
        VERIFY(n_b >= 1 || (n_b == 0 && sub_band == JPEG2000::SubBand::HorizontalLowpassVerticalLowpass));

        // If n_b is 0, `1 << (n_b - 1)` is undefined, but n_b is only 0 for the LL band, where xob and yob are 0 anyways.
        // So the value of o_scale doesn't matter in that case.
        int o_scale = 0;
        if (n_b > 0)
            o_scale = 1 << (n_b - 1);

        // (B-15)
        auto component_rect = reference_grid_coordinates_for_tile_component(tile_rect, component_index);
        int denominator = 1 << n_b;
        int tbx0 = ceil_div(component_rect.left() - o_scale * xob, denominator);
        int tby0 = ceil_div(component_rect.top() - o_scale * yob, denominator);
        int tbx1 = ceil_div(component_rect.right() - o_scale * xob, denominator);
        int tby1 = ceil_div(component_rect.bottom() - o_scale * yob, denominator);

        return { tbx0, tby0, tbx1 - tbx0, tby1 - tby0 };
    }

    IntRect reference_grid_coordinates_for_sub_band(IntPoint tile_2d_index, int component_index, int n_b, JPEG2000::SubBand sub_band) const
    {
        auto tile_rect = reference_grid_coordinates_for_tile(tile_2d_index);
        return reference_grid_coordinates_for_sub_band(tile_rect, component_index, n_b, sub_band);
    }
};

static ErrorOr<ImageAndTileSize> read_image_and_tile_size(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };

    ImageAndTileSize siz;
    siz.needed_decoder_capabilities = TRY(stream.read_value<BigEndian<u16>>());
    siz.width = TRY(stream.read_value<BigEndian<u32>>());
    siz.height = TRY(stream.read_value<BigEndian<u32>>());
    siz.x_offset = TRY(stream.read_value<BigEndian<u32>>());
    siz.y_offset = TRY(stream.read_value<BigEndian<u32>>());
    siz.tile_width = TRY(stream.read_value<BigEndian<u32>>());
    siz.tile_height = TRY(stream.read_value<BigEndian<u32>>());
    siz.tile_x_offset = TRY(stream.read_value<BigEndian<u32>>());
    siz.tile_y_offset = TRY(stream.read_value<BigEndian<u32>>());
    u16 component_count = TRY(stream.read_value<BigEndian<u16>>()); // "Csiz" in spec.

    // Table A.9 – Image and tile size parameter values
    // Xsiz, Ysiz, XTsiz, YTsiz: 1 to 2^32-1.
    if (siz.width == 0 || siz.height == 0 || siz.tile_width == 0 || siz.tile_height == 0)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid image or tile size");

    // Ad-hoc: Limit image size to < 4 GiB.
    if (static_cast<u64>(siz.width) * siz.height > INT32_MAX)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Image is suspiciously large, not decoding");

    // CSiz: 1 to 16384.
    if (component_count < 1 || component_count > 16384)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid number of components");

    for (size_t i = 0; i < component_count; ++i) {
        ImageAndTileSize::ComponentInformation component;
        component.depth_and_sign = TRY(stream.read_value<u8>());
        if (component.bit_depth() > 38)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid component depth");
        component.horizontal_separation = TRY(stream.read_value<u8>());
        component.vertical_separation = TRY(stream.read_value<u8>());
        siz.components.append(component);
    }

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: SIZ marker segment: needed_decoder_capabilities={}, width={}, height={}, x_offset={}, y_offset={}, tile_width={}, tile_height={}, tile_x_offset={}, tile_y_offset={}", siz.needed_decoder_capabilities, siz.width, siz.height, siz.x_offset, siz.y_offset, siz.tile_width, siz.tile_height, siz.tile_x_offset, siz.tile_y_offset);
    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: SIZ marker segment: {} components:", component_count);
    for (auto [i, component] : enumerate(siz.components))
        dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: SIZ marker segment: component[{}]: is_signed={}, bit_depth={}, horizontal_separation={}, vertical_separation={}", i, component.is_signed(), component.bit_depth(), component.horizontal_separation, component.vertical_separation);

    return siz;
}

// Data shared by COD and COC marker segments
struct CodingStyleParameters {
    // Table A.15 – Coding style parameter values of the SPcod and SPcoc parameters
    // "Number of decomposition levels, NL, Zero implies no transformation."
    u8 number_of_decomposition_levels { 0 };
    u8 code_block_width_exponent { 0 };  // "xcb" in spec; 2 already added.
    u8 code_block_height_exponent { 0 }; // "ycb" in spec; 2 already added.
    u8 code_block_style { 0 };
    JPEG2000::Transformation transformation { JPEG2000::Transformation::Irreversible_9_7_Filter };

    // Table A.19 – Code-block style for the SPcod and SPcoc parameters
    bool uses_selective_arithmetic_coding_bypass() const { return code_block_style & 1; }
    bool reset_context_probabilities() const { return code_block_style & 2; }
    bool uses_termination_on_each_coding_pass() const { return code_block_style & 4; }
    bool uses_vertically_causal_context() const { return code_block_style & 8; }
    bool uses_predictable_termination() const { return code_block_style & 0x10; }
    bool uses_segmentation_symbols() const { return code_block_style & 0x20; }

    // If has_explicit_precinct_size is false, this contains the default { 15, 15 } number_of_decomposition_levels + 1 times.
    // If has_explicit_precinct_size is true, this contains number_of_decomposition_levels + 1 explicit values stored in the COD marker segment.
    struct PrecinctSize {
        u8 PPx { 0 };
        u8 PPy { 0 };
    };
    Vector<PrecinctSize> precinct_sizes;
};

static ErrorOr<CodingStyleParameters> read_coding_style_parameters(ReadonlyBytes data, StringView name, bool has_explicit_precinct_size)
{
    FixedMemoryStream stream { data };

    CodingStyleParameters parameters;

    parameters.number_of_decomposition_levels = TRY(stream.read_value<u8>());
    if (parameters.number_of_decomposition_levels > 32)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid number of decomposition levels");

    // Table A.18 – Width or height exponent of the code-blocks for the SPcod and SPcoc parameters
    u8 xcb = (TRY(stream.read_value<u8>()) & 0xF) + 2;
    u8 ycb = (TRY(stream.read_value<u8>()) & 0xF) + 2;
    if (xcb > 10 || ycb > 10 || xcb + ycb > 12)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid code block size");
    parameters.code_block_width_exponent = xcb;
    parameters.code_block_height_exponent = ycb;

    parameters.code_block_style = TRY(stream.read_value<u8>());
    if (parameters.code_block_style & 0xC0)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Reserved code block style bits set");

    // Table A.20 – Transformation for the SPcod and SPcoc parameters
    u8 transformation = TRY(stream.read_value<u8>());
    if (transformation > 1)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid transformation");
    parameters.transformation = transformation == 0 ? JPEG2000::Transformation::Irreversible_9_7_Filter : JPEG2000::Transformation::Reversible_5_3_Filter;

    if (has_explicit_precinct_size) {
        for (size_t i = 0; i < parameters.number_of_decomposition_levels + 1u; ++i) {
            u8 b = TRY(stream.read_value<u8>());

            // Table A.21 – Precinct width and height for the SPcod and SPcoc parameters
            CodingStyleParameters::PrecinctSize precinct_size;
            precinct_size.PPx = b & 0xF;
            precinct_size.PPy = b >> 4;
            if ((precinct_size.PPx == 0 || precinct_size.PPy == 0) && i > 0)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid precinct size");
            parameters.precinct_sizes.append(precinct_size);
        }
    } else {
        for (size_t i = 0; i < parameters.number_of_decomposition_levels + 1u; ++i)
            parameters.precinct_sizes.append({ 15, 15 });
    }

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: number_of_decomposition_levels={}, code_block_width_exponent={}, code_block_height_exponent={}", name, parameters.number_of_decomposition_levels, parameters.code_block_width_exponent, parameters.code_block_height_exponent);
    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: code_block_style={}, transformation={}", name, parameters.code_block_style, (int)parameters.transformation);
    if (has_explicit_precinct_size) {
        dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: {} explicit precinct sizes:", name, parameters.precinct_sizes.size());
        for (auto [i, precinct_size] : enumerate(parameters.precinct_sizes))
            dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: precinct_size[{}]: PPx={}, PPy={}", name, i, precinct_size.PPx, precinct_size.PPy);
    }

    return parameters;
}

// A.6.1 Coding style default (COD)
struct CodingStyleDefault {
    // Table A.13 – Coding style parameter values for the Scod parameter
    bool has_explicit_precinct_size { false };
    bool may_use_SOP_marker { false };
    bool shall_use_EPH_marker { false };

    // Table A.16 – Progression order for the SGcod, SPcoc, and Ppoc parameters
    // B.12 Progression order
    enum ProgressionOrder {
        LayerResolutionComponentPosition = 0,
        ResolutionLayerComponentPosition = 1,
        ResolutionPositionComponentLayer = 2,
        PositionComponentResolutionLayer = 3,
        ComponentPositionResolutionLayer = 4,
    };

    // Table A.17 – Multiple component transformation for the SGcod parameters
    enum MultipleComponentTransformationType {
        None = 0,
        MultipleComponentTransformationUsed = 1, // See Annex G
    };

    // Table A.14 – Coding style parameter values of the SGcod parameter
    ProgressionOrder progression_order { LayerResolutionComponentPosition };
    u16 number_of_layers { 0 };
    MultipleComponentTransformationType multiple_component_transformation_type { None };

    CodingStyleParameters parameters;
};

static ErrorOr<CodingStyleDefault> read_coding_style_default(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };

    CodingStyleDefault cod;

    u8 Scod = TRY(stream.read_value<u8>());
    cod.has_explicit_precinct_size = Scod & 1;
    cod.may_use_SOP_marker = Scod & 2;
    cod.shall_use_EPH_marker = Scod & 4;

    u32 SGcod = TRY(stream.read_value<BigEndian<u32>>());
    u8 progression_order = SGcod >> 24;
    if (progression_order > 4)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid progression order");
    cod.progression_order = static_cast<CodingStyleDefault::ProgressionOrder>(progression_order);

    cod.number_of_layers = (SGcod >> 8) & 0xFFFF;
    if (cod.number_of_layers == 0)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid number of layers");

    u8 multiple_component_transformation_type = SGcod & 0xFF;
    if (multiple_component_transformation_type > 1)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid multiple component transformation type");
    cod.multiple_component_transformation_type = static_cast<CodingStyleDefault::MultipleComponentTransformationType>(multiple_component_transformation_type);

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: COD marker segment: has_explicit_precinct_size={}, may_use_SOP_marker={}, shall_use_EPH_marker={}", cod.has_explicit_precinct_size, cod.may_use_SOP_marker, cod.shall_use_EPH_marker);
    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: COD marker segment: progression_order={}, number_of_layers={}, multiple_component_transformation_type={}", (int)cod.progression_order, cod.number_of_layers, (int)cod.multiple_component_transformation_type);

    cod.parameters = TRY(read_coding_style_parameters(data.slice(stream.offset()), "COD"sv, cod.has_explicit_precinct_size));

    return cod;
}

// A.6.2 Coding style component (COC)
struct CodingStyleComponent {
    u16 component_index { 0 }; // "Ccoc" in spec.

    // Table A.23 – Coding style parameter values for the Scoc parameter
    bool has_explicit_precinct_size { false }; // "Scoc" in spec.

    CodingStyleParameters parameters;
};

static ErrorOr<CodingStyleComponent> read_coding_style_component(ReadonlyBytes data, size_t number_of_components)
{
    FixedMemoryStream stream { data };

    // Table A.22 – Coding style component parameter values
    CodingStyleComponent coc;
    if (number_of_components < 257)
        coc.component_index = TRY(stream.read_value<u8>());
    else
        coc.component_index = TRY(stream.read_value<BigEndian<u16>>());

    u8 Scoc = TRY(stream.read_value<u8>());
    coc.has_explicit_precinct_size = Scoc & 1;

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: COC marker segment: component_index={}", coc.component_index);
    coc.parameters = TRY(read_coding_style_parameters(data.slice(TRY(stream.tell())), "COC"sv, coc.has_explicit_precinct_size));

    return coc;
}

// A.6.3 Region of interest (RGN)
struct RegionOfInterest {
    u16 component_index { 0 }; // "Crgn" in spec.
    // The only valid ROI style in T.800 is 0, so this doesn't "Srgn".
    u8 implicit_roi_shift { 0 }; // "SPrgn" in spec and Table A.26 – Region-of-interest values from SPrgn parameter (Srgn = 0).
};

static ErrorOr<RegionOfInterest> read_region_of_interest(ReadonlyBytes data, size_t number_of_components)
{
    FixedMemoryStream stream { data };

    RegionOfInterest rgn;
    if (number_of_components < 257)
        rgn.component_index = TRY(stream.read_value<u8>());
    else
        rgn.component_index = TRY(stream.read_value<BigEndian<u16>>());

    u8 roi_style = TRY(stream.read_value<u8>());
    if (roi_style != 0)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid ROI style");

    rgn.implicit_roi_shift = TRY(stream.read_value<u8>());

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: RGN marker segment: component_index={}, implicit_roi_shift={}", rgn.component_index, rgn.implicit_roi_shift);

    return rgn;
}

// A.6.4 Quantization default (QCD)
struct QuantizationDefault {
    enum QuantizationStyle {
        NoQuantization = 0,
        ScalarDerived = 1,
        ScalarExpounded = 2,
    };
    QuantizationStyle quantization_style { NoQuantization };
    u8 number_of_guard_bits { 0 };

    struct ReversibleStepSize {
        u8 exponent { 0 };
    };
    struct IrreversibleStepSize {
        u16 mantissa { 0 };
        u8 exponent { 0 };
    };

    // Stores a Vector<ReversibleStepSize> if quantization_style is NoQuantization, and a Vector<IrreversibleStepSize> otherwise.
    // The size of the vector is >= 3*number_of_decomposition_levels + 1 if quantization_style is not ScalarDerived, and 1 otherwise.
    using StepSizeType = Variant<Empty, Vector<ReversibleStepSize>, Vector<IrreversibleStepSize>>;
    StepSizeType step_sizes;
};

static ErrorOr<QuantizationDefault> read_quantization_default(ReadonlyBytes data, StringView marker_name = "QCD"sv)
{
    FixedMemoryStream stream { data };

    QuantizationDefault qcd;

    u8 sqcd = TRY(stream.read_value<u8>());
    u8 quantization_style = sqcd & 0x1F;
    if (quantization_style > 2)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid quantization style");
    qcd.quantization_style = static_cast<QuantizationDefault::QuantizationStyle>(quantization_style);
    qcd.number_of_guard_bits = sqcd >> 5;

    qcd.step_sizes = TRY([&]() -> ErrorOr<QuantizationDefault::StepSizeType> {
        if (quantization_style == QuantizationDefault::NoQuantization) {
            // Table A.29 – Reversible step size values for the SPqcd and SPqcc parameters (reversible transform only)
            if (data.size() < 2)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for QCD marker segment");
            u8 number_of_decomposition_levels = (data.size() - 2) / 3;

            Vector<QuantizationDefault::ReversibleStepSize> reversible_step_sizes;
            for (size_t i = 0; i < 1u + 3u * number_of_decomposition_levels; ++i)
                reversible_step_sizes.append({ static_cast<u8>(TRY(stream.read_value<u8>()) >> 3) });
            return reversible_step_sizes;
        }

        // Table A.30 – Quantization values for the SPqcd and SPqcc parameters (irreversible transformation only)
        if (data.size() < 3)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for QCD marker segment");
        u8 number_of_decomposition_levels = 0;
        if (quantization_style == QuantizationDefault::ScalarExpounded)
            number_of_decomposition_levels = (data.size() - 3) / 6;

        Vector<QuantizationDefault::IrreversibleStepSize> irreversible_step_sizes;
        for (size_t i = 0; i < 1u + 3u * number_of_decomposition_levels; ++i) {
            u16 value = TRY(stream.read_value<BigEndian<u16>>());
            QuantizationDefault::IrreversibleStepSize step_size;
            step_size.mantissa = value & 0x7FF;
            step_size.exponent = value >> 11;
            irreversible_step_sizes.append(step_size);
        }
        return irreversible_step_sizes;
    }());

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: quantization_style={}, number_of_guard_bits={}", marker_name, (int)qcd.quantization_style, qcd.number_of_guard_bits);
    qcd.step_sizes.visit(
        [](Empty) { VERIFY_NOT_REACHED(); },
        [&](Vector<QuantizationDefault::ReversibleStepSize> const& step_sizes) {
            dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: {} step sizes:", marker_name, step_sizes.size());
            for (auto [i, step_size] : enumerate(step_sizes)) {
                dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: step_size[{}]: exponent={}", marker_name, i, step_size.exponent);
            }
        },
        [&](Vector<QuantizationDefault::IrreversibleStepSize> const& step_sizes) {
            dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: {} step sizes:", marker_name, step_sizes.size());
            for (auto [i, step_size] : enumerate(step_sizes)) {
                dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: {} marker segment: step_size[{}]: mantissa={}, exponent={}", marker_name, i, step_size.mantissa, step_size.exponent);
            }
        });

    return qcd;
}

// A.6.5 Quantization component (QCC)
struct QuantizationComponent {
    u16 component_index { 0 }; // "Cqcc" in spec.
    QuantizationDefault qcd;
};

static ErrorOr<QuantizationComponent> read_quantization_component(ReadonlyBytes data, size_t number_of_components)
{
    FixedMemoryStream stream { data };

    QuantizationComponent qcc;
    if (number_of_components < 257)
        qcc.component_index = TRY(stream.read_value<u8>());
    else
        qcc.component_index = TRY(stream.read_value<BigEndian<u16>>());

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: QCC marker segment: component_index={}", qcc.component_index);
    qcc.qcd = TRY(read_quantization_default(data.slice(TRY(stream.tell())), "QCC"sv));

    return qcc;
}

// A.6.6 Progression order change (POC)
struct ProgressionOrderChange {
    struct Entry {
        // Start indices are all inclusive, end indices all exclusive.

        // layer_start is implicitly always 0 and not stored in the codestream.
        u8 resolution_level_start { 0 }; // "RSpoc" in spec.
        u16 component_start { 0 };       // "CSpoc" in spec.

        u16 layer_end { 0 };           // "LYEpoc" in spec.
        u8 resolution_level_end { 0 }; // "REpoc" in spec.
        u16 component_end { 0 };       // "CEpoc" in spec.

        CodingStyleDefault::ProgressionOrder progression_order { CodingStyleDefault::ProgressionOrder::LayerResolutionComponentPosition }; // "Ppoc" in spec.
    };
    Vector<Entry> entries;
};

static ErrorOr<ProgressionOrderChange> read_progression_order_change(ReadonlyBytes data, size_t number_of_components)
{
    FixedMemoryStream stream { data };

    auto entry_size = number_of_components < 257 ? 7 : 9;
    if (data.size() % entry_size != 0)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid POC marker segment size");
    auto entry_count = data.size() / entry_size;

    ProgressionOrderChange poc;
    TRY(poc.entries.try_ensure_capacity(entry_count));
    for (size_t i = 0; i < entry_count; ++i) {
        // Table A.32 – Progression order change, tile parameter values
        ProgressionOrderChange::Entry entry;

        entry.resolution_level_start = TRY(stream.read_value<u8>());
        if (entry.resolution_level_start > 32)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid resolution level start in POC");

        if (number_of_components < 257)
            entry.component_start = TRY(stream.read_value<u8>());
        else
            entry.component_start = TRY(stream.read_value<BigEndian<u16>>());
        if (entry.component_start > 16'383)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid component start in POC");

        entry.layer_end = TRY(stream.read_value<BigEndian<u16>>());
        if (entry.layer_end == 0)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid layer end in POC");

        entry.resolution_level_end = TRY(stream.read_value<u8>());
        if (entry.resolution_level_end <= entry.resolution_level_start || entry.resolution_level_end > 33)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid resolution level end in POC");

        if (number_of_components < 257)
            entry.component_end = TRY(stream.read_value<u8>());
        else
            entry.component_end = TRY(stream.read_value<BigEndian<u16>>());
        if (entry.component_end == 0)
            entry.component_end = 256; // "(0 is interpreted as 256)"
        if (entry.component_end <= entry.component_start || entry.component_end > 16'384)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid component end in POC");

        u8 progression_order = TRY(stream.read_value<u8>());
        if (progression_order > 4)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid progression order in POC");
        entry.progression_order = static_cast<CodingStyleDefault::ProgressionOrder>(progression_order);

        dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: POC marker segment: entry[{}]: resolution_level_start={}, component_start={}, layer_end={}, resolution_level_end={}, component_end={}, progression_order={}",
            i, entry.resolution_level_start, entry.component_start, entry.layer_end, entry.resolution_level_end, entry.component_end, (int)entry.progression_order);

        poc.entries.append(entry);
    }

    return poc;
}

// A.9.2 Comment (COM)
struct Comment {
    enum CommentType {
        Binary = 0,
        ISO_IEC_8859_15 = 1,
    };
    CommentType type { Binary }; // "Rcom" in spec.
    ReadonlyBytes data;
};

static ErrorOr<Comment> read_comment(ReadonlyBytes data)
{
    FixedMemoryStream stream { data };

    Comment com;
    u16 comment_type = TRY(stream.read_value<BigEndian<u16>>());
    if (comment_type > 1)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid comment type");
    com.type = static_cast<Comment::CommentType>(comment_type);
    com.data = data.slice(TRY(stream.tell()));

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: COM marker segment: comment_type={}, size()={}", (int)com.type, com.data.size());
    if (com.type == Comment::ISO_IEC_8859_15)
        dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: COM marker segment, ISO/IEC 8859-15 text: '{}'", TRY(TextCodec::decoder_for("ISO-8859-15"sv)->to_utf8(StringView { com.data })));

    return com;
}

struct TilePartData {
    StartOfTilePart sot;
    Vector<Comment> coms;
    ReadonlyBytes data;
};

struct DecodedCodeBlock {
    IntRect rect; // Confined to sub-band rect.

    // Transient state used to read packet headers.

    // B.10.4 Code-block inclusion
    bool is_included { false };

    // B.10.7.1 Single codeword segment
    // "Lblock is a code-block state variable. [...] The value of Lblock is initially set to three."
    u32 Lblock { 3 };

    // Becomes true when the first packet including this codeblock is read.
    bool has_been_included_in_previous_packet { false };

    // Data read from packet headers.

    // B.10.5 Zero bit-plane information
    // "the number of missing most significant bit-planes, P, may vary from code-block to code-block;
    //  these missing bit-planes are all taken to be zero."
    u32 p { 0 };

    struct Layer {
        struct Segment {
            ReadonlyBytes data;
            u32 index { 0 };
            int number_of_passes { 0 };
        };
        Vector<Segment, 1> segments;
    };
    Vector<Layer, 1> layers;

    u32 number_of_coding_passes() const
    {
        u32 total = 0;
        for (auto const& layer : layers)
            for (auto const& segment : layer.segments)
                total += segment.number_of_passes;
        return total;
    }

    u32 number_of_coding_passes_in_segment(u32 segment_index) const
    {
        u32 total = 0;
        for (auto const& layer : layers) {
            for (auto const& segment : layer.segments) {
                if (segment.index == segment_index)
                    total += segment.number_of_passes;
            }
        }
        return total;
    }

    Optional<u32> highest_segment_index() const
    {
        Optional<u32> highest_index;
        for (auto const& layer : layers) {
            for (auto const& segment : layer.segments)
                highest_index = max(highest_index.value_or(segment.index), segment.index);
        }
        return highest_index;
    }

    ErrorOr<Vector<ReadonlyBytes, 1>> segments_for_all_layers(ByteBuffer& maybe_storage) const
    {
        Vector<Vector<ReadonlyBytes, 1>, 1> all_segment_parts_for_segment;
        all_segment_parts_for_segment.resize(highest_segment_index().value_or(0) + 1);

        for (auto const& layer : layers)
            for (auto const& segment : layer.segments)
                TRY(all_segment_parts_for_segment[segment.index].try_append(segment.data));

        // Copy segments with multiple parts into consecutive storage.
        size_t total_scratch_size = 0;
        for (auto const& segment_parts : all_segment_parts_for_segment) {
            if (segment_parts.size() > 1) {
                for (auto const& segment_part : segment_parts)
                    total_scratch_size += segment_part.size();
            }
        }

        if (total_scratch_size > 0)
            maybe_storage = TRY(ByteBuffer::create_uninitialized(total_scratch_size));

        Vector<ReadonlyBytes, 1> all_segments;
        size_t scratch_offset = 0;
        for (auto& segment_parts : all_segment_parts_for_segment) {
            if (segment_parts.size() == 1) {
                TRY(all_segments.try_append(segment_parts[0]));
                continue;
            }

            auto start = scratch_offset;
            for (auto const& segment_part : segment_parts) {
                memcpy(maybe_storage.offset_pointer(scratch_offset), segment_part.data(), segment_part.size());
                scratch_offset += segment_part.size();
            }
            TRY(all_segments.try_append(maybe_storage.bytes().slice(start, scratch_offset - start)));
        }
        return all_segments;
    }
};

struct DecodedPrecinct {
    IntRect rect; // NOT confined to sub-band rect.

    int num_code_blocks_wide { 0 };
    int num_code_blocks_high { 0 };
    Vector<DecodedCodeBlock> code_blocks;

    // Transient state used to read packet headers.
    Optional<JPEG2000::TagTree> code_block_inclusion_tree;
    Optional<JPEG2000::TagTree> p_tree;
};

struct DecodedSubBand {
    IntRect rect;

    // These are the same for all three sub-bands at a given resolution level.
    int num_precincts_wide { 0 };
    int num_precincts_high { 0 };

    Vector<DecodedPrecinct> precincts;

    // Valid after bitplane decoding. rect.width() * rect.height() == coefficients.size().
    Vector<float> coefficients;
};

struct DecodedTileComponent {
    IntRect rect;
    DecodedSubBand nLL; // N_L LL in the spec, corresponds to resolution level 0.

    using DecodedSubBands = Array<DecodedSubBand, 3>; // Ordered HL, LH, HH.
    Vector<DecodedSubBands> decompositions;
    static constexpr Array SubBandOrder { JPEG2000::SubBand::HorizontalHighpassVerticalLowpass, JPEG2000::SubBand::HorizontalLowpassVerticalHighpass, JPEG2000::SubBand::HorizontalHighpassVerticalHighpass };

    // Valid after IDWT.
    Vector<float> samples;
};

struct TileData {
    // Data from codestream markers.
    Optional<CodingStyleDefault> cod;
    Vector<CodingStyleComponent> cocs;
    Vector<RegionOfInterest> rgns;
    Optional<QuantizationDefault> qcd;
    Vector<QuantizationComponent> qccs;
    Optional<ProgressionOrderChange> poc;
    Vector<TilePartData> tile_parts;

    // Data used during decoding.
    IntRect rect;
    Vector<DecodedTileComponent> components;
    Vector<Vector<float>> channels;
    Vector<ImageAndTileSize::ComponentInformation> channel_information;

    // FIXME: This will have to move and be reorganized come POC support.
    OwnPtr<JPEG2000::ProgressionIterator> progression_iterator;
};

enum class ColorSpace {
    sRGB,
    Gray,
    CMYK,
    Unsupported,
};

struct JPEG2000LoadingContext {
    enum class State {
        NotDecoded = 0,
        DecodedImage,
        Error,
    };
    State state { State::NotDecoded };
    ReadonlyBytes codestream_data;
    size_t codestream_cursor { 0 };
    JPEG2000DecoderOptions options;

    Optional<ISOBMFF::JPEG2000ColorSpecificationBox const&> color_box; // This is always set for box-based files.

    Optional<ISOBMFF::JPEG2000PaletteBox const&> palette_box;
    Optional<ISOBMFF::JPEG2000ComponentMappingBox const&> component_mapping_box;
    Optional<ISOBMFF::JPEG2000ChannelDefinitionBox const&> channel_definition_box;

    IntSize size;

    ISOBMFF::BoxList boxes;

    // Data from marker segments:
    ImageAndTileSize siz;
    CodingStyleDefault cod;
    Vector<CodingStyleComponent> cocs;
    Vector<RegionOfInterest> rgns;
    QuantizationDefault qcd;
    Vector<QuantizationComponent> qccs;
    Optional<ProgressionOrderChange> poc;
    Vector<Comment> coms;
    Vector<TileData> tiles;

    // Valid after headers have been decoded.
    // The awkward `color_space_error` is so that determine_color_space() can always succeed and
    // e.g. `file` can return data for JPEG2000s even if we can't decode the image data due to not
    // yet supporting its colorspace.
    ColorSpace color_space { ColorSpace::Unsupported };
    Optional<Error> color_space_error;

    // Valid once `state` is StateDecodedImage.
    RefPtr<Bitmap> bitmap;
    RefPtr<CMYKBitmap> cmyk_bitmap;

    CodingStyleParameters const& coding_style_parameters_for_component(TileData const& tile, size_t component_index) const
    {
        // Tile-part COC > Tile-part COD > Main COC > Main COD
        for (auto const& coc : tile.cocs) {
            if (coc.component_index == component_index)
                return coc.parameters;
        }
        if (tile.cod.has_value())
            return tile.cod->parameters;

        for (auto const& coc : cocs) {
            if (coc.component_index == component_index)
                return coc.parameters;
        }
        return cod.parameters;
    }

    QuantizationDefault const& quantization_parameters_for_component(TileData const& tile, size_t component_index)
    {
        // Tile-part QCC > Tile-part QCD > Main QCC > Main QCD
        for (auto const& qcc : tile.qccs) {
            if (qcc.component_index == component_index)
                return qcc.qcd;
        }
        if (tile.qcd.has_value())
            return tile.qcd.value();

        for (auto const& qcc : qccs) {
            if (qcc.component_index == component_index)
                return qcc.qcd;
        }
        return qcd;
    }

    ErrorOr<JPEG2000::ProgressionData> next_progression_data(TileData& tile) const
    {
        JPEG2000::ProgressionData progression_data;

        auto progression_data_has_packet = [&](JPEG2000::ProgressionData const& progression_data) {
            if (progression_data.resolution_level > coding_style_parameters_for_component(tile, progression_data.component).number_of_decomposition_levels)
                return false;

            // "It can happen that numprecincts is 0 for a particular tile-component and resolution level. When this happens, there are no
            //  packets for this tile-component and resolution level."
            // `num_precincts_wide` and `num_precincts_high` are the same for all sub-bands at a given resolution level, so it's
            // enough to only check the first.
            auto& component = tile.components[progression_data.component];
            auto& sub_band_data = progression_data.resolution_level == 0 ? component.nLL : component.decompositions[progression_data.resolution_level - 1][0];
            if (sub_band_data.num_precincts_wide == 0 || sub_band_data.num_precincts_high == 0)
                return false;

            return true;
        };

        do {
            if (!tile.progression_iterator->has_next())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: No more progression orders but packets left");
            progression_data = tile.progression_iterator->next();
        } while (!progression_data_has_packet(progression_data));

        return progression_data;
    }
};

struct MarkerSegment {
    u16 marker;

    // OptionalNone for markers that don't have data.
    // For markers that do have data, this does not include the marker length data. (`data.size() + 2` is the value of the marker length field.)
    Optional<ReadonlyBytes> data;
};

static ErrorOr<u16> peek_marker(ReadonlyBytes data)
{
    if (2 > data.size())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for marker");
    return *reinterpret_cast<BigEndian<u16> const*>(data.data());
}

static ErrorOr<u16> peek_marker(JPEG2000LoadingContext& context)
{
    return peek_marker(context.codestream_data.slice(context.codestream_cursor));
}

static ErrorOr<MarkerSegment> read_marker_at_cursor(JPEG2000LoadingContext& context)
{
    u16 marker = TRY(peek_marker(context));
    // "All markers with the marker code between 0xFF30 and 0xFF3F have no marker segment parameters. They shall be skipped by the decoder."
    // "The SOC, SOD and EOC are delimiting markers not marker segments, and have no explicit length information or other parameters."
    bool is_marker_segment = !(marker >= 0xFF30 && marker <= 0xFF3F) && marker != J2K_SOC && marker != J2K_SOD && marker != J2K_EOC;

    MarkerSegment marker_segment;
    marker_segment.marker = marker;

    if (is_marker_segment) {
        if (context.codestream_cursor + 4 > context.codestream_data.size())
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for marker segment length");
        u16 marker_length = *reinterpret_cast<BigEndian<u16> const*>(context.codestream_data.data() + context.codestream_cursor + 2);
        if (marker_length < 2)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Marker segment length too small");
        if (context.codestream_cursor + 2 + marker_length > context.codestream_data.size())
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for marker segment data");
        marker_segment.data = ReadonlyBytes { context.codestream_data.data() + context.codestream_cursor + 4, marker_length - 2u };
    }

    context.codestream_cursor += 2;
    if (is_marker_segment)
        context.codestream_cursor += 2 + marker_segment.data->size();

    return marker_segment;
}

static ErrorOr<void> parse_codestream_main_header(JPEG2000LoadingContext& context)
{
    // Figure A.3 – Construction of the main header
    // "Required as the first marker"
    auto marker = TRY(read_marker_at_cursor(context));
    if (marker.marker != J2K_SOC)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected SOC marker");

    // "Required as the second marker segment"
    marker = TRY(read_marker_at_cursor(context));
    if (marker.marker != J2K_SIZ)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected SIZ marker");
    context.siz = TRY(read_image_and_tile_size(marker.data.value()));

    bool saw_COD_marker = false;
    bool saw_QCD_marker = false;
    while (true) {
        u16 marker = TRY(peek_marker(context));
        switch (marker) {
        case J2K_COD:
        case J2K_COC:
        case J2K_QCD:
        case J2K_QCC:
        case J2K_RGN:
        case J2K_POC:
        case J2K_PPM:
        case J2K_TLM:
        case J2K_PLM:
        case J2K_CRG:
        case J2K_COM: {
            auto marker = TRY(read_marker_at_cursor(context));
            if (marker.marker == J2K_COD) {
                if (saw_COD_marker)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple COD markers in main header");
                context.cod = TRY(read_coding_style_default(marker.data.value()));
                saw_COD_marker = true;
            } else if (marker.marker == J2K_COC) {
                context.cocs.append(TRY(read_coding_style_component(marker.data.value(), context.siz.components.size())));
            } else if (marker.marker == J2K_QCD) {
                if (saw_QCD_marker)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple QCD markers in main header");
                context.qcd = TRY(read_quantization_default(marker.data.value()));
                saw_QCD_marker = true;
            } else if (marker.marker == J2K_QCC) {
                context.qccs.append(TRY(read_quantization_component(marker.data.value(), context.siz.components.size())));
            } else if (marker.marker == J2K_RGN) {
                context.rgns.append(TRY(read_region_of_interest(marker.data.value(), context.siz.components.size())));
            } else if (marker.marker == J2K_POC) {
                if (context.poc.has_value())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple POC markers in main header");
                context.poc = TRY(read_progression_order_change(marker.data.value(), context.siz.components.size()));
            } else if (marker.marker == J2K_PPM) {
                // FIXME: Implement. (I haven't yet found a way to generate files that use this.)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: PPM marker not yet implemented");
            } else if (marker.marker == J2K_TLM) {
                // TLM describes tile-part lengths, for random access. They can be ignored for now.
            } else if (marker.marker == J2K_PLM) {
                // PLM describes packet lengths, for random access. They can be ignored for now.
            } else if (marker.marker == J2K_CRG) {
                // FIXME: Implement. (I haven't yet found a way to generate files that use this.)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: CRG marker not yet implemented");
            } else if (marker.marker == J2K_COM) {
                context.coms.append(TRY(read_comment(marker.data.value())));
            } else {
                VERIFY_NOT_REACHED();
            }
            break;
        }
        case J2K_SOT: {
            // SOT terminates the main header.
            // A.4.2: "There shall be at least one SOT in a codestream."
            if (!saw_COD_marker)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Required COD marker not present in main header");
            if (!saw_QCD_marker)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Required QCD marker not present in main header");

            // A.6.4: "there is not necessarily a correspondence with the number of sub-bands present because the sub-bands
            //         can be truncated with no requirement to correct [the QCD] marker segment."
            size_t step_sizes_count = context.qcd.step_sizes.visit(
                [](Empty) -> size_t { VERIFY_NOT_REACHED(); },
                [](Vector<QuantizationDefault::ReversibleStepSize> const& step_sizes) { return step_sizes.size(); },
                [](Vector<QuantizationDefault::IrreversibleStepSize> const& step_sizes) { return step_sizes.size(); });
            // FIXME: What if number_of_decomposition_levels is in context.cocs and varies by component?
            if (context.qcd.quantization_style != QuantizationDefault::ScalarDerived && step_sizes_count < context.cod.parameters.number_of_decomposition_levels * 3u + 1u)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough step sizes for number of decomposition levels");

            return {};
        }
        default:
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Unexpected marker in main header");
        }
    }
}

static ErrorOr<void> parse_codestream_tile_header(JPEG2000LoadingContext& context)
{
    // Figure A.4 – Construction of the first tile-part header of a given tile
    // Figure A.5 – Construction of a non-first tile-part header

    // "Required as the first marker segment of every tile-part header"
    auto tile_start = context.codestream_cursor;
    auto marker = TRY(read_marker_at_cursor(context));
    if (marker.marker != J2K_SOT)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected SOT marker");
    auto start_of_tile = TRY(read_start_of_tile_part(marker.data.value()));

    context.tiles.resize(max(context.tiles.size(), (size_t)start_of_tile.tile_index + 1));
    auto& tile = context.tiles[start_of_tile.tile_index];

    if (tile.tile_parts.size() != start_of_tile.tile_part_index)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Tile part index out of order");
    tile.tile_parts.append({});
    auto& tile_part = tile.tile_parts.last();
    tile_part.sot = start_of_tile;

    bool found_start_of_data = false;
    while (!found_start_of_data) {
        u16 marker = TRY(peek_marker(context));
        switch (marker) {
        case J2K_SOD:
            // "Required as the last marker segment of every tile-part header"
            context.codestream_cursor += 2;
            found_start_of_data = true;
            break;
        case J2K_COD:
        case J2K_COC:
        case J2K_QCD:
        case J2K_QCC:
        case J2K_RGN:
            if (start_of_tile.tile_part_index != 0)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: COD, COC, QCD, QCC, RGN markers are only valid in the first tile-part header");
            [[fallthrough]];
        case J2K_POC:
        case J2K_PPT:
        case J2K_PLT:
        case J2K_COM: {
            auto marker = TRY(read_marker_at_cursor(context));
            if (marker.marker == J2K_COD) {
                if (tile.cod.has_value())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple COD markers in tile header");
                tile.cod = TRY(read_coding_style_default(marker.data.value()));
            } else if (marker.marker == J2K_COC) {
                tile.cocs.append(TRY(read_coding_style_component(marker.data.value(), context.siz.components.size())));
            } else if (marker.marker == J2K_QCD) {
                if (tile.qcd.has_value())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple QCD markers in tile header");
                tile.qcd = TRY(read_quantization_default(marker.data.value()));
            } else if (marker.marker == J2K_QCC) {
                tile.qccs.append(TRY(read_quantization_component(marker.data.value(), context.siz.components.size())));
            } else if (marker.marker == J2K_RGN) {
                tile.rgns.append(TRY(read_region_of_interest(marker.data.value(), context.siz.components.size())));
            } else if (marker.marker == J2K_POC) {
                if (tile.poc.has_value())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple POC markers in tile header");
                tile.poc = TRY(read_progression_order_change(marker.data.value(), context.siz.components.size()));
            } else if (marker.marker == J2K_PPT) {
                // FIXME: Implement. (I haven't yet found a way to generate files that use this.)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: PPT marker not yet implemented");
            } else if (marker.marker == J2K_PLT) {
                // PLT describes packet lengths, for random access. They can be ignored for now.
            } else if (marker.marker == J2K_COM) {
                tile_part.coms.append(TRY(read_comment(marker.data.value())));
            } else {
                VERIFY_NOT_REACHED();
            }
            break;
        }
        default:
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Unexpected marker in tile header");
        }
    }

    u32 tile_bitstream_length;
    if (start_of_tile.tile_part_length == 0) {
        // Leave room for EOC marker.
        if (context.codestream_data.size() - context.codestream_cursor < 2)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for EOC marker");
        tile_bitstream_length = context.codestream_data.size() - context.codestream_cursor - 2;
    } else {
        u32 tile_header_length = context.codestream_cursor - tile_start;
        if (start_of_tile.tile_part_length < tile_header_length)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid tile part length");
        tile_bitstream_length = start_of_tile.tile_part_length - tile_header_length;
    }

    if (context.codestream_cursor + tile_bitstream_length > context.codestream_data.size())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for tile bitstream");
    tile_part.data = context.codestream_data.slice(context.codestream_cursor, tile_bitstream_length);

    context.codestream_cursor += tile_bitstream_length;
    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: Tile bitstream length: {}", tile_bitstream_length);

    return {};
}

static ErrorOr<void> parse_codestream_tile_headers(JPEG2000LoadingContext& context)
{
    while (true) {
        auto marker = TRY(peek_marker(context));
        if (marker == J2K_EOC) {
            context.codestream_cursor += 2;
            break;
        }
        TRY(parse_codestream_tile_header(context));
    }

    if (context.codestream_cursor < context.codestream_data.size())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Unexpected data after EOC marker");
    return {};
}

static ErrorOr<void> decode_jpeg2000_header(JPEG2000LoadingContext& context, ReadonlyBytes data)
{
    if (!JPEG2000ImageDecoderPlugin::sniff(data))
        return Error::from_string_literal("JPEG2000LoadingContext: Invalid JPEG2000 header");

    if (data.starts_with(marker_id_string)) {
        context.codestream_data = data;
        TRY(parse_codestream_main_header(context));
        context.size = { context.siz.width, context.siz.height };
        return {};
    }

    auto reader = TRY(Gfx::ISOBMFF::Reader::create(TRY(try_make<FixedMemoryStream>(data))));
    context.boxes = TRY(reader.read_entire_file());

    dbgln_if(JPEG2000_DEBUG, "Embedded ISOBMFF boxes:");
    if constexpr (JPEG2000_DEBUG) {
        for (auto& box : context.boxes)
            box->dump();
    }

    // I.2.2 File organization
    // "A particular order of those boxes in the file is not generally implied. However, the JPEG 2000 Signature box
    //  shall be the first box in a JP2 file, the File Type box shall immediately follow the JPEG 2000 Signature box
    //  and the JP2 Header box shall fall before the Contiguous Codestream box."
    if (context.boxes.size() < 4)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected at least four boxes");

    // Required toplevel boxes: signature box, file type box, jp2 header box, contiguous codestream box.

    if (context.boxes[0]->box_type() != ISOBMFF::BoxType::JPEG2000SignatureBox)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected JPEG2000SignatureBox as first box");
    if (context.boxes[1]->box_type() != ISOBMFF::BoxType::FileTypeBox)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected FileTypeBox as second box");

    Optional<size_t> jp2_header_box_index;
    Optional<size_t> contiguous_codestream_box_index;
    for (size_t i = 2; i < context.boxes.size(); ++i) {
        if (context.boxes[i]->box_type() == ISOBMFF::BoxType::JPEG2000HeaderBox) {
            // "Within a JP2 file, there shall be one and only one JP2 Header box."
            if (jp2_header_box_index.has_value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple JP2 Header boxes");
            jp2_header_box_index = i;
        }
        if (context.boxes[i]->box_type() == ISOBMFF::BoxType::JPEG2000ContiguousCodestreamBox && !contiguous_codestream_box_index.has_value()) {
            // "a conforming reader shall ignore all codestreams after the first codestream found in the file.
            //  Contiguous Codestream boxes may be found anywhere in the file except before the JP2 Header box."
            contiguous_codestream_box_index = i;
            if (!jp2_header_box_index.has_value() || contiguous_codestream_box_index.value() < jp2_header_box_index.value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: JP2 Header box must come before Contiguous Codestream box");
        }
    }

    if (!jp2_header_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected JP2 Header box");
    if (!contiguous_codestream_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected Contiguous Codestream box");

    // FIXME: JPEG2000ContiguousCodestreamBox makes a copy of the codestream data. That's too heavy for header scanning.
    // Add a mode to ISOBMFF::Reader where it only stores offsets for the codestream data and the ICC profile.
    auto const& codestream_box = static_cast<ISOBMFF::JPEG2000ContiguousCodestreamBox const&>(*context.boxes[contiguous_codestream_box_index.value()]);
    context.codestream_data = codestream_box.codestream.bytes();

    // Required child boxes of the jp2 header box: image header box, color box.

    Optional<size_t> image_header_box_index;
    Optional<size_t> color_header_box_index;
    Optional<size_t> palette_box_index;
    Optional<size_t> component_mapping_box_index;
    Optional<size_t> channel_definition_box_index;
    auto const& header_box = static_cast<ISOBMFF::JPEG2000HeaderBox const&>(*context.boxes[jp2_header_box_index.value()]);
    for (size_t i = 0; i < header_box.child_boxes().size(); ++i) {
        auto const& subbox = header_box.child_boxes()[i];
        if (subbox->box_type() == ISOBMFF::BoxType::JPEG2000ImageHeaderBox) {
            if (image_header_box_index.has_value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple Image Header boxes");
            image_header_box_index = i;
        }
        if (subbox->box_type() == ISOBMFF::BoxType::JPEG2000ColorSpecificationBox) {
            // T.800 says there should be just one 'colr' box, but T.801 allows several and says to pick the one with highest precedence.
            bool use_this_color_box;
            if (!color_header_box_index.has_value()) {
                use_this_color_box = true;
            } else {
                auto const& new_header_box = static_cast<ISOBMFF::JPEG2000ColorSpecificationBox const&>(*header_box.child_boxes()[i]);
                auto const& current_color_box = static_cast<ISOBMFF::JPEG2000ColorSpecificationBox const&>(*header_box.child_boxes()[color_header_box_index.value()]);
                use_this_color_box = new_header_box.precedence > current_color_box.precedence;
            }

            if (use_this_color_box)
                color_header_box_index = i;
        }
        if (subbox->box_type() == ISOBMFF::BoxType::JPEG2000PaletteBox) {
            // T.800, I.5.3.4 Palette box
            // "There shall be at most one Palette box inside a JP2 Header box."
            if (palette_box_index.has_value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple Palette boxes");
            palette_box_index = i;
        }
        if (subbox->box_type() == ISOBMFF::BoxType::JPEG2000ComponentMappingBox) {
            // T.800, I.5.3.5 Component Mapping box
            // "There shall be at most one Component Mapping box inside a JP2 Header box."
            if (component_mapping_box_index.has_value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple Component Mapping boxes");
            component_mapping_box_index = i;
        }
        if (subbox->box_type() == ISOBMFF::BoxType::JPEG2000ChannelDefinitionBox) {
            // T.800, I.5.3.6 Channel Definition box
            // "There shall be at most one Channel Definition box inside a JP2 Header box."
            if (channel_definition_box_index.has_value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple Channel Definition boxes");
            channel_definition_box_index = i;
        }
    }

    if (!image_header_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected Image Header box");
    if (!color_header_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected Color Specification box");

    auto const& image_header_box = static_cast<ISOBMFF::JPEG2000ImageHeaderBox const&>(*header_box.child_boxes()[image_header_box_index.value()]);
    context.size = { image_header_box.width, image_header_box.height };

    if (image_header_box.compression_type != ISOBMFF::JPEG2000ImageHeaderBox::CompressionType::Default)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Decoding of non-jpeg2000 data embedded in jpeg2000 files is not implemented");

    context.color_box = static_cast<ISOBMFF::JPEG2000ColorSpecificationBox const&>(*header_box.child_boxes()[color_header_box_index.value()]);

    // "If the JP2 Header box contains a Palette box, then it shall also contain a Component Mapping box.
    //  If the JP2 Header box does not contain a Palette box, then it shall not contain a Component Mapping box."
    // This is violated in practice though; some files have a Palette box without a Component Mapping box.
    // So check for something weaker.
    if (!palette_box_index.has_value() && component_mapping_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Component Mapping should not be present without Palette box");

    if (palette_box_index.has_value())
        context.palette_box = static_cast<ISOBMFF::JPEG2000PaletteBox const&>(*header_box.child_boxes()[palette_box_index.value()]);
    if (component_mapping_box_index.has_value())
        context.component_mapping_box = static_cast<ISOBMFF::JPEG2000ComponentMappingBox const&>(*header_box.child_boxes()[component_mapping_box_index.value()]);
    if (channel_definition_box_index.has_value())
        context.channel_definition_box = static_cast<ISOBMFF::JPEG2000ChannelDefinitionBox const&>(*header_box.child_boxes()[channel_definition_box_index.value()]);

    TRY(parse_codestream_main_header(context));

    auto size_from_siz = IntSize { context.siz.width, context.siz.height };
    if (size_from_siz != context.size) {
        // FIXME: If this is common, warn and use size from SIZ marker.
        dbgln("JPEG2000ImageDecoderPlugin: Image size from SIZ marker ({}) does not match image size from JP2 header ({})", size_from_siz, context.size);
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Image size from SIZ marker does not match image size from JP2 header");
    }

    return {};
}

static IntRect aligned_enclosing_rect(IntRect outer_rect, IntRect inner_rect, int width_increment, int height_increment)
{
    int new_x = (inner_rect.x() / width_increment) * width_increment;
    int new_y = (inner_rect.y() / height_increment) * height_increment;
    int new_right = inner_rect.width() == 0 ? new_x : ceil_div(inner_rect.right(), width_increment) * width_increment;
    int new_bottom = inner_rect.height() == 0 ? new_y : ceil_div(inner_rect.bottom(), height_increment) * height_increment;
    return IntRect::intersection(outer_rect, IntRect::from_two_points({ new_x, new_y }, { new_right, new_bottom }));
}

static ErrorOr<void> compute_decoding_metadata(JPEG2000LoadingContext& context)
{
    auto make_precinct = [&](DecodedSubBand const& sub_band, IntRect precinct_rect, int xcb_prime, int ycb_prime) -> ErrorOr<DecodedPrecinct> {
        auto rect_covered_by_codeblocks = aligned_enclosing_rect(precinct_rect, sub_band.rect, 1 << xcb_prime, 1 << ycb_prime);
        auto num_code_blocks_wide = rect_covered_by_codeblocks.width() / (1 << xcb_prime);
        auto num_code_blocks_high = rect_covered_by_codeblocks.height() / (1 << ycb_prime);

        DecodedPrecinct precinct;
        precinct.rect = precinct_rect;
        precinct.num_code_blocks_wide = num_code_blocks_wide;
        precinct.num_code_blocks_high = num_code_blocks_high;
        precinct.code_blocks.resize(num_code_blocks_wide * num_code_blocks_high);

        dbgln_if(JPEG2000_DEBUG, "Precinct rect: {}, num_code_blocks_wide: {}, num_code_blocks_high: {}", precinct.rect, num_code_blocks_wide, num_code_blocks_high);

        for (auto [code_block_index, current_block] : enumerate(precinct.code_blocks)) {
            size_t code_block_x = code_block_index % num_code_blocks_wide;
            size_t code_block_y = code_block_index / num_code_blocks_wide;

            auto code_block_rect = IntRect { { code_block_x * (1 << xcb_prime), code_block_y * (1 << ycb_prime) }, { 1 << xcb_prime, 1 << ycb_prime } };
            code_block_rect.set_location(code_block_rect.location() + rect_covered_by_codeblocks.location());

            // B.7 Division of the sub-bands into code-blocks
            // "NOTE – Code-blocks in the partition may extend beyond the boundaries of the sub-band coefficients. When this happens, only the
            //  coefficients lying within the sub-band are coded using the method described in Annex D. The first stripe coded using this method
            //  corresponds to the first four rows of sub-band coefficients in the code-block or to as many such rows as are present."
            current_block.rect = code_block_rect.intersected(sub_band.rect);
        }

        if (!precinct.code_blocks.is_empty()) {
            precinct.code_block_inclusion_tree = TRY(JPEG2000::TagTree::create(num_code_blocks_wide, num_code_blocks_high));
            precinct.p_tree = TRY(JPEG2000::TagTree::create(num_code_blocks_wide, num_code_blocks_high));
        }

        return precinct;
    };

    auto make_sub_band = [&](TileData const& tile, int component_index, DecodedSubBand& sub_band, JPEG2000::SubBand sub_band_type, int r) -> ErrorOr<void> {
        auto const& coding_parameters = context.coding_style_parameters_for_component(tile, component_index);
        auto N_L = coding_parameters.number_of_decomposition_levels;

        // Table F.1 – Decomposition level nb for sub-band b
        // Note: The spec suggests that this ends with n_b = 1, but if N_L is 0, we have 0LL and nothing else.
        auto n_b = [N_L](int r) { return r == 0 ? N_L : (N_L + 1 - r); };

        sub_band.rect = context.siz.reference_grid_coordinates_for_sub_band(tile.rect, component_index, n_b(r), sub_band_type);

        // Compute tile size at resolution level r.
        auto ll_rect = context.siz.reference_grid_coordinates_for_ll_band(tile.rect, component_index, r, N_L);

        dbgln_if(JPEG2000_DEBUG, "Sub-band rect: {}, ll rect {}", sub_band.rect, ll_rect);

        // B.6
        // (B-16)
        int num_precincts_wide = 0;
        int num_precincts_high = 0;
        int PPx = coding_parameters.precinct_sizes[r].PPx;
        int PPy = coding_parameters.precinct_sizes[r].PPy;

        if (ll_rect.width() != 0)
            num_precincts_wide = ceil_div(ll_rect.right(), 1 << PPx) - (ll_rect.left() / (1 << PPx));
        if (ll_rect.height() != 0)
            num_precincts_high = ceil_div(ll_rect.bottom(), 1 << PPy) - (ll_rect.top() / (1 << PPy));

        sub_band.num_precincts_wide = num_precincts_wide;
        sub_band.num_precincts_high = num_precincts_high;

        auto precinct_origin = IntPoint { ll_rect.x() & ~((1 << PPx) - 1), ll_rect.y() & ~((1 << PPy) - 1) };

        if (r > 0) {
            PPx--;
            PPy--;
            precinct_origin /= 2;
        }

        // B.7
        // (B-17)
        // (The r > 0 check was done right above already.)
        int xcb_prime = min(coding_parameters.code_block_width_exponent, PPx);

        // (B-18)
        // (The r > 0 check was done right above already.)
        int ycb_prime = min(coding_parameters.code_block_height_exponent, PPy);

        for (int precinct_y_index = 0; precinct_y_index < num_precincts_high; ++precinct_y_index) {
            for (int precinct_x_index = 0; precinct_x_index < num_precincts_wide; ++precinct_x_index) {
                auto precinct_rect = IntRect({ precinct_x_index * (1 << PPx), precinct_y_index * (1 << PPy), 1 << PPx, 1 << PPy });
                precinct_rect.set_location(precinct_rect.location() + precinct_origin);

                sub_band.precincts.append(TRY(make_precinct(sub_band, precinct_rect, xcb_prime, ycb_prime)));
            }
        }

        return {};
    };

    auto make_component = [&](TileData const& tile, int component_index) -> ErrorOr<DecodedTileComponent> {
        DecodedTileComponent component;
        component.rect = context.siz.reference_grid_coordinates_for_tile_component(tile.rect, component_index);

        dbgln_if(JPEG2000_DEBUG, "making nLL for component {}", component_index);
        TRY(make_sub_band(tile, component_index, component.nLL, JPEG2000::SubBand::HorizontalLowpassVerticalLowpass, 0));

        auto N_L = context.coding_style_parameters_for_component(tile, component_index).number_of_decomposition_levels;
        for (int resolution_level = 1; resolution_level <= N_L; ++resolution_level) {
            DecodedTileComponent::DecodedSubBands sub_bands;
            for (auto [sub_band_index, sub_band] : enumerate(DecodedTileComponent::SubBandOrder)) {
                dbgln_if(JPEG2000_DEBUG, "r {} making sub-band {} for component {}", resolution_level, (int)sub_band, component_index);
                TRY(make_sub_band(tile, component_index, sub_bands[sub_band_index], sub_band, resolution_level));
            }
            component.decompositions.append(move(sub_bands));
        }

        return component;
    };

    auto make_tile = [&](size_t tile_index, TileData& tile) -> ErrorOr<void> {
        auto const& cod = tile.cod.value_or(context.cod);
        auto pq = context.siz.tile_2d_index_from_1d_index(tile_index);
        tile.rect = context.siz.reference_grid_coordinates_for_tile(pq);

        dbgln_if(JPEG2000_DEBUG, "tile {} rect {}", tile_index, tile.rect);

        for (auto [component_index, component] : enumerate(context.siz.components)) {
            VERIFY(component.bit_depth() >= 1);
            VERIFY(component.bit_depth() <= 38);
            if (component.horizontal_separation != 1)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Horizontal separation not yet implemented");
            if (component.vertical_separation != 1)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Vertical separation not yet implemented");
            tile.components.append(TRY(make_component(tile, component_index)));
        }

        return {};
    };

    auto make_progression_iterator = [&](JPEG2000LoadingContext const& context, TileData const& tile) -> ErrorOr<OwnPtr<JPEG2000::ProgressionIterator>> {
        if (tile.poc.has_value() || context.poc.has_value())
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: POC markers not yet supported");

        auto number_of_layers = tile.cod.value_or(context.cod).number_of_layers;

        int max_number_of_decomposition_levels = 0;
        for (size_t component_index = 0; component_index < context.siz.components.size(); ++component_index)
            max_number_of_decomposition_levels = max(max_number_of_decomposition_levels, context.coding_style_parameters_for_component(tile, component_index).number_of_decomposition_levels);

        auto number_of_precincts_from_resolution_level_and_component = [&](int r, int component_index) {
            auto const& sub_band = r == 0 ? tile.components[component_index].nLL : tile.components[component_index].decompositions[r - 1][0];
            return sub_band.num_precincts_wide * sub_band.num_precincts_high;
        };

        switch (tile.cod.value_or(context.cod).progression_order) {
        case CodingStyleDefault::ProgressionOrder::LayerResolutionComponentPosition:
            return make<JPEG2000::LayerResolutionLevelComponentPositionProgressionIterator>(number_of_layers, max_number_of_decomposition_levels, context.siz.components.size(), move(number_of_precincts_from_resolution_level_and_component));
        case CodingStyleDefault::ResolutionLayerComponentPosition:
            return make<JPEG2000::ResolutionLevelLayerComponentPositionProgressionIterator>(number_of_layers, max_number_of_decomposition_levels, context.siz.components.size(), move(number_of_precincts_from_resolution_level_and_component));
        case CodingStyleDefault::ResolutionPositionComponentLayer:
        case CodingStyleDefault::PositionComponentResolutionLayer:
        case CodingStyleDefault::ComponentPositionResolutionLayer: {
            auto XRsiz = [&](size_t i) { return context.siz.components[i].horizontal_separation; };
            auto YRsiz = [&](size_t i) { return context.siz.components[i].vertical_separation; };

            // "To use this progression, XRsiz and YRsiz values must be powers of two for each component."
            for (size_t component_index = 0; component_index < context.siz.components.size(); ++component_index) {
                if (!is_power_of_two(XRsiz(component_index)) || !is_power_of_two(YRsiz(component_index)))
                    return Error::from_string_literal("JPEG2000Loader: ResolutionPositionComponentLayer progression order requires XRsiz and YRsiz to be powers of two");
            }

            auto PPx = [&](int resolution_level, int component) {
                return context.coding_style_parameters_for_component(tile, component).precinct_sizes[resolution_level].PPx;
            };
            auto PPy = [&](int resolution_level, int component) {
                return context.coding_style_parameters_for_component(tile, component).precinct_sizes[resolution_level].PPy;
            };
            auto N_L = [&](int component) {
                return context.coding_style_parameters_for_component(tile, component).number_of_decomposition_levels;
            };
            auto num_precincts_wide = [&](int resolution_level, int component) {
                auto const& sub_band = resolution_level == 0 ? tile.components[component].nLL : tile.components[component].decompositions[resolution_level - 1][0];
                return sub_band.num_precincts_wide;
            };
            auto ll_rect = [&](int resolution_level, int component) {
                // The outer N_L lambda has been move()d away by the time this runs and can't be called here.
                auto N_L = context.coding_style_parameters_for_component(tile, component).number_of_decomposition_levels;
                return context.siz.reference_grid_coordinates_for_ll_band(tile.rect, component, resolution_level, N_L);
            };

            if (tile.cod.value_or(context.cod).progression_order == CodingStyleDefault::ResolutionPositionComponentLayer)
                return make<JPEG2000::ResolutionLevelPositionComponentLayerProgressionIterator>(
                    number_of_layers, max_number_of_decomposition_levels, context.siz.components.size(), move(number_of_precincts_from_resolution_level_and_component),
                    move(XRsiz), move(YRsiz), move(PPx), move(PPy), move(N_L), move(num_precincts_wide), tile.rect, move(ll_rect));
            if (tile.cod.value_or(context.cod).progression_order == CodingStyleDefault::PositionComponentResolutionLayer)
                return make<JPEG2000::PositionComponentResolutionLevelLayerProgressionIterator>(
                    number_of_layers, context.siz.components.size(), move(number_of_precincts_from_resolution_level_and_component),
                    move(XRsiz), move(YRsiz), move(PPx), move(PPy), move(N_L), move(num_precincts_wide), tile.rect, move(ll_rect));
            return make<JPEG2000::ComponentPositionResolutionLevelLayerProgressionIterator>(
                number_of_layers, context.siz.components.size(), move(number_of_precincts_from_resolution_level_and_component),
                move(XRsiz), move(YRsiz), move(PPx), move(PPy), move(N_L), move(num_precincts_wide), tile.rect, move(ll_rect));
        }
        }
        VERIFY_NOT_REACHED();
    };

    for (auto const& [tile_index, tile] : enumerate(context.tiles)) {
        TRY(make_tile(tile_index, tile));
        tile.progression_iterator = TRY(make_progression_iterator(context, tile));
    }

    return {};
}

static ErrorOr<u32> read_one_packet_header(JPEG2000LoadingContext& context, TileData& tile, ReadonlyBytes data)
{
    auto progression_data = TRY(context.next_progression_data(tile));

    FixedMemoryStream stream { data };

    if (tile.cod.value_or(context.cod).may_use_SOP_marker && data.size() >= 2 && TRY(peek_marker(data)) == J2K_SOP) {
        // A.8.1 Start of packet (SOP)
        // "It may be used in the bit stream in front of every packet. It shall not be used unless indicated that it is
        //  allowed in the proper COD marker segment (see A.6.1). If PPM or PPT marker segments are used, then the SOP marker
        //  segment may appear immediately before the packet data in the bit stream.
        //  If SOP marker segments are allowed (by signalling in the COD marker segment, see A.6.1), each packet in any given tile-
        //  part may or may not be appended with an SOP marker segment."
        // Just skip this data if it's there.
        // FIMXE: Tweak once we add support for PPM and PPT.
        u16 marker = TRY(stream.read_value<BigEndian<u16>>());
        u16 marker_length = TRY(stream.read_value<BigEndian<u16>>());
        u16 packet_sequence_number = TRY(stream.read_value<BigEndian<u16>>());
        VERIFY(marker == J2K_SOP); // Due to the peek_marker check above.
        if (marker_length != 4)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid SOP marker length");
        (void)packet_sequence_number; // FIXME: Do something with this?
    }

    BigEndianInputBitStream bitstream { MaybeOwned { stream } };

    // B.9 Packets
    // "All compressed image data representing a specific tile, layer, component, resolution level and precinct appears in the
    //  codestream in a contiguous segment called a packet. Packet data is aligned at 8-bit (one byte) boundaries."
    auto const& coding_parameters = context.coding_style_parameters_for_component(tile, progression_data.component);
    auto const r = progression_data.resolution_level;
    u32 const current_layer_index = progression_data.layer;

    // B.10 Packet header information coding
    // "The packets have headers with the following information:
    // - zero length packet;
    // - code-block inclusion;
    // - zero bit-plane information;
    // - number of coding passes;
    // - length of the code-block compressed image data from a given code-block."

    // B.10.1 Bit-stuffing routine
    // "If the value of the byte is 0xFF, the next byte includes an extra zero bit stuffed into the MSB. Once all bits of the
    //  packet header have been assembled, the last byte is packed to the byte boundary and emitted."
    u8 last_full_byte { 0 };
    Function<ErrorOr<bool>()> read_bit = [&bitstream, &last_full_byte]() -> ErrorOr<bool> {
        if (bitstream.is_aligned_to_byte_boundary()) {
            if (last_full_byte == 0xFF) {
                bool stuff_bit = TRY(bitstream.read_bit());
                if (stuff_bit)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid bit-stuffing");
            }
            last_full_byte = 0;
        }
        bool bit = TRY(bitstream.read_bit());
        last_full_byte = (last_full_byte << 1) | bit;
        return bit;
    };

    // The most useful section to understand the overall flow is B.10.8 Order of information within packet header,
    // which has an example packet header bitstream, and the data layout:
    // "bit for zero or non-zero length packet
    //  for each sub-band (LL or HL, LH and HH)
    //      for all code-blocks in this sub-band confined to the relevant precinct, in raster order
    //          code-block inclusion bits (if not previously included then tag tree, else one bit)
    //          if code-block included
    //              if first instance of code-block
    //                  zero bit-planes information
    //              number of coding passes included
    //              increase of code-block length indicator (Lblock)
    //              for each codeword segment
    //                  length of codeword segment"
    // The below implements these steps.

    // "bit for zero or non-zero length packet"
    // B.10.3 Zero length packet
    // "The first bit in the packet header denotes whether the packet has a length of zero (empty packet). The value 0 indicates a
    //  zero length; no code-blocks are included in this case. The value 1 indicates a non-zero length; this case is considered
    //  exclusively hereinafter."
    bool is_non_zero = TRY(read_bit());
    bool is_empty = !is_non_zero;

    // " for each sub-band (LL or HL, LH and HH)"
    struct TemporaryCodeBlockData {
        struct Segment {
            u32 length { 0 };
            u32 index { 0 };
            int number_of_passes { 0 };
        };
        Vector<Segment, 1> codeword_segments;
    };
    struct TemporarySubBandData {
        DecodedPrecinct* precinct { nullptr };
        Vector<TemporaryCodeBlockData> temporary_code_block_data;
    };
    Array<TemporarySubBandData, 3> temporary_sub_band_data {};

    static constexpr Array level_0_sub_bands { JPEG2000::SubBand::HorizontalLowpassVerticalLowpass };
    auto sub_bands = r == 0 ? level_0_sub_bands.span() : DecodedTileComponent::SubBandOrder.span();
    for (auto [sub_band_index, sub_band] : enumerate(sub_bands)) {
        auto& component = tile.components[progression_data.component];
        auto& sub_band_data = r == 0 ? component.nLL : component.decompositions[r - 1][sub_band_index];
        auto& precinct = sub_band_data.precincts[progression_data.precinct];

        // B.9: "Only those code-blocks that contain samples from the relevant sub-band, confined to the precinct, have any representation in the packet."
        if (is_empty || precinct.num_code_blocks_wide == 0 || precinct.num_code_blocks_high == 0)
            continue;

        temporary_sub_band_data[sub_band_index].precinct = &precinct;
        TRY(temporary_sub_band_data[sub_band_index].temporary_code_block_data.try_resize(precinct.code_blocks.size()));

        for (auto const& [code_block_index, current_block] : enumerate(precinct.code_blocks)) {
            size_t code_block_x = code_block_index % precinct.num_code_blocks_wide;
            size_t code_block_y = code_block_index / precinct.num_code_blocks_wide;

            // B.10.4 Code-block inclusion
            bool is_included;
            if (current_block.has_been_included_in_previous_packet) {
                // "For code-blocks that have been included in a previous packet, a single bit is used to represent the information, where
                //  a 1 means that the code-block is included in this layer and a 0 means that it is not."
                is_included = TRY(read_bit());
            } else {
                // "For code-blocks that have not been previously included in any packet, this information is signalled with a separate tag
                //  tree code for each precinct as confined to a sub-band. The values in this tag tree are the number of the layer in which the
                //  current code-block is first included."
                is_included = TRY(precinct.code_block_inclusion_tree->read_value(code_block_x, code_block_y, read_bit, current_layer_index + 1)) <= current_layer_index;
            }
            dbgln_if(JPEG2000_DEBUG, "code-block inclusion: {}", is_included);
            current_block.is_included = is_included;

            if (!is_included)
                continue;

            // B.10.5 Zero bit-plane information
            // "If a code-block is included for the first time,
            //  [...] the number of actual bit-planes for which coding passes are generated is Mb – P
            //  [...] these missing bit-planes are all taken to be zero
            //  [...] The value of P is coded in the packet header with a separate tag tree for every precinct"
            // And Annex E, E.1 Inverse quantization procedure:
            // "Mb = G + exp_b - 1       (E-2)
            //  where the number of guard bits G and the exponent exp_b are specified in the QCD or QCC marker segments (see A.6.4 and A.6.5)."
            bool is_included_for_the_first_time = is_included && !current_block.has_been_included_in_previous_packet;
            if (is_included_for_the_first_time) {
                u32 p = TRY(precinct.p_tree->read_value(code_block_x, code_block_y, read_bit));
                dbgln_if(JPEG2000_DEBUG, "zero bit-plane information: {}", p);
                current_block.p = p;
                current_block.has_been_included_in_previous_packet = true;
            }

            // B.10.6 Number of coding passes
            // Table B.4 – Codewords for the number of coding passes for each code-block
            u8 number_of_coding_passes = TRY([&]() -> ErrorOr<u8> {
                if (!TRY(read_bit()))
                    return 1;
                if (!TRY(read_bit()))
                    return 2;

                u8 bits = TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                if (bits != 3)
                    return 3 + bits;

                bits = TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                if (bits != 31)
                    return 6 + bits;

                bits = TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                bits = (bits << 1) | TRY(read_bit());
                return 37 + bits;
            }());
            dbgln_if(JPEG2000_DEBUG, "number of coding passes: {}", number_of_coding_passes);

            // B.10.7 Length of the compressed image data from a given code-block
            // "Multiple codeword segments arise when a termination occurs between coding passes which are included in the packet"

            u32 passes_from_previous_layers = precinct.code_blocks[code_block_index].number_of_coding_passes();

            JPEG2000::BitplaneDecodingOptions options;
            options.uses_termination_on_each_coding_pass = coding_parameters.uses_termination_on_each_coding_pass();
            options.uses_selective_arithmetic_coding_bypass = coding_parameters.uses_selective_arithmetic_coding_bypass();
            int number_of_segments = [&]() {
                auto old_segment_index = passes_from_previous_layers == 0 ? 0 : JPEG2000::segment_index_from_pass_index(options, passes_from_previous_layers - 1);
                auto new_segment_index = JPEG2000::segment_index_from_pass_index(options, passes_from_previous_layers + number_of_coding_passes - 1);
                auto number_of_segments = new_segment_index - old_segment_index;

                // If the old layer does not end on a segment boundary, the new layer has to add one segment for continuing the previous segment
                // in addition to counting the segments it contains and starts.
                if (old_segment_index == JPEG2000::segment_index_from_pass_index(options, passes_from_previous_layers))
                    number_of_segments++;
                return number_of_segments;
            }();

            // B.10.7.1 Single codeword segment
            // "A codeword segment is the number of bytes contributed to a packet by a code-block.
            //  The length of a codeword segment is represented by a binary number of length:
            //      bits = Lblock + ⌊log2(number_of_coding_passes)⌋
            //  where Lblock is a code-block state variable. A separate Lblock is used for each code-block in the precinct.
            //  The value of Lblock is initially set to three. The number of bytes contributed by each code-block is preceded by signalling
            //  bits that increase the value of Lblock, as needed. A signalling bit of zero indicates the current value of Lblock is sufficient.
            //  If there are k ones followed by a zero, the value of Lblock is incremented by k."
            // B.10.7.2 Multiple codeword segments
            // "Let T be the set of indices of terminated coding passes included for the code-block in the packet as indicated in Tables D.8
            //  and D.9. If the index final coding pass included in the packet is not a member of T, then it is added to T. Let n_1 < ... < n_K
            //  be the indices in T. K lengths are signalled consecutively with each length using the mechanism described in B.10.7.1."
            // "using the mechanism" means adjusting Lblock just once, and then reading one code word segment length with the
            // number of passes per segment, apparently.
            // We combine both cases: the single segment case is a special case of the multiple segment case.
            // For the B.10.7.1 case, we'll have number_of_segments = 1 and number_of_passes_in_segment = number_of_coding_passes.

            u32 k = 0;
            while (TRY(read_bit()))
                k++;
            current_block.Lblock += k;

            auto read_one_codeword_segment_length = [&](int number_of_passes) -> ErrorOr<u32> {
                u32 bits = current_block.Lblock + (u32)floor(log2(number_of_passes));
                if (bits > 32)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Too many bits for length of codeword segment");

                u32 length = 0;
                for (u32 i = 0; i < bits; ++i) {
                    bool bit = TRY(read_bit());
                    length = (length << 1) | bit;
                }
                return length;
            };

            VERIFY(temporary_sub_band_data[sub_band_index].temporary_code_block_data[code_block_index].codeword_segments.is_empty());

            int number_of_passes_used = 0;
            for (int i = 0; i < number_of_segments; ++i) {
                int number_of_passes_in_segment = number_of_coding_passes;
                u32 segment_index = JPEG2000::segment_index_from_pass_index(options, passes_from_previous_layers) + i;

                if (coding_parameters.uses_termination_on_each_coding_pass()) {
                    number_of_passes_in_segment = 1;
                } else if (coding_parameters.uses_selective_arithmetic_coding_bypass()) {
                    number_of_passes_in_segment = JPEG2000::number_of_passes_from_segment_index_in_bypass_mode(segment_index);

                    // Correction at start: Did the previous layer end in an incomplete segment that's continued in this layer?
                    Optional<u32> previous_segment_id = precinct.code_blocks[code_block_index].highest_segment_index();
                    if (previous_segment_id.has_value() && segment_index == previous_segment_id.value())
                        number_of_passes_in_segment -= precinct.code_blocks[code_block_index].number_of_coding_passes_in_segment(segment_index);

                    // Correction at end: Does this layer end in an incomplete segment that's continued in the next layer?
                    if (i == number_of_segments - 1)
                        number_of_passes_in_segment = min(number_of_coding_passes - number_of_passes_used, number_of_passes_in_segment);
                }
                u32 length = TRY(read_one_codeword_segment_length(number_of_passes_in_segment));
                dbgln_if(JPEG2000_DEBUG, "length({}) {}", i, length);
                temporary_sub_band_data[sub_band_index].temporary_code_block_data[code_block_index].codeword_segments.append({ length, segment_index, number_of_passes_in_segment });
                number_of_passes_used += number_of_passes_in_segment;
                VERIFY(number_of_passes_used <= number_of_coding_passes);
            }
            VERIFY(number_of_passes_used == number_of_coding_passes);
        }
    }

    if (last_full_byte == 0xFF) {
        bool final_stuff_bit = TRY(read_bit());
        if (final_stuff_bit)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid bit-stuffing");
    }

    if (tile.cod.value_or(context.cod).shall_use_EPH_marker) {
        // A.8.2 End of packet header (EPH)
        // "If EPH markers are required (by signalling in the COD marker segment, see A.6.1), each packet header in any given tile-
        //  part shall be postpended with an EPH marker segment. If the packet headers are moved to a PPM or PPT marker segments
        //  (see A.7.4 and A.7.5), then the EPH markers shall appear after the packet headers in the PPM or PPT marker segments."
        // Just skip this data if it's there.
        // FIMXE: Tweak once we add support for PPM and PPT.
        u16 marker = TRY(stream.read_value<BigEndian<u16>>());
        if (marker != J2K_EPH)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected EPH marker");
    }

    // Done reading packet header. Set `data` on each codeblock on the packet.
    u32 offset = stream.offset();
    for (auto const& temporary_sub_band : temporary_sub_band_data) {
        for (auto const& [code_block_index, temporary_code_block] : enumerate(temporary_sub_band.temporary_code_block_data)) {
            DecodedCodeBlock::Layer layer;
            for (auto [length, index, number_of_passes] : temporary_code_block.codeword_segments) {
                auto segment_data = data.slice(offset, length);
                offset += length;
                layer.segments.append({ segment_data, index, number_of_passes });
            }
            TRY(temporary_sub_band.precinct->code_blocks[code_block_index].layers.try_append(layer));
        }
    }

    return offset;
}

static ErrorOr<void> read_tile_part_packet_headers(JPEG2000LoadingContext& context, TileData& tile, TilePartData& tile_part)
{
    if (!context.rgns.is_empty() || !tile.rgns.is_empty())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: RGN markers not yet supported");

    auto data = tile_part.data;
    while (!data.is_empty()) {
        auto length = TRY(read_one_packet_header(context, tile, data));
        data = data.slice(length);
    }

    return {};
}

static ErrorOr<void> read_tile_packet_headers(JPEG2000LoadingContext& context, TileData& tile)
{
    for (auto& tile_part : tile.tile_parts)
        TRY(read_tile_part_packet_headers(context, tile, tile_part));
    return {};
}

static ErrorOr<void> read_packet_headers(JPEG2000LoadingContext& context)
{
    for (auto& tile : context.tiles)
        TRY(read_tile_packet_headers(context, tile));
    return {};
}

static u8 get_exponent(QuantizationDefault const& quantization_parameters, JPEG2000::SubBand sub_band, int resolution_level, int N_L)
{
    switch (quantization_parameters.quantization_style) {
    case QuantizationDefault::QuantizationStyle::NoQuantization: {
        auto const& steps = quantization_parameters.step_sizes.get<Vector<QuantizationDefault::ReversibleStepSize>>();
        if (sub_band == JPEG2000::SubBand::HorizontalLowpassVerticalLowpass) {
            VERIFY(resolution_level == 0);
            return steps[0].exponent;
        }
        VERIFY(resolution_level > 0);
        return steps[1 + (resolution_level - 1) * 3 + (int)sub_band - 1].exponent;
    }
    case QuantizationDefault::QuantizationStyle::ScalarDerived:
    case QuantizationDefault::QuantizationStyle::ScalarExpounded: {
        auto const& steps = quantization_parameters.step_sizes.get<Vector<QuantizationDefault::IrreversibleStepSize>>();

        if (quantization_parameters.quantization_style == QuantizationDefault::QuantizationStyle::ScalarDerived) {
            // Table F.1 – Decomposition level nb for sub-band b
            // Note: The spec suggests that this ends with n_b = 1, but if N_L is 0, we have 0LL and nothing else.
            int n_b = resolution_level == 0 ? N_L : (N_L + 1 - resolution_level);
            // (E-5)
            return steps[0].exponent - N_L + n_b;
            // This is the same as `return resolution_level == 0 ? steps[0].exponent : steps[0].exponent - (resolution_level - 1);`
        }

        if (sub_band == JPEG2000::SubBand::HorizontalLowpassVerticalLowpass) {
            VERIFY(resolution_level == 0);
            return steps[0].exponent;
        }
        VERIFY(resolution_level > 0);
        return steps[1 + (resolution_level - 1) * 3 + (int)sub_band - 1].exponent;
    }
    }
    VERIFY_NOT_REACHED();
}

static int compute_M_b(JPEG2000LoadingContext& context, TileData& tile, int component_index, JPEG2000::SubBand sub_band_type, int r, int N_L)
{
    // Annex E, E.1 Inverse quantization procedure:
    // "Mb = G + exp_b - 1       (E-2)
    //  where the number of guard bits G and the exponent exp_b are specified in the QCD or QCC marker segments (see A.6.4 and A.6.5)."
    auto quantization_parameters = context.quantization_parameters_for_component(tile, component_index);
    auto exponent = get_exponent(quantization_parameters, sub_band_type, r, N_L);
    return quantization_parameters.number_of_guard_bits + exponent - 1;
}

static ErrorOr<void> decode_bitplanes_to_coefficients(JPEG2000LoadingContext& context)
{
    auto copy_and_dequantize_if_needed = [&](JPEG2000::Span2D<float> output, ReadonlySpan<float> input, QuantizationDefault const& quantization_parameters, JPEG2000::SubBand sub_band_type, int component_index, int r, int N_L) {
        int w = output.size.width();
        int h = output.size.height();
        VERIFY(w * h == static_cast<int>(input.size()));

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                float value = input[y * w + x];

                // E.1 Inverse quantization procedure
                // The coefficients store qbar_b.
                if (quantization_parameters.quantization_style != QuantizationDefault::QuantizationStyle::NoQuantization) {
                    // E.1.1 Irreversible transformation
                    auto R_I = context.siz.components[component_index].bit_depth();

                    // Table E.1 – Sub-band gains
                    auto log_2_gain_b = sub_band_type == JPEG2000::SubBand::HorizontalLowpassVerticalLowpass ? 0 : (sub_band_type == JPEG2000::SubBand::HorizontalHighpassVerticalLowpass || sub_band_type == JPEG2000::SubBand::HorizontalLowpassVerticalHighpass ? 1 : 2);
                    auto R_b = R_I + log_2_gain_b; // (E-4)

                    u16 mantissa;
                    if (quantization_parameters.quantization_style == QuantizationDefault::QuantizationStyle::ScalarDerived) {
                        // (E-5)
                        mantissa = quantization_parameters.step_sizes.get<Vector<QuantizationDefault::IrreversibleStepSize>>()[0].mantissa;
                    } else {
                        if (r == 0)
                            mantissa = quantization_parameters.step_sizes.get<Vector<QuantizationDefault::IrreversibleStepSize>>()[0].mantissa;
                        else
                            mantissa = quantization_parameters.step_sizes.get<Vector<QuantizationDefault::IrreversibleStepSize>>()[3 * (r - 1) + (int)sub_band_type].mantissa;
                    }

                    // (E-3)
                    auto exponent = get_exponent(quantization_parameters, sub_band_type, r, N_L);
                    float step_size = powf(2.0f, R_b - exponent) * (1.0f + mantissa / powf(2.0f, 11.0f));

                    // (E-6), with r chosen as 0 (see NOTE below (E-6)).
                    value *= step_size;
                }

                output.data[y * output.pitch + x] = value;
            }
        }
    };

    auto decode_bitplanes = [&](TileData& tile, JPEG2000::SubBand sub_band_type, DecodedSubBand& sub_band, int component_index, int r, int N_L) -> ErrorOr<void> {
        TRY(sub_band.coefficients.try_resize(sub_band.rect.width() * sub_band.rect.height()));

        auto const& coding_style = context.coding_style_parameters_for_component(tile, component_index);
        JPEG2000::BitplaneDecodingOptions bitplane_decoding_options;
        bitplane_decoding_options.uses_selective_arithmetic_coding_bypass = coding_style.uses_selective_arithmetic_coding_bypass();
        bitplane_decoding_options.reset_context_probabilities_each_pass = coding_style.reset_context_probabilities();
        bitplane_decoding_options.uses_termination_on_each_coding_pass = coding_style.uses_termination_on_each_coding_pass();
        bitplane_decoding_options.uses_vertically_causal_context = coding_style.uses_vertically_causal_context();
        bitplane_decoding_options.uses_segmentation_symbols = coding_style.uses_segmentation_symbols();

        int M_b = compute_M_b(context, tile, component_index, sub_band_type, r, N_L);

        // FIXME: Codeblocks all use independent arithmetic coders, so this could run in parallel.
        for (auto& precinct : sub_band.precincts) {

            Vector<float> precinct_coefficients;
            auto clipped_precinct_rect = precinct.rect.intersected(sub_band.rect);
            precinct_coefficients.resize(clipped_precinct_rect.width() * clipped_precinct_rect.height());

            for (auto& code_block : precinct.code_blocks) {
                int total_number_of_coding_passes = code_block.number_of_coding_passes();
                ByteBuffer storage;
                Vector<ReadonlyBytes, 1> combined_segments = TRY(code_block.segments_for_all_layers(storage));

                JPEG2000::Span2D<float> output;
                output.size = code_block.rect.size();
                output.pitch = clipped_precinct_rect.width();
                output.data = precinct_coefficients.span().slice((code_block.rect.y() - clipped_precinct_rect.y()) * output.pitch + (code_block.rect.x() - clipped_precinct_rect.x()));
                TRY(JPEG2000::decode_code_block(output, sub_band_type, total_number_of_coding_passes, combined_segments, M_b, code_block.p, bitplane_decoding_options));
            }

            JPEG2000::Span2D<float> output;
            output.size = clipped_precinct_rect.size();
            output.pitch = sub_band.rect.width();
            output.data = sub_band.coefficients.span().slice((clipped_precinct_rect.y() - sub_band.rect.y()) * output.pitch + (clipped_precinct_rect.x() - sub_band.rect.x()));
            copy_and_dequantize_if_needed(output, precinct_coefficients, context.quantization_parameters_for_component(tile, component_index), sub_band_type, component_index, r, N_L);
        }

        return {};
    };

    for (auto& tile : context.tiles) {
        for (auto [component_index, component] : enumerate(tile.components)) {
            int N_L = component.decompositions.size();
            TRY(decode_bitplanes(tile, JPEG2000::SubBand::HorizontalLowpassVerticalLowpass, component.nLL, component_index, 0, N_L));
            for (auto const& [decomposition_index, decomposition] : enumerate(component.decompositions)) {
                int r = decomposition_index + 1;
                for (auto [sub_band_index, sub_band] : enumerate(DecodedTileComponent::SubBandOrder)) {
                    TRY(decode_bitplanes(tile, sub_band, decomposition[sub_band_index], component_index, r, N_L));
                }
            }
        }
    }

    return {};
}

static ErrorOr<void> run_inverse_discrete_wavelet_transform(JPEG2000LoadingContext& context)
{
    // FIXME: Could run these in parallel.
    for (auto& tile : context.tiles) {
        for (auto [component_index, component] : enumerate(tile.components)) {
            int N_L = component.decompositions.size();

            Gfx::JPEG2000::IDWTInput input;
            input.transformation = context.coding_style_parameters_for_component(tile, component_index).transformation;
            input.LL.rect = component.nLL.rect;
            input.LL.data = { component.nLL.coefficients, component.nLL.rect.size(), component.nLL.rect.width() };

            for (auto const& [decomposition_index, decomposition] : enumerate(component.decompositions)) {
                int r = decomposition_index + 1;

                JPEG2000::IDWTDecomposition idwt_decomposition;
                idwt_decomposition.ll_rect = context.siz.reference_grid_coordinates_for_ll_band(tile.rect, component_index, r, N_L);

                VERIFY(DecodedTileComponent::SubBandOrder[0] == JPEG2000::SubBand::HorizontalHighpassVerticalLowpass);
                auto hl_rect = decomposition[0].rect;
                idwt_decomposition.hl = { hl_rect, { decomposition[0].coefficients, hl_rect.size(), hl_rect.width() } };

                VERIFY(DecodedTileComponent::SubBandOrder[1] == JPEG2000::SubBand::HorizontalLowpassVerticalHighpass);
                auto lh_rect = decomposition[1].rect;
                idwt_decomposition.lh = { lh_rect, { decomposition[1].coefficients, lh_rect.size(), lh_rect.width() } };

                VERIFY(DecodedTileComponent::SubBandOrder[2] == JPEG2000::SubBand::HorizontalHighpassVerticalHighpass);
                auto hh_rect = decomposition[2].rect;
                idwt_decomposition.hh = { hh_rect, { decomposition[2].coefficients, hh_rect.size(), hh_rect.width() } };

                input.decompositions.append(idwt_decomposition);
            }

            auto output = TRY(JPEG2000::IDWT(input));
            VERIFY(component.rect == output.rect);
            component.samples = move(output.data);

            // FIXME: Could release coefficient data here, to reduce peak memory use.
        }
    }

    return {};
}

static ErrorOr<void> postprocess_samples(JPEG2000LoadingContext& context)
{
    auto undo_multiple_component_transformation = [&](TileData& tile) -> ErrorOr<void> {
        VERIFY(context.siz.components.size() == tile.components.size());
        if (tile.components.size() < 3)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple component transformation type but fewer than 3 components");

        // T.800, I.5.3.6 Channel Definition box
        // "If a multiple component transform is specified within the codestream, the image must be in an RGB colourspace and the
        //  red, green and blue colours as channels 0, 1 and 2 in the codestream, respectively."
        // FIXME: Check this.

        auto transformation0 = context.coding_style_parameters_for_component(tile, 0).transformation;
        auto transformation1 = context.coding_style_parameters_for_component(tile, 1).transformation;
        auto transformation2 = context.coding_style_parameters_for_component(tile, 2).transformation;
        if (transformation0 != transformation1 || transformation1 != transformation2)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple component transformation type but components disagree on lossiness");

        // "The three components [...] shall have the same separation on the reference grid and the same bit-depth."
        if (context.siz.components[0].horizontal_separation != context.siz.components[1].horizontal_separation
            || context.siz.components[1].horizontal_separation != context.siz.components[2].horizontal_separation) {
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple component transformation type but components disagree on horizontal separation");
        }

        if (context.siz.components[0].vertical_separation != context.siz.components[1].vertical_separation
            || context.siz.components[1].vertical_separation != context.siz.components[2].vertical_separation) {
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple component transformation type but components disagree on vertical separation");
        }

        // Note: Spec says "bit-depth" but we check bit depth and sign. That must be what the spec means?
        if (context.siz.components[0].depth_and_sign != context.siz.components[1].depth_and_sign
            || context.siz.components[1].depth_and_sign != context.siz.components[2].depth_and_sign) {
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple component transformation type but components disagree on bit depth");
        }

        if (tile.components[0].rect.size() != tile.components[1].rect.size()
            || tile.components[0].rect.size() != tile.components[1].rect.size()) {
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple component transformation type but components disagree on dimensions");
        }

        auto& c0 = tile.components[0].samples;
        auto& c1 = tile.components[1].samples;
        auto& c2 = tile.components[2].samples;
        int w = tile.components[0].rect.width();

        if (transformation0 == JPEG2000::Transformation::Reversible_5_3_Filter) {
            // G.2 Reversible multiple component transformation (RCT)
            // "The three components input into the RCT shall have the same separation on the reference grid and the same bit-depth."
            // Same for RCT and ICT; checked above this branch.
            for (int y = 0; y < tile.components[0].rect.height(); ++y) {
                for (int x = 0; x < w; ++x) {
                    float Y = c0[y * w + x];
                    float Cb = c1[y * w + x];
                    float Cr = c2[y * w + x];

                    float G = Y - floorf((Cb + Cr) / 4); // (G-6)
                    float R = Cr + G;                    // (G-7)
                    float B = Cb + G;                    // (G-8)

                    c0[y * w + x] = R;
                    c1[y * w + x] = G;
                    c2[y * w + x] = B;
                }
            }
        } else {
            VERIFY(transformation0 == JPEG2000::Transformation::Irreversible_9_7_Filter);

            // G.3 Irreversible multiple component transformation (ICT)
            // "The three components input into the ICT shall have the same separation on the reference grid and the same bit-depth."
            // Same for RCT and ICT; checked above this branch.
            for (int y = 0; y < tile.components[0].rect.height(); ++y) {
                for (int x = 0; x < w; ++x) {
                    float Y = c0[y * w + x];
                    float Cb = c1[y * w + x];
                    float Cr = c2[y * w + x];

                    float R = Y + 1.402f * Cr;                  // (G-12)
                    float G = Y - 0.34413f * Cb - 0.7141f * Cr; // (G-13)
                    float B = Y + 1.772f * Cb;                  // (G-14)

                    c0[y * w + x] = R;
                    c1[y * w + x] = G;
                    c2[y * w + x] = B;
                }
            }
        }

        return {};
    };

    auto undo_dc_level_shift = [&](TileData& tile) -> ErrorOr<void> {
        VERIFY(context.siz.components.size() == tile.components.size());

        // DC level shift
        // G.1.2 Inverse DC level shifting of tile-components
        for (auto [component_index, component] : enumerate(tile.components)) {
            if (!context.siz.components[component_index].is_signed()) {
                for (auto& coefficient : component.samples)
                    coefficient += 1u << (context.siz.components[component_index].bit_depth() - 1); // (G-2)
            }
        }

        return {};
    };

    for (auto& tile : context.tiles) {
        // Figure G.1 – Placement of the DC level shifting with component transformation
        if (tile.cod.value_or(context.cod).multiple_component_transformation_type == CodingStyleDefault::MultipleComponentTransformationType::MultipleComponentTransformationUsed)
            TRY(undo_multiple_component_transformation(tile));

        TRY(undo_dc_level_shift(tile));
    }

    return {};
}

static ErrorOr<void> convert_to_bitmap(JPEG2000LoadingContext& context)
{
    // determine_color_space() defers returning an error until here, so that JPEG2000ImageDecoderPlugin::create()
    // can succeed even with unsupported color spaces.
    if (context.color_space == ColorSpace::Unsupported)
        return move(context.color_space_error.value());

    // Map components to channels.
    if (context.palette_box.has_value() && context.options.palette_handling != JPEG2000DecoderOptions::PaletteHandling::PaletteIndicesAsGrayscale) {
        ISOBMFF::JPEG2000ComponentMappingBox cmap;
        if (context.component_mapping_box.has_value()) {
            cmap = context.component_mapping_box.value();
        } else {
            // The spec requires that cmap is present if pclr is, but in practice some (very few) files have pclr without cmap.
            // Assume that everything maps through directly in this case.
            for (size_t i = 0; i < context.palette_box->bit_depths.size(); ++i) {
                ISOBMFF::JPEG2000ComponentMappingBox::Mapping mapping;
                mapping.component_index = 0;
                mapping.palette_component_index = i;
                mapping.mapping_type = ISOBMFF::JPEG2000ComponentMappingBox::Mapping::Type::Palette;
                cmap.component_mappings.append(mapping);
            }
        }

        // I.5.3.4 Palette box
        // "This value shall be in the range 1 to 1024"
        if (context.palette_box->palette_entries.size() > 1024)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only up to 1024 palette entries allowed");

        for (auto& palette_entry : context.palette_box->palette_entries)
            VERIFY(palette_entry.size() == context.palette_box->bit_depths.size()); // Enforced in JPEG2000PaletteBox::read_from_stream().
        auto palette_channel_count = context.palette_box->bit_depths.size();

        for (auto& tile : context.tiles) {
            TRY(tile.channels.try_resize(cmap.component_mappings.size()));

            for (auto const& [i, mapping] : enumerate(cmap.component_mappings)) {
                if (mapping.component_index >= tile.components.size())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Component mapping index out of range");

                if (mapping.mapping_type == ISOBMFF::JPEG2000ComponentMappingBox::Mapping::Type::Direct) {
                    tile.channels[mapping.component_index] = move(tile.components[mapping.component_index].samples);
                    TRY(tile.channel_information.try_append(context.siz.components[mapping.component_index]));
                    continue;
                }

                if (mapping.mapping_type != ISOBMFF::JPEG2000ComponentMappingBox::Mapping::Type::Palette)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Unknown mapping type");

                if (context.siz.components[mapping.component_index].is_signed())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Don't know how to handle signed palette components");

                if (mapping.palette_component_index >= palette_channel_count)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Palette component index out of range");

                ImageAndTileSize::ComponentInformation component_information;
                component_information.depth_and_sign = context.palette_box->bit_depths[mapping.palette_component_index].depth - 1;
                if (context.palette_box->bit_depths[mapping.palette_component_index].is_signed)
                    component_information.depth_and_sign |= 0x80;
                component_information.horizontal_separation = context.siz.components[mapping.component_index].horizontal_separation;
                component_information.vertical_separation = context.siz.components[mapping.component_index].vertical_separation;
                TRY(tile.channel_information.try_append(component_information));

                auto const& component = tile.components[mapping.component_index];
                TRY(tile.channels[i].try_ensure_capacity(component.samples.size()));
                for (auto sample : component.samples) {
                    int index = static_cast<int>(sample);
                    if (index < 0 || static_cast<size_t>(index) >= context.palette_box->palette_entries.size())
                        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Palette index out of range");
                    tile.channels[i].append(context.palette_box->palette_entries[index][mapping.palette_component_index]);
                }
            }

            for (auto& component : tile.components)
                component.samples.clear();
        }
    } else {
        for (auto& tile : context.tiles) {
            for (size_t i = 0; i < tile.components.size(); ++i) {
                TRY(tile.channels.try_append(move(tile.components[i].samples)));
                TRY(tile.channel_information.try_append(context.siz.components[i]));
            }
        }
    }
    auto const channel_count = context.tiles[0].channels.size();

    bool has_alpha = false;
    if (context.palette_box.has_value() && context.options.palette_handling == JPEG2000DecoderOptions::PaletteHandling::PaletteIndicesAsGrayscale) {
        for (auto& tile : context.tiles) {
            if (tile.channels.size() != 1)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Palette indices as grayscale require single component");
            for (auto sample : tile.channels[0]) {
                // The JPEG2000 spec allows palette indices up to 1023, but the PDF spec says that JPEG2000 images
                // embedded in PDFs must have indices that fit in a one byte.
                if (sample < 0 || sample >= 256)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Palette indices out of range");
            }
        }
    } else {
        if (context.channel_definition_box.has_value()) {
            if (context.channel_definition_box->channels.size() != channel_count)
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Channel definition box channel count doesn't match channel count");

            Vector<bool, 4> channel_used;
            channel_used.resize(context.channel_definition_box->channels.size());

            // If you make this more flexible in the future and implement channel swapping,
            // check if that should happen for JPEG2000 files in PDFs as well.
            for (auto channel : context.channel_definition_box->channels) {
                if (channel.channel_index >= channel_count)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Channel definition box channel index out of range");
                if (channel_used[channel.channel_index])
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Channel definition box channel index used multiple times");
                channel_used[channel.channel_index] = true;

                if (channel.channel_type != ISOBMFF::JPEG2000ChannelDefinitionBox::Channel::Type::Color
                    && channel.channel_type != ISOBMFF::JPEG2000ChannelDefinitionBox::Channel::Type::Opacity) {
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only color and opacity channels supported yet");
                }
                if (channel.channel_type == ISOBMFF::JPEG2000ChannelDefinitionBox::Channel::Type::Color) {
                    if (channel.channel_index + 1 != channel.channel_association)
                        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only unshuffled color channel indices supported yet");
                } else {
                    VERIFY(channel.channel_type == ISOBMFF::JPEG2000ChannelDefinitionBox::Channel::Type::Opacity);
                    if (channel.channel_index != channel_count - 1)
                        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only opacity channel as last channel supported yet");
                    if (channel.channel_association != 0)
                        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only full opacity channel supported yet");
                    has_alpha = true;
                }
            }
        } else if (!context.color_box.has_value()) {
            // Raw codestream. Go by number of channels.
            has_alpha = channel_count == 2 || channel_count == 4;
        }

        unsigned expected_channel_count = [](ColorSpace color_space) {
            switch (color_space) {
            case ColorSpace::Gray:
                return 1;
            case ColorSpace::sRGB:
                return 3;
            case ColorSpace::CMYK:
                return 4;
            case ColorSpace::Unsupported: // Rejected above.
                VERIFY_NOT_REACHED();
            };
            VERIFY_NOT_REACHED();
        }(context.color_space);
        if (has_alpha)
            expected_channel_count++;
        if (channel_count < expected_channel_count)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough channels for expected channel count");

        if (channel_count > expected_channel_count)
            dbgln("JPEG2000ImageDecoderPlugin: More channels ({}) than expected channel count ({}), ignoring superfluous channels", context.siz.components.size(), expected_channel_count);

        // Convert to 8bpp.
        for (auto& tile : context.tiles) {
            for (auto const& [channel_index, channel] : enumerate(tile.channels)) {
                if (tile.channel_information[channel_index].is_signed())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only unsigned components supported yet");

                if (tile.channel_information[channel_index].bit_depth() == 8)
                    continue;

                // > 16bpp currently overflow the u16s internal to decode_code_block().
                if (tile.channel_information[channel_index].bit_depth() > 16)
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: More than 16 bits per component not supported yet");

                for (float& sample : channel)
                    sample = (sample * 255.0f) / ((1 << tile.channel_information[channel_index].bit_depth()) - 1);
            }
        }
    }

    if (context.color_space == ColorSpace::CMYK) {
        if (has_alpha)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: CMYK with alpha not yet supported");

        auto bitmap = TRY(Gfx::CMYKBitmap::create_with_size({ context.siz.width, context.siz.height }));

        for (auto& tile : context.tiles) {
            // compute_decoding_metadata currently rejects images with horizontal_separation or vertical_separation != 1.
            for (auto& component : tile.components) {
                if (component.rect.size() != tile.components[0].rect.size())
                    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Components with differing sizes not yet supported");
            }
            int w = tile.components[0].rect.width();
            int h = tile.components[0].rect.height();

            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    float C = round_to<u8>(clamp(tile.channels[0][y * w + x], 0.0f, 255.0f));
                    float M = round_to<u8>(clamp(tile.channels[1][y * w + x], 0.0f, 255.0f));
                    float Y = round_to<u8>(clamp(tile.channels[2][y * w + x], 0.0f, 255.0f));
                    float K = round_to<u8>(clamp(tile.channels[3][y * w + x], 0.0f, 255.0f));
                    bitmap->scanline(y + tile.components[0].rect.top())[x + tile.components[0].rect.left()] = { (u8)C, (u8)M, (u8)Y, (u8)K };
                }
            }
        }

        context.cmyk_bitmap = move(bitmap);
        return {};
    }

    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { context.siz.width, context.siz.height }));

    for (auto& tile : context.tiles) {
        // compute_decoding_metadata currently rejects images with horizontal_separation or vertical_separation != 1.
        for (auto& component : tile.components) {
            if (component.rect.size() != tile.components[0].rect.size())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Components with differing sizes not yet supported");
        }

        int w = tile.components[0].rect.width();
        int h = tile.components[0].rect.height();

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                float value = tile.channels[0][y * w + x];

                u8 byte_value = round_to<u8>(clamp(value, 0.0f, 255.0f));
                u8 r = byte_value;
                u8 g = byte_value;
                u8 b = byte_value;
                u8 a = 255;

                if (tile.channels.size() == 2) {
                    a = round_to<u8>(clamp(tile.channels[1][y * w + x], 0.0f, 255.0f));
                } else if (tile.channels.size() == 3) {
                    g = round_to<u8>(clamp(tile.channels[1][y * w + x], 0.0f, 255.0f));
                    b = round_to<u8>(clamp(tile.channels[2][y * w + x], 0.0f, 255.0f));
                } else if (tile.channels.size() == 4) {
                    g = round_to<u8>(clamp(tile.channels[1][y * w + x], 0.0f, 255.0f));
                    b = round_to<u8>(clamp(tile.channels[2][y * w + x], 0.0f, 255.0f));
                    a = round_to<u8>(clamp(tile.channels[3][y * w + x], 0.0f, 255.0f));
                }

                Color pixel;
                pixel.set_red(r);
                pixel.set_green(g);
                pixel.set_blue(b);
                pixel.set_alpha(a);
                bitmap->set_pixel(x + tile.components[0].rect.left(), y + tile.components[0].rect.top(), pixel);
            }
        }
    }

    // FIXME: Could release sample data here, to reduce peak memory use.

    context.bitmap = move(bitmap);

    return {};
}

static ErrorOr<void> decode_image(JPEG2000LoadingContext& context)
{
    TRY(parse_codestream_tile_headers(context));
    TRY(compute_decoding_metadata(context));
    TRY(read_packet_headers(context));
    TRY(decode_bitplanes_to_coefficients(context));
    TRY(run_inverse_discrete_wavelet_transform(context));
    TRY(postprocess_samples(context));
    TRY(convert_to_bitmap(context));

    return {};
}

bool JPEG2000ImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.starts_with(jp2_id_string) || data.starts_with(marker_id_string);
}

JPEG2000ImageDecoderPlugin::JPEG2000ImageDecoderPlugin(JPEG2000DecoderOptions options)
{
    m_context = make<JPEG2000LoadingContext>();
    m_context->options = options;
}

JPEG2000ImageDecoderPlugin::~JPEG2000ImageDecoderPlugin() = default;

IntSize JPEG2000ImageDecoderPlugin::size()
{
    return m_context->size;
}

static void determine_color_space(JPEG2000LoadingContext& context)
{
    if (context.palette_box.has_value() && context.options.palette_handling == JPEG2000DecoderOptions::PaletteHandling::PaletteIndicesAsGrayscale) {
        // context.color_box has the color space after palette expansion. But in this mode, we don't expand the palette.
        context.color_space = ColorSpace::Gray;
        return;
    }

    if (context.color_box.has_value()) {
        if (context.color_box->method == ISOBMFF::JPEG2000ColorSpecificationBox::Method::Enumerated) {
            if (context.color_box->enumerated_color_space == ISOBMFF::JPEG2000ColorSpecificationBox::EnumCS::sRGB) {
                context.color_space = ColorSpace::sRGB;
            } else if (context.color_box->enumerated_color_space == ISOBMFF::JPEG2000ColorSpecificationBox::EnumCS::Greyscale) {
                context.color_space = ColorSpace::Gray;
            } else if (context.color_box->enumerated_color_space == ISOBMFF::JPEG2000ColorSpecificationBox::EnumCS::CMYK) {
                context.color_space = ColorSpace::CMYK;
            } else {
                context.color_space = ColorSpace::Unsupported;
                context.color_space_error = Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only sRGB, grayscale, and CMYK enumerated color spaces supported yet");
            }
        } else if (context.color_box->method == ISOBMFF::JPEG2000ColorSpecificationBox::Method::ICC_Restricted
            || context.color_box->method == ISOBMFF::JPEG2000ColorSpecificationBox::Method::ICC_Any) {
            auto icc_header_or_error = ICC::Profile::read_header(context.color_box->icc_data.bytes());
            if (icc_header_or_error.is_error()) {
                context.color_space = ColorSpace::Unsupported;
                context.color_space_error = icc_header_or_error.release_error();
                return;
            }

            auto icc_header = icc_header_or_error.release_value();
            if (icc_header.data_color_space == ICC::ColorSpace::RGB) {
                context.color_space = ColorSpace::sRGB;
            } else if (icc_header.data_color_space == ICC::ColorSpace::Gray) {
                context.color_space = ColorSpace::Gray;
            } else if (icc_header.data_color_space == ICC::ColorSpace::CMYK) {
                context.color_space = ColorSpace::CMYK;
            } else {
                context.color_space = ColorSpace::Unsupported;
                context.color_space_error = Error::from_string_literal("JPEG2000ImageDecoderPlugin: Only sRGB, grayscale, and CMYK ICC color spaces supported yet");
            }
        } else {
            context.color_space = ColorSpace::Unsupported;
            context.color_space_error = Error::from_string_literal("JPEG2000ImageDecoderPlugin: Can only handle enumerated and ICC color specification methods yet");
        }
    } else {
        // Raw codestream. Go by number of components.
        context.color_space = context.siz.components.size() < 3 ? ColorSpace::Gray : ColorSpace::sRGB;
    }
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEG2000ImageDecoderPlugin::create(ReadonlyBytes data)
{
    return create_with_options(data, {});
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEG2000ImageDecoderPlugin::create_with_options(ReadonlyBytes data, JPEG2000DecoderOptions options)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JPEG2000ImageDecoderPlugin(options)));
    TRY(decode_jpeg2000_header(*plugin->m_context, data));
    determine_color_space(*plugin->m_context);
    return plugin;
}

ErrorOr<ImageFrameDescriptor> JPEG2000ImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index != 0)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid frame index");

    if (m_context->state == JPEG2000LoadingContext::State::Error)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Decoding failed");

    if (m_context->state < JPEG2000LoadingContext::State::DecodedImage) {
        TRY(decode_image(*m_context));
        m_context->state = JPEG2000LoadingContext::State::DecodedImage;
    }

    if (m_context->cmyk_bitmap && !m_context->bitmap)
        return ImageFrameDescriptor { TRY(m_context->cmyk_bitmap->to_low_quality_rgb()), 0 };

    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

ErrorOr<Optional<ReadonlyBytes>> JPEG2000ImageDecoderPlugin::icc_data()
{
    if (m_context->color_box.has_value()
        && (m_context->color_box->method == ISOBMFF::JPEG2000ColorSpecificationBox::Method::ICC_Restricted
            || m_context->color_box->method == ISOBMFF::JPEG2000ColorSpecificationBox::Method::ICC_Any)) {
        return m_context->color_box->icc_data.bytes();
    }

    return OptionalNone {};
}

NaturalFrameFormat JPEG2000ImageDecoderPlugin::natural_frame_format() const
{
    if (m_context->state == JPEG2000LoadingContext::State::Error)
        return NaturalFrameFormat::RGB;

    switch (m_context->color_space) {
    case ColorSpace::sRGB:
        return NaturalFrameFormat::RGB;
    case ColorSpace::Gray:
        return NaturalFrameFormat::Grayscale;
    case ColorSpace::CMYK:
        return NaturalFrameFormat::CMYK;
    case ColorSpace::Unsupported:
        return NaturalFrameFormat::RGB;
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<CMYKBitmap>> JPEG2000ImageDecoderPlugin::cmyk_frame()
{
    VERIFY(natural_frame_format() == NaturalFrameFormat::CMYK);

    if (m_context->state < JPEG2000LoadingContext::State::DecodedImage) {
        if (auto result = decode_image(*m_context); result.is_error()) {
            m_context->state = JPEG2000LoadingContext::State::Error;
            return result.release_error();
        }
        m_context->state = JPEG2000LoadingContext::State::DecodedImage;
    }

    return *m_context->cmyk_bitmap;
}

}
