/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitmapFont.h"
#include "Bitmap.h"
#include "Emoji.h"
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibCore/FileStream.h>
#include <LibGfx/FontDatabase.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

namespace Gfx {

struct [[gnu::packed]] FontFileHeader {
    char magic[4];
    u8 glyph_width;
    u8 glyph_height;
    u8 type;
    u8 is_variable_width;
    u8 glyph_spacing;
    u8 baseline;
    u8 mean_line;
    u8 presentation_size;
    u16 weight;
    char name[32];
    char family[32];
    u16 unused;
};

static_assert(sizeof(FontFileHeader) == 80);

NonnullRefPtr<Font> BitmapFont::clone() const
{
    size_t bytes_per_glyph = sizeof(u32) * glyph_height();
    auto* new_rows = static_cast<unsigned*>(malloc(bytes_per_glyph * m_glyph_count));
    memcpy(new_rows, m_rows, bytes_per_glyph * m_glyph_count);
    auto* new_widths = static_cast<u8*>(malloc(m_glyph_count));
    memcpy(new_widths, m_glyph_widths, m_glyph_count);
    return adopt_ref(*new BitmapFont(m_name, m_family, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, m_type, m_baseline, m_mean_line, m_presentation_size, m_weight, true));
}

NonnullRefPtr<BitmapFont> BitmapFont::create(u8 glyph_height, u8 glyph_width, bool fixed, FontTypes type)
{
    size_t bytes_per_glyph = sizeof(u32) * glyph_height;
    size_t count = glyph_count_by_type(type);
    auto* new_rows = static_cast<unsigned*>(malloc(bytes_per_glyph * count));
    memset(new_rows, 0, bytes_per_glyph * count);
    auto* new_widths = static_cast<u8*>(malloc(count));
    memset(new_widths, 0, count);
    return adopt_ref(*new BitmapFont("Untitled", "Untitled", new_rows, new_widths, fixed, glyph_width, glyph_height, 1, type, 0, 0, 0, 400, true));
}

BitmapFont::BitmapFont(String name, String family, unsigned* rows, u8* widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing, FontTypes type, u8 baseline, u8 mean_line, u8 presentation_size, u16 weight, bool owns_arrays)
    : m_name(name)
    , m_family(family)
    , m_type(type)
    , m_rows(rows)
    , m_glyph_widths(widths)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
    , m_min_glyph_width(glyph_width)
    , m_max_glyph_width(glyph_width)
    , m_glyph_spacing(glyph_spacing)
    , m_baseline(baseline)
    , m_mean_line(mean_line)
    , m_presentation_size(presentation_size)
    , m_weight(weight)
    , m_fixed_width(is_fixed_width)
    , m_owns_arrays(owns_arrays)
{
    VERIFY(m_rows);
    VERIFY(m_glyph_widths);

    update_x_height();

    m_glyph_count = glyph_count_by_type(m_type);

    if (!m_fixed_width) {
        u8 maximum = 0;
        u8 minimum = 255;
        for (size_t i = 0; i < m_glyph_count; ++i) {
            minimum = min(minimum, m_glyph_widths[i]);
            maximum = max(maximum, m_glyph_widths[i]);
        }
        m_min_glyph_width = minimum;
        m_max_glyph_width = max(maximum, m_glyph_width);
    }
}

BitmapFont::~BitmapFont()
{
    if (m_owns_arrays) {
        free(m_glyph_widths);
        free(m_rows);
    }
}

RefPtr<BitmapFont> BitmapFont::load_from_memory(const u8* data)
{
    auto& header = *reinterpret_cast<const FontFileHeader*>(data);
    if (memcmp(header.magic, "!Fnt", 4)) {
        dbgln("header.magic != '!Fnt', instead it's '{:c}{:c}{:c}{:c}'", header.magic[0], header.magic[1], header.magic[2], header.magic[3]);
        return nullptr;
    }
    if (header.name[sizeof(header.name) - 1] != '\0') {
        dbgln("Font name not fully null-terminated");
        return nullptr;
    }

    if (header.family[sizeof(header.family) - 1] != '\0') {
        dbgln("Font family not fully null-terminated");
        return nullptr;
    }

    FontTypes type;
    if (header.type == 0)
        type = FontTypes::Default;
    else if (header.type == 1)
        type = FontTypes::LatinExtendedA;
    else if (header.type == 2)
        type = FontTypes::Cyrillic;
    else if (header.type == 3)
        type = FontTypes::Hebrew;
    else
        VERIFY_NOT_REACHED();

    size_t count = glyph_count_by_type(type);
    size_t bytes_per_glyph = sizeof(unsigned) * header.glyph_height;

    auto* rows = const_cast<unsigned*>((const unsigned*)(data + sizeof(FontFileHeader)));
    u8* widths = (u8*)(rows) + count * bytes_per_glyph;
    return adopt_ref(*new BitmapFont(String(header.name), String(header.family), rows, widths, !header.is_variable_width, header.glyph_width, header.glyph_height, header.glyph_spacing, type, header.baseline, header.mean_line, header.presentation_size, header.weight));
}

size_t BitmapFont::glyph_count_by_type(FontTypes type)
{
    if (type == FontTypes::Default)
        return 256;

    if (type == FontTypes::LatinExtendedA)
        return 384;

    if (type == FontTypes::Cyrillic)
        return 1280;

    if (type == FontTypes::Hebrew)
        return 1536;

    dbgln("Unknown font type: {}", (int)type);
    VERIFY_NOT_REACHED();
}

String BitmapFont::type_name_by_type(FontTypes type)
{
    if (type == FontTypes::Default)
        return "Default";

    if (type == FontTypes::LatinExtendedA)
        return "LatinExtendedA";

    if (type == FontTypes::Cyrillic)
        return "Cyrillic";

    if (type == FontTypes::Hebrew)
        return "Hebrew";

    dbgln("Unknown font type: {}", (int)type);
    VERIFY_NOT_REACHED();
}

RefPtr<BitmapFont> BitmapFont::load_from_file(String const& path)
{
    if (Core::File::is_device(path))
        return nullptr;

    auto file_or_error = MappedFile::map(path);
    if (file_or_error.is_error())
        return nullptr;

    auto font = load_from_memory((const u8*)file_or_error.value()->data());
    if (!font)
        return nullptr;

    font->m_mapped_file = file_or_error.release_value();
    return font;
}

bool BitmapFont::write_to_file(String const& path)
{
    FontFileHeader header;
    memset(&header, 0, sizeof(FontFileHeader));
    memcpy(header.magic, "!Fnt", 4);
    header.glyph_width = m_glyph_width;
    header.glyph_height = m_glyph_height;
    header.type = m_type;
    header.baseline = m_baseline;
    header.mean_line = m_mean_line;
    header.is_variable_width = !m_fixed_width;
    header.glyph_spacing = m_glyph_spacing;
    header.presentation_size = m_presentation_size;
    header.weight = m_weight;
    memcpy(header.name, m_name.characters(), min(m_name.length(), sizeof(header.name) - 1));
    memcpy(header.family, m_family.characters(), min(m_family.length(), sizeof(header.family) - 1));

    size_t bytes_per_glyph = sizeof(unsigned) * m_glyph_height;
    size_t count = glyph_count_by_type(m_type);

    auto stream_result = Core::OutputFileStream::open_buffered(path);
    if (stream_result.is_error())
        return false;
    auto& stream = stream_result.value();

    stream << ReadonlyBytes { &header, sizeof(header) };
    stream << ReadonlyBytes { m_rows, count * bytes_per_glyph };
    stream << ReadonlyBytes { m_glyph_widths, count };

    stream.flush();
    if (stream.handle_any_error())
        return false;

    return true;
}

Glyph BitmapFont::glyph(u32 code_point) const
{
    auto width = glyph_width(code_point);
    return Glyph(
        GlyphBitmap(&m_rows[code_point * m_glyph_height], { width, m_glyph_height }),
        0,
        width,
        m_glyph_height);
}

int BitmapFont::glyph_or_emoji_width_for_variable_width_font(u32 code_point) const
{
    if (code_point < m_glyph_count) {
        if (m_glyph_widths[code_point] > 0)
            return glyph_width(code_point);
        else
            return glyph_width('?');
    }

    auto* emoji = Emoji::emoji_for_code_point(code_point);
    if (emoji == nullptr)
        return glyph_width('?');
    return emoji->size().width();
}

int BitmapFont::width(const StringView& string) const
{
    Utf8View utf8 { string };
    return width(utf8);
}

int BitmapFont::width(const Utf8View& utf8) const
{
    bool first = true;
    int width = 0;

    for (u32 code_point : utf8) {
        if (!first)
            width += glyph_spacing();
        first = false;
        width += glyph_or_emoji_width(code_point);
    }

    return width;
}

int BitmapFont::width(const Utf32View& view) const
{
    if (view.length() == 0)
        return 0;
    int width = (view.length() - 1) * glyph_spacing();
    for (size_t i = 0; i < view.length(); ++i)
        width += glyph_or_emoji_width(view.code_points()[i]);
    return width;
}

void BitmapFont::set_type(FontTypes type)
{
    if (type == m_type)
        return;

    if (type == FontTypes::Default)
        return;

    size_t new_glyph_count = glyph_count_by_type(type);
    if (new_glyph_count <= m_glyph_count) {
        m_glyph_count = new_glyph_count;
        return;
    }

    int item_count_to_copy = min(m_glyph_count, new_glyph_count);

    size_t bytes_per_glyph = sizeof(u32) * glyph_height();

    auto* new_rows = static_cast<unsigned*>(kmalloc(bytes_per_glyph * new_glyph_count));
    memset(new_rows, (unsigned)0, bytes_per_glyph * new_glyph_count);
    memcpy(new_rows, m_rows, bytes_per_glyph * item_count_to_copy);

    auto* new_widths = static_cast<u8*>(kmalloc(new_glyph_count));
    memset(new_widths, (u8)0, new_glyph_count);
    memcpy(new_widths, m_glyph_widths, item_count_to_copy);

    kfree(m_rows);
    kfree(m_glyph_widths);

    m_type = type;
    m_glyph_count = new_glyph_count;
    m_rows = new_rows;
    m_glyph_widths = new_widths;
}

String BitmapFont::qualified_name() const
{
    return String::formatted("{} {} {}", family(), presentation_size(), weight());
}

const Font& BitmapFont::bold_variant() const
{
    if (m_bold_variant)
        return *m_bold_variant;
    m_bold_variant = Gfx::FontDatabase::the().get(m_family, m_presentation_size, 700);
    if (!m_bold_variant)
        m_bold_variant = this;
    return *m_bold_variant;
}

}
