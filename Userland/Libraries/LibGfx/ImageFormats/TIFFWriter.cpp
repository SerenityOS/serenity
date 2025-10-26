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
        [](ByteBuffer const&) { return TIFF::Type::Undefined; },

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

static u32 ifd_entry_value_count(IFDEntry const& entry)
{
    VERIFY(!entry.value.is_empty());
    if (entry.value[0].has<ByteBuffer>()) {
        VERIFY(entry.value.size() == 1);
        return entry.value[0].get<ByteBuffer>().size();
    }
    return entry.value.size();
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
    case TIFF::Type::Undefined:
        VERIFY(entry.value.size() == 1);
        TRY(stream.write_until_depleted(entry.value[0].get<ByteBuffer>()));
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return {};
}

static Vector<IFDEntry> make_rgb_ifd(u32 image_width, u32 image_height, u32 image_data_size, bool include_alpha, Optional<ByteBuffer> icc_data)
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

    Vector<IFDEntry> entries;
    entries.append({ TIFF::Tag::ImageWidth, { image_width } });
    entries.append({ TIFF::Tag::ImageLength, { image_height } });
    if (include_alpha)
        entries.append({ TIFF::Tag::BitsPerSample, { 8u, 8u, 8u, 8u } });
    else
        entries.append({ TIFF::Tag::BitsPerSample, { 8u, 8u, 8u } });
    entries.append({ TIFF::Tag::Compression, { static_cast<u32>(TIFF::Compression::NoCompression) } });
    entries.append({ TIFF::Tag::PhotometricInterpretation, { static_cast<u32>(TIFF::PhotometricInterpretation::RGB) } });
    entries.append({ TIFF::Tag::StripOffsets, { 0u } });
    if (include_alpha)
        entries.append({ TIFF::Tag::SamplesPerPixel, { 4u } });
    else
        entries.append({ TIFF::Tag::SamplesPerPixel, { 3u } });
    entries.append({ TIFF::Tag::RowsPerStrip, { image_height } });
    entries.append({ TIFF::Tag::StripByteCounts, { image_data_size } });
    entries.append({ TIFF::Tag::XResolution, { TIFF::Rational<u32> { 1, 1 } } });
    entries.append({ TIFF::Tag::YResolution, { TIFF::Rational<u32> { 1, 1 } } });
    entries.append({ TIFF::Tag::ResolutionUnit, { static_cast<u32>(TIFF::ResolutionUnit::NoAbsolute) } });

    if (include_alpha)
        entries.append({ TIFF::Tag::ExtraSamples, { static_cast<u32>(TIFF::ExtraSample::UnassociatedAlpha) } });

    if (icc_data.has_value()) {
        // https://www.color.org/technotes/ICC-Technote-ProfileEmbedding.pdf
        // "An ICC profile is embedded, in its entirety, as a single TIFF field or Image File Directory (IFD) entry in
        // the IFD containing the corresponding image data."
        entries.append({ TIFF::Tag::ICCProfile, { move(icc_data.value()) } });
    }
    return entries;
}

static Vector<IFDEntry> make_cmyk_ifd(u32 image_width, u32 image_height, u32 image_data_size, Optional<ByteBuffer> icc_data)
{
    Vector<IFDEntry> entries;
    entries.append({ TIFF::Tag::ImageWidth, { image_width } });
    entries.append({ TIFF::Tag::ImageLength, { image_height } });
    entries.append({ TIFF::Tag::BitsPerSample, { 8u, 8u, 8u, 8u } });
    entries.append({ TIFF::Tag::Compression, { static_cast<u32>(TIFF::Compression::NoCompression) } });
    entries.append({ TIFF::Tag::PhotometricInterpretation, { static_cast<u32>(TIFF::PhotometricInterpretation::CMYK) } });
    entries.append({ TIFF::Tag::StripOffsets, { 0u } });
    entries.append({ TIFF::Tag::SamplesPerPixel, { 4u } });
    entries.append({ TIFF::Tag::RowsPerStrip, { image_height } });
    entries.append({ TIFF::Tag::StripByteCounts, { image_data_size } });
    entries.append({ TIFF::Tag::XResolution, { TIFF::Rational<u32> { 1, 1 } } });
    entries.append({ TIFF::Tag::YResolution, { TIFF::Rational<u32> { 1, 1 } } });
    entries.append({ TIFF::Tag::ResolutionUnit, { static_cast<u32>(TIFF::ResolutionUnit::NoAbsolute) } });
    if (icc_data.has_value()) {
        // https://www.color.org/technotes/ICC-Technote-ProfileEmbedding.pdf
        // "An ICC profile is embedded, in its entirety, as a single TIFF field or Image File Directory (IFD) entry in
        // the IFD containing the corresponding image data."
        entries.append({ TIFF::Tag::ICCProfile, { move(icc_data.value()) } });
    }
    return entries;
}

static ErrorOr<void> encode_ifd(Stream& stream, u32 ifd_offset, Vector<IFDEntry> entries)
{
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
        if (ifd_entry_value_count(entry) * TIFF::size_of_type(type) > 4)
            size_of_ifd_entry_data_area += ifd_entry_value_count(entry) * TIFF::size_of_type(type);
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
        TRY(stream.write_value<LittleEndian<u32>>(ifd_entry_value_count(entry)));

        auto data_size = ifd_entry_value_count(entry) * TIFF::size_of_type(type);
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

static bool is_bitmap_fully_opaque(Bitmap const& bitmap)
{
    for (ARGB32 const& pixel : bitmap)
        if (Color::from_argb(pixel).alpha() != 255)
            return false;
    return true;
}

ErrorOr<void> TIFFWriter::encode(Stream& stream, Bitmap const& bitmap, Options const& options)
{
    Optional<ByteBuffer> icc_data;
    if (options.icc_data.has_value()) {
        // FIXME: TIFF::Value currently only supports owning ByteBuffers. Needing to copy here is a bit unfortunate.
        icc_data = TRY(ByteBuffer::copy(options.icc_data.value()));
    }

    u32 const ifd_offset = tiff_header_size;
    TRY(encode_tiff_header(stream, ifd_offset));

    bool const include_alpha = !is_bitmap_fully_opaque(bitmap);

    u32 const image_data_size = bitmap.width() * bitmap.height() * (include_alpha ? 4 : 3);
    auto ifd_entries = make_rgb_ifd(bitmap.width(), bitmap.height(), image_data_size, include_alpha, move(icc_data));
    TRY(encode_ifd(stream, ifd_offset, move(ifd_entries)));

    for (ARGB32 const& pixel : bitmap) {
        auto color = Color::from_argb(pixel);
        TRY(stream.write_value<u8>(color.red()));
        TRY(stream.write_value<u8>(color.green()));
        TRY(stream.write_value<u8>(color.blue()));
        if (include_alpha)
            TRY(stream.write_value<u8>(color.alpha()));
    }
    return {};
}

ErrorOr<void> TIFFWriter::encode(Stream& stream, CMYKBitmap const& bitmap, Options const& options)
{
    Optional<ByteBuffer> icc_data;
    if (options.icc_data.has_value()) {
        // FIXME: TIFF::Value currently only supports owning ByteBuffers. Needing to copy here is a bit unfortunate.
        icc_data = TRY(ByteBuffer::copy(options.icc_data.value()));
    }

    u32 const ifd_offset = tiff_header_size;
    TRY(encode_tiff_header(stream, ifd_offset));

    u32 const image_data_size = bitmap.size().width() * bitmap.size().height() * 4;
    auto ifd_entries = make_cmyk_ifd(bitmap.size().width(), bitmap.size().height(), image_data_size, move(icc_data));
    TRY(encode_ifd(stream, ifd_offset, move(ifd_entries)));

    for (int y = 0; y < bitmap.size().height(); ++y) {
        for (int x = 0; x < bitmap.size().width(); ++x) {
            CMYK const& pixel = bitmap.scanline(y)[x];
            TRY(stream.write_value<u8>(pixel.c));
            TRY(stream.write_value<u8>(pixel.m));
            TRY(stream.write_value<u8>(pixel.y));
            TRY(stream.write_value<u8>(pixel.k));
        }
    }
    return {};
}

}
