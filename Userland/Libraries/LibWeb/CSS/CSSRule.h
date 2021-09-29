/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class CSSRule
    : public RefCounted<CSSRule>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::CSSRuleWrapper;

    virtual ~CSSRule();

    enum class Type : u32 {
        Style,
        Import,
        Media,
        __Count,
    };

    virtual StringView class_name() const = 0;
    virtual Type type() const = 0;

    template<typename T>
    bool fast_is() const = delete;

private:
};

}
