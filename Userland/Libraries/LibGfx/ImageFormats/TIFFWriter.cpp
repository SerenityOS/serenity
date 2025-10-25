/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// See TIFFLoader.h for spec links.

#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <AK/Stream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>
#include <LibGfx/ImageFormats/TIFFWriter.h>

namespace Gfx {

namespace {

struct IFDEntry {
    TIFF::Tag tag;
    Vector<TIFF::Value, 1> value;
};

}

static constexpr u32 tiff_header_size = 8;

static ErrorOr<void> encode_tiff_header(Stream& stream, u32 ifd_offset)
{
    // Section 2: TIFF Structure, Image File Header

    // Always write little-endian TIFFs.
    TRY(stream.write_value<u8>('I'));
    TRY(stream.write_value<u8>('I'));

    TRY(stream.write_value<LittleEndian<u16>>(42));

    // Offset to first IFD.
    TRY(stream.write_value<LittleEndian<u32>>(ifd_offset));

    return {};
}

static TIFF::Type type_for_ifd_entry(IFDEntry const& entry)
{
    VERIFY(!entry.value.is_empty());

    return entry.value[0].visit(
        [&](u32) {
            bool fits_in_short = true;
            for (auto const& v : entry.value) {
                if (v.get<u32>() > 0xFFFF) {
                    fits_in_short = false;
                    break;
                }
            }
            if (fits_in_short)
                return TIFF::Type::UnsignedShort;
            return TIFF::Type::UnsignedLong;
        },

        [](TIFF::Rational<u32>) { return TIFF::Type::UnsignedRational; },

        // FIXME: Add more types as needed.
        [](auto const&) -> TIFF::Type { TODO(); });
}

static ErrorOr<void> encode_ifd_entry_value_data(Stream& stream, IFDEntry const& entry)
{
    switch (type_for_ifd_entry(entry)) {
    case TIFF::Type::UnsignedShort:
        for (auto const& value : entry.value)
            TRY(stream.write_value<LittleEndian<u16>>(value.get<u32>()));
        break;
    case TIFF::Type::UnsignedLong:
        for (auto const& value : entry.value)
            TRY(stream.write_value<LittleEndian<u32>>(value.get<u32>()));
        break;
    case TIFF::Type::UnsignedRational:
        for (auto const& v : entry.value) {
            auto const& rational = v.get<TIFF::Rational<u32>>();
            TRY(stream.write_value<LittleEndian<u32>>(rational.numerator));
            TRY(stream.write_value<LittleEndian<u32>>(rational.denominator));
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return {};
}

static ErrorOr<void> encode_ifd(Stream& stream, u32 image_width, u32 image_height, u32 ifd_offset, u32 image_data_size)
{
    // Section 2: TIFF Structure, Image File Directory
    //  "An Image File Directory (IFD) consists of a 2-byte count of the number of direc-
    //  tory entries (i.e., the number of fields), followed by a sequence of 12-byte field
    //  entries, followed by a 4-byte offset of the next IFD (or 0 if none). (Do not forget to
    //  write the 4 bytes of 0 after the last IFD.)"

    // Section 6: RGB Full Color Images, Required Fields for RGB Images
    // "These are the required fields for RGB images (in numerical order):
    //
    // TagName                Decimal  Hex  Type            Value
    // ---------------------------------------------------------------
    // ImageWidth                 256  100  SHORT or LONG
    // ImageLength                257  101  SHORT or LONG
    // BitsPerSample              258  102  SHORT           8,8,8
    // Compression                259  103  SHORT           1 or 32773
    // PhotometricInterpretation  262  106  SHORT           2
    // StripOffsets               273  111  SHORT or LONG
    // SamplesPerPixel            277  115  SHORT           3 or more
    // RowsPerStrip               278  116  SHORT or LONG
    // StripByteCounts            279  117  LONG or SHORT
    // XResolution                282  11A  RATIONAL
    // YResolution                283  11B  RATIONAL
    // ResolutionUnit             296  128  SHORT           1, 2 or 3"

    // FIXME: Also write alpha.
    Vector<IFDEntry> entries;
    entries.append({ TIFF::Tag::ImageWidth, { image_width } });
    entries.append({ TIFF::Tag::ImageLength, { image_height } });
    entries.append({ TIFF::Tag::BitsPerSample, { 8u, 8u, 8u } });
    entries.append({ TIFF::Tag::Compression, { static_cast<u32>(TIFF::Compression::NoCompression) } });
    entries.append({ TIFF::Tag::PhotometricInterpretation, { static_cast<u32>(TIFF::PhotometricInterpretation::RGB) } });
    entries.append({ TIFF::Tag::StripOffsets, { 0u } });
    entries.append({ TIFF::Tag::SamplesPerPixel, { 3u } });
    entries.append({ TIFF::Tag::RowsPerStrip, { image_height } });
    entries.append({ TIFF::Tag::StripByteCounts, { image_data_size } });
    entries.append({ TIFF::Tag::XResolution, { TIFF::Rational<u32> { 1, 1 } } });
    entries.append({ TIFF::Tag::YResolution, { TIFF::Rational<u32> { 1, 1 } } });
    entries.append({ TIFF::Tag::ResolutionUnit, { static_cast<u32>(TIFF::ResolutionUnit::NoAbsolute) } });

    // "The entries in an IFD must be sorted in ascending order by Tag."
    for (size_t i = 1; i < entries.size(); ++i)
        VERIFY(entries[i - 1].tag < entries[i].tag);

    // Compute sizes.
    u32 size_of_ifd = 0;
    u32 size_of_ifd_entry_data_area = 0;
    size_of_ifd += 2; // u16 number of entries
    for (auto const& entry : entries) {
        size_of_ifd += 2; // u16 tag
        size_of_ifd += 2; // u16 type
        size_of_ifd += 4; // u32 count
        size_of_ifd += 4; // u32 value or offset

        auto type = type_for_ifd_entry(entry);
        if (entry.value.size() * TIFF::size_of_type(type) > 4)
            size_of_ifd_entry_data_area += entry.value.size() * TIFF::size_of_type(type);
    }
    size_of_ifd += 4; // u32 offset to next IFD

    // Update entries that need offsets.
    auto strip_offsets = entries.find_if([](auto& entry) {
        return entry.tag == TIFF::Tag::StripOffsets;
    });
    strip_offsets->value[0] = ifd_offset + size_of_ifd + size_of_ifd_entry_data_area;

    // Finally, write the IFD.
    TRY(stream.write_value<LittleEndian<u16>>(entries.size()));
    AllocatingMemoryStream value_data_stream;
    for (auto const& entry : entries) {
        auto type = type_for_ifd_entry(entry);

        TRY(stream.write_value<LittleEndian<u16>>(to_underlying(entry.tag)));
        TRY(stream.write_value<LittleEndian<u16>>(to_underlying(type)));
        TRY(stream.write_value<LittleEndian<u32>>(entry.value.size()));

        auto data_size = entry.value.size() * TIFF::size_of_type(type);
        if (data_size <= 4) {
            TRY(encode_ifd_entry_value_data(stream, entry));
            for (u32 i = data_size; i < 4; ++i)
                TRY(stream.write_value<u8>(0));
        } else {
            u32 offset = ifd_offset + size_of_ifd + value_data_stream.used_buffer_size();
            TRY(stream.write_value<LittleEndian<u32>>(offset));
            TRY(encode_ifd_entry_value_data(value_data_stream, entry));
        }
    }
    TRY(stream.write_value<LittleEndian<u32>>(0));

    ByteBuffer value_data_buffer = TRY(value_data_stream.read_until_eof());
    VERIFY(value_data_buffer.size() == size_of_ifd_entry_data_area);
    TRY(stream.write_until_depleted(value_data_buffer));

    return {};
}

ErrorOr<void> TIFFWriter::encode(Stream& stream, Bitmap const& bitmap, Options const&)
{
    u32 const ifd_offset = tiff_header_size;
    TRY(encode_tiff_header(stream, ifd_offset));

    u32 const image_data_size = bitmap.width() * bitmap.height() * 3;
    TRY(encode_ifd(stream, bitmap.width(), bitmap.height(), ifd_offset, image_data_size));

    for (ARGB32 const& pixel : bitmap) {
        auto color = Color::from_argb(pixel);
        TRY(stream.write_value<u8>(color.red()));
        TRY(stream.write_value<u8>(color.green()));
        TRY(stream.write_value<u8>(color.blue()));
    }
    return {};
}

}
