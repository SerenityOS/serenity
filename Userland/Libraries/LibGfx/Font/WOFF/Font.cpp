/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/IntegralMath.h>
#include <AK/MemoryStream.h>
#include <LibCompress/Zlib.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/WOFF/Font.h>

namespace WOFF {

static constexpr u32 WOFF_SIGNATURE = 0x774F4646;
static constexpr size_t WOFF_HEADER_SIZE = 44;
static constexpr size_t WOFF_TABLE_SIZE = 20;
static constexpr size_t SFNT_HEADER_SIZE = 12;
static constexpr size_t SFNT_TABLE_SIZE = 16;

static u16 be_u16(u8 const* ptr)
{
    return (((u16)ptr[0]) << 8) | ((u16)ptr[1]);
}

static void be_u16(u8* ptr, u16 value)
{
    ptr[0] = (value >> 8) & 0xff;
    ptr[1] = value & 0xff;
}

static u32 be_u32(u8 const* ptr)
{
    return (((u32)ptr[0]) << 24) | (((u32)ptr[1]) << 16) | (((u32)ptr[2]) << 8) | ((u32)ptr[3]);
}

static void be_u32(u8* ptr, u32 value)
{
    ptr[0] = (value >> 24) & 0xff;
    ptr[1] = (value >> 16) & 0xff;
    ptr[2] = (value >> 8) & 0xff;
    ptr[3] = value & 0xff;
}

static u16 pow_2_less_than_or_equal(u16 x)
{
    u16 result = 1;
    while (result < x)
        result <<= 1;
    return result;
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_file(DeprecatedString path, unsigned int index)
{
    auto file = TRY(Core::MappedFile::map(path));
    return try_load_from_externally_owned_memory(file->bytes(), index);
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_externally_owned_memory(ReadonlyBytes buffer, unsigned int index)
{
    // https://www.w3.org/TR/WOFF/#WOFFHeader
    if (buffer.size() < WOFF_HEADER_SIZE)
        return Error::from_string_literal("WOFF file too small");

    // The signature field in the WOFF header MUST contain the "magic number" 0x774F4646. If the field does not contain this value, user agents MUST reject the file as invalid.
    u32 signature = be_u32(buffer.data());
    if (signature != WOFF_SIGNATURE)
        return Error::from_string_literal("Invalid WOFF signature");
    // The flavor field corresponds to the "sfnt version" field found at the beginning of an sfnt file,
    // indicating the type of font data contained. Although only fonts of type 0x00010000 (the version number 1.0 as a 16.16 fixed-point value, indicating TrueType glyph data)
    // and 0x4F54544F (the tag 'OTTO', indicating CFF glyph data) are widely supported at present,
    // it is not an error in the WOFF file if the flavor field contains a different value,
    // indicating a WOFF-packaged version of a different sfnt flavor.
    // (The value 0x74727565 'true' has been used for some TrueType-flavored fonts on Mac OS, for example.)
    // Whether client software will actually support other types of sfnt font data is outside the scope of the WOFF specification, which simply describes how the sfnt is repackaged for Web use.
    u32 flavor = be_u32(buffer.offset(4));           // The "sfnt version" of the input font.
    u32 length = be_u32(buffer.offset(8));           // Total size of the WOFF file.
    u16 num_tables = be_u16(buffer.offset(12));      // Number of entries in directory of font tables.
    u16 reserved = be_u16(buffer.offset(14));        // Reserved; set to zero.
    u32 total_sfnt_size = be_u32(buffer.offset(16)); // Total size needed for the uncompressed font data, including the sfnt header, directory, and font tables (including padding).
    // Skip: major_version, minor_version
    u32 meta_offset = be_u32(buffer.offset(24)); // Offset to metadata block, from beginning of WOFF file.
    u32 meta_length = be_u32(buffer.offset(28)); // Length of compressed metadata block.
    // Skip: meta_orig_length
    u32 priv_offset = be_u32(buffer.offset(36)); // Offset to private data block, from beginning of WOFF file.
    u32 priv_length = be_u32(buffer.offset(40)); // Length of private data block.
    if (length > buffer.size())
        return Error::from_string_literal("Invalid WOFF length");
    if (reserved != 0)
        return Error::from_string_literal("Invalid WOFF reserved field");
    if (meta_length == 0 && meta_offset != 0)
        return Error::from_string_literal("Invalid WOFF meta block offset");
    if (priv_length == 0 && priv_offset != 0)
        return Error::from_string_literal("Invalid WOFF private block offset");
    if (WOFF_HEADER_SIZE + num_tables * WOFF_TABLE_SIZE > length)
        return Error::from_string_literal("Truncated WOFF table directory");
    if (total_sfnt_size > 10 * MiB)
        return Error::from_string_literal("Uncompressed font is more than 10 MiB");
    auto font_buffer = TRY(ByteBuffer::create_zeroed(total_sfnt_size));

    // ISO-IEC 14496-22:2019 4.5.1 Offset table
    u16 search_range = pow_2_less_than_or_equal(num_tables);
    be_u32(font_buffer.data() + 0, flavor);
    be_u16(font_buffer.data() + 4, num_tables);
    be_u16(font_buffer.data() + 6, search_range * 16);
    be_u16(font_buffer.data() + 8, AK::log2(search_range));
    be_u16(font_buffer.data() + 10, num_tables * 16 - search_range * 16);

    size_t font_buffer_offset = SFNT_HEADER_SIZE + num_tables * SFNT_TABLE_SIZE;
    for (size_t i = 0; i < num_tables; ++i) {
        size_t base_offset = WOFF_HEADER_SIZE + i * WOFF_TABLE_SIZE;
        u32 tag = be_u32(buffer.offset(base_offset));
        u32 offset = be_u32(buffer.offset(base_offset + 4));
        u32 comp_length = be_u32(buffer.offset(base_offset + 8));
        u32 orig_length = be_u32(buffer.offset(base_offset + 12));
        u32 orig_checksum = be_u32(buffer.offset(base_offset + 16));

        if ((size_t)offset + comp_length > length)
            return Error::from_string_literal("Truncated WOFF table");
        if (font_buffer_offset + orig_length > font_buffer.size())
            return Error::from_string_literal("Uncompressed WOFF table too big");
        if (comp_length < orig_length) {
            auto compressed_data_stream = make<FixedMemoryStream>(buffer.slice(offset, comp_length));
            auto decompressor = TRY(Compress::ZlibDecompressor::create(move(compressed_data_stream)));
            auto decompressed = TRY(decompressor->read_until_eof());
            if (orig_length != decompressed.size())
                return Error::from_string_literal("Invalid decompressed WOFF table length");
            font_buffer.overwrite(font_buffer_offset, decompressed.data(), orig_length);
        } else {
            if (comp_length != orig_length)
                return Error::from_string_literal("Invalid uncompressed WOFF table length");
            font_buffer.overwrite(font_buffer_offset, buffer.data() + offset, orig_length);
        }

        // ISO-IEC 14496-22:2019 4.5.2 Table directory
        size_t table_directory_offset = SFNT_HEADER_SIZE + i * SFNT_TABLE_SIZE;
        be_u32(font_buffer.data() + table_directory_offset, tag);
        be_u32(font_buffer.data() + table_directory_offset + 4, orig_checksum);
        be_u32(font_buffer.data() + table_directory_offset + 8, font_buffer_offset);
        be_u32(font_buffer.data() + table_directory_offset + 12, orig_length);

        font_buffer_offset += orig_length;
    }

    auto input_font = TRY(OpenType::Font::try_load_from_externally_owned_memory(font_buffer.bytes(), index));
    auto font = adopt_ref(*new Font(input_font, move(font_buffer)));
    return font;
}

}
