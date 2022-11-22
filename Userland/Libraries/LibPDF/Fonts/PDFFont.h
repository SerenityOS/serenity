/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Forward.h>
#include <LibPDF/Document.h>
#include <LibPDF/Encoding.h>

namespace PDF {

class PDFFont : public RefCounted<PDFFont> {
public:
    enum class Type {
        Type0,
        Type1,
        TrueType
    };

    // This is used both by Type 1 and TrueType fonts.
    struct CommonData {
        RefPtr<StreamObject> to_unicode;
        RefPtr<Encoding> encoding;
        HashMap<u16, u16> widths;
        u16 missing_width;
        bool is_standard_font;

        PDFErrorOr<void> load_from_dict(Document*, NonnullRefPtr<DictObject>);
    };

    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject>);

    virtual ~PDFFont() = default;

    virtual u32 char_code_to_code_point(u16 char_code) const = 0;
    virtual float get_char_width(u16 char_code, float font_size) const = 0;

    virtual void draw_glyph(Gfx::Painter& painter, Gfx::IntPoint const& point, float width, u32 char_code, Color color) = 0;

    virtual bool is_standard_font() const { return m_is_standard_font; }
    virtual Type type() const = 0;

protected:
    bool m_is_standard_font { false };
};

}
