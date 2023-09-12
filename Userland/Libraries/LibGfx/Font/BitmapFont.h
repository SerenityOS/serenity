/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/DeprecatedString.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Size.h>

namespace Gfx {

class BitmapFont final : public Font {
public:
    virtual NonnullRefPtr<Font> clone() const override;
    ErrorOr<NonnullRefPtr<Font>> try_clone() const override;
    static NonnullRefPtr<BitmapFont> create(u8 glyph_height, u8 glyph_width, bool fixed, size_t glyph_count);
    static ErrorOr<NonnullRefPtr<BitmapFont>> try_create(u8 glyph_height, u8 glyph_width, bool fixed, size_t glyph_count);

    virtual FontPixelMetrics pixel_metrics() const override;

    ErrorOr<NonnullRefPtr<BitmapFont>> masked_character_set() const;
    ErrorOr<NonnullRefPtr<BitmapFont>> unmasked_character_set() const;

    static RefPtr<BitmapFont> load_from_file(DeprecatedString const& path);
    static ErrorOr<NonnullRefPtr<BitmapFont>> try_load_from_file(DeprecatedString const& path);
    static ErrorOr<NonnullRefPtr<BitmapFont>> try_load_from_mapped_file(OwnPtr<Core::MappedFile>);

    ErrorOr<void> write_to_file(DeprecatedString const& path);
    ErrorOr<void> write_to_file(NonnullOwnPtr<Core::File> file);

    ~BitmapFont();

    u8* rows() { return m_rows; }
    u8* widths() { return m_glyph_widths; }

    virtual float point_size() const override { return m_presentation_size; }

    u8 presentation_size() const override { return m_presentation_size; }
    void set_presentation_size(u8 size) { m_presentation_size = size; }

    virtual float pixel_size() const override { return m_glyph_height; }
    virtual int pixel_size_rounded_up() const override { return m_glyph_height; }

    u16 width() const override { return FontWidth::Normal; }

    u16 weight() const override { return m_weight; }
    void set_weight(u16 weight) { m_weight = weight; }

    virtual u8 slope() const override { return m_slope; }
    void set_slope(u8 slope) { m_slope = slope; }

    Glyph glyph(u32 code_point) const override;
    Glyph glyph(u32 code_point, GlyphSubpixelOffset) const override { return glyph(code_point); }

    float glyph_left_bearing(u32) const override { return 0; }

    Glyph raw_glyph(u32 code_point) const;
    bool contains_glyph(u32 code_point) const override;
    bool contains_raw_glyph(u32 code_point) const { return m_glyph_widths[code_point] > 0; }

    virtual float glyph_or_emoji_width(Utf8CodePointIterator&) const override;
    virtual float glyph_or_emoji_width(Utf32CodePointIterator&) const override;

    float glyphs_horizontal_kerning(u32, u32) const override { return 0.f; }
    u8 glyph_height() const { return m_glyph_height; }
    int x_height() const override { return m_x_height; }
    virtual float preferred_line_height() const override { return glyph_height() + m_line_gap; }

    virtual float glyph_width(u32 code_point) const override;
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

    virtual float width(StringView) const override;
    virtual float width(Utf8View const&) const override;
    virtual float width(Utf32View const&) const override;

    virtual int width_rounded_up(StringView) const override;

    virtual String name() const override { return m_name; }
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

    virtual String family() const override { return m_family; }
    void set_family(String family) { m_family = move(family); }
    virtual String variant() const override;

    virtual String qualified_name() const override;
    virtual String human_readable_name() const override { return MUST(String::formatted("{} {} {}", family(), variant(), presentation_size())); }

    virtual RefPtr<Font> with_size(float point_size) const override;

private:
    BitmapFont(String name, String family, u8* rows, u8* widths, bool is_fixed_width,
        u8 glyph_width, u8 glyph_height, u8 glyph_spacing, u16 range_mask_size, u8* range_mask,
        u8 baseline, u8 mean_line, u8 presentation_size, u16 weight, u8 slope, bool owns_arrays = false);

    static ErrorOr<NonnullRefPtr<BitmapFont>> load_from_memory(u8 const*);

    template<typename T>
    int unicode_view_width(T const& view) const;

    void update_x_height() { m_x_height = m_baseline - m_mean_line; }

    virtual bool has_color_bitmaps() const override { return false; }

    String m_name;
    String m_family;
    size_t m_glyph_count { 0 };

    u16 m_range_mask_size { 0 };
    u8* m_range_mask { nullptr };
    Vector<Optional<size_t>> m_range_indices;

    u8* m_rows { nullptr };
    u8* m_glyph_widths { nullptr };
    OwnPtr<Core::MappedFile> m_mapped_file;

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
    u8 m_line_gap { 4 };

    bool m_fixed_width { false };
    bool m_owns_arrays { false };
};

}
