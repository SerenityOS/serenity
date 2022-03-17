/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/Namespace.h>

namespace Web::DOM {

HTMLCollection::HTMLCollection(ParentNode& root, Function<bool(Element const&)> filter)
    : m_root(root)
    , m_filter(move(filter))
{
}

HTMLCollection::~HTMLCollection() = default;

Vector<NonnullRefPtr<Element>> HTMLCollection::collect_matching_elements() const
{
    Vector<NonnullRefPtr<Element>> elements;
    m_root->for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
        if (m_filter(element))
            elements.append(element);
        return IterationDecision::Continue;
    });
    return elements;
}

// https://dom.spec.whatwg.org/#dom-htmlcollection-length
size_t HTMLCollection::length()
{
    // The length getter steps are to return the number of nodes represented by the collection.
    return collect_matching_elements().size();
}

// https://dom.spec.whatwg.org/#dom-htmlcollection-item
Element* HTMLCollection::item(size_t index) const
{
    // The item(index) method steps are to return the indexth element in the collection. If there is no indexth element in the collection, then the method must return null.
    auto elements = collect_matching_elements();
    if (index >= elements.size())
        return nullptr;
    return elements[index];
}

// https://dom.spec.whatwg.org/#dom-htmlcollection-nameditem-key
Element* HTMLCollection::named_item(FlyString const& name) const
{
    // 1. If key is the empty string, return null.
    if (name.is_empty())
        return nullptr;
    auto elements = collect_matching_elements();
    // 2. Return the first element in the collection for which at least one of the following is true:
    //      - it has an ID which is key;
    if (auto it = elements.find_if([&](auto& entry) { return entry->attribute(HTML::AttributeNames::id) == name; }); it != elements.end())
        return *it;
    //      - it is in the HTML namespace and has a name attribute whose value is key;
    if (auto it = elements.find_if([&](auto& entry) { return entry->namespace_() == Namespace::HTML && entry->name() == name; }); it != elements.end())
        return *it;
    //    or null if there is no such element.
    return nullptr;
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-names
Vector<String> HTMLCollection::supported_property_names() const
{
    // 1. Let result be an empty list.
    Vector<String> result;

    // 2. For each element represented by the collection, in tree order:
    auto elements = collect_matching_elements();

    for (auto& element : elements) {
        // 1. If element has an ID which is not in result, append element’s ID to result.
        if (element->has_attribute(HTML::AttributeNames::id)) {
            auto id = element->attribute(HTML::AttributeNames::id);

            if (!result.contains_slow(id))
                result.append(id);
        }

        // 2. If element is in the HTML namespace and has a name attribute whose value is neither the empty string nor is in result, append element’s name attribute value to result.
        if (element->namespace_() == Namespace::HTML && element->has_attribute(HTML::AttributeNames::name)) {
            auto name = element->attribute(HTML::AttributeNames::name);

            if (!name.is_empty() && !result.contains_slow(name))
                result.append(name);
        }
    }

    // 3. Return result.
    return result;
}

// https://dom.spec.whatwg.org/#ref-for-dfn-supported-property-indices%E2%91%A1
bool HTMLCollection::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of elements represented by the collection.
    // If there are no such elements, then there are no supported property indices.
    auto elements = collect_matching_elements();
    if (elements.is_empty())
        return false;

    return index < elements.size();
}

}
