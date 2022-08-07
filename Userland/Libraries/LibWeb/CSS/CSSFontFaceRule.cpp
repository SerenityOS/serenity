/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSFontFaceRulePrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>

namespace Web::CSS {

CSSFontFaceRule* CSSFontFaceRule::create(Bindings::WindowObject& window_object, FontFace&& font_face)
{
    return window_object.heap().allocate<CSSFontFaceRule>(window_object.realm(), window_object, move(font_face));
}

CSSFontFaceRule::CSSFontFaceRule(Bindings::WindowObject& window_object, FontFace&& font_face)
    : CSSRule(window_object)
    , m_font_face(move(font_face))
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::CSSFontFaceRulePrototype>("CSSFontFaceRule"));
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
