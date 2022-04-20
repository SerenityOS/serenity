/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/DOMStringMap.h>

namespace Web::HTML {

DOMStringMap::DOMStringMap(DOM::Element& associated_element)
    : m_associated_element(associated_element)
{
}

DOMStringMap::~DOMStringMap() = default;

// https://html.spec.whatwg.org/multipage/dom.html#concept-domstringmap-pairs
Vector<DOMStringMap::NameValuePair> DOMStringMap::get_name_value_pairs() const
{
    // 1. Let list be an empty list of name-value pairs.
    Vector<NameValuePair> list;

    // 2. For each content attribute on the DOMStringMap's associated element whose first five characters are the string "data-" and whose remaining characters (if any) do not include any ASCII upper alphas,
    //    in the order that those attributes are listed in the element's attribute list, add a name-value pair to list whose name is the attribute's name with the first five characters removed and whose value
    //    is the attribute's value.
    m_associated_element->for_each_attribute([&](auto& name, auto& value) {
        if (!name.starts_with("data-"))
            return;

        auto name_after_starting_data = name.view().substring_view(5);

        for (auto character : name_after_starting_data) {
            if (is_ascii_upper_alpha(character))
                return;
        }

        // 3. For each name in list, for each U+002D HYPHEN-MINUS character (-) in the name that is followed by an ASCII lower alpha, remove the U+002D HYPHEN-MINUS character (-) and replace the character
        //    that followed it by the same character converted to ASCII uppercase.
        StringBuilder builder;
        for (size_t character_index = 0; character_index < name_after_starting_data.length(); ++character_index) {
            auto current_character = name_after_starting_data[character_index];

            if (character_index + 1 < name_after_starting_data.length() && current_character == '-') {
                auto next_character = name_after_starting_data[character_index + 1];

                if (is_ascii_lower_alpha(next_character)) {
                    builder.append(to_ascii_uppercase(next_character));

                    // Skip the next character
                    ++character_index;

                    continue;
                }
            }

            builder.append(current_character);
        }

        list.append({ builder.to_string(), value });
    });

    // 4. Return list.
    return list;
}

// https://html.spec.whatwg.org/multipage/dom.html#concept-domstringmap-pairs
// NOTE: There isn't a direct link to this, so the link is to one of the algorithms above it.
Vector<String> DOMStringMap::supported_property_names() const
{
    // The supported property names on a DOMStringMap object at any instant are the names of each pair returned from getting the DOMStringMap's name-value pairs at that instant, in the order returned.
    Vector<String> names;
    auto name_value_pairs = get_name_value_pairs();
    for (auto& name_value_pair : name_value_pairs) {
        names.append(name_value_pair.name);
    }
    return names;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-domstringmap-nameditem
String DOMStringMap::determine_value_of_named_property(String const& name) const
{
    // To determine the value of a named property name for a DOMStringMap, return the value component of the name-value pair whose name component is name in the list returned from getting the
    // DOMStringMap's name-value pairs.
    auto name_value_pairs = get_name_value_pairs();
    auto optional_value = name_value_pairs.first_matching([&name](NameValuePair& name_value_pair) {
        return name_value_pair.name == name;
    });

    // NOTE: determine_value_of_named_property is only called if `name` is in supported_property_names.
    VERIFY(optional_value.has_value());

    return optional_value->value;
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-domstringmap-setitem
DOM::ExceptionOr<void> DOMStringMap::set_value_of_new_named_property(String const& name, String const& value)
{
    AK::StringBuilder builder;

    // 3. Insert the string data- at the front of name.
    // NOTE: This is done out of order because StringBuilder doesn't have prepend.
    builder.append("data-");

    for (size_t character_index = 0; character_index < name.length(); ++character_index) {
        // 1. If name contains a U+002D HYPHEN-MINUS character (-) followed by an ASCII lower alpha, then throw a "SyntaxError" DOMException.
        auto current_character = name[character_index];

        if (current_character == '-' && character_index + 1 < name.length()) {
            auto next_character = name[character_index + 1];
            if (is_ascii_lower_alpha(next_character))
                return DOM::SyntaxError::create("Name cannot contain a '-' followed by a lowercase character.");
        }

        // 2. For each ASCII upper alpha in name, insert a U+002D HYPHEN-MINUS character (-) before the character and replace the character with the same character converted to ASCII lowercase.
        if (is_ascii_upper_alpha(current_character)) {
            builder.append('-');
            builder.append(to_ascii_lowercase(current_character));
            continue;
        }

        builder.append(current_character);
    }

    auto data_name = builder.to_string();

    // FIXME: 4. If name does not match the XML Name production, throw an "InvalidCharacterError" DOMException.

    // 5. Set an attribute value for the DOMStringMap's associated element using name and value.
    m_associated_element->set_attribute(data_name, value);

    return {};
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-domstringmap-setitem
DOM::ExceptionOr<void> DOMStringMap::set_value_of_existing_named_property(String const& name, String const& value)
{
    return set_value_of_new_named_property(name, value);
}

// https://html.spec.whatwg.org/multipage/dom.html#dom-domstringmap-removeitem
bool DOMStringMap::delete_existing_named_property(String const& name)
{
    AK::StringBuilder builder;

    // 2. Insert the string data- at the front of name.
    // NOTE: This is done out of order because StringBuilder doesn't have prepend.
    builder.append("data-");

    for (auto character : name) {
        // 1. For each ASCII upper alpha in name, insert a U+002D HYPHEN-MINUS character (-) before the character and replace the character with the same character converted to ASCII lowercase.
        if (is_ascii_upper_alpha(character)) {
            builder.append('-');
            builder.append(to_ascii_lowercase(character));
            continue;
        }

        builder.append(character);
    }

    // Remove an attribute by name given name and the DOMStringMap's associated element.
    auto data_name = builder.to_string();
    m_associated_element->remove_attribute(data_name);

    // The spec doesn't have the step. This indicates that the deletion was successful.
    return true;
}

}
