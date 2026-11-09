/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibACPI/Node.h>
#include <LibACPI/Object.h>

namespace ACPI::AML {

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

class MethodNode : public Node {
public:
    MethodNode(size_t start, size_t end, size_t table_index, MethodFlags flags, u64 number_mask)
        : m_start(start)
        , m_end(end)
        , m_table_index(table_index)
        , m_flags(flags)
        , m_number_mask(number_mask)
    {
    }

    virtual ErrorOr<void> write_description(StringBuilder& builder) override;

    virtual NodeType type() const override { return NodeType::Method; }
    virtual StringView kind() const override { return "Method"sv; }

    size_t table_index() const { return m_table_index; }

    size_t start() const { return m_start; }
    size_t end() const { return m_end; }

    MethodFlags flags() const { return m_flags; }
    u8 argument_count() const { return m_flags.argument_count; }

    u64 number_mask() const { return m_number_mask; }

private:
    size_t m_start { 0 };
    size_t m_end { 0 };
    size_t m_table_index { 0 };

    MethodFlags m_flags {};
    u64 m_number_mask { 0 };
};

class MutexNode : public Node {
public:
    MutexNode(u8 sync_flags)
        : m_sync_flags(sync_flags)
    {
    }

    virtual NodeType type() const override { return NodeType::Mutex; }
    virtual StringView kind() const override { return "Mutex"sv; }

    u8 sync_flags() const { return m_sync_flags; }

private:
    u8 m_sync_flags { 0 };
};

class OperationRegionNode : public Node {
public:
    OperationRegionNode(u8 region_space, u64 region_offset, u64 region_length)
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

    u64 offset() const { return m_offset; }
    u64 length() const { return m_length; }

private:
    u8 m_space_raw;
    u64 m_offset;
    u64 m_length;
};

class FieldNode : public Node {
public:
    FieldNode(NonnullRefPtr<OperationRegionNode> const& operation_region, u8 flags, size_t offset, size_t size)
        : m_operation_region(operation_region)
        , m_flags(flags)
        , m_offset(offset)
        , m_length(size)
    {
    }

    virtual ErrorOr<void> write_description(StringBuilder& builder) override;

    virtual NodeType type() const override { return NodeType::Field; }
    virtual StringView kind() const override { return "Field"sv; }

    NonnullRefPtr<OperationRegionNode> const& operation_region() const { return m_operation_region; }

    u8 flags() const { return m_flags; }
    u64 offset() const { return m_offset; }
    u64 length() const { return m_length; }

private:
    NonnullRefPtr<OperationRegionNode> m_operation_region;

    u8 m_flags;
    u64 m_offset;
    u64 m_length;
};

class DeviceNode : public Node {
public:
    virtual NodeType type() const override { return NodeType::Device; }
    virtual StringView kind() const override { return "Device"sv; }
};

class ProcessorNode : public Node {
public:
    ProcessorNode(u8 id, u32 processor_register_block_address, u8 processor_register_block_length)
        : m_id(id)
        , m_processor_register_block_address(processor_register_block_address)
        , m_processor_register_block_length(processor_register_block_length)
    {
    }

    virtual NodeType type() const override { return NodeType::Processor; }
    virtual StringView kind() const override { return "Processor"sv; }

private:
    u8 m_id { 0 };
    u32 m_processor_register_block_address { 0 };
    u8 m_processor_register_block_length { 0 };
};

}
