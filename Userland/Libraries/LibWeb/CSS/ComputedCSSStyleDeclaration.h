/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSStyleDeclaration.h>

namespace Web::CSS {

class ComputedCSSStyleDeclaration final : public CSSStyleDeclaration {
public:
    static NonnullRefPtr<ComputedCSSStyleDeclaration> create(DOM::Element& element)
    {
        return adopt_ref(*new ComputedCSSStyleDeclaration(element));
    }

    virtual ~ComputedCSSStyleDeclaration() override;

    virtual size_t length() const override;
    virtual String item(size_t index) const override;
    virtual Optional<StyleProperty> property(PropertyID) const override;
    virtual bool set_property(PropertyID, StringView css_text) override;

private:
    explicit ComputedCSSStyleDeclaration(DOM::Element&);

    NonnullRefPtr<DOM::Element> m_element;
};

}
