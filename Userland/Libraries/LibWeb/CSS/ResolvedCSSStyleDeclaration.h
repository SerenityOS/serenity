/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSStyleDeclaration.h>

namespace Web::CSS {

class ResolvedCSSStyleDeclaration final : public CSSStyleDeclaration {
    WEB_PLATFORM_OBJECT(ResolvedCSSStyleDeclaration, CSSStyleDeclaration);
    JS_DECLARE_ALLOCATOR(ResolvedCSSStyleDeclaration);

public:
    [[nodiscard]] static JS::NonnullGCPtr<ResolvedCSSStyleDeclaration> create(DOM::Element&);

    virtual ~ResolvedCSSStyleDeclaration() override = default;

    virtual size_t length() const override;
    virtual String item(size_t index) const override;
    virtual Optional<StyleProperty> property(PropertyID) const override;
    virtual WebIDL::ExceptionOr<void> set_property(PropertyID, StringView css_text, StringView priority) override;
    virtual WebIDL::ExceptionOr<String> remove_property(PropertyID) override;

    virtual String serialized() const override;
    virtual WebIDL::ExceptionOr<void> set_css_text(StringView) override;

private:
    explicit ResolvedCSSStyleDeclaration(DOM::Element&);

    virtual void visit_edges(Cell::Visitor&) override;

    RefPtr<StyleValue const> style_value_for_property(Layout::NodeWithStyle const&, PropertyID) const;

    JS::NonnullGCPtr<DOM::Element> m_element;
};

}
