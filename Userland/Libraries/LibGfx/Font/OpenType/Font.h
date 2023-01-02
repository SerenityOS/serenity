/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
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
    virtual Gfx::ScaledGlyphMetrics glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const override;
    virtual float glyphs_horizontal_kerning(u32 left_glyph_id, u32 right_glyph_id, float x_scale) const override;
    virtual RefPtr<Gfx::Bitmap> rasterize_glyph(u32 glyph_id, float x_scale, float y_scale, Gfx::GlyphSubpixelOffset) const override;
    virtual u32 glyph_count() const override;
    virtual u16 units_per_em() const override;
    virtual u32 glyph_id_for_code_point(u32 code_point) const override { return m_cmap.glyph_id_for_code_point(code_point); }
    virtual DeprecatedString family() const override;
    virtual DeprecatedString variant() const override;
    virtual u16 weight() const override;
    virtual u8 slope() const override;
    virtual bool is_fixed_width() const override;

private:
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

    Font(ReadonlyBytes bytes, Head&& head, Name&& name, Hhea&& hhea, Maxp&& maxp, Hmtx&& hmtx, Cmap&& cmap, Loca&& loca, Glyf&& glyf, Optional<OS2> os2, Optional<Kern>&& kern)
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
    {
    }

    RefPtr<Core::MappedFile> m_mapped_file;

    ReadonlyBytes m_buffer;

    // These are stateful wrappers around non-owning slices
    Head m_head;
    Name m_name;
    Hhea m_hhea;
    Maxp m_maxp;
    Hmtx m_hmtx;
    Loca m_loca;
    Glyf m_glyf;
    Cmap m_cmap;
    Optional<OS2> m_os2;
    Optional<Kern> m_kern;
};

}
