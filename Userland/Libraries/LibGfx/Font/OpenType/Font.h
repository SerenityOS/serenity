/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/OpenType/Cmap.h>
#include <LibGfx/Font/OpenType/Glyf.h>
#include <LibGfx/Font/OpenType/Tables.h>
#include <LibGfx/Font/VectorFont.h>

namespace OpenType {

class Font : public Gfx::VectorFont {
    AK_MAKE_NONCOPYABLE(Font);

public:
    static ErrorOr<NonnullRefPtr<Font>> try_load_from_file(DeprecatedString path, unsigned index = 0);
    static ErrorOr<NonnullRefPtr<Font>> try_load_from_externally_owned_memory(ReadonlyBytes bytes, unsigned index = 0);

    virtual Gfx::ScaledFontMetrics metrics(float x_scale, float y_scale) const override;
    virtual Gfx::ScaledGlyphMetrics glyph_metrics(u32 glyph_id, float x_scale, float y_scale, float point_width, float point_height) const override;
    virtual float glyphs_horizontal_kerning(u32 left_glyph_id, u32 right_glyph_id, float x_scale) const override;
    virtual RefPtr<Gfx::Bitmap> rasterize_glyph(u32 glyph_id, float x_scale, float y_scale, Gfx::GlyphSubpixelOffset) const override;
    virtual u32 glyph_count() const override;
    virtual u16 units_per_em() const override;
    virtual u32 glyph_id_for_code_point(u32 code_point) const override;
    virtual String family() const override;
    virtual String variant() const override;
    virtual u16 weight() const override;
    virtual u16 width() const override;
    virtual u8 slope() const override;
    virtual bool is_fixed_width() const override;
    virtual bool has_color_bitmaps() const override;

    Optional<ReadonlyBytes> font_program() const;
    Optional<ReadonlyBytes> control_value_program() const;
    Optional<ReadonlyBytes> glyph_program(u32 glyph_id) const;

private:
    RefPtr<Gfx::Bitmap> color_bitmap(u32 glyph_id) const;

    struct EmbeddedBitmapWithFormat17 {
        CBLC::BitmapSize const& bitmap_size;
        CBDT::Format17 const& format17;
    };

    using EmbeddedBitmapData = Variant<EmbeddedBitmapWithFormat17, Empty>;

    EmbeddedBitmapData embedded_bitmap_data_for_glyph(u32 glyph_id) const;

    enum class Offsets {
        NumTables = 4,
        TableRecord_Offset = 8,
        TableRecord_Length = 12,
    };
    enum class Sizes {
        TTCHeaderV1 = 12,
        OffsetTable = 12,
        TableRecord = 16,
    };

    static ErrorOr<NonnullRefPtr<Font>> try_load_from_offset(ReadonlyBytes, unsigned index = 0);

    Font(
        ReadonlyBytes bytes,
        Head&& head,
        Name&& name,
        Hhea&& hhea,
        Maxp&& maxp,
        Hmtx&& hmtx,
        Cmap&& cmap,
        Optional<Loca>&& loca,
        Optional<Glyf>&& glyf,
        Optional<OS2> os2,
        Optional<Kern>&& kern,
        Optional<Fpgm> fpgm,
        Optional<Prep> prep,
        Optional<CBLC> cblc,
        Optional<CBDT> cbdt,
        Optional<GPOS> gpos)
        : m_buffer(move(bytes))
        , m_head(move(head))
        , m_name(move(name))
        , m_hhea(move(hhea))
        , m_maxp(move(maxp))
        , m_hmtx(move(hmtx))
        , m_loca(move(loca))
        , m_glyf(move(glyf))
        , m_cmap(move(cmap))
        , m_os2(move(os2))
        , m_kern(move(kern))
        , m_fpgm(move(fpgm))
        , m_prep(move(prep))
        , m_cblc(move(cblc))
        , m_cbdt(move(cbdt))
        , m_gpos(move(gpos))
    {
    }

    OwnPtr<Core::MappedFile> m_mapped_file;

    ReadonlyBytes m_buffer;

    // These are stateful wrappers around non-owning slices
    Head m_head;
    Name m_name;
    Hhea m_hhea;
    Maxp m_maxp;
    Hmtx m_hmtx;
    Optional<Loca> m_loca;
    Optional<Glyf> m_glyf;
    Cmap m_cmap;
    Optional<OS2> m_os2;
    Optional<Kern> m_kern;
    Optional<Fpgm> m_fpgm;
    Optional<Prep> m_prep;
    Optional<CBLC> m_cblc;
    Optional<CBDT> m_cbdt;
    Optional<GPOS> m_gpos;

    // This cache stores information per code point.
    // It's segmented into pages with data about 256 code points each.
    struct GlyphPage {
        static constexpr size_t glyphs_per_page = 256;

        u32 glyph_ids[glyphs_per_page];
    };

    // Fast cache for GlyphPage #0 (code points 0-255) to avoid hash lookups for all of ASCII and Latin-1.
    mutable OwnPtr<GlyphPage> m_glyph_page_zero;

    mutable HashMap<size_t, NonnullOwnPtr<GlyphPage>> m_glyph_pages;

    mutable HashMap<u32, i16> m_kerning_cache;

    GlyphPage const& glyph_page(size_t page_index) const;
    void populate_glyph_page(GlyphPage&, size_t page_index) const;
};

}
