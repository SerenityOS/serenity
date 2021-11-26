/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSRule.h>

namespace Web::CSS {

CSSRule::~CSSRule()
{
}

// https://www.w3.org/TR/cssom/#dom-cssrule-csstext
String CSSRule::css_text() const
{
    // The cssText attribute must return a serialization of the CSS rule.
    return serialized();
}

// https://www.w3.org/TR/cssom/#dom-cssrule-csstext
void CSSRule::set_css_text(StringView)
{
    // On setting the cssText attribute must do nothing.
}

}
