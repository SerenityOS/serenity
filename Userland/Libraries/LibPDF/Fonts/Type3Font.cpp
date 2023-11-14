/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/Type3Font.h>

namespace PDF {

PDFErrorOr<void> Type3Font::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(SimpleFont::initialize(document, dict, font_size));

    // FIXME: /CharProcs, /FontBBox, /FontMatrix, /Resources

    return {};
}

Optional<float> Type3Font::get_glyph_width(u8) const
{
    return OptionalNone {};
}

void Type3Font::set_font_size(float)
{
}

PDFErrorOr<void> Type3Font::draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u8, Color)
{
    return Error { Error::Type::RenderingUnsupported, "Type3 fonts not yet implemented" };
}
}
