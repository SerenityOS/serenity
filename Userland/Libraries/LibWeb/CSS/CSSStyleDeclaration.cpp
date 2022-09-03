/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

CSSStyleDeclaration::CSSStyleDeclaration(HTML::Window& window_object)
    : PlatformObject(window_object.cached_web_prototype("CSSStyleDeclaration"))
{
}

PropertyOwningCSSStyleDeclaration* PropertyOwningCSSStyleDeclaration::create(HTML::Window& window_object, Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties)
{
    return window_object.heap().allocate<PropertyOwningCSSStyleDeclaration>(window_object.realm(), window_object, move(properties), move(custom_properties));
}

PropertyOwningCSSStyleDeclaration::PropertyOwningCSSStyleDeclaration(HTML::Window& window_object, Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties)
    : CSSStyleDeclaration(window_object)
    , m_properties(move(properties))
    , m_custom_properties(move(custom_properties))
{
}

String PropertyOwningCSSStyleDeclaration::item(size_t index) const
{
    if (index >= m_properties.size())
        return {};
    return CSS::string_from_property_id(m_properties[index].property_id);
}

ElementInlineCSSStyleDeclaration* ElementInlineCSSStyleDeclaration::create(DOM::Element& element, Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties)
{
    auto& window_object = element.document().window();
    return window_object.heap().allocate<ElementInlineCSSStyleDeclaration>(window_object.realm(), element, move(properties), move(custom_properties));
}

ElementInlineCSSStyleDeclaration::ElementInlineCSSStyleDeclaration(DOM::Element& element, Vector<StyleProperty> properties, HashMap<String, StyleProperty> custom_properties)
    : PropertyOwningCSSStyleDeclaration(element.document().window(), move(properties), move(custom_properties))
    , m_element(element.make_weak_ptr<DOM::Element>())
{
}

void ElementInlineCSSStyleDeclaration::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_element.ptr());
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

// https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-setproperty
DOM::ExceptionOr<void> PropertyOwningCSSStyleDeclaration::set_property(PropertyID property_id, StringView value, StringView priority)
{
    // 1. If the computed flag is set, then throw a NoModificationAllowedError exception.
    // NOTE: This is handled by the virtual override in ResolvedCSSStyleDeclaration.

    // FIXME: 2. If property is not a custom property, follow these substeps:
    // FIXME:    1. Let property be property converted to ASCII lowercase.
    // FIXME:    2. If property is not a case-sensitive match for a supported CSS property, then return.
    // NOTE: This must be handled before we've turned the property string into a PropertyID.

    // 3. If value is the empty string, invoke removeProperty() with property as argument and return.
    if (value.is_empty()) {
        MUST(remove_property(property_id));
        return {};
    }

    // 4. If priority is not the empty string and is not an ASCII case-insensitive match for the string "important", then return.
    if (!priority.is_empty() && !priority.equals_ignoring_case("important"sv))
        return {};

    // 5. Let component value list be the result of parsing value for property property.
    auto component_value_list = parse_css_value(CSS::Parser::ParsingContext {}, value, property_id);

    // 6. If component value list is null, then return.
    if (!component_value_list)
        return {};

    // 7. Let updated be false.
    bool updated = false;

    // FIXME: 8. If property is a shorthand property, then for each longhand property longhand that property maps to, in canonical order, follow these substeps:
    // FIXME:    1. Let longhand result be the result of set the CSS declaration longhand with the appropriate value(s) from component value list,
    //              with the important flag set if priority is not the empty string, and unset otherwise, and with the list of declarations being the declarations.
    // FIXME:    2. If longhand result is true, let updated be true.

    // 9. Otherwise, let updated be the result of set the CSS declaration property with value component value list,
    //    with the important flag set if priority is not the empty string, and unset otherwise,
    //    and with the list of declarations being the declarations.
    updated = set_a_css_declaration(property_id, component_value_list.release_nonnull(), !priority.is_empty() ? Important::Yes : Important::No);

    // 10. If updated is true, update style attribute for the CSS declaration block.
    if (updated)
        update_style_attribute();

    return {};
}

// https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-removeproperty
DOM::ExceptionOr<String> PropertyOwningCSSStyleDeclaration::remove_property(PropertyID property_id)
{
    // 1. If the computed flag is set, then throw a NoModificationAllowedError exception.
    // NOTE: This is handled by the virtual override in ResolvedCSSStyleDeclaration.

    // 2. If property is not a custom property, let property be property converted to ASCII lowercase.
    // NOTE: We've already converted it to a PropertyID enum value.

    // 3. Let value be the return value of invoking getPropertyValue() with property as argument.
    // FIXME: The trip through string_from_property_id() here is silly.
    auto value = get_property_value(string_from_property_id(property_id));

    // 4. Let removed be false.
    bool removed = false;

    // FIXME: 5. If property is a shorthand property, for each longhand property longhand that property maps to:
    //           1. If longhand is not a property name of a CSS declaration in the declarations, continue.
    //           2. Remove that CSS declaration and let removed be true.

    // 6. Otherwise, if property is a case-sensitive match for a property name of a CSS declaration in the declarations, remove that CSS declaration and let removed be true.
    removed = m_properties.remove_first_matching([&](auto& entry) { return entry.property_id == property_id; });

    // 7. If removed is true, Update style attribute for the CSS declaration block.
    if (removed)
        update_style_attribute();

    // 8. Return value.
    return value;
}

// https://drafts.csswg.org/cssom/#update-style-attribute-for
void ElementInlineCSSStyleDeclaration::update_style_attribute()
{
    // 1. Assert: declaration block’s computed flag is unset.
    // NOTE: Unnecessary, only relevant for ResolvedCSSStyleDeclaration.

    // 2. Let owner node be declaration block’s owner node.
    // 3. If owner node is null, then return.
    if (!m_element)
        return;

    // 4. Set declaration block’s updating flag.
    m_updating = true;

    // 5. Set an attribute value for owner node using "style" and the result of serializing declaration block.
    m_element->set_attribute(HTML::AttributeNames::style, serialized());

    // 6. Unset declaration block’s updating flag.
    m_updating = false;
}

// https://drafts.csswg.org/cssom/#set-a-css-declaration
bool PropertyOwningCSSStyleDeclaration::set_a_css_declaration(PropertyID property_id, NonnullRefPtr<StyleValue> value, Important important)
{
    // FIXME: Handle logical property groups.

    for (auto& property : m_properties) {
        if (property.property_id == property_id) {
            if (property.important == important && *property.value == *value)
                return false;
            property.value = move(value);
            property.important = important;
            return true;
        }
    }

    m_properties.append(CSS::StyleProperty {
        .important = important,
        .property_id = property_id,
        .value = move(value),
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

// https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-getpropertypriority
String CSSStyleDeclaration::get_property_priority(StringView property_name) const
{
    auto property_id = property_id_from_string(property_name);
    if (property_id == CSS::PropertyID::Invalid)
        return {};
    auto maybe_property = property(property_id);
    if (!maybe_property.has_value())
        return {};
    return maybe_property->important == Important::Yes ? "important" : "";
}

DOM::ExceptionOr<void> CSSStyleDeclaration::set_property(StringView property_name, StringView css_text, StringView priority)
{
    auto property_id = property_id_from_string(property_name);
    if (property_id == CSS::PropertyID::Invalid)
        return {};
    return set_property(property_id, css_text, priority);
}

DOM::ExceptionOr<String> CSSStyleDeclaration::remove_property(StringView property_name)
{
    auto property_id = property_id_from_string(property_name);
    if (property_id == CSS::PropertyID::Invalid)
        return String::empty();
    return remove_property(property_id);
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

static CSS::PropertyID property_id_from_name(StringView name)
{
    // FIXME: Perhaps this should go in the code generator.
    if (name == "cssFloat"sv)
        return CSS::PropertyID::Float;

    if (auto property_id = CSS::property_id_from_camel_case_string(name); property_id != CSS::PropertyID::Invalid)
        return property_id;

    if (auto property_id = CSS::property_id_from_string(name); property_id != CSS::PropertyID::Invalid)
        return property_id;

    return CSS::PropertyID::Invalid;
}

JS::ThrowCompletionOr<bool> CSSStyleDeclaration::internal_has_property(JS::PropertyKey const& name) const
{
    if (!name.is_string())
        return Base::internal_has_property(name);
    return property_id_from_name(name.to_string()) != CSS::PropertyID::Invalid;
}

JS::ThrowCompletionOr<JS::Value> CSSStyleDeclaration::internal_get(JS::PropertyKey const& name, JS::Value receiver) const
{
    if (!name.is_string())
        return Base::internal_get(name, receiver);
    auto property_id = property_id_from_name(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::internal_get(name, receiver);
    if (auto maybe_property = property(property_id); maybe_property.has_value())
        return { js_string(vm(), maybe_property->value->to_string()) };
    return { js_string(vm(), String::empty()) };
}

JS::ThrowCompletionOr<bool> CSSStyleDeclaration::internal_set(JS::PropertyKey const& name, JS::Value value, JS::Value receiver)
{
    if (!name.is_string())
        return Base::internal_set(name, value, receiver);
    auto property_id = property_id_from_name(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::internal_set(name, value, receiver);

    auto css_text = TRY(value.to_string(vm()));

    impl().set_property(property_id, css_text);
    return true;
}

}
