/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Fonts/TrueTypeFont.h>
#include <LibPDF/Fonts/Type0Font.h>
#include <LibPDF/Fonts/Type1Font.h>

namespace PDF {

static bool is_standard_latin_font(DeprecatedFlyString const& font)
{
    return font.is_one_of(
        "Times-Roman", "TimesNewRoman",
        "Helvetica", "Arial",
        "Courier", "CourierNew",
        "Times-Bold", "TimesNewRoman,Bold",
        "Helvetica-Bold", "Arial,Bold",
        "Courier-Bold", "CourierNew,Bold",
        "Times-Italic", "TimesNewRoman,Italic",
        "Helvetica-Oblique", "Arial,Italic",
        "Courier-Oblique", "CourierNew,Italic",
        "Times-BoldItalic", "TimesNewRoman,BoldItalic",
        "Helvetica-BoldOblique", "Arial,BoldItalic",
        "Courier-BoldOblique", "CourierNew,BoldItalic");
}

PDFErrorOr<void> PDFFont::CommonData::load_from_dict(Document* document, NonnullRefPtr<DictObject> dict, float font_size)
{
    auto base_font = TRY(dict->get_name(document, CommonNames::BaseFont))->name();
    if ((is_standard_font = is_standard_latin_font(base_font))) {
        auto replacement = replacement_for_standard_latin_font(base_font.to_lowercase());
        font = Gfx::FontDatabase::the().get(replacement.get<0>(), replacement.get<1>(), font_size);
        VERIFY(font);
    }

    if (dict->contains(CommonNames::Encoding)) {
        auto encoding_object = MUST(dict->get_object(document, CommonNames::Encoding));
        encoding = TRY(Encoding::from_object(document, encoding_object));
    } else {
        // FIXME: The spec doesn't specify what the encoding should be in this case
        if (is_standard_font)
            encoding = Encoding::standard_encoding();
        // Otherwise, use the built-in encoding of the font.
    }

    if (dict->contains(CommonNames::ToUnicode))
        to_unicode = TRY(dict->get_stream(document, CommonNames::ToUnicode));

    if (dict->contains(CommonNames::FirstChar) && dict->contains(CommonNames::LastChar) && dict->contains(CommonNames::Widths)) {
        auto first_char = dict->get_value(CommonNames::FirstChar).get<int>();
        auto last_char = dict->get_value(CommonNames::LastChar).get<int>();
        auto widths_array = TRY(dict->get_array(document, CommonNames::Widths));

        VERIFY(widths_array->size() == static_cast<size_t>(last_char - first_char + 1));

        for (size_t i = 0; i < widths_array->size(); i++)
            widths.set(first_char + i, widths_array->at(i).to_int());
    }

    if (dict->contains(CommonNames::FontDescriptor)) {
        auto descriptor = TRY(dict->get_dict(document, CommonNames::FontDescriptor));
        if (descriptor->contains(CommonNames::MissingWidth))
            missing_width = descriptor->get_value(CommonNames::MissingWidth).to_int();
    }

    return {};
}

PDFErrorOr<NonnullRefPtr<PDFFont>> PDFFont::create(Document* document, NonnullRefPtr<DictObject> dict, float font_size)
{
    auto subtype = TRY(dict->get_name(document, CommonNames::Subtype))->name();

    if (subtype == "Type0")
        return TRY(Type0Font::create(document, dict));
    if (subtype == "Type1")
        return TRY(Type1Font::create(document, dict, font_size));
    if (subtype == "TrueType")
        return TRY(TrueTypeFont::create(document, dict, font_size));

    return Error(Error::Type::Internal, TRY(String::formatted("Unhandled font subtype: {}", subtype)).to_deprecated_string());
}

Tuple<DeprecatedString, DeprecatedString> PDFFont::replacement_for_standard_latin_font(StringView name)
{
    bool is_bold = name.contains("bold"sv);
    bool is_italic = name.contains("italic"sv);

    DeprecatedString font_variant;

    if (is_bold && is_italic) {
        font_variant = "BoldItalic";
    } else if (is_bold) {
        font_variant = "Bold";
    } else if (is_italic) {
        font_variant = "Italic";
    } else {
        font_variant = "Regular";
    }

    return { "Liberation Serif", font_variant };
}

}
