/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/AtomicRefCounted.h>
#include <AK/Function.h>
#include <AK/RefPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Variant.h>
#include <AK/WeakPtr.h>
#include <LibACPI/Definitions.h>
#include <LibACPI/NameString.h>
#include <LibACPI/Object.h>

namespace ACPI::AML {

enum class NodeType : u8 {
    Untyped,
    Scope,
    Name,
};

class Node : public AtomicRefCounted<Node>
    , public Weakable<Node> {
public:
    ErrorOr<NonnullRefPtr<Node>> find_child(NameSegment name) const;
    ErrorOr<NonnullRefPtr<Node>> find_child(StringView name) const;

    ErrorOr<void> insert_child(NameSegment name, NonnullRefPtr<Node> node);
    ErrorOr<void> insert_child(StringView name, NonnullRefPtr<Node> node);

    NameSegment const& name() const { return m_name; }

    WeakPtr<Node> parent() const { return m_parent; }
    Vector<NonnullRefPtr<Node>> const& children() const { return m_children; }

    virtual ErrorOr<void> write_description(StringBuilder& builder);

    virtual NodeType type() const { return NodeType::Untyped; }
    virtual StringView kind() const { return "Node"sv; }
    virtual ~Node() = default;

protected:
    Node(RefPtr<Node> parent)
        : m_parent(move(parent))
    {
    }

    NameSegment m_name;

    WeakPtr<Node> m_parent;
    Vector<NonnullRefPtr<Node>> m_children;
};

class ScopeNode : public Node {
public:
    ScopeNode()
        : Node(nullptr)
    {
    }

    virtual NodeType type() const override { return NodeType::Scope; }
    virtual StringView kind() const override { return "Scope"sv; }
};

class NameNode : public Node {
public:
    NameNode(Object value)
        : Node(nullptr)
        , m_data(move(value))
    {
    }

    virtual ErrorOr<void> write_description(StringBuilder& builder) override;

    virtual NodeType type() const override { return NodeType::Name; }
    virtual StringView kind() const override { return "Name"sv; }

    Object& data() { return m_data; }

protected:
    Object m_data;
};

}
