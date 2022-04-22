/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class CSSRule
    : public RefCounted<CSSRule>
    , public Bindings::Wrappable
    , public Weakable<CSSRule> {
public:
    using WrapperType = Bindings::CSSRuleWrapper;

    virtual ~CSSRule() = default;

    // https://drafts.csswg.org/cssom/#dom-cssrule-type
    enum class Type : u16 {
        Style = 1,
        Import = 3,
        Media = 4,
        FontFace = 5,
        Supports = 12,
    };

    virtual StringView class_name() const = 0;
    virtual Type type() const = 0;

    String css_text() const;
    void set_css_text(StringView);

    CSSRule* parent_rule() { return m_parent_rule; }
    void set_parent_rule(CSSRule*);

    CSSStyleSheet* parent_style_sheet() { return m_parent_style_sheet; }
    virtual void set_parent_style_sheet(CSSStyleSheet*);

    template<typename T>
    bool fast_is() const = delete;

protected:
    virtual String serialized() const = 0;

    WeakPtr<CSSRule> m_parent_rule;
    WeakPtr<CSSStyleSheet> m_parent_style_sheet;
};

}
