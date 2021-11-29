/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/Font.h>
#include <LibGfx/Size.h>

namespace Gfx {

class BitmapFont final : public Font {
public:
    NonnullRefPtr<Font> clone() const override;
    static NonnullRefPtr<BitmapFont> create(u8 glyph_height, u8 glyph_width, bool fixed, size_t glyph_count);

    NonnullRefPtr<BitmapFont> masked_character_set() const;
    NonnullRefPtr<BitmapFont> unmasked_character_set() const;

    static RefPtr<BitmapFont> load_from_file(String const& path);
    bool write_to_file(String const& path);

    ~BitmapFont();

    u8 presentation_size() const override { return m_presentation_size; }
    void set_presentation_size(u8 size) { m_presentation_size = size; }

    u16 weight() const override { return m_weight; }
    void set_weight(u16 weight) { m_weight = weight; }

    u8 slope() const { return m_slope; }
    void set_slope(u8 slope) { m_slope = slope; }

    Glyph glyph(u32 code_point) const override;
    Glyph raw_glyph(u32 code_point) const;
    bool contains_glyph(u32 code_point) const override;
    bool contains_raw_glyph(u32 code_point) const { return m_glyph_widths[code_point] > 0; }

    ALWAYS_INLINE int glyph_or_emoji_width(u32 code_point) const override
    {
        if (m_fixed_width)
            return m_glyph_width;
        return glyph_or_emoji_width_for_variable_width_font(code_point);
    }
    u8 glyph_height() const override { return m_glyph_height; }
    int x_height() const override { return m_x_height; }

    u8 glyph_width(u32 code_point) const override;
    u8 raw_glyph_width(u32 code_point) const { return m_glyph_widths[code_point]; }

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

    int width(StringView) const override;
    int width(Utf8View const&) const override;
    int width(Utf32View const&) const override;

    String name() const override { return m_name; }
    void set_name(String name) { m_name = move(name); }

    bool is_fixed_width() const override { return m_fixed_width; }
    void set_fixed_width(bool b) { m_fixed_width = b; }

    u8 glyph_spacing() const override { return m_glyph_spacing; }
    void set_glyph_spacing(u8 spacing) { m_glyph_spacing = spacing; }

    void set_glyph_width(u32 code_point, u8 width)
    {
        VERIFY(m_glyph_widths);
        m_glyph_widths[code_point] = width;
    }

    size_t glyph_count() const override { return m_glyph_count; }
    Optional<size_t> glyph_index(u32 code_point) const;

    u16 range_size() const { return m_range_mask_size; }
    bool is_range_empty(u32 code_point) const { return !(m_range_mask[code_point / 256 / 8] & 1 << (code_point / 256 % 8)); }

    String family() const override { return m_family; }
    void set_family(String family) { m_family = move(family); }
    String variant() const override;

    String qualified_name() const override;

private:
    BitmapFont(String name, String family, u8* rows, u8* widths, bool is_fixed_width,
        u8 glyph_width, u8 glyph_height, u8 glyph_spacing, u16 range_mask_size, u8* range_mask,
        u8 baseline, u8 mean_line, u8 presentation_size, u16 weight, u8 slope, bool owns_arrays = false);

    static RefPtr<BitmapFont> load_from_memory(u8 const*);

    template<typename T>
    int unicode_view_width(T const& view) const;

    void update_x_height() { m_x_height = m_baseline - m_mean_line; };
    int glyph_or_emoji_width_for_variable_width_font(u32 code_point) const;

    String m_name;
    String m_family;
    size_t m_glyph_count { 0 };

    u16 m_range_mask_size { 0 };
    u8* m_range_mask { nullptr };
    Vector<Optional<size_t>> m_range_indices;

    u8* m_rows { nullptr };
    u8* m_glyph_widths { nullptr };
    RefPtr<Core::MappedFile> m_mapped_file;

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
    u8 m_slope { 0 };

    bool m_fixed_width { false };
    bool m_owns_arrays { false };
};

}
