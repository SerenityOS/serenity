/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSFontFaceRule.h>

namespace Web::CSS {

CSSFontFaceRule::CSSFontFaceRule(FontFace&& font_face)
    : m_font_face(move(font_face))
{
}

CSSStyleDeclaration* CSSFontFaceRule::style()
{
    // FIXME: Return a CSSStyleDeclaration subclass that directs changes to the FontFace.
    return nullptr;
}

// https://www.w3.org/TR/cssom/#ref-for-cssfontfacerule
String CSSFontFaceRule::serialized() const
{
    // FIXME: Implement this!
    return "@font-face { /* FIXME: Implement CSSFontFaceRule::serialized()! */ }";
}

}
