/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/IntrusiveList.h>
#include <AK/NeverDestroyed.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/ACPI/Bytecode/Device.h>
#include <Kernel/ACPI/Bytecode/FieldList.h>
#include <Kernel/ACPI/Bytecode/Name.h>
#include <Kernel/ACPI/Bytecode/NameString.h>
#include <Kernel/ACPI/Bytecode/NamedObject.h>
#include <Kernel/ACPI/Bytecode/Package.h>
#include <Kernel/ACPI/Bytecode/Processor.h>
#include <Kernel/ACPI/Bytecode/Scope.h>
#include <Kernel/ACPI/Bytecode/TermObjectEnumerator.h>
#include <Kernel/ACPI/Bytecode/TermObjectEvaluator.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT TermObjectEnumerator::TermObjectEnumerator(ScopeBase& scope_base, Span<const u8> encoded_bytes)
    : m_scope_base(scope_base)
    , m_encoded_bytes(encoded_bytes)
{
}

UNMAP_AFTER_INIT EncodedObjectOpcode TermObjectEnumerator::current_opcode() const
{
    return EncodedObjectOpcode({ m_encoded_bytes[m_decode_pointer.current_pointer()], m_encoded_bytes[m_decode_pointer.current_pointer() + 1] });
}

bool TermObjectEnumerator::enumeration_ended() const
{
    return !(m_decode_pointer.current_pointer() < m_encoded_bytes.size());
}

void TermObjectEnumerator::enumerate_with_object_opcode()
{
    VERIFY(current_opcode().opcode().has_value());
    switch (current_opcode().opcode().value()) {
    case EncodedObjectOpcode::Opcode::Scope:
        add_scope();
        break;
    case EncodedObjectOpcode::Opcode::Alias:
        add_alias();
        break;
    case EncodedObjectOpcode::Opcode::Name:
        add_name();
        break;
    case EncodedObjectOpcode::Opcode::CreateBitField:
        add_create_bit_field();
        break;
    case EncodedObjectOpcode::Opcode::CreateByteField:
        add_create_byte_field();
        break;
    case EncodedObjectOpcode::Opcode::CreateWordField:
        add_create_word_field();
        break;
    case EncodedObjectOpcode::Opcode::CreateDWordField:
        add_create_dword_field();
        break;
    case EncodedObjectOpcode::Opcode::CreateQWordField:
        add_create_qword_field();
        break;
    case EncodedObjectOpcode::Opcode::CreateField:
        add_create_field();
        break;
    case EncodedObjectOpcode::Opcode::External:
        add_external();
        break;
    case EncodedObjectOpcode::Opcode::BankField:
        add_bank_field();
        break;
    case EncodedObjectOpcode::Opcode::DataRegion:
        add_data_region();
        break;
    case EncodedObjectOpcode::Opcode::OpRegion:
        add_op_region();
        break;
    case EncodedObjectOpcode::Opcode::PowerResource:
        add_power_resource();
        break;
    case EncodedObjectOpcode::Opcode::Processor:
        add_processor();
        break;
    case EncodedObjectOpcode::Opcode::ThermalZone:
        add_thermal_zone();
        break;
    case EncodedObjectOpcode::Opcode::Device:
        add_device();
        break;
    case EncodedObjectOpcode::Opcode::Event:
        add_event();
        break;
    case EncodedObjectOpcode::Opcode::Field:
        add_field();
        break;
    case EncodedObjectOpcode::Opcode::IndexField:
        add_index_field();
        break;
    case EncodedObjectOpcode::Opcode::Method:
        add_method();
        break;
    case EncodedObjectOpcode::Opcode::Mutex:
        add_mutex();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void TermObjectEnumerator::enumerate()
{
    dbgln_if(ACPI_AML_DEBUG, "Start enumeration of ScopeBase: Length {}, Current pointer {}, ended? {}", m_encoded_bytes.size(), m_decode_pointer.current_pointer(), enumeration_ended());
    while (1) {
        m_decode_pointer.load_new_pointer();
        dbgln_if(ACPI_AML_DEBUG, "Relative scope pointer now at {}", m_decode_pointer.current_pointer());
        if (enumeration_ended())
            break;
        dbgln_if(ACPI_AML_DEBUG, "Current opcode {:x}", m_encoded_bytes[m_decode_pointer.current_pointer()]);

        VERIFY(current_opcode().opcode().has_value());
        m_decode_pointer.increment_next_object_gap(current_opcode().has_extended_prefix() ? 2 : 1);
        enumerate_with_object_opcode();
    }
    dbgln_if(ACPI_AML_DEBUG, "End of enumeration for this ScopeBase");
}

void TermObjectEnumerator::add_scope()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Scope);
    VERIFY(current_object_is_package());
    auto scope_data = current_data_remainder(SkipPackageSizeEncoding::Yes);
    VERIFY(scope_data.has_value());
    m_scope_base.add_named_object({}, *Scope::must_create(*this, scope_data.value()));
    add_dynamic_length_object_to_pointer(calculate_package_length().package_size);
    dbgln_if(ACPI_AML_DEBUG, "End of handling scope!");
}
void TermObjectEnumerator::add_name()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Name);
    auto name_data = current_data_remainder(SkipPackageSizeEncoding::No);
    VERIFY(name_data.has_value());
    auto name = Name::must_create(*this, name_data.value());
    dbgln_if(ACPI_AML_DEBUG, "Found name declaration: {}", name->name_string().full_name());
    m_scope_base.add_named_object({}, *name);
    add_dynamic_length_object_to_pointer(name->encoded_length());
}
void TermObjectEnumerator::add_alias()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Alias);
    TODO();
}

void TermObjectEnumerator::add_create_bit_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::CreateBitField);
    TODO();
}
void TermObjectEnumerator::add_create_byte_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::CreateByteField);
    TODO();
}
void TermObjectEnumerator::add_create_word_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::CreateByteField);
    TODO();
}
void TermObjectEnumerator::add_create_dword_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::CreateDWordField);
    TODO();
}
void TermObjectEnumerator::add_create_qword_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::CreateQWordField);
    TODO();
}
void TermObjectEnumerator::add_create_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::CreateField);
    TODO();
}
void TermObjectEnumerator::add_external()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::External);
    TODO();
}

void TermObjectEnumerator::add_bank_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::BankField);
    TODO();
}
void TermObjectEnumerator::add_data_region()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::DataRegion);
    TODO();
}
void TermObjectEnumerator::add_op_region()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::OpRegion);
    auto op_region_settings = current_data_remainder(SkipPackageSizeEncoding::No);
    VERIFY(op_region_settings.has_value());
    auto region_name_string = NameString::try_to_create(op_region_settings.value());
    auto region_space = op_region_settings.value().slice(region_name_string->encoded_length(), 1)[0];
    auto region_offset_term_span = op_region_settings.value().slice(region_name_string->encoded_length() + 1);
    TermObjectEvaluator region_offset_evaluator(region_offset_term_span);
    auto region_length_term_span = op_region_settings.value().slice(region_name_string->encoded_length() + 1 + region_offset_evaluator.overall_terms_span_length());
    TermObjectEvaluator region_length_evaluator(region_length_term_span);
    dbgln_if(ACPI_AML_DEBUG, "OpRegion: {}, Length {}, Space {}, Region Offset 0x{:x}, Length {}", region_name_string->full_name(), region_name_string->encoded_length(), region_space, region_offset_evaluator.try_to_evaluate_value().as_unsigned_integer(), region_length_evaluator.try_to_evaluate_value().as_unsigned_integer());
    add_dynamic_length_object_to_pointer(region_name_string->encoded_length() + 1 + region_offset_evaluator.overall_terms_span_length() + region_length_evaluator.overall_terms_span_length());
}
void TermObjectEnumerator::add_power_resource()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::PowerResource);
    TODO();
}
void TermObjectEnumerator::add_processor()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Processor);
    VERIFY(current_object_is_package());
    auto processor_settings = current_data_remainder(SkipPackageSizeEncoding::Yes);
    VERIFY(processor_settings.has_value());
    auto processor_name_string = NameString::try_to_create(processor_settings.value());
    u8 processor_id = processor_settings.value().slice(processor_name_string->encoded_length(), 1)[0];
    u32 processor_block_address = 0;
    ByteReader::load<u32>(processor_settings.value().slice(processor_name_string->encoded_length() + 1, 4).data(), processor_block_address);
    u8 processor_block_length = processor_settings.value().slice(processor_name_string->encoded_length() + 1 + 4, 1)[0];
    dbgln_if(ACPI_AML_DEBUG, "New processor: package length {}, name {}, processor id {}, processor block address 0x{:x}, length {}", calculate_package_length().package_size, processor_name_string->full_name(), processor_id, processor_block_address, processor_block_length);

    dbgln_if(ACPI_AML_DEBUG, "ScopeBase created that is Processor actually");
    // FIXME: Add a processor scope object!
    m_scope_base.add_named_object({}, *Processor::must_create(processor_id, processor_block_address, *processor_name_string, processor_settings.value().slice(processor_name_string->encoded_length() + 1 + 4 + 1)));
    add_dynamic_length_object_to_pointer(calculate_package_length().package_size);
}
void TermObjectEnumerator::add_thermal_zone()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::ThermalZone);
    TODO();
}

void TermObjectEnumerator::add_device()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Device);
    // Please note that a Device is essentially the same thing like a Scope!
    VERIFY(current_object_is_package());
    auto scope_data = current_data_remainder(SkipPackageSizeEncoding::Yes);
    VERIFY(scope_data.has_value());
    dbgln_if(ACPI_AML_DEBUG, "ScopeBase created that is Device actually");
    m_scope_base.add_named_object({}, *Device::must_create(*this, scope_data.value()));
    add_dynamic_length_object_to_pointer(calculate_package_length().package_size);
}
void TermObjectEnumerator::add_event()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Event);
    TODO();
}
void TermObjectEnumerator::add_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Field);
    VERIFY(current_object_is_package());
    auto field_settings = current_data_remainder(SkipPackageSizeEncoding::Yes);
    VERIFY(field_settings.has_value());
    dbgln_if(ACPI_AML_DEBUG, "Field flags 0x{:x}", field_settings.value()[0]);
    auto field_name_string = NameString::try_to_create(field_settings.value());
    auto field_flags = field_settings.value().slice(field_name_string->encoded_length(), 1)[0];
    auto encoded_field_list = field_settings.value().slice(field_name_string->encoded_length() + 1);
    dbgln_if(ACPI_AML_DEBUG, "package length {}, field_flags {}, field list length {}", calculate_package_length().package_size, field_flags, encoded_field_list.size());
    auto field_list = FieldList::create(encoded_field_list);
    for (auto& field_element : field_list->elements()) {
        dbgln_if(ACPI_AML_DEBUG, "Field Element {}: Name(?) - {}", (u32)field_element.type(), field_element.possible_name_string() ? field_element.possible_name_string()->full_name() : "");
    }
    // FIXME: Maybe we should just use the calculate_package_length().package_size instead
    add_dynamic_length_object_to_pointer(calculate_package_length().encoding_length + field_name_string->encoded_length() + 1 + encoded_field_list.size());
}
void TermObjectEnumerator::add_index_field()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::IndexField);
    TODO();
}
void TermObjectEnumerator::add_method()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Method);
    VERIFY(current_object_is_package());
    auto method_settings = current_data_remainder(SkipPackageSizeEncoding::Yes);
    VERIFY(method_settings.has_value());
    auto method_name_string = NameString::try_to_create(method_settings.value());
    auto method_flags = method_settings.value().slice(method_name_string->encoded_length(), 1)[0];
    dbgln_if(ACPI_AML_DEBUG, "New method: package length {}, name {}, flags {}", calculate_package_length().package_size, method_name_string->full_name(), method_flags);
    add_dynamic_length_object_to_pointer(calculate_package_length().package_size);
}
void TermObjectEnumerator::add_mutex()
{
    VERIFY(current_opcode().opcode() == EncodedObjectOpcode::Opcode::Mutex);
    auto mutex_settings = current_data_remainder(SkipPackageSizeEncoding::No);
    VERIFY(mutex_settings.has_value());
    auto mutex_name_string = NameString::try_to_create(mutex_settings.value());
    auto mutex_flags = mutex_settings.value().slice(mutex_name_string->encoded_length(), 1)[0];
    dbgln_if(ACPI_AML_DEBUG, "New mutex: name {}, flags {}", mutex_name_string->full_name(), mutex_flags);
    // Note: We append mutex_name_string->encoded_length() + 1, and the 1 is for mutex flags!
    add_dynamic_length_object_to_pointer(mutex_name_string->encoded_length() + 1);
}

void TermObjectEnumerator::add_dynamic_length_object_to_pointer(size_t calculated_length)
{
    m_decode_pointer.increment_next_object_gap(calculated_length);
}

Package::DecodingResult TermObjectEnumerator::calculate_package_length() const
{
    VERIFY(current_object_is_package());

    auto possible_data_remainder = possible_data_remainder_after_opcode();
    VERIFY(possible_data_remainder.has_value());

    auto actual_data_after_opcode = possible_data_remainder.value();
    // Note: There must be at least one byte after the opcode in a packaged object.
    VERIFY(actual_data_after_opcode.size() >= 1);

    Vector<u8> package_encoding_other_bytes;
    {
        size_t index = 1;
        while (index < 4 && index < actual_data_after_opcode.size()) {
            package_encoding_other_bytes.append(actual_data_after_opcode[index]);
            index++;
        }
    }
    return Package::parse_encoded_package_length(actual_data_after_opcode[0], package_encoding_other_bytes);
}

bool TermObjectEnumerator::current_object_is_package() const
{
    VERIFY(current_opcode().opcode().has_value());
    switch (current_opcode().opcode().value()) {
    case EncodedObjectOpcode::Opcode::Scope:
    case EncodedObjectOpcode::Opcode::BankField:
    case EncodedObjectOpcode::Opcode::Field:
    case EncodedObjectOpcode::Opcode::PowerResource:
    case EncodedObjectOpcode::Opcode::Processor:
    case EncodedObjectOpcode::Opcode::Device:
    case EncodedObjectOpcode::Opcode::IndexField:
    case EncodedObjectOpcode::Opcode::Method:
        return true;
    default:
        return false;
    }
}

UNMAP_AFTER_INIT Optional<Span<u8 const>> TermObjectEnumerator::current_data_remainder(SkipPackageSizeEncoding skip_package_size_encoding = SkipPackageSizeEncoding::No) const
{

    // Note: For every packaged object, we can return a sliced span instead of the parent span + opcode to the end of it.
    // For every other object that has a fixed known length, we return that data. For anything else, we just return the entire data
    // just after the opcode to the end of the parent span.

    if (!current_object_is_package()) {
        return possible_data_remainder_after_opcode();
    }

    auto possible_data_remainder = possible_data_remainder_after_opcode();
    if (!possible_data_remainder.has_value())
        return {};
    auto actual_data_after_opcode = possible_data_remainder.value();

    // Note: There must be at least one byte after the opcode in a packaged object.
    VERIFY(actual_data_after_opcode.size() >= 1);

    Vector<u8> package_encoding_other_bytes;
    {
        size_t index = 1;
        while (index < 4 && index < actual_data_after_opcode.size()) {
            package_encoding_other_bytes.append(actual_data_after_opcode[index]);
            index++;
        }
    }
    auto package_decoding_result = Package::parse_encoded_package_length(actual_data_after_opcode[0], package_encoding_other_bytes);
    if (skip_package_size_encoding == SkipPackageSizeEncoding::Yes)
        return actual_data_after_opcode.slice(package_decoding_result.encoding_length, package_decoding_result.package_size - package_decoding_result.encoding_length);
    return actual_data_after_opcode.slice(0, package_decoding_result.encoding_length + package_decoding_result.package_size);
}

UNMAP_AFTER_INIT Optional<Span<u8 const>> TermObjectEnumerator::possible_data_remainder_after_opcode() const
{
    if (current_opcode().has_extended_prefix()) {
        if ((m_encoded_bytes.size() - m_decode_pointer.current_pointer() < 2))
            return {};
        return m_encoded_bytes.slice(m_decode_pointer.current_pointer() + 2);
    }
    if ((m_encoded_bytes.size() - m_decode_pointer.current_pointer() < 1))
        return {};
    return m_encoded_bytes.slice(m_decode_pointer.current_pointer() + 1);
}

}
