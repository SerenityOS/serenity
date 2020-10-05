/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Font.h"
#include "Bitmap.h"
#include "Emoji.h"
#include <AK/MappedFile.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <AK/kmalloc.h>
#include <LibCore/FileStream.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

namespace Gfx {

struct [[gnu::packed]] FontFileHeader
{
    char magic[4];
    u8 glyph_width;
    u8 glyph_height;
    u8 type;
    u8 is_variable_width;
    u8 glyph_spacing;
    u8 baseline;
    u8 mean_line;
    u8 unused[3];
    char name[64];
};

Font& Font::default_font()
{
    static Font* s_default_font;
    static const char* default_font_path = "/res/fonts/Katica10.font";
    if (!s_default_font) {
        s_default_font = Font::load_from_file(default_font_path).leak_ref();
        ASSERT(s_default_font);
    }
    return *s_default_font;
}

Font& Font::default_fixed_width_font()
{
    static Font* s_default_fixed_width_font;
    static const char* default_fixed_width_font_path = "/res/fonts/CsillaThin7x10.font";
    if (!s_default_fixed_width_font) {
        s_default_fixed_width_font = Font::load_from_file(default_fixed_width_font_path).leak_ref();
        ASSERT(s_default_fixed_width_font);
    }
    return *s_default_fixed_width_font;
}

Font& Font::default_bold_fixed_width_font()
{
    static Font* font;
    static const char* default_bold_fixed_width_font_path = "/res/fonts/CsillaBold7x10.font";
    if (!font) {
        font = Font::load_from_file(default_bold_fixed_width_font_path).leak_ref();
        ASSERT(font);
    }
    return *font;
}

Font& Font::default_bold_font()
{
    static Font* s_default_bold_font;
    static const char* default_bold_font_path = "/res/fonts/KaticaBold10.font";
    if (!s_default_bold_font) {
        s_default_bold_font = Font::load_from_file(default_bold_font_path).leak_ref();
        ASSERT(s_default_bold_font);
    }
    return *s_default_bold_font;
}

NonnullRefPtr<Font> Font::clone() const
{
    size_t bytes_per_glyph = sizeof(u32) * glyph_height();
    // FIXME: This is leaked!
    auto* new_rows = static_cast<unsigned*>(kmalloc(bytes_per_glyph * m_glyph_count));
    memcpy(new_rows, m_rows, bytes_per_glyph * m_glyph_count);
    auto* new_widths = static_cast<u8*>(kmalloc(m_glyph_count));
    if (m_glyph_widths)
        memcpy(new_widths, m_glyph_widths, m_glyph_count);
    else
        memset(new_widths, m_glyph_width, m_glyph_count);
    return adopt(*new Font(m_name, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, m_type, m_baseline, m_mean_line));
}

NonnullRefPtr<Font> Font::create(u8 glyph_height, u8 glyph_width, bool fixed, FontTypes type)
{
    size_t bytes_per_glyph = sizeof(u32) * glyph_height;
    // FIXME: This is leaked!
    size_t count = glyph_count_by_type(type);
    auto* new_rows = static_cast<unsigned*>(malloc(bytes_per_glyph * count));
    memset(new_rows, 0, bytes_per_glyph * count);
    auto* new_widths = static_cast<u8*>(malloc(count));
    memset(new_widths, glyph_width, count);
    return adopt(*new Font("Untitled", new_rows, new_widths, fixed, glyph_width, glyph_height, 1, type, 0, 0));
}

Font::Font(const StringView& name, unsigned* rows, u8* widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing, FontTypes type, u8 baseline, u8 mean_line)
    : m_name(name)
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
    , m_fixed_width(is_fixed_width)
{
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
        m_max_glyph_width = maximum;
    }

    set_family_fonts();
}

Font::~Font()
{
}

RefPtr<Font> Font::load_from_memory(const u8* data)
{
    auto& header = *reinterpret_cast<const FontFileHeader*>(data);
    if (memcmp(header.magic, "!Fnt", 4)) {
        dbgprintf("header.magic != '!Fnt', instead it's '%c%c%c%c'\n", header.magic[0], header.magic[1], header.magic[2], header.magic[3]);
        return nullptr;
    }
    if (header.name[63] != '\0') {
        dbgprintf("Font name not fully null-terminated\n");
        return nullptr;
    }

    FontTypes type;
    if (header.type == 0)
        type = FontTypes::Default;
    else if (header.type == 1)
        type = FontTypes::LatinExtendedA;
    else
        ASSERT_NOT_REACHED();

    size_t count = glyph_count_by_type(type);
    size_t bytes_per_glyph = sizeof(unsigned) * header.glyph_height;

    auto* rows = const_cast<unsigned*>((const unsigned*)(data + sizeof(FontFileHeader)));
    u8* widths = nullptr;
    if (header.is_variable_width)
        widths = (u8*)(rows) + count * bytes_per_glyph;
    return adopt(*new Font(String(header.name), rows, widths, !header.is_variable_width, header.glyph_width, header.glyph_height, header.glyph_spacing, type, header.baseline, header.mean_line));
}

size_t Font::glyph_count_by_type(FontTypes type)
{
    if (type == FontTypes::Default)
        return 256;

    if (type == FontTypes::LatinExtendedA)
        return 384;

    dbg() << "Unknown font type:" << type;
    ASSERT_NOT_REACHED();
}

RefPtr<Font> Font::load_from_file(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;

    auto font = load_from_memory((const u8*)mapped_file.data());
    if (!font)
        return nullptr;

    font->m_mapped_file = move(mapped_file);
    return font;
}

bool Font::write_to_file(const StringView& path)
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
    memcpy(header.name, m_name.characters(), min(m_name.length(), (size_t)63));

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

GlyphBitmap Font::glyph_bitmap(u32 code_point) const
{
    return GlyphBitmap(&m_rows[code_point * m_glyph_height], { glyph_width(code_point), m_glyph_height });
}

int Font::glyph_or_emoji_width(u32 code_point) const
{
    if (code_point < m_glyph_count)
        return glyph_width(code_point);

    if (m_fixed_width)
        return m_glyph_width;

    auto* emoji = Emoji::emoji_for_code_point(code_point);
    if (emoji == nullptr)
        return glyph_width('?');
    return emoji->size().width();
}

int Font::width(const StringView& string) const
{
    Utf8View utf8 { string };
    return width(utf8);
}

int Font::width(const Utf8View& utf8) const
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

int Font::width(const Utf32View& view) const
{
    if (view.length() == 0)
        return 0;
    int width = (view.length() - 1) * glyph_spacing();
    for (size_t i = 0; i < view.length(); ++i)
        width += glyph_or_emoji_width(view.code_points()[i]);
    return width;
}

void Font::set_type(FontTypes type)
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

void Font::set_family_fonts()
{
    String typeface;
    String weight;
    StringBuilder size;

    auto parts = this->name().split(' ');
    if (parts.size() < 2) {
        typeface = this->name();
    } else {
        typeface = parts[0];
        weight = parts[1];
    }

    if (this->is_fixed_width()) {
        size.appendf("%d", this->m_max_glyph_width);
        size.append("x");
    }
    size.appendf("%d", this->m_glyph_height);

    StringBuilder path;

    if (weight != "Bold") {
        path.appendf("/res/fonts/%sBold%s.font", &typeface[0], &size.to_string()[0]);
        m_bold_family_font = Font::load_from_file(path.to_string());
        if (m_bold_family_font)
            set_boldface(true);
        path.clear();
    }
}

}
