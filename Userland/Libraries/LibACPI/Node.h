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
    OperationRegion,
    Field,
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
    NameSegment m_name;

    WeakPtr<Node> m_parent;
    Vector<NonnullRefPtr<Node>> m_children;
};

class ScopeNode : public Node {
public:
    virtual NodeType type() const override { return NodeType::Scope; }
    virtual StringView kind() const override { return "Scope"sv; }
};

class NameNode : public Node {
public:
    NameNode(Object value)
        : m_data(move(value))
    {
    }

    virtual ErrorOr<void> write_description(StringBuilder& builder) override;

    virtual NodeType type() const override { return NodeType::Name; }
    virtual StringView kind() const override { return "Name"sv; }

    Object& data() { return m_data; }

protected:
    Object m_data;
};

class OperationRegionNode : public Node {
public:
    OperationRegionNode(u8 region_space, i64 region_offset, i64 region_length)
        : m_space_raw(region_space)
        , m_offset(region_offset)
        , m_length(region_length)
    {
    }

    virtual ErrorOr<void> write_description(StringBuilder& builder) override;

    virtual NodeType type() const override { return NodeType::OperationRegion; }
    virtual StringView kind() const override { return "Operation Region"sv; }

    GenericAddressStructure::AddressSpace space() const
    {
        if (m_space_raw >= 0x80)
            return GenericAddressStructure::AddressSpace::OEMDefined;

        return static_cast<GenericAddressStructure::AddressSpace>(m_space_raw);
    }

    i64 offset() const { return m_offset; }
    i64 length() const { return m_length; }

protected:
    u8 m_space_raw;
    i64 m_offset;
    i64 m_length;
};

class FieldNode : public Node {
public:
    FieldNode(RefPtr<Node> const& operation_region, u8 flags, size_t offset, size_t size)
        : m_operation_region(operation_region)
        , m_flags(flags)
        , m_offset(offset)
        , m_size(size)
    {
    }

    virtual ErrorOr<void> write_description(StringBuilder& builder) override;

    virtual NodeType type() const override { return NodeType::Field; }
    virtual StringView kind() const override { return "Field"sv; }

    RefPtr<Node> const& operation_region() const { return m_operation_region; }

    u8 flags() const { return m_flags; }
    size_t offset() const { return m_offset; }
    size_t size() const { return m_size; }

private:
    RefPtr<Node> m_operation_region;

    u8 m_flags;
    size_t m_offset;
    size_t m_size;
};

}
