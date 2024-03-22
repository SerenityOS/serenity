/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JPEG2000Boxes.h"
#include <AK/Function.h>

namespace Gfx::ISOBMFF {

ErrorOr<void> JPEG2000HeaderBox::read_from_stream(BoxStream& stream)
{
    auto make_subbox = [](BoxType type, BoxStream& stream) -> ErrorOr<Optional<NonnullOwnPtr<Box>>> {
        switch (type) {
        case BoxType::JPEG2000ImageHeaderBox:
            return TRY(JPEG2000ImageHeaderBox::create_from_stream(stream));
        default:
            return OptionalNone {};
        }
    };

    TRY(SuperBox::read_from_stream(stream, move(make_subbox)));
    return {};
}

void JPEG2000HeaderBox::dump(String const& prepend) const
{
    SuperBox::dump(prepend);
}

ErrorOr<void> JPEG2000ImageHeaderBox::read_from_stream(BoxStream& stream)
{
    height = TRY(stream.read_value<BigEndian<u32>>());
    width = TRY(stream.read_value<BigEndian<u32>>());
    num_components = TRY(stream.read_value<BigEndian<u16>>());
    bits_per_component = TRY(stream.read_value<u8>());
    compression_type = TRY(stream.read_value<u8>());
    is_colorspace_unknown = TRY(stream.read_value<u8>());
    contains_intellectual_property_rights = TRY(stream.read_value<u8>());
    return {};
}

void JPEG2000ImageHeaderBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- height = {}", prepend, height);
    outln("{}- width = {}", prepend, width);
    outln("{}- num_components = {}", prepend, num_components);
    if (bits_per_component == 0xFF) {
        outln("{}- components vary in bit depth", prepend);
    } else {
        outln("{}- are_components_signed = {}", prepend, (bits_per_component & 0x80) != 0);
        outln("{}- bits_per_component = {}", prepend, (bits_per_component & 0x7f) + 1);
    }
    outln("{}- compression_type = {}", prepend, compression_type);
    outln("{}- is_colorspace_unknown = {}", prepend, is_colorspace_unknown);
    outln("{}- contains_intellectual_property_rights = {}", prepend, contains_intellectual_property_rights);
}

ErrorOr<void> JPEG2000SignatureBox::read_from_stream(BoxStream& stream)
{
    signature = TRY(stream.read_value<BigEndian<u32>>());
    return {};
}

void JPEG2000SignatureBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- signature = {:#08x}", prepend, signature);
}

}
