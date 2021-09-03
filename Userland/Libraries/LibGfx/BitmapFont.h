/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/CharacterTypes.h>
#include <YAK/MappedFile.h>
#include <YAK/RefCounted.h>
#include <YAK/RefPtr.h>
#include <YAK/String.h>
#include <YAK/Types.h>
#include <LibGfx/Font.h>
#include <LibGfx/Size.h>

namespace Gfx {

// Note: Perhaps put glyph count directly in header
// and sidestep FontType conflation/sync maintenance
enum FontTypes {
    Default = 0,
    LatinExtendedA,
    Cyrillic,
    Hebrew,
    __Count
};

class BitmapFont final : public Font {
public:
    NonnullRefPtr<Font> clone() const override;
    static NonnullRefPtr<BitmapFont> create(u8 glyph_height, u8 glyph_width, bool fixed, FontTypes type);

    static RefPtr<BitmapFont> load_from_file(String const& path);
    bool write_to_file(String const& path);

    ~BitmapFont();

    u8 presentation_size() const override { return m_presentation_size; }
    void set_presentation_size(u8 size) { m_presentation_size = size; }

    u16 weight() const override { return m_weight; }
    void set_weight(u16 weight) { m_weight = weight; }

    Glyph glyph(u32 code_point) const override;
    bool contains_glyph(u32 code_point) const override { return code_point < (u32)glyph_count() && m_glyph_widths[code_point] > 0; }

    u8 glyph_width(size_t ch) const override
    {
        if (is_ascii(ch) && !is_ascii_printable(ch))
            return 0;

        return m_fixed_width ? m_glyph_width : m_glyph_widths[ch];
    }
    ALWAYS_INLINE int glyph_or_emoji_width(u32 code_point) const override
    {
        if (m_fixed_width)
            return m_glyph_width;
        return glyph_or_emoji_width_for_variable_width_font(code_point);
    }
    u8 glyph_height() const override { return m_glyph_height; }
    int x_height() const override { return m_x_height; }

    u8 raw_glyph_width(size_t ch) const { return m_glyph_widths[ch]; }

    u8 min_glyph_width() const override { return m_min_glyph_width; }
    u8 max_glyph_width() const override { return m_max_glyph_width; }
    u8 glyph_fixed_width() const override { return m_glyph_width; }

    u8 baseline() const override { return m_baseline; }
    void set_baseline(u8 baseline)
    {
        m_baseline = baseline;
        update_x_height();
    }

    u8 mean_line() const override { return m_mean_line; }
    void set_mean_line(u8 mean_line)
    {
        m_mean_line = mean_line;
        update_x_height();
    }

    int width(StringView const&) const override;
    int width(Utf8View const&) const override;
    int width(Utf32View const&) const override;

    String name() const override { return m_name; }
    void set_name(String name) { m_name = move(name); }

    bool is_fixed_width() const override { return m_fixed_width; }
    void set_fixed_width(bool b) { m_fixed_width = b; }

    u8 glyph_spacing() const override { return m_glyph_spacing; }
    void set_glyph_spacing(u8 spacing) { m_glyph_spacing = spacing; }

    void set_glyph_width(size_t ch, u8 width)
    {
        VERIFY(m_glyph_widths);
        m_glyph_widths[ch] = width;
    }

    size_t glyph_count() const override { return m_glyph_count; }

    FontTypes type() { return m_type; }
    void set_type(FontTypes type);

    String family() const override { return m_family; }
    void set_family(String family) { m_family = move(family); }
    String variant() const override { return String::number(weight()); }

    String qualified_name() const override;

    static size_t glyph_count_by_type(FontTypes type);
    static String type_name_by_type(FontTypes type);

private:
    BitmapFont(String name, String family, unsigned* rows, u8* widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing, FontTypes type, u8 baseline, u8 mean_line, u8 presentation_size, u16 weight, bool owns_arrays = false);

    static RefPtr<BitmapFont> load_from_memory(u8 const*);

    template<typename T>
    int unicode_view_width(T const& view) const;

    void update_x_height() { m_x_height = m_baseline - m_mean_line; };
    int glyph_or_emoji_width_for_variable_width_font(u32 code_point) const;

    String m_name;
    String m_family;
    FontTypes m_type;
    size_t m_glyph_count { 256 };

    unsigned* m_rows { nullptr };
    u8* m_glyph_widths { nullptr };
    RefPtr<MappedFile> m_mapped_file;

    u8 m_glyph_width { 0 };
    u8 m_glyph_height { 0 };
    u8 m_x_height { 0 };
    u8 m_min_glyph_width { 0 };
    u8 m_max_glyph_width { 0 };
    u8 m_glyph_spacing { 0 };
    u8 m_baseline { 0 };
    u8 m_mean_line { 0 };
    u8 m_presentation_size { 0 };
    u16 m_weight { 0 };

    bool m_fixed_width { false };
    bool m_owns_arrays { false };
};

}
