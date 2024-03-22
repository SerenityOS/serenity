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
        case BoxType::JPEG2000ColorSpecificationBox:
            return TRY(JPEG2000ColorSpecificationBox::create_from_stream(stream));
        case BoxType::JPEG2000ImageHeaderBox:
            return TRY(JPEG2000ImageHeaderBox::create_from_stream(stream));
        case BoxType::JPEG2000ResolutionBox:
            return TRY(JPEG2000ResolutionBox::create_from_stream(stream));
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

ErrorOr<void> JPEG2000ColorSpecificationBox::read_from_stream(BoxStream& stream)
{
    method = TRY(stream.read_value<u8>());
    precedence = TRY(stream.read_value<i8>());
    approximation = TRY(stream.read_value<u8>());
    if (method == 1)
        enumerated_color_space = TRY(stream.read_value<BigEndian<u32>>());
    if (method == 2) {
        ByteBuffer local_icc_data = TRY(ByteBuffer::create_uninitialized(stream.remaining()));
        TRY(stream.read_until_filled(local_icc_data));
        icc_data = move(local_icc_data);
    }
    return {};
}

void JPEG2000ColorSpecificationBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- method = {}", prepend, method);
    outln("{}- precedence = {}", prepend, precedence);
    outln("{}- approximation = {}", prepend, approximation);
    if (method == 1)
        outln("{}- enumerated_color_space = {}", prepend, enumerated_color_space);
    if (method == 2)
        outln("{}- icc_data = {} bytes", prepend, icc_data.size());
}

ErrorOr<void> JPEG2000ResolutionBox::read_from_stream(BoxStream& stream)
{
    auto make_subbox = [](BoxType type, BoxStream& stream) -> ErrorOr<Optional<NonnullOwnPtr<Box>>> {
        switch (type) {
        case BoxType::JPEG2000CaptureResolutionBox:
            return TRY(JPEG2000CaptureResolutionBox::create_from_stream(stream));
        default:
            return OptionalNone {};
        }
    };

    TRY(SuperBox::read_from_stream(stream, move(make_subbox)));
    return {};
}

void JPEG2000ResolutionBox::dump(String const& prepend) const
{
    SuperBox::dump(prepend);
}

ErrorOr<void> JPEG2000CaptureResolutionBox::read_from_stream(BoxStream& stream)
{
    vertical_capture_grid_resolution_numerator = TRY(stream.read_value<BigEndian<u16>>());
    vertical_capture_grid_resolution_denominator = TRY(stream.read_value<BigEndian<u16>>());
    horizontal_capture_grid_resolution_numerator = TRY(stream.read_value<BigEndian<u16>>());
    horizontal_capture_grid_resolution_denominator = TRY(stream.read_value<BigEndian<u16>>());
    vertical_capture_grid_resolution_exponent = TRY(stream.read_value<i8>());
    horizontal_capture_grid_resolution_exponent = TRY(stream.read_value<i8>());
    return {};
}

void JPEG2000CaptureResolutionBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- vertical_capture_grid_resolution = {}/{} * 10^{}", prepend, vertical_capture_grid_resolution_numerator, vertical_capture_grid_resolution_denominator, vertical_capture_grid_resolution_exponent);
    outln("{}- horizontal_capture_grid_resolution = {}/{} * 10^{}", prepend, horizontal_capture_grid_resolution_numerator, horizontal_capture_grid_resolution_denominator, horizontal_capture_grid_resolution_exponent);
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

ErrorOr<void> JPEG2000UUIDInfoBox::read_from_stream(BoxStream& stream)
{
    auto make_subbox = [](BoxType type, BoxStream& stream) -> ErrorOr<Optional<NonnullOwnPtr<Box>>> {
        switch (type) {
        case BoxType::JPEG2000UUIDListBox:
            return TRY(JPEG2000UUIDListBox::create_from_stream(stream));
        default:
            return OptionalNone {};
        }
    };

    TRY(SuperBox::read_from_stream(stream, move(make_subbox)));
    return {};
}

void JPEG2000UUIDInfoBox::dump(String const& prepend) const
{
    SuperBox::dump(prepend);
}

ErrorOr<void> JPEG2000UUIDListBox::read_from_stream(BoxStream& stream)
{
    u16 count = TRY(stream.read_value<BigEndian<u16>>());
    for (u32 i = 0; i < count; ++i) {
        Array<u8, 16> uuid;
        TRY(stream.read_until_filled(uuid));
        uuids.append(uuid);
    }
    return {};
}

void JPEG2000UUIDListBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    for (auto const& uuid : uuids) {
        out("{}- ", prepend);
        for (auto byte : uuid) {
            out("{:02x}", byte);
        }
        outln();
    }
}

}
