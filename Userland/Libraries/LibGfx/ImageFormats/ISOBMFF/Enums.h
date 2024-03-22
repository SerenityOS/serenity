/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>
#include <LibRIFF/ChunkID.h>

namespace Gfx::ISOBMFF {

// Define all Box types.
// In FourCC-asciibetical order.
// The JPEG2000 types are from T-REC-T.800-201511-S!!PDF-E.pdf, Table I.2 – Defined boxes
#define ENUMERATE_ALL()                                        \
    ENUMERATE_ONE(JPEG2000BitsPerComponentBox, "bpcc")         \
    ENUMERATE_ONE(JPEG2000ChannelDefinitionBox, "cdef")        \
    ENUMERATE_ONE(JPEG2000ComponentMappingBox, "cmap")         \
    ENUMERATE_ONE(JPEG2000ColorSpecificationBox, "colr")       \
    ENUMERATE_ONE(FreeBox, "free")                             \
    ENUMERATE_ONE(FileTypeBox, "ftyp")                         \
    ENUMERATE_ONE(JPEG2000ImageHeaderBox, "ihdr")              \
    ENUMERATE_ONE(JPEG2000SignatureBox, "jP  ")                \
    ENUMERATE_ONE(JPEG2000ContiguousCodestreamBox, "jp2c")     \
    ENUMERATE_ONE(JPEG2000HeaderBox, "jp2h")                   \
    ENUMERATE_ONE(JPEG2000IntellectualPropertyBox, "jp2i")     \
    ENUMERATE_ONE(MediaDataBox, "mdat")                        \
    ENUMERATE_ONE(MetaBox, "meta")                             \
    ENUMERATE_ONE(MovieBox, "moov")                            \
    ENUMERATE_ONE(JPEG2000PaletteBox, "pclr")                  \
    ENUMERATE_ONE(JPEG2000ResolutionBox, "res ")               \
    ENUMERATE_ONE(JPEG2000CaptureResolutionBox, "resc")        \
    ENUMERATE_ONE(JPEG2000DefaultDisplayResolutionBox, "resd") \
    ENUMERATE_ONE(JPEG2000UUIDInfoBox, "uinf")                 \
    ENUMERATE_ONE(JPEG2000UUIDListBox, "ulst")                 \
    ENUMERATE_ONE(JPEG2000URLBox, "url ")                      \
    ENUMERATE_ONE(UserExtensionBox, "uuid")                    \
    ENUMERATE_ONE(XMLBox, "xml ")

enum class BoxType : u32 {
    None = 0,

#define ENUMERATE_ONE(box_name, box_4cc) box_name = RIFF::ChunkID(box_4cc).as_number(),

    ENUMERATE_ALL()

#undef ENUMERATE_ONE
};

static bool is_valid_box_type(BoxType type)
{
    return (
#define ENUMERATE_ONE(box_name, _) type == BoxType::box_name ||
        ENUMERATE_ALL()
#undef ENUMERATE_ONE
            false);
}

#undef ENUMERATE_ALL

// Define all FileTypeBox brand identifiers:
#define ENUMERATE_ALL() \
    ENUMERATE_ONE(iso8) \
    ENUMERATE_ONE(avif) \
    ENUMERATE_ONE(avis) \
    ENUMERATE_ONE(mif1) \
    ENUMERATE_ONE(msf1) \
    ENUMERATE_ONE(miaf) \
    ENUMERATE_ONE(MA1A)

enum class BrandIdentifier : u32 {
    None = 0,

#define ENUMERATE_ONE(brand_4cc) brand_4cc = RIFF::ChunkID(#brand_4cc).as_number(),

    ENUMERATE_ALL()

#undef ENUMERATE_ONE
};

#undef ENUMERATE_ALL

}

template<>
struct AK::Formatter<Gfx::ISOBMFF::BoxType> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ISOBMFF::BoxType const& box_type)
    {
        StringView format_string = Gfx::ISOBMFF::is_valid_box_type(box_type) ? "({})"sv : "Unknown Box ({})"sv;
        return Formatter<FormatString>::format(builder, format_string, RIFF::ChunkID::from_number(to_underlying(box_type)));
    }
};

template<>
struct AK::Formatter<Gfx::ISOBMFF::BrandIdentifier> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ISOBMFF::BrandIdentifier const& brand_identifier)
    {
        return Formatter<FormatString>::format(builder, "{}"sv, RIFF::ChunkID::from_number(to_underlying(brand_identifier)));
    }
};
