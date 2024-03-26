/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Boxes.h"
#include "Reader.h"
#include <AK/Function.h>

namespace Gfx::ISOBMFF {

ErrorOr<BoxHeader> read_box_header(BoxStream& stream)
{
    BoxHeader header;
    u64 total_size = TRY(stream.read_value<BigEndian<u32>>());
    header.type = TRY(stream.read_value<BigEndian<BoxType>>());

    u64 data_size_read = sizeof(u32) + sizeof(BoxType);

    if (total_size == 0) {
        header.contents_size = stream.remaining();
    } else {
        if (total_size == 1) {
            total_size = TRY(stream.read_value<BigEndian<u64>>());
            data_size_read += sizeof(u64);
        }

        header.contents_size = total_size - data_size_read;
    }
    return header;
}

void Box::dump(String const& prepend) const
{
    outln("{}{}", prepend, box_type());
}

ErrorOr<void> FullBox::read_from_stream(BoxStream& stream)
{
    u32 data = TRY(stream.read_value<BigEndian<u32>>());
    // unsigned int(8) version
    version = static_cast<u8>(data >> 24);
    // unsigned int(24) flags
    flags = data & 0xFFF;
    return {};
}

void FullBox::dump(String const& prepend) const
{
    outln("{}{} (version = {}, flags = {:#x})", prepend, box_type(), version, flags);
}

static String add_indent(String const& string)
{
    return MUST(String::formatted("{}  ", string));
}

ErrorOr<void> UnknownBox::read_from_stream(BoxStream& stream)
{
    m_contents_size = stream.remaining();
    TRY(stream.discard_remaining());
    return {};
}

void UnknownBox::dump(String const& prepend) const
{
    Box::dump(prepend);

    auto indented_prepend = add_indent(prepend);
    outln("{}[ {} bytes ]", prepend, m_contents_size);
}

ErrorOr<void> FileTypeBox::read_from_stream(BoxStream& stream)
{
    // unsigned int(32) major_brand;
    major_brand = TRY(stream.read_value<BigEndian<BrandIdentifier>>());
    // unsigned int(32) minor_version;
    minor_version = TRY(stream.read_value<BigEndian<u32>>());

    // unsigned int(32) compatible_brands[]; // to end of the box
    if (stream.remaining() % sizeof(BrandIdentifier) != 0)
        return Error::from_string_literal("FileTypeBox compatible_brands contains a partial brand");

    for (auto minor_brand_count = stream.remaining() / sizeof(BrandIdentifier); minor_brand_count > 0; minor_brand_count--)
        TRY(compatible_brands.try_append(TRY(stream.read_value<BigEndian<BrandIdentifier>>())));

    return {};
}

void FileTypeBox::dump(String const& prepend) const
{
    Box::dump(prepend);

    auto indented_prepend = add_indent(prepend);

    outln("{}- major_brand = {}", prepend, major_brand);
    outln("{}- minor_version = {}", prepend, minor_version);

    StringBuilder compatible_brands_string;
    compatible_brands_string.append("- compatible_brands = { "sv);
    for (size_t i = 0; i < compatible_brands.size() - 1; i++)
        compatible_brands_string.appendff("{}, ", compatible_brands[i]);
    compatible_brands_string.appendff("{} }}", compatible_brands[compatible_brands.size() - 1]);
    outln("{}{}", prepend, compatible_brands_string.string_view());
}

ErrorOr<void> SuperBox::read_from_stream(BoxStream& stream, BoxCallback box_factory)
{
    auto reader = TRY(Gfx::ISOBMFF::Reader::create(MaybeOwned { stream }));
    m_child_boxes = TRY(reader.read_entire_file(move(box_factory)));
    return {};
}

void SuperBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    auto indented_prepend = add_indent(prepend);
    for (auto const& child_box : m_child_boxes)
        child_box->dump(indented_prepend);
}

ErrorOr<void> UserExtensionBox::read_from_stream(BoxStream& stream)
{
    // unsigned int(8)[16] uuid;
    TRY(stream.read_until_filled(uuid));
    // unsigned int(8) data[];
    data = TRY(ByteBuffer::create_uninitialized(stream.remaining()));
    TRY(stream.read_until_filled(data));
    return {};
}

void UserExtensionBox::dump(String const& prepend) const
{
    Box::dump(prepend);

    auto indented_prepend = add_indent(prepend);

    out("{}- uuid = ", prepend);
    for (auto byte : uuid)
        out("{:02x}"sv, byte);
    outln();

    outln("{}- data = [ {} bytes ]", prepend, data.size());
}

}
