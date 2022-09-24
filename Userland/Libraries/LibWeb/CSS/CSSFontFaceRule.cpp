/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSFontFaceRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

CSSFontFaceRule* CSSFontFaceRule::create(JS::Realm& realm, FontFace&& font_face)
{
    return realm.heap().allocate<CSSFontFaceRule>(realm, realm, move(font_face));
}

CSSFontFaceRule::CSSFontFaceRule(JS::Realm& realm, FontFace&& font_face)
    : CSSRule(realm)
    , m_font_face(move(font_face))
{
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSFontFaceRulePrototype>(realm, "CSSFontFaceRule"));
}

CSSStyleDeclaration* CSSFontFaceRule::style()
{
    // FIXME: Return a CSSStyleDeclaration subclass that directs changes to the FontFace.
    return nullptr;
}

// https://www.w3.org/TR/cssom/#ref-for-cssfontfacerule
String CSSFontFaceRule::serialized() const
{
    StringBuilder builder;
    // The result of concatenating the following:

    // 1. The string "@font-face {", followed by a single SPACE (U+0020).
    builder.append("@font-face { "sv);

    // 2. The string "font-family:", followed by a single SPACE (U+0020).
    builder.append("font-family: "sv);

    // 3. The result of performing serialize a string on the rule’s font family name.
    serialize_a_string(builder, m_font_face.font_family());

    // 4. The string ";", i.e., SEMICOLON (U+003B).
    builder.append(';');

    // 5. If the rule’s associated source list is not empty, follow these substeps:
    if (!m_font_face.sources().is_empty()) {
        // 1. A single SPACE (U+0020), followed by the string "src:", followed by a single SPACE (U+0020).
        builder.append(" src: "sv);

        // 2. The result of invoking serialize a comma-separated list on performing serialize a URL or serialize a LOCAL for each source on the source list.
        serialize_a_comma_separated_list(builder, m_font_face.sources(), [&](FontFace::Source source) {
            if (source.url.cannot_be_a_base_url()) {
                serialize_a_url(builder, source.url.to_string());
            } else {
                serialize_a_local(builder, source.url.to_string());
            }

            // NOTE: No spec currently exists for format()
            if (source.format.has_value()) {
                builder.append("format(\""sv);
                serialize_a_string(builder, source.format.value());
                builder.append("\")"sv);
            }
        });

        // 3. The string ";", i.e., SEMICOLON (U+003B).
        builder.append(';');
    }

    // 6. If rule’s associated unicode-range descriptor is present, a single SPACE (U+0020), followed by the string "unicode-range:", followed by a single SPACE (U+0020), followed by the result of performing serialize a <'unicode-range'>, followed by the string ";", i.e., SEMICOLON (U+003B).
    builder.append(" unicode-range: "sv);
    serialize_unicode_ranges(builder, m_font_face.unicode_ranges());
    builder.append(';');

    // FIXME: 7. If rule’s associated font-variant descriptor is present, a single SPACE (U+0020),
    // followed by the string "font-variant:", followed by a single SPACE (U+0020),
    // followed by the result of performing serialize a <'font-variant'>,
    // followed by the string ";", i.e., SEMICOLON (U+003B).

    // FIXME: 8. If rule’s associated font-feature-settings descriptor is present, a single SPACE (U+0020),
    // followed by the string "font-feature-settings:", followed by a single SPACE (U+0020),
    // followed by the result of performing serialize a <'font-feature-settings'>,
    // followed by the string ";", i.e., SEMICOLON (U+003B).

    // FIXME: 9. If rule’s associated font-stretch descriptor is present, a single SPACE (U+0020),
    // followed by the string "font-stretch:", followed by a single SPACE (U+0020),
    // followed by the result of performing serialize a <'font-stretch'>,
    // followed by the string ";", i.e., SEMICOLON (U+003B).

    // FIXME: 10. If rule’s associated font-weight descriptor is present, a single SPACE (U+0020),
    // followed by the string "font-weight:", followed by a single SPACE (U+0020),
    // followed by the result of performing serialize a <'font-weight'>,
    // followed by the string ";", i.e., SEMICOLON (U+003B).

    // FIXME: 11. If rule’s associated font-style descriptor is present, a single SPACE (U+0020),
    // followed by the string "font-style:", followed by a single SPACE (U+0020),
    // followed by the result of performing serialize a <'font-style'>,
    // followed by the string ";", i.e., SEMICOLON (U+003B).

    // 12. A single SPACE (U+0020), followed by the string "}", i.e., RIGHT CURLY BRACKET (U+007D).
    builder.append(" }"sv);

    return builder.to_string();
}

}
