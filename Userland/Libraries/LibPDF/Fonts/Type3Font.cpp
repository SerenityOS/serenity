/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/Type3Font.h>
#include <LibPDF/Renderer.h>

namespace PDF {

PDFErrorOr<void> Type3Font::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(SimpleFont::initialize(document, dict, font_size));

    // "TABLE 5.9 Entries in a Type 3 font dictionary"

    if (!dict->contains(CommonNames::CharProcs))
        return Error { Error::Type::MalformedPDF, "Type3 font missing /CharProcs" };
    auto char_procs = TRY(document->resolve_to<DictObject>(dict->get_value(CommonNames::CharProcs)));
    for (auto const& [name, value] : char_procs->map())
        m_char_procs.set(name, TRY(document->resolve_to<StreamObject>(value)));

    if (!dict->contains(CommonNames::FontMatrix))
        return Error { Error::Type::MalformedPDF, "Type3 font missing /FontMatrix" };
    auto font_matrix_object = TRY(document->resolve_to<ArrayObject>(dict->get_value(CommonNames::FontMatrix)));
    if (font_matrix_object->size() != 6)
        return Error { Error::Type::MalformedPDF, "Type3 font /FontMatrix must have 6 elements" };
    font_matrix() = Gfx::AffineTransform {
        TRY(document->resolve(font_matrix_object->at(0))).to_float(),
        TRY(document->resolve(font_matrix_object->at(1))).to_float(),
        TRY(document->resolve(font_matrix_object->at(2))).to_float(),
        TRY(document->resolve(font_matrix_object->at(3))).to_float(),
        TRY(document->resolve(font_matrix_object->at(4))).to_float(),
        TRY(document->resolve(font_matrix_object->at(5))).to_float(),
    };

    if (dict->contains(CommonNames::Resources))
        m_resources = TRY(document->resolve_to<DictObject>(dict->get_value(CommonNames::Resources)));

    // FIXME: /FontBBox

    return {};
}

Optional<float> Type3Font::get_glyph_width(u8) const
{
    return OptionalNone {};
}

void Type3Font::set_font_size(float)
{
}

PDFErrorOr<void> Type3Font::draw_glyph(Gfx::Painter&, Gfx::FloatPoint point, float, u8 char_code, Renderer const& renderer)
{
    // PDF 1.7 spec, 5.5.4 Type 3 Fonts:
    // "For each character code shown by a text-showing operator that uses a Type 3 font,
    //  the consumer application does the following:"

    // "1. Looks up the character code in the font’s Encoding entry, as described in Sec-
    //  tion 5.5.5, “Character Encoding,” to obtain a character name."
    auto char_name = encoding()->get_name(char_code);

    // "2. Looks up the character name in the font’s CharProcs dictionary to obtain a
    //  stream object containing a glyph description. (If the name is not present as a
    //  key in CharProcs, no glyph is painted.)"
    auto char_proc = m_char_procs.get(char_name);
    if (!char_proc.has_value())
        return {};

    // "3. Invokes the glyph description, as described below."
    // The Gfx::Painter isn't used because `renderer` paints to it already.
    // FIXME: Do color things dependent on if the glyph data starts with d0 or d1.
    // FIXME: Glyph caching.
    return const_cast<Renderer&>(renderer).render_type3_glyph(point, *char_proc.value(), font_matrix(), m_resources);
}
}
