/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibWeb/Bindings/CSSFontFaceRuleWrapper.h>
#include <LibWeb/Bindings/CSSImportRuleWrapper.h>
#include <LibWeb/Bindings/CSSMediaRuleWrapper.h>
#include <LibWeb/Bindings/CSSRuleWrapper.h>
#include <LibWeb/Bindings/CSSRuleWrapperFactory.h>
#include <LibWeb/Bindings/CSSStyleRuleWrapper.h>
#include <LibWeb/Bindings/CSSSupportsRuleWrapper.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSMediaRule.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/CSSSupportsRule.h>

namespace Web::Bindings {

CSSRuleWrapper* wrap(JS::GlobalObject& global_object, CSS::CSSRule& rule)
{
    if (rule.wrapper())
        return static_cast<CSSRuleWrapper*>(rule.wrapper());

    if (is<CSS::CSSStyleRule>(rule))
        return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, verify_cast<CSS::CSSStyleRule>(rule)));
    if (is<CSS::CSSImportRule>(rule))
        return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, verify_cast<CSS::CSSImportRule>(rule)));
    if (is<CSS::CSSMediaRule>(rule))
        return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, verify_cast<CSS::CSSMediaRule>(rule)));
    if (is<CSS::CSSFontFaceRule>(rule))
        return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, verify_cast<CSS::CSSFontFaceRule>(rule)));
    if (is<CSS::CSSSupportsRule>(rule))
        return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, verify_cast<CSS::CSSSupportsRule>(rule)));
    return static_cast<CSSRuleWrapper*>(wrap_impl(global_object, rule));
}

}
