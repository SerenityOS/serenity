/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSStyleDeclaration.h>

namespace Web::CSS {

class ResolvedCSSStyleDeclaration final : public CSSStyleDeclaration {
    JS_OBJECT(ResolvedCSSStyleDeclaration, CSSStyleDeclaration);

public:
    static ResolvedCSSStyleDeclaration* create(DOM::Element& element);
    explicit ResolvedCSSStyleDeclaration(DOM::Element&);

    virtual ~ResolvedCSSStyleDeclaration() override = default;

    virtual size_t length() const override;
    virtual String item(size_t index) const override;
    virtual Optional<StyleProperty> property(PropertyID) const override;
    virtual DOM::ExceptionOr<void> set_property(PropertyID, StringView css_text, StringView priority) override;
    virtual DOM::ExceptionOr<String> remove_property(PropertyID) override;

    virtual String serialized() const override;

private:
    RefPtr<StyleValue> style_value_for_property(Layout::NodeWithStyle const&, PropertyID) const;

    NonnullRefPtr<DOM::Element> m_element;
};

}
