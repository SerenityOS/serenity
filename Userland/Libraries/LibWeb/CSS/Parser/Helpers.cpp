/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2023, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSMediaRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web {

CSS::CSSStyleSheet* parse_css_stylesheet(CSS::Parser::ParsingContext const& context, StringView css, Optional<AK::URL> location)
{
    if (css.is_empty()) {
        auto rule_list = CSS::CSSRuleList::create_empty(context.realm());
        auto media_list = CSS::MediaList::create(context.realm(), {});
        return CSS::CSSStyleSheet::create(context.realm(), rule_list, media_list, location);
    }
    auto parser = CSS::Parser::Parser::create(context, css).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_css_stylesheet(location);
}

CSS::ElementInlineCSSStyleDeclaration* parse_css_style_attribute(CSS::Parser::ParsingContext const& context, StringView css, DOM::Element& element)
{
    if (css.is_empty())
        return CSS::ElementInlineCSSStyleDeclaration::create(element, {}, {});
    auto parser = CSS::Parser::Parser::create(context, css).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_style_attribute(element);
}

RefPtr<CSS::StyleValue> parse_css_value(CSS::Parser::ParsingContext const& context, StringView string, CSS::PropertyID property_id)
{
    if (string.is_empty())
        return nullptr;
    auto parser = MUST(CSS::Parser::Parser::create(context, string));
    return parser.parse_as_css_value(property_id);
}

CSS::CSSRule* parse_css_rule(CSS::Parser::ParsingContext const& context, StringView css_text)
{
    auto parser = CSS::Parser::Parser::create(context, css_text).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_css_rule();
}

Optional<CSS::SelectorList> parse_selector(CSS::Parser::ParsingContext const& context, StringView selector_text)
{
    auto parser = CSS::Parser::Parser::create(context, selector_text).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_selector();
}

RefPtr<CSS::MediaQuery> parse_media_query(CSS::Parser::ParsingContext const& context, StringView string)
{
    auto parser = CSS::Parser::Parser::create(context, string).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_media_query();
}

Vector<NonnullRefPtr<CSS::MediaQuery>> parse_media_query_list(CSS::Parser::ParsingContext const& context, StringView string)
{
    auto parser = CSS::Parser::Parser::create(context, string).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_media_query_list();
}

RefPtr<CSS::Supports> parse_css_supports(CSS::Parser::ParsingContext const& context, StringView string)
{
    if (string.is_empty())
        return {};
    auto parser = CSS::Parser::Parser::create(context, string).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_supports();
}

Optional<CSS::StyleProperty> parse_css_supports_condition(CSS::Parser::ParsingContext const& context, StringView string)
{
    if (string.is_empty())
        return {};
    auto parser = CSS::Parser::Parser::create(context, string).release_value_but_fixme_should_propagate_errors();
    return parser.parse_as_supports_condition();
}

}
