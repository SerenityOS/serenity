/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/FontFace.h>

namespace Web::CSS {

class CSSFontFaceRule final : public CSSRule {
    WEB_PLATFORM_OBJECT(CSSFontFaceRule, CSSRule);

public:
    [[nodiscard]] static JS::NonnullGCPtr<CSSFontFaceRule> create(JS::Realm&, FontFace&&);

    virtual ~CSSFontFaceRule() override = default;

    virtual Type type() const override { return Type::FontFace; }

    FontFace const& font_face() const { return m_font_face; }
    CSSStyleDeclaration* style();

private:
    CSSFontFaceRule(JS::Realm&, FontFace&&);

    virtual void initialize(JS::Realm&) override;
    virtual DeprecatedString serialized() const override;

    FontFace m_font_face;
};

template<>
inline bool CSSRule::fast_is<CSSFontFaceRule>() const { return type() == CSSRule::Type::FontFace; }

}
