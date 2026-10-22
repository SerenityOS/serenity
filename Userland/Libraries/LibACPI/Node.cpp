/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Node.h"
#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/RefPtr.h>
#include <LibACPI/NameString.h>

namespace ACPI::AML {

ErrorOr<RefPtr<Node>> Node::find_node(NameString const& path, RefPtr<Node> const& scope)
{
    auto root = scope;

    bool search = path.type() == NameString::Type::Relative && path.count() == 1 && path.depth() == 0;
    if (path.type() == NameString::Type::Relative && path.depth() > 0) {
        for (size_t i = 0; i < path.depth(); i++) {
            root = root->parent();
            if (root.is_null()) {
                return Error::from_string_view_or_print_error_and_return_errno("Can't go higher than root!"sv, EINVAL);
            }
        }
    } else if (path.type() == NameString::Type::Absolute) {
        while (!root->parent().is_null()) {
            root = root->parent();
        }
    }

    if (search) {
        // Path is a single name segment, is relative and has no depth.
        // Crawl upwards from the current scope and try finding a node with the given name.
        auto node_name = TRY(path.segment(0));
        while (!root.is_null()) {
            auto found = root->find_child(node_name);
            if (found.is_error()) {
                root = root->parent();
                continue;
            }

            return found.release_value();
        }

        dbgln("[ACPI] Node::find_node: Could not find node with name {}!", node_name.to_string_view());
        return Error::from_string_view_or_print_error_and_return_errno("Node not found!"sv, EINVAL);
    }

    // Search crawls down from root via name segments.
    for (size_t i = 0; i < path.count(); i++) {
        auto node_name = TRY(path.segment(i));
        auto found = root->find_child(node_name);
        if (found.is_error()) {
            // FIXME: Warn without allocating any string.
            dbgln("[ACPI] Node::find_node: Could not find node at path {}!", path.to_string());
            return found.release_error();
        }

        root = found.release_value();
    }
    return root;
}

ErrorOr<RefPtr<Node>> Node::find_child(NameSegment name) const
{
    for (auto const& child : m_children) {
        if (child->name() == name)
            return child;
    }

    dbgln("[ACPI] Node::find_child: Child {} does not exist relative to node {}!", name.to_string_view(), m_name.to_string_view());
    return Error::from_string_view_or_print_error_and_return_errno("Child does not exist."sv, EINVAL);
}

ErrorOr<RefPtr<Node>> Node::find_child(StringView name) const
{
    if (name.length() != 4)
        return Error::from_string_view_or_print_error_and_return_errno("Name must have a length of 4."sv, EINVAL);

    auto broken_name = TRY(NameSegment::from_string_view(name));
    return find_child(broken_name);
}

ErrorOr<void> Node::insert_child(NameSegment name, RefPtr<Node> const& node)
{
    /// FIXME: Duplicate name detection.
    node->m_name = name;
    node->m_parent = this;
    m_children.append(node);
    return {};
}

ErrorOr<void> Node::insert_child(StringView name, RefPtr<Node> const& node)
{
    if (name.length() != 4)
        return Error::from_string_view_or_print_error_and_return_errno("Name must have a length of 4."sv, EINVAL);

    auto broken_name = TRY(NameSegment::from_string_view(name));
    return insert_child(broken_name, node);
}

ErrorOr<void> Node::write_description(StringBuilder& builder)
{
    TRY(builder.try_append(kind()));
    return {};
}

ErrorOr<void> NameNode::write_description(StringBuilder& builder)
{
    if (m_data.is<u64>()) {
        auto value = MUST(m_data.as<u64>());
        TRY(builder.try_appendff("(Decimal: {}, Hexadecimal: {:X})", value, value));
    } else if (m_data.is<NameString>()) {
        auto value = MUST(m_data.as<NameString>());
        TRY(builder.try_appendff("{}"sv, TRY(value.to_string())));
    } else {
        TRY(builder.try_append("Invalid"sv));
    }

    return {};
}

}
