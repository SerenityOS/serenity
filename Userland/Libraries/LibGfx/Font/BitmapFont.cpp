/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitmapFont.h"
#include "Emoji.h"
#include <AK/BuiltinWrappers.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibCore/File.h>
#include <LibCore/Resource.h>
#include <LibCore/System.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibGfx/Painter.h>
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

}

template<>
class AK::Traits<Gfx::FontFileHeader> : public DefaultTraits<Gfx::FontFileHeader> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace Gfx {

NonnullRefPtr<Font> BitmapFont::clone() const
{
    return MUST(try_clone());
}

ErrorOr<NonnullRefPtr<Font>> BitmapFont::try_clone() const
{
    auto new_range_mask = TRY(Core::System::allocate(m_range_mask.size(), 1));
    m_range_mask.copy_to(new_range_mask);
    size_t bytes_per_glyph = sizeof(u32) * glyph_height();
    auto new_rows = TRY(Core::System::allocate(m_glyph_count, bytes_per_glyph));
    m_rows.copy_to(new_rows);
    auto new_widths = TRY(Core::System::allocate(m_glyph_count, 1));
    m_glyph_widths.copy_to(new_widths);
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) BitmapFont(m_name, m_family, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, new_range_mask, m_baseline, m_mean_line, m_presentation_size, m_weight, m_slope, true)));
}

ErrorOr<NonnullRefPtr<BitmapFont>> BitmapFont::create(u8 glyph_height, u8 glyph_width, bool fixed, size_t glyph_count)
{
    glyph_count += 256 - (glyph_count % 256);
    glyph_count = min(glyph_count, s_max_glyph_count);
    size_t glyphs_per_range = 8 * 256;
    u16 range_mask_size = ceil_div(glyph_count, glyphs_per_range);
    auto new_range_mask = TRY(Core::System::allocate(range_mask_size, 1));
    for (size_t i = 0; i < glyph_count; i += 256) {
        new_range_mask[i / 256 / 8] |= 1 << (i / 256 % 8);
    }
    size_t bytes_per_glyph = sizeof(u32) * glyph_height;
    auto new_rows = TRY(Core::System::allocate(glyph_count, bytes_per_glyph));
    auto new_widths = TRY(Core::System::allocate(glyph_count, 1));
    return adopt_nonnull_ref_or_enomem(new (nothrow) BitmapFont("Untitled"_string, "Untitled"_string, new_rows, new_widths, fixed, glyph_width, glyph_height, 1, new_range_mask, 0, 0, 0, 400, 0, true));
}

ErrorOr<NonnullRefPtr<BitmapFont>> BitmapFont::unmasked_character_set() const
{
    auto new_range_mask = TRY(Core::System::allocate(s_max_range_mask_size, 1));
    constexpr u8 max_bits { 0b1111'1111 };
    memset(new_range_mask.data(), max_bits, s_max_range_mask_size);
    size_t bytes_per_glyph = sizeof(u32) * glyph_height();
    auto new_rows = TRY(Core::System::allocate(s_max_glyph_count, bytes_per_glyph));
    auto new_widths = TRY(Core::System::allocate(s_max_glyph_count, 1));
    for (size_t code_point = 0; code_point < s_max_glyph_count; ++code_point) {
        auto index = glyph_index(code_point);
        if (index.has_value()) {
            new_widths[code_point] = m_glyph_widths[index.value()];
            memcpy(&new_rows[code_point * bytes_per_glyph], &m_rows[index.value() * bytes_per_glyph], bytes_per_glyph);
        }
    }
    return adopt_nonnull_ref_or_enomem(new (nothrow) BitmapFont(m_name, m_family, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, new_range_mask, m_baseline, m_mean_line, m_presentation_size, m_weight, m_slope, true));
}

ErrorOr<NonnullRefPtr<BitmapFont>> BitmapFont::masked_character_set() const
{
    auto new_range_mask = TRY(Core::System::allocate(s_max_range_mask_size, 1));
    u16 new_range_mask_size { 0 };
    for (size_t i = 0; i < m_glyph_count; ++i) {
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
    auto new_rows = TRY(Core::System::allocate(new_glyph_count, bytes_per_glyph));
    auto new_widths = TRY(Core::System::allocate(new_glyph_count, 1));
    for (size_t i = 0, j = 0; i < m_glyph_count; ++i) {
        if (!(new_range_mask[i / 256 / 8] & 1 << (i / 256 % 8))) {
            j++;
            i += 255;
            continue;
        }
        new_widths[i - j * 256] = m_glyph_widths[i];
        memcpy(&new_rows[(i - j * 256) * bytes_per_glyph], &m_rows[i * bytes_per_glyph], bytes_per_glyph);
    }
    // Now that we're done working with the range-mask memory, reduce its reported size down to what it should be.
    new_range_mask = { new_range_mask.data(), new_range_mask_size };
    return adopt_nonnull_ref_or_enomem(new (nothrow) BitmapFont(m_name, m_family, new_rows, new_widths, m_fixed_width, m_glyph_width, m_glyph_height, m_glyph_spacing, new_range_mask, m_baseline, m_mean_line, m_presentation_size, m_weight, m_slope, true));
}

BitmapFont::BitmapFont(String name, String family, Bytes rows, Span<u8> widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing, Bytes range_mask, u8 baseline, u8 mean_line, u8 presentation_size, u16 weight, u8 slope, bool owns_arrays)
    : m_name(move(name))
    , m_family(move(family))
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
    update_x_height();

    for (size_t i = 0, index = 0; i < m_range_mask.size(); ++i) {
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
        free(m_glyph_widths.data());
        free(m_rows.data());
        free(m_range_mask.data());
    }
}

ErrorOr<NonnullRefPtr<BitmapFont>> BitmapFont::try_load_from_stream(FixedMemoryStream& stream)
{
    auto& header = *TRY(stream.read_in_place<FontFileHeader const>());
    if (memcmp(header.magic, "!Fnt", 4))
        return Error::from_string_literal("Gfx::BitmapFont::load_from_memory: Incompatible header");
    if (header.name[sizeof(header.name) - 1] != '\0')
        return Error::from_string_literal("Gfx::BitmapFont::load_from_memory: Nonnull-terminated name");
    if (header.family[sizeof(header.family) - 1] != '\0')
        return Error::from_string_literal("Gfx::BitmapFont::load_from_memory: Nonnull-terminated family");

    size_t bytes_per_glyph = sizeof(u32) * header.glyph_height;
    size_t glyph_count { 0 };

    // FIXME: These ReadonlyFoo -> Foo casts are awkward, and only needed because BitmapFont is
    //        sometimes editable and sometimes not. Splitting it into editable/non-editable classes
    //        would make this a lot cleaner.
    ReadonlyBytes readonly_range_mask = TRY(stream.read_in_place<u8 const>(header.range_mask_size));
    Bytes range_mask { const_cast<u8*>(readonly_range_mask.data()), readonly_range_mask.size() };
    for (size_t i = 0; i < header.range_mask_size; ++i)
        glyph_count += 256 * popcount(range_mask[i]);

    ReadonlyBytes readonly_rows = TRY(stream.read_in_place<u8 const>(glyph_count * bytes_per_glyph));
    Bytes rows { const_cast<u8*>(readonly_rows.data()), readonly_rows.size() };

    ReadonlySpan<u8> readonly_widths = TRY(stream.read_in_place<u8 const>(glyph_count));
    Span<u8> widths { const_cast<u8*>(readonly_widths.data()), readonly_widths.size() };

    if (!stream.is_eof())
        return Error::from_string_literal("Gfx::BitmapFont::load_from_memory: Trailing data in file");

    auto name = TRY(String::from_utf8(ReadonlyBytes { header.name, strlen(header.name) }));
    auto family = TRY(String::from_utf8(ReadonlyBytes { header.family, strlen(header.family) }));
    auto font = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) BitmapFont(move(name), move(family), rows, widths, !header.is_variable_width, header.glyph_width, header.glyph_height, header.glyph_spacing, range_mask, header.baseline, header.mean_line, header.presentation_size, header.weight, header.slope)));
    return font;
}

ErrorOr<NonnullRefPtr<BitmapFont>> BitmapFont::try_load_from_resource(NonnullRefPtr<Core::Resource> resource)
{
    auto stream = resource->stream();
    auto font = TRY(try_load_from_stream(stream));
    font->m_owned_data = move(resource);
    return font;
}

ErrorOr<NonnullRefPtr<BitmapFont>> BitmapFont::try_load_from_mapped_file(NonnullOwnPtr<Core::MappedFile> mapped_file)
{
    auto font = TRY(try_load_from_stream(*mapped_file));
    font->m_owned_data = move(mapped_file);
    return font;
}

NonnullRefPtr<BitmapFont> BitmapFont::load_from_uri(StringView uri)
{
    return MUST(try_load_from_uri(uri));
}

ErrorOr<NonnullRefPtr<BitmapFont>> BitmapFont::try_load_from_uri(StringView uri)
{
    return try_load_from_resource(TRY(Core::Resource::load_from_uri(uri)));
}

ErrorOr<void> BitmapFont::write_to_file(ByteString const& path)
{
    auto stream = TRY(Core::File::open(path, Core::File::OpenMode::Write));
    TRY(write_to_file(move(stream)));

    return {};
}

ErrorOr<void> BitmapFont::write_to_file(NonnullOwnPtr<Core::File> file)
{
    FontFileHeader header;
    memset(&header, 0, sizeof(FontFileHeader));
    memcpy(header.magic, "!Fnt", 4);
    header.glyph_width = m_glyph_width;
    header.glyph_height = m_glyph_height;
    header.range_mask_size = m_range_mask.size();
    header.baseline = m_baseline;
    header.mean_line = m_mean_line;
    header.is_variable_width = !m_fixed_width;
    header.glyph_spacing = m_glyph_spacing;
    header.presentation_size = m_presentation_size;
    header.weight = m_weight;
    header.slope = m_slope;
    memcpy(header.name, m_name.bytes().data(), min(m_name.bytes().size(), sizeof(header.name) - 1));
    memcpy(header.family, m_family.bytes().data(), min(m_family.bytes().size(), sizeof(header.family) - 1));

    TRY(file->write_until_depleted({ &header, sizeof(header) }));
    TRY(file->write_until_depleted(m_range_mask));
    TRY(file->write_until_depleted(m_rows));
    TRY(file->write_until_depleted(m_glyph_widths));

    return {};
}

Glyph BitmapFont::glyph(u32 code_point) const
{
    // Note: Until all fonts support the 0xFFFD replacement
    // character, fall back to painting '?' if necessary.
    auto index = glyph_index(code_point).value_or('?');
    auto width = m_glyph_widths[index];
    auto glyph_byte_count = m_glyph_height * GlyphBitmap::bytes_per_row();
    return Glyph(
        GlyphBitmap(m_rows.slice(index * glyph_byte_count, glyph_byte_count), { width, m_glyph_height }),
        0,
        width,
        m_glyph_height);
}

Glyph BitmapFont::raw_glyph(u32 code_point) const
{
    auto width = m_glyph_widths[code_point];
    auto glyph_byte_count = m_glyph_height * GlyphBitmap::bytes_per_row();
    return Glyph(
        GlyphBitmap(m_rows.slice(code_point * glyph_byte_count, glyph_byte_count), { width, m_glyph_height }),
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

float BitmapFont::glyph_width(u32 code_point) const
{
    if (is_ascii(code_point) && !is_ascii_printable(code_point))
        return 0;
    auto index = glyph_index(code_point);
    return m_fixed_width || !index.has_value() ? m_glyph_width : m_glyph_widths[index.value()];
}

template<typename CodePointIterator>
static float glyph_or_emoji_width_impl(BitmapFont const& font, CodePointIterator& it)
{
    if (auto const* emoji = Emoji::emoji_for_code_point_iterator(it))
        return font.pixel_size() * emoji->width() / emoji->height();

    if (font.is_fixed_width())
        return font.glyph_fixed_width();

    return font.glyph_width(*it);
}

float BitmapFont::glyph_or_emoji_width(Utf8CodePointIterator& it) const
{
    return glyph_or_emoji_width_impl(*this, it);
}

float BitmapFont::glyph_or_emoji_width(Utf32CodePointIterator& it) const
{
    return glyph_or_emoji_width_impl(*this, it);
}

int BitmapFont::width_rounded_up(StringView view) const
{
    return static_cast<int>(ceilf(width(view)));
}

float BitmapFont::width(StringView view) const { return unicode_view_width(Utf8View(view)); }
float BitmapFont::width(Utf8View const& view) const { return unicode_view_width(view); }
float BitmapFont::width(Utf32View const& view) const { return unicode_view_width(view); }

template<typename T>
ALWAYS_INLINE int BitmapFont::unicode_view_width(T const& view) const
{
    if (view.is_empty())
        return 0;
    bool first = true;
    int width = 0;
    int longest_width = 0;

    for (auto it = view.begin(); it != view.end(); ++it) {
        auto code_point = *it;

        if (code_point == '\n' || code_point == '\r') {
            first = true;
            longest_width = max(width, longest_width);
            width = 0;
            continue;
        }
        if (!first)
            width += glyph_spacing();
        first = false;

        width += glyph_or_emoji_width(it);
    }

    longest_width = max(width, longest_width);
    return longest_width;
}

String BitmapFont::qualified_name() const
{
    return MUST(String::formatted("{} {} {} {}", family(), presentation_size(), weight(), slope()));
}

String BitmapFont::variant() const
{
    StringBuilder builder;
    builder.append(weight_to_name(weight()));
    if (slope() != 0) {
        if (builder.string_view() == "Regular"sv)
            builder.clear();
        else
            builder.append(' ');
        builder.append(slope_to_name(slope()));
    }
    return MUST(builder.to_string());
}

NonnullRefPtr<Font> BitmapFont::with_size(float point_size) const
{
    auto scaled_font = Gfx::FontDatabase::the().get(family(), point_size, weight(), width(), slope(), AllowInexactSizeMatch::Yes);
    VERIFY(scaled_font); // The inexact lookup should, at the very least, return `this` font.

    return *scaled_font;
}

Font const& Font::bold_variant() const
{
    if (m_bold_variant)
        return *m_bold_variant;
    m_bold_variant = Gfx::FontDatabase::the().get(family(), presentation_size(), 700, Gfx::FontWidth::Normal, 0);
    if (!m_bold_variant)
        m_bold_variant = this;
    return *m_bold_variant;
}

FontPixelMetrics BitmapFont::pixel_metrics() const
{
    return FontPixelMetrics {
        .size = (float)pixel_size(),
        .x_height = (float)x_height(),
        .advance_of_ascii_zero = (float)glyph_width('0'),
        .glyph_spacing = (float)glyph_spacing(),
        .ascent = (float)m_baseline,
        .descent = (float)(m_glyph_height - m_baseline),
        .line_gap = Gfx::Painter::LINE_SPACING,
    };
}

}
