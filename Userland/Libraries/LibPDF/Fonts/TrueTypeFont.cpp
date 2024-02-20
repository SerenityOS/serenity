/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/ScaledFont.h>
#include <LibGfx/Painter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/TrueTypeFont.h>
#include <LibPDF/Renderer.h>

namespace PDF {

PDFErrorOr<void> TrueTypeFont::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(SimpleFont::initialize(document, dict, font_size));

    m_base_font_name = TRY(dict->get_name(document, CommonNames::BaseFont))->name();

    // If there's an embedded font program we use that; otherwise we try to find a replacement font
    if (dict->contains(CommonNames::FontDescriptor)) {
        auto descriptor = MUST(dict->get_dict(document, CommonNames::FontDescriptor));
        if (descriptor->contains(CommonNames::FontFile2)) {
            auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile2));
            auto ttf_font = TRY(OpenType::Font::try_load_from_externally_owned_memory(font_file_stream->bytes(), { .skip_tables = pdf_skipped_opentype_tables }));
            float point_size = (font_size * POINTS_PER_INCH) / DEFAULT_DPI;
            m_font = adopt_ref(*new Gfx::ScaledFont(*ttf_font, point_size, point_size));
        }
    }
    if (!m_font) {
        m_font = TRY(replacement_for(base_font_name().to_lowercase(), font_size));
    }

    VERIFY(m_font);
    return {};
}

Optional<float> TrueTypeFont::get_glyph_width(u8 char_code) const
{
    return m_font->glyph_width(char_code);
}

void TrueTypeFont::set_font_size(float font_size)
{
    m_font = m_font->with_size((font_size * POINTS_PER_INCH) / DEFAULT_DPI);
}

PDFErrorOr<void> TrueTypeFont::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const& renderer)
{
    auto style = renderer.state().paint_style;

    // Undo shift in Glyf::Glyph::append_simple_path() via OpenType::Font::rasterize_glyph().
    auto position = point.translated(0, -m_font->pixel_metrics().ascent);

    if (style.has<Color>()) {
        painter.draw_glyph(position, char_code, *m_font, style.get<Color>());
    } else {
        // FIXME: Bounding box and sample point look to be pretty wrong
        style.get<NonnullRefPtr<Gfx::PaintStyle>>()->paint(Gfx::IntRect(position.x(), position.y(), width, 0), [&](auto sample) {
            painter.draw_glyph(position, char_code, *m_font, sample(Gfx::IntPoint(position.x(), position.y())));
        });
    }
    return {};
}

}
