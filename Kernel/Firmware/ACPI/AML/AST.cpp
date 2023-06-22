/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/ACPI/AML/AST.h>
#include <Kernel/Firmware/ACPI/AML/Value.h>

namespace Kernel::ACPI::AML {

static void print_indent(size_t indent)
{
    dbg("{:>{}}", "", indent);
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

ErrorOr<Value> DefinePackage::evaluate() const
{
    Vector<Value> package;
    TRY(package.try_ensure_capacity(m_element_count));

    for (auto const& element : m_element_list) {
        TRY(package.try_append(TRY(element->evaluate())));
    }

    while (package.size() != m_element_count) {
        TRY(package.try_append(Value()));
    }

    return Value(Value::Package(move(package)));
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

void IntegerData::dump(size_t indent) const
{
    print_indent(indent);
    dbg("{:#x}", m_value);
}

ErrorOr<Value> IntegerData::evaluate() const
{
    return Value(m_value);
}

void StringData::dump(size_t indent) const
{
    print_indent(indent);
    dbg("\"{}\"", m_value);
}

}
