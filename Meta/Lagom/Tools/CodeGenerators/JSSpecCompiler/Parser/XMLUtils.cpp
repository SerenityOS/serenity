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

ParseErrorOr<StringView> get_attribute_by_name(XML::Node const* node, StringView attribute_name)
{
    auto const& attribute = node->as_element().attributes.get(attribute_name);

    if (!attribute.has_value())
        return ParseError::create(String::formatted("Attribute {} is not present", attribute_name), node);
    return attribute.value();
}

ParseErrorOr<StringView> get_text_contents(XML::Node const* node)
{
    auto const& children = node->as_element().children;

    if (children.size() != 1 || !children[0]->is_text())
        return ParseError::create("Expected single text node in a child list of the node"sv, node);
    return children[0]->as_text().builder.string_view();
}

ParseErrorOr<XML::Node const*> get_only_child(XML::Node const* element, StringView tag_name)
{
    XML::Node const* result = nullptr;

    for (auto const& child : element->as_element().children) {
        TRY(child->content.visit(
            [&](XML::Node::Element const& element) -> ParseErrorOr<void> {
                if (element.name != tag_name)
                    return ParseError::create(String::formatted("Expected child with the tag name {} but found {}", tag_name, element.name), child);
                if (result != nullptr)
                    return ParseError::create("Element must have only one child"sv, child);
                result = child;
                return {};
            },
            [&](XML::Node::Text const&) -> ParseErrorOr<void> {
                if (!contains_empty_text(child))
                    return ParseError::create("Element should not have non-empty child text nodes"sv, element);
                return {};
            },
            move(ignore_comments)));
    }

    if (result == nullptr)
        return ParseError::create(String::formatted("Element must have only one child"), element);
    return result;
}

}
