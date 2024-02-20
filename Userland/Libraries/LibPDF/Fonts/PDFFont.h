/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Tuple.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Forward.h>
#include <LibPDF/Document.h>
#include <LibPDF/Encoding.h>

namespace PDF {

class Renderer;

// PDF files don't need most of the data in OpenType fonts, and even contain invalid data for
// these tables in some cases. Skip reading these tables.
constexpr u32 pdf_skipped_opentype_tables = OpenType::FontOptions::SkipTables::Name | OpenType::FontOptions::SkipTables::Hmtx | OpenType::FontOptions::SkipTables::OS2;

class PDFFont : public RefCounted<PDFFont> {
public:
    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject> const&, float font_size);

    virtual ~PDFFont() = default;

    virtual void set_font_size(float font_size) = 0;
    virtual PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint, ByteString const&, Renderer const&) = 0;

protected:
    virtual PDFErrorOr<void> initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size);
    static PDFErrorOr<NonnullRefPtr<Gfx::Font>> replacement_for(StringView name, float font_size);
};

}
