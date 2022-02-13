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
        .important = Important::No,
        .property_id = property_id,
        .value = new_value.release_nonnull(),
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

String CSSStyleDeclaration::css_text() const
{
    TODO();
    return "";
}

void CSSStyleDeclaration::set_css_text(StringView)
{
    TODO();
}

// https://www.w3.org/TR/cssom/#serialize-a-css-declaration
static String serialize_a_css_declaration(CSS::PropertyID property, String value, Important important)
{
    StringBuilder builder;

    // 1. Let s be the empty string.
    // 2. Append property to s.
    builder.append(string_from_property_id(property));

    // 3. Append ": " (U+003A U+0020) to s.
    builder.append(": "sv);

    // 4. Append value to s.
    builder.append(value);

    // 5. If the important flag is set, append " !important" (U+0020 U+0021 U+0069 U+006D U+0070 U+006F U+0072 U+0074 U+0061 U+006E U+0074) to s.
    if (important == Important::Yes)
        builder.append(" !important"sv);

    // 6. Append ";" (U+003B) to s.
    builder.append(';');

    // 7. Return s.
    return builder.to_string();
}

// https://www.w3.org/TR/cssom/#serialize-a-css-declaration-block
String PropertyOwningCSSStyleDeclaration::serialized() const
{
    // 1. Let list be an empty array.
    Vector<String> list;

    // 2. Let already serialized be an empty array.
    HashTable<PropertyID> already_serialized;

    // 3. Declaration loop: For each CSS declaration declaration in declaration block’s declarations, follow these substeps:
    for (auto& declaration : m_properties) {
        // 1. Let property be declaration’s property name.
        auto property = declaration.property_id;

        // 2. If property is in already serialized, continue with the steps labeled declaration loop.
        if (already_serialized.contains(property))
            continue;

        // FIXME: 3. If property maps to one or more shorthand properties, let shorthands be an array of those shorthand properties, in preferred order.

        // FIXME: 4. Shorthand loop: For each shorthand in shorthands, follow these substeps: ...

        // 5. Let value be the result of invoking serialize a CSS value of declaration.
        auto value = declaration.value->to_string();

        // 6. Let serialized declaration be the result of invoking serialize a CSS declaration with property name property, value value,
        //    and the important flag set if declaration has its important flag set.
        auto serialized_declaration = serialize_a_css_declaration(property, move(value), declaration.important);

        // 7. Append serialized declaration to list.
        list.append(move(serialized_declaration));

        // 8. Append property to already serialized.
        already_serialized.set(property);
    }

    // 4. Return list joined with " " (U+0020).
    StringBuilder builder;
    builder.join(' ', list);
    return builder.to_string();
}

}
