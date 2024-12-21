/*
 * Copyright (c) 2024, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Math.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>
#include <limits.h>
#include <unistd.h>

#define PCF_GLYPH_PAD_MASK (3 << 0)
#define PCF_BYTE_MASK (1 << 2)
#define PCF_BIT_MASK (1 << 3)
#define PCF_SCAN_UNIT_MASK (3 << 4)

#define PCF_PROPERTIES (1 << 0)
#define PCF_ACCELERATORS (1 << 1)
#define PCF_METRICS (1 << 2)
#define PCF_BITMAPS (1 << 3)
#define PCF_INK_METRICS (1 << 4)
#define PCF_BDF_ENCODINGS (1 << 5)
#define PCF_SWIDTHS (1 << 6)
#define PCF_GLYPH_NAMES (1 << 7)
#define PCF_BDF_ACCELERATORS (1 << 8)

#define PCF_DEFAULT_FORMAT 0x00000000
#define PCF_INKBOUNDS 0x00000200
#define PCF_ACCEL_W_INKBOUNDS 0x00000100
#define PCF_COMPRESSED_METRICS 0x00000100

class PCFFile : public RefCounted<PCFFile> {
public:
    static ErrorOr<NonnullRefPtr<PCFFile>> create(ReadonlyBytes);

    ErrorOr<String> construct_filename() const;

    Optional<i16> glyph_index_for(u16 code_point) const;
    ErrorOr<void> draw_glyph(u16 index, Gfx::GlyphBitmap&) const;
    u8 glyph_width(u16 index) const { return m_glyphs.at(index).width; }
    u8 baseline() const;
    size_t highest_codepoint() const;

    String family() const;
    String name() const;
    String weight_name() const;

    i32 weight() const;
    i32 relative_weight() const;
    i32 slope() const;
    i32 pixel_size() const;
    i32 x_height() const;
    Gfx::IntSize glyph_size() const;
    bool is_fixed_width() const { return m_acc.constant_width != 0; }
    size_t glyph_count() const { return m_glyphs.size(); }

private:
    PCFFile(ReadonlyBytes);

    ErrorOr<void> populate_tables();
    ErrorOr<void> convert_glyphs();

    template<typename T>
    ErrorOr<T> read(i32 format) const
    {
        T value;
        if (format & PCF_BYTE_MASK)
            value = TRY(m_stream.read_value<BigEndian<T>>());
        else
            value = TRY(m_stream.read_value<LittleEndian<T>>());

        if (!(format & PCF_BIT_MASK))
            return value;

        return value;
    }

    struct Header {
        char magic[4];
        i32 table_count;
        struct TOCEntry {
            i32 type;
            i32 format;
            i32 size;
            i32 offset;
        };
    };

    typedef Variant<String, i32> Property;
    struct PropertiesTable {
        i32 nprops;
        struct Props {
            i32 name_offset;
            i8 is_string_prop;
            i32 value;
        };
    };

    struct Metrics {
        i16 left_side_bearing;
        i16 right_side_bearing;
        i16 character_width;
        i16 character_ascent;
        i16 character_descent;
    };

    struct AcceleratorTable {
        u8 no_overlap;
        u8 constant_metrics;
        u8 terminal_font;
        u8 constant_width;
        u8 ink_inside;
        u8 ink_metrics;
        u8 draw_direction;
        i32 font_ascent;
        i32 font_descent;
        i32 max_overlap;
    };

    struct BitmapData {
        i32 format;
        i32 glyph_count;
        Vector<i32> offsets;
        i32 bitmap_sizes[4];
        ByteBuffer data;
    };

    struct EncodingTable {
        i16 min_char_or_byte2;
        i16 max_char_or_byte2;
        i16 min_byte1;
        i16 max_byte1;
        i16 default_char;
        Vector<i16> indices;
    };

    struct Glyph {
        u8 width;
        Vector<u8> data;
    };

    BitmapData m_bitmap_data;

    EncodingTable m_encoding;

    Vector<Header::TOCEntry> m_tables;
    HashMap<String, Property> m_properties;
    Vector<Metrics> m_metrics;
    Vector<Metrics> m_ink_metrics;
    Vector<Glyph> m_glyphs;
    AcceleratorTable m_acc;

    i16 m_max_ascent { 0 };
    i16 m_max_descent { 0 };

    i16 m_max_width { 0 };

    mutable FixedMemoryStream m_stream;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView input_path;
    String output_path;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Convert an X11 PCF (Portable Compiled Format) font to Serenity's format.");
    args_parser.add_positional_argument(input_path, "Path to PCF file", "path", Core::ArgsParser::Required::No);
    args_parser.add_option(output_path, "Path to output file", "output", 'o', "FILE");
    if (!args_parser.parse(arguments))
        return -1;

    OwnPtr<Core::File> file;

    if (input_path.is_empty()) {
        file = TRY(Core::File::standard_input());
    } else {
        file = TRY(Core::File::open(input_path, Core::File::OpenMode::Read));
    }

    auto buffer = TRY(file->read_until_eof());
    auto pcf = TRY(PCFFile::create(buffer));

    if (pcf->glyph_size().width() > 32 || pcf->glyph_size().height() > 32) {
        outln(stderr, "At this time, glyphs may only be 32px wide");
        return -1;
    }

    auto glyph_size = pcf->glyph_size();
    auto bitmap_font = TRY(Gfx::BitmapFont::create(glyph_size.height(), glyph_size.width(), pcf->is_fixed_width(), pcf->highest_codepoint()));
    bitmap_font->set_family(pcf->family());
    bitmap_font->set_name(pcf->name());
    bitmap_font->set_presentation_size(pcf->pixel_size());
    bitmap_font->set_glyph_spacing(0);
    bitmap_font->set_weight(pcf->weight());
    bitmap_font->set_slope(pcf->slope());
    bitmap_font->set_baseline(pcf->baseline());

    if (output_path.is_empty()) {
        output_path = TRY(pcf->construct_filename());
    }

    for (size_t i = 0; i < pcf->highest_codepoint(); ++i) {
        auto maybe_glyph = pcf->glyph_index_for(i);
        if (!maybe_glyph.has_value())
            continue;

        auto pcf_index = maybe_glyph.value();

        bitmap_font->set_glyph_width(i, pcf->glyph_width(pcf_index));
        auto bitmap = bitmap_font->raw_glyph(i).glyph_bitmap();
        TRY(pcf->draw_glyph(pcf_index, bitmap));
    }

    auto set = TRY(bitmap_font->masked_character_set());
    TRY(set->write_to_file(output_path.to_byte_string()));

    bool printed_hyperlink = false;
    if (isatty(STDOUT_FILENO)) {
        auto full_path_or_error = FileSystem::real_path(output_path);
        if (!full_path_or_error.is_error()) {
            auto url = URL::create_with_file_scheme(full_path_or_error.value(), {});
            out("\033]8;;{}\033\\", url.serialize());
            printed_hyperlink = true;
        }
    }

    out("{}", output_path);

    if (printed_hyperlink) {
        out("\033]8;;\033\\");
    }

    outln();

    return 0;
}

PCFFile::PCFFile(ReadonlyBytes bytes)
    : m_stream(bytes)
{
}

ErrorOr<NonnullRefPtr<PCFFile>> PCFFile::create(ReadonlyBytes bytes)
{
    auto pcf = adopt_ref(*new PCFFile(bytes));

    Header header;
    TRY(pcf->m_stream.read_some(Bytes { header.magic, sizeof(header.magic) }));
    if (header.magic[0] != '\1' || header.magic[1] != 'f' || header.magic[2] != 'c' || header.magic[3] != 'p')
        return Error::from_string_literal("Mismatching magic value");

    header.table_count = TRY(pcf->m_stream.read_value<LittleEndian<i32>>());
    VERIFY(header.table_count > 0);

    for (i32 i = 0; i < header.table_count; ++i) {
        Header::TOCEntry table;
        table.type = TRY(pcf->m_stream.read_value<LittleEndian<i32>>());
        table.format = TRY(pcf->m_stream.read_value<LittleEndian<i32>>());
        table.size = TRY(pcf->m_stream.read_value<LittleEndian<i32>>());
        table.offset = TRY(pcf->m_stream.read_value<LittleEndian<i32>>());
        TRY(pcf->m_tables.try_append(table));
    }

    TRY(pcf->populate_tables());
    TRY(pcf->convert_glyphs());

    return pcf;
}

ErrorOr<String> PCFFile::construct_filename() const
{
    StringBuilder builder;

    TRY(builder.try_append(TRY(family().replace(" "sv, ""sv, ReplaceMode::All))));

    auto wei = weight();
    auto sl = slope();

    // Only name the weight if it's either non-regular, or
    // the slope is non-regular and thus omitted.
    // This results in names like TerminusRegular16, TerminusBoldItalic24,
    // but _not_ TerminusRegularRegular16.
    if (sl == 0 || wei != 400)
        TRY(builder.try_append(Gfx::weight_to_name(weight())));

    if (sl != 0)
        TRY(builder.try_append(Gfx::slope_to_name(sl)));

    if (m_properties.contains("PIXEL_SIZE"sv))
        TRY(builder.try_append(ByteString::formatted("{}", m_properties.get("PIXEL_SIZE"sv).value().get<i32>())));

    TRY(builder.try_append(".font"sv));

    return builder.to_string();
}

Optional<i16> PCFFile::glyph_index_for(u16 code_point) const
{
    ssize_t table_index = 0;
    if (m_encoding.min_byte1 == 0 && m_encoding.max_byte1 == 0) {
        table_index = code_point - m_encoding.min_char_or_byte2;
    } else {
        u8 hi = code_point >> 8;
        u8 lo = code_point & 0xff;
        table_index = (hi - m_encoding.min_byte1)
                * (m_encoding.max_char_or_byte2 - m_encoding.min_char_or_byte2 + 1)
            + lo - m_encoding.min_char_or_byte2;
    }

    if (table_index < 0)
        return {};

    if (table_index >= static_cast<ssize_t>(m_encoding.indices.size()))
        return {};

    auto index = m_encoding.indices[table_index];
    if (index < 0)
        return {};

    return index;
}

ErrorOr<void> PCFFile::draw_glyph(u16 index, Gfx::GlyphBitmap& bitmap) const
{
    auto size = glyph_size();
    auto const& glyph = m_glyphs.at(index);

    for (int y = 0; y < size.height(); ++y) {
        for (int x = 0; x < glyph.width; ++x) {
            u8 pixel = glyph.data[y * glyph.width + x];
            bitmap.set_bit_at(x, y, pixel != 0);
        }
    }
    return {};
}

u8 PCFFile::baseline() const
{
    return m_acc.font_ascent - 1;
}

size_t PCFFile::highest_codepoint() const
{
    return m_encoding.indices.size();
}

String PCFFile::family() const
{
    StringBuilder builder;
    if (m_properties.contains("FAMILY_NAME"sv))
        builder.append(m_properties.get("FAMILY_NAME"sv).value().get<String>());
    else
        builder.append("Unknown"sv);
    return MUST(builder.to_string());
}

String PCFFile::name() const
{
    StringBuilder builder;
    builder.append(family());
    builder.append(" "sv);
    builder.append(weight_name());

    return MUST(builder.to_string());
}

String PCFFile::weight_name() const
{
    StringBuilder builder;
    if (m_properties.contains("WEIGHT_NAME"sv))
        builder.append(m_properties.get("WEIGHT_NAME"sv).value().get<String>());
    else
        builder.append("Regular"sv);

    return MUST(builder.to_string());
}

i32 PCFFile::weight() const
{
    // HACK: Use some common weight names because some fonts don't include any other weight info.
    auto name = weight_name();
    if (name.equals_ignoring_ascii_case("thin"sv))
        return Gfx::name_to_weight("Thin"sv);
    if (name.equals_ignoring_ascii_case("light"sv))
        return Gfx::name_to_weight("Light"sv);
    if (name.equals_ignoring_ascii_case("medium"sv) || name.equals_ignoring_ascii_case("regular"sv))
        return Gfx::name_to_weight("Regular"sv);
    if (name.equals_ignoring_ascii_case("bold"sv))
        return Gfx::name_to_weight("Bold"sv);

    if (m_properties.contains("WEIGHT"sv)) {
        return m_properties.get("WEIGHT"sv).value().get<i32>();
    } else {
        // FIXME: This can be calulated: https://www.x.org/releases/X11R7.6/doc/xorg-docs/specs/XLFD/xlfd.html#weight
        TODO();
    }

    return relative_weight();
}

i32 PCFFile::relative_weight() const
{
    // Convert X11 weight to Serenity weight.
    if (m_properties.contains("RELATIVE_WEIGHT"sv))
        return m_properties.get("RELATIVE_WEIGHT"sv).value().get<i32>() * 10;
    return 500;
}

i32 PCFFile::slope() const
{
    if (m_properties.contains("SLANT"sv)) {
        auto slant = m_properties.get("SLANT"sv).value().get<String>();
        if (slant == "I"sv)
            return Gfx::name_to_slope("Italic"sv);
        if (slant == "O"sv)
            return Gfx::name_to_slope("Oblique"sv);
        // FIXME: Do something with Reverse Italic, Reverse Oblique, Other.
    }
    return Gfx::name_to_slope("Regular"sv);
}

i32 PCFFile::pixel_size() const
{
    if (m_properties.contains("PIXEL_SIZE"sv))
        return m_properties.get("PIXEL_SIZE"sv).value().get<i32>();
    return 0;
}

i32 PCFFile::x_height() const
{
    if (m_properties.contains("X_HEIGHT"sv))
        return m_properties.get("X_HEIGHT"sv).value().get<i32>();
    TODO();
    return 0;
}

Gfx::IntSize PCFFile::glyph_size() const
{
    return { m_max_width, m_max_ascent + m_max_descent };
}

ErrorOr<void> PCFFile::populate_tables()
{
    for (auto& table : m_tables) {
        TRY(m_stream.seek(table.offset));
        auto format = TRY(m_stream.read_value<LittleEndian<i32>>());

        switch (table.type) {
        case PCF_PROPERTIES: {
            PropertiesTable table;
            table.nprops = TRY(read<i32>(format));
            VERIFY(table.nprops >= 0);

            Vector<PropertiesTable::Props> props;
            TRY(props.try_resize(table.nprops));

            for (i32 i = 0; i < table.nprops; ++i) {
                auto& prop = props.at(i);
                prop.name_offset = TRY(read<i32>(format));
                prop.is_string_prop = TRY(read<i8>(format));
                prop.value = TRY(read<i32>(format));
            }

            // Skip padding.
            TRY(m_stream.seek((table.nprops & 3) == 0 ? 0 : (4 - (table.nprops & 3)), SeekMode::FromCurrentPosition));
            auto string_size = TRY(read<i32>(format));
            auto strings = TRY(ByteBuffer::create_uninitialized(string_size));
            TRY(m_stream.read_some(strings.bytes()));

            for (i32 prop_index = 0; prop_index < table.nprops; ++prop_index) {
                auto& prop = props.at(prop_index);

                auto index = static_cast<size_t>(prop.name_offset);
                StringBuilder builder;
                for (size_t i = index; i < strings.size(); ++i) {
                    auto ch = strings.bytes().at(i);
                    if (ch == 0)
                        break;
                    TRY(builder.try_append(ch));
                }

                auto name = TRY(builder.to_string());

                Property value = [&prop, &strings]() -> Property {
                    if (prop.is_string_prop == 1) {
                        StringBuilder builder;
                        for (size_t i = prop.value; i < strings.size(); ++i) {
                            auto ch = strings.bytes().at(i);
                            if (ch == 0)
                                break;
                            MUST(builder.try_append(ch));
                        }
                        return MUST(builder.to_string());
                    }
                    return prop.value;
                }();

                TRY(m_properties.try_set(name, value));
            }
        } break;
        case PCF_ACCELERATORS:
            m_acc.no_overlap = TRY(read<u8>(format));
            m_acc.constant_metrics = TRY(read<u8>(format));
            m_acc.terminal_font = TRY(read<u8>(format));
            m_acc.constant_width = TRY(read<u8>(format));
            m_acc.ink_inside = TRY(read<u8>(format));
            m_acc.ink_metrics = TRY(read<u8>(format));
            m_acc.draw_direction = TRY(read<u8>(format));
            TRY(read<u8>(format)); // Padding
            m_acc.font_ascent = TRY(read<i32>(format));
            m_acc.font_descent = TRY(read<i32>(format));
            m_acc.max_overlap = TRY(read<i32>(format));
            break;
        case PCF_METRICS:
        case PCF_INK_METRICS: {
            u32 metrics_count;
            if (format & PCF_COMPRESSED_METRICS)
                metrics_count = TRY(read<u16>(format));
            else
                metrics_count = TRY(read<u32>(format));

            for (u32 i = 0; i < metrics_count; ++i) {
                Metrics m;

                auto read_short = [this](i32 format) -> ErrorOr<i16> {
                    if (format & PCF_COMPRESSED_METRICS) {
                        u8 compressed = TRY(read<u8>(format));
                        return static_cast<i16>(compressed) - 0x80;
                    }
                    return TRY(read<i16>(format));
                };

                m.left_side_bearing = TRY(read_short(format));
                m.right_side_bearing = TRY(read_short(format));
                m.character_width = TRY(read_short(format));
                m.character_ascent = TRY(read_short(format));
                m.character_descent = TRY(read_short(format));

                if (table.type == PCF_METRICS) {
                    // Size of bitmaps
                    TRY(m_metrics.try_append(m));
                    m_max_ascent = max(m_max_ascent, m.character_ascent);
                    m_max_descent = max(m_max_descent, m.character_descent);
                    m_max_width = max(m_max_width, m.character_width);
                } else {
                    // Minimum bounding box
                    TRY(m_ink_metrics.try_append(m));
                }
            }
        } break;
        case PCF_BITMAPS:
            m_bitmap_data.glyph_count = TRY(read<i32>(format));
            m_bitmap_data.format = format;

            TRY(m_bitmap_data.offsets.try_resize(m_bitmap_data.glyph_count));
            for (i32 i = 0; i < m_bitmap_data.glyph_count; ++i)
                m_bitmap_data.offsets[i] = TRY(read<i32>(format));

            for (i32 i = 0; i < 4; ++i)
                m_bitmap_data.bitmap_sizes[i] = TRY(read<i32>(format));

            TRY(m_bitmap_data.data.try_resize(m_bitmap_data.bitmap_sizes[format & 3] + 1));
            TRY(m_stream.read_some(m_bitmap_data.data));
            break;
        case PCF_BDF_ENCODINGS: {
            m_encoding.min_char_or_byte2 = TRY(read<i16>(format));
            m_encoding.max_char_or_byte2 = TRY(read<i16>(format));
            m_encoding.min_byte1 = TRY(read<i16>(format));
            m_encoding.max_byte1 = TRY(read<i16>(format));
            m_encoding.default_char = TRY(read<i16>(format));

            size_t num = (m_encoding.max_char_or_byte2 - m_encoding.min_char_or_byte2 + 1)
                * (m_encoding.max_byte1 - m_encoding.min_byte1 + 1);
            TRY(m_encoding.indices.try_resize(num));
            for (size_t i = 0; i < num; ++i)
                m_encoding.indices[i] = TRY(read<i16>(format));
        } break;
        default:
            break;
        }
    }

    return {};
}

ErrorOr<void> PCFFile::convert_glyphs()
{
    // Both of these should have been populated by now, hopefully.
    VERIFY(m_metrics.size() == static_cast<size_t>(m_bitmap_data.glyph_count));

    TRY(m_glyphs.try_resize(m_bitmap_data.glyph_count));

    auto data = m_bitmap_data.data.bytes();
    auto size = glyph_size();

    auto format = m_bitmap_data.format;

    auto padding = format & 3;
    auto padding_bytes = padding == 0 ? 1 : padding * 2;

    auto lsb_first = static_cast<bool>(format & 8);

    for (i32 i = 0; i < m_bitmap_data.glyph_count; ++i) {
        auto& glyph = m_glyphs.at(i);

        auto offset = m_bitmap_data.offsets[i];

        auto w = m_metrics.at(i).character_width + m_acc.max_overlap;
        auto h = m_metrics[i].character_ascent + m_metrics[i].character_descent;

        auto bytes_per_row = max(w / 8, 1);
        if (bytes_per_row % padding_bytes != 0)
            bytes_per_row += padding_bytes - (bytes_per_row % padding_bytes);

        glyph.width = w;
        TRY(glyph.data.try_resize(w * size.height()));

        auto shift = max(0, baseline() - m_metrics[i].character_ascent + 1);

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                auto index = (x / 8) + bytes_per_row * y;
                u8 byte = data[offset + index];
                u8 pixel;
                if (lsb_first)
                    pixel = (byte << (x % 8)) & 0x80;
                else
                    pixel = (byte >> (x % 8)) & 1;
                glyph.data[x + (y + shift) * w] = pixel;
            }
        }
    }

    return {};
}
