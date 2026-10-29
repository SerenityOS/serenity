/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibACPI/Definitions.h>
#include <LibACPI/OpcodeFilters.h>
#include <LibACPI/OpcodeFrame.h>

#ifdef KERNEL
#    define warnln dbgln
#endif

namespace ACPI::AML {

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#aml-byte-stream-byte-values
constexpr static Array<OperandType, 1> EMPTY_OPERANDS = { OperandType::End };

constexpr static Array<OperandType, 3> SCOPE_OPERANDS = { OperandType::PackageLength, OperandType::NameString, OperandType::End };
constexpr static Array<OperandType, 3> NAME_OPERANDS = { OperandType::NameString, OperandType::DataRefObject, OperandType::End };

constexpr static Array<OperandType, 2> BYTE_CONST_OPERANDS = { OperandType::ByteData, OperandType::End };
constexpr static Array<OperandType, 2> WORD_CONST_OPERANDS = { OperandType::WordData, OperandType::End };
constexpr static Array<OperandType, 2> DWORD_CONST_OPERANDS = { OperandType::DWordData, OperandType::End };
constexpr static Array<OperandType, 2> QWORD_CONST_OPERANDS = { OperandType::QWordData, OperandType::End };
constexpr static Array<OperandType, 2> STRING_CONST_OPERANDS = { OperandType::String, OperandType::End };

constexpr static Array<OperandType, 4> BUFFER_CONST_OPERANDS = {
    OperandType::PackageLength,
    OperandType::TermArg,
    OperandType::ByteBuffer,
    OperandType::End
};

constexpr static Array<OperandType, 4> METHOD_OPERANDS = {
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::End
};

constexpr static Array<OperandType, 5> OP_REGION_OPERANDS = {
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::TermArg,
    OperandType::TermArg,
    OperandType::End
};

constexpr static Array<OperandType, 7> FIELD_OPERANDS = {
    OperandType::MarkAsCount, OperandType::PackageLength,
    OperandType::NameString,
    OperandType::ByteData,
    OperandType::RepeatNextBytes, OperandType::FieldElement,
    OperandType::End
};

constexpr static Array<OperandType, 3> DEVICE_OPERANDS = {
    OperandType::PackageLength,
    OperandType::NameString,
    OperandType::End
};

static ErrorOr<ReadonlySpan<OperandType>> get_opcode_operand_template(Opcode opcode)
{
    switch (opcode) {
    case Opcode::BytePrefix:
        return BYTE_CONST_OPERANDS.span();
    case Opcode::WordPrefix:
        return WORD_CONST_OPERANDS.span();
    case Opcode::DWordPrefix:
        return DWORD_CONST_OPERANDS.span();
    case Opcode::QWordPrefix:
        return QWORD_CONST_OPERANDS.span();
    case Opcode::StringPrefix:
        return STRING_CONST_OPERANDS.span();
    case Opcode::BufferOp:
        return BUFFER_CONST_OPERANDS.span();

    case Opcode::ScopeOp:
        return SCOPE_OPERANDS.span();
    case Opcode::NameOp:
        return NAME_OPERANDS.span();
    case Opcode::MethodOp:
        return METHOD_OPERANDS.span();
    case Opcode::OpRegionOp:
        return OP_REGION_OPERANDS.span();
    case Opcode::FieldOp:
        return FIELD_OPERANDS.span();
    case Opcode::DeviceOp:
        return DEVICE_OPERANDS.span();

    case Opcode::ZeroOp:
    case Opcode::OneOp:
        return EMPTY_OPERANDS.span();

    default:
        warnln("ACPI: get_opcode_operand_template: Unknown opcode template: {}!", opcode);
        return Error::from_string_view_or_print_error_and_return_errno("Unknown opcode template!"sv, EINVAL);
    }
}

ErrorOr<OpcodeFrame> OpcodeFrame::from_opcode(Opcode opcode, size_t start)
{
    auto operand_template = TRY(get_opcode_operand_template(opcode));
    return OpcodeFrame(opcode, TRY(Vector<OperandType>::from_span(operand_template)), start);
}

ErrorOr<OpcodeFrame> OpcodeFrame::root_frame()
{
    return OpcodeFrame({}, TRY(Vector<OperandType>::from_span(EMPTY_OPERANDS)), 0);
}

OperandType OpcodeFrame::peek_next_operand()
{
    auto operand = m_operand_template[m_operand_template_index];
    if (operand == OperandType::MarkAsCount || operand == OperandType::RepeatNextBytes)
        operand = m_operand_template[m_operand_template_index + 1];

    return operand;
}

bool OpcodeFrame::filter_opcode_for_next_operand(Opcode opcode)
{
    auto pending_operand = peek_next_operand();

    switch (pending_operand) {
    case OperandType::NameString:
    case OperandType::PackageLength:
    case OperandType::ByteData:
    case OperandType::WordData:
    case OperandType::DWordData:
    case OperandType::QWordData:
    case OperandType::String:
    case OperandType::ByteBuffer:
        return false;

    case OperandType::FieldElement:
        return true;

    case OperandType::TermArg:
        return term_arg_filter(opcode);

    case OperandType::DataRefObject:
        return data_ref_object_filter(opcode);

    case OperandType::End:
        return term_filter(opcode);

    case OperandType::MarkAsCount:
    case OperandType::RepeatNextBytes:
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<OperandType> OpcodeFrame::begin_operand()
{
    auto operand = m_operand_template[m_operand_template_index];

    if (operand == OperandType::MarkAsCount) {
        m_counter_value_index = m_operands.size();
    } else if (operand == OperandType::RepeatNextBytes) {
        if (!m_counter.has_value()) {
            auto count = TRY(m_operands.at(m_counter_value_index).as<u64>());
            if (count == 0) {
                m_counter = {};
                m_operand_template_index += 2;
            } else {
                m_counter = count;
            }
        }
    }

    return peek_next_operand();
}

ErrorOr<void> OpcodeFrame::finish_operand(size_t current_offset)
{
    auto operand_type = m_operand_template[m_operand_template_index];
    if (operand_type == OperandType::MarkAsCount) {
        m_operand_template_index += 2;
        return {};
    }

    if (operand_type == OperandType::RepeatNextBytes) {
        if (current_offset > m_start + m_counter.value()) {
            m_counter = {};
            m_operand_template_index += 2;
            return {};
        }

        return {};
    }

    m_operand_template_index++;
    return {};
}

ErrorOr<void> OpcodeFrame::finish_operand(Object const& operand, size_t current_offset)
{
    TRY(m_operands.try_append(operand));
    return finish_operand(current_offset);
}

}
