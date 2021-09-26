/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

PropertyOwningCSSStyleDeclaration::PropertyOwningCSSStyleDeclaration(Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties)
    : m_properties(move(properties))
    , m_custom_properties(move(custom_properties))
{
}

PropertyOwningCSSStyleDeclaration::~PropertyOwningCSSStyleDeclaration()
{
}

CSSStyleDeclaration::~CSSStyleDeclaration()
{
}

String PropertyOwningCSSStyleDeclaration::item(size_t index) const
{
    if (index >= m_properties.size())
        return {};
    return CSS::string_from_property_id(m_properties[index].property_id);
}

ElementInlineCSSStyleDeclaration::ElementInlineCSSStyleDeclaration(DOM::Element& element)
    : PropertyOwningCSSStyleDeclaration({}, {})
    , m_element(element.make_weak_ptr<DOM::Element>())
{
}

ElementInlineCSSStyleDeclaration::ElementInlineCSSStyleDeclaration(DOM::Element& element, PropertyOwningCSSStyleDeclaration& declaration)
    : PropertyOwningCSSStyleDeclaration(move(declaration.m_properties), move(declaration.m_custom_properties))
    , m_element(element.make_weak_ptr<DOM::Element>())
{
}

ElementInlineCSSStyleDeclaration::~ElementInlineCSSStyleDeclaration()
{
}

size_t PropertyOwningCSSStyleDeclaration::length() const
{
    return m_properties.size();
}

Optional<StyleProperty> PropertyOwningCSSStyleDeclaration::property(PropertyID property_id) const
{
    for (auto& property : m_properties) {
        if (property.property_id == property_id)
            return property;
    }
    return {};
}

bool PropertyOwningCSSStyleDeclaration::set_property(PropertyID property_id, StringView css_text)
{
    auto new_value = parse_css_value(CSS::ParsingContext {}, css_text, property_id);
    if (!new_value) {
        m_properties.remove_all_matching([&](auto& entry) {
            return entry.property_id == property_id;
        });
        return false;
    }

    ScopeGuard style_invalidation_guard = [&] {
        auto& declaration = verify_cast<CSS::ElementInlineCSSStyleDeclaration>(*this);
        if (auto* element = declaration.element())
            element->invalidate_style();
    };

    // FIXME: I don't think '!important' is being handled correctly here..

    for (auto& property : m_properties) {
        if (property.property_id == property_id) {
            property.value = new_value.release_nonnull();
            return true;
        }
    }

    m_properties.append(CSS::StyleProperty {
        .property_id = property_id,
        .value = new_value.release_nonnull(),
        .important = false,
    });
    return true;
}

String CSSStyleDeclaration::get_property_value(StringView property_name) const
{
    auto property_id = property_id_from_string(property_name);
    if (property_id == CSS::PropertyID::Invalid)
        return {};
    auto maybe_property = property(property_id);
    if (!maybe_property.has_value())
        return {};
    return maybe_property->value->to_string();
}

void CSSStyleDeclaration::set_property(StringView property_name, StringView css_text)
{
    auto property_id = property_id_from_string(property_name);
    if (property_id == CSS::PropertyID::Invalid)
        return;
    set_property(property_id, css_text);
}

}
