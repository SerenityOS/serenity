/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/TypeCasts.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Fonts/TrueTypeFont.h>
#include <LibPDF/Fonts/Type0Font.h>
#include <LibPDF/Fonts/Type1Font.h>
#include <LibPDF/Fonts/Type3Font.h>

namespace PDF {

[[maybe_unused]] static bool is_standard_latin_font(DeprecatedFlyString const& font)
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

PDFErrorOr<NonnullRefPtr<PDFFont>> PDFFont::create(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    auto subtype = TRY(dict->get_name(document, CommonNames::Subtype))->name();

    RefPtr<PDFFont> font;
    if (subtype == "Type1") {
        font = adopt_ref(*new Type1Font());
    } else if (subtype == "TrueType") {
        font = adopt_ref(*new TrueTypeFont());
    } else if (subtype == "Type0") {
        font = adopt_ref(*new Type0Font());
    } else if (subtype == "Type3") {
        font = adopt_ref(*new Type3Font());
    } else {
        dbgln_if(PDF_DEBUG, "Unhandled font subtype: {}", subtype);
        return Error::internal_error("Unhandled font subtype");
    }

    TRY(font->initialize(document, dict, font_size));
    return font.release_nonnull();
}

PDFErrorOr<void> PDFFont::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float)
{
    if (dict->contains(CommonNames::FontDescriptor)) {
        auto descriptor = TRY(dict->get_dict(document, CommonNames::FontDescriptor));
        if (descriptor->contains(CommonNames::Flags))
            m_flags = descriptor->get_value(CommonNames::Flags).to_int();
    }

    return {};
}

PDFErrorOr<NonnullRefPtr<Gfx::ScaledFont>> PDFFont::replacement_for(StringView name, float font_size)
{
    bool is_bold = name.contains("bold"sv, CaseSensitivity::CaseInsensitive);
    bool is_italic = name.contains("italic"sv, CaseSensitivity::CaseInsensitive) || name.contains("oblique"sv, CaseSensitivity::CaseInsensitive);

    FlyString font_family;
    if (name.contains("times"sv, CaseSensitivity::CaseInsensitive)) {
        font_family = "Liberation Serif"_fly_string;
    } else if (name.contains("courier"sv, CaseSensitivity::CaseInsensitive)) {
        font_family = "Liberation Mono"_fly_string;
    } else {
        font_family = "Liberation Sans"_fly_string;
    }

    FlyString font_variant;

    if (is_bold && is_italic) {
        font_variant = "Bold Italic"_fly_string;
    } else if (is_bold) {
        font_variant = "Bold"_fly_string;
    } else if (is_italic) {
        font_variant = "Italic"_fly_string;
    } else {
        font_variant = "Regular"_fly_string;
    }

    float point_size = (font_size * POINTS_PER_INCH) / DEFAULT_DPI;
    auto font = Gfx::FontDatabase::the().get(font_family, font_variant, point_size);
    if (!font)
        return Error::internal_error("Failed to load {} {} at {}pt", font_family, font_variant, point_size);

    VERIFY(is<Gfx::ScaledFont>(*font));
    return static_ptr_cast<Gfx::ScaledFont>(font.release_nonnull());
}

}
