/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Boxes.h"

namespace Gfx::ISOBMFF {

struct JPEG2000HeaderBox final : public SuperBox {
    BOX_SUBTYPE(JPEG2000HeaderBox);
};

// I.5.3.1 Image Header box
struct JPEG2000ImageHeaderBox final : public Box {
    BOX_SUBTYPE(JPEG2000ImageHeaderBox);

    u32 height { 0 };
    u32 width { 0 };
    u16 num_components { 0 };
    u8 bits_per_component { 0 };
    u8 compression_type { 0 };
    u8 is_colorspace_unknown { 0 };
    u8 contains_intellectual_property_rights { 0 };

    enum CompressionType {
        // T.800, I.5.3.1 Image Header box

        // "The value of this field shall be 7."
        Default = 7,

        // T.801, Table M.19 – Legal C values
        Uncompressed = 0,

        // "Rec. ITU-T T.4, the basic algorithm known as MH (Modified Huffman). This value is only permitted for bi-level images."
        T_4_Modified_Huffman = 1,

        // "Rec. ITU-T T.4, commonly known as MR (Modified READ). This value is only permitted for bi-level images."
        T_4_Modified_Read = 2,

        // "Rec. ITU-T T.6, commonly known as MMR (Modified Modified READ). This value is only permitted for bi-level images."
        T_4_Modified_Modified_Read = 3,

        // "Rec. ITU-T T.82 | ISO/IEC 11544. Commonly known as JBIG. This value is only permitted for bi-level images."
        JBIG_BILEVEL = 4,

        // "Rec. ITU-T T.81 | ISO/IEC 10918-1 or Rec. ITU-T T.84 | ISO/IEC 10918-3. Commonly known as JPEG. [...]
        //  This value is only permitted for continuous tone, greyscale or colour images."
        JPEG = 5,

        JPEG_LS = 6,

        JBIG2 = 8,

        // "Rec. ITU-T T.82 | ISO/IEC 11544. Commonly known as JBIG. This value is permitted for any image permitted by the JBIG standard."
        JBIG_ANY = 9,

        RUN_LENGTH = 10,
        JPEG_XR = 11,
        JPEG_XS = 12,
    };
};

// I.5.3.2 Bits Per Component box
struct JPEG2000BitsPerComponentBox final : public Box {
    BOX_SUBTYPE(JPEG2000BitsPerComponentBox);

    struct BitsPerComponent {
        u8 depth;
        bool is_signed;
    };
    Vector<BitsPerComponent> bits_per_components;
};

// I.5.3.3 Colour Specification box
struct JPEG2000ColorSpecificationBox final : public Box {
    BOX_SUBTYPE(JPEG2000ColorSpecificationBox);

    u8 method { 0 };
    i8 precedence { 0 };
    u8 approximation { 0 };
    u32 enumerated_color_space { 0 }; // Only set if method == Method::Enumerated
    ByteBuffer icc_data;              // Only set if method == Method::ICC_Restricted or Method::ICC_Any

    enum Method {
        // T.800, Table I.9 – Legal METH values

        // "Enumerated Colourspace. This colourspace specification box contains the enumerated value of the colourspace of this image. The
        //  enumerated value is found in the EnumCS field in this box."
        Enumerated = 1,

        // "Restricted ICC profile. This Colour Specification box contains an ICC profile in the PROFILE field. This profile shall specify the
        //  transformation needed to convert the decompressed image data into the PCSXYZ, and shall conform to either the Monochrome Input, the
        //  Three-Component Matrix-Based Input profile class, the Monochrome Display or the Three-Component Matrix-Based Display class and
        //  contain all the required tags specified therein"
        ICC_Restricted = 2,

        // "other values" "Reserved for other ITU-T | ISO uses. If the value of METH is not 1 or 2, there may be fields in this box following the APPROX field,
        //  and a conforming JP2 reader shall ignore the entire Colour Specification box."

        // T.801, Table M.22 – Legal METH values

        // "Any ICC method. This Colour Specification box indicates that the colourspace of the codestream is specified by an
        //  embedded input ICC profile. Contrary to the Restricted ICC method defined in the JP2 file format, this method allows
        //  for any input ICC profile"
        ICC_Any = 3,

        // "Vendor Colour method. This Colour Specification box indicates that the colourspace of the codestream is specified by
        //  a unique vendor defined code."
        Vendor = 4,

        // "Parameterized colourspace. This Colour Specification box indicates that the colourspace of the codestream is
        // parameterized"
        Parameterized = 5,
    };

    enum EnumCS {
        // T.800, Table I.10 – Legal EnumCS values

        // "sRGB as defined by IEC 61966-2-1 with Lmini=0 and Lmaxi=255. This colourspace shall be used with channels carrying unsigned values only."
        sRGB = 16,

        // "A greyscale space where image luminance is related to code values using the sRGB non-linearity given in Equations (2) to (4) of IEC 61966-2-1 (sRGB) specification. [...]
        //  This colourspace shall be used with channels carrying unsigned values only."
        Greyscale = 17,

        // "sYCC as defined by IEC 61966-2-1 Amd. 1with Lmini=0 and Lmaxi=255. This colourspace shall be used with channels carrying unsigned values only."
        sYCC = 18,

        // T.801, Table M.25 – Additional legal EnumCS values

        // "This value shall be used to indicate bi-level images. Each image sample is one bit: 0 = white, 1 = black."
        BiLevel = 0,

        YCbCr1 = 1,
        YCbCr2 = 3,
        YCbCr3 = 4,
        PhotoYCC = 9,
        CMY = 11,
        CMYK = 12,
        YCCK = 13,
        CIELab = 14,

        // "This value shall be used to indicate bi-level images. Each image sample is one bit: 1 = white, 0 = black."
        BiLevel2 = 15,

        // (T.801 also lists 18 for sYCC, but that's already in T.800 above.)

        CIEJab = 19,
        e_sRGB = 20,
        ROMM_RGB = 21,
        YPbPr_1125_60 = 22,
        YPbPr_1150_50 = 23,
        e_sYCC = 24,
        scRGB = 25,
        scRGB_Gray_Scale = 26, // [sic], inconsistent with the spelling of "greyscale" in T.800.
    };
};

// I.5.3.4 Palette box
struct JPEG2000PaletteBox final : public Box {
    BOX_SUBTYPE(JPEG2000PaletteBox);

    struct BitDepth {
        u8 depth;
        bool is_signed;
    };
    Vector<BitDepth> bit_depths;

    // BitDepth::depth is at most 38 per spec (Table I.13).
    // i64 is more than enough. Palettes don't have a ton of entries, so memory use here isn't critical.
    using Color = Vector<i64, 4>;
    Vector<Color> palette_entries;
};

// I.5.3.5 Component Mapping box
struct JPEG2000ComponentMappingBox final : public Box {
    BOX_SUBTYPE(JPEG2000ComponentMappingBox);

    struct Mapping {
        u16 component_index;

        enum Type {
            // "0: Direct use. This channel is created directly from an actual component in the codestream. The index of the
            //     component mapped to this channel is specified in the CMP^i field for this channel."
            Direct = 0,

            // "1: Palette mapping. This channel is created by applying the palette to an actual component in the codestream. The
            //     index of the component mapped into the palette is specified in the CMP^i field for this channel. The column from
            //     the palette to use is specified in the PCOL^i field for this channel."
            Palette = 1,

            // "2 to 255: Reserved for ITU-T | ISO use"
        };
        u8 mapping_type;

        u8 palette_component_index;
    };
    Vector<Mapping> component_mappings;
};

// I.5.3.6 Channel Definition box
struct JPEG2000ChannelDefinitionBox final : public Box {
    BOX_SUBTYPE(JPEG2000ChannelDefinitionBox);

    struct Channel {
        u16 channel_index;
        u16 channel_type;

        // "0              : This channel is associated as the image as a whole (for example, an independent opacity channel that
        //                   should be applied to all colour channels).
        //  1 to (2^16 – 2): This channel is associated with a particular colour as indicated by this value. This value is used to
        //                   associate a particular channel with a particular aspect of the specification of the colourspace of this
        //                   image. For example, indicating that a channel is associated with the red channel of an RGB image allows
        //                   the reader to associate that decoded channel with the Red input to an ICC profile contained within a
        //                   Colour Specification box. Colour indicators are specified in Table I.18.
        //  2^16 – 1       : This channel is not associated with any particular colour."
        u16 channel_association;

        enum Type {
            // T.800, Table I.16 – Typi field values

            // "This channel is the colour image data for the associated colour."
            Color = 0,

            // "Opacity. A sample value of 0 indicates that the sample is 100% transparent, and the maximum value of the
            //  channel (related to the bit depth of the codestream component or the related palette component mapped to this
            //  channel) indicates a 100% opaque sample. All opacity channels shall be mapped from unsigned components."
            Opacity = 1,

            // "Premultiplied opacity. An opacity channel as specified above, except that the value of the opacity channel has
            //  been multiplied into the colour channels for which this channel is associated."
            PremultipliedOpacity = 2,

            // 3 to (2^16 – 2) Reserved for ITU-T | ISO use

            // The type of this channel is not specified.
            Unspecified = 0xFFFF,
        };
    };
    Vector<Channel> channels;
};

// I.5.3.7 Resolution box (superbox)
struct JPEG2000ResolutionBox final : public SuperBox {
    BOX_SUBTYPE(JPEG2000ResolutionBox);
};

struct JPEG2000ResolutionSubboxBase : public Box {
    ErrorOr<void> read_from_stream(BoxStream&);
    virtual void dump(String const& prepend) const override;

    u16 vertical_capture_grid_resolution_numerator { 0 };
    u16 vertical_capture_grid_resolution_denominator { 0 };
    u16 horizontal_capture_grid_resolution_numerator { 0 };
    u16 horizontal_capture_grid_resolution_denominator { 0 };
    i8 vertical_capture_grid_resolution_exponent { 0 };
    i8 horizontal_capture_grid_resolution_exponent { 0 };
};

// I.5.3.7.1 Capture Resolution box
struct JPEG2000CaptureResolutionBox final : public JPEG2000ResolutionSubboxBase {
    BOX_SUBTYPE(JPEG2000CaptureResolutionBox);
};

// I.5.3.7.2 Default Display Resolution box
struct JPEG2000DefaultDisplayResolutionBox final : public JPEG2000ResolutionSubboxBase {
    BOX_SUBTYPE(JPEG2000DefaultDisplayResolutionBox);
};

// I.5.4 Contiguous Codestream box
struct JPEG2000ContiguousCodestreamBox final : public Box {
    BOX_SUBTYPE(JPEG2000ContiguousCodestreamBox);

    ByteBuffer codestream;
};

struct JPEG2000SignatureBox final : public Box {
    BOX_SUBTYPE(JPEG2000SignatureBox);

    u32 signature { 0 };
};

// I.7.3 UUID Info boxes (superbox)
struct JPEG2000UUIDInfoBox final : public SuperBox {
    BOX_SUBTYPE(JPEG2000UUIDInfoBox);
};

// I.7.3.1 UUID List box
struct JPEG2000UUIDListBox final : public Box {
    BOX_SUBTYPE(JPEG2000UUIDListBox);

    Vector<Array<u8, 16>> uuids;
};

// I.7.3.2 Data Entry URL box
struct JPEG2000URLBox final : public Box {
    BOX_SUBTYPE(JPEG2000URLBox);

    ErrorOr<String> url_as_string() const;

    u8 version_number { 0 };
    u32 flag { 0 };
    ByteBuffer url_bytes;
};

}
