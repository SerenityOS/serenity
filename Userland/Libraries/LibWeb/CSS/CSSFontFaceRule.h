/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/FontFace.h>

namespace Web::CSS {

class CSSFontFaceRule final : public CSSRule {
    AK_MAKE_NONCOPYABLE(CSSFontFaceRule);
    AK_MAKE_NONMOVABLE(CSSFontFaceRule);

public:
    using WrapperType = Bindings::CSSFontFaceRuleWrapper;

    static NonnullRefPtr<CSSFontFaceRule> create(FontFace&& font_face)
    {
        return adopt_ref(*new CSSFontFaceRule(move(font_face)));
    }

    virtual ~CSSFontFaceRule() override = default;

    virtual StringView class_name() const override { return "CSSFontFaceRule"; }
    virtual Type type() const override { return Type::FontFace; }

    FontFace const& font_face() const { return m_font_face; }
    CSSStyleDeclaration* style();

private:
    explicit CSSFontFaceRule(FontFace&&);

    virtual String serialized() const override;

    FontFace m_font_face;
};

template<>
inline bool CSSRule::fast_is<CSSFontFaceRule>() const { return type() == CSSRule::Type::FontFace; }

}
