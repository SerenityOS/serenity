/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>

namespace Gfx::ISOBMFF {

// Define all Box types:
#define ENUMERATE_ALL()               \
    ENUMERATE_ONE(FileTypeBox, ftyp)  \
    ENUMERATE_ONE(MetaBox, meta)      \
    ENUMERATE_ONE(MovieBox, moov)     \
    ENUMERATE_ONE(MediaDataBox, mdat) \
    ENUMERATE_ONE(FreeBox, free)

constexpr u32 fourcc_to_number(char const fourcc[4])
{
    return AK::convert_between_host_and_big_endian((fourcc[0] << 24) | (fourcc[1] << 16) | (fourcc[2] << 8) | fourcc[3]);
}

enum class BoxType : u32 {
    None = 0,

#define ENUMERATE_ONE(box_name, box_4cc) box_name = fourcc_to_number(#box_4cc),

    ENUMERATE_ALL()

#undef ENUMERATE_ONE
};

static Optional<StringView> box_type_to_string(BoxType type)
{
    switch (type) {
#define ENUMERATE_ONE(box_name, box_4cc) \
    case BoxType::box_name:              \
        return #box_name " ('" #box_4cc "')"sv;

        ENUMERATE_ALL()

#undef ENUMERATE_ONE

    default:
        return {};
    }
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

#define ENUMERATE_ONE(brand_4cc) brand_4cc = fourcc_to_number(#brand_4cc),

    ENUMERATE_ALL()

#undef ENUMERATE_ONE
};

static Optional<StringView> brand_identifier_to_string(BrandIdentifier type)
{
    switch (type) {
#define ENUMERATE_ONE(brand_4cc)     \
    case BrandIdentifier::brand_4cc: \
        return #brand_4cc##sv;

        ENUMERATE_ALL()

#undef ENUMERATE_ONE

    default:
        return {};
    }
}

#undef ENUMERATE_ALL

}

template<>
struct AK::Formatter<Gfx::ISOBMFF::BoxType> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ISOBMFF::BoxType const& box_type)
    {
        auto string = Gfx::ISOBMFF::box_type_to_string(box_type);
        if (string.has_value()) {
            return Formatter<FormatString>::format(builder, "{}"sv, string.release_value());
        }
        return Formatter<FormatString>::format(builder, "Unknown Box ('{}')"sv, StringView((char const*)&box_type, 4));
    }
};

template<>
struct AK::Formatter<Gfx::ISOBMFF::BrandIdentifier> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ISOBMFF::BrandIdentifier const& brand_identifier)
    {
        auto string = Gfx::ISOBMFF::brand_identifier_to_string(brand_identifier);
        if (string.has_value()) {
            return Formatter<FormatString>::format(builder, "{}"sv, string.release_value());
        }
        return Formatter<FormatString>::format(builder, "{}"sv, StringView((char const*)&brand_identifier, 4));
    }
};
