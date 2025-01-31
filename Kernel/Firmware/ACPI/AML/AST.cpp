/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/ACPI/AML/AST.h>

namespace Kernel::ACPI::AML {

static void print_indent(size_t indent)
{
    for (auto i = 0u; i < indent; ++i) {
        dbg("  ");
    }
}

void DefinitionBlock::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("DefinitionBlock");
    print_indent(indent);
    dbgln("{{");
    for (auto const& term : m_terms)
        term->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void DefineName::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Name ({}, ", m_name);
    m_object->dump(0);
    dbgln(")");
}

void DefineScope::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Scope ({})", m_location);
    print_indent(indent);
    dbgln("{{");
    for (auto const& term : m_terms)
        term->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void DefineBuffer::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Buffer (");
    m_size->dump(0);
    dbgln(")");
    print_indent(indent);
    dbgln("{{");
    print_indent(indent + 1);
    for (auto i = 0u; i < m_byte_list.size(); ++i) {
        dbg("{:#02x}", m_byte_list[i]);
        if (i != (m_byte_list.size() - 1))
            dbg(", ");
    }
    print_indent(indent);
    dbgln("}}");
}

void DefinePackage::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Package ({:#02x})", m_element_count);
    print_indent(indent);
    dbgln("{{");
    for (auto i = 0u; i < m_element_list.size(); ++i) {
        print_indent(indent + 1);
        m_element_list[i]->dump(indent + 1);
        if (i != (m_element_list.size() - 1))
            dbg(",");
        dbgln();
    }
    print_indent(indent);
    dbgln("}}");
}

void DefineVariablePackage::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Package (");
    m_element_count->dump(0);
    dbgln(")");
    print_indent(indent);
    dbgln("{{");
    for (auto const& element : m_element_list)
        element->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void DefineMethod::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Method ({}, {}, {})", m_name, m_argument_count, m_serialized ? "Serialized"sv : "NotSerialized"sv);
    print_indent(indent);
    dbgln("{{");
    for (auto const& term : m_terms)
        term->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void DefineMutex::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Mutex ({}, {:#02x}", m_name, m_synchronization_level);
}

static StringView address_space_to_string(GenericAddressStructure::AddressSpace address_space)
{
    switch (address_space) {
    case GenericAddressStructure::AddressSpace::SystemMemory:
        return "SystemMemory"sv;
    case GenericAddressStructure::AddressSpace::SystemIO:
        return "SystemIO"sv;
    case GenericAddressStructure::AddressSpace::PCIConfigurationSpace:
        return "PCIConfigurationSpace"sv;
    case GenericAddressStructure::AddressSpace::EmbeddedController:
        return "EmbeddedController"sv;
    case GenericAddressStructure::AddressSpace::SMBus:
        return "SMBus"sv;
    case GenericAddressStructure::AddressSpace::SystemCMOS:
        return "SystemCMOS"sv;
    case GenericAddressStructure::AddressSpace::PCIBarTarget:
        return "PCIBarTarget"sv;
    case GenericAddressStructure::AddressSpace::IPMI:
        return "IPMI"sv;
    case GenericAddressStructure::AddressSpace::GeneralPurposeIO:
        return "GeneralPurposeIO"sv;
    case GenericAddressStructure::AddressSpace::GenericSerialBus:
        return "GenericSerialBus"sv;
    case GenericAddressStructure::AddressSpace::PCC:
        return "PCC"sv;
    case GenericAddressStructure::AddressSpace::FunctionalFixedHardware:
        return "FunctionalFixedHardware"sv;
    default:
        return "OEM Defined"sv;
    }
}

void DefineOperationRegion::dump(size_t indent) const
{
    print_indent(indent);
    dbg("OperationRegion ({}, {}, ", m_name, address_space_to_string(m_region_space));
    m_region_offset->dump(0);
    dbg(", ");
    m_region_length->dump(0);
    dbgln(")");
}

static StringView field_access_type_to_string(FieldAccessType access_type)
{
    switch (access_type) {
    case FieldAccessType::AnyAccess:
        return "AnyAcc"sv;
    case FieldAccessType::ByteAccess:
        return "ByteAcc"sv;
    case FieldAccessType::WordAccess:
        return "WordAcc"sv;
    case FieldAccessType::DWordAccess:
        return "DWordAcc"sv;
    case FieldAccessType::QWordAccess:
        return "QWordAcc"sv;
    case FieldAccessType::BufferAccess:
        return "BufferAcc"sv;
    default:
        return "Reserved"sv;
    }
}

static StringView field_lock_rule_to_string(FieldLockRule lock_rule)
{
    switch (lock_rule) {
    case FieldLockRule::NoLock:
        return "NoLock"sv;
    case FieldLockRule::Lock:
        return "Lock"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

static StringView field_update_rule_to_string(FieldUpdateRule update_rule)
{
    switch (update_rule) {
    case FieldUpdateRule::Preserve:
        return "Preserve"sv;
    case FieldUpdateRule::WriteAsOnes:
        return "WriteAsOnes"sv;
    case FieldUpdateRule::WriteAsZeros:
        return "WriteAsZeros"sv;
    default:
        return "Reserved"sv;
    }
}

void NamedField::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("{}, {}", m_name, m_size);
}

void ReservedField::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("RESERVED, {}", m_size);
}

void DefineField::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Field ({}, {}, {}, {})", m_operation_region_name, field_access_type_to_string(m_access_type), field_lock_rule_to_string(m_lock_rule), field_update_rule_to_string(m_update_rule));
    print_indent(indent);
    dbgln("{{");
    for (auto const& field : m_field_list)
        field->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void DefineDevice::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Device ({})", m_name);
    print_indent(indent);
    dbgln("{{");
    for (auto const& term : m_terms)
        term->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void DefineProcessor::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Processor ({}, {:#02x}, {:#08x}, {:#02x})", m_name, m_processor_id, m_processor_block_address, m_processor_block_length);
    print_indent(indent);
    dbgln("{{");
    for (auto const& term : m_terms)
        term->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void DebugObject::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Debug ()");
}

void LocalObject::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Local{}", m_local_index);
}

void ArgumentObject::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Arg{}", m_argument_index);
}

void IntegerData::dump(size_t indent) const
{
    print_indent(indent);
    dbg("{:#x}", m_value);
}

void StringData::dump(size_t indent) const
{
    print_indent(indent);
    dbg("\"{}\"", m_value);
}

void Reference::dump(size_t indent) const
{
    print_indent(indent);
    dbg("{}", m_value);
}

void Acquire::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Acquire (");
    m_target->dump(0);
    dbgln(", {:#04x})", m_timeout);
}

void CreateDWordField::dump(size_t indent) const
{
    print_indent(indent);
    dbg("CreateDWordField (");
    m_source_buffer->dump(0);
    dbg(", ");
    m_byte_index->dump(0);
    dbgln(", {})", m_name);
}

void Release::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Acquire (");
    m_target->dump(0);
    dbgln(")");
}

void Store::dump(size_t indent) const
{
    print_indent(indent);
    m_target->dump(0);
    dbg(" = ");
    m_operand->dump(0);
    dbgln();
}

static StringView binary_operation_to_string(BinaryOperation operation)
{
    switch (operation) {
    case BinaryOperation::Add:
        return "+"sv;
    case BinaryOperation::Subtract:
        return "-"sv;
    case BinaryOperation::ShiftLeft:
        return "<<"sv;
    case BinaryOperation::ShiftRight:
        return ">>"sv;
    case BinaryOperation::BitwiseAnd:
        return "&"sv;
    case BinaryOperation::BitwiseOr:
        return "|"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void BinaryExpression::dump(size_t indent) const
{
    print_indent(indent);
    if (m_target) {
        m_target->dump(0);
        dbg(" = ");
    }
    dbg("(");
    m_first_operand->dump(0);
    dbg(" {} ", binary_operation_to_string(m_operation));
    m_second_operand->dump(0);
    dbg(")");
    if (m_target)
        dbgln();
}

static StringView update_operation_to_string(UpdateOperation operation)
{
    switch (operation) {
    case UpdateOperation::Increment:
        return "++"sv;
    case UpdateOperation::Decrement:
        return "--"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void UpdateExpression::dump(size_t indent) const
{
    print_indent(indent);
    m_target->dump(0);
    dbgln("{}", update_operation_to_string(m_operation));
}

static StringView unary_operation_to_string(UnaryOperation operation)
{
    switch (operation) {
    case UnaryOperation::LogicalNot:
        return "!"sv;
    case UnaryOperation::SizeOf:
        return "SizeOf"sv;
    case UnaryOperation::DerefOf:
        return "DerefOf"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void UnaryExpression::dump(size_t indent) const
{
    print_indent(indent);
    dbg("{} (", unary_operation_to_string(m_operation));
    m_operand->dump(0);
    dbg(")");
}

static StringView logical_operation_to_string(LogicalOperation operation)
{
    switch (operation) {
    case LogicalOperation::LogicalAnd:
        return "&&"sv;
    case LogicalOperation::LogicalOr:
        return "||"sv;
    case LogicalOperation::LogicalEqual:
        return "=="sv;
    case LogicalOperation::LogicalGreater:
        return ">"sv;
    case LogicalOperation::LogicalLess:
        return "<"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void LogicalExpression::dump(size_t indent) const
{
    print_indent(indent);
    dbg("(");
    m_first_operand->dump(0);
    dbg(" {} ", logical_operation_to_string(m_operation));
    m_second_operand->dump(0);
    dbg(")");
}

void ToBuffer::dump(size_t indent) const
{
    print_indent(indent);
    dbg("ToBuffer (");
    m_operand->dump(0);
    dbg(", ");
    if (m_target)
        m_target->dump(0);
    dbgln(")");
}

void ToHexString::dump(size_t indent) const
{
    print_indent(indent);
    dbg("ToHexString (");
    m_operand->dump(0);
    dbg(", ");
    if (m_target)
        m_target->dump(0);
    dbgln(")");
}

void Index::dump(size_t indent) const
{
    print_indent(indent);
    if (m_target) {
        m_target->dump(0);
        dbg(" = ");
    }
    m_first_operand->dump(0);
    dbg(" [");
    m_second_operand->dump(0);
    dbg("]");
    if (m_target)
        dbgln();
}

void MethodInvocation::dump(size_t indent) const
{
    print_indent(indent);
    dbg("{} (", m_target);
    for (auto i = 0u; i < m_arguments.size(); ++i) {
        m_arguments[i]->dump(0);
        if (i != (m_arguments.size() - 1))
            dbg(", ");
    }
    dbgln(")");
}

void Notify::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Notify (");
    m_object->dump(0);
    dbg(", ");
    m_value->dump(0);
    dbgln(")");
}

void If::dump(size_t indent) const
{
    print_indent(indent);
    dbg("If (");
    m_predicate->dump(0);
    dbgln(")");
    print_indent(indent);
    dbgln("{{");
    for (auto const& term : m_terms)
        term->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
    if (!m_else_terms.is_empty()) {
        print_indent(indent);
        dbgln("Else");
        print_indent(indent);
        dbgln("{{");
        for (auto const& term : m_else_terms)
            term->dump(indent + 1);
        print_indent(indent);
        dbgln("}}");
    }
}

void While::dump(size_t indent) const
{
    print_indent(indent);
    dbg("While (");
    m_predicate->dump(0);
    dbgln(")");
    print_indent(indent);
    dbgln("{{");
    for (auto const& term : m_terms)
        term->dump(indent + 1);
    print_indent(indent);
    dbgln("}}");
}

void Return::dump(size_t indent) const
{
    print_indent(indent);
    dbg("Return (");
    m_value->dump(0);
    dbgln(")");
}

void Break::dump(size_t indent) const
{
    print_indent(indent);
    dbgln("Break");
}

}
