/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

struct StyleProperty {
    CSS::PropertyID property_id;
    NonnullRefPtr<StyleValue> value;
    String custom_name {};
    bool important { false };
};

class CSSStyleDeclaration
    : public RefCounted<CSSStyleDeclaration>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::CSSStyleDeclarationWrapper;

    static NonnullRefPtr<CSSStyleDeclaration> create(Vector<StyleProperty>&& properties, HashMap<String, StyleProperty>&& custom_properties)
    {
        return adopt_ref(*new CSSStyleDeclaration(move(properties), move(custom_properties)));
    }

    virtual ~CSSStyleDeclaration();

    const Vector<StyleProperty>& properties() const { return m_properties; }
    const Optional<StyleProperty> custom_property(const String& custom_property_name) const { return m_custom_properties.get(custom_property_name); }
    size_t custom_property_count() const { return m_custom_properties.size(); };

    size_t length() const { return m_properties.size(); }
    String item(size_t index) const;

protected:
    explicit CSSStyleDeclaration(Vector<StyleProperty>&&, HashMap<String, StyleProperty>&&);

private:
    friend class Bindings::CSSStyleDeclarationWrapper;

    Vector<StyleProperty> m_properties;
    HashMap<String, StyleProperty> m_custom_properties;
};

class ElementInlineCSSStyleDeclaration final : public CSSStyleDeclaration {
public:
    static NonnullRefPtr<ElementInlineCSSStyleDeclaration> create(DOM::Element& element) { return adopt_ref(*new ElementInlineCSSStyleDeclaration(element)); }
    virtual ~ElementInlineCSSStyleDeclaration() override;

    DOM::Element* element() { return m_element.ptr(); }
    const DOM::Element* element() const { return m_element.ptr(); }

private:
    explicit ElementInlineCSSStyleDeclaration(DOM::Element&);

    WeakPtr<DOM::Element> m_element;
};

}

namespace Web::Bindings {

CSSStyleDeclarationWrapper* wrap(JS::GlobalObject&, CSS::CSSStyleDeclaration&);

}
