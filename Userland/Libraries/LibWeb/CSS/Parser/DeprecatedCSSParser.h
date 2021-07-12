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
class DeprecatedParsingContext {
public:
    DeprecatedParsingContext();
    explicit DeprecatedParsingContext(const DOM::Document&);
    explicit DeprecatedParsingContext(const DOM::ParentNode&);

    bool in_quirks_mode() const;

    URL complete_url(const String&) const;

private:
    const DOM::Document* m_document { nullptr };
};
}

namespace Web {

RefPtr<CSS::CSSStyleSheet> parse_css(const CSS::DeprecatedParsingContext&, const StringView&);
RefPtr<CSS::CSSStyleDeclaration> parse_css_declaration(const CSS::DeprecatedParsingContext&, const StringView&);
RefPtr<CSS::StyleValue> parse_css_value(const CSS::DeprecatedParsingContext&, const StringView&, CSS::PropertyID property_id = CSS::PropertyID::Invalid);
RefPtr<CSS::Selector> parse_selector(const CSS::DeprecatedParsingContext&, const StringView&);

RefPtr<CSS::LengthStyleValue> parse_line_width(const CSS::DeprecatedParsingContext&, const StringView&);
RefPtr<CSS::ColorStyleValue> parse_color(const CSS::DeprecatedParsingContext&, const StringView&);
RefPtr<CSS::IdentifierStyleValue> parse_line_style(const CSS::DeprecatedParsingContext&, const StringView&);

RefPtr<CSS::StyleValue> parse_html_length(const DOM::Document&, const StringView&);

}
