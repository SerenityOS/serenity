/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Attribute.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/NamedNodeMap.h>
#include <LibWeb/Namespace.h>

namespace Web::DOM {

NonnullRefPtr<NamedNodeMap> NamedNodeMap::create(Element& associated_element)
{
    return adopt_ref(*new NamedNodeMap(associated_element));
}

NamedNodeMap::NamedNodeMap(Element& associated_element)
    : RefCountForwarder(associated_element)
{
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-indices%E2%91%A3
bool NamedNodeMap::is_supported_property_index(u32 index) const
{
    return index < m_attributes.size();
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-names%E2%91%A0
Vector<String> NamedNodeMap::supported_property_names() const
{
    // 1. Let names be the qualified names of the attributes in this NamedNodeMap object’s attribute list, with duplicates omitted, in order.
    Vector<String> names;
    names.ensure_capacity(m_attributes.size());

    for (auto const& attribute : m_attributes) {
        if (!names.contains_slow(attribute.name()))
            names.append(attribute.name());
    }

    // 2. If this NamedNodeMap object’s element is in the HTML namespace and its node document is an HTML document, then for each name in names:
    // FIXME: Handle the second condition, assume it is an HTML document for now.
    if (associated_element().namespace_uri() == Namespace::HTML) {
        // 1. Let lowercaseName be name, in ASCII lowercase.
        // 2. If lowercaseName is not equal to name, remove name from names.
        names.remove_all_matching([](auto const& name) { return name != name.to_lowercase(); });
    }

    // 3. Return names.
    return names;
}

// https://dom.spec.whatwg.org/#dom-namednodemap-item
Attribute const* NamedNodeMap::item(u32 index) const
{
    // 1. If index is equal to or greater than this’s attribute list’s size, then return null.
    if (index >= m_attributes.size())
        return nullptr;

    // 2. Otherwise, return this’s attribute list[index].
    return &m_attributes[index];
}

// https://dom.spec.whatwg.org/#dom-namednodemap-getnameditem
Attribute const* NamedNodeMap::get_named_item(StringView qualified_name) const
{
    return get_attribute(qualified_name);
}

// https://dom.spec.whatwg.org/#dom-namednodemap-setnameditem
ExceptionOr<Attribute const*> NamedNodeMap::set_named_item(Attribute& attribute)
{
    return set_attribute(attribute);
}

// https://dom.spec.whatwg.org/#dom-namednodemap-removenameditem
ExceptionOr<Attribute const*> NamedNodeMap::remove_named_item(StringView qualified_name)
{
    // 1. Let attr be the result of removing an attribute given qualifiedName and element.
    auto const* attribute = remove_attribute(qualified_name);

    // 2. If attr is null, then throw a "NotFoundError" DOMException.
    if (!attribute)
        return NotFoundError::create(String::formatted("Attribute with name '{}' not found", qualified_name));

    // 3. Return attr.
    return nullptr;
}

// https://dom.spec.whatwg.org/#concept-element-attributes-get-by-name
Attribute* NamedNodeMap::get_attribute(StringView qualified_name, size_t* item_index)
{
    return const_cast<Attribute*>(const_cast<NamedNodeMap const*>(this)->get_attribute(qualified_name, item_index));
}

// https://dom.spec.whatwg.org/#concept-element-attributes-get-by-name
Attribute const* NamedNodeMap::get_attribute(StringView qualified_name, size_t* item_index) const
{
    if (item_index)
        *item_index = 0;

    // 1. If element is in the HTML namespace and its node document is an HTML document, then set qualifiedName to qualifiedName in ASCII lowercase.
    // FIXME: Handle the second condition, assume it is an HTML document for now.
    bool compare_as_lowercase = associated_element().namespace_uri() == Namespace::HTML;

    // 2. Return the first attribute in element’s attribute list whose qualified name is qualifiedName; otherwise null.
    for (auto const& attribute : m_attributes) {
        if (compare_as_lowercase) {
            if (attribute.name().equals_ignoring_case(qualified_name))
                return &attribute;
        } else {
            if (attribute.name() == qualified_name)
                return &attribute;
        }

        if (item_index)
            ++(*item_index);
    }

    return nullptr;
}

// https://dom.spec.whatwg.org/#concept-element-attributes-set
ExceptionOr<Attribute const*> NamedNodeMap::set_attribute(Attribute& attribute)
{
    // 1. If attr’s element is neither null nor element, throw an "InUseAttributeError" DOMException.
    if ((attribute.owner_element() != nullptr) && (attribute.owner_element() != &associated_element()))
        return InUseAttributeError::create("Attribute must not already be in use"sv);

    // 2. Let oldAttr be the result of getting an attribute given attr’s namespace, attr’s local name, and element.
    // FIXME: When getNamedItemNS is implemented, use that instead.
    size_t old_attribute_index = 0;
    auto* old_attribute = get_attribute(attribute.local_name(), &old_attribute_index);

    // 3. If oldAttr is attr, return attr.
    if (old_attribute == &attribute)
        return &attribute;

    // 4. If oldAttr is non-null, then replace oldAttr with attr.
    if (old_attribute) {
        replace_attribute(*old_attribute, attribute, old_attribute_index);
    }
    // 5. Otherwise, append attr to element.
    else {
        append_attribute(attribute);
    }

    // 6. Return oldAttr.
    return old_attribute;
}

// https://dom.spec.whatwg.org/#concept-element-attributes-replace
void NamedNodeMap::replace_attribute(Attribute& old_attribute, Attribute& new_attribute, size_t old_attribute_index)
{
    // 1. Handle attribute changes for oldAttr with oldAttr’s element, oldAttr’s value, and newAttr’s value.
    // FIXME: The steps to handle an attribute change deal with mutation records and custom element states.
    //        Once those are supported, implement these steps: https://dom.spec.whatwg.org/#handle-attribute-changes

    // 2. Replace oldAttr by newAttr in oldAttr’s element’s attribute list.
    m_attributes.remove(old_attribute_index);
    m_attributes.insert(old_attribute_index, new_attribute);

    // 3. Set newAttr’s element to oldAttr’s element.
    new_attribute.set_owner_element(old_attribute.owner_element());

    // 4 .Set oldAttr’s element to null.
    old_attribute.set_owner_element(nullptr);
}

// https://dom.spec.whatwg.org/#concept-element-attributes-append
void NamedNodeMap::append_attribute(Attribute& attribute)
{
    // 1. Handle attribute changes for attribute with element, null, and attribute’s value.
    // FIXME: The steps to handle an attribute change deal with mutation records and custom element states.
    //        Once those are supported, implement these steps: https://dom.spec.whatwg.org/#handle-attribute-changes

    // 2. Append attribute to element’s attribute list.
    m_attributes.append(attribute);

    // 3. Set attribute’s element to element.
    attribute.set_owner_element(&associated_element());
}

// https://dom.spec.whatwg.org/#concept-element-attributes-remove-by-name
Attribute const* NamedNodeMap::remove_attribute(StringView qualified_name)
{
    size_t item_index = 0;

    // 1. Let attr be the result of getting an attribute given qualifiedName and element.
    auto const* attribute = get_attribute(qualified_name, &item_index);

    // 2. If attr is non-null, then remove attr.
    if (attribute)
        m_attributes.remove(item_index);

    // 3. Return attr.
    return attribute;
}

}
