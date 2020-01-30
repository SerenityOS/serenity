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
#include <AK/BufferStream.h>
#include <AK/MappedFile.h>
#include <AK/StdLibExtras.h>
#include <AK/kmalloc.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <stdio.h>
#include <unistd.h>
#include <AK/Utf8View.h>
#include "Emoji.h"
#include "GraphicsBitmap.h"

struct [[gnu::packed]] FontFileHeader
{
    char magic[4];
    u8 glyph_width;
    u8 glyph_height;
    u8 type;
    u8 is_variable_width;
    u8 glyph_spacing;
    u8 unused[5];
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

RefPtr<Font> Font::clone() const
{
    size_t bytes_per_glyph = sizeof(u32) * glyph_height();
    // FIXME: This is leaked!
    auto* new_rows = static_cast<unsigned*>(kmalloc(bytes_per_glyph * 256));
    memcpy(new_rows, m_rows, bytes_per_glyph * 256);
    auto* new_widths = static_cast<u8*>(kmalloc(256));
    if (m_glyph_widths)
        memcpy(new_widths, m_glyph_widths, 256);
    else
        memset(new_widths, m_glyph_width, 256);
    return adopt(*new Font(m_name, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing));
}

Font::Font(const StringView& name, unsigned* rows, u8* widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing)
    : m_name(name)
    , m_rows(rows)
    , m_glyph_widths(widths)
    , m_glyph_width(glyph_width)
    , m_glyph_height(glyph_height)
    , m_min_glyph_width(glyph_width)
    , m_max_glyph_width(glyph_width)
    , m_glyph_spacing(glyph_spacing)
    , m_fixed_width(is_fixed_width)
{
    if (!m_fixed_width) {
        u8 maximum = 0;
        u8 minimum = 255;
        for (int i = 0; i < 256; ++i) {
            minimum = min(minimum, m_glyph_widths[i]);
            maximum = max(maximum, m_glyph_widths[i]);
        }
        m_min_glyph_width = minimum;
        m_max_glyph_width = maximum;
    }
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

    size_t bytes_per_glyph = sizeof(unsigned) * header.glyph_height;

    auto* rows = const_cast<unsigned*>((const unsigned*)(data + sizeof(FontFileHeader)));
    u8* widths = nullptr;
    if (header.is_variable_width)
        widths = (u8*)(rows) + 256 * bytes_per_glyph;
    return adopt(*new Font(String(header.name), rows, widths, !header.is_variable_width, header.glyph_width, header.glyph_height, header.glyph_spacing));
}

RefPtr<Font> Font::load_from_file(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;

    auto font = load_from_memory((const u8*)mapped_file.data());
    font->m_mapped_file = move(mapped_file);
    return font;
}

bool Font::write_to_file(const StringView& path)
{
    int fd = creat_with_path_length(path.characters_without_null_termination(), path.length(), 0644);
    if (fd < 0) {
        perror("open");
        return false;
    }

    FontFileHeader header;
    memset(&header, 0, sizeof(FontFileHeader));
    memcpy(header.magic, "!Fnt", 4);
    header.glyph_width = m_glyph_width;
    header.glyph_height = m_glyph_height;
    header.type = 0;
    header.is_variable_width = !m_fixed_width;
    header.glyph_spacing = m_glyph_spacing;
    memcpy(header.name, m_name.characters(), min(m_name.length(), (size_t)63));

    size_t bytes_per_glyph = sizeof(unsigned) * m_glyph_height;

    auto buffer = ByteBuffer::create_uninitialized(sizeof(FontFileHeader) + (256 * bytes_per_glyph) + 256);
    BufferStream stream(buffer);

    stream << ByteBuffer::wrap(&header, sizeof(FontFileHeader));
    stream << ByteBuffer::wrap(m_rows, (256 * bytes_per_glyph));
    stream << ByteBuffer::wrap(m_glyph_widths, 256);

    ASSERT(stream.at_end());
    ssize_t nwritten = write(fd, buffer.data(), buffer.size());
    ASSERT(nwritten == (ssize_t)buffer.size());
    int rc = close(fd);
    ASSERT(rc == 0);
    return true;
}

int Font::glyph_or_emoji_width(u32 codepoint) const
{
    if (codepoint < 256)
        return glyph_width((char)codepoint);

    if (m_fixed_width)
        return m_glyph_width;

    auto* emoji = Emoji::emoji_for_codepoint(codepoint);
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

    for (u32 codepoint : utf8) {
        if (!first)
            width += glyph_spacing();
        first = false;
        width += glyph_or_emoji_width(codepoint);
    }

    return width;
}
