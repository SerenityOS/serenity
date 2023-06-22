/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Library/KString.h>

namespace Kernel::ACPI::AML {

class Parser;

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() = default;

    virtual void dump(size_t indent) const = 0;

    virtual bool is_define_method() const { return false; }
};

class DefinitionBlock final : public ASTNode {
public:
    DefinitionBlock(Vector<NonnullRefPtr<ASTNode>> terms)
        : m_terms(move(terms))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    Vector<NonnullRefPtr<ASTNode>> m_terms;
};

class DefineName final : public ASTNode {
public:
    DefineName(NonnullOwnPtr<KString> name, NonnullRefPtr<ASTNode> object)
        : m_name(move(name))
        , m_object(move(object))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_name;
    NonnullRefPtr<ASTNode> m_object;
};

class DefineScope final : public ASTNode {
public:
    DefineScope(NonnullOwnPtr<KString> location, Vector<NonnullRefPtr<ASTNode>> terms)
        : m_location(move(location))
        , m_terms(move(terms))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_location;
    Vector<NonnullRefPtr<ASTNode>> m_terms;
};

class DefineBuffer final : public ASTNode {
public:
    DefineBuffer(NonnullRefPtr<ASTNode> size, ByteBuffer byte_list)
        : m_size(move(size))
        , m_byte_list(move(byte_list))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_size;
    ByteBuffer m_byte_list;
};

class DefinePackage final : public ASTNode {
public:
    DefinePackage(u8 element_count, Vector<NonnullRefPtr<ASTNode>> element_list)
        : m_element_count(element_count)
        , m_element_list(move(element_list))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    u8 m_element_count { 0 };
    Vector<NonnullRefPtr<ASTNode>> m_element_list;
};

class DefineVariablePackage final : public ASTNode {
public:
    DefineVariablePackage(NonnullRefPtr<ASTNode> element_count, Vector<NonnullRefPtr<ASTNode>> element_list)
        : m_element_count(move(element_count))
        , m_element_list(move(element_list))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_element_count;
    Vector<NonnullRefPtr<ASTNode>> m_element_list;
};

class DefineMethod final : public ASTNode {
public:
    DefineMethod(NonnullOwnPtr<KString> name, u8 argument_count, bool serialized, u8 synchronization_level, Vector<NonnullRefPtr<ASTNode>> terms)
        : m_name(move(name))
        , m_argument_count(argument_count)
        , m_serialized(serialized)
        , m_synchronization_level(synchronization_level)
        , m_terms(move(terms))
    {
    }

    virtual void dump(size_t indent) const override;

    virtual bool is_define_method() const override { return true; }

    u8 argument_count() const { return m_argument_count; }

    void set_terms(Badge<Parser>, Vector<NonnullRefPtr<ASTNode>> terms) { m_terms = move(terms); }

private:
    NonnullOwnPtr<KString> m_name;
    u8 m_argument_count { 0 };
    bool m_serialized { false };
    u8 m_synchronization_level { 0x0 };
    Vector<NonnullRefPtr<ASTNode>> m_terms;
};

class DefineMutex final : public ASTNode {
public:
    DefineMutex(NonnullOwnPtr<KString> name, u8 synchronization_level)
        : m_name(move(name))
        , m_synchronization_level(synchronization_level)
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_name;
    u8 m_synchronization_level { 0 };
};

class DefineOperationRegion final : public ASTNode {
public:
    DefineOperationRegion(NonnullOwnPtr<KString> name, GenericAddressStructure::AddressSpace region_space, NonnullRefPtr<ASTNode> region_offset, NonnullRefPtr<ASTNode> region_length)
        : m_name(move(name))
        , m_region_space(region_space)
        , m_region_offset(move(region_offset))
        , m_region_length(move(region_length))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_name;
    GenericAddressStructure::AddressSpace m_region_space;
    NonnullRefPtr<ASTNode> m_region_offset;
    NonnullRefPtr<ASTNode> m_region_length;
};

enum class FieldAccessType {
    AnyAccess = 0,
    ByteAccess = 1,
    WordAccess = 2,
    DWordAccess = 3,
    QWordAccess = 4,
    BufferAccess = 5,
    Reserved = 6,
};

enum class FieldLockRule {
    NoLock = 0,
    Lock = 1,
};

enum class FieldUpdateRule {
    Preserve = 0,
    WriteAsOnes = 1,
    WriteAsZeros = 2,
    Reserved = 3,
};

class NamedField final : public ASTNode {
public:
    explicit NamedField(NonnullOwnPtr<KString> name, u32 size)
        : m_name(move(name))
        , m_size(size)
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_name;
    u32 m_size;
};

class ReservedField final : public ASTNode {
public:
    explicit ReservedField(u32 size)
        : m_size(size)
    {
    }

    virtual void dump(size_t indent) const override;

private:
    u32 m_size;
};

class DefineField final : public ASTNode {
public:
    DefineField(NonnullOwnPtr<KString> operation_region_name, FieldAccessType access_type, FieldLockRule lock_rule, FieldUpdateRule update_rule, Vector<NonnullRefPtr<ASTNode>> field_list)
        : m_operation_region_name(move(operation_region_name))
        , m_access_type(access_type)
        , m_lock_rule(lock_rule)
        , m_update_rule(update_rule)
        , m_field_list(move(field_list))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_operation_region_name;
    FieldAccessType m_access_type;
    FieldLockRule m_lock_rule;
    FieldUpdateRule m_update_rule;
    Vector<NonnullRefPtr<ASTNode>> m_field_list;
};

class DefineDevice final : public ASTNode {
public:
    DefineDevice(NonnullOwnPtr<KString> name, Vector<NonnullRefPtr<ASTNode>> terms)
        : m_name(move(name))
        , m_terms(move(terms))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_name;
    Vector<NonnullRefPtr<ASTNode>> m_terms;
};

class DefineProcessor final : public ASTNode {
public:
    DefineProcessor(NonnullOwnPtr<KString> name, u8 processor_id, u32 processor_block_address, u8 processor_block_length, Vector<NonnullRefPtr<ASTNode>> terms)
        : m_name(move(name))
        , m_processor_id(processor_id)
        , m_processor_block_address(processor_block_address)
        , m_processor_block_length(processor_block_length)
        , m_terms(move(terms))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_name;
    u8 m_processor_id { 0 };
    u32 m_processor_block_address { 0 };
    u8 m_processor_block_length { 0 };
    Vector<NonnullRefPtr<ASTNode>> m_terms;
};

class DebugObject final : public ASTNode {
public:
    explicit DebugObject() = default;

    virtual void dump(size_t indent) const override;
};

class LocalObject final : public ASTNode {
public:
    explicit LocalObject(size_t local_index)
        : m_local_index(local_index)
    {
    }

    virtual void dump(size_t indent) const override;

private:
    size_t m_local_index { 0 };
};

class ArgumentObject final : public ASTNode {
public:
    explicit ArgumentObject(size_t argument_index)
        : m_argument_index(argument_index)
    {
    }

    virtual void dump(size_t indent) const override;

private:
    size_t m_argument_index { 0 };
};

class IntegerData final : public ASTNode {
public:
    IntegerData(u64 value)
        : m_value(value)
    {
    }

    virtual void dump(size_t indent) const override;

private:
    u64 m_value { 0 };
};

class StringData final : public ASTNode {
public:
    StringData(NonnullOwnPtr<KString> value)
        : m_value(move(value))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_value;
};

class Reference final : public ASTNode {
public:
    Reference(NonnullOwnPtr<KString> value)
        : m_value(move(value))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_value;
};

class Acquire final : public ASTNode {
public:
    Acquire(NonnullRefPtr<ASTNode> target, u16 timeout)
        : m_target(move(target))
        , m_timeout(timeout)
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_target;
    u16 m_timeout { 0 };
};

class CreateDWordField final : public ASTNode {
public:
    CreateDWordField(NonnullRefPtr<ASTNode> source_buffer, NonnullRefPtr<ASTNode> byte_index, NonnullOwnPtr<KString> name)
        : m_source_buffer(move(source_buffer))
        , m_byte_index(move(byte_index))
        , m_name(move(name))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_source_buffer;
    NonnullRefPtr<ASTNode> m_byte_index;
    NonnullOwnPtr<KString> m_name;
};

class Release final : public ASTNode {
public:
    Release(NonnullRefPtr<ASTNode> target)
        : m_target(move(target))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_target;
};

class Store final : public ASTNode {
public:
    Store(NonnullRefPtr<ASTNode> operand, NonnullRefPtr<ASTNode> target)
        : m_operand(move(operand))
        , m_target(move(target))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_operand;
    NonnullRefPtr<ASTNode> m_target;
};

enum class BinaryOperation {
    Add,
    Subtract,
    ShiftLeft,
    ShiftRight,
    BitwiseAnd,
    BitwiseOr
};
class BinaryExpression final : public ASTNode {
public:
    BinaryExpression(BinaryOperation operation, NonnullRefPtr<ASTNode> first_operand, NonnullRefPtr<ASTNode> second_operand, RefPtr<ASTNode> target)
        : m_operation(operation)
        , m_first_operand(move(first_operand))
        , m_second_operand(move(second_operand))
        , m_target(move(target))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    BinaryOperation m_operation;
    NonnullRefPtr<ASTNode> m_first_operand;
    NonnullRefPtr<ASTNode> m_second_operand;
    RefPtr<ASTNode> m_target;
};

enum class UnaryOperation {
    LogicalNot,
    SizeOf,
    DerefOf
};
class UnaryExpression final : public ASTNode {
public:
    UnaryExpression(UnaryOperation operation, NonnullRefPtr<ASTNode> operand)
        : m_operation(operation)
        , m_operand(move(operand))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    UnaryOperation m_operation;
    NonnullRefPtr<ASTNode> m_operand;
};

enum class UpdateOperation {
    Increment,
    Decrement,
};
class UpdateExpression final : public ASTNode {
public:
    UpdateExpression(UpdateOperation operation, NonnullRefPtr<ASTNode> target)
        : m_operation(operation)
        , m_target(move(target))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    UpdateOperation m_operation;
    NonnullRefPtr<ASTNode> m_target;
};

enum class LogicalOperation {
    LogicalAnd,
    LogicalOr,
    LogicalEqual,
    LogicalGreater,
    LogicalLess
};
class LogicalExpression final : public ASTNode {
public:
    LogicalExpression(LogicalOperation operation, NonnullRefPtr<ASTNode> first_operand, NonnullRefPtr<ASTNode> second_operand)
        : m_operation(operation)
        , m_first_operand(move(first_operand))
        , m_second_operand(move(second_operand))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    LogicalOperation m_operation;
    NonnullRefPtr<ASTNode> m_first_operand;
    NonnullRefPtr<ASTNode> m_second_operand;
};

class ToBuffer final : public ASTNode {
public:
    ToBuffer(NonnullRefPtr<ASTNode> operand, RefPtr<ASTNode> target)
        : m_operand(move(operand))
        , m_target(move(target))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_operand;
    RefPtr<ASTNode> m_target;
};

class ToHexString final : public ASTNode {
public:
    ToHexString(NonnullRefPtr<ASTNode> operand, RefPtr<ASTNode> target)
        : m_operand(move(operand))
        , m_target(move(target))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_operand;
    RefPtr<ASTNode> m_target;
};

class Index final : public ASTNode {
public:
    Index(NonnullRefPtr<ASTNode> first_operand, NonnullRefPtr<ASTNode> second_operand, RefPtr<ASTNode> target)
        : m_first_operand(move(first_operand))
        , m_second_operand(move(second_operand))
        , m_target(move(target))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_first_operand;
    NonnullRefPtr<ASTNode> m_second_operand;
    RefPtr<ASTNode> m_target;
};

class MethodInvocation final : public ASTNode {
public:
    MethodInvocation(NonnullOwnPtr<KString> target, Vector<NonnullRefPtr<ASTNode>> arguments)
        : m_target(move(target))
        , m_arguments(move(arguments))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullOwnPtr<KString> m_target;
    Vector<NonnullRefPtr<ASTNode>> m_arguments;
};

class Notify final : public ASTNode {
public:
    Notify(NonnullRefPtr<ASTNode> object, NonnullRefPtr<ASTNode> value)
        : m_object(move(object))
        , m_value(move(value))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_object;
    NonnullRefPtr<ASTNode> m_value;
};

class If final : public ASTNode {
public:
    If(NonnullRefPtr<ASTNode> predicate, Vector<NonnullRefPtr<ASTNode>> terms, Vector<NonnullRefPtr<ASTNode>> else_terms)
        : m_predicate(move(predicate))
        , m_terms(move(terms))
        , m_else_terms(move(else_terms))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_predicate;
    Vector<NonnullRefPtr<ASTNode>> m_terms;
    Vector<NonnullRefPtr<ASTNode>> m_else_terms;
};

class While final : public ASTNode {
public:
    While(NonnullRefPtr<ASTNode> predicate, Vector<NonnullRefPtr<ASTNode>> terms)
        : m_predicate(move(predicate))
        , m_terms(move(terms))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_predicate;
    Vector<NonnullRefPtr<ASTNode>> m_terms;
};

class Return final : public ASTNode {
public:
    Return(NonnullRefPtr<ASTNode> value)
        : m_value(move(value))
    {
    }

    virtual void dump(size_t indent) const override;

private:
    NonnullRefPtr<ASTNode> m_value;
};

class Break final : public ASTNode {
public:
    explicit Break()
    {
    }

    virtual void dump(size_t indent) const override;
};

}
