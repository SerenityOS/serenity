/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts.h>

namespace PDF {

static bool is_standard_latin_font(FlyString const& font)
{
    return font.is_one_of(
        "Times-Roman",
        "Helvetica",
        "Courier",
        "Times-Bold",
        "Helvetica-Bold",
        "Courier-Bold",
        "Times-Italic",
        "Helvetica-Oblique",
        "Courier-Oblique",
        "Times-BoldItalic",
        "Helvetica-BoldOblique",
        "Courier-BoldOblique");
}

PDFErrorOr<NonnullRefPtr<PDFFont>> PDFFont::create(Document* document, NonnullRefPtr<DictObject> dict)
{
    auto subtype = TRY(dict->get_name(document, CommonNames::Subtype))->name();

    if (subtype == "Type1")
        return TRY(Type1Font::create(document, dict));

    TODO();
}

PDFErrorOr<NonnullRefPtr<Type1Font>> Type1Font::create(Document* document, NonnullRefPtr<DictObject> dict)
{
    // FIXME: "Required except for the standard 14 fonts"...
    //        "Beginning with PDF 1.5, the special treatment given to the standard 14
    //        fonts is deprecated. [...] For backwards capability, conforming readers
    //        shall still provide the special treatment identifier for the standard
    //        14 fonts."

    RefPtr<Encoding> encoding;

    if (dict->contains(CommonNames::Encoding)) {
        auto encoding_object = MUST(dict->get_object(document, CommonNames::Encoding));
        encoding = TRY(Encoding::from_object(document, encoding_object));
    } else {
        auto base_font = MUST(dict->get_name(document, CommonNames::BaseFont))->name();
        if (is_standard_latin_font(base_font)) {
            // FIXME: The spec doesn't specify what the encoding should be in this case
            encoding = Encoding::standard_encoding();
        } else {
            TODO();
        }
    }

    RefPtr<StreamObject> to_unicode;
    if (dict->contains(CommonNames::ToUnicode))
        to_unicode = MUST(dict->get_stream(document, CommonNames::ToUnicode));

    return adopt_ref(*new Type1Font(to_unicode, encoding.release_nonnull()));
}

Type1Font::Type1Font(RefPtr<StreamObject> to_unicode, NonnullRefPtr<Encoding> encoding)
    : m_to_unicode(to_unicode)
    , m_encoding(encoding)
{
}

u32 Type1Font::char_code_to_code_point(u16 char_code) const
{
    if (m_to_unicode)
        TODO();

    auto descriptor = m_encoding->get_char_code_descriptor(char_code);
    return descriptor.code_point;
}

}
