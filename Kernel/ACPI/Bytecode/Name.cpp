/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Bytecode/Name.h>
#include <Kernel/ACPI/Bytecode/Package.h>
#include <Kernel/ACPI/Bytecode/TermObjectEvaluator.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

NonnullRefPtr<Name> Name::must_create(const TermObjectEnumerator& parent_enumerator, Span<const u8> encoded_name)
{
    auto new_name = adopt_ref_if_nonnull(new (nothrow) Name(encoded_name)).release_nonnull();
    new_name->eval_associated_data(parent_enumerator);
    return new_name;
}

void Name::eval_associated_data(const TermObjectEnumerator& parent_enumerator)
{
    auto name_data_bytes = parent_enumerator.current_data_remainder(TermObjectEnumerator::SkipPackageSizeEncoding::No);
    // Note: We assert here because a scope with no data in it makes no sense.
    VERIFY(name_data_bytes.has_value());
    VERIFY(name_data_bytes.value().size());
    // Note: Start the evaluation from the end of the Scope NameString.
    TermObjectEvaluator evaluator(name_data_bytes.value().slice(m_name_string->encoded_length()));
    dbgln_if(ACPI_AML_DEBUG, "Name - evaluated_value {}", (u32)evaluator.current_opcode().opcode().value());
    auto evaluated_value = evaluator.try_to_evaluate_value();
    switch (evaluated_value.type()) {
    case EvaluatedValue::Type::ByteData:
        m_possible_value.byte_data = evaluated_value.as_u8();
        m_evaluated_data_type = AssociatedTypeData::ByteData;
        break;
    case EvaluatedValue::Type::WordData:
        m_possible_value.word_data = evaluated_value.as_u16();
        m_evaluated_data_type = AssociatedTypeData::WordData;
        break;
    case EvaluatedValue::Type::DWordData:
        m_possible_value.dword_data = evaluated_value.as_u32();
        m_evaluated_data_type = AssociatedTypeData::DWordData;
        break;
    case EvaluatedValue::Type::QWordData:
        m_possible_value.qword_data = evaluated_value.as_u64();
        m_evaluated_data_type = AssociatedTypeData::QWordData;
        break;
    case EvaluatedValue::Type::Const:
        m_evaluated_data_type = AssociatedTypeData::ConstObject;
        switch (evaluated_value.as_const_object_type()) {
        case ConstObjectType::One:
            m_possible_value.byte_data = 1;
            break;
        case ConstObjectType::Ones:
            m_possible_value.byte_data = 0xFF;
            break;
        case ConstObjectType::Zero:
            m_possible_value.byte_data = 0x00;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    case EvaluatedValue::Type::String:
        m_evaluated_data_type = AssociatedTypeData::NullTerminatedString;
        m_null_terminated_string = KString::try_create(evaluated_value.as_string());
        break;
    case EvaluatedValue::Type::Buffer:
        m_evaluated_data_type = AssociatedTypeData::Buffer;
        m_possible_byte_buffer.data = evaluated_value.as_byte_buffer().data;
        m_possible_byte_buffer.size = evaluated_value.as_byte_buffer().size;
        break;
    case EvaluatedValue::Type::Package:
        m_evaluated_data_type = AssociatedTypeData::Package;
        m_possible_elements_package = evaluated_value.as_package();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

u8 Name::as_byte_data() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::ByteData);
    return m_possible_value.byte_data;
}
u16 Name::as_word_data() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::WordData);
    return m_possible_value.word_data;
}
u32 Name::as_dword_data() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::DWordData);
    return m_possible_value.dword_data;
}
u64 Name::as_qword_data() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::QWordData);
    return m_possible_value.qword_data;
}
ConstObjectType Name::as_const_object() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::ConstObject);
    return m_possible_value.const_opcode;
}
const KString* Name::as_null_terminated_string() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::NullTerminatedString);
    VERIFY(m_null_terminated_string);
    return m_null_terminated_string.ptr();
}
ByteBufferPackage Name::as_byte_buffer() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::Buffer);
    return m_possible_byte_buffer;
}
RefPtr<ElementsPackage> Name::as_elements_package() const
{
    VERIFY(m_evaluated_data_type == AssociatedTypeData::Package);
    VERIFY(m_possible_elements_package);
    return m_possible_elements_package;
}

size_t Name::encoded_length() const
{
    VERIFY(m_evaluated_data_type != AssociatedTypeData::NotEvaluated);
    // Note: We have the NameString and at least one byte after it
    size_t length = m_name_string->encoded_length() + 1;
    dbgln_if(ACPI_AML_DEBUG, "Name m_evaluated_data_type is {}", (u32)m_evaluated_data_type);
    switch (m_evaluated_data_type) {
    case AssociatedTypeData::TermOpcode:
        length += EncodedTermOpcode(m_possible_encoded_term_opcode).length();
        // FIXME: Figure out the right amount of count to append!
        TODO();
        break;
    case AssociatedTypeData::NullTerminatedString:
        VERIFY(m_null_terminated_string);
        length += m_null_terminated_string->view().length();
        // Note: Add one for the null terminator
        length += 1;
        break;
    case AssociatedTypeData::QWordData:
        length += 8;
        break;
    case AssociatedTypeData::DWordData:
        length += 4;
        break;
    case AssociatedTypeData::WordData:
        length += 2;
        break;
    case AssociatedTypeData::ByteData:
        length += 1;
        break;
    case AssociatedTypeData::ConstObject:
        // Note: The encoded byte of the ConstObject was already appended to the length before!
        break;
    case AssociatedTypeData::Buffer:
        length += m_possible_byte_buffer.size.package_size;
        break;
    case AssociatedTypeData::Package:
        VERIFY(m_possible_elements_package);
        length += m_possible_elements_package->encoded_length();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    dbgln_if(ACPI_AML_DEBUG, "Name length is {}", length);
    return length;
}

Name::Name(Span<const u8> encoded_name)
    : NamedObject(encoded_name)
{
}

}
