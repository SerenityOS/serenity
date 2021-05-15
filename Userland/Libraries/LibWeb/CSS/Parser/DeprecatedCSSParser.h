/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibWeb/CSS/CSSStyleSheet.h>

namespace Web::CSS {
class ParsingContext {
public:
    ParsingContext();
    explicit ParsingContext(const DOM::Document&);
    explicit ParsingContext(const DOM::ParentNode&);

    bool in_quirks_mode() const;

    URL complete_url(const String&) const;

private:
    const DOM::Document* m_document { nullptr };
};
}

namespace Web {

RefPtr<CSS::CSSStyleSheet> parse_css(const CSS::ParsingContext&, const StringView&);
RefPtr<CSS::CSSStyleDeclaration> parse_css_declaration(const CSS::ParsingContext&, const StringView&);
RefPtr<CSS::StyleValue> parse_css_value(const CSS::ParsingContext&, const StringView&, CSS::PropertyID property_id = CSS::PropertyID::Invalid);
Optional<CSS::Selector> parse_selector(const CSS::ParsingContext&, const StringView&);

RefPtr<CSS::LengthStyleValue> parse_line_width(const CSS::ParsingContext&, const StringView&);
RefPtr<CSS::ColorStyleValue> parse_color(const CSS::ParsingContext&, const StringView&);
RefPtr<CSS::IdentifierStyleValue> parse_line_style(const CSS::ParsingContext&, const StringView&);

RefPtr<CSS::StyleValue> parse_html_length(const DOM::Document&, const StringView&);

}
