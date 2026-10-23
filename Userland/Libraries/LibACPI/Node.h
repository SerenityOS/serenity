/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "NameString.h"
#include "NodeData.h"
#include <AK/Array.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Variant.h>
#include <AK/WeakPtr.h>
#include <LibACPI/Definitions.h>

namespace ACPI::AML {

enum class NodeType : u8 {
    Untyped,
    Scope,
    Name,
};

class Node : public RefCounted<Node>
    , public Weakable<Node> {
public:
    static ErrorOr<RefPtr<Node>> find_node(NameString const& path, RefPtr<Node> const& scope);

    ErrorOr<RefPtr<Node>> find_child(NameSegment name) const;
    ErrorOr<RefPtr<Node>> find_child(StringView name) const;

    ErrorOr<void> insert_child(NameSegment name, RefPtr<Node> const& node);
    ErrorOr<void> insert_child(StringView name, RefPtr<Node> const& node);

    NameSegment name() const { return m_name; }

    WeakPtr<Node> parent() const { return m_parent; }
    Vector<RefPtr<Node>> const& children() const { return m_children; }

    virtual ErrorOr<void> write_description(StringBuilder& builder);

    virtual NodeType type() const { return NodeType::Untyped; }
    virtual StringView kind() const { return "Node"sv; }
    virtual ~Node() = default;

protected:
    Node(RefPtr<Node> const& parent)
        : m_parent(parent)
    {
    }

    NameSegment m_name;

    WeakPtr<Node> m_parent;
    Vector<RefPtr<Node>> m_children;
};

class ScopeNode : public Node {
public:
    ScopeNode()
        : Node(nullptr)
    {
    }
    ~ScopeNode() = default;

    virtual NodeType type() const override { return NodeType::Scope; }
    virtual StringView kind() const override { return "Scope"sv; }
};

class NameNode : public Node {
public:
    NameNode(NodeData value)
        : Node(nullptr)
        , m_data(value)
    {
    }
    ~NameNode() = default;

    virtual ErrorOr<void> write_description(StringBuilder& builder) override;

    virtual NodeType type() const override { return NodeType::Name; }
    virtual StringView kind() const override { return "Name"sv; }

    NodeData& data() { return m_data; }

protected:
    NodeData m_data;
};

}
