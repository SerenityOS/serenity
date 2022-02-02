/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitmapFont.h"
#include "Emoji.h"
#include <AK/BuiltinWrappers.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibCore/FileStream.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/FontStyleMapping.h>
#include <string.h>

namespace Gfx {

struct [[gnu::packed]] FontFileHeader {
    char magic[4];
    u8 glyph_width;
    u8 glyph_height;
    u16 range_mask_size;
    u8 is_variable_width;
    u8 glyph_spacing;
    u8 baseline;
    u8 mean_line;
    u8 presentation_size;
    u16 weight;
    u8 slope;
    char name[32];
    char family[32];
};

static_assert(AssertSize<FontFileHeader, 80>());

static constexpr size_t s_max_glyph_count = 0x110000;
static constexpr size_t s_max_range_mask_size = s_max_glyph_count / (256 * 8);

NonnullRefPtr<Font> BitmapFont::clone() const
{
    auto* new_range_mask = static_cast<u8*>(malloc(m_range_mask_size));
    memcpy(new_range_mask, m_range_mask, m_range_mask_size);
    size_t bytes_per_glyph = sizeof(u32) * glyph_height();
    auto* new_rows = static_cast<u8*>(kmalloc_array(m_glyph_count, bytes_per_glyph));
    memcpy(new_rows, m_rows, bytes_per_glyph * m_glyph_count);
    auto* new_widths = static_cast<u8*>(malloc(m_glyph_count));
    memcpy(new_widths, m_glyph_widths, m_glyph_count);
    return adopt_ref(*new BitmapFont(m_name, m_family, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, m_range_mask_size, new_range_mask, m_baseline, m_mean_line, m_presentation_size, m_weight, m_slope, true));
}

NonnullRefPtr<BitmapFont> BitmapFont::create(u8 glyph_height, u8 glyph_width, bool fixed, size_t glyph_count)
{
    glyph_count += 256 - (glyph_count % 256);
    glyph_count = min(glyph_count, s_max_glyph_count);
    size_t glyphs_per_range = 8 * 256;
    u16 range_mask_size = ceil_div(glyph_count, glyphs_per_range);
    auto* new_range_mask = static_cast<u8*>(calloc(range_mask_size, 1));
    for (size_t i = 0; i < glyph_count; i += 256) {
        new_range_mask[i / 256 / 8] |= 1 << (i / 256 % 8);
    }
    size_t bytes_per_glyph = sizeof(u32) * glyph_height;
    auto* new_rows = static_cast<u8*>(calloc(glyph_count, bytes_per_glyph));
    auto* new_widths = static_cast<u8*>(calloc(glyph_count, 1));
    return adopt_ref(*new BitmapFont("Untitled", "Untitled", new_rows, new_widths, fixed, glyph_width, glyph_height, 1, range_mask_size, new_range_mask, 0, 0, 0, 400, 0, true));
}

NonnullRefPtr<BitmapFont> BitmapFont::unmasked_character_set() const
{
    auto* new_range_mask = static_cast<u8*>(malloc(s_max_range_mask_size));
    constexpr u8 max_bits { 0b1111'1111 };
    memset(new_range_mask, max_bits, s_max_range_mask_size);
    size_t bytes_per_glyph = sizeof(u32) * glyph_height();
    auto* new_rows = static_cast<u8*>(kmalloc_array(s_max_glyph_count, bytes_per_glyph));
    auto* new_widths = static_cast<u8*>(calloc(s_max_glyph_count, 1));
    for (size_t code_point = 0; code_point < s_max_glyph_count; ++code_point) {
        auto index = glyph_index(code_point);
        if (index.has_value()) {
            memcpy(&new_widths[code_point], &m_glyph_widths[index.value()], 1);
            memcpy(&new_rows[code_point * bytes_per_glyph], &m_rows[index.value() * bytes_per_glyph], bytes_per_glyph);
        }
    }
    return adopt_ref(*new BitmapFont(m_name, m_family, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, s_max_range_mask_size, new_range_mask, m_baseline, m_mean_line, m_presentation_size, m_weight, m_slope, true));
}

NonnullRefPtr<BitmapFont> BitmapFont::masked_character_set() const
{
    auto* new_range_mask = static_cast<u8*>(calloc(s_max_range_mask_size, 1));
    u16 new_range_mask_size { 0 };
    for (size_t i = 0; i < s_max_glyph_count; ++i) {
        if (m_glyph_widths[i] > 0) {
            new_range_mask[i / 256 / 8] |= 1 << (i / 256 % 8);
            if (i / 256 / 8 + 1 > new_range_mask_size)
                new_range_mask_size = i / 256 / 8 + 1;
        }
    }
    size_t new_glyph_count { 0 };
    for (size_t i = 0; i < new_range_mask_size; ++i) {
        new_glyph_count += 256 * popcount(new_range_mask[i]);
    }
    size_t bytes_per_glyph = sizeof(u32) * m_glyph_height;
    auto* new_rows = static_cast<u8*>(calloc(new_glyph_count, bytes_per_glyph));
    auto* new_widths = static_cast<u8*>(calloc(new_glyph_count, 1));
    for (size_t i = 0, j = 0; i < s_max_glyph_count; ++i) {
        if (!(new_range_mask[i / 256 / 8] & 1 << (i / 256 % 8))) {
            j++;
            i += 255;
            continue;
        }
        memcpy(&new_widths[i - j * 256], &m_glyph_widths[i], 1);
        memcpy(&new_rows[(i - j * 256) * bytes_per_glyph], &m_rows[i * bytes_per_glyph], bytes_per_glyph);
    }
    return adopt_ref(*new BitmapFont(m_name, m_family, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, new_range_mask_size, new_range_mask, m_baseline, m_mean_line, m_presentation_size, m_weight, m_slope, true));
}

BitmapFont::BitmapFont(String name, String family, u8* rows, u8* widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing, u16 range_mask_size, u8* range_mask, u8 baseline, u8 mean_line, u8 presentation_size, u16 weight, u8 slope, bool owns_arrays)
    : m_name(move(name))
    , m_family(move(family))
    , m_range_mask_size(range_mask_size)
    , m_range_mask(range_mask)
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
    , m_slope(slope)
    , m_fixed_width(is_fixed_width)
    , m_owns_arrays(owns_arrays)
{
    VERIFY(m_range_mask);
    VERIFY(m_rows);
    VERIFY(m_glyph_widths);

    update_x_height();

    for (size_t i = 0, index = 0; i < m_range_mask_size; ++i) {
        for (size_t j = 0; j < 8; ++j) {
            if (m_range_mask[i] & (1 << j)) {
                m_glyph_count += 256;
                m_range_indices.append(index++);
            } else {
                m_range_indices.append({});
            }
        }
    }

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
        free(m_range_mask);
    }
}

RefPtr<BitmapFont> BitmapFont::load_from_memory(u8 const* data)
{
    auto const& header = *reinterpret_cast<const FontFileHeader*>(data);
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

    size_t bytes_per_glyph = sizeof(u32) * header.glyph_height;
    size_t glyph_count { 0 };
    u8* range_mask = const_cast<u8*>(data + sizeof(FontFileHeader));
    for (size_t i = 0; i < header.range_mask_size; ++i)
        glyph_count += 256 * popcount(range_mask[i]);
    u8* rows = range_mask + header.range_mask_size;
    u8* widths = (u8*)(rows) + glyph_count * bytes_per_glyph;
    return adopt_ref(*new BitmapFont(String(header.name), String(header.family), rows, widths, !header.is_variable_width, header.glyph_width, header.glyph_height, header.glyph_spacing, header.range_mask_size, range_mask, header.baseline, header.mean_line, header.presentation_size, header.weight, header.slope));
}

RefPtr<BitmapFont> BitmapFont::load_from_file(String const& path)
{
    if (Core::File::is_device(path))
        return nullptr;

    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error())
        return nullptr;

    auto font = load_from_memory((u8 const*)file_or_error.value()->data());
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
    header.range_mask_size = m_range_mask_size;
    header.baseline = m_baseline;
    header.mean_line = m_mean_line;
    header.is_variable_width = !m_fixed_width;
    header.glyph_spacing = m_glyph_spacing;
    header.presentation_size = m_presentation_size;
    header.weight = m_weight;
    header.slope = m_slope;
    memcpy(header.name, m_name.characters(), min(m_name.length(), sizeof(header.name) - 1));
    memcpy(header.family, m_family.characters(), min(m_family.length(), sizeof(header.family) - 1));

    auto stream_result = Core::OutputFileStream::open_buffered(path);
    if (stream_result.is_error())
        return false;
    auto& stream = stream_result.value();

    size_t bytes_per_glyph = sizeof(u32) * m_glyph_height;
    stream << ReadonlyBytes { &header, sizeof(header) };
    stream << ReadonlyBytes { m_range_mask, m_range_mask_size };
    stream << ReadonlyBytes { m_rows, m_glyph_count * bytes_per_glyph };
    stream << ReadonlyBytes { m_glyph_widths, m_glyph_count };

    stream.flush();
    return !stream.handle_any_error();
}

Glyph BitmapFont::glyph(u32 code_point) const
{
    // Note: Until all fonts support the 0xFFFD replacement
    // character, fall back to painting '?' if necessary.
    auto index = glyph_index(code_point).value_or('?');
    auto width = m_glyph_widths[index];
    return Glyph(
        GlyphBitmap(m_rows, index * m_glyph_height, { width, m_glyph_height }),
        0,
        width,
        m_glyph_height);
}

Glyph BitmapFont::raw_glyph(u32 code_point) const
{
    auto width = m_glyph_widths[code_point];
    return Glyph(
        GlyphBitmap(m_rows, code_point * m_glyph_height, { width, m_glyph_height }),
        0,
        width,
        m_glyph_height);
}

Optional<size_t> BitmapFont::glyph_index(u32 code_point) const
{
    auto index = code_point / 256;
    if (index >= m_range_indices.size())
        return {};
    if (!m_range_indices[index].has_value())
        return {};
    return m_range_indices[index].value() * 256 + code_point % 256;
}

bool BitmapFont::contains_glyph(u32 code_point) const
{
    auto index = glyph_index(code_point);
    return index.has_value() && m_glyph_widths[index.value()] > 0;
}

u8 BitmapFont::glyph_width(u32 code_point) const
{
    if (is_ascii(code_point) && !is_ascii_printable(code_point))
        return 0;
    auto index = glyph_index(code_point);
    return m_fixed_width || !index.has_value() ? m_glyph_width : m_glyph_widths[index.value()];
}

int BitmapFont::glyph_or_emoji_width_for_variable_width_font(u32 code_point) const
{
    // FIXME: This is a hack in lieu of proper code point identification.
    // 0xFFFF is arbitrary but also the end of the Basic Multilingual Plane.
    if (code_point < 0xFFFF) {
        auto index = glyph_index(code_point);
        if (!index.has_value())
            return glyph_width(0xFFFD);
        if (m_glyph_widths[index.value()] > 0)
            return glyph_width(code_point);
        return glyph_width(0xFFFD);
    }

    auto const* emoji = Emoji::emoji_for_code_point(code_point);
    if (emoji == nullptr)
        return glyph_width(0xFFFD);
    return glyph_height() * emoji->width() / emoji->height();
}

int BitmapFont::width(StringView view) const { return unicode_view_width(Utf8View(view)); }
int BitmapFont::width(Utf8View const& view) const { return unicode_view_width(view); }
int BitmapFont::width(Utf32View const& view) const { return unicode_view_width(view); }

template<typename T>
ALWAYS_INLINE int BitmapFont::unicode_view_width(T const& view) const
{
    if (view.is_empty())
        return 0;
    bool first = true;
    int width = 0;
    int longest_width = 0;

    for (u32 code_point : view) {
        if (code_point == '\n' || code_point == '\r') {
            first = true;
            longest_width = max(width, longest_width);
            width = 0;
            continue;
        }
        if (!first)
            width += glyph_spacing();
        first = false;
        width += glyph_or_emoji_width(code_point);
    }
    longest_width = max(width, longest_width);
    return longest_width;
}

String BitmapFont::qualified_name() const
{
    return String::formatted("{} {} {} {}", family(), presentation_size(), weight(), slope());
}

String BitmapFont::variant() const
{
    StringBuilder builder;
    builder.append(weight_to_name(weight()));
    if (slope() != 0) {
        if (builder.string_view() == "Regular"sv)
            builder.clear();
        else
            builder.append(" ");
        builder.append(slope_to_name(slope()));
    }
    return builder.to_string();
}

Font const& Font::bold_variant() const
{
    if (m_bold_variant)
        return *m_bold_variant;
    m_bold_variant = Gfx::FontDatabase::the().get(family(), presentation_size(), 700, 0);
    if (!m_bold_variant)
        m_bold_variant = this;
    return *m_bold_variant;
}

FontMetrics Font::metrics(u32 code_point) const
{
    return FontMetrics {
        .size = (float)presentation_size(),
        .x_height = (float)x_height(),
        .glyph_width = (float)glyph_width(code_point),
        .glyph_spacing = (float)glyph_spacing(),
    };
}

}
