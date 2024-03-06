/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Forward.h>
#include <LibPDF/Document.h>
#include <LibPDF/Encoding.h>

namespace PDF {

class Renderer;

// PDF files don't need most of the data in OpenType fonts, and even contain invalid data for
// these tables in some cases. Skip reading these tables.
constexpr u32 pdf_skipped_opentype_tables = OpenType::FontOptions::SkipTables::Name | OpenType::FontOptions::SkipTables::Hmtx | OpenType::FontOptions::SkipTables::OS2;

enum class WritingMode {
    Horizontal,
    Vertical,
};

class PDFFont : public RefCounted<PDFFont> {
public:
    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject> const&, float font_size);

    virtual ~PDFFont() = default;

    virtual void set_font_size(float font_size) = 0;
    virtual PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint, ByteString const&, Renderer const&) = 0;

    virtual WritingMode writing_mode() const { return WritingMode::Horizontal; }

    // TABLE 5.20 Font flags
    bool is_fixed_pitch() const { return m_flags & (1 << (1 - 1)); }
    bool is_serif() const { return m_flags & (1 << (2 - 1)); }

    static constexpr unsigned Symbolic = 1 << (3 - 1);
    bool is_symbolic() const { return m_flags & Symbolic; }

    bool is_script() const { return m_flags & (1 << (4 - 1)); }

    // Note: No bit position 5.
    static constexpr unsigned NonSymbolic = 1 << (6 - 1);
    bool is_nonsymbolic() const { return m_flags & NonSymbolic; }
    bool is_italic() const { return m_flags & (1 << (7 - 1)); }
    // Note: Big jump in bit positions.
    bool is_all_cap() const { return m_flags & (1 << (17 - 1)); }
    bool is_small_cap() const { return m_flags & (1 << (18 - 1)); }
    bool is_force_bold() const { return m_flags & (1 << (19 - 1)); }

protected:
    virtual PDFErrorOr<void> initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size);
    static PDFErrorOr<NonnullRefPtr<Gfx::ScaledFont>> replacement_for(StringView name, float font_size);

    unsigned m_flags { 0 };
};

}
