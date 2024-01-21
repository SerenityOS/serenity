/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibXML/DOM/Node.h>

#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

bool contains_empty_text(XML::Node const* node)
{
    return node->as_text().builder.string_view().trim_whitespace().is_empty();
}

Optional<StringView> get_attribute_by_name(XML::Node const* node, StringView attribute_name)
{
    auto const& attribute = node->as_element().attributes.get(attribute_name);

    if (!attribute.has_value())
        return {};
    return attribute.value();
}

Optional<StringView> get_text_contents(XML::Node const* node)
{
    auto const& children = node->as_element().children;
    if (children.size() != 1 || !children[0]->is_text())
        return {};
    return children[0]->as_text().builder.string_view();
}

Optional<XML::Node const*> get_single_child_with_tag(XML::Node const* element, StringView tag_name)
{
    XML::Node const* result = nullptr;

    for (auto const& child : element->as_element().children) {
        auto is_valid = child->content.visit(
            [&](XML::Node::Element const& element) {
                result = child;
                return result != nullptr || element.name != tag_name;
            },
            [&](XML::Node::Text const&) {
                return contains_empty_text(child);
            },
            [&](auto const&) { return true; });
        if (!is_valid)
            return {};
    }

    if (result == nullptr)
        return {};
    return result;
}

}
