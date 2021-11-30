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
    bool important { false };
    CSS::PropertyID property_id;
    NonnullRefPtr<StyleValue> value;
    String custom_name {};
};

class CSSStyleDeclaration
    : public RefCounted<CSSStyleDeclaration>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::CSSStyleDeclarationWrapper;

    virtual ~CSSStyleDeclaration();

    virtual size_t length() const = 0;
    virtual String item(size_t index) const = 0;

    virtual Optional<StyleProperty> property(PropertyID) const = 0;
    virtual bool set_property(PropertyID, StringView css_text) = 0;

    void set_property(StringView property_name, StringView css_text);

    String get_property_value(StringView property) const;

    String css_text() const;
    void set_css_text(StringView);

    virtual String serialized() const = 0;

protected:
    CSSStyleDeclaration() { }
};

class PropertyOwningCSSStyleDeclaration : public CSSStyleDeclaration {
    friend class ElementInlineCSSStyleDeclaration;

public:
    static NonnullRefPtr<PropertyOwningCSSStyleDeclaration> create(Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties)
    {
        return adopt_ref(*new PropertyOwningCSSStyleDeclaration(move(properties), move(custom_properties)));
    }

    virtual ~PropertyOwningCSSStyleDeclaration() override;

    virtual size_t length() const override;
    virtual String item(size_t index) const override;

    virtual Optional<StyleProperty> property(PropertyID) const override;
    virtual bool set_property(PropertyID, StringView css_text) override;

    const Vector<StyleProperty>& properties() const { return m_properties; }
    const HashMap<String, StyleProperty>& custom_properties() const { return m_custom_properties; }
    Optional<StyleProperty> custom_property(const String& custom_property_name) const { return m_custom_properties.get(custom_property_name); }
    size_t custom_property_count() const { return m_custom_properties.size(); }

    virtual String serialized() const final override;

protected:
    explicit PropertyOwningCSSStyleDeclaration(Vector<StyleProperty>, HashMap<String, StyleProperty>);

private:
    Vector<StyleProperty> m_properties;
    HashMap<String, StyleProperty> m_custom_properties;
};

class ElementInlineCSSStyleDeclaration final : public PropertyOwningCSSStyleDeclaration {
public:
    static NonnullRefPtr<ElementInlineCSSStyleDeclaration> create(DOM::Element& element) { return adopt_ref(*new ElementInlineCSSStyleDeclaration(element)); }
    static NonnullRefPtr<ElementInlineCSSStyleDeclaration> create_and_take_properties_from(DOM::Element& element, PropertyOwningCSSStyleDeclaration& declaration) { return adopt_ref(*new ElementInlineCSSStyleDeclaration(element, declaration)); }
    virtual ~ElementInlineCSSStyleDeclaration() override;

    DOM::Element* element() { return m_element.ptr(); }
    const DOM::Element* element() const { return m_element.ptr(); }

private:
    explicit ElementInlineCSSStyleDeclaration(DOM::Element&);
    explicit ElementInlineCSSStyleDeclaration(DOM::Element&, PropertyOwningCSSStyleDeclaration&);

    WeakPtr<DOM::Element> m_element;
};

}

namespace Web::Bindings {

CSSStyleDeclarationWrapper* wrap(JS::GlobalObject&, CSS::CSSStyleDeclaration&);

}
