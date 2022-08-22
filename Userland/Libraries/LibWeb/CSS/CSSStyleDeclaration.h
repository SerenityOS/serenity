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

enum class Important {
    No,
    Yes,
};

struct StyleProperty {
    Important important { Important::No };
    CSS::PropertyID property_id;
    NonnullRefPtr<StyleValue> value;
    String custom_name {};
};

class CSSStyleDeclaration
    : public RefCounted<CSSStyleDeclaration>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::CSSStyleDeclarationWrapper;

    virtual ~CSSStyleDeclaration() = default;

    virtual size_t length() const = 0;
    virtual String item(size_t index) const = 0;

    virtual Optional<StyleProperty> property(PropertyID) const = 0;

    virtual DOM::ExceptionOr<void> set_property(PropertyID, StringView css_text, StringView priority = ""sv) = 0;
    virtual DOM::ExceptionOr<String> remove_property(PropertyID) = 0;

    DOM::ExceptionOr<void> set_property(StringView property_name, StringView css_text, StringView priority);
    DOM::ExceptionOr<String> remove_property(StringView property_name);

    String get_property_value(StringView property) const;
    String get_property_priority(StringView property) const;

    String css_text() const;
    void set_css_text(StringView);

    virtual String serialized() const = 0;

protected:
    CSSStyleDeclaration() = default;
};

class PropertyOwningCSSStyleDeclaration : public CSSStyleDeclaration {
    friend class ElementInlineCSSStyleDeclaration;

public:
    static NonnullRefPtr<PropertyOwningCSSStyleDeclaration> create(Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties)
    {
        return adopt_ref(*new PropertyOwningCSSStyleDeclaration(move(properties), move(custom_properties)));
    }

    virtual ~PropertyOwningCSSStyleDeclaration() override = default;

    virtual size_t length() const override;
    virtual String item(size_t index) const override;

    virtual Optional<StyleProperty> property(PropertyID) const override;

    virtual DOM::ExceptionOr<void> set_property(PropertyID, StringView css_text, StringView priority) override;
    virtual DOM::ExceptionOr<String> remove_property(PropertyID) override;

    Vector<StyleProperty> const& properties() const { return m_properties; }
    HashMap<String, StyleProperty> const& custom_properties() const { return m_custom_properties; }
    Optional<StyleProperty> custom_property(String const& custom_property_name) const { return m_custom_properties.get(custom_property_name); }
    size_t custom_property_count() const { return m_custom_properties.size(); }

    virtual String serialized() const final override;

protected:
    explicit PropertyOwningCSSStyleDeclaration(Vector<StyleProperty>, HashMap<String, StyleProperty>);

    virtual void update_style_attribute() { }

private:
    bool set_a_css_declaration(PropertyID, NonnullRefPtr<StyleValue>, Important);

    Vector<StyleProperty> m_properties;
    HashMap<String, StyleProperty> m_custom_properties;
};

class ElementInlineCSSStyleDeclaration final : public PropertyOwningCSSStyleDeclaration {
public:
    static NonnullRefPtr<ElementInlineCSSStyleDeclaration> create(DOM::Element& element, Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties) { return adopt_ref(*new ElementInlineCSSStyleDeclaration(element, move(properties), move(custom_properties))); }
    virtual ~ElementInlineCSSStyleDeclaration() override = default;

    DOM::Element* element() { return m_element.ptr(); }
    const DOM::Element* element() const { return m_element.ptr(); }

    bool is_updating() const { return m_updating; }

private:
    explicit ElementInlineCSSStyleDeclaration(DOM::Element&, Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties);

    virtual void update_style_attribute() override;

    WeakPtr<DOM::Element> m_element;

    // https://drafts.csswg.org/cssom/#cssstyledeclaration-updating-flag
    bool m_updating { false };
};

}

namespace Web::Bindings {

CSSStyleDeclarationWrapper* wrap(JS::Realm&, CSS::CSSStyleDeclaration&);

}
