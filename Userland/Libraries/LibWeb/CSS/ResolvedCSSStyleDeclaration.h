/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSStyleDeclaration.h>

namespace Web::CSS {

class ResolvedCSSStyleDeclaration final : public CSSStyleDeclaration {
public:
    static NonnullRefPtr<ResolvedCSSStyleDeclaration> create(DOM::Element& element)
    {
        return adopt_ref(*new ResolvedCSSStyleDeclaration(element));
    }

    virtual ~ResolvedCSSStyleDeclaration() override;

    virtual size_t length() const override;
    virtual String item(size_t index) const override;
    virtual Optional<StyleProperty> property(PropertyID) const override;
    virtual bool set_property(PropertyID, StringView css_text) override;

    virtual String serialized() const override;

private:
    explicit ResolvedCSSStyleDeclaration(DOM::Element&);

    RefPtr<StyleValue> style_value_for_property(Layout::NodeWithStyle const&, PropertyID) const;

    NonnullRefPtr<DOM::Element> m_element;
};

}
