/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibWeb/Bindings/CSSRuleWrapper.h>
#include <LibWeb/Bindings/CSSRuleWrapperFactory.h>
#include <LibWeb/Bindings/CSSStyleRuleWrapper.h>
#include <LibWeb/CSS/CSSStyleRule.h>

namespace Web::Bindings {

CSSRuleWrapper* wrap(JS::GlobalObject& global_object, CSS::CSSRule& rule)
{
    if (is<CSS::CSSStyleRule>(rule))
        return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, verify_cast<CSS::CSSStyleRule>(rule)));
    return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, rule));
}

}
