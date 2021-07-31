/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <Kernel/ACPI/Bytecode/ElementsPackage.h>
#include <Kernel/ACPI/Bytecode/TermObjectEvaluator.h>

namespace Kernel::ACPI {

TermObjectEvaluator::TermObjectEvaluator(Span<const u8> encoded_bytes)
    : m_encoded_bytes(encoded_bytes)
{
}

EncodedTermOpcode TermObjectEvaluator::current_opcode() const
{
    if (m_encoded_bytes.size() >= 2)
        return EncodedTermOpcode({ m_encoded_bytes[m_byte_pointer], m_encoded_bytes[m_byte_pointer + 1] });
    return EncodedTermOpcode(m_encoded_bytes[m_byte_pointer]);
}

EvaluatedValue TermObjectEvaluator::try_to_evaluate_value() const
{
    // FIXME: Add a sanity check to ensure we don't take values out of the Span boundaries.
    if (!current_opcode().opcode().has_value())
        return {};

    switch (current_opcode().opcode().value()) {
    case EncodedTermOpcode::Opcode::BytePrefix:
        return load_byte();
    case EncodedTermOpcode::Opcode::WordPrefix:
        return load_word();
    case EncodedTermOpcode::Opcode::DWordPrefix:
        return load_dword();
    case EncodedTermOpcode::Opcode::QWordPrefix:
        return load_qword();
    case EncodedTermOpcode::Opcode::Ones:
        return load_ones();
    case EncodedTermOpcode::Opcode::One:
        return load_one();
    case EncodedTermOpcode::Opcode::Zero:
        return load_zero();
    case EncodedTermOpcode::Opcode::StringPrefix:
        return load_null_terminated_string();
    case EncodedTermOpcode::Opcode::Buffer:
        return load_buffer();
    case EncodedTermOpcode::Opcode::Package:
        return load_package();
    default:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

size_t TermObjectEvaluator::overall_terms_span_length() const
{
    // FIXME: Find a better way to put the hardcoded lengths
    VERIFY(current_opcode().opcode().has_value());
    switch (current_opcode().opcode().value()) {
    case EncodedTermOpcode::Opcode::BytePrefix:
        return 2;
    case EncodedTermOpcode::Opcode::WordPrefix:
        return 3;
    case EncodedTermOpcode::Opcode::DWordPrefix:
        return 5;
    case EncodedTermOpcode::Opcode::QWordPrefix:
        return 9;
    case EncodedTermOpcode::Opcode::Ones:
        return 1;
    case EncodedTermOpcode::Opcode::One:
        return 1;
    case EncodedTermOpcode::Opcode::Zero:
        return 1;
    default:
        VERIFY_NOT_REACHED();
    }
    return {};
}

EvaluatedValue TermObjectEvaluator::load_ones() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::Ones);
    return EvaluatedValue(ConstObjectType::Ones);
}
EvaluatedValue TermObjectEvaluator::load_one() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::One);
    return EvaluatedValue(ConstObjectType::One);
}
EvaluatedValue TermObjectEvaluator::load_zero() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::Zero);
    return EvaluatedValue(ConstObjectType::Zero);
}

EvaluatedValue TermObjectEvaluator::load_byte() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::BytePrefix);
    u8 integer = 0;
    ByteReader::load<u8>(m_encoded_bytes.slice(1, 1).data(), integer);
    return EvaluatedValue(integer);
}

EvaluatedValue TermObjectEvaluator::load_word() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::WordPrefix);
    u16 integer = 0;
    ByteReader::load<u16>(m_encoded_bytes.slice(1, 2).data(), integer);
    return EvaluatedValue(integer);
}

EvaluatedValue TermObjectEvaluator::load_dword() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::DWordPrefix);
    u32 integer = 0;
    ByteReader::load<u32>(m_encoded_bytes.slice(1, 4).data(), integer);
    return EvaluatedValue(integer);
}

EvaluatedValue TermObjectEvaluator::load_qword() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::QWordPrefix);
    u64 integer = 0;
    ByteReader::load<u64>(m_encoded_bytes.slice(1, 8).data(), integer);
    return EvaluatedValue(integer);
}

EvaluatedValue TermObjectEvaluator::load_null_terminated_string() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::StringPrefix);
    return EvaluatedValue(StringView(reinterpret_cast<const char*>(m_encoded_bytes.slice(1).data())));
}

EvaluatedValue TermObjectEvaluator::load_buffer() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::Buffer);
    // FIXME: Find a way to use something like TermObjectEnumerator::current_data_remainder()!
    auto rest_of_encoded_bytes = m_encoded_bytes.slice(1);
    Vector<u8> package_encoding_other_bytes;
    {
        size_t index = 1;
        while (index < 4 && index < rest_of_encoded_bytes.size()) {
            package_encoding_other_bytes.append(rest_of_encoded_bytes[index]);
            index++;
        }
    }

    auto result = Package::parse_encoded_package_length(rest_of_encoded_bytes[0], package_encoding_other_bytes);
    dbgln_if(ACPI_AML_DEBUG, "Buffer package length {}, encoding package length {}", result.package_size, result.encoding_length);

    TermObjectEvaluator buffer_size_evaluator(rest_of_encoded_bytes.slice(result.encoding_length));
    auto possible_buffer_size_value = buffer_size_evaluator.try_to_evaluate_value();
    VERIFY(possible_buffer_size_value.type() != EvaluatedValue::Type::NotEvaluated);
    // FIXME: For now we support fixed-size buffers, but AML allows to have dynamically-evaluated sized buffers!
    auto buffer_size_value = possible_buffer_size_value.as_unsigned_integer();
    auto buffer_data = rest_of_encoded_bytes.slice(result.encoding_length + buffer_size_evaluator.overall_terms_span_length(), buffer_size_value);
    dbgln_if(ACPI_AML_DEBUG, "Buffer data length {}", buffer_size_value);
    return EvaluatedValue(ByteBuffer::copy(buffer_data), result);
}

EvaluatedValue TermObjectEvaluator::load_package() const
{
    VERIFY(current_opcode().opcode().has_value());
    VERIFY(current_opcode().opcode().value() == EncodedTermOpcode::Opcode::Package);
    // FIXME: Find a way to use something like TermObjectEnumerator::current_data_remainder()!
    auto rest_of_encoded_bytes = m_encoded_bytes.slice(1);
    Vector<u8> package_encoding_other_bytes;
    {
        size_t index = 1;
        while (index < 4 && index < rest_of_encoded_bytes.size()) {
            package_encoding_other_bytes.append(rest_of_encoded_bytes[index]);
            index++;
        }
    }
    auto result = Package::parse_encoded_package_length(rest_of_encoded_bytes[0], package_encoding_other_bytes);
    dbgln_if(ACPI_AML_DEBUG, "Package of Elements, package length {}, encoding package length {}", result.package_size, result.encoding_length);
    // Note: We slice rest_of_encoded_bytes to start with result.encoding_length + 1, because we have NumElements which occupies 1 byte.
    return EvaluatedValue(*ElementsPackage::must_create(result.package_size, result.encoding_length, rest_of_encoded_bytes.slice(result.encoding_length + 1, result.package_size - result.encoding_length - 1)));
}

/*void TermObjectEvaluator::append_local0()
{
    TODO();
}
void TermObjectEvaluator::append_local1()
{
    TODO();
}
void TermObjectEvaluator::append_local2()
{
    TODO();
}
void TermObjectEvaluator::append_local3()
{
    TODO();
}
void TermObjectEvaluator::append_local4()
{
    TODO();
}
void TermObjectEvaluator::append_local5()
{
    TODO();
}
void TermObjectEvaluator::append_local6()
{
    TODO();
}
void TermObjectEvaluator::append_local7()
{
    TODO();
}
void TermObjectEvaluator::append_arg0()
{
    TODO();
}
void TermObjectEvaluator::append_arg1()
{
    TODO();
}
void TermObjectEvaluator::append_arg2()
{
    TODO();
}
void TermObjectEvaluator::append_arg3()
{
    TODO();
}
void TermObjectEvaluator::append_arg4()
{
    TODO();
}
void TermObjectEvaluator::append_arg5()
{
    TODO();
}
void TermObjectEvaluator::append_arg6()
{
    TODO();
}
void TermObjectEvaluator::append_zero()
{
    TODO();
}
void TermObjectEvaluator::append_one()
{
    TODO();
}
void TermObjectEvaluator::append_ones()
{
    TODO();
}
void TermObjectEvaluator::append_buffer()
{
    TODO();
}
void TermObjectEvaluator::append_package()
{
    TODO();
}
void TermObjectEvaluator::append_varpackage()
{
    TODO();
}
void TermObjectEvaluator::append_add()
{
    TODO();
}
void TermObjectEvaluator::append_and()
{
    TODO();
}
void TermObjectEvaluator::append_concat()
{
    TODO();
}
void TermObjectEvaluator::append_concatres()
{
    TODO();
}
void TermObjectEvaluator::append_copyobject()
{
    TODO();
}
void TermObjectEvaluator::append_decrement()
{
    TODO();
}
void TermObjectEvaluator::append_derefof()
{
    TODO();
}
void TermObjectEvaluator::append_divide()
{
    TODO();
}
void TermObjectEvaluator::append_findsetleftbit()
{
    TODO();
}
void TermObjectEvaluator::append_findsetrightbit()
{
    TODO();
}
void TermObjectEvaluator::append_increment()
{
    TODO();
}
void TermObjectEvaluator::append_index()
{
    TODO();
}
void TermObjectEvaluator::append_land()
{
    TODO();
}
void TermObjectEvaluator::append_lequal()
{
    TODO();
}
void TermObjectEvaluator::append_lgreater()
{
    TODO();
}
void TermObjectEvaluator::append_lgreaterequal()
{
    TODO();
}
void TermObjectEvaluator::append_lless()
{
    TODO();
}
void TermObjectEvaluator::append_llessequal()
{
    TODO();
}
void TermObjectEvaluator::append_lnot()
{
    TODO();
}
void TermObjectEvaluator::append_lnotequal()
{
    TODO();
}
void TermObjectEvaluator::append_lor()
{
    TODO();
}
void TermObjectEvaluator::append_match()
{
    TODO();
}
void TermObjectEvaluator::append_mid()
{
    TODO();
}
void TermObjectEvaluator::append_mod()
{
    TODO();
}
void TermObjectEvaluator::append_multiply()
{
    TODO();
}
void TermObjectEvaluator::append_nand()
{
    TODO();
}
void TermObjectEvaluator::append_nor()
{
    TODO();
}
void TermObjectEvaluator::append_not()
{
    TODO();
}
void TermObjectEvaluator::append_objecttype()
{
    TODO();
}
void TermObjectEvaluator::append_or()
{
    TODO();
}
void TermObjectEvaluator::append_refof()
{
    TODO();
}
void TermObjectEvaluator::append_shiftleft()
{
    TODO();
}
void TermObjectEvaluator::append_shiftright()
{
    TODO();
}
void TermObjectEvaluator::append_sizeof()
{
    TODO();
}
void TermObjectEvaluator::append_store()
{
    TODO();
}
void TermObjectEvaluator::append_subtract()
{
    TODO();
}
void TermObjectEvaluator::append_tobuffer()
{
    TODO();
}
void TermObjectEvaluator::append_todecimalstring()
{
    TODO();
}
void TermObjectEvaluator::append_tohexstring()
{
    TODO();
}
void TermObjectEvaluator::append_tointeger()
{
    TODO();
}
void TermObjectEvaluator::append_tostring()
{
    TODO();
}
void TermObjectEvaluator::append_xor()
{
    TODO();
}
void TermObjectEvaluator::append_byteprefix()
{
    TODO();
}
void TermObjectEvaluator::append_wordprefix()
{
    TODO();
}
void TermObjectEvaluator::append_dwordprefix()
{
    TODO();
}
void TermObjectEvaluator::append_qwordprefix()
{
    TODO();
}
void TermObjectEvaluator::append_stringprefix()
{
    TODO();
}
void TermObjectEvaluator::append_revision()
{
    TODO();
}
void TermObjectEvaluator::append_debugop()
{
    TODO();
}
void TermObjectEvaluator::append_acquire()
{
    TODO();
}
void TermObjectEvaluator::append_condrefof()
{
    TODO();
}
void TermObjectEvaluator::append_frombcd()
{
    TODO();
}
void TermObjectEvaluator::append_loadtable()
{
    TODO();
}
void TermObjectEvaluator::append_timer()
{
    TODO();
}
void TermObjectEvaluator::append_tobcd()
{
    TODO();
}
void TermObjectEvaluator::append_wait()
{
    TODO();
}
void TermObjectEvaluator::append_break()
{
    TODO();
}
void TermObjectEvaluator::append_breakpoint()
{
    TODO();
}
void TermObjectEvaluator::append_continue()
{
    TODO();
}
void TermObjectEvaluator::append_else()
{
    TODO();
}
void TermObjectEvaluator::append_ifelse()
{
    TODO();
}
void TermObjectEvaluator::append_noop()
{
    TODO();
}
void TermObjectEvaluator::append_notify()
{
    TODO();
}
void TermObjectEvaluator::append_return()
{
    TODO();
}
void TermObjectEvaluator::append_while()
{
    TODO();
}
void TermObjectEvaluator::append_fatal()
{
    TODO();
}
void TermObjectEvaluator::append_load()
{
    TODO();
}
void TermObjectEvaluator::append_release()
{
    TODO();
}
void TermObjectEvaluator::append_reset()
{
    TODO();
}
void TermObjectEvaluator::append_signal()
{
    TODO();
}
void TermObjectEvaluator::append_sleep()
{
    TODO();
}
void TermObjectEvaluator::append_stall()
{
    TODO();
}*/

}
