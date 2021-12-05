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

    virtual ~CSSRule();

    enum class Type : u32 {
        Style,
        Import,
        Media,
        Supports,
        __Count,
    };

    virtual StringView class_name() const = 0;
    virtual Type type() const = 0;

    String css_text() const;
    void set_css_text(StringView);

    template<typename T>
    bool fast_is() const = delete;

protected:
    virtual String serialized() const = 0;
};

}
